module matilda.test {
    requires matilda.core;
    requires org.junit.jupiter.api;
    opens org.khaleesicodes.test to org.junit.platform.commons;
    opens org.khaleesicodes.test.bootstrap to org.junit.platform.commons;
}