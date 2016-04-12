import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;

/**
 * Created by jiaweizhang on 4/12/16.
 */
public class Application {
    private static Jedis jedis;
    public static void main(String args[]) {
        jedis = new Jedis("localhost", 6379);
        System.out.println("Connected to Redis");

        check();
        add();

    }

    private static void add() {
        jedis.lpush("first-key", "Jiawei");
        jedis.lpush("first-key", "Alex");

        // only get the first 6
        List<String> list = jedis.lrange("first-key", 0, 5);
        for (String s : list) {
            System.out.println("Val: " + s);
        }
    }

    private static void check() {
        Set<String> list = jedis.keys("*");
        for (String s : list) {
            System.out.println("Key: " +s);
        }
    }
}
