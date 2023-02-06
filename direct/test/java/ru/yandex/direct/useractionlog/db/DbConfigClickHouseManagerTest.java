package ru.yandex.direct.useractionlog.db;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.clickhouse.ClickHouseClusterBuilder;
import ru.yandex.direct.clickhouse.ClickHouseUtils;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.dbutil.wrapper.DataSourceFactory;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapper;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;
import ru.yandex.direct.dbutil.wrapper.SimpleDb;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.test.clickhouse.ClusterTestUtil;
import ru.yandex.direct.test.clickhouse.JunitRuleClickHouseCluster;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.useractionlog.TableNames;
import ru.yandex.direct.utils.Checked;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class DbConfigClickHouseManagerTest {
    private static final String[] CLICKHOUSES = new String[]{"ch0", "ch1", "ch2", "ch3"};
    private static final String DB_NAME = "default";
    @ClassRule
    public static JunitRuleClickHouseCluster junitRuleClickHouseCluster = new JunitRuleClickHouseCluster();
    private static ClickHouseClusterBuilder clickHouseClusterBuilder;
    private static ClickHouseCluster clickHouseCluster;
    private static DbConfigFactory dbConfigFactory;
    private static DatabaseWrapperProvider databaseWrapperProvider;
    private static DbConfigClickHouseManager manager;
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @BeforeClass
    public static void initializeClickHouse() throws InterruptedException, IOException {
        clickHouseClusterBuilder = new ClickHouseClusterBuilder()
                .withZooKeeper(1, "zk")
                .withClickHouse(CLICKHOUSES[0])
                .withClickHouse(CLICKHOUSES[1])
                .withClickHouse(CLICKHOUSES[2])
                .withClickHouse(CLICKHOUSES[3]);
        clickHouseClusterBuilder.shardGroup()
                .withShard(1, CLICKHOUSES[0], CLICKHOUSES[1])
                .withShard(2, CLICKHOUSES[2], CLICKHOUSES[3]);

        clickHouseCluster = junitRuleClickHouseCluster.build(clickHouseClusterBuilder);
        dbConfigFactory = junitRuleClickHouseCluster.createDbConfigFactory(
                clickHouseClusterBuilder, clickHouseCluster, SimpleDb.PPCHOUSE_PPC.toString(),
                DB_NAME);
        databaseWrapperProvider = DatabaseWrapperProvider.newInstance(
                new DataSourceFactory(DirectConfigFactory.getConfig(EnvironmentType.DB_TESTING)),
                dbConfigFactory, EnvironmentType.DB_TESTING);

        manager = DbConfigClickHouseManager.create(
                dbConfigFactory, SimpleDb.PPCHOUSE_PPC.toString(), databaseWrapperProvider);
    }

    public static Object[] parametersForGetForReadingBalancing() {
        return new Object[]{
                new Object[]{TableNames.READ_USER_ACTION_LOG_TABLE}};
    }

    public static Object[] parametersForGetForReadingSingle() {
        return new Object[]{
                new Object[]{TableNames.DICT_TABLE},
                new Object[]{TableNames.USER_ACTION_LOG_STATE_TABLE}};
    }

    public static Object[] parametersForGetForWritingBalancing() {
        return new Object[]{
                new Object[]{TableNames.WRITE_USER_ACTION_LOG_TABLE}};
    }

    public static Object[] parametersForGetForWritingSingle() {
        return new Object[]{
                new Object[]{TableNames.DICT_TABLE},
                new Object[]{TableNames.USER_ACTION_LOG_STATE_TABLE}};
    }

    /**
     * Для таблиц, где не нужна линеаризуемая вставка, позволяется балансировка чтения. Могут быть выбраны все шарды и
     * реплики.
     */
    @Parameters
    @Test
    public void getForReadingBalancing(String tableName) throws Exception {
        Set<String> collectedJdbcUrls = applyForAllHostAndCollectJdbcUrls(manager::getForReading, tableName);
        Set<String> expectedJdbcUrls = getAllClickHouseJdbcUrlsFromDbConfig();
        softly.assertThat(collectedJdbcUrls)
                .as("Yielded all possible shards and replicas")
                .isEqualTo(expectedJdbcUrls);
    }

    private Set<String> applyForAllHostAndCollectJdbcUrls(Function<String, DatabaseWrapper> fn, String tableName) {
        // Тесты пользуются знанием того, что ручка выдаёт все шарды и реплики по round-robin
        // Одна и та же функция вызывается столько раз, сколько всего разных хостов в dbconfig
        Set<String> result = new HashSet<>();
        for (String ignored : CLICKHOUSES) {
            result.add(dbConfigFactory.get(fn.apply(tableName).getDbname()).getJdbcUrl());
        }
        return result;
    }

    private Set<String> getAllClickHouseJdbcUrlsFromDbConfig() {
        return ClusterTestUtil
                .getDbConfigChildrenRecursively(dbConfigFactory, SimpleDb.PPCHOUSE_PPC.toString() + ":shards")
                .keySet()
                .stream()
                .map(dbConfigFactory::get)
                .map(DbConfig::getJdbcUrl)
                .collect(Collectors.toSet());
    }

    /**
     * Для таблиц, где нужна линеаризуемая вставка, выбирается одна и та же реплика для чтения.
     */
    @Parameters
    @Test
    public void getForReadingSingle(String tableName) throws Exception {
        Set<String> collectedJdbcUrls = applyForAllHostAndCollectJdbcUrls(manager::getForReading, tableName);
        Set<String> possibleJdbcUrls = getAllClickHouseJdbcUrlsFromDbConfig();
        softly.assertThat(collectedJdbcUrls)
                .as("Yielded one of expected dbconfig path")
                .isSubsetOf(possibleJdbcUrls)
                .as("Always yields same dbconfig path")
                .size().isEqualTo(1);
    }

    /**
     * Для таблиц, где не нужна линеаризуемая вставка, позволяется балансировка записи. Могут быть выбраны все шарды и
     * реплики.
     */
    @Parameters
    @Test
    public void getForWritingBalancing(String tableName) throws Exception {
        Set<String> collectedJdbcUrls = applyForAllHostAndCollectJdbcUrls(manager::getForWriting, tableName);
        Set<String> expectedJdbcUrls = getAllClickHouseJdbcUrlsFromDbConfig();
        softly.assertThat(collectedJdbcUrls)
                .as("Yielded all possible shards and replicas")
                .isEqualTo(expectedJdbcUrls);
    }

    /**
     * Для таблиц, где нужна линеаризуемая вставка, выбирается одна и та же реплика для записи.
     */
    @Parameters
    @Test
    public void getForWritingSingle(String tableName) throws Exception {
        Set<String> collectedJdbcUrls = applyForAllHostAndCollectJdbcUrls(manager::getForWriting, tableName);
        Set<String> possibleJdbcUrls = getAllClickHouseJdbcUrlsFromDbConfig();
        softly.assertThat(collectedJdbcUrls)
                .as("Yielded one of expected dbconfig path")
                .isSubsetOf(possibleJdbcUrls)
                .as("Always yields same dbconfig path")
                .size().isEqualTo(1);
    }

    /**
     * {@link DbConfigClickHouseManager#findEachShardLeader(String, Duration)} действительно возвращает лидеров.
     * Проверяется запросом OPTIMIZE TABLE на непустой таблице - он выполнится только на лидере.
     */
    @Ignore("New clickhouse versions allows to call OPTIMIZE TABLE on replicas")
    @Test
    public void findEachShardLeader() throws Exception {
        new DbSchemaCreator(manager).execute();
        final String tableName = TableNames.WRITE_USER_ACTION_LOG_TABLE;
        final String dbConfigShardPrefix = SimpleDb.PPCHOUSE_PPC.toString() + ":shards";
        Set<String> allMergeTreeShardReplicas = ClusterTestUtil
                .getDbConfigChildrenRecursively(dbConfigFactory, dbConfigShardPrefix)
                .keySet();

        Collection<DatabaseWrapper> leaders = manager.findEachShardLeader(tableName, Duration.ofSeconds(5));

        softly.assertThat(leaders)
                .as("Has host for all shards")
                .hasSize(dbConfigFactory.getShardNumbers(dbConfigShardPrefix).size());

        for (DatabaseWrapper wrapper : leaders) {
            softly.assertThat(wrapper.getDbname())
                    .as("jdbc url of leader from shard " + wrapper.getDbname() + " is from cluster")
                    .isIn(allMergeTreeShardReplicas);
        }

        checkHostsAreLeaders(tableName, allMergeTreeShardReplicas, leaders,
                "INSERT INTO " + tableName + " (type, id, value) VALUES (1, 1, '1')");
    }

    /**
     * {@link DbConfigClickHouseManager#findEachShardLeader(String, Duration)} действительно возвращает лидеров.
     * Проверяется запросом OPTIMIZE TABLE на непустой таблице - он выполнится только на лидере.
     * <p>
     * Для таблиц состояния и временного словаря должен возвращаться только один шард.
     */
    @Ignore("New clickhouse versions allows to call OPTIMIZE TABLE on replicas")
    @Test
    public void findMainShardLeader() throws Exception {
        new DbSchemaCreator(manager).execute();
        final String tableName = TableNames.WRITE_USER_ACTION_LOG_TABLE;
        final String dbConfigShardPrefix = SimpleDb.PPCHOUSE_PPC.toString() + ":shards:1";
        Set<String> allMergeTreeShardReplicas = ClusterTestUtil
                .getDbConfigChildrenRecursively(dbConfigFactory, dbConfigShardPrefix)
                .keySet();

        Collection<DatabaseWrapper> leaders = manager.findEachShardLeader(tableName, Duration.ofSeconds(5));

        softly.assertThat(leaders.stream().map(DatabaseWrapper::getDbname).collect(Collectors.toList()))
                .as("Has host only for one shard 1")
                .hasOnlyOneElementSatisfying(s ->
                        Assertions.assertThat(s).matches(".*:shards:1(:replicas:[0-9]+)?$"));

        checkHostsAreLeaders(tableName, allMergeTreeShardReplicas, leaders,
                "INSERT INTO " + tableName + " (type, id, value, server_uuid, server_event_id)"
                        + " VALUES (1, 1, '1', '1', 1)");
    }

    private void checkHostsAreLeaders(String tableName, Set<String> allMergeTreeShardReplicas,
                                      Collection<DatabaseWrapper> leaders, String insertSql) {
        Set<String> nonLeaders = new HashSet<>(allMergeTreeShardReplicas);
        for (DatabaseWrapper leader : leaders) {
            nonLeaders.remove(leader.getDbname());
        }
        TestUtils.assumeThat("Test should have non-leader replicas", nonLeaders.isEmpty(), Matchers.is(false));

        Consumer<DatabaseWrapper> tryDoJobThatCanDoOnlyLeader = wrapper -> {
            try (Connection conn = wrapper.getDataSource().getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeQuery(insertSql);
                }
                ClickHouseUtils.optimizeTable(conn, DB_NAME, tableName);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        };

        // Если это действительно лидеры, то на них можно ALTER TABLE DROP PARTITION, ошибки не будет
        for (DatabaseWrapper wrapper : leaders) {
            softly.assertThatCode(() -> tryDoJobThatCanDoOnlyLeader.accept(wrapper))
                    .as("Leader " + wrapper.getDbname() + " should be allowed to OPTIMIZE TABLE")
                    .doesNotThrowAnyException();
        }

        for (String path : nonLeaders) {
            DatabaseWrapper wrapper = databaseWrapperProvider.get(path);
            softly.assertThatCode(() -> tryDoJobThatCanDoOnlyLeader.accept(wrapper))
                    .as("Non-leader " + path + " shouldn't be allowed to OPTIMIZE TABLE")
                    .hasNoSuppressedExceptions();
        }
    }

    @Test
    public void checkIsHostUp() {
        DbConfig validConfig = dbConfigFactory.get(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:1");
        softly.assertThat(DbConfigClickHouseManager.isHostUp(validConfig))
                .describedAs("Valid db-config. Host should be up.")
                .isTrue();

        int localPort;
        InetAddress localhost = Checked.get(InetAddress::getLoopbackAddress);
        DbConfig invalidConfig = dbConfigFactory.get(SimpleDb.PPCHOUSE_PPC.toString() + ":shards:1");
        try (ServerSocket serverSocket = new ServerSocket(0, 1, localhost)) {
            localPort = serverSocket.getLocalPort();
            invalidConfig.setHosts(Collections.singletonList(localhost.getHostAddress()));
            invalidConfig.setPort(localPort);
            invalidConfig.setConnectTimeout(1);
            try (Socket ignored = new Socket(localhost.getHostAddress(), localPort)) {
                // serverSocket поддерживает максимум одно соединение, и это соединение уже установлено
                softly.assertThat(DbConfigClickHouseManager.isHostUp(invalidConfig))
                        .describedAs("Should fail with timeout")
                        .isFalse();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // Маловероятно, что за десятки миллисекунд, которые прошли с момента закрытия serverSocket,
        // кто-нибудь захватит тот же порт.
        softly.assertThat(DbConfigClickHouseManager.isHostUp(invalidConfig))
                .describedAs("Should fail with connection refused")
                .isFalse();
    }
}
