package org.khaleesicodes;
import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;


@SuppressWarnings("preview")
public class MatildaTools {
    interface MatildaCodeTransformer{
        Predicate<CodeElement> getTransformPredicate();
        CodeTransform getTransform(AtomicBoolean modified);
    }
    static class SystemExitTransformer implements MatildaCodeTransformer {
        @Override
        public Predicate<CodeElement> getTransformPredicate() {
            return codeElement -> codeElement instanceof InvokeInstruction i
                    && i.opcode() == Opcode.INVOKESTATIC
                    && "java/lang/System".equals(i.owner().asInternalName())
                    && "exit".equals(i.name().stringValue())
                    && "(I)V".equals(i.type().stringValue());
        }

        @Override
        public CodeTransform getTransform(AtomicBoolean modified) {
            Predicate<CodeElement> predicate = getTransformPredicate();
            return (codeBuilder, codeElement) -> {
                if (predicate.test(codeElement)) {
                    /*
                     * Rewrite every invokestatic of System::exit(int) to an athrow of RuntimeException.
                     */
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
        }
    }

    static class SystemExecTransformer implements MatildaCodeTransformer {
        @Override
        // private Process start(ProcessBuilder.Redirect[] redirects) throws IOException {
        public Predicate<CodeElement> getTransformPredicate() {
            return codeElement ->
                    codeElement instanceof InvokeInstruction i
                            && i.opcode() == Opcode.INVOKEVIRTUAL
                            && "java/lang/ProcessBuilder".equals(i.owner().asInternalName())
                            && "start".equals(i.name().stringValue())
                            && "([Ljava/lang/ProcessBuilder$Redirect;)Ljava/lang/Process;".equals(i.type().stringValue());
        }

        @Override
        public CodeTransform getTransform(AtomicBoolean modified) {
            Predicate<CodeElement> predicate = getTransformPredicate();
            return (codeBuilder, codeElement) -> {
                if (predicate.test(codeElement)) {
                    /*
                     * Rewrite every invokestatic of System::exit(int) to an athrow of RuntimeException.
                     */
                    var runtimeException = ClassDesc.of("java.lang.RuntimeException");
                    codeBuilder.new_(runtimeException)
                            .dup()
                            .ldc("ProceesBuilder.start(...) not allowed")
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
        }

    }

    static class NetworkSocketTransformer implements MatildaCodeTransformer {
        @Override
        public Predicate<CodeElement> getTransformPredicate() {
            return null;
        }

        @Override
        public CodeTransform getTransform(AtomicBoolean modified) {
            return null;
        }

    }

    public static class CombinedTransformer implements MatildaCodeTransformer {
        private final List<MatildaCodeTransformer> transformer = List.of(new SystemExitTransformer(), new SystemExecTransformer());

        @Override
        public Predicate<CodeElement> getTransformPredicate() {
            return codeElement -> {
                for (MatildaCodeTransformer transformer : transformer) {
                    if (transformer.getTransformPredicate().test(codeElement)) {
                        return true;
                    }
                }
                return false;
            };
        }

        @Override
        public CodeTransform getTransform(AtomicBoolean modified) {
            return (codeBuilder, codeElement) ->{
                for (MatildaCodeTransformer transformer : transformer) {
                    if (transformer.getTransformPredicate().test(codeElement)) {
                        transformer.getTransform(modified).accept(codeBuilder, codeElement);
                        return;
                    }
                }
            };
        }
    }
}


