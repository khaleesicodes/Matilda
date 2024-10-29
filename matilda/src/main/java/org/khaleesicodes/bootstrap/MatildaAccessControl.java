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

package org.khaleesicodes.bootstrap;

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
    // TODO add the possibiilty to configure other modules for certain methods
    //  matilda.modulename=socket.connect
    //  matilda.gradle.worker=system.exit
    //  System.getProperties() for example
    private static final MatildaAccessControl INSTANCE = new MatildaAccessControl(System.getProperties());

    public static MatildaAccessControl getInstance() {
        return INSTANCE;
    }

    public MatildaAccessControl(Properties properties) {
        String systemExistAllow = properties.getProperty("matilda.system.exit.allow", "");
        properties.get("matilda.system.exec.allow");
        properties.get("matilda.network.connect.allow");

        Set<String> systemExitAllowModules = Set.of(systemExistAllow.split(","));
    }
    public static void checkPermission(String method){
        INSTANCE.checkPermissionInternal(method);
    }
    void checkPermissionInternal(String method) {
        switch (method) {
            case "System.exit":
                if (checkSystemExit()){
                    return;
                }
                else throw new RuntimeException("System.exit not allowed");
            case "ProcessBuilder.start":
                throw new RuntimeException("ProceesBuilder.start(...) not allowed");
            case "Socket.connect":
                throw new RuntimeException("Socket not allowed");
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    boolean checkSystemExit() {
        // 2 Frames need to be skipped in order to check if the caller is junit which is allowed to exit maybe
        var callingClass = callingClassModule();
        Logger logger = Logger.getLogger(MatildaAccessControl.class.getName());
        logger.log(Level.WARNING,"Class that initially called the method" + callingClass.toString() );
        return callingClass.toString().equals("module gradle.worker");
    }

    boolean checkProcessBuilderPermission() {
        var callingClass = callingClassModule();

        Properties properties = System.getProperties();
        return false;
    }

    boolean checkSocketPermission(){
        return false;
    }

    private Module callingClassModule() {
        // checkSystemexit
        // checkPermission
        // getTransformer
        // method that we are looking for
        // calling class
        int framesToSkip = 1  // getCallingClass (this method)
                + 1
                + 1  // the checkXxx method
                + 1  // the runtime config method
                + 1  // the instrumented method
                ;
        return callingClass(framesToSkip);
    }

    // Gets requesting class
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
