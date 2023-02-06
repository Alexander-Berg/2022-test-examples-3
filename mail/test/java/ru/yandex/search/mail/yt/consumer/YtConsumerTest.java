package ru.yandex.search.mail.yt.consumer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonString;
import ru.yandex.json.dom.PositionSavingContainerFactory;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.search.mail.yt.consumer.config.SourceConsumerConfig;
import ru.yandex.search.mail.yt.consumer.cypress.CypressNode;
import ru.yandex.search.mail.yt.consumer.cypress.NodeType;
import ru.yandex.search.mail.yt.consumer.upload.SourceConsumerFactory;
import ru.yandex.test.util.Checker;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.tskv.TskvRecord;

public class YtConsumerTest extends TestBase {
    private static final String UPDATE_METHOD = "update";
    private static final String NOTIFY_METHOD = "notify";
    private static final String SERVICE = "service";
    private static final String PREFIX = "prefix";
    private static final String METHOD = "method";
    private static final String DATA = "data";

    private static final String BP_SERVICE = "change_log";
    private static final String CORP_SERVICE = "corp_change_log";

    private static final long TIMEOUT = 5000;
    private static final long INTERVAL = 100;

    // CSOFF: MultipleStringLiterals
    // CSOFF: MagicNumber
    // CSOFF: MethodLength
    @Test
    public void testAlice() throws Exception {
        DateTimeFormatter formatter = ISODateTimeFormat.dateHourMinuteSecond();
        try (YtConsumerCluster cluster = new YtConsumerCluster()) {
            final String lockUri =
                "/_lock?service=incoming_iex"
                    + "&name=scheduler-ALICE&timeout=300000";
            cluster.producer().add(
                lockUri,
                "localhost_1519668090449_c0d981c4-20cc-491e-a139-2edc84c55dec");

            String date1 = DateTime.now().minusHours(1).toString(formatter);
            String date2 = DateTime.now().toString(formatter);

            SourceConsumerConfig aliceConfig = null;
            for (Map.Entry<SourceConsumerFactory, SourceConsumer> entry
                : cluster.ytProducer().consumers().entrySet())
            {
                if (entry.getKey() == SourceConsumerFactory.ALICE) {
                    aliceConfig = entry.getValue().config();
                    break;
                }
            }

            Assert.assertNotNull(aliceConfig);

            CypressNode aliceMonitorNode =
                cluster.yt().getNode(aliceConfig.monitorPath());

            Assert.assertSame(NodeType.MAP_NODE, aliceMonitorNode.type());

            CypressNode job1 = aliceMonitorNode.addTable(date1);
            CypressNode job2 = aliceMonitorNode.addTable(date2);
            TskvRecord record1 = new TskvRecord();
            record1.put("server_time", "1511684502");
            record1.put("uuid", "uu/515c9855187d7f6905b9e75895c13314");
            record1.put("request", "{\"additional_options\": {}}");
            record1.put("response", "{\"cards\": []}");
            record1.put("form", "{\"form\":\"\"}");
            record1.put(
                "form_name",
                "personal_assistant.handcrafted.userinfo_name");
            record1.put("provider", "vins");
            record1.put("type", "UTTERANCE");
            record1.put("app_id", "pa");
            record1.put("utterance_source", "voice");
            record1.put("location_lat", "43.92144012451172");
            record1.put("location_lon", "42.70939254760742");
            record1.put("lang", "ru-RU");
            record1.put("environment", "vins-int_stable");
            record1.put(
                "utterance_text",
                "ты скажи пожалуйста я плохая хорошая девочка");
            record1.put("client_time", "1511684498");
            job1.write(Collections.singletonList(tskvToJson(record1)));

            TskvRecord record2 = new TskvRecord();
            record2.put("server_time", "1511684503");
            record2.put("uuid", "uu/515c9855187d7f6905b9e75895c13315");
            record2.put("request", "{\"request\": {}}");
            record2.put(
                "form_name",
                "personal_assistant.handcrafted.userinfo_name");

            TskvRecord record3 = new TskvRecord();
            record3.put("server_time", "1511684504");
            record3.put("uuid", "uu/515c9855187d7f6905b9e75895c13316");
            record3.put("request", "{\"request\": {}}");
            record3.put(
                "form_name",
                "personal_assistant.handcrafted.userinfo_name");

            TskvRecord record4 = new TskvRecord();
            record4.put("server_time", "1511684505");
            record4.put("uuid", "uu/515c9855187d7f6905b9e75895c13317");
            record4.put("request", "{\"request\": {}}");
            record4.put(
                "form_name",
                "personal_assistant.handcrafted.userinfo_name");

            job2.write(
                Arrays.asList(
                    tskvToJson(record2),
                    tskvToJson(record3),
                    tskvToJson(record4)));

            cluster.start();

            String expBody1 =
                "{\"app_id\":\"pa\", \"client_time\":\"1511684498\", "
                    + "\"environment\":\"vins-int_stable\","
                    + "\"form\":\"{\\\"form\\\":\\\"\\\"}\",\"form_name\":"
                    + "\"personal_assistant.handcrafted.userinfo_name\", "
                    + "\"lang\":\"ru-RU\", "
                    + "\"location_lat\":\"43.92144012451172\", "
                    + "\"location_lon\":\"42.70939254760742\", "
                    + "\"prefix\":\"uu/515c9855187d7f6905b9e75895c13314\", "
                    + "\"provider\":\"vins\",\"request\":"
                    + "\"{\\\"additional_options\\\": {}}\", \"response\":"
                    + "\"{\\\"cards\\\": []}\", \"server_time\":\"1511684502\","
                    + "\"type\":\"UTTERANCE\", \"utterance_source\":\"voice\","
                    + "\"utterance_text\":\"ты скажи пожалуйста я плохая "
                    + "хорошая девочка\", \"uuid\":"
                    + "\"uu/515c9855187d7f6905b9e75895c13314\"}";

            Function<String, Checker> request2CheckerFactory = (boundary) ->
                new StringChecker("--" + boundary
                    + "\r\nZooShardId: 89511\r\n"
                    + "URI: /notify?service=incoming_iex&source=ytconsumer"
                    + "&path=//home/voice/vins/logs/15m/" + date2
                    + "&shard=89511&uuid=uu/515c9855187d7f6905b9e75895c13315"
                    + "\r\nContent-Disposition: form-data; "
                    + "name=\"envelope.json\"; filename=\"envelope.json\"\r\n"
                    + "Content-Type: application/json; charset=UTF-8\r\n"
                    + "Content-Transfer-Encoding: binary\r\n\r\n"
                    + "{\"prefix\":\"uu/515c9855187d7f6905b9e75895c13315\","
                    + "\"server_time\":\"1511684503\","
                    + "\"request\":\"{\\\"request\\\": {}}\","
                    + "\"uuid\":\"uu/515c9855187d7f6905b9e75895c13315\","
                    + "\"form_name\":\"personal_assistant.handcrafted"
                    + ".userinfo_name\"}\r\n--"
                    + boundary
                    + "\r\nZooShardId: 89510\r\n"
                    + "URI: /notify?service=incoming_iex&source=ytconsumer"
                    + "&path=//home/voice/vins/logs/15m/" + date2
                    + "&shard=89510&uuid=uu/515c9855187d7f6905b9e75895c13316"
                    + "\r\nContent-Disposition: form-data; "
                    + "name=\"envelope.json\"; filename=\"envelope.json\"\r\n"
                    + "Content-Type: application/json; charset=UTF-8\r\n"
                    + "Content-Transfer-Encoding: binary\r\n\r\n"
                    + "{\"prefix\":\"uu/515c9855187d7f6905b9e75895c13316\","
                    + "\"server_time\":\"1511684504\","
                    + "\"request\":\"{\\\"request\\\": {}}\","
                    + "\"uuid\":\"uu/515c9855187d7f6905b9e75895c13316\","
                    + "\"form_name\":"
                    + "\"personal_assistant.handcrafted.userinfo_name\"}\r\n--"
                    + boundary + "--\r\n");
            ExpectingHttpItem expecting1 =
                new ExpectingHttpItem(new JsonChecker(expBody1));

            String expBody3 =
                "{\"prefix\":\"uu/515c9855187d7f6905b9e75895c13317\","
                    + "\"server_time\":\"1511684505\",\"request\":\""
                    + "{\\\"request\\\": {}}\",\"uuid\":\""
                    + "uu/515c9855187d7f6905b9e75895c13317\","
                    + "\"form_name\":\"personal_assistant.handcrafted."
                    + "userinfo_name\"}";

            String notify1 =
                "/notify?service=incoming_iex&source=ytconsumer"
                    + "&path=//home/voice/vins/logs/15m/" + date1
                    + "&shard=89512&uuid=uu/515c9855187d7f6905b9e75895c13314";

            String notify2 =
                "/notify?service=incoming_iex&first-operation-date="
                    + "1511684503000&batch-size=2&consumer=ytConsumer"
                    + "&path=//home/voice/vins/logs/15m/" + date2
                    + "&jobId=0_" + date2 + "&ytPosition=0";

            String notify3 =
                "/notify?service=incoming_iex&source=ytconsumer&path="
                    + "//home/voice/vins/logs/15m/" + date2
                    + "&shard=89509&uuid=uu/515c9855187d7f6905b9e75895c13317";

            cluster.producer().add(notify1, expecting1);

            cluster.producer().add(
                notify2,
                new MultipartExpectingHttpItem(request2CheckerFactory));

            cluster.producer().add(notify3, expBody3);

            waitProducerRequests(cluster.producer(), notify1, 1);
            waitProducerRequests(cluster.producer(), notify2, 1);
            waitProducerRequests(cluster.producer(), notify3, 1);

            System.out.println(cluster.ytProducer().status(true));
            String completedPath = aliceConfig.basePath() + "/completed";

            System.out.println(cluster.yt().root());
            CypressNode job2Completed =
                cluster.yt().getNode(completedPath + '/' + date2);
            String scheduledPath = aliceConfig.basePath() + "/scheduled";

            long deadline = System.currentTimeMillis() + 10000;
            boolean got = false;
            while (deadline > System.currentTimeMillis()) {
                CypressNode scheduleNode = cluster.yt().getNode(scheduledPath);
                CypressNode completeNode = cluster.yt().getNode(completedPath);

                if (completeNode.children().size() == 1
                    && scheduleNode.children().size() == 0)
                {
                    got = true;
                    break;
                }
            }

            Assert.assertNotNull(job2Completed);
            Assert.assertEquals(NodeType.MAP_NODE, job2Completed.type());
            Assert.assertTrue(got);
        }
    }

