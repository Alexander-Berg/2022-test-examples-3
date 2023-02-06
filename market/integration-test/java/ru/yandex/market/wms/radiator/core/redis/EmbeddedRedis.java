package ru.yandex.market.wms.radiator.core.redis;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisExecProvider;
import redis.embedded.RedisServer;
import redis.embedded.util.Architecture;
import redis.embedded.util.OS;
import redis.embedded.util.RedisExecutable;

import static redis.embedded.RedisExecProvider.DEFAULT_REDIS_READY_PATTERN;

/**
 * EmbeddedRedis uses redis binary from sandbox.
 * To update it:
 * 1. Build Redis from source, as described on Redis website
 *    (NB: Cannot just use redis from Ubuntu repo: it uses jemalloc, missing from CI boxes)
 * 2. Remove debug information from executable, to save space: {@code strip redis-server}
 * 3. Put it to archive: {@code tar cfj redis.tar.bz2 redis-server
 * 4. Upload to sandbox: {@code ya upload --ttl inf -d='Redis binaries for [OS]' -a=[osx|linux|windows] redis.tar.bz2}
 * 5. Replace corresponding resource id in {@linkplain #sandboxResourceId()}
 */
public class EmbeddedRedis implements BeforeAllCallback, BeforeEachCallback, ExtensionContext.Store.CloseableResource  {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedRedis.class);
    private static final String RESOURCE_NAME = "redis.tar.bz2";

    private static int port;
    private static RedisServer redisServer;

    private static AtomicBoolean started = new AtomicBoolean(false);
    private static RedisClient redisClient;

    @Override
    public void beforeAll(ExtensionContext context) {
        try {
            startedAtPort();
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } catch (Exception e) {
            logger.error("beforeAll failed:", e);
            throw e;
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        redisClient.connect().sync().flushall();
    }

    public static RedisClient getRedisClient() {
        startedAtPort();
        return redisClient;
    }

    public static RedisClient newRedisClient(int database) {
        startedAtPort();
        return newRedisClient(port, database);
    }

    public synchronized static int startedAtPort()  {
        if (started.compareAndSet(false, true)) {
            logger.error("Downloading Redis...");
            var sandboxResourceId = sandboxResourceId();
            var rootPath = FileUtils.getTempDirectory().toPath().resolve("embedded-redis-" + sandboxResourceId);

            if (!rootPath.toFile().exists()) {
                var archive = rootPath.resolve(RESOURCE_NAME).toFile();
                SandboxResourceDownloader.download(sandboxResourceId, archive);
                CompressionUtil.extract(archive, rootPath.toFile());
            }

            var port = nextPort();
            var executablePath = rootPath.resolve("redis-server");
            var executable = executablePath.toFile();

            redisServer = redisServer(port, executable);

            logger.error("Starting Redis on port {}...", port);
            redisServer.start();
            ensureStarted(port);
            return EmbeddedRedis.port = port;
        } else {
            return port;
        }
    }

    private static void ensureStarted(int port) {
        redisClient = newRedisClient(port, 0);

        try (var connection = redisClient.connect()) {
            for (var i = 0; i < 10; i++) {
                if ("PONG".equals(connection.sync().ping())) {
                    break;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static RedisClient newRedisClient(int port, int database) {
        return RedisClient.create(RedisURI.builder()
                .withDatabase(database)
                .withHost("localhost")
                .withPort(port)
                .build()
        );
    }

    private static long sandboxResourceId() {
        if (SystemUtils.IS_OS_WINDOWS) {
            throw new UnsupportedOperationException("Unsupported OS WINDOWS");
        }
        if (SystemUtils.IS_OS_MAC_OSX) {
            return 1177436410;
        }
        if (SystemUtils.IS_OS_LINUX) {
            return 1176055361;
        }
        throw new UnsupportedOperationException("Unknown OS " + SystemUtils.OS_NAME);
    }

    private static RedisServer redisServer(int port, File executable) {
        return new RedisServer.Builder()
                .redisExecProvider(
                        RedisExecProvider.build()
                                .override(
                                        OS.UNIX, Architecture.x86_64,
                                        RedisExecutable.build(executable.toString(), DEFAULT_REDIS_READY_PATTERN)
                                )
                                .override(
                                        OS.MAC_OS_X, Architecture.x86_64,
                                        RedisExecutable.build(executable.toString(), DEFAULT_REDIS_READY_PATTERN)
                                )
                )
                .port(port)
                .logProcessOutput()
                .build();
    }

    @Override
    public void close() {
        this.stop();
    }

    private void stop() {
        if (started.compareAndSet(true, false)) {
            logger.error("Stopping Redis...");
            redisServer.stop();
        }
    }

    private static int nextPort() {
        try {
            try (var socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
