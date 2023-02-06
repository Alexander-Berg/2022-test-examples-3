package ru.yandex.direct.binlogbroker.logbrokerwriter.components;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.binlog.model.Operation;
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
import static ru.yandex.direct.mysql.MysqlBinlogRowImage.MINIMAL;

/**
 * Проверяет корректную десериализацию разных типов данных, которые приходят в бинлоге.
 * <p>
 * Инициализация нового Binlogbroker - небыстрая операция. Если тестировать каждый случай отдельно,
 * то тесты будут работать очень долго. Поэтому запускается только один Binlogbroker, и если он в одном из
 * тестов ломается, то последующие тесты не смогут быть проведены корректно.
 */
@ParametersAreNonnullByDefault
public class BinlogbrokerTypesTest {
    private static final SourceType SOURCE = SourceType.fromType(DEVTEST, "ppc:1");
    private static final int QUEUE_POLL_SECONDS = 60;

    @ClassRule
    public static JunitRuleMySQLServerCreator mysqlServerCreator = new JunitRuleMySQLServerCreator();
    /**
     * Флаг, указывающий, что весь набор тестов сломан. Взводится только если от Binlogbroker была получена ошибка
     * либо если он не отвечает. Случай, когда тест получил от Binlogbroker ожидаемое количество объектов,
     * но сами объекты не равны ожиданию, не является поломкой.
     */
    private static boolean broken = false;
    private static TmpMySQLServerWithDataDir mysql;
    private static BlockingQueue<BinlogWithSeqId> queue;
    private static Thread binlogbrokerThread;
    private static AtomicReference<Throwable> exceptionHolder;

    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        binlogbrokerThread.interrupt();
        binlogbrokerThread.join(10_000);
    }

    @BeforeClass
    public static void setUpClass() throws InterruptedException, SQLException {
        MySQLServerBuilder mysqlServerBuilder =
                new DirectMysqlDb(TestMysqlConfig.directConfig())
                        .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                                .setGracefulStopTimeout(Duration.ZERO)
                                .setServerId(13579)
                                .withNoSync(true));
        mysql = mysqlServerCreator.createWithBinlog(SOURCE.getDbName(), mysqlServerBuilder.copy(), MINIMAL);

        DbConfig dbConfig = new DbConfig();
        dbConfig.setConnectTimeout(1.0);
        dbConfig.setDb("ppc");
        dbConfig.setDbName(SOURCE.getDbName());
        dbConfig.setEngine(DbConfig.Engine.MYSQL);
        dbConfig.setHosts(ImmutableList.of(mysql.getHost()));
        dbConfig.setPass(mysql.getPassword());
        dbConfig.setPort(mysql.getPort());
        dbConfig.setUser(mysql.getUsername());

        BinlogbrokerTest.MemoryStateRepository stateRepository = new BinlogbrokerTest.MemoryStateRepository();
        queue = new ArrayBlockingQueue<>(100);

        mysql.awaitConnectivity(Duration.ofSeconds(30));
        try (Connection conn = mysql.connect()) {
            MySQLUtils.executeUpdate(conn, "create database ppc");
            MySQLBinlogState mySQLBinlogState = MySQLBinlogState.snapshot(conn);
            stateRepository.saveState(SOURCE, new ImmutableSourceState(1, 0, mySQLBinlogState.getGtidSet(), 0,
                    mySQLBinlogState.getSerializedServerSchema()));
        }
        binlogbrokerThread = new Thread(Binlogbroker.builder()
                .withSource(SOURCE)
                .withSourceDbConfigSupplier(() -> dbConfig)
                .withBinlogEventConsumer(new LogbrokerWriter<BinlogWithSeqId>() {
                    @Override
                    public CompletableFuture<Integer> write(List<BinlogWithSeqId> eventsWithSeqId)
                            throws LogBrokerWriterException {
                        queue.addAll(eventsWithSeqId);
                        return CompletableFuture.completedFuture(0);
                    }

                    @Override
                    public Long getInitialMaxSeqNo() {
                        return 0L;
                    }

                    @Override
                    public void close() {
                    }
                })
                .withSourceStateRepository(stateRepository)
                .withMysqlServerBuilder(mysqlServerBuilder.copy())
                .withInitialServerId(null)
                .withKeepAliveTimeout(Duration.ofSeconds(30))
                .withMaxBufferedEvents(100)
                .withConsumerChunkSize(1)
                .withMaxEventsPerTransaction(20)
                .withConsumerChunkDuration(Duration.ofDays(999))
                .withLogbrokerWriterMonitoring(mock(LogbrokerWriterMonitoring.class))
                .withTraceHelper(new TraceHelper("", mock(TraceLogger.class)))
                .withFlushEventsTimeout(Duration.ofSeconds(60))
                .build());
        exceptionHolder = new AtomicReference<>();
        binlogbrokerThread.setUncaughtExceptionHandler((t, e) -> exceptionHolder.set(e));
        binlogbrokerThread.setDaemon(true);
        binlogbrokerThread.start();
    }

    @Before
    public void setUp() {
        if (broken) {
            throw new AssumptionViolatedException("This test was broken by another test");
        }
    }

    @After
    public void tearDown() {
        checkException();
    }

    private void checkException() {
        Throwable throwable = exceptionHolder.get();
        if (throwable != null) {
            broken = true;
            throw new IllegalStateException(throwable);
        }
    }

    private void runAndCompare(String columnType, List<String> quotedObjects, List<?> expectedResultObjects)
            throws InterruptedException {
        runAndCompare(columnType, quotedObjects, expectedResultObjects,
                (actual, expected) -> Assertions.assertThat(actual).isEqualTo(expected));
    }

    private void runAndCompare(String columnType, List<String> quotedObjects, List<?> expectedResultObjects,
                               BiConsumer<List<Object>, List<?>> actualAndExpectedConsumer)
            throws InterruptedException {
        Preconditions.checkArgument(quotedObjects.size() == expectedResultObjects.size());
        String tableName = TestUtils.randomName("test_", 12);
        StringBuilder insertQueryBuilder = new StringBuilder()
                .append("insert into ")
                .append(tableName)
                .append(" (id, field) values ");
        String delimiter = "";
        int primaryKey = 0;
        for (String quotedObject : quotedObjects) {
            insertQueryBuilder
                    .append(delimiter)
                    .append("(")
                    .append(primaryKey++)
                    .append(", ")
                    .append(quotedObject)
                    .append(")");
            delimiter = ", ";
        }
        String insertSql = insertQueryBuilder.toString();
        String createTableSql = "create table " + tableName + " (id int primary key, field " + columnType + ")";
        int resultsSize = expectedResultObjects.size();

        List<Object> fetchedObjects = createInsertResults(createTableSql, insertSql, resultsSize);

        actualAndExpectedConsumer.accept(fetchedObjects, expectedResultObjects);
    }

    private List<Object> createInsertResults(String createTableSql, String insertSql, int resultsSize) throws InterruptedException {
        try (Connection conn = mysql.connect()) {
            MySQLUtils.executeUpdate(conn, "use ppc");
            // Не везде в Директе используется строгий режим SQL-запросов. Из-за этого в БД появляются
            // некорректные данные (например, пустые строки в enum), работу с которыми тоже следует протестировать.
            MySQLUtils.executeUpdate(conn, "SET SESSION sql_mode = ''");
            MySQLUtils.executeUpdate(conn,
                    createTableSql);
            MySQLUtils.executeUpdate(conn, "begin");
            MySQLUtils.executeUpdate(conn, insertSql);
            MySQLUtils.executeUpdate(conn, "commit");
        } catch (SQLException exc) {
            throw new IllegalStateException(exc);
        }

        List<Object> fetchedObjects = new ArrayList<>(resultsSize);
        while (fetchedObjects.size() < resultsSize) {
            BinlogWithSeqId event = null;
            for (int i = 0; i < QUEUE_POLL_SECONDS && event == null; ++i) {
                event = queue.poll(1, TimeUnit.SECONDS);
                checkException();
            }
            if (event == null) {
                fetchedObjects.add("<sudden break>");
                broken = true;
                break;
            }
            if (event.event.getOperation() == Operation.INSERT) {
                event.event.getRows().stream()
                        .flatMap(rows -> rows.getAfter().entrySet().stream())
                        .filter(row -> row.getKey().equals("field"))
                        .map(Map.Entry::getValue)
                        .forEachOrdered(fetchedObjects::add);
            }
        }
        return fetchedObjects;
    }

    @Test
    public void tinyIntNotNull() throws InterruptedException {
        runAndCompare("tinyint not null",
                Arrays.asList("0", "1", "-1"),
                Arrays.asList(0L, 1L, -1L));
    }

    @Test
    public void tinyIntUnsignedNotNull() throws InterruptedException {
        runAndCompare("tinyint unsigned not null",
                Arrays.asList("0", "1", "150"),
                Arrays.asList(0L, 1L, 150L));
    }

    @Test
    public void tinyIntUnsignedNotNullTestDefaultValue() throws InterruptedException {
        String tableName = TestUtils.randomName("test_", 12);
        var createTableSql = "create table " + tableName + " (id int primary key, field tinyint(1) unsigned NOT NULL " +
                "DEFAULT '22')";
        var insertQuerySql = "insert into " + tableName + " (id) values (5)";
        List<Object> insertResults = createInsertResults(createTableSql, insertQuerySql, 1);
        Assertions.assertThat(insertResults).isEqualTo(List.of(22L));
    }

    @Test
    public void bitNotNull() throws InterruptedException {
        runAndCompare("bit(2) not null",
                Arrays.asList("b'00'", "b'01'", "b'11'"),
                Arrays.asList(new byte[]{}, new byte[]{1}, new byte[]{3}),
                (actual, expected) -> Assertions.assertThat(actual)
                        .usingElementComparator((o1, o2) ->
                                Arrays.compare((byte[]) o1, (byte[]) o2)
                        ).isEqualTo(expected));
    }

    @Test
    public void bitNull() throws InterruptedException {
        List<byte[]> listWithNull = new ArrayList<>();
        listWithNull.add(null);

        runAndCompare("bit(2) null",
                List.of("null"),
                listWithNull,
                (actual, expected) -> Assertions.assertThat(actual)
                        .usingElementComparator((o1, o2) -> Arrays.compare((byte[]) o1, (byte[]) o2))
                        .isEqualTo(expected));
    }

    @Test
    public void bit17NotNull() throws InterruptedException {
        runAndCompare("bit(17) not null",
                List.of("b'11111111111111111'"),
                List.of(new byte[]{-1, -1, 1}),
                (actual, expected) -> Assertions.assertThat(actual)
                        .usingElementComparator((o1, o2) -> Arrays.compare((byte[]) o1, (byte[]) o2))
                        .isEqualTo(expected));
    }

    @Test
    public void intNotNull() throws InterruptedException {
        runAndCompare("int not null",
                Arrays.asList("0", "1", "1000", "-1"),
                Arrays.asList(0L, 1L, 1000L, -1L));
    }

    @Test
    public void intUnsignedNotNull() throws InterruptedException {
        runAndCompare("int unsigned not null",
                Arrays.asList("0", "1", "1000", "3000000000"),
                Arrays.asList(0L, 1L, 1000L, 3_000_000_000L));
    }

    @Test
    public void smallintUnsignedNotNull() throws InterruptedException {
        runAndCompare("smallint unsigned not null",
                Arrays.asList("0", "1", "1000", "40000"),
                Arrays.asList(0L, 1L, 1000L, 40000L));
    }

    @Test
    public void intNull() throws InterruptedException {
        runAndCompare("int null",
                Arrays.asList("0", "1", "1000", "null"),
                Arrays.asList(0L, 1L, 1000L, null));
    }

    @Test
    public void bigint() throws InterruptedException {
        runAndCompare("bigint",
                Arrays.asList("0", "1", "-1", "power(2, 63) - 1", "-power(2, 63)"),
                Arrays.asList(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE));
    }

    @Test
    public void bigintUnsignedNonNull() throws InterruptedException {
        List<BigInteger> bigIntegers = Arrays.asList(BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.valueOf(Long.MAX_VALUE),
                BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE),
                BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(101)),
                BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE));
        runAndCompare("bigint(20) unsigned not null",
                bigIntegers.stream().map(BigInteger::toString).collect(Collectors.toList()),
                bigIntegers);
    }

    @Test
    public void decimalNotNull() throws InterruptedException {
        runAndCompare("decimal(10,2) not null",
                Arrays.asList("0", "1", "3.14", "3.14159", "12345678.90"),
                Arrays.asList(
                        new BigDecimal("0.00"),
                        new BigDecimal("1.00"),
                        new BigDecimal("3.14"),
                        new BigDecimal("3.14"),
                        new BigDecimal("12345678.90")));
    }

    @Test
    public void floatNotNull() throws InterruptedException {
        runAndCompare("float not null",
                Arrays.asList("0", "1", "3.14", "3.14159", "12345678.90"),
                Arrays.asList(0.0, 1.0, 3.14, 3.14159, 12345678.9),
                (actual, expected) -> Assertions
                        .assertThat(actual)
                        .usingElementComparator((o1, o2) -> {
                            if (o1 instanceof Double && o2 instanceof Double) {
                                return Math.abs((Double) o1 - (Double) o2) < 1e6 ? 0 : -1;
                            } else {
                                return -1;
                            }
                        })
                        .containsExactlyElementsOf(expected));
    }

    @Test
    public void doubleNotNull() throws InterruptedException {
        runAndCompare("double not null",
                Arrays.asList("0", "1", "3.14", "3.14159", "12345678.90"),
                Arrays.asList(0.0, 1.0, 3.14, 3.14159, 12345678.9),
                (actual, expected) -> Assertions
                        .assertThat(actual)
                        .usingElementComparator((o1, o2) -> {
                            if (o1 instanceof Double && o2 instanceof Double) {
                                return Math.abs((Double) o1 - (Double) o2) < 1e6 ? 0 : -1;
                            } else {
                                return -1;
                            }
                        })
                        .containsExactlyElementsOf(expected));
    }

    @Test
    public void dateNotNull() throws InterruptedException {
        runAndCompare("date not null",
                Collections.singletonList("'2018-06-28'"),
                Collections.singletonList(LocalDate.of(2018, 6, 28)));
    }

    @Ignore("This test relies on a system timezone and may fail in some machines")
    @Test
    public void datetimeNotNull() throws InterruptedException {
        runAndCompare("datetime not null",
                Collections.singletonList("'2018-06-28 18:14:30'"),
                Collections.singletonList(LocalDateTime.of(2018, 6, 28, 18, 14, 30)));
    }

    @Ignore("This test relies on a system timezone and may fail in some machines")
    @Test
    public void timestampNotNull() throws InterruptedException {
        runAndCompare("timestamp not null",
                Collections.singletonList("'2018-06-28 14:14:30'"),
                Collections.singletonList(LocalDateTime.of(2018, 6, 28, 11, 14, 30)));
    }

    @Test
    public void varcharNull() throws InterruptedException {
        runAndCompare("varchar(5) null",
                Arrays.asList("null", "''", "'foo'", "'bar'"),
                Arrays.asList(null, "", "foo", "bar"));
    }

    @Test
    public void textNull() throws InterruptedException {
        runAndCompare("text null",
                Arrays.asList("null", "''", "'foo'", "'bar'"),
                Arrays.asList(null, "", "foo", "bar"));
    }

    @Test
    public void enumNotNull() throws InterruptedException {
        runAndCompare("enum ('one', 'two', 'three') not null",
                Arrays.asList("'one'", "'two'", "'three'"),
                Arrays.asList("one", "two", "three"));
    }

    @Test
    public void enumNull() throws InterruptedException {
        runAndCompare("enum ('one', 'two', 'three') null",
                // https://dev.mysql.com/doc/refman/5.7/en/enum.html#enum-nulls
                // В нестрогом режиме mysql позволяет вставлять несуществующие enum-значения.
                // В таком случае в БД вставляется enum с кодом 0, который в mysql отображается как пустая строка.
                // Binlogbroker трактует такое значение как null.
                Arrays.asList("null", "'oops'", "'one'", "'two'", "'three'"),
                Arrays.asList(null, "", "one", "two", "three"));
    }
}

