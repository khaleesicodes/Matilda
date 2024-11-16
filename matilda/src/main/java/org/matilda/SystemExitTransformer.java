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

import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;


/**
 * Custom Transformer that allows granular blocking of System.exit()
 */
@SuppressWarnings("preview")
public class SystemExitTransformer implements MatildaCodeTransformer {
    /**
     * Matches CodeElement (Instruction) against elements specific to the java.lang.System exit() and returns true accordingly
     * A CodeModel describes a Code attribute; we can iterate over its CodeElements and handle those that
     * include symbolic references to other types (JEP466)
     *
     * @return Predicate - Holds structure of class that should be transformed
     *  Uses the invokeinstruction of current codeElement, as we are looking for a method that is invoked virtual we check
     * for INVOKEVIRTUAL
     * as we are looking for methods owned by "java/lang/ProcessBuilder" we check for the owner
     * check if method that is called is the start method
     * check if method has the correct method descriptor
     */
    @Override
    public Predicate<CodeElement> getTransformPredicate() {
        return codeElement -> codeElement instanceof InvokeInstruction i
                // checks if i is invoked virtual
                && i.opcode() == Opcode.INVOKESTATIC
                // compare class we are looking for to method owner of the currently called
                // method using their internal byte name
                && "java/lang/System".equals(i.owner().asInternalName())
                // check if method called equals start method
                && "exit".equals(i.name().stringValue())
                //check for the correct method descriptor
                // L is a reference to className
                && "(I)V".equals(i.type().stringValue());
    }

    /**
     * Transformes a class that test positiv for the TransformPredicate
     * @param modified - Flags wether class has been transformed
     *
     */
    @Override
    public CodeTransform getTransform(AtomicBoolean modified) {
        Predicate<CodeElement> predicate = getTransformPredicate();
        return (codeBuilder, codeElement) -> {
            if (predicate.test(codeElement)) {
                    var accessControl = ClassDesc.of("org.matilda.bootstrap.MatildaAccessControl");
                    var methodTypeDesc = MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V");
                    codeBuilder
                            // Needs to be hard coded in order to not run into classpath issues when using MatildaAccessControl, as it is not loaded yet
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

