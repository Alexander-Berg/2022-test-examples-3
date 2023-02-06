package ru.yandex.direct.binlogclickhouse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.binlog.reader.BinlogStateOptimisticSnapshotter;
import ru.yandex.direct.binlog.reader.BinlogStateUnsafeSnapshotter;
import ru.yandex.direct.binlogclickhouse.schema.BinlogStateSchema;
import ru.yandex.direct.binlogclickhouse.schema.BinlogStateTable;
import ru.yandex.direct.binlogclickhouse.schema.DbChangeLog;
import ru.yandex.direct.binlogclickhouse.schema.DbChangeLogRecord;
import ru.yandex.direct.binlogclickhouse.schema.DbChangeLogSchema;
import ru.yandex.direct.binlogclickhouse.schema.FieldValue;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.binlogclickhouse.schema.Operation;
import ru.yandex.direct.binlogclickhouse.schema.QueryLog;
import ru.yandex.direct.binlogclickhouse.schema.QueryLogRecord;
import ru.yandex.direct.binlogclickhouse.schema.QueryLogSchema;
import ru.yandex.direct.clickhouse.ClickHouseContainer;
import ru.yandex.direct.mysql.AsyncStreamer;
import ru.yandex.direct.mysql.LastQueryWatcher;
import ru.yandex.direct.mysql.MySQLBinlogDataClientProvider;
import ru.yandex.direct.mysql.MySQLBinlogDataStreamer;
import ru.yandex.direct.mysql.MySQLBinlogState;
import ru.yandex.direct.mysql.MySQLInstance;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.mysql.MySQLUtils;
import ru.yandex.direct.mysql.TmpMySQLServerWithDataDir;
import ru.yandex.direct.process.Docker;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;
import ru.yandex.direct.tracing.data.DirectTraceInfo;
import ru.yandex.direct.utils.Interrupts;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class DbChangeLogTest {
    // TODO akimov@: протестировать отсутствие ключа
    // TODO akimov@: протестировать unique key вместо primary key
    // TODO akimov@: протестировать null в primary key

    private static final String TEST_DB_NAME = "changelog_test";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final int SERVER_ID = 99;

    private static final List<QueryLogRecord> EXPECTED_QUERYLOG = Arrays.asList(
            new TestQueryLogRecord(new DirectTraceInfo(872326L), "src", 0, "/* reqid:872326 */ INSERT INTO table1 " +
                    "(value) values (1), (2), (42)"),
            new TestQueryLogRecord(new DirectTraceInfo(1L), "src", 0, "UPDATE /* reqid:1 */ table1 SET value=null " +
                    "WHERE id=3"),
            new TestQueryLogRecord(DirectTraceInfo.empty(), "src", 0, "DELETE FROM table1 WHERE value is null"),
            new TestQueryLogRecord(DirectTraceInfo.empty(), "src", 0, "UPDATE table1 SET id=22,value=43 WHERE value=2"),
            new TestQueryLogRecord(DirectTraceInfo.empty(), "src", 0, "UPDATE table1 SET id=11 WHERE value=1"),
            new TestQueryLogRecord(DirectTraceInfo.empty(), "src", 1, "UPDATE table1 SET id=12 WHERE value=1")
    );

    private static final List<DbChangeLogRecord> EXPECTED_CHANGELOG = Arrays.asList(
            new TestDbChangeLogRecord(
                    new DirectTraceInfo(872326L), "src", TEST_DB_NAME, "table1", Operation.INSERT, 0, 0,
                    new FieldValueList(new FieldValue<>("id", "1")),
                    new FieldValueList(new FieldValue<>("value", "1"))
            ),
            new TestDbChangeLogRecord(
                    new DirectTraceInfo(872326L), "src", TEST_DB_NAME, "table1", Operation.INSERT, 0, 1,
                    new FieldValueList(new FieldValue<>("id", "2")),
                    new FieldValueList(new FieldValue<>("value", "2"))
            ),
            new TestDbChangeLogRecord(
                    new DirectTraceInfo(872326L), "src", TEST_DB_NAME, "table1", Operation.INSERT, 0, 2,
                    new FieldValueList(new FieldValue<>("id", "3")),
                    new FieldValueList(new FieldValue<>("value", "42"))
            ),
            new TestDbChangeLogRecord(
                    new DirectTraceInfo(1L), "src", TEST_DB_NAME, "table1", Operation.UPDATE, 0, 0,
                    new FieldValueList(new FieldValue<>("id", "3")),
                    new FieldValueList(new FieldValue<>("value", null))
            ),
            new TestDbChangeLogRecord(
                    DirectTraceInfo.empty(), "src", TEST_DB_NAME, "table1", Operation.DELETE, 0, 0,
                    new FieldValueList(new FieldValue<>("id", "3")),
                    FieldValueList.empty()
            ),
            new TestDbChangeLogRecord(
                    DirectTraceInfo.empty(), "src", TEST_DB_NAME, "table1", Operation.UPDATE, 0, 0,
                    new FieldValueList(new FieldValue<>("id", "2")),
                    new FieldValueList(new FieldValue<>("id", "22"), new FieldValue<>("value", "43"))
            ),
            new TestDbChangeLogRecord(
                    DirectTraceInfo.empty(), "src", TEST_DB_NAME, "table1", Operation.UPDATE, 0, 0,
                    new FieldValueList(new FieldValue<>("id", "1")),
                    new FieldValueList(new FieldValue<>("id", "11"))
            ),
            new TestDbChangeLogRecord(
                    DirectTraceInfo.empty(), "src", TEST_DB_NAME, "table1", Operation.UPDATE, 1, 1,
                    new FieldValueList(new FieldValue<>("id", "11")),
                    new FieldValueList(new FieldValue<>("id", "12"))
            )
    );

    public static final Docker DOCKER = new Docker();

    public static final MySQLServerBuilder MYSQL_BUILDER = new DirectMysqlDb(TestMysqlConfig.directConfig())
            .useSandboxMysqlServerIfPossible(
                    new MySQLServerBuilder().setGracefulStopTimeout(Duration.ofSeconds(0)).withNoSync(true)
            );

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @ClassRule
    public static final TemporaryFolder SOURCE_DATA_DIR = new TemporaryFolder() {
        @Override
        protected void before() throws Throwable {
            assumeTrue(MYSQL_BUILDER.mysqldIsAvailable());
            super.before();
            MYSQL_BUILDER.copy().setDataAndConfigDir(SOURCE_DATA_DIR.getRoot().toPath()).initializeDataDir();
        }
    };

    @ClassRule
    @Rule
    public static final DbCleanerRule<TmpMySQLServerWithDataDir> MYSQL_WITH_BINLOG1 = new DbCleanerRule<>(
            () -> TmpMySQLServerWithDataDir.createWithBinlog(
                    "mysql-with-binlog1",
                    MYSQL_BUILDER.copy().setServerId(1),
                    SOURCE_DATA_DIR.getRoot().toPath()
            )
    );

    @ClassRule
    @Rule
    public static final DbCleanerRule<TmpMySQLServerWithDataDir> MYSQL_WITH_BINLOG2 = new DbCleanerRule<>(
            () -> TmpMySQLServerWithDataDir.createWithBinlog(
                    "mysql-with-binlog2",
                    MYSQL_BUILDER.copy().setServerId(2),
                    SOURCE_DATA_DIR.getRoot().toPath()
            )
    );

    @ClassRule
    @Rule
    public static final DbCleanerRule<TmpMySQLServerWithDataDir> MYSQL1 = new DbCleanerRule<>(
            () -> TmpMySQLServerWithDataDir.create(
                    "mysql1",
                    MYSQL_BUILDER.copy(),
                    SOURCE_DATA_DIR.getRoot().toPath()
            )
    );

    @ClassRule
    @Rule
    public static final DbCleanerRule<TmpMySQLServerWithDataDir> MYSQL2 = new DbCleanerRule<>(
            () -> TmpMySQLServerWithDataDir.create(
                    "mysql1",
                    MYSQL_BUILDER.copy(),
                    SOURCE_DATA_DIR.getRoot().toPath()
            )
    );

    @ClassRule
    @Rule
    public static final DbCleanerRule<ClickHouseContainer> CLICKHOUSE =
            Interrupts.failingGet(DOCKER::isAvailable)
                    ? new DbCleanerRule<>(() -> ClickHouseContainer.builder().withDocker(DOCKER).build())
                    : null;

    private static final String ROWS_TABLE = "binlog_rows";
    private static final String QUERIES_TABLE = "binlog_queries";
    private static final String STATE_TABLE = "binlog_state";

    public static ClickHouseContainer getClickhouse() {
        assumeNotNull(CLICKHOUSE);
        return CLICKHOUSE.getDb();
    }

    private static void createClickHouseDb(String name, Connection clickHouseConn) throws SQLException {
        MySQLUtils.executeUpdate(clickHouseConn, "CREATE DATABASE " + MySQLUtils.quoteName(name) + ";");

        new DbChangeLogSchema(name, ROWS_TABLE).createTable(clickHouseConn);
        new QueryLogSchema(name, QUERIES_TABLE).createTable(clickHouseConn);
        new BinlogStateSchema(name, STATE_TABLE).createTable(clickHouseConn);
    }

    private void initMySQLTestDb(Connection mysqlConn) throws SQLException {
        MySQLUtils.executeUpdate(mysqlConn, "CREATE DATABASE " + MySQLUtils.quoteName(TEST_DB_NAME));
        mysqlConn.setCatalog(TEST_DB_NAME);
        MySQLUtils.executeUpdate(
                mysqlConn,
                "CREATE TABLE table1 (id int not null primary key auto_increment, value int)"
        );
    }

    private static MySQLBinlogDataClientProvider makeBinlogProvider(MySQLInstance mysql) {
        return new MySQLBinlogDataClientProvider(
                mysql.getHost(),
                mysql.getPort(),
                mysql.getUsername(),
                mysql.getPassword(),
                SERVER_ID
        );
    }

    private static void waitTransactionsCount(DbChangeLogWriter writer, Duration timeout, long minTransactionsCount) {
        Interrupts.criticalTimeoutWait(
                timeout,
                remainingTimeout -> {
                    Thread.sleep(100);
                    return writer.getTransactionsCount() >= minTransactionsCount;
                }
        );
    }

    @Test
    public void test() throws IOException, SQLException, InterruptedException, TimeoutException {
        String source = "src";
        TmpMySQLServerWithDataDir mysql = MYSQL_WITH_BINLOG1.getDb();
        TmpMySQLServerWithDataDir streamerSchemaReplicator = MYSQL1.getDb();
        ClickHouseContainer clickHouse = getClickhouse();

        try (
                Connection clickHouseConn = clickHouse.connect(CONNECT_TIMEOUT);
                Connection mysqlConn = mysql.connect(CONNECT_TIMEOUT)
        ) {
            createClickHouseDb(TEST_DB_NAME, clickHouseConn);

            DbChangeLog changeLog = new DbChangeLogSchema(TEST_DB_NAME, ROWS_TABLE).connect(clickHouseConn);
            QueryLog queryLog = new QueryLogSchema(TEST_DB_NAME, QUERIES_TABLE).connect(clickHouseConn);
            BinlogStateTable binlogState = new BinlogStateSchema(TEST_DB_NAME, STATE_TABLE).connect(clickHouseConn);

            initMySQLTestDb(mysqlConn);

            try (
                    DbCleaner ignored = new DbCleaner(streamerSchemaReplicator.connect(CONNECT_TIMEOUT));
                    BufferedInserter inserter = new BufferedInserter(
                            new ClickHouseInserter(changeLog, queryLog, binlogState),
                            Duration.ofMinutes(5),
                            3
                    );
                    AsyncStreamer asyncStreamer = new AsyncStreamer(
                            new MySQLBinlogDataStreamer(makeBinlogProvider(mysql),
                                    MySQLBinlogState.snapshot(mysqlConn)),
                            new LastQueryWatcher(new ClickHouseConsumer(source, inserter)),
                            streamerSchemaReplicator
                    )
            ) {
                mysqlConn.setCatalog(TEST_DB_NAME);

                MySQLUtils.executeUpdate(mysqlConn, "/* reqid:872326 */ INSERT INTO table1 (value) values (1), (2), " +
                        "(42)");
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE /* reqid:1 */ table1 SET value=null WHERE id=3");
                MySQLUtils.executeUpdate(mysqlConn, "DELETE FROM table1 WHERE value is null");

                // Эта строчка будет продублирована, потому что состояние сохранилось после 3 трех транзакций в кликхаус
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table1 SET id=22,value=43 WHERE value=2");

                ((LastQueryWatcher) asyncStreamer.getConsumer()).waitForQuery(
                        4,
                        "UPDATE table1 SET id=22,value=43 WHERE value=2",
                        TIMEOUT
                );

                // Эти строчки будут отсутствовать в clickhouse, потому что не меняют ни одной записи в mysql
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table1 SET id=22,value=43 WHERE value=2");
                MySQLUtils.executeUpdate(mysqlConn, "BEGIN");
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table1 SET id=11 WHERE value=1");
                MySQLUtils.executeUpdate(mysqlConn, "ROLLBACK");

                // Эти строчки тоже будут в clickhouse в двух экземплярах
                MySQLUtils.executeUpdate(mysqlConn, "BEGIN");
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table1 SET id=11 WHERE value=1");
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table1 SET id=12 WHERE value=1");
                MySQLUtils.executeUpdate(mysqlConn, "COMMIT");

                ((LastQueryWatcher) asyncStreamer.getConsumer()).waitForQuery(
                        5,
                        "UPDATE table1 SET id=12 WHERE value=1",
                        TIMEOUT
                );

                // Имитируем факап, когда не все обработанные транзакции были записаны в кликхаус
                inserter.clear();
            }

            try (
                    BufferedInserter inserter = new BufferedInserter(
                            new ClickHouseInserter(changeLog, queryLog, binlogState),
                            Duration.ofMinutes(5),
                            1000
                    );
                    AsyncStreamer asyncStreamer = new AsyncStreamer(
                            new MySQLBinlogDataStreamer(makeBinlogProvider(mysql),
                                    binlogState.getLastState(source).get()),
                            new LastQueryWatcher(new ClickHouseConsumer(source, inserter)),
                            streamerSchemaReplicator
                    )
            ) {
                ((LastQueryWatcher) asyncStreamer.getConsumer()).waitForQuery(
                        2,
                        "UPDATE table1 SET id=12 WHERE value=1",
                        TIMEOUT
                );
            }

            MySQLUtils.executeUpdate(clickHouseConn, "OPTIMIZE TABLE " + changeLog.getQuotedFullTableName());
            Assert.assertEquals(EXPECTED_CHANGELOG, changeLog
                    .select(
                            "SELECT * FROM " + changeLog.getQuotedFullTableName() +
                                    " ORDER BY toUInt64(replaceRegexpOne(gtid, '^.*:', '')), query_seq_num, " +
                                    "change_seq_num")
                    .collect(Collectors.toList())
            );

            MySQLUtils.executeUpdate(clickHouseConn, "OPTIMIZE TABLE " + queryLog.getQuotedFullTableName());
            Assert.assertEquals(EXPECTED_QUERYLOG, queryLog
                    .select("SELECT * FROM " + queryLog.getQuotedFullTableName() +
                            " ORDER BY toUInt64(replaceRegexpOne(gtid, '^.*:', '')), query_seq_num")
                    .collect(Collectors.toList())
            );
        }
    }

    @Test
    public void testSaveState() throws SQLException, IOException, InterruptedException {
        try (
                Connection clickHouseConn = getClickhouse().connect(CONNECT_TIMEOUT);
                Connection mysqlConn = MYSQL_WITH_BINLOG1.getDb().connect(CONNECT_TIMEOUT);
        ) {
            createClickHouseDb(TEST_DB_NAME, clickHouseConn);
            BinlogStateTable binlogState = new BinlogStateSchema(TEST_DB_NAME, STATE_TABLE).connect(clickHouseConn);

            MySQLBinlogState state0 = MySQLBinlogState.snapshot(mysqlConn);
            binlogState.saveState("source1", state0, LocalDateTime.of(2000, 1, 1, 0, 0));

            MySQLUtils.executeUpdate(mysqlConn, "create database test1");
            MySQLBinlogState state1 = MySQLBinlogState.snapshot(mysqlConn);
            binlogState.saveState("source1", state1, LocalDateTime.of(2000, 1, 2, 0, 0));

            MySQLUtils.executeUpdate(mysqlConn, "create database test2");
            MySQLBinlogState state2 = MySQLBinlogState.snapshot(mysqlConn);
            binlogState.saveState("source2", state2, LocalDateTime.of(2000, 1, 3, 0, 0));

            Assert.assertNotEquals(state0, state1);
            Assert.assertNotEquals(state1, state2);
            Assert.assertNotEquals(state0, binlogState.getLastState("source1").get());
            Assert.assertEquals(state1, binlogState.getLastState("source1").get());
            Assert.assertEquals(state2, binlogState.getLastState("source2").get());
        }
    }

    @Test
    public void testMultiMySQL() throws SQLException, InterruptedException, IOException {
        TmpMySQLServerWithDataDir mysql1 = MYSQL_WITH_BINLOG1.getDb();
        TmpMySQLServerWithDataDir mysql2 = MYSQL_WITH_BINLOG2.getDb();
        try (
                Connection clickHouseConn = getClickhouse().connect(CONNECT_TIMEOUT);
                Connection mysqlConn1 = mysql1.connect(CONNECT_TIMEOUT);
                Connection mysqlConn2 = mysql2.connect(CONNECT_TIMEOUT)
        ) {
            initMySQLTestDb(mysqlConn1);
            initMySQLTestDb(mysqlConn2);

            createClickHouseDb(TEST_DB_NAME, clickHouseConn);
            DbChangeLog changeLog = new DbChangeLogSchema(TEST_DB_NAME, ROWS_TABLE).connect(clickHouseConn);
            QueryLog queryLog = new QueryLogSchema(TEST_DB_NAME, QUERIES_TABLE).connect(clickHouseConn);
            BinlogStateTable binlogState = new BinlogStateSchema(TEST_DB_NAME, STATE_TABLE).connect(clickHouseConn);

            try (
                    DbChangeLogWriter writer = new DbChangeLogWriterBuilder(
                            new ClickHouseInserter(changeLog, queryLog, binlogState),
                            binlogState,
                            Arrays.asList(
                                    new NamedBinlogProvider("shard1", makeBinlogProvider(mysql1), MYSQL1.getDb()),
                                    new NamedBinlogProvider("shard2", makeBinlogProvider(mysql2), MYSQL2.getDb())
                            ),
                            new BinlogStateUnsafeSnapshotter()
                    ).build()
            ) {
                mysqlConn1.setCatalog(TEST_DB_NAME);
                mysqlConn2.setCatalog(TEST_DB_NAME);

                MySQLUtils.executeUpdate(mysqlConn1, "INSERT INTO table1 VALUES (10, 10)");
                MySQLUtils.executeUpdate(mysqlConn2, "INSERT INTO table1 VALUES (20, 20)");

                waitTransactionsCount(writer, TIMEOUT, 2);
            }

            Assert.assertEquals(
                    Arrays.asList(
                            new TestDbChangeLogRecord(
                                    DirectTraceInfo.empty(), "shard1", TEST_DB_NAME, "table1", Operation.INSERT, 0, 0,
                                    new FieldValueList(new FieldValue<>("id", "10")),
                                    new FieldValueList(new FieldValue<>("value", "10"))
                            ),
                            new TestDbChangeLogRecord(
                                    DirectTraceInfo.empty(), "shard2", TEST_DB_NAME, "table1", Operation.INSERT, 0, 0,
                                    new FieldValueList(new FieldValue<>("id", "20")),
                                    new FieldValueList(new FieldValue<>("value", "20"))
                            )
                    ),
                    changeLog
                            .select("SELECT * FROM " + changeLog.getQuotedFullTableName() + " ORDER BY primary_key")
                            .collect(Collectors.toList())
            );

            Assert.assertEquals(
                    Arrays.asList(
                            new TestQueryLogRecord(DirectTraceInfo.empty(), "shard1", 0, "INSERT INTO table1 VALUES " +
                                    "(10, 10)"),
                            new TestQueryLogRecord(DirectTraceInfo.empty(), "shard2", 0, "INSERT INTO table1 VALUES " +
                                    "(20, 20)")
                    ),
                    queryLog
                            .select("SELECT * FROM " + queryLog.getQuotedFullTableName() + " ORDER BY query")
                            .collect(Collectors.toList())
            );
        }
    }

    @Test
    public void testSnapshotNoDDL() throws SQLException, IOException {
        TmpMySQLServerWithDataDir mysql = MYSQL_WITH_BINLOG1.getDb();
        try (Connection mysqlConn = mysql.connect(CONNECT_TIMEOUT)) {
            initMySQLTestDb(mysqlConn);
            AccumulatingInserter inserter = new AccumulatingInserter();

            try (
                    DbCleaner ignored = new DbCleaner(MYSQL1.getDb().connect(CONNECT_TIMEOUT));
                    DbChangeLogWriter writer = new DbChangeLogWriterBuilder(
                            inserter, inserter,
                            Collections.singletonList(
                                    new NamedBinlogProvider("shard1", makeBinlogProvider(mysql), MYSQL1.getDb())
                            ),
                            new BinlogStateUnsafeSnapshotter()
                    ).build()
            ) {
                mysqlConn.setCatalog(TEST_DB_NAME);
                MySQLUtils.executeUpdate(mysqlConn, "INSERT INTO table1 VALUES (30, 30)");
                waitTransactionsCount(writer, TIMEOUT, 1);
            }

            // Разбито на два врайтера, чтобы проверить, что предыдущий запрос не прочитается второй раз.
            try (
                    DbChangeLogWriter writer = new DbChangeLogWriterBuilder(
                            inserter, inserter,
                            Collections.singletonList(
                                    new NamedBinlogProvider("shard1", makeBinlogProvider(mysql), MYSQL1.getDb())
                            ),
                            new BinlogStateUnsafeSnapshotter()
                    ).build()
            ) {
                mysqlConn.setCatalog(TEST_DB_NAME);
                MySQLUtils.executeUpdate(mysqlConn, "INSERT INTO table1 VALUES (40, 40)");
                waitTransactionsCount(writer, TIMEOUT, 1);
            }

            Assert.assertEquals(
                    Arrays.asList(
                            new TestQueryLogRecord(DirectTraceInfo.empty(), "shard1", 0, "INSERT INTO table1 VALUES " +
                                    "(30, 30)"),
                            new TestQueryLogRecord(DirectTraceInfo.empty(), "shard1", 0, "INSERT INTO table1 VALUES " +
                                    "(40, 40)")
                    ),
                    inserter.getBatch()
                            .getTransactions()
                            .stream()
                            .flatMap(tx -> tx.getQueries().stream())
                            .collect(Collectors.toList())
            );
        }
    }

    @Test
    public void testSnapshotBreakingDDLWithRetry() throws SQLException, IOException {
        TmpMySQLServerWithDataDir mysql = MYSQL_WITH_BINLOG1.getDb();
        try (Connection mysqlConn = mysql.connect(CONNECT_TIMEOUT)) {
            initMySQLTestDb(mysqlConn);
            mysqlConn.setCatalog(TEST_DB_NAME);

            AccumulatingInserter inserter = new AccumulatingInserter();

            try (
                    DbChangeLogWriter writer = new DbChangeLogWriterBuilder(
                            inserter, inserter,
                            Collections.singletonList(
                                    new NamedBinlogProvider("shard1", makeBinlogProvider(mysql), MYSQL1.getDb())
                            ),
                            new BinlogStateOptimisticSnapshotter(
                                    SERVER_ID, 2,
                                    new IntrusiveSnapshotter(
                                            new BinlogStateUnsafeSnapshotter(),
                                            () -> MySQLUtils.executeUpdate(mysqlConn, "CREATE TABLE table2 (id int)"),
                                            () -> {
                                            },
                                            () -> {
                                            }
                                    )
                            )
                    ).build()
            ) {
                MySQLUtils.executeUpdate(mysqlConn, "INSERT INTO table1 VALUES (50, 50)");
                waitTransactionsCount(writer, TIMEOUT, 1);
            }
        }
    }

    @Test
    public void testSnapshotBreakingDDL() throws SQLException, IOException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Schema changed while we read it");

        TmpMySQLServerWithDataDir mysql = MYSQL_WITH_BINLOG1.getDb();
        try (Connection mysqlConn = mysql.connect(CONNECT_TIMEOUT)) {
            initMySQLTestDb(mysqlConn);
            mysqlConn.setCatalog(TEST_DB_NAME);

            AccumulatingInserter inserter = new AccumulatingInserter();

            try (
                    DbChangeLogWriter writer = new DbChangeLogWriterBuilder(
                            inserter, inserter,
                            Collections.singletonList(
                                    new NamedBinlogProvider("shard1", makeBinlogProvider(mysql), MYSQL1.getDb())
                            ),
                            new BinlogStateOptimisticSnapshotter(
                                    SERVER_ID, 2,
                                    new IntrusiveSnapshotter(
                                            new BinlogStateUnsafeSnapshotter(),
                                            () -> MySQLUtils.executeUpdate(mysqlConn, "CREATE TABLE table3 (id int)"),
                                            () -> MySQLUtils.executeUpdate(mysqlConn, "CREATE TABLE table4 (id int)"),
                                            () -> MySQLUtils.executeUpdate(mysqlConn, "CREATE TABLE table5 (id int)"),
                                            () -> {
                                            }
                                    )
                            )
                    ).build()
            ) {
                MySQLUtils.executeUpdate(mysqlConn, "INSERT INTO table1 VALUES (50, 50)");
                waitTransactionsCount(writer, TIMEOUT, 1);
            }
        }
    }

    @Test
    public void testSnapshotNonBreakingDDL() throws SQLException, IOException, InterruptedException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Failed to make snapshot in 1 tries");

        TmpMySQLServerWithDataDir mysql = MYSQL_WITH_BINLOG1.getDb();
        try (Connection mysqlConn = mysql.connect(CONNECT_TIMEOUT)) {
            initMySQLTestDb(mysqlConn);
            mysqlConn.setCatalog(TEST_DB_NAME);
            AccumulatingInserter inserter = new AccumulatingInserter();

            try (
                    DbChangeLogWriter ignored = new DbChangeLogWriterBuilder(
                            inserter, inserter,
                            Collections.singletonList(
                                    new NamedBinlogProvider("shard1", makeBinlogProvider(mysql), MYSQL1.getDb())
                            ),
                            new BinlogStateOptimisticSnapshotter(
                                    SERVER_ID, 1,
                                    new IntrusiveSnapshotter(
                                            new BinlogStateUnsafeSnapshotter(),
                                            () -> {
                                                MySQLUtils.executeUpdate(mysqlConn, "CREATE TABLE table2 (id int)");
                                                MySQLUtils.executeUpdate(mysqlConn, "DROP TABLE table2");
                                            },
                                            () -> {
                                            }
                                    )
                            )
                    ).build()
            ) {
                Thread.sleep(1000);
            }
        }
    }

    @Test
    public void testSnapshotDML() throws SQLException, IOException {
        TmpMySQLServerWithDataDir mysql = MYSQL_WITH_BINLOG1.getDb();
        try (Connection mysqlConn = mysql.connect(CONNECT_TIMEOUT)) {
            initMySQLTestDb(mysqlConn);
            mysqlConn.setCatalog(TEST_DB_NAME);
            AccumulatingInserter inserter = new AccumulatingInserter();

            try (
                    DbChangeLogWriter writer = new DbChangeLogWriterBuilder(
                            inserter, inserter,
                            Collections.singletonList(
                                    new NamedBinlogProvider("shard1", makeBinlogProvider(mysql), MYSQL1.getDb())
                            ),
                            new BinlogStateOptimisticSnapshotter(
                                    SERVER_ID, 1,
                                    new IntrusiveSnapshotter(
                                            new BinlogStateUnsafeSnapshotter(),
                                            () -> MySQLUtils.executeUpdate(mysqlConn, "INSERT INTO table1 VALUES (60," +
                                                    " 60)"),
                                            () -> {
                                                // Второй запрос нужен только для того, чтобы продвинуть бинлог на одну
                                                // транзакцию вперед, иначе снепшоттер не сможет понять, что прочитал
                                                // все
                                                // транзакции в опасном диапазоне.
                                                MySQLUtils.executeUpdate(mysqlConn, "INSERT INTO table1 VALUES (61, " +
                                                        "61)");
                                            }
                                    )
                            )
                    ).build()
            ) {
                MySQLUtils.executeUpdate(mysqlConn, "INSERT INTO table1 VALUES (70, 70)");
                waitTransactionsCount(writer, TIMEOUT, 2);
            }

            Assert.assertEquals(
                    Arrays.asList(
                            new TestQueryLogRecord(DirectTraceInfo.empty(), "shard1", 0, "INSERT INTO table1 VALUES " +
                                    "(61, 61)"),
                            new TestQueryLogRecord(DirectTraceInfo.empty(), "shard1", 0, "INSERT INTO table1 VALUES " +
                                    "(70, 70)")
                    ),
                    inserter.getBatch()
                            .getTransactions()
                            .stream()
                            .flatMap(tx -> tx.getQueries().stream())
                            .collect(Collectors.toList())
            );
        }
    }

    @Test
    public void testSnapshotDDLAfterSnapshot() throws SQLException, IOException {
        TmpMySQLServerWithDataDir mysql = MYSQL_WITH_BINLOG1.getDb();
        try (Connection mysqlConn = mysql.connect(CONNECT_TIMEOUT)) {
            initMySQLTestDb(mysqlConn);
            AccumulatingInserter inserter = new AccumulatingInserter();

            try (
                    DbChangeLogWriter writer = new DbChangeLogWriterBuilder(
                            inserter, inserter,
                            Collections.singletonList(
                                    new NamedBinlogProvider("shard1", makeBinlogProvider(mysql), MYSQL1.getDb())
                            ),
                            new BinlogStateUnsafeSnapshotter()
                    ).build()
            ) {
                mysqlConn.setCatalog(TEST_DB_NAME);
                MySQLUtils.executeUpdate(mysqlConn, "CREATE TABLE table2 (id int)");
                MySQLUtils.executeUpdate(mysqlConn, "INSERT INTO table1 VALUES (70, 70)");
                waitTransactionsCount(writer, TIMEOUT, 1);
            }
        }
    }

    /*
     Проверяет, что в лог записываются лишь те поля, которые изменились в оригинальном запросе.
     */
    @Test
    public void testCompactDML() throws SQLException, IOException {
        TmpMySQLServerWithDataDir mysql = MYSQL_WITH_BINLOG1.getDb();
        TmpMySQLServerWithDataDir streamerSchemaReplicator = MYSQL1.getDb();
        try (
                Connection mysqlConn = mysql.connect(CONNECT_TIMEOUT)
        ) {
            assumeTrue(mysqlConn.getAutoCommit());

            MySQLUtils.executeUpdate(mysqlConn, "CREATE DATABASE " + MySQLUtils.quoteName(TEST_DB_NAME));
            mysqlConn.setCatalog(TEST_DB_NAME);
            MySQLUtils.executeUpdate(
                    mysqlConn,
                    "CREATE TABLE table2 (id int not null primary key auto_increment, value1 int, value2 int, value3 " +
                            "int)"
            );

            AccumulatingInserter inserter = new AccumulatingInserter();
            LastQueryWatcher queryWatcher = new LastQueryWatcher(new ClickHouseConsumer("src", inserter));

            try (
                    DbCleaner ignored = new DbCleaner(streamerSchemaReplicator.connect(CONNECT_TIMEOUT));
                    AsyncStreamer ignored2 = new AsyncStreamer(
                            new MySQLBinlogDataStreamer(makeBinlogProvider(mysql),
                                    MySQLBinlogState.snapshot(mysqlConn)),
                            queryWatcher,
                            streamerSchemaReplicator
                    )
            ) {
                // После вызова MySQLBinlogState.shapshot, mysqlConn может указывать на другую базу.
                // TODO: это баг или фича?
                mysqlConn.setCatalog(TEST_DB_NAME);

                int transactions = 0;
                MySQLUtils.executeUpdate(mysqlConn, "INSERT INTO table2 (value1, value2, value3) VALUES (1, 1, 1)");
                ++transactions;

                // Изменилось 3 поля из 3
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table2 SET value1 = 2, value2 = 2, value3 = 2 WHERE id = " +
                        "1");
                ++transactions;

                // Изменилось 2 поля из 3
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table2 SET value1 = 3, value2 = 3, value3 = 2 WHERE id = " +
                        "1");
                ++transactions;

                // Изменилось 1 поле из 3
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table2 SET value1 = 4, value2 = 3, value3 = 2 WHERE id = " +
                        "1");
                ++transactions;

                // Изменилось 0 полей из 3
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table2 SET value1 = 4, value2 = 3, value3 = 2 WHERE id = " +
                        "1");
                // Ничего не изменилось - счётчик транзакций не инкрементируется

                // Изменилось 1 поле из 3, а также primary key
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table2 SET id = 2, value1 = 5, value2 = 3, value3 = 2 " +
                        "WHERE id = 1");
                ++transactions;

                // Изменилось 0 полей из 3, но также изменился primary key
                MySQLUtils.executeUpdate(mysqlConn, "UPDATE table2 SET id = 3 WHERE id = 2");
                ++transactions;

                MySQLUtils.executeUpdate(mysqlConn, "DELETE FROM table2 WHERE id = 3");
                ++transactions;

                queryWatcher.waitForQuery(transactions, "DELETE FROM table2 WHERE id = 3", TIMEOUT);
            }

            DirectTraceInfo traceInfo = DirectTraceInfo.empty();
            Assert.assertEquals(
                    Arrays.asList(
                            new TestDbChangeLogRecord(
                                    traceInfo, "src", TEST_DB_NAME, "table2", Operation.INSERT, 0, 0,
                                    new FieldValueList(new FieldValue<>("id", "1")),
                                    new FieldValueList(
                                            new FieldValue<>("value1", "1"),
                                            new FieldValue<>("value2", "1"),
                                            new FieldValue<>("value3", "1")
                                    )
                            ),

                            // Изменилось 3 поля из 3
                            new TestDbChangeLogRecord(
                                    traceInfo, "src", TEST_DB_NAME, "table2", Operation.UPDATE, 0, 0,
                                    new FieldValueList(new FieldValue<>("id", "1")),
                                    new FieldValueList(
                                            new FieldValue<>("value1", "2"),
                                            new FieldValue<>("value2", "2"),
                                            new FieldValue<>("value3", "2")
                                    )
                            ),

                            // Изменилось 2 поля из 3
                            new TestDbChangeLogRecord(
                                    traceInfo, "src", TEST_DB_NAME, "table2", Operation.UPDATE, 0, 0,
                                    new FieldValueList(new FieldValue<>("id", "1")),
                                    new FieldValueList(
                                            new FieldValue<>("value1", "3"),
                                            new FieldValue<>("value2", "3")
                                    )
                            ),

                            // Изменилось 1 поле из 3
                            new TestDbChangeLogRecord(
                                    traceInfo, "src", TEST_DB_NAME, "table2", Operation.UPDATE, 0, 0,
                                    new FieldValueList(new FieldValue<>("id", "1")),
                                    new FieldValueList(
                                            new FieldValue<>("value1", "4")
                                    )
                            ),

                            // Изменилось 0 полей из 3
                            // Нет записи в changelog

                            // Изменилось 1 поле из 3, а также primary key
                            new TestDbChangeLogRecord(
                                    traceInfo, "src", TEST_DB_NAME, "table2", Operation.UPDATE, 0, 0,
                                    new FieldValueList(new FieldValue<>("id", "1")),
                                    new FieldValueList(
                                            new FieldValue<>("id", "2"),
                                            new FieldValue<>("value1", "5")
                                    )
                            ),

                            // Изменилось 0 полей из 3, но изменился primary key
                            new TestDbChangeLogRecord(
                                    traceInfo, "src", TEST_DB_NAME, "table2", Operation.UPDATE, 0, 0,
                                    new FieldValueList(new FieldValue<>("id", "2")),
                                    new FieldValueList(
                                            new FieldValue<>("id", "3")
                                    )
                            ),

                            // При удалении записывается только primary key
                            new TestDbChangeLogRecord(
                                    traceInfo, "src", TEST_DB_NAME, "table2", Operation.DELETE, 0, 0,
                                    new FieldValueList(new FieldValue<>("id", "3")),
                                    FieldValueList.empty()
                            )
                    ),
                    inserter.getBatch().getTransactions().stream()
                            .flatMap(transaction -> transaction.getChanges().stream())
                            .collect(Collectors.toList())
            );
        }
    }
}
