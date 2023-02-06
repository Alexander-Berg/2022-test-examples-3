package ru.yandex.direct.test.clickhouse;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.clickhouse.ClickHouseClusterBuilder;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.dbutil.wrapper.DataSourceFactory;
import ru.yandex.direct.dbutil.wrapper.SimpleDb;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.test.utils.TestUtils;

/**
 * Тестирование работоспособности ClickHouseCluster без сохранения состояния.
 * Тест тяжёлый, выполняется несколько секунд, поэтому по умолчанию игнорируется, предназначен для запуска вручную.
 * <p>
 * 3 шарда по 2 реплики.
 */
@ParametersAreNonnullByDefault
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class ClickHouseCluster3x2Test extends ClickHouseClusterTestBase {
    @ClassRule
    public static JunitRuleClickHouseCluster junitRuleClickHouseCluster = new JunitRuleClickHouseCluster();

    @Before
    public void createRandomDatabase() throws SQLException {
        dbName = TestUtils.randomName("test_ch_", 16);
        cluster.createDatabaseIfNotExists(dbName);
        dataSources = cluster.getClickHouseJdbcUrls(dbName).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new ClickHouseDataSource(e.getValue())));
    }

    @BeforeClass
    public static void startCluster() throws IOException, InterruptedException {
        ClickHouseClusterBuilder builder3x2 = new ClickHouseClusterBuilder()
                .withZooKeeper(1, "zookeeper");
        for (String hostName : CLICKHOUSES) {
            builder3x2.withClickHouse(hostName);
        }

        builder3x2.shardGroup()
                .withShard(1, CLICKHOUSES[0], CLICKHOUSES[1])
                .withShard(2, CLICKHOUSES[2], CLICKHOUSES[3])
                .withShard(3, CLICKHOUSES[4], CLICKHOUSES[5]);

        cluster = junitRuleClickHouseCluster.build(builder3x2);

        DirectConfig directConfig = DirectConfigFactory.getConfig(EnvironmentType.DB_TESTING);
        originalDbConfigJson = LiveResourceFactory.get(directConfig.getString("db_config")).getContent();
        try (Reader reader = new StringReader(originalDbConfigJson);
             StringWriter writer = new StringWriter()) {
            ClickHouseClusterDbConfig.generateDbConfig(
                    builder3x2, cluster, reader, SimpleDb.PPCHOUSE_PPC.toString(), null, "default", writer);
            dbConfigFactory = new DbConfigFactory(writer.toString());
        }
        dataSourceFactory = new DataSourceFactory(directConfig);
    }

    /**
     * Тестирование Distributed-таблиц, 3 шарда, в каждом по 2 реплики.
     * <p>
     * На каждый запрос в Distributed-таблицу генерируется запрос в одну из реплик каждого шарда.
     * Хост, на который был отправлен запрос, будет выбран в качестве отвечающей реплики в своём шарде.
     * Во всех остальных шардах может быть выбрана любая реплика.
     */
    @Test
    public void distributedTable3x2() throws SQLException {
        Map<String, List<String>> fetchedHostsByHost = distributedTestPart();

        Map<String, Integer> indexes = IntStream.range(0, CLICKHOUSES.length)
                .boxed()
                .collect(Collectors.toMap(
                        i -> CLICKHOUSES[i],
                        i -> i));
        Map<Integer, List<Integer>> fetchedHostsByHostIndexes = fetchedHostsByHost.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> indexes.get(e.getKey()),
                        e -> e.getValue().stream().map(indexes::get).collect(Collectors.toList())));
        Assertions.assertThat(fetchedHostsByHostIndexes)
                .containsOnlyKeys(indexes.values().toArray(new Integer[0]))
                .containsAnyOf(
                        Pair.of(0, Arrays.asList(0, 2, 4)),
                        Pair.of(0, Arrays.asList(0, 2, 5)),
                        Pair.of(0, Arrays.asList(0, 3, 4)),
                        Pair.of(0, Arrays.asList(0, 3, 5)))
                .containsAnyOf(
                        Pair.of(1, Arrays.asList(1, 2, 4)),
                        Pair.of(1, Arrays.asList(1, 2, 5)),
                        Pair.of(1, Arrays.asList(1, 3, 4)),
                        Pair.of(1, Arrays.asList(1, 3, 5)))
                .containsAnyOf(
                        Pair.of(2, Arrays.asList(0, 2, 4)),
                        Pair.of(2, Arrays.asList(0, 2, 5)),
                        Pair.of(2, Arrays.asList(1, 2, 4)),
                        Pair.of(2, Arrays.asList(1, 2, 5)))
                .containsAnyOf(
                        Pair.of(3, Arrays.asList(0, 3, 4)),
                        Pair.of(3, Arrays.asList(0, 3, 5)),
                        Pair.of(3, Arrays.asList(1, 3, 4)),
                        Pair.of(3, Arrays.asList(1, 3, 5)))
                .containsAnyOf(
                        Pair.of(4, Arrays.asList(0, 2, 4)),
                        Pair.of(4, Arrays.asList(0, 3, 4)),
                        Pair.of(4, Arrays.asList(1, 2, 4)),
                        Pair.of(4, Arrays.asList(1, 3, 4)))
                .containsAnyOf(
                        Pair.of(5, Arrays.asList(0, 2, 5)),
                        Pair.of(5, Arrays.asList(0, 3, 5)),
                        Pair.of(5, Arrays.asList(1, 2, 5)),
                        Pair.of(5, Arrays.asList(1, 3, 5)));
    }

    /**
     * Тестирование таблиц ReplicatedMergeTree. 3 шарда, в каждом по 2 реплики.
     * <p>
     * При вставке записи в одну из реплик запись копируется на все остальные реплики.
     * Внутри одного шарда у всех реплик одинаковый набор данных.
     */
    @Test
    public void replicatedMergeTree3x2() throws InterruptedException, SQLException {
        Map<String, List<String>> fetchedHostsByHost = mergeTestPart();

        Map<String, Integer> indexes = IntStream.range(0, CLICKHOUSES.length)
                .boxed()
                .collect(Collectors.toMap(
                        i -> CLICKHOUSES[i],
                        i -> i));
        Map<Integer, List<Integer>> fetchedHostsByHostIndexes = fetchedHostsByHost.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> indexes.get(e.getKey()),
                        e -> e.getValue().stream().map(indexes::get).collect(Collectors.toList())));
        Assertions.assertThat(fetchedHostsByHostIndexes)
                .containsOnly(
                        Pair.of(0, Arrays.asList(0, 1)),
                        Pair.of(1, Arrays.asList(0, 1)),
                        Pair.of(2, Arrays.asList(2, 3)),
                        Pair.of(3, Arrays.asList(2, 3)),
                        Pair.of(4, Arrays.asList(4, 5)),
                        Pair.of(5, Arrays.asList(4, 5)));
    }

    /**
     * Проверяется, что во всех созданных путях в dbconfig указан именно тот хост, который ожидается.
     */
    @Test
    public void allDbConfigMembersWorks() {
        Map<String, String> got = forAllDbConfigChildrenRecursively(SimpleDb.PPCHOUSE_PPC.toString(), dbConfig -> {
            try (Connection conn = dataSourceFactory.createDataSource(dbConfig).getConnection();
                 Statement statement = conn.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT hostName()");
                rs.next();
                return rs.getString(1);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });

        Map<String, String> expected = new HashMap<>();
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:1", CLICKHOUSES[0]);
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:1:replicas:1", CLICKHOUSES[1]);
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:2", CLICKHOUSES[2]);
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:2:replicas:1", CLICKHOUSES[3]);
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:3", CLICKHOUSES[4]);
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:3:replicas:1", CLICKHOUSES[5]);

        Assertions.assertThat(got).isEqualTo(expected);
    }
}
