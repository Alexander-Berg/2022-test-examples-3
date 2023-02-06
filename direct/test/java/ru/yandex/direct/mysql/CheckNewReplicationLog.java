package ru.yandex.direct.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class CheckNewReplicationLog {
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Logger logger = LoggerFactory.getLogger(CheckNewReplicationLog.class);

    private static String translateEvent(BinlogEvent event) {
        switch (event.getType()) {
            case DDL:
                return "DDL: " + event.<BinlogEventData.DDL>getData().getData().getSql();
            case BEGIN:
                return "BEGIN";
            case DML:
                return "DML: " + event.<BinlogEventData.DML>getData().getData().getSql();
            case ROWS_QUERY:
                return "ROWS_QUERY: " + event.<BinlogEventData.RowsQuery>getData().getData().getQuery();
            case INSERT: {
                BinlogEventData.Insert data = event.getData();
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO ");
                sb.append(data.getTableMap().getTable());
                for (MySQLSimpleRow row : data.getRows()) {
                    sb.append("\n    ROW");
                    for (MySQLColumnData entry : row) {
                        sb.append(" ");
                        sb.append(entry.getSchema().getName());
                        sb.append("=");
                        sb.append(entry.getRawValue());
                    }
                }
                return sb.toString();
            }
            case UPDATE: {
                BinlogEventData.Update data = event.getData();
                StringBuilder sb = new StringBuilder();
                sb.append("UPDATE ");
                sb.append(data.getTableMap().getTable());
                for (MySQLUpdateRow row : data.getRows()) {
                    sb.append("\n    SET");
                    for (MySQLColumnData entry : row.getAfterUpdate()) {
                        sb.append(" ");
                        sb.append(entry.getSchema().getName());
                        sb.append("=");
                        sb.append(entry.getRawValue());
                    }
                    sb.append(" WHERE");
                    for (MySQLColumnData entry : row.getBeforeUpdate()) {
                        sb.append(" ");
                        sb.append(entry.getSchema().getName());
                        sb.append("=");
                        sb.append(entry.getRawValue());
                    }
                }
                return sb.toString();
            }
            case DELETE: {
                BinlogEventData.Delete data = event.getData();
                StringBuilder sb = new StringBuilder();
                sb.append("DELETE FROM ");
                sb.append(data.getTableMap().getTable());
                for (MySQLSimpleRow row : data.getRows()) {
                    sb.append("\n    WHERE");
                    for (MySQLColumnData entry : row) {
                        sb.append(" ");
                        sb.append(entry.getSchema().getName());
                        sb.append("=");
                        sb.append(entry.getRawValue());
                    }
                }
                return sb.toString();
            }
            case COMMIT:
                return "COMMIT";
            case ROLLBACK:
                return "ROLLBACK";
        }
        throw new IllegalArgumentException("Unexpected event: " + event);
    }

    @Test
    public void test() throws IOException, SQLException, InterruptedException {
        MySQLServerBuilder serverBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder());
        assumeTrue(serverBuilder.mysqldIsAvailable());

        try (
                TmpMySQLServerWithDataDir master = TmpMySQLServerWithDataDir
                        .createWithBinlog("master", serverBuilder.copy().setServerId(1))
        ) {
            try (Connection mconn = master.connect(CONNECT_TIMEOUT)) {
                MySQLUtils.executeUpdate(mconn, "CREATE DATABASE foobar");
                mconn.setCatalog("foobar");
                MySQLUtils.executeUpdate(mconn,
                        "CREATE TABLE tbl1 (id int not null primary key auto_increment, value int not null)");
                MySQLUtils.executeUpdate(mconn, "INSERT INTO tbl1 (value) values (1), (2), (42)");
                final MySQLBinlogState slaveSnapshot = MySQLBinlogState.snapshot(mconn);
                final String lastExpectedQuery = "INSERT INTO tbl4 () VALUES ()";
                CompletableFuture<List<String>> background = CompletableFuture.supplyAsync(() -> {
                    List<String> events = new ArrayList<>();
                    try (TmpMySQLServerWithDataDir mysqlSchemaReplicator = MySQLBinlogDataStreamer
                            .createMySQL(serverBuilder.copy())) {
                        BinlogEventConverter converter = new BinlogEventConverter(slaveSnapshot);
                        try (Connection mysqlSchemaConnection = mysqlSchemaReplicator.connect(CONNECT_TIMEOUT)) {
                            converter.attachSchemaConnection(mysqlSchemaConnection);
                            try (BinlogRawEventSource rawEventSource = new BinlogRawEventServerSource(
                                    master.getHost(),
                                    master.getPort(),
                                    master.getUsername(),
                                    master.getPassword(),
                                    99,
                                    converter.getState().getGtidSet(),
                                    16
                            )) {
                                converter.attachRawEventSource(rawEventSource);
                                BinlogEvent event;
                                boolean stopAfterCommit = false;
                                readLoop:
                                while ((event = converter.readEvent(1, TimeUnit.SECONDS)) != null) {
                                    events.add(translateEvent(event));
                                    switch (event.getType()) {
                                        case ROWS_QUERY:
                                            if (event.<BinlogEventData.RowsQuery>getData().getData().getQuery()
                                                    .equals(lastExpectedQuery)) {
                                                stopAfterCommit = true;
                                            }
                                            break;
                                        case COMMIT:
                                            if (stopAfterCommit) {
                                                break readLoop;
                                            }
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        throw new CompletionException(e);
                    }
                    return events;
                });
                try {
                    MySQLUtils.executeUpdate(mconn, "SET SESSION SQL_MODE='ALLOW_INVALID_DATES'");
                    MySQLUtils.executeUpdate(mconn, "UPDATE tbl1 SET value = value + 33");
                    MySQLUtils.executeUpdate(mconn, "UPDATE tbl1 SET id = 777 WHERE id = 2");
                    MySQLUtils.executeUpdate(mconn,
                            "ALTER TABLE tbl1 ADD COLUMN cool int not null default 555");
                    MySQLUtils.executeUpdate(mconn,
                            "ALTER TABLE tbl1 CHANGE COLUMN value data int not null");
                    MySQLUtils.executeUpdate(mconn, "INSERT INTO tbl1 (data) VALUES (98)");
                    MySQLUtils.executeUpdate(mconn, "DELETE FROM tbl1 WHERE id = 777");
                    MySQLUtils.executeUpdate(mconn,
                            "CREATE TABLE tbl2 (id int not null primary key auto_increment, value enum('foo', 'bar', " +
                                    "'baz'))");
                    MySQLUtils.executeUpdate(mconn,
                            "INSERT INTO tbl2 (value) VALUES ('foo'), ('bar'), ('foo'), ('baz')");
                    MySQLUtils.executeUpdate(mconn,
                            "ALTER TABLE tbl2 ALTER value SET DEFAULT 'bar'");
                    MySQLUtils.executeUpdate(mconn,
                            "INSERT INTO tbl2 () VALUES ()");
                    MySQLUtils.executeUpdate(mconn,
                            "CREATE TABLE tbl3 (value set('foo', 'bar', 'baz') default 'foo,baz')");
                    MySQLUtils.executeUpdate(mconn,
                            "INSERT INTO tbl3 (value) VALUES ('foo,bar'), ('bar,baz'), ('foo,baz')");
                    MySQLUtils.executeUpdate(mconn, "INSERT INTO tbl3 () VALUES ()");
                    MySQLUtils.executeUpdate(mconn,
                            "CREATE TABLE tbl4 (dt datetime not null default '0000-00-00 00:00:00')");
                    MySQLUtils.executeUpdate(mconn, "INSERT INTO tbl4 () VALUES ()");
                } catch (Throwable e) {
                    background.cancel(true);
                    throw e;
                }

                logger.info("Waiting for operations to settle in binlog...");
                List<String> events = background.join();

                for (String event : events) {
                    logger.info("Captured: {}", event);
                }
                List<String> expected = new ArrayList<>();
                expected.add("BEGIN");
                expected.add("ROWS_QUERY: UPDATE tbl1 SET value = value + 33");
                expected.add("UPDATE tbl1\n"
                        + "    SET id=1 value=34 WHERE id=1 value=1\n"
                        + "    SET id=2 value=35 WHERE id=2 value=2\n"
                        + "    SET id=3 value=75 WHERE id=3 value=42");
                expected.add("COMMIT");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY: UPDATE tbl1 SET id = 777 WHERE id = 2");
                expected.add("UPDATE tbl1\n"
                        + "    SET id=777 value=35 WHERE id=2 value=35");
                expected.add("COMMIT");
                expected.add("DDL: ALTER TABLE tbl1 ADD COLUMN cool int not null default 555");
                expected.add("DDL: ALTER TABLE tbl1 CHANGE COLUMN value data int not null");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY: INSERT INTO tbl1 (data) VALUES (98)");
                expected.add("INSERT INTO tbl1\n"
                        + "    ROW id=4 data=98 cool=555");
                expected.add("COMMIT");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY: DELETE FROM tbl1 WHERE id = 777");
                expected.add("DELETE FROM tbl1\n"
                        + "    WHERE id=777 data=35 cool=555");
                expected.add("COMMIT");
                expected.add(
                        "DDL: CREATE TABLE tbl2 (id int not null primary key auto_increment, value enum('foo', 'bar'," +
                                " 'baz'))");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY: INSERT INTO tbl2 (value) VALUES ('foo'), ('bar'), ('foo'), ('baz')");
                expected.add("INSERT INTO tbl2\n"
                        + "    ROW id=1 value=1\n"
                        + "    ROW id=2 value=2\n"
                        + "    ROW id=3 value=1\n"
                        + "    ROW id=4 value=3");
                expected.add("COMMIT");
                expected.add("DDL: ALTER TABLE tbl2 ALTER value SET DEFAULT 'bar'");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY: INSERT INTO tbl2 () VALUES ()");
                expected.add("INSERT INTO tbl2\n"
                        + "    ROW id=5 value=2");
                expected.add("COMMIT");
                expected.add(
                        "DDL: CREATE TABLE tbl3 (value set('foo', 'bar', 'baz') default 'foo,baz')");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY: INSERT INTO tbl3 (value) VALUES ('foo,bar'), ('bar,baz'), ('foo,baz')");
                expected.add("INSERT INTO tbl3\n"
                        + "    ROW value=3\n"
                        + "    ROW value=6\n"
                        + "    ROW value=5");
                expected.add("COMMIT");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY: INSERT INTO tbl3 () VALUES ()");
                expected.add("INSERT INTO tbl3\n"
                        + "    ROW value=5");
                expected.add("COMMIT");
                expected.add("DDL: CREATE TABLE tbl4 (dt datetime not null default '0000-00-00 00:00:00')");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY: INSERT INTO tbl4 () VALUES ()");
                expected.add("INSERT INTO tbl4\n"
                        + "    ROW dt=null");
                expected.add("COMMIT");
                assertThat(events, beanDiffer(expected));
            }
        }
    }
}
