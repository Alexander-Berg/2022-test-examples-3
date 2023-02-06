package ru.yandex.market.delivery.transport_manager.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@NotThreadSafe
public final class YaGrpcCleanupExtension implements AfterEachCallback, BeforeEachCallback {

    private final List<YaGrpcCleanupExtension.Resource> resources = new ArrayList<>();
    private long timeoutNanos = TimeUnit.SECONDS.toNanos(10L);
    private Stopwatch stopwatch = Stopwatch.createUnstarted();

    private Throwable firstException;

    public YaGrpcCleanupExtension setTimeout(long timeout, TimeUnit timeUnit) {
        checkArgument(timeout > 0, "timeout should be positive");
        timeoutNanos = timeUnit.toNanos(timeout);
        return this;
    }

    public <T extends ManagedChannel> T register(@Nonnull T channel) {
        checkNotNull(channel, "channel");
        register(new YaGrpcCleanupExtension.ManagedChannelResource(channel));
        return channel;
    }

    public <T extends Server> T register(@Nonnull T server) {
        checkNotNull(server, "server");
        register(new YaGrpcCleanupExtension.ServerResource(server));
        return server;
    }

    @VisibleForTesting
    void register(YaGrpcCleanupExtension.Resource resource) {
        resources.add(resource);
    }

    private void teardown() {
        stopwatch.start();

        if (firstException == null) {
            for (int i = resources.size() - 1; i >= 0; i--) {
                resources.get(i).cleanUp();
            }
        }

        for (int i = resources.size() - 1; i >= 0; i--) {
            if (firstException != null) {
                resources.get(i).forceCleanUp();
                continue;
            }

            try {
                boolean released = resources.get(i).awaitReleased(
                    timeoutNanos - stopwatch.elapsed(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
                if (!released) {
                    firstException = new AssertionError(
                        "Resource " + resources.get(i) + " can not be released in time at the end of test");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                firstException = e;
            }

            if (firstException != null) {
                resources.get(i).forceCleanUp();
            }
        }

        resources.clear();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        try {
            teardown();
        } catch (Throwable t) {
            firstException = t;

            try {
                teardown();
            } catch (Throwable t2) {
                throw new Exception(t2);
            }

            throw t;
        }

        if (firstException != null) {
            try {
                throw firstException;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        try {
            teardown();
        } catch (Throwable t) {
            firstException = t;

            try {
                teardown();
            } catch (Throwable t2) {
                throw new Exception(t2);
            }

            throw t;
        }

        if (firstException != null) {
            try {
                throw firstException;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @VisibleForTesting
    interface Resource {
        void cleanUp();

        void forceCleanUp();

        boolean awaitReleased(long duration, TimeUnit timeUnit) throws InterruptedException;
    }

    private static final class ManagedChannelResource implements YaGrpcCleanupExtension.Resource {
        final ManagedChannel channel;

        ManagedChannelResource(ManagedChannel channel) {
            this.channel = channel;
        }

        @Override
        public void cleanUp() {
            channel.shutdown();
        }

        @Override
        public void forceCleanUp() {
            channel.shutdownNow();
        }

        @Override
        public boolean awaitReleased(long duration, TimeUnit timeUnit) throws InterruptedException {
            return channel.awaitTermination(duration, timeUnit);
        }

        @Override
        public String toString() {
            return channel.toString();
        }
    }

    private static final class ServerResource implements YaGrpcCleanupExtension.Resource {
        final Server server;

        ServerResource(Server server) {
            this.server = server;
        }

        @Override
        public void cleanUp() {
            server.shutdown();
        }

        @Override
        public void forceCleanUp() {
            server.shutdownNow();
        }

        @Override
        public boolean awaitReleased(long duration, TimeUnit timeUnit) throws InterruptedException {
            return server.awaitTermination(duration, timeUnit);
        }

        @Override
        public String toString() {
            return server.toString();
        }
    }
}
