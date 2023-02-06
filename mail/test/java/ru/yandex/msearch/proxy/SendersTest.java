package ru.yandex.msearch.proxy;

import java.nio.file.Files;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class SendersTest extends MsearchProxyTestBase {
    private static String SENDERS_DOC = sendersDoc(
        "0",
        "gifts@onetwotrip.com",
        "\"senders_last_contacted\": \"1484216433\",\n"
            + "\"senders_received_count\": \"3\",\n"
            + "\"senders_from_read_count\": \"2\",\n"
            + "\"senders_names\": \"OneTwoTrip!\",\n"
            + "\"senders_lcn\": \"1099\"\n");

    private static String SENDERS_DOMAIN_DOC = sendersDomainDoc(
        "0",
        "onetwotrip.com",
        "\"senders_last_contacted\": \"1500371896\","
            + "\"senders_received_count\": \"128\",\n"
            + "\"senders_from_read_count\": \"50\",\n"
            + "\"senders_lcn\": \"2534\"");

    private static String sendersDoc(
        final String uid,
        final String from,
        final String parts)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\"url\": \"senders_uid_");
        sb.append(uid);
        sb.append('_');
        sb.append(from);
        sb.append("\",");
        sb.append("\"senders_uid\": \"");
        sb.append(uid);
        sb.append("\",");
        sb.append(parts);
        return sb.toString();
    }

    private static String sendersDomainDoc(
        final String uid,
        final String domain,
        final String parts)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\"url\": \"senders_domain_uid_");
        sb.append(uid);
        sb.append('_');
        sb.append(domain);
        sb.append("\",");
        sb.append("\"senders_domain_uid\": \"");
        sb.append(uid);
        sb.append("\",");
        sb.append(parts);
        return sb.toString();
    }

    @Test
    public void testSenders() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(
                    this,
                    MsearchProxyCluster.SOSEARCH_CONFIG,
                    new MsearchProxyCluster.MproxyClusterContext().producer());
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add("/*", "[{\"localhost\":6}]");
            cluster.start();
            final long uid = 0L;

            cluster.backend().add(
                SENDERS_DOC,
                SENDERS_DOMAIN_DOC);

            cluster.backend().add(
                new LongPrefix(3L),
                sendersDoc(
                    "3",
                    "gifts@onetwotrip.com",
                    "\"senders_last_contacted\": \"1484216453\",\n"
                    + "\"senders_received_count\": \"4\",\n"
                    + "\"senders_from_read_count\": \"6\",\n"
                    + "\"senders_names\": \"OneTwoTrip!\",\n"
                    + "\"senders_lcn\": \"1098\",\n"
                    + "\"senders_sent_count\":\"15\""),
                sendersDomainDoc(
                    "3",
                    "onetwotrip.com",
                    "\"senders_last_contacted\": \"1500371806\","
                    + "\"senders_received_count\": \"12\",\n"
                    + "\"senders_from_read_count\": \"5\",\n"
                    + "\"senders_lcn\": \"253\""));
            cluster.backend().add(
                "\"url\": \"user_ml_features_uid_0\",\n"
                + Files.readString(resource("user-ml-features-doc.json")));

            HttpPost post = new HttpPost(
                cluster.proxy().host()
                    + "/api/async/senders?names-max=1&sender-uid=0");

            post.setEntity(
                new StringEntity(
                    "{\"requests\":[{\"uid\":\"0\",\"suid\":1,"
                    + "\"email\":\"gifts@onetwotrip.com\","
                    + "\"domain\":\"onetwotrip.com\"},"
                    + "{\"uid\":\"3\",\"suid\":\"4\","
                    + "\"email\":\"gifts@onetwotrip.com\","
                    + "\"domain\":\"onetwotrip.com\"}]}"));

            String expected =
                "{\"results\":[{\"request\":{"
                + "\"suid\":1,\"uid\":0,"
                + "\"email\":\"gifts@onetwotrip.com\",\"domain\":\""
                + "onetwotrip.com\"},\"response\":{\"sent_count\":0,"
                + "\"received_count\":3,\"last_contacted\":1484216433,"
                + "\"names\":[\"OneTwoTrip!\"],\"sender_type\":\"from\","
                + "\"domain_send_count\":0,"
                + "\"received_read_count\":2, "
                + "\"domain_received_read_count\":50,"
                + "\"domain_received_count\":128,"
                + "\"domain_sender_type\":\"from\","
                + Files.readString(resource("user-ml-features-doc.json"))
                + "}},{\"request\":{"
                + "\"suid\":4,\"uid\":3,"
                + "\"email\":\"gifts@onetwotrip.com\",\"domain\":\""
                + "onetwotrip.com\"},\"response\":{\"sent_count\":15,"
                + "\"received_count\":4,\"last_contacted\":1484216453,"
                + "\"names\":[\"OneTwoTrip!\"],\"sender_type\":\"from\","
                + "\"domain_send_count\":0,"
                + "\"received_read_count\":6, "
                + "\"domain_received_read_count\":5,"
                + "\"domain_received_count\":12,"
                + "\"domain_sender_type\":\"from\"}}],"
                + Files.readString(resource("user-ml-features-doc.json"))
                    .replaceAll("\"user_ml_", "\"sender_ml_")
                + '}';
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPersonalFilter() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(
                    this,
                    MsearchProxyCluster.SOSEARCH_CONFIG,
                    new MsearchProxyCluster.MproxyClusterContext().producer());
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add("/*", "[{\"localhost\":6}]");
            cluster.start();
            final long uid = 0L;

            cluster.backend().add(
                SENDERS_DOC,
                "\"url\":\"pfilters4_0_gifts@onetwotrip.com/ott.net\",\n"
                + "\"pfilters_last_type\":\"ham\",\n"
                + "\"pfilters_last_timestamp\":4375,\n"
                + "\"pfilters_spams\":3,\n"
                + "\"pfilters_hams\":1",
                "\"url\": \"user_ml_features_uid_0\",\n"
                + Files.readString(resource("user-ml-features-doc.json")),
                "\"url\":\"1\",\"msg_id\":\"123\",\"folder_type\":\"inbox\"",
                "\"url\":\"2\",\"msg_id\":\"456\",\"folder_type\":\"spam\"");

            HttpPost post = new HttpPost(
                cluster.proxy().host()
                    + "/api/async/senders?names-max=1");

            post.setEntity(new StringEntity(
                "{\"requests\":["
                + "{\"uid\":\"0\",\"suid\":1,"
                + "\"email\":\"gifts@onetwotrip.com\","
                + "\"sender-host\":\"ott.net\","
                + "\"in-reply-to\":[\"345\",\"456\"]}]}"));

            String expected =
                "{\"results\":[{\"request\":{"
                + "\"suid\":1,\"uid\":0,"
                + "\"email\":\"gifts@onetwotrip.com\","
                + "\"sender-host\":\"ott.net\","
                + "\"in-reply-to\":[\"345\",\"456\"]},"
                + "\"response\":{\"sent_count\":0,"
                + "\"received_count\":3,\"last_contacted\":1484216433,"
                + "\"names\":[\"OneTwoTrip!\"],\"sender_type\":\"from\","
                + "\"received_read_count\":2,"
                + "\"pfilters_last_type\":\"ham\",\"pfilters_last_time\":"
                + "4375,\"pfilters_spams\":3,\"pfilters_hams\":1,"
                + "\"pfilters_sender_type\":\"from\","
                + Files.readString(resource("user-ml-features-doc.json"))
                + "}}]}";
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testListOwnerPersonalFilter() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(
                    this,
                    MsearchProxyCluster.SOSEARCH_CONFIG,
                    new MsearchProxyCluster.MproxyClusterContext().producer());
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add("/*", "[{\"localhost\":6}]");
            cluster.start();
            final long uid = 0L;

            cluster.backend().add(
                "\"url\":\"senders_list_owner_uid_0_gifts@onetwotrip.com\","
                + "\"senders_uid\":0,"
                + "\"senders_last_contacted\": \"1484216433\","
                + "\"senders_received_count\": \"3\","
                + "\"senders_from_read_count\": \"2\","
                + "\"senders_names\": \"OneTwoTrip!\","
                + "\"senders_lcn\": \"1099\"",
                "\"url\":\"pfilters4_list_post_0_gifts@google.com/ott.net"
                + "\",\"pfilters_last_type\":\"ham\","
                + "\"pfilters_last_timestamp\":4375,"
                + "\"pfilters_spams\":3,"
                + "\"pfilters_hams\":1",
                "\"url\":\"pfilters4_list_owner_0_gifts@onetwotrip.com/ott.net"
                + "\",\"pfilters_last_type\":\"ham\","
                + "\"pfilters_last_timestamp\":4372,"
                + "\"pfilters_spams\":2,"
                + "\"pfilters_hams\":2",
                "\"url\":\"2\",\"msg_id\":\"123\"",
                "\"url\":\"3\",\"msg_id\":\"456\"",
                "\"url\":\"4\",\"msg_id\":\"789\""
            );
            HttpPost post = new HttpPost(
                cluster.proxy().host()
                    + "/api/async/senders?names-max=1");

            post.setEntity(new StringEntity(
                "{\"requests\":["
                + "{\"uid\":\"0\",\"suid\":1,\"email\":\"me@yandex.ru\","
                + "\"list_owner\":\"gifts@onetwotrip.com\","
                + "\"list_post\":\"gifts@google.com\","
                + "\"sender-host\":\"ott.net\","
                + "\"in-reply-to\":[\"123\"],"
                + "\"references\":[\"789\", \"012\"]}]}"));

            String expected =
                "{\"results\":[{\"request\":{"
                + "\"suid\":1,\"uid\":0,"
                + "\"email\":\"me@yandex.ru\","
                + "\"list_owner\":\"gifts@onetwotrip.com\","
                + "\"list_post\":\"gifts@google.com\","
                + "\"sender-host\":\"ott.net\","
                + "\"in-reply-to\":[\"123\"],"
                + "\"references\":[\"789\", \"012\"]},"
                + "\"response\":{\"sent_count\":0,"
                + "\"received_count\":3,\"last_contacted\":1484216433,"
                + "\"names\":[\"OneTwoTrip!\"],"
                + "\"sender_type\":\"list_owner\","
                + "\"received_read_count\":2,"
                + "\"pfilters_last_type\":\"ham\",\"pfilters_last_time\":"
                + "4375,\"pfilters_spams\":3,\"pfilters_hams\":1,"
                // list-post has bigger priority than list owner
                + "\"pfilters_sender_type\":\"list_post\","
                + "\"in_reply_to_matches\": true,"
                + "\"references_matches\": true}}]}";
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMissingPersonalFilter() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(
                    this,
                    MsearchProxyCluster.SOSEARCH_CONFIG,
                    new MsearchProxyCluster.MproxyClusterContext().producer());
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add("/*", "[{\"localhost\":6}]");
            cluster.start();
            final long uid = 0L;

            cluster.backend().add(SENDERS_DOC);
            HttpPost post = new HttpPost(
                cluster.proxy().host()
                    + "/api/async/senders?names-max=1");

            post.setEntity(new StringEntity(
                "{\"requests\":[" +
                    "{\"uid\":\"0\",\"suid\":1," +
                    "\"email\":\"gifts@onetwotrip.com\"," +
                    "\"sender-host\":\"ott.net\"}]}"
            ));

            String expected =
                "{\"results\":[{\"request\":{"
                + "\"suid\":1,\"uid\":0,"
                + "\"email\":\"gifts@onetwotrip.com\",\"sender-host\":\""
                + "ott.net\"},\"response\":{\"sent_count\":0,"
                + "\"received_count\":3,\"last_contacted\":1484216433,"
                + "\"names\":[\"OneTwoTrip!\"],\"sender_type\":\"from\","
                + "\"received_read_count\":2,"
                + "\"pfilters_last_type\":null,\"pfilters_last_time\":0,"
                + "\"pfilters_spams\":0,\"pfilters_hams\":0,"
                + "\"pfilters_sender_type\":null}}]}";
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testEmptySenders() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(
                    this,
                    MsearchProxyCluster.SOSEARCH_CONFIG,
                    new MsearchProxyCluster.MproxyClusterContext().producer());
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add("/*", "[{\"localhost\":6}]");
            cluster.start();
            final long uid = 0L;

            String userMlFeatures =
                "\"{\\\"USER_FACTORS_COPY_TO_HAM_YESTERDAY\\\":0,"
                + "\\\"USER_FACTORS_HAM_LAST_2_WEEKS\\\":47}\"";
            cluster.backend().add(
                SENDERS_DOC,
                "\"url\":\"pfilters4_0_gifts@onetwotrip.com/ott.net\",\n"
                + "\"pfilters_last_type\":\"ham\",\n"
                + "\"pfilters_last_timestamp\":4375,\n"
                + "\"pfilters_spams\":3,\n"
                + "\"pfilters_hams\":1",
                "\"url\":\"user_ml_features_uid_0\",\n"
                + "\"user_ml_features\":" + userMlFeatures,
                "\"url\":\"1\",\"msg_id\":\"123\",\"folder_type\":\"inbox\"",
                "\"url\":\"2\",\"msg_id\":\"456\",\"folder_type\":\"spam\"");

            HttpPost post = new HttpPost(
                cluster.proxy().host()
                + "/api/async/senders?names-max=1&parse-user-features=false");

            post.setEntity(new StringEntity(
                "{\"requests\":["
                + "{\"uid\":\"0\",\"suid\":1,\"sender-host\":\"ott.net\","
                + "\"in-reply-to\":[\"123\",\"456\"]}]}"));

            String expected =
                "{\"results\":[{\"request\":{"
                + "\"suid\":1,\"uid\":0,\"sender-host\":\"ott.net\","
                + "\"in-reply-to\":[\"123\",\"456\"]},"
                + "\"response\":{"
                + "\"pfilters_last_type\":null,\"pfilters_last_time\":"
                + "0,\"pfilters_spams\":0,\"pfilters_hams\":0,"
                + "\"pfilters_sender_type\":null,"
                + "\"in_reply_to_matches\": true,"
                + "\"user_ml_features\":" + userMlFeatures + "}}]}";
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}
