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
import module java.base;
import module java.instrument;

import org.matilda.bootstrap.MatildaAccessControl;

import java.util.logging.Logger;

/**
 *
 * The matilda agent allows the attachment to the JVM, manipulating the bytecode of all classes
 * that are being loaded according to the configured permissions.
 * The agent heavily uses the ClassFile API for bytecode manipulation. The API is a preview Feature in JDK 23. It
 * delivers bytecode manipulation capabilities similar to ASM, but shipped in the JDK. Using the API makes Matilda
 * independent of any external dependencies.
 *
 * @author Elina Eickstaedt
 * @see org.matilda.bootstrap.MatildaAccessControl
 *
 */
public final class AgentMatilda {

    /**
     * Agent needs to be hooked when JVM is started using the  following commandline argument
     * "--enable-preview", - needs to be enabled to use ClassFile APU
     * "-javaagent:/path/to/matilda-agent.jar", - Agent jar
     * "-Dmatilda.bootstrap.jar=/path/to/matilda-bootstrap.jar - jar of AccessController
     * Transforms ClassFile before it is loaded
     * @param agentArgs - Specifies Agent that should be loaded, can be passed via -javaagent
     * @param inst - initializes an Interface into low level JVM functionalities that allows bytecode manipulation
     * @throws IOException - in the case of an exception during class loading
     * @throws UnmodifiableClassException - if one of the transformed classes can't be modified
     */
    public static void premain(String agentArgs, Instrumentation inst) throws IOException, UnmodifiableClassException {
        // Path to bootstrap jar that will be needed later
        var bootStrapJarPath = System.getProperty("matilda.bootstrap.jar");
        // check if bootstrap jar is present
        //TODO Addd check to ensure that nothing else was added to the bootstrap path
        if (bootStrapJarPath == null) {
            throw new IllegalStateException("No matilda.bootstrap.jar file specified");
        }

        JarFile bootstrapJar = new JarFile(bootStrapJarPath);


        /*
          The ClassFileTransformer provides a byte Array of the loaded class, it will be triggered for any class loaded
          return null if class should not be modified -> managed in AccessController
         */
        var transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader      loader,
                                    String           className,
                                    Class<?>         classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[]           classBytes) {
                switch (className) {
                    case "java/lang/ProcessBuilder":
                        return processClasses(classBytes, new SystemExecTransformer());
                    case "java/net/Socket":
                        return processClasses(classBytes, new NetworkSocketTransformer());
                    case "java/net/ServerSocket":
                        return processClasses(classBytes, new ServerSocketTransformer());
                    case "sun/misc/Unsafe":
                        return processClasses(classBytes, new UnsafeMemoryAccessTransformer());
                    default:
                        return null;
                }
            }

        };
        inst.addTransformer(transformer, false);

        var retransform = new ClassFileTransformer() {
            private final AtomicBoolean wasExecuted = new AtomicBoolean(false);
            @Override
            public byte[] transform(ClassLoader      loader,
                                    String           className,
                                    Class<?>         classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[]           classBytes) {
                if (className.equals("java/lang/Runtime")) {
                    if (!wasExecuted.getAndSet(true)) {
                        return processClasses(classBytes, new SystemExitTransformer());
                    }
                }
                return null;
            }

        };
        inst.addTransformer(retransform, true);
        /*
        Needs to be set to allow retransformation of Runtime class as the Agent is started
        after System classes were loaded
         */
        inst.retransformClasses(Runtime.class);

        /*
         * As a reference to the MatildaAccessController is injected into each of the transformed classes, it needs to
         * be accessible to the classloader of the classes or its parent. Since System classes are loaded by the
         * platform classloader they need to be discoverable for the bootstrap classloader
         */
        inst.appendToBootstrapClassLoaderSearch(bootstrapJar);
        try {
            // Force initialization to catch current system properties and avoid race conditions
            Class.forName(MatildaAccessControl.class.getName(), true, null);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e); //cannot happen
        }
    }

    /**
     * Performs the actual transformation of a method / class with the provided {@link }MatildaCodeTransformer}
     * @param transformer - Transformer that should be used to perform the transformation
     */
    @SuppressWarnings("preview")
    private static ClassTransform transformClass(MatildaCodeTransformer transformer) {
        CodeTransform codeTransform = transformer.getTransform();
        Predicate<MethodModel> invokesTransformer = transformer.getModelPredicate();
        return ClassTransform.transformingMethodBodies(invokesTransformer, codeTransform);
    }

    /**
     * Transforms the Bytearray into a ClassFile and manipulates it with a given Transformer
     * @param classBytes - byte representation of the class that should be tranformed
     * @param transformer - Transformer that should be used
     * @return byte[] - Transformed Class or null if it's not been transformed.
     */
    @SuppressWarnings("preview")
    static byte[] processClasses(byte[] classBytes, MatildaCodeTransformer transformer) {
        ClassFile cf = ClassFile.of(ClassFile.DebugElementsOption.DROP_DEBUG);
        ClassModel classModel = cf.parse(classBytes);
        byte[] transformedClass =  cf.transform(classModel, transformClass(transformer));
        final Logger logger = Logger.getLogger(AgentMatilda.class.getName());
        return transformedClass;
    }
}