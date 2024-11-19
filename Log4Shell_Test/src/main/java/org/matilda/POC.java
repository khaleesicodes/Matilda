package org.matilda;

/**
 * Class to start and run a server to mock the Log4Shell exploit
 */
public class POC {
    public static void main(String[] args) throws InterruptedException {
        ServerLog serverLog = new ServerLog();
        serverLog.logging();
    }
}