    @Test
    public void testMobileActions() throws Exception {
        try (YtConsumerCluster cluster = new YtConsumerCluster()) {
            final String lockUri =
                "/_lock?service=change_log&name=scheduler-MOBILE_ACTIONS"
                    + "&timeout=300000";
            cluster.producer().add(
                lockUri,
                "localhost_1520668090449_c0d981c4-20cc-491e-a139-2edc84");

            SourceConsumerConfig actionsConfig = null;
            for (Map.Entry<SourceConsumerFactory, SourceConsumer> entry
                : cluster.ytProducer().consumers().entrySet())
            {
                if (entry.getKey() == SourceConsumerFactory.MOBILE_ACTIONS) {
                    actionsConfig = entry.getValue().config();
                    break;
                }
            }

            Assert.assertNotNull(actionsConfig);
            cluster.yt().create(actionsConfig.monitorPath(), NodeType.MAP_NODE);

            CypressNode actionsMonitorNode =
                cluster.yt().getNode(actionsConfig.monitorPath());

            Assert.assertSame(NodeType.MAP_NODE, actionsMonitorNode.type());

            TskvRecord record1 = new TskvRecord();
            String data =
                "{\"uid\": 51, \"change_type\":\"fields_update\","
                    + "\"operation_date\":1522077012,\"changed\":["
                    + "{\"mid\":\"165225811329408215\", "
                    + "\"clicks_total_count\":{\"function\":\"inc\"},"
                    + "\"senders\":{\"senders_from_read_count\":{"
                    + "\"function\":\"inc\"}}},{\"mid\":\"165225811329408238\","
                    + "\"clicks_total_count\":{\"function\":\"inc\"},"
                    + "\"senders\":{\"senders_from_read_count\":{"
                    + "\"function\":\"inc\"}}},{\"mid\":\"165225811329408288\","
                    + " \"clicks_total_count\":{\"function\":\"inc\"},"
                    + "\"senders\":{\"senders_from_read_count\":"
                    + "{\"function\":\"inc\"}}},{\"mid\":\"165225811329408295\""
                    + ",\"clicks_total_count\":{\"function\":\"inc\"},"
                    + "\"senders\":{\"senders_from_read_count\":"
                    + "{\"function\":\"inc\"}}}]}";

            record1.put(DATA, data);
            record1.put(METHOD, NOTIFY_METHOD);
            record1.put("mdb", "pg");
            record1.put("mid", "165225811329408288");
            record1.put("operation_time", "1522077012");
            record1.put("uid", "51");
            CypressNode table1 =
                actionsMonitorNode.addTable("mobile.2018-03-26.index");
            table1.write(Collections.singletonList(record1.toString()));

            String notify1 =
                "/notify?service=change_log&source=mobile_actions&mdb=pg&path="
                    + actionsConfig.monitorPath() + "/mobile.2018-03-26.index"
                    + "&tableOffset=0&shard=51&prefix=51";

            cluster.producer().add(notify1, new ExpectingHttpItem(data));
            cluster.start();
            waitProducerRequests(cluster.producer(), notify1, 1);
        }
    }
    // CSON: MethodLength
    // CSON: MultipleStringLiterals
    // CSON: MagicNumber

