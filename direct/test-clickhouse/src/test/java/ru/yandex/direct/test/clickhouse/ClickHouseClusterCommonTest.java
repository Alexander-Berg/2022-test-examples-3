package ru.yandex.direct.test.clickhouse;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
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
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.Parameters;
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
import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.db.config.DbConfigException;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.dbutil.wrapper.DataSourceFactory;
import ru.yandex.direct.dbutil.wrapper.SimpleDb;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.process.DockerContainer;
import ru.yandex.direct.test.utils.TestUtils;

@ParametersAreNonnullByDefault
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class ClickHouseClusterCommonTest extends ClickHouseClusterTestBase {
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
        ClickHouseClusterBuilder builder = new ClickHouseClusterBuilder()
                .withZooKeeper(1, "zookeeper");
        for (String hostName : CLICKHOUSES) {
            builder.withClickHouse(hostName);
        }

        builder.shardGroup()
                .withShard(1, CLICKHOUSES[0], CLICKHOUSES[1])
                .withShard(2, CLICKHOUSES[2], CLICKHOUSES[3])
                .withShard(3, CLICKHOUSES[4], CLICKHOUSES[5]);

        cluster = junitRuleClickHouseCluster.build(builder);

        DirectConfig directConfig = DirectConfigFactory.getConfig(EnvironmentType.DB_TESTING);
        originalDbConfigJson = LiveResourceFactory.get(directConfig.getString("db_config")).getContent();
        try (Reader reader = new StringReader(originalDbConfigJson);
             StringWriter writer = new StringWriter()) {
            ClickHouseClusterDbConfig.generateDbConfig(
                    builder, cluster, reader, SimpleDb.PPCHOUSE_PPC.toString(), null, "default", writer);
            dbConfigFactory = new DbConfigFactory(writer.toString());
        }
        dataSourceFactory = new DataSourceFactory(directConfig);
    }

    /**
     * Тестирование работоспособности каждого отдельного узла в кластере, без реплик и шардов.
     */
    @Test
    @Parameters
    public void simpleTable() throws SQLException {
        Map<String, List<Integer>> insertedData = new HashMap<>();
        insertedData.put(CLICKHOUSES[0], Arrays.asList(27374, 38273, 43963, 53621));
        insertedData.put(CLICKHOUSES[1], Arrays.asList(831));
        insertedData.put(CLICKHOUSES[2], Arrays.asList(36025, 40881, 31304, 20723));
        insertedData.put(CLICKHOUSES[3], Arrays.asList(63007));
        insertedData.put(CLICKHOUSES[4], Arrays.asList(13165, 52167, 17596));
        insertedData.put(CLICKHOUSES[5], Arrays.asList(44402, 5427));
        Assertions.assertThat(insertedData.size()).isEqualTo(CLICKHOUSES.length);

        for (Map.Entry<String, List<Integer>> entry : insertedData.entrySet()) {
            try (Connection conn = dataSources.get(entry.getKey()).getConnection()) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("CREATE TABLE test_simple_table (value Int32) ENGINE = TinyLog()");
                }
                try (PreparedStatement st = conn.prepareStatement("INSERT INTO test_simple_table (value) VALUES (?)")) {
                    for (int value : insertedData.get(entry.getKey())) {
                        st.setInt(1, value);
                        st.addBatch();
                    }
                    st.executeBatch();
                }
            }
        }

        Map<String, Integer> summedDataByHost = new HashMap<>();
        for (String host : CLICKHOUSES) {
            try (Connection conn = dataSources.get(host).getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT sum(value) FROM test_simple_table")) {
                if (rs.next()) {
                    summedDataByHost.put(host, rs.getInt(1));
                }
            }
        }

        Assertions.assertThat(summedDataByHost)
                .isEqualTo(insertedData.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().reduce((x, y) -> x + y).orElse(0))));
    }

    /**
     * smoke-тест на метод readContainerStderrs
     */
    @Test
    public void readContainerStderrs() throws InterruptedException {
        Map<String, Optional<String>> logs =
                cluster.readContainersStderr(DockerContainer.Tail.lines(1), Duration.ofSeconds(3));
        Assertions.assertThat(logs)
                .containsKey("zookeeper")
                .containsKeys(CLICKHOUSES)
                .doesNotContainValue(Optional.empty());
    }

    /**
     * Проверяется, что патчинг dbconfig не портит другие dbconfig-и.
     */
    @Test
    public void otherDbConfigsNotBroken() {
        DbConfigFactory originalDbConfigFactory = new DbConfigFactory(originalDbConfigJson);
        List<String> checkDbConfigs = new ArrayList<>();
        for (String label : Arrays.asList("ppc", "ppcdict", "redis", "ppchouse")) {
            try {
                checkDbConfigs.addAll(originalDbConfigFactory.getChildNames(label).stream()
                        .map(x -> label + ":" + x)
                        .filter(x -> !x.equals(SimpleDb.PPCHOUSE_PPC.toString()))
                        .collect(Collectors.toList()));
            } catch (DbConfigException e) {
                if (!e.getMessage().equals("No such path: ppchouse")) {
                    throw e;
                }
            }
        }
        Map<String, DbConfig> originalDbConfigs = checkDbConfigs.stream()
                .collect(Collectors.toMap(k -> k, originalDbConfigFactory::get));
        Map<String, DbConfig> patchedDbConfigs = checkDbConfigs.stream()
                .collect(Collectors.toMap(k -> k, dbConfigFactory::get));
        Assertions.assertThat(patchedDbConfigs)
                .isEqualToComparingFieldByFieldRecursively(originalDbConfigs);
    }
}
