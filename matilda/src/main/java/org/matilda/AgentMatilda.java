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
     * @throws IOException - in the case of an exception during class loading
     * @throws UnmodifiableClassException - if one of the transformed classes can't be modified
     */
    public static void premain(String agentArgs, Instrumentation inst) throws IOException, UnmodifiableClassException {
        var bootStrapJarPath = System.getProperty("matilda.bootstrap.jar");
        if (bootStrapJarPath == null) {
            throw new IllegalStateException("No matilda.bootstrap.jar file specified");
        }

        JarFile bootstrapJar = new JarFile(bootStrapJarPath);
        var sysExitTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader      loader,
                                    String           className,
                                    Class<?>         classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[]           classBytes) {
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
        // TODO explain why we need to put stuff on the bootclasspath
        inst.addTransformer(socketTransformer, true);
        inst.retransformClasses(Socket.class, System.class, ProcessBuilder.class);
        inst.appendToBootstrapClassLoaderSearch(bootstrapJar);
    }

    @SuppressWarnings("preview")
    private static ClassTransform getClassTransform(AtomicBoolean modified, MatildaCodeTransformer transformer) {
        Predicate<CodeElement> predicate = transformer.getTransformPredicate();
        CodeTransform rewriteSystemExit = transformer.getTransform(modified);
        Predicate<MethodModel> invokesSystemExit =
                methodModel -> methodModel.code()
                        .map(codeModel ->
                                codeModel.elementStream()
                                        .anyMatch(predicate)).orElse(false);
        return ClassTransform.transformingMethodBodies(invokesSystemExit, rewriteSystemExit);
    }

    @SuppressWarnings("preview")
    static byte[] processClasses(byte[] classBytes, MatildaCodeTransformer transformer) {
        var modified = new AtomicBoolean();
        ClassFile cf = ClassFile.of(ClassFile.DebugElementsOption.DROP_DEBUG);
        ClassModel classModel = cf.parse(classBytes);
        byte[] newClassBytes = cf.transform(classModel, getClassTransform(modified, transformer));
        if (modified.get()) {
            return newClassBytes;
        } else {
            return null;
        }
    }
}