package ru.yandex.market.logshatter;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseConnectionImpl;
import ru.yandex.clickhouse.ClickHouseStatement;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.health.configs.logshatter.LogBatch;
import ru.yandex.market.logshatter.parser.TableDescription;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class LogShatterServiceCatchRequestTest {

    @Test
    @Ignore
    public void saveOneLogBatchSuccess() throws Exception {
        ClickHouseConnection connection = new ClickHouseConnectionImpl("jdbc:clickhouse://127.0.0.1:8123");
        ClickHouseStatement statement = connection.createStatement();
        List<Column> columns = List.of(
            new Column("date", ColumnType.Date),
            new Column("timestamp", ColumnType.UInt32),
            new Column("_time", ColumnType.DateTime),
            new Column("_layer", ColumnType.LowCardinalityString),
            new Column("_service", ColumnType.String),
            new Column("_version", ColumnType.NullableString),
            new Column("_branch", ColumnType.NullableString),
            new Column("_canary", ColumnType.NullableUInt8),
            new Column("_host", ColumnType.NullableString),
            new Column("_level", ColumnType.LowCardinalityString),
            new Column("_request_id", ColumnType.String),
            new Column("_uuid", ColumnType.UUID),
            new Column("_context", ColumnType.NullableString),
            new Column("_thread", ColumnType.NullableString),
            new Column("_message", ColumnType.String),
            new Column("_rest", ColumnType.NullableString),
            new Column("_dc", ColumnType.LowCardinalityString),
            new Column("_allocation_id", ColumnType.NullableString),
            new Column("_container_id", ColumnType.NullableString),
            new Column("_time_nano", ColumnType.UInt64)
        );

        String sql = "INSERT INTO logs.logs_shard_v2 (" +
            "date, " +
            "timestamp, " +
            "_time, " +
            "_layer, " +
            "_service, " +
            "_version, " +
            "_branch, " +
            "_canary, " +
            "_host, " +
            "_level, " +
            "_request_id, " +
            "_uuid, " +
            "_context, " +
            "_thread, " +
            "_message, " +
            "_rest, " +
            "_dc, " +
            "_allocation_id, " +
            "_container_id, " +
            "_time_nano)";

        List<LogBatch> logBatches = List.of(
            new LogBatch(Stream.empty(), 0, 0, 0, Duration.ofMillis(0), columns, "sourceName", "sourceHost", null)
        );

        OffsetDateTime dateTime = LocalDateTime.parse("2022-03-19T00:00:00").atOffset(ZoneOffset.ofHours(3));
        logBatches.get(0).write(
            new Date(dateTime.toEpochSecond() * 1_000L),
            new Date(dateTime.toEpochSecond() * 1000L), // _time
            "prod", // _layer
            "eats-catalog", // _service
            "", // _version
            "", // _branch
            0, // _canary
            "a.b-c.d", // _host
            "INFO", // _level
            "637231b143884d269c16ca93e9ac0753", // _request_id
            UUID.fromString("00000000-0000-0000-0000-000000000000"), // _uuid
            "GetValue ( libraries/experiments3/src/experiments3/models/clients_cache_impl.cpp:884 ) ", // _context
            "trace_id", // _thread
            "text", // _message
            "", // _rest
            "dc", // _dc
            "", // _allocation_id
            "", // _container_id
            dateTime.toEpochSecond() * 1_000_000_000 // _time_nano
        );
        dateTime = LocalDateTime.parse("2022-03-19T00:00:01").atOffset(ZoneOffset.ofHours(3));
        logBatches.get(0).write(
            new Date(dateTime.toEpochSecond() * 1_000L),
            new Date(dateTime.toEpochSecond() * 1000L), // _time
            "prod", // _layer
            "eats-catalog", // _service
            "a", // _version
            "a", // _branch
            0, // _canary
            "", // _host
            "INFO", // _level
            "637231b143884d269c16ca93e9ac0753", // _request_id
            UUID.fromString("00000000-0000-0000-0000-000000000000"), // _uuid
            "GetValue ( libraries/experiments3/src/experiments3/models/clients_cache_impl.cpp:884 ) ", // _context
            "trace_id", // _thread
            "text", // _message
            "a", // _rest
            "dc", // _dc
            "a", // _allocation_id
            "a", // _container_id
            dateTime.toEpochSecond() * 1_000_000_000 // _time_nano
        );
        logBatches.get(0).onParseComplete(Duration.ofMillis(1000), 2, 0, 0);


        statement.sendNativeStream(sql, stream -> {
            for (LogBatch logBatch : logBatches) {
                logBatch.writeTo(stream, columns);
            }

            System.out.println("smth");
        });
    }

    @Test
    @Ignore
    public void writeNullFieldsAndGetError() throws SQLException {
        ClickHouseConnection connection = new ClickHouseConnectionImpl("jdbc:clickhouse://127.0.0.1:8123");
        ClickHouseStatement statement = connection.createStatement();
        List<Column> columns = List.of(
            new Column("date", ColumnType.Date),
            new Column("timestamp", ColumnType.UInt32),
            new Column("_time", ColumnType.DateTime),
            new Column("_layer", ColumnType.LowCardinalityString),
            new Column("_service", ColumnType.String),
            new Column("_version", ColumnType.NullableString),
            new Column("_branch", ColumnType.NullableString),
            new Column("_canary", ColumnType.NullableUInt8),
            new Column("_host", ColumnType.NullableString),
            new Column("_level", ColumnType.LowCardinalityString),
            new Column("_request_id", ColumnType.String),
            new Column("_uuid", ColumnType.UUID),
            new Column("_context", ColumnType.NullableString),
            new Column("_thread", ColumnType.NullableString),
            new Column("_message", ColumnType.String),
            new Column("_rest", ColumnType.NullableString),
            new Column("_dc", ColumnType.LowCardinalityString),
            new Column("_allocation_id", ColumnType.NullableString),
            new Column("_container_id", ColumnType.NullableString),
            new Column("_time_nano", ColumnType.UInt64)
        );

//        FileOutputStream stream = new FileOutputStream("./lsh_query");

        String sql = "INSERT INTO logs.logs_shard_v2 (date, timestamp, _time, _layer, _service, _version, _branch, " +
            "_canary, _host, " +
            "_level, _request_id, _uuid, _context, _thread, _message, _rest, _dc, _allocation_id, _container_id, " +
            "_time_nano)";

        List<LogBatch> logBatches = List.of(
            new LogBatch(Stream.empty(), 0, 0, 0, Duration.ofMillis(0), columns, "sourceName", "sourceHost", null)
        );

        OffsetDateTime dateTime = LocalDateTime.parse("2022-03-19T00:00:00").atOffset(ZoneOffset.ofHours(3));
        try {
            logBatches.get(0).write(
                new Date(dateTime.toEpochSecond() * 1_000L),
                new Object[]{null});

            logBatches.get(0).onParseComplete(Duration.ofMillis(1000), 2, 0, 0);

            statement.sendNativeStream(sql, stream -> {
                for (LogBatch logBatch : logBatches) {
                    logBatch.writeTo(stream, columns);
                }
            });
            fail("null field saved");
        } catch (IllegalArgumentException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    @Ignore
    public void saveTwoLogBatchesSuccess() throws Exception {
        ClickHouseConnection connection = new ClickHouseConnectionImpl("jdbc:clickhouse://127.0.0.1:8123");
        ClickHouseStatement statement = connection.createStatement();
        List<Column> columns = List.of(
            new Column("date", ColumnType.Date),
            new Column("timestamp", ColumnType.UInt32),
            new Column("_time", ColumnType.DateTime),
            new Column("_layer", ColumnType.LowCardinalityString),
            new Column("_service", ColumnType.String),
            new Column("_version", ColumnType.NullableString),
            new Column("_branch", ColumnType.NullableString),
            new Column("_canary", ColumnType.NullableUInt8),
            new Column("_host", ColumnType.NullableString),
            new Column("_level", ColumnType.LowCardinalityString),
            new Column("_request_id", ColumnType.String),
            new Column("_uuid", ColumnType.UUID),
            new Column("_context", ColumnType.NullableString),
            new Column("_thread", ColumnType.NullableString),
            new Column("_message", ColumnType.String),
            new Column("_rest", ColumnType.NullableString),
            new Column("_dc", ColumnType.LowCardinalityString),
            new Column("_allocation_id", ColumnType.NullableString),
            new Column("_container_id", ColumnType.NullableString),
            new Column("_time_nano", ColumnType.UInt64)
        );

        String databaseName = "logs";
        String tableName = "logs_shard_v2";
        TableDescription tableDescription = initDatabase(statement, databaseName, tableName, columns);
        String sql = String.format("INSERT INTO %s.%s %s",
            databaseName, tableName,
            tableDescription.getColumns().stream().map(Column::getName).collect(Collectors.joining(", ", "(", ")"))
        );

        List<LogBatch> logBatches = List.of(
            new LogBatch(Stream.empty(), 0, 0, 0, Duration.ofMillis(0), columns, "sourceName", "sourceHost", null),
            new LogBatch(Stream.empty(), 0, 0, 0, Duration.ofMillis(0), columns, "sourceName", "sourceHost", null)
        );

        OffsetDateTime dateTime = LocalDateTime.parse("2022-03-19T00:00:00").atOffset(ZoneOffset.ofHours(3));
        logBatches.get(0).write(
            new Date(dateTime.toEpochSecond() * 1_000L),
            new Date(dateTime.toEpochSecond() * 1000L), // _time
            "prod", // _layer
            "eats-catalog", // _service
            null, // _version
            null, // _branch
            0, // _canary
            "a", // _host
            "INFO", // _level
            "637231b143884d269c16ca93e9ac0753", // _request_id
            UUID.fromString("00000000-0000-0000-0000-000000000000"), // _uuid
            "GetValue ( libraries/experiments3/src/experiments3/models/clients_cache_impl.cpp:884 ) ", // _context
            "trace_id", // _thread
            "text", // _message
            "", // _rest
            "dc", // _dc
            "", // _allocation_id
            "", // _container_id
            dateTime.toEpochSecond() * 1_000_000_000 // _time_nano
        );
        dateTime = LocalDateTime.parse("2022-03-19T00:00:01").atOffset(ZoneOffset.ofHours(3));
        logBatches.get(0).write(
            new Date(dateTime.toEpochSecond() * 1_000L),
            new Date(dateTime.toEpochSecond() * 1000L), // _time
            "prod", // _layer
            "eats-catalog", // _service
            null, // _version
            null, // _branch
            0, // _canary
            "b", // _host
            "INFO", // _level
            "637231b143884d269c16ca93e9ac0753", // _request_id
            UUID.fromString("00000000-0000-0000-0000-000000000000"), // _uuid
            "GetValue ( libraries/experiments3/src/experiments3/models/clients_cache_impl.cpp:884 ) ", // _context
            "trace_id", // _thread
            "text", // _message
            "", // _rest
            "dc", // _dc
            "", // _allocation_id
            "", // _container_id
            dateTime.toEpochSecond() * 1_000_000_000 // _time_nano
        );
        logBatches.get(0).onParseComplete(Duration.ofMillis(1000), 2, 0, 0);
        dateTime = LocalDateTime.parse("2022-03-19T00:00:02").atOffset(ZoneOffset.ofHours(3));
        logBatches.get(1).write(
            new Date(dateTime.toEpochSecond() * 1_000L),
            new Date(dateTime.toEpochSecond() * 1000L), // _time
            "prod", // _layer
            "eats-catalog", // _service
            "", // _version
            "", // _branch
            0, // _canary
            "c", // _host
            "INFO", // _level
            "637231b143884d269c16ca93e9ac0753", // _request_id
            UUID.fromString("00000000-0000-0000-0000-000000000000"), // _uuid
            "GetValue ( libraries/experiments3/src/experiments3/models/clients_cache_impl.cpp:884 ) ", // _context
            "trace_id", // _thread
            "text", // _message
            "", // _rest
            "dc", // _dc
            "", // _allocation_id
            "", // _container_id
            dateTime.toEpochSecond() * 1_000_000_000 // _time_nano
        );
        dateTime = LocalDateTime.parse("2022-03-19T00:00:03").atOffset(ZoneOffset.ofHours(3));
        logBatches.get(1).write(
            new Date(dateTime.toEpochSecond() * 1_000L),
            new Date(dateTime.toEpochSecond() * 1000L), // _time
            "prod", // _layer
            "eats-catalog", // _service
            "", // _version
            null, // _branch
            0, // _canary
            "d", // _host
            "INFO", // _level
            "637231b143884d269c16ca93e9ac0753", // _request_id
            UUID.fromString("00000000-0000-0000-0000-000000000000"), // _uuid
            "GetValue ( libraries/experiments3/src/experiments3/models/clients_cache_impl.cpp:884 ) ", // _context
            "trace_id", // _thread
            "text", // _message
            "", // _rest
            "dc", // _dc
            "", // _allocation_id
            "", // _container_id
            dateTime.toEpochSecond() * 1_000_000_000 // _time_nano
        );
        logBatches.get(1).onParseComplete(Duration.ofMillis(1000), 2, 0, 0);


        statement.sendNativeStream(sql, stream -> {
            for (LogBatch logBatch : logBatches) {
                logBatch.writeTo(stream, columns);
            }
        });
    }

    @Test
    @Ignore
    public void writeTwoNullFieldsWithOneError() throws Exception {
        ClickHouseConnection connection = new ClickHouseConnectionImpl("jdbc:clickhouse://127.0.0.1:8123");
        ClickHouseStatement statement = connection.createStatement();

        String databaseName = "test";
        String tableName = "corrupted";

        List<Column> dataColumns = List.of(
            new Column("corrupted", ColumnType.NullableString)
        );
        TableDescription tableDescription = initDatabase(statement, databaseName, tableName, dataColumns);


        String sql = String.format("INSERT INTO %s.%s %s",
            databaseName, tableName,
            tableDescription.getColumns().stream().map(Column::getName).collect(Collectors.joining(", ", "(", ")"))
        );

        List<LogBatch> logBatches = List.of(
            new LogBatch(Stream.empty(), 0, 0, 0, Duration.ofMillis(0), tableDescription.getColumns(), "sourceName",
                "sourceHost", null)
        );
        OffsetDateTime dateTime;
        try {
            dateTime = LocalDateTime.now().atOffset(ZoneOffset.ofHours(3));
            logBatches.get(0).write(
                new Date(dateTime.toEpochSecond() * 1_000L),
                null
            );
            fail("saved null fields, but it should be new Object[]{null}");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }

        dateTime = LocalDateTime.now().atOffset(ZoneOffset.ofHours(3));
        logBatches.get(0).write(
            new Date(dateTime.toEpochSecond() * 1_000L),
            new Object[]{null}
        );
        logBatches.get(0).onParseComplete(Duration.ofMillis(1000), 2, 0, 0);


        statement.sendNativeStream(sql, stream -> {
            for (LogBatch logBatch : logBatches) {
                logBatch.writeTo(stream, tableDescription.getColumns());
            }
        });
    }

    private TableDescription initDatabase(
        ClickHouseStatement statement,
        String databaseName, String tableName,
        List<Column> dataColumns
    ) throws SQLException {
        //language=SQL
        statement.execute(String.format("CREATE DATABASE IF NOT EXISTS %s", databaseName));

        TableDescription tableDescription = TableDescription.createDefault(
            dataColumns
        );

        String columnsList = tableDescription.getColumns().stream()
            .map(Column::getQuotedDll)
            .collect(Collectors.joining(", "));
        //language=SQL
        statement.execute(
            String.format(
                "CREATE TABLE IF NOT EXISTS %s.%s ( %s ) ENGINE = %s",
                databaseName, tableName, columnsList, tableDescription.getEngine().createEngineDDL()
            )
        );

        return tableDescription;
    }
}
