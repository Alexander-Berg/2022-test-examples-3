package ru.yandex.msearch.proxy;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.parser.uri.CgiParams;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class DirectAndCountHandlersTest extends MsearchProxyTestBase {
    private String serp(final long size, final String... docs) {
        StringBuilder sb = new StringBuilder("{\"hitsArray\": ");
        sb.append(TestSearchBackend.concatDocs(docs));
        sb.append(", \"hitsCount\":");
        sb.append(size);
        sb.append("}");
        return sb.toString()
            .replaceAll("\"hid\":0}", "\"hid\":\"0\"}")
            .replaceAll("\"uid\":0,", "\"uid\":\"0\",");
    }

    @Test
    public void testCount() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            String[] docs = {
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"hdr_subject\":\"Миру мир и почту\"", ""),
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                        + ",\"received_date\":\"1234567891\""
                        + ",\"message_type\": \"4 people\""
                        + ",\"unread\": 1"
                        + ",\"hdr_subject\":\"Миру мир и почту\"", ""),
                doc(
                    "100502",
                    "\"hdr_to_normalized\":\"tmp@yandex.ru\""
                        + ",\"received_date\":\"1234567892\""
                        + ",\"message_type\": \"19 s_travel\""
                        + ",\"hdr_subject\":\"почту народу\"", ""),
                doc(
                    "100503",
                    "\"hdr_to_normalized\":\"ivan@yandex.ru\""
                        + ",\"received_date\":\"1234567893\""
                        + ",\"message_type\": \"4 people\""
                        + ",\"hdr_subject\":\"почту россии\"", ""),
                doc(
                    "100504",
                    "\"hdr_to_normalized\":\"other@yandex.ru\""
                        + ",\"received_date\":\"1234567894\""
                        + ",\"unread\": 1"
                        + ",\"message_type\": \"\""
                        + ",\"lids\": \"105\n101\n110\""
                        + ",\"hdr_subject\":\"почту россии\"", ""),
                doc(
                    "100505",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                        + ",\"received_date\":\"1234567891\""
                        + ",\"message_type\": \"4 people\""
                        + ",\"unread\": 1"
                        + ",\"user_type\": \"social\""
                        + ",\"hdr_subject\":\"Миру мир и почту\"", ""),
            };

            cluster.backend().add(docs);

            String[] sortedDocs = Arrays.copyOf(docs, docs.length);
            ArrayUtils.reverse(sortedDocs);

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/count?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&filters=trips,people,social")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[1, 2, 1]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/count?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&filters=trips,people,social&unread")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[0, 1, 1]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/count?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&filters=people&unread")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[1]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/count?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&filters=&unread")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.filterSearch().add(
                "/labels?caller=msearch&mdb=mdb200&suid=0",
                "{\"labels\":{\"5\":{\"type\":{\"title\":\"user\"},"
                    + "\"name\":\"Jira\"},"
                    + "\"9\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"draft\"}},"
                    + "\"8\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"important_label\"},"
                    + "\"name\":\"priority_high\"},"
                    + "\"102\":{\"name\":\"ОтСаши\","
                    + "\"creationTime\":\"1484658495\","
                    + "\"color\":\"3262267\",\"isUser\":true,"
                    + "\"isSystem\":false,"
                    + "\"type\":{\"code\":1,\"title\":\"user\"}},"
                    + "\"101\" : {\n"
                    + "         \"isUser\" : false,\n"
                    + "         \"creationTime\" : \"1482511637\",\n"
                    + "         \"name\" : \"system_hamon\",\n"
                    + "         \"isSystem\" : true,\n"
                    + "         \"symbolicName\" : {\n"
                    + "            \"title\" : \"hamon_label\",\n"
                    + "            \"code\" : 37\n"
                    + "         },\n"
                    + "         \"color\" : \"\",\n"
                    + "         \"type\" : {\n"
                    + "            \"title\" : \"system\",\n"
                    + "            \"code\" : 3\n"
                    + "         },\n"
                    + "         \"messagesCount\" : 133\n"
                    + "      },"
                    + "\"103\":{\"name\":\"ОтМаши\","
                    + "\"creationTime\":\"1484658528\",\"color\":\"8176580\","
                    + "\"isUser\":true,\"isSystem\":false,\"type\":{\"code\":1,"
                    + "\"title\":\"user\"},\"symbolicName\":{\"code\":0,"
                    + "\"title\":\"\"},\"messagesCount\":1}"
                    + "}}");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/count?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&filters=hamon&unread&exclude-trash")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[1]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDirect() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            String[] docs = {
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"hdr_subject\":\"Миру мир и почту\"", ""),
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                        + ",\"received_date\":\"1234567891\""
                        + ",\"message_type\": \"4 people\""
                        + ",\"hdr_subject\":\"Миру мир и почту\"", ""),
                doc(
                    "100502",
                    "\"hdr_to_normalized\":\"tmp@yandex.ru\""
                        + ",\"received_date\":\"1234567892\""
                        + ",\"message_type\": \"13 news\""
                        + ",\"hdr_subject\":\"почту народу\"", ""),
                doc(
                    "100503",
                    "\"hdr_to_normalized\":\"ivan@yandex.ru\""
                        + ",\"received_date\":\"1234567893\""
                        + ",\"message_type\": \"13 news\""
                        + ",\"hdr_subject\":\"почту россии\"", ""),
            };

            cluster.backend().add(docs);

            String[] sortedDocs = Arrays.copyOf(docs, docs.length);
            ArrayUtils.reverse(sortedDocs);

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/direct?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&request=мир")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String expected = serp(2, docs[1], docs[0]);
                String got = CharsetUtils.toString(response.getEntity());
                System.out.println("EXPECTED " + expected);
                System.out.println("GOT " + got);
                YandexAssert.check(
                    new JsonChecker(expected),
                    got);
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/direct?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&request=мир&message_type=4")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(1, docs[1])),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/direct?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&query-language&request=filter:people")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(1, docs[1])),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/direct?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&request=почту&count=0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(4)),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search/direct?"
                        + "mdb=mdb200&suid=0&first=0"
                        + "&request=почту&count=2&offset=2")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String expected =
                    serp(4, docs[3], docs[2]);
                String got = CharsetUtils.toString(response.getEntity());

                YandexAssert.check(
                    new JsonChecker(expected),
                    got);
            }
        }
    }

    @Test
    @Ignore
    public void test() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.producer().add(
                "/update?&prefix=0&service=change_log&msproxy=utype_change",
                new ExpectingHttpItem(new JsonChecker(
                    "{\"prefix\":0,"
                        + "\"AddIfNotExists\":true,"
                        + "\"docs\":["
                        + "{\"url\":\"usrtype_0_test@yandex.ru\","
                        + "\"user_types\":\"social\"}]}")));
            cluster.producer().add(
                "/notify?&prefix=0&service=change_log"
                    + "&msproxy=utype_change_reindex&email=test@yandex.ru",
                new NotifyHandler()
            );

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/usertype?"
                        + "uid=0&mdb=pg&from=test@yandex.ru&target=social"));
        }
    }

    private static class NotifyHandler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            CgiParams params = new CgiParams(request);
            assert params.getString("uid").equals("0");
            assert params.getString("change_type").equals("USER_TYPE_UPDATE");
            assert params.getString("hdr_from").equals("test@yandex.ru");
            assert params.getString("userType").equals("social");
            response.setStatusCode(HttpStatus.SC_OK);
        }
    }

    @Test
    public void testSubscriptions() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.backend().add(
                AsyncMailSearchTest.doc(
                    "100500",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"hdr_from\":\"\\\"potapovd@gmail.com\\\" "
                        + "<potapovd@gmail.com>\n\""
                        + ",\"hdr_from_normalized\":\"potapovd@gmail.com\""
                        + ",\"lids\":\"FAKE_SEEN_LBL\n\""
                        + ",\"message_type\":\"3 social 4 people\""
                        + ",\"clicks_total_count\":10"
                        + ",\"folder_type\":\"inbox\""
                        + ",\"received_date\":\"2092271312\""
                        + ",\"hdr_subject\":\"Миру мир\"",
                    "\"pure_body\":\"иди\""),
                AsyncMailSearchTest.doc(
                    "100501",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"hdr_from\":\"\\\"potapovd@gmail.com\\\" "
                        + "<potapovd@gmail.com>\""
                        + ",\"hdr_from_normalized\":\"potapovd@gmail.com\""
                        + ",\"lids\":\"\n\""
                        + ",\"folder_type\":\"inbox\""
                        + ",\"message_type\":\"18 s_social 4 people\""
                        + ",\"clicks_total_count\":10"
                        + ",\"received_date\":\"2092271313\""
                        + ",\"hdr_subject\":\"Миру мир\"",
                    "\"pure_body\":\"иди\""));
            cluster.backend().add(
                AsyncMailSearchTest.doc(
                    "100502",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"hdr_from\":\"\\\"potapovd@gmail.com\\\" "
                        + "<potapovd@gmail.com>\""
                        + ",\"hdr_from_normalized\":\"potapovd@gmail.com\""
                        + ",\"lids\":\"FAKE_SEEN_LBL\n\""
                        + ",\"folder_type\":\"inbox\""
                        + ",\"message_type\":\"3 social\""
                        + ",\"received_date\":\"2092271314\""
                        + ",\"hdr_subject\":\"Миру мир\"",
                    "\"pure_body\":\"иди\""),
                AsyncMailSearchTest.doc(
                    "100503",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"hdr_from\":\"\\\"Vasya Pup\\\" <vasya@yandex.ru>\""
                        + ",\"hdr_from_normalized\":\"vasya@yandex.ru\""
                        + ",\"lids\":\"FAKE_SEEN_LBL\n\""
                        + ",\"message_type\":\"4 people\""
                        + ",\"received_date\":\"2092271315\""
                        + ",\"folder_type\":\"inbox\""
                        + ",\"hdr_subject\":\"Миру мир\"",
                    "\"pure_body\":\"иди\""),
                // next to mails are too old more than year ago
                AsyncMailSearchTest.doc(
                    "100504",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"hdr_from\":\"\\\"Vasya Pup\\\" <vasya@yandex.ru>\""
                        + ",\"hdr_from_normalized\":\"vasya@yandex.ru\""
                        + ",\"lids\":\"FAKE_SEEN_LBL\n\""
                        + ",\"message_type\":\"4 people\""
                        + ",\"folder_type\":\"inbox\""
                        + ",\"received_date\":\"1402271316\""
                        + ",\"hdr_subject\":\"Миру мир\"",
                    "\"pure_body\":\"иди\""),
                AsyncMailSearchTest.doc(
                    "100505",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"hdr_from\":\"\\\"Vasya Pup\\\" <vasya@yandex.ru>\""
                        + ",\"hdr_from_normalized\":\"vasya@yandex.ru\""
                        + ",\"lids\":\"FAKE_SEEN_LBL\n\""
                        + ",\"message_type\":\"4 people\""
                        + ",\"received_date\":\"1402271313\""
                        + ",\"folder_type\":\"inbox\""
                        + ",\"hdr_subject\":\"Миру мир\"",
                    "\"pure_body\":\"иди\""),
                AsyncMailSearchTest.doc(
                    "100506",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"hdr_from\":\"\\\"notification+kr4n2kkybnwn"
                        + "@facebookmail.com\n\\\" "
                        + "<notification+kr4n2kkybnwn@facebookmail.com>\""
                        + ",\"hdr_from_normalized\":"
                        + "\"notification+kr4n2kkybnwn@facebookmail.com\n\""
                        + ",\"lids\":\"FAKE_SEEN_LBL\n\""
                        + ",\"folder_type\":\"inbox\""
                        + ",\"message_type\":\"7 notification\""
                        + ",\"received_date\":\"2092271317\""
                        + ",\"hdr_subject\":\"Миру мир\"",
                    "\"pure_body\":\"иди\""),
                AsyncMailSearchTest.doc(
                    "100507",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"hdr_from\":\"\\\"notification+asfgqerg"
                        + "@facebookmail.com\\\" "
                        + "<notification+asfgqerg@facebookmail.com>\""
                        + ",\"hdr_from_normalized\":"
                        + "\"notification+asfgqerg@facebookmail.com\""
                        + ",\"lids\":\"FAKE_SEEN_LBL\n\""
                        + ",\"message_type\":\"7 notification\""
                        + ",\"folder_type\":\"inbox\""
                        + ",\"clicks_total_count\":1"
                        + ",\"received_date\":\"2092271318\""
                        + ",\"hdr_subject\":\"Миру мир\"",
                    "\"pure_body\":\"иди\"")
            );

            cluster.producer().add(
                "/_status?service=change_log&prefix=0"
                    + "&allow_cached&all&json-type=dollar",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":100500}]")));

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/subscriptions?minTotal=0&fast=false"
                        + "&uid=0&mdb=pg&types=social,people&full&request=")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"from\":\"vasya@yandex.ru\",\"percentShow\":0,"
                            + "\"searchEmail\":\"vasya@yandex.ru\","
                            + "\"mid\":\"100503\","
                            + "\"displayName\":\"Vasya Pup\","
                            + "\"percentWithFlagSeen\":100, \"show\": 0,"
                            + "\"seenFlag\":1,"
                            + "\"total\":1,\"type\":\"4\"},"
                            + "{\"from\":\"potapovd@gmail.com\",\"percentShow\":50.0,"
                            + "\"mid\":\"100502\","
                            + "\"displayName\":\"potapovd\","
                            + "\"searchEmail\":\"potapovd@gmail.com\","
                            + "\"seenFlag\":2,"
                            + "\"total\":2,\"type\":\"3\"," +
                            "\"percentWithFlagSeen\":100.0, \"show\": 1},"
                            + "{\"from\":\"potapovd@gmail.com\",\"percentShow\":100.0,"
                            + "\"displayName\":\"potapovd\","
                            + "\"mid\":\"100501\","
                            + "\"seenFlag\":1,"
                            + "\"searchEmail\":\"potapovd@gmail.com\","
                            + "\"percentWithFlagSeen\":50.0, \"show\": 2,"
                            + "\"total\":2,\"type\":\"4\"}"
                            + "]"
                    ), CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/subscriptions?uid=0&mdb=pg&fast=false"
                        + "&types=social,people&full&request=&minTotal=2")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "[{\"from\":\"potapovd@gmail.com\","
                            + "\"percentShow\":50.0,"
                            + "\"total\":2,\"type\":\"3\","
                            + "\"searchEmail\":\"potapovd@gmail.com\","
                            + "\"displayName\":\"potapovd\","
                            + "\"mid\":\"100502\","
                            + "\"seenFlag\":2,"
                            + "\"percentWithFlagSeen\":100.0, \"show\": 1},"
                            + "{\"from\":\"potapovd@gmail.com\","
                            + "\"percentShow\":100.0,"
                            + "\"displayName\":\"potapovd\","
                            + "\"searchEmail\":\"potapovd@gmail.com\","
                            + "\"mid\":\"100501\","
                            + "\"seenFlag\":1,"
                            + "\"percentWithFlagSeen\":50.0, \"show\": 2,"
                            + "\"total\":2,\"type\":\"4\"}]"
                    ), CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/subscriptions?uid=0&mdb=pg"
                        + "&fast=false&types=social,people&minTotal=0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"email\":\"vasya@yandex.ru\","
                            + "\"searchEmail\":\"vasya@yandex.ru\","
                            + "\"displayName\":\"Vasya Pup\","
                            + "\"mid\":\"100503\","
                            + "\"readFrequency\":0.0, "
                            + "\"type\":\"4\"},"
                            + "{\"email\":\"potapovd@gmail.com\","
                            + "\"type\":\"3\","
                            + "\"searchEmail\":\"potapovd@gmail.com\","
                            + "\"displayName\":\"potapovd\","
                            + "\"mid\":\"100502\","
                            + "\"readFrequency\":0.5},"
                            + "{\"email\":\"potapovd@gmail.com\","
                            + "\"displayName\":\"potapovd\","
                            + "\"readFrequency\":1.0, "
                            + "\"searchEmail\":\"potapovd@gmail.com\","
                            + "\"mid\":\"100501\","
                            + "\"type\":\"4\"}"
                            + "]"
                    ), CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/subscriptions?uid=0&mdb=pg"
                        + "&fast=false&types=notification&minTotal=0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(
                        "[{\"email\":\"notification@facebookmail.com\","
                            + "\"type\":\"7\","
                            + "\"displayName\":\"Facebook\","
                            + "\"mid\":\"100507\","
                            + "\"searchEmail\":"
                            + "\"notification*@facebookmail.com\","
                            + "\"readFrequency\":0.5}]"
                    ), CharsetUtils.toString(response.getEntity()));
            }

            String producerUpdateUri =
                "/update?&prefix=0&service=change_log"
                    + "&caller=subscriptions_reindex&subsMonth=8_2080";
            String deleteBase =
                "/delete?&prefix=0&service=change_log&caller="
                    + "subscriptions_reindex&text=url:subs_0_*"
                    + "+AND+subs_received_month:";
            cluster.producer().add(
                deleteBase + "3489685200",
                (rq,rsp,ctx) -> {
                    if (!RequestHandlerMapper.GET.equalsIgnoreCase(
                        rq.getRequestLine().getMethod()))
                    {
                        throw new NotImplementedException(
                            "Unknown method "
                                + rq.getRequestLine().getMethod()
                                + ' '
                                + rq.getRequestLine().getUri());
                    }

                    rsp.setStatusCode(HttpStatus.SC_OK);
                });
            cluster.producer().add(deleteBase + "3487006800",HttpStatus.SC_OK);
            cluster.producer().add(deleteBase + "3484414800",HttpStatus.SC_OK);
            cluster.producer().add(deleteBase + "3481736400",HttpStatus.SC_OK);
            cluster.producer().add(deleteBase + "3479144400",HttpStatus.SC_OK);
            cluster.producer().add(deleteBase + "3473960400",HttpStatus.SC_OK);
            cluster.producer().add(deleteBase + "3471282000",HttpStatus.SC_OK);
            cluster.producer().add(deleteBase + "3468603600",HttpStatus.SC_OK);
            cluster.producer().add(deleteBase + "3466011600",HttpStatus.SC_OK);
            cluster.producer().add(deleteBase + "3463333200",HttpStatus.SC_OK);
            cluster.producer().add(deleteBase + "3460741200",HttpStatus.SC_OK);
            cluster.producer().add(
                deleteBase + "3476466000",HttpStatus.SC_OK);

            cluster.producer().add(
                producerUpdateUri,
                new ExpectingHttpItem(
                    new JsonChecker("{\"AddIfNotExists\": true,"
                        + "\"docs\": [{\"subs_email\": \"vasya@yandex.ru\","
                        + "\"subs_names\": \"Vasya Pup\","
                        + "\"subs_read_types\":\"\","
                        + "\"subs_received_month\": 3489685200,"
                        + "\"subs_received_types\": \"4\\t1\","
                        + "\"url\": \"subs_0_vasya@yandex.ru_3489685200\""
                        + "},{\"subs_email\": \"potapovd@gmail.com\","
                        + "\"subs_read_types\": \"4\\t2\\n18\\t1\","
                        + "\"subs_names\": \"potapovd\","
                        + "\"subs_received_month\": 3489685200,"
                        + "\"subs_received_types\": \"4\\t2\\n18\\t1\","
                        + "\"url\": \"subs_0_potapovd@gmail.com_3489685200\""
                        + "}],\"prefix\": \"0\"}")));
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/subscriptions?uid=0&mdb=pg"
                        + "&types=people,s_social&reindex&minTotal=0"
                        + "&startTs=3492271400000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
        }
    }

    @Test
    public void testFastSubscriptions() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.backend().add(
                "\"url\": \"subs_0_deals@onetwotrip.com_1493586000\","
                    + "\"subs_email\": \"deals@onetwotrip.com\","
                    + "\"subs_received_month\": \"1493586000\","
                    + "\"subs_read_types\": \"13\\t4\","
                    + "\"subs_received_types\": \"13\\t4\"");

            cluster.backend().add(
                "\"url\": \"subs_0_deals@onetwotrip.com_1493486000\","
                    + "\"subs_email\": \"deals@onetwotrip.com\","
                    + "\"subs_names\": \"OneTwoTrip\","
                    + "\"subs_received_month\": \"1493486000\","
                    + "\"subs_received_types\": \"13\\t10\"");

            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":-1}]");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/subscriptions?uid=0&mdb=pg"
                        + "&types=people,news&minTotal=0&fromTs=1493000000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"email\":\"deals@onetwotrip.com\","
                            + "\"displayName\":\"deals\",\"mid\":\"\","
                            + "\"searchEmail\":\"\","
                            + "\"readFrequency\":0.28571428571428575,"
                            + "\"type\":\"13\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // test negative values f**up
            cluster.producer().add(
                "/_status?service=change_log&prefix=73001211&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":-1}]");

            cluster.backend().add(
                new LongPrefix(73001211L),
                "\"url\": \"subs_73001211_"
                    + "aliexpressalert@aliexpress.com_1525122000\","
                + "\"subs_email\": \"aliexpressalert@aliexpress.com\","
                + "\"subs_received_month\": \"1525122000\","
                + "\"subs_last_received_date\": \"1525291817\",\n"
                + "\"subs_names\": \"AliExpress Price Alert\",\n"
                + "\"subs_read_types\": \"55\\t1\","
                + "\"subs_received_types\": "
                + "\"55\\t1\\n23\\t1\\n67\\t1\\n71\\t1\\n65\\t1\"");
            cluster.backend().add(
                new LongPrefix(73001211),
                "\"url\": "
                + "\"subs_73001211_aliexpressalert@aliexpress.com_1522530000\","
                + "\"subs_email\": \"aliexpressalert@aliexpress.com\","
                + "\"subs_received_month\": \"1522530000\","
                + "\"subs_last_received_date\": \"1525020027\",\n"
                + "\"subs_names\": \"AliExpress Price Alert\",\n"
                + "\"subs_received_types\": "
                + "\"55\\t-3\\n66\\t-2\\n23\\t-3\\n62\\t-2\"");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/subscriptions?uid=73001211&mdb=pg"
                        + "&types=trust_5&minTotal=0&fromTs=1493000000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"email\":\"aliexpressalert@aliexpress.com\","
                            + "\"displayName\":\"AliExpress Price Alert\","
                            + "\"mid\":\"\","
                            + "\"searchEmail\":\"\","
                            + "\"readFrequency\":1.0,"
                            + "\"type\":\"55\"}]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClassification() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            String uri = "/_status?service=change_log&prefix=0&allow_cached"
                + ALL_DOLLARS;
            cluster.producer().add(uri, "[{\"localhost\":-1}]");
            cluster.backend().add(
                new LongPrefix(0L),
                doc(
                    "100501",
                    "\"clicks_total_count\":1,\"clicks_serp_count\": 1,"
                    + "\"folder_type\": \"user\"",
                    "\"hid\": 0"),
                doc(
                    "100502",
                    "\"hdr_from\":\"a@b.ru\",\"folder_type\": \"inbox\"",
                    "\"hid\": 0"),
                doc(
                    "100503",
                    "\"clicks_total_count\":4, \"folder_type\": \"user\"",
                    "\"hid\": 0"),
                doc(
                    "100504",
                    "\"clicks_total_count\":2,\"clicks_serp_count\": 2"
                        + ",\"folder_type\": \"user\"",
                    "\"hid\": 0"),
                doc(
                    "100505",
                    "\"clicks_serp_count\": 2,\"folder_type\": \"user\"",
                    "\"hid\": 0"),
                doc(
                    "100507",
                    "\"clicks_serp_count\": 2, \"folder_type\": \"draft\"",
                    "\"hid\": 0"),
                doc(
                    "100508",
                    "\"clicks_serp_count\": 2, \"folder_type\": \"spam\"",
                    "\"hid\": 0"),
                doc(
                    "100509",
                    "\"clicks_serp_count\": 2, \"folder_type\": \"unsubscribe\"",
                    "\"hid\": 0"),
                doc(
                    "100510",
                    "\"clicks_serp_count\": 2, \"folder_type\": \"archive\"",
                    "\"hid\": 0"),
                doc(
                    "100511",
                    "\"clicks_serp_count\": 2, \"folder_type\": \"trash\"",
                    "\"hid\": 0"),
                doc(
                    "100512",
                    "\"clicks_serp_count\": 4, \"folder_type\": \"trash\"",
                    "\"hid\": 0"),
                doc(
                    "100513",
                    "\"clicks_serp_count\": 2",
                    "\"hid\": 0"),
                doc(
                    "100514",
                    "\"clicks_serp_count\": 2,\"folder_type\": \"inbox\"",
                    "\"hid\": 0")
                );
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/classification?uid=0"
                        + "&mids=100501,100502,100503,100504,100505,100506,100514")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[\"100501\",\"100502\",\"100505\", "
                            + "\"100506\", \"100514\"]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            //test post
            HttpPost post =
                new HttpPost(
                    cluster.proxy().host()
                        + "/api/async/mail/classification?uid=0");
            post.setEntity(new StringEntity(
                "[100501,100502,100503,100504,100505,100506,100514]"));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[\"100501\",\"100502\",\"100505\", "
                            + "\"100506\", \"100514\"]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testZeroSuggest() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":-1}]");
            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            cluster.backend().add(
                "\"url\": \"senders_uid_0_info@email.belkacar.ru\",\n" +
                    "\"senders_uid\": \"0\",\n" +
                    "\"senders_last_contacted\": \"1504230276\",\n" +
                    "\"senders_received_count\": \"2\",\n" +
                    "\"senders_names\": \"Belkacar\",\n" +
                    "\"senders_lcn\": \"2913\"");


            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/suggest/zero?uid=0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"people\":[],"
                            + "\"other\":[{\"target\":\"contact\","
                            + "\"show_text\":\"\\\"Belkacar\\\" info@email"
                            + ".belkacar.ru\",\"search_text\":\"info@email"
                            + ".belkacar.ru\",\"display_name\":\"Belkacar\","
                            + "\"email\":\"info@email.belkacar.ru\","
                            + "\"unread_cnt\":0,\"search_params\":{},"
                            + "\"email_highlighted\":\"info@email.belkacar.ru\""
                            + ",\"display_name_highlighted\":\"Belkacar\","
                            + "\"show_text_highlighted\":\"\\\"Belkacar\\\" "
                            + "info@email.belkacar.ru\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFuritaHandler() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            String uri = "/_status?service=change_log&prefix=0&allow_cached"
                + ALL_DOLLARS;
            cluster.producer().add(uri, "[{\"localhost\":-1}]");
            cluster.backend().add(
                new LongPrefix(0L),
                doc(
                    "100501",
                    "\"hdr_from\":\"vasya@yandex.ru\","
                        + "\"folder_type\": \"inbox\","
                        + "\"received_date\": 0",
                    "\"hid\": 0"),
                doc(
                    "100502",
                    "\"hdr_from\":\"petya@yandex.ru\","
                        + "\"folder_type\": \"sent\","
                        + "\"received_date\": 1,"
                        + "\"fid\": \"4\"",
                    "\"hid\": 0", "\"hid\": 1"),
                doc(
                    "100503",
                    "\"hdr_from\":\"vasya@gmail.com\","
                        + "\"folder_type\": \"inbox\","
                        + "\"received_date\": 2,"
                        + "\"fid\": \"1\"",
                    "\"hid\": 0"),
                doc(
                    "100504",
                    "\"hdr_from\":\"vasya@yandex.ru\","
                        + "\"received_date\": 3,"
                        + "\"folder_type\": \"spam\"",
                    "\"hid\": 0"));

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/furita?uid=0&request="
                        + "(hdr_from%3A(*vasya*)%20OR"
                        + "%20hdr_from_keyword%3A(*vasya*))&first=0&count=5000")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"details\":{\"crc32\":\"0\","
                            + "\"search-limits\":{\"offset\":0,\"length\":5000},"
                            + "\"search-options\":{"
                            + "\"request\":\"(hdr_from:(*vasya*) "
                            + "OR hdr_from_keyword:(*vasya*))\"},"
                            + "\"total-found\":3},"
                            + "\"envelopes\":["
                            + "\"100504\", \"100503\", \"100501\"]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/furita?uid=0&get=mid&request="
                        + "(hdr_from%3A(*vasya*)%20OR"
                        + "%20hdr_from_keyword%3A(*vasya*))&first=0&count=5")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"details\":{\"crc32\":\"0\","
                            + "\"search-limits\":{\"offset\":0,\"length\":5},"
                            + "\"search-options\":{"
                            + "\"request\":\"(hdr_from:(*vasya*) "
                            + "OR hdr_from_keyword:(*vasya*))\"},"
                            + "\"total-found\":3},"
                            + "\"envelopes\":["
                            + "\"100504\",\"100503\",\"100501\"]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/furita?uid=0&get=mid&request="
                        + "(hdr_from%3A(*vasya*)%20OR"
                        + "%20hdr_from_keyword%3A(*vasya*))&first=2&count=5")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"details\":{\"crc32\":\"0\","
                            + "\"search-limits\":{\"offset\":2,\"length\":5},"
                            + "\"search-options\":{"
                            + "\"request\":\"(hdr_from:(*vasya*) "
                            + "OR hdr_from_keyword:(*vasya*))\"},"
                            + "\"total-found\":3},"
                            + "\"envelopes\":[\"100501\"]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}
