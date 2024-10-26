module matilda.core {
    requires java.logging;
    requires java.base;
    requires java.instrument;
    exports org.khaleesicodes.bootstrap;
    opens org.khaleesicodes to java.instrument;
    opens org.khaleesicodes.bootstrap to java.instrument, matilda.test;
}