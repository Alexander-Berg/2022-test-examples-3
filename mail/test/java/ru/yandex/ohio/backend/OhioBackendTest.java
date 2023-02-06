package ru.yandex.ohio.backend;

import java.io.File;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.NotImplementedHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class OhioBackendTest extends TestBase {
    private static final String EXPECTED_SERVICES =
        "{\"status\":\"success\",\"code\":200,\"data\":{\"services\":["
        + "{\"service_id\":\"124\",\"subservice_id\":\"124\"},"
        + "{\"service_id\":\"600\",\"subservice_id\":\"600\"},"
        // service_id 609 is not in whitelist and was skipped
        + "{\"service_id\":\"610\",\"subservice_id\":\"610\"},"
        + "{\"service_id\":\"629\",\"subservice_id\":\"629\"},"
        + "{\"service_id\":\"694\",\"subservice_id\":\"694\"},"
        + "{\"service_id\":\"711\",\"subservice_id\":\"711\"}]}}";

    private static final String DARKSPIRIT_URI =
        "/v1/receipts/search-by-payment-ids?limit=100&offset=0";

    private static final long MAIN_USER = 3000441857L;
    private static final long SECONDARY_USER = 4500777L;
    private static final long NON_EXISTING_USER = 12345L;

    private static void prepareIndex(final OhioBackendCluster cluster)
        throws Exception
    {
        HttpPost post =
            new HttpPost(cluster.searchBackend().indexerUri() + "/add");
        post.setEntity(
            new FileEntity(
                new File(
                    OhioBackendTest.class.getResource("transactions.json")
                        .toURI()),
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(
            HttpStatus.SC_OK,
            cluster.searchBackend().client(),
            post);
        cluster.searchBackend().setQueueId(
            new User("ohio_index", new LongPrefix(MAIN_USER)),
            OhioBackendCluster.BACKEND_POSITION);
        cluster.searchBackend().setQueueId(
            new User("ohio_index", new LongPrefix(NON_EXISTING_USER)),
            OhioBackendCluster.BACKEND_POSITION);
    }

    @Test
    public void test() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            cluster.messageSenderCluster().sendMessages(
                loadResourceAsString("test-data1.1.json"),
                loadResourceAsString("test-data1.2.json"),
                loadResourceAsString("test-data1.3.json"),
                loadResourceAsString("test-data1.4.json"),
                loadResourceAsString("test-data1.5.json"));

            cluster.searchBackend().checkSearch(
                "/search?prefix=898653777&text=uid:898653777"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("test-result1.1.json"));
            cluster.searchBackend().checkSearch(
                "/search?prefix=32854777&text=uid:32854777"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("test-result1.2.json"));
            cluster.searchBackend().checkSearch(
                "/search?prefix=655410777&text=uid:655410777"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("test-result1.3.json"));

            cluster.searchBackend().checkSearch(
                "/search?prefix=655410777&text=rows_fiscal_titles:Топливо"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("test-result1.4.json"));
            cluster.searchBackend().checkSearch(
                "/search?prefix=655410777&text=rows_fiscal_titles:service"
                + "&sort=sequence_number&get=*",
                "{\"hitsCount\":0,\"hitsArray\":[]}");
            // Check there is no garbage tokens
            cluster.searchBackend().checkSearch(
                "/printkeys?prefix=655410777%23&field=rows_fiscal_titles",
                new StringChecker(
                    "655410777#31\n"
                    + "655410777#95\n"
                    + "655410777#аи\n"
                    + "655410777#аи\"\n"
                    + "655410777#ая\n"
                    + "655410777#багаж\n"
                    + "655410777#багажа\"\n"
                    + "655410777#и\n"
                    + "655410777#и\"\n"
                    + "655410777#л\n"
                    + "655410777#л\"\n"
                    + "655410777#оплата\n"
                    + "655410777#оплата\"\n"
                    + "655410777#пассажир\n"
                    + "655410777#пассажиров\"\n"
                    + "655410777#перевозка\n"
                    + "655410777#перевозка\"\n"
                    + "655410777#подмосковный\n"
                    + "655410777#подмосковный\"\n"
                    + "655410777#татнефть\n"
                    + "655410777#татнефть\"\n"
                    + "655410777#топлива\"\n"
                    + "655410777#топливо\n"
                    + "655410777#ф\n"
                    + "655410777#ф\"\n"));

            cluster.searchBackend().checkSearch(
                "/search?prefix=440516777&text=refunds_fiscal_titles:кетчуп"
                + "+AND+refunds_fiscal_titles:шашлык&sort=sequence_number&get=*",
                loadResourceAsString("test-result1.5.json"));

            // Try import all data for 440516777
            String data;
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(
                        new HttpGet(
                            cluster.ohioBackend().httpHost()
                            + "/dump-user-data?uid=440516777"
                            + "&new-uid=440516888")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                data = CharsetUtils.toString(response.getEntity());
            }

            HttpPost post =
                new HttpPost(cluster.searchBackend().indexerUri() + "/add");
            post.setEntity(
                new StringEntity(data, ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.searchBackend().client(),
                post);

            cluster.searchBackend().checkSearch(
                "/search?prefix=440516888&text=rows_fiscal_titles:кетчуп"
                + "+AND+rows_fiscal_titles:шашлык&sort=sequence_number&get=*",
                loadResourceAsString("test-result1.5.json")
                    .replaceAll("440516777", "440516888"));

            // Try import single purchase from 655410777
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(
                        new HttpGet(
                            cluster.ohioBackend().httpHost()
                            + "/dump-user-data?uid=655410777"
                            + "&new-uid=655410888&purchase_token="
                            + "ffff904b42d88a8c73bd2d914eb61772")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                data = CharsetUtils.toString(response.getEntity());
            }

            post = new HttpPost(cluster.searchBackend().indexerUri() + "/add");
            post.setEntity(
                new StringEntity(data, ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.searchBackend().client(),
                post);

            cluster.searchBackend().checkSearch(
                "/search?prefix=655410888&text=uid:655410888"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("test-result1.4.json")
                    .replaceAll("655410777", "655410888"));

            // Try import two purchases from 655410777
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(
                        new HttpGet(
                            cluster.ohioBackend().httpHost()
                            + "/dump-user-data?uid=655410777"
                            + "&new-uid=655410999"
                            + "&purchase_token="
                            + "ffff904b42d88a8c73bd2d914eb61772"
                            + "&purchase_token="
                            + "ffff0b2d7391a5549174c70ce819895f")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                data = CharsetUtils.toString(response.getEntity());
            }

            post = new HttpPost(cluster.searchBackend().indexerUri() + "/add");
            post.setEntity(
                new StringEntity(data, ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.searchBackend().client(),
                post);

            cluster.searchBackend().checkSearch(
                "/search?prefix=655410999&text=uid:655410999"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("test-result1.3.json")
                    .replaceAll("655410777", "655410999"));
        }
    }

    @Test
    public void testRowsFiscalTitles() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            HttpPost post =
                new HttpPost(cluster.searchBackend().indexerUri() + "/add");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("test.json"),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.searchBackend().client(),
                post);

            // Check that there is no problems on tokenizer reuse
            for (int i = 0; i < 32; ++i) {
                post =
                    new HttpPost(
                        cluster.searchBackend().indexerUri() + "/update");
                post.setEntity(
                    new StringEntity(
                        loadResourceAsString("test.json"),
                        ContentType.APPLICATION_JSON));
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    cluster.searchBackend().client(),
                    post);
                try (CloseableHttpResponse response =
                        cluster.searchBackend().client().execute(
                            new HttpGet(
                                cluster.searchBackend().searchUri()
                                + "/printkeys?prefix=65964%23"
                                + "&field=rows_fiscal_titles")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals(
                        "65964#багаж\n"
                        + "65964#багажа\"\n"
                        + "65964#и\n"
                        + "65964#и\"\n"
                        + "65964#пассажир\n"
                        + "65964#пассажиров\"\n"
                        + "65964#перевозка\n"
                        + "65964#перевозка\"\n",
                        CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }

    @Test
    public void testServices() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            prepareIndex(cluster);
            cluster.start();
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/services");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(EXPECTED_SERVICES),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check that uid from request is ignored
            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + NON_EXISTING_USER + "/services");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(EXPECTED_SERVICES),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testServicesDirect() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            prepareIndex(cluster);
            cluster.start();
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/services?uid=" + MAIN_USER);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(EXPECTED_SERVICES),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/services?uid=" + NON_EXISTING_USER);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"status\":\"success\",\"code\":200,"
                        + "\"data\":{\"services\":[]}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testOrders() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            prepareIndex(cluster);
            cluster.start();
            cluster.yandexPayMessageSenderCluster().sendMessages(
                loadResourceAsString("yandexpay-data1.1.json"),
                loadResourceAsString("yandexpay-data1.2.json"),
                loadResourceAsString("yandexpay-data1.3.json"),
                loadResourceAsString("yandexpay-data1.4.json"),
                loadResourceAsString("yandexpay-data1.5.json"),
                loadResourceAsString("yandexpay-data1.6.json"),
                loadResourceAsString("yandexpay-data1.7.json"),
                loadResourceAsString("yandexpay-data1.8.json"));

            // Check YandexPay payment status
            cluster.searchBackend().checkSearch(
                "/search?prefix=" + MAIN_USER
                + "&text=purchase_token:%221:gAAAAABhAAAA_AAAAAAAAgYMjWpSY8TDV"
                + "JlycoZNgMG4wMB12pcDWlz8Unr0HACTofT4WBKZ6rDTwyX7dX_pjANLcxRy"
                + "YnouUSILnaHTiZOSXERFftQeWZjwrdh2-dfTIn7V8tD3btUq%22&get=*",
                loadResourceAsString("yandexpay-result1.7.json"));
            // Check YandexPay items labels search
            cluster.searchBackend().checkSearch(
                "/search?prefix=" + MAIN_USER
                + "&text=yandexpay_items_labels:шапку&get=*",
                loadResourceAsString("yandexpay-result1.2.json"));
            // Check there is no garbage tokens
            cluster.searchBackend().checkSearch(
                "/printkeys?prefix=" + MAIN_USER
                + "%23&field=yandexpay_items_labels",
                new StringChecker(
                    "3000441857#766777\n"
                    + "3000441857#beanie\n"
                    + "3000441857#beanie\"\n"
                    + "3000441857#lamo\n"
                    + "3000441857#lamoda\n"
                    + "3000441857#lamoda\"\n"
                    + "3000441857#mushroom\n"
                    + "3000441857#mushroom\"\n"
                    + "3000441857#one\n"
                    + "3000441857#one\"\n"
                    + "3000441857#ru220112\n"
                    + "3000441857#siz\n"
                    + "3000441857#size\n"
                    + "3000441857#size\"\n"
                    + "3000441857#timberland\n"
                    + "3000441857#timberland\"\n"
                    + "3000441857#ön\n"
                    + "3000441857#в\n"
                    + "3000441857#в\"\n"
                    + "3000441857#комиссия\n"
                    + "3000441857#комиссия\"\n"
                    + "3000441857#курьер\n"
                    + "3000441857#курьером\"\n"
                    + "3000441857#мкад\n"
                    + "3000441857#мкад\"\n"
                    + "3000441857#москва\n"
                    + "3000441857#москве\"\n"
                    + "3000441857#по\n"
                    + "3000441857#по\"\n"
                    + "3000441857#предел\n"
                    + "3000441857#пределах\"\n"
                    + "3000441857#сумма\n"
                    + "3000441857#сумма\"\n"
                    + "3000441857#чаевой\n"
                    + "3000441857#чаевые\n"
                    + "3000441857#чаевых\"\n"
                    + "3000441857#шапка\n"
                    + "3000441857#шапка\"\n"));

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/services?uid=" + MAIN_USER);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(EXPECTED_SERVICES),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/services?show-yandexpay&uid=" + MAIN_USER);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        EXPECTED_SERVICES.replace(
                            "{\"service_id\":\"124\","
                            + "\"subservice_id\":\"124\"},",
                            "{\"service_id\":\"1042\","
                            + "\"subservice_id\":\"1042\"},"
                            + "{\"service_id\":\"124\","
                            + "\"subservice_id\":\"124\"},")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=9");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER
                    + "/orders?before-timestamp=1629648000"
                    + "&after-timestamp=1628348464");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("orders-date-range.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER
                    + "/orders?limit=2"
                    + "&created_keyset=75e157b2e3df1bfdb6cf5e99d6009f11"
                    + "&order_id_keyset=1629019634000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("orders-only2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER
                    + "/orders?limit=2&subservice_ids=629&subservice_ids=609");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("lavka-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=4"
                    + "&created_keyset=caf52c09d3e36b441558f68b6a093211"
                    + "&order_id_keyset=1623592492000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("apple-pay-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check that extended subscription is hidden
            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=1"
                    + "&created_keyset=de391a74424f2960e527081a103b168a"
                    + "&order_id_keyset=1614184622000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("extended-subscription.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER
                    + "/orders?limit=9&show-yandexpay");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("yandexpay-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER
                    + "/orders?limit=14&show-yandexpay"
                    + "&created_keyset=528ff9a9f3b51c5ba5b2a211d9c13791"
                    + "&order_id_keyset=1628335699000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("yandexpay-orders2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test subservice_ids filters on Yandex.Pay
            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER
                    + "/orders?show-yandexpay&limit=8"
                    + "&subservice_ids=1042&subservice_ids=629");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("yandexpay-filter-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testRefundOrders() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            cluster.messageSenderCluster().sendMessages(
                loadResourceAsString("test-data1.6.json"),
                loadResourceAsString("test-data1.6-refund.json"));
            cluster.searchBackend().setQueueId(
                new User("ohio_index", new LongPrefix(4016432630L)),
                OhioBackendCluster.BACKEND_POSITION);
            cluster.searchBackend().checkSearch(
                "/search?prefix=4016432630&text=uid:4016432630"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("test-result1.6.json"));

            String userTicket =
                "3:user:CA0Q__________9_GhYKBgj2y5f7DhD2y5f7DiDShdjMBCgB:HzU4K"
                + "EMQWWL_S4ZzzYVxgmgBt-_p2n-uhyClKh124aMOU6FeKOqqnvcr7jcKBtqS"
                + "qrCEaxfTZ-WIeXXRpet7tsxL0kMTCfAQ37qjh5_ob8z9bZmEE4ODaFP5Qc9"
                + "4LbGourfkwn9YtkJ2Oe_nxWA2UYkkvGjkvr-82LmhfaI0ErY";

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/4016432630/orders");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(YandexHeaders.X_YA_USER_TICKET, userTicket);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("refund-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/4016432630/orders"
                    + "?created_keyset=69c57414e334bd50d2a239175c38b9b1"
                    + "&order_id_keyset=1629616900000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(YandexHeaders.X_YA_USER_TICKET, userTicket);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"code\":200,\"data\":{\"orders\":[],\"next\":{}},"
                        + "\"status\":\"success\"}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testYandexAccountOrders() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            prepareIndex(cluster);
            cluster.start();
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER
                    + "/yandex_account_orders?limit=5");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("ya-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testKinopoiskPromos() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            prepareIndex(cluster);
            cluster.start();
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=4"
                    + "&created_keyset=eb121d9493df7831d4340e12e11314be"
                    + "&order_id_keyset=1615733118000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("kinopoisk-promos.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testImportRefunds() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            prepareIndex(cluster);
            cluster.start();
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=1"
                    + "&created_keyset=c40a333a43f81a4bf727759cfab7ec9b"
                    + "&order_id_keyset=1627815738000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("without-refund1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=1"
                    + "&created_keyset=970ad69dc35f0d9a3753ad9d2e663990"
                    + "&order_id_keyset=1618345482000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("without-refund2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/import-refunds?file="
                    + Paths.getSourcePath(
                        "mail/ohio/ohio_backend/test/resources/ru/yandex/ohio/"
                        + "backend/refunds-from-yt.json"));
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "5 lines processed",
                    CharsetUtils.toString(response.getEntity()));
            }

            // Refunds imported, but ignored
            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=1"
                    + "&created_keyset=c40a333a43f81a4bf727759cfab7ec9b"
                    + "&order_id_keyset=1627815738000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("with-refund1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=1"
                    + "&created_keyset=970ad69dc35f0d9a3753ad9d2e663990"
                    + "&order_id_keyset=1618345482000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("with-refund2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testHistoricalWithdraw() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            prepareIndex(cluster);
            cluster.start();
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=4"
                    + "&created_keyset=6ff528c1c35bbe244518d394d0560f9b"
                    + "&order_id_keyset=1620676383000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("topup.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testHistoricalRefuelPromocode() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            cluster.searchBackend().setQueueId(
                new User("ohio_index", new LongPrefix(MAIN_USER)),
                OhioBackendCluster.BACKEND_POSITION);

            HttpPost post =
                new HttpPost(cluster.searchBackend().indexerUri() + "/add");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("refuel-cashback.json"),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.searchBackend().client(),
                post);

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("refuel-cashback-response.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPaymentStatusUpdate() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            cluster.messageSenderCluster().sendMessages(
                // Will be shown as authorized
                "{\"purchase_token\":\"tkn1\",\"passport_id\":\"5598601\","
                + "\"dt\":\"2021-07-08T03:01:03+00:00\","
                + "\"payment_status\":\"initial\","
                + "\"service_id\":610,\"terminal_id\":211103,"
                + "\"payment_method\":\"card\","
                + "\"rows\":[{}],"
                + "\"payment_type\":\"TRUST_PAYMENT\",\"amount\":3,"
                + "\"currency\":\"RUB\",\"trust_payment_id\":4}",
                "{\"purchase_token\":\"tkn1\",\"passport_id\":\"5598601\","
                + "\"dt\":\"2021-07-08T03:01:03+00:00\","
                + "\"payment_dt\":\"2021-07-08T03:04:05+00:00\","
                + "\"payment_status\":\"authorized\","
                + "\"service_id\":610,\"terminal_id\":211103,"
                + "\"payment_method\":\"card\","
                + "\"rows\":[{}],"
                + "\"payment_type\":\"TRUST_PAYMENT\",\"amount\":3,"
                + "\"currency\":\"RUB\",\"trust_payment_id\":4}",
                // Will be shown as postauthorized
                "{\"purchase_token\":\"tkn2\",\"passport_id\":\"5598601\","
                + "\"dt\":\"2021-07-08T04:01:03+00:00\","
                + "\"payment_status\":\"initial\","
                + "\"service_id\":610,\"terminal_id\":211103,"
                + "\"payment_method\":\"card\","
                + "\"rows\":[{}],"
                + "\"payment_type\":\"TRUST_PAYMENT\",\"amount\":3,"
                + "\"currency\":\"RUB\",\"trust_payment_id\":4}",
                "{\"purchase_token\":\"tkn2\",\"passport_id\":\"5598601\","
                + "\"dt\":\"2021-07-08T04:01:03+00:00\","
                + "\"payment_dt\":\"2021-07-08T04:04:05+00:00\","
                + "\"payment_status\":\"authorized\","
                + "\"service_id\":610,\"terminal_id\":211103,"
                + "\"payment_method\":\"card\","
                + "\"rows\":[{}],"
                + "\"payment_type\":\"TRUST_PAYMENT\",\"amount\":3,"
                + "\"currency\":\"RUB\",\"trust_payment_id\":4}",
                "{\"purchase_token\":\"tkn2\",\"passport_id\":\"5598601\","
                + "\"dt\":\"2021-07-08T04:01:03+00:00\","
                + "\"payment_dt\":\"2021-07-08T04:04:05+00:00\","
                + "\"postauth_dt\":\"2021-07-08T04:05:06+00:00\","
                + "\"payment_status\":\"postauthorized\","
                + "\"service_id\":610,\"terminal_id\":211103,"
                + "\"payment_method\":\"card\","
                + "\"rows\":[{}],"
                + "\"payment_type\":\"TRUST_PAYMENT\",\"amount\":3,"
                + "\"currency\":\"RUB\",\"trust_payment_id\":4}",
                // Won't be shown because of initial state
                "{\"purchase_token\":\"tkn3\",\"passport_id\":\"5598601\","
                + "\"dt\":\"2021-07-08T05:01:03+00:00\","
                + "\"payment_status\":\"initial\","
                + "\"service_id\":610,\"terminal_id\":211103,"
                + "\"payment_method\":\"card\","
                + "\"rows\":[{}],"
                + "\"payment_type\":\"TRUST_PAYMENT\",\"amount\":3,"
                + "\"currency\":\"RUB\",\"trust_payment_id\":4}",
                // Won't be shown because of not authorized state
                "{\"purchase_token\":\"tkn4\",\"passport_id\":\"5598601\","
                + "\"dt\":\"2021-07-08T06:01:03+00:00\","
                + "\"payment_status\":\"initial\","
                + "\"service_id\":610,\"terminal_id\":211103,"
                + "\"payment_method\":\"card\","
                + "\"rows\":[{}],"
                + "\"payment_type\":\"TRUST_PAYMENT\",\"amount\":3,"
                + "\"currency\":\"RUB\",\"trust_payment_id\":4}");
            cluster.messageSenderCluster().sendMessages(
                "{\"purchase_token\":\"tkn4\",\"passport_id\":\"5598601\","
                + "\"dt\":\"2021-07-08T06:01:03+00:00\","
                + "\"cancel_dt\":\"2021-07-08T06:04:05+00:00\","
                + "\"payment_status\":\"not authorized\","
                + "\"service_id\":610,\"terminal_id\":211103,"
                + "\"payment_method\":\"card\","
                + "\"rows\":[{}],"
                + "\"payment_type\":\"TRUST_PAYMENT\",\"amount\":3,"
                + "\"currency\":\"RUB\",\"trust_payment_id\":4}");
            cluster.searchBackend().checkSearch(
                "/search?prefix=5598601&text=uid:5598601"
                + "&sort=sequence_number"
                + "&get=sequence_number,purchase_token,payment_status"
                + ",last_payment_status",
                loadResourceAsString("status-result.json"));
            cluster.searchBackend().checkSearch(
                "/search?prefix=5598601&text=payment_status:authorized"
                + "&sort=sequence_number"
                + "&get=sequence_number,purchase_token,payment_status"
                + ",last_payment_status",
                loadResourceAsString("authorized-result.json"));

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/orders?uid=5598601");
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("status-orders-with-holds.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTaxiFolding() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            prepareIndex(cluster);
            cluster.start();
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=2"
                    + "&created_keyset=6347e38cd340c8a8f3eeab94243f34f4"
                    + "&order_id_keyset=1616860112000");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("taxi-orders1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?limit=2"
                    + "&created_keyset=62f5532742bdf0a302874501420f0bb8"
                    + "&order_id_keyset=1627827850000"
                    + "&subservice_ids=124&subservice_ids=125");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("taxi-orders2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBnpl() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            HttpPost post =
                new HttpPost(cluster.searchBackend().indexerUri() + "/add");
            post.setEntity(
                new FileEntity(
                    new File(
                        OhioBackendTest.class.getResource("bnpl.json")
                            .toURI()),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.searchBackend().client(),
                post);
            cluster.searchBackend().setQueueId(
                new User("ohio_index", new LongPrefix(SECONDARY_USER)),
                OhioBackendCluster.BACKEND_POSITION);

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + SECONDARY_USER + "/orders");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.SECONDARY_USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("bnpl-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDonation() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            HttpPost post =
                new HttpPost(cluster.searchBackend().indexerUri() + "/add");
            post.setEntity(
                new FileEntity(
                    new File(
                        OhioBackendTest.class.getResource("donation.json")
                            .toURI()),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.searchBackend().client(),
                post);
            cluster.searchBackend().setQueueId(
                new User("ohio_index", new LongPrefix(SECONDARY_USER)),
                OhioBackendCluster.BACKEND_POSITION);

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + SECONDARY_USER + "/orders");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.SECONDARY_USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("donation-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testReceipts() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.darkspirit().add(
                DARKSPIRIT_URI,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            loadResourceAsString("darkspirit-request1.json")),
                        loadResourceAsString("darkspirit-response1.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    OhioBackendCluster.DARKSPIRIT_SERVICE_TICKET),
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            loadResourceAsString("darkspirit-request2.json")),
                        loadResourceAsString("darkspirit-response2.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    OhioBackendCluster.DARKSPIRIT_SERVICE_TICKET),
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            loadResourceAsString("darkspirit-request3.json")),
                        loadResourceAsString("darkspirit-response3.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    OhioBackendCluster.DARKSPIRIT_SERVICE_TICKET),
                NotImplementedHttpItem.INSTANCE);
            cluster.darkspirit().add(
                "/v1/fiscal_storages/9960440300355180/documents/228074"
                + "/569806060",
                loadResourceAsString("income-receipt.json"));
            cluster.darkspirit().add(
                "/v1/fiscal_storages/9960440300355978/documents/216359"
                + "/4176031672",
                loadResourceAsString("return-receipt.json"));
            cluster.darkspirit().add(
                "/v1/fiscal_storages/9960440300354856/documents/84216"
                + "/192659148",
                loadResourceAsString("card-receipt.json"));
            cluster.darkspirit().add(
                "/v1/fiscal_storages/9960440300351827/documents/109240"
                + "/417822257",
                loadResourceAsString("prepayment-receipt.json"));
            cluster.start();

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/receipts"
                    + "?purchase_token=4f18f6fc827cc62d67fb7910d32fae1f");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("return-receipts.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/receipts"
                    + "?purchase_token=d168f541825e25ea176c5d0adea81061");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("prepayment-receipts.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/receipts"
                    + "?purchase_token=0000f6fc827cc62d67fb7910d32fae1f");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("empty-receipts.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBynPlus() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            HttpPost post =
                new HttpPost(cluster.searchBackend().indexerUri() + "/add");
            post.setEntity(
                new FileEntity(
                    new File(
                        OhioBackendTest.class.getResource(
                            "byn-transactions.json")
                            .toURI()),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.searchBackend().client(),
                post);
            cluster.searchBackend().setQueueId(
                new User("ohio_index", new LongPrefix(1482667777L)),
                OhioBackendCluster.BACKEND_POSITION);

            String userTicket =
                "3:user:CA0Q__________9_GhYKBgiB7v7CBRCB7v7CBSDShdjMBCgB:DKlC6"
                + "4Ar4uY5xyNfsMeDhrEMZ1DEIEijUAqf4keQDrcqGJXSVrgOY8kmDTdjoYex"
                + "lfyTgGfEUuF_X2S-sZSaM0y2a_QHys4wjmVzlbNZh6kR1VWJ33-bDA5Z0CM"
                + "GFbffFIk7SBQUIICwiasVIEOCsYZk1bwk9GPMrtVnzlkoxjI";

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/1482667777/orders");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(YandexHeaders.X_YA_USER_TICKET, userTicket);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("byn-plus-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPayment() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            prepareIndex(cluster);
            cluster.start();
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER
                    + "/payment?trust_payment_id=610e6e50bed21e69990f6cbc");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("payment1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testServiceFee() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            HttpPost post =
                new HttpPost(cluster.searchBackend().indexerUri() + "/add");
            post.setEntity(
                new FileEntity(
                    new File(
                        OhioBackendTest.class.getResource("service-fee.json")
                            .toURI()),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.searchBackend().client(),
                post);
            cluster.searchBackend().setQueueId(
                new User("ohio_index", new LongPrefix(45007777L)),
                OhioBackendCluster.BACKEND_POSITION);
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/45007777/orders");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                "3:user:CA0Q__________9_GhQKBQihh7sVEKGHuxUg0oXYzAQoAQ:H5x4PCp"
                + "hgptTvlLwysCvs_WWUXLabNgbzVh1CjC_KYFc1Yw_b62mHOyKZYBemYnNbP"
                + "36dQtjG5JMfzpKHR_Awql5k2cIxwQ9j9IChD0Md8ub6vcuIeuNpQPez9uuZ"
                + "tMGz8wIh97Jy95AGpva-YAGFV56BSYTL3V0tHVJj_LP1yQ");
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("service-fee-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFamilypay() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            cluster.messageSenderCluster().sendMessages(
                loadResourceAsString("familypay-transaction.json"));
            cluster.messageSenderCluster().sendMessages(
                loadResourceAsString("familypay-transaction2.json"));
            cluster.messageSenderCluster().sendMessages(
                loadResourceAsString("non-familypay-transaction.json"));
            cluster.searchBackend().setQueueId(
                new User("ohio_index", new LongPrefix(738317777L)),
                OhioBackendCluster.BACKEND_POSITION);
            cluster.searchBackend().setQueueId(
                new User("ohio_index", new LongPrefix(738317778L)),
                OhioBackendCluster.BACKEND_POSITION);
            cluster.searchBackend().setQueueId(
                new User("ohio_index", new LongPrefix(42177777L)),
                OhioBackendCluster.BACKEND_POSITION);
            cluster.searchBackend().checkSearch(
                "/search?prefix=738317777&text=uid:738317777"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("familypay-initiator.json"));
            cluster.searchBackend().checkSearch(
                "/search?prefix=738317777&text=sponsor_uid:42177777"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("familypay-initiator.json"));
            cluster.searchBackend().checkSearch(
                "/search?prefix=42177777&text=uid:42177777"
                + "&sort=timestamp&get=*",
                loadResourceAsString("familypay-sponsor-all.json"));
            cluster.searchBackend().checkSearch(
                "/search?prefix=42177777&text=initiator_uid:738317777"
                + "&sort=sequence_number&get=*",
                loadResourceAsString("familypay-sponsor.json"));

            String initiatorUserTicket =
                "3:user:CA0Q__________9_GhYKBgjRq4fgAhDRq4fgAiDShdjMBCgB:LlmIZ"
                + "oxp-DKvmCK9sGB3bYftJlGA_iPxT9zqoADn6u15z-1YiwZXTQrbHDJYXVZ_"
                + "-P37Qj8eEiizw1snNh5o70l6g6OAiFZhkyCPQxcf1qsygVtgvLhDhdUL8MJ"
                + "wFIIWBseaFOIi9ctS60Xb9CZu2hUZh5DgaMxRKsMlaciYUMI";
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/738317777/orders");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                initiatorUserTicket);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "familypay-initiator-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/familypay_users?uid=738317777");
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"status\":\"success\",\"code\":200,\"data\":"
                        + "{\"familypay_users\":[]}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            String sponsorUserTicket =
                "3:user:CA0Q__________9_GhQKBQjxqY4UEPGpjhQg0oXYzAQoAQ:Gzxe3xq"
                + "ilX7eVi4EAu-lFeXDx7RL1oebZ2wB4JzwZ5Pj4zc84Tmtj6H6y_Fwj0KGVD"
                + "Z57rhm3pL03Jltk-y-Qa78gjwtFNGNKgOsWNIlIjwm63NnkFebVyjVljU4F"
                + "qh1Zd8KWSNCllqd3l3Wc8kUXYooTBliP-4ITlZourdbUwI";
            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/42177777/orders");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                sponsorUserTicket);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "familypay-sponsor-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/42177777/orders?hide-family-payments");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                sponsorUserTicket);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "familypay-only-sponsor-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/42177777/orders?initiator_uid=738317777");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                sponsorUserTicket);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "familypay-sponsor-child-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/42177777/orders?"
                    + "initiator_uid=738317778&initiator_uid=738317777");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                sponsorUserTicket);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString(
                            "familypay-sponsor-children-orders.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/42177777/familypay_users");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                sponsorUserTicket);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"status\":\"success\",\"code\":200,\"data\":"
                        + "{\"familypay_users\":"
                        + "[{\"initiator_uid\":738317777},"
                        + "{\"initiator_uid\":738317778}]}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFnsOrders() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.dyngo().add(
                "/ya/user/3000441857/checks?sort=check_date+desc,uuid+desc"
                + "&limit=10&offset=0",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString("fns-checks-0-9.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    OhioBackendCluster.DYNGO_SERVICE_TICKET));
            cluster.dyngo().add(
                "/ya/user/3000441857/checks?sort=check_date+desc,uuid+desc"
                + "&limit=20&offset=10",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString("fns-checks-10-20.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    OhioBackendCluster.DYNGO_SERVICE_TICKET));
            cluster.dyngo().add(
                "/ya/user/3000441857/checks?sort=check_date+desc,uuid+desc"
                + "&limit=20&offset=21",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_NO_CONTENT),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    OhioBackendCluster.DYNGO_SERVICE_TICKET));
            cluster.start();

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/fns_orders?uid=3000441857&limit=10");
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("fns-orders-0-9.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/3000441857/fns_orders?limit=10"
                    + "&order_id_keyset=10"
                    + "&created_keyset=de2e7bc3-bd31-5c5d-9336-44e710d2d01a");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("fns-orders-10-18.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/fns_orders?uid=3000441857&limit=10"
                    + "&order_id_keyset=21"
                    + "&created_keyset=f34262e4-36ba-560d-96e1-236f9a6b7213");
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"code\":200,\"data\":{\"orders\":[],\"next\":{}},"
                        + "\"status\":\"success\"}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFnsOrdersWithRetryFns() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.dyngo().add(
                "/ya/user/3000441857/checks?sort=check_date+desc,uuid+desc"
                + "&limit=10&offset=0",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString("fns-checks2.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    OhioBackendCluster.DYNGO_SERVICE_TICKET));
            cluster.start();

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/fns_orders?uid=3000441857&limit=10");
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("fns-orders2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFnsLinker() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            String fnslinkerResponse =
                "{\"status\": \"has_binding\", \"urls\": "
                + "{\"touch\": \"https://checks.edadeal.yandex.ru\"}}";
            cluster.fnslinker().add(
                "/api/ya/v1/users/3000441857",
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(fnslinkerResponse),
                        YandexHeaders.X_YA_SERVICE_TICKET,
                        OhioBackendCluster.FNSLINKER_SERVICE_TICKET)));
            cluster.start();

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/3000441857/fns_binding_status");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(fnslinkerResponse),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/fns_binding_status?uid=3000441857");
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(fnslinkerResponse),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCashbackBalance() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.dyngo().add(
                "/ya/user/" + MAIN_USER + "/balance",
                "{\"available\": 39332}");
            cluster.start();
            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/cashback_balance?uid=" + MAIN_USER);
            String expected =
                "{\"code\":200,\"status\":\"success\","
                + "\"data\":{\"amount\":\"393.32\"}}";
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/cashback_balance");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testImportYandexPay() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();

            HttpGet get =
                new HttpGet(
                    cluster.ohioIndexer().host()
                    + "/import-file?ungzip&offset=1&batch-size=2&file="
                    + Paths.getSourcePath(
                        "mail/ohio/ohio_backend/test/resources/ru/yandex/ohio/"
                        + "backend/yandex-pay-yt.json.gz"));
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "4 lines processed",
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders?show-yandexpay");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("imported-yandexpay.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testYandexBankPlus() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            cluster.messageSenderCluster().sendMessages(
                loadResourceAsString("yandex-bank1.json"),
                loadResourceAsString("yandex-bank2.json"));

            HttpGet get =
                new HttpGet(
                    cluster.ohioBackend().httpHost()
                    + "/v1/customer/" + MAIN_USER + "/orders");
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                OhioBackendCluster.SERVICE_TICKET);
            get.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                OhioBackendCluster.USER_TICKET);
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(get))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("yandex-bank-response.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.searchBackend().checkSearch(
                "/search?prefix=" + MAIN_USER + "&text=cashback_service:bank"
                + "&get=purchase_token",
                "{\"hitsCount\":1,\"hitsArray\":[{\"purchase_token\":\""
                + "b05fb71733e9bd84d01d2f06474effff\"}]}");
        }
    }

    @Test
    public void testAlertsConfigs() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(
                        new HttpGet(
                            cluster.ohioBackend().httpHost()
                            + "/generate-alerts-config")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        loadResourceAsString(
                            "ohio-backend-alerts-config.ini")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSearcherGolovanPanel() throws Exception {
        try (OhioBackendCluster cluster = new OhioBackendCluster(this)) {
            cluster.start();
            try (CloseableHttpResponse response =
                    cluster.searchBackend().client().execute(
                        new HttpGet(
                            cluster.searchBackend().searchUri()
                            + "/generate-golovan-panel")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("searcher-panel.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

