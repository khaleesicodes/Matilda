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


// TODO add javadocs {@link org.matilda.bootstrap.MatildaAccessControl}
// TODO explain how to hook this into the jvm and also explain that it adds our access controller on the boot classpath
// otherwise the classes like System won't find the accessControl since they are all loaded by the bootclassloader
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
     * // TODO explain how this method is hooked in the jvm --agentblafoo
     * //TODO Check if we can use just one ClassFileTransformer and decide which Customized transformer is needed by checking the class type
     * // Transform ClassFile before it is loaded
     * @param agentArgs - Specifies Agent that should be loaded, can be passed via -javaagent
     * @param inst - initializes an Interface into low level JVM functionalities that allows bytecode manipulaion
     * @throws IOException - in the case of an exception during class loading
     * @throws UnmodifiableClassException - if one of the transformed classes can't be modified
     */
    public static void premain(String agentArgs, Instrumentation inst) throws IOException, UnmodifiableClassException {
        // Path to bootstrap jar that will be needed later
        var bootStrapJarPath = System.getProperty("matilda.bootstrap.jar");
        if (bootStrapJarPath == null) {
            throw new IllegalStateException("No matilda.bootstrap.jar file specified");
        }

        JarFile bootstrapJar = new JarFile(bootStrapJarPath);

        /*
          The ClassFileTransformer provides a byte Array of the loaded class, it will be triggered for any class loaded
          return null if class should not be modified -> managed in AccessController
         */

        var sysExitTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader      loader,
                                    String           className,
                                    Class<?>         classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[]           classBytes) {
                    // returns manipulated bytearray
                    return processClasses(classBytes, new SystemExitTransformer());
            }
        };
        inst.addTransformer(sysExitTransformer, true);


        var sysExecTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader      loader,
                                    String           className,
                                    Class<?>         classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[]           classBytes) {
                    return processClasses(classBytes, new SystemExecTransformer());
              }
        };
        inst.addTransformer(sysExecTransformer, true);
        var socketTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader      loader,
                                    String           className,
                                    Class<?>         classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[]           classBytes) {
                    return processClasses(classBytes, new NetworkSocketTransformer());
            }
        };

        inst.addTransformer(socketTransformer, true);
        /*
        Needs to be set to allow retransformation of classes that have been already loaded as the Agent is started
        after System classes were loaded
         */
        inst.retransformClasses(Socket.class, System.class, ProcessBuilder.class);
        /*
         * As a reference to the MatildaAccessController is injected into each of the tranformed classes, it needs to
         * be accessible to the classloader of the classes or its parent. Since System classes are loaded by the
         * platform classloader they need to be discoverable for the bootstrap classloader
         */
        inst.appendToBootstrapClassLoaderSearch(bootstrapJar);
    }

    //TODO Explain why two steps are needed
    /**
     *
     * @param modified - true if class has been modified else false
     * @param transformer - Transformer that should be used
     */
    @SuppressWarnings("preview")
    private static ClassTransform transformClass(AtomicBoolean modified, MatildaCodeTransformer transformer) {
        Predicate<CodeElement> predicate = transformer.getTransformPredicate();
        CodeTransform codeTransform = transformer.getTransform(modified);
        Predicate<MethodModel> invokesTransformer =
                methodModel -> methodModel.code()
                        .map(codeModel ->
                                codeModel.elementStream()
                                        .anyMatch(predicate)).orElse(false);
        return ClassTransform.transformingMethodBodies(invokesTransformer, codeTransform);
    }

    /**
     * Transforms the Bytearray into a ClassFile and manipulates it with a given Transformer
     * @param classBytes - Classbytes of class that should be tranformed
     * @param transformer - Transformer that should be used
     * @return byte[] - Transformed Class
     */
    @SuppressWarnings("preview")
    static byte[] processClasses(byte[] classBytes, MatildaCodeTransformer transformer) {
        var modified = new AtomicBoolean();
        ClassFile cf = ClassFile.of(ClassFile.DebugElementsOption.DROP_DEBUG);
        ClassModel classModel = cf.parse(classBytes);
        byte[] newClassBytes = cf.transform(classModel, transformClass(modified, transformer));
        if (modified.get()) {
            return newClassBytes;
        } else {
            return null;
        }
    }
}