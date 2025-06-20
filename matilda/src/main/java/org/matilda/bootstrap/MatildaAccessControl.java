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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The Matilda AccessController allows granting permissions per module via System.properties()
 * permissions can be passed using the following format
 * "matilda.<function>.allow=<Module that should be allowed>"
 * Example: -Dmatilda.runtime.exit.allow=module gradle.worker
 *
 * @author Elina Eickstaedt
 */
// record for security reasons. Cannot be manipulated with Field.set
public record MatildaAccessControl(
        Set<String> systemExitAllowPermissions,
        Set<String> systemExecAllowPermissions,
        Set<String> networkConnectAllowPermissions,
        Set<String> serverSocketBindAllowPermissions,
        Set<String> unsafeMemoryAccessPermissions) {
    // TODO implement option that spare the need of using the module prefix
    // TODO: replace the Allowed modules with a simple check for "java.base"
    // TODO: Fix, potential circular dependency
    // List of System.class Modules, only Modules that are of interest
    private static final Set<Module> ALLOWED_MODULES = Set.of(
            System.class.getModule(),
            ModuleLayer.boot().findModule("jdk.unsupported").orElseThrow());
    private static final MatildaAccessControl INSTANCE = new MatildaAccessControl(System.getProperties());
    private static final Logger logger = Logger.getLogger(MatildaAccessControl.class.getName());

    /**
     * Creates and returns a single instances of MatildaAccessControl using the singleton pattern,
     * This instance is created with {@link System#getProperties()} configured via the commandline
     * in order to ensure that there is no manipulation
     * @return MatildaAccessControl - Instances that is used to do access control
     */
    public static MatildaAccessControl getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new MatildaAccessControl instance configured via properties.
     *
     * @param properties - Properties should be passed via System.properties => matilda.runtime.exit.allow=Module that should be allowed
     */
    public MatildaAccessControl(Properties properties) {
        // validates syntax correctness of property configuration
        for(Object elem:properties.keySet()){
            if(elem instanceof String){
                String key = elem.toString();
                if (key.startsWith("matilda.")){
                    switch (key){
                        case "matilda.runtime.exit.allow":
                        case "matilda.system.exec.allow":
                        case "matilda.network.connect.allow":
                        case "matilda.server.bind.allow":
                        case "matilda.unsafe.memory-access.allow":
                        case "matilda.bootstrap.jar":
                            break;
                        default: throw new IllegalArgumentException(elem + " is not a valid key. Allowed keys are: matilda.runtime.exit.allow, matilda.system.exec.allow,matilda.network.connect.allow");
                    }
                }
            }

        }
        // Initialization of method specific properties
        String systemExistAllow = properties.getProperty("matilda.runtime.exit.allow", "");
        String systemExecAllow = properties.getProperty("matilda.system.exec.allow", "");
        String networkConnectAllow = properties.getProperty("matilda.network.connect.allow", "");
        String serverSocketBindAllow = properties.getProperty("matilda.server.bind.allow", "");
        String unsafeMemoryAccessAllow = properties.getProperty("matilda.unsafe.memory-access.allow", "");


        // Loading and validation of set configuration
        this(validateModuleConfig(
                systemExistAllow.isEmpty()? Set.of() : Set.of(systemExistAllow.split(","))),
            validateModuleConfig(
                systemExecAllow.isEmpty()? Set.of() : Set.of(systemExecAllow.split(","))),
            validateModuleConfig(
                networkConnectAllow.isEmpty() ? Set.of() : Set.of(networkConnectAllow.split(","))),
            validateModuleConfig(
                serverSocketBindAllow.isEmpty() ? Set.of() : Set.of(serverSocketBindAllow.split(","))),
            validateModuleConfig(unsafeMemoryAccessAllow.isEmpty() ? Set.of(): Set.of(unsafeMemoryAccessAllow.split(",")))
        );
    }

    /*
     * override all assessors to not expose internal state
     */
    public Set<String> systemExitAllowPermissions() {
        throw new UnsupportedOperationException();
    }
    public Set<String> systemExecAllowPermissions() {
        throw new UnsupportedOperationException();
    }
    public Set<String> networkConnectAllowPermissions() {
        throw new UnsupportedOperationException();
    }
    public Set<String> serverSocketBindAllowPermissions() {
        throw new UnsupportedOperationException();
    }
    public Set<String> unsafeMemoryAccessPermissions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks for a valid configuration
     * @param modules - set of Strings the potentially represent moduls
     * @return - Set of Strings that are valid configurations
     */
    public static Set<String> validateModuleConfig(Set<String> modules) {
        Pattern pattern = Pattern.compile("module \\S+");
        for (String moduleName : modules) {
            Matcher matcher = pattern.matcher(moduleName);
            if (!matcher.matches()){
                throw new IllegalArgumentException("Not a valid module name: " + moduleName);
            }
        }
        return modules;
    }



    /**
     * Is called by method that is instrumented by the agent, this is necessary to get the correct call stack
     * @param method - method that should be checked for permissions
     */
    public static void checkPermission(String method) { // seems unused since we inject this into the generated code
        // this is an indirection to simplify the code generated in the agent
        INSTANCE.checkPermissionInternal(method);
    }

    /**
     * Method checks if called method has the permissions to be executed
     * @param method - method that is currently called
     * @throws RuntimeException - if method/ callers don't have the permissions to be executed
     */
    // should be private
    public void checkPermissionInternal(String method) {
        var callingModule = callingClassModule();
        switch (method) {
            case "Runtime.exit":
                if (!checkSystemExit(callingModule)) {
                    throw new RuntimeException("Runtime.exit not allowed for Module: " +  getModuleName(callingModule));
                }
                else return;
            case "ProcessBuilder.start":
                if (!checkSystemExec(callingModule)) {
                    throw new RuntimeException("ProceesBuilder.start(...) not allowed for Module: " +  getModuleName(callingModule));
                }
                else return;
            case "Socket.connect":
                if (!checkSocketPermission(callingModule)) {
                    throw new RuntimeException("Socket.connect not allowed for Module: " +  getModuleName(callingModule));
                }
                else return;
            case "ServerSocket.bind":
                if (!checkSocketBindPermission(callingModule)) {
                    throw new RuntimeException("ServerSocket.bind not allowed for Module: " +  getModuleName(callingModule));
                }
                else return;
            case "Unsafe.beforeMemoryAccess":
                if (!checkUnsafeMemoryAccessPermission(callingModule))
                    throw new RuntimeException("Unsafe memory access not allowed for Module: " +  getModuleName(callingModule));
                else return;
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    /**
     * Returns differntiated error message when an unnamed module appears
     * @param module
     * @return - Module name if module exist otherwise returns information of unnamed module
     */
    private String getModuleName(Module module) {
        return module.isNamed() ? module.getName() : "unnamed module";
    }

    /**
     * Checks if caller has permission to call System.exit()
     * @return boolean - true iff caller module has the right permissions otherwise false
     * @see #callingClassModule() for reference how the caller module is identified
     */
    private boolean checkSystemExit(Module callingModule) {
        logger.log(Level.FINE, "Module that initially called the method {0} ", callingModule);
        return this.systemExitAllowPermissions.contains(callingModule.toString());
    }

    /**
     * Checks if caller has permission to call System.exec()
     *@return boolean - true iff caller module has the right permissions otherwise false
     *@see #callingClassModule() for reference how the caller module is identified
     */
    private boolean checkSystemExec(Module callingModule) {
        logger.log(Level.FINE, "Module that initially called the method {0} ", callingModule);
        return this.systemExecAllowPermissions.contains(callingModule.toString());
    }


    /**
     * Checks if caller has permission to call Socket.connect()
     *@return boolean - true iff caller module has the right permissions otherwise false
     *@see #callingClassModule() for reference how the caller module is identified
     */
    private boolean checkSocketPermission(Module callingModule) {
        logger.log(Level.FINE, "Module that initially called the method {0} ", callingModule);
        return this.networkConnectAllowPermissions.contains(callingModule.toString());
    }


    /**
     * Checks if caller has permission to call Socket.bind()
     *@return boolean - true iff caller module has the right permissions otherwise false
     *@see #callingClassModule() for reference how the caller module is identified
     */
    private boolean checkSocketBindPermission(Module callingModule) {
        logger.log(Level.FINE, "Module that initially called the method {0} ", callingModule);
        return this.serverSocketBindAllowPermissions.contains(callingModule.toString());
    }


    /**
     * Checks if caller has permission to access memory via Unsafe
     *@return boolean - true iff caller module has the right permissions otherwise false
     *@see #callingClassModule() for reference how the caller module is identified
     */
    private boolean checkUnsafeMemoryAccessPermission(Module callingModule) {
        logger.log(Level.FINE, "Module that initially called the method {0} ", callingModule);
        return this.unsafeMemoryAccessPermissions.contains(callingModule.toString());
    }

    /**
     * In order to identify the caller skip frames of the helper methods as well as the called method needs to be skipped
     * needs to be adapted if structure of the AccessController changes
     * @return Module - Returns module that initially called method
     */
    private Module callingClassModule() {
        final int framesToSkip =
                1  // MatildaAccessControl.callingClass
                + 1  // MatildaAccessControl.checkPermissionInternal
                + 1  // MatildaAccessControl.checkPermission
                + 1  // the instrumented method ie. Runtime.exit / exec etc.
                ;
        return callingClass(framesToSkip);
    }


    /**
     * Iterates over the current Stack and skips specified number of elements
     * @param framesToSkip - number of frames, element on stack that should be skipped
     * @return Module - calling Module
     */
    // Is not private as MatildaAccessControl can only be instantiated once
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
