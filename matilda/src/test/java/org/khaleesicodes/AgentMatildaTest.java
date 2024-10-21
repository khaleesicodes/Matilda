package org.khaleesicodes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class AgentMatildaTest {
    @Test
    public void testModifyMethod() throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        // now that we are done rewriting we try to invoke the new class method loading it with a custom
        // class loader to avoid letting the standard class loader load the unchanged class
        class MyClassLoader extends ClassLoader {
            public Class defineClass(String name, byte[] b) {
                return defineClass(name, b, 0, b.length);
            }
        }

        byte[] classBytes = AgentMatildaTest.class.getClassLoader().getResourceAsStream(ProcessBuilder.class.getName().replace('.', '/').concat(".class")).readAllBytes();
        byte[] rewrittenClassContent = AgentMatilda.processClasses(classBytes, new SystemExecTransformer());
        Class c = new MyClassLoader()
                .defineClass(ProcessBuilder.class.getName(), rewrittenClassContent);
        Object o = c.getConstructor().newInstance();

        Method startMethod = c.getMethod("start");
        RuntimeException uOE = Assertions.assertThrows(RuntimeException.class, () -> {
            try {
                startMethod.invoke(o);
            } catch (InvocationTargetException ex) {
                throw ex.getCause(); // get the original exception we don't care about the exception from reflection API
            }
        });
        Assertions.assertEquals("Process Execution not allowed", uOE.getMessage());
    }

}