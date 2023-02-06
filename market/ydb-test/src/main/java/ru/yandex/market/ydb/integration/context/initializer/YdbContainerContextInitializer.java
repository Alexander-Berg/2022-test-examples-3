package ru.yandex.market.ydb.integration.context.initializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.yandex.ydb.core.grpc.GrpcTransport;
import com.yandex.ydb.table.Session;
import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.description.TableDescription;
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc;
import com.yandex.ydb.table.transaction.TxControl;
import com.yandex.ydb.table.values.PrimitiveType;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import ru.yandex.market.ydb.integration.application.ApplicationProfile;
import ru.yandex.market.ydb.integration.context.config.TestYdbProperties;
import ru.yandex.market.ydb.integration.utils.TestUtils;

import static java.util.Objects.requireNonNull;
import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

public class YdbContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger LOG = LoggerFactory.getLogger(YdbContainerContextInitializer.class);

    public static final int GRPC_PORT = 2136;
    public static final int WEB_UI_PORT = 8765;
    public static final boolean YA_TEST = TestUtils.isYaTest();
    protected static String prefix = TestYdbProperties.PREFIX;

    public static class ContainerHolder {

        public static final GenericContainer<?> YDB_CONTAINER = new GenericContainer<>(
                "registry.yandex.net/yandex-docker-local-ydb:latest")
                .withStartupTimeout(Duration.of(1, ChronoUnit.MINUTES))
                .withExposedPorts(GRPC_PORT, WEB_UI_PORT)
                .withEnv("YDB_USE_IN_MEMORY_PDISKS", "true")
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .withCreateContainerCmdModifier(modifier ->
                        modifier.withName("ydb-" + UUID.randomUUID().toString()));
    }

    @Override
    public void initialize(@Nonnull ConfigurableApplicationContext applicationContext) {
        Set<String> activeProfiles = Set.of(applicationContext.getEnvironment().getActiveProfiles());

        if (activeProfiles.contains(ApplicationProfile.TESTING) ||
                activeProfiles.contains(ApplicationProfile.PRODUCTION)) {
            return;
        }

        configureYdbProperties(applicationContext, prefix);
        startupYdbContainerAndWait(applicationContext, requireNonNull(
                applicationContext.getEnvironment().getProperty(prefix + ".dbname")
        ), applicationContext.getEnvironment().getProperty(prefix + ".endpoint"));
    }

    private void startupYdbContainerAndWait(@Nonnull ConfigurableApplicationContext applicationContext,
                                            @Nonnull String database,
                                            @Nullable String endpoint) {
        if (YA_TEST) {
            //Ждем запущенный рецептом инстанс YDB
            GrpcTransport transport = GrpcTransport.forEndpoint(
                    requireNonNull(endpoint),
                    database
            ).build();
            TableClient tableClient = TableClient.newClient(GrpcTableRpc.useTransport(transport)).build();
            waitUntilDatabaseStarted(tableClient, database, RateLimiterBuilder
                    .newBuilder()
                    .withRate(12, TimeUnit.MINUTES)
                    .withConstantThroughput()
                    .build(), 120);
        } else {
            //Ждем запущенный контейнером инстанс YDB
            Runtime.getRuntime().addShutdownHook(new Thread(ContainerHolder.YDB_CONTAINER::stop));
            ContainerHolder.YDB_CONTAINER.waitingFor(new YdbCanCreateTableWaitStrategy(database));
            ContainerHolder.YDB_CONTAINER.start();

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    prefix + ".endPoint=",
                    prefix + ".host=" + ContainerHolder.YDB_CONTAINER.getContainerIpAddress(),
                    prefix + ".port=" + ContainerHolder.YDB_CONTAINER.getMappedPort(GRPC_PORT)
            );
        }
    }

    private void configureYdbProperties(@Nonnull ConfigurableApplicationContext applicationContext,
                                        @Nonnull String prefix) {
        if (YA_TEST) {
            var database = ensureStartingSlash(System.getenv("YDB_DATABASE"));
            var endpoint = System.getenv("YDB_ENDPOINT");
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    prefix + ".dbname=" + database,
                    prefix + ".endpoint=" + endpoint
            );
            LOG.info("YDB init database[{}:{}]", database, endpoint);
        } else {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    prefix + ".dbname=/local"
            );
        }
    }

    @Nonnull
    private static String ensureStartingSlash(@Nonnull String dbname) {
        return dbname.startsWith("/") ? dbname : "/" + dbname;
    }

    private static void waitUntilDatabaseStarted(
            @Nonnull TableClient tableClient,
            @Nonnull String path,
            @Nonnull RateLimiter rateLimiter,
            int startupTimeoutSeconds
    ) {
        retryUntilSuccess(startupTimeoutSeconds, TimeUnit.SECONDS, () -> {
            rateLimiter.doWhenReady(() -> {
                LOG.info("Checking  container");
                try {
                    Session session = tableClient.createSession().join()
                            .expect("session should exists");
                    session.executeDataQuery("select 1;", TxControl.staleRo()).join()
                            .expect("should get result");

                    session.createTable(path + "/startup", TableDescription.newBuilder()
                            .addNullableColumn("id", PrimitiveType.int32())
                            .setPrimaryKey("id")
                            .build())
                            .get()
                            .expect("should create table");
                } catch (Exception e) {
                    LOG.warn("Check failed", e);
                    throw new RuntimeException(e);
                }
            });
            return true;
        });
    }

    /**
     * Ждем, пока не получится выполнить запрос в ydb.
     */
    private static class YdbCanCreateTableWaitStrategy extends AbstractWaitStrategy {

        private final String path;

        public YdbCanCreateTableWaitStrategy(String path) {
            this.path = path;
        }

        @Override
        protected void waitUntilReady() {
            String host = waitStrategyTarget.getContainerIpAddress();
            int mappedPort = waitStrategyTarget.getMappedPort(GRPC_PORT);
            GrpcTransport transport = GrpcTransport.forHost(host, mappedPort).build();
            TableClient tableClient = TableClient.newClient(GrpcTableRpc.useTransport(transport)).build();

            waitUntilDatabaseStarted(tableClient, path, RateLimiterBuilder
                    .newBuilder()
                    .withRate(6, TimeUnit.MINUTES)
                    .withConstantThroughput()
                    .build(), (int) startupTimeout.getSeconds());
        }
    }
}
