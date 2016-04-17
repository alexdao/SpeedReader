package services;

import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * Created by jiaweizhang on 4/12/16.
 */
public class RedisService {
    private Jedis jedis;
    private Random random;
    public final int NUM_OF_SLAVES = 10;

    // just for test
    Map<String, String> map = new HashMap<String, String>();

    public RedisService() {
        random = new Random();
        jedis = new Jedis("localhost", 6379);
        System.out.println("Connected to Redis");
    }

    public int read(String name) {
        // perform name mapping - assumes no duplicates
        String key = map.get(name);

        if (key == null) {
            System.out.println("File does not exist");
            return -1;
        }

        // randomly select slave to read from
        String chosenServer = jedis.srandmember(key);

        // get double representation of timestamp
        double ts = System.currentTimeMillis();

        // track reads for a file and where they're stored and when
        String fileToServerKey = "fileToServer"+key;
        jedis.zadd(fileToServerKey, ts, chosenServer);

        // track servers and what files were read when
        jedis.zadd(chosenServer, ts, key);

        // add to list to keep track of its reads
        jedis.lpush("reads", key);

        System.out.println("Reading file with name "+ name + " from server " + chosenServer);
        return Integer.parseInt(chosenServer);
    }

    public int write(String name) {
        // TODO - write dissemination
        // always assumes that write is a create

        // append timestamp and random int - basic guarantee of uniqueness
        String prepend = Long.toString(System.currentTimeMillis()) + random.nextInt();
        String key = prepend + name;

        // just for testing
        map.put(name, key);

        // choose random location for new write
        int chosenServer = random.nextInt(NUM_OF_SLAVES);
        String chosenServerString = Integer.toString(chosenServer);

        // set the server of the original copy
        String originalKey = "original"+key;
        jedis.set(originalKey, chosenServerString);

        // add to sever set
        jedis.sadd(key, chosenServerString);

        System.out.println("Writing file with name " + name + " to server " + chosenServerString + " with unique key " + key);
        return chosenServer;
    }

    public void readBalance() {
        // look through last 1000 reads - higher in production
        List<String> lastReads = jedis.lrange("reads", 0, 999);

        Map<String, Integer> readCounts = new HashMap<String, Integer>();
        for (String read : lastReads) {
            if (readCounts.containsKey(read)) {
                readCounts.put(read, readCounts.get(read)+1);
            } else {
                readCounts.put(read, 1);
            }
        }

        // determines how many copies are desired
        Map<String, Integer> dupCounts = new HashMap<String, Integer>();
        for (String file : readCounts.keySet()) {
            if (readCounts.get(file) >= 100) {
                dupCounts.put(file, NUM_OF_SLAVES);
            } else {
                // always at least 1
                dupCounts.put(file, readCounts.get(file) % 10 + 1);
            }
        }

        for (String key : readCounts.keySet()) {
            // check how many copies currently exist
            String originalKey = "original"+key;
            String originalServer = jedis.get(originalKey);
            // ensure this is never deleted

            // servers on which the file exists
            Set<String> allServers = jedis.smembers(key);

            int desiredDups = dupCounts.get(key);
            if (desiredDups == allServers.size()) {
                // do nothing if desired number is same as actual
                continue;
            } else if (desiredDups > allServers.size()) {
                // add servers randomly
                int toAdd = desiredDups - allServers.size();
                for (int i=0; i<toAdd; i++) {
                    String randomServer = randomNotIntSet(NUM_OF_SLAVES, allServers);
                    jedis.sadd(key, randomServer);
                }
            } else {
                // remove servers randomly
                int toRemove = allServers.size() - desiredDups;
                List<String> availableToRemove = new ArrayList<String>(allServers);

                // ensures that original server can never be removed
                availableToRemove.remove(originalServer);
                for (int i=0; i<toRemove; i++) {
                    Collections.shuffle(availableToRemove);
                    String serverToRemove = availableToRemove.remove(0);

                    jedis.srem(key, serverToRemove);
                }
            }
        }
        System.out.println("Finished read rebalancing");
    }

    public void serverBalance() {
        // TODO

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
}
