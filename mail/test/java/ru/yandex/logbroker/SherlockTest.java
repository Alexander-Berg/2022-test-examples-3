package ru.yandex.logbroker;

import java.io.StringReader;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.logbroker.client.LogbrokerConsumerCluster;
import ru.yandex.logbroker.client.LogbrokerConsumerClusterBuilder;
import ru.yandex.logbroker.client.LogbrokerDcBalancer;
import ru.yandex.logbroker.client.LogbrokerNodeServer;
import ru.yandex.logbroker.config.ClientConfigBuilder;
import ru.yandex.logbroker.config.LogConfigBuilder;
import ru.yandex.logbroker.log.consumer.LogConsumerFactoryType;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.JsonChecker;

public class SherlockTest extends ConsumerTestBase {
    // CSOFF: MultipleStringLiterals
    // CSOFF: MagicNumber
    @Ignore
    @Test
    public void test() throws Exception {
        LogConfigBuilder sherlockConfig = new LogConfigBuilder();
        ClientConfigBuilder clientConfig = new ClientConfigBuilder();
        clientConfig.clientId("debug").waitTimeout(500)
            .sessionHttpConfig().connections(1);

        sherlockConfig.name("sherlock-templates")
            .clientConfig(clientConfig.build());

        sherlockConfig.ident("mail");
        sherlockConfig.logType("");
        sherlockConfig.consumerFactory(
            LogConsumerFactoryType.SHERLOCK_TEMPLATES);

        LogbrokerConsumerClusterBuilder clusterBuilder =
            new LogbrokerConsumerClusterBuilder();

        String record1 = "{\"templates\": [{\"stable_sign\": "
            + "855224474536176741, \"similarity\": 0.447214}, "
            + "{\"stable_sign\": 809932751949272728, \"similarity\": "
            + "0.447214}, {\"stable_sign\": 424699650391545294, "
            + "\"similarity\": 0.447214}, {\"stable_sign\": "
            + "392248405108207500, \"similarity\": 0.447214}, "
            + "{\"stable_sign\": 41064330791599617, \"similarity\": "
            + "0.447214}, {\"stable_sign\": 90089033004598577, "
            + "\"similarity\": 0.424264}, {\"stable_sign\": "
            + "1105614760552867490, \"similarity\": 0.402492}, "
            + "{\"stable_sign\": 1070358837228270124, \"similarity\": "
            + "0.402492}, {\"stable_sign\": 957600293265546856, "
            + "\"similarity\": 0.402492}, {\"stable_sign\": "
            + "760185669019561771, \"similarity\": 0.402492}, "
            + "{\"stable_sign\": 725225959908295942, \"similarity\": "
            + "0.402492}, {\"stable_sign\": 662481989879606027, "
            + "\"similarity\": 0.402492}, {\"stable_sign\": "
            + "647116902175280327, \"similarity\": 0.402492}, "
            + "{\"stable_sign\": 478804718536650820, \"similarity\": "
            + "0.402492}, {\"stable_sign\": 360343137707168773, "
            + "\"similarity\": 0.402492}, {\"stable_sign\": "
            + "284794543780637722, \"similarity\": 0.402492}, "
            + "{\"stable_sign\": 228555057236670585, \"similarity\": "
            + "0.402492}, {\"stable_sign\": 23222990850918802, "
            + "\"similarity\": 0.402492}, {\"stable_sign\": "
            + "16616271521577713, \"similarity\": 0.402492}], \"uid\": "
            + "483498602, \"stid\": "
            + "\"320.mail:483498602.E1041787:90192746287096997619648666534\","
            + " \"session_id\": \"IeXgEOLO\", \"datetime\": "
            + "\"2018-07-31T06:59:02.489086\", \"apphost_request_id\": "
            + "\"b7159fab-15a5-7291-6c6f-a41b008a0b05\", \"unixtime_ms\": "
            + "1533009542489, \"logger_name\": \"mail.apphost"
            + ".template_identifier.lib.apphost_service\", \"message\": "
            + "\"Templates found\"}";

        String record2 = "{\"templates\": [{\"stable_sign\": "
            + "1111659461105764417, \"similarity\": 0.534522}],"
            + "\"uid\": 135753993, \"session_id\": \"0f7A9Y2b\", "
            + "\"datetime\": \"2018-07-31T06:59:02.651658\", "
            + "\"apphost_request_id\": \"c1ba5b54-fa25-3804-f386-298400bccb07\""
            + ",\"unixtime_ms\":1533009542651, \"logger_name\": \"mail.apphost"
            + ".template_identifier.lib.apphost_service\", \"message\": "
            + "\"Templates found\"}";
        String record3 = "{\"uid\": 135753993, \"session_id\":\"0f7A9Y2b\","
            + "\"datetime\": \"2018-07-31T06:59:02.651658\", "
            + "\"stid\": "
            + "\"320.mail:135753993.E1042664:420281507784224926734445401019\","
            + "\"apphost_request_id\": \"c1ba5b54-fa25-3804-f386-298400bccb07\""
            + ",\"unixtime_ms\":1533009542651, \"logger_name\": \"mail.apphost"
            + ".template_identifier.lib.apphost_service\", \"message\": "
            + "\"Templates found\"}";
        String record5 = "{\"templates\": [{\"stable_sign\": "
            + "1111659461105764417, \"similarity\": 0.534522}], "
            + "\"uid\": 135753993, \"stid\": "
            + "\"320.mail:135753993.E1042664:420281507784224926734445401019"
            + "\", \"session_id\": \"0f7A9Y2b\", \"datetime\": "
            + "\"2018-07-31T06:59:02.651658\", \"apphost_request_id\": "
            + "\"c1ba5b54-fa25-3804-f386-298400bccb07\", \"unixtime_ms\": "
            + "1533009542651, \"logger_name\": \"mail.apphost"
            + ".template_identifier.lib.apphost_service\", \"message\": "
            + "\"Templates found\"}";

        String expected1 =
            "{\"prefix\":483498602,\"AddIfNotExists\":true"
                + ",\"PreserveFields\": [\"sherlock_mid\"]"
                + ",\"docs\":[{\"url\":\"shrlck_483498602_320.mail"
                + ":483498602.E1041787:90192746287096997619648666534\","
                + "\"sherlock_templates\":{\"function\":\"make_set\","
                + "\"args\":[\"855224474536176741\n809932751949272728"
                + "\n424699650391545294\n392248405108207500\n"
                + "41064330791599617\n90089033004598577\n1105614760552867490\n"
                + "1070358837228270124\n957600293265546856\n"
                + "760185669019561771\n725225959908295942\n"
                + "662481989879606027\n647116902175280327\n478804718536650820"
                + "\n360343137707168773\n284794543780637722\n"
                + "228555057236670585\n23222990850918802\n16616271521577713"
                + "\",{\"function\":\"get\",\"args\":["
                + "\"sherlock_templates\"]}]}}]}";
        String expected2 =
            "{\"prefix\":135753993,\"AddIfNotExists\":true"
                + ",\"PreserveFields\": [\"sherlock_mid\"]"
                + ",\"docs\":[{\"url\":\"shrlck_135753993_320."
                + "mail:135753993.E1042664:420281507784224926734445401019\","
                + "\"sherlock_templates\":{\"function\":\"make_set\","
                + "\"args\":[\"1111659461105764417\",{"
                + "\"function\":\"get\",\"args\":[\"sherlock_templates\"]"
                + "}]}}]}";

        try (StaticServer producer = new StaticServer(Configs.baseConfig());
             StringReader config = new StringReader(
                 "connections = 1\n"
                     + "host = " + producer.host());
             LogbrokerConsumerCluster cluster = clusterBuilder
                 .logs(sherlockConfig.consumerFactorySection(
                     new IniConfig(config)))
                 .build())
        {
            final String ident = "mail";
            final String logType = "template-identifier-log";

            LogbrokerDcBalancer.TopicMeta topic =
                cluster.emulator().dc("sas").addTopic(ident, logType, 2);
            LogbrokerNodeServer.TestPartition partition =
                cluster.emulator().dc("sas").partition(topic, 0);
            partition.write(record1, true);
            partition.write(record2, false);
            partition.write(record3, false);
            partition.write(record5, false);

            String update0 =
                "/update?&service=change_log&prefix=483498602"
                    + "&logbroker=sherlock_consumer";

            producer.add(
                update0,
                new ExpectingHttpItem(new JsonChecker(expected1)));

            String update1 =
                "/update?&service=change_log&prefix=135753993"
                    + "&logbroker=sherlock_consumer";

            producer.add(
                update1,
                new ExpectingHttpItem(new JsonChecker(expected2)));

            producer.start();
            cluster.start();

            waitForRequest(producer, update0, 10000);
        }
    }
    // CSON: MultipleStringLiterals
    // CSON: MagicNumber
}
