package org.khaleesicodes.bootstrap;

import java.util.Optional;
import java.util.logging.Logger;

public class MatildaAccessControl {
    private static final Logger LOGGER = Logger.getLogger(MatildaAccessControl.class.getName());
    public static void checkPermission(String method){
        LOGGER.warning("checking method " + method);
        switch (method) {
            case "System.exit":
                // Get Caller Class
                // Check if calling method is instance of Test => Calling System.exit is allowed
                // Else throw Exception
                throw new RuntimeException("System.exit not allowed");
            case "ProcessBuilder.start":
                throw new RuntimeException("ProceesBuilder.start(...) not allowed");
            case "Socket.connect":
                throw new RuntimeException("Socket not allowed");
            default:
                throw new IllegalArgumentException("Unknown method: " + method);

        }

    }

    

}
