package services;

import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * Created by jiaweizhang on 4/12/16.
 */
public class RedisService {
    private Jedis jedis;
    private Random random;

    public RedisService() {
        random = new Random();
        jedis = new Jedis("localhost", 6379);
        System.out.println("Connected to Redis");
    }

    public void read(String path) {
        // push into read queue
        jedis.lpush("read", path);
        System.out.println("Reading file with path "+ path);
        // TODO choose least popular or random slave to read from?
        String slave = findSlave(path);
        System.out.println("Reading from slave "+ slave);
        jedis.lpush("slaveUses", slave); // add to slave being used
    }

    public void write(String path) {
        // choose optimal location if new file
        if (!jedis.hexists("originalSlave", path)) {
            String leastPopularSlave = leastPopularSlave();
            jedis.hset("originalSlave", path, leastPopularSlave);
            jedis.hset("slaves", path, leastPopularSlave);
            jedis.lpush("slaveUses", leastPopularSlave); // add to slave being used
            System.out.println("Created file on " + leastPopularSlave + " with path "+ path);
        }

        // push into write queue
        jedis.lpush("write", path);
        System.out.println("Writing file with path " + path);

        // perform write dissemination TODO
    }

    public void readBalance() {
        // get last 1000 files read
        List<String> paths = jedis.lrange("read", 0, 999);

        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (String path : paths) {
            if (!counts.containsKey(path)) {
                counts.put(path, 1);
                continue;
            }
            counts.put(path, counts.get(path) + 1);
        }

        // TODO no limit currently
        // maintain 1 slave for every 100 reads

        for (String path: counts.keySet()) {
            String slaves = jedis.hget("slaves", path);
            String[] arr = slaves.split(",");
            int currentSlaves = arr.length;
            int desiredSlaves = counts.get(path)/100 + 1;
            rebalanceSlaves(path, arr, desiredSlaves - currentSlaves);
            System.out.println("Adding " + (desiredSlaves - currentSlaves) + " slaves for read rebalancing");
        }

    }

    public void rebalanceSlaves(String path, String[] arr, int toAdd) {
        if (toAdd == 0) {
            return;
        } else if (toAdd > 0) {
            // add extra slaves
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<arr.length; i++) {
                sb.append(arr[i] +",");
            }
            for (int i=0; i<toAdd; i++) {
                sb.append(leastPopularSlave()+",");
            }
            sb.deleteCharAt(sb.length()-1); // delete last comma
            jedis.hset("slaves", path, sb.toString());
            // file dissemination TODO
        } else if (toAdd < 0) {
            // remove slaves random for now, (most popular ones later) TODO
            Set<String> toRemove = new HashSet<String>();
            for (int i=0; i<(-toAdd); i++) {
                toRemove.add(findSlave(path));
            }
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<arr.length; i++) {
                if (!toRemove.contains(arr[i])) {
                    sb.append(arr[i] +",");
                }
            }
            sb.deleteCharAt(sb.length()-1); // delete last comma
            jedis.hset("slaves", path, sb.toString());
            // file dissemination TODO
        }
    }

    private String findSlave(String path) {
        // find random for now. TODO
        String slaves = jedis.hget("slaves", path);
        String[] arr = slaves.split(",");
        return arr[random.nextInt(arr.length)];
    }

    private String leastPopularSlave() {
        // TODO have to actually iterate
        // cache this value and only re-calculate every once 'n' new writes
        List<String> slaves = jedis.lrange("slaveUses", 0, 999);
        Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        for (String slave : slaves) {
            int intSlave = Integer.parseInt(slave);
            if (!counts.containsKey(intSlave)) {
                counts.put(intSlave, 1);
                continue;
            }
            counts.put(intSlave, counts.get(intSlave) + 1);
        }

        int lowestSlave = 0;
        int lowest = Integer.MAX_VALUE;
        for (int i : counts.keySet()) {
            if (counts.get(i) < lowest) {
                lowest = counts.get(i);
                lowestSlave = i;
            }
        }
        return Integer.toString(lowestSlave);
    }

}
