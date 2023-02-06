package ru.yandex.direct.binlog.reader;

import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.direct.mysql.MySQLColumnData;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.mysql.MySQLSimpleConnector;
import ru.yandex.direct.mysql.MySQLUtils;
import ru.yandex.direct.mysql.TmpMySQLServerWithDataDir;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;
import ru.yandex.direct.test.utils.SocketProxy;
import ru.yandex.direct.utils.Checked;

public class ReaderTest {
    public static final int MYSQL_SERVER_ID = 218734;
    public static final String TEST_DB_NAME = "reader_test";


    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Rule
    public Timeout timeoutRule = new Timeout(3, TimeUnit.MINUTES);

    @Test
    public void test() throws InterruptedException, SQLException {
        MySQLServerBuilder mysqlBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                        .setGracefulStopTimeout(Duration.ZERO)
                        .setServerId(MYSQL_SERVER_ID)
                        .withNoSync(true)
                );

        // Стартуем mysql-сервер источник для тестов. С него будем читать бинлоги.
        try (TmpMySQLServerWithDataDir mysql = TmpMySQLServerWithDataDir
                .createWithBinlog("mysql", mysqlBuilder.copy())
        ) {
            mysql.awaitConnectivity(Duration.ofSeconds(60));

            // Создаем на сервере-источнике тестовую БД и таблицу.
            // В ней будем делать разные изменения, которые должны отражаться в бинлоге.
            try (Connection conn = mysql.connect()) {
                MySQLUtils.executeUpdate(conn, "CREATE DATABASE " + MySQLUtils.quoteName(TEST_DB_NAME));
                conn.setCatalog(TEST_DB_NAME);
                MySQLUtils.executeUpdate(conn, "CREATE TABLE table1 (a int, b int default -1)");
            }

            // Читаем состояние сервера-источника (схему БД и gtid-set).
            BinlogSource source = new BinlogSource("test", mysql);
            BinlogStateOptimisticSnapshotter snapshotter = new BinlogStateOptimisticSnapshotter(MYSQL_SERVER_ID);
            StatefulBinlogSource statefulSource = new StatefulBinlogSource(
                    source,
                    snapshotter.snapshot(source)
            );

            try (
                    // Инициализируем читалку бинлога
                    BinlogReader reader = new BinlogReader(
                            statefulSource,
                            new DirectMysqlDb(TestMysqlConfig.directConfig())
                                    .useSandboxMysqlServerIfPossible(
                                            new MySQLServerBuilder()
                                                    .setGracefulStopTimeout(Duration.ZERO)
                                                    .addExtraArgs(Collections.singletonList("--skip-innodb-use-native" +
                                                            "-aio"))
                                    ),
                            null,
                            Duration.ofSeconds(60),
                            MYSQL_SERVER_ID,
                            32
                    )
            ) {
                // подключаемся к серверу-источнику и делаем несколько изменений
                try (Connection conn = mysql.connect()) {
                    conn.setCatalog(TEST_DB_NAME);
                    MySQLUtils.executeUpdate(conn, "INSERT /* reqid:8761234586 */ INTO table1 VALUES (1, 1)");
                    MySQLUtils.executeUpdate(conn, "BEGIN");
                    MySQLUtils.executeUpdate(conn, "INSERT INTO table1 (a) VALUES (1)");
                    MySQLUtils.executeUpdate(conn, "UPDATE table1 SET b=10 WHERE a=1");
                    MySQLUtils.executeUpdate(conn, "COMMIT");
                }

                // Обертка над reader-ом, аггрегирующая события по транзакциям
                TransactionReader txReader = new TransactionReader(reader);

                // Теперь ждем когда эти изменения приедут из бинлога и убеждаемся, что они ровно такие, как мы ожидали
                StateBound<Transaction> tx;
                List<EnrichedInsertRow> insertRows;
                List<EnrichedUpdateRow> updateRows;

                // Первая транзакция
                tx = txReader.readTransaction(Duration.ofSeconds(10));
                Assert.assertEquals(
                        Lists.emptyList(),
                        tx.getData().getEnrichedEvents().flatMap(EnrichedEvent::deletedRowsStream)
                                .collect(Collectors.toList())
                );
                Assert.assertEquals(
                        Lists.emptyList(),
                        tx.getData().getEnrichedEvents().flatMap(EnrichedEvent::updatedRowsStream)
                                .collect(Collectors.toList())
                );
                insertRows = tx.getData().getEnrichedEvents().flatMap(EnrichedEvent::insertedRowsStream)
                        .collect(Collectors.toList());
                Assert.assertEquals(1, insertRows.size());
                Assert.assertEquals(OptionalLong.of(8761234586L), insertRows.get(0).getDirectTraceInfo().getReqId());
                Assert.assertEquals("1",
                        insertRows.get(0).getFields().getByName("a").getValueAsString());
                Assert.assertEquals("1",
                        insertRows.get(0).getFields().getByName("b").getValueAsString());

                // Вторая транзакция. Она посложней, в ней два event-а (INSERT и UPDATE), и три изменившихся row
                // (одна строка в первом событии, и две - во втором).
                tx = txReader.readTransaction(Duration.ofSeconds(10));

                Assert.assertEquals(
                        Arrays.asList("INSERT INTO table1 (a) VALUES (1)", "UPDATE table1 SET b=10 WHERE a=1"),
                        tx.getData().getEnrichedEvents().map(e -> e.getQuery().getQueryString())
                                .collect(Collectors.toList())
                );

                Assert.assertEquals(
                        Lists.emptyList(),
                        tx.getData().getEnrichedEvents().flatMap(EnrichedEvent::deletedRowsStream)
                                .collect(Collectors.toList())
                );

                insertRows = tx.getData().getEnrichedEvents().flatMap(EnrichedEvent::insertedRowsStream)
                        .collect(Collectors.toList());

                Assert.assertEquals(1, insertRows.size());
                Assert.assertEquals("1",
                        insertRows.get(0).getFields().getByName("a").getValueAsString());
                Assert.assertEquals("-1",
                        insertRows.get(0).getFields().getByName("b").getValueAsString());

                updateRows = tx.getData().getEnrichedEvents().flatMap(EnrichedEvent::updatedRowsStream)
                        .collect(Collectors.toList());

                Assert.assertEquals(2, updateRows.size());
                Assert.assertEquals("1",
                        updateRows.get(0).getFields().getBefore().getByName("b").getValueAsString());
                Assert.assertEquals("10",
                        updateRows.get(0).getFields().getAfter().getByName("b").getValueAsString());
                Assert.assertEquals("-1",
                        updateRows.get(1).getFields().getBefore().getByName("b").getValueAsString());
                Assert.assertEquals("10",
                        updateRows.get(1).getFields().getAfter().getByName("b").getValueAsString());
            }

        }
    }

    /**
     * Запускает чтение бинлога через соединение, которое часто рвётся.
     */
    @Test
    public void reconnects() throws InterruptedException, SQLException, UnknownHostException {
        MySQLServerBuilder mysqlBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                        .setGracefulStopTimeout(Duration.ZERO)
                        .setServerId(MYSQL_SERVER_ID)
                        .withNoSync(true)
                );

        // Стартуем mysql-сервер источник для тестов. С него будем читать бинлоги.
        try (TmpMySQLServerWithDataDir mysql = TmpMySQLServerWithDataDir
                .createWithBinlog("mysql", mysqlBuilder.copy())
        ) {
            mysql.awaitConnectivity(Duration.ofSeconds(60));

            // Создаем на сервере-источнике тестовую БД и таблицу.
            // В ней будем делать разные изменения, которые должны отражаться в бинлоге.
            try (Connection conn = mysql.connect()) {
                MySQLUtils.executeUpdate(conn, "CREATE DATABASE " + MySQLUtils.quoteName(TEST_DB_NAME));
                conn.setCatalog(TEST_DB_NAME);
                MySQLUtils.executeUpdate(conn, "CREATE TABLE table1 (a int)");
            }

            InetSocketAddress mysqlAddress =
                    new InetSocketAddress(InetAddress.getByName(mysql.getHost()), mysql.getPort());
            try (SocketProxy socketProxy = new SocketProxy(
                    new InetSocketAddress(Inet4Address.getLocalHost(), 0),
                    mysqlAddress)) {
                socketProxy.setDaemon(true);
                socketProxy.setConcurrency(4);
                ByteLimitedManInTheMiddle limiter = new ByteLimitedManInTheMiddle(mysqlAddress);
                socketProxy.addDataInterceptor(limiter);
                socketProxy.start();

                InetSocketAddress proxyAddress = ((InetSocketAddress) socketProxy.getListenAddress());
                // Читаем состояние сервера-источника (схему БД и gtid-set).
                BinlogSource source = new BinlogSource("test", new MySQLSimpleConnector(
                        proxyAddress.getHostName(),
                        proxyAddress.getPort(),
                        mysql.getUsername(),
                        mysql.getPassword()));
                BinlogStateOptimisticSnapshotter snapshotter = new BinlogStateOptimisticSnapshotter(MYSQL_SERVER_ID);
                StatefulBinlogSource statefulSource = new StatefulBinlogSource(
                        source,
                        snapshotter.snapshot(source));

                final int count = 500;
                try (Connection conn = mysql.connect()) {
                    conn.setCatalog(TEST_DB_NAME);
                    for (int i = 0; i < count; ++i) {
                        MySQLUtils.executeUpdate(conn, "BEGIN");
                        MySQLUtils.executeUpdate(conn, "INSERT /* reqid:8761234586 */ INTO table1 VALUES (" + i + ")");
                        MySQLUtils.executeUpdate(conn, "COMMIT");
                    }
                }

                try (BinlogReader reader = new BinlogReader(
                        statefulSource,
                        new DirectMysqlDb(TestMysqlConfig.directConfig())
                                .useSandboxMysqlServerIfPossible(
                                        new MySQLServerBuilder()
                                                .setGracefulStopTimeout(Duration.ZERO)
                                                .addExtraArgs(Collections.singletonList("--skip-innodb-use-native-aio"))
                                ),
                        null,
                        Duration.ofSeconds(60),
                        MYSQL_SERVER_ID,
                        32,
                        Duration.ofMinutes(5),
                        60 * 4,
                        Duration.ofSeconds(1))) {
                    TransactionReader txReader = new TransactionReader(reader);
                    limiter.enable();
                    List<Long> values = new ArrayList<>();
                    while (values.size() < count) {
                        txReader.readTransaction(Duration.ofSeconds(10))
                                .getData()
                                .getEnrichedEvents()
                                .flatMap(EnrichedEvent::rowsStream)
                                .map(EnrichedInsertRow.class::cast)
                                .map(EnrichedInsertRow::getFields)
                                .map(c -> c.getByName("a"))
                                .map(MySQLColumnData::getValueAsLong)
                                .forEach(values::add);
                    }
                    softly.assertThat(values)
                            .isEqualTo(LongStream.range(0, count).boxed().collect(Collectors.toList()));
                }
                // Проверка, что соединение действительно рвалось несколько раз
                softly.assertThat(limiter.forcedDisconnects).isGreaterThan(3);
            }
        }
    }

    /**
     * Если давно не было видно новых событий, мог просто порваться сокет.
     * Подключение должно быть установлено заново незаметно для пользователя.
     */
    @Test
    public void keepAlive() throws InterruptedException, SQLException, UnknownHostException {
        MySQLServerBuilder mysqlBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                        .setGracefulStopTimeout(Duration.ZERO)
                        .setServerId(MYSQL_SERVER_ID)
                        .withNoSync(true)
                );

        // Стартуем mysql-сервер источник для тестов. С него будем читать бинлоги.
        try (TmpMySQLServerWithDataDir mysql = TmpMySQLServerWithDataDir
                .createWithBinlog("mysql", mysqlBuilder.copy())
        ) {
            mysql.awaitConnectivity(Duration.ofSeconds(60));

            // Создаем на сервере-источнике тестовую БД и таблицу.
            // В ней будем делать разные изменения, которые должны отражаться в бинлоге.
            try (Connection conn = mysql.connect()) {
                MySQLUtils.executeUpdate(conn, "CREATE DATABASE " + MySQLUtils.quoteName(TEST_DB_NAME));
                conn.setCatalog(TEST_DB_NAME);
                MySQLUtils.executeUpdate(conn, "CREATE TABLE table1 (a int)");
            }

            InetSocketAddress mysqlAddress =
                    new InetSocketAddress(InetAddress.getByName(mysql.getHost()), mysql.getPort());
            try (SocketProxy socketProxy = new SocketProxy(
                    new InetSocketAddress(Inet4Address.getLocalHost(), 0),
                    mysqlAddress)) {
                socketProxy.setDaemon(true);
                socketProxy.setConcurrency(4);
                DataLoosingManInTheMiddle timeoutEmulator = new DataLoosingManInTheMiddle();
                socketProxy.addDataInterceptor(timeoutEmulator);
                socketProxy.start();

                InetSocketAddress proxyAddress = ((InetSocketAddress) socketProxy.getListenAddress());
                // Читаем состояние сервера-источника (схему БД и gtid-set).
                BinlogSource source = new BinlogSource("test", new MySQLSimpleConnector(
                        proxyAddress.getHostName(),
                        proxyAddress.getPort(),
                        mysql.getUsername(),
                        mysql.getPassword()));
                BinlogStateOptimisticSnapshotter snapshotter = new BinlogStateOptimisticSnapshotter(MYSQL_SERVER_ID);
                StatefulBinlogSource statefulSource = new StatefulBinlogSource(
                        source,
                        snapshotter.snapshot(source));

                try (
                        // Инициализируем читалку бинлога
                        BinlogReader reader = new BinlogReader(
                                statefulSource,
                                new DirectMysqlDb(TestMysqlConfig.directConfig())
                                        .useSandboxMysqlServerIfPossible(
                                                new MySQLServerBuilder()
                                                        .setGracefulStopTimeout(Duration.ZERO)
                                                        .addExtraArgs(Collections.singletonList("--skip-innodb-use" +
                                                                "-native-aio"))
                                        ),
                                null,
                                Duration.ofSeconds(5),
                                MYSQL_SERVER_ID,
                                32
                        )
                ) {
                    TransactionReader txReader = new TransactionReader(reader);
                    try (Connection conn = mysql.connect()) {
                        conn.setCatalog(TEST_DB_NAME);
                        MySQLUtils.executeUpdate(conn, "BEGIN");
                        MySQLUtils.executeUpdate(conn, "INSERT /* reqid:8761234586 */ INTO table1 VALUES (123)");
                        MySQLUtils.executeUpdate(conn, "COMMIT");
                    }

                    // Первое чтение - без препятствий
                    List<Long> values = txReader.readTransaction(Duration.ofSeconds(5))
                            .getData()
                            .getEnrichedEvents()
                            .flatMap(EnrichedEvent::rowsStream)
                            .map(EnrichedInsertRow.class::cast)
                            .map(EnrichedInsertRow::getFields)
                            .map(c -> c.getByName("a"))
                            .map(MySQLColumnData::getValueAsLong)
                            .collect(Collectors.toList());

                    softly.assertThat(values)
                            .describedAs("First reading without connection loosing should be successful")
                            .containsExactly(123L);

                    int connectionsEstablishedBeforeLoosing = timeoutEmulator.connectionsEstablished;

                    // Второе чтение - с препятствиями. Передаваемые данные будут теряться до тех пор,
                    // пока не будет установлено новое соединение.
                    timeoutEmulator.startLoosingData();

                    try (Connection conn = mysql.connect()) {
                        conn.setCatalog(TEST_DB_NAME);
                        MySQLUtils.executeUpdate(conn, "BEGIN");
                        MySQLUtils.executeUpdate(conn, "INSERT /* reqid:8761234586 */ INTO table1 VALUES (456)");
                        MySQLUtils.executeUpdate(conn, "COMMIT");
                    }

                    values = txReader.readTransaction(Duration.ofSeconds(20))
                            .getData()
                            .getEnrichedEvents()
                            .flatMap(EnrichedEvent::rowsStream)
                            .map(EnrichedInsertRow.class::cast)
                            .map(EnrichedInsertRow::getFields)
                            .map(c -> c.getByName("a"))
                            .map(MySQLColumnData::getValueAsLong)
                            .collect(Collectors.toList());
                    softly.assertThat(values)
                            .describedAs("Second reading with connection loosing should be successful too")
                            .containsExactly(456L);
                    softly.assertThat(timeoutEmulator.connectionsEstablished - connectionsEstablishedBeforeLoosing)
                            .describedAs("Expected at least one registered reconnection")
                            .isGreaterThan(0);
                }
            }
        }
    }

    /**
     * DIRECT-82254
     * <p>
     * Если mysql пропал совсем, не стоит бесконечно пытаться пересоединиться к нему.
     */
    @Test
    public void noInfiniteAttemptsToReconnect() throws InterruptedException, SQLException {
        MySQLServerBuilder mysqlBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                        .setGracefulStopTimeout(Duration.ZERO)
                        .setServerId(MYSQL_SERVER_ID)
                        .withNoSync(true)
                );

        BinlogReader binlogReader = null;
        try {
            // Стартуем mysql-сервер источник для тестов. С него будем читать бинлоги.
            try (TmpMySQLServerWithDataDir mysql = TmpMySQLServerWithDataDir
                    .createWithBinlog("mysql", mysqlBuilder.copy())) {
                mysql.awaitConnectivity(Duration.ofSeconds(60));

                // Создаем на сервере-источнике тестовую БД и таблицу.
                // В ней будем делать разные изменения, которые должны отражаться в бинлоге.
                try (Connection conn = mysql.connect()) {
                    MySQLUtils.executeUpdate(conn, "CREATE DATABASE " + MySQLUtils.quoteName(TEST_DB_NAME));
                    conn.setCatalog(TEST_DB_NAME);
                    MySQLUtils.executeUpdate(conn, "CREATE TABLE table1 (a int, b int default -1)");
                }

                // Читаем состояние сервера-источника (схему БД и gtid-set).
                BinlogSource source = new BinlogSource("test", mysql);
                BinlogStateOptimisticSnapshotter snapshotter = new BinlogStateOptimisticSnapshotter(MYSQL_SERVER_ID);
                StatefulBinlogSource statefulSource = new StatefulBinlogSource(
                        source,
                        snapshotter.snapshot(source)
                );

                binlogReader = new BinlogReader(
                        statefulSource,
                        mysqlBuilder.copy(),
                        null,
                        Duration.ofSeconds(60),
                        MYSQL_SERVER_ID,
                        32);
            }

            final BinlogReader binlogReaderRef = binlogReader;
            softly.assertThatCode(() -> binlogReaderRef.readEvent(Duration.ofSeconds(60)))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasRootCauseExactlyInstanceOf(ConnectException.class)
                    .hasMessageContaining("Failed to connect to MySQL");
        } finally {
            if (binlogReader != null) {
                binlogReader.close();
            }
        }
    }

    @Test
    public void expectedGtid() throws InterruptedException, SQLException {
        String sourceName = "ppc:1";
        MySQLServerBuilder mysqlBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                        .setGracefulStopTimeout(Duration.ZERO)
                        .setServerId(MYSQL_SERVER_ID)
                        .withNoSync(true)
                );

        // Стартуем mysql-сервер источник для тестов. С него будем читать бинлоги.
        try (TmpMySQLServerWithDataDir mysql = TmpMySQLServerWithDataDir
                .createWithBinlog("mysql", mysqlBuilder.copy())
        ) {
            mysql.awaitConnectivity(Duration.ofSeconds(60));

            StatefulBinlogSource binlogSource;

            // Создаем на сервере-источнике тестовую БД и таблицу.
            // В ней будем делать разные изменения, которые должны отражаться в бинлоге.
            try (Connection conn = mysql.connect()) {
                MySQLUtils.executeUpdate(conn, "CREATE DATABASE " + MySQLUtils.quoteName(TEST_DB_NAME));
                conn.setCatalog(TEST_DB_NAME);
                MySQLUtils.executeUpdate(conn, "CREATE TABLE table1 (a int)");
            }

            try (Connection conn = mysql.connect()) {
                conn.setCatalog(TEST_DB_NAME);
                MySQLUtils.executeUpdate(conn, "BEGIN");
                MySQLUtils.executeUpdate(conn, "INSERT INTO table1 VALUES (100)");
                MySQLUtils.executeUpdate(conn, "COMMIT");
            }

            binlogSource = new StatefulBinlogSource(
                    new BinlogSource(sourceName, mysql),
                    new BinlogStateOptimisticSnapshotter(MYSQL_SERVER_ID).snapshot(mysql));

            try (Connection conn = mysql.connect()) {
                conn.setCatalog(TEST_DB_NAME);
                MySQLUtils.executeUpdate(conn, "BEGIN");
                MySQLUtils.executeUpdate(conn, "INSERT INTO table1 VALUES (200)");
                MySQLUtils.executeUpdate(conn, "COMMIT");
            }

            try (
                    // Инициализируем читалку бинлога
                    BinlogReader reader = new BinlogReader(
                            binlogSource,
                            new DirectMysqlDb(TestMysqlConfig.directConfig())
                                    .useSandboxMysqlServerIfPossible(
                                            new MySQLServerBuilder()
                                                    .setGracefulStopTimeout(Duration.ZERO)
                                                    .addExtraArgs(Collections.singletonList("--skip-innodb-use-native" +
                                                            "-aio"))
                                    ),
                            null,
                            Duration.ofSeconds(5),
                            MYSQL_SERVER_ID,
                            32
                    )
            ) {
                TransactionReader txReader = new TransactionReader(reader);
                StateBound<Transaction> tx = txReader.readTransaction(Duration.ofSeconds(5));

                softly.assertThat(tx.getData().getQueries().get(0).getQueryString())
                        .isEqualTo("INSERT INTO table1 VALUES (200)");
            }
        }
    }

    @ParametersAreNonnullByDefault
    private static class ByteLimitedManInTheMiddle implements SocketProxy.ManInTheMiddle {
        private final Map<SocketChannel, Integer> bytesRemainsBySocket = new WeakHashMap<>();
        private final SocketAddress upstreamAddress;
        private boolean enabled = false;
        private int initialBytesRemains = 10_000;
        private int forcedDisconnects = 0;

        private ByteLimitedManInTheMiddle(SocketAddress upstreamAddress) {
            this.upstreamAddress = upstreamAddress;
        }

        void enable() {
            enabled = true;
        }

        @Override
        public boolean connectionRequested(SocketChannel source) {
            return true;
        }

        @Override
        public Pair<byte[], Boolean> transferRequested(SocketChannel source, SocketChannel sink, byte[] data) {
            boolean keep = true;
            if (enabled && Checked.get(source::getRemoteAddress).equals(upstreamAddress)) {
                int bytesRemained = bytesRemainsBySocket.getOrDefault(sink, initialBytesRemains);
                if (bytesRemained >= data.length) {
                    bytesRemained -= data.length;
                    keep = bytesRemained != 0;
                } else {
                    byte[] newData = new byte[bytesRemained];
                    while (bytesRemained > 0) {
                        --bytesRemained;
                        newData[bytesRemained] = data[bytesRemained];
                    }
                    data = newData;
                    keep = false;
                }
                if (bytesRemained > 0) {
                    bytesRemainsBySocket.put(sink, bytesRemained);
                }
            }
            if (!keep) {
                ++forcedDisconnects;
                initialBytesRemains += 3_456;
            }
            return Pair.of(data, keep);
        }
    }

    @ParametersAreNonnullByDefault
    private static class DataLoosingManInTheMiddle implements SocketProxy.ManInTheMiddle {
        int connectionsEstablished = 0;
        boolean loosingData = false;

        void startLoosingData() {
            loosingData = true;
        }

        @Override
        public boolean connectionRequested(SocketChannel source) {
            loosingData = false;
            ++connectionsEstablished;
            return true;
        }

        @Override
        public Pair<byte[], Boolean> transferRequested(SocketChannel source, SocketChannel sink, byte[] data) {
            if (loosingData) {
                return Pair.of(new byte[0], true);
            } else {
                return Pair.of(data, true);
            }
        }
    }
}
