package ru.yandex.search.district;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.search.district.search.DistrictResultItem;
import ru.yandex.search.district.search.DistrictSearchSession;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class DistrictSearchTest extends TestBase {
    // CSOFF: MultipleStringLiterals
    // CSOFF: MagicNumber
    @Test
    public void testRequests() throws Exception {
        try (DistrictSearchCluster cluster = new DistrictSearchCluster(this)) {
            HttpGet get = new HttpGet(cluster.proxy().host()
                + "/api/district/search?"
                + "&length=6&scope=event&city=3"
                + "&request=...............");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            get = new HttpGet(cluster.proxy().host()
                + "/api/district/search?"
                + "&length=6&scope=event&city=3"
                + "&request=-hello");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            get = new HttpGet(cluster.proxy().host()
                + "/api/district/search?"
                + "&length=6&scope=event&city=3"
                + "&request=-");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            cluster.producer().add(
                "/_status?service=district_change_log&prefix=1&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            get = new HttpGet(cluster.proxy().host()
                + "/api/district/search?"
                + "&length=6&district=3&city=1"
                + "&request=-");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            get = new HttpGet(cluster.proxy().host()
                + "/api/district/search?"
                + "&length=6&district=3&city=1"
                + "&request=hello");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            get = new HttpGet(cluster.proxy().host()
                + "/api/district/search?"
                + "&length=6&district=3"
                + "&request=hello");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            get = new HttpGet(cluster.proxy().host()
                + "/api/district/search?"
                + "&length=6&city=3"
                + "&request=hello&time_from=1597076735&time_to=1598076735");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
        }
    }

    private static DistrictResultItem genItem(
        final long entityId)
        throws Exception
    {
        JsonObject jo = TypesafeValueContentHandler.parse(
            "{\"created_at\": 1532438041,\n"
                + "      \"likes_cnt\": 0,\n"
                + "      \"district_id\": 2,\n"
                + "      \"dislikes_cnt\": 0,\n"
                + "      \"entity_type\": \"comment\",\n"
                + "      \"user_id\": 92581,\n"
                + "      \"event_id\": 316948,\n"
                + "      \"id\": \"cmnt_316948_12\",\n"
                + "      \"text\": \"про регистрацию\","
                + "      \"entity_id\": "
                + entityId + '}');
        return new DistrictResultItem(
            jo.asMap(),
            Collections.emptyList(),
            Collections.emptyList(),
            0);
    }

    @Test
    public void testNoDistrictSearch() throws Exception {
        try (DistrictSearchCluster cluster =
                 new DistrictSearchCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            LongPrefix prefix1 = new LongPrefix(1);

            String post1 =
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"created_at\": \"1543920200\",\n"
                    + "\"entity_id\": \"100\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"text\": \"One post - no districts\",\n"
                    + "\"index_type\": \"city\"\n";
            cluster.searchBackend().add(
                prefix1,
                post1 + ",\"district_id\": \"-1\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"id\": \"city_1_event_-1_100\"\n");

            String reqRaw =
                cluster.proxy().host()
                    + "/api/district/search?&get=id&with-session=false"
                    + "&request=post";

            HttpGet get = new HttpGet(reqRaw + "&city=1");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":1, \"results\":["
                            + "{\"id\":\"city_1_event_-1_100\"}]}"),
                    responseStr);
            }

            get = new HttpGet(reqRaw + "&city=1&tags=114,37");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":1, \"results\":["
                            + "{\"id\":\"city_1_event_-1_100\"}]}"),
                    responseStr);
            }
        }
    }

    @Test
    public void testMultiDistrictSearch() throws Exception {
        try (DistrictSearchCluster cluster =
                 new DistrictSearchCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            LongPrefix prefix1 = new LongPrefix(3);
            LongPrefix prefix2 = new LongPrefix(3);
            LongPrefix prefix3 = new LongPrefix(3);

            String post1 =
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"created_at\": \"1543920200\",\n"
                    + "\"entity_id\": \"100\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"text\": \"One post - three districts\",\n"
                    + "\"city_id\": \"3\"";
            cluster.searchBackend().add(
                prefix1,
                post1 + ",\"district_id\": \"1894\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"id\": \"event_1894_100\"\n");
            cluster.searchBackend().add(
                prefix2,
                post1 + ",\"district_id\": \"1895\",\n"
                    + "\"likes_cnt\": \"300\",\n"
                    + "\"id\": \"event_1895_100\"\n");
            cluster.searchBackend().add(
                prefix3,
                post1 + ",\"district_id\": \"1896\",\n"
                    + "\"likes_cnt\": \"5\",\n"
                    + "\"id\": \"event_1896_100\"\n");

            String reqRaw =
                cluster.proxy().host()
                    + "/api/district/search?&get=id&with-session=false"
                    + "&request=post";

            HttpGet get = new HttpGet(reqRaw + "&city=3");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":1, \"results\":["
                            + "{\"id\":\"event_1895_100\"}]}"),
                    responseStr);
            }

            get = new HttpGet(reqRaw + "&districts=1894,1895,1896&city=3");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":1, \"results\":["
                            + "{\"id\":\"event_1895_100\"}]}"),
                    responseStr);
            }

            String post2 =
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543900000\",\n"
                    + "\"entity_id\": \"102\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"text\": \"Other post\",\n"
                    + "\"city_id\": \"3\"";
            cluster.searchBackend().add(
                prefix1,
                post2 + ",\"district_id\": \"1894\",\n"
                    + "\"id\": \"event_1894_102\"\n");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":2, \"results\":["
                            + "{\"id\":\"event_1895_100\"},"
                            + "{\"id\":\"event_1894_102\"}"
                            + "]}"),
                    responseStr);
            }
        }
    }

    @Test
    public void testCommon() throws Exception {
        try (DistrictSearchCluster cluster =
                 new DistrictSearchCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            LongPrefix prefix = new LongPrefix(3);

            cluster.searchBackend().add(
                prefix,
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543920200\",\n"
                    + "\"entity_id\": \"100\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_3_1884_100\",\n"
                    + "\"district_id\": \"1884\",\n"
                    + "\"text\": \"Варан - оружие героев\",\n"
                    + "\"city_id\": \"3\"");
            cluster.searchBackend().add(
                prefix,
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543920000\",\n"
                    + "\"entity_id\": \"101\",\n"
                    + "\"tags\": \"114\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_3_1884_101\",\n"
                    + "\"district_id\": \"1884\",\n"
                    + "\"text\": \"И не узнает твой герой , "
                    + "что ты прячешь под подушкой бутерброды с колбасой\",\n"
                    + "\"city_id\": \"3\"");
            cluster.searchBackend().add(
                prefix,
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543920100\",\n"
                    + "\"entity_id\": \"102\",\n"
                    + "\"tags\": \"37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_3_1884_102\",\n"
                    + "\"district_id\": \"1884\",\n"
                    + "\"text\": \"Герой - это когда написал тесты\",\n"
                    + "\"city_id\": \"3\"");

            String requestRaw =
                cluster.proxy().host()
                    + "/api/district/search?&get=id&with-session=false"
                    + "&request=";
            // Герою
            String req1 = "%D0%B3%D0%B5%D1%80%D0%BE%D1%8E&city=3";

            HttpGet get = new HttpGet(requestRaw + req1 + "&time_from=1543920050");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":2, \"results\":["
                            + "{\"id\":\"event_3_1884_100\"},"
                            + "{\"id\":\"event_3_1884_102\"}]}"),
                    responseStr);
            }

             get = new HttpGet(requestRaw + req1);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":3, \"results\":["
                            + "{\"id\":\"event_3_1884_100\"},"
                            + "{\"id\":\"event_3_1884_102\"},"
                            + "{\"id\":\"event_3_1884_101\"}]}"),
                    responseStr);
            }

            get = new HttpGet(requestRaw + req1 + "&tags=39,37");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":2, \"results\":["
                            + "{\"id\":\"event_3_1884_100\"},"
                            + "{\"id\":\"event_3_1884_102\"}]}"),
                    responseStr);
            }

            get = new HttpGet(requestRaw + req1 + "&tags=114");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":2, \"results\":["
                            + "{\"id\":\"event_3_1884_100\"},"
                            + "{\"id\":\"event_3_1884_101\"}]}"),
                    responseStr);
            }

            get = new HttpGet(
                requestRaw
                    + req1 + "&time_from=1543920050&time_to=1543920150");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":1, \"results\":["
                            + "{\"id\":\"event_3_1884_102\"}]}"),
                    responseStr);
            }

            get = new HttpGet(requestRaw
                + "%D0%B3%D0%B5%D1%80%D0%BE%D1%8E&district=1884&city=3"
                + "&time_to=1543920150");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":2, \"results\":["
                            + "{\"id\":\"event_3_1884_102\"},"
                            + "{\"id\":\"event_3_1884_101\"}]}"),
                    responseStr);
            }
        }
    }

    @Test
    public void testSession() throws Exception {
        DistrictSearchSession session =
            new DistrictSearchSession();

        List<Long> docs = new ArrayList<>();
        Random random = new Random();
        for (int i = 1000; i >= 0; i--) {
            DistrictResultItem item = genItem(random.nextInt(100000));
            docs.add(item.entityId());
            session.add(item);
        }

        String sesId = session.getSessionId();
        System.out.println("Encoded session: " + sesId.length());
        //System.out.println("Encoded session: " + sesId);
        session = new DistrictSearchSession(sesId, 0);
        //System.out.println("Decoded session: " + session.getDocs());
    }

    @Test
    public void testFuzzySearch() throws Exception {
        try (DistrictSearchCluster cluster =
                 new DistrictSearchCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            LongPrefix prefix = new LongPrefix(3);
            cluster.searchBackend().add(
                prefix,
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543928321\",\n"
                    + "\"entity_id\": \"570016\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_3_1774_570016\",\n"
                    + "\"district_id\": \"1774\",\n"
                    + "\"text\": \"Герой Ветеринар идет по полю с лопатой\",\n"
                    + "\"city_id\": \"3\"");

            cluster.searchBackend().add(
                prefix,
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543920021\",\n"
                    + "\"entity_id\": \"570015\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_3_1774_570015\",\n"
                    + "\"district_id\": \"1774\",\n"
                    + "\"text\": \"Герой Ветер идет за ветеринаром\",\n"
                    + "\"city_id\": \"3\"");

            String requestRaw1 =
                cluster.proxy().host()
                    + "/api/district/search?&get=id"
                    + "&request=%D0%B2%D0%B5%D1%82%D0%B5%D1%80";
            String request1 = requestRaw1 + "&length=6";

            HttpGet get = new HttpGet(request1 + "&city=3");
            String expected = "{\"total\":2,"
                + "\"session\": \"XQAAgAAQAAAAAAAAAAAAaQByj05-KmdgAAA\","
                + "\"results\":["
                // event_1774_570015 is exact match, so it is upper
                + "{\"id\":\"event_3_1774_570015\"},"
                + "{\"id\":\"event_3_1774_570016\"}]}";
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker(expected), responseStr);
            }

            String request2 =
                cluster.proxy().host()
                    + "/api/district/search?&length=6&get=id"
                    + "&request=%D0%93%D0%B5%D1%80%D0%BE%D0%B9"
                    + "%20%D0%B2%D0%B5%D1%82%D0%B5%D1%80";
            get = new HttpGet(request2 + "&city=3");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker(expected), responseStr);
            }

            String session = "XQAAgAAIAAAAAAAAAAAAaQByj0VYQAA";
            get = new HttpGet(requestRaw1 + "&length=1&city=3");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":2,"
                            + "\"session\": \"" + session
                            + "\", \"results\":["
                            + "{\"id\":\"event_3_1774_570015\"}]}"),
                    responseStr);
            }

            get = new HttpGet(
                requestRaw1
                    + "&offset=1&length=1&city=3&session=" + session);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":2,"
                            + "\"session\": \"XQAAgAAQAAAAAAAAAAAAaQByj"
                            + "05-KmdgAAA\","
                            + "\"results\":["
                            + "{\"id\":\"event_3_1774_570016\"}]}"),
                    responseStr);
            }

            get = new HttpGet(request1 + "&city=3");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker(expected), responseStr);
            }

            get = new HttpGet(request2 + "&city=3");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker(expected), responseStr);
            }
        }
    }

    @Test
    public void testPrunningOnShortRequests() throws Exception {
        try (DistrictSearchCluster cluster =
                 new DistrictSearchCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            LongPrefix prefix = new LongPrefix(3);
            cluster.searchBackend().add(
                prefix,
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"0\",\n"
                    + "\"created_at\": \"1543928321\",\n"
                    + "\"entity_id\": \"570016\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11000\",\n"
                    + "\"likes_cnt\": \"11000\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_1774_880016\",\n"
                    + "\"district_id\": \"1774\",\n"
                    + "\"text\": \"Еж подумал Штирлиц\",\n"
                    + "\"city_id\": \"3\"",
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543928323\",\n"
                    + "\"entity_id\": \"570017\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_1774_880017\",\n"
                    + "\"district_id\": \"1774\",\n"
                    + "\"text\": \"Штирлиц подумал еж\",\n"
                    + "\"city_id\": \"3\"",
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543928322\",\n"
                    + "\"entity_id\": \"570018\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"test\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_1774_880018\",\n"
                    + "\"district_id\": \"1774\",\n"
                    + "\"text\": \"Ежики это основа экосистемы\",\n"
                    + "\"city_id\": \"3\"");

            HttpGet get = new HttpGet(
                cluster.proxy().host()
                    + "/api/district/search?&get=id"
                    + "&request=%D0%B5%D0%B6"
                    + "&district=1774&city=3&get=id&with-session=false");

            // get sorted by date
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker("{\"total\":1,"
                    + "\"total\": 3,"
                    + "\"results\":["
                    + "{\"id\":\"event_1774_880017\"}, {\"id"
                    + "\":\"event_1774_880018\"},"
                    + "{\"id\":\"event_1774_880016\"}]}"), responseStr);
            }

            get = new HttpGet(
                cluster.proxy().host()
                    + "/api/district/search?&get=id"
                    + "&request=%D0%B5%D0%B6"
                    + "&city=3&get=id&with-session=false");

            // get sorted by date
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker("{\"total\":1,"
                    + "\"total\": 3,"
                    + "\"results\":["
                    + "{\"id\":\"event_1774_880017\"}, {\"id"
                    + "\":\"event_1774_880018\"},"
                    + "{\"id\":\"event_1774_880016\"}]}"), responseStr);
            }
        }
    }

    @Test
    public void testSaveRequests() throws Exception {
        try (DistrictSearchCluster cluster =
                 new DistrictSearchCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            LongPrefix prefix = new LongPrefix(3);
            cluster.searchBackend().add(
                prefix,
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543928321\",\n"
                    + "\"entity_id\": \"570016\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_1774_880016\",\n"
                    + "\"district_id\": \"1774\",\n"
                    + "\"text\": \"Алиса Алиса, пойдем копать сосиски!\",\n"
                    + "\"city_id\": \"3\"");

            HttpGet get = new HttpGet(
                cluster.proxy().host()
                    + "/api/district/search?&get=id"
                    + "&request=%D1%81%D0%BE%D1%81%D0%B8%D1%81%D0%BA%D0%B8"
                    + "&city=3&district=1774&get=id&save-request=true");
            String requestUri =
                    "/api/district/index/request?&"
                    + "request=%D1%81%D0%BE%D1%81%D0%B8%D1%81%D0%BA%D0%B8";
            cluster.producer().add(requestUri, HttpStatus.SC_OK);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker("{\"total\":1,"
                    + "\"session\": \"XQAAgAAIAAAAAAAAAAAAaQByj1TYAAA\","
                    + "\"results\":["
                    + "{\"id\":\"event_1774_880016\"}]}"), responseStr);
            }

            Thread.sleep(200);
            Assert.assertEquals(cluster.producer().accessCount(requestUri), 1);
        }
    }

    @Test
    public void testWizardSearch() throws Exception {
        try (DistrictSearchCluster cluster =
                 new DistrictSearchCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            LongPrefix prefix = new LongPrefix(3);

            cluster.producer().add(
                "/_status?service=district_change_log&prefix=3&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.searchBackend().add(
                prefix,
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543928800\",\n"
                    + "\"entity_id\": \"570016\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"looks_nice\": \"true\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"index_type\": \"wizzard\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_1774_880016\",\n"
                    + "\"district_id\": \"1774\",\n"
                    + "\"text\": \"Алиса Алиса, пойдем копать сосиски!\",\n"
                    + "\"city_id\": \"3\"",
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543928900\",\n"
                    + "\"entity_id\": \"570016\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"looks_nice\": \"true\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"index_type\": \"wizzard\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_1775_880016\",\n"
                    + "\"district_id\": \"1775\",\n"
                    + "\"text\": \"Алиса Алиса, пойдем копать сосиски!\",\n"
                    + "\"city_id\": \"3\"",
                "\"comments_cnt\": \"10\",\n"
                    + "\"dislikes_cnt\": \"7\",\n"
                    + "\"likes_cnt\": \"3\",\n"
                    + "\"created_at\": \"1543928950\",\n"
                    + "\"entity_id\": \"570016\",\n"
                    + "\"tags\": \"114\\n37\\n39\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"index_type\": \"wizzard\",\n"
                    + "\"looks_nice\": \"true\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"user_id\": \"1881272\",\n"
                    + "\"views_cnt\": \"11\",\n"
                    + "\"last_comment_at\": \"1545296862\",\n"
                    + "\"id\": \"event_1776_880016\",\n"
                    + "\"district_id\": \"1776\",\n"
                    + "\"text\": \"Алиса Алиса, пойдем копать сосиски!\",\n"
                    + "\"city_id\": \"2\"");

            String uri1 = "/api/district/wizard/search?&get=id"
                + "&request=%D1%81%D0%BE%D1%81%D0%B8%D1%81%D0%BA%D0%B8"
                + "&city=3&get=id";
            HttpGet get = new HttpGet(
                cluster.proxy().host() + uri1);

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":1,\"results\":["
                            + "{\"id\":\"event_1774_880016\"}]}"),
                    responseStr);
            }

            get = new HttpGet(
                cluster.proxy().host() + uri1 + "&length=2");

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"total\":2,\"results\":["
                            + "{\"id\":\"event_1774_880016\"},{\"id"
                            + "\":\"event_1775_880016\"}]}"),
                    responseStr);
            }
        }
    }
    // CSON: MultipleStringLiterals
    // CSON: MagicNumber

    // TODO Добавить тест на entity_type
}

