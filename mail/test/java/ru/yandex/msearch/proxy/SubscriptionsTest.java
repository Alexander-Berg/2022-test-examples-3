package ru.yandex.msearch.proxy;

import java.nio.charset.StandardCharsets;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;

public class SubscriptionsTest extends MsearchProxyTestBase {
    private static void addSubs(
        final MsearchProxyCluster cluster,
        final String email,
        final long uid,
        final String rcvdTypes,
        final long rcvDate)
        throws Exception
    {
        LongPrefix prefix = new LongPrefix(uid);
        HttpPost post = new HttpPost(cluster.backend().indexerUri() + "/add?db=subscriptions");
        post.setEntity(
            new StringEntity(
                TestSearchBackend.concatDocs(
                    prefix,
                    "\"subs_email\": \"" + email+"\",\n" +
                        "\"subs_domain\": \"" + email.substring(email.indexOf('@') + 1) + "\",\n" +
                        "\"subs_uid\": \"" + uid + "\",\n" +
                        "\"subs_received_date\": \"" + rcvDate + "\",\n" +
                        "\"subs_names\": \"eBay\",\n" +
                        "\"subs_read_types\": \"\",\n" +
                        "\"subs_message_types\": \"" + rcvdTypes + "\",\n" +
                        "\"url\": \"subs_" + uid + "_" + email + "\""),
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
    }

    private static void addSubsOptin(
        final MsearchProxyCluster cluster,
        final String email,
        final long uid,
        final String rcvdTypes,
        final String activeTypes,
        final long rcvDate)
        throws Exception
    {
        LongPrefix prefix = new LongPrefix(uid);
        HttpPost post = new HttpPost(cluster.backend().indexerUri() + "/add?db=subscriptions");
        post.setEntity(
            new StringEntity(
                TestSearchBackend.concatDocs(
                    prefix,
                    "\"subs_email\": \"" + email+"\",\n" +
                        "\"subs_domain\": \"" + email.substring(email.indexOf('@') + 1) + "\",\n" +
                        "\"subs_uid\": \"" + uid + "\",\n" +
                        "\"subs_received_date\": \"" + rcvDate + "\",\n" +
                        "\"subs_names\": \"eBay\",\n" +
                        "\"subs_read_types\": \"\",\n" +
                        "\"subs_message_types\": \"" + rcvdTypes + "\",\n" +
                        "\"subs_optin_active_types\": \"" + activeTypes + "\",\n" +
                        "\"url\": \"subs_" + uid + "_" + email + "\""),
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
    }

    @Test
    public void testListAndUpdate() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.producer().add(
                "/update*",
                new StaticHttpResource(new ProxyMultipartHandler(cluster.backend().indexerPort())));

            String baseListUri = cluster.proxy().host() +
                "/api/async/mail/subscriptions/list?&uid=379079136";
            String baseUpdateUri = cluster.proxy().host() +
                "/api/async/mail/subscriptions/update?&uid=379079136";
            HttpAssert.assertJsonResponse(
                client,
                baseListUri + "&statuses=active",
                "{\"active\":[]}");

            HttpPost postUpdate = new HttpPost(baseUpdateUri);
            postUpdate.setEntity(
                new StringEntity(
                    "[{\"email\": \"news@beru.ru\", \"action\": \"hide\"}]",
                StandardCharsets.UTF_8));

            cluster.backend().flush();

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                postUpdate);

            HttpAssert.assertJsonResponse(
                client,
                baseListUri + "&statuses=active",
                "{\"active\":[]}");

            HttpAssert.assertJsonResponse(
                client,
                baseListUri + "&statuses=active,hidden",
                "{\"active\":[],\"hidden\":[" +
                    "{\"email\":\"news@beru.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"news@beru.ru\"}" +
                    "]}");

            cluster.backend().update(
                new LongPrefix(379079136L),
                "\"subs_email\": \"ebay@reply5.ebay.com\",\n" +
                    "\"subs_domain\": \"reply5.ebay.com\",\n" +
                    "\"subs_uid\": \"379079136\",\n" +
                    "\"subs_received_date\": \"1595008228\",\n" +
                    "\"subs_names\": \"eBay\",\n" +
                    "\"subs_read_types\": \"\",\n" +
                    "\"subs_received_types\": \"55\\t2\\n23\\t2\\n100\\t2\\n13\\t2\\n27\\t2\",\n" +
                    "\"url\": \"subs_379079136_ebay@reply5.ebay.com\"",
                "\"subs_email\": \"news@beru.ru\",\n" +
                    "\"subs_domain\": \"beru.ru\",\n" +
                    "\"subs_uid\": \"379079136\",\n" +
                    "\"subs_received_date\": \"1595008227\",\n" +
                    "\"subs_names\": \"eBay\",\n" +
                    "\"subs_read_types\": \"\",\n" +
                    "\"subs_received_types\": \"55\\t2\\n23\\t2\\n100\\t2\\n13\\t2\\n27\\t2\",\n" +
                    "\"url\": \"subs_379079136_news@beru.ru\"");

//            HttpAssert.assertStatusCode(
//                HttpStatus.SC_OK,
//                client,
//                new HttpGet(baseUpdateUri
//                    + "&action=activate&email=newsletter@info.lamoda.ru"));

            HttpAssert.assertJsonResponse(
                client,
                baseListUri + "&statuses=active,hidden&check",
                "{\"active\":[],\"hidden\":[" +
                    "{\"email\":\"news@beru.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"news@beru.ru\"}]}");

        }
    }

//    /**
//     * Proxy + Indexation circuit
//     */
//    private static class SubscriptionCluster implements GenericAutoCloseable<IOException> {
//        private final MsearchProxyCluster proxyCluster;
//        private final KamajiCluster kamajiCluster;
//        private final GenericAutoCloseableChain<IOException> chain;
//
//        public SubscriptionCluster(final TestBase testBase) throws Exception {
//            try (GenericAutoCloseableHolder<
//                IOException,
//                GenericAutoCloseableChain<IOException>> chain =
//                     new GenericAutoCloseableHolder<>(
//                         new GenericAutoCloseableChain<>()))
//            {
//                proxyCluster = new MsearchProxyCluster(testBase);
//                chain.get().add(proxyCluster);
//                proxyCluster.start();
//                kamajiCluster = new KamajiCluster(proxyCluster.backend());
//                chain.get().add(kamajiCluster);
//                this.chain = chain.release();
//                proxyCluster.producer().add(
//                    "*",
//                    new ProxyMultipartHandler(kamajiCluster.kamaji().port()));
//            }
//        }
//
//        @Override
//        public void close() throws IOException {
//            chain.close();
//        }
//    }

    @Test
    public void testListAndUpdateOptIn() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.producer().add(
                "/update*",
                new StaticHttpResource(new ProxyMultipartHandler(cluster.backend().indexerPort())));

            String baseListUri = cluster.proxy().host() +
                "/api/async/mail/subscriptions/list?&uid=379079136&opt_in_subs=true";
            String baseUpdateUri = cluster.proxy().host() +
                "/api/async/mail/subscriptions/update?&uid=379079136&opt_in_subs=true";
            HttpAssert.assertJsonResponse(
                client,
                baseListUri + "&statuses=active,hidden,pending",
                "{\"active\":[], \"hidden\":[], \"pending\":[]}");

            long receiveDate = System.currentTimeMillis() / 1000;
            addSubs(cluster, "sporloto1@sport.ru", 379079136L, "13", receiveDate);
            addSubs(cluster, "sporloto2@sport.ru", 379079136L, "13", receiveDate);
            addSubs(cluster, "sporloto3@sport.ru", 379079136L, "13", receiveDate);
            HttpAssert.assertJsonResponse(
                client,
                baseListUri + "&statuses=active,hidden,pending",
                "{\"active\":[], \"hidden\":[], \"pending\":[" +
                    "{\"email\":\"sporloto1@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}," +
                    "{\"email\":\"sporloto2@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}," +
                    "{\"email\":\"sporloto3@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}" +
                    "]}");

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(baseUpdateUri
                    + "&action=activate&email=sporloto1@sport.ru&opt_in_subs=true"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(baseUpdateUri
                    + "&action=hide&email=sporloto2@sport.ru&opt_in_subs=true"));

            HttpAssert.assertJsonResponse(
                client,
                baseListUri + "&statuses=active,hidden,pending",
                "{\"active\":[" +
                    "{\"email\":\"sporloto1@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}" +
                    "], \"hidden\":[" +
                    "{\"email\":\"sporloto2@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}" +
                    "], \"pending\":[" +
                    "{\"email\":\"sporloto3@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}" +
                    "]}");

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(baseUpdateUri
                    + "&action=activate&email=sporloto2@sport.ru&opt_in_subs=true"));


