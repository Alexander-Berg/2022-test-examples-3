package ru.yandex.search.salo;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.NotImplementedHttpItem;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.salo.config.MailSaloConfigBuilder;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.util.string.StringUtils;

// CSOFF: MagicNumber
public class SaloTest extends TestBase {
    private static final String MSAL = "MSAL";
    private static final String ZOOLOOSER = "ZooLooser";
    private static final String MDB_100 = "mdb100";
    private static final String MDB_200 = "mdb200";
    private static final String HOST_DEFAULTS =
        "host = localhost\nconnections = 100\ntimeout = 1m";
    private static final String CONNECTIONS = "connections = ";
    private static final String PORT = "port = ";
    private static final String PRODUCER_LOCK =
        "/_producer_lock?service=change_log&session-timeout=10000&";
    private static final String OPQUEUE =
        "/operations-queue-envelopes?json-type=dollar&namespace="
        + "operations_queue&mdb=pg&pgshard=";
    private static final String NO_RO = "&no-ro";
    private static final String MIN_TRANSCATION_DATE =
        "/get-min-transaction-date?json-type=dollar&mdb=pg&pgshard=";
    private static final String PING_SUFFIX =
        "&operation-ids=0-0&first-operation-date=0&optional&batch-size=1";
    private static final long SLEEP_INTERVAL = 5000L;
    private static final long DELAY = 1000L;

    private static final UnaryOperator<String> TRANSFER_TIMESTAMP_ERASER =
        new UnaryOperator<String>() {
            @Override
            public String apply(final String uri) {
                String timestamp = "&transfer-timestamp=";
                int pos = uri.indexOf(timestamp);
                if (pos == -1) {
                    return uri;
                } else {
                    return uri.substring(0, pos)
                        + uri.substring(pos + timestamp.length() + 13);
                }
            }
        };

    private static Path mdblist(final String... mdbs) throws IOException {
        Path mdblist = Files.createTempFile("salo", "mdblist");
        mdblist.toFile().deleteOnExit();
        Files.write(mdblist, Arrays.asList(mdbs), Charset.defaultCharset());
        return mdblist.toAbsolutePath();
    }

    private static String pgData(final String... rows) {
        return StringUtils.join(rows, "},{", "{\"rows\":[{", "}]}");
    }

