package ru.yandex.direct.binlog.reader;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.direct.mysql.BinlogEvent;
import ru.yandex.direct.mysql.BinlogEventData;
import ru.yandex.direct.mysql.BinlogEventType;
import ru.yandex.direct.mysql.JunitRuleMySQLServerCreator;
import ru.yandex.direct.mysql.MySQLBinlogState;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.mysql.MySQLUtils;
import ru.yandex.direct.mysql.TmpMySQLServerWithDataDir;
import ru.yandex.direct.mysql.schema.TableSchema;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;
import ru.yandex.direct.utils.Checked;

@ParametersAreNonnullByDefault
public class TransactionReaderTest {
    private static final int MYSQL_SERVER_ID = 218735;
    private static final String TEST_DB_NAME = "trans_reader_test";
    private static final List<String> COMMON_QUERIES = List.of(
            "BEGIN",
            "INSERT INTO table1 (a, b) values (1, 2), (3, 4)",
            "INSERT INTO table1 (a, b) values (5, 6), (7, 8)",
            "COMMIT",

            "BEGIN",
            "INSERT INTO table1 (a, b) values (777, 999)",
            "ROLLBACK",

            "CREATE TABLE table2 (a int not null)",

            "BEGIN",
            "INSERT INTO table1 (a, b) values (9, 10)",
            "COMMIT"
    );

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Rule
    public Timeout timeoutRule = new Timeout(3, TimeUnit.MINUTES);

    @Rule
    public JunitRuleMySQLServerCreator mySQLServerCreator = new JunitRuleMySQLServerCreator();

    private static List<String> getQueriesFromTransaction(Transaction transaction) {
        return transaction.getEnrichedEvents()
                .map(e -> e.getQuery().getQueryString())
                .collect(Collectors.toList());
    }

    private <E extends Exception> void insideTransactionReader(
            Checked.CheckedConsumer<TransactionReader, E> transactionReaderConsumer,
            List<String> queries,
            @Nullable Integer maxEventsPerTransaction)
            throws InterruptedException, SQLException {
        MySQLServerBuilder mysqlBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                        .setGracefulStopTimeout(Duration.ZERO)
                        .setServerId(MYSQL_SERVER_ID)
                        .withNoSync(true));

        TmpMySQLServerWithDataDir mysql = mySQLServerCreator.createWithBinlog(TEST_DB_NAME, mysqlBuilder.copy());
        mysql.awaitConnectivity(Duration.ofSeconds(60));

        MySQLBinlogState startState;
        try (Connection conn = mysql.connect()) {
            MySQLUtils.executeUpdate(conn, "CREATE DATABASE " + MySQLUtils.quoteName(TEST_DB_NAME));
            conn.setCatalog(TEST_DB_NAME);
            MySQLUtils.executeUpdate(conn, "CREATE TABLE table1 (a int, b int default -1)");

            startState = MySQLBinlogState.snapshot(conn);

            for (String query: queries) {
                MySQLUtils.executeUpdate(conn, query);
            }
        }

