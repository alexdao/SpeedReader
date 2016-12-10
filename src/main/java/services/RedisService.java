package services;

import org.eclipse.jetty.server.Server;
import redis.clients.jedis.Jedis;

import java.util.*;

public class RedisService {
    private final int NUM_OF_SLAVES = 10;
    // look through last 100 reads - higher in production
    private final int READ_BALANCE_PAST_ACCESSES = 40;
    // just for test
    private Map<String, String> map = new HashMap<>();

    // Follower list
    private List<FollowerService> followers;

    private static final String SERVER_PREFIX = "server";
    private static final String ORIGINAL_PREFIX = "original";
    private static final String ALL_FILES = "all_files";
    private static final String READS = "reads";

    private Jedis jedis;
    private Random random;

    public RedisService() {
        random = new Random();
        jedis = new Jedis("localhost", 6379);
        System.out.println("Connected to Redis");

        initializeFollowers();
    }

    private void initializeFollowers() {
        followers = new ArrayList<>();
        for (int i = 0; i < NUM_OF_SLAVES; i++) {
            followers.add(new FollowerService());
        }
    }

    void flush() {
        jedis.flushDB();
    }

    synchronized int read(String fileName) {
        // perform name mapping - assumes no duplicates
        String key = map.get(fileName);

        if (key == null) {
            System.out.println("File does not exist");
            return -1;
        }

        // randomly select follower to read from
        String chosenServer = jedis.srandmember(key);
        int followerNum = Integer.parseInt(chosenServer);
        List<String> fileData = followers.get(followerNum).read(fileName).getValues();
        // TODO: Resolve fileData

        // add to server actions
        long ts = System.currentTimeMillis();
        String listtype = jedis.type(chosenServer);
        jedis.lpush(chosenServer, Long.toString(ts));

        // add to list to keep track of its reads
        jedis.lpush(READS, key);

        System.out.println("Reading file with name " + fileName + " from server " + chosenServer);
        return Integer.parseInt(chosenServer);
    }

    // Randomly resolve list by picking a random value
    private String resolveData(List<String> fileData) {
        return fileData.get(random.nextInt(fileData.size()));
    }

    synchronized int write(String fileName, String fileData) {
        // always assumes that write is a create and that filenames are unique

        // append timestamp and random int - basic guarantee of uniqueness
        String prepend = Integer.toString(Math.abs(random.nextInt())) + "_";
        String key = prepend + fileName;

        // just for testing
        map.put(fileName, key);

        // choose random location for new write
        int chosenServer = random.nextInt(NUM_OF_SLAVES);
        // Send data to follower
        followers.get(chosenServer).write(fileName, fileData, 0);
        String chosenServerString = Integer.toString(chosenServer);

        // add file to list of all files
        // Note: (this key does not conflict with any others)
        jedis.sadd(ALL_FILES, key);

        // set the server of the original copy
        String originalKey = ORIGINAL_PREFIX + key;
        jedis.set(originalKey, chosenServerString);

        // add to server set
        jedis.sadd(key, chosenServerString);

        // add to server-file map
        String serverMap = SERVER_PREFIX + chosenServerString;
        jedis.sadd(serverMap, key);

        // add to server actions
        long ts = System.currentTimeMillis();
        jedis.lpush(chosenServerString, Long.toString(ts));

        System.out.println("Writing file with name " + fileName + " to server " + chosenServerString + " with unique key " + key);
        return chosenServer;
    }

