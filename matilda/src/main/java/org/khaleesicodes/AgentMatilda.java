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
                if (loader != null && loader != ClassLoader.getPlatformClassLoader()) {
                    return blockSystemExit(classBytes);
                } else {
                    return null;
                }
            }
        };
        inst.addTransformer(transformer, true);
    }

    /*
     * Rewrite every invokestatic of System::exit(int) to an athrow of RuntimeException.
     */
    @SuppressWarnings("preview")
    private static byte[] blockSystemExit(byte[] classBytes) {
        var modified = new AtomicBoolean();
        ClassFile cf = ClassFile.of(ClassFile.DebugElementsOption.DROP_DEBUG);
        ClassModel classModel = cf.parse(classBytes);
        ClassTransform ct = getClassTransform(modified);
        byte[] newClassBytes = cf.transform(classModel, ct);
        if (modified.get()) {
            return newClassBytes;
        } else {
            return null;
        }
    }



    @SuppressWarnings("preview")
    private static ClassTransform getClassTransform(AtomicBoolean modified) {
        /* simonw: I think we need to make this extensible in our own way to plug this ClassTransform
         * like for instance with our own MatildaCodeTransform that can detect and rewrite multiple method signatures
         * Maybe it looks like something like this:
         * abstract class MatildaCodeTransform {
         *    Predicate<MethodModel> getTransformPredicate();
         *    CodeTransform getTransform();
         * }
         *
         * this way we can chain multiple code elements together
         */
        Predicate<CodeElement> predicate = AgentMatilda::isInvocationOfSystemExit; // you can or this by predicate.or(otherPredicate)
        Predicate<MethodModel> invokesSystemExit =
                methodModel -> methodModel.code()
                        .map(codeModel ->
                                codeModel.elementStream()
                                        .anyMatch(predicate))
                        .orElse(false);
        CodeTransform rewriteSystemExit =
                (codeBuilder, codeElement) -> {
                    if (isInvocationOfSystemExit(codeElement)) {
                        var runtimeException = ClassDesc.of("java.lang.RuntimeException");
                        codeBuilder.new_(runtimeException)
                                .dup()
                                .ldc("System.exit not allowed")
                                .invokespecial(runtimeException,
                                        "<init>",
                                        MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"),
                                        false)
                                .athrow();
                        modified.set(true);
                    } else {
                        codeBuilder.with(codeElement);
                    }
                };

        ClassTransform ct = ClassTransform.transformingMethodBodies(invokesSystemExit, rewriteSystemExit);
        return ct;
    }


    // public final class ProcessBuilder
    // private Process start(Redirect[] redirects) throws IOException
    @SuppressWarnings("preview")
    private static boolean isInvocationOfSystemExit(CodeElement codeElement) {
        return codeElement instanceof InvokeInstruction i
                && i.opcode() == Opcode.INVOKESTATIC
                && "java/lang/System".equals(i.owner().asInternalName())
                && "exit".equals(i.name().stringValue())
                && "(I)V".equals(i.type().stringValue());
    }


}