        try (BinlogReader binlogReader = new BinlogReader(
                new StatefulBinlogSource(new BinlogSource("test", mysql), startState),
                mysqlBuilder.copy(),
                null,
                Duration.ofSeconds(30),
                MYSQL_SERVER_ID,
                100)) {
            TransactionReader transactionReader = new TransactionReader(binlogReader)
                    .withMaxEventsPerTransaction(maxEventsPerTransaction);
            Checked.run(() -> transactionReaderConsumer.accept(transactionReader));
        }
    }

    @Test
    public void readTransaction() throws InterruptedException, SQLException {
        insideTransactionReader(transactionReader -> {
            softly.assertThat(
                    getQueriesFromTransaction(transactionReader.readTransaction(Duration.ofSeconds(10)).getData()))
                    .describedAs("First transaction with two insert queries")
                    .containsExactly(
                            "INSERT INTO table1 (a, b) values (1, 2), (3, 4)",
                            "INSERT INTO table1 (a, b) values (5, 6), (7, 8)");

            // ROLLBACK ignored
            // CREATE TABLE ignored

            softly.assertThat(
                    getQueriesFromTransaction(transactionReader.readTransaction(Duration.ofSeconds(10)).getData()))
                    .describedAs("Second transaction with one insert query")
                    .containsExactly(
                            "INSERT INTO table1 (a, b) values (9, 10)");
        }, COMMON_QUERIES, null);
    }

    @Test
    public void readTransactionOrDdl() throws InterruptedException, SQLException {
        insideTransactionReader(transactionReader -> {
            StateBound<TransactionReader.TransactionOrDdl> transactionOrDdl;

            transactionOrDdl = transactionReader.readTransactionOrDdl(Duration.ofSeconds(10));
            softly.assertThat(transactionOrDdl.getData().getTransaction())
                    .describedAs("First transaction with two insert queries")
                    .isNotNull();
            softly.assertThat(transactionOrDdl.getData().getDdl())
                    .describedAs("First transaction with two insert queries")
                    .isNull();
            if (transactionOrDdl.getData().getTransaction() != null) {
                softly.assertThat(getQueriesFromTransaction(transactionOrDdl.getData().getTransaction()))
                        .describedAs("First transaction with two insert queries")
                        .containsExactly(
                                "INSERT INTO table1 (a, b) values (1, 2), (3, 4)",
                                "INSERT INTO table1 (a, b) values (5, 6), (7, 8)");
            }

            // ROLLBACK ignored

            transactionOrDdl = transactionReader.readTransactionOrDdl(Duration.ofSeconds(10));
            softly.assertThat(transactionOrDdl.getData().getTransaction())
                    .describedAs("CREATE TABLE")
                    .isNull();
            softly.assertThat(transactionOrDdl.getData().getDdl() != null)
                    .describedAs("CREATE TABLE")
                    .isNotNull();
            if (transactionOrDdl.getData().getDdl() != null) {
                BinlogEvent event = transactionOrDdl.getData().getDdl();
                softly.assertThat(event.getType())
                        .describedAs("CREATE TABLE")
                        .isEqualTo(BinlogEventType.DDL);
                softly.assertThat((Object) event.getData())
                        .describedAs("CREATE TABLE")
                        .isInstanceOf(BinlogEventData.DDL.class);
                if (event.getData() instanceof BinlogEventData.DDL) {
                    List<TableSchema> oldTables = ((BinlogEventData.DDL) event.getData())
                            .getBefore().getDatabases().get(0).getTables();
                    softly.assertThat(oldTables)
                            .describedAs("Before this query was one table")
                            .size()
                            .isEqualTo(1);
                    List<TableSchema> newTables = ((BinlogEventData.DDL) event.getData())
                            .getAfter().getDatabases().get(0).getTables();
                    softly.assertThat(newTables)
                            .describedAs("After this query exists two tables")
                            .size()
                            .isEqualTo(2);
                    if (newTables.size() == 2) {
                        TableSchema newTable = newTables.get(1);
                        softly.assertThat(newTable.getName())
                                .isEqualTo("table2");
                        softly.assertThat(newTable.getColumns())
                                .extracting("name")
                                .containsExactly("a");
                        softly.assertThat(newTable.getColumns())
                                .extracting("dataType")
                                .containsExactly("int");
                        softly.assertThat(newTable.getColumns())
                                .extracting("nullable")
                                .containsExactly(false);
                    }
                }
            }

            transactionOrDdl = transactionReader.readTransactionOrDdl(Duration.ofSeconds(10));
            softly.assertThat(transactionOrDdl.getData().getTransaction())
                    .describedAs("Second transaction with one insert query")
                    .isNotNull();
            softly.assertThat(transactionOrDdl.getData().getDdl())
                    .describedAs("Second transaction with one insert query")
                    .isNull();
            if (transactionOrDdl.getData().getTransaction() != null) {
                softly.assertThat(getQueriesFromTransaction(transactionOrDdl.getData().getTransaction()))
                        .describedAs("Second transaction with one insert query")
                        .containsExactly(
                                "INSERT INTO table1 (a, b) values (9, 10)");
            }
        }, COMMON_QUERIES, null);
    }

    @Test
    public void readLongTransaction() throws InterruptedException, SQLException {
        List<String> queries = new ArrayList<>();

        int maxEventsPerTransaction = 2;
        int rowsNumber = 1995;
        queries.add("BEGIN");
        for (int i = 0; i < rowsNumber; ++i) {
            queries.add("INSERT INTO table1 (a, b) values (" + i + ", " + i + ")");
        }
        queries.add("COMMIT");

        queries.add("BEGIN");
        queries.add("INSERT INTO table1 SELECT * FROM table1");
        queries.add("INSERT INTO table1 SELECT * FROM table1");
        queries.add("INSERT INTO table1 (a, b) values (123, 456)");
        queries.add("COMMIT");

        insideTransactionReader(transactionReader -> {
            StateBound<TransactionReader.TransactionOrDdl> transactionOrDdl;
            int rowsDone = 0;
            while (rowsDone < rowsNumber) {
                transactionOrDdl = transactionReader.readTransactionOrDdl(Duration.ofSeconds(10));
                softly.assertThat(transactionOrDdl.getData().getTransaction()).isNotNull();
                softly.assertThat(transactionOrDdl.getData().getDdl()).isNull();
                List<EnrichedEvent> events = transactionOrDdl.getData().getTransaction()
                        .getEnrichedEvents()
                        .collect(Collectors.toList());

                softly.assertThat(events.size()).isLessThanOrEqualTo(2);
                for (EnrichedEvent event : events) {
                    softly.assertThat(event.getQuery().getQueryString())
                            .isEqualTo("INSERT INTO table1 (a, b) values (" + rowsDone + ", " + rowsDone + ")");
                    softly.assertThat(event.getQuerySerial()).isEqualTo(rowsDone);
                    softly.assertThat(event.getEventSerial()).isEqualTo(rowsDone);
                    ++rowsDone;
                }
            }

            rowsDone = 0;
            int eventsCounter = 0;
            while (rowsDone < rowsNumber * 3 + 1) {
                transactionOrDdl = transactionReader.readTransactionOrDdl(Duration.ofSeconds(10));
                softly.assertThat(transactionOrDdl.getData().getTransaction()).isNotNull();
                softly.assertThat(transactionOrDdl.getData().getDdl()).isNull();
                List<EnrichedEvent> events = transactionOrDdl.getData().getTransaction()
                        .getEnrichedEvents()
                        .collect(Collectors.toList());

                softly.assertThat(events.size()).isLessThanOrEqualTo(2);
                for (EnrichedEvent event : events) {
                    if (rowsDone == rowsNumber * 3) {
                        softly.assertThat(event.getQuery().getQueryString())
                                .isEqualTo("INSERT INTO table1 (a, b) values (123, 456)");
                        softly.assertThat(event.getQuerySerial()).isEqualTo(2);
                        softly.assertThat(event.getEventSerial()).isEqualTo(eventsCounter);
                    }
                    rowsDone += ((BinlogEventData.Insert) event.getEvent().getData()).getRows().size();
                    ++eventsCounter;
                }
            }
        }, queries, maxEventsPerTransaction);
    }
}
