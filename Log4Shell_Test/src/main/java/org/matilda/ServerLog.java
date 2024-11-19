package org.matilda;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;

/**
 * Mock Server that listens for ldap call by log4j, which is the first step in the Log4Shell exploit
 */
public class ServerLog {
    public void logging() throws InterruptedException {
        Logger logger = LogManager.getLogger(ServerLog.class);
        AtomicInteger port = new AtomicInteger();
        // AtomicBoolean to check if
        AtomicBoolean hasRead = new AtomicBoolean(false);
        // Create latch in order to allow thread to wait for other operation before continuing
        CountDownLatch latch = new CountDownLatch(1);
        // Create reference to get random port nu
        AtomicReference<ServerSocket> socketRef = new AtomicReference<>();
        Thread server = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket()) {
                socketRef.set(serverSocket);
                serverSocket.bind(new InetSocketAddress("localhost", 0));
                int localPort = serverSocket.getLocalPort();
                logger.error("start server on port {} address: {} ", localPort,
                        serverSocket.getInetAddress().getHostAddress());
                port.set(localPort);
                latch.countDown();
                logger.error("starting to listen");
                // checks if connection to socket was requested
                try (Socket accept = serverSocket.accept()) {
                    logger.error("accept");
                    try (InputStream stream = accept.getInputStream()) {
                        stream.read();
                        hasRead.set(true);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        });
        server.start();
        latch.await();
        // String to exploit Log4Shell with port that listens
        logger.error("${jndi:ldap://127.0.0.1:" + port +"/matilda-poc}");
        // checks if any execution of connection has been done by the Logger - indicater for exploitation of Log4Shell
        Assertions.assertFalse(hasRead.get(), "LogForShell was not blocked by matilda");

        logger.error("Matilda has successfully blocked log4shell");

        // close the server socket and wait for the thread to die
        try {
            socketRef.get().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.join();

    }
}
