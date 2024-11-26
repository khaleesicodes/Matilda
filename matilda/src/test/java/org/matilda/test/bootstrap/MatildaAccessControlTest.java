/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matilda.test.bootstrap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matilda.bootstrap.MatildaAccessControl;


import java.util.Properties;

/**
 * Class to Test functionalities by mocking a MatildaAccessController
 * Test set the properties and test the configuration accordingly
 */
class MatildaAccessControlTest {

    /**
     * Test if setting permissions works for SystemExit
     */
    @Test
    void testSystemExitAllowed() {
        Properties props = new Properties();
        props.setProperty("matilda.runtime.exit.allow", "module org.junit.platform.commons");
        MatildaAccessControl accessControl = new MatildaAccessControl(props);
        accessControl.checkPermissionInternal("Runtime.exit");
    }

    /**
     * Tests if blocking works when no permissions are granted (default)
     */
    @Test
    void testSystemExitDenied() {
        Properties props = new Properties();
        MatildaAccessControl accessControl = new MatildaAccessControl(props);
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            accessControl.checkPermissionInternal("ProcessBuilder.start");
            Assertions.fail("should not have been able to exit the process");
        });
        Assertions.assertEquals("ProceesBuilder.start(...) not allowed for Module: org.junit.jupiter.api", uOE.getMessage());

    }

    /**
     * Test if setting permissions works for SystemExec
     */
    @Test
    void testSystemExecAllowed() {
        Properties props = new Properties();
        props.setProperty("matilda.system.exec.allow", "module org.junit.platform.commons");
        MatildaAccessControl accessControl = new MatildaAccessControl(props);
        accessControl.checkPermissionInternal("ProcessBuilder.start");
    }

    /**
     * Tests if blocking works when no permissions are granted (default)
     */
    @Test
    void testSystemExecDenied() {
        Properties props = new Properties();
        MatildaAccessControl accessControl = new MatildaAccessControl(props);
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            accessControl.checkPermissionInternal("ProcessBuilder.start");
            Assertions.fail("should not have been able to run a process");

        });
        Assertions.assertEquals("ProceesBuilder.start(...) not allowed for Module: org.junit.jupiter.api", uOE.getMessage());
    }

    /**
     * Test if setting permissions works for Socket.connect()
     */
    @Test
    void testOpenSocketAllowed() {
        Properties props = new Properties();
        props.setProperty("matilda.network.connect.allow", "module org.junit.platform.commons");
        MatildaAccessControl accessControl = new MatildaAccessControl(props);
        accessControl.checkPermissionInternal("Socket.connect");
    }

    /**
     * Tests if blocking works when no permissions are granted (default)
     */
    @Test
    void testOpenSocketDenied() {
        Properties props = new Properties();
        MatildaAccessControl accessControl = new MatildaAccessControl(props);
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            accessControl.checkPermissionInternal("Socket.connect");
            Assertions.fail("should not have been able to open a connection");
        });
        Assertions.assertEquals("Socket.connect not allowed for Module: org.junit.jupiter.api", exception.getMessage());

    }


    /**
     * Tests input validation for keys
     */
    @Test
    void testMaleFormedPropertyKey() {
        Properties props = new Properties();
        props.setProperty("matilda.system.foo.allow", "module org.junit.platform.commons");
        // Needed to extract Exceptio
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            MatildaAccessControl accessControl = new MatildaAccessControl(props);
        });
        Assertions.assertEquals("matilda.system.foo.allow is not a valid key. Allowed keys are: matilda.runtime.exit.allow, matilda.system.exec.allow,matilda.network.connect.allow", uOE.getMessage());
    }

    /**
     * Tests input validation for property values
     */
    @Test
    void testMaleFormedPropertyValue() {
        Properties props = new Properties();
        props.setProperty("matilda.system.exec.allow", "module org junit.platform.commons");
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            MatildaAccessControl accessControl = new MatildaAccessControl(props);
        });
        Assertions.assertEquals("Not a valid module name: module org junit.platform.commons", uOE.getMessage());
    }

    /**
     * Tests input validation for property values
     */
    @Test
    void testMaleFormedPropertyValueSpelling() {
        Properties props = new Properties();
        props.setProperty("matilda.system.exec.allow", "m0dule org junit.platform.commons");
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            MatildaAccessControl accessControl = new MatildaAccessControl(props);
        });
        Assertions.assertEquals("Not a valid module name: m0dule org junit.platform.commons", uOE.getMessage());
    }




}