package org.khaleesicodes;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        System.out.println("Hello world!");
        // Test statement to check if System exec is blocked
        System.exit(-1);

        System.out.println("foobar");
    }
}