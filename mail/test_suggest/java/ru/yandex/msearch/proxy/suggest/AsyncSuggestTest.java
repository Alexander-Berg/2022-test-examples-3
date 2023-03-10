package ru.yandex.msearch.proxy.suggest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.function.StringVoidProcessor;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.dom.ValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.xpath.ValueUtils;
import ru.yandex.msearch.proxy.AsyncHttpServer;
import ru.yandex.msearch.proxy.MsearchProxyCluster;
import ru.yandex.msearch.proxy.MsearchProxyTestBase;
import ru.yandex.msearch.proxy.MsearchProxyCluster.MproxyClusterContext;
import ru.yandex.msearch.proxy.api.async.mail.Side;
import ru.yandex.msearch.proxy.api.async.mail.relevance.search.MailSearchRelevanceConfigBuilder;
import ru.yandex.msearch.proxy.api.async.suggest.SuggestRequestText;
import ru.yandex.msearch.proxy.api.async.suggest.history.StoredRequestFields;
import ru.yandex.msearch.proxy.api.async.suggest.history.StoredRequestFields.Join;
import ru.yandex.msearch.proxy.api.async.suggest.history.StoredRequestFields.StorableRequestTextField;
import ru.yandex.msearch.proxy.config.MsearchProxyConfigBuilder;
import ru.yandex.msearch.proxy.config.RankingConfigBuilder;
import ru.yandex.msearch.proxy.config.RelevanceConfig;
import ru.yandex.msearch.proxy.suggest.utils.MailUser;
import ru.yandex.msearch.proxy.suggest.utils.SuggestTestUtil;
import ru.yandex.msearch.proxy.suggest.utils.SuggestTestUtil.Email;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.uri.PctEncoder;
import ru.yandex.parser.uri.PctEncodingRule;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

/**
 * Testing request store rule
 *
 */
public class AsyncSuggestTest extends MsearchProxyTestBase {
    private final SuggestTestUtil contactsUtil =
        new SuggestTestUtil(SuggestContactsTest.CONTACT_NEW_API);

    protected static String suggestRequest(
        final MsearchProxyCluster cluster,
        final String route,
        final String params) throws Exception
    {
        String uri = cluster.proxy().host()
            + route
            + params;

        StringVoidProcessor<char[], CharacterCodingException> encoder =
            new StringVoidProcessor<>(new PctEncoder(PctEncodingRule.FRAGMENT));
        encoder.process(uri);
        return encoder.toString();
    }

    protected static List<Map<String, String>> suggestsParamMap(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String route,
        final String params)
        throws Exception
    {
        try (CloseableHttpResponse response = client.execute(
            new HttpGet(suggestRequest(cluster, route, params))))
        {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            String responseTxt = CharsetUtils.toString(response.getEntity());

            JsonObject respObject =
                TypesafeValueContentHandler.parse(responseTxt);
            List<Map<String, String>> result = new ArrayList<>();
            JsonList suggestList;
            if (respObject.type() == JsonObject.Type.MAP) {
                suggestList = respObject.asMap().getList("documents");
            } else {
                suggestList = respObject.asList();
            }

            for (JsonObject sObj: suggestList) {
                Map<String, String> suggest = new LinkedHashMap<>();
                for (Map.Entry<String, JsonObject> entry
                    : sObj.asMap().entrySet())
                {
                    String value;
                    if (entry.getValue() instanceof JsonMap) {
                        value = JsonType.NORMAL.toString(entry.getValue());
                    } else {
                        value = entry.getValue().asString();
                    }

                    suggest.put(entry.getKey(), value);
                }

                result.add(suggest);
            }

            return result;
        }
    }

    protected static List<String> suggestsParam(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String route,
        final String params)
        throws Exception
    {
        List<String> result = new ArrayList<>();
        for (Map<String, String> sMap
            : suggestsParamMap(cluster, client, route, params))
        {
            result.add(sMap.get("orig_text"));
        }

        return result;
    }

    public static List<String> suggests(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String route,
        final String request)
        throws Exception
    {
        String requestParam = "mdb=mdb200&suid=0&limit=10";
        if (request != null) {
            requestParam = requestParam + "&request=" + request;
        }

        return suggestsParam(cluster, client, route, requestParam);
    }


    @Test
    public void testNormalization() throws Exception {
        String[] parts = SuggestRequestText.buildParts(
            " ==??NonNormalized * +crude#\t ??line\n W   end-?? ??????@??????==  ");
        String spaceless = StoredRequestFields.SPACELESS.processForStore(
            Arrays.asList(parts));

        Assert.assertEquals("??nonnormalizedcrude??linewend????????@??????", spaceless);
    }

    @Test
    public void testUtils() throws Exception {
        Join join = new Join(Join.ESCAPED_SPACE);
        String result = join.apply(Arrays.asList("test", "string"));
        Assert.assertEquals("test\\ string", result);

        StorableRequestTextField field =
            new StorableRequestTextField("test_field", join, join);
        StringBuilder storeValue = field.processForStore(
            new StringBuilder(),
            Arrays.asList("test", "string"));
        Assert.assertEquals("test\\ string", storeValue.toString());
    }

    @Test
    public void testCategorySuggest() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");


            String request = "/api/async/mail/suggest?mdb=pg&uid=0&request=";

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(request + "")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(request + "pe")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"target\": \"category\", " +
                            "\"show_text\": \"people\"," +
                            "\"search_params\":{}," +
                            "\"search_text\": \"????????????:people\"" +
                            "}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            QueryConstructor qc =
                new QueryConstructor("/api/async/mail/suggest?mdb=pg&uid=0");
            qc.append("request", "  asdf pe");

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"target\": \"category\", " +
                            "\"show_text\": \"people\"," +
                            "\"search_params\":{}," +
                            "\"search_text\": \"  asdf ????????????:people\"" +
                            "}]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSuggestLabels() throws Exception {
        String suggestRoute = "/api/async/mail/suggest/label";

        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.filterSearch().add(
                "/labels?caller=msearch&mdb=pg&uid=0",
                "{\"labels\":{\"5\":{\"type\":{\"title\":\"user\"},"
                    + "\"name\":\"Jira\"},"
                    + "\"9\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"draft\"}},"
                    + "\"8\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"important_label\"},"
                    + "\"name\":\"priority_high\"},"
                    + "\"102\":{\"name\":\"????????????\","
                    + "\"creationTime\":\"1484658495\","
                    + "\"color\":\"3262267\",\"isUser\":true,"
                    + "\"isSystem\":false,"
                    + "\"type\":{\"code\":1,\"title\":\"user\"}},"
                    + "\"802\":{\"name\":\"?????????? ?????? vonid\","
                    + "\"creationTime\":\"1484658495\","
                    + "\"color\":\"3262267\",\"isUser\":true,"
                    + "\"isSystem\":false,"
                    + "\"type\":{\"code\":1,\"title\":\"user\"}},"
                    + "\"103\":{\"name\":\"????????????\","
                    + "\"creationTime\":\"1484658528\",\"color\":\"8176580\","
                    + "\"isUser\":true,\"isSystem\":false,\"type\":{\"code\":1,"
                    + "\"title\":\"user\"},\"symbolicName\":{\"code\":0,"
                    + "\"title\":\"\"},\"messagesCount\":1}"
                    + "}}");

