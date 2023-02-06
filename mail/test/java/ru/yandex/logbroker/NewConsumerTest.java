package ru.yandex.logbroker;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.concurrent.TimeFrameQueue;
import ru.yandex.function.BasicGenericConsumer;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.http.util.server.DefaultHttpServerFactory;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.json.dom.ValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.xpath.ValueUtils;
import ru.yandex.logbroker.client.LogbrokerConsumerCluster;
import ru.yandex.logbroker.client.LogbrokerConsumerClusterBuilder;
import ru.yandex.logbroker.client.LogbrokerDcBalancer.TopicMeta;
import ru.yandex.logbroker.client.LogbrokerMetaServer;
import ru.yandex.logbroker.client.LogbrokerNodeServer.TestPartition;
import ru.yandex.logbroker.config.ClientConfigBuilder;
import ru.yandex.logbroker.config.ImmutableLogbrokerConsumerServerConfig;
import ru.yandex.logbroker.config.LogConfigBuilder;
import ru.yandex.logbroker.config.blackbox.OnlineUsersStorageConfigBuilder;
import ru.yandex.logbroker.log.consumer.LogConsumerFactoryType;
import ru.yandex.logbroker.log.consumer.blackbox.BlackboxConsumerContext;
import ru.yandex.logbroker.log.consumer.blackbox.BlackboxEventsParser;
import ru.yandex.logbroker.log.consumer.blackbox.SessionCheck;
import ru.yandex.logbroker.server.LogbrokerConsumerServer;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.stater.ImmutableStatersConfig;
import ru.yandex.stater.StaterConfigBuilder;
import ru.yandex.stater.StatersConfigBuilder;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.tskv.InplaceTskvParser;
import ru.yandex.tskv.TskvRecord;

public class NewConsumerTest extends ConsumerTestBase {
    /**
     * We have to declare a lot of connections
     * due to killing politics of BaseHttpServer
     * when limit is reached, it will clean all keep-alive
     * connections
     */
    private static final int MAX_CONNECTIONS = 20;

    private static final String BLACKBOX = "blackbox";
    private static final String BLACKBOX_IDENT = BLACKBOX;
    private static final String BLACKBOX_LOG_TYPE = "blackbox-log";

    private static final String INTEGRATION = "integration";
    private static final String INTEGRATION_IDENT = "mail";
    private static final String INTEGRATION_LOG_TYPE = "mail-jsintegration-log";

    // CSOFF: MagicNumber
    // CSOFF: MultipleStringLiterals

    protected ImmutableStatersConfig createStaters(
        final String prefix)
        throws ConfigException
    {
        StatersConfigBuilder stsb = new StatersConfigBuilder();
        final String uri = '/' + prefix;

        StaterConfigBuilder stb = new StaterConfigBuilder();
        stb.prefix(prefix);
        stsb.staters().put(new Pattern<>(uri, true), stb);

        return stsb.build();
    }

