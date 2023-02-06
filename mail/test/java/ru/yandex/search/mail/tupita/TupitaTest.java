package ru.yandex.search.mail.tupita;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.dbfields.MailIndexFields;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.msearch.MessageContext;
import ru.yandex.test.util.Checker;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class TupitaTest extends TupitaTestBase {
    // CSOFF: MultipleStringLiterals
    // CSOFF: ParameterNumber
    private void check(
        final TupitaCluster cluster,
        final String message,
        final int status)
        throws Exception
    {
        check(cluster, message, QueryParseErrorPolicy.STRICT, status);
    }

    private void check(
        final TupitaCluster cluster,
        final String message,
        final QueryParseErrorPolicy errorPolicy,
        final int status)
        throws Exception
    {
        HttpPost post = new HttpPost(
            HTTP_LOCALHOST + cluster.tupita().port() + FAT_CHECK + UID
                + "&parse-error="
                + errorPolicy.toString().toLowerCase(Locale.ROOT));
        post.setEntity(new StringEntity(message));
        post.addHeader(YandexHeaders.TICKET, TICKET);
        HttpAssert.assertStatusCode(status, post);
    }
    // CSON: ParameterNumber

    private void check(
        final TupitaCluster cluster,
        final String message,
        final String... matched)
        throws Exception
    {
        HttpPost post = new HttpPost(
            HTTP_LOCALHOST + cluster.tupita().port() + FAT_CHECK + UID);
        post.setEntity(new StringEntity(message));
        check(post, matched);
    }

    private void check(
        final HttpPost post,
        final String... matched)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post))
        {
            StringBuilder checkSb = new StringBuilder("[");
            for (String query : matched) {
                checkSb.append("\"");
                checkSb.append(query);
                checkSb.append("\",");
            }

            if (matched.length > 0) {
                checkSb.setLength(checkSb.length() - 1);
            }

            checkSb.append("]");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            YandexAssert.check(
                new TupitaChecker(new LinkedHashSet<>(Arrays.asList(matched))),
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testNoUsers() throws Exception {
        String request =
            "{\"users\":[],\"message\":{\"types\":[],"
                + "\"subject\":\"............ .......... ............ .. "
                + "...................... ................ "
                + ".................... .... 130 000 ....... \","
                + "\"spam\":false,\"from\":[{\"local\":\"news\","
                + "\"domain\":\"list.komus.ru\","
                + "\"display_name\":\"=?utf-8?B?0JrQvtC80YPRgQ==?=\"}],"
                + "\"stid\":\"320.mail:1130000037324531.E1607180"
                + ":1837575382128626122920282114414\","
                + "\"to\":[{\"local\":\"petr\",\"domain\":\"oskt.ru\","
                + "\"display_name\":\"\"}],\"attachmentsCount\":0}}";

        try (TupitaCluster cluster = new TupitaCluster(this)) {
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port()
                    + "/check/?uid=&reqid=1551688867640465-manxx");
            post.setEntity(new StringEntity(request));
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"result\" : []}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSnakeCaseInContacts() throws Exception {
        // PS-3098
        String request =
            "{\"message\":{\"attachmentsCount\":0,"
                + "\"to\":[{\"display_name\":\"\",\"domain\":\"yandex-team"
                + ".ru\",\"local\":\"dskut\"},{\"display_name\":\"\","
                + "\"domain\":\"yandex-team.ru\",\"local\":\"prez\"}],"
                + "\"stid\":\"320.mail:1120000000099126.E1534344"
                + ":3276761776187178507619488917600\","
                + "\"from\":[{\"display_name\":\"User events tables "
                + "monitoring\",\"domain\":\"yandex-team.ru\","
                + "\"local\":\"mail-logs\"}],\"spam\":false,"
                + "\"subject\":\"Problems with user events tables\","
                + "\"types\":[56]},\"users\":[{\"uid\":1120000000099126,"
                + "\"queries\":[{\"id\":\"25368\",\"stop\":false,"
                + "\"query\":\"hdr_from_keyword:*User\\\\ events\\\\ * AND "
                + "NOT folder_type: spam\"},{\"id\":\"25369\",\"stop\":false,"
                + "\"query\":\"(hdr_to_keyword:*aaaaa* OR "
                + "hdr_cc_keyword:*aaaaa*) AND NOT folder_type: spam\"}]}]}";

        try (TupitaCluster cluster = new TupitaCluster(this)) {
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port()
                    + CHECK + "1120000000099126&debug=true");
            post.setEntity(new StringEntity(request));
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker("1120000000099126", "25368"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDoubleAsterisk() throws Exception {
        String config = "batched-queries-parsing.query-batch-size = 20\n"
            + "batched-queries-parsing.core-threads = 2\n"
            + "batched-queries-parsing.max-threads = 2\n";
        String request = "{\n"
            + "  \"users\": [\n"
            + "    {\n"
            + "      \"queries\": [\n"
            + "        {\n"
            + "          \"query\": \"hdr_subject_keyword:*Review\\\\ "
            + "Request* AND NOT folder_type: spam\",\n"
            + "          \"stop\": false,\n"
            + "          \"id\": \"25258\"\n"
            + "        }"
            + "      ],\n"
            + "      \"uid\": 1120000000099142\n"
            + "    }\n"
            + "  ],\n"
            + "  \"message\": {\n"
            + "    \"types\": [\n"
            + "      56\n"
            + "    ],\n"
            + "    \"stid\": "
            + "\"320.mail:1120000000099142.E1569680"
            + ":38731452184685551651579538816\",\n"
            + "    \"cc\": [\n"
            + "      {\n"
            + "        \"local\": \"review-checker\",\n"
            + "        \"domain\": \"yandex-team.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"local\": \"alex89\",\n"
            + "        \"domain\": \"yandex-team.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"local\": \"mafanasev\",\n"
            + "        \"domain\": \"yandex-team.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"local\": \"kharybin\",\n"
            + "        \"domain\": \"yandex-team.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"local\": \"pierre\",\n"
            + "        \"domain\": \"yandex-team.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"local\": \"ssart\",\n"
            + "        \"domain\": \"yandex-team.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"to\": [\n"
            + "      {\n"
            + "        \"local\": \"alexandr21\",\n"
            + "        \"domain\": \"yandex-team.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"spam\": false,\n"
            + "    \"attachmentsCount\": 0,\n"
            + "    \"from\": [\n"
            + "      {\n"
            + "        \"local\": \"kharybin\",\n"
            + "        \"domain\": \"yandex-team.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"subject\": \"Review Request 553558: Add MDBSAVE to "
            + "MAILCORP graph\"\n"
            + "  }\n}";
        try (TupitaCluster cluster = new TupitaCluster(this, config)) {
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port()
                    + CHECK + "1120000000099142&debug=true");
            post.setEntity(new StringEntity(request));
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker("1120000000099142", "25258"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testTupitaFatSimple() throws Exception {
        try (TupitaCluster cluster = new TupitaCluster(this)) {
            cluster.tikaite().add(
                TIKAITE_URI,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(TICKAITE_RSP),
                        new BasicHeader(
                            YandexHeaders.X_SRW_KEY,
                            STID),
                        new BasicHeader(
                            YandexHeaders.X_SRW_NAMESPACE,
                            SRW_NS_MAIL),
                        new BasicHeader(
                            YandexHeaders.X_SRW_KEY_TYPE,
                            KEY_STID))));

            final String message1 =
                "{\"queries\": [{\"id\": \"9007\", "
                    + "\"query\": \"uid:227356512 AND (hdr_from_email"
                    + ":ivan.dudinov* OR hdr_from_display_"
                    + "name:\\\"Paramparam\\\")  AND folder_type:(inbox)"
                    + "\", \"stop\": \"false\"}, {\"id\": \"9008\", "
                    + "\"query\": \"uid:227356512 AND "
                    + "(hdr_from_email:\\\"ivan.dudinov@yandex.ru\\\" "
                    + "AND hdr_from_display_name:\\\"Paramparam\\\")"
                    + " AND folder_type:(inbox)\", \"stop\": \"false\"}]"
                    + message(STID);

            check(cluster, message1, "9007");

            final String message2 =
                queries(
                    query(
                        "1007",
                        "body_text:hel* AND folder_type:(inbox)"),
                    query(
                        "1008",
                        "body_text:tollo  AND folder_type:(inbox)"))
                    + message(STID);

            HttpPost post2 = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port() + FAT_CHECK + UID);
            post2.addHeader(YandexHeaders.TICKET, TICKET);

            post2.setEntity(new StringEntity(message2));

            check(post2, "1007");

            HttpPost post3 = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port() + FAT_CHECK + UID);
            post3.addHeader(YandexHeaders.TICKET, TICKET);

            final String message3 =
                queries(
                    query(
                        "2007",
                        "hdr_from_normalized:ivan-dudinov* "
                            + "AND folder_type:(spam)"),
                    query("2008", " folder_type:(inbox)"))
                    + message(STID, "", true);

            post3.setEntity(new StringEntity(message3));
            check(post3, "2007");

            final String message4 =
                queries(
                    query(
                        "10",
                    "uid:227356512 AND (hdr_subject_normalized:dear*)"))
                    + message(STID);
            check(cluster, message4, "10");

            // check empty match
            check(
                cluster,
                "{\"queries\": []" + message(STID));

            String message6 = queries(
                query("21", ""),
                query("22", "hdr_subject:dear*"))
                + message(STID);

            check(cluster, message6, "22");

            final String message7 = queries(
                query("10", ""),
                query("20", "hdr_subject:param\\\\ "))
                + message(STID);

            check(cluster, message7);

            final String message8 = queries(
                query(
                    "116",
                    "uid:227356512 AND (hdr_subject_normalized:*dear*)"))
                + message(STID);

            check(cluster, message8, "116");

            // first query is ok and with body, second is bad
            final String message9 = queries(
                query("22", "pure_body:asdgasdf"),
                query(
                    "90",
                    "uid:1130000025188983 AND NOT folder_type: spam AND ("
                        + "(hdr_to_keyword:*poliiis.fi\t* OR "
                        + "hdr_cc_keyword:*poliiis.fi\t*))"))
                + message(STID);
            check(cluster, message9, HttpStatus.SC_BAD_REQUEST);
        }
    }

    /**
     * https://st.yandex-team.ru/PS-3079
     */
    @Test
    public void testPrefixedFields() throws Exception {
        String requestData1 = "{\n"
            + "  \"users\": [\n"
            + "    {\n"
            + "      \"queries\": [\n"
            + "        {\n"
            + "          \"query\": \"(uid:1120000000006316 AND NOT "
            + "(body_text:\\\"тело\\\\ письма\\\\ фильтра\\\" OR "
            + "pure_body:\\\"тело\\\\ письма\\\\ фильтра\\\")) AND NOT "
            + "folder_type: spam\",\n"
            + "          \"stop\": false,\n"
            + "          \"id\": \"211331\"\n"
            + "        }\n"
            + "      ],\n"
            + "      \"uid\": 1120000000006316\n"
            + "    }\n"
            + "  ],\n"
            + "  \"message\": {\n"
            + "    \"types\": [],\n"
            + "    \"subject\": \"Простая тема\",\n"
            + "    \"spam\": true,\n"
            + "    \"from\": [\n"
            + "      {\n"
            + "        \"local\": \"aga-aga\",\n"
            + "        \"domain\": \"e1plusSatt.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"stid\": "
            + "\"320.mail:0.E1483201:332619385996302180836784565909\",\n"
            + "    \"to\": [\n"
            + "      {\n"
            + "        \"local\": \"mxfilter-test-01\",\n"
            + "        \"domain\": \"yandex.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      }\n"
            + "    ]\n"
            + "  }}";

        try (TupitaCluster cluster = new TupitaCluster(this)) {
            String stid1 = "320.mail:0.E1483201:332619385996302180836784565909";
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid1,
                tikaiteResp(stid1));

            HttpPost post1 = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port() + CHECK
                    + "1120000000006316&debug");
            post1.setEntity(
                new StringEntity(requestData1, StandardCharsets.UTF_8));
            post1.addHeader(YandexHeaders.TICKET, TICKET);

            int luceneShards = cluster.tupita().lucene().config().shards();
            for (int i = 0; i < luceneShards; i++) {
                cluster.tupita().lucene().index(
                    (
                        "{\"docs\": [{\"uid\": \"8\", \"pure_body\": \"body\", "
                            + "\"folder_type\":\"user\", "
                            + "\"url\":\"8\", \"body_text\": \"body\"}],"
                            + "\"AddIfNotExists\": true, \"prefix\": "
                            + i + '}').getBytes(StandardCharsets.UTF_8),
                    new MessageContext() {
                        @Override
                        public Logger logger() {
                            return Logger.getAnonymousLogger();
                        }
                    });
            }

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post1))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "{\"result\":[{\"uid\":1120000000006316,"
                            + "\"matched_queries\":[]}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testHeaders() throws Exception {
        //https://st.yandex-team.ru/PS-3084
        String request = "{\"message\":{\"to\":[{\"display_name\":\"Alexey "
            + "Antipovsky\",\"domain\":\"yandex-team.ru\","
            + "\"local\":\"ssart\"},{\"display_name\":\"Mikhail Afanasev\","
            + "\"domain\":\"yandex-team.ru\",\"local\":\"mafanasev\"}],"
            + "\"stid\":\"320.mail:1120000000002270.E1504991"
            + ":76609539118967899748124464440\","
            + "\"cc\":[{\"display_name\":\"\",\"domain\":\"yandex-team.ru\","
            + "\"local\":\"golem-cc\"}],"
            + "\"from\":[{\"display_name\":\"Ekaterina Barantsova\","
            + "\"domain\":\"yandex-team.ru\",\"local\":\"barantsovaes\"}],"
            + "\"spam\":false,\"subject\":\"golem jrnl: mail_forward "
            + "(forward105o.mail.yandex.net)\",\"types\":[4,56]},"
            + "\"users\":[{\"uid\":1120000000099138,"
            + "\"queries\":[{\"id\":\"24786\",\"stop\":true,"
            + "\"query\":\"headers:\\\"y-exchange-calendar: Yes\\\" AND NOT "
            + "folder_type: spam\"},{\"id\":\"24787\",\"stop\":true,"
            + "\"query\":\"hdr_to_keyword:*psfups@yandex\\\\-team.ru* AND NOT"
            + " folder_type: spam\"},{\"id\":\"24788\",\"stop\":true,"
            + "\"query\":\"(headers:X-Yandex-Spam\\\\:\\\\ *4* OR "
            + "headers:X-Spam-Flag\\\\:\\\\ *YES*) AND NOT folder_type: "
            + "spam\"},{\"id\":\"24789\",\"stop\":false,\"query\":\""
            + "(hdr_from_email:\\\"startrek@yandex\\\\-team.ru\\\" OR "
            + "hdr_from_display_name:\\\"startrek@yandex\\\\-team.ru\\\") AND"
            + " NOT folder_type: spam\"},{\"id\":\"24790\",\"stop\":false,"
            + "\"query\":\"headers:X-ReviewRequest-URL\\\\:\\\\ "
            + "*yandex\\\\-team* AND NOT folder_type: spam\"},"
            + "{\"id\":\"24791\",\"stop\":false,\"query\":\""
            + "(hdr_from_email:\\\"noreply@github.yandex\\\\-team.ru\\\" OR "
            + "hdr_from_display_name:\\\"noreply@github.yandex\\\\-team"
            + ".ru\\\") AND NOT folder_type: spam\"},{\"id\":\"24792\","
            + "\"stop\":true,\"query\":\"headers:\\\"X-Golem: yes\\\" AND NOT"
            + " folder_type: spam\"}]}]}";

        String stid =
            "320.mail:1120000000002270.E1504991:76609539118967899748124464440";
        String stid2 =
            "320.mail:1120000000014403.E1504991:76609539118967899748124464440";
        String request2 =
            "{\"message\":{\"to\":[{\"display_name\":\"Alexey "
                + "Antipovsky\",\"domain\":\"yandex-team.ru\","
                + "\"local\":\"ssart\"},{\"display_name\":\"Mikhail Afanasev\","
                + "\"domain\":\"yandex-team.ru\",\"local\":\"mafanasev\"}],"
                + "\"stid\":\"320.mail:1120000000014403.E1504991"
                + ":76609539118967899748124464440\","
                + "\"cc\":[{\"display_name\":\"\",\"domain\":\"yandex-team"
                + ".ru\","
                + "\"local\":\"golem-cc\"}],"
                + "\"from\":[{\"display_name\":\"Startrek\","
                + "\"domain\":\"yandex-team.ru\",\"local\":\"startrek\"}],"
                + "\"spam\":false,\"subject\":\"Вася призывает вас"
                + " принести холодного пива\","
                + "\"types\":[4,56]},"
                + "\"users\":[{\"uid\": 1120000000014403, "
                + "\"queries\":[{\"id\":100200, \"query\":"
                + "\"uid:1120000000014403 AND NOT folder_type: spam AND"
                + " NOT headers:\\\"X-Startrek-Assignee: ppalex\\\" "
                + "AND NOT hid:0 AND headers:X-Startrek-CC\\\\:\\\\ "
                + "*ppalex@startrek\\\\-cc* "
                + " AND hdr_from_keyword:*startrek@yandex\\\\-team.ru* AND NOT "
                + " hdr_subject_keyword:"
                + "*призывает\\\\ вас\\\\ в\\\\ комментарии*\"}]}]}";
        try (TupitaCluster cluster = new TupitaCluster(this)) {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                tikaiteResp(stid));

            HttpPost post2 = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port() + CHECK
                    + "1120000000002270&debug");
            post2.setEntity(
                new StringEntity(request, StandardCharsets.UTF_8));
            post2.addHeader(YandexHeaders.TICKET, TICKET);
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post2))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "{\"result\":[{\"uid\":1120000000099138,"
                            + "\"matched_queries\":[\"24792\"]}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid2,
                new StaticHttpItem(
                    "{\"docs\": [{\"hid\": \"1.1\", \"headers\": "
                        + "\"x-startrek-cc: ppalex@startrek-cc\n"
                        + "x-startrek-assignee: vasya\"}]}"));

            HttpPost post3 = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port() + CHECK
                    + "1120000000014403&debug");
            post3.setEntity(
                new StringEntity(request2, StandardCharsets.UTF_8));
            post3.addHeader(YandexHeaders.TICKET, TICKET);
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post3))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "{\"result\":[{\"uid\":1120000000014403,"
                            + "\"matched_queries\":[\"100200\"]}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testHasAttachments() throws Exception {
        String requestData = "{\"users\":[{\"uid\": 271344218, "
            + "\"queries\":[{\"query\":\""
            + "has_attachments:1\",\"stop\":false,\"id\":\"5\"}]}], "
            + "\"message\":{\"types\":[],\"subject\":\"Простая тема\","
            + "\"spam\":true,\"from\":[{\"local\":\"aga-aga\","
            + "\"domain\":\"e1plusSatt.ru\",\"display_name\":\"\"}],"
            + "\"stid\":\"320.mail:271344218.E226106:500\","
            + "\"attachmentsCount\": 1,"
            + "\"to\":[{\"local\":\"mxfilter-test-01\",\"domain\":\"yandex"
            + ".ru\",\"display_name\":\"\"}]}}";

        try (TupitaCluster cluster = new TupitaCluster(this)) {
            HttpPost post2 = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port()
                    + CHECK + "271344218&debug");
            post2.setEntity(
                new StringEntity(requestData, StandardCharsets.UTF_8));
            post2.addHeader(YandexHeaders.TICKET, TICKET);
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post2))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "{\"result\":[{\"uid\":271344218,"
                            + "\"matched_queries\":[\"5\"]}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSubscriptions() throws Exception {
        String request = "{\n"
            + "  \"message\": {\n"
            + "    \"to\": [\n"
            + "      {\n"
            + "        \"display_name\": \"sonder_ded\",\n"
            + "        \"domain\": \"gmail.com\",\n"
            + "        \"local\": \"andrey.sovetsk1985\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"stid\": "
            + "\"320.mail:109265139.E1682621:296996507311116779061531093211"
            + "\",\n"
            + "    \"cc\": [\n"
            + "    ],\n"
            + "    \"from\": [\n"
            + "      {\n"
            + "        \"display_name\": \"YouTube\",\n"
            + "        \"domain\": \"youtube.com\",\n"
            + "        \"local\": \"noreply\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"spam\": false,\n"
            + "    \"subject\": \"AkerMehanik только что добавил(а) видео\",\n"
            + "    \"types\": [13]\n"
            + "  },\n"
            + "  \"users\": [\n"
            + "    {\n"
            + "      \"uid\": 109265139,\n"
            + "      \"spam\": false,\n"
            + "      \"queries\": [\n"
            + "        {\n"
            + "          \"id\": \"1\",\n"
            + "          \"stop\": false,\n"
            + "          \"query\": "
            + "\"subscription-email:no\\\\-reply@goodgame.ru AND "
            + "message_type:13 AND folder_type:(inbox)\"\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  ]}";

        String uid = "109265139";
        try (TupitaCluster cluster = new TupitaCluster(this)) {
            HttpPost post2 = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port() + CHECK + uid);
            post2.setEntity(
                new StringEntity(request, StandardCharsets.UTF_8));
            post2.addHeader(YandexHeaders.TICKET, TICKET);
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post2))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "{\"result\":[{\"uid\":" + uid
                            + ",\"matched_queries\":[]}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBodyFilters() throws Exception {
        String requestData2 = "{\"users\":[{\"queries\":[{\"query\":\""
            + "(hdr_to_email:\\\"filter-02@yandex.ru\\\" OR "
            + "hdr_to_display_name:\\\"filter-02@yandex.ru\\\") AND NOT "
            + "folder_type: spam\",\"stop\":false,\"id\":\"211331\"},"
            + "{\"query\":\"(hdr_cc_email:\\\"filter-02@yandex.ru\\\" OR "
            + "hdr_cc_display_name:\\\"filter-02@yandex.ru\\\") AND NOT "
            + "folder_type: spam\",\"stop\":false,\"id\":\"211332\"},"
            + "{\"query\":\"(hdr_subject:1att OR "
            + "(hdr_from_email:\\\"aga-aga@e1att.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e1att.ru\\\")) AND NOT "
            + "folder_type: spam AND NOT has_attachments:1\",\"stop\":false,"
            + "\"id\":\"211333\"},{\"query\":\"(hdr_subject:1plusS# OR "
            + "(hdr_from_email:\\\"aga-aga@e1plusSatt.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e1plusSatt.ru\\\")) AND NOT "
            + "has_attachments:1\",\"stop\":false,\"id\":\"211334\"},"
            + "{\"query\":\"hdr_subject:11 AND NOT folder_type: spam AND "
            + "(hdr_from_email:\\\"aga-aga@e11.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e11.ru\\\") AND "
            + "has_attachments:1\",\"stop\":false,\"id\":\"211335\"},"
            + "{\"query\":\"hdr_subject:11# AND NOT folder_type: spam AND "
            + "(hdr_from_email:\\\"aga-aga@e11at.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e11at.ru\\\") AND NOT "
            + "has_attachments:1\",\"stop\":false,\"id\":\"211336\"},"
            + "{\"query\":\"hdr_subject:11plusS AND "
            + "(hdr_from_email:\\\"aga-aga@e11plusS.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e11plusS.ru\\\") AND "
            + "has_attachments:1\",\"stop\":false,\"id\":\"211337\"},"
            + "{\"query\":\"hdr_subject:11s AND "
            + "(hdr_from_email:\\\"aga-aga@e11s.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e11s.ru\\\") AND "
            + "folder_type: spam AND has_attachments:1\",\"stop\":false,"
            + "\"id\":\"211338\"},{\"query\":\"hdr_subject:11plusS# AND "
            + "(hdr_from_email:\\\"aga-aga@e11plusSat.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e11plusSat.ru\\\") AND NOT "
            + "has_attachments:1\",\"stop\":false,\"id\":\"211339\"},"
            + "{\"query\":\"(hdr_subject:1satt OR "
            + "(hdr_from_email:\\\"aga-aga@e1satt.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e1satt.ru\\\")) AND "
            + "folder_type: spam AND has_attachments:1\",\"stop\":false,"
            + "\"id\":\"211340\"},{\"query\":\"(hdr_subject:1 OR "
            + "(hdr_from_email:\\\"aga-aga@e1.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e1.ru\\\")) AND NOT "
            + "folder_type: spam AND has_attachments:1\",\"stop\":false,"
            + "\"id\":\"211341\"},{\"query\":\"(hdr_subject:1s OR "
            + "(hdr_from_email:\\\"aga-aga@e1s.ru\\\" OR "
            + "hdr_from_display_name:\\\"aga-aga@e1s.ru\\\")) AND "
            + "folder_type: spam AND NOT has_attachments:1\",\"stop\":false,"
            + "\"id\":\"211342\"}],\"uid\":271344218}],"
            + "\"message\":{\"types\":[],\"subject\":\"Простая тема\","
            + "\"spam\":true,\"from\":[{\"local\":\"aga-aga\","
            + "\"domain\":\"e1plusSatt.ru\",\"display_name\":\"\"}],"
            + "\"stid\":\"320.mail:271344218.E226106"
            + ":4140383168143712979252061511724\","
            + "\"labels\" : [\"FAKE_ATTACHED_LBL\"],"
            + "\"labelsInfo\":{\"FAKE_ATTACHED_LBL\":{\"type\":{\"title\":"
            + "\"system\"},\"symbolicName\":{\"title\":\"attached_label\"}}},"
            + "\"to\":[{\"local\":\"mxfilter-test-01\",\"domain\":\"yandex"
            + ".ru\",\"display_name\":\"\"}]}}";

        String stid =
            "320.mail:271344218.E226106:4140383168143712979252061511724";
        try (TupitaCluster cluster = new TupitaCluster(this)) {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                tikaiteResp(stid));

            HttpPost post2 = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port() + CHECK + UID);
            post2.setEntity(
                new StringEntity(requestData2, StandardCharsets.UTF_8));
            post2.addHeader(YandexHeaders.TICKET, TICKET);
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post2))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "{\"result\":[{\"uid\":271344218,"
                            + "\"matched_queries\":[]}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFatBatchParsing() throws Exception {
        String config = "batched-queries-parsing.query-batch-size = 2\n"
            + "batched-queries-parsing.core-threads = 2\n"
            + "batched-queries-parsing.max-threads = 2\n";
        try (TupitaCluster cluster =
                 new TupitaCluster(this, config))
        {
            cluster.tikaite().add(
                TIKAITE_URI,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(TICKAITE_RSP),
                    new BasicHeader(
                        YandexHeaders.X_SRW_KEY,
                        STID),
                    new BasicHeader(
                        YandexHeaders.X_SRW_NAMESPACE,
                        SRW_NS_MAIL),
                    new BasicHeader(
                        YandexHeaders.X_SRW_KEY_TYPE,
                        KEY_STID)));

            String queryText = "uid:227356512 AND hdr_from_email:ivan.d*";
            StringBuilder query = new StringBuilder("{\"queries\":  [");
            final int queryCount = 10;
            for (int i = 0; i < queryCount; i++) {
                if (i % 2 == 0) {
                    query.append(query(String.valueOf(i), queryText));
                } else {
                    query.append(query(String.valueOf(i), ""));
                }

                if (i < queryCount - 1) {
                    query.append(',');
                }
            }
            query.append(']');

            query.append(message(STID));

            HttpPost post2 = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port() + FAT_CHECK + UID);
            post2.addHeader(YandexHeaders.TICKET, TICKET);

            post2.setEntity(new StringEntity(query.toString()));

            String[] matched = new String[] {"8", "0", "2", "4", "6"};
            check(post2, matched);
        }
    }

    @Test
    public void testNotBodyTest() throws Exception {
        // PS-3098
        String stid = "320.mail:0.E1457383:2930544821130138800250549909739";

        byte[] tikaiteResponse =
            Files.readAllBytes(
                Paths.get(
                    this.getClass().getResource(
                        "2930544821130138800250549909739.tikaite")
                        .toURI()));

        Map<String, Set<String>> expected = new LinkedHashMap<>();
        expected.put(
            "52503287",
            new LinkedHashSet<>(
                Arrays.asList("27461", "27462")));
        expected.put(
            "52328359",
            new LinkedHashSet<>(Collections.singleton("27437")));
        try (TupitaCluster cluster = new TupitaCluster(this)) {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                new ByteArrayEntity(tikaiteResponse));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port()
                    + CHECK + "52503287&debug=true");
            post.setEntity(
                new InputStreamEntity(this.getClass()
                .getResourceAsStream(
                "2930544821130138800250549909739.json")));
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMultipleParts() throws Exception {
        String stid =
            "320.mail:134067156.E239433:33261938595033511893819798209";

        String requestData = "{\n"
            + "  \"users\": [\n"
            + "    {\n"
            + "      \"queries\": [\n"
            + "        {\n"
            + "          \"query\": \"uid:134067156 AND NOT "
            + "folder_type: spam AND pure_body:*амбруазович*\",\n"
            + "          \"stop\": false,\n"
            + "          \"id\": \"331\"\n"
            + "        }\n"
            + "      ],\n"
            + "      \"uid\": 134067156\n"
            + "    }\n"
            + "  ],\n"
            + "  \"message\": {\n"
            + "    \"types\": [],\n"
            + "    \"subject\": \"амвросий амбруазович Выбегалло"
            + " прислала новое сообщение\",\n"
            + "    \"spam\": false,\n"
            + "    \"from\": [\n"
            + "      {\n"
            + "        \"local\": \"bezotveta\",\n"
            + "        \"domain\": \"odnoklassniki.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"stid\": \"" + stid + "\",\n"
            + "    \"to\": [\n"
            + "      {\n"
            + "        \"local\": \"vibegallo.zabegalo\",\n"
            + "        \"domain\": \"yandex.ru\",\n"
            + "        \"display_name\": \"\"\n"
            + "      }\n"
            + "    ]\n"
            + "  }}";

        byte[] tikaiteResponse =
            Files.readAllBytes(
                Paths.get(
                    this.getClass().getResource(
                        "many_parts.tikaite")
                        .toURI()));

        try (TupitaCluster cluster = new TupitaCluster(this)) {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                new ByteArrayEntity(tikaiteResponse));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.tupita().port()
                    + CHECK + "134067156&debug=true");
            post.setEntity(new StringEntity(requestData));
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker("134067156", "331"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testReplaceEeFilter() throws Exception {
        String stid = "";
        String message =
            ",\"message\": {\"subject\" : \"Приёмка базы геопоиска"
                + "бинарникa геопоискового метапоиска 110\",\n"
            + "         \"types\" : [4,52],"
            + "         \"uid\" : \"100500\",\n"
            + "         \"to\" : [{\n"
            + "               \"domain\" : \"gmail.com\",\n"
            + "               \"local\" : \"vasya\",\n"
            + "               \"displayName\" : \"Vasya Pupkin\"\n"
            + "            }],\n"
            + "         \"from\" : [{\n"
            + "               \"domain\" : \"yandex.ru\","
            + "               \"local\" : \"pupka.vasin\","
            + "               \"displayName\" : \"Pupka Vasin\"}],\n"
            + "         \"stid\" : \"320.mail:500.E600:444523463\"}}";
        String request1 =
            request(
                message,
                userQueries(
                    "1909",
                    query(
                        "3002",
                        "hdr_subject:*приёмка*"),
                    query(
                        "3003",
                        "hdr_subject:приемка")));

        Map<String, Set<String>> expected1 = new LinkedHashMap<>();
        expected1.put(
            "1909",
            new LinkedHashSet<>(Arrays.asList("3002", "3003")));
        try (TupitaCluster cluster = new TupitaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                new StaticHttpItem(TICKAITE_RSP));

            String uri = HTTP_LOCALHOST + cluster.tupita().port()
                + CHECK + "1909&debug=true&parse-error=skip";

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(request1, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker(expected1),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPersonalSpam() throws Exception {
        String stid = "320.mail:500.E600:444523463";
        String request1 =
            request(
                message(stid),
                userQueries(
                    "909",
                    true,
                    query(
                        "3002",
                        "hdr_to:*vonidu* AND NOT folder_type: spam"),
                    query("3003", "hdr_to:*vonidu*")),
                userQueries(
                    "910",
                    query("3002", "hdr_to:*vonidu*")));

        Map<String, Set<String>> expected1 = new LinkedHashMap<>();
        expected1.put("909", Collections.singleton("3003"));
        expected1.put("910", Collections.singleton("3002"));
        try (TupitaCluster cluster = new TupitaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                new StaticHttpItem(TICKAITE_RSP));

            String uri = HTTP_LOCALHOST + cluster.tupita().port()
                + CHECK + "910&debug=true&parse-error=skip";

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(request1, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker(expected1),
                    CharsetUtils.toString(response.getEntity()));
            }

            //ok now check with fat/nonfat

            final int queriesCnt = 200;
            String[] queries = new String[queriesCnt + 1];
            for (int i = 0; i < queriesCnt; i++) {
                queries[i] =
                    query(String.valueOf(i), "hdr_to:*not_matched*");
            }

            // matched
            queries[queriesCnt] =
                query(String.valueOf(queriesCnt), "hdr_to:*vonidu*");

            String request2 =
                request(
                    message(stid),
                    userQueries(
                        "1010",
                        true,
                        query(
                            "3002",
                            "hdr_to:*vonidu* AND NOT folder_type: spam"),
                        query("3003", "hdr_to:*vonidu*")),
                    userQueries("1011", false, queries),
                    userQueries("1012", false));

            post = new HttpPost(uri);
            post.setEntity(new StringEntity(request2, StandardCharsets.UTF_8));

            Map<String, Set<String>> expected2 = new LinkedHashMap<>();
            expected2.put("1010", Collections.singleton("3003"));
            expected2.put("1011", Collections.singleton("200"));
            expected2.put("1012", Collections.emptySet());

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker(expected2),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Ignore
    public void testSpecialSmallQueries() throws Exception {
        String requestMatch = "{\"users\":[{\"queries\":[{\"query\":\""
            + "(hdr_subject_keyword:*\\\\\\\\*\\\\\\\\** OR "
            + "hdr_subject_keyword:*Cron* OR hdr_subject_keyword:*cvs\\\\ "
            + "commit* OR hdr_subject_keyword:*golem\\\\ jrnl*) AND NOT "
            + "folder_type: spam\",\"stop\":false,\"id\":\"1\"}],"
            + "\"spam\":false,\"uid\":109265139}],"
            + "\"message\":{\"types\":[13],\"subject\":\"AkerMehanik ** только "
            + "что добавил(а) видео\",\"spam\":false,"
            + "\"from\":[{\"local\":\"noreply\",\"domain\":\"youtube.com\","
            + "\"display_name\":\"YouTube\"}],\"cc\":[],"
            + "\"stid\":\"320.mail:109265139.E1682621"
            + ":296996507311116779061531093211\",\"to\":[{\"local\":\"andrey"
            + ".sovetsk1985\",\"domain\":\"gmail.com\","
            + "\"display_name\":\"sonder_ded\"}]}}";

        String requestNotMatch = "{\"users\":[{\"queries\":[{\"query\":\""
            + "(hdr_subject_keyword:*\\\\\\\\*\\\\\\\\** OR "
            + "hdr_subject_keyword:*Cron* OR hdr_subject_keyword:*cvs\\\\ "
            + "commit* OR hdr_subject_keyword:*golem\\\\ jrnl*) AND NOT "
            + "folder_type: spam\",\"stop\":false,\"id\":\"1\"}],"
            + "\"spam\":false,\"uid\":109265139}],"
            + "\"message\":{\"types\":[13],\"subject\":\"AkerMehanik только "
            + "что добавил(а) видео\",\"spam\":false,"
            + "\"from\":[{\"local\":\"noreply\",\"domain\":\"youtube.com\","
            + "\"display_name\":\"YouTube\"}],\"cc\":[],"
            + "\"stid\":\"320.mail:109265139.E1682621"
            + ":296996507311116779061531093211\",\"to\":[{\"local\":\"andrey"
            + ".sovetsk1985\",\"domain\":\"gmail.com\","
            + "\"display_name\":\"sonder_ded\"}]}}";

        try (TupitaCluster cluster = new TupitaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String uri = HTTP_LOCALHOST + cluster.tupita().port()
                + CHECK + "109265139&debug=true&parse-error=skip";

            HttpPost post = new HttpPost(uri);
            post.setEntity(
                new StringEntity(
                    requestMatch,
                    StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker("109265139", "1"),
                    CharsetUtils.toString(response.getEntity()));
            }

            post = new HttpPost(uri);
            post.setEntity(
                new StringEntity(
                    requestNotMatch,
                    StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker("109265139"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testParseErrorPolicy() throws Exception {
        String stid = "320.mail:500.E600:234523463";
        String request1 =
            request(
                message(stid),
                userQueries(
                    "500",
                    query("1001", "hdr_to:vonidu: AND NOT spam:true"),
                    query("1002", "hdr_to:*vonidu*")),
                userQueries(
                    "700",
                    query("1002", "hdr_to:*vonidu*")));

        Map<String, Set<String>> expected1 = new LinkedHashMap<>();
        expected1.put("500", Collections.singleton("1002"));
        expected1.put("700", Collections.singleton("1002"));

        String request2 =
            request(
                message(stid),
                userQueries(
                    "500",
                    query("1001", "headers:vonidu:asdf\\\\:sadg")));

        String request3 =
            request(
                message(stid),
                userQueries(
                    "500",
                    query("1001", "headers:vonidu:asdf\\\\:sadg"),
                    query("1003", "body_text:hello"),
                    query(
                        "1004",
                        "uid:500 AND NOT folder_type: spam AND "
                            + "hdr_to_keyword:*vonidu\t*")));

        try (TupitaCluster cluster = new TupitaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                new StaticHttpItem(TICKAITE_RSP));

            String uri = HTTP_LOCALHOST + cluster.tupita().port()
                + CHECK + "52503287&debug=true&parse-error=skip";

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(request1));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker(expected1),
                    CharsetUtils.toString(response.getEntity()));
            }

            post.setEntity(new StringEntity(request2));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker("500"),
                    CharsetUtils.toString(response.getEntity()));
            }

            post.setEntity(new StringEntity(request3));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker("500", "1003"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    //https://st.yandex-team.ru/PS-3382
    @Test
    public void testMessageInMessage() throws Exception {
        String request =
            "{\n"
                + "  \"message\": {\n"
                + "    \"attachmentsCount\": 0,\n"
                + "    \"to\": [\n"
                + "      {\n"
                + "        \"display_name\": \"\",\n"
                + "        \"domain\": \"ya.ru\",\n"
                + "        \"local\": \"hudyaarmavir\"\n"
                + "      }\n"
                + "    ],\n"
                + "    \"stid\": "
                + "\"320.mail:1120000000039071.E1039890"
                + ":270837165018574806726539690830\",\n"
                + "    \"from\": [\n"
                + "      {\n"
                + "        \"display_name\": \"Яндекс.Такси\",\n"
                + "        \"domain\": \"taxi.yandex.ru\",\n"
                + "        \"local\": \"no-reply\"\n"
                + "      }\n"
                + "    ],\n"
                + "    \"spam\": false,\n"
                + "    \"subject\": \"Яндекс.Такси – отчёт о поездке 23 "
                + "апреля 2019 г.\",\n"
                + "    \"types\": []\n"
                + "  },\n"
                + "  \"users\": [\n"
                + "    {\n"
                + "      \"uid\": 500,\n"
                + "      \"queries\": [\n"
                + "        {\n"
                + "          \"id\": \"153509\",\n"
                + "          \"stop\": false,\n"
                + "          \"query\": \"(headers:X-Yandex-Spam\\\\:\\\\ *4*"
                + " OR headers:X-Spam-Flag\\\\:\\\\ *YES*) AND NOT "
                + "folder_type: spam\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"spam\": false\n"
                + "    }\n"
                + "  ]}";
        String stid =
            "320.mail:1120000000039071.E1039890:270837165018574806726539690830";

        Map<String, Set<String>> expected1 = new LinkedHashMap<>();
        expected1.put("500", Collections.emptySet());
        try (TupitaCluster cluster = new TupitaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                tikaiteResp(stid, "message_inside_message.tikaite"));

            String uri = HTTP_LOCALHOST + cluster.tupita().port()
                + CHECK + "500&debug=true&parse-error=skip";

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(request));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker(expected1),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    // https://st.yandex-team.ru/PS-3301
    @Test
    public void testAttachname() throws Exception {
        String stid = "320.mail:500.E600:234523463";
        String request1 =
            request(
                message(stid, "\"attachmentsCount\":2"),
                userQueries(
                    "227356512",
                    query(
                        "1000",
                        "(attachname:*_test_* OR attachname_keyword:*_test_*)"
                            + " AND NOT folder_type: spam AND "
                            + "has_attachments:1")));

        Map<String, Set<String>> expected1 = new LinkedHashMap<>();
        expected1.put("227356512", Collections.singleton("1000"));

        try (TupitaCluster cluster = new TupitaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                tikaiteResp(stid, "ps_3301.tikaite"));

            String uri = HTTP_LOCALHOST + cluster.tupita().port()
                + CHECK + "227356512&debug=true&parse-error=skip";

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(request1));
            System.out.println(request1);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker(expected1),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDomainLabel() throws Exception {
        String stid = "320.mail:500.E600:234523463";
        String request1 =
            request(
                message(
                    stid,
                    "\"labelsInfo\": {\n"
                        + " \"FAKE_DOMAIN_LABEL\": {\n"
                        + "    \"name\": \"vtnrf0grouponsite\",\n"
                        + "    \"isSystem\": false,\n"
                        + "    \"type\": {\n"
                        + "        \"title\": \"social\"\n"
                        + "    },\n"
                        + "    \"isUser\": false\n"
                        + "}}"),
                userQueries(
                    "500",
                    query(
                        "1000",
                        "hdr_to:vonidu AND domain_label:sadbafd"),
                    query(
                        "1001",
                        "hdr_to:vonidu AND domain_label:vtnrf0grouponsite")));

        Map<String, Set<String>> expected1 = new LinkedHashMap<>();
        expected1.put("500", Collections.singleton("1001"));

        try (TupitaCluster cluster = new TupitaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                new StaticHttpItem(TICKAITE_RSP));

            String uri = HTTP_LOCALHOST + cluster.tupita().port()
                + CHECK + "500&debug=true&parse-error=skip";

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(request1));
            System.out.println(request1);

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker(expected1),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCorpmail4575() throws Exception {
        String stid = "corpmail.4575.stid";
        String request1 =
            request(
                message(stid),
                userQueries(
                    "500",
                    query(
                        "1000",
                        "uid:1120000000117239 AND NOT folder_type: spam AND " +
                            "(body_text:\\\"Исполнитель\\\\\\tМитя\\\\ Дубильт\\\" OR " +
                            "pure_body:\\\"Исполнитель\\\\\\tМитя\\\\ Дубильт\\\") AND " +
                            "(body_text:\\\"oebs\\\\-support\\\" OR pure_body:\\\"oebs\\\\-support\\\")")));
        try (TupitaCluster cluster = new TupitaCluster(this, "",true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.addStid(stid, "./emls/corpmail_4575.eml");

            Map<String, Set<String>> expected1 = new LinkedHashMap<>();
            expected1.put("500", Collections.singleton("1000"));

            String uri = HTTP_LOCALHOST + cluster.tupita().port()
                + CHECK + "500&debug=true&parse-error=skip";
            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(request1, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaNewFormatChecker(expected1),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private static final class TupitaChecker implements Checker {
        private final Set<String> expected;

        private TupitaChecker(final Set<String> expected) {
            this.expected = expected;
        }

        @Override
        public String check(final String value) {
            System.out.println(value);
            String result = null;
            Set<String> queries = new LinkedHashSet<>();
            try {
                JsonObject root = TypesafeValueContentHandler.parse(value);
                for (JsonObject query : root.asList()) {
                    queries.add(query.asString());
                }

                if (!queries.equals(expected)) {
                    result =
                        "Expected " + expected.toString()
                            + " but got " + queries.toString();
                }
            } catch (JsonException je) {
                je.printStackTrace();
                result = "Failed to parse " + value;
            }

            return result;
        }
    }

    public static final class TupitaNewFormatChecker implements Checker {
        private final Map<String, Set<String>> expected;

        public TupitaNewFormatChecker(
            final Map<String, Set<String>> expected)
        {
            this.expected = expected;
        }

        public TupitaNewFormatChecker(
            final String uid,
            final String... queries)
        {
            this(
                Collections.singletonMap(
                    uid,
                    new LinkedHashSet<>(Arrays.asList(queries))));
        }

        @Override
        public String check(final String value) {
            System.out.println("Raw got " + value);
            String result = "";
            try {
                JsonMap root =
                    TypesafeValueContentHandler.parse(value).asMap();
                Map<String, Set<String>> userMap = new LinkedHashMap<>();

                for (JsonObject user: root.getList("result")) {
                    String uid = user.asMap().getString(MailIndexFields.UID);
                    Set<String> queries =
                        userMap.computeIfAbsent(
                            uid,
                            (u) -> new LinkedHashSet<>());

                    for (JsonObject query
                        : user.asMap().getList("matched_queries"))
                    {
                        queries.add(query.asString());
                    }

                    if (!queries.equals(expected.get(uid))) {
                        result +=
                            "Expected " + expected.toString()
                                + " but got " + queries.toString() + '\n'
                                + " for " + uid;
                    }
                }

                if (!expected.keySet().equals(userMap.keySet())) {
                    result += "Inequals list of uids, expected\n"
                        + expected.keySet()
                        + "\ngot\n"
                        + userMap.keySet()
                        + '\n';
                }
            } catch (JsonException je) {
                je.printStackTrace();
                result = "Failed to parse " + value;
            }

            if (result.isEmpty()) {
                return null;
            } else {
                return result;
            }
        }
    }
    // CSON: MultipleStringLiterals
}
