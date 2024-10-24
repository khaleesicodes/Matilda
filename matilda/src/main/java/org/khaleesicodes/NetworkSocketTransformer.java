/*
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
package org.khaleesicodes;

import org.khaleesicodes.bootstrap.MatildaAccessControl;

import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("preview")
public class NetworkSocketTransformer implements MatildaCodeTransformer{

    /**
     * Matches CodeElement (Instruction) against elements specific to the java.net.Socket connect() and returns true accordingly
     * A CodeModel describes a Code attribute; we can iterate over its CodeElements and handle those that include symbolic references to other types (JEP466)
     * @return
     */
    @Override
    public Predicate<CodeElement> getTransformPredicate() {
        return codeElement ->
                codeElement instanceof InvokeInstruction i
                        && i.opcode() == Opcode.INVOKEVIRTUAL
                        && "java/net/Socket".equals(i.owner().asInternalName())
                        && "connect".equals(i.name().stringValue())
                        && "(Ljava/net/SocketAddress;)V".equals(i.type().stringValue());
    }

    /**
     * Transforms connect method  so that it throws a runtime exception
     * @param modified
     * @return
     */
    @Override
    public CodeTransform getTransform(AtomicBoolean modified) {
        //TODO find out if whole class or just specific method is being rewritten
        //TODO Check why tranform doesn't work for url connect
        Predicate<CodeElement> predicate = getTransformPredicate();
        return (codeBuilder, codeElement) -> {
            if (predicate.test(codeElement)) {
                Logger.getLogger(NetworkSocketTransformer.class.getName()).log(Level.WARNING, "transform Socket.connect");
                var accessControl = ClassDesc.of("org.khaleesicodes.bootstrap.MatildaAccessControl");
                var methodTypeDesc = MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V");
                codeBuilder
                        .ldc("Socket.connect")
                        .invokestatic(accessControl, "checkPermission", methodTypeDesc)
                        .with(codeElement);
                modified.set(true);
            } else {
                codeBuilder.with(codeElement);
            }
        };
    }

}

