package ru.yandex.msearch.proxy.suggest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.collection.IntInterval;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.json.dom.ValueContentHandler;
import ru.yandex.json.xpath.ValueUtils;
import ru.yandex.msearch.proxy.MsearchProxyCluster;
import ru.yandex.msearch.proxy.MsearchProxyTestBase;
import ru.yandex.msearch.proxy.SinxProxyHandler;
import ru.yandex.msearch.proxy.highlight.HtmlHighlighter;
import ru.yandex.msearch.proxy.api.async.suggest.history.UpdateStoredRequest;
import ru.yandex.msearch.proxy.highlight.RequestMatcher;
import ru.yandex.msearch.proxy.suggest.utils.MailUser;
import ru.yandex.msearch.proxy.suggest.utils.SuggestTestUtil;
import ru.yandex.msearch.proxy.suggest.utils.SuggestTestUtil.Email;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class SuggestUnitedTest extends MsearchProxyTestBase {
    private final SuggestTestUtil async =
        new SuggestTestUtil(SuggestContactsTest.CONTACT_NEW_API);

    // SUHOLET asked to siable this feature 04.10.2018
    @Ignore
    @Test
    public void testSubjectQl() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100502",
                    "\"thread_id\":100502," +
                        "\"received_date\":\"1234567892\"," +
                        "\"hdr_from\":\"\\\"Ivan\\\"<ivan@yandex.ru>\"," +
                        "\"hdr_subject\":" +
                        "\"Новая тема\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Новая тема\"",
                    "\"pure_body\":\"Тема\""));
            cluster.backend().add(
                doc(
                    "100503",
                    "\"thread_id\":100503," +
                        "\"received_date\":\"1234567893\"," +
                        "\"hdr_from\":\"\\\"Ivan\\\"<ivan@yandex.ru>\"," +
                        "\"hdr_subject\":" +
                        "\"Новая тема для разговора\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Новая тема для разговора\"",
                    "\"pure_body\":\"Тема\""));
            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            String requestBase =
                "/api/async/mail/suggest?type=subject&side=mobile&uid=0";

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(
                    requestBase + "&request=новая+тема")))
            {
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"target\": \"subject\","
                            + "\"show_text\":\"Новая тема для разговора\", "
                            + "\"search_params\":"
                            + "{\"scope\": \"subject_thread_100503\"},"
                            + "\"search_text\": \"тема:(Новая тема для разговора)\","
                            + "\"mail-count\": 1},"
                            + "{\"target\": \"subject\","
                            + "\"show_text\":\"Новая тема\", "
                            + "\"search_params\":"
                            + "{\"scope\": \"subject_thread_100502\"},"
                            + "\"search_text\": \"тема:(Новая тема)\",\"mail-count\": 1}]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testUnitedSuggest() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String producerURI = UpdateStoredRequest.API_SAVE_ROUTE + "*";
            String fsURI = "/filter_search?order=default&mdb=mdb200&suid=0" +
                "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";
            String defaultParams = "/api/async/mail/search?"
                + "mdb=mdb200&suid=0&first=0"
                + "&request=";

            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=0",
                MsearchProxyCluster.blackboxResponse(0L, 1L, "pg"));

            cluster.backend().add(
                doc(
                    "100502",
                    "\"thread_id\":100502," +
                        "\"received_date\":\"1234567892\"," +
                        "\"hdr_from\":\"\\\"Ivan\\\"<ivan@yandex.ru>\"," +
                        "\"hdr_subject\":" +
                        "\"Фото мастерская вучетича\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Фото мастерская вучетича\"",
                    "\"pure_body\":\"Привет vonidu хочу заказать портертъ!\""),
                doc(
                    "100504",
                    "\"thread_id\":100503," +
                        "\"received_date\":\"1234567894\","
                        + "\"hdr_from\":\"\\\"Ivan\\\"<ivan@yandex.ru>\","
                        + "\"folder_type\":\"inbox\","
                        + "\"fid\":\"1\","
                        + "\"hdr_subject\":\"Re: Vonidu " +
                        "привет в этом чатике\","
                        + "\"hdr_subject_normalized\":" +
                        "\"Vonidu привет в этом чатике\"",
                    ""));
            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            MailUser user =
                new MailUser(0, "pg", "united-suggest@yandex.ru");

            String[] emailsFrom = {
                "vasfonidu@yandex.ru",
                "vonidu@yandex.ru",
                "von@yandex.ru",
            };

            String[] emailsTo = {
                "vasfonidu@yandex.ru",
                "vonidu1@yandex.ru",
                "12vonidu@yandex.ru",
                "vonidu@yandex-team.ru",
                "von@yandex.ru",
                "vesta@lada.ru",
                "vesna@gmail.com"
            };

            String[] mids = {
                "100504", "100502",
                "100505", "100506", "100507",
                "100508", "100509", "100510",
                "100511", "100512", "100513",
                "100514"
            };

            int midInd = 2;
            for (String email: emailsFrom) {
                String displayName = RandomStringUtils.randomAlphanumeric(15);
                Email mail = new Email(mids[midInd])
                    .from(displayName, email)
                    .to(user.email(), user.email());

                async.indexDoc(cluster, user, mail);
                midInd ++;
            }

            for (String email: emailsTo) {
                String displayName = RandomStringUtils.randomAlphanumeric(15);
                Email mail = new Email(mids[midInd])
                    .from(user.email(), user.email())
                    .to(displayName, email);

                async.indexDoc(cluster, user, mail);
                midInd ++;
            }

            filterSearch(cluster, fsURI, mids[0], mids[1]);
            filterSearch(cluster, fsURI, mids[8], mids[3]);
            filterSearch(cluster, fsURI, mids[3]);

            final HttpHost backendHost = new HttpHost(
                "localhost",
                cluster.backend().indexerPort());

            SinxProxyHandler indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME);

            cluster.producer().add(
                producerURI,
                new StaticHttpResource(indexerProxy));

            String searchRequest1 = "vonidu";
            searchOk(
                cluster,
                client,
                defaultParams + searchRequest1);

            String searchRequest2 = "vonidu@yandex.ru";
            searchOk(
                cluster,
                client,
                defaultParams + searchRequest2);

            searchOk(
                cluster,
                client,
                defaultParams + "Привет vonidu");


            indexerProxy.waitForRequests(3, 10000);
            QueryConstructor qc = new QueryConstructor("/api/async/mail/suggest?");
            qc.append("mdb", "mdb200");
            qc.append("suid", "0");
            qc.append("request", "vonidu");
            LinkedHashMap<String, List<Map<?, ?>>> suggestsMap =
                new LinkedHashMap<>();
            List<Map<?, ?>> suggestsList = new ArrayList<>();
            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseTxt = CharsetUtils.toString(
                    response.getEntity());
                Object responseObj = ValueContentHandler.parse(responseTxt);

                List<?> suggestList = ValueUtils.asList(responseObj);
                for (Object suggestObj: suggestList) {
                    Map<?, ?> suggest = ValueUtils.asMap(suggestObj);
                    String target = ValueUtils.asString(suggest.get("target"));
                    Assert.assertNotNull(target);
                    if (!suggestsMap.containsKey(target)) {
                        suggestsMap.put(target, new ArrayList<>());
                    }

                    suggestsMap.get(target).add(suggest);
                    suggestsList.add(suggest);
                }
            }

            Assert.assertEquals(4, suggestsMap.size());
            List<String> targetList = new ArrayList<>(suggestsMap.keySet());
            Assert.assertEquals(targetList.get(0), "history");
            Assert.assertEquals(targetList.get(1), "contact");
            Assert.assertEquals(targetList.get(2), "subject");
            Assert.assertEquals(targetList.get(3), "mail");

            List<Map<?, ?>> contacts = suggestsMap.get("contact");
            Assert.assertEquals(2, contacts.size());
            String email = ValueUtils.asString(contacts.get(0).get("email"));
            Assert.assertEquals("vonidu@yandex-team.ru", email);
            email = ValueUtils.asString(contacts.get(1).get("email"));
            Assert.assertEquals("vonidu1@yandex.ru", email);

            List<Map<?, ?>> history = suggestsMap.get("history");
            Assert.assertEquals(3, history.size());
            String showText =
                ValueUtils.asString(history.get(0).get("show_text"));
            Assert.assertEquals("Привет vonidu", showText);
            showText =
                ValueUtils.asString(history.get(1).get("show_text"));
            Assert.assertEquals("vonidu@yandex.ru", showText);
            showText =
                ValueUtils.asString(history.get(2).get("show_text"));
            Assert.assertEquals("vonidu", showText);

            List<Map<?, ?>> subject = suggestsMap.get("subject");
            Assert.assertEquals(1, subject.size());
            showText = ValueUtils.asString(subject.get(0).get("show_text"));

            Assert.assertEquals("Vonidu привет в этом чатике", showText);

            qc.append("limit", "3");
            int total = 0;
            Set<String> targets = new LinkedHashSet<>();
            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseTxt = CharsetUtils.toString(
                    response.getEntity());
                Object responseObj = ValueContentHandler.parse(responseTxt);

                List<?> suggestList = ValueUtils.asList(responseObj);
                for (Object suggestObj: suggestList) {
                    targets.add(ValueUtils.asString(
                        ValueUtils.asMap(suggestObj).get("target")));
                    total += 1;
                }
            }


            //ignoring supplied limits