    //TODO: Send fileData to replicas
    synchronized void readBalance() {
        System.out.println("Started read rebalance");
        List<String> lastReads = jedis.lrange(READS, 0, READ_BALANCE_PAST_ACCESSES);

        Map<String, Integer> readCounts = new HashMap<>();

        //init all files to 1
        Set<String> all_files = jedis.smembers(ALL_FILES);
        for (String file : all_files) {
            readCounts.put(file, 1);
        }

        for (String read : lastReads) {
            if (readCounts.containsKey(read)) {
                readCounts.put(read, readCounts.get(read) + 1);
            } else {
                readCounts.put(read, 1);
            }
        }

        // determines how many copies are desired
        Map<String, Integer> dupCounts = new HashMap<>();
        for (String file : readCounts.keySet()) {
            if (readCounts.get(file) >= 20) {
                dupCounts.put(file, NUM_OF_SLAVES);
            } else {
                // always at least 1
                dupCounts.put(file, readCounts.get(file) / 2 + 1);
            }
        }

        // TODO - separate removal so it always works
        for (String key : readCounts.keySet()) {
            // check how many copies currently exist
            String originalKey = ORIGINAL_PREFIX + key;
            String originalServer = jedis.get(originalKey);
            // ensure this is never deleted

            // servers on which the file exists
            Set<String> allServers = jedis.smembers(key);
            System.out.print("File " + key + " currently exist on: ");
            for (String s : allServers) {
                System.out.print(s + " ");
            }
            System.out.println();

            int desiredDups = dupCounts.get(key);
            if (desiredDups == allServers.size()) {
                // do nothing if desired number is same as actual
                continue;
            } else if (desiredDups > allServers.size()) {
                // add servers randomly
                int toAdd = desiredDups - allServers.size();
                System.out.println("Adding " + toAdd + " servers for key " + key);
                Set<String> availableToAdd = new HashSet<>(allServers);
                for (int i = 0; i < toAdd; i++) {
                    String randomServer = randomNotIntSet(NUM_OF_SLAVES, availableToAdd);
                    availableToAdd.add(randomServer);
                    jedis.sadd(key, randomServer);
                    jedis.sadd(SERVER_PREFIX + randomServer, key);
                }
            } else {
                // remove servers randomly
                int toRemove = allServers.size() - desiredDups;
                System.out.println("Removing " + toRemove + " servers for key " + key);
                List<String> availableToRemove = new ArrayList<>(allServers);

                // ensures that original server can never be removed
                availableToRemove.remove(originalServer);
                for (int i = 0; i < toRemove; i++) {
                    Collections.shuffle(availableToRemove);
                    String serverToRemove = availableToRemove.remove(0);

                    jedis.srem(key, serverToRemove);
                    jedis.srem(SERVER_PREFIX + serverToRemove, key);
                }
            }
        }
        System.out.println("Finished read rebalancing");
    }

    //TODO: Send fileData to replicas
    synchronized void serverBalance() {
        System.out.println("Starting server rebalance");

        String mostBusy = "0";
        double mostBusyAvg = 0;
        String leastBusy = "1";
        double leastBusyAvg = Long.MAX_VALUE;
        System.out.println("Rebalancing servers: ");
        for (int i = 0; i < NUM_OF_SLAVES; i++) {
            List<String> stringTimes = jedis.lrange(Integer.toString(i), 0, 49);
            System.out.print("For server " + i + " in recent time, there have been " + stringTimes.size() + " reads at times ");
            List<Long> times = new ArrayList<>();
            for (String time : stringTimes) {
                times.add(Long.parseLong(time));
                System.out.print(time + " ");
            }
            System.out.println();

            double average;
            if (times.size() != 0) {
                average = times.get(times.size() - 1); // get earliest
            } else {
                average = 0; // not used to oldest
            }
            if (average > mostBusyAvg) {
                mostBusy = Integer.toString(i);
                mostBusyAvg = average;
            }
            if (average < leastBusyAvg) {
                leastBusy = Integer.toString(i);
                leastBusyAvg = average;
            }
        }

        // move random files from most busy to least busy

        // get random file
        String key = jedis.srandmember(SERVER_PREFIX + mostBusy);

        if (key != null) {
            // move to least busy server - doesn't matter if the server already has it
            // done to reduce load on most loaded server
            jedis.sadd(key, leastBusy);
            jedis.sadd(SERVER_PREFIX + leastBusy, key);

            // remove from old
            jedis.srem(key, mostBusy);
            jedis.srem(SERVER_PREFIX + mostBusy, key);

            System.out.println("Moved file " + key + " from " + mostBusy + " to " + leastBusy);
        }

        System.out.println("Finished server rebalancing");
    }

    private String randomNotIntSet(int range, Set<String> existing) {
        // simple to just go until you hit
        while (true) {
            int randInt = random.nextInt(range);
            if (!existing.contains(Integer.toString(randInt))) {
                return Integer.toString(randInt);
            }
        }
    }

    String getFileLocations() {
        StringBuilder sb = new StringBuilder();
        sb.append("File locations: \n");
        for (String name : map.keySet()) {
            String key = map.get(name);
            Set<String> servers = jedis.smembers(key);
            sb.append(name).append(": ");
            for (String server : servers) {
                sb.append(server).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