    // CSOFF: ParameterNumber
    private MailSaloConfigBuilder config(
        final int msalPort,
        final int zoolooserPort,
        final Path mdblist,
        final Path pgshards,
        final int selectLength)
        throws ConfigException, IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[server]").append('\n');
        sb.append(PORT).append(0).append('\n');
        sb.append(CONNECTIONS).append(100).append('\n');
        sb.append("timeout = ").append(10000).append('\n');
        sb.append('\n');
        sb.append("[msal]").append('\n');
        sb.append(HOST_DEFAULTS).append('\n');
        sb.append(PORT).append(msalPort).append('\n');
        sb.append('\n');
        sb.append("[zoolooser]").append('\n');
        sb.append(HOST_DEFAULTS).append('\n');
        sb.append(PORT).append(zoolooserPort).append('\n');
        sb.append('\n');
        sb.append("[salo]").append('\n');
        sb.append("mdblist = ").append(mdblist).append('\n');
        sb.append("pgshards = ").append(pgshards).append('\n');
        sb.append("select-length = ").append(selectLength).append('\n');
        sb.append("workers-per-mdb = 4\n");
        sb.append('\n');
        MailSaloConfigBuilder builder = new MailSaloConfigBuilder(
            new IniConfig(new StringReader(new String(sb))));
        builder.msalResponsesStaterConfig().prefix("msal-responses");
        builder.minTransactionStaterConfig().prefix("min-transaction");
        builder.transferLagStaterConfig().prefix("transfer-lag");
        return builder;
    }
    // CSON: ParameterNumber

    private static String envelopes(final String... envelopes) {
        return StringUtils.join(envelopes, ',', "{\"rows\":[", "]}");
    }

    @Test
    public void testSimple() throws Exception {
        try (TestZoolooser zoolooser =
                new TestZoolooser(Configs.baseConfig(ZOOLOOSER));
            TestMsal msal =
                new TestMsal(Configs.baseConfig(MSAL)))
        {
            zoolooser.operationId(MDB_100, -1);
            zoolooser.operationId(MDB_200, 9);
            zoolooser.start();
            msal.envelope(MDB_100, 0, "1.100.0");
            msal.envelope(MDB_100, 1, "1.100.1");
            msal.envelope(MDB_100, 3, "1.100.2");
            msal.envelope(MDB_100, 4, "1.100.3");
            msal.envelope(MDB_100, 6, "1.100.4");
            msal.envelope(MDB_100, 8, "1.100.5");
            msal.envelope(MDB_200, 10, "1.200.1");
            msal.start();
            try (Server salo = new Server(config(
                    msal.port(),
                    zoolooser.port(),
                    mdblist(MDB_100, MDB_200),
                    mdblist(),
                    2).build()))
            {
                salo.start();
                Thread.sleep(SLEEP_INTERVAL);
                zoolooser.assertEquals(msal);
            }
        }
    }

    @Test
    public void testConcurrent() throws Exception {
        try (TestZoolooser zoolooser =
                new TestZoolooser(Configs.baseConfig(ZOOLOOSER));
            TestMsal msal =
                new TestMsal(Configs.baseConfig(MSAL)))
        {
            zoolooser.operationId(MDB_100, -1);
            zoolooser.start();
            for (int i = 1; i <= 100; ++i) {
                msal.envelope(MDB_100, i, "2.100." + i);
            }
            msal.start();
            try (Server salo = new Server(config(
                    msal.port(),
                    zoolooser.port(),
                    mdblist(MDB_100, MDB_100, MDB_100, MDB_100),
                    mdblist(),
                    5).build()))
            {
                salo.start();
                Thread.sleep(SLEEP_INTERVAL);
                zoolooser.assertEquals(msal);
            }
        }
    }

    // CSOFF: MethodLength
    // CSOFF: MultipleStringLiterals
    @Test
    public void testTokenRejects() throws Exception {
        try (StaticServer zoolooser =
                new StaticServer(Configs.baseConfig(ZOOLOOSER));
            StaticServer msal = new StaticServer(Configs.baseConfig(MSAL));
            Server salo = new Server(config(
                msal.port(),
                zoolooser.port(),
                mdblist(MDB_100),
                mdblist(Integer.toString(1)),
                1)
                .workersPerMdb(1)
                .workersLookahead(1)
                .envelopesCheckInterval(SLEEP_INTERVAL)
                .lockCheckInterval(100L)
                .build()))
        {
            String lockUri =
                "/_producer_lock?service=opqueue&session-timeout=10000"
                + "&producer-name=" + MDB_100;
            zoolooser.add(
                lockUri,
                new StaticHttpItem(HttpStatus.SC_FORBIDDEN),
                new StaticHttpItem("mdb100@1"),
                new StaticHttpItem("mdb100@2"),
                new StaticHttpItem("mdb100@3"),
                new StaticHttpItem("mdb100@4"));
            String pgLockUri =
                "/_producer_lock?service=change_log&session-timeout=10000"
                + "&producer-name=pg1";
            zoolooser.add(pgLockUri, new StaticHttpItem("pg1@1"));
            String pos1 =
                "/_producer_position?service=opqueue&producer-name=mdb100:0";
            String pos2 =
                "/_producer_position?service=opqueue&producer-name=mdb100:1";
            zoolooser.add(
                pos1,
                new StaticHttpItem(HttpStatus.SC_FORBIDDEN),
                new StaticHttpItem("Parse this!"),
                new StaticHttpItem(Long.toString(-1L)),
                new StaticHttpItem(Long.toString(1)));
            zoolooser.add(pos2, "-1");
            String pgPos1 =
                "/_producer_position?service=change_log&producer-name=pg1:0";
            String pgPos2 =
                "/_producer_position?service=change_log&producer-name=pg1:1";
            zoolooser.add(pgPos1, new StaticHttpItem(Long.toString(2 + 2)));
            zoolooser.add(pgPos2, new StaticHttpItem(Long.toString(-1L)));
            String notify1 =
                "/notify?mdb=mdb100&operation-id=0&operation-date=1234567889."
                + "456&suid=9000&mid=100500&action-type=delete&batch-size=1&*";
            String notify2 =
                "/notify?mdb=mdb100&operation-id=2&operation-date=1234567889.4"
                + "56789123&suid=9000&mid=100501&action-type=add"
                + "&batch-size=1&*";
            String notify3 =
                "/notify?mdb=mdb100&operation-id=3"
                + "&operation-date=1234567890&suid=9000&action-type="
                + "folder_update&action-type=labels_update&batch-size=1&*";
            zoolooser.add(
                notify1,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_FORBIDDEN),
                    YandexHeaders.ZOO_SHARD_ID,
                    "9000"));
            zoolooser.add(notify2, new StaticHttpItem(HttpStatus.SC_CONFLICT));
            zoolooser.add(notify3, new StaticHttpItem(HttpStatus.SC_OK));
            String pgNotify =
                "/notify?mdb=pg&pgshard=1&operation-id=5&operation-date=143695"
                + "1904.326374&uid=42&change-type=store&changed-size=1&"
                + "batch-size=1&*";
            zoolooser.add(
                pgNotify,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    YandexHeaders.ZOO_SHARD_ID,
                    "42"));
            String opqueuePrefix =
                "/operations-queue-envelopes?json-type=dollar"
                + "&namespace=operations_queue&mdb=mdb100&length=1&op-id=";
            String opqueue1 = opqueuePrefix + '0';
            String opqueue2 = opqueuePrefix + '2';
            String opqueue3 = opqueuePrefix + '3';
            String opqueue4 = opqueuePrefix + '4';
            msal.add(
                opqueue1,
                new StaticHttpItem(
                    "{\"rows\":[{\"mid\":100500,\"uname\":9000,"
                    + "\"operation_date\":\"1234567889.456\","
                    + "\"operation_id\":0,\"action_type\":[\"delete\"]}]}"));
            msal.add(
                opqueue2,
                new StaticHttpItem(
                    "{\"rows\":[{\"mid\":100501,\"uname\":9000,"
                    + "\"operation_date\":\"1234567889.456789123\","
                    + "\"operation_id\":2,\"action_type\":[\"add\"]}]}"));
            msal.add(
                opqueue3,
                new StaticHttpItem(
                    "{\"rows\":[{\"operation_date\":1234567890,\"uname\":9000,"
                    + "\"operation_id\":3,\"action_type\":["
                    + "\"folder_update\",\"labels_update\"]}]}"));
            msal.add(
                opqueue4,
                new SlowpokeHttpItem(
                    new StaticHttpItem("{\"rows\":[]}"),
                    DELAY >> 1),
                NotImplementedHttpItem.INSTANCE);
            String pgOpqueue = OPQUEUE + "1&length=1&op-id=5";
            msal.add(
                pgOpqueue,
                "{\"rows\":[{\"operation_id\": \"5\","
                + "    \"uid\": \"42\","
                + "    \"lcn\": \"33\","
                + "    \"change_type\": \"store\","
                + "    \"operation_date\": \"1436951904.326374\","
                + "    \"changed\": ["
                + "        {\"lids\": [7],"
                + "            \"fid\": 1,"
                + "            \"tid\": 155937137097703442,"
                + "            \"mid\": 155937137097703442,"
                + "            \"seen\": false,"
                + "            \"deleted\": false,"
                + "            \"recent\": false}],"
                + "    \"fresh_count\": \"1\","
                + "    \"useful_new_messages\": \"1\","
                + "    \"pgshard\": \"1\"}]}");
            zoolooser.start();
            msal.start();
            salo.start();
            Thread.sleep(DELAY);
            HttpAssert.assertStat(
                "pg-active-msals_ammv",
                Integer.toString(1),
                salo.port());
            HttpAssert.assertStat(
                "pg-active-shards_ammv",
                Integer.toString(1),
                salo.port());
            final long maxSleep = SLEEP_INTERVAL + (SLEEP_INTERVAL >> 1);
            for (int i = (int) (maxSleep / DELAY); i >= 0; --i) {
                try {
                    Assert.assertEquals(5, zoolooser.accessCount(lockUri));
                    Assert.assertEquals(4, zoolooser.accessCount(pos1));
                    Assert.assertEquals(2, zoolooser.accessCount(pos2));
                    Assert.assertEquals(1, zoolooser.accessCount(notify1));
                    Assert.assertEquals(1, zoolooser.accessCount(notify2));
                    Assert.assertEquals(1, zoolooser.accessCount(notify3));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify));
                    Assert.assertEquals(1, msal.accessCount(opqueue4));
                    String stats = HttpAssert.stats(salo.port());
                    HttpAssert.assertStat(
                        "ora-ssd-transferred-count_ammm",
                        Integer.toString(2),
                        stats);
                    HttpAssert.assertStat(
                        "ora-ssd-active-msals_ammv",
                        Integer.toString(1),
                        stats);
                    HttpAssert.assertStat(
                        "ora-sata-active-msals_ammv",
                        Integer.toString(0),
                        stats);
                    break;
                } catch (AssertionError e) {
                    if (i == 0) {
                        throw e;
                    }
                }
                Thread.sleep(DELAY);
            }
        }
    }

    @Test
    public void testPg() throws Exception {
        try (StaticServer zoolooser =
                new StaticServer(Configs.baseConfig(ZOOLOOSER));
            StaticServer msal = new StaticServer(Configs.baseConfig(MSAL));
            Server salo = new Server(config(
                msal.port(),
                zoolooser.port(),
                mdblist(""),
                mdblist(Integer.toString(1 + 1)),
                1)
                .workersPerMdb(1)
                .workersLookahead(1)
                .envelopesCheckInterval(SLEEP_INTERVAL)
                .lockCheckInterval(100L)
                .build()))
        {
            String pgLockUri =
                "/_producer_lock?service=change_log&session-timeout=10000"
                + "&producer-name=pg2";
            zoolooser.add(pgLockUri, new StaticHttpItem("pg2@1"));
            String pgPos1 =
                "/_producer_position?service=change_log&producer-name=pg2:0";
            String pgPos2 =
                "/_producer_position?service=change_log&producer-name=pg2:1";
            zoolooser.add(pgPos1, new StaticHttpItem(Long.toString(2 + 2 + 1)));
            zoolooser.add(pgPos2, new StaticHttpItem(Long.toString(-1L)));
            String pgNotify =
                "/notify?mdb=pg&pgshard=2&operation-id=6&operation-date=143696"
                + "1904.326375&uid=43&change-type=store&changed-size="
                + "1&batch-size=1&*";
            zoolooser.add(
                pgNotify,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    YandexHeaders.ZOO_SHARD_ID,
                    "43"));
            String pgOpqueue = OPQUEUE + "2&length=1&op-id=6";
            msal.add(
                pgOpqueue,
                "{\"rows\":[{\"operation_id\": \"6\","
                + "    \"uid\": \"43\","
                + "    \"lcn\": \"34\","
                + "    \"change_type\": \"store\","
                + "    \"operation_date\": \"1436961904.326375\","
                + "    \"changed\": ["
                + "        {\"lids\": [8],"
                + "            \"fid\": 2,"
                + "            \"tid\": 155937137097703443,"
                + "            \"mid\": 155937137097703443,"
                + "            \"seeen\": false,"
                + "            \"deleeted\": false,"
                + "            \"receent\": false}],"
                + "    \"fresh_count\": \"2\","
                + "    \"useful_new_messages\": \"2\","
                + "    \"pgshard\": \"2\"}]}");
            zoolooser.start();
            msal.start();
            salo.start();
            final long maxSleep = SLEEP_INTERVAL + (SLEEP_INTERVAL >> 1);
            for (int i = (int) (maxSleep / DELAY); i >= 0; --i) {
                try {
                    Thread.sleep(DELAY);
                    Assert.assertEquals(1, zoolooser.accessCount(pgLockUri));
                    Assert.assertEquals(1, zoolooser.accessCount(pgPos1));
                    Assert.assertEquals(1, zoolooser.accessCount(pgPos2));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue));
                    HttpAssert.assertStat(
                        "pg-transferred-count_ammm",
                        "1",
                        salo.port());
                    HttpAssert.assertStat(
                        "pg-transferred-mids_ammm",
                        "1",
                        salo.port());
                    break;
                } catch (AssertionError e) {
                    if (i == 0) {
                        throw e;
                    }
                }
            }
        }
    }

    @Test
    public void testPgSplit() throws Exception {
        try (StaticServer zoolooser =
                new StaticServer(Configs.baseConfig(ZOOLOOSER));
            StaticServer msal = new StaticServer(Configs.baseConfig(MSAL));
            Server salo = new Server(config(
                msal.port(),
                zoolooser.port(),
                mdblist(""),
                mdblist(Integer.toString(1 + 1)),
                1)
                .workersPerMdb(1)
                .workersLookahead(1)
                .midsLimit(1)
//                .requestsBatchSize(1)
                .envelopesCheckInterval(SLEEP_INTERVAL)
                .lockCheckInterval(100L)
                .build()))
        {
            String pgLockUri =
                "/_producer_lock?service=change_log&session-timeout=10000"
                + "&producer-name=pg2";
            zoolooser.add(pgLockUri, new StaticHttpItem("pg2@1"));
            String pgPos1 =
                "/_producer_position?service=change_log&producer-name=pg2:0";
            String pgPos2 =
                "/_producer_position?service=change_log&producer-name=pg2:1";
            zoolooser.add(pgPos1, new StaticHttpItem(Long.toString(2 + 2 + 1)));
            zoolooser.add(pgPos2, new StaticHttpItem(Long.toString(-1L)));
            String pgNotify1 =
                "/notify?mdb=pg&pgshard=2&operation-id=6.000000&"
                + "operation-date=143696"
                + "1904.326375&uid=43&change-type=store&changed-size="
                + "1&split-offset=0&batch-size=1&*";
            zoolooser.add(
                pgNotify1,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    YandexHeaders.ZOO_SHARD_ID,
                    "43"));
            String pgNotify2 =
                "/notify?mdb=pg&pgshard=2&operation-id=6.000001&"
                + "operation-date=143696"
                + "1904.326375&uid=43&change-type=store&changed-size="
                + "1&split-offset=1&batch-size=1&*";
            zoolooser.add(
                pgNotify2,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    YandexHeaders.ZOO_SHARD_ID,
                    "43"));
            String pgOpqueue = OPQUEUE + "2&length=1&op-id=6";
            msal.add(
                pgOpqueue,
                "{\"rows\":[{\"operation_id\": \"6\","
                + "    \"uid\": \"43\","
                + "    \"lcn\": \"34\","
                + "    \"change_type\": \"store\","
                + "    \"operation_date\": \"1436961904.326375\","
                + "    \"changed\": ["
                + "        {\"lids\": [8],"
                + "            \"fid\": 2,"
                + "            \"tid\": 155937137097703443,"
                + "            \"mid\": 155937137097703443,"
                + "            \"seeen\": false,"
                + "            \"deleeted\": false,"
                + "            \"receent\": false},"
                + "        {\"lids\": [7],"
                + "            \"fid\": 2,"
                + "            \"tid\": 155937137097703444,"
                + "            \"mid\": 155937137097703444,"
                + "            \"seen\": false,"
                + "            \"deleted\": false,"
                + "            \"recent\": false}],"
                + "    \"fresh_count\": \"2\","
                + "    \"useful_new_messages\": \"2\","
                + "    \"pgshard\": \"2\"}]}");
            zoolooser.start();
            msal.start();
            salo.start();
            final long maxSleep = SLEEP_INTERVAL + (SLEEP_INTERVAL >> 1);
            for (int i = (int) (maxSleep / DELAY); i >= 0; --i) {
                try {
                    Thread.sleep(DELAY);
                    Assert.assertEquals(1, zoolooser.accessCount(pgLockUri));
                    Assert.assertEquals(1, zoolooser.accessCount(pgPos1));
                    Assert.assertEquals(1, zoolooser.accessCount(pgPos2));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify1));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify2));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue));
                    HttpAssert.assertStat(
                        "pg-transferred-count_ammm",
                        "2",
                        salo.port());
                    HttpAssert.assertStat(
                        "pg-transferred-mids_ammm",
                        "2",
                        salo.port());
                    break;
                } catch (AssertionError e) {
                    if (i == 0) {
                        throw e;
                    }
                }
            }
        }
    }
    // CSON: MethodLength
    // CSON: MultipleStringLiterals

    @Test
    public void testBatchRequests() throws Exception {
        try (StaticServer zoolooser =
                new StaticServer(Configs.baseConfig(ZOOLOOSER));
            StaticServer msal = new StaticServer(Configs.baseConfig(MSAL));
            Server salo = new Server(config(
                msal.port(),
                zoolooser.port(),
                mdblist(MDB_200),
                mdblist(),
                10)
                .workersPerMdb(2)
                .workersLookahead(0)
                .requestsBatchSize(100)
                .envelopesCheckInterval(100000L)
                .sessionTimeout(100000L)
                .lockCheckInterval(100L)
                .build()))
        {
            String lockUri =
                "/_producer_lock?service=opqueue&session-timeout=100000"
                + "&producer-name=mdb200";
            zoolooser.add(
                lockUri,
                new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                new StaticHttpItem("mdb200@1"));
            String pos =
                "/_producer_position?service=opqueue&producer-name=mdb200:";
            zoolooser.add(
                pos + 0,
                new StaticHttpItem(Long.toString(7L)));
            zoolooser.add(
                pos + 1,
                new StaticHttpItem(Long.toString(7L)));
            String notifyWarmup1 =
                "/notify?mdb=mdb200&operation-id=8&operation-date=555.666"
                + "&suid=9001&mid=100598&action-type=delete&batch-size=1*";
            String notifyWarmup2 =
                "/notify?mdb=mdb200&operation-id=9&operation-date=666.777"
                + "&suid=39002&mid=100599&action-type=delete&batch-size=1*";
            // Possibly we will catch ping in second warmup message
            String notifyWarmup2batch =
                "/notify?mdb=mdb200&salo-worker=mdb200:1&operation-ids=9-9"
                + "&first-operation-date=666&batch-size=2";
            // First batch notify will ontain two envelopes with
            // operation ids 10 and 11 and ping with operation id 13
            String notify1 =
                "/notify?mdb=mdb200&salo-worker=mdb200:0&operation-ids=10-13"
                + "&first-operation-date=1234567891&batch-size=3";
            String notify2 =
                "/notify?mdb=mdb200&salo-worker=mdb200:1&operation-ids=12-13"
                + "&first-operation-date=1234567893&batch-size=2";
            String envelopeWarmup1 =
                "{\"mid\":100598,\"uname\":9001,"
                + "\"operation_date\":\"555.666\","
                + "\"operation_id\":8,\"action_type\":[\"delete\"]}";
            String envelopeWarmup2 =
                "{\"mid\":100599,\"uname\":39002,"
                + "\"operation_date\":\"666.777\","
                + "\"operation_id\":9,\"action_type\":[\"delete\"]}";
            String envelope1 =
                "{\"mid\":100600,\"uname\":9001,"
                + "\"operation_date\":\"1234567891\","
                + "\"operation_id\":10,\"action_type\":[\"delete\"]}";
            String envelope2 =
                "{\"mid\":100601,\"uname\":74535,"
                + "\"operation_date\":\"1234567892\","
                + "\"operation_id\":11,\"action_type\":[\"add\"]}";
            String envelope3 =
                "{\"mid\":100602,\"uname\":39002,"
                + "\"operation_date\":\"1234567893\","
                + "\"operation_id\":12,\"action_type\":[\"add\"]}";
            String envelope4 =
                "{\"mid\":100603,\"uname\":104536,"
                + "\"operation_date\":\"1234567894\","
                + "\"operation_id\":13,\"action_type\":[\"add\"]}";
            // Warmup envelopes, one per worker will keep them occupied while
            // mdb thread will dispatch all batching envelopes
            zoolooser.add(
                notifyWarmup1,
                new SlowpokeHttpItem(
                    new StaticHttpItem(envelopeWarmup1),
                    DELAY << 1));
            zoolooser.add(
                notifyWarmup2,
                new SlowpokeHttpItem(
                    new StaticHttpItem(envelopeWarmup2),
                    DELAY << 1));
            zoolooser.add(
                notifyWarmup2batch,
                new SlowpokeHttpItem(
                    new MultipartAssert(envelopeWarmup2),
                    DELAY << 1));
            MultipartAssert check1 = new MultipartAssert(envelope1, envelope2);
            MultipartAssert check2 = new MultipartAssert(envelope3, envelope4);
            zoolooser.add(notify1, check1);
            zoolooser.add(notify2, check2);
            String opqueueWarmup =
                "/operations-queue-envelopes?json-type=dollar&"
                + "namespace=operations_queue&mdb=mdb200&length=10&op-id=8";
            String opqueue =
                "/operations-queue-envelopes?json-type=dolla"
                + "r&namespace=operations_queue&mdb=mdb200&length=10&op-id=10";
            msal.add(
                opqueueWarmup,
                new StaticHttpItem(
                    envelopes(envelopeWarmup1, envelopeWarmup2)));
            // Delay batching envelopes, so mdb workers will consume all warmup
            // envelopes and send them to zoolooser
            msal.add(
                opqueue,
                new SlowpokeHttpItem(
                    new StaticHttpItem(
                        envelopes(envelope1, envelope2, envelope3, envelope4)),
                    DELAY));
            zoolooser.start();
            msal.start();
            salo.start();
            final long maxSleep = SLEEP_INTERVAL + (SLEEP_INTERVAL >> 1);
            for (int i = (int) (maxSleep / DELAY); i >= 0; --i) {
                try {
                    Thread.sleep(DELAY);
                    Assert.assertEquals(2, zoolooser.accessCount(lockUri));
                    Assert.assertEquals(1, zoolooser.accessCount(pos + 0));
                    Assert.assertEquals(1, zoolooser.accessCount(pos + 1));
                    Assert.assertEquals(1, msal.accessCount(opqueueWarmup));
                    Assert.assertEquals(1, msal.accessCount(opqueue));
                    Assert.assertEquals(
                        1,
                        zoolooser.accessCount(notifyWarmup1));
                    if (zoolooser.accessCount(notifyWarmup2batch) != 1) {
                        Assert.assertEquals(
                            1,
                            zoolooser.accessCount(notifyWarmup2));
                    }
                    Assert.assertEquals(1, zoolooser.accessCount(notify1));
                    Assert.assertEquals(1, zoolooser.accessCount(notify2));
                    break;
                } catch (AssertionError e) {
                    if (i == 0) {
                        throw e;
                    }
                }
            }
        }
    }

    @Test
    public void testPings() throws Exception {
        try (StaticServer zoolooser = new StaticServer(
                Configs.baseConfig(ZOOLOOSER),
                TRANSFER_TIMESTAMP_ERASER);
            StaticServer msal = new StaticServer(Configs.baseConfig(MSAL));
            Server salo = new Server(config(
                msal.port(),
                zoolooser.port(),
                mdblist("mdb300"),
                mdblist(),
                10)
                .workersPerMdb(4)
                .workersLookahead(0)
                .requestsBatchSize(100)
                .envelopesCheckInterval(1000L)
                .sessionTimeout(20000L)
                .lockCheckInterval(100L)
                .build()))
        {
            String lockUri =
                "/_producer_lock?service=opqueue&session-timeout=20000"
                + "&producer-name=mdb300";
            zoolooser.add(lockUri, "mdb300@2");
            zoolooser.add(
                "/_producer_position?*",
                new StaticHttpItem(Long.toString(-1L)),
                new StaticHttpItem(Long.toString(-1L)),
                new StaticHttpItem(Long.toString(-1L)),
                new StaticHttpItem(Long.toString(-1L)));
            String notify =
                "/notify?mdb=mdb300&operation-id=0&operation-date=1234567890&"
                + "suid=29003&mid=100503&action-type=add&batch-size=1"
                + "&salo-worker=mdb300:1";
            zoolooser.add(
                notify,
                new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                new StaticHttpItem(HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);
            String notifyOptional0 =
                "/notify?mdb=mdb300&salo-worker=mdb300:0&operation-ids=0-0"
                + "&first-operation-date=0&optional&batch-size=1";
            zoolooser.add(notifyOptional0, HttpStatus.SC_OK);
            String notifyOptional1 =
                "/notify?mdb=mdb300&salo-worker=mdb300:1&operation"
                + "-ids=0-0&first-operation-date=0&optional&batch-size=1";
            zoolooser.add(notifyOptional1, HttpStatus.SC_OK);
            String notifyOptional2 =
                "/notify?mdb=mdb300&salo-worker=mdb300:2" + PING_SUFFIX;
            zoolooser.add(notifyOptional2, HttpStatus.SC_OK);
            String notifyOptional3 =
                "/notify?mdb=mdb300&salo-worker=mdb300:3" + PING_SUFFIX;
            zoolooser.add(notifyOptional3, HttpStatus.SC_OK);
            String opqueue =
                "/operations-queue-envelopes?json-type=dollar&namespace=op"
                + "erations_queue&mdb=mdb300&length=10&op-id=0";
            msal.add(
                opqueue,
                new StaticHttpItem(
                    "{\"rows\":[{\"mid\":100503,\"uname\":29003,"
                    + "\"operation_date\":1234567890,\"operation_id\":0,"
                    + "\"action_type\":[\"add\"]}]}"));
            zoolooser.start();
            msal.start();
            salo.start();
            final long maxSleep = SLEEP_INTERVAL + (SLEEP_INTERVAL >> 1);
            for (int i = (int) (maxSleep / DELAY); i >= 0; --i) {
                try {
                    Thread.sleep(DELAY);
                    Assert.assertEquals(2, zoolooser.accessCount(notify));
                    Assert.assertEquals(
                        0,
                        zoolooser.accessCount(notifyOptional0));
                    Assert.assertEquals(
                        1,
                        zoolooser.accessCount(notifyOptional1));
                    Assert.assertEquals(
                        0,
                        zoolooser.accessCount(notifyOptional2));
                    Assert.assertEquals(
                        0,
                        zoolooser.accessCount(notifyOptional3));
                    Assert.assertEquals(1, msal.accessCount(opqueue));
                    break;
                } catch (AssertionError e) {
                    if (i == 0) {
                        throw e;
                    }
                }
            }
        }
    }

    @Test
    public void testDropPosition() throws Exception {
        try (StaticServer zoolooser =
                new StaticServer(Configs.baseConfig(ZOOLOOSER));
            Server salo = new Server(config(
                1,
                zoolooser.port(),
                mdblist("mdb30"),
                mdblist(Integer.toString(7)),
                10)
                .workersPerMdb(4)
                .workersLookahead(0)
                .requestsBatchSize(100)
                .envelopesCheckInterval(100000L)
                .sessionTimeout(20000L)
                .lockCheckInterval(100L)
                .build()))
        {
            String dropPgUri =
                "/_producer_drop_position?service=change_log&producer-name=pg7"
                + "&positions-count=4&session-timeout=20000";
            zoolooser.add(dropPgUri, HttpStatus.SC_FORBIDDEN);
            String dropMdbUri =
                "/_producer_drop_position?service=opqueue&producer-name=mdb30&"
                + "positions-count=4&session-timeout=20000";
            zoolooser.add(dropMdbUri, HttpStatus.SC_OK);
            zoolooser.start();
            salo.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_FORBIDDEN,
                salo.port(),
                "/drop-position?pgshard=7");
            Assert.assertEquals(1, zoolooser.accessCount(dropPgUri));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                salo.port(),
                "/drop-position?mdb=mdb30");
            Assert.assertEquals(1, zoolooser.accessCount(dropMdbUri));
        }
    }

    // CSOFF: MethodLength
    @Test
    public void testPinholeTransactionGone() throws Exception {
        String pgShard = "322";
        try (StaticServer zoolooser = new StaticServer(
                Configs.baseConfig(ZOOLOOSER),
                TRANSFER_TIMESTAMP_ERASER);
            StaticServer msal = new StaticServer(Configs.baseConfig(MSAL));
            Server salo = new Server(config(
                msal.port(),
                zoolooser.port(),
                mdblist(""),
                mdblist(pgShard),
                100)
                .workersPerMdb(1)
                .workersLookahead(1)
                .midsLimit(1)
                .requestsBatchSize(1)
                .envelopesCheckInterval(SLEEP_INTERVAL)
                .lockCheckInterval(1000L)
                .build()))
        {
            String pgLockUri = PRODUCER_LOCK + "producer-name=pg322";
            zoolooser.add(pgLockUri, new StaticHttpItem("pg322@1"));
            String pgPos1 =
                "/_producer_position?service=change_log&producer-name=pg322:0";
            String pgPos2 =
                "/_producer_position?service=change_log&producer-name=pg322:1";
            final long initialPosition = 4L;
            zoolooser.add(
                pgPos1,
                new StaticHttpItem(Long.toString(initialPosition)));
            zoolooser.add(pgPos2, new StaticHttpItem(Long.toString(-1L)));

            String opQueueRow5 =
                "\"operation_id\":\"5\",\"uid\":\"5598600\",\"lcn\": \"5\","
                + "\"change_type\":\"store\",\"operation_date\":\"1234667895"
                + ".123466\",\"changed\":[{\"mid\":123466789012345675}]";
            String opQueueRow6 =
                "\"operation_id\":\"6\",\"uid\":\"5598600\",\"lcn\": \"6\","
                + "\"change_type\":\"store\",\"operation_date\":\"1234667896"
                + ".123466\",\"changed\":[{\"mid\":123466789012345676}]";
            String opQueueRow7 =
                "\"operation_id\":\"7\",\"uid\":\"5598600\",\"lcn\": \"7\","
                + "\"change_type\":\"store\",\"operation_date\":\"1234667897"
                + ".123466\",\"changed\":[{\"mid\":123466789012345677}]";
            String opQueueRow8 =
                "\"operation_id\":\"8\",\"uid\":\"5598600\",\"lcn\": \"8\","
                + "\"change_type\":\"store\",\"operation_date\":\"1234667898"
                + ".123466\",\"changed\":[{\"mid\":123466789012345678}]";

            String pgOpqueueBase = OPQUEUE + "322&length=100&op-id=";
            String pgOpqueue0 = pgOpqueueBase + (initialPosition + 1);
            // Initial request to replica
            msal.add(pgOpqueue0, pgData(opQueueRow5, opQueueRow8));
            // Pinhole detected at "[6,8)", op_id 0 can be sent to queue
            String notifySuffix =
                "&change-type=store&changed-size=1&split-offset=0&"
                + "batch-size=1&salo-worker=pg322:0";
            String pgNotify5 =
                "/notify?mdb=pg&pgshard=322&operation-id=5.000000&"
                + "operation-date=1234667895.123466&uid=5598600"
                + notifySuffix;
            zoolooser.add(
                pgNotify5,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"change_type\":\"store\",\"lcn\":\"5\","
                        + "\"uid\":\"5598600\",\"operation_date\":"
                        + "\"1234667895.123466\",\"operation_part\":0,"
                        + "\"operation_id\":\"5\","
                        + "\"changed\":[{\"mid\":123466789012345675}]}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);
            // Ping request will be overriden by next envelope
            // Advance position and request once again
            String pgOpqueue1 = pgOpqueueBase + (initialPosition + 2);
            String pgData1 = pgData(opQueueRow8);
            msal.add(pgOpqueue1, pgData1);
            // Still have pinhole, fallback to master
            // Since there is no transactions on server, data will be requested
            // again, after receiving min transaction date
            String pgOpqueue1NoRo = pgOpqueue1 + NO_RO;
            msal.add(
                pgOpqueue1NoRo,
                new StaticHttpItem(pgData1), // Awww... pinhole still here
                new StaticHttpItem(
                    pgData(
                        opQueueRow6,
                        opQueueRow7,
                        opQueueRow8)),
                NotImplementedHttpItem.INSTANCE);
            // Ask master for any transactions going
            // Since there is no transactions, server timestamp doesn't matter
            String minTransactionDate = MIN_TRANSCATION_DATE + pgShard;
            msal.add(
                minTransactionDate,
                new StaticHttpItem(
                    "{\"min_transaction_date\":null,"
                    + "\"server_timestamp\":1533343444.123456}"),
                NotImplementedHttpItem.INSTANCE);
            // op_ids 6, 7 and 8 will be transferred
            String pgNotify6 =
                "/notify?mdb=pg&pgshard=322&operation-id=6.000000&"
                + "operation-date=1234667896.123466&uid=5598600"
                + notifySuffix;
            String pgNotify7 =
                "/notify?mdb=pg&pgshard=322&operation-id=7.000000&"
                + "operation-date=1234667897.123466&uid=5598600"
                + notifySuffix;
            String pgNotify8 =
                "/notify?mdb=pg&pgshard=322&operation-id=8.000000&"
                + "operation-date=1234667898.123466&uid=5598600"
                + notifySuffix;
            zoolooser.add(
                pgNotify6,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"change_type\":\"store\",\"lcn\":\"6\",\"uid\":\""
                        + "5598600\",\"operation_date\":\"1234667896.123466\","
                        + "\"operation_part\":0,\"operation_id\":\"6\","
                        + "\"changed\":[{\"mid\":123466789012345676}]}")),
                NotImplementedHttpItem.INSTANCE);
            zoolooser.add(
                pgNotify7,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"change_type\":\"store\",\"lcn\":\"7\",\"uid\":\""
                        + "5598600\",\"operation_date\":\"1234667897.123466\","
                        + "\"operation_part\":0,\"operation_id\":\"7\","
                        + "\"changed\":[{\"mid\":123466789012345677}]}")),
                NotImplementedHttpItem.INSTANCE);
            zoolooser.add(
                pgNotify8,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"change_type\":\"store\",\"lcn\":\"8\",\"uid\":\""
                        + "5598600\",\"operation_date\":\"1234667898.123466\","
                        + "\"operation_part\":0,\"operation_id\":\"8\","
                        + "\"changed\":[{\"mid\":123466789012345678}]}")),
                NotImplementedHttpItem.INSTANCE);

            zoolooser.start();
            msal.start();
            salo.start();
            final long maxSleep = SLEEP_INTERVAL << 2;
            for (int i = (int) (maxSleep / DELAY); i >= 0; --i) {
                try {
                    Thread.sleep(DELAY);
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify5));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify6));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify7));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify8));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue0));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue1));
                    Assert.assertEquals(2, msal.accessCount(pgOpqueue1NoRo));
                    Assert.assertEquals(
                        1,
                        msal.accessCount(minTransactionDate));
                    break;
                } catch (AssertionError e) {
                    if (i == 0) {
                        throw e;
                    }
                }
            }
        }
    }

    @Test
    public void testPinholeTransactionExpired() throws Exception {
        String pgShard = "323";
        try (StaticServer zoolooser = new StaticServer(
                Configs.baseConfig(ZOOLOOSER),
                TRANSFER_TIMESTAMP_ERASER);
            StaticServer msal = new StaticServer(Configs.baseConfig(MSAL));
            Server salo = new Server(config(
                msal.port(),
                zoolooser.port(),
                mdblist(""),
                mdblist(pgShard),
                100)
                .workersPerMdb(1)
                .workersLookahead(1)
                .midsLimit(1)
                .requestsBatchSize(1)
                .envelopesCheckInterval(SLEEP_INTERVAL)
                .lockCheckInterval(1000L)
                .build()))
        {
            String pgLockUri = PRODUCER_LOCK + "producer-name=pg323";
            zoolooser.add(pgLockUri, new StaticHttpItem("pg323@1"));
            String pgPos1 =
                "/_producer_position?service=change_log&producer-name=pg323:0";
            String pgPos2 =
                "/_producer_position?service=change_log&producer-name=pg323:1";
            zoolooser.add(pgPos1, new StaticHttpItem(Long.toString(-1L)));
            zoolooser.add(pgPos2, new StaticHttpItem(Long.toString(-1L)));

            String opQueueRow0 =
                "\"operation_id\":\"0\",\"uid\":\"5598601\",\"lcn\": \"0\","
                + "\"change_type\":\"store\",\"operation_date\":\"1234567890"
                + ".123456\",\"changed\":[{\"mid\":123456789012345670}]";
            String opQueueRow3 =
                "\"operation_id\":\"3\",\"uid\":\"5598601\",\"lcn\": \"3\","
                + "\"change_type\":\"store\",\"operation_date\":\"1234567893"
                + ".123456\",\"changed\":[{\"mid\":123456789012345673}]";
            String opQueueRow4 =
                "\"operation_id\":\"4\",\"uid\":\"5598601\",\"lcn\": \"4\","
                + "\"change_type\":\"store\",\"operation_date\":\"1234567894"
                + ".123456\",\"changed\":[{\"mid\":123456789012345674}]";
            String opQueueRow6 =
                "\"operation_id\":\"6\",\"uid\":\"5598601\",\"lcn\": \"6\","
                + "\"change_type\":\"store\",\"operation_date\":\"1234567896"
                + ".123456\",\"changed\":[{\"mid\":123456789012345676}]";

            String pgOpqueueBase = OPQUEUE + "323&length=100&op-id=";
            String pgOpqueue0 = pgOpqueueBase + 0;
            // Initial request to replica
            msal.add(pgOpqueue0, pgData(opQueueRow0, opQueueRow3));
            // Pinhole detected at "[1,3)", op_id 0 can be sent to queue
            String notifySuffix =
                "&change-type=store&changed-size=1&split-offset=0"
                + "&batch-size=1&salo-worker=pg323:0";
            String pgNotify0 =
                "/notify?mdb=pg&pgshard=323&operation-id=0.000000&"
                + "operation-date=1234567890.123456&uid=5598601"
                + notifySuffix;
            zoolooser.add(
                pgNotify0,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"change_type\":\"store\",\"lcn\":\"0\","
                        + "\"uid\":\"5598601\",\"operation_date\":"
                        + "\"1234567890.123456\",\"operation_part\":0,"
                        + "\"operation_id\":\"0\","
                        + "\"changed\":[{\"mid\":123456789012345670}]}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);
            // Ping request will be generated
            String pgPingNotify =
                "/notify?mdb=pg&pgshard=323&salo-worker=pg323:0" + PING_SUFFIX;
            zoolooser.add(pgPingNotify, HttpStatus.SC_OK);
            // Advance position and request once again
            String pgOpqueue1 = pgOpqueueBase + 1;
            msal.add(pgOpqueue1, pgData(opQueueRow3));
            // Still have pinhole, fallback to master
            String pgOpqueue1NoRo = pgOpqueue1 + NO_RO;
            msal.add(
                pgOpqueue1NoRo,
                new StaticHttpItem(pgData(opQueueRow3)), // Pinhole still here
                new StaticHttpItem(
                    pgData(
                        opQueueRow3,
                        opQueueRow4,
                        opQueueRow6)),
                new StaticHttpItem(
                    pgData(
                        opQueueRow3,
                        opQueueRow4,
                        opQueueRow6)),
                new StaticHttpItem(
                    pgData(
                        opQueueRow3,
                        opQueueRow4,
                        opQueueRow6)),
                NotImplementedHttpItem.INSTANCE);
            // Ask master for any transactions going
            // Save first server timestamp and wait while min_transaction_date
            // will exceed it
            String minTransactionDate = MIN_TRANSCATION_DATE + pgShard;
            msal.add(
                minTransactionDate,
                new StaticHttpItem(
                    "{\"min_transaction_date\":1534343440.020,"
                    + "\"server_timestamp\":1534343444.123}"),
                new StaticHttpItem(
                    "{\"min_transaction_date\":1534343442.343,"
                    + "\"server_timestamp\":1534343446.123}"),
                new StaticHttpItem(
                    "{\"min_transaction_date\":1534343445.323,"
                    + "\"server_timestamp\":1534343448.123}"),
                NotImplementedHttpItem.INSTANCE);
            // op_ids 3 and 4 will be transferred, op_id 6 will be delayed
            // because of another pinhole
            String pgNotify3 =
                "/notify?mdb=pg&pgshard=323&operation-id=3.000000&"
                + "operation-date=1234567893.123456&uid=5598601"
                + notifySuffix;
            String pgNotify4 =
                "/notify?mdb=pg&pgshard=323&operation-id=4.000000&"
                + "operation-date=1234567894.123456&uid=5598601"
                + notifySuffix;
            String opQueueEnvelope3 =
                "{\"change_type\":\"store\",\"lcn\":\"3\",\"uid\":\"5598601\","
                + "\"operation_date\":\"1234567893.123456\","
                + "\"operation_part\":0,\"operation_id\":\"3\","
                + "\"changed\":[{\"mid\":123456789012345673}]}";
            String opQueueEnvelope4 =
                "{\"change_type\":\"store\",\"lcn\":\"4\",\"uid\":\""
                + "5598601\",\"operation_date\":\"1234567894.123456\","
                + "\"operation_part\":0,\"operation_id\":\"4\","
                + "\"changed\":[{\"mid\":123456789012345674}]}";
            zoolooser.add(
                pgNotify3,
                new ExpectingHttpItem(new JsonChecker(opQueueEnvelope3)),
                NotImplementedHttpItem.INSTANCE);
            zoolooser.add(
                pgNotify4,
                new ExpectingHttpItem(new JsonChecker(opQueueEnvelope4)),
                NotImplementedHttpItem.INSTANCE);

            zoolooser.start();
            msal.start();
            salo.start();
            long maxSleep = SLEEP_INTERVAL << 2;
            for (int i = (int) (maxSleep / DELAY); i >= 0; --i) {
                try {
                    Thread.sleep(DELAY);
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify0));
                    Assert.assertEquals(
                        1,
                        zoolooser.accessCount(pgPingNotify));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify3));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify4));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue0));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue1));
                    Assert.assertEquals(4, msal.accessCount(pgOpqueue1NoRo));
                    Assert.assertEquals(
                        2 + 1,
                        msal.accessCount(minTransactionDate));
                    break;
                } catch (AssertionError e) {
                    if (i == 0) {
                        throw e;
                    }
                }
            }
        }
    }

    @Test
    public void testPinholeAtStart() throws Exception {
        String pgShard = "324";
        try (StaticServer zoolooser = new StaticServer(
                Configs.baseConfig(ZOOLOOSER),
                TRANSFER_TIMESTAMP_ERASER);
            StaticServer msal = new StaticServer(Configs.baseConfig(MSAL));
            Server salo = new Server(config(
                msal.port(),
                zoolooser.port(),
                mdblist(""),
                mdblist(pgShard),
                100)
                .workersPerMdb(1)
                .workersLookahead(1)
                .midsLimit(1)
                .requestsBatchSize(1)
                .envelopesCheckInterval(SLEEP_INTERVAL)
                .lockCheckInterval(1000L)
                .build()))
        {
            String pgLockUri = PRODUCER_LOCK + "producer-name=pg324";
            zoolooser.add(pgLockUri, new StaticHttpItem("pg324@1"));
            String pgPos1 =
                "/_producer_position?service=change_log&producer-name=pg324:0";
            String pgPos2 =
                "/_producer_position?service=change_log&producer-name=pg324:1";
            zoolooser.add(pgPos1, new StaticHttpItem(Long.toString(0L)));
            zoolooser.add(pgPos2, new StaticHttpItem(Long.toString(-1L)));

            String opQueueRow1 =
                "\"operation_id\":\"1\",\"uid\":\"5598602\",\"lcn\": \"1\","
                + "\"change_type\":\"store\",\"operation_date\":\"1334567891"
                + ".123456\",\"changed\":[{\"mid\":133456789012345671}]";
            String opQueueRow2 =
                "\"operation_id\":\"2\",\"uid\":\"5598602\",\"lcn\": \"2\","
                + "\"change_type\":\"store\",\"operation_date\":\"1334567892"
                + ".123456\",\"changed\":[{\"mid\":133456789012345672}]";
            String opQueueRow3 =
                "\"operation_id\":\"3\",\"uid\":\"5598602\",\"lcn\": \"3\","
                + "\"change_type\":\"store\",\"operation_date\":\"1334567893"
                + ".123456\",\"changed\":[{\"mid\":133456789012345673}]";

            String pgOpqueueBase = OPQUEUE + "324&length=100&op-id=";
            String pgOpqueue1 = pgOpqueueBase + 1;
            // Initial request to replica
            msal.add(pgOpqueue1, pgData(opQueueRow1));
            // No pinhole detected, send to queue
            String notifySuffix =
                "&change-type=store&changed-size=1&split-offset=0&batch"
                + "-size=1&salo-worker=pg324:0";
            String pgNotify1 =
                "/notify?mdb=pg&pgshard=324&operation-id=1.000000&"
                + "operation-date=1334567891.123456&uid=5598602"
                + notifySuffix;
            zoolooser.add(
                pgNotify1,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"change_type\":\"store\",\"lcn\":\"1\""
                        + ",\"uid\":\"5598602\",\"operation_date\":"
                        + "\"1334567891.123456\",\"operation_part\":0,"
                        + "\"operation_id\":\"1\",\"changed\":"
                        + "[{\"mid\":133456789012345671}]}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);
            // Advance position and request once again
            String pgOpqueue2 = pgOpqueueBase + 2;
            String pgData2 = pgData(opQueueRow3);
            msal.add(pgOpqueue2, pgData2);
            // Pinhole detected, fallback to master
            // Since there is no transactions on server, data will be requested
            // again, after receiving min transaction date
            String pgOpqueue2NoRo = pgOpqueue2 + NO_RO;
            msal.add(
                pgOpqueue2NoRo,
                new StaticHttpItem(pgData(opQueueRow2, opQueueRow3)), // Gone
                NotImplementedHttpItem.INSTANCE);
            // op_ids 2, 3
            String pgNotify2 =
                "/notify?mdb=pg&pgshard=324&operation-id=2.000000&"
                + "operation-date=1334567892.123456&uid=5598602"
                + notifySuffix;
            String pgNotify3 =
                "/notify?mdb=pg&pgshard=324&operation-id=3.000000&"
                + "operation-date=1334567893.123456&uid=5598602"
                + notifySuffix;
            String opQueueEnvelope2 =
                "{\"change_type\":\"store\",\"lcn\":\"2\",\"uid\":\""
                + "5598602\",\"operation_date\":\"1334567892.123456\","
                + "\"operation_part\":0,\"operation_id\":\"2\",\""
                + "changed\":[{\"mid\":133456789012345672}]}";
            String opQueueEnvelope3 =
                "{\"change_type\":\"store\",\"lcn\":\"3\",\"uid\":\""
                + "5598602\",\"operation_date\":\"1334567893.123456\",\""
                + "operation_part\":0,\"operation_id\":\"3\",\""
                + "changed\":[{\"mid\":133456789012345673}]}";
            zoolooser.add(
                pgNotify2,
                new ExpectingHttpItem(new JsonChecker(opQueueEnvelope2)),
                NotImplementedHttpItem.INSTANCE);
            zoolooser.add(
                pgNotify3,
                new ExpectingHttpItem(new JsonChecker(opQueueEnvelope3)),
                NotImplementedHttpItem.INSTANCE);

            zoolooser.start();
            msal.start();
            salo.start();
            final long maxSleep = SLEEP_INTERVAL << 2;
            for (int i = (int) (maxSleep / DELAY); i >= 0; --i) {
                try {
                    Thread.sleep(DELAY);
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify1));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify2));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify3));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue1));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue2));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue2NoRo));
                    break;
                } catch (AssertionError e) {
                    if (i == 0) {
                        throw e;
                    }
                }
            }
        }
    }

    @Test
    public void testPinholeFailfast() throws Exception {
        String pgShard = "325";
        try (StaticServer zoolooser = new StaticServer(
                Configs.baseConfig(ZOOLOOSER),
                TRANSFER_TIMESTAMP_ERASER);
            StaticServer msal = new StaticServer(Configs.baseConfig(MSAL));
            Server salo = new Server(config(
                msal.port(),
                zoolooser.port(),
                mdblist(""),
                mdblist(pgShard),
                100)
                .workersPerMdb(1)
                .workersLookahead(1)
                .midsLimit(1)
                .requestsBatchSize(1)
                .envelopesCheckInterval(SLEEP_INTERVAL)
                .lockCheckInterval(1000L)
                .build()))
        {
            String pgLockUri = PRODUCER_LOCK + "producer-name=pg325";
            zoolooser.add(pgLockUri, new StaticHttpItem("pg325@1"));
            String pgPos1 =
                "/_producer_position?service=change_log&producer-name=pg325:0";
            String pgPos2 =
                "/_producer_position?service=change_log&producer-name=pg325:1";
            zoolooser.add(pgPos1, new StaticHttpItem("525"));
            zoolooser.add(pgPos2, new StaticHttpItem(Long.toString(-1L)));

            String opQueueRow526 =
                "\"operation_id\":\"526\",\"uid\":\"5598603\",\"lcn\":\"426\","
                + "\"change_type\":\"store\",\"operation_date\":\"1434567526"
                + ".143456\",\"changed\":[{\"mid\":\"143456789012345526\"}]";
            String opQueueRow527 =
                "\"operation_id\":\"527\",\"uid\":\"5598603\",\"lcn\":\"427\","
                + "\"change_type\":\"store\",\"operation_date\":\"1434567527"
                + ".143456\",\"changed\":[{\"mid\":\"143456789012345527\"}]";
            String opQueueRow528 =
                "\"operation_id\":\"528\",\"uid\":\"5598603\",\"lcn\":\"428\","
                + "\"change_type\":\"store\",\"operation_date\":\"1434567528"
                + ".143456\",\"changed\":[{\"mid\":\"143456789012345528\"}]";
            String opQueueRow530 =
                "\"operation_id\":\"530\",\"uid\":\"5598603\",\"lcn\":\"430\","
                + "\"change_type\":\"store\",\"operation_date\":\"1434567530"
                + ".143456\",\"changed\":[{\"mid\":\"143456789012345530\"}]";
            String opQueueRow531 =
                "\"operation_id\":\"531\",\"uid\":\"5598603\",\"lcn\":\"431\","
                + "\"change_type\":\"store\",\"operation_date\":\"1434567531"
                + ".143456\",\"changed\":[{\"mid\":\"143456789012345531\"}]";
            String opQueueRow532 =
                "\"operation_id\":\"532\",\"uid\":\"5598603\",\"lcn\":\"432\","
                + "\"change_type\":\"store\",\"operation_date\":\"1434567532"
                + ".143456\",\"changed\":[{\"mid\":\"143456789012345532\"}]";

            String pgOpqueueBase = OPQUEUE + "325&length=100&op-id=";
            String pgOpqueue526 = pgOpqueueBase + 526;
            // Initial request to replica
            msal.add(
                pgOpqueue526,
                pgData(opQueueRow526, opQueueRow530, opQueueRow531));
            // row 526 transferred
            String notifySuffix =
                '&' + "change-type=store&changed-size=1&split-offset=0"
                + "&batch-size=1&salo-worker=pg325:0";
            String pgNotify526 =
                "/notify?mdb=pg&pgshard=325&operation-id=526.000000&"
                + "operation-date=1434567526.143456&uid=5598603"
                + notifySuffix;
            zoolooser.add(
                pgNotify526,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"change_type\":\"store\",\"lcn\":\"426\","
                        + "\"uid\":\"5598603\",\"operation_date\":"
                        + "\"1434567526.143456\",\"operation_part\":0,"
                        + "\"operation_id\":\"526\","
                        + "\"changed\":[{\"mid\":\"143456789012345526\"}]}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);

            // Ping request will be generated
            String pgPingNotify =
                "/notify?mdb=pg&pgshard=325&salo-worker=pg325:0&operation-ids"
                + "=526-526&first-operation-date=0&optional&batch-size=1";
            zoolooser.add(pgPingNotify, HttpStatus.SC_OK);

            // Advance operation and request once again
            String pgOpqueue527 = pgOpqueueBase + 527;
            msal.add(pgOpqueue527, pgData(opQueueRow530, opQueueRow531));

            // Still have pinhole, fallback to master
            String pgOpqueue527NoRo = pgOpqueue527 + NO_RO;
            msal.add(
                pgOpqueue527NoRo,
                // Pinhole still here
                new StaticHttpItem(pgData(opQueueRow530, opQueueRow531)),
                // Request server transaction date and server timestamp
                // Store server timestamp, wait and request data once again
                new StaticHttpItem(
                    pgData(
                        opQueueRow527,
                        opQueueRow528,
                        opQueueRow530,
                        opQueueRow531,
                        opQueueRow532)),
                NotImplementedHttpItem.INSTANCE);

            String minTransactionDate = MIN_TRANSCATION_DATE + pgShard;
            msal.add(
                minTransactionDate,
                // Doesn't really matter, because of fail-fast
                new StaticHttpItem(
                    "{\"min_transaction_date\":1534343840.020,"
                    + "\"server_timestamp\":1534343844.123}"),
                NotImplementedHttpItem.INSTANCE);

            // Fail-fast occured, rows 527 and 528 will be transferred
            String pgNotify527 =
                "/notify?mdb=pg&pgshard=325&operation-id=527.000000&"
                + "operation-date=1434567527.143456&uid=5598603"
                + notifySuffix;
            zoolooser.add(
                pgNotify527,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"change_type\":\"store\",\"lcn\":\"427\","
                        + "\"uid\":\"5598603\",\"operation_date\":\"1434567527"
                        + ".143456\",\"operation_part\":0,"
                        + "\"operation_id\":\"527\",\"changed\":[{\"mid\":\""
                        + "143456789012345527\"}]}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);
            String pgNotify528 =
                "/notify?mdb=pg&pgshard=325&operation-id=528.000000&"
                + "operation-date=1434567528.143456&uid=5598603"
                + notifySuffix;
            zoolooser.add(
                pgNotify528,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"change_type\":\"store\",\"lcn\":\"428\","
                        + "\"uid\":\"5598603\",\"operation_date\":\"1434567528"
                        + ".143456\",\"operation_part\":0,\"operation_id\":\""
                        + "528\",\"changed\":[{\"mid\":\""
                        + "143456789012345528\"}]}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);

            zoolooser.start();
            msal.start();
            salo.start();
            long maxSleep = SLEEP_INTERVAL << 2;
            for (int i = (int) (maxSleep / DELAY); i >= 0; --i) {
                try {
                    Thread.sleep(DELAY);
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify526));
                    Assert.assertEquals(
                        1,
                        zoolooser.accessCount(pgPingNotify));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify527));
                    Assert.assertEquals(1, zoolooser.accessCount(pgNotify528));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue526));
                    Assert.assertEquals(1, msal.accessCount(pgOpqueue527));
                    Assert.assertEquals(2, msal.accessCount(pgOpqueue527NoRo));
                    Assert.assertEquals(
                        1,
                        msal.accessCount(minTransactionDate));
                    break;
                } catch (AssertionError e) {
                    if (i == 0) {
                        throw e;
                    }
                }
            }
        }
    }
    // CSON: MethodLength

    private static class MultipartAssert implements HttpRequestHandler {
        private final List<JsonChecker> envelopes;

        MultipartAssert(final String... envelopes) throws Exception {
            this.envelopes = new ArrayList<>(envelopes.length);
            for (String envelope: envelopes) {
                this.envelopes.add(new JsonChecker(envelope));
            }
        }

        private static String emptyBodyEraser(final String body) {
            if (body.isEmpty()) {
                return null;
            } else {
                return body;
            }
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws NotImplementedException
        {
            try {
                HttpEntity entity =
                    ((HttpEntityEnclosingRequest) request).getEntity();
                HttpAssert.assertMultipart(
                    entity,
                    envelopes,
                    MultipartAssert::emptyBodyEraser);
            } catch (Throwable t) {
                throw new NotImplementedException(t);
            }
        }
    }
}
// CSON: MagicNumber

