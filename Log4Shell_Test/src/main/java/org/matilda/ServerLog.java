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

public class ServerLog {
    public void logging() throws InterruptedException {
        Logger logger = LogManager.getLogger(ServerLog.class);
        AtomicInteger port = new AtomicInteger();
        AtomicBoolean hasRead = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
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
        logger.error("${jndi:ldap://127.0.0.1:" + port +"/33f00f7a-70b9-4efc-8fcd-9cde1085e17d}");
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
