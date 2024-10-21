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

import java.io.IOException;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("__________START__________");

        try{
            System.out.println("try running exec");
            Runtime.getRuntime().exec("foo");
            throw new Error("FAILED");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        // Test statement to check if Open Socket is executed
        try(Socket socket = new Socket("localhost", 9999)){
            System.out.println("try opening a Socket");
            throw new Error("FAILED");
        } catch(RuntimeException e){
            e.printStackTrace();
        }


        // Test statement to check if System exec is blocked

        try{
            System.out.println("try running EXIT");
            System.exit(-1);
            throw new Error("FAILED");
        } catch(RuntimeException e){
            e.printStackTrace();
        }

        System.out.println("__________END__________");

    }



}