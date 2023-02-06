package ru.yandex.direct.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.RowsQueryEventData;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.mysql.schema.ServerSchema;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Helper class for manually checking that binlog streaming works
 * <p>
 * Requires mysql 5.7.6+, on OSX may be installed with brew install percona-server
 */
public class CheckReplicationLog {
    private static final Logger logger = LoggerFactory.getLogger(CheckReplicationLog.class);

    @Test
    public void test() throws IOException, InterruptedException, SQLException, TimeoutException {
        MySQLServerBuilder serverBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder());
        assumeTrue(serverBuilder.mysqldIsAvailable());

        try (
                TmpMySQLServerWithDataDir master = TmpMySQLServerWithDataDir
                        .createWithBinlog("master", serverBuilder.copy().setServerId(1))
        ) {
            try (Connection mconn = master.connect()) {
                MySQLUtils.executeUpdate(mconn, "CREATE DATABASE foobar");
                mconn.setCatalog("foobar");
                MySQLUtils.executeUpdate(mconn,
                        "CREATE TABLE tbl1 (id int not null primary key auto_increment, value int not null)");
                MySQLUtils.executeUpdate(mconn, "INSERT INTO tbl1 (value) values (1), (2), (42)");
                final List<String> events = new ArrayList<>();

                try (
                        TmpMySQLServerWithDataDir mysqlSchemaReplicator =
                                MySQLBinlogDataStreamer.createMySQL(serverBuilder.copy());
                        AsyncStreamer asyncStreamer = new AsyncStreamer(
                                new MySQLBinlogDataStreamer(
                                        new MySQLBinlogDataClientProvider(
                                                master.getHost(),
                                                master.getPort(),
                                                master.getUsername(),
                                                master.getPassword(),
                                                99
                                        ),
                                        // use the slave connection to make a consistent snapshot
                                        MySQLBinlogState.snapshot(mconn)
                                ),
                                new LastQueryWatcher(new MySQLBinlogConsumer() {
                                    @Override
                                    public void onConnect(MySQLBinlogDataStreamer streamer) {
                                        logger.info("connected");
                                    }

                                    @Override
                                    public void onDisconnect(MySQLBinlogDataStreamer streamer) {
                                        logger.info("disconnected");
                                    }

                                    @Override
                                    public void onDDL(String gtid, QueryEventData data, ServerSchema before,
                                                      ServerSchema after) {
                                        events.add("DDL: " + data.getSql());
                                    }

                                    @Override
                                    public void onTransactionBegin(String gtid) {
                                        events.add("BEGIN");
                                    }

                                    @Override
                                    public void onRowsQuery(RowsQueryEventData data, long timestamp) {
                                        events.add("ROWS_QUERY");
                                    }

                                    @Override
                                    public void onInsertRows(MySQLSimpleData data) {
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
                                        events.add(sb.toString());
                                    }

                                    @Override
                                    public void onUpdateRows(MySQLUpdateData data) {
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
                                        events.add(sb.toString());
                                    }

                                    @Override
                                    public void onDeleteRows(MySQLSimpleData data) {
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
                                        events.add(sb.toString());
                                    }

                                    @Override
                                    public void onTransactionCommit(String gtid) {
                                        events.add("COMMIT");
                                    }
                                }),
                                mysqlSchemaReplicator
                        )
                ) {
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

                    logger.info("wait for operations to settle in binlog...");
                    ((LastQueryWatcher) asyncStreamer.getConsumer()).waitForQuery(
                            5,
                            "INSERT INTO tbl2 (value) VALUES ('foo'), ('bar'), ('foo'), ('baz')",
                            Duration.ofSeconds(30)
                    );
                }

                for (String event : events) {
                    logger.info("Captured: {}", event);
                }
                List<String> expected = new ArrayList<>();
                expected.add("BEGIN");
                expected.add("ROWS_QUERY");
                // В формате бинлога noblob в запрос UPDATE включаются даже те поля, которые не были изменены.
                expected.add("UPDATE tbl1\n"
                        + "    SET id=1 value=34 WHERE id=1 value=1\n"
                        + "    SET id=2 value=35 WHERE id=2 value=2\n"
                        + "    SET id=3 value=75 WHERE id=3 value=42");
                expected.add("COMMIT");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY");
                expected.add("UPDATE tbl1\n"
                        + "    SET id=777 value=35 WHERE id=2 value=35");
                expected.add("COMMIT");
                expected.add("DDL: ALTER TABLE tbl1 ADD COLUMN cool int not null default 555");
                expected.add("DDL: ALTER TABLE tbl1 CHANGE COLUMN value data int not null");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY");
                // В формате бинлога noblob в запрос INSERT включаются даже те поля, которые не были указаны.
                expected.add("INSERT INTO tbl1\n"
                        + "    ROW id=4 data=98 cool=555");
                expected.add("COMMIT");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY");
                // В формате бинлога noblob в запрос DELETE включаются даже те поля, которые не были указаны в WHERE.
                expected.add("DELETE FROM tbl1\n"
                        + "    WHERE id=777 data=35 cool=555");
                expected.add("COMMIT");
                expected.add(
                        "DDL: CREATE TABLE tbl2 (id int not null primary key auto_increment, value enum('foo', 'bar'," +
                                " 'baz'))");
                expected.add("BEGIN");
                expected.add("ROWS_QUERY");
                expected.add("INSERT INTO tbl2\n"
                        + "    ROW id=1 value=1\n"
                        + "    ROW id=2 value=2\n"
                        + "    ROW id=3 value=1\n"
                        + "    ROW id=4 value=3");
                expected.add("COMMIT");
                assertThat(events, beanDiffer(expected));
            }
        }
    }
}
