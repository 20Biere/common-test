package be.itlive.test.nio;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.rules.ExternalResource;

/**
 * @author vbiertho
 *
 */
public final class NioTemporaryDirectory extends ExternalResource {

    /**
     * @author vbiertho
     *
     */
    private final class DeleteFileVisitorImplementation implements FileVisitor<Path> {
        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    private Path tempRootDirectory;

    public Path getTempRootDirectory() {
        return tempRootDirectory;
    }

    @Override
    protected void before() throws Throwable {
        tempRootDirectory = Files.createTempDirectory("test");
    }

    @Override
    protected void after() {
        try {
            Files.walkFileTree(tempRootDirectory, new DeleteFileVisitorImplementation());
        } catch (final IOException e) {
            throw new junit.framework.AssertionFailedError("Error while deleting files after test. " + e.getMessage());
        }
    }
}
