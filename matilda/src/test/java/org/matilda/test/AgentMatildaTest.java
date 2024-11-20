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
import org.matilda.bootstrap.Caller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;


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
        Assertions.assertEquals("System.exit not allowed", uOE.getMessage());

        // now with reflection
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
        Assertions.assertEquals("System.exit not allowed", uOE.getMessage());
    }

    @Test
    public void testSystemExecTransformer() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            Runtime.getRuntime().exec("echo");
            Assertions.fail("should not have been able to run a process");
        });
        Assertions.assertEquals("ProceesBuilder.start(...) not allowed", uOE.getMessage());
        Class<?> aClass = Class.forName("java.lang.Runtime");
        Method exec = aClass.getMethod("exec", String.class);

        Process echo = (Process) Caller.call(Runtime.getRuntime(), exec, "echo foo");
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
        Assertions.assertEquals("Socket.connect not allowed", exception.getMessage());
        // TODO add a test that uses org.matilda.bootstrap.Caller to establish a network connection with a small server
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
        Assertions.assertEquals("Socket.connect not allowed", exception_url.getMessage());
    }


}