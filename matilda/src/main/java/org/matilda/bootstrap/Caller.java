/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matilda.bootstrap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Object that mock the invocation of methods or initialization of Objects for testing purposes
 */
public final class Caller {

    /**
     * Mocks invocation of a method and return object accordingly
     * @param inst - Object of method
     * @param method - that should be invoked
     * @param args - method argument
     * @return - results of invoking the method with the given object
     * @throws InvocationTargetException - if the underlying method throws an exception.
     * @throws IllegalAccessException - if Method object is enforcing Java language access control and the underlying method is inaccessible.
     */

    public static Object call(Object inst, Method method, Object... args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(inst, args);
    }

    /**
     * Mocks initialization of an Object
     * @param constructor - that should be called
     * @param args - parameter of constructor
     * @return - Initialized Object
     * @param <T> - Generic Typ in order to adapt to initialized Object
     * @throws InvocationTargetException - if the underlying constructor throws an exception.
     * @throws IllegalAccessException - if this Constructor object is enforcing Java language access control and the underlying constructor is inaccessible.
     * @throws InstantiationException - if the class that declares the underlying constructor represents an abstract class.
     */
    public static <T> T call(Constructor<T> constructor, Object... args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        return constructor.newInstance(args);
    }
}