//            Assert.assertEquals(7, total);
            Assert.assertEquals(1, targets.size());

            // test additional from and to contancts suggests
            // if only one contact where returned
//            qc = new QueryConstructor("/api/async/mail/suggest?");
//            qc.append("mdb", "mdb200");
//            qc.append("suid", "0");
//            qc.append("request", "search vasfonidu");
//            qc.append("senderReceiver", "true");
//
//            suggestsMap = new LinkedHashMap<>();
//            suggestsList = new ArrayList<>();
//            try (CloseableHttpResponse response = client.execute(
//                cluster.proxy().host(), new HttpGet(qc.toString())))
//            {
//                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
//                String responseTxt = CharsetUtils.toString(
//                    response.getEntity());
//
//                Object responseObj = ValueContentHandler.parse(responseTxt);
//                List<?> suggestList = ValueUtils.asList(responseObj);
//
//                for (Object suggestObj: suggestList) {
//                    Map<?, ?> suggest = ValueUtils.asMap(suggestObj);
//                    String target = ValueUtils.asString(suggest.get("target"));
//                    Assert.assertNotNull(target);
//                    if (!suggestsMap.containsKey(target)) {
//                        suggestsMap.put(target, new ArrayList<>());
//                    }
//
//                    suggestsMap.get(target).add(suggest);
//                    suggestsList.add(suggest);
//                }
//            }
//
//            Assert.assertEquals(3, suggestsMap.size());
//            contacts = suggestsMap.get("contact");
//            Assert.assertEquals(1, contacts.size());
//            email = ValueUtils.asString(contacts.get(0).get("email"));
//            String search = ValueUtils.asString(
//                contacts.get(0).get("search_text"));
//
//            Assert.assertEquals("vasfonidu@yandex.ru", email);
//            Assert.assertEquals("search vasfonidu@yandex.ru", search);
//
//            List<Map<?,?>> senders = suggestsMap.get("sender");
//            email = ValueUtils.asString(senders.get(0).get("email"));
//            search = ValueUtils.asString(senders.get(0).get("search_text"));
//            Assert.assertEquals("vasfonidu@yandex.ru", email);
//            Assert.assertEquals("search от:vasfonidu@yandex.ru", search);
//
//            List<Map<?, ?>> receivers = suggestsMap.get("receiver");
//            email = ValueUtils.asString(receivers.get(0).get("email"));
//            search = ValueUtils.asString(receivers.get(0).get("search_text"));
//            Assert.assertEquals("vasfonidu@yandex.ru", email);
//            Assert.assertEquals("search кому:vasfonidu@yandex.ru", search);

            // test with folders and labels

            cluster.filterSearch().add(
                "/labels?caller=msearch&mdb=mdb200&suid=0",
                "{\"labels\":{\"5\":{\"type\":{\"title\":\"user\"},"
                    + "\"name\":\"Jira\"},"
                    + "\"9\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"draft\"}},"
                    + "\"8\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"important_label\"},"
                    + "\"name\":\"priority_high\"},"
                    + "\"102\":{\"name\":\"VOSTOK\","
                    + "\"creationTime\":\"1484658495\","
                    + "\"color\":\"3262267\",\"isUser\":true,"
                    + "\"isSystem\":false,"
                    + "\"type\":{\"code\":1,\"title\":\"user\"}},"
                    + "\"103\":{\"name\":\"мщение\","
                    + "\"creationTime\":\"1484658528\",\"color\":\"8176580\","
                    + "\"isUser\":true,\"isSystem\":false,\"type\":{\"code\":1,"
                    + "\"title\":\"user\"},\"symbolicName\":{\"code\":0,"
                    + "\"title\":\"\"},\"messagesCount\":1}"
                    + "}}");

            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=mdb200&suid=0",
                "{\"folders\":{\"1\":{\"name\":\"Inbox\",\"isUser\":false," +
                    "\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":1," +
                    "\"title\":\"inbox\"}},\"2\":{\"name\":\"Spam\"," +
                    "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":4," +
                    "\"title\":\"spam\"}},\"3\":{\"name\":\"Trash\"," +
                    "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":3," +
                    "\"title\":\"trash\"}},\"4\":{\"name\":\"Sent\"," +
                    "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":2," +
                    "\"title\":\"sent\"}},\"5\":{\"name\":\"Outbox\"," +
                    "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":6," +
                    "\"title\":\"outbox\"}}, " +
                    "\"7\":{\"name\":\"VOSHOD\"," +
                    "\"isUser\":true,\"isSystem\":false,\"type\":{\"code\":1,"
                    + "\"title\":\"user\"}}}}");

            qc = new QueryConstructor("/api/async/mail/suggest?");
            qc.append("mdb", "mdb200");
            qc.append("suid", "0");
            qc.append("request", "vo");

            suggestsMap = new LinkedHashMap<>();
            suggestsList = new ArrayList<>();
            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseTxt = CharsetUtils.toString(
                    response.getEntity());

                Object responseObj = ValueContentHandler.parse(responseTxt);
                List<?> suggestListObj = ValueUtils.asList(responseObj);

                for (Object suggestObj: suggestListObj) {
                    Map<?, ?> suggest = ValueUtils.asMap(suggestObj);
                    String target = ValueUtils.asString(suggest.get("target"));
                    Assert.assertNotNull(target);
                    if (!suggestsMap.containsKey(target)) {
                        suggestsMap.put(target, new ArrayList<>());
                    }

                    suggestsMap.get(target).add(suggest);
                    suggestsList.add(suggest);
                }
            }

            Assert.assertEquals(10, suggestsList.size());
            Map<?, ?> suggest = suggestsList.get(0);
            Assert.assertEquals(
                "history",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "Привет vonidu",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(1);
            Assert.assertEquals(
                "history",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "vonidu@yandex.ru",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(2);
            Assert.assertEquals(
                "history",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "vonidu",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(3);
            Assert.assertEquals(
                "contact",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "von@yandex.ru",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(4);
            Assert.assertEquals(
                "contact",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "vonidu@yandex-team.ru",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(5);
            Assert.assertEquals(
                "contact",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "vonidu1@yandex.ru",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(6);
            Assert.assertEquals(
                "subject",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "Vonidu привет в этом чатике",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(7);
            Assert.assertEquals(
                "folder",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "папка:VOSHOD",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(8);
            Assert.assertEquals(
                "label",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "метка:VOSTOK",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(9);
            Assert.assertEquals(
                "label",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "метка:мщение",
                ValueUtils.asString(suggest.get("search_text")));

            qc = new QueryConstructor("/api/async/mail/suggest?");
            qc.append("mdb", "mdb200");
            qc.append("suid", "0");
            qc.append("request", "v");

            suggestsMap = new LinkedHashMap<>();
            suggestsList = new ArrayList<>();
            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString() + "&hr")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseTxt = CharsetUtils.toString(
                    response.getEntity());
                Object responseObj = ValueContentHandler.parse(responseTxt);
                List<?> suggestListObj = ValueUtils.asList(responseObj);

                for (Object suggestObj: suggestListObj) {
                    Map<?, ?> suggestMapObj = ValueUtils.asMap(suggestObj);
                    String target = ValueUtils.asString(suggestMapObj.get("target"));
                    Assert.assertNotNull(target);
                    if (!suggestsMap.containsKey(target)) {
                        suggestsMap.put(target, new ArrayList<>());
                    }

                    suggestsMap.get(target).add(suggestMapObj);
                    suggestsList.add(suggestMapObj);
                }
            }

            System.out.println("Suggests: " + suggestsList);
            Assert.assertEquals(3, suggestsMap.size());
            Assert.assertEquals(2, suggestsMap.get("history").size());
            Assert.assertEquals(6, suggestsMap.get("contact").size());
            Assert.assertEquals(2, suggestsMap.get("subject").size());

            suggestsMap.clear();
            suggestsList.clear();

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(),
                new HttpGet(qc.toString() + "&hr&history=false")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseTxt = CharsetUtils.toString(
                    response.getEntity());

                Object responseObj = ValueContentHandler.parse(responseTxt);
                List<?> suggestListObj = ValueUtils.asList(responseObj);

                for (Object suggestObj: suggestListObj) {
                    Map<?, ?> suggestMapObj = ValueUtils.asMap(suggestObj);
                    String target = ValueUtils.asString(suggestMapObj.get("target"));
                    Assert.assertNotNull(target);
                    if (!suggestsMap.containsKey(target)) {
                        suggestsMap.put(target, new ArrayList<>());
                    }

                    suggestsMap.get(target).add(suggestMapObj);
                    suggestsList.add(suggestMapObj);
                }
            }

            Assert.assertNull(suggestsMap.get("history"));
            // test suggest contact and history on empty request
//            qc = new QueryConstructor("/api/async/mail/suggest?");
//            qc.append("mdb", "mdb200");
//            qc.append("suid", "0");
//            qc.append("request", "");
//
//            suggestsList = suggestResponse(cluster, client, qc.toString());
//            Set<String> targetsSet = new HashSet<>();
//            for (Map<?, ?> s: suggestsList) {
//                targetsSet.add(ValueUtils.asString(s.get("target")));
//            }
//
//            Assert.assertTrue(targetsSet.contains("history"));
//            Assert.assertTrue(targetsSet.contains("contact"));
//            Assert.assertEquals(2, targetsSet.size());

            //test multiword order. Suggests after space should be lower
            qc = new QueryConstructor("/api/async/mail/suggest?");
            qc.append("mdb", "mdb200");
            qc.append("suid", "0");
            qc.append("request", "Привет vonid");

            suggestsList = suggestResponse(cluster, client, qc.toString());

            suggest = suggestsList.get(0);
            Assert.assertEquals(
                "history",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "Привет vonidu",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(1);
            Assert.assertEquals(
                "subject",
                ValueUtils.asString(suggest.get("target")));
            Assert.assertEquals(
                "Vonidu привет в этом чатике",
                ValueUtils.asString(suggest.get("search_text")));

            suggest = suggestsList.get(2);
            Assert.assertEquals(
                "contact",
                ValueUtils.asString(suggest.get("target")));
            suggest = suggestsList.get(3);
            Assert.assertEquals(
                "contact",
                ValueUtils.asString(suggest.get("target")));
            suggest = suggestsList.get(4);
            Assert.assertEquals(
                "contact",
                ValueUtils.asString(suggest.get("target")));


            // test no translit on empty request
            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=0&suid=1",
                "{\"folders\":{" + systemFolders() + "}}");

            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            cluster.backend().add(
                "\"url\": \"reqs_0_хитрый запрос\","
                    + "\"request_raw\": \"хитрый запрос\","
                    + "\"request_date\": \"1485527972\","
                    + "\"request_normalized\": \"хитрый запрос\","
                    + "\"request_spaceless\": \"хитрыйзапрос\"");

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
                            + "\"show_text\": \"Մուտքային\", "
                            + "\"fid\": \"1\","
                            + "\"search_params\":{},"
                            + "\"search_text\": "
                            + "\"folder:Մուտքային\"}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testHightlighter() throws Exception {
        Assert.assertEquals(
            "Вот твоя <span class=\"msearch-highlight\">О</span>ценка",
            HtmlHighlighter.INSTANCE.highlight("о", "Вот твоя Оценка"));
    }

    @Test
    public void testHighlightIntervals() throws Exception {
        RequestMatcher highlighter = RequestMatcher.INSTANCE;

        List<String> request = highlighter.prepareRequest("Я пошел на ОзЕрцо");
        String text = "На озерце ничего я не нашел";
        List<IntInterval> intervals =
            highlighter.match(text, request, true);
        Assert.assertEquals(
            Arrays.asList(
                new IntInterval(0, 2),
                new IntInterval(3, 9),
                new IntInterval(17, 18)),
            intervals);

        request = highlighter.prepareRequest("yandex.ru name@");
        text = "name.surname@yandex.ru";
        intervals = highlighter.match(text, request, false, true);
        Assert.assertEquals(
            Collections.singletonList(new IntInterval(8, 22)),
            intervals);

        String value =
            HtmlHighlighter.INSTANCE.highlight(text, request, false,true)
                .asString();
        Assert.assertEquals(
            "name.sur<span class=\"msearch-highlight\">name@yandex.ru</span>",
            value);

        request = highlighter.prepareRequest("забористый забор");
        text = "забор забористый";
        value = HtmlHighlighter.INSTANCE.highlight(text, request, true)
            .asString();
        Assert.assertEquals(
            "<span class=\"msearch-highlight\">забор</span> <span "
                + "class=\"msearch-highlight\">забористый</span>",
            value);

        request = highlighter.prepareRequest("Огонь вода и медные отруби");
        text = "Ироды пожгли хаты медную кастрюлю, я это сотру бистро";
        intervals =
            highlighter.match(text, request, true);
        Assert.assertEquals(
            Collections.singletonList(
                new IntInterval(18, 24)),
            intervals);

        request = highlighter.prepareRequest("transaction");
        text = "Tinkoff_Transaction_document_92563231.pdf";
        intervals =
            highlighter.match(text, request, false, false);
        Assert.assertEquals(
            Collections.singletonList(
                new IntInterval(8, 19)),
            intervals);
    }

    @Test
    public void testHighlight() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2"
                + "&emails=getall&sid=2&uid=0",
                MsearchProxyCluster.blackboxResponse(0L, 1L, "pg"));

            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            cluster.backend().add(
                doc(
                    "100502",
                    "\"thread_id\":100502," +
                        "\"received_date\":\"1234567892\"," +
                        "\"hdr_from\":\"\\\"Derevo\\\" <derevo@sklad.ru>\"," +
                        "\"hdr_subject\":" +
                        "\"Привет vonidu хочу заказать портертъ!\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Привет vonidu хочу заказать красивый портертъ!\"",
                    "\"pure_body\":\"Портерт очень известной личности\""),
                "\"url\": \"reqs_359689700_хитрый запрос\","
                    + "\"request_raw\": \"хитрый запрос\","
                    + "\"request_date\": \"1485527972\","
                    + "\"request_normalized\": \"хитрый запрос\","
                    + "\"request_spaceless\": \"хитрыйзапрос\""
            );

            String[] names =
                new String[] {"Лидия Попело", "Владислав", "Дмитрий", "Олег", "buchungsbestaetigung"};
            String[] emails =
                new String[] {
                    "inmotion@yandex.ru",
                    "tabolin@yandex.ru",
                    "d-potapov@gmail.com",
                    "okk@kuznetsov.io",
                    "buchungsbestaetigung@bahn.de"};

            String[] mids = new String[] {"100500", "100501", "100502", "100503", "100504"};
            MailUser user =
                new MailUser(0, "pg", "united-suggest@mail.ru");

            for (int i =0; i < mids.length; i++) {
                async.indexDoc(cluster, user, new Email(mids[i])
                    .from(names[i], emails[i])
                    .to(user.email(), user.email()));
            }

            QueryConstructor qc = new QueryConstructor("/api/async/mail/suggest?");
            qc.append("mdb", "pg");
            qc.append("uid", "0");
            qc.append("request", "yandex");
            qc.append("highlight", "true");

            List<Map<?, ?>> suggestsList =
                suggestResponse(cluster, client, qc.toString());

            Assert.assertEquals(2, suggestsList.size());

            Map<?, ?> suggest = suggestsList.get(0);
            Assert.assertEquals(
                "\"Владислав\" tabolin@yandex.ru",
                suggest.get("show_text"));
            Assert.assertEquals(
                "tabolin@<span class=\"msearch-highlight\">yandex</span>.ru",
                suggest.get("email_highlighted"));
            Assert.assertEquals(
                "Владислав",
                suggest.get("display_name_highlighted"));
            Assert.assertEquals(
                "\"Владислав\" tabolin@<span class=\"msearch-highlight\">"
                + "yandex</span>.ru",
                suggest.get("show_text_highlighted"));

            suggest = suggestsList.get(1);
            Assert.assertEquals(
                "\"Лидия Попело\" inmotion@yandex.ru",
                suggest.get("show_text"));
            Assert.assertEquals(
                "Лидия Попело",
                suggest.get("display_name_highlighted"));
            Assert.assertEquals(
                "inmotion@<span class=\"msearch-highlight\">yandex</span>.ru",
                suggest.get("email_highlighted"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?mdb=pg&uid=0&request=kblbz&highlight");

            suggest = suggestsList.get(0);
            Assert.assertEquals(
                "\"Лидия Попело\" inmotion@yandex.ru",
                suggest.get("show_text"));
            Assert.assertEquals(
                "inmotion@yandex.ru",
                suggest.get("email_highlighted"));
            Assert.assertEquals(
                "<span class=\"msearch-highlight\">Лидия</span> Попело",
                suggest.get("display_name_highlighted"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?mdb=pg&uid=0&request=pfrfpfn&highlight");
            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                "Привет vonidu хочу <span class=\"msearch-highlight\">"
                    + "заказат</span>ь красивый портертъ!",
                suggest.get("show_text_highlighted"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=хитрый+запр&highlight");
            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                "<span class=\"msearch-highlight\">хитрый запр</span>ос",
                suggest.get("show_text_highlighted"));

            //test morph
            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=красивое&highlight");
            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                "Привет vonidu хочу заказать "
                    + "<span class=\"msearch-highlight\">красивый</span> портертъ!",
                suggest.get("show_text_highlighted"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=таболин&highlight");
            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                "\"Владислав\" tabolin@yandex.ru",
                suggest.get("show_text"));
            Assert.assertEquals(
                "<span class=\"msearch-highlight\">tabolin</span>@yandex.ru",
                suggest.get("email_highlighted"));

            cluster.filterSearch().add(
                "/labels?caller=msearch&mdb=pg&uid=0",
                "{\"labels\":{\"5\":{\"type\":{\"title\":\"user\"},"
                    + "\"name\":\"Jira\"},"
                    + "\"9\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"draft\"}},"
                    + "\"8\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"important_label\"},"
                    + "\"name\":\"priority_high\"},"
                    + "\"102\":{\"name\":\"VOSTOK\","
                    + "\"creationTime\":\"1484658495\","
                    + "\"color\":\"3262267\",\"isUser\":true,"
                    + "\"isSystem\":false,"
                    + "\"type\":{\"code\":1,\"title\":\"user\"}},"
                    + "\"103\":{\"name\":\"мщение\","
                    + "\"creationTime\":\"1484658528\",\"color\":\"8176580\","
                    + "\"isUser\":true,\"isSystem\":false,\"type\":{\"code\":1,"
                    + "\"title\":\"user\"},\"symbolicName\":{\"code\":0,"
                    + "\"title\":\"\"},\"messagesCount\":1}"
                    + "}}");

            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=0",
                "{\"folders\":{\"1\":{\"name\":\"Inbox\",\"isUser\":false," +
                    "\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":1," +
                    "\"title\":\"inbox\"}},\"2\":{\"name\":\"Spam\"," +
                    "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":4," +
                    "\"title\":\"spam\"}},\"3\":{\"name\":\"Trash\"," +
                    "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":3," +
                    "\"title\":\"trash\"}},\"4\":{\"name\":\"Sent\"," +
                    "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":2," +
                    "\"title\":\"sent\"}},\"5\":{\"name\":\"Outbox\"," +
                    "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
                    "\"title\":\"system\"},\"symbolicName\":{\"code\":6," +
                    "\"title\":\"outbox\"}}, " +
                    "\"7\":{\"name\":\"VOSHOD\"," +
                    "\"isUser\":true,\"isSystem\":false,\"type\":{\"code\":1,"
                    + "\"title\":\"user\"}}}}");

            cluster.backend().add(
                doc(
                    "100510",
                    "\"thread_id\":100510," +
                        "\"received_date\":\"1234567892\"," +
                        "\"hdr_from\":\"\\\"Derevo\\\" <derevo@sklad.ru>\"," +
                        "\"hdr_subject\":" +
                        "\"тема письма\"," +
                        "\"folder_type\":\"inbox\"," +
                        "\"fid\":\"1\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"тема письма\"",
                    "\"pure_body\":\"У письма хорошая тема\""));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=письма&highlight&type=mail");

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                "тема <span class=\"msearch-highlight\">письма</span>",
                suggest.get("show_text_highlighted"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=входя&highlight");
            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                    "<span class=\"msearch-highlight\">Входя</span>щие",
                suggest.get("show_text_highlighted"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=vost&highlight");
            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);
            Assert.assertEquals(
                "<span class=\"msearch-highlight\">VOST</span>OK",
                suggest.get("show_text_highlighted"));
            Assert.assertEquals("102", suggest.get("lid"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=хорошая&highlight"
            );

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);

            Assert.assertEquals("mail", suggest.get("target"));
            Assert.assertEquals("100510", suggest.get("mid"));
            Assert.assertEquals(
                "тема письма",
                suggest.get("show_text_highlighted"));
            Assert.assertEquals(
                "тема письма",
                suggest.get("show_text"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=hello+fro&highlight&lang=en"
            );

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);

            Assert.assertEquals("ql", suggest.get("target"));
            Assert.assertEquals(
                "<span class=\"msearch-highlight\">fro</span>m:",
                suggest.get("show_text_highlighted"));
            Assert.assertEquals(
                "from:",
                suggest.get("show_text"));
            Assert.assertEquals(
                "hello from:",
                suggest.get("search_text"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=год:2017+меся&highlight&lang=en"
            );

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);

            Assert.assertEquals("ql", suggest.get("target"));
            Assert.assertEquals(
                "<span class=\"msearch-highlight\">меся</span>ц:",
                suggest.get("show_text_highlighted"));
            Assert.assertEquals(
                "месяц:",
                suggest.get("show_text"));
            Assert.assertEquals(
                "год:2017 месяц:",
                suggest.get("search_text"));

            //test escape

        }
    }

    @Test
    public void testHighlightEscaping() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2"
                + "&emails=getall&sid=2&uid=0",
                MsearchProxyCluster.blackboxResponse(0L, 1L, "pg"));

            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            QueryConstructor qc = new QueryConstructor(
                "/api/async/mail/suggest?");
            qc.append("mdb", "pg");
            qc.append("uid", "0");
            qc.append("request", "yandex");
            qc.append("highlight", "true");

            List<Map<?, ?>> suggestsList;
            Map<?, ?> suggest;

            cluster.backend().add(
                doc(
                    "100513",
                    "\"thread_id\":100513," +
                        "\"received_date\":\"1234567892\"," +
                        "\"hdr_from\":\"\\\"Derevo\\\" <derevo@sklad.ru>\"," +
                        "\"hdr_subject\":" +
                        "\"Эскейпи нормально <script>alert('hi')</script> "
                        + "или страдай вечно\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Эскейпи нормально <script>alert('hi')</script> "
                        + "или страдай вечно\"",
                    "\"pure_body\":\"<p> Summer <br></p>\""));
            cluster.backend().add(
                doc(
                    "100514",
                    "\"thread_id\":100514," +
                        "\"received_date\":\"1234567892\"," +
                        "\"fid\":\"1\"," +
                        "\"folder_type\":\"inbox\"," +
                        "\"hdr_from\":\"\\\"Derevo\\\" <derevo@sklad.ru>\"," +
                        "\"hdr_subject\":\"<p> Summer <br></p>\"," +
                        "\"hdr_subject_normalized\":\"<p> Summer <br></p>\"",
                    "\"pure_body\":\"Summer soon\""));
            cluster.backend().add(
                "\"url\": \"reqs_0_<script>alert('hi')</script>\","
                    + "\"request_raw\": \"<script>alert('hi')</script>\","
                    + "\"request_date\": \"1485527972\","
                    + "\"request_normalized\": \"<script>alert('hi')</script>\","
                    + "\"request_spaceless\": \"<script>alert('hi')</script>\"");

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=&highlight&type=history"
            );

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);

            Assert.assertEquals("history", suggest.get("target"));
            Assert.assertEquals(
                "&lt;script&gt;alert('hi')&lt;/script&gt;",
                suggest.get("show_text_highlighted"));
            Assert.assertEquals(
                "<script>alert('hi')</script>",
                suggest.get("show_text"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=нормально&highlight"
            );

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);

            Assert.assertEquals("subject", suggest.get("target"));
            Assert.assertEquals(
                "Эскейпи <span class=\"msearch-highlight\">нормально</span> " +
                    "&lt;script&gt;alert('hi')&lt;/script&gt; или страдай вечно",
                suggest.get("show_text_highlighted"));
            Assert.assertEquals(
                "Эскейпи нормально <script>alert('hi')</script> "
                    + "или страдай вечно",
                suggest.get("show_text"));

            suggestsList = suggestResponse(
                cluster,
                client,
                "/api/async/mail/suggest?"
                    + "mdb=pg&uid=0&request=soon&highlight"
            );

            Assert.assertEquals(1, suggestsList.size());
            suggest = suggestsList.get(0);

            Assert.assertEquals("mail", suggest.get("target"));
            Assert.assertEquals(
                    "&lt;p&gt; Summer &lt;br&gt;&lt;/p&gt;",
                suggest.get("show_text_highlighted"));
            Assert.assertEquals(
                "<p> Summer <br></p>",
                suggest.get("show_text"));
        }
    }

    private List<Map<?, ?>> suggestResponse(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String request)
        throws Exception
    {
        List<Map<?, ?>> suggestsList = new ArrayList<>();
        try (CloseableHttpResponse response = client.execute(
            cluster.proxy().host(), new HttpGet(request)))
        {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            String responseTxt = CharsetUtils.toString(
                response.getEntity());
            Object responseObj = ValueContentHandler.parse(responseTxt);
            List<?> suggestListObj = ValueUtils.asList(responseObj);

            for (Object suggestObj: suggestListObj) {
                Map<?, ?> suggestMapObj = ValueUtils.asMap(suggestObj);
                String target =
                    ValueUtils.asString(suggestMapObj.get("target"));

                Assert.assertNotNull(target);

                suggestsList.add(suggestMapObj);
            }
        }

        return suggestsList;
    }
}
