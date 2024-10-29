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

package org.matilda.bootstrap;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;



// TODO add javadocs
// TODO for testing need to be pluggable in some way maybe with subclass and non static methods MatildaTestAccessControl

/**
 * The Matilda AccessController allows granting permissions per module via System.properties()
 *
 */
// Made class final so it can't be manipulated
public final class MatildaAccessControl {
    // TODO: replace the Allowed modules with a simple check for "java.base"
    private static final Set<Module> ALLOWED_MODULES = Set.of(System.class.getModule());
    private static final MatildaAccessControl INSTANCE = new MatildaAccessControl(System.getProperties());
    private final Set<String> systemExitAllowPermissions;
    private final Set<String> systemExecAllowPermissions;
    private final Set<String> networkConnectAllowPermissions;

    public static MatildaAccessControl getInstance() {
        return INSTANCE;
    }

    /**
     * Costumized properties can be passed via the command line
     * @param properties - Properties should be passed via System.properties
     */
    public MatildaAccessControl(Properties properties) {
        String systemExistAllow = properties.getProperty("matilda.system.exit.allow", "");
        String systemExecAllow = properties.getProperty("matilda.system.exec.allow", "");
        String networkConnectAllow = properties.getProperty("matilda.network.connect.allow", "");
        this.systemExitAllowPermissions = Set.of(systemExistAllow.split(","));
        this.systemExecAllowPermissions = Set.of(systemExecAllow.split(","));
        this.networkConnectAllowPermissions = Set.of(networkConnectAllow.split(","));
    }
    public static void checkPermission(String method){
        INSTANCE.checkPermissionInternal(method);
    }

    /**
     * Method checks if called method has the permissions to be executed
     * @param method - method that is currently called
     * @throws RuntimeException - if method/ callers don't have the permissions to be executed
     *
     */
    void checkPermissionInternal(String method) {
        switch (method) {
            case "System.exit":
                if (!checkSystemExit()) throw new RuntimeException("System.exit not allowed");
                else return;
            case "ProcessBuilder.start":
                if(!checkSystemExec())throw new RuntimeException("ProceesBuilder.start(...) not allowed");
                else return;
            case "Socket.connect":
                if(!checkSocketPermission())throw new RuntimeException("Socket not allowed");
                else return;
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    /**
     *
     * Checks if caller has permission to call System.exit()
     * @return boolean - returns true if caller has the right permissions
     *
     */
    boolean checkSystemExit() {
        var callingClass = callingClassModule();
        Logger logger = Logger.getLogger(MatildaAccessControl.class.getName());
        logger.log(Level.WARNING,"Class that initially called the method" + callingClass.toString() );
        return this.systemExitAllowPermissions.contains(callingClass.toString());
    }

    /**
     *
     * Checks if caller has permission to call System.exec()
     * @return boolean - returns true if caller has the right permissions
     *
     */
    boolean checkSystemExec() {
        var callingClass = callingClassModule();
        Logger logger = Logger.getLogger(MatildaAccessControl.class.getName());
        logger.log(Level.WARNING,"Class that initially called the method" + callingClass.toString() );
        return this.systemExecAllowPermissions.contains(callingClass.toString());
    }


    /**
     *
     * Checks if caller has permission to call Socket.connect()
     * @return boolean - returns true if caller has the right permissions
     *
     */
    boolean checkSocketPermission(){
        var callingClass = callingClassModule();
        Logger logger = Logger.getLogger(MatildaAccessControl.class.getName());
        logger.log(Level.WARNING,"Class that initially called the method" + callingClass.toString() );
        return this.networkConnectAllowPermissions.contains(callingClass.toString());
    }

    /**
     *
     *
     * @return Module - Returns module that initially called method
     *
     */
    //TODO: do we need this?
    private Module callingClassModule() {
        // checkSystemexit
        // checkPermission
        // Instantiation
        // getTransformer
        // method that we are looking for
        // calling class
        int framesToSkip = 1  // getCallingClass (this method)
                + 1  // the checkXxx method
                + 1  // Instantiation
                + 1  // the runtime config method
                + 1  // the instrumented method
                ;
        return callingClass(framesToSkip);
    }


    /**
     *
     * Iterates over the current Stack and skips specified number of elements
     * @param framesToSkip - number of frames, element on stack that should be skipped
     * @return Module - calling Modul
     */
    //TODO make private, is just public for testing purposes
    public Module callingClass(int framesToSkip) {
        if(framesToSkip < 0) throw new IllegalArgumentException("framesToSkip must be >=0");
        Optional<Module> module = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(
                        s -> s.skip(framesToSkip)
                                .map(f -> {
                                    Class<?> declaringClass = f.getDeclaringClass();
                                    return declaringClass.getModule();
                                })
                                .filter(m -> !ALLOWED_MODULES.contains(m))
                                .findFirst()
                );
        return module.orElse(null);
    }

}
