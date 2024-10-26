package org.khaleesicodes.bootstrap;

import java.net.Socket;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public class MatildaAccessControl {

    private static final Set<Module> ALLOWED_MODULES = Set.of(System.class.getModule(),
            MatildaAccessControl.class.getModule()
    );
    private static final Logger LOGGER = Logger.getLogger(MatildaAccessControl.class.getName());
    public static void checkPermission(String method){
        LOGGER.warning("checking method " + method);
        switch (method) {
            case "System.exit":
                if (checkSystemExit()){
                    return;
                }else{
                    // Check if calling method is instance of Test => Calling System.exit is allowed
                    // Else throw Exception
                    throw new RuntimeException("System.exit not allowed");
                }
            case "ProcessBuilder.start":
                throw new RuntimeException("ProceesBuilder.start(...) not allowed");
            case "Socket.connect":
                throw new RuntimeException("Socket not allowed");
            default:
                throw new IllegalArgumentException("Unknown method: " + method);

        }

    }

    // Maybe not needed as solve in one line see lin 37
    public static Boolean checkSystemExit() {
        // 2 Frames need to be skipped in order to check if the caller is junit which is allowed to exit maybe
        var callingClass = callingClass(2);
        System.out.println("Class that initially called the method" + callingClass.toString());
        return callingClass.toString().equals("module gradle.worker");
    }

    // Gets requesting class
    //TODO make private, is just public for testing purposes
    public static Module callingClass(int framesToSkip) {
        Optional<Module> module = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(
                        s -> s.skip(framesToSkip)
                                .map(f -> {
                                    Class<?> declaringClass = f.getDeclaringClass();
                                    return declaringClass.getModule();
                                })
                                .filter(m -> !ALLOWED_MODULES.contains(m))
                                .findFirst()
                );
        return module.orElse(null);
    }

    // ignore for now as we solved it otherwise
    public static Module callingClassModule() {
        // checkSystemexit
        // checkPermission
        // getTransformer
        // method that we are looking for
        // calling class
        int framesToSkip = 1  // getCallingClass (this method)
                + 1  // the checkXxx method
                + 1  // the runtime config method
                + 1  // the instrumented method
                ;
        return callingClass(framesToSkip);
    }

}
