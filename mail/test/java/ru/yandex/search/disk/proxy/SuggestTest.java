package ru.yandex.search.disk.proxy;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.disk.proxy.suggest.SuggestType;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class SuggestTest extends TestBase {
    private static final String PRODUCER_URI =
        "/_status?service=disk_queue&prefix=0&all&json-type=dollar";
    private static final String PRODUCER_RESPONSE = "[{$localhost\0:100500}]";
    private static final long BACKEND_POS = 100500L;
    private static final User USER =
        new User("disk_queue", new LongPrefix(0L));
    private static final String PROMO_URI = "/suggest/promo?uid=0";
    private static final String SUGGEST_PREFIX =
        "{\"retry-suggest-types\":[],\"suggest\":[";
    private static final String DISK = "/disk/";
    private static final String IMAGE_JPEG = "image/jpeg";
    private static final String ID = ",\"id\":\"";
    private static final String RESOURCE_ID = "\",\"resource_id\":\"0:";

    @Test
    public void testPromo() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(USER, BACKEND_POS);

            cluster.producer().add(PRODUCER_URI, PRODUCER_RESPONSE);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + PROMO_URI)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"promo-suggest\":null}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.backend().add(SearchHandlerTest.doc(2, "root"));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + PROMO_URI)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"promo-suggest\":\"root\"}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.backend().add(
                SearchHandlerTest.doc(2 + 1, "Папка", "файл3.жпг"),
                SearchHandlerTest.doc(0, "Мамка", "файл2.жпг"),
                SearchHandlerTest.doc(1, "Дедка", "файл1.жпг"));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + PROMO_URI)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"promo-suggest\":\"файл2.жпг\"}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    // CSOFF: ParameterNumber
    private static String filesSuggest(
        final SuggestType type,
        final String suggestText,
        final String text,
        final int id,
        final String key,
        final String mimetype)
    {
        StringBuilder sb = new StringBuilder("{\"type\":\"");
        sb.append(type.toString());
        sb.append("\",\"suggest-text\":\"");
        sb.append(suggestText);
        sb.append("\",\"text\":\"");
        sb.append(text);
        sb.append('"');
        sb.append(ID);
        sb.append(id);
        sb.append(RESOURCE_ID);
        sb.append(id);
        sb.append("\",\"key\":\"");
        sb.append(key);
        if (mimetype != null) {
            sb.append("\",\"mimetype\":\"");
            sb.append(mimetype);
        }
        sb.append('"');
        sb.append('}');
        return new String(sb);
    }
    // CSON: ParameterNumber

    private static String djfsUri(final String... ids) {
        StringBuilder sb = new StringBuilder(
            "/api/v1/indexer/resources?service=disk-search&uid=0");
        for (String id: ids) {
            sb.append("&resource_id=0:");
            sb.append(id);
        }
        return new String(sb);
    }

    private static String djfsResponse(final String... ids) {
        StringBuilder sb = new StringBuilder("{\"items\":[{");
        for (int i = 0; i < ids.length; ++i) {
            if (i != 0) {
                sb.append('}');
                sb.append(',');
                sb.append('{');
            }
            sb.append("\"visible\":\"true\",\"version\":");
            sb.append(i);
            sb.append(ID);
            sb.append(ids[i]);
            sb.append(RESOURCE_ID);
            sb.append(ids[i]);
            sb.append('"');
        }
        sb.append('}');
        sb.append(']');
        sb.append('}');
        return new String(sb);
    }

    // CSOFF: MagicNumber
    // CSOFF: MethodLength
    @Test
    public void testFiles() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(USER, BACKEND_POS);

            cluster.producer().add(PRODUCER_URI, PRODUCER_RESPONSE);

            String folder = "folder";
            String file = "file";
            String fil = "fil";
            String fol = "fol";
            String test = "test.jpg";
            String braces = "110-dummy (3).jpg";
            String cucumber = "Hıyar.jpg";
            cluster.backend().add(
                SearchHandlerTest.doc(0, file + '/' + folder),
                SearchHandlerTest.doc(1, folder),
                SearchHandlerTest.doc(2, folder, file),
                SearchHandlerTest.doc(3, file, folder),
                // add CV tags, so this will have mediatype image/jpeg
                SearchHandlerTest.doc(4, folder, fil, "ab"),
                SearchHandlerTest.doc(5, folder, test),
                SearchHandlerTest.doc(6, folder, braces),
                SearchHandlerTest.doc(7, folder, cucumber, "trash"));
            cluster.djfs().add(
                djfsUri(Integer.toString(2), Integer.toString(4)),
                djfsResponse(
                    Integer.toString(2),
                    "4\",\"media_type\":\"image"));
            String uri =
                "/suggest?uid=0&length=6&suggest-types=files&request=";
            String highlight =
                "<span class=\\\"disk-search-highlight\\\">fil</span>";
            // Should find files starting with 'fil', no folders
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "FIL")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + filesSuggest(
                            SuggestType.FILES,
                            highlight + 'e',
                            file,
                            2,
                            DISK + folder + '/' + file,
                            null)
                        + ','
                        + filesSuggest(
                            SuggestType.FILES,
                            highlight,
                            fil,
                            4,
                            DISK + folder + '/' + fil,
                            "image/jpeg\",\"mediatype\":\"image")
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.djfs().add(
                djfsUri(Integer.toString(3)),
                djfsResponse(Integer.toString(3)));
            cluster.djfs().add(
                djfsUri(Integer.toString(0), Integer.toString(1)),
                djfsResponse(Integer.toString(0), Integer.toString(1)));
            uri =
                "/suggest?uid=0&length=6&suggest-types=files,folders&request=";
            highlight =
                "<span class=\\\"disk-search-highlight\\\">fol</span>der";
            // Should be files and folders starting with 'fol', only leaf paths
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + fol)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + filesSuggest(
                            SuggestType.FOLDERS,
                            highlight,
                            folder,
                            0,
                            DISK + file + '/' + folder,
                            null)
                        + ','
                        + filesSuggest(
                            SuggestType.FOLDERS,
                            highlight,
                            folder,
                            1,
                            DISK + folder,
                            null)
                        + ','
                        + filesSuggest(
                            SuggestType.FILES,
                            highlight,
                            folder,
                            3,
                            DISK + file + '/' + folder,
                            null)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Nothing should be found
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "screen")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(SUGGEST_PREFIX + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test that last token will be tokenized as alnum sequence
            cluster.djfs().add(
                djfsUri(Integer.toString(5)),
                djfsResponse(Integer.toString(5)));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + test)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + filesSuggest(
                            SuggestType.FILES,
                            "<span class=\\\"disk-search-highlight\\\">test"
                            + "</span>.<span class=\\\"disk-search-highlight"
                            + "\\\">jpg</span>",
                            test,
                            5,
                            DISK + folder + '/' + test,
                            null)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test complex query with spec-symbols
            cluster.djfs().add(
                djfsUri(Integer.toString(6)),
                djfsResponse(Integer.toString(6)));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + uri + "110-dummy+(3).jpg")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + filesSuggest(
                            SuggestType.FILES,
                            "<span class=\\\"disk-search-highlight\\\">110"
                            + "</span>-<span class=\\\"disk-search-highlight"
                            + "\\\">dummy</span> (<span class=\\\"disk-search-"
                            + "highlight\\\">3</span>).<span class="
                            + "\\\"disk-search-highlight\\\">jpg</span>",
                            braces,
                            6,
                            DISK + folder + '/' + braces,
                            null)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test locale sensitive request
            cluster.djfs().add(
                djfsUri(Integer.toString(7)),
                djfsResponse(Integer.toString(7)));
            // No locale specified, so lowercase won't work as expected and
            // nothing will be found
            String cucumberUpper = "HIYAR";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + cucumberUpper)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(SUGGEST_PREFIX + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }

            // With proper locale 'I' will be converted to dotless 'i'
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + uri + "HIYAR&locale=tr")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + filesSuggest(
                            SuggestType.FILES,
                            "<span class=\\\"disk-search-highlight\\\">Hıyar"
                            + "</span>.jpg",
                            cucumber,
                            7,
                            DISK + folder + '/' + cucumber,
                            IMAGE_JPEG)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test that this file cat be found
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/?kps=0&visible=1&only=id&text=HIYAR")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SearchHandlerTest.serp(cucumberUpper)),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/?kps=0&visible=1&only=id&text=HIYAR&locale=tr")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SearchHandlerTest.serp(
                            cucumberUpper,
                            SearchHandlerTest.doc("name", 7))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    // CSON: MethodLength
    // CSON: MagicNumber
}

