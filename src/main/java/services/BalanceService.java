package services;

/**
 * Created by jiaweizhang on 4/12/16.
 */
public class BalanceService {
    private RedisService r;

    public BalanceService(RedisService r) {
        this.r = r;
        startBalancer();
    }

    private void startBalancer() {
        // on a timer run balance()
        readBalance();
    }

    private void readBalance() {
        r.readBalance();
    }

}
