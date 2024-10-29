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
package org.khaleesicodes.test.bootstrap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.khaleesicodes.bootstrap.MatildaAccessControl;

//TODO: make test mehtods camelCase
class MatildaAccessControlTest {
    // Don't forget to build first in order to have the updated jars
    @Test
    void testcallingClassFrame0(){
        Module callingClass = MatildaAccessControl.getInstance().callingClass(0);
        Assertions.assertEquals("module matilda.core", callingClass.toString());
    }

    @Test
    void testcallingClassFrame1(){
        Module callingClass = MatildaAccessControl.getInstance().callingClass(1);
        Assertions.assertEquals("module matilda.test", callingClass.toString());
    }

    @Test
    void testcallingClassFrame2(){
        Module callingClass = MatildaAccessControl.getInstance().callingClass(2);
        Assertions.assertEquals("module org.junit.platform.commons", callingClass.toString());
    }


}