    @Ignore
    public void testBlackbox() throws Exception {
        LogConfigBuilder blackboxConfig = new LogConfigBuilder();
        ClientConfigBuilder clientConfig = new ClientConfigBuilder();
        clientConfig.clientId("debug").waitTimeout(500)
            .sessionHttpConfig().connections(1);

        blackboxConfig.name(BLACKBOX).clientConfig(clientConfig.build());
        blackboxConfig.ident(BLACKBOX_IDENT);
        blackboxConfig.logType(BLACKBOX_LOG_TYPE);
        blackboxConfig.consumerFactory(LogConsumerFactoryType.BLACKBOX);

        LogbrokerConsumerClusterBuilder clusterBuilder =
            new LogbrokerConsumerClusterBuilder();

        try (StaticServer userOnline = new StaticServer(Configs.baseConfig());
             StringReader config = new StringReader(
                 "connections = 2\n"
                 + "majormap.content = major host:"
                     + userOnline.host().getHostName()
                     + ",search_port:" + userOnline.port()
                     + ",search_port_ng:" + userOnline.port()
                     + ",json_indexer_port:" + userOnline.port()
                     + ",shards:0-65533");

             LogbrokerConsumerCluster cluster = clusterBuilder
                 .logs(blackboxConfig.consumerFactorySection(
                     new IniConfig(config)))
                 .build())
        {
            cluster.start();

            TopicMeta topic =
                cluster.emulator().dc(LogbrokerConsumerCluster.SAS)
                    .addTopic(BLACKBOX_IDENT, BLACKBOX_LOG_TYPE, 2);
            TestPartition partition =
                cluster.emulator().dc(LogbrokerConsumerCluster.SAS)
                    .partition(topic, 0);
            long ts = System.currentTimeMillis() / 1000;
            String record0 = "tskv\tsource_uri=prt://blackbox@pass-f1.sezam"
                + ".yandex.net/opt/sezam-logs/blackbox-statbox"
                + ".log\tiso_eventtime=2017-07-18 "
                + "05:10:01\t\ttable=blackbox-log/2017-07-18\tsubkey"
                + "=\ttskv_format=blackbox-log\tunixtime="
                + ts
                + "\taction"
                + "=sescheck\tuids=1130000022110715,221107\t"
                + "host=mail.yandex.ru\tuserip=195.239.97.214\t"
                + "_stbx=rt3.man--blackbox--blackbox-log:0:437084:";
            partition.write(record0);
            String record1 = "tskv\tsource_uri=prt://blackbox@pass-f1.sezam"
                + ".yandex.net/opt/sezam-logs/blackbox-statbox"
                + ".log\tiso_eventtime=2017-07-18 "
                + "05:10:01\t\ttable=blackbox-log/2017-07-18\tsubkey"
                + "=\ttskv_format=blackbox-log\tunixtime="
                + ts
                + "\taction"
                + "=sescheck\tuids=221107,759\tyuid=888\t"
                + "host=mail.yandex.ru\tuserip=195.239.97.214\t"
                + "_stbx=rt3.man--blackbox--blackbox-log:0:437084:";

            partition.write(record1, false);

            ExpectingHttpItem expected = new ExpectingHttpItem(
                new JsonChecker(
                "{\"1130000022110715\":{\"ts\":"
                    + ts * 1000 + ",\"yuids\":[]},"
                    + "\"221107\": {\"ts\":"
                    + ts * 1000 + ",\"yuids\":[\"888\"]},"
                    + "\"759\": {\"ts\":"
                    + ts * 1000 + ",\"yuids\":[\"888\"]}}"));

            String uri1 = "/update?&service=major&prefix=759";
            userOnline.add(uri1, expected);

            userOnline.start();
            long startTs = System.currentTimeMillis();
            while (userOnline.accessCount(uri1) < 1
                && System.currentTimeMillis() - startTs < 10000)
            {
                Thread.sleep(100);
            }

            Assert.assertEquals(userOnline.accessCount(uri1), 1);

            String record2 = "tskv\tsource_uri=prt://blackbox@pass-f1.sezam"
                + ".yandex.net/opt/sezam-logs/blackbox-statbox"
                + ".log\tiso_eventtime=2017-07-18 "
                + "05:10:01\t\ttable=blackbox-log/2017-07-18\tsubkey"
                + "=\ttskv_format=blackbox-log\tunixtime="
                + ts
                + "\taction"
                + "=sescheck\tuids=900,1000\tyuid=456\t"
                + "host=mail.yandex.ru\tuserip=195.239.97.214\t"
                + "_stbx=rt3.man--blackbox--blackbox-log:0:437084:";
            partition.write(record2);
            String record3 = "tskv\tsource_uri=prt://blackbox@pass-f1.sezam"
                + ".yandex.net/opt/sezam-logs/blackbox-statbox"
                + ".log\tiso_eventtime=2017-07-18 "
                + "05:10:01\t\ttable=blackbox-log/2017-07-18\tsubkey"
                + "=\ttskv_format=blackbox-log\tunixtime="
                + ts
                + "\taction"
                + "=sescheck\tuids=1000\tyuid=77\t"
                + "host=mail.yandex.ru\tuserip=195.239.97.214\t"
                + "_stbx=rt3.man--blackbox--blackbox-log:0:437084:";

            partition.write(record3, false);

            expected = new ExpectingHttpItem(
                new JsonChecker(
                "{\"900\": {\"ts\":"
                    + ts * 1000 + ", \"yuids\":[\"456\"]},"
                    + "\"1000\": {\"ts\":"
                    + ts * 1000 + ", \"yuids\":[\"456\", \"77\"]}}"));
            String uri2 = "/update?&service=major&prefix=1000";
            userOnline.add(uri2, expected);

            startTs = System.currentTimeMillis();
            while (userOnline.accessCount(uri2) < 1
                && System.currentTimeMillis() - startTs < 10000)
            {
                Thread.sleep(100);
            }

            Assert.assertEquals(userOnline.accessCount(uri2), 1);

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(
                     new HttpGet(cluster.server().host() + "/stat")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String content =
                    CharsetUtils.toString(response.getEntity());
                BasicGenericConsumer<Object, JsonException> consumer =
                    new BasicGenericConsumer<>();

                ValueContentHandler.prepareParser(consumer).parse(content);
                List<?> statList = ValueUtils.asList(consumer.get());
                Map<String, Object> stat = new HashMap<>();
                for (Object item : statList) {
                    List<?> statItem = ValueUtils.asList(item);
                    stat.put(statItem.get(0).toString(), statItem.get(1));
                }

                Object parsed = stat.get("blackbox-parser-records_ammm");
                Assert.assertEquals(4, ValueUtils.asLong(parsed));
            }
        }
    }

