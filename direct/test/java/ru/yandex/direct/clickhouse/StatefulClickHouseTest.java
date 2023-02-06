package ru.yandex.direct.clickhouse;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.process.Docker;
import ru.yandex.direct.process.DockerContainer;
import ru.yandex.direct.utils.Checked;
import ru.yandex.direct.utils.io.TempDirectory;

/**
 * Тестирование способности ClickHouseCluster сохранять состояние на диск и переживать перезапуски.
 * Тест тяжёлый, выполняется несколько десятков секунд, поэтому по умолчанию игнорируется, предназначен для запуска
 * вручную.
 */
@ParametersAreNonnullByDefault
@Ignore("docker needed for run")
public class StatefulClickHouseTest {
    private static final String CLUSTER_NAME = "stateful_clickhouse_cluster_test";
    private static final String[] CLICKHOUSES = new String[]{"clickhouse01", "clickhouse02"};

    @BeforeClass
    public static void checkDocker() throws InterruptedException {
        Assume.assumeTrue("This test requires running Docker", new Docker().isAvailable());
    }

    private static ClickHouseCluster makeCluster(TempDirectory tmpDir) throws IOException, InterruptedException {
        ClickHouseClusterBuilder builder = new ClickHouseClusterBuilder()
                .withZooKeeper(1, "zookeeper");
        for (String hostName : CLICKHOUSES) {
            builder.withClickHouse(hostName);
        }
        int shardNumber = 1;
        for (String host : CLICKHOUSES) {
            builder.shardGroup().withShard(shardNumber++, host);
        }
        return builder.build(tmpDir, new Docker(), CLUSTER_NAME, tmpDir.getPath().resolve("volumes"));
    }

    /**
     * После перезапуска кластера все данные должны сохраниться, должна быть возможность записывать новые данные.
     * У пользователя, запускающего кластер, должны быть права на запись создаваемыми кластером файлов и каталогов.
     */
    @Test
    public void runWithRestarts() throws Exception {
        Map<String, List<Pair<String, Integer>>> countsByHostFirst = new HashMap<>();
        Map<String, List<Pair<String, Integer>>> countsByHostSecond = new HashMap<>();
        Map<String, List<Pair<String, Integer>>> countsByHostThird = new HashMap<>();
        try (TempDirectory tmpDir = new TempDirectory(Paths.get(".").toAbsolutePath(), CLUSTER_NAME)) {
            launchClusterAndExecute(tmpDir, dataSources -> {
                initializeDatabases(dataSources);
                insertHosts(dataSources);
                fillCountsByHost(countsByHostFirst, dataSources);
            });
            launchClusterAndExecute(tmpDir, dataSources -> {
                insertHosts(dataSources);
                fillCountsByHost(countsByHostSecond, dataSources);
            });
            launchClusterAndExecute(tmpDir, dataSources -> {
                insertHosts(dataSources);
                fillCountsByHost(countsByHostThird, dataSources);
            });
        }

        Assertions.assertThat(countsByHostFirst)
                .containsOnly(
                        Pair.of(CLICKHOUSES[0], Arrays.asList(
                                Pair.of(CLICKHOUSES[0], 1),
                                Pair.of(CLICKHOUSES[1], 1))),
                        Pair.of(CLICKHOUSES[1], Arrays.asList(
                                Pair.of(CLICKHOUSES[0], 1),
                                Pair.of(CLICKHOUSES[1], 1))));
        Assertions.assertThat(countsByHostSecond)
                .containsOnly(
                        Pair.of(CLICKHOUSES[0], Arrays.asList(
                                Pair.of(CLICKHOUSES[0], 2),
                                Pair.of(CLICKHOUSES[1], 2))),
                        Pair.of(CLICKHOUSES[1], Arrays.asList(
                                Pair.of(CLICKHOUSES[0], 2),
                                Pair.of(CLICKHOUSES[1], 2))));
        Assertions.assertThat(countsByHostThird)
                .containsOnly(
                        Pair.of(CLICKHOUSES[0], Arrays.asList(
                                Pair.of(CLICKHOUSES[0], 3),
                                Pair.of(CLICKHOUSES[1], 3))),
                        Pair.of(CLICKHOUSES[1], Arrays.asList(
                                Pair.of(CLICKHOUSES[0], 3),
                                Pair.of(CLICKHOUSES[1], 3))));
    }

    private void fillCountsByHost(Map<String, List<Pair<String, Integer>>> countsByHost,
                                  Map<String, DataSource> dataSources)
            throws SQLException {
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            List<Pair<String, Integer>> counts = new ArrayList<>();
            countsByHost.put(entry.getKey(), counts);
            try (Connection conn = entry.getValue().getConnection();
                 Statement statement = conn.createStatement()) {
                ResultSet resultSet = statement.executeQuery(
                        "SELECT `host`, count() FROM `distr` GROUP BY `host` ORDER BY `host`");
                while (resultSet.next()) {
                    counts.add(Pair.of(resultSet.getString(1), resultSet.getInt(2)));
                }
            }
        }
    }

    private void initializeDatabases(Map<String, DataSource> dataSources) throws SQLException {
        CompletableFuture.allOf(dataSources.entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(Checked.runnable(() -> {
                    try (Connection conn = entry.getValue().getConnection()) {
                        try (Statement statement = conn.createStatement()) {
                            statement.executeUpdate("CREATE TABLE `data` (`host` String) ENGINE = TinyLog()");
                        }
                        try (Statement statement = conn.createStatement()) {
                            statement.executeUpdate("CREATE TABLE `distr` (`host` String)"
                                    + " ENGINE = Distributed(`shard`, `default`, `data`)");
                        }
                    }
                })))
                .toArray(CompletableFuture[]::new)
        ).join();
    }

    private void insertHosts(Map<String, DataSource> dataSources) throws SQLException, InterruptedException {
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            try (Connection conn = entry.getValue().getConnection()) {
                String sql = "INSERT INTO `data` (`host`) VALUES (?)";
                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.setString(1, entry.getKey());
                    statement.execute();
                }
            }
        }
    }

    private void launchClusterAndExecute(TempDirectory tmpDir,
                                         Checked.CheckedConsumer<Map<String, DataSource>, ?> callback)
            throws Exception {
        try (ClickHouseCluster cluster = makeCluster(tmpDir)) {
            try {
                cluster.awaitConnectivity(Duration.ofSeconds(3));
                Map<String, DataSource> dataSources = cluster.getClickHousesStream().collect(Collectors.toMap(
                        DockerContainer::getHostname,
                        ClickHouseContainer::getDataSource
                ));
                callback.accept(dataSources);
            } catch (Exception e) {
                try {
                    Map<String, Optional<String>> logs =
                            cluster.readContainersStderr(DockerContainer.Tail.lines(40), Duration.ofSeconds(10));
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
    }
}
