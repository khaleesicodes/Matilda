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
package org.matilda.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matilda.bootstrap.ModuleProxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Tests the functionalities of the Agent and the customized transformer
 */

public class AgentMatildaTest {
    @Test
    public void testSystemExitTransformer()  {
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            System.exit(-1);
            Assertions.fail("should not have been able to exit the process");
        });
        Assertions.assertEquals("Runtime.exit not allowed for Module: matilda.test", uOE.getMessage());

        // Tests if matilda also protects against the use of reflections
        uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            Class<?> aClass = Class.forName("java.lang.System");
            Method exit = aClass.getMethod("exit", int.class);
            try {
                exit.invoke(null, 1);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            Assertions.fail("should not have been able to exit the process");
        });
        Assertions.assertEquals("Runtime.exit not allowed for Module: matilda.test", uOE.getMessage());
    }

    @Test
    public void testSystemExecTransformer() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, InterruptedException {
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            Runtime.getRuntime().exec("echo");
            Assertions.fail("should not have been able to run a process");
        });
        Assertions.assertEquals("ProceesBuilder.start(...) not allowed for Module: matilda.test", uOE.getMessage());


        // Tests if matilda also protects against the use of reflections
        uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            Class<?> aClass = Class.forName("java.lang.Runtime");
            Method exec = aClass.getMethod("exec", String.class);
            try {
                exec.invoke(Runtime.getRuntime(), "echo");
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            Assertions.fail("should not have been able to run a process");
        });
        Assertions.assertEquals("ProceesBuilder.start(...) not allowed for Module: matilda.test", uOE.getMessage());

    }

    // Negative case, test if method call is not blocked
    @Test
    public void testSystemExecTransformerNegative() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, InterruptedException {
        Class<?> aClass = Class.forName("java.lang.Runtime");
        Method exec = aClass.getMethod("exec", String.class);
        Process echo = (Process) ModuleProxy.call(Runtime.getRuntime(), exec, "echo foo");
        echo.waitFor(3, TimeUnit.SECONDS);
        Assertions.assertEquals(0, echo.exitValue());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader( echo.getInputStream()))) {
            String value = reader.readLine();
            Assertions.assertEquals("foo", value);
        }
    }

    @Test
    public void openSocketTest() {
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            Socket socket = new Socket("localhost", 9999);
            Assertions.fail("should not have been able to open a connection");
        });
        Assertions.assertEquals("Socket.connect not allowed for Module: matilda.test", exception.getMessage());

        // Tests if matilda also protects against the use of reflections
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            Class<?> aClass = Class.forName("java.net.Socket");
            var ctor = aClass.getConstructor(String.class, int.class);
            try {
                ctor.newInstance("localhost", 9999);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            Assertions.fail("should not have been able to open a connection");
        });
        Assertions.assertEquals("Socket.connect not allowed for Module: matilda.test", uOE.getMessage());

    }

    /**
     * Test the "negative" case e.g. if a connection can be achieved when permissions are granted
     * @throws InterruptedException
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Test
    public void serverConnectionNotBlockTest() throws InterruptedException, IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
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
                latch.countDown();
                // checks if connection to socket was requested
                try (Socket accept = serverSocket.accept()) {

                    try (InputStream stream = accept.getInputStream()) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        Assertions.assertEquals("Test connection", reader.readLine());
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

        // Gets Socket class
        Class<Socket> networkClass = Socket.class;
        // Get Constructor for Socket, this is needed in order to get a non transformed version of the Socket class
        Constructor<Socket> networkClassConstructor = networkClass.getConstructor(String.class, int.class);

        try(Socket clientSocket = ModuleProxy.call(networkClassConstructor,"localhost", port)){
            clientSocket.getOutputStream().write("Test connection".getBytes());
        };
        // checks if the clientSocket was able to connect to the server
        Assertions.assertTrue(hasRead.get(), "Socket connect has been blocked");

        try {
            socketRef.get().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.join();
    }

    @Test
    public void openURLTest() {
        String url = "http://localhost:9999";
        RuntimeException exception_url = Assertions.assertThrows(RuntimeException.class, () -> {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("Accept-Charset", "text/html");
            InputStream response = connection.getInputStream();
            Assertions.fail("should not have been able to open a connection");
        });
        Assertions.assertEquals("Socket.connect not allowed for Module: matilda.test", exception_url.getMessage());
    }



    @Test
    public void serverSocketTest() throws IOException{
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            try (ServerSocket serverSocket = new ServerSocket()) {
                serverSocket.bind(new InetSocketAddress("localhost", 0));
                Assertions.fail("should not have been able to open a connection");
            }
        });
        Assertions.assertEquals("Socket.connect not allowed for Module: matilda.test", exception.getMessage());

    }}