    //CSOFF: MethodLength
    @Ignore
    @Test
    public void testIntegration() throws Exception {
        LogConfigBuilder integrationConfig = new LogConfigBuilder();
        ClientConfigBuilder clientConfig = new ClientConfigBuilder();
        clientConfig.clientId("debug")
            .waitTimeout(500)
            .sessionHttpConfig().connections(1);

        final String ident = INTEGRATION_IDENT;
        final String logType = INTEGRATION_LOG_TYPE;

        integrationConfig.name(INTEGRATION).clientConfig(clientConfig.build());
        integrationConfig.ident(ident);
        integrationConfig.logType(logType);
        integrationConfig.consumerFactory(LogConsumerFactoryType.INTEGRATION);

        LogbrokerConsumerClusterBuilder clusterBuilder =
            new LogbrokerConsumerClusterBuilder();

        try (StaticServer producer = new StaticServer(Configs.baseConfig());
             StringReader config = new StringReader(
                 "connections = 1\n"
                 + "host = " + producer.host() + '\n'
                 + "notify.indexUri = /notify?\n"
                 + "update.indexUri = /update?");
             LogbrokerConsumerCluster cluster = clusterBuilder
                 .logs(integrationConfig.consumerFactorySection(
                    new IniConfig(config)))
                 .build())
        {
            TopicMeta topic =
                cluster.emulator().dc("sas").addTopic(ident, logType, 2);
            TestPartition partition =
                cluster.emulator().dc("sas").partition(topic, 0);

            // first record

            String record0 =
                "tskv\ttskv_format=mail-jsintegration-log\ttimestamp="
                    + "2018-03-18 04:30:24\ttimezone=+0300\tunixtime="
                    + "1521336624\tuid=61470200\tside=web\texp="
                    + "63581,70306,68094,68646\taction=show\tplatform=Win32\t"
                    + "filter={\"page\":\"messages\",\"type\":\"thread\","
                    + "\"param\":\"t164944336352445412\"}\t"
                    + "messages={\"count\":0,\"threads\":0}";
            String record1 =
                "tskv\ttskv_format=mail-jsintegration-log\ttimestamp="
                    + "2018-03-18 05:57:59\ttimezone=+0300\tunixtime=1521341879"
                    + "\tuid=1130000025645713\tside=web\texp="
                    + "61000,53505,60671,55591\taction=show\tplatform=Win32\t"
                    + "filter={\"page\":\"message\",\"type\":null,"
                    + "\"param\":null}\tmessage={\"thread\":0,\"timestamp\":"
                    + "1521119738000,\"type\":[23,53],\"from\":"
                    + "\"tech@skyeng.ru\",\"index\":2,\"subject\":\""
                    + "Не могу назначить урок ученику\",\"folder\":{"
                    + "\"fid\":\"1\",\"symbol\":\"inbox\"},"
                    + "\"mid\":\"164944336352445079\"}";

            String record2 =
                "tskv\ttskv_format=mail-jsintegration-log\ttimestamp="
                    + "2018-03-18 05:57:39\ttimezone=+0300\tunixtime=1521341860"
                    + "\tuid=307290083\tside=web\texp=55591\taction=show"
                    + "\tplatform=Win32\tfilter={\"page\":\"message\","
                    + "\"type\":\"search\",\"param\":\"request="
                    + "no-reply%40ruweber.ru&scope=hdr_from&reqid=\"}"
                    + "\tmessage={\"thread\":0,\"timestamp\":1516099132000,"
                    + "\"type\":[7,54,65],\"from\":\"no-reply@ruweber.ru\","
                    + "\"index\":1,\"subject\":\"Приглашение на мероприятие\","
                    + "\"folder\":{\"fid\":\"3\",\"symbol\":\"trash\"},"
                    + "\"mid\":\"164381386399023249\"}";

            partition.write(record0);
            partition.write(record1, false);
            partition.write(record2, false);

            String notify0 =
                "/notify?&service=change_log&prefix=61470200"
                    + "&mdb=pg&logbroker=click_consumer";
            producer.add(
                notify0,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"uid\":61470200,\"change_type\":"
                            + "\"fields_update\",\"operation_date\":1521336624,"
                            + "\"changed\":[{\"mid\":\"164944336352445412\","
                            + "\"clicks_total_count\":{\"function\":\"inc\"}"
                            + "}]}"
                    )));
            String notify1 =
                "/notify?&service=change_log&prefix=1130000025645713"
                    + "&mdb=pg&logbroker=click_consumer";
            producer.add(
                notify1,
                new ExpectingHttpItem(
                    new JsonChecker(
                    "{\"uid\":1130000025645713,\"change_type\":"
                        + "\"fields_update\",\"operation_date\":1521341879,"
                        + "\"changed\":[{\"mid\":\"164944336352445079\","
                        + "\"clicks_total_count\":{\"function\":\"inc\"},"
                        + "\"senders\":{\"senders_from_read_count\":"
                        + "{\"function\":\"inc\"}}}]}")));

