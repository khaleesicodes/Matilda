/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;

/**
 * Mock Server that listens for ldap call by log4j, which is the first step in the Log4Shell exploit
 */
public class ServerLog {
    /**
     * This is a working example of the log4shell exploit. It starts a simple socket server and subsequently
     * tries to inject a ldap instruction to connect to the server via a log4j logging statement.
     * If the call succeed this method with fail with an exception otherwise logs that the ldap call was successfully
     * blocked.
     */
    public void logging() throws InterruptedException {
        Logger logger = LogManager.getLogger(ServerLog.class);
        // AtomicBoolean to check if the socket has read any bytes, that's the indicator if the exploit worked or not
        AtomicBoolean hasRead = new AtomicBoolean(false);
        // Create latch in order to allow thread to wait for other operation before continuing
        CountDownLatch latch = new CountDownLatch(1);
        // Create reference grant access to the socket to obtain the socket port and to stop the socket
        AtomicReference<ServerSocket> socketRef = new AtomicReference<>();
        Thread server = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket()) {
                socketRef.set(serverSocket);
                serverSocket.bind(new InetSocketAddress("localhost", 0));
                logger.error("start server on port {} address: {} ", serverSocket.getLocalPort(),
                        serverSocket.getInetAddress().getHostAddress());
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
            } catch (IOException e) {
                if (socketRef.get().isClosed()) {
                    // ignore
                } else {
                    throw new RuntimeException(e);
                }
            }
        });
        server.start();
        latch.await();
        int port = socketRef.get().getLocalPort();
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
