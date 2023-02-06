package ru.yandex.direct.binlogbroker.logbrokerwriter.components;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.shyiko.mysql.binlog.GtidSet;
import com.google.common.base.Preconditions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.ColumnType;
import ru.yandex.direct.binlog.model.CreateOrModifyColumn;
import ru.yandex.direct.binlog.model.CreateTable;
import ru.yandex.direct.binlog.model.DropColumn;
import ru.yandex.direct.binlog.model.DropTable;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.binlog.model.RenameTable;
import ru.yandex.direct.binlog.model.Truncate;
import ru.yandex.direct.binlogbroker.logbroker_utils.models.SourceType;
import ru.yandex.direct.binlogbroker.logbroker_utils.writer.LogBrokerWriterException;
import ru.yandex.direct.binlogbroker.logbroker_utils.writer.LogbrokerWriter;
import ru.yandex.direct.binlogbroker.logbrokerwriter.models.BinlogWithSeqId;
import ru.yandex.direct.binlogbroker.logbrokerwriter.models.ImmutableSourceState;
import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.mysql.JunitRuleMySQLServerCreator;
import ru.yandex.direct.mysql.MySQLBinlogState;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.mysql.MySQLUtils;
import ru.yandex.direct.mysql.TmpMySQLServerWithDataDir;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.tracing.TraceHelper;
import ru.yandex.direct.tracing.TraceLogger;

import static org.mockito.Mockito.mock;
import static ru.yandex.direct.env.EnvironmentType.DEVTEST;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
public class BinlogbrokerTest {
    private static final SourceType SOURCE_TYPE = SourceType.fromType(DEVTEST, "ppc:1");

