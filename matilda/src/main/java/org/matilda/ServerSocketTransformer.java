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
 * Custom Transformer that allows granular blocking of Server Capabilities
 */

public class ServerSocketTransformer implements MatildaCodeTransformer{

    /**
     * Matches MethodeElement against characteristics specific to the java.net.Socket connect() and returns true accordingly
     * MethodModel models a method and can be traversed with a stream
     *
     * @return Predicate - Holds structure of method that should be transformed
     * Gets the method owner/ class method is an element of
     * as we are looking for methods owned by "java/lang/ProcessBuilder" we check for the owner
     * check if method that is called is the connect method
     * check if method has the correct method descriptor
     */
    @Override
    public Predicate<MethodModel> getModelPredicate() {
        return null;
    }

    /**
     * Transforms a class that test positive for the TransformPredicate
     */
    @Override
    public CodeTransform getTransform() {
        return null;
    }
}
