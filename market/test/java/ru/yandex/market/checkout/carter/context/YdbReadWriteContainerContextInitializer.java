package ru.yandex.market.checkout.carter.context;

import java.util.Set;
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
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import ru.yandex.market.ydb.integration.application.ApplicationProfile;
import ru.yandex.market.ydb.integration.context.initializer.YdbContainerContextInitializer.ContainerHolder;
import ru.yandex.market.ydb.integration.utils.TestUtils;

import static java.util.Objects.requireNonNull;
import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

public class YdbReadWriteContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final int GRPC_PORT = 2136;
    public static final boolean YA_TEST = TestUtils.isYaTest();
    private static final Logger LOG = LoggerFactory.getLogger(YdbReadWriteContainerContextInitializer.class);
    protected static String prefix = "market.carter.ydb";

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

    @Override
    public void initialize(@Nonnull ConfigurableApplicationContext applicationContext) {
        Set<String> activeProfiles = Set.of(applicationContext.getEnvironment().getActiveProfiles());

        if (activeProfiles.contains(ApplicationProfile.TESTING) ||
                activeProfiles.contains(ApplicationProfile.PRODUCTION)) {
            return;
        }

        configureYdbProperties(applicationContext);
        startupYdbContainerAndWait(applicationContext, requireNonNull(
                applicationContext.getEnvironment().getProperty(prefix + ".read.dbname")
        ), applicationContext.getEnvironment().getProperty(prefix + ".read.endpoint"));
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
                    prefix + ".read.endPoint=",
                    prefix + ".write.endPoint=",
                    prefix + ".read.host=" + ContainerHolder.YDB_CONTAINER.getContainerIpAddress(),
                    prefix + ".write.host=" + ContainerHolder.YDB_CONTAINER.getContainerIpAddress(),
                    prefix + ".read.port=" + ContainerHolder.YDB_CONTAINER.getMappedPort(GRPC_PORT),
                    prefix + ".write.port=" + ContainerHolder.YDB_CONTAINER.getMappedPort(GRPC_PORT)
            );
        }
    }

    private void configureYdbProperties(@Nonnull ConfigurableApplicationContext applicationContext) {
        if (YA_TEST) {
            var database = ensureStartingSlash(System.getenv("YDB_DATABASE"));
            var endpoint = System.getenv("YDB_ENDPOINT");
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    prefix + ".read.dbname=" + database,
                    prefix + ".read.endpoint=" + endpoint,
                    prefix + ".write.dbname=" + database,
                    prefix + ".write.endpoint=" + endpoint
            );
            LOG.info("YDB init database[{}:{}]", database, endpoint);
        } else {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    prefix + ".read.dbname=/local",
                    prefix + ".write.dbname=/local"
            );
        }
    }

    /**
     * Ждем, пока не получится выполнить запрос в ydb.
     */
    private static class YdbCanCreateTableWaitStrategy extends AbstractWaitStrategy {

        private final String path;

        YdbCanCreateTableWaitStrategy(String path) {
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
