package org.khaleesicodes;
import module java.base;
import module java.instrument;

public class AgentMatilda {
    /*
     * Before the application starts, register a transformer of class files.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        var transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader      loader,
                                    String           className,
                                    Class<?>         classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[]           classBytes) {
                //TODO: find specific classloader for network, read/write
                // needs to be done separately because JVM can not exit
                 if (loader != null && loader != ClassLoader.getPlatformClassLoader()) {
                    return processClasses(classBytes, new MatildaTools.SystemExitTransformer());
               } else {
                     return processClasses(classBytes, new MatildaTools.NetworkSocketTransformer());
                }
            }
        };
        inst.addTransformer(transformer, true);
    }

    @SuppressWarnings("preview")
    private static ClassTransform getClassTransform(AtomicBoolean modified, MatildaTools.MatildaCodeTransformer transformer) {
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
    static byte[] processClasses(byte[] classBytes, MatildaTools.MatildaCodeTransformer transformer) {
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