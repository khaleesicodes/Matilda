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
import java.util.function.Predicate;

/**
 *
 * Custom interface to create customized ClassTransformer
 * @author Elina Eickstaedt
 *
 */
@SuppressWarnings("preview")
public interface MatildaCodeTransformer {

    /**
     * Matches CodeElement (Instruction) against elements specific to the java.net.Socket connect() and returns true accordingly
     * A CodeModel describes a Code attribute; we can iterate over its CodeElements and handle those that
     * include symbolic references to other types (JEP466)
     **/
    Predicate<MethodModel> getModelPredicate();

    /**
     * Transforms a class that test positive for the TransformPredicate*
     */
    CodeTransform getTransform();
}
