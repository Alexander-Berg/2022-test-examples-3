package ru.yandex.direct.test.clickhouse;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assume;
import org.junit.rules.ExternalResource;

import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.clickhouse.ClickHouseClusterBuilder;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.process.Docker;
import ru.yandex.direct.process.DockerContainer;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.utils.AutoCloseableList;
import ru.yandex.direct.utils.Transient;
import ru.yandex.direct.utils.io.TempDirectory;

import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

/**
 * JUnit Rule для создания тестов с настоящим кластером ClickHouse. Требует Docker. Убирается за собой сам.
 */
@ParametersAreNonnullByDefault
public class JunitRuleClickHouseCluster extends ExternalResource {
    private static final Duration DEFAULT_WAIT_CONNECTIVITY = Duration.ofSeconds(15);
    private static final Docker DOCKER = new Docker();
    private final AutoCloseableList<TempDirectory> configTmpDirs = new AutoCloseableList<>();
    private final AutoCloseableList<ClickHouseCluster> clusters = new AutoCloseableList<>();

    @Override
    protected void before() throws Throwable {
        Assume.assumeTrue("ClickHouse cluster requires running Docker", DOCKER.isAvailable());
    }

    @Override
    protected void after() {
        try (AutoCloseableList<?> ignored1 = configTmpDirs) {
            clusters.close();
        }
    }

    /**
     * Создать кластер ClickHouse для теста. По окончании теста кластер будет уничтожен.
     *
     * @param builder Конфигурация кластера.
     */
    public ClickHouseCluster build(ClickHouseClusterBuilder builder) throws IOException, InterruptedException {
        String clusterName = TestUtils.randomName("test_cluster_", 16);
        TempDirectory tmpDir = new TempDirectory(Paths.get(".").toAbsolutePath(), clusterName);
        configTmpDirs.add(new Transient<>(tmpDir));
        TimeZone initialTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(MOSCOW_TIMEZONE));
            try (Transient<ClickHouseCluster> clusterTransient = new Transient<>(
                    builder.build(tmpDir, DOCKER, clusterName, null))) {
                try {
                    clusterTransient.item.awaitConnectivity(DEFAULT_WAIT_CONNECTIVITY);
                    ClickHouseCluster result = clusterTransient.item;
                    clusters.add(clusterTransient);
                    return result;
                } catch (Exception e) {
                    try {
                        Map<String, Optional<String>> logs = clusterTransient.item.readContainersStderr(
                                DockerContainer.Tail.lines(40), Duration.ofSeconds(10));
                        e.addSuppressed(new RuntimeException("Container logs:\n" + logs.entrySet().stream()
                                .map(entry -> String.format("--- %s ---%n%s%n",
                                        entry.getKey(), entry.getValue().orElse("<empty>")))
                                .collect(Collectors.joining("\n"))));
                    } catch (Exception e2) {
                        e.addSuppressed(e2);
                    }
                    throw e;
                }
            }
        } finally {
            TimeZone.setDefault(initialTimeZone);
        }
    }

    /**
     * Создать кластер из одного ClickHouse с названием "clickhouse".
     */
    public ClickHouseCluster singleServerCluster() throws IOException, InterruptedException {
        return build(new ClickHouseClusterBuilder().withClickHouse("clickhouse"));
    }

    /**
     * См. {@link ru.yandex.direct.test.clickhouse.ClickHouseClusterDbConfig}
     */
    public DbConfigFactory createDbConfigFactory(ClickHouseClusterBuilder builder, ClickHouseCluster cluster,
                                                 String injectTo, String dbName) {
        return createDbConfigFactory(
                LiveResourceFactory.get(DirectConfigFactory.getConfig(EnvironmentType.DB_TESTING)
                        .getString("db_config")).getContent(),
                builder, cluster, injectTo, dbName);
    }

    /**
     * См. {@link ru.yandex.direct.test.clickhouse.ClickHouseClusterDbConfig}
     *
     * @param sourceDbConfig Исходный dbconfig, сериализованный в json
     */
    public DbConfigFactory createDbConfigFactory(String sourceDbConfig, ClickHouseClusterBuilder builder,
                                                 ClickHouseCluster cluster, String injectTo, String dbName) {
        try (Reader reader = new StringReader(sourceDbConfig); StringWriter writer = new StringWriter()) {
            ClickHouseClusterDbConfig.generateDbConfig(builder, cluster, reader, injectTo, null, dbName, writer);
            return new DbConfigFactory(writer.toString());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
