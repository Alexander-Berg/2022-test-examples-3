package ru.yandex.passport.familypay.backend;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.client.pg.PgClientCluster;
import ru.yandex.client.pg.SqlQuery;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.NotImplementedHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class FamilypayBackendTest extends TestBase {
    private static final long SLEEP_INTERVAL = 3000L;

    private static final String FAMILY1_MEMBER_REMOVED =
        "tskv\tattribute=members.100502.uid\tconsumer=kopusha\t"
        + "entity=members\tentity_id=100502\tevent=family_info_modification\t"
        + "family_id=f9000\tip=127.194.163.69\tnew=-\told=100502\t"
        + "operation=deleted\ttskv_format=passport-family-log\t"
        + "unixtime=1632471511\tuser_agent=Mozilla/5.0\n";
    private static final String FAMILY1_MEMBER_ADDED =
        "tskv\tattribute=members.100504.uid\tconsumer=passport\t"
        + "entity=members\tentity_id=100504\tevent=family_info_modification\t"
        + "family_id=f9000\tip=2a02:6b8:b081:8011::1:25\tnew=100504\told=-\t"
        + "operation=created\ttskv_format=passport-family-log\t"
        + "unixtime=1631123111\tuser_agent=Mozilla/5.0 (Windows NT "
        + "10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
        + "Chrome/93.0.4577.63 Safari/537.36\n";
    private static final String FAMILY2_MEMBER_REMOVED =
        "tskv\tattribute=members.100504.uid\tconsumer=kopusha\t"
        + "entity=members\tentity_id=100504\tevent=family_info_modification\t"
        + "family_id=f9001\tip=127.194.163.69\tnew=-\told=100504\t"
        + "operation=deleted\ttskv_format=passport-family-log\t"
        + "unixtime=1632471511\tuser_agent=Mozilla/5.0\n";
    private static final String FAMILY2_MEMBER_ADDED =
        "tskv\tattribute=members.100502.uid\tconsumer=passport\t"
        + "entity=members\tentity_id=100502\tevent=family_info_modification\t"
        + "family_id=f9001\tip=2a02:6b8:b081:8011::1:25\tnew=100502\told=-\t"
        + "operation=created\ttskv_format=passport-family-log\t"
        + "unixtime=1631123111\tuser_agent=Mozilla/5.0 (Windows NT "
        + "10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
        + "Chrome/93.0.4577.63 Safari/537.36\n";

    @Test
    public void test() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9000",
                100500,
                100502,
                100503);

            cluster.addFamily(
                "f9001",
                100501,
                100504,
                100505);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            logger.info("Starting family 9000");
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            HttpGet get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/single_limit");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-single.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check /family/{id}/start/single_limit
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/start/single_limit?uid=100501");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family2-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            logger.info("Starting family 9001");
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family2-single.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/9001/single_limit");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family2-single.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check idempotency
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            logger.info("Check family 9000 idempotency");
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test delete
            SqlQuery selectUsers =
                new SqlQuery(
                    "select-users",
                    "SELECT uid FROM familypay.family_member ORDER BY uid");
            String allUids =
                "[[{\"uid\":100500},{\"uid\":100501},{\"uid\":100502},"
                + "{\"uid\":100503},{\"uid\":100504},{\"uid\":100505}]]";
            YandexAssert.check(
                new JsonChecker(allUids),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        selectUsers)
                        .get()));
            /* TODO
             * YandexAssert.check(
                new JsonChecker(allUids),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        "SELECT uid FROM familypay.expenses ORDER BY uid")
                        .get()));*/

            // Admin uid mismatch
            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/stop");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/stop");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/stop");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }

            String halfUids =
                "[[{\"uid\":100500},{\"uid\":100502},{\"uid\":100503}]]";
            YandexAssert.check(
                new JsonChecker(halfUids),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        selectUsers)
                        .get()));
            /* TODO
             * YandexAssert.check(
                new JsonChecker(halfUids),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        "SELECT uid FROM familypay.expenses ORDER BY uid")
                        .get()));*/

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/stop");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            YandexAssert.check(
                new JsonChecker("[[]]"),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        selectUsers)
                        .get()));
            /* TODO
             * YandexAssert.check(
                new JsonChecker("[[]]"),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        "SELECT uid FROM familypay.expenses ORDER BY uid")
                        .get()));*/
        }
    }

    @Test
    public void testFamilyCreateConflicts() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9000",
                100500,
                100502,
                100503);

            // Not an admin uid
            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100501");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_FORBIDDEN, response);
            }

            // cgi-parameter and ticket mismatch
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_FORBIDDEN, response);
            }

            logger.info("Starting family 9000");
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            logger.info("Check family 9000 idempotency");
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            logger.info("Manually drop family members");
            SqlQuery dropMembers =
                new SqlQuery(
                    "drop-members",
                    "DELETE FROM familypay.family_member");
            cluster.pgClientCluster().client().executeOnMaster(dropMembers)
                .get();

            logger.info("Check that family considered as non existent");
            HttpGet get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }

            logger.info("Create family once again");
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            logger.info("Manually drop family members once again");
            cluster.pgClientCluster().client().executeOnMaster(dropMembers)
                .get();

            logger.info("Change family settings to induce conflict");
            cluster.pgClientCluster().client().executeOnMaster(
                new SqlQuery(
                    "set-card-mask",
                    "UPDATE familypay.family SET card_mask = '9999****9999'"))
                .get();

            logger.info("Try create family, expect conflict");
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            }

            logger.info("And try again to check that settings not updated");
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            }

            logger.info("Check that no users were added to family");
            YandexAssert.check(
                new JsonChecker("[[]]"),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        new SqlQuery(
                            "select-members",
                            "SELECT * FROM familypay.family_member"))
                        .get()));
        }
    }

    @Test
    public void testUpdate() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9000",
                100500,
                100502,
                100503);

            cluster.addFamily(
                "f9001",
                100501,
                100504,
                100505);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/start/single_limit?uid=100501");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family3-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family3-single.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check idempotency
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/start/single_limit?uid=100501");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family3-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family3-single.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            Thread.sleep(SLEEP_INTERVAL);
            Assert.assertEquals(6, cluster.antifraud().accessCount());
            // Admin uid mismatch, no update will take place
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties-update.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100509);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }
            Thread.sleep(SLEEP_INTERVAL);
            // Only core family settings was changed, so no pushes will be sent
            Assert.assertEquals(6, cluster.antifraud().accessCount());

            // Admin uid mismatch, no update will take place
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/users/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-users-update.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100509);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"updated\":[],\"skipped\":[100500,100501,100503]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            HttpGet get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test update with single limit
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties-update.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/users/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-users-update.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"updated\":[100500,100503],\"skipped\":[100501]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-updated.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test update with multi limit
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9001/update");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family3-properties-update.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/update/users");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family3-users-update.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"updated\":[100501,100504],\"skipped\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family3-updated.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/single_limit");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "family3-updated-single-limit.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            Thread.sleep(SLEEP_INTERVAL);
            Assert.assertEquals(10, cluster.antifraud().accessCount());
            // Test update with multi limit
            logger.info("Unbinding card and enabling all services");
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9001/update");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString(
                        "family3-properties-unbound-card.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }
            Thread.sleep(SLEEP_INTERVAL);
            // All family members just lost their privileges
            // For each member there will be two pushes;
            // One for member and one for admin
            // But familypay was already disabled for 100504, so only 2 pushes
            Assert.assertEquals(12, cluster.antifraud().accessCount());

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/single_limit");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "family3-updated-single-limit-card-unbound.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/bind_card?cardId=x1235");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/bind_card?cardId=x1236");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/single_limit");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "family3-updated-single-limit-all-services.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPayments() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9000",
                100500,
                100502,
                100503);

            cluster.addFamily(
                "f9001",
                100501,
                100504,
                100505);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Taxi is disabled for user
            HttpGet get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/info?service_id=124");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }

            // Eats enabled for user
            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/info?service_id=629");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user1-zero-expenses.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc1&uid=100502"
                            + "&serviceId=629&cardId=card-x1234&amount=500"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user1-payment1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // limit currency mismatch is in blacklist
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc7&uid=100502"
                            + "&serviceId=629&cardId=card-x1234&amount=550"
                            + "&currency=USD")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            }

            // Service 'taxi' is in blacklist
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc7&uid=100502"
                            + "&serviceId=124&cardId=card-x1234&amount=550"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            }

            // Revert rejected payment
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/complete?paymentId=abc7"
                            + "&status=rejected")))
            {
                // Payment wasn't inserted, so nothing will be found
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_FOUND,
                    response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc3&uid=100503"
                            + "&serviceId=629&cardId=card-x1234&amount=300"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user2-payment1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc2&uid=100502"
                            + "&serviceId=629&cardId=card-x1234&amount=150"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user1-payment2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc8&uid=100502"
                            + "&serviceId=629&cardId=card-x1234&amount=100"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user1-payment3.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-expenses1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check idempotency
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc8&uid=100502"
                            + "&serviceId=629&cardId=card-x1234&amount=100"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user1-payment3.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-expenses1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test that invalid status doesn't change payments table
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/complete?paymentId=abc1"
                            + "&status=badstatus")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-expenses1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test that payments can be properly rejected
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/complete?paymentId=abc1"
                            + "&status=rejected")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            // ... and completed
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/complete?paymentId=abc2"
                            + "&status=completed")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            // ... and this operation is idempotent
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/complete?paymentId=abc2"
                            + "&status=completed")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            // ... and there is not way back
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/complete?paymentId=abc2"
                            + "&status=rejected")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_FOUND,
                    response);
            }

            // ... and no way to restart it
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc2&uid=100502"
                            + "&serviceId=629&cardId=card-x1234&amount=150"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-expenses2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Taxi is blocked for user
            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/info?service_id=124");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/info?service_id=629");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("user1-info.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/settings");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user1-settings.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/info/single_limit?service_id=629");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user1-info-single-limit.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Admin uid mismatch, expenses won't be reset
            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/reset_expenses");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_FOUND,
                    response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/info?service_id=629");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("user1-info.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Drop user expenses for real
            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/reset_expenses");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-expenses3.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost()
                    + "/user/100502/info?service_id=629");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user1-zero-expenses.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc9&uid=100502"
                            + "&serviceId=629&cardId=card-x1234&amount=500"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user1-payment1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/users/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-users-update-currency.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"updated\":[100502,100503],\"skipped\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-expenses3-eur.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/users/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-users-update-disable.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"updated\":[100503],\"skipped\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "family1-expenses3-disabled.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPassportConsumer() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9000",
                100500,
                100502,
                100503);

            cluster.addFamily(
                "f9001",
                100501,
                100504,
                100505);

            cluster.addFamily(
                "f9002",
                100509,
                100513);

            cluster.addFamily(
                "f9003",
                100510,
                100511);

            cluster.addFamily(
                "f9004",
                100531,
                100532);

            cluster.addFamily(
                "f9007",
                100701,
                100702,
                100703,
                100704);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=family_info&family_id=f9005",
                new StaticHttpResource(
                    new StaticHttpItem(
                        "{\"status\":{\"value\":\"MISSING_FAMILY\","
                        + "\"description\":\"Family was not found: 'f9005'\"}"
                        + ",\"family\":{\"f9005\":{}}}")));

            String push100500 = "/execute?app=family-push&uid=100500&*";
            cluster.antifraud().add(
                push100500,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"recipient_uid\":\"100500\",\"push_request\":\""
                        + "/am/push/family\",\"event_name\":\"family_change\","
                        + "\"push_id\":\"<any value>\","
                        + "\"request_params\":{},\"template_id\":\""
                        + "add-family-card-for-admin\",\"template_parameters\""
                        + ":{}}"),
                    HttpStatus.SC_OK),
                // TODO:
                new StaticHttpItem(HttpStatus.SC_OK),
                new StaticHttpItem(HttpStatus.SC_OK),
                new StaticHttpItem(HttpStatus.SC_OK),
                new StaticHttpItem(HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);
            String push100502 = "/execute?app=family-push&uid=100502&*";
            cluster.antifraud().add(
                push100502,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"recipient_uid\":\"100502\",\"push_request\":\""
                        + "/am/push/family-limits-member\",\"uid\":\"100500\","
                        + "\"event_name\":\"family_change\","
                        + "\"push_id\":\"<any value>\","
                        + "\"request_params\":{},\"template_id\":"
                        + "\"get-card-access-for-close\",\"template_parameters"
                        + "\":{\"limit\":\"1\u00a0000\u00a0\u20bd\","
                        + "\"range\":\"day\","
                        + "\"action\":\"share\"}}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);

            String push100501 = "/execute?app=family-push&uid=100501&*";
            cluster.antifraud().add(
                push100501,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"recipient_uid\":\"100501\",\"push_request\":\""
                        + "/am/push/family\",\"event_name\":\"family_change\","
                        + "\"push_id\":\"<any value>\","
                        + "\"request_params\":{},\"template_id\":\""
                        + "add-family-card-for-admin\",\"template_parameters\""
                        + ":{}}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);

            String push100509 = "/execute?app=family-push&uid=100509&*";
            cluster.antifraud().add(
                push100509,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"recipient_uid\":\"100509\",\"push_request\":\""
                        + "/am/push/family\",\"event_name\":\"family_change\","
                        + "\"push_id\":\"<any value>\","
                        + "\"request_params\":{},\"template_id\":\""
                        + "add-family-card-for-admin\",\"template_parameters\""
                        + ":{}}"),
                    HttpStatus.SC_OK),
                // TODO:
                new StaticHttpItem(HttpStatus.SC_OK),
                new StaticHttpItem(HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);

            String push100510 = "/execute?app=family-push&uid=100510&*";
            cluster.antifraud().add(
                push100510,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"recipient_uid\":\"100510\",\"push_request\":\""
                        + "/am/push/family\",\"event_name\":\"family_change\","
                        + "\"push_id\":\"<any value>\","
                        + "\"request_params\":{},\"template_id\":\""
                        + "add-family-card-for-admin\",\"template_parameters\""
                        + ":{}}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);

            String push100531 = "/execute?app=family-push&uid=100531&*";
            cluster.antifraud().add(
                push100531,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"recipient_uid\":\"100531\",\"push_request\":\""
                        + "/am/push/family\",\"push_id\":\"some-topic-1-1-0_"
                        + "100531_member-leave-family-for-admin\","
                        + "\"request_params\":{\"member_uid\":\"100533\"},"
                        + "\"event_name\":\"family_change\",\"uid\":\"100533\""
                        + ",\"template_id\":\"member-leave-family-for-admin\","
                        + "\"template_parameters\":{\"action\":\"leave\"}}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);
            String push100533 = "/execute?app=family-push&uid=100533&*";
            cluster.antifraud().add(
                push100533,
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"recipient_uid\":\"100533\",\"push_request\":\""
                        + "/am/push/family\",\"push_id\":\"some-topic-1-1-0_"
                        + "100533_admin-remove-member-for-close\","
                        + "\"request_params\":{},"
                        + "\"event_name\":\"family_change\",\"template_id\":\""
                        + "admin-remove-member-for-close\","
                        + "\"template_parameters\":{}}"),
                    HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties-update.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/start/single_limit?uid=100501");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family2-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family2-single.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9002/start?uid=100509");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family4-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100509);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family4.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9003/start?uid=100510");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family5-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100510);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family5.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9007/start?uid=100701&origin=passport");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family7-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                "3:user:CA0Q__________9_GhIKBAjdkgYQ3ZIGINKF2MwEKAE:CsbQ_OMwD7"
                + "2XBufiwDEwWbRa0aTlnYcMaZ_21BK8NX6Bg7tt9Y1I09hxr2e_fAIzdzTVg"
                + "Uy2DfRLqE4v18PfIax5db16g_w_IbBzYpxhg_ZEUPPnTnYxGXEDMYix3wm7"
                + "5WZkOZ7S-qNgbx0mt83Bid_U-axp8hcHgGvVJ7DvA-s");

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family7.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // No conflict expected here
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9007/start?uid=100701&origin=passport");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family7-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                "3:user:CA0Q__________9_GhIKBAjdkgYQ3ZIGINKF2MwEKAE:CsbQ_OMwD7"
                + "2XBufiwDEwWbRa0aTlnYcMaZ_21BK8NX6Bg7tt9Y1I09hxr2e_fAIzdzTVg"
                + "Uy2DfRLqE4v18PfIax5db16g_w_IbBzYpxhg_ZEUPPnTnYxGXEDMYix3wm7"
                + "5WZkOZ7S-qNgbx0mt83Bid_U-axp8hcHgGvVJ7DvA-s");

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family7.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            logger.info("Manually drop family members");
            SqlQuery dropMembers =
                new SqlQuery(
                    "drop-members",
                    "DELETE FROM familypay.family_member "
                    + "WHERE family_id = 'f9007'");

            cluster.pgClientCluster().client().executeOnMaster(dropMembers)
                .get();
            logger.info("Check that family considered as non existent");

            // No conflict expected here
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9007/start?uid=100701&origin=passport");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family7-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                "3:user:CA0Q__________9_GhIKBAjdkgYQ3ZIGINKF2MwEKAE:CsbQ_OMwD7"
                + "2XBufiwDEwWbRa0aTlnYcMaZ_21BK8NX6Bg7tt9Y1I09hxr2e_fAIzdzTVg"
                + "Uy2DfRLqE4v18PfIax5db16g_w_IbBzYpxhg_ZEUPPnTnYxGXEDMYix3wm7"
                + "5WZkOZ7S-qNgbx0mt83Bid_U-axp8hcHgGvVJ7DvA-s");

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family7.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            logger.info("Manually drop single family member");
            SqlQuery dropMember =
                new SqlQuery(
                    "drop-members",
                    "DELETE FROM familypay.family_member "
                    + "WHERE uid = 100703");

            cluster.pgClientCluster().client().executeOnMaster(dropMember)
                .get();
            logger.info("Check that family will be recovered");

            // No conflict expected here
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9007/start?uid=100701&origin=passport");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family7-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                "3:user:CA0Q__________9_GhIKBAjdkgYQ3ZIGINKF2MwEKAE:CsbQ_OMwD7"
                + "2XBufiwDEwWbRa0aTlnYcMaZ_21BK8NX6Bg7tt9Y1I09hxr2e_fAIzdzTVg"
                + "Uy2DfRLqE4v18PfIax5db16g_w_IbBzYpxhg_ZEUPPnTnYxGXEDMYix3wm7"
                + "5WZkOZ7S-qNgbx0mt83Bid_U-axp8hcHgGvVJ7DvA-s");

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family7.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            Thread.sleep(SLEEP_INTERVAL);

            // family start. family update doesn't require push
            Assert.assertEquals(
                1,
                cluster.antifraud().accessCount(push100500));
            Assert.assertEquals(
                1,
                cluster.antifraud().accessCount(push100502));
            // family start
            Assert.assertEquals(
                1,
                cluster.antifraud().accessCount(push100501));
            Assert.assertEquals(
                1,
                cluster.antifraud().accessCount(push100509));
            // No card bound
            Assert.assertEquals(
                0,
                cluster.antifraud().accessCount(push100510));
            Assert.assertEquals(12, cluster.antifraud().accessCount());

            cluster.addFamily(
                "f9000",
                100500,
                100507,
                100512);

            cluster.addFamily(
                "f9002",
                100509,
                100508);

            cluster.addFamily(
                "f9007",
                100701,
                100702,
                100703,
                100704,
                100705);

            cluster.passportMessageSenderCluster().sendMessages(
                // Unknown family, will affect nothing
                "tskv\tconsumer=kopusha\tentity=admin_uid\t"
                + "event=family_info_modification\tfamily_id=f198830\t"
                + "ip=127.6.223.58\tnew=-\told=4081178310\toperation=deleted\t"
                + "tskv_format=passport-family-log\tunixtime=1631127792\t"
                + "user_agent=Mozilla/5.0\n",
                // Delete member from family without enabled familypay
                "tskv\tattribute=members.100533.uid\tconsumer=kopusha\t"
                + "entity=members\tentity_id=100533\t"
                + "event=family_info_modification\tfamily_id=f9004\t"
                + "ip=127.194.163.69\tnew=-\told=100533\toperation=deleted\t"
                + "tskv_format=passport-family-log\tunixtime=1632471511\t"
                + "user_agent=Mozilla/5.0\n",
                // Delete member from family not known to blackbox
                "tskv\tattribute=members.100534.uid\tconsumer=kopusha\t"
                + "entity=members\tentity_id=100534\t"
                + "event=family_info_modification\tfamily_id=f9005\t"
                + "ip=127.194.163.69\tnew=-\told=100534\toperation=deleted\t"
                + "tskv_format=passport-family-log\tunixtime=1632471511\t"
                + "user_agent=Mozilla/5.0\n",
                // Delete member from family1
                "tskv\tattribute=members.100503.uid\tconsumer=kopusha\t"
                + "entity=members\tentity_id=100503\t"
                + "event=family_info_modification\tfamily_id=f9000\t"
                + "ip=127.194.163.69\tnew=-\told=100503\toperation=deleted\t"
                + "tskv_format=passport-family-log\tunixtime=1632471511\t"
                + "user_agent=Mozilla/5.0\n",
                // Delete another member from family1
                "tskv\tattribute=members.100502.uid\tconsumer=kopusha\t"
                + "entity=members\tentity_id=100502\t"
                + "event=family_info_modification\tfamily_id=f9000\t"
                + "ip=127.194.163.69\tnew=-\told=100502\toperation=deleted\t"
                + "tskv_format=passport-family-log\tunixtime=1632471512\t"
                + "user_agent=Mozilla/5.0\n",
                // Add member to family1
                "tskv\tattribute=uid\tconsumer=kopusha\tentity=kid\t"
                + "entity_id=100507\tevent=family_info_modification\t"
                + "family_id=f9000\tip=127.121.108.18\tnew=100507\told=-\t"
                + "operation=created\ttskv_format=passport-family-log\t"
                + "unixtime=1632471497\t"
                + "user_agent=Apache-HttpClient/4.5.12 (Java/1.8.0_221)\n",
                // Add member to family4
                "tskv\tattribute=members.100508.uid\tconsumer=passport\t"
                + "entity=members\tentity_id=100508\t"
                + "event=family_info_modification\tfamily_id=f9002\t"
                + "ip=2a02:6b8:b081:8011::1:25\tnew=100508\told=-\t"
                + "operation=created\ttskv_format=passport-family-log\t"
                + "unixtime=1631123111\tuser_agent=Mozilla/5.0 (Windows NT "
                + "10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/93.0.4577.63 Safari/537.36\n",
                // Add member to family7
                "tskv\tattribute=members.100705.uid\tconsumer=passport\t"
                + "entity=members\tentity_id=100705\t"
                + "event=family_info_modification\tfamily_id=f9007\t"
                + "ip=2a02:6b8:b081:8011::1:25\tnew=100705\told=-\t"
                + "operation=created\ttskv_format=passport-family-log\t"
                + "unixtime=1631123111\tuser_agent=Mozilla/5.0 (Windows NT "
                + "10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/93.0.4577.63 Safari/537.36\n",
                // Drop family2
                "tskv\tconsumer=kopusha\tentity=admin_uid\t"
                + "event=family_info_modification\tfamily_id=f9001\t"
                + "ip=127.194.163.69\tnew=-\told=100501\toperation=deleted\t"
                + "tskv_format=passport-family-log\tunixtime=1632471511\t"
                + "user_agent=Mozilla/5.0\n");
            Thread.sleep(SLEEP_INTERVAL);

            // Additional member was found in blackbox
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString(
                        "family1-members-changed.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9001")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }

            // One member was dropped because wasn't found in blackbox
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9002")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString(
                        "family4-members-changed.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Not changed
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9003")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family5.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Member added with all services allowed
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9007")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString(
                        "family7-members-changed.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            Thread.sleep(SLEEP_INTERVAL);

            // family start + two deleted + two added
            Assert.assertEquals(
                5,
                cluster.antifraud().accessCount(push100500));
            // family start + one deleted + one added
            Assert.assertEquals(
                3,
                cluster.antifraud().accessCount(push100509));
            Assert.assertEquals(
                1,
                cluster.antifraud().accessCount(push100531));
            Assert.assertEquals(
                1,
                cluster.antifraud().accessCount(push100533));
            Assert.assertEquals(33, cluster.antifraud().accessCount());
        }
    }

    @Test
    public void testPassportConsumerFamilyCreated() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9003",
                100510,
                100511);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9003/start?uid=100510");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family5-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100510);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family5.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            int code =
                cluster.passportMessageSenderCluster().sendMessages(
                    // Family created, event ignored
                    "tskv\tconsumer=kp-user-profile-api\tentity=admin_uid\t"
                    + "event=family_info_modification\tfamily_id=f1285990\t"
                    + "ip=2a02:6b8:c23:3021:0:40cb:bf75:0\tnew=1134353246\t"
                    + "old=-\t"
                    + "operation=created\ttskv_format=passport-family-log\t"
                    + "unixtime=1632762571\t"
                    + "user_agent=ott-api/ott_8659180_0.220494_dev "
                    + "(qloud=ott-api.production.backend)\n")
                    .first();

            Assert.assertEquals(HttpStatus.SC_NO_CONTENT, code);

            // Not changed
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9003")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family5.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPassportConsumerBadMessage() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=family_info&family_id=*",
                new StaticHttpResource(
                    new StaticHttpItem(
                        "{\"status\":{\"value\":\"MISSING_FAMILY\","
                        + "\"description\":\"Family was not found: 'f9005'\"}"
                        + ",\"family\":{\"f9005\":{}}}")));

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/update-family");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("bad-family-update.tskv"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }
        }
    }

    private void testUserFamilyChange(
        final FamilypayCluster.FamilyChange... changes)
        throws Exception
    {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9000",
                100500,
                100502,
                100503);

            cluster.addFamily(
                "f9001",
                100501,
                100504,
                100505);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/start/single_limit?uid=100501");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family2-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family2-single.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            for (int i = 0; i < changes.length; ++i) {
                logger.info("Processing change #" + i);
                FamilypayCluster.FamilyChange change = changes[i];
                for (FamilypayCluster.BlackboxFamilyState family
                    : change.familiesStateAtTheMomentOfChange())
                {
                    cluster.addFamily(family);
                }
                cluster.passportMessageSenderCluster()
                    .sendMessages(change.change());
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString(
                        "family1-member-moved.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9001")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString(
                        "family2-member-moved.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testUserFamilyChange() throws Exception {
        // Consequent family modifications
        testUserFamilyChange(
            new FamilypayCluster.FamilyChange(
                FAMILY1_MEMBER_REMOVED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100503, true))),
            new FamilypayCluster.FamilyChange(
                FAMILY2_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100502, false),
                    new FamilypayCluster.BlackboxUserState(100504, true),
                    new FamilypayCluster.BlackboxUserState(100505, true))),
            new FamilypayCluster.FamilyChange(
                FAMILY2_MEMBER_REMOVED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100502, false),
                    new FamilypayCluster.BlackboxUserState(100505, true))),
            new FamilypayCluster.FamilyChange(
                FAMILY1_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100503, true),
                    new FamilypayCluster.BlackboxUserState(100504, false))));
    }

    @Test
    public void testUserFamilyChangeAllRemovedThenAdded() throws Exception {
        testUserFamilyChange(
            new FamilypayCluster.FamilyChange(
                FAMILY1_MEMBER_REMOVED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100503, true))),
            new FamilypayCluster.FamilyChange(
                FAMILY2_MEMBER_REMOVED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100505, true))),
            new FamilypayCluster.FamilyChange(
                FAMILY2_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100502, false),
                    new FamilypayCluster.BlackboxUserState(100505, true))),
            new FamilypayCluster.FamilyChange(
                FAMILY1_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100503, true),
                    new FamilypayCluster.BlackboxUserState(100504, false))));
    }

    @Test
    public void testUserFamilyChangeAtomicMigrations() throws Exception {
        testUserFamilyChange(
            new FamilypayCluster.FamilyChange(
                FAMILY1_MEMBER_REMOVED
                + FAMILY2_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100503, true)),
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100502, false),
                    new FamilypayCluster.BlackboxUserState(100504, true),
                    new FamilypayCluster.BlackboxUserState(100505, true))),
            new FamilypayCluster.FamilyChange(
                FAMILY2_MEMBER_REMOVED
                + FAMILY1_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100502, false),
                    new FamilypayCluster.BlackboxUserState(100505, true)),
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100503, true),
                    new FamilypayCluster.BlackboxUserState(100504, false))));
    }

    @Test
    public void testUserFamilyChangeAtomicMigrationsReverse()
        throws Exception
    {
        testUserFamilyChange(
            new FamilypayCluster.FamilyChange(
                FAMILY2_MEMBER_REMOVED
                + FAMILY1_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100502, true),
                    new FamilypayCluster.BlackboxUserState(100503, true),
                    new FamilypayCluster.BlackboxUserState(100504, false)),
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100505, true))),
            new FamilypayCluster.FamilyChange(
                FAMILY1_MEMBER_REMOVED
                + FAMILY2_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100502, false),
                    new FamilypayCluster.BlackboxUserState(100505, true)),
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100503, true),
                    new FamilypayCluster.BlackboxUserState(100504, false))));
    }

    @Test
    public void testUserFamilyChangeBatch() throws Exception {
        testUserFamilyChange(
            new FamilypayCluster.FamilyChange(
                FAMILY1_MEMBER_REMOVED
                + FAMILY2_MEMBER_REMOVED
                + FAMILY1_MEMBER_ADDED
                + FAMILY2_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100502, false),
                    new FamilypayCluster.BlackboxUserState(100505, true)),
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100503, true),
                    new FamilypayCluster.BlackboxUserState(100504, false))));
    }

    @Test
    public void testUserFamilyChangeBatchReverse() throws Exception {
        testUserFamilyChange(
            new FamilypayCluster.FamilyChange(
                FAMILY2_MEMBER_REMOVED
                + FAMILY1_MEMBER_REMOVED
                + FAMILY2_MEMBER_ADDED
                + FAMILY1_MEMBER_ADDED,
                new FamilypayCluster.BlackboxFamilyState(
                    "f9001",
                    new FamilypayCluster.BlackboxUserState(100501, true),
                    new FamilypayCluster.BlackboxUserState(100502, false),
                    new FamilypayCluster.BlackboxUserState(100505, true)),
                new FamilypayCluster.BlackboxFamilyState(
                    "f9000",
                    new FamilypayCluster.BlackboxUserState(100500, true),
                    new FamilypayCluster.BlackboxUserState(100503, true),
                    new FamilypayCluster.BlackboxUserState(100504, false))));
    }

    @Test
    public void testCardUnbind() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9000",
                100500,
                100502,
                100503);

            cluster.addFamily(
                "f9001",
                100501,
                100504,
                100505);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9001/start/single_limit?uid=100501");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family2-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100501);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family2-single.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            int code =
                cluster.cardEventsMessageSenderCluster().sendMessages(
                    loadResourceAsString("card-unbind-cardid-mismatch.json"))
                    .first();

            Assert.assertEquals(HttpStatus.SC_NO_CONTENT, code);

            // Not changed
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            code =
                cluster.cardEventsMessageSenderCluster().sendMessages(
                    loadResourceAsString("card-unbind.json"))
                    .first();

            Assert.assertEquals(HttpStatus.SC_NO_CONTENT, code);

            // Card unbound
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-card-unbound.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Not changed
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9001")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            code =
                cluster.cardEventsMessageSenderCluster().sendMessages(
                    loadResourceAsString("card-unbind-short-cardid.json"))
                    .first();

            Assert.assertEquals(HttpStatus.SC_NO_CONTENT, code);

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9001")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family2-card-unbound.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTaxiImport() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9006",
                100601,
                100602,
                100603);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9006/start?uid=100601&origin=taxi");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family6-properties.json"),
                    ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family6.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            logger.info("Manually drop family members");
            SqlQuery dropMembers =
                new SqlQuery(
                    "drop-members",
                    "DELETE FROM familypay.family_member "
                    + "WHERE family_id = 'f9006'");

            cluster.pgClientCluster().client().executeOnMaster(dropMembers)
                .get();
            logger.info("Check that family considered as non existent");

            // No conflict expected here
            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9006/start?uid=100601&origin=taxi");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family6-properties.json"),
                    ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family6.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // All services banned except Taxi
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc1&uid=100602"
                            + "&serviceId=629&cardId=x2234&amount=500"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/complete?paymentId=abc1"
                            + "&status=completed")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/create?paymentId=abc1&uid=100602"
                            + "&serviceId=124&cardId=x2234&amount=500"
                            + "&currency=RUB")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("user3-payment.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/payment/complete?paymentId=abc1"
                            + "&status=completed")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            Thread.sleep(SLEEP_INTERVAL);

            Assert.assertEquals(2, cluster.antifraud().accessCount());
        }
    }

    @Test
    public void testPassportTemplate() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9007",
                100701,
                100702,
                100703,
                100704);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9007/start?uid=100701&origin=passport");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family7-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                "3:user:CA0Q__________9_GhIKBAjdkgYQ3ZIGINKF2MwEKAE:CsbQ_OMwD7"
                + "2XBufiwDEwWbRa0aTlnYcMaZ_21BK8NX6Bg7tt9Y1I09hxr2e_fAIzdzTVg"
                + "Uy2DfRLqE4v18PfIax5db16g_w_IbBzYpxhg_ZEUPPnTnYxGXEDMYix3wm7"
                + "5WZkOZ7S-qNgbx0mt83Bid_U-axp8hcHgGvVJ7DvA-s");

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family7.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            Thread.sleep(SLEEP_INTERVAL);

            Assert.assertEquals(4, cluster.antifraud().accessCount());
        }
    }

    @Test
    public void testMalformedJson() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    "ochen plohie dannye",
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                "3:user:CA0Q__________9_GhIKBAjdkgYQ3ZIGINKF2MwEKAE:CsbQ_OMwD7"
                + "2XBufiwDEwWbRa0aTlnYcMaZ_21BK8NX6Bg7tt9Y1I09hxr2e_fAIzdzTVg"
                + "Uy2DfRLqE4v18PfIax5db16g_w_IbBzYpxhg_ZEUPPnTnYxGXEDMYix3wm7"
                + "5WZkOZ7S-qNgbx0mt83Bid_U-axp8hcHgGvVJ7DvA-s");

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
            }
        }
    }

    @Test
    public void testMigration() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this)) {
            cluster.clearDatabase();
            cluster.applyMigration1();
            cluster.applyMigration2();
            cluster.applyMigration3();
            cluster.applyMigration4();

            SqlQuery addFamilies =
                new SqlQuery(
                    "add-families",
                    "INSERT INTO familypay.family VALUES"
                    + "('f1',1,'x1','1','RUB','MIR',true,'',1,1,1,1,"
                    + "'{}',false),"
                    + "('f2',2,'x2','2','USD','MIR',true,'',2,2,2,2,"
                    + "'{}',false),"
                    + "('f3',3,'x3','3','EUR','MIR',true,'',3,3,3,3,"
                    + "ARRAY['dostavka','drive','eats','games',"
                    + "'kinopoisk','lavka','market','passport',"
                    + "'travel'],false),"
                    + "('f4',4,'x4','4','ILS','MIR',true,'',4,4,4,4,"
                    + "ARRAY['dostavka','drive','games',"
                    + "'kinopoisk','market','passport',"
                    + "'travel'],false),"
                    + "('f5',5,'x5','5','AMD','MIR',true,'',5,5,5,5,"
                    + "ARRAY['dostavka','games','drive','eats',"
                    + "'kinopoisk','lavka','market','passport',"
                    + "'travel'],false),"
                    + "('f6',6,'x6','6','RUB','MIR',true,'',6,6,6,6,"
                    + "ARRAY['dostavka','drive','games',"
                    + "'travel','kinopoisk','market','passport'],false)");
            cluster.pgClientCluster().client().executeOnMaster(addFamilies)
                .get();

            SqlQuery addUsers =
                new SqlQuery(
                    "add-users",
                    "INSERT INTO familypay.family_member VALUES"
                    + "(1,'f1',true,1,1,1,1,'{}',false),"
                    + "(2,'f2',true,1,1,1,1,"
                    + "ARRAY['dostavka','drive','eats','games',"
                    + "'kinopoisk','lavka','market','passport',"
                    + "'travel'],false),"
                    + "(3,'f3',true,1,1,1,1,"
                    + "ARRAY['dostavka','drive',"
                    + "'kinopoisk','market','passport','travel'],false)");
            cluster.pgClientCluster().client().executeOnMaster(addUsers).get();

            SqlQuery selectBlockedServices =
                new SqlQuery(
                    "blocked-services",
                    "SELECT family_id AS id, default_blocked_services AS s "
                    + "FROM familypay.family ORDER BY id");
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"id\":\"f1\",\"s\":[]},"
                    + "{\"id\":\"f2\",\"s\":[]},"
                    + "{\"id\":\"f3\",\"s\":["
                    + "\"dostavka\",\"drive\",\"eats\",\"games\","
                    + "\"kinopoisk\",\"lavka\",\"market\","
                    + "\"passport\",\"travel\"]},"
                    + "{\"id\":\"f4\",\"s\":["
                    + "\"dostavka\",\"drive\",\"games\","
                    + "\"kinopoisk\",\"market\",\"passport\","
                    + "\"travel\"]},"
                    + "{\"id\":\"f5\",\"s\":["
                    + "\"dostavka\",\"games\",\"drive\",\"eats\","
                    + "\"kinopoisk\",\"lavka\",\"market\","
                    + "\"passport\",\"travel\"]},"
                    + "{\"id\":\"f6\",\"s\":["
                    + "\"dostavka\",\"drive\",\"games\","
                    + "\"travel\",\"kinopoisk\",\"market\","
                    + "\"passport\"]}]]"),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        selectBlockedServices)
                        .get()));

            cluster.applyMigration5();
            cluster.applyMigration6();
            cluster.applyMigration7();
            cluster.applyMigration8();

            SqlQuery selectAllowedServices =
                new SqlQuery(
                    "blocked-services",
                    "SELECT family_id AS id, default_allowed_services AS s "
                    + "FROM familypay.family ORDER BY id");
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"id\":\"f1\",\"s\":null},"
                    + "{\"id\":\"f2\",\"s\":null},"
                    + "{\"id\":\"f3\",\"s\":[\"taxi\"]},"
                    + "{\"id\":\"f4\",\"s\":"
                    + "[\"eats\",\"lavka\",\"taxi\"]},"
                    + "{\"id\":\"f5\",\"s\":[\"taxi\"]},"
                    + "{\"id\":\"f6\",\"s\":"
                    + "[\"eats\",\"lavka\",\"taxi\"]}]]"),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        selectAllowedServices)
                        .get()));

            SqlQuery selectAllowedUserServices =
                new SqlQuery(
                    "blocked-user-services",
                    "SELECT uid, allowed_services AS s, limit_currency as c "
                    + "FROM familypay.family_member ORDER BY uid");
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"uid\":1,\"c\":\"RUB\",\"s\":null},"
                    + "{\"uid\":2,\"c\":\"USD\",\"s\":[\"taxi\"]},"
                    + "{\"uid\":3,\"c\":\"EUR\",\"s\":"
                    + "[\"eats\",\"games\",\"lavka\",\"taxi\"]}]]"),
                PgClientCluster.toJsonString(
                    cluster.pgClientCluster().client().executeOnMaster(
                        selectAllowedUserServices)
                        .get()));
        }
    }

    @Test
    public void testSecurePhoneStatusUpdate() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9000",
                100500,
                100502,
                100503);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("family1-properties.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            String message1 =
                "tskv\tconsumer=SoFraud\tentity=account.global_logout_datetime"
                + "\tevent=account_modification"
                + "\tip=2a02:6b8:c1d:65c9:0:4d1b:9c91:0"
                + "\tnew=2022-02-18 12:43:11\told=1970-01-01 03:00:01"
                + "\toperation=updated\tpy=1\ttimestamp=2022-02-18 12:43:11"
                + "\ttimezone=+0300\ttskv_format=passport-log\tuid=100502"
                + "\tunixtime=1645177391\tuser_agent=-"
                + "\trequest_id=@17,68355,1645177391.28,b01645b8ad,-,"
                + "1567734229,axxumip-jifonq";
            String message2 =
                "tskv\tconsumer=passport"
                + "\tentity=account.global_logout_datetime"
                + "\tevent=account_modification\tip=176.59.199.203"
                + "\tnew=2022-02-18 12:47:32\told=2021-11-09 19:28:27"
                + "\toperation=updated\tpy=1\ttimestamp=2022-02-18 12:47:32"
                + "\ttimezone=+0300\ttskv_format=passport-log\tuid=100502"
                + "\tunixtime=1645177652"
                + "\tuser_agent=Mozilla/5.0 (Linux; Android 9; SM-G955F "
                + "Build/PPR1.180610.011; wv) AppleWebKit/537.36 (KHTML, like "
                + "Gecko) Version/4.0 Chrome/98.0.4758.101 Mobile "
                + "Safari/537.36 PassportSDK/7.23.16.723162230"
                + "\trequest_id=@B,12100,1645177652.3,14ae8f2e8d,"
                + "989c0a059eb686331eaa8a3088dd28500f,1431685812,lidapregert";
            String message3 =
                "tskv\tconsumer=mobileproxy\tentity=phones.secure"
                + "\tevent=account_modification"
                + "\tip=2a00:1fa1:c6fd:5ab3:0:52:d82:6201\tnew=+79191******"
                + "\tnew_entity_id=596067638\told=-\told_entity_id=-"
                + "\toperation=created\tpy=1\ttimestamp=2022-02-18 12:47:32"
                + "\ttimezone=+0300\ttskv_format=passport-log\tuid=100502"
                + "\tunixtime=1645177652"
                + "\tuser_agent=com.yandex.mobile.auth.sdk/7.17.0.717001865 "
                + "(Xiaomi M2004J19C; Android 11)"
                + "\trequest_id=@15,2700,1645177652.09,e0aab5ad3c,"
                + "cbfcc4840a49c6132eb4f3e4ae49c4d40e,-,-";

            cluster.addBlackboxUserinfo(
                new FamilypayCluster.BlackboxUserState(100502, false));

            int code =
                cluster.phoneEventsMessageSenderCluster().sendMessages(
                    message1)
                    .first();

            Assert.assertEquals(HttpStatus.SC_OK, code);

            // Not changed
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            code =
                cluster.phoneEventsMessageSenderCluster().sendMessages(
                    message2 + '\n' + message3)
                    .first();

            Assert.assertEquals(HttpStatus.SC_OK, code);

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-unsecured.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.addBlackboxUserinfo(
                new FamilypayCluster.BlackboxUserState(100502, true));

            code =
                cluster.phoneEventsMessageSenderCluster().sendMessages(
                    message3)
                    .first();

            Assert.assertEquals(HttpStatus.SC_OK, code);

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/family/f9000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("family1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            Assert.assertEquals(0, cluster.yandexpay().accessCount());

            cluster.yandexpay().add(
                "/user-glogout?uid=100502*",
                HttpStatus.SC_OK);
            code =
                cluster.glogoutMessageSenderCluster().sendMessages(
                    message1 + '\n' + message2 + '\n' + message3)
                    .first();

            Assert.assertEquals(HttpStatus.SC_OK, code);

            Assert.assertEquals(2, cluster.yandexpay().accessCount());
        }
    }

    @Test
    public void testAllowedServices() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            cluster.addFamily(
                "f9000",
                100500,
                100502,
                100503);

            HttpPost post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/9000/start?uid=100500");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString(
                        "family1-properties-allowed-services.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("family1-allowed-services.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString(
                        "family1-properties-allowed-services-update1.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            HttpGet get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "family1-allowed-services-updated1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString(
                        "family1-properties-allowed-services-update2.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NO_CONTENT,
                    response);
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "family1-allowed-services-updated2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.familypayBackend().httpHost()
                    + "/family/f9000/update/users/single_limit");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString(
                        "family1-users-update-allowed-services.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);
            post.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                FamilypayCluster.USER_TICKET_100500);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"updated\":[100500,100503],\"skipped\":[100501]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.familypayBackend().httpHost() + "/family/f9000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                FamilypayCluster.SERVICE_TICKET);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "family1-updated-allowed-services.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testAlerts() throws Exception {
        try (FamilypayCluster cluster = new FamilypayCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.familypayBackend().httpHost()
                            + "/generate-alerts-config")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        loadResourceAsString("alerts-config.ini")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

