package ru.yandex.market.logshatter;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.clickhouse.settings.ClickHouseProperties;
import ru.yandex.clickhouse.util.ClickHouseRowBinaryStream;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickhouse.ddl.enums.EnumColumnType;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Stream;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 01/06/2017
 */
public class LogBatchTest {
    @Test
    public void arraysTest() throws Exception {
        LogBatch batch = new LogBatch(
            Stream.empty(), 0, 0, 0, Duration.ofMillis(0),
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("array", ColumnType.ArrayInt32)
            ),
            "sourceName"
        );
        batch.write(new Date(1496130122000L), new Object[]{new Object[]{1, 2}});
        batch.write(new Date(1496130122000L), new Object[]{new Object[]{3}});
        batch.write(new Date(1496130122000L), new Object[]{new Object[]{4, 5, 6}});
        batch.onParseComplete(Duration.ZERO, 0, 0);

        check(
            batch::writeTo,
            new byte[]{
                3, 3, 4, 100, 97, 116, 101, 4, 68, 97, 116, 101, -92, 67, -92, 67,
                -92, 67, 9, 116, 105, 109, 101, 115, 116, 97, 109, 112, 6, 85, 73, 110,
                116, 51, 50, 74, 34, 45, 89, 74, 34, 45, 89, 74, 34, 45, 89, 5,
                97, 114, 114, 97, 121, 12, 65, 114, 114, 97, 121, 40, 73, 110, 116, 51,
                50, 41, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0,
                0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0,
                0, 0, 3, 0, 0, 0, 4, 0, 0, 0, 5, 0, 0, 0, 6, 0,
                0, 0
            }
        );
    }

    @Test
    public void test1() throws Exception {
//        https://paste.yandex-team.ru/247709

        LogBatch batch = new LogBatch(
            Stream.empty(), 0, 0, 0, Duration.ofMillis(0),
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("click_host", ColumnType.String),
                new Column("show_time", ColumnType.DateTime),
                new Column("show_host", ColumnType.String),
                new Column("pp", ColumnType.Int32),
                new Column("pof", ColumnType.Int32),
                new Column("clid", ColumnType.Int32),
                new Column("at", ColumnType.Int8),
                new Column("dtype", ColumnType.String),
                new Column("cpa", ColumnType.Int8),
                new Column("referer", ColumnType.String),
                new Column("shop_id", ColumnType.Int32),
                new Column("price", ColumnType.Float64),
                new Column("fee", ColumnType.Float64),
                new Column("yandexuid", ColumnType.String),
                new Column("cp", ColumnType.Int32)
            ),
            "sourceName"
        );

        batch.write(
            new Date(1496130122000L),
            "clickd01ht.supermarket.yandex.net",
            new Date(1496130122000L),
            "msh04ht.market.yandex.net",
            7,
            -1,
            -1,
            0,
            "market",
            0,
            "http://market.yandex.ru/search.xml?text=cltestoffer20080603",
            4089,
            63660.0,
            Double.NaN,
            "",
            27
        );
        batch.onParseComplete(Duration.ZERO, 0, 0);

        //clickhouse-client -q "select toDate(toDateTime(1496130122)) as date, toUInt32(1496130122) as timestamp, 'clickd01ht.supermarket.yandex.net' as click_host, toDateTime(1496130122) as show_time, 'msh04ht.market.yandex.net' as show_host, toInt32(7) as pp, toInt32(-1) as pof, toInt32(-1) as clid, toInt8(0) as at, 'market' as dtype, toInt8(0) as cpa, 'http://market.yandex.ru/search.xml?text=cltestoffer20080603' as referer, toInt32(4089) as shop_id, toFloat64(63660.0) as price, toFloat64(nan) as fee, '' as yandexuid, toInt32(27) as cp FORMAT Native" | od -vAn -td1
        check(
            batch::writeTo,
            new byte[]{
                17, 1, 4, 100, 97, 116, 101, 4, 68, 97, 116, 101, -92, 67, 9, 116,
                105, 109, 101, 115, 116, 97, 109, 112, 6, 85, 73, 110, 116, 51, 50, 74,
                34, 45, 89, 10, 99, 108, 105, 99, 107, 95, 104, 111, 115, 116, 6, 83,
                116, 114, 105, 110, 103, 33, 99, 108, 105, 99, 107, 100, 48, 49, 104, 116,
                46, 115, 117, 112, 101, 114, 109, 97, 114, 107, 101, 116, 46, 121, 97, 110,
                100, 101, 120, 46, 110, 101, 116, 9, 115, 104, 111, 119, 95, 116, 105, 109,
                101, 8, 68, 97, 116, 101, 84, 105, 109, 101, 74, 34, 45, 89, 9, 115,
                104, 111, 119, 95, 104, 111, 115, 116, 6, 83, 116, 114, 105, 110, 103, 25,
                109, 115, 104, 48, 52, 104, 116, 46, 109, 97, 114, 107, 101, 116, 46, 121,
                97, 110, 100, 101, 120, 46, 110, 101, 116, 2, 112, 112, 5, 73, 110, 116,
                51, 50, 7, 0, 0, 0, 3, 112, 111, 102, 5, 73, 110, 116, 51, 50,
                -1, -1, -1, -1, 4, 99, 108, 105, 100, 5, 73, 110, 116, 51, 50, -1,
                -1, -1, -1, 2, 97, 116, 4, 73, 110, 116, 56, 0, 5, 100, 116, 121,
                112, 101, 6, 83, 116, 114, 105, 110, 103, 6, 109, 97, 114, 107, 101, 116,
                3, 99, 112, 97, 4, 73, 110, 116, 56, 0, 7, 114, 101, 102, 101, 114,
                101, 114, 6, 83, 116, 114, 105, 110, 103, 59, 104, 116, 116, 112, 58, 47,
                47, 109, 97, 114, 107, 101, 116, 46, 121, 97, 110, 100, 101, 120, 46, 114,
                117, 47, 115, 101, 97, 114, 99, 104, 46, 120, 109, 108, 63, 116, 101, 120,
                116, 61, 99, 108, 116, 101, 115, 116, 111, 102, 102, 101, 114, 50, 48, 48,
                56, 48, 54, 48, 51, 7, 115, 104, 111, 112, 95, 105, 100, 5, 73, 110,
                116, 51, 50, -7, 15, 0, 0, 5, 112, 114, 105, 99, 101, 7, 70, 108,
                111, 97, 116, 54, 52, 0, 0, 0, 0, -128, 21, -17, 64, 3, 102, 101,
                101, 7, 70, 108, 111, 97, 116, 54, 52, 0, 0, 0, 0, 0, 0, -8,
                127, 9, 121, 97, 110, 100, 101, 120, 117, 105, 100, 6, 83, 116, 114, 105,
                110, 103, 0, 2, 99, 112, 5, 73, 110, 116, 51, 50, 27, 0, 0, 0
            }
        );

    }


    @Test
    public void test2() throws Exception {
//        https://paste.yandex-team.ru/247096

        LogBatch batch = new LogBatch(
            Stream.empty(), 0, 0, 0, Duration.ofMillis(0),
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("host", ColumnType.String),
                new Column("method", ColumnType.String),
                new Column("http_code", ColumnType.UInt16),
                new Column("resptime_ms", ColumnType.Int32),
                new Column("corba", ColumnType.UInt8)
            ),
            "sourceName"
        );

        batch.write(
            new Date(1496226716000L),
            "kgb02ht.market.yandex.net",
            "ping",
            200,
            0,
            false
        );
        batch.onParseComplete(Duration.ZERO, 0, 0);

        //clickhouse-client -q "select toDate(toDateTime(1496226716)) as date, toUInt32(1496226716) as timestamp, 'kgb02ht.market.yandex.net' host, 'ping' as method, toUInt16(200) as http_code, toInt32(0) as resptime_ms, toUInt8(0) as corba FORMAT Native" | od -vAn -td1
        check(
            batch::writeTo,
            new byte[]{
                7, 1, 4, 100, 97, 116, 101, 4, 68, 97, 116, 101, -91, 67, 9, 116,
                105, 109, 101, 115, 116, 97, 109, 112, 6, 85, 73, 110, 116, 51, 50, -100,
                -101, 46, 89, 4, 104, 111, 115, 116, 6, 83, 116, 114, 105, 110, 103, 25,
                107, 103, 98, 48, 50, 104, 116, 46, 109, 97, 114, 107, 101, 116, 46, 121,
                97, 110, 100, 101, 120, 46, 110, 101, 116, 6, 109, 101, 116, 104, 111, 100,
                6, 83, 116, 114, 105, 110, 103, 4, 112, 105, 110, 103, 9, 104, 116, 116,
                112, 95, 99, 111, 100, 101, 6, 85, 73, 110, 116, 49, 54, -56, 0, 11,
                114, 101, 115, 112, 116, 105, 109, 101, 95, 109, 115, 5, 73, 110, 116, 51,
                50, 0, 0, 0, 0, 5, 99, 111, 114, 98, 97, 5, 85, 73, 110, 116,
                56, 0
            }
        );
    }

    @Test
    public void testEnum() throws Exception {
        LogBatch batch = new LogBatch(
            Stream.empty(), 0, 0, 0, Duration.ofMillis(0),
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("type", EnumColumnType.enum8(RequestType.class))
            ),
            "sourceName"
        );

        batch.write(
            new Date(1496226716000L),
            RequestType.OUT
        );
        batch.onParseComplete(Duration.ZERO, 0, 0);

        check(
            batch::writeTo,
            new byte[]{
                3, 1, 4, 100, 97, 116, 101, 4, 68, 97, 116, 101, -91, 67, 9, 116,
                105, 109, 101, 115, 116, 97, 109, 112, 6, 85, 73, 110, 116, 51, 50, -100,
                -101, 46, 89, 4, 116, 121, 112, 101, 39, 69, 110, 117, 109, 56, 40, 39,
                73, 78, 39, 32, 61, 32, 48, 44, 32, 39, 79, 85, 84, 39, 32, 61,
                32, 49, 44, 32, 39, 80, 82, 79, 88, 89, 39, 32, 61, 32, 50, 41,
                1
            }
        );
    }

    @Test
    public void testTrace() throws Exception {

        //https://paste.yandex-team.ru/247719

        LogBatch batch = new LogBatch(
            Stream.empty(), 0, 0, 0, Duration.ofMillis(0),
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("id_ms", ColumnType.UInt64),
                new Column("id_hash", ColumnType.String),
                new Column("id_seq", ColumnType.ArrayUInt32),
                new Column("start_time_ms", ColumnType.UInt64),
                new Column("end_time_ms", ColumnType.UInt64),
                new Column("duration_ms", ColumnType.UInt32),
                new Column("type", EnumColumnType.enum8(RequestType.class)),
                new Column("module", ColumnType.String),
                new Column("host", ColumnType.String),
                new Column("source_module", ColumnType.String),
                new Column("source_host", ColumnType.String),
                new Column("target_module", ColumnType.String),
                new Column("target_host", ColumnType.String),
                new Column("environment", EnumColumnType.enum8(Environment.class)),
                new Column("request_method", ColumnType.String),
                new Column("http_code", ColumnType.Int16),
                new Column("retry_num", ColumnType.UInt8),
                new Column("error_code", ColumnType.String),
                new Column("protocol", ColumnType.String),
                new Column("http_method", ColumnType.String),
                new Column("query_params", ColumnType.String),
                new Column("yandex_uid", ColumnType.String),
                new Column("yandex_login", ColumnType.String),
                new Column("kv_keys", ColumnType.ArrayString),
                new Column("kv_values", ColumnType.ArrayString),
                new Column("events_names", ColumnType.ArrayString),
                new Column("events_timestamps", ColumnType.ArrayUInt64),
                new Column("test_ids", ColumnType.ArrayUInt32),
                new Column("page_id", ColumnType.String),
                new Column("page_type", ColumnType.String)
            ),
            "sourceName"
        );

        batch.write(
            new Date(1496434944803L),
            1496434944803L,
            "543acfff60e94a0fe9e4c1a3000c9903",
            new Integer[0],
            1496434944803L,
            1496434944807L,
            4,
            RequestType.PROXY,
            "nginx",
            "graph01gt.market.yandex.net",
            "",
            "::1",
            "",
            "[::1]:6031",
            Environment.TESTING,
            "",
            200,
            1,
            "",
            "http",
            "GET",
            "/render?format=json",
            "",
            "",
            Collections.emptyList(),
            new Object[0],
            new String[]{"upstream"},
            new Long[]{1496434944807L},
            new Integer[0],
            "",
            ""
        );
        batch.onParseComplete(Duration.ZERO, 0, 0);

        //clickhouse-client -d market -q "select * from trace where timestamp = 1496434944 and id_hash = '543acfff60e94a0fe9e4c1a3000c9903' and date = toDate(toDateTime(1496434944)) FORMAT Native"  | od -vAn -td1


        check(
            batch::writeTo,
            new byte[]{
                32, 1, 4, 100, 97, 116, 101, 4, 68, 97, 116, 101, -89, 67, 9, 116,
                105, 109, 101, 115, 116, 97, 109, 112, 6, 85, 73, 110, 116, 51, 50, 0,
                -55, 49, 89, 5, 105, 100, 95, 109, 115, 6, 85, 73, 110, 116, 54, 52,
                35, 43, 121, 106, 92, 1, 0, 0, 7, 105, 100, 95, 104, 97, 115, 104,
                6, 83, 116, 114, 105, 110, 103, 32, 53, 52, 51, 97, 99, 102, 102, 102,
                54, 48, 101, 57, 52, 97, 48, 102, 101, 57, 101, 52, 99, 49, 97, 51,
                48, 48, 48, 99, 57, 57, 48, 51, 6, 105, 100, 95, 115, 101, 113, 13,
                65, 114, 114, 97, 121, 40, 85, 73, 110, 116, 51, 50, 41, 0, 0, 0,
                0, 0, 0, 0, 0, 13, 115, 116, 97, 114, 116, 95, 116, 105, 109, 101,
                95, 109, 115, 6, 85, 73, 110, 116, 54, 52, 35, 43, 121, 106, 92, 1,
                0, 0, 11, 101, 110, 100, 95, 116, 105, 109, 101, 95, 109, 115, 6, 85,
                73, 110, 116, 54, 52, 39, 43, 121, 106, 92, 1, 0, 0, 11, 100, 117,
                114, 97, 116, 105, 111, 110, 95, 109, 115, 6, 85, 73, 110, 116, 51, 50,
                4, 0, 0, 0, 4, 116, 121, 112, 101, 39, 69, 110, 117, 109, 56, 40,
                39, 73, 78, 39, 32, 61, 32, 48, 44, 32, 39, 79, 85, 84, 39, 32,
                61, 32, 49, 44, 32, 39, 80, 82, 79, 88, 89, 39, 32, 61, 32, 50,
                41, 2, 6, 109, 111, 100, 117, 108, 101, 6, 83, 116, 114, 105, 110, 103,
                5, 110, 103, 105, 110, 120, 4, 104, 111, 115, 116, 6, 83, 116, 114, 105,
                110, 103, 27, 103, 114, 97, 112, 104, 48, 49, 103, 116, 46, 109, 97, 114,
                107, 101, 116, 46, 121, 97, 110, 100, 101, 120, 46, 110, 101, 116, 13, 115,
                111, 117, 114, 99, 101, 95, 109, 111, 100, 117, 108, 101, 6, 83, 116, 114,
                105, 110, 103, 0, 11, 115, 111, 117, 114, 99, 101, 95, 104, 111, 115, 116,
                6, 83, 116, 114, 105, 110, 103, 3, 58, 58, 49, 13, 116, 97, 114, 103,
                101, 116, 95, 109, 111, 100, 117, 108, 101, 6, 83, 116, 114, 105, 110, 103,
                0, 11, 116, 97, 114, 103, 101, 116, 95, 104, 111, 115, 116, 6, 83, 116,
                114, 105, 110, 103, 10, 91, 58, 58, 49, 93, 58, 54, 48, 51, 49, 11,
                101, 110, 118, 105, 114, 111, 110, 109, 101, 110, 116, 74, 69, 110, 117, 109,
                56, 40, 39, 68, 69, 86, 69, 76, 79, 80, 77, 69, 78, 84, 39, 32,
                61, 32, 48, 44, 32, 39, 84, 69, 83, 84, 73, 78, 71, 39, 32, 61,
                32, 49, 44, 32, 39, 80, 82, 69, 83, 84, 65, 66, 76, 69, 39, 32,
                61, 32, 50, 44, 32, 39, 80, 82, 79, 68, 85, 67, 84, 73, 79, 78,
                39, 32, 61, 32, 51, 41, 1, 14, 114, 101, 113, 117, 101, 115, 116, 95,
                109, 101, 116, 104, 111, 100, 6, 83, 116, 114, 105, 110, 103, 0, 9, 104,
                116, 116, 112, 95, 99, 111, 100, 101, 5, 73, 110, 116, 49, 54, -56, 0,
                9, 114, 101, 116, 114, 121, 95, 110, 117, 109, 5, 85, 73, 110, 116, 56,
                1, 10, 101, 114, 114, 111, 114, 95, 99, 111, 100, 101, 6, 83, 116, 114,
                105, 110, 103, 0, 8, 112, 114, 111, 116, 111, 99, 111, 108, 6, 83, 116,
                114, 105, 110, 103, 4, 104, 116, 116, 112, 11, 104, 116, 116, 112, 95, 109,
                101, 116, 104, 111, 100, 6, 83, 116, 114, 105, 110, 103, 3, 71, 69, 84,
                12, 113, 117, 101, 114, 121, 95, 112, 97, 114, 97, 109, 115, 6, 83, 116,
                114, 105, 110, 103, 19, 47, 114, 101, 110, 100, 101, 114, 63, 102, 111, 114,
                109, 97, 116, 61, 106, 115, 111, 110, 10, 121, 97, 110, 100, 101, 120, 95,
                117, 105, 100, 6, 83, 116, 114, 105, 110, 103, 0, 12, 121, 97, 110, 100,
                101, 120, 95, 108, 111, 103, 105, 110, 6, 83, 116, 114, 105, 110, 103, 0,
                7, 107, 118, 95, 107, 101, 121, 115, 13, 65, 114, 114, 97, 121, 40, 83,
                116, 114, 105, 110, 103, 41, 0, 0, 0, 0, 0, 0, 0, 0, 9, 107,
                118, 95, 118, 97, 108, 117, 101, 115, 13, 65, 114, 114, 97, 121, 40, 83,
                116, 114, 105, 110, 103, 41, 0, 0, 0, 0, 0, 0, 0, 0, 12, 101,
                118, 101, 110, 116, 115, 95, 110, 97, 109, 101, 115, 13, 65, 114, 114, 97,
                121, 40, 83, 116, 114, 105, 110, 103, 41, 1, 0, 0, 0, 0, 0, 0,
                0, 8, 117, 112, 115, 116, 114, 101, 97, 109, 17, 101, 118, 101, 110, 116,
                115, 95, 116, 105, 109, 101, 115, 116, 97, 109, 112, 115, 13, 65, 114, 114,
                97, 121, 40, 85, 73, 110, 116, 54, 52, 41, 1, 0, 0, 0, 0, 0,
                0, 0, 39, 43, 121, 106, 92, 1, 0, 0, 8, 116, 101, 115, 116, 95,
                105, 100, 115, 13, 65, 114, 114, 97, 121, 40, 85, 73, 110, 116, 51, 50,
                41, 0, 0, 0, 0, 0, 0, 0, 0, 7, 112, 97, 103, 101, 95, 105,
                100, 6, 83, 116, 114, 105, 110, 103, 0, 9, 112, 97, 103, 101, 95, 116,
                121, 112, 101, 6, 83, 116, 114, 105, 110, 103, 0,
            }
        );
    }


    private void check(StreamWriter streamWriter, byte[] expected) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ClickHouseRowBinaryStream stream = new ClickHouseRowBinaryStream(
            byteArrayOutputStream, TimeZone.getTimeZone("ETC"), new ClickHouseProperties()
        );
        streamWriter.write(stream);
        byte[] actual = byteArrayOutputStream.toByteArray();
        Assert.assertArrayEquals(expected, actual);
    }

    private interface StreamWriter {
        void write(ClickHouseRowBinaryStream stream) throws Exception;
    }

    /**
     * Snapshot of ru.yandex.market.logshatter.parser.trace.Environment
     * Duplicated for CH binary stream consistency. Avoiding stream content change on class change.
     */
    private enum Environment {
        DEVELOPMENT,
        TESTING,
        PRESTABLE,
        PRODUCTION
    }

    /**
     * Snapshot of ru.yandex.market.logshatter.parser.trace.RequestType
     * Duplicated for CH binary stream consistency. Avoiding stream content change on class change.
     */
    private enum RequestType {
        IN, OUT, PROXY
    }
}