    private static final class MultipartExpectingHttpItem
        extends StaticHttpItem
    {
        private final Function<String, Checker> checkerFactory;

        private MultipartExpectingHttpItem(
            final Function<String, Checker> checkerFactory)
        {
            super(HttpStatus.SC_OK);

            this.checkerFactory = checkerFactory;
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            String contentType =
                request.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue();
            String boundary = contentType.split("boundary=")[1];

            if (request instanceof HttpEntityEnclosingRequest) {
                String body = CharsetUtils.toString(
                    ((HttpEntityEnclosingRequest) request).getEntity());
                String result = checkerFactory.apply(boundary).check(body);
                if (result != null) {
                    throw new NotImplementedException(
                        "For '" + request.getRequestLine().getUri()
                            + '\'' + ' ' + result);
                }
            } else {
                throw new NotImplementedException(
                    "Entity enclosing request expected");
            }

            super.handle(request, response, context);
        }
    }

    private static String tskvToJson(final TskvRecord record) throws Exception {
        JsonMap json = new JsonMap(PositionSavingContainerFactory.INSTANCE);
        for (Map.Entry<String, String> entry: record.entrySet()) {
            json.put(entry.getKey(), new JsonString(entry.getValue()));
        }

        StringBuilderWriter writer =
            new StringBuilderWriter(new StringBuilder());
        try (JsonWriter jsonWriter = JsonType.NORMAL.create(writer)) {
            json.writeValue(jsonWriter);
        }

        return writer.toString();
    }

