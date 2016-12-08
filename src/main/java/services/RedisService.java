package services;

import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * Created by jiaweizhang on 4/12/16.
 */
public class RedisService {
    private final int NUM_OF_SLAVES = 10;
    // look through last 100 reads - higher in production
    private final int READ_BALANCE_PAST_ACCESSES = 40;
    // just for test
    private Map<String, String> map = new HashMap<>();

    private Jedis jedis;
    private Random random;

    public RedisService() {
        random = new Random();
        jedis = new Jedis("localhost", 6379);
        System.out.println("Connected to Redis");
    }

    void flush() {
        jedis.flushDB();
    }

    synchronized int read(String name) {
        // perform name mapping - assumes no duplicates
        String key = map.get(name);

        if (key == null) {
            System.out.println("File does not exist");
            return -1;
        }

        // randomly select slave to read from
        String chosenServer = jedis.srandmember(key);

        // track reads for a file and where they're stored and when
        String fileToServerKey = "fileToServer" + key;
        //jedis.zadd(fileToServerKey, ts, chosenServer);

        // add to server actions
        long ts = System.currentTimeMillis();
        jedis.lpush(chosenServer, Long.toString(ts));

        // add to list to keep track of its reads
        jedis.lpush("reads", key);

        System.out.println("Reading file with name " + name + " from server " + chosenServer);
        return Integer.parseInt(chosenServer);
    }

    synchronized int write(String name) {
        // TODO - write dissemination
        // always assumes that write is a create

        String trykey = map.get(name);

        if (trykey == null) {
            // append timestamp and random int - basic guarantee of uniqueness
            //String prepend = Long.toString(System.currentTimeMillis()) + random.nextInt();
            String prepend = Integer.toString(Math.abs(random.nextInt())) + "_";
            String key =  prepend + name;

            // just for testing
            map.put(name, key);

            // choose random location for new write
            int chosenServer = random.nextInt(NUM_OF_SLAVES);
            String chosenServerString = Integer.toString(chosenServer);

            // add file to list of all files
            // Note: (this key does not conflict with any others)
            jedis.sadd("all_files", key);

            // set the server of the original copy
            String originalKey = "original" + key;
            jedis.set(originalKey, chosenServerString);

            // add to server set
            jedis.sadd(key, chosenServerString);

            //add key to server
            jedis.sadd(chosenServerString, name);

            // add to server-file map
            String serverMap = "server" + chosenServerString;
            jedis.sadd(serverMap, key);

            // add to server actions
            long ts = System.currentTimeMillis();
            jedis.lpush(chosenServerString, Long.toString(ts));

            System.out.println("Writing file with name " + name + " to server " + chosenServerString + " with unique key " + key);
            return chosenServer;
        } else {
            Set<String> servers = jedis.smembers(trykey);
            for (String server : servers) {
                long ts = System.currentTimeMillis();
                jedis.lpush(server, Long.toString(ts));

                // add to list to keep track of its reads
                jedis.lpush("reads", trykey);

                System.out.println("Reading file with name " + name + " from server " + server);
                return -1;
            }

            return -1;
        }
    }

    synchronized void readBalance() {
        System.out.println("Started read rebalance");
        List<String> lastReads = jedis.lrange("reads", 0, READ_BALANCE_PAST_ACCESSES);
        //System.out.println(Arrays.toString(lastReads.toArray()));

        Map<String, Integer> readCounts = new HashMap<>();

        //init all files to 1
        Set<String> all_files = jedis.smembers("all_files");
        for(String file : all_files){
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
            String originalKey = "original" + key;
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
                    jedis.sadd("server" + randomServer, key);
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
                    jedis.srem("server" + serverToRemove, key);
                }
            }
        }
        System.out.println("Finished read rebalancing");
    }

    synchronized void serverBalance() {
        System.out.println("Starting server rebalance");

        String mostBusy = "0";
        double mostBusyAvg = 0;
        String leastBusy = "1";
        double leastBusyAvg = Long.MAX_VALUE;
        System.out.println("Rebalancing servers: ");
        for (int i = 0; i < NUM_OF_SLAVES; i++) {
            long current = System.currentTimeMillis();

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
        String key = jedis.srandmember("server" + mostBusy);

        if (key != null) {
            // move to least busy server - doesn't matter if the server already has it
            // done to reduce load on most loaded server
            jedis.sadd(key, leastBusy);
            jedis.sadd("server" + leastBusy, key);

            // remove from old
            jedis.srem(key, mostBusy);
            jedis.srem("server" + mostBusy, key);

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
            sb.append(name + ": ");
            for (String server : servers) {
                sb.append(server + " ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
