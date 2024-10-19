package org.khaleesicodes.filesystem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatildaFileSystemProviderTest {

    @Test
    void testNewInputStream() {

        MatildaFileSystemProvider provider = new MatildaFileSystemProvider(null, null);
        //provider.getFileSystem();
    }

    @Test
    void testNewOutputStream() {
    }
}