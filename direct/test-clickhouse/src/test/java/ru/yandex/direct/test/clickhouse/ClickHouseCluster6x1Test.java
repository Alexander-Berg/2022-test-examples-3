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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
 * 6 шардов по 1 реплике.
 */
@ParametersAreNonnullByDefault
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class ClickHouseCluster6x1Test extends ClickHouseClusterTestBase {
    @ClassRule
    public static JunitRuleClickHouseCluster junitRuleClickHouseCluster = new JunitRuleClickHouseCluster();

    @BeforeClass
    public static void startCluster() throws IOException, InterruptedException {
        ClickHouseClusterBuilder builder6x1 = new ClickHouseClusterBuilder()
                .withZooKeeper(1, "zookeeper");
        for (String hostName : CLICKHOUSES) {
            builder6x1.withClickHouse(hostName);
        }

        builder6x1.shardGroup()
                .withShard(1, CLICKHOUSES[0])
                .withShard(2, CLICKHOUSES[1])
                .withShard(3, CLICKHOUSES[2])
                .withShard(4, CLICKHOUSES[3])
                .withShard(5, CLICKHOUSES[4])
                .withShard(6, CLICKHOUSES[5]);

        cluster = junitRuleClickHouseCluster.build(builder6x1);

        DirectConfig directConfig = DirectConfigFactory.getConfig(EnvironmentType.DB_TESTING);
        originalDbConfigJson = LiveResourceFactory.get(directConfig.getString("db_config")).getContent();
        try (Reader reader = new StringReader(originalDbConfigJson);
             StringWriter writer = new StringWriter()) {
            ClickHouseClusterDbConfig.generateDbConfig(
                    builder6x1, cluster, reader, SimpleDb.PPCHOUSE_PPC.toString(), null, "default", writer);
            dbConfigFactory = new DbConfigFactory(writer.toString());
        }
        dataSourceFactory = new DataSourceFactory(directConfig);
    }

    @Before
    public void createRandomDatabase() throws SQLException {
        dbName = TestUtils.randomName("test_ch_", 16);
        cluster.createDatabaseIfNotExists(dbName);
        dataSources = cluster.getClickHouseJdbcUrls(dbName).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new ClickHouseDataSource(e.getValue())));
    }

    /**
     * Тестирование Distributed-таблиц, где каждый хост находится в отдельном шарде.
     * <p>
     * На каждый запрос в Distributed-таблицу генерируется запрос в одну из реплик каждого шарда.
     * Так как все шарды содержат по одному хосту, то в итоге должны быть запрошены данные со всех хостов.
     */
    @Test
    public void distributedTable6x1() throws SQLException {
        Map<String, List<String>> fetchedHostsByHost = distributedTestPart();
        Assertions.assertThat(fetchedHostsByHost)
                .containsOnly(Pair.of(CLICKHOUSES[0], Arrays.asList(CLICKHOUSES)),
                        Pair.of(CLICKHOUSES[1], Arrays.asList(CLICKHOUSES)),
                        Pair.of(CLICKHOUSES[2], Arrays.asList(CLICKHOUSES)),
                        Pair.of(CLICKHOUSES[3], Arrays.asList(CLICKHOUSES)),
                        Pair.of(CLICKHOUSES[4], Arrays.asList(CLICKHOUSES)),
                        Pair.of(CLICKHOUSES[5], Arrays.asList(CLICKHOUSES)));
    }

    /**
     * Тестирование таблиц ReplicatedMergeTree. 6 шардов по 1 реплике.
     * <p>
     * При вставке записи в одну из реплик запись больше никуда не копируется.
     */
    @Test
    public void replicatedMergeTree6x1() throws InterruptedException, SQLException {
        Map<String, List<String>> fetchedHostsByHost = mergeTestPart();

        Assertions.assertThat(fetchedHostsByHost)
                .containsOnly(Pair.of(CLICKHOUSES[0], Collections.singletonList(CLICKHOUSES[0])),
                        Pair.of(CLICKHOUSES[1], Collections.singletonList(CLICKHOUSES[1])),
                        Pair.of(CLICKHOUSES[2], Collections.singletonList(CLICKHOUSES[2])),
                        Pair.of(CLICKHOUSES[3], Collections.singletonList(CLICKHOUSES[3])),
                        Pair.of(CLICKHOUSES[4], Collections.singletonList(CLICKHOUSES[4])),
                        Pair.of(CLICKHOUSES[5], Collections.singletonList(CLICKHOUSES[5])));
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
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:2", CLICKHOUSES[1]);
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:3", CLICKHOUSES[2]);
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:4", CLICKHOUSES[3]);
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:5", CLICKHOUSES[4]);
        expected.put(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:6", CLICKHOUSES[5]);

        Assertions.assertThat(got).isEqualTo(expected);
    }

}
