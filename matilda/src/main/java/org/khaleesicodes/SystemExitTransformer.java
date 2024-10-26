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

import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;


@SuppressWarnings("preview")
public class SystemExitTransformer implements MatildaCodeTransformer {
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
                    var accessControl = ClassDesc.of("org.khaleesicodes.bootstrap.MatildaAccessControl");
                    var methodTypeDesc = MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V");
                    codeBuilder
                            .ldc("System.exit")
                            .invokestatic(accessControl, "checkPermission", methodTypeDesc)
                            .with(codeElement);

                    modified.set(true);
                } else {
                    codeBuilder.with(codeElement);
                }
            };
        }
    }

