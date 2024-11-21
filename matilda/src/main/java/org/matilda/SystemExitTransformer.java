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

import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;


/**
 * Custom transformer that allows granular blocking of System.exit()
 */
@SuppressWarnings("preview")
public class SystemExitTransformer implements MatildaCodeTransformer {

    private final AtomicBoolean hasRun = new AtomicBoolean(false);
    /**
     * Transforms a class that test positive for the TransformPredicate*
     */
    @Override
    public CodeTransform getTransform() {
        return (codeBuilder, codeElement) -> {
            if (!hasRun.getAndSet(true)) { // this must only be run / added once on top of the method
                var accessControl = ClassDesc.of("org.matilda.bootstrap.MatildaAccessControl");
                    var methodTypeDesc = MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V");
                    codeBuilder
                            // Needs to be hard coded in order to not run into classpath issues when using MatildaAccessControl, as it is not loaded yet
                            .ldc("Runtime.exit")
                            .invokestatic(accessControl, "checkPermission", methodTypeDesc)
                            .with(codeElement);
                } else {
                    codeBuilder.with(codeElement);
                }
            };
        }


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
    public Predicate<MethodModel> getModelPredicate() {
        return methodElements -> {
            String internalName = methodElements.parent().get().thisClass().asInternalName();
                return internalName.equals("java/lang/Runtime")
                    && "exit".equals(methodElements.methodName().stringValue())
                    && "(I)V".equals(methodElements.methodType().stringValue());
        };
    }
}

