import services.BalanceService;
import services.RedisService;
import services.RequestService;

/**
 * Created by jiaweizhang on 4/12/16.
 */
public class Application {
    public static void main(String args[]) {
        Application a = new Application();
        a.start();
    }

    private void start() {
        RedisService r = new RedisService();
        new RequestService(r);
        new BalanceService(r);
    }

}