            HttpAssert.assertJsonResponse(
                client,
                baseListUri + "&statuses=active,hidden,pending",
                "{\"active\":[" +
                    "{\"email\":\"sporloto1@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}," +
                    "{\"email\":\"sporloto2@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}" +
                    "], \"hidden\":[" +
                    "], \"pending\":[" +
                    "{\"email\":\"sporloto3@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}" +
                    "]}");

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(baseUpdateUri
                    + "&action=hide&email=sporloto1@sport.ru&opt_in_subs=true"));

            HttpAssert.assertJsonResponse(
                client,
                baseListUri + "&statuses=active,hidden,pending",
                "{\"active\":[" +
                    "{\"email\":\"sporloto2@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}" +
                    "], \"hidden\":[" +
                    "{\"email\":\"sporloto1@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}" +
                    "], \"pending\":[" +
                    "{\"email\":\"sporloto3@sport.ru\",\"types\":\"\",\"readFrequency\":0.0,\"displayName\":\"eBay\"}" +
                    "]}");

            // check partial active
            addSubsOptin(cluster, "sporloto1@sport.ru", 379079136L, "13", "13", receiveDate);
        }
    }

    @Test
    public void testUnsubscribeWithMoveExisting() throws Exception {
        MsearchProxyCluster.MproxyClusterContext context =
            new MsearchProxyCluster.MproxyClusterContext();
        context.producer(true, true);
        context.useMops(true);
        context.usePostgres(true);

        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(
                     this,
                     MsearchProxyCluster.PROD_CONFIG,
                     context);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.producer().add(
                "/update*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(cluster.backend().indexerPort())));

            String baseListUri = cluster.proxy().host() +
                "/api/async/mail/subscriptions/list?&uid=379079136";
            String baseUpdateUri = cluster.proxy().host() +
                "/api/async/mail/subscriptions/update?&uid=379079136";
            HttpPost postUpdate = new HttpPost(baseUpdateUri + "&move_existing_batch_size=2");
            postUpdate.setEntity(
                new StringEntity(
                    "[{\"email\": \"news@beru.ru\", \"action\": \"hide\", \"move_existing\": \"true\"}," +
                        "{\"email\": \"news@vasya.ru\", \"action\": \"hide\", \"move_existing\": \"false\"}," +
                        "{\"email\": \"news@petya.ru\", \"action\": \"hide\", \"move_existing\": \"true\"}," +
                        "{\"email\": \"news@empty.ru\", \"action\": \"hide\", \"move_existing\": \"true\"}" +
                        "]",
                    StandardCharsets.UTF_8));

            LongPrefix prefix = new LongPrefix(379079136L);
            cluster.backend().add(
                prefix,
                "\"mid\": 1, \"hid\": 0, \"message_type\": \"13\", \"url\":1, \"hdr_from_normalized\":\"news@beru.ru\"",
                "\"mid\": 2, \"url\":2, \"hid\": 0, \"message_type\": \"13\",\"hdr_from_normalized\":\"news@beru.ru\"",
                "\"mid\": 3, \"url\":3, \"hid\": 0, \"message_type\": \"13\",\"hdr_from_normalized\":\"news@beru.ru\"");

            cluster.backend().add(
                prefix,
                "\"mid\": 4, \"url\":4,\"hid\": 0, \"message_type\": \"13\", \"hdr_from_normalized\":\"news@petya.ru\"");

            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=2&source=mail_search&mids=1,2", HttpStatus.SC_OK, "{\"result\": \"ok\"}");
            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=1&source=mail_search&mids=3", HttpStatus.SC_OK, "{\"result\": \"ok\"}");
            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=1&source=mail_search&mids=4", HttpStatus.SC_OK, "{\"result\": \"ok\"}");

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                postUpdate);
        }
    }

    @Test
    public void testOptInWithMoveExisting() throws Exception {
        MsearchProxyCluster.MproxyClusterContext context =
            new MsearchProxyCluster.MproxyClusterContext();
        context.producer(true, true);
        context.useMops(true);
        context.usePostgres(true);

        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(
                     this,
                     MsearchProxyCluster.PROD_CONFIG,
                     context);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.producer().add(
                "/update*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(cluster.backend().indexerPort())));

            String baseListUri = cluster.proxy().host() +
                "/api/async/mail/subscriptions/list?&uid=379079136";
            String baseUpdateUri = cluster.proxy().host() +
                "/api/async/mail/subscriptions/update?&uid=379079136&opt_in_subs=true";

            HttpPost postUpdate = new HttpPost(baseUpdateUri + "&move_existing_batch_size=2");
            postUpdate.setEntity(
                new StringEntity(
                    "[{\"email\": \"news@beru.ru\", \"action\": \"activate\", \"move_existing\": \"true\"}," +
                        "{\"email\": \"news@vasya.ru\", \"action\": \"activate\", \"move_existing\": \"false\"}," +
                        "{\"email\": \"news@petya.ru\", \"action\": \"activate\", \"move_existing\": \"true\"}," +
                        "{\"email\": \"news@empty.ru\", \"action\": \"activate\", \"move_existing\": \"true\"}" +
                        "]",
                    StandardCharsets.UTF_8));

            LongPrefix prefix = new LongPrefix(379079136L);
            cluster.backend().add(
                prefix,
                "\"mid\": 1, \"hid\": 0, \"fid\":10, \"message_type\": \"13\", \"url\":1, \"hdr_from_normalized\":\"news@beru.ru\"",
                "\"mid\": 2, \"url\":2, \"hid\": 0, \"fid\":11, \"message_type\": \"13\",\"hdr_from_normalized\":\"news@beru.ru\"",
                "\"mid\": 3, \"url\":3, \"hid\": 0, \"fid\":10, \"message_type\": \"13\",\"hdr_from_normalized\":\"news@beru.ru\"");

            cluster.backend().add(
                prefix,
                "\"mid\": 4, \"url\":4,\"hid\": 0,  \"fid\": 12, \"message_type\": \"13\", \"hdr_from_normalized\":\"news@petya.ru\"");

            String foldersResponse =
                "{\n" +
                    "  \"folders\": {\n" +
                    "    \"1\": {\n" +
                    "      \"name\": \"Inbox\",\n" +
                    "      \"isUser\": false,\n" +
                    "      \"isSystem\": true,\n" +
                    "      \"type\": {\n" +
                    "        \"code\": 3,\n" +
                    "        \"title\": \"system\"\n" +
                    "      },\n" +
                    "      \"symbolicName\": {\n" +
                    "        \"code\": 1,\n" +
                    "        \"title\": \"inbox\"\n" +
                    "      }\n" +
                    "    },\n" +
                    "    \"10\": {\n" +
                    "      \"symbolicName\": {\n" +
                    "        \"title\": \"pending\",\n" +
                    "        \"code\": 11\n" +
                    "      },\n" +
                    "      \"type\": {\n" +
                    "        \"title\": \"system\",\n" +
                    "        \"code\": 3\n" +
                    "      },\n" +
                    "      \"isSystem\": true,\n" +
                    "      \"isUser\": false,\n" +
                    "      \"name\": \"pending\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            String furitaResponse =
                "{\n" +
                    "  \"session\": \"LPYcA11pZ4Y1\",\n" +
                    "  \"rules\": [\n" +
                    "    {\n" +
                    "      \"id\": \"103178\",\n" +
                    "      \"name\": \"Письма из ящика vonidu@gmail.com\",\n" +
                    "      \"priority\": 0,\n" +
                    "      \"stop\": false,\n" +
                    "      \"enabled\": true,\n" +
                    "      \"created\": 1517224936,\n" +
                    "      \"type\": \"user\",\n" +
                    "      \"actions\": [\n" +
                    "        {\n" +
                    "          \"type\": \"movel\",\n" +
                    "          \"parameter\": \"127\",\n" +
                    "          \"verified\": true\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"query\": \"headers:X-yandex-rpop-id\\\\:\\\\ *2286471* AND NOT folder_type: spam\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": \"269253\",\n" +
                    "      \"name\": \"\",\n" +
                    "      \"priority\": 3,\n" +
                    "      \"stop\": false,\n" +
                    "      \"enabled\": true,\n" +
                    "      \"created\": 1628528406,\n" +
                    "      \"type\": \"user\",\n" +
                    "      \"actions\": [\n" +
                    "        {\n" +
                    "          \"type\": \"move\",\n" +
                    "          \"parameter\": \"40\",\n" +
                    "          \"verified\": true\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"query\": \"((hdr_from_email:\\\"noreply@utkonos.ru\\\" OR " +
                    "hdr_from_display_name:\\\"noreply@utkonos.ru\\\") OR (hdr_from_email:\\\"auto@utkonos.ru\\\" OR " +
                    "hdr_from_display_name:\\\"auto@utkonos.ru\\\")) AND NOT folder_type: spam\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": \"269254\",\n" +
                    "      \"name\": \"\",\n" +
                    "      \"priority\": 1,\n" +
                    "      \"stop\": true,\n" +
                    "      \"enabled\": true,\n" +
                    "      \"created\": 1628528410,\n" +
                    "      \"type\": \"user\",\n" +
                    "      \"actions\": [\n" +
                    "        {\n" +
                    "          \"type\": \"move\",\n" +
                    "          \"parameter\": \"30\",\n" +
                    "          \"verified\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"type\": \"status\",\n" +
                    "          \"parameter\": \"RO\",\n" +
                    "          \"verified\": true\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"query\": \"(hdr_from_email:\\\"no\\\\-reply@taxi.yandex.ru\\\" OR " +
                    "hdr_from_display_name:\\\"no\\\\-reply@taxi.yandex.ru\\\") AND NOT folder_type: spam\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": \"270156\",\n" +
                    "      \"name\": \"\",\n" +
                    "      \"priority\": 4,\n" +
                    "      \"stop\": false,\n" +
                    "      \"enabled\": true,\n" +
                    "      \"created\": 1630956280,\n" +
                    "      \"type\": \"user\",\n" +
                    "      \"actions\": [\n" +
                    "        {\n" +
                    "          \"type\": \"move\",\n" +
                    "          \"parameter\": \"40\",\n" +
                    "          \"verified\": true\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"query\": \"hdr_subject_keyword:*Нет\\\\ таких\\\\ тем\\\\ ну\\\\ вот\\\\ совсем\\\\\\\\?* " +
                    "AND NOT folder_type: spam\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n";

            cluster.furita().add("/api/list.json?db=pg&detailed=1&uid=379079136", furitaResponse);

            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=379079136",
                foldersResponse);

            String fsUri =
                "/filter_search?&uid=379079136&mdb=pg&full_folders_and_labels=1&incl_folders=pending&order=default";
            filterSearch(cluster, fsUri, "1", "3");
            cluster.tupita().add(
                "/check?subscriptions&uid=379079136",
                "{\"result\":[{\"uid\":379079136, \"matched_queries\":[\"270156\"]}]}");
            cluster.mops().add(
                "/complex_move?&subscription_activate_subs_optin&dest_fid=40&with_sent=0" +
                    "&uid=379079136&request_mids_count=2&source=mail_search&mids=1,3",
                HttpStatus.SC_OK,
                "{\"result\": \"ok\"}");
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                postUpdate);

            postUpdate = new HttpPost(baseUpdateUri + "&move_existing_batch_size=2");
            postUpdate.setEntity(
                new StringEntity(
                    "[{\"email\": \"news@beru.ru\", \"action\": \"hide\", \"move_existing\": \"true\"}," +
                        "{\"email\": \"news@vasya.ru\", \"action\": \"hide\", \"move_existing\": \"false\"}," +
                        "{\"email\": \"news@petya.ru\", \"action\": \"hide\", \"move_existing\": \"true\"}," +
                        "{\"email\": \"news@empty.ru\", \"action\": \"hide\", \"move_existing\": \"true\"}" +
                        "]",
                    StandardCharsets.UTF_8));


            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=2&source=mail_search&mids=1,2", HttpStatus.SC_OK, "{\"result\": \"ok\"}");
            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=1&source=mail_search&mids=3", HttpStatus.SC_OK, "{\"result\": \"ok\"}");
            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=1&source=mail_search&mids=4", HttpStatus.SC_OK, "{\"result\": \"ok\"}");
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                postUpdate);
        }
    }

    @Test
    public void testEmailNormalization() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.start();
            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.producer().add(
                "/update*",
                new StaticHttpResource(new ProxyMultipartHandler(cluster.backend().indexerPort())));

            String baseListUri =
                cluster.proxy().host() +
                    "/api/async/mail/subscriptions/list?&uid=379079136&opt_in_subs=true";
            String baseUpdateUri =
                cluster.proxy().host() +
                    "/api/async/mail/subscriptions/update?&uid=379079136&opt_in_subs=true";

            long uid = 227356512;
            String email = "Coursera.shmoursera@yandex.ru";
            long receiveDate = System.currentTimeMillis() / 1000;
            addSubsOptin(cluster, "coursera-shmoursera@yandex.ru", uid, "100", "13\n100", receiveDate);
            String statusUri =
                cluster.proxy().host() + "/api/async/mail/subscriptions/status?uid="
                    + uid + "&opt_in_subs_uid=" + uid;
            HttpAssert.assertJsonResponse(
                client,
                statusUri + "&email=" + email,
                "{\"subscriptions\": ["
                    + statusResponse("active", email, uid)
                    + "]}");
        }
    }

    @Test
    public void testDeliveryOptin() throws Exception {
        MsearchProxyCluster.MproxyClusterContext context =
            new MsearchProxyCluster.MproxyClusterContext();
        context.producer(true, true);
        context.useMops(true);

        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(
                     this,
                     MsearchProxyCluster.PROD_CONFIG,
                     context);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.producer().add(
                "/update*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(cluster.backend().indexerPort())));

            long uid = 379079136L;
            addSubs(cluster, "sporloto1@sport.ru", uid, "13", 100500);
            addSubs(cluster, "sporloto2@sport.ru", uid, "100", 100500);
            addSubs(cluster, "sporloto3@sport.ru", uid, "13", 100500);
            addSubs(cluster, "sporloto4@sport.ru", uid, "23\n36", 100500);
            addSubs(cluster, "sporloto5@sport.ru", uid, "23\n36\n100\n17\n13", 100500);

            String statusUri =
                cluster.proxy().host() + "/api/async/mail/subscriptions/status?uid="
                    + uid + "&opt_in_subs_uid=" + uid;
            HttpAssert.assertJsonResponse(
                client,
                statusUri + "&email=sporloto1@sport.ru",
                "{\"subscriptions\": ["
                    + statusResponse("pending", "sporloto1@sport.ru", uid)
                    + "]}");
            HttpAssert.assertJsonResponse(
                client,
                statusUri + "&email=sporloto5@sport.ru",
                "{\"subscriptions\": ["
                    + statusResponse("pending", "sporloto5@sport.ru", uid)
                    + "]}");
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.proxy().port(),
                "/api/async/mail/subscriptions/update?&action=activate&uid="
                    + uid + "&email=sporloto5@sport.ru&opt_in_subs=true");
            HttpAssert.assertJsonResponse(
                client,
                statusUri + "&email=sporloto5@sport.ru",
                "{\"subscriptions\": ["
                    + statusResponse("active", "sporloto5@sport.ru", uid)
                    + "]}");
        }
    }

    @Test
    public void testDelivery() throws Exception {
        MsearchProxyCluster.MproxyClusterContext context =
            new MsearchProxyCluster.MproxyClusterContext();
        context.producer(true, true);
        context.useMops(true);

        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(
                     this,
                     MsearchProxyCluster.PROD_CONFIG,
                     context);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.producer().add(
                "/update*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(cluster.backend().indexerPort())));

            long uid = 379079136L;
            addSubs(cluster, "sporloto1@sport.ru", uid, "13", 100500);
            addSubs(cluster, "sporloto2@sport.ru", uid, "100", 100500);
            addSubs(cluster, "sporloto3@sport.ru", uid, "13", 100500);

            String baseUri =
                cluster.proxy().host() + "/api/async/mail/subscriptions/status?uid=";

            HttpAssert.assertJsonResponse(
                client,
                baseUri + uid + "&opt_in_subs_uid=&email=sporloto1@sport.ru",
                "{\"subscriptions\": ["
                    + statusResponse("active", "sporloto1@sport.ru", uid)
                    + "]}");

            HttpAssert.assertJsonResponse(
                client,
                baseUri + uid + ",100500"
                    + "&opt_in_subs_uid=&email=sporloto1@sport.ru,sporloto2@sport.ru",
                "{\"subscriptions\": ["
                    + statusResponse("active", "sporloto1@sport.ru", uid) + ','
                    + statusResponse("active", "sporloto2@sport.ru", uid) + ','
                    + statusResponse("active", "sporloto1@sport.ru", 100500)  + ','
                    + statusResponse("active", "sporloto2@sport.ru", 100500)
                    + "]}");

            String hideUri =
                "/api/async/mail/subscriptions/update?&action=hide&uid=";
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.proxy().port(),
                hideUri + uid +"&email=sporloto1@sport.ru");

            HttpAssert.assertJsonResponse(
                client,
                baseUri + uid + ",100500"
                    + "&opt_in_subs_uid=&email=sporloto1@sport.ru,sporloto2@sport.ru",
                "{\"subscriptions\": ["
                    + statusResponse("hidden", "sporloto1@sport.ru", uid) + ','
                    + statusResponse("active", "sporloto2@sport.ru", uid) + ','
                    + statusResponse("active", "sporloto1@sport.ru", 100500)  + ','
                    + statusResponse("active", "sporloto2@sport.ru", 100500)
                    + "]}");

            // Optin
            HttpAssert.assertJsonResponse(
                client,
                baseUri + uid + ",100500"
                    + "&opt_in_subs_uid=" + uid + "&email=sporloto1@sport.ru,sporloto2@sport.ru",
                "{\"subscriptions\": ["
                    + statusResponse("active", "sporloto1@sport.ru", 100500)  + ','
                    + statusResponse("active", "sporloto2@sport.ru", 100500) + ','
                    + statusResponse("hidden", "sporloto1@sport.ru", uid) + ','
                    + statusResponse("pending", "sporloto2@sport.ru", uid)
                    + "]}");

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.proxy().port(),
                "/api/async/mail/subscriptions/update?&action=activate&uid="
                    + uid + "&email=sporloto2@sport.ru&opt_in_subs=true");

            HttpAssert.assertJsonResponse(
                client,
                baseUri + uid + ",100500"
                    + "&opt_in_subs_uid=" + uid + "&email=sporloto1@sport.ru,sporloto2@sport.ru",
                "{\"subscriptions\": ["
                    + statusResponse("active", "sporloto1@sport.ru", 100500)  + ','
                    + statusResponse("active", "sporloto2@sport.ru", 100500) + ','
                    + statusResponse("hidden", "sporloto1@sport.ru", uid) + ','
                    + statusResponse("active", "sporloto2@sport.ru", uid)
                    + "]}");
        }
    }

    private static String statusResponse(
        final String status,
        final String email,
        final long uid)
    {
        return "{" + "\"email\": \"" + email + "\"," +
            "\"status\": \"" + status + "\"," +
            "\"uid\": " + uid + "}";
    }

    @Test
    public void testInvalidEmail() throws  Exception {
        String email = "support@тридевятоецарство.рф";
        Assert.assertTrue(EmailValidator.getInstance().isValid(email));
        email = "support@тридевятоецарство";
        Assert.assertFalse(EmailValidator.getInstance().isValid(email));
//        NormalizingProcessor processor = new NormalizingProcessor();
//        processor.process(email.toCharArray());
//        InternetAddress emailAddr = new InternetAddress(processor.toString(), false);
//        emailAddr.validate();

//        Assert.assertFalse(result);
//        InternetAddress emailAddr = new InternetAddress(regEmail);
//        emailAddr.validate();
//        EmailPr
//        EmailParser parser = new EmailParser();
//        List<Mailbox> list = parser.parseDecoded("MAIL_BOX");
//        System.out.println(list);
//        Assert.assertEquals(0, list.size());
    }
}

