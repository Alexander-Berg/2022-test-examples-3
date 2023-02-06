package ru.yandex.msearch.proxy;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.request.RequestPatternParser;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class DkimStatsTest extends TestBase {
    private static final long UPDATE_DELAY = 500L;

    public DkimStatsTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.producer().register(
                RequestPatternParser.INSTANCE.apply(
                    "/update*{arg_service:change_log}"),
                new ProxyHandler(cluster.backend().indexerHost()));
            cluster.producer().add("/*", "[{\"localhost\":6}]");
            cluster.start();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@yandex.ru"
                            + "&dkim-domain=yandex.ru&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[],"
                        + "\"best_domain\":0,"
                        + "\"total\":0,"
                        + "\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check that basic case works and counter incremented properly
            for (int i = 1; i <= 3; ++i) {
                logger.info("Iteration #" + i);
                long timestamp = 1234567890 + i;
                try (CloseableHttpResponse response =
                        client.execute(
                            new HttpGet(
                                cluster.proxy().host()
                                + "/api/async/so/update-dkim-stats"
                                + "?from=analizer@ya.ru"
                                + "&dkim-domain=mail.yandex.ru"
                                + "&timestamp=" + timestamp)))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                }
                Thread.sleep(UPDATE_DELAY);
                HttpAssert.assertStat(
                    "dkim-stats-update-count_dmmm",
                    Integer.toString(i),
                    cluster.proxy().port());

                try (CloseableHttpResponse response =
                        client.execute(
                            new HttpGet(
                                cluster.proxy().host()
                                + "/api/async/so/get-dkim-stats"
                                + "?from=analizer@yandex.ru"
                                + "&dkim-domain=yandex.ru&timestamp="
                                + timestamp)))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"dkim_stats\":{"
                            + "\"top_domains\":[" + i
                            + "],\"best_domain\":" + i
                            + ",\"total\":" + i
                            + ",\"dkimless\":0}}"),
                        CharsetUtils.toString(response.getEntity()));
                }
            }

            // Check that there is only requested domains in response
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/update-dkim-stats"
                            + "?from=analizer@yandex.kz"
                            + "&dkim-domain=google.com"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            Thread.sleep(UPDATE_DELAY);

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&dkim-domain=mail.yandex.ru"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[3,1],"
                        + "\"best_domain\":3,"
                        + "\"total\":4,\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&dkim-domain=mail.google.com"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[3,1],"
                        + "\"best_domain\":1,"
                        + "\"total\":4,\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check that multi-domain requests supported
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&dkim-domain=mail.google.com"
                            + "&dkim-domain=yandex.ru&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[3,1],"
                        + "\"best_domain\":3,"
                        + "\"total\":4,\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check that dkim domains list may be empty
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[3,1],"
                        + "\"total\":4,\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check that multi domain counters may be updated
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/update-dkim-stats"
                            + "?from=analizer@yandex.kz"
                            + "&dkim-domain=google.com"
                            + "&dkim-domain=mail.yandex.ru"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            Thread.sleep(UPDATE_DELAY);

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&dkim-domain=mail.yandex.ru"
                            + "&dkim-domain=google.com"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[4,2],"
                        + "\"best_domain\":4,"
                        + "\"total\":5,\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&dkim-domain=google.com"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[4,2],"
                        + "\"best_domain\":2,"
                        + "\"total\":5,\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check that there is a counter for e-mails without dkim
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/update-dkim-stats"
                            + "?from=analizer@yandex.kz"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            Thread.sleep(UPDATE_DELAY);

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&dkim-domain=mail.yandex.ru"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[4,2],"
                        + "\"best_domain\":4,"
                        + "\"total\":6,\"dkimless\":1}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Check that multi domain counters may be updated with bigger
            // count and check that top_domains is limited
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/update-dkim-stats"
                            + "?from=analizer@yandex.kz"
                            + "&dkim-domain=google.com"
                            + "&dkim-domain=mail.ru"
                            + "&dkim-domain=hotmail.com"
                            + "&timestamp=1234567890&count=5")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            Thread.sleep(UPDATE_DELAY);

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&dkim-domain=yandex.ru&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[7,5,5],"
                        + "\"best_domain\":4,"
                        + "\"total\":11,\"dkimless\":1}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Check that time limit is working
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&dkim-domain=yandex.ru&timestamp=1235777490")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[7,5,5],"
                        + "\"best_domain\":4,"
                        + "\"total\":11,\"dkimless\":1}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=analizer@ya.ru"
                            + "&dkim-domain=yandex.ru&timestamp=1235950290")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[],"
                        + "\"best_domain\":0,"
                        + "\"total\":0,\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testNegativeHash() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.producer().register(
                RequestPatternParser.INSTANCE.apply(
                    "/update*{arg_service:change_log}"),
                new ProxyHandler(cluster.backend().indexerHost()));
            cluster.producer().add("/*", "[{\"localhost\":6}]");
            cluster.start();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/update-dkim-stats"
                            + "?from=%2B79119392542@unknown.email"
                            + "&dkim-domain=mail.yandex.ru"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            Thread.sleep(UPDATE_DELAY);

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=%2B79119392542@unknown.email"
                            + "&dkim-domain=yandex.ru&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[1],"
                        + "\"best_domain\":1,"
                        + "\"total\":1,"
                        + "\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCacheTtl() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(
                    this,
                    new MsearchProxyCluster.MproxyClusterContext()
                        .producer(true)
                        .dkimCacheTtl(2000)))
        {
            cluster.producer().register(
                RequestPatternParser.INSTANCE.apply(
                    "/update*{arg_service:change_log}"),
                new ProxyHandler(cluster.backend().indexerHost()));
            cluster.producer().add("/*", "[{\"localhost\":6}]");
            cluster.start();

            try (CloseableHttpClient client = Configs.createDefaultClient();
                CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/update-dkim-stats"
                            + "?from=%2B79119392542@unknown.email"
                            + "&dkim-domain=mail.yandex.ru"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            Thread.sleep(UPDATE_DELAY);

            String stats = HttpAssert.stats(cluster.proxy().port());
            HttpAssert.assertStat(
                "dkim-stats-cache-hit-type-miss_ammm",
                "0",
                stats);
            HttpAssert.assertStat(
                "dkim-stats-cache-element-count_ammm",
                "0",
                stats);

            try (CloseableHttpClient client = Configs.createDefaultClient();
                CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=%2B79119392542@unknown.email"
                            + "&dkim-domain=yandex.ru&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[1],"
                        + "\"best_domain\":1,"
                        + "\"total\":1,"
                        + "\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            stats = HttpAssert.stats(cluster.proxy().port());
            HttpAssert.assertStat(
                "dkim-stats-cache-hit-type-miss_ammm",
                "1",
                stats);
            HttpAssert.assertStat(
                "dkim-stats-cache-hit-type-hit_ammm",
                "0",
                stats);
            HttpAssert.assertStat(
                "dkim-stats-cache-element-count_ammm",
                "1",
                stats);

            try (CloseableHttpClient client = Configs.createDefaultClient();
                CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/update-dkim-stats"
                            + "?from=%2B79119392542@unknown.email"
                            + "&dkim-domain=mail.yandex.ru"
                            + "&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            Thread.sleep(UPDATE_DELAY);

            // Result will be taken from cache
            try (CloseableHttpClient client = Configs.createDefaultClient();
                CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=%2B79119392542@unknown.email"
                            + "&dkim-domain=yandex.ru&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[1],"
                        + "\"best_domain\":1,"
                        + "\"total\":1,"
                        + "\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            stats = HttpAssert.stats(cluster.proxy().port());
            HttpAssert.assertStat(
                "dkim-stats-cache-hit-type-miss_ammm",
                "1",
                stats);
            HttpAssert.assertStat(
                "dkim-stats-cache-hit-type-hit_ammm",
                "1",
                stats);
            HttpAssert.assertStat(
                "dkim-stats-cache-element-count_ammm",
                "1",
                stats);

            Thread.sleep(2100);

            logger.info("Cache will be invalidated by now");
            try (CloseableHttpClient client = Configs.createDefaultClient();
                CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/get-dkim-stats"
                            + "?from=%2B79119392542@unknown.email"
                            + "&dkim-domain=yandex.ru&timestamp=1234567890")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"dkim_stats\":{"
                        + "\"top_domains\":[2],"
                        + "\"best_domain\":2,"
                        + "\"total\":2,"
                        + "\"dkimless\":0}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            stats = HttpAssert.stats(cluster.proxy().port());
            HttpAssert.assertStat(
                "dkim-stats-cache-hit-type-miss_ammm",
                "2",
                stats);
            HttpAssert.assertStat(
                "dkim-stats-cache-hit-type-hit_ammm",
                "1",
                stats);
            HttpAssert.assertStat(
                "dkim-stats-cache-element-count_ammm",
                "1",
                stats);
        }
    }
}

