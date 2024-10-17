package org.khaleesicodes;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        System.out.println("__________START__________");


        try{
            System.out.println("try running exec");
            Runtime.getRuntime().exec("foo");
            throw new Error("FAILED");
        } catch (RuntimeException e) {
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