    @Ignore
    @Test
    public void test() throws Exception {
        System.setProperty("BSCONFIG_IPORT", "0");
        System.setProperty("BSCONFIG_INAME", "ytproducer");

        final String prefix1 = "1130000022103350";
        final String prefix2 = "1130000020301497";
        try (YtConsumerCluster cluster = new YtConsumerCluster()) {
            CypressNode node =
                cluster.yt().create(
                    "monitorPath",
                    NodeType.MAP_NODE);

            CypressNode job = node.addTable("job.index");
            String data1 = "{\"uid\":" + prefix1
                + ",\"change_type\":\"fields_update\","
                + "\"operation_date\":2147483647,"
                + "\"changed\":["
                + "{\"mid\":\"164099911422323153\","
                + "\"clicks_total_count\":{\"function\":\"inc\"},"
                + "\"senders\":{"
                + "\"senders_from_read_count\":{\"function\":\"inc\"}}}]}";
            String data2 = "{\"prefix\":" + prefix2
                + ",\"AddIfNotExists\":true,"
                + "\"docs\":["
                + "{\"url\":\"umtype_" + prefix2
                + "\",\"mtype_show_count\":{\"function\":\"sum_map\","
                + "\"args\":[\"4\\t1\\nall\\t1\","
                + "{\"function\":\"get\","
                + "\"args\":[\"mtype_show_count\"]}]}},"
                + "{\"url\":\"reqs_" + prefix2 + "_belirsiz alacak\","
                + "\"request_mids\":\"157907461934678046\"}]}";

            TskvRecord record1 = new TskvRecord();
            record1.put(PREFIX, prefix1);
            record1.put(SERVICE, BP_SERVICE);
            record1.put(METHOD, NOTIFY_METHOD);
            record1.put(DATA, data1);
            TskvRecord record2 = new TskvRecord();
            record2.put(PREFIX, prefix2);
            record2.put(SERVICE, CORP_SERVICE);
            record2.put(METHOD, UPDATE_METHOD);
            record2.put(DATA, data2);

            job.write(Arrays.asList(tskvToJson(record1), tskvToJson(record2)));
            final String path1 = "job.index.lock";
            String producerUri1 =
                producerUri(NOTIFY_METHOD, BP_SERVICE, prefix1, path1, 0);

            String producerUri2 =
                producerUri(UPDATE_METHOD, CORP_SERVICE, prefix2, path1, 1);
            System.out.println("ProducerURI2 " + producerUri2);
            cluster.producer().add(
                producerUri1,
                new ExpectingHttpItem(new JsonChecker(data1)));

            cluster.producer().add(
                producerUri2,
                new ExpectingHttpItem(new JsonChecker(data2)));
            cluster.start();

            waitProducerRequests(cluster.producer(), producerUri1, 1);
            waitProducerRequests(cluster.producer(), producerUri2, 1);

            Thread.sleep(INTERVAL);
            checkNoTables(cluster.yt().root());

            //try second job with consumer fail
            final String path2 = "second.job.index.lock";
            String producerUri3 =
                producerUri(NOTIFY_METHOD, BP_SERVICE, prefix1, path2, 1);
            String producerUri4 =
                producerUri(UPDATE_METHOD, CORP_SERVICE, prefix2, path2, 0);

            cluster.producer().add(
                producerUri4,
                new ChainedHttpResource(
                    new StaticHttpItem(HttpStatus.SC_INTERNAL_SERVER_ERROR),
                    new ExpectingHttpItem(new JsonChecker(data2))));

            cluster.producer().add(
                producerUri3,
                new ExpectingHttpItem(new JsonChecker(data1)));

            job = node.addTable("second.job.index");
            job.write(Arrays.asList(record2.toString(), record1.toString()));

            waitProducerRequests(cluster.producer(), producerUri4, 2);
            waitProducerRequests(cluster.producer(), producerUri3, 1);

            Thread.sleep(INTERVAL);
            checkNoTables(cluster.yt().root());

            final String errorMessage = "Expecting 1 requests for ";
            Assert.assertEquals(
                errorMessage + producerUri1,
                1,
                cluster.producer().accessCount(producerUri1));
            Assert.assertEquals(
                errorMessage + producerUri2,
                1,
                cluster.producer().accessCount(producerUri2));
            Assert.assertEquals(
                errorMessage + producerUri3,
                1,
                cluster.producer().accessCount(producerUri3));
            Assert.assertEquals(
                errorMessage + producerUri4,
                2,
                cluster.producer().accessCount(producerUri4));
        }
    }

    //CSOFF: ParameterNumber
    private static String producerUri(
        final String method,
        final String service,
        final String prefix,
        final String path,
        final int offset)
    {
        return '/' + method + "?&service=" + service + "&prefix=" + prefix
            + "&ytconsumer=//home/mail-search/index/" + path
            + "&ytOffset=" + offset + "&uid=" + prefix + "&mdb=pg"
            + "&changed-size=1";
    }
    //CSON: ParameterNumber

    private static void waitProducerRequests(
        final StaticServer producer,
        final String uri,
        final int count)
        throws Exception
    {
        long start = System.currentTimeMillis();
        while (producer.accessCount(uri) < count) {
            Thread.sleep(INTERVAL);
            if (System.currentTimeMillis() - start > TIMEOUT) {
                throw new TimeoutException(
                    "Expecting " + count
                        + " requests to " + uri
                        + " but got " + producer.accessCount(uri));
            }
        }
    }

    private static void checkNoTables(final CypressNode node) throws Exception {
        Assert.assertNotEquals(
            "Table still left in yt " + node.path(),
            node.type(),
            NodeType.TABLE);

        for (CypressNode child: node.children().values()) {
            checkNoTables(child);
        }
    }
}