            String update2 =
                "/update?&service=change_log&prefix=307290083"
                    + "&logbroker=update_consumer";
            producer.add(
                update2,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"prefix\":307290083,\"AddIfNotExists\":true"
                            + ",\"docs\":[{\"url\":\"umtype_307290083\","
                            + "\"mtype_show_count\":{\"function\":\"sum_map\","
                            + "\"args\":[\"7,54,65\\t1\\nall\\t1\",{"
                            + "\"function\":\"get\",\"args\":["
                            + "\"mtype_show_count\"]}]}},{\"url\":"
                            + "\"reqs_307290083_no-reply@ruweber.ru\","
                            + "\"type\":\"mail_request_history\","
                            + "\"request_mids\":\"164381386399023249\"}]}"
                    )));

            String notify2 = "/notify?&service=change_log&prefix=307290083"
                + "&mdb=pg&logbroker=click_consumer";
            producer.add(
                notify2,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"uid\":307290083,\"change_type\":"
                            + "\"fields_update\",\"operation_date\":1521341860,"
                            + "\"changed\":[{\"mid\":\"164381386399023249\","
                            + "\"clicks_serp_count\":{\"function\":\"inc\"},"
                            + "\"clicks_total_count\":{\"function\":\"inc\"},"
                            + "\"senders\":{\"senders_from_read_count\":"
                            + "{\"function\":\"inc\"}}}]}")));
            //record0
            producer.start();
            cluster.start();

            waitForRequest(producer, notify0, 10000);
            waitForRequest(producer, notify1, 10000);
            waitForRequest(producer, notify2, 10000);
            waitForRequest(producer, update2, 10000);

            // test that after we still working in topic
            // tskv format
            String tskv1 = "tskv\ttskv_format=mail-jsintegration-log\t"
                + "timestamp=2018-01-16 13:26:04\ttimezone=+0300\t"
                + "unixtime=1516098365\tuid=12879447\tside=web\texp=60110\t"
                + "action=show\tplatform=Win32\tfilter={\"page\":\"message\","
                + "\"type\":\"search\",\"param\":\"request=iepirkumi%40ims.lv"
                + "&reqid=151609836612212879447\"}\t"
                + "message={\"thread\":0,\"timestamp\":1502354000000,"
                + "\"type\":[4,13,54],\"from\":\"info@regrand.eu\","
                + "\"index\":8,\"subject\":\"regrand\",\"folder\":{"
                + "\"fid\":\"4\",\"symbol\":\"sent\"},"
                + "\"mid\":\"162974011515502546\"}";

            String tskvBad1 =
                "tskv\tdaemon=mail.jsintegration\tsuid=-\tmdb=-\t"
                    + "timestamp=2017-12-20 16:21:59\ttimezone=+03:00\t"
                    + "type=YANDEXUID_WRONG\tuid=-\tsome_id=759058\ttext=\t"
                    + "tskv_format=mail-jsintegration-log\tunixtime=1511262885";

            String tskvNotify1 = '{'
                + "    \"uid\": 12879447,"
                + "    \"operation_date\": 1516098365.000,"
                + "    \"change_type\": \"fields_update\","
                + "    \"changed\": ["
                + "        {"
                + "            \"mid\": \"162974011515502546\","
                + "            \"clicks_serp_count\": {\"function\": \"inc\"},"
                + "            \"clicks_total_count\": {\"function\": \"inc\"},"
                + "            \"senders\": {"
                + "                 \"senders_from_read_count\":"
                + "                             {\"function\": \"inc\"}"
                + "             }"
                + "        }"
                + "    ]"
                + '}';

            String tskvUpdateUrl1 =
                "/update?&service=change_log"
                    + "&prefix=12879447&logbroker=update_consumer";

            String tskvUpdate1 = "{\n"
                + "  \"prefix\": 12879447,\n"
                + "  \"AddIfNotExists\":true,\n"
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_12879447\",\n"
                + "       \"mtype_show_count\": {\"function\": \"sum_map\", "
                + "\"args\": [\"4,13,54\\t1\\nall\\t1\",{\"function\": \"get\","
                + "\"args\": [\"mtype_show_count\"]}]}\n"
                + "    }, {"
                + "       \"url\": \"reqs_12879447_iepirkumi@ims.lv\","
                + "       \"type\":\"mail_request_history\","
                + "       \"request_mids\": \"162974011515502546\""
                + "    }"
                + "]\n}";

            partition.write(tskvBad1);
            partition.write(tskv1);

            String kamajiTskvUri1 = "/notify?&service=change_log"
                + "&prefix=12879447&mdb=pg&logbroker=click_consumer";

            StaticHttpResource resTskv1 = new StaticHttpResource(
                new ExpectingHttpItem(new JsonChecker(tskvNotify1)));

            StaticHttpResource resTskv2 = new StaticHttpResource(
                new ExpectingHttpItem(new JsonChecker(tskvUpdate1)));

            producer.add(kamajiTskvUri1, resTskv1);
            producer.add(tskvUpdateUrl1, resTskv2);

            waitForRequests(resTskv1, 1, 10000);
            waitForRequests(resTskv2, 1, 10000);

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(
                     new HttpGet(cluster.server().host() + "/stat")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String content = CharsetUtils.toString(response.getEntity());
                BasicGenericConsumer<Object, JsonException> consumer =
                    new BasicGenericConsumer<>();

                ValueContentHandler.prepareParser(consumer).parse(content);
                List<?> statList = ValueUtils.asList(consumer.get());
                Map<String, Object> stat = new HashMap<>();
                for (Object item: statList) {
                    List<?> statItem = ValueUtils.asList(item);
                    stat.put(statItem.get(0).toString(), statItem.get(1));
                }
            }
        }
    }

    //CSON: MethodLength
    @Test
    public void testDcParse() throws Exception {
        String dumpJson = "{\n"
            + "  \"@class\" : \"ru.yandex.iss.Instance\",\n"
            + "  \"slot\" : \"24840@ws25-322.search.yandex.net\",\n"
            + "  \"configurationId\" : "
            + "\"mail#mail-logbroker-consumer-prod-1470147174677\",\n"
            + "  \"targetState\" : \"ACTIVE\",\n"
            + "  \"targetStateOperationId\" : "
            + "\"4b773ba0-58bc-11e6-98cf-9960f363a00e\",\n"
            + "  \"transitionTimestamp\" : 1470147642458,\n"
            + "  \"properties\" : {\n"
            + "    \"BSCONFIG_IHOST\" : \"ws25-322\",\n"
            + "    \"BSCONFIG_INAME\" : \"ws25-322:24840\",\n"
            + "    \"BSCONFIG_IPORT\" : \"24840\",\n"
            + "    \"BSCONFIG_ITAGS\" : \"MSK_MYT_MSEARCH_LOG_BROKER "
            + "a_ctype_none a_dc_myt a_geo_msk a_itype_msearchlogbroker "
            + "a_line_myt-5 a_metaprj_unknown a_prj_none "
            + "a_sandbox_task_65769820 a_tier_none a_topology_cgset-memory"
            + ".limit_in_bytes=5473566720 a_topology_cgset-memory"
            + ".low_limit_in_bytes=5368709120 "
            + "a_topology_group-MSK_MYT_MSEARCH_LOG_BROKER "
            + "a_topology_trunk-2417596 a_topology_version-trunk-2417596 "
            + "cgset_memory_recharge_on_pgfault_1\",\n"
            + "    \"BSCONFIG_SHARDDIR\" : \"./\",\n"
            + "    \"BSCONFIG_SHARDNAME\" : \"\",\n"
            + "    \"NANNY_SERVICE_ID\" : \"mail-logbroker-consumer-prod\",\n"
            + "    \"annotated_ports\" : \"{}\",\n"
            + "    \"tags\" : \"MSK_MYT_MSEARCH_LOG_BROKER a_ctype_none "
            + "a_dc_myt a_geo_msk a_itype_msearchlogbroker a_line_myt-5 "
            + "a_metaprj_unknown a_prj_none a_sandbox_task_65769820 a_tier_none"
            + " a_topology_cgset-memory.limit_in_bytes=5473566720 "
            + "a_topology_cgset-memory.low_limit_in_bytes=5368709120 "
            + "a_topology_group-MSK_MYT_MSEARCH_LOG_BROKER "
            + "a_topology_trunk-2417596 a_topology_version-trunk-2417596 "
            + "cgset_memory_recharge_on_pgfault_1\"\n"
            + "  }}";

        Map<?, ?> dump = ValueUtils.asMap(new JSONParser().parse(dumpJson));
        Assert.assertEquals(
            "myt",
            LogbrokerConsumer.parseDcFromJson(dump));
    }
    // CSON: MagicNumber
    // CSON: MultipleStringLiterals

    @Ignore
    public void testConfig() throws Exception {
        System.setProperty("BSCONFIG_IPORT", "0");
        System.setProperty("BSCONFIG_INAME", "localhost:0");
        System.setProperty("MAJOR_SEARCHMAP_PATH", ".");

        final String path =
            "src/logbroker-consumer/main/bundle/logbroker-consumer-prod.conf";

        Set<String> dcs =
            new HashSet<>(
                Arrays.asList(
                    LogbrokerConsumerCluster.SAS,
                    LogbrokerConsumerCluster.IVA,
                    LogbrokerConsumerCluster.MAN));

        LogbrokerConsumerCluster.setUpDns(Collections.singleton("meta"), 0);
        LogbrokerConsumerCluster.setUpDns(dcs, 0);

        try (LogbrokerMetaServer emulator =
            new LogbrokerMetaServer(
                new BaseServerConfigBuilder(Configs.baseConfig("LogbrokerMeta"))
                    .connections(MAX_CONNECTIONS).port(0).build(),
                dcs,
                2);
            HttpServer<ImmutableLogbrokerConsumerServerConfig, Object> server =
                LogbrokerConsumerServer.main(
                    new LogbrokerTestHttpServerFactory(emulator.port()),
                    new File(path).getAbsolutePath());
            CloseableHttpClient client = HttpClients.createDefault())
        {
            // ms for wait after read timeout expired
            final long extraWait = 100;
            final long waitTimeout =
                server.config()
                    .consumerConfig().logConfig().get(BLACKBOX)
                    .clientConfig().waitTimeout()
                    + extraWait;

            TopicMeta bbTopic =
                emulator.dc(LogbrokerConsumerCluster.SAS)
                    .addTopic(BLACKBOX_IDENT, BLACKBOX_LOG_TYPE, 2);
            TestPartition bbPartition =
                emulator.dc(LogbrokerConsumerCluster.SAS)
                    .partition(bbTopic, 0);
            String data = "tskv\ttskv_format=blackbox-log\tunixtime="
                + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
                + "\taction=sescheck\tuids=227356512\t"
                + "def_uid=227356512\thost=mail.yandex.ru\t"
                + "userip=85.31.113.224";

            bbPartition.write(data);
            emulator.start();

            Thread.sleep(waitTimeout);
            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(server.host() + "/stat")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String content = CharsetUtils.toString(response.getEntity());
                BasicGenericConsumer<Object, JsonException> consumer =
                    new BasicGenericConsumer<>();

                ValueContentHandler.prepareParser(consumer).parse(content);
                List<?> statList = ValueUtils.asList(consumer.get());
                Map<String, Object> stat = new HashMap<>();
                for (Object item: statList) {
                    List<?> statItem = ValueUtils.asList(item);
                    stat.put(statItem.get(0).toString(), statItem.get(1));
                }

                Assert.assertEquals(
                    0L,
                    ValueUtils.asLong(
                        stat.get("blackbox-parser-errors_ammm")));
                Assert.assertEquals(
                    1L,
                    ValueUtils.asLong(
                        stat.get("blackbox-parser-records_ammm")));
            }
        }
    }

    protected static void patchConfig(final IniConfig config, final int port) {
        // remove logs
        config.sections().remove("log");
        config.sections().remove("log./consumer");
        config.sections().remove("accesslog");
        config.sections().remove("stdout");
        config.sections().remove("stderr");
        IniConfig majormap = config.section("topic.blackbox.consumer.majormap");
        majormap.put("content", "");
        majormap.put("file", null);
        config.sections().get("logbroker")
            .put("host", "meta.localhost:" + port);

        config.put("stop-timeout", "1000");
        // no dump.json, so force dc
        config.put("dc", "sas");
    }

    private static final class LogbrokerTestHttpServerFactory
        extends DefaultHttpServerFactory
        <ImmutableLogbrokerConsumerServerConfig>
    {
        private final int emulatorPort;

        private LogbrokerTestHttpServerFactory(final int emulatorPort) {
            super(LogbrokerConsumerServer.class.getName());

            this.emulatorPort = emulatorPort;
        }

        @Override
        public HttpServer<ImmutableLogbrokerConsumerServerConfig, Object>
            create(final IniConfig config)
            throws ConfigException, IOException
        {
            patchConfig(config, emulatorPort);
            return super.create(config);
        }
    }

    @Ignore
    public void testBlackboxParse() throws Exception {
        BlackboxConsumerContext context =
            new BlackboxConsumerContext(
                new TimeFrameQueue<>(1L),
                new TimeFrameQueue<>(1L),
                null,
                new OnlineUsersStorageConfigBuilder().connections(2).searchmap(
                    new SearchMapConfigBuilder().content(
                        "major host:localhost:80,search_port:80,search_port_ng"
                            + ":80,json_indexer_port:80,shards:0-65533"))
                    .build(),
                new PrefixedLogger(Logger.getAnonymousLogger(), "", ""));
        InplaceTskvParser tskvParser = new InplaceTskvParser();

        BlackboxEventsParser parser = new BlackboxEventsParser(context);
        String authEventStr =
            "tskv\ttskv_format=blackbox-log\taction=auth"
                + "\tstatus=successful\tcomment=clid"
                + "=7a54f58d4ebe431caaaa53895522bf2d;tokid=618124907;"
                + "devid=7d417ae8d57946525de21b6c9d019dc7;devnm=Lenovo+A316i;"
                + "scope=cloud_api:disk.app_folder,cloud_api:disk.read,"
                + "cloud_api:disk.write,mobile:all,mobmail:all,yadisk:disk;"
                + "\tuid=227356512\tunixtime=1501850113\tclient_name=bb\t"
                + "user_ip=31.173.81.183\tversion=1\thost_id=DF\t"
                + "type=oauthcheck";

        final long uid = 227356512L;
        final long ts1 = 1501850113L;
        TskvRecord authEvent =
            tskvParser.next(new StringReader(authEventStr));
        SessionCheck check = parser.parse(authEvent);

        Assert.assertEquals(check.host(), "mobile_Lenovo+A316i");
        Assert.assertEquals(check.uid().size(), 1);
        Assert.assertEquals(check.uid().get(0).longValue(), uid);
        Assert.assertEquals(check.ts(), ts1);

        String sescheckEventStr = "tskv\ttskv_format=blackbox-log\tunixtime"
            + "=1501822042\taction=sescheck\tuids=227356512\tdef_uid=227356512"
            + "\thost=mail.yandex.ru\tuserip=85.31.113.224";

        final long ts2 = 1501822042L;
        tskvParser = new InplaceTskvParser();
        TskvRecord sescheckEvent =
            tskvParser.next(new StringReader(sescheckEventStr));
        check = parser.parse(sescheckEvent);
        Assert.assertEquals("mail.yandex.ru", check.host());
        Assert.assertEquals(check.uid().size(), 1);
        Assert.assertEquals(check.uid().get(0).longValue(), uid);
        Assert.assertEquals(check.ts(), ts2);

        authEventStr = "tskv\ttskv_format=blackbox-log\taction=auth\tstatus"
            + "=failed\tcomment=whitelisted;"
            + "\tuid=198669434\tunixtime=1502104221\tclient_name=bb\tuser_ip=37"
            + ".140.190.58\tversion=1\tsid=2\thost_id=EC\tlogin=gorsvet"
            + ".31@yandex.ru\ttype=imap";
        authEvent =
            tskvParser.next(new StringReader(authEventStr));
        Assert.assertNull(parser.parse(authEvent));
        //check = parser.parse(authEvent);
    }
}
