package org.khaleesicodes.test.bootstrap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.khaleesicodes.bootstrap.MatildaAccessControl;


class MatildaAccessControlTest {

    public static void main(String[] args) {
        Module callingClass = MatildaAccessControl.callingClass(0);

    }
    // Don't forget to build first in order to have the updated jars
    @Test
    void testcallingClassFrame0(){
        Module callingClass = MatildaAccessControl.callingClass(0);
        Assertions.assertEquals("module matilda.test", callingClass.toString());
        //Assertions.fail(" Frame0: " + callingClass);
    }

    @Test
    void testcallingClassFrame1(){
        Module callingClass = MatildaAccessControl.callingClass(1);
        Assertions.assertEquals("module matilda.test", callingClass.toString());

    }

    @Test
    void testcallingClassFrame2(){
        Module callingClass = MatildaAccessControl.callingClass(2);
        Assertions.assertEquals("module org.junit.platform.commons", callingClass.toString());
    }

}