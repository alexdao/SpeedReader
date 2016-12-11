import services.BalanceService;
import services.RedisService;
import services.RequestService;

public class Application {
    public static void main(String args[]) {
        Application a = new Application();
        a.start();
    }

    private void start() {
        RedisService r = new RedisService();
        new RequestService(r);
        //new BalanceService(r);
    }

}
