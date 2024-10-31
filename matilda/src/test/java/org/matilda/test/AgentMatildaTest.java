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

import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

// TODO javadoc
// TODO bigger task -- add test methods for the Transformers --  need stubs and some mocks but doable
// TODO need the ability to test if actually allows some code to pass checks

public class AgentMatildaTest {
    @Test
    public void testSystemExitTransformer()  {
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            System.exit(-1);
            Assertions.fail("should not have been able to exit the process");
        });
        Assertions.assertEquals("System.exit not allowed", uOE.getMessage());

    }

    @Test
    public void testSystemExecTransformer()  {
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            //TODO Fix to non deprecated version
            Runtime.getRuntime().exec("echo");
            Assertions.fail("should not have been able to run a process");

        });
        Assertions.assertEquals("ProceesBuilder.start(...) not allowed", uOE.getMessage());
    }

    @Test
    public void openSocketTest() {
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            Socket socket = new Socket("localhost", 9999);
            Assertions.fail("should not have been able to open a connection");
        });
        Assertions.assertEquals("Socket not allowed", exception.getMessage());

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
        Assertions.assertEquals("Socket not allowed", exception_url.getMessage());

    }
}