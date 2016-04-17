package services;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by jiaweizhang on 4/12/16.
 */
public class BalanceService {
    private RedisService r;
    private Timer readBalanceTimer;
    private Timer serverBalanceTimer;

    public BalanceService(RedisService r) {
        this.r = r;
        readBalanceTimer = new Timer();
        serverBalanceTimer = new Timer();
        startBalancers();
    }

    private void startBalancers() {
        // run every 10 seconds - in production much higher
        readBalanceTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                readBalance();
            }
        }, 10*1000, 10*1000);


        // run every 22 seconds
        serverBalanceTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                serverBalance();
            }
        }, 22*1000, 22*1000);
    }

    private void readBalance() {
        r.readBalance();
    }

    private void serverBalance() {
        r.serverBalance();
    }

}
