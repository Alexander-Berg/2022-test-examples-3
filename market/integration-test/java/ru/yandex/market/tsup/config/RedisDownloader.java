package ru.yandex.market.tsup.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static ru.yandex.devtools.test.Paths.getSandboxResourcesRoot;

@Component
@RequiredArgsConstructor
@Slf4j
class RedisDownloader {
    private static final String REDIS_SERVER_BINARY_NAME = "redis-server";

    private static final String TARBALL_FILENAME_LINUX = "redis-3.2.6.tar.gz";
    private static final String TARBALL_SANDBOX_URL_LINUX = "http://proxy.sandbox.yandex-team.ru/204417534";

    private static final String TARBALL_FILENAME_MAC_OS_X = "redis-3.2.6_mac_os_x.tar.gz";
    private static final String TARBALL_SANDBOX_URL_MAC_OS_X = "http://proxy.sandbox.yandex-team.ru/1182831412";

    private final String sandboxOauthToken;

    String getRedisPath() {
        String osName = System.getProperty("os.name");

        switch (osName) {
            case "Linux":
                return getRedisPath(TARBALL_FILENAME_LINUX, TARBALL_SANDBOX_URL_LINUX);
            case "Mac OS X":
                return getRedisPath(TARBALL_FILENAME_MAC_OS_X, TARBALL_SANDBOX_URL_MAC_OS_X);
            default:
                throw new IllegalArgumentException();
        }
    }

    private String getRedisPath(String tarballFilename, String tarballSandboxUrl) {
        Path tarballPath = Paths.get(tarballFilename);
        boolean isSandboxEnv = getSandboxResourcesRoot() != null;

        try {
            if (!tarballPath.toFile().exists()) {
                if (isSandboxEnv) {
                    getFromSandboxRoot(tarballPath);
                } else {
                    downloadFromSandboxProxy(tarballPath, tarballSandboxUrl);
                }
            }

            Path dir = Paths.get("");

            if (getSandboxResourcesRoot() != null && "Linux".equals(System.getProperty("os.name"))) {
                dir = createTmpDir();
            }

            Path pathToRedis = dir.resolve(REDIS_SERVER_BINARY_NAME);

            if (!pathToRedis.toFile().exists()) {
                new ProcessBuilder()
                    .directory(tarballPath.toAbsolutePath().getParent().toFile())
                    .command("tar", "-xvf", tarballPath.toAbsolutePath().toString(),
                        "-C", dir.toAbsolutePath().toString(),
                        REDIS_SERVER_BINARY_NAME)
                    .redirectError(tarballPath.resolveSibling("tar.log").toFile())
                    .start().waitFor();
            }

            return pathToRedis.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Unable to start tests", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Unable to start tests", e);
        }
    }

    private static Path createTmpDir() throws IOException {
        Path dir = Files.createTempDirectory("redis-");
        Thread shutdownThread = new NamedThreadFactory("DeleteTmpRedisDir")
            .newThread(() -> {
                Arrays
                    .stream(Objects.requireNonNull(dir.toFile().listFiles()))
                    .forEach(File::delete);
                dir.toFile().delete();
            });
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        return dir;
    }

    private void getFromSandboxRoot(Path tarball) throws IOException {
        log.info("Using resource from sandbox");
        log.info("Sandbox root is " + getSandboxResourcesRoot());
        log.info("Sandbox root content: " + Arrays.toString(Paths.get(getSandboxResourcesRoot()).toFile().list()));
        if (!Paths.get(getSandboxResourcesRoot()).resolve(tarball).toFile().exists()) {
            throw new IOException("Failed to fetch redis from sandbox");
        }
    }

    private void downloadFromSandboxProxy(Path tarball, String sandboxUrl) throws IOException {
        log.info("Downloading redis");
        URL sandboxProxyUrl = new URL(sandboxUrl);
        URLConnection uc = sandboxProxyUrl.openConnection();
        uc.addRequestProperty("Authorization", "OAuth " + sandboxOauthToken);
        Files.copy(uc.getInputStream(), tarball, REPLACE_EXISTING);
    }
}
