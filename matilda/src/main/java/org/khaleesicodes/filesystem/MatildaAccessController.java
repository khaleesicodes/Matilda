package org.khaleesicodes.filesystem;

import java.nio.file.Path;

public interface MatildaAccessController {
    public boolean canRead(Path path);
    public boolean canWrite(Path path);
}
