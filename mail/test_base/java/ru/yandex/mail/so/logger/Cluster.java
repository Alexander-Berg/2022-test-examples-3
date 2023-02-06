package ru.yandex.mail.so.logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import ru.yandex.function.GenericAutoCloseable;

public interface Cluster extends GenericAutoCloseable<IOException> {
    /**
     * Starts the cluster
     *
     * @throws Exception exception that may be thrown during startup process
     */
    void start() throws Exception;

    /**
     * Gets default logger
     * @return default logger
     */
    Logger logger();

    /**
     * Get default port for the cluster
     *
     * @return default port for the cluster
     * @throws IOException exception that may be thrown during of port number receiving
     */
    int port() throws IOException;

    default int writePort() throws IOException {
        return port();
    }

    default int readPort() throws IOException {
        return port();
    }

    default String loadResource(final String resourcePath) throws Exception {
        return loadFile(getClass().getResource(resourcePath).toURI());
    }

    default String loadSource(final String sourcePath) throws Exception {
        return loadFile(new File(
            ru.yandex.devtools.test.Paths.getSourcePath(sourcePath)
        ).toPath());
    }

    default String loadFile(final URI pathUri) throws Exception {
        Path path = Paths.get(pathUri);
        return loadFile(path);
    }

    default String loadFile(final Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8).trim();
    }

    default void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            logger().warning("Cluster.sleep interrupted: " + e);
        }
    }
}
