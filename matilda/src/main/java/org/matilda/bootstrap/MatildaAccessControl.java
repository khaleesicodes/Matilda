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

    // TODO add the possibiilty to configure other modules for certain methods
    boolean checkSystemExit() {
        var callingClass = callingClassModule();
        Logger logger = Logger.getLogger(MatildaAccessControl.class.getName());
        logger.log(Level.WARNING,"Class that initially called the method" + callingClass.toString() );
        return callingClass.toString().equals("module gradle.worker");
        //this.systemExitAllowPermissions.contains(callingClass.toString());

    }

    boolean checkSystemExec() {
        var callingClass = callingClassModule();
        return this.systemExecAllowPermissions.contains(callingClass.toString());
    }

    boolean checkSocketPermission(){
        var callingClass = callingClassModule();
        return this.networkConnectAllowPermissions.contains(callingClass.toString());
    }

    private Module callingClassModule() {
        // checkSystemexit
        // checkPermission
        // Instantiation
        // getTransformer
        // method that we are looking for
        // calling class
        int framesToSkip = 1  // getCallingClass (this method)
                + 1  // Instantiation
                + 1  // the checkXxx method
                + 1  // the runtime config method
                + 1  // the instrumented method
                ;
        return callingClass(framesToSkip);
    }


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