    private static final MySQLServerBuilder MYSQL_SERVER_BUILDER =
            new DirectMysqlDb(TestMysqlConfig.directConfig())
                    .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                            .setGracefulStopTimeout(Duration.ZERO)
                            .setServerId(54321)
                            .withNoSync(true));
    private static final LocalDateTime STUB_TIME = LocalDateTime.of(2018, 6, 10, 17, 55);

    @ClassRule
    public static JunitRuleMySQLServerCreator mysqlServerCreator = new JunitRuleMySQLServerCreator();

    private static TmpMySQLServerWithDataDir mysql;
    private static DbConfig dbConfig;
    private static String serverUuid;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private Thread binlogbrokerThread;
    private String dbName;
    private MemoryStateRepository stateRepository;
    private BlockingQueue<List<BinlogWithSeqId>> queue;
    private AtomicReference<Throwable> exceptionHolder;
    private Binlogbroker.Builder binlogbrokerBuilder;

    @BeforeClass
    public static void setUpClass() throws InterruptedException {
        mysql = mysqlServerCreator.createWithBinlog(SOURCE_TYPE.getSourceName(), MYSQL_SERVER_BUILDER.copy());
        dbConfig = makeDbConfig(mysql);
    }

    @Nonnull
    private static DbConfig makeDbConfig(TmpMySQLServerWithDataDir mysql) {
        DbConfig cfg = new DbConfig();
        cfg.setConnectTimeout(1.0);
        cfg.setDbName(SOURCE_TYPE.getDbName());
        cfg.setEngine(DbConfig.Engine.MYSQL);
        cfg.setHosts(List.of(mysql.getHost()));
        cfg.setPass(mysql.getPassword());
        cfg.setPort(mysql.getPort());
        cfg.setUser(mysql.getUsername());
        return cfg;
    }

    private List<BinlogWithSeqId> fetchAllEvents(int expectedSize) throws InterruptedException {
        List<BinlogWithSeqId> result = new ArrayList<>();
        while (result.size() < expectedSize) {
            List<BinlogWithSeqId> events = null;
            for (int i = 0; i < 60 && events == null; ++i) {
                events = queue.poll(1, TimeUnit.SECONDS);
                checkExceptionHolder();
            }
            if (events == null) {
                do {
                    result.add(null);
                } while (result.size() < expectedSize);
                break;
            } else {
                events.forEach(e -> e.event.withUtcTimestamp(STUB_TIME));
                result.addAll(events);
            }
        }
        return result;
    }

    private List<List<BinlogWithSeqId>> fetchAllEventLists(int expectedSize) throws InterruptedException {
        List<List<BinlogWithSeqId>> result = new ArrayList<>();
        while (result.size() < expectedSize) {
            List<BinlogWithSeqId> events = null;
            for (int i = 0; i < 60 && events == null; ++i) {
                events = queue.poll(60, TimeUnit.SECONDS);
                checkExceptionHolder();
            }
            if (events == null) {
                do {
                    result.add(null);
                } while (result.size() < expectedSize);
                break;
            } else {
                events.forEach(e -> e.event.withUtcTimestamp(STUB_TIME));
                result.add(events);
            }
        }
        return result;
    }

    @Before
    public void setUp() throws SQLException {
        dbName = TestUtils.randomName("test_db_", 14);
        queue = new ArrayBlockingQueue<>(100);
        stateRepository = new MemoryStateRepository();
        exceptionHolder = new AtomicReference<>();

        dbConfig.setDb(dbName);

        mysql.awaitConnectivity(Duration.ofSeconds(30));
        try (Connection conn = mysql.connect()) {
            MySQLUtils.executeUpdate(conn, "create database " + dbName);
            try (Statement statement = conn.createStatement()) {
                ResultSet resultSet = statement.executeQuery("select @@server_uuid");
                assumeThat(resultSet.next(), Matchers.is(true));
                serverUuid = resultSet.getString(1);
            }
        }

        binlogbrokerBuilder = Binlogbroker.builder()
                .withSource(SOURCE_TYPE)
                .withSourceDbConfigSupplier(() -> dbConfig)
                .withBinlogEventConsumer(new LogbrokerWriter<BinlogWithSeqId>() {
                    @Override
                    public CompletableFuture<Integer> write(List<BinlogWithSeqId> eventsWithSeqId)
                            throws LogBrokerWriterException {
                        queue.add(eventsWithSeqId);
                        return CompletableFuture.completedFuture(0);
                    }

                    @Override
                    public Long getInitialMaxSeqNo() {
                        return 0L;  // Unused stub
                    }

                    @Override
                    public void close() {
                    }
                })
                .withSourceStateRepository(stateRepository)
                .withLogbrokerWriterMonitoring(mock(LogbrokerWriterMonitoring.class))
                .withMysqlServerBuilder(MYSQL_SERVER_BUILDER.copy())
                .withInitialServerId(null)
                .withKeepAliveTimeout(Duration.ofSeconds(30))
                .withMaxBufferedEvents(100)
                .withConsumerChunkSize(1)
                .withMaxEventsPerTransaction(30)
                .withTraceHelper(new TraceHelper("", mock(TraceLogger.class)))
                .withConsumerChunkDuration(Duration.ofDays(999))
                .withFlushEventsTimeout(Duration.ofSeconds(60));
    }

    private void startBinlogbroker() {
        Preconditions.checkState(binlogbrokerThread == null);
        binlogbrokerThread = new Thread(binlogbrokerBuilder.build());
        binlogbrokerThread.setUncaughtExceptionHandler((t, e) -> exceptionHolder.set(e));
        binlogbrokerThread.setDaemon(true);
        binlogbrokerThread.start();
    }

    @After
    public void tearDown() throws SQLException, InterruptedException {
        try {
            checkExceptionHolder();
        } finally {
            if (binlogbrokerThread != null) {
                binlogbrokerThread.interrupt();
            }
            try (Connection conn = mysql.connect()) {
                MySQLUtils.executeUpdate(conn, "drop database if exists " + dbName);
            } finally {
                if (binlogbrokerThread != null) {
                    binlogbrokerThread.join(2_000);
                }
            }
        }
    }

    private void checkExceptionHolder() {
        Throwable e = exceptionHolder.get();
        if (e != null && !(e instanceof InterruptedException)) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Поверхностный функциональный тест, который запускает настоящий mysqld и проверяет общую работоспособность
     * Binlogbroker.
     * <p>
     * Тестирует INSERT, UPDATE, DELETE.
     */
    @Test
    public void binlogReaderCrud() throws InterruptedException, SQLException {
        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);
            MySQLUtils.executeUpdate(conn,
                    "create table example (id int primary key, value text, weight bigint null)");
            MySQLBinlogState mySQLBinlogState = MySQLBinlogState.snapshot(conn);
            stateRepository.saveState(SOURCE_TYPE, new ImmutableSourceState(0, 0, mySQLBinlogState.getGtidSet(), 0,
                    mySQLBinlogState.getSerializedServerSchema()));
        }
        GtidSet startingGtidSet =
                new GtidSet(Objects.requireNonNull(stateRepository.loadState(SOURCE_TYPE).getGtid()));
        long startingTransactionId = startingGtidSet.getUUIDSets().iterator().next().getIntervals().get(0).getEnd();
        startBinlogbroker();

        List<BinlogWithSeqId> expectedEvents = new ArrayList<>();

        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);

            MySQLUtils.executeUpdate(conn, "begin");

            MySQLUtils.executeUpdate(conn,
                    "insert into example (id, value, weight) values (1, 'one', 1234), (2, 'two', 5678)");
            expectedEvents.add(new BinlogWithSeqId(1, new BinlogEvent()
                    .withDb(dbName)
                    .withOperation(Operation.INSERT)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withRows(List.of(
                            new BinlogEvent.Row()
                                    .withAfter(Map.of(
                                            "id", 1L,
                                            "value", "one",
                                            "weight", 1234L))
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 1L))
                                    .withRowIndex(0),
                            new BinlogEvent.Row()
                                    .withAfter(Map.of(
                                            "id", 2L,
                                            "value", "two",
                                            "weight", 5678L))
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 2L))
                                    .withRowIndex(1)))
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("example")
                    .withTransactionId(startingTransactionId + 1)
                    .withUtcTimestamp(STUB_TIME)));

            MySQLUtils.executeUpdate(conn,
                    "insert into example (id, value, weight) values (3, 'three', 9012)");
            expectedEvents.add(new BinlogWithSeqId(2, new BinlogEvent()
                    .withDb(dbName)
                    .withOperation(Operation.INSERT)
                    .withQueryIndex(1)
                    .withEventIndex(1)
                    .withWriteQuery(true)
                    .withRows(List.of(
                            new BinlogEvent.Row()
                                    .withAfter(Map.of(
                                            "id", 3L,
                                            "value", "three",
                                            "weight", 9012L))
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 3L))
                                    .withRowIndex(0)))
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("example")
                    .withTransactionId(startingTransactionId + 1)
                    .withUtcTimestamp(STUB_TIME)));

            MySQLUtils.executeUpdate(conn, "commit");

            MySQLUtils.executeUpdate(conn, "begin");
            MySQLUtils.executeUpdate(conn, "update example set weight = null where id in (1, 3)");
            expectedEvents.add(new BinlogWithSeqId(3, new BinlogEvent()
                    .withDb(dbName)
                    .withOperation(Operation.UPDATE)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withRows(List.of(
                            new BinlogEvent.Row()
                                    .withAfter(mapWithNullValue("weight"))
                                    .withBefore(Map.of(
                                            "weight", 1234L))
                                    .withPrimaryKey(Map.of(
                                            "id", 1L))
                                    .withRowIndex(0),
                            new BinlogEvent.Row()
                                    .withAfter(mapWithNullValue("weight"))
                                    .withBefore(Map.of(
                                            "weight", 9012L))
                                    .withPrimaryKey(Map.of(
                                            "id", 3L))
                                    .withRowIndex(1)))
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("example")
                    .withTransactionId(startingTransactionId + 2)
                    .withUtcTimestamp(STUB_TIME)));
            MySQLUtils.executeUpdate(conn, "commit");

            MySQLUtils.executeUpdate(conn, "begin");
            MySQLUtils.executeUpdate(conn, "delete from example");
            expectedEvents.add(new BinlogWithSeqId(4, new BinlogEvent()
                    .withDb(dbName)
                    .withOperation(Operation.DELETE)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withRows(List.of(
                            new BinlogEvent.Row()
                                    .withAfter(Map.of())
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 1L))
                                    .withRowIndex(0),
                            new BinlogEvent.Row()
                                    .withAfter(Map.of())
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 2L))
                                    .withRowIndex(1),
                            new BinlogEvent.Row()
                                    .withAfter(Map.of())
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 3L))
                                    .withRowIndex(2)))
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("example")
                    .withTransactionId(startingTransactionId + 3)
                    .withUtcTimestamp(STUB_TIME)));
            MySQLUtils.executeUpdate(conn, "commit");
        }

        List<BinlogWithSeqId> result = fetchAllEvents(expectedEvents.size());
        softly.assertThat(result).isEqualTo(expectedEvents);
    }

    /**
     * Поверхностный функциональный тест, который запускает настоящий mysqld и проверяет общую работоспособность
     * Binlogbroker.
     * <p>
     * Тестирует INSERT, UPDATE, DELETE.
     */
    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void binlogReaderCrudWithTraceInfo() throws InterruptedException, SQLException {
        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);
            MySQLUtils.executeUpdate(conn,
                    "create table example (id int primary key, value text, weight bigint null)");
            MySQLBinlogState mySQLBinlogState = MySQLBinlogState.snapshot(conn);
            stateRepository.saveState(SOURCE_TYPE, new ImmutableSourceState(0, 0, mySQLBinlogState.getGtidSet(), 0,
                    mySQLBinlogState.getSerializedServerSchema()));
        }
        GtidSet startingGtidSet =
                new GtidSet(Objects.requireNonNull(stateRepository.loadState(SOURCE_TYPE).getGtid()));
        long startingTransactionId = startingGtidSet.getUUIDSets().iterator().next().getIntervals().get(0).getEnd();
        startBinlogbroker();

        List<BinlogWithSeqId> expectedEvents = new ArrayList<>();

        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);

            MySQLUtils.executeUpdate(conn, "begin");

            MySQLUtils.executeUpdate(conn,
                    "insert /*reqid:1:service2:method3:operator=4:resharding=1*/ into example (id, value, weight)" +
                            " values (1, 'one', 1234), (2, 'two', 5678)");
            expectedEvents.add(new BinlogWithSeqId(1, new BinlogEvent()
                    .withDb(dbName)
                    .withOperation(Operation.INSERT)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withRows(List.of(
                            new BinlogEvent.Row()
                                    .withAfter(Map.of(
                                            "id", 1L,
                                            "value", "one",
                                            "weight", 1234L))
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 1L))
                                    .withRowIndex(0),
                            new BinlogEvent.Row()
                                    .withAfter(Map.of(
                                            "id", 2L,
                                            "value", "two",
                                            "weight", 5678L))
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 2L))
                                    .withRowIndex(1)))
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("example")
                    .withTransactionId(startingTransactionId + 1)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(1L)
                    .withTraceInfoService("service2")
                    .withTraceInfoMethod("method3")
                    .withTraceInfoOperatorUid(4L)
                    .withResharding(true)
            ));

            MySQLUtils.executeUpdate(conn,
                    "insert /*reqid:5:service6:method7:operator=8:ess=user1*/ into example (id, value, weight)" +
                            " values (3, 'three', 9012)");
            expectedEvents.add(new BinlogWithSeqId(2, new BinlogEvent()
                    .withDb(dbName)
                    .withOperation(Operation.INSERT)
                    .withQueryIndex(1)
                    .withEventIndex(1)
                    .withWriteQuery(true)
                    .withRows(List.of(
                            new BinlogEvent.Row()
                                    .withAfter(Map.of(
                                            "id", 3L,
                                            "value", "three",
                                            "weight", 9012L))
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 3L))
                                    .withRowIndex(0)))
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("example")
                    .withTransactionId(startingTransactionId + 1)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(5L)
                    .withTraceInfoService("service6")
                    .withTraceInfoMethod("method7")
                    .withTraceInfoOperatorUid(8L)
                    .withEssTag("user1")
                    .withResharding(false)
            ));

            MySQLUtils.executeUpdate(conn, "commit");

            MySQLUtils.executeUpdate(conn, "begin");
            MySQLUtils.executeUpdate(conn, "update /*reqid:9:service10:method11:operator=12:ess=user2:resharding=1*/ " +
                    "example" +
                    " set weight = null where id in (1, 3)");
            expectedEvents.add(new BinlogWithSeqId(3, new BinlogEvent()
                    .withDb(dbName)
                    .withOperation(Operation.UPDATE)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withRows(List.of(
                            new BinlogEvent.Row()
                                    .withAfter(mapWithNullValue("weight"))
                                    .withBefore(Map.of(
                                            "weight", 1234L))
                                    .withPrimaryKey(Map.of(
                                            "id", 1L))
                                    .withRowIndex(0),
                            new BinlogEvent.Row()
                                    .withAfter(mapWithNullValue("weight"))
                                    .withBefore(Map.of(
                                            "weight", 9012L))
                                    .withPrimaryKey(Map.of(
                                            "id", 3L))
                                    .withRowIndex(1)))
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("example")
                    .withTransactionId(startingTransactionId + 2)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(9L)
                    .withTraceInfoService("service10")
                    .withTraceInfoMethod("method11")
                    .withTraceInfoOperatorUid(12L)
                    .withEssTag("user2")
                    .withResharding(true)
            ));
            MySQLUtils.executeUpdate(conn, "commit");

            MySQLUtils.executeUpdate(conn, "begin");
            MySQLUtils.executeUpdate(conn, "delete /*reqid:13:service14:method15:operator=16:ess=user3*/ from example");
            expectedEvents.add(new BinlogWithSeqId(4, new BinlogEvent()
                    .withDb(dbName)
                    .withOperation(Operation.DELETE)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withRows(List.of(
                            new BinlogEvent.Row()
                                    .withAfter(Map.of())
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 1L))
                                    .withRowIndex(0),
                            new BinlogEvent.Row()
                                    .withAfter(Map.of())
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 2L))
                                    .withRowIndex(1),
                            new BinlogEvent.Row()
                                    .withAfter(Map.of())
                                    .withBefore(Map.of())
                                    .withPrimaryKey(Map.of(
                                            "id", 3L))
                                    .withRowIndex(2)))
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("example")
                    .withTransactionId(startingTransactionId + 3)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(13L)
                    .withTraceInfoService("service14")
                    .withTraceInfoMethod("method15")
                    .withTraceInfoOperatorUid(16L)
                    .withEssTag("user3")
                    .withResharding(false)
            ));
            MySQLUtils.executeUpdate(conn, "commit");
        }

        List<BinlogWithSeqId> result = fetchAllEvents(expectedEvents.size());
        softly.assertThat(result).isEqualTo(expectedEvents);
    }

    @Test
    public void binlogReaderDdl() throws InterruptedException, SQLException {
        try (Connection conn = mysql.connect()) {
            MySQLBinlogState mySQLBinlogState = MySQLBinlogState.snapshot(conn);
            stateRepository.saveState(SOURCE_TYPE, new ImmutableSourceState(0, 0, mySQLBinlogState.getGtidSet(), 0,
                    mySQLBinlogState.getSerializedServerSchema()));
        }
        GtidSet startingGtidSet =
                new GtidSet(Objects.requireNonNull(stateRepository.loadState(SOURCE_TYPE).getGtid()));
        long startingTransactionId = startingGtidSet.getUUIDSets().iterator().next().getIntervals().get(0).getEnd();
        startBinlogbroker();

        List<BinlogWithSeqId> expectedEvents = new ArrayList<>();

        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);

            MySQLUtils.executeUpdate(conn,
                    "create table sometable (a int, b tinyint, c text not null, primary key (a, b))");
            expectedEvents.add(new BinlogWithSeqId(1, new BinlogEvent()
                    .withAddedSchemaChanges(new CreateTable()
                            .withAddedColumns(
                                    new CreateOrModifyColumn()
                                            .withColumnName("a")
                                            .withColumnType(ColumnType.INTEGER)
                                            .withNullable(false),
                                    new CreateOrModifyColumn()
                                            .withColumnName("b")
                                            .withColumnType(ColumnType.INTEGER)
                                            .withNullable(false),
                                    new CreateOrModifyColumn()
                                            .withColumnName("c")
                                            .withColumnType(ColumnType.STRING)
                                            .withNullable(false))
                            .withPrimaryKey(List.of("a", "b")))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 1)
                    .withUtcTimestamp(STUB_TIME)));

            MySQLUtils.executeUpdate(conn, "truncate table sometable");
            expectedEvents.add(new BinlogWithSeqId(2, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new Truncate())
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 2)
                    .withUtcTimestamp(STUB_TIME)));

            MySQLUtils.executeUpdate(conn, "alter table sometable modify column c text null");
            expectedEvents.add(new BinlogWithSeqId(3, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new CreateOrModifyColumn()
                                    .withColumnName("c")
                                    .withColumnType(ColumnType.STRING)
                                    .withNullable(true))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 3)
                    .withUtcTimestamp(STUB_TIME)));

            MySQLUtils.executeUpdate(conn, "alter table sometable add column d float not null");
            expectedEvents.add(new BinlogWithSeqId(4, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new CreateOrModifyColumn()
                                    .withColumnName("d")
                                    .withColumnType(ColumnType.FLOATING_POINT)
                                    .withNullable(false))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 4)
                    .withUtcTimestamp(STUB_TIME)));

            // DIRECT-81584 Переименование колонки оставлено на потом
            // Запрос: alter table sometable change column d e float not null
            // Ожидаемое изменение схемы:
            // new RenameColumn().withOldColumnName("d").withNewColumnName("e")

            MySQLUtils.executeUpdate(conn, "alter table sometable drop column d");
            expectedEvents.add(new BinlogWithSeqId(5, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new DropColumn()
                                    .withColumnName("d"))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 5)
                    .withUtcTimestamp(STUB_TIME)));

            MySQLUtils.executeUpdate(conn, "rename table sometable to sometable2");
            expectedEvents.add(new BinlogWithSeqId(6, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new RenameTable()
                                    .withAddRename("sometable", "sometable2"))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 6)
                    .withUtcTimestamp(STUB_TIME)));

            MySQLUtils.executeUpdate(conn, "drop table sometable2");
            expectedEvents.add(new BinlogWithSeqId(7, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new DropTable("sometable2"))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable2")
                    .withTransactionId(startingTransactionId + 7)
                    .withUtcTimestamp(STUB_TIME)));
        }

        List<BinlogWithSeqId> result = fetchAllEvents(expectedEvents.size());
        softly.assertThat(result).isEqualTo(expectedEvents);
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void binlogReaderDdlWithTraceInfo() throws InterruptedException, SQLException {
        try (Connection conn = mysql.connect()) {
            MySQLBinlogState mySQLBinlogState = MySQLBinlogState.snapshot(conn);
            stateRepository.saveState(SOURCE_TYPE, new ImmutableSourceState(0, 0, mySQLBinlogState.getGtidSet(), 0,
                    mySQLBinlogState.getSerializedServerSchema()));
        }
        GtidSet startingGtidSet =
                new GtidSet(Objects.requireNonNull(stateRepository.loadState(SOURCE_TYPE).getGtid()));
        long startingTransactionId = startingGtidSet.getUUIDSets().iterator().next().getIntervals().get(0).getEnd();
        startBinlogbroker();

        List<BinlogWithSeqId> expectedEvents = new ArrayList<>();

        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);

            MySQLUtils.executeUpdate(conn,
                    "create /*reqid:1:service2:method3:operator=4*/ table sometable" +
                            " (a int, b tinyint, c text not null, primary key (a, b))");
            expectedEvents.add(new BinlogWithSeqId(1, new BinlogEvent()
                    .withAddedSchemaChanges(new CreateTable()
                            .withAddedColumns(
                                    new CreateOrModifyColumn()
                                            .withColumnName("a")
                                            .withColumnType(ColumnType.INTEGER)
                                            .withNullable(false),
                                    new CreateOrModifyColumn()
                                            .withColumnName("b")
                                            .withColumnType(ColumnType.INTEGER)
                                            .withNullable(false),
                                    new CreateOrModifyColumn()
                                            .withColumnName("c")
                                            .withColumnType(ColumnType.STRING)
                                            .withNullable(false))
                            .withPrimaryKey(List.of("a", "b")))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 1)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(1L)
                    .withTraceInfoService("service2")
                    .withTraceInfoMethod("method3")
                    .withTraceInfoOperatorUid(4L)));

            MySQLUtils.executeUpdate(conn, "truncate /*reqid:5:service6:method7:operator=8:ess=user11*/ table " +
                    "sometable");
            expectedEvents.add(new BinlogWithSeqId(2, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new Truncate())
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 2)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(5L)
                    .withTraceInfoService("service6")
                    .withTraceInfoMethod("method7")
                    .withTraceInfoOperatorUid(8L)
                    .withEssTag("user11")));

            MySQLUtils.executeUpdate(conn, "alter /*reqid:9:service10:method11:operator=12:ess=user12*/ table " +
                    "sometable" +
                    " modify column c text null");
            expectedEvents.add(new BinlogWithSeqId(3, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new CreateOrModifyColumn()
                                    .withColumnName("c")
                                    .withColumnType(ColumnType.STRING)
                                    .withNullable(true))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 3)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(9L)
                    .withTraceInfoService("service10")
                    .withTraceInfoMethod("method11")
                    .withTraceInfoOperatorUid(12L)
                    .withEssTag("user12")));

            MySQLUtils.executeUpdate(conn, "alter /*reqid:13:service14:method15:operator=16:ess=user13*/ table " +
                    "sometable" +
                    " add column d float not null");
            expectedEvents.add(new BinlogWithSeqId(4, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new CreateOrModifyColumn()
                                    .withColumnName("d")
                                    .withColumnType(ColumnType.FLOATING_POINT)
                                    .withNullable(false))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 4)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(13L)
                    .withTraceInfoService("service14")
                    .withTraceInfoMethod("method15")
                    .withTraceInfoOperatorUid(16L)
                    .withEssTag("user13")));

            // DIRECT-81584 Переименование колонки оставлено на потом
            // Запрос: alter table sometable change column d e float not null
            // Ожидаемое изменение схемы:
            // new RenameColumn().withOldColumnName("d").withNewColumnName("e")

            MySQLUtils.executeUpdate(conn, "alter /*reqid:17:service18:method19:operator=20*/ table" +
                    " sometable drop column d");
            expectedEvents.add(new BinlogWithSeqId(5, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new DropColumn()
                                    .withColumnName("d"))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 5)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(17L)
                    .withTraceInfoService("service18")
                    .withTraceInfoMethod("method19")
                    .withTraceInfoOperatorUid(20L)));

            MySQLUtils.executeUpdate(conn, "rename /*reqid:21:service22:method23:operator=24*/ table" +
                    " sometable to sometable2");
            expectedEvents.add(new BinlogWithSeqId(6, new BinlogEvent()
                    .withAddedSchemaChanges(
                            new RenameTable()
                                    .withAddRename("sometable", "sometable2"))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 6)
                    .withUtcTimestamp(STUB_TIME)
                    .withTraceInfoReqId(21L)
                    .withTraceInfoService("service22")
                    .withTraceInfoMethod("method23")
                    .withTraceInfoOperatorUid(24L)));
        }

        List<BinlogWithSeqId> result = fetchAllEvents(expectedEvents.size());
        softly.assertThat(result).isEqualTo(expectedEvents);
    }

    @Test
    public void binlogReaderReadLongTransaction() throws InterruptedException, SQLException {
        try (Connection conn = mysql.connect()) {
            MySQLBinlogState mySQLBinlogState = MySQLBinlogState.snapshot(conn);
            stateRepository.saveState(SOURCE_TYPE, new ImmutableSourceState(0, 0, mySQLBinlogState.getGtidSet(), 0,
                    mySQLBinlogState.getSerializedServerSchema()));
        }

        int maxEventsPerTransaction = 2;
        binlogbrokerBuilder.withMaxEventsPerTransaction(maxEventsPerTransaction);
        startBinlogbroker();

        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);

            MySQLUtils.executeUpdate(conn, "CREATE TABLE long_transaction (a int, b int)");
            int rowsNumber = 1995;
            MySQLUtils.executeUpdate(conn, "BEGIN");
            for (int i = 0; i < rowsNumber; ++i) {
                MySQLUtils.executeUpdate(conn, "INSERT INTO long_transaction (a, b) VALUES (" + i + ", " + i + ")");
            }
            MySQLUtils.executeUpdate(conn, "COMMIT");

            MySQLUtils.executeUpdate(conn, "BEGIN");
            MySQLUtils.executeUpdate(conn, "INSERT INTO long_transaction SELECT * FROM long_transaction");
            MySQLUtils.executeUpdate(conn, "INSERT INTO /* 2 */ long_transaction SELECT * FROM long_transaction");
            MySQLUtils.executeUpdate(conn, "INSERT INTO long_transaction (a, b) values (123, 456)");
            MySQLUtils.executeUpdate(conn, "COMMIT");

            List<BinlogWithSeqId> result = fetchAllEvents(rowsNumber + 1);
            int rowsDone = 0;
            for (BinlogWithSeqId event : result.subList(1, result.size())) {
                softly.assertThat(event.event.getQuery())
                        .isEqualTo("INSERT INTO long_transaction (a, b) VALUES (" + rowsDone + ", " + rowsDone + ")");
                softly.assertThat(event.event.getQueryIndex()).isEqualTo(rowsDone);
                softly.assertThat(event.event.getEventIndex()).isEqualTo(rowsDone);
                ++rowsDone;
            }

            rowsDone = 0;
            int eventsInTransaction = 0;
            int writeQueries = 0;
            while (rowsDone < rowsNumber * 3 + 1) {
                List<BinlogWithSeqId> events = fetchAllEvents(1);
                softly.assertThat(events.size()).isLessThanOrEqualTo(2);
                for (BinlogWithSeqId event : events) {
                    softly.assertThat(event.event.getEventIndex()).isEqualTo(eventsInTransaction);
                    softly.assertThat(event.event.getQueryIndex())
                            .isEqualTo(rowsDone < rowsNumber ? 0 : rowsDone < rowsNumber * 3 ? 1 : 2);
                    softly.assertThat(event.event.getQuery())
                            .isEqualTo(rowsDone < rowsNumber ?
                                    "INSERT INTO long_transaction SELECT * FROM long_transaction" :
                                    rowsDone < rowsNumber * 3 ?
                                            "INSERT INTO /* 2 */ long_transaction SELECT * FROM long_transaction" :
                                            "INSERT INTO long_transaction (a, b) values (123, 456)");

                    writeQueries += event.event.getWriteQuery() ? 1 : 0;
                    rowsDone += event.event.getRows().size();
                    ++eventsInTransaction;
                }
            }
            softly.assertThat(writeQueries).isEqualTo(3);
        }
    }

    /**
     * Некоторые DDL-запросы не привносят важных для приложения изменений в схему.
     * От таких запросов ничего не должно ломаться.
     */
    @Test
    public void binlogReaderDdlNoChanges() throws InterruptedException, SQLException {
        try (Connection conn = mysql.connect()) {
            MySQLBinlogState mySQLBinlogState = MySQLBinlogState.snapshot(conn);
            stateRepository.saveState(SOURCE_TYPE, new ImmutableSourceState(0, 0, mySQLBinlogState.getGtidSet(), 0,
                    mySQLBinlogState.getSerializedServerSchema()));
        }
        GtidSet startingGtidSet =
                new GtidSet(Objects.requireNonNull(stateRepository.loadState(SOURCE_TYPE).getGtid()));
        long startingTransactionId = startingGtidSet.getUUIDSets().iterator().next().getIntervals().get(0).getEnd();
        startBinlogbroker();

        List<BinlogWithSeqId> expectedEvents = new ArrayList<>();

        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);

            MySQLUtils.executeUpdate(conn, "create table sometable "
                    + "( a enum('x', 'y', 'z') not null"
                    + ", b set('p', 'q', 's') not null"
                    + ")");
            expectedEvents.add(new BinlogWithSeqId(1, new BinlogEvent()
                    .withAddedSchemaChanges(new CreateTable()
                            .withAddedColumns(
                                    new CreateOrModifyColumn()
                                            .withColumnName("a")
                                            .withColumnType(ColumnType.STRING)
                                            .withNullable(false),
                                    new CreateOrModifyColumn()
                                            .withColumnName("b")
                                            .withColumnType(ColumnType.STRING)
                                            .withNullable(false))
                            .withPrimaryKey(Collections.emptyList()))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 1)
                    .withUtcTimestamp(STUB_TIME)));

            MySQLUtils.executeUpdate(conn, "alter table sometable modify a enum('x', 'y', 'z', 'w') not null");
            MySQLUtils.executeUpdate(conn, "alter table sometable change b b set('p', 'q', 's', 't') not null");

            // Пока что такие изменения игнорируются. Их стоит уметь поддерживать. DIRECT-82186
            MySQLUtils.executeUpdate(conn, "alter table sometable add primary key (a, b)");
            MySQLUtils.executeUpdate(conn, "alter table sometable drop primary key");
            MySQLUtils.executeUpdate(conn, "alter table sometable add index test_index (a, b)");
            MySQLUtils.executeUpdate(conn, "alter table sometable drop index test_index");

            MySQLUtils.executeUpdate(conn, "drop table sometable");
            expectedEvents.add(new BinlogWithSeqId(2, new BinlogEvent()
                    .withAddedSchemaChanges(new DropTable("sometable"))
                    .withDb(dbName)
                    .withOperation(Operation.SCHEMA)
                    .withQueryIndex(0)
                    .withEventIndex(0)
                    .withWriteQuery(true)
                    .withServerUuid(serverUuid)
                    .withSource(SOURCE_TYPE.getSourceName())
                    .withTable("sometable")
                    .withTransactionId(startingTransactionId + 8)
                    .withUtcTimestamp(STUB_TIME)));
        }

        List<BinlogWithSeqId> result = fetchAllEvents(expectedEvents.size());
        softly.assertThat(result).isEqualTo(expectedEvents);
    }

    private List<List<BinlogWithSeqId>> batchesTestInternal(int inserts, int batchSize, boolean oneTransaction)
            throws SQLException, InterruptedException {
        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);
            MySQLUtils.executeUpdate(conn, "create table foobar (id int primary key)");
            MySQLBinlogState mySQLBinlogState = MySQLBinlogState.snapshot(conn);
            stateRepository.saveState(SOURCE_TYPE, new ImmutableSourceState(0, 0, mySQLBinlogState.getGtidSet(), 0,
                    mySQLBinlogState.getSerializedServerSchema()));
        }

        // Появляется желание уменьшить здесь logbrokerChunkDuration, чтобы протестировать неполное получение пачки,
        // но не стоит этого делать. На CI случаются безумные тормоза, из-за которых mysql может больше половины минуты
        // не передавать бинлог-события в приложение. Соответственно, если поставить duration в несколько секунд, то
        // высока вероятность того, что не только последняя пачка придёт неполной. Тест будет моргать.
        binlogbrokerBuilder.withConsumerChunkSize(batchSize);
        startBinlogbroker();

        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);
            if (oneTransaction) {
                MySQLUtils.executeUpdate(conn, "begin");
                for (int i = 1; i <= inserts; ++i) {
                    MySQLUtils.executeUpdate(conn, "insert into foobar values (" + i + ")");
                }
                MySQLUtils.executeUpdate(conn, "commit");
            } else {
                for (int i = 1; i <= inserts; ++i) {
                    MySQLUtils.executeUpdate(conn, "begin");
                    MySQLUtils.executeUpdate(conn, "insert into foobar values (" + i + ")");
                    MySQLUtils.executeUpdate(conn, "commit");
                }
            }
        }

        return fetchAllEventLists((int) Math.ceil((double) inserts / batchSize));
    }

    private List<long[]> extractIds(List<List<BinlogWithSeqId>> eventBatches) {
        return eventBatches.stream()
                .map(eventBatch -> eventBatch.stream()
                        .map(event -> event.event.getRows().get(0).getPrimaryKey().values().iterator().next())
                        .mapToLong(Long.class::cast)
                        .toArray())
                .collect(Collectors.toList());
    }

    /**
     * Binlogbroker умеет группировать BinlogEvent'ы в пачки по несколько штук. Версия для пачки из 5 BinlogEvent'ов,
     * каждый SQL-запрос в отдельной транзакции.
     */
    @Test
    public void testBatchesWith5QueriesManyTransactions() throws SQLException, InterruptedException {
        List<List<BinlogWithSeqId>> eventBatches = batchesTestInternal(15, 5, false);
        softly.assertThat(extractIds(eventBatches)).containsExactly(
                new long[]{1L, 2L, 3L, 4L, 5L},
                new long[]{6L, 7L, 8L, 9L, 10L},
                new long[]{11L, 12L, 13L, 14L, 15L});
        checkWasWrittenCorrectState(eventBatches);
    }

    /**
     * Binlogbroker умеет группировать BinlogEvent'ы в пачки по несколько штук. Версия для пачки из 3 BinlogEvent'ов,
     * каждый SQL-запрос в отдельной транзакции.
     */
    @Test
    public void testBatchesWith3QueriesManyTransactions() throws SQLException, InterruptedException {
        List<List<BinlogWithSeqId>> eventBatches = batchesTestInternal(15, 3, false);
        softly.assertThat(extractIds(eventBatches)).containsExactly(
                new long[]{1L, 2L, 3L},
                new long[]{4L, 5L, 6L},
                new long[]{7L, 8L, 9L},
                new long[]{10L, 11L, 12L},
                new long[]{13L, 14L, 15L});
        checkWasWrittenCorrectState(eventBatches);
    }

    /**
     * DIRECT-81764 Обработка таблиц без primary key.
     */
    @Test
    public void tableWithoutPrimaryKey() throws SQLException, InterruptedException {
        try (Connection conn = mysql.connect()) {
            MySQLBinlogState mySQLBinlogState = MySQLBinlogState.snapshot(conn);
            stateRepository.saveState(SOURCE_TYPE, new ImmutableSourceState(0, 0, mySQLBinlogState.getGtidSet(), 0,
                    mySQLBinlogState.getSerializedServerSchema()));
        }
        startBinlogbroker();

        try (Connection conn = mysql.connect()) {
            conn.setCatalog(dbName);

            MySQLUtils.executeUpdate(conn, "create table sometable (a int, b tinyint)");
            MySQLUtils.executeUpdate(conn, "begin");
            MySQLUtils.executeUpdate(conn, "insert into sometable (a, b) values (1, 2)");
            MySQLUtils.executeUpdate(conn, "commit");
        }

        List<BinlogWithSeqId> events = fetchAllEvents(2);
        softly.assertThat(events).hasSize(2);
        if (events.size() != 2) {
            return;
        }
        softly.assertThat(events.get(0).event.getSchemaChanges()).containsExactly(
                new CreateTable()
                        .withAddedColumns(
                                new CreateOrModifyColumn()
                                        .withColumnName("a")
                                        .withColumnType(ColumnType.INTEGER)
                                        .withNullable(true),
                                new CreateOrModifyColumn()
                                        .withColumnName("b")
                                        .withColumnType(ColumnType.INTEGER)
                                        .withNullable(true))
                        .withPrimaryKey(Collections.emptyList()));
        softly.assertThat(events.get(1).event.getRows()).containsExactly(
                new BinlogEvent.Row()
                        .withAfter(Map.of(
                                "a", 1L,
                                "b", 2L))
                        .withBefore(Map.of())
                        .withPrimaryKey(Map.of())
                        .withRowIndex(0));
    }

    private Map<String, Object> mapWithNullValue(String key) {
        var map = new HashMap<String, Object>();
        map.put(key, null);
        return map;
    }

    private void checkWasWrittenCorrectState(List<List<BinlogWithSeqId>> eventBatches) {
        BinlogWithSeqId lastEvent;
        try {
            List<BinlogWithSeqId> lastEventBatch = eventBatches.get(eventBatches.size() - 1);
            lastEvent = lastEventBatch.get(lastEventBatch.size() - 1);
        } catch (IndexOutOfBoundsException ignored) {
            softly.assertThat(true)
                    .describedAs("Failing the test because got incorrect batches")
                    .isFalse();
            return;
        }
        String gtid = Objects.requireNonNull(stateRepository.map.get(SOURCE_TYPE).getGtid());
        softly.assertThat(lastEvent.event.getGtid())
                .describedAs("Written correct state")
                .isEqualTo(gtid.replaceAll(":1-", ":"));
    }

    static class MemoryStateRepository implements SourceStateRepository {
        private final Map<SourceType, ImmutableSourceState> map = new HashMap<>();

        @Override
        public ImmutableSourceState loadState(SourceType source) {
            return map.computeIfAbsent(source, k -> new ImmutableSourceState());
        }

        @Override
        public void saveState(SourceType source, ImmutableSourceState immutableSourceState) {
            map.put(source, immutableSourceState);
        }

        @Override
        public void close() throws Exception {
            // nothing to close
        }
    }
}
