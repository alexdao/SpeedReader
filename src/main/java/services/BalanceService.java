package services;

import java.util.Timer;
import java.util.TimerTask;

public class BalanceService {
    private RedisService r;
    private Timer readBalanceTimer;
    private Timer serverBalanceTimer;

    // run every 10 seconds - in production much higher
    private static final long readBalanceInterval = 10 * 1000;
    // run every 22 seconds
    private static final long serverBalanceInterval = 22 * 1000;

    public BalanceService(RedisService r) {
        this.r = r;
        readBalanceTimer = new Timer();
        serverBalanceTimer = new Timer();
        startBalancers();
    }

    private void startBalancers() {
        readBalanceTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                readBalance();
            }
        }, readBalanceInterval, readBalanceInterval);


        /**serverBalanceTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                serverBalance();
            }
        }, serverBalanceInterval, serverBalanceInterval);*/
    }

    private void readBalance() {
        r.readBalance();
    }

    private void serverBalance() {
        r.serverBalance();
    }

}
