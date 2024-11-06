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
 *  TODO: please document how your property format works and how you con configure this
 */
// Made class final so it can't be manipulated
public final class MatildaAccessControl {
    // TODO: replace the Allowed modules with a simple check for "java.base"
    private static final Set<Module> ALLOWED_MODULES = Set.of(System.class.getModule());
    private static final MatildaAccessControl INSTANCE = new MatildaAccessControl(System.getProperties());
    private final Set<String> systemExitAllowPermissions;
    private final Set<String> systemExecAllowPermissions;
    private final Set<String> networkConnectAllowPermissions;
    static Logger logger;

    // TODO document that this returns a singleton instance of the access control. ie. simple singleton pattern
    public static MatildaAccessControl getInstance() {
        return INSTANCE;
    }

    /**
     * Customized properties can be passed via the command line
     * // TODO this is not true the constructor uses only properties -- you need to document the format
     * @param properties - Properties should be passed via System.properties
     */
    public MatildaAccessControl(Properties properties) {
        // TODO iterate over all the keys and see if there is any of them that we don't know that starts with matilda
        // if so throw an exception -- also write a test for it
        String systemExistAllow = properties.getProperty("matilda.system.exit.allow", "");
        String systemExecAllow = properties.getProperty("matilda.system.exec.allow", "");
        String networkConnectAllow = properties.getProperty("matilda.network.connect.allow", "");
        this.systemExitAllowPermissions = Set.of(systemExistAllow.split(","));
        this.systemExecAllowPermissions = Set.of(systemExecAllow.split(","));
        this.networkConnectAllowPermissions = Set.of(networkConnectAllow.split(","));
    }

    // TODO document that this one is actually called by the methods instrumented in the agend
    public static void checkPermission(String method) {
        // this is an indirection to simplify the code generated in the agent
        INSTANCE.checkPermissionInternal(method);
    }

    /**
     * Method checks if called method has the permissions to be executed
     * @param method - method that is currently called
     * @throws RuntimeException - if method/ callers don't have the permissions to be executed
     *
     */
    // should be private
    public void checkPermissionInternal(String method) {
        switch (method) {
            case "System.exit":
                if (!checkSystemExit()) throw new RuntimeException("System.exit not allowed");
                else return;
            case "ProcessBuilder.start":
                if (!checkSystemExec()) throw new RuntimeException("ProceesBuilder.start(...) not allowed");
                else return;
            case "Socket.connect":
                // TODO fix the exceptin to actually reflect that it's socket.connect
                if (!checkSocketPermission()) throw new RuntimeException("Socket.connect not allowed");
                else return;
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    /**
     *
     * Checks if caller has permission to call System.exit()
     * @return boolean - true iff caller module has the right permissions otherwise false
     * @see #callingClassModule() for reference how the caller module is identified
     */
    private boolean checkSystemExit() {
        var callingClass = callingClassModule();
        logger = Logger.getLogger(MatildaAccessControl.class.getName());
        // TODO message should say module and should reflect that we are checking. also include the return value of the permission checking
        logger.log(Level.FINE, "Class that initially called the method " + callingClass.toString());
        return this.systemExitAllowPermissions.contains(callingClass.toString());
    }

    /**
     *
     * Checks if caller has permission to call System.exec()
     * @return boolean - returns true if caller has the right permissions
     *  // TODO fix javadoc see checksystemexit
     */
    private boolean checkSystemExec() {
        var callingClass = callingClassModule();
        logger = Logger.getLogger(MatildaAccessControl.class.getName());
        // TODO message should say module and should reflect that we are checking. also include the return value of the permission checking
        logger.log(Level.FINE, "Class that initially called the method " + callingClass.toString());
        return this.systemExecAllowPermissions.contains(callingClass.toString());
    }


    /**
     *
     * Checks if caller has permission to call Socket.connect()
     * @return boolean - returns true if caller has the right permissions
     *  // TODO fix javadoc see checksystemexit
     */
    private boolean checkSocketPermission() {
        var callingClass = callingClassModule();
        // TODO assign the logger as a static var to this class..
        Logger logger = Logger.getLogger(MatildaAccessControl.class.getName());

        // TODO level warning is too high use FINE
        // TODO message should say module and should reflect that we are checking. also include the return value of the permission checking
        logger.log(Level.WARNING, "Class that initially called the method {0} ", callingClass);
        return this.networkConnectAllowPermissions.contains(callingClass.toString());
    }

    /**
     *
     * // TODO document here that we are exepcting this to be called in a certain order ie. skipping frames
     * @return Module - Returns module that initially called method
     *
     */
    private Module callingClassModule() {
        final int framesToSkip = 1  // getCallingClass (this method)
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
     * @return Module - calling Module
     */
    //TODO make private, is just public for testing purposes
    public Module callingClass(int framesToSkip) {
        if (framesToSkip < 0) throw new IllegalArgumentException("framesToSkip must be >=0");
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