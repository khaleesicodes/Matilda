package org.khaleesicodes.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

// Uses customized Filesystem to handle read/write access to files no agent is needed to implement this kid of access controls
// Notes from the Javadoc:
// The first invocation of any of the methods defined by this class causes the default provider to be loaded. The default provider,
// identified by the URI scheme "file", creates the FileSystem that provides access to the file systems accessible to the Java virtual machine.
// Providers are typically installed by placing them in a JAR file on the application class path or in the extension directory,
// the JAR file contains a provider-configuration file named java.nio.file.spi.FileSystemProvider in the resource directory META-INF/services,
// and the file lists one or more fully-qualified names of concrete subclass of FileSystemProvider that have a zero argument constructor.
//If a thread initiates the loading of the installed file system providers and another thread invokes a method that also attempts to load
// the providers then the method will block until the loading completes.
public class MatildaFileSystemProvider extends FileSystemProvider {
    protected final FileSystemProvider delegate;
    private final MatildaAccessController accessController;

    public MatildaFileSystemProvider(FileSystem delegate, MatildaAccessController accessController) {
        this.accessController = accessController;
        this.delegate = delegate.provider();

    }


    @Override
    public String getScheme() {
        return delegate.getScheme();
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        return delegate.newFileSystem(uri, env);
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        return delegate.getFileSystem(uri);
    }

    @Override
    public Path getPath(URI uri) {
        return delegate.getPath(uri);
    }

    public static List<FileSystemProvider> installedProviders() {
        return FileSystemProvider.installedProviders();
    }

    @Override
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        return delegate.newFileSystem(path, env);
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        return delegate.newInputStream(path, options);
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        return delegate.newOutputStream(path, options);
    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return delegate.newFileChannel(path, options, attrs);
    }

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
        return delegate.newAsynchronousFileChannel(path, options, executor, attrs);
    }

    @Override
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
        delegate.createSymbolicLink(link, target, attrs);
    }

    @Override
    public void createLink(Path link, Path existing) throws IOException {
        delegate.createLink(link, existing);
    }

    @Override
    public boolean deleteIfExists(Path path) throws IOException {
        return delegate.deleteIfExists(path);
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        return delegate.readSymbolicLink(link);
    }

    @Override
    public boolean exists(Path path, LinkOption... options) {
        return delegate.exists(path, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributesIfExists(Path path, Class<A> type, LinkOption... options) throws IOException {
        return delegate.readAttributesIfExists(path, type, options);
    }

    // Opens or creates a file, returning a seekable byte channel to access the file.
    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        performAccessCheck(path, options);
        return delegate.newByteChannel(path, options, attrs);
    }

    // Checks different Access Options, Standard READ, WRITE, APPEND, CREATE, CREATE_NEW, DELETE_ON_CLOSE
    private void performAccessCheck(Path path, Set<? extends OpenOption> options) throws IOException {
        for (OpenOption option : options) {
            switch (option){
                case StandardOpenOption.CREATE:
                case StandardOpenOption.CREATE_NEW:
                case StandardOpenOption.WRITE:
                case StandardOpenOption.DELETE_ON_CLOSE:
                case StandardOpenOption.APPEND:
                case StandardOpenOption.TRUNCATE_EXISTING:
                    if (accessController.canWrite(path) == false) {
                        throw new IOException("Permission denied"+ path.toString());
                    }
                    break;

                case StandardOpenOption.READ:
                    if (accessController.canRead(path) == false) {
                        throw new IOException("Permission denied"+ path.toString());
                    };
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + option);
            }
        }
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        performAccessCheck(dir, Set.of(StandardOpenOption.READ));
        return delegate.newDirectoryStream(dir, filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        performAccessCheck(dir, Set.of(StandardOpenOption.WRITE));
        delegate.createDirectory(dir, attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        performAccessCheck(path, Set.of(StandardOpenOption.WRITE));
        delegate.delete(path);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        performAccessCheck(source, Set.of(StandardOpenOption.READ));
        performAccessCheck(target, Set.of(StandardOpenOption.WRITE));
        delegate.copy(source, target, options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        performAccessCheck(source, Set.of(StandardOpenOption.WRITE));
        performAccessCheck(target, Set.of(StandardOpenOption.WRITE));
        delegate.move(source, target, options);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        performAccessCheck(path, Set.of(StandardOpenOption.READ));
        performAccessCheck(path2, Set.of(StandardOpenOption.READ));
        return delegate.isSameFile(path, path2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        performAccessCheck(path, Set.of(StandardOpenOption.READ));
        return delegate.isHidden(path);
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        performAccessCheck(path, Set.of(StandardOpenOption.READ));
        return delegate.getFileStore(path);
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        //TODO check if sysexec needs to be done
        for (AccessMode mode : modes) {
            switch (mode) {
                case READ:
                    performAccessCheck(path, Set.of(StandardOpenOption.READ));
                    break;
                case WRITE:
                    performAccessCheck(path, Set.of(StandardOpenOption.WRITE));
                    break;
                case EXECUTE:
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mode);
            }
        }

        delegate.checkAccess(path, modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        //TODO implement access check, issue with exception that is thrown by perfomAccessCheck
        return delegate.getFileAttributeView(path, type, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        performAccessCheck(path, Set.of(StandardOpenOption.READ));
        return delegate.readAttributes(path, type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        performAccessCheck(path, Set.of(StandardOpenOption.READ));
        return delegate.readAttributes(path, attributes, options);
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        performAccessCheck(path, Set.of(StandardOpenOption.WRITE));
        delegate.setAttribute(path, attribute, value, options);
    }
}