            //test not suggesting labels on empty request
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?uid=0&mdb=pg&request=")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?uid=0&mdb=pg&request=??")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"label\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"102\", "
                            + "\"search_text\": \"??????????:????????????\"},"
                            + "{\"target\": \"label\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"103\", "
                            + "\"search_text\": \"??????????:????????????\"},"
                            + "{\"target\": \"label\", "
                            + "\"show_text\": \"Jira\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"5\", "
                            + "\"search_text\": \"??????????:Jira\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?uid=0&mdb=pg&request=j")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"label\", "
                            + "\"show_text\": \"Jira\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"5\", "
                            + "\"search_text\": \"??????????:Jira\"},"
                            + "{\"target\": \"label\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"102\", "
                            + "\"search_text\": \"??????????:????????????\"},"
                            + "{\"target\": \"label\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"103\", "
                            + "\"search_text\": \"??????????:????????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }


            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?uid=0&mdb=pg&request=??")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"important\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"lid\": \"8\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:????????????\"},"
                            + "{\"lid\":\"802\", \"search_text\":"
                            + "\"??????????:?????????? ?????? vonid\", "
                            + "\"search_params\":{},"
                            + "\"show_text\":\"?????????? ?????? vonid\", "
                            + "\"target\":\"label\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?uid=0&mdb=pg&request=fl&lang=en")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"important\", "
                            + "\"show_text\": \"Flagged\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"8\", "
                            + "\"search_text\": \"label:Flagged\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?uid=0&mdb=pg&request=un&lang=jx")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"unread\", "
                            + "\"show_text\": \"Unread\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"FAKE_SEEN_LBL\", "
                            + "\"search_text\": \"label:Unread\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            //test multiword

            QueryConstructor qc =
                new QueryConstructor(suggestRoute + "?uid=0&mdb=pg");
            qc.append("request", "  ??????????????\t  ??");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"important\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"8\", "
                            + "\"search_text\": \"  ??????????????\t  ??????????:????????????\"},"
                            + "{\"lid\":\"802\", \"search_text\":"
                            + "\"  ??????????????\t  ??????????:?????????? ?????? vonid\", "
                            + "\"show_text\":\"?????????? ?????? vonid\", "
                            + "\"search_params\":{},"
                            + "\"target\":\"label\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(suggestRoute + "?uid=0&mdb=pg");
            qc.append("request", "  ");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(suggestRoute + "?uid=0&mdb=pg");
            qc.append("request", "vonid");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"label\", "
                            + "\"show_text\": \"?????????? ?????? vonid\", "
                            + "\"search_params\":{},"
                            + "\"lid\": \"802\", "
                            + "\"search_text\": \"??????????:?????????? ?????? vonid\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(suggestRoute + "?uid=0&mdb=pg&highlight");
            qc.append("request", "?????? vonid");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\":\"label\", "
                            + "\"show_text\":\"?????????? ?????? vonid\", "
                            + "\"search_params\":{},"
                            + "\"show_text_highlighted\":\"?????????? <span "
                            + "class=\\\"msearch-highlight\\\">"
                            + "?????? vonid</span>\","
                            + "\"lid\": \"802\", "
                            + "\"search_text\": \"??????????:?????????? ?????? vonid\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testLocalization() throws Exception {
        String suggestRoute =
            "/api/async/mail/suggest?&mdb=pg&uid=0&suid=1&request=";

        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=0&suid=1",
                "{\"folders\":{" + systemFolders() + "}}");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host() + suggestRoute + "????&lang=kk")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"??????????\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"4\","
                            + "\"search_text\": \"folder:??????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host() + suggestRoute + "????&lang=uk")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"1\","
                            + "\"search_text\": \"folder:????????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "i&lang=en")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"Inbox\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"1\","
                            + "\"search_text\": \"folder:Inbox\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute
                        + "????????&lang=ru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"????????????????????????\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"4\","
                            + "\"search_text\": \"??????????:????????????????????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "i&lang=ss")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"search_params\":{},"
                            + "\"show_text\": \"Inbox\", "
                            + "\"fid\": \"1\","
                            + "\"search_text\": \"folder:Inbox\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?????????&lang=ka")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"????????????????????????????????????\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"4\","
                            + "\"search_text\": \"folder:????????????????????????????????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            cluster.backend().add(
                "\"url\": \"reqs_359689700_???????????? ????????????\","
                    + "\"request_raw\": \"???????????? ????????????\","
                    + "\"request_date\": \"1485527972\","
                    + "\"request_normalized\": \"???????????? ????????????\","
                    + "\"request_spaceless\": \"????????????????????????\"");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/suggest?"
                        + "&mdb=pg&uid=0&suid=1"
                        + "&request=%D5%84%D5%B8%D6%82%D5%BF&lang=hy")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"??????????????????\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"1\","
                            + "\"search_text\": "
                            + "\"folder:??????????????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/suggest?"
                        + "&mdb=pg&uid=0&suid=1"
                        + "&request=%D0%BA%D0%B8&lang=tt")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"1\","
                            + "\"search_text\": "
                            + "\"folder:????????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/suggest?"
                        + "&mdb=pg&uid=0&suid=1"
                        + "&request=pri&lang=ro")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"Mesaje primite\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"1\","
                            + "\"search_text\": "
                            + "\"folder:\\\"Mesaje primite\\\"\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/suggest?"
                        + "&mdb=pg&uid=0&suid=1"
                        + "&request=G%C3%B6&lang=az")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"G??nd??ril??nl??r\", "
                            + "\"fid\": \"4\","
                            + "\"search_params\":{},"
                            + "\"search_text\": "
                            + "\"folder:G??nd??ril??nl??r\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSuggestFoldersTree() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            // Beware nbsp in meta response!
            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=0",
                "{\"folders\":{\"186\":{\"name\":\"Inbox|White Cat|Child\","
                    + "       \"type\" : {\n"
                    + "            \"title\" : \"user\",\n"
                    + "            \"code\" : 1\n"
                    + "         },"
                    + "\"parentId\": \"187\"},"
                    + "\"18\":{\"name\":\"Inbox|Folder??One|Folder??Tw\","
                    + "\"isUser\":true,\"isSystem\":false,\"type\":{\"code\":1,"
                    + "\"title\":\"user\"},\"symbolicName\":{\"code\":0,"
                    + "\"title\":\"\"},\"bytes\":0,\"messagesCount\":0,"
                    + "\"newMessagesCount\":0,\"recentMessagesCount\":0,"
                    + "\"unvisited\":false,"
                    + "\"folderOptions\":{\"getPosition\":0},\"position\":0,"
                    + "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\","
                    + "\"creationTime\":\"1503068321\",\"subscribed\":\"\","
                    + "\"shared\":\"0\"},"
                    +  "\"187\":{\"name\":\"Inbox|White Cat\","
                    + "       \"type\" : {\n"
                    + "            \"title\" : \"user\",\n"
                    + "            \"code\" : 1\n"
                    + "         },"
                    + "\"parentId\" : \"0\"}}}");


            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/suggest/folder"
                        + "?uid=0&mdb=pg&request=White+Cat")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"fid\": \"187\","
                            + "\"search_params\":{},"
                            + "\"show_text\": \"White Cat\", "
                            + "\"search_text\":\"??????????:\\\"White Cat\\\"\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }


            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/suggest/folder"
                        + "?uid=0&mdb=pg&request=Chi")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"fid\": \"186\","
                            + "\"search_params\":{},"
                            + "\"show_text\": \"White Cat ??? Child\", "
                            + "\"search_text\":\"??????????:Child\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            String suggestRoute = "/api/async/mail/suggest/folder";
            QueryConstructor qc =
                new QueryConstructor(suggestRoute + "?uid=0&mdb=pg");
            qc.append("request", "tw");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"fid\": \"18\","
                            + "\"search_params\":{},"
                            + "\"show_text\": \"Folder??Tw\", "
                            + "\"search_text\":\"??????????:\\\"Folder??Tw\\\"\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSuggestFolders() throws Exception {
        String suggestRoute = "/api/async/mail/suggest/folder";

        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            // Beware nbsp in meta response!
            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=0",
               "{\"folders\":{\"1\":{\"name\":\"Inbox\",\"isUser\":false," +
                   "\"isSystem\":true,\"type\":{\"code\":3," +
                   "\"title\":\"system\"},\"symbolicName\":{\"code\":1," +
                   "\"title\":\"inbox\"},\"bytes\":12720961," +
                   "\"messagesCount\":161,\"newMessagesCount\":70," +
                   "\"recentMessagesCount\":152,\"unvisited\":false," +
                   "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
                   "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
                   "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
                   "\"shared\":\"0\"},\"2\":{\"name\":\"Spam\"," +
                   "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                   "\"title\":\"system\"},\"symbolicName\":{\"code\":4," +
                   "\"title\":\"spam\"},\"bytes\":0,\"messagesCount\":0," +
                   "\"newMessagesCount\":0,\"recentMessagesCount\":0," +
                   "\"unvisited\":false," +
                   "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
                   "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
                   "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
                   "\"shared\":\"0\"},\"3\":{\"name\":\"Trash\"," +
                   "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                   "\"title\":\"system\"},\"symbolicName\":{\"code\":3," +
                   "\"title\":\"trash\"},\"bytes\":0,\"messagesCount\":0," +
                   "\"newMessagesCount\":0,\"recentMessagesCount\":0," +
                   "\"unvisited\":false," +
                   "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
                   "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
                   "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
                   "\"shared\":\"0\"}," +
                   "  \"777\": {\n" +
                   "    \"shared\": \"0\",\n" +
                   "    \"subscribed\": \"\",\n" +
                   "    \"newMessagesCount\": 3,\n" +
                   "    \"messagesCount\": 22,\n" +
                   "    \"bytes\": 99901,\n" +
                   "    \"symbolicName\": {\n" +
                   "      \"title\": \"\",\n" +
                   "      \"code\": 0\n" +
                   "    },\n" +
                   "    \"type\": {\n" +
                   "      \"title\": \"user\",\n" +
                   "      \"code\": 1\n" +
                   "    },\n" +
                   "    \"isSystem\": false,\n" +
                   "    \"isUser\": true,\n" +
                   "    \"name\": \"????????????????????vonidu\",\n" +
                   "    \"recentMessagesCount\": 22,\n" +
                   "    \"unvisited\": false,\n" +
                   "    \"folderOptions\": {\n" +
                   "      \"getPosition\": 0\n" +
                   "    },\n" +
                   "    \"position\": 0,\n" +
                   "    \"parentId\": \"0\",\n" +
                   "    \"pop3On\": \"0\",\n" +
                   "    \"scn\": \"0\",\n" +
                   "    \"creationTime\": \"1474187704\"\n" +
                   "  }," +
                   "  \"119\": {\n" +
                   "    \"shared\": \"0\",\n" +
                   "    \"subscribed\": \"\",\n" +
                   "    \"newMessagesCount\": 0,\n" +
                   "    \"messagesCount\": 0,\n" +
                   "    \"bytes\": 0,\n" +
                   "    \"symbolicName\": {\n" +
                   "      \"title\": \"\",\n" +
                   "      \"code\": 0\n" +
                   "    },\n" +
                   "    \"type\": {\n" +
                   "      \"title\": \"user\",\n" +
                   "      \"code\": 1\n" +
                   "    },\n" +
                   "    \"isSystem\": false,\n" +
                   "    \"isUser\": true,\n" +
                   "    \"name\": \"????????????????????vonidu|?????????????? ??????????????\",\n" +
                   "    \"recentMessagesCount\": 0,\n" +
                   "    \"unvisited\": false,\n" +
                   "    \"folderOptions\": {\n" +
                   "      \"getPosition\": 0\n" +
                   "    },\n" +
                   "    \"position\": 0,\n" +
                   "    \"parentId\": \"777\",\n" +
                   "    \"pop3On\": \"0\",\n" +
                   "    \"scn\": \"0\",\n" +
                   "    \"creationTime\": \"1501882177\"\n" +
                   "  }," +
                   "\"4\":{\"name\":\"Sent\"," +
                   "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                   "\"title\":\"system\"},\"symbolicName\":{\"code\":2," +
                   "\"title\":\"sent\"},\"bytes\":24483112," +
                   "\"messagesCount\":18,\"newMessagesCount\":0," +
                   "\"recentMessagesCount\":18,\"unvisited\":false," +
                   "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
                   "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
                   "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
                   "\"shared\":\"0\"},\"5\":{\"name\":\"Outbox\"," +
                   "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                   "\"title\":\"system\"},\"symbolicName\":{\"code\":6," +
                   "\"title\":\"outbox\"},\"bytes\":0,\"messagesCount\":0," +
                   "\"newMessagesCount\":0,\"recentMessagesCount\":0," +
                   "\"unvisited\":false," +
                   "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
                   "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
                   "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
                   "\"shared\":\"0\"},\"6\":{\"name\":\"Drafts\"," +
                   "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                   "\"title\":\"system\"},\"symbolicName\":{\"code\":5," +
                   "\"title\":\"draft\"},\"bytes\":20994397," +
                   "\"messagesCount\":8,\"newMessagesCount\":0," +
                   "\"recentMessagesCo* Connection #0 to host meta.mail" +
                   ".yandex.net left intact\n" +
                   "unt\":8,\"unvisited\":false," +
                   "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
                   "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
                   "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
                   "\"shared\":\"0\"},\"7\":{\"name\":\"????????????\"," +
                   "\"isUser\":true,\"isSystem\":false,\"type\":{\"code\":1," +
                   "\"title\":\"user\"},\"symbolicName\":{\"code\":0," +
                   "\"title\":\"\"},\"bytes\":15562,\"messagesCount\":4," +
                   "\"newMessagesCount\":2,\"recentMessagesCount\":4," +
                   "\"unvisited\":false," +
                   "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
                   "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
                   "\"creationTime\":\"1474187704\",\"subscribed\":\"1\"," +
                   "\"shared\":\"0\"}}}");

            //test not suggesting labels on empty request
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?uid=0&mdb=pg&request=")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?uid=0&mdb=pg&request=??")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"fid\": \"7\","
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:????????????\"},"
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"????????????????????????\", "
                            + "\"fid\": \"4\","
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:????????????????????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            //translit
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + suggestRoute + "?uid=0&mdb=pg&request=j")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"????????????\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"7\","
                            + "\"search_text\": \"??????????:????????????\"},"
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"????????????????????????\", "
                            + "\"search_params\":{},"
                            + "\"fid\": \"4\","
                            + "\"search_text\": \"??????????:????????????????????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            //test multiword
            QueryConstructor qc =
                new QueryConstructor(suggestRoute + "?uid=0&mdb=pg");
            qc.append("request", "  ??????????????\t  ????");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"show_text\": \"????????????????\", "
                            + "\"fid\": \"1\","
                            + "\"search_params\":{},"
                            + "\"search_text\": "
                            + "\"  ??????????????\t  ??????????:????????????????\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(suggestRoute + "?uid=0&mdb=pg");
            qc.append("request", " \t ");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(suggestRoute + "?uid=0&mdb=pg");
            qc.append("request", "??????????");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"fid\": \"119\","
                            + "\"search_params\":{},"
                            + "\"show_text\": "
                            + "\"????????????????????vonidu ??? ?????????????? ??????????????\", "
                            + "\"search_text\":"
                            + "\"??????????:\\\"?????????????? ??????????????\\\"\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(suggestRoute + "?uid=0&mdb=pg&highlight");
            qc.append("request", "????????????");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String resp = CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"fid\": \"119\","
                            + "\"search_params\":{},"
                            + "\"show_text\": "
                            + "\"????????????????????vonidu "
                            + "??? ?????????????? ??????????????\", "
                            + "\"show_text_highlighted\": "
                            + "\"??????????&nbsp;??????&nbsp;vonidu "
                            + "<span class=\\\"msearch-folder"
                            + "-separator\\\">&rsaquo;</span> ?????????????? <span " +
                            "class=\\\"msearch-highlight\\\">????????????</span>??\", "
                            + "\"search_text\":"
                            + "\"??????????:\\\"?????????????? ??????????????\\\"\"}"
                            + "]"),
                    resp);
            }

            qc = new QueryConstructor(suggestRoute + "?uid=0&mdb=pg&highlight");
            qc.append("request", "vonid");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"fid\": \"777\","
                            + "\"search_params\":{},"
                            + "\"show_text\": "
                            + "\"????????????????????vonidu\", "
                            + "\"show_text_highlighted\": "
                            + "\"??????????&nbsp;??????&nbsp;<span " +
                            "class=\\\"msearch-highlight\\\">vonid</span>u\","
                            + "\"search_text\":"
                            + "\"??????????:\\\"????????????????????vonidu\\\"\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(suggestRoute + "?uid=0&mdb=pg&highlight");
            qc.append("request", "?????? vonid");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"folder\", "
                            + "\"fid\": \"777\","
                            + "\"search_params\":{},"
                            + "\"show_text\": "
                            + "\"????????????????????vonidu\", "
                            + "\"show_text_highlighted\": "
                            + "\"??????????&nbsp;<span " +
                            "class=\\\"msearch-highlight\\\">??????&nbsp;vonid</span>u\","
                            + "\"search_text\":"
                            + "\"??????????:\\\"????????????????????vonidu\\\"\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSuggestQueryField() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            String suggestRoute =
                cluster.proxy().host() + "/api/async/mail/suggest?type=ql&";

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(suggestRoute + "uid=0&mdb=pg&request=f")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"filename:\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"filename:\"},"
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"filter:\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"filter:\"},"
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"folder:\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"folder:\"},"
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"from:\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"from:\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(suggestRoute + "uid=0&mdb=pg&request=????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"ql\", "
                            + "\"search_params\":{},"
                            + "\"show_text\": \"????????:\", "
                            + "\"search_text\": \"????????:\"},"
                            + "{\"target\": \"ql\", "
                            + "\"search_params\":{},"
                            + "\"show_text\": \"??????????:\", "
                            + "\"search_text\": \"??????????:\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMailSuggest() throws Exception {
        //test relevance
        String model1 =
            "    static unsigned short GeneratedCompactIndicesTbl[] = {\n"
                + "        0,7,7,7,7,6\n"
                + " };\n"
                + "    static int GeneratedDataTbl[] = {\n"
                + "        0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,\n"
                + "        20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,\n"
                +"         37,38,39,40\n"
                + " };\n"
                + "i64 resInt = 0;\n"
                + "{\n"
                + "    const int *fFactorInt = reinterpret_cast<const int*>(fFactor);\n"
                + "    bool vars[8];\n"
                + "    vars[0] = fFactorInt[0] > 1059817308 ? 1 : 0; // 5.0\n"
                + "    vars[1] = fFactorInt[1] > 1076576646 ? 1 : 0; // 10000.0\n"
                + "    vars[2] = fFactorInt[2] > 1079397180 ? 1 : 0; // 10000.0\n"
                + "    vars[3] = fFactorInt[3] > 1059817308 ? 1 : 0; // 10000.0\n"
                + "    vars[4] = fFactorInt[4] > 1076576646 ? 1 : 0; // 10000.0\n"
                + "    vars[5] = fFactorInt[5] > 1079397180 ? 1 : 0; // 10000.0\n"
                + "    vars[6] = fFactorInt[6] > 1076576646 ? 1 : 0; // 0.9\n"
                + "    vars[7] = fFactorInt[7] > 1079397180 ? 1 : 0; // 0.9\n"
                + "    for (int z = 0; z < 1; ++z) {\n"
                + "        ui32 i0 = (reinterpret_cast<const ui32*>(indices))[0];\n"
                + "        ui32 i1 = (reinterpret_cast<const ui32*>(indices))[1];\n"
                + "        ui32 i2 = (reinterpret_cast<const ui32*>(indices))[2];\n"
                + "        int idx = vars[i2 >> 16];\n" +
                "        idx = idx * 2 + vars[i2 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i1 >> 16];\n" +
                "        idx = idx * 2 + vars[i1 & 0xffff];\n" +
                "        idx = idx * 2 + vars[i0 >> 16];\n" +
                "        idx = idx * 2 + vars[i0 & 0xffff];\n" +
                "        resInt += data[idx];\n" +
                "        indices += 6;\n" +
                "        data += 64;\n" +
                "    }\n" +
                "}\n" +
                "double res = 0.0 + resInt * 1;\n";
        List<String> factors = Arrays.asList(
            ("fid,mtype_freq,hdr_bcc,request_email,hdr_cc,req_in_subj,"
                + "lcn_score,hdr_to,pure_body,serp_clicks,hdr_subject,"
                + "hdr_from,mtype,reply_to,total_clicks,to_email_group,"
                + "from_email_group,age,weekday,daytime,serp_clicks_n"
                + ",total_clicks_n,age_p,was_rel").split(","));

        ByteArrayInputStream bis =
            new ByteArrayInputStream(model1.getBytes(Charset.defaultCharset()));

        MailSearchRelevanceConfigBuilder curRankingConfig
            = new MailSearchRelevanceConfigBuilder();

        curRankingConfig.usageStatus(
            RelevanceConfig.RelevanceUsageStatus.DEFAULT);
        curRankingConfig.factors(new LinkedHashSet<>(factors));
        curRankingConfig.sides(Collections.singleton(Side.WEB));
        curRankingConfig.content(bis);
        curRankingConfig.name("model1");

        RankingConfigBuilder ranking = new RankingConfigBuilder();
        ranking.mailSearch(Collections.singletonList(curRankingConfig));

        MproxyClusterContext context = new MproxyClusterContext();
        context.producer(true);
        context.matrixnet(ranking);

        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, context);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.backend().add(
                new LongPrefix(10),
                doc(
                    "200500",
                        "\"fid\": \"1\",\n"
                        + "\"attachname\": \"Booking_60491410.PDF\",\n"
                        + "\"disposition_type\": \"attachment\",\n"
                        + "\"hdr_subject\": \"?????????????????????????? ?????????? 60491410\",\n"
                        + "\"hdr_from\": \"\\\"Tallink & Silja Line "
                        + "Reservations\\\" <noreply@tallink.com>\\n\",\n"
                        + "\"received_date\": \"1553151148\",\n"
                        + "\"folder_type\": \"inbox\",\n"
                        + "\"message_type\": \"64 transact 19 "
                        + "s_travel 5 eticket 102 t_notification 55 trust_5 46 "
                        + "firstmail\",\n"
                        + "\"has_attachments\": \"true\",\n"
                        + "\"clicks_serp_count\": 0,\n"
                        + "\"clicks_total_count\": 0\n",
                    "\"hid\": \"0\","
                        + "\"mimetype\": \"text/html\", "
                        + "\"body_text\": \"Firstline\"",
                    "\"hid\": \"1.2\", "
                        + "\"mimetype\": \"image/png\", "
                        + "\"body_text\": \"Ristorante Italiano\""));

            cluster.blackbox().add(
                MsearchProxyCluster.blackboxUri(10),
                MsearchProxyCluster.blackboxResponse(10, 10, "pg"));

            cluster.backend().add(
                doc(
                    "100500",
                    "\"thread_id\":100500," +
                        "\"hdr_from\":\"\\\"Irina Valter\\\" " +
                        "<irina.valter@yandex.ru>\\n\"," +
                        "\"received_date\":\"1234567890\"," +
                        "\"has_attachments\":true," +
                        "\"folder_type\": \"sent\"," +
                        "\"fid\":4," +
                        "\"hdr_subject\":" +
                        "\"??????????: Linnen Cotton Pillow Case\"",
                    ""),
                doc(
                    "100502",
                    "\"thread_id\":100500," +
                        "\"hdr_from\":\"\\\"Irina Valter\\\" " +
                        "<irina.valter@yandex.ru>\\n\"," +
                        "\"received_date\":\"1234567890\"," +
                        "\"fid\":10," +
                        "\"hdr_subject\":" +
                        "\"??????????: Linnen Cotton Pillow Case ??????????????\"",
                    ""),
                doc(
                    "100501",
                    "\"thread_id\":100501," +
                        "\"received_date\":\"1234567891\"," +
                        "\"hdr_from\":\"\\\"????????\\\" <sasha@yandex.ru>\\n\"," +
                        "\"hdr_subject_normalized\":\"??????????\"," +
                        "\"folder_type\": \"trash\"," +
                        "\"fid\":3," +
                        "\"hdr_subject\":\"" +
                        "?????? ?????????? ?? ?????????? ???????????????? ?????????? ???? ??????????\"",
                    ""),
                doc(
                    "100503",
                    "\"thread_id\":100503," +
                        "\"received_date\":\"1234567891\"," +
                        "\"hdr_from\":\"\\\"????????\\\" <sasha@yandex.ru>\\n\"," +
                        "\"hdr_subject_normalized\":\"??????????\"," +
                        "\"folder_type\": \"spam\"," +
                        "\"fid\":2," +
                        "\"hdr_subject\":\"" +
                        "?????? ?????????? ?? ?????????? ???????????????? ?????????? ???? ?????????? ?????? ??????\"",
                    ""),
                doc(
                    "100504",
                    "\"thread_id\":100504," +
                        "\"received_date\":\"1234567891\"," +
                        "\"hdr_from\":\"\\\"????????\\\" <sasha@yandex.ru>\\n\"," +
                        "\"hdr_subject_normalized\":\"??????????\"," +
                        "\"folder_type\": \"inbox\"," +
                        "\"fid\":1," +
                        "\"hdr_subject\":\"" +
                        "?????? ?????????? ?? ?????????? ???????????????? ?????????? ???? ?????????? ?????? ????\"",
                    ""));

            cluster.producer().add(
                "/_status?service=change_log"
                    + "&prefix=0&allow_cached&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.producer().add(
                "/_status?service=change_log"
                    + "&prefix=10&allow_cached&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.proxy().host()
                                 + "/api/async/mail/suggest?uid=10&mdb=pg"
                                 + "&type=mail"
                                 + "&side=web&request=Ristorante")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"display_name\":"
                            + "\"Tallink & Silja Line Reservations\",\n"
                            + "\"email\": \"noreply@tallink.com\",\n"
                            + "\"fid\": \"1\",\n"
                            + "\"target\": \"mail\",\n"
                            + "\"has_attachments\": true,\n"
                            + "\"mid\": \"200500\",\n"
                            + "\"received_date\": \"1553151148\",\n"
                            + "\"attachments\": [{"
                                + "\"disposition_filename\":"
                                + "\"Booking_60491410.PDF\","
                                + "\"disposition_value\":\"attachment\",\n"
                                + "\"fileext\": \".pdf\",\n"
                                + "\"filename\": \"Booking_60491410.PDF\",\n"
                                + "\"hid\": \"1.2\",\n"
                                + "\"subtype\": \"png\",\n"
                                + "\"type\": \"image\"}],"
                            + "\"scopes\": [\"attachment\"],\n"
                            + "\"search_params\": {},\n"
                            + "\"search_text\": \"mid:200500\",\n"
                            + "\"show_text\":"
                            + "\"?????????????????????????? ?????????? 60491410\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(
                    cluster.backend().indexerUri()
                        + "/delete?prefix=10&text=mid_p:200500"));

            cluster.backend().add(
                new LongPrefix(10),
                doc(
                    "200501",
                    "\"fid\": \"1\",\n"
                        + "\"hdr_subject\": \"?????????????????????????? ?????????? 60491410\",\n"
                        + "\"hdr_from\": \"\\\"Tallink & Silja Line "
                        + "Reservations\\\" <noreply@tallink.com>\\n\",\n"
                        + "\"received_date\": \"1553151148\",\n"
                        + "\"folder_type\": \"inbox\",\n"
                        + "\"message_type\": \"64 transact 19 "
                        + "s_travel 5 eticket 102 t_notification 55 trust_5 46 "
                        + "firstmail\",\n"
                        + "\"has_attachments\": \"true\",\n"
                        + "\"clicks_serp_count\": 0,\n"
                        + "\"clicks_total_count\": 0\n",
                    "\"hid\": \"0\","
                        + "\"mimetype\": \"text/html\", "
                        + "\"body_text\": \"Firstline\"",
                    "\"hid\": \"1.2\", "
                        + "\"mimetype\": \"application/doc\", "
                        + "\"attachname\": \"italiano.doc\",\n"
                        + "\"disposition_type\": \"attachment\",\n"
                        + "\"body_text\": \"Ristorante Italiano\"",
                    "\"hid\": \"1.3\", "
                        + "\"mimetype\": \"application/pdf\", "
                        + "\"body_text\": \"Ristorante Italiano\","
                        + "\"attachname\": \"Booking_60491410.PDF\",\n"
                        + "\"disposition_type\": \"attachment\"\n"
                    ));

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.proxy().host()
                                 + "/api/async/mail/suggest?uid=10&mdb=pg"
                                 + "&type=mail"
                                 + "&side=web&request=Ristorante")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"display_name\":"
                            + "\"Tallink & Silja Line Reservations\",\n"
                            + "\"email\": \"noreply@tallink.com\",\n"
                            + "\"fid\": \"1\",\n"
                            + "\"target\": \"mail\",\n"
                            + "\"has_attachments\": true,\n"
                            + "\"mid\": \"200501\",\n"
                            + "\"received_date\": \"1553151148\",\n"
                            + "\"attachments\": [{"
                            + "\"disposition_filename\":\"italiano.doc\","
                            + "\"disposition_value\":\"attachment\",\n"
                            + "\"fileext\": \".doc\",\n"
                            + "\"filename\": \"italiano.doc\",\n"
                            + "\"hid\": \"1.2\",\n"
                            + "\"subtype\": \"doc\",\n"
                            + "\"type\": \"application\"}, {"
                            + "\"disposition_filename\":"
                            + "\"Booking_60491410.PDF\","
                            + "\"disposition_value\":\"attachment\",\n"
                            + "\"fileext\": \".pdf\",\n"
                            + "\"filename\": \"Booking_60491410.PDF\",\n"
                            + "\"hid\": \"1.3\",\n"
                            + "\"subtype\": \"pdf\",\n"
                            + "\"type\": \"application\"}],"
                            + "\"scopes\": [\"attachment\"],\n"
                            + "\"search_params\": {},\n"
                            + "\"search_text\": \"mid:200501\",\n"
                            + "\"show_text\":"
                            + "\"?????????????????????????? ?????????? 60491410\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            String baseRequest =
                cluster.proxy().host()
                + "/api/async/mail/suggest?suid=0&mdb=mdb200&type=mail"
                    + "&side=web&request=";

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(baseRequest + "??????????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"mail\", "
                            + "\"received_date\":\"1234567891\","
                            + "\"email\":\"sasha@yandex.ru\","
                            + "\"display_name\": \"????????\","
                            + "\"has_attachments\": false,"
                            + "\"search_params\":{},"
                            + "\"mid\": \"100504\","
                            + "\"fid\": \"1\","
                            + "\"scopes\":[\"subject\"],"
                            + "\"show_text\": \""
                            + "?????? ?????????? ?? ?????????? ???????????????? ?????????? ???? ?????????? ?????? ????\", "
                            + "\"search_text\": \"mid:100504\"},"
                            + "{\"target\": \"mail\", "
                            + "\"received_date\":\"1234567890\","
                            + "\"email\":\"irina.valter@yandex.ru\","
                            + "\"display_name\": \"Irina Valter\","
                            + "\"has_attachments\": true,"
                            + "\"search_params\":{},"
                            + "\"fid\": \"4\","
                            + "\"scopes\":[\"subject\"],"
                            + "\"mid\": \"100500\","
                            + "\"show_text\": \""
                            + "??????????: Linnen Cotton Pillow Case\", "
                            + "\"search_text\": \"mid:100500\"}"
                            + "]"),
                    responseStr);
            }

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(baseRequest + "??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(baseRequest + "Case%20cotton")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"mail\", "
                            + "\"received_date\":\"1234567890\","
                            + "\"email\":\"irina.valter@yandex.ru\","
                            + "\"display_name\": \"Irina Valter\","
                            + "\"has_attachments\": true,"
                            + "\"search_params\":{},"
                            + "\"fid\": \"4\","
                            + "\"mid\": \"100500\","
                            + "\"scopes\":[\"body\"],"
                            + "\"show_text\": \""
                            + "??????????: Linnen Cotton Pillow Case\", "
                            + "\"search_text\": \"mid:100500\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(baseRequest + "Irina&scope=hdr_subject")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(baseRequest + "Irina&scope=hdr_from")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"mail\", "
                            + "\"received_date\":\"1234567890\","
                            + "\"email\":\"irina.valter@yandex.ru\","
                            + "\"display_name\": \"Irina Valter\","
                            + "\"has_attachments\": true,"
                            + "\"search_params\":{},"
                            + "\"mid\": \"100500\","
                            + "\"fid\": \"4\","
                            + "\"scopes\":[\"body\"],"
                            + "\"show_text\": \""
                            + "??????????: Linnen Cotton Pillow Case\", "
                            + "\"search_text\": \"mid:100500\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSuggestQueryFieldValue() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2"
                + "&emails=getall&sid=2&uid=0",
                MsearchProxyCluster.blackboxResponse(0, 0, "pg"));

            //folder
            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=0",
                "{\"folders\":{"
                    + "\"7\":{\"name\":\"trips\","
                    + "\"type\":{\"title\":\"user\"},"
                    + "\"isUser\":true,\"isSystem\":false}}}");

            //label
            cluster.filterSearch().add(
                "/labels?caller=msearch&mdb=pg&uid=0",
                "{\"labels\":{\"5\":{\"type\":{\"title\":\"user\"},"
                    + "\"name\":\"Trip\"}}}");

            //subject
            cluster.backend().add(
                doc(
                    "100500",
                    "\"thread_id\":100500," +
                        "\"received_date\":\"1234567892\"," +
                        "\"hdr_subject_normalized\":\"?? ?????? Trips ????????????\"",
                ""));

            //contact
            MailUser user =
                new MailUser(0, "pg", "united-suggest@yandex.ru");

            contactsUtil.indexDoc(
                cluster,
                user,
                new Email("100501")
                    .from("???????? ??????????", "ivan@yandex.ru")
                    .to("other", user.email()));

            contactsUtil.indexDoc(
                cluster,
                user,
                new Email("100502")
                    .from("???????? ??????????", user.email())
                    .to("???????????? ??????????", "trips@yandex.ru"));

            //history
            cluster.backend().add(
                "\"url\":\"reqs_0_trips\","
                    + "\"request_raw\":\"trips\","
                    + "\"request_normalized\":\"trips\","
                    + "\"request_spaceless\":\"trips\","
                    + "\"request_date\":\"1476796939\"," +
                    "\"request_count\":\"1\"");

            String suggestRoute =
                cluster.proxy().host() + "/api/async/mail/suggest?uid=0&mdb=pg&hr";

            QueryConstructor qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", "????: ivan@yandex.ru ??????");

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseTxt =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"????????:\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"????: ivan@yandex.ru ????????:\"}]"),
                    responseTxt);
            }

            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", "????:tr");

            List<Map<?, ?>> suggestsList = new ArrayList<>();

            Map<?, ?> suggest;
            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseTxt = CharsetUtils.toString(response.getEntity());
                Object responseObj = ValueContentHandler.parse(responseTxt);

                List<?> suggestList = ValueUtils.asList(responseObj);
                for (Object suggestObj: suggestList) {
                    suggest = ValueUtils.asMap(suggestObj);
                    suggestsList.add(suggest);
                }
            }

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("target")), "contact");
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("search_text")),
                "????:ivan@yandex.ru");
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("show_text")),
                "\"???????? ??????????\" ivan@yandex.ru"
            );

            //test space after colon
            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", "????????: trip");

            suggestsList = new ArrayList<>();

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseTxt = CharsetUtils.toString(response.getEntity());
                Object responseObj = ValueContentHandler.parse(responseTxt);

                List<?> suggestList = ValueUtils.asList(responseObj);
                for (Object suggestObj: suggestList) {
                    suggest = ValueUtils.asMap(suggestObj);
                    suggestsList.add(suggest);
                }
            }

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("target")), "contact");
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("search_text")),
                "????????:trips@yandex.ru");
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("show_text")),
                "\"???????????? ??????????\" trips@yandex.ru"
            );

            //folder
            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", " ????????\t ??????????: tr");

            suggestsList = new ArrayList<>();

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseTxt = CharsetUtils.toString(response.getEntity());
                Object responseObj = ValueContentHandler.parse(responseTxt);

                List<?> suggestList = ValueUtils.asList(responseObj);
                for (Object suggestObj: suggestList) {
                    suggest = ValueUtils.asMap(suggestObj);
                    suggestsList.add(suggest);
                }
            }

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("target")), "folder");
            Assert.assertEquals(
                " ????????\t ??????????:trips",
                ValueUtils.asString(suggest.get("search_text")));
            Assert.assertEquals(
                "trips",
                ValueUtils.asString(suggest.get("show_text")));

            // label
            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "en");
            qc.append("request", "????????-???? label:");

            suggestsList = new ArrayList<>();

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseTxt = CharsetUtils.toString(response.getEntity());

                Object responseObj = ValueContentHandler.parse(responseTxt);

                List<?> suggestList = ValueUtils.asList(responseObj);
                for (Object suggestObj: suggestList) {
                    suggest = ValueUtils.asMap(suggestObj);
                    suggestsList.add(suggest);
                }
            }

            // user and system folders
            System.out.println("BUG " + suggestsList.toString());
            Assert.assertEquals(3, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                "unread",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "????????-???? label:Unread",
                ValueUtils.asString(suggest.get("search_text")));
            suggest = suggestsList.get(1);
            Assert.assertEquals(
                "important",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "????????-???? label:Flagged",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(2);
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("target")), "label");
            Assert.assertEquals(
                "????????-???? label:Trip",
                ValueUtils.asString(suggest.get("search_text")));
            Assert.assertEquals(
                "Trip",
                ValueUtils.asString(suggest.get("show_text")));
