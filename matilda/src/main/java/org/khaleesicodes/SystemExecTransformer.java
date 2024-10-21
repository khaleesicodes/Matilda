package org.khaleesicodes;

import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@SuppressWarnings("preview")
public class SystemExecTransformer implements MatildaCodeTransformer {
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
