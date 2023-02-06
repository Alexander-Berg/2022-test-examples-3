package ru.yandex.mail.so2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.JsonSubsetChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class So2Test extends TestBase {
    private static final String SENDERS_URI =
        "/api/async/senders?names-max=1&json-type=dollar";

    public So2Test() {
        super(false, 0L);
        System.setProperty("ADDITIONAL_CONFIG", "empty.conf");
    }

    @Test
    public void test() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.start();
            cluster.check(
                "/antispam?CONNECT=sas2-823da8e01fdd.qloud-c.yandex.net+"
                + "%5B2a02%3A6b8%3Ac08%3Abc8e%3A0%3A640%3A823d%3Aa8e0%5D+"
                + "QID%3D&HELO=sas2-823da8e01fdd.qloud-c.yandex.net"
                + "&MAILFROM=root%40server."
                + "local+SIZE%3D648+frm%3Droot%40server.local&RCPTTO=shev%40"
                + "liner-tour.ru+ID%3D1130000030210774+UID%3D1130000013010799"
                + "+COUNTRY%3Dru",
                loadResourceAsString("test.eml"),
                loadResourceAsString("test-so-response.json"));
        }
    }

    @Test
    public void testProtobufJson() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=5598601,203889311",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "analizer-hirthwork-blackbox-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/antispam?session_id=aIZo3O77Ey-Os7i02gq"
                + "&format=protobuf-json",
                loadResourceAsString("protobuf-request.json"),
                loadResourceAsString("protobuf-response.json"));
        }
    }

    @Test
    public void testProtobufJsonWithoutUid() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=analizer@yandex.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "analizer-blackbox-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=hirthwork@yandex.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "hirthwork-blackbox-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/antispam?session_id=aIZo3O77Ey-Os7i02gq"
                + "&format=protobuf-json",
                loadResourceAsString("protobuf-request-without-uid.json"),
                loadResourceAsString("protobuf-response.json"));
        }
    }

    @Test
    public void testSenders() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI + "&sender-uid=67061655",
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            loadResourceAsString("senders-request1.json")),
                        loadResourceAsString("senders-response1.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.SENDERS_TVM_TICKET));
            cluster.senders().add(
                "/api/async/so/get-dkim-stats"
                + "?timestamp=1583399650"
                + "&from=arttech051@gmail.com"
                + "&dkim-domain=n-seminar.com",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        "{\"dkim_stats\":"
                        + "{\"total\":5,\"dkimless\":2,\"top_domains\":[3,1],"
                        + "\"best_domain\":3}}"),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.SENDERS_TVM_TICKET));
            cluster.bigb().add(
                "/bigb?client=so&format=protobuf&puid=67061655",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        Files.readAllBytes(resource("my-crypta.pb"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BIGB_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=realmanometr@yandex.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "realmanometr-blackbox-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=me@yandex.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString("me-blackbox-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.templateMaster().add(
                "/route?domain=yandex.ru",
                new StaticHttpResource(HttpStatus.SC_OK));
            cluster.yadisk().add(
                "/v1/disk/public/resources?limit=1&public_key="
                + "gXlZAIHr3vzYttb1f4NmJ3+K2hW8AQOpsiexETbjA1bKQ30EA4hYLakGqs2"
                + "pz1E1q/J6bpmRyOJonT3VoXnDag==",
                loadResourceAsString("yadisk-hash-info.json"));
            cluster.yadisk().add(
                "/v1/disk/public/resources?limit=1"
                + "&public_key=http%3A//yadi.sk/d/7z8lkn5sDfqa8Q",
                loadResourceAsString("yadisk-info.json"));
            cluster.start();

            String uri =
                "/antispam?CONNECT=forward100j.mail.yandex.net+"
                + "%5B5.45.198.240%5D+QID%3D&HELO=forward100j.mail.yandex.net"
                + "&MAILFROM=Me%2Btag1%2Btag2%40ya.ru+SIZE%3D256167"
                + "&RCPTTO=realmanometr%40yandex.ru+COUNTRY%3Dru&only-so2";
            HttpPost post = new HttpPost(cluster.so2().host() + uri);
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("senders1.eml"),
                    StandardCharsets.UTF_8));
            try (CloseableHttpClient client = Configs.createDefaultClient();
                CloseableHttpResponse response = client.execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("senders-so-response1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
            Assert.assertEquals(
                0,
                cluster.templateMaster().accessCount(
                    "/route?domain=yandex.ru"));
        }
    }

    @Test
    public void testSendersResolve() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI,
                new ExpectingHttpItem(
                    new JsonChecker(
                        loadResourceAsString("senders-request2.json")),
                    loadResourceAsString("senders-response2.json")));
            cluster.senders().add(
                "/api/async/so/get-dkim-stats"
                + "?timestamp=1583399650"
                + "&from=arttech051@gmail.com",
                "{\"dkim_stats\":"
                + "{\"total\":5,\"dkimless\":2,\"top_domains\":[3]}}");
            // last uid comes first, other uids saves order
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=1130000043768820,67061658",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "admin@reinventedcode."
                             + "com-blackbox-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=analizer@yandex.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "analizer-blackbox-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=dpotapov@yandex-team.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "dpotapov-blackbox-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.CORP_BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=external-user@gmail.com",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "blackbox-empty-response.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.activityShingler().add(
                "/api/v1",
                new ExpectingHttpItem(
                    new JsonChecker(
                        "[{\"type\":\"Get\","
                        + "\"scheme\":[\"activity\",\"compls\"],\"fields\":["
                        + "{\"uid\":5598601},"
                        + "{\"uid\":1120000000004695},"
                        + "{\"uid\":67061658},"
                        + "{\"uid\":1130000043768820}]}]"),
                    loadResourceAsString("activity-response.json")));

            cluster.start();

            // Only 50 recipients will present in userinfos list
            String uri =
                "/antispam?CONNECT=forward100j.mail.yandex.net+"
                + "%5B5.45.198.240%5D+QID%3D&HELO=forward100j.mail.yandex.net"
                + "&MAILFROM=Mail%40prom-kip.ru+SIZE%3D256167"
                + "&RCPTTO=Analizer%2Btag1@yandex.ru"
                + "&RCPTTO=dpotapov@ld.yandex.ru"
                + "&RCPTTO=realmanometr%40yandex.ru+ID%3D183442461+"
                + "UID%3D67061658+COUNTRY%3Dru"
                + "&RCPTTO=admin@reinventedcode.com+UID%3D1130000043768820"
                + "&RCPTTO=external-user01@gmail.com"
                + "&RCPTTO=external-user06@gmail.com"
                + "&RCPTTO=external-user07@gmail.com"
                + "&RCPTTO=external-user08@gmail.com"
                + "&RCPTTO=external-user09@gmail.com"
                + "&RCPTTO=external-user10@gmail.com"
                + "&RCPTTO=external-user11@gmail.com"
                + "&RCPTTO=external-user12@gmail.com"
                + "&RCPTTO=external-user13@gmail.com"
                + "&RCPTTO=external-user14@gmail.com"
                + "&RCPTTO=external-user15@gmail.com"
                + "&RCPTTO=external-user16@gmail.com"
                + "&RCPTTO=external-user17@gmail.com"
                + "&RCPTTO=external-user18@gmail.com"
                + "&RCPTTO=external-user19@gmail.com"
                + "&RCPTTO=external-user20@gmail.com"
                + "&RCPTTO=external-user21@gmail.com"
                + "&RCPTTO=external-user22@gmail.com"
                + "&RCPTTO=external-user23@gmail.com"
                + "&RCPTTO=external-user24@gmail.com"
                + "&RCPTTO=external-user25@gmail.com"
                + "&RCPTTO=external-user26@gmail.com"
                + "&RCPTTO=external-user27@gmail.com"
                + "&RCPTTO=external-user28@gmail.com"
                + "&RCPTTO=external-user29@gmail.com"
                + "&RCPTTO=external-user30@gmail.com"
                + "&RCPTTO=external-user31@gmail.com"
                + "&RCPTTO=external-user32@gmail.com"
                + "&RCPTTO=external-user33@gmail.com"
                + "&RCPTTO=external-user34@gmail.com"
                + "&RCPTTO=external-user35@gmail.com"
                + "&RCPTTO=external-user36@gmail.com"
                + "&RCPTTO=external-user37@gmail.com"
                + "&RCPTTO=external-user38@gmail.com"
                + "&RCPTTO=external-user39@gmail.com"
                + "&RCPTTO=external-user40@gmail.com"
                + "&RCPTTO=external-user41@gmail.com"
                + "&RCPTTO=external-user42@gmail.com"
                + "&RCPTTO=external-user43@gmail.com"
                + "&RCPTTO=external-user44@gmail.com"
                + "&RCPTTO=external-user45@gmail.com"
                + "&RCPTTO=external-user46@gmail.com"
                + "&RCPTTO=external-user47@gmail.com"
                + "&RCPTTO=external-user48@gmail.com"
                + "&RCPTTO=external-user49@gmail.com"
                + "&RCPTTO=external-user50@gmail.com"
                + "&RCPTTO=external-user51@gmail.com"
                + "&RCPTTO=external-user52@gmail.com"
                + "&RCPTTO=external-user53@gmail.com"
                + "&RCPTTO=external-user54@gmail.com"
                + "&RCPTTO=external-user55@gmail.com"
                + "&RCPTTO=external-user56@gmail.com"
                + "&RCPTTO=external-user57@gmail.com"
                + "&RCPTTO=external-user58@gmail.com"
                + "&RCPTTO=external-user59@gmail.com";
            cluster.check(
                uri,
                loadResourceAsString("senders2.eml"),
                loadResourceAsString("senders-so-response2.json"));
            String stats = HttpAssert.stats(cluster.so2().host());
            HttpAssert.assertStat(
                "so2-senders-pfilters-resolution-null_ammm",
                Integer.toString(3),
                stats);
            HttpAssert.assertStat(
                "so2-senders-pfilters-resolution-ham_ammm",
                Integer.toString(1),
                stats);
            HttpAssert.assertStat(
                "so2-senders-pfilters-resolution-total_ammm",
                Integer.toString(4),
                stats);
        }
    }

    @Test
    public void testEmptySenders() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI + "&sender-uid=1120000000004695",
                new ExpectingHttpItem(
                    new JsonChecker(
                        Files.readString(resource("senders-request4.json"))),
                    Files.readString(resource("senders-response4.json"))));
            cluster.bigb().add(
                "/bigb?client=so&format=protobuf&puid=1120000000004695",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        Files.readAllBytes(resource("no-crypta.pb"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BIGB_TVM_TICKET));
            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=1120000000004695",
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            Files.readString(
                                resource("dpotapov-blackbox-userinfo.json"))),
                        YandexHeaders.X_YA_SERVICE_TICKET,
                        So2Cluster.CORP_BLACKBOX_TVM_TICKET)));
            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=dpotapov@yandex-team.ru",
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            Files.readString(
                                resource("dpotapov-blackbox-userinfo.json"))),
                        YandexHeaders.X_YA_SERVICE_TICKET,
                        So2Cluster.CORP_BLACKBOX_TVM_TICKET)));
            cluster.start();

            cluster.check(
                "/antispam?CONNECT=forward100j.mail.yandex.net+"
                + "%5B5.45.198.240%5D+QID%3D&HELO=forward100j.mail.yandex.net"
                + "&MAILFROM=ID%3D12345+UID%3D1120000000004695+SIZE%3D256167"
                + "&RCPTTO=dpotapov@yandex-team.ru+COUNTRY%3Dru",
                loadResourceAsString("senders4.eml"),
                loadResourceAsString("senders-so-response4.json"));
        }
    }

    @Test
    public void testSendersPop3() throws Exception {
        // Also tests TemplateMaster
        try (So2Cluster cluster = new So2Cluster(this)) {
            String templateMasterHandle = "/route?domain=loveeto.ru"
                + "&attributes=%7B%22from%22:%22notifications@loveeto.ru%22,"
                + "%22subject%22:%22%E2%9A%A1+1+%D0%BD%D0%BE%D0%B2%D0%BE%D0%B5"
                + "+%D1%81%D0%BE%D0%BE%D0%B1%D1%89%D0%B5%D0%BD%D0%B8%D0%B5+%D0"
                + "%BE%D1%82+%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%"
                + "D1%82%D0%B5%D0%BB%D1%8F+%D0%9E%D0%BB%D1%8C%D0%B3%D0%B0+%22,"
                + "%22queueId%22:%22%22,%22uids%22:%5B44627449%5D%7D";

            cluster.templateMaster().add(
                templateMasterHandle,
                new StaticHttpItem(
                    "{\"status\":\"FoundInDb\","
                    + "\"delta\":[[\"\r\nHello, world\"],[]],"
                    + "\"attributes\":["
                    + "{\"from\":null,"
                    + "\"subject\":null,"
                    + "\"queue_id\":\"MANUALLY CREATED\"}],"
                    + "\"stable_sign\":663465322314928363}"));

            cluster.senders().add(
                SENDERS_URI,
                new ExpectingHttpItem(
                    new JsonChecker(
                        Files.readString(
                            resource("senders-request-pop3.json"))),
                    Files.readString(resource("senders-response-pop3.json"))));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=44627449",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("twirl-team-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=5SvVDWQYSd-vLbC45lA"
                + "&CONNECT=del5.i.mail.ru+%5B217.69.138.7%5D+QID%3D"
                + "&HELO=DEL5.I.MAIL.RU"
                + "&MAILFROM=Notifications@loveeto.ru+SIZE%3D16391"
                + "&RCPTTO=twirl-team@yandex.ru+ID%3D119060630+UID%3D44627449"
                + "+COUNTRY%3Dru",
                loadResourceAsString("pop3.eml"),
                loadResourceAsString("pop3-so-response.json"),
                true);
            String stats = HttpAssert.stats(cluster.so2().host());
            HttpAssert.assertStat(
                "so2-node-unperson_pure_body-factors-extracted_dmmm",
                Integer.toString(2),
                stats);
            Assert.assertEquals(
                1,
                cluster.templateMaster().accessCount(
                    templateMasterHandle));
        }
    }

    @Test
    public void testSendersTrustedZoneOldPF() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI + "&sender-uid=67061659",
                new ExpectingHttpItem(
                    new JsonChecker(
                        Files.readString(resource("senders-request3.json"))),
                    Files.readString(resource("senders-response3.json"))));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=realmanometr@yandex.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("realmanometr-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=mail@prom-kip.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource(
                                "mail@prom-kip.ru-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));

            cluster.start();

            cluster.check(
                "/antispam?CONNECT=forward100j.mail.yandex.net+"
                + "%5B5.45.198.240%5D+QID%3D&HELO=forward100j.mail.yandex.net"
                + "&MAILFROM=Mail%40prom-kip.ru+SIZE%3D256167"
                + "&RCPTTO=realmanometr%40yandex.ru+COUNTRY%3Dru",
                loadResourceAsString("senders3.eml"),
                loadResourceAsString("senders-so-response3.json"));
        }
    }

    @Test
    public void testSendersBadRcptTo() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI,
                new StaticHttpResource(HttpStatus.SC_OK));

            String rcpttoBlackboxUrl =
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=root@localhost";
            cluster.blackbox().add(
                rcpttoBlackboxUrl,
                new StaticHttpResource(HttpStatus.SC_OK));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=me@yandex.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("me-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/antispam?CONNECT=forward100j.mail.yandex.net+"
                + "%5B5.45.198.240%5D+QID%3D&HELO=forward100j.mail.yandex.net"
                + "&MAILFROM=Me%2Btag1%2Btag2%40ya.ru+SIZE%3D256167"
                + "&RCPTTO=root@localhost+COUNTRY%3Dru",
                loadResourceAsString("senders1.eml"),
                loadResourceAsString("senders-so-response-bad-rcptto.json"));
            Assert.assertEquals(0, cluster.senders().accessCount(SENDERS_URI));
            Assert.assertEquals(
                0,
                cluster.blackbox().accessCount(rcpttoBlackboxUrl));
        }
    }

    @Test
    public void testSendersImap() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI,
                new ExpectingHttpItem(
                    new JsonChecker(
                        Files.readString(
                            resource("senders-request-imap.json"))),
                    Files.readString(resource("senders-response-imap.json"))));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=673525683",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource(
                                "ilyushenkovalera-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=iZIoVxoy3Y-N2HasNbu&"
                + "CONNECT=vla1-07e6959f7382.qloud-c.yandex.net+%5B2a02%3A6b8"
                + "%3Ac0d%3A3ea2%3A0%3A640%3A7e6%3A959f%5D+QID%3D&"
                + "HELO=vla1-07e6959f7382.qloud-c.yandex.net&"
                + "MAILFROM=ilyushenkovalera@yandex.ru+SIZE%3D30328&"
                + "RCPTTO=ilyushenkovalera@yandex.ru+ID%3D1130902819+"
                + "UID%3D673525683+COUNTRY%3Dru",
                loadResourceAsString("imap.eml"),
                loadResourceAsString("imap-so-response.json"),
                true);
        }
    }

    @Test
    public void testSendersImapGmail() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI,
                new ExpectingHttpItem(
                    new JsonChecker(
                        Files.readString(
                            resource("senders-request-imap2.json"))),
                    Files.readString(resource("senders-response-imap2.json"))));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=787564844",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource(
                                "drumeagabriel-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=bqG39z2aOH-ZaVihixG"
                + "&CONNECT=sas1-98019faccb8c.qloud-c.yandex.net+%5B2a02%3A6b8"
                + "%3Ac14%3A3906%3A0%3A640%3A9801%3A9fac%5D+QID%3D"
                + "&HELO=sas1-98019faccb8c.qloud-c.yandex.net"
                + "&MAILFROM=drumeagabriel@yandex.ru+SIZE%3D9127"
                + "&RCPTTO=drumeagabriel@yandex.ru+ID%3D1226455164+"
                + "UID%3D787564844+COUNTRY%3Dus",
                loadResourceAsString("imap2.eml"),
                loadResourceAsString("imap2-so-response.json"),
                true);
        }
    }

    @Test
    public void testSendersImapMailRu() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI,
                new ExpectingHttpItem(
                    new JsonChecker(
                        Files.readString(
                            resource("senders-request-imap3.json"))),
                    Files.readString(
                        resource("senders-response-imap3.json"))));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=863536366",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("pooh-panda-1-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));

            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=cSFgxRPzLl-kUWmufQB&CONNECT"
                + "=iva2-ee06083a3b08.qloud-c.yandex.net+%5B2a02%3A6b8%3Ac0c"
                + "%3A1991%3A0%3A640%3Aee06%3A83a%5D+QID%3D"
                + "&HELO=iva2-ee06083a3b08.qloud-c.yandex.net"
                + "&MAILFROM=pooh-panda-1@yandex.ru+SIZE%3D128843"
                + "&RCPTTO=pooh-panda-1@yandex.ru+ID%3D1287556906+"
                + "UID%3D863536366+COUNTRY%3Dru",
                loadResourceAsString("imap3.eml"),
                "{}",
                true);
            Assert.assertEquals(1, cluster.senders().accessCount(SENDERS_URI));
        }
    }

    @Test
    public void testSendersTcpInfoRdns() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI,
                new ExpectingHttpItem(
                    new JsonChecker(
                        Files.readString(
                            resource("senders-request-tcp-info-rdns.json"))),
                    Files.readString(
                        resource("senders-response-tcp-info-rdns.json"))));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=1130000034982945",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource(
                                "info@sutochno.ru-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=5pNc1V9Q85-TFke4JuM&CONNECT"
                + "=d42050.acod.regrucolo.ru+%5B178.21.15.101%5D+QID%3D"
                + "&HELO=sutochno.ru&MAILFROM=%3C%3E+SIZE%3D3776"
                + "&RCPTTO=info@sutochno.ru+ID%3D1130000052185338+"
                + "UID%3D1130000034982945+COUNTRY%3Dru",
                loadResourceAsString("tcp-info-rdns.eml"),
                loadResourceAsString("tcp-info-rdns-so-response.json"),
                true);
        }
    }

    @Test
    public void testSendersUnknown() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI,
                new ExpectingHttpItem(
                    new JsonChecker(
                        Files.readString(
                            resource("senders-request-unknown.json"))),
                    Files.readString(
                        resource("senders-response-unknown.json"))));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=1130000009694740",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource(
                                "office@indigo-chel.ru-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=hmNbJEZtcn-e89qBDBv&CONNECT"
                + "=unknown+%5B92.53.81.210%5D+FRNR+QID%3D&HELO=arti-m.ru"
                + "&MAILFROM=info@arti-m.ru+SIZE%3D1077272"
                + "&RCPTTO=office@indigo-chel.ru+ID%3D1130000023046533+"
                + "UID%3D1130000009694740+COUNTRY%3Dru",
                loadResourceAsString("unknown.eml"),
                loadResourceAsString("unknown-so-response.json"),
                true);
        }
    }

    @Test
    public void testNoReceived() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI,
                new ExpectingHttpItem(
                    new JsonChecker(
                        Files.readString(
                            resource("senders-request-no-received.json"))),
                    Files.readString(
                        resource("senders-response-no-received.json"))));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=909923683",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("ylya-boss-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=%3C%3E"
                + "&CONNECT=mta-82.gamma.getresponse-mail.com+"
                + "%5B104.160.65.82%5D+QID%3D"
                + "&HELO=mta-82.gamma.getresponse-mail.com"
                + "&MAILFROM=bounce-32862706@bounce.getresponse-mail.com+"
                + "SIZE%3D22269"
                + "&RCPTTO=Ylya-boss@yandex.ru+ID%3D1322066345+UID%3D909923683"
                + "+COUNTRY%3Dru+IS_MAILLIST%3D0",
                "From: analizer@yandex.ru\r\n\r\n",
                loadResourceAsString("no-received-so-response.json"),
                true);
        }
    }

    @Test
    public void testSendersLocalhostReceived() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.senders().add(
                SENDERS_URI,
                new ExpectingHttpItem(
                    new JsonChecker(
                        loadResourceAsString(
                            "senders-request-localhost-received.json")),
                    loadResourceAsString(
                        "senders-response-localhost-received.json")));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=44627449",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "twirl-team-blackbox-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=5SvVDWQYSd-vLbC45lA"
                + "&CONNECT=del5.i.mail.ru+%5B217.69.138.7%5D+QID%3D"
                + "&HELO=DEL5.I.MAIL.RU"
                + "&MAILFROM=donotreply@mailfier.com+SIZE%3D16391"
                + "&RCPTTO=twirl-team@yandex.ru+ID%3D119060630+UID%3D44627449"
                + "+COUNTRY%3Dru",
                loadResourceAsString("localhost-received.eml"),
                loadResourceAsString("localhost-received-so-response.json"),
                true);
        }
    }

    @Test
    public void testNestedMail() throws Exception {
        try (So2Cluster cluster = new So2Cluster(
                this,
                "mail/so/daemons/so2/so2_config/files/tikaite-extract.conf"))
        {
            cluster.start();

            cluster.check(
                "/?extractor-name=nested-mail",
                loadResourceAsString("rfc822.eml"),
                loadResourceAsString("rfc822-response.json"));
        }
    }

    @Test
    public void testOnlyFastText() throws Exception {
        try (So2Cluster cluster = new So2Cluster(
                this,
                "mail/so/daemons/so2/so2_config/files/tikaite-extract.conf"))
        {
            cluster.start();

            cluster.check(
                "/?extractor-name=only-fast-text",
                loadResourceAsString("test.eml"),
                loadResourceAsString("test-fast-text-embedding.json"));
        }
    }

    private void testJsonSubset(final String fileName, final String expected)
        throws Exception
    {
        testJsonSubset(fileName, "/?only-so2", expected);
    }

    private void testJsonSubset(
        final String fileName,
        final String uri,
        final String expected)
        throws Exception
    {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.start();
            HttpPost post = new HttpPost(cluster.so2().host() + uri);
            post.setEntity(
                new StringEntity(
                    Files.readString(resource(fileName)),
                    ContentType.TEXT_PLAIN.withCharset(
                        StandardCharsets.UTF_8)));
            try (CloseableHttpClient client = Configs.createDefaultClient();
                    CloseableHttpResponse response = client.execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String actual = CharsetUtils.toString(response.getEntity());
                try {
                    YandexAssert.check(
                        new JsonSubsetChecker(expected),
                        actual);
                } catch (Error e) {
                    e.addSuppressed(new Exception("Caused by " + actual));
                    throw e;
                }
            }
        }
    }

    @Test
    public void testUgcGoogleDrive() throws Exception {
        testJsonSubset(
            "google-drive.eml",
            "{\"docs\":[{},{"
            + "\"ugc_id\":\"google-drive\","
            + "\"deobfuscated_ugc\":\""
            + "вашем счету неподтвержден возврат компенсации расходов "
            + "%Number% docx\"}]}");
    }

    @Test
    public void testUgcVkMessageSimple() throws Exception {
        testJsonSubset(
            "vk-message-simple.eml",
            "{\"docs\":[{},{"
            + "\"ugc_id\":\"vk-message\","
            + "\"deobfuscated_ugc\":\""
            + "%FirstName% %LastName% привет %FirstName% %FirstName% "
            + "забронировали коттедж %LastName% %Date% поэтому "
            + "приезжайте %FirstName% бронь %Time% сутки договору"
            + " менеджер просил часам %ShortNumber% приехали\"}]}");
    }

    @Test
    public void testUgcVkMessageWithImage() throws Exception {
        testJsonSubset(
            "vk-message-image.eml",
            "{\"docs\":[{},{"
            + "\"ugc_id\":\"vk-message\","
            + "\"deobfuscated_ugc\":\""
            + "%FirstName% %MayBeName% %FirstName% %FirstName% "
            + "поздравляем %FirstName% днем бракосочетания желаем "
            + "огромной любви взаимопонимания поддержки радости "
            + "общих интересов ценностей также здоровья благополучия "
            + "удачи счастливы\"}]}");
    }

    @Test
    public void testUgcFbGroupInvite() throws Exception {
        testJsonSubset(
            "fb-group-invite.eml",
            "{\"docs\":[{},{"
            + "\"ugc_id\":\"fb-group-invite\","
            + "\"deobfuscated_ugc\":\""
            + "%FirstName% %LastName% super лото доброго времени суток "
            + "повторное письмо связи закончили получение "
            + "выигрыша банковскую карту денежные средства "
            + "аннулированы ближайшее время приветсвует компания супер "
            + "loto честь юбилея дарим %Number% билетов абсолютно "
            + "бесплатно %MayBeName% государственная лотерея "
            + "проводится более %ShortNumber% лет время победителями "
            + "лотереи стали %Number% человек призовой фонд составил "
            + "более %Number% рублей получите сейчас билет "
            + "выиграйте 4 миллионов рублей href %Uri% %Uri%\","
            + "\"ugc_urls\":\"https://t.co/mpjpoRwPlw\"}]}");
    }

    // Because of fall from multipart to text/html, boundary is decoded from
    // base64
    @Test
    public void testDoubleContentType() throws Exception {
        testJsonSubset(
            "mail/library/tikaite/test/resources/ru/yandex/tikaite/server"
            + "/preamble.eml",
            "{\"docs\":[{\"hid\":\"1\",\"pure_body\":\""
            + "Второй раз не могу Вам передать, у Вас транзакция не "
            + "обработанная, больше чем на сто штук.\nУспейте вывести\n"
            + "Смотрите, потому что пропадут\n;cz\"}]}");
    }

    @Test
    public void testHbfProjectId() throws Exception {
        testJsonSubset("forms-spam.eml", "{\"hbf_project_id\":\"10dba78\"}");
    }

    @Test
    public void testHbfProjectIdFakeReceived() throws Exception {
        testJsonSubset(
            "forms-spam-fake-received.eml",
            "{\"hbf_project_id\":\"10dba78\"}");
    }

    @Test
    public void testMailNetsHbfProjectId() throws Exception {
        testJsonSubset("mailnets.eml", "{\"hbf_project_id\":\"640\"}");
    }

    @Test
    public void testAllIpsHbfProjectId() throws Exception {
        testJsonSubset("outback-hbf-id.eml", "{\"hbf_project_id\":\"640\"}");
        testJsonSubset("outback-hbf-id2.eml", "{\"hbf_project_id\":\"640\"}");
    }

    @Test
    public void testMalformedRcptTo() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=5598601",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("analizer-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=%3C%3E"
                + "&CONNECT=mta-82.gamma.getresponse-mail.com+"
                + "%5B104.160.65.82%5D+QID%3D"
                + "&HELO=mta-82.gamma.getresponse-mail.com"
                + "&MAILFROM=analizer@yandex.ru+ID%3D12054080+UID%3D5598601+"
                + "SIZE%3D865+COUNTRY%3Dru+KARMA%3D0+KARMA_STATUS%3D0"
                + "&RCPTTO=a%3Db@f.mail.timepad.ru+IS_MAILLIST%3D0",
                "From: analizer@yandex.ru\r\n\r\n",
                loadResourceAsString("malformed-rcptto-so-response.json"));
        }
    }

    @Test
    public void testMalformedMailFrom() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=1130000046880478",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("forum@gsmkolik.com-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=elierushtonchk@yahoo.com",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource(
                                "elierushtonchk@yahoo.com-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            HttpPost post =
                new HttpPost(
                    cluster.cretur().httpHost()
                    + "/set-organization-ip-whitelist?org_id=12345");
            post.setEntity(
                new StringEntity(
                    "{\"ip_whitelist\":[\"104.160.64.0/23\"]}",
                    StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            cluster.so2().updateExternalData();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=%3C%3E"
                + "&CONNECT=mta-82.gamma.getresponse-mail.com+"
                + "%5B104.160.65.82%5D+QID%3D"
                + "&HELO=mta-82.gamma.getresponse-mail.com"
                + "&MAILFROM=forum%2Bae5feed0%2Belierushtonchk%3Dyahoo.com@"
                + "gsmkolik.com+ID%3D1130000064082979+UID%3D1130000046880478+"
                + "SIZE%3D1343+COUNTRY%3Dus+KARMA%3D0+KARMA_STATUS%3D0"
                + "&RCPTTO=elierushtonchk@yahoo.com+IS_MAILLIST%3D0",
                "From: forum@gsmkolik.com\r\nContent-Type: text/html\r\n\r\n"
                + "<html><body><div>text</div></body></html>",
                loadResourceAsString("malformed-mailfrom-so-response.json"));
            Assert.assertEquals(
                0,
                cluster.templateMaster().accessCount());
        }
    }

    @Test
    public void testMissingMailFromEmail() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=1130000046880478",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("forum@gsmkolik.com-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=elierushtonchk@yahoo.com",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource(
                                "elierushtonchk@yahoo.com-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=%3C%3E"
                + "&CONNECT=mta-82.gamma.getresponse-mail.com+"
                + "%5B104.160.65.82%5D+QID%3D"
                + "&HELO=mta-82.gamma.getresponse-mail.com"
                + "&MAILFROM=ID%3D1130000064082979+UID%3D1130000046880478+"
                + "SIZE%3D1343+COUNTRY%3Dus+KARMA%3D0+KARMA_STATUS%3D0"
                + "&RCPTTO=elierushtonchk@yahoo.com+IS_MAILLIST%3D0",
                "From: forum@gsmkolik.com\r\nContent-Type: text/html\r\n\r\n"
                + "<html><body><div>text</div></body></html>",
                loadResourceAsString("missing-mailfrom-so-response.json"));
            Assert.assertEquals(
                0,
                cluster.templateMaster().accessCount());
        }
    }

    @Test
    public void testSpamSamplesAddRemove() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();

            String body =
                "Subject: =?utf-8?b?0J/RgNC40LLQtdGC0YHRgtCy0YPQtdC8INCy0LDRgQ"
                + "==?=\r\n\r\n"
                + "Купи слона\r\n"
                + "> цитата 2002-03-02";

            String originalDistances =
                "{\"docs\":[{"
                + "\"pure_body_wmd_distance\":0.9186516404151917,"
                + "\"pure_body_wmd_neighbour_id\":\""
                + "spam_samples_991949281_so_compains_171981210770334057\","
                + "\"pure_body_wmd_neighbour_word_count\":17,"
                + "\"pure_body_wmd_neighbour_labels\":null,"
                + "\"hdr_subject\": \"Приветствуем вас\","
                + "\"deobfuscated_subject\": \"приветствуем\","
                + "\"subject_word_count\": 1,"
                + "\"subject_language\": \"ru\"}]}";
            cluster.check(
                "/",
                body,
                originalDistances,
                true);

            // Add some spam samples
            // Dssm spam should match first sample better, because of subject
            // match
            // Second spam sample should match wmd, because of exact pure body
            // match
            HttpPost post =
                new HttpPost(cluster.so2().host() + "/add-spam-samples");
            post.setEntity(
                new StringEntity(
                    Files.readString(resource("new-spam-samples.json")),
                    StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            Thread.sleep(1000L);

            String newDistances =
                "{\"docs\":[{"
                + "\"pure_body_wmd_distance\":0.06483122706413269,"
                + "\"pure_body_wmd_neighbour_id\":\"dssm\","
                + "\"pure_body_wmd_neighbour_labels\":"
                + "\"\\nsome labels\\nhid_1.1\\n\"}]}";
            cluster.check(
                "/",
                body,
                newDistances,
                true);

            // Remove added spam samples
            post = new HttpPost(cluster.so2().host() + "/add-spam-samples");
            post.setEntity(
                new StringEntity(
                    "{\"url\":\"dssm\",\"revision\":\"266599838\","
                    + "\"type\":\"so_compains\"}\n"
                    + "{\"url\":\"wmd\",\"revision\":\"266599839\","
                    + "\"type\":\"so_compains\"}",
                    StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            Thread.sleep(1000L);

            cluster.check(
                "/",
                body,
                originalDistances,
                true);
        }
    }

    @Test
    public void testCleanLanguageDetect() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.start();

            cluster.check(
                "/",
                "\r\nմեծ П0К0КККААСВВА 15.06.1986T10:00:00 1000000\n",
                "{\"docs\":[{"
                + "\"deobfuscated_pure_body\":"
                + "\"մեծ %Password% %Timestamp% %Number%\","
                + "\"pure_body_language\":\"hy\"}]}",
                true);
        }
    }

    @Test
    public void testCalendar() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=forum@gsmkolik.com",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("forum@gsmkolik.com-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=info@sutochno.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource(
                                "info@sutochno.ru-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=dpotapov@yandex-team.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("dpotapov-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.CORP_BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=forum@gsmkolik.com",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("forum@gsmkolik.com-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=&add_headers=0&"
                + "client_ip=2a02%3A6b8%3Ac0c%3A5a16%3A0%3A640%3Aa390%3A93ab"
                + "&from=forum@gsmkolik.com"
                + "&request_id=5d34fc46bfc5a5b.qloud-c.yandex.net"
                + "&source=calendar&to=info%40sutochno.ru"
                + "%3Bdpotapov%40yandex-team.ru"
                + "&to=forum@gsmkolik.com",
                "From: forum@gsmkolik.com\r\nContent-Type: text/html\r\n\r\n"
                + "<html><body ><div >text</div></body></html>",
                loadResourceAsString("calendar-so-response.json"));
            Assert.assertEquals(
                1,
                cluster.templateMaster().accessCount());
        }
    }

    @Test
    public void testCalendarMailish() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=754987777",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("potapov.d-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&login=analizer@yandex.ru",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        Files.readString(
                            resource("analizer-blackbox-userinfo.json"))),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/mail?sanitize-html&fast-mode&qid=&add_headers=0&"
                + "client_ip=192.168.0.1"
                + "&from=potapov.d%40gmail.com"
                + "&request_id=5d34fc46bfc5a5b.qloud-c.yandex.net"
                + "&source=calendar&to=analizer%40yandex.ru&uid=754987777",
                "From: potapov.d@gmail.com\r\n\r\n",
                loadResourceAsString("calendar-mailish-so-response.json"));
        }
    }

    @Test
    public void testCommonOrgId() throws Exception {
        try (So2Cluster cluster = new So2Cluster(this)) {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&"
                + "dbfields=subscription.suid.2,userinfo.reg_date.uid,userinfo"
                + ".country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031"
                + "&sid=smtp&uid=1130000053087110",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString(
                            "pupkin-userinfo.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2Cluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            cluster.check(
                "/antispam?session_id=aIZo3O77Ey-Os7i02gq"
                + "&format=protobuf-json",
                loadResourceAsString("protobuf-common-org-id-request.json"),
                "{\"common_org_id\":3837892}",
                true);
        }
    }
}

