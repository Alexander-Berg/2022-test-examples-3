package ru.yandex.antifraud;

import java.nio.file.Files;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class EncryptedAntifraudTest extends TestBase {
    @Test
    public void testFamilyInfo() throws Exception {
        try (Cluster cluster = new Cluster(this,
                Paths.getSourcePath("mail/so/daemons/antifraud/rules/channels.conf"));
             CloseableHttpClient client = Configs.createDefaultClient()) {


            cluster.blackbox().add(
                    "/blackbox/" +
                    "?phone_attributes=3,102,104,106,108,109" +
                    "&method=userinfo" +
                    "&getphones=bound" +
                    "&get_family_info=true" +
                    "&regname=true" +
                    "&aliases=1,2,3,5,6,7,8,9,10,11,12,13,15,16,17,18,19,20,21,22" +
                    "&get_public_name=true" +
                    "&uid=448130096" +
                    "&attributes=1,31,34,36,107,110,132,200,1003,1015" +
                    "&userip=127.0.0.1" +
                    "&format=json" +
                    "&dbfields=userinfo.sex.uid" +
                    "&sid=2",
                    new ExpectingHeaderHttpItem(
                            new StaticHttpItem(
                                    Files.readString(
                                            resource("family-info-bb-response.json"))),
                            YandexHeaders.X_YA_SERVICE_TICKET,
                            Cluster.BLACKBOX_TVM_TICKET));

            cluster.familypayCluster().prepareDatabase();
            cluster.familypayCluster().addFamily(
                    "f198778",
                    4081110292L,
                    448130096L);

            cluster.start();

            {
                HttpPost post =
                        new HttpPost(
                                cluster.familypayCluster().familypayBackend().httpHost()
                                        + "/family/f198778/start/single_limit?uid=4081110292");
                post.setEntity(
                        new StringEntity(
                                Files.readString(resource("family1-properties.json")),
                                ContentType.APPLICATION_JSON));
                post.addHeader(
                        YandexHeaders.X_YA_USER_TICKET,
                        "3:user:CA0Q__________9_GhYKBgiUmoOaDxCUmoOaDyDShdjMBCgB:O"
                                + "O9OgPpIsOkNsRcp1M3-vXS6MXm9ZOOhFd1I2kANPwW_XyeZZT0QrPWn"
                                + "z8wqpPueo7-8-pe39uNyLoN8ZUE9jVsdOIwja606M7kAI0SSsdsha7U"
                                + "eD2IzRdH3kzEWDKD1sDCmzTgUpBlD42c_ASGXb1lSBO-MCJA-afw-qm"
                                + "L4jW8");
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                }
            }
            {
                HttpPost post = new HttpPost(cluster.server().host() + "/execute?app=family-info");
                post.setEntity(new StringEntity(Files.readString(resource("family-info-request.json")),
                        ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new JsonChecker(Files.readString(resource("family-info-response.json"))),
                            CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }
    @Test
    public void testLoginIdCards() throws Exception {
        try (Cluster cluster = new Cluster(this,
                Paths.getSourcePath("mail/so/daemons/antifraud/rules/channels.conf"));
             CloseableHttpClient client = Configs.createDefaultClient()) {

            cluster.blackbox().add(
                    "/blackbox/" +
                            "?phone_attributes=3,102,104,106,108,109" +
                            "&method=userinfo" +
                            "&getphones=bound" +
                            "&get_family_info=true" +
                            "&regname=true" +
                            "&aliases=1,2,3,5,6,7,8,9,10,11,12,13,15,16,17,18,19,20,21,22" +
                            "&get_public_name=true" +
                            "&uid=448130096" +
                            "&attributes=1,31,34,36,107,110,132,200,1003,1015" +
                            "&userip=127.0.0.1" +
                            "&format=json" +
                            "&dbfields=userinfo.sex.uid" +
                            "&sid=2",
                    Files.readString(resource("login-id-cards-bb-response.json")));

            cluster.getStorageAggregation().add(
                    "/sequential/search" +
                            "?json-type=dollar" +
                            "&service=so_fraud_login_id" +
                            "&text=(uid:%22448130096%22+AND+txn_status:%22OK%22)+AND+type:VERIFICATION_LEVEL" +
                            "&fraud-request-type=VERIFICATION_LEVEL" +
                            "&IO_PRIO=0" +
                            "&prefix=0" +
                            "&length=1000" +
                            "&early-interrupt=1" +
                            "&get=*",
                    Files.readString(resource("login-id-cards-verfification-levels-response.json")));

            cluster.start();

            {
                HttpPost post = new HttpPost(cluster.server().host() + "/execute?app=login-id-cards");
                post.setEntity(new StringEntity(Files.readString(resource("login-id-cards-untrusted-request.json")),
                        ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new JsonChecker(Files.readString(resource("login-id-cards-untrusted-response.json"))),
                            CharsetUtils.toString(response.getEntity()));
                }
            }
            {
                HttpPost post = new HttpPost(cluster.server().host() + "/execute?app=login-id-cards");
                post.setEntity(new StringEntity(Files.readString(resource("login-id-cards-trusted-request.json")),
                        ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new JsonChecker(Files.readString(resource("login-id-cards-trusted-response.json"))),
                            CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }

    @Test
    public void testLoginIdCardsRegressFilter() throws Exception {
        try (Cluster cluster = new Cluster(this,
                Paths.getSourcePath("mail/so/daemons/antifraud/rules/channels.conf"));
             CloseableHttpClient client = Configs.createDefaultClient()) {

            cluster.blackbox().add(
                    "/blackbox/" +
                            "?phone_attributes=3,102,104,106,108,109" +
                            "&method=userinfo" +
                            "&getphones=bound" +
                            "&get_family_info=true" +
                            "&regname=true" +
                            "&aliases=1,2,3,5,6,7,8,9,10,11,12,13,15,16,17,18,19,20,21,22" +
                            "&get_public_name=true" +
                            "&uid=1552517705" +
                            "&attributes=1,31,34,36,107,110,132,200,1003,1015" +
                            "&userip=127.0.0.1" +
                            "&format=json" +
                            "&dbfields=userinfo.sex.uid" +
                            "&sid=2",
                    Files.readString(resource("login-id-cards-regress-filter-bb-response.json")));

            cluster.getStorageAggregation().add(
                    "/sequential/search" +
                            "?json-type=dollar" +
                            "&service=so_fraud_login_id" +
                            "&text=(uid:%221552517705%22+AND+txn_status:%22OK%22)+AND+type:VERIFICATION_LEVEL" +
                            "&fraud-request-type=VERIFICATION_LEVEL" +
                            "&IO_PRIO=0" +
                            "&prefix=0" +
                            "&length=1000" +
                            "&early-interrupt=1" +
                            "&get=*",
                    Files.readString(resource("login-id-cards-regress-filter-verfification-levels-response.json")));

            cluster.start();

            {
                HttpPost post = new HttpPost(cluster.server().host() + "/execute?app=login-id-cards");
                post.setEntity(new StringEntity(Files.readString(resource("login-id-cards-regress-filter-request.json")),
                        ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new JsonChecker(Files.readString(resource("login-id-cards-regress-filter-response.json"))),
                            CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }
}