//            suggest = suggestsList.get(3);
//            Assert.assertEquals(
//                ValueUtils.asString(suggest.get("target")), "label");
//            Assert.assertEquals(
//                "????????-???? label:Read",
//                ValueUtils.asString(suggest.get("search_text")));
//            suggest = suggestsList.get(4);
//            Assert.assertEquals(
//                ValueUtils.asString(suggest.get("target")), "label");
//            Assert.assertEquals(
//                ValueUtils.asString(suggest.get("search_text")),
//                "????????-???? label:Flagged");

            // subject
            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", "??????????:???????????? SuBject: ??");

            suggestsList = new ArrayList<>();

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseTxt = CharsetUtils.toString(response.getEntity());
                Object responseObj = ValueContentHandler.parse(responseTxt);

                List<?> suggestList = ValueUtils.asList(responseObj);
                for (Object suggestObj: suggestList) {
                    suggest = ValueUtils.asMap(suggestObj);
                    suggestsList.add(suggest);
                }
            }

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("target")), "subject");
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("search_text")),
                "??????????:???????????? SuBject:?? ?????? Trips ????????????");
            Assert.assertEquals(
                ValueUtils.asString(suggest.get("show_text")),
                "?? ?????? Trips ????????????");

            // filter

            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", "????????????:tri");

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"category\", "
                            + "\"show_text\": \"trips\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"????????????:trips\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // month with prefix

            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", "??????????:????");
            qc.append("highlight", "true");
            // Jun 15, 2017
            qc.append("fixed-timestamp", "1497474000000");

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"target\": \"ql\", "
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_text\": \"??????????:??????\","
                            + "\"search_params\":{},"
                            + "\"show_text_highlighted\":\"<span class="
                            + "\\\"msearch-highlight\\\">??????????:????</span>??\"},"
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_params\":{},"
                            + "\"show_text_highlighted\":\"<span class="
                            + "\\\"msearch-highlight\\\">??????????:????</span>??\","
                            + "\"search_text\": \"??????????:??????\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // month with prefix, in august

            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", "??????????:????");
            // Aug 15, 2017
            qc.append("fixed-timestamp", "1502744400000");

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"target\": \"ql\", "
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:??????\"},"
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:??????\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // month with long prefix

            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", "??????????:????????");

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"target\": \"ql\", "
                            + "\"show_text\": \"??????????:????????????\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:????????????\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // month without prefix

            qc = new QueryConstructor(suggestRoute);
            qc.append("lang", "ru");
            qc.append("request", "??????????:");
            // Jun 15, 2017
            qc.append("fixed-timestamp", "1497474000000");

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"target\": \"ql\", "
                            + "\"search_params\":{},"
                            + "\"show_text\": \"??????????:\", "
                            + "\"search_text\": \"??????????:\"},"
                            + "{\"target\": \"ql\", "
                            + "\"search_params\":{},"
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_text\": \"??????????:??????\"},"
                            + "{\"target\": \"ql\", "
                            + "\"search_params\":{},"
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_text\": \"??????????:??????\"},"
                            + "{\"target\": \"ql\", "
                            + "\"search_params\":{},"
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_text\": \"??????????:??????\"},"
                            + "{\"target\": \"ql\", "
                            + "\"search_params\":{},"
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_text\": \"??????????:??????\"},"
                            + "{\"target\": \"ql\", "
                            + "\"search_params\":{},"
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_text\": \"??????????:??????\"},"
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:??????\"},"
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:??????\"},"
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:??????\"},"
                            + "{\"target\": \"ql\", "
                            + "\"show_text\": \"??????????:??????\", "
                            + "\"search_params\":{},"
                            + "\"search_text\": \"??????????:??????\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Ignore
    public void testRunLocal() throws Exception {
        System.setProperty("BSCONFIG_IPORT", "0");
        System.setProperty("SERVER_NAME", "localhost");
        System.setProperty("BSCONFIG_INAME", "localhost");
        System.setProperty("BSCONFIG_IDIR", ".");
        System.setProperty("TVM_CLIENT_ID", "2");

        System.setProperty("TVM_BP_ALLOWED_SRCS", "");
        System.setProperty("TVM_CORP_ALLOWED_SRCS", "");
        System.setProperty("SECRET", "1234567890123456789011");
        System.setProperty("CORP_BLACKBOX_CLIENT_ID", "4");
        System.setProperty("BLACKBOX_CLIENT_ID", "4");
        System.setProperty("JKS_PASSWORD", "0");

        StaticServer tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
        tvm2.add(
            "/2/keys/?lib_version=" + Version.get(),
            new File(
                StaticServer.class.getResource("tvm-keys.txt")
                    .toURI()));
        tvm2.add("/2/ticket/", "{\"4\":{\"ticket\":\"here the ticket\"}}");
        tvm2.start();

        System.setProperty("TVM_API_HOST", tvm2.host().toString());

        IniConfig config =
            new IniConfig(
                new File(
                    "/Users/vonidu/Projects/PS/saas-personal-local/git/src/msearch-proxy/main/bundle/msearch-proxy-prod.conf"));
        //config.sections().remove("tvm2");
        config.sections().remove("access_log");
        config.sections().remove("full_log");
        config.sections().remove("error_log");
        config.sections().remove("tskv_log");
        config.sections().remove("auth");
        config.sections().remove("searchmap");
        config.sections().remove("mail-search-relevance");
        //config.section("blackbox").sections().remove("tvm2");
        //config.section("corp-blackbox").sections().remove("tvm2");
        config.section("server").sections().remove("https");
        config.section("suggest").sections().remove("mail");


        MsearchProxyConfigBuilder configBuilder =
            new MsearchProxyConfigBuilder(config);
        configBuilder.searchMapConfig().file(null);
        configBuilder.searchMapConfig().content(
            "change_log shards:0-65533"
            + ",host:man1-6366.search.yandex.net,search_port:10000"
            + ",search_port_ng:10001"
            + ",json_indexer_port:10002");
        AsyncHttpServer server =
            new AsyncHttpServer(configBuilder.build(), null);
        server.start();
    }
}
