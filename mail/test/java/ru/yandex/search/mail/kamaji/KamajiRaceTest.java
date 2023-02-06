package ru.yandex.search.mail.kamaji;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.concurrent.LockStorage;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.config.RetriesConfigBuilder;
import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.BadResponseException;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.ServerException;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.EmptyAsyncConsumerFactory;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.json.xpath.ValueUtils;
import ru.yandex.search.mail.kamaji.lock.FastSlowLock;
import ru.yandex.search.mail.kamaji.usertype.UserTypeConfigBuilder;
import ru.yandex.test.search.backend.TestMailSearchBackend;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class KamajiRaceTest extends KamajiTestBase {
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final long UID = 124124;
    private static final String SLOW = "&slow";
    private static final long WAIT_RESPONSE_TIMEOUT = 1000;
    private static final long WAIT_REQUEST_TIMEOUT = 10000;
    private static final long HEATUP_UID = 9999;
    private static final String TO = "hirthwork2@yandex.ru";
    private static final String UID_P = "&uid=";
    private static final String FID1 = "1";
    private static final String BLACKBOX_URI = blackboxUri(UID_P + UID);
    private static final String FILTER_SEARCH =
        "/filter_search?order=default&full_folders_and_labels=1&uid="
            + UID + "&mdb=pg&suid=90000&lcn=1&operation-id=2";
    private static final String LUCENE_EMPTY
        = "{\"hitsArray\":[], \"hitsCount\":0}";
    private static final String TIKAITE_URI =
        "/mail/handler?json-type=dollar&stid=1.1.1";
    private static final String TIKAITE_RESPONSE =
        "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello\"}]}";

    private static final KamajiConfig TESTCONF;

    static {
        KamajiConfigBuilder config = new KamajiConfigBuilder();
        config.userTypeConfig(
            new UserTypeConfigBuilder()
                .enabled(true)
                .reindexBatchSize(2));
        TESTCONF = config;
    }

    private enum Status {
        OK,
        FAILED,
        CANCELLED
    }

    private static String filterSearch(
        final String receivedDate,
        final String mid)
    {
        return "{\"envelopes\":[{\"stid\":\"1.1.1\",\"fid\":1,"
            + "\"folder\":{\"type\":{\"title\":\"user\"},\"name\":\"fld\"},"
            + "\"from\":[{\"displayName\":\"Spamer"
            + "\",\"domain\":\"hotmail.com\",\"local\":\"spammer\"}],"
            + "\"to\":[{\"displayName\":\"yandex user\","
            + "\"domain\":\"ya.ru\",\"local\":\"user\"}],"
            + "\"receiveDate\":" + receivedDate
            + ",\"mid\":" + mid
            + ",\"threadId\":" + mid + '}' + ']' + '}';
    }

    private static String delete(final String... mids) {
        StringBuilder base = new StringBuilder(
            "{\"operation_id\": \"160014\","
            + "\"uid\": \"" + UID + "\","
            + "\"lcn\": \"245\","
            + "\"change_type\": \"delete\","
            + "\"operation_date\": \"1436810748.043101\","
            + "\"changed\": [");
        for (String mid: mids) {
            base.append("{\"src_fid\": 1, \"mid\": " + mid + "},");
        }

        base.setLength(base.length() - 1);
        base.append(
            "], \"fresh_count\": \"2558\","
                + "\"useful_new_messages\": \"0\","
                + "\"pgshard\": \"1\"}");
        return base.toString();
    }

    private static String store(final String... mids) {
        StringBuilder sb = new StringBuilder(
            "{\"lcn\":1,\"operation_id\":2,\"uid\":" + UID
            + ",\"operation_date\": \"1436810748.043103\","
            + "\"change_type\":\"store\",\"changed\":[");
        for (String mid: mids) {
            sb.append("{\"mid\": \"" + mid + "\"} ,");
        }

        sb.setLength(sb.length() - 1);
        sb.append("]}");
        return sb.toString();
    }

    private static String move(final String... mids) {
        StringBuilder sb = new StringBuilder(
            "{\"lcn\":1, \"operation_id\":2, \"uid\":" + UID
                + ",\"operation_date\":\"1436810748.043103\","
                + "\"change_type\":\"move\",\"changed\":[");
        for (String mid: mids) {
            sb.append("{\"src_fid\":1, \"fid\":3, \"mid\": \"" + mid + "\"},");
        }

        sb.setLength(sb.length() - 1);
        sb.append(" ]}");
        return sb.toString();
    }

    private static String storeNotifyUri(
        final KamajiCluster cluster,
        final String queueId)
        throws IOException
    {
        return notifyUri(cluster, "store", queueId);
    }

    private static String moveNotifyUri(
        final KamajiCluster cluster,
        final String queueId)
        throws IOException
    {
        return notifyUri(cluster, "move", queueId);
    }

    private static String notifyUri(
        final KamajiCluster cluster,
        final String changeType,
        final String queueId)
        throws IOException
    {
        return HTTP_LOCALHOST + cluster.kamaji().port()
                + "/notify?mdb=pg&pgshard=2095&operation-id=3710067614"
                + "&operation-date=1500033698.776273&uid=" + UID
                + "&change-type=" + changeType + "&changed-size=1&batch-size=1"
                + "&salo-worker=pg2095:8&transfer-timestamp=1500033698853"
                + "&zoo-queue-id=" + queueId;
    }

    private static String deleteNotifyUri(
        final KamajiCluster cluster,
        final String queueId)
        throws IOException
    {
        return HTTP_LOCALHOST + cluster.kamaji().port()
            + "/notify?mdb=pg&pgshard=2095&operation-id=3710067615"
            + "&operation-date=1500033699.776273&uid=" + UID
            + "&change-type=delete&changed-size=1&salo-worker=pg2095:8"
            + "&transfer-timestamp=1500033698853&zoo-queue-id=" + queueId;
    }

    private static String filterSearchUri(final String... mids) {
        StringBuilder sb = new StringBuilder(FILTER_SEARCH);
        for (String mid: mids) {
            sb.append("&mids=");
            sb.append(mid);
        }

        return sb.toString();
    }

    @Test
    public void testAllHeatUp() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, TESTCONF);
             AsyncClient client = cluster.kamaji().client(
                 "KamajiClient",
                 new HttpTargetConfigBuilder().connections(2).build()))
        {
            cluster.kamaji().start();
            WaitingCallback heatUp = new WaitingCallback();

            client.execute(
                cluster.kamaji().host(),
                new BasicAsyncRequestProducerGenerator(
                    "/notify?mdb=pg&pgshard=0&operation-id=0"
                        + "&operation-date=0&uid=" + HEATUP_UID
                        + "&change-type=store&zoo-queue-id=1",
                    store("1111")),
                heatUp);

            Assert.assertEquals(
                Status.FAILED,
                heatUp.waitResponse(WAIT_RESPONSE_TIMEOUT));
        }
    }

    private static String luceneCheckUri(final String mid) {
        return "/search?IOPRIO=3000&prefix=" + UID + "&text=mid_p:"
            + mid + "&get=mid,fid,queueId&hr";
    }

    private static String luceneCheckEntry(
        final String mid,
        final String fid,
        final String qid)
    {
        return "{\"mid\":\"" + mid + "\", \"fid\":\"" + fid + "\", "
            + "\"queueId\":\"" + qid + "\"}";
    }

    private static String luceneCheckBody(final String... entries) {
        StringBuilder sb = new StringBuilder("{\"hitsArray\":[");
        for (String entry: entries) {
            sb.append(entry);
            sb.append(',');
        }

        sb.setLength(sb.length() - 1);
        sb.append("], \"hitsCount\":");
        sb.append(entries.length);
        sb.append('}');
        return sb.toString();
    }

    @Test
    public void testMidGone() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, TESTCONF);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.blackbox().add(
                BLACKBOX_URI,
                blackboxResponse(UID, TO));

            String mid1 = "41";
            String mid2 = "42";
            cluster.filterSearch().add(
                filterSearchUri(mid1, mid2),
                filterSearch("554", mid1));
            cluster.tikaite().add(TIKAITE_URI, TIKAITE_RESPONSE);

            cluster.kamaji().start();

            String queueId = "792";
            String storeNotify = storeNotifyUri(cluster, queueId);

            HttpPost post = new HttpPost(storeNotify);
            post.setEntity(new StringEntity(store(mid1, mid2)));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            checkNoLocks(cluster.kamaji());
            lucene.checkSearch(
                luceneCheckUri(mid2),
                new JsonChecker(LUCENE_EMPTY));

            lucene.checkSearch(
                luceneCheckUri(mid1),
                new JsonChecker(
                    luceneCheckBody(
                        luceneCheckEntry(mid1, FID1, queueId),
                        luceneCheckEntry(mid1, FID1, queueId))));
        }
    }

    @Test
    public void testDelete() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, TESTCONF);
             CloseableHttpClient client = HttpClients.createDefault();
             PeachEmulator peach = new PeachEmulator(cluster.kamaji()))
        {
            cluster.blackbox().add(
                BLACKBOX_URI,
                blackboxResponse(UID, TO));

            String mid = "156";
            cluster.filterSearch().add(
                filterSearchUri(mid),
                filterSearch("12356", mid));
            cluster.tikaite().add(TIKAITE_URI, TIKAITE_RESPONSE);

            cluster.slowIndexerProxy().register(new Pattern<>("", true), peach);

            cluster.kamaji().start();

            String queueId1 = "3136014";
            String queueId2 = "3136015";
            String storeNotify = storeNotifyUri(cluster, queueId1);
            String deleteNotify = deleteNotifyUri(cluster, queueId2);

            HttpPost post = new HttpPost(storeNotify);
            post.setEntity(new StringEntity(store(mid)));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            post = new HttpPost(deleteNotify);
            post.setEntity(new StringEntity(delete(mid)));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            peach.waitRequests(WAIT_REQUEST_TIMEOUT);
            Assert.assertEquals(1, peach.requestCount.get());

            lucene.checkSearch(
                luceneCheckUri(mid),
                new JsonChecker(LUCENE_EMPTY));

            checkNoLocks(cluster.kamaji());
        }
    }

    @Test
    public void testSlowDeleteRace() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, TESTCONF);
             CloseableHttpClient client = HttpClients.createDefault();
             PeachEmulator peach = new PeachEmulator(cluster.kamaji()))
        {
            cluster.blackbox().add(
                BLACKBOX_URI,
                blackboxResponse(UID, TO));

            String mid = "157";
            cluster.filterSearch().add(
                filterSearchUri(mid),
                filterSearch("123567", mid));

            cluster.tikaite().add(TIKAITE_URI, TIKAITE_RESPONSE);

            cluster.slowIndexerProxy().register(new Pattern<>("", true), peach);

            cluster.kamaji().start();

            HttpPost post = new HttpPost(storeNotifyUri(cluster, "3136034"));
            post.setEntity(new StringEntity(store(mid)));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            String deleteNotify = deleteNotifyUri(cluster, "3136035");
            post = new HttpPost(deleteNotify);
            post.setEntity(new StringEntity(delete(mid)));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            peach.waitRequests(WAIT_REQUEST_TIMEOUT);
            Assert.assertEquals(1, peach.requestCount.get());

            lucene.checkSearch(
                luceneCheckUri(mid),
                new JsonChecker(LUCENE_EMPTY));

            checkNoLocks(cluster.kamaji());
        }
    }

    @Test
    public void testSlowSimultaneously() throws Exception {
        final int maxQueueId = 9;
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, TESTCONF);
             CloseableHttpClient client = HttpClients.createDefault();
             PeachEmulator peach = new PeachEmulator(
                 cluster.kamaji(),
                 maxQueueId))
        {
            cluster.blackbox().add(
                BLACKBOX_URI,
                blackboxResponse(UID, TO));

            String mid = "957";
            cluster.filterSearch().add(
                filterSearchUri(mid),
                filterSearch("6312356", mid));

            final long tikiteTO = 400L;
            cluster.tikaite().add(
                TIKAITE_URI,
                new StaticHttpResource(
                    new SlowpokeHttpItem(
                        new StaticHttpItem(TIKAITE_RESPONSE), tikiteTO)));

            cluster.slowIndexerProxy().register(
                new Pattern<>("", true),
                new StaticHttpItem(HttpStatus.SC_OK));

            cluster.slowIndexerProxy().register(new Pattern<>("", true), peach);
            cluster.kamaji().start();

            HttpPost post;

            for (int i = 1; i <= maxQueueId; i++) {
                post = new HttpPost(
                    storeNotifyUri(cluster, String.valueOf(i)));
                post.setEntity(new StringEntity(store(mid)));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            }

            peach.waitRequests(maxQueueId, WAIT_REQUEST_TIMEOUT);

            String expected =
                luceneCheckBody(
                    luceneCheckEntry(mid, FID1, String.valueOf(maxQueueId)),
                    luceneCheckEntry(mid, FID1, String.valueOf(maxQueueId)));

            lucene.checkSearch(
                luceneCheckUri(mid),
                new JsonChecker(expected));

            checkNoLocks(cluster.kamaji());

            peach.reset();
            final int queueId = maxQueueId + 1;
            for (int i = 1; i <= maxQueueId; i++) {
                post = new HttpPost(
                    storeNotifyUri(cluster, String.valueOf(queueId)));
                post.setEntity(new StringEntity(store(mid)));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            }

            peach.waitRequests(maxQueueId, WAIT_REQUEST_TIMEOUT);

            expected =
                luceneCheckBody(
                    luceneCheckEntry(mid, FID1, String.valueOf(queueId)),
                    luceneCheckEntry(mid, FID1, String.valueOf(queueId)));

            lucene.checkSearch(
                luceneCheckUri(mid),
                new JsonChecker(expected));

            checkNoLocks(cluster.kamaji());
        }
    }

    /**
     * Common situation when we forgot to unlock on one of kamaji request failed
     * Here i'm trying to solve this problem in general, we are define all
     * requests for notify and trying sequentially fail them, checking no locks
     * left
     * @throws Exception
     */
    @Test
    public void testRequestFailures() throws Exception {
        final String mid = "12351235";
        final String ts = "15351235";

        KamajiConfigBuilder config =
            KamajiCluster.loadConfig(
                Paths.getSourcePath(
                    "mail/search/mail/kamaji_config/files/kamaji-test.conf"));
        try (KamajiCluster cluster = new KamajiCluster(null, config)) {
            cluster.kamaji().start();
            List<RequestData> requests = new ArrayList<>();
            requests.add(
                new RequestData(cluster.blackbox(),
                    BLACKBOX_URI,
                    blackboxResponse(UID, TO)));
            requests.add(
                new RequestData(
                    cluster.filterSearch(),
                    filterSearchUri(mid),
                    filterSearch(ts, mid)));

            String luceneEmptyResponse =
                "{\"hitsCount\":0, \"hitsArray\":[]}";

            String userTypeRequest =
                "/search?IO_PRIO=3000&prefix=124124&text="
                    + "url:usrtype_from_124124_spammer@hotmail.com"
                    + "&get=user_types";

            requests.add(
                new RequestData(
                    cluster.backend(),
                    userTypeRequest,
                    luceneEmptyResponse));
            requests.add(
                new RequestData(
                    cluster.backend(),
                    "/update?uid=124124&fast&mid=12351235",
                    ""));
            //slow
            requests.add(
                new RequestData(cluster.blackbox(),
                    BLACKBOX_URI,
                    blackboxResponse(UID, TO)));

            requests.add(
                new RequestData(
                    cluster.backend(),
                    "/search?IO_PRIO=3000&prefix=124124&get=mid,queueId"
                        + "&text=url:124124_12351235/0",
                    luceneEmptyResponse));
            requests.add(
                new RequestData(
                    cluster.backend(),
                    userTypeRequest,
                    luceneEmptyResponse));
            requests.add(
                new RequestData(
                    cluster.tikaite(),
                    TIKAITE_URI,
                    TIKAITE_RESPONSE));
            requests.add(
                new RequestData(
                    cluster.backend(),
                    "/update?uid=124124&mid=12351235",
                    ""));
            requests.add(
                new RequestData(
                    cluster.filterSearch(),
                    filterSearchUri(mid),
                    filterSearch(ts, mid)));

            final String queueId1 = "5555555";
            HttpPost storePost =
                new HttpPost(storeNotifyUri(cluster, queueId1));
            storePost.setEntity(new StringEntity(store(mid)));
            requestFailures(cluster, new ArrayList<>(requests), storePost);

            requests.add(
                new RequestData(
                    cluster.backend(),
                    "/search?IO_PRIO=3000&prefix=124124&text=url:"
                        + "124124_12351235/0&get=clicks_total_count,fid",
                    "{\"hitsCount\":1, \"hitsArray\":[{\"fid\":1, "
                    + "\"clicks_total_count\": 0}]}"));
            HttpPost movePost = new HttpPost(moveNotifyUri(cluster, queueId1));
            movePost.setEntity(new StringEntity(move(mid)));
            requestFailures(cluster, new ArrayList<>(requests), movePost);
        }
    }

    public void requestFailures(
        final KamajiCluster cluster,
        final List<RequestData> requests,
        final HttpPost post)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            IdentityHashMap<StaticServer, Map<String, List<String>>> serversMap
                = new IdentityHashMap<>();
            for (int i = 0; i < requests.size(); i++) {
                RequestData request = requests.get(i);
                serversMap
                    .computeIfAbsent(
                        request.server(),
                        (k) -> new LinkedHashMap<>())
                    .computeIfAbsent(
                        request.uri(), (k) -> new ArrayList<>())
                    .add(request.response());
            }

            for (int i = 0; i < requests.size(); i++) {
                System.out.println(
                    "Execute step with fail " + i);
                fillServers(serversMap, i);
                try (CloseableHttpResponse response = client.execute(post)) {
                    CharsetUtils.consume(response.getEntity());
                }
                fillServers(serversMap, -1);
                checkNoLocks(cluster.kamaji());
                System.out.println(
                    "Execute step with success expected " + i);
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
                checkNoLocks(cluster.kamaji());
                System.out.println("Step completed " + i);
            }
        }
    }

    private static void fillServers(
        final Map<StaticServer, Map<String, List<String>>> serversMap,
        final int badIndex)
    {
        int j = 0;
        for (Map.Entry<StaticServer, Map<String, List<String>>> entry
            : serversMap.entrySet())
        {
            StaticServer server = entry.getKey();
            for (Map.Entry<String, List<String>> uriEntry
                : entry.getValue().entrySet())
            {
                List<String> responses = uriEntry.getValue();
                HttpRequestHandler[] handlers =
                    new HttpRequestHandler[responses.size()];

                for (int k = 0; k < uriEntry.getValue().size(); k++) {
                    if (j == badIndex) {
                        handlers[k] =
                            new StaticHttpItem(
                                HttpStatus.SC_BAD_GATEWAY);
                        System.out.println(
                            "On this step we fail " + uriEntry.getKey());
                    } else {
                        handlers[k] =
                            new StaticHttpItem(
                                HttpStatus.SC_OK,
                                responses.get(k));
                    }

                    j++;
                }

                server.add(
                    uriEntry.getKey(),
                    new ChainedHttpResource(handlers));
            }
        }
    }

    private static final class RequestData {
        private final StaticServer server;
        private final String uri;
        private final String response;

        private RequestData(
            final StaticServer server,
            final String uri,
            final String response)
        {
            this.server = server;
            this.uri = uri;
            this.response = response;
        }

        public StaticServer server() {
            return server;
        }

        public String uri() {
            return uri;
        }

        public String response() {
            return response;
        }
    }

    @Test
    public void testSlowReorder() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, TESTCONF);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.blackbox().add(
                BLACKBOX_URI,
                blackboxResponse(UID, TO));

            String mid = "357";
            cluster.filterSearch().add(
                filterSearchUri(mid),
                filterSearch("312356", mid));

            cluster.tikaite().add(TIKAITE_URI, TIKAITE_RESPONSE);

            cluster.slowIndexerProxy().register(
                new Pattern<>("", true),
                new StaticHttpItem(HttpStatus.SC_OK));

            cluster.kamaji().start();

            final String queueId1 = "3136024";
            HttpPost post = new HttpPost(storeNotifyUri(cluster, queueId1));
            post.setEntity(new StringEntity(store(mid)));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            final String queueId2 = "3136025";
            post = new HttpPost(storeNotifyUri(cluster, queueId2));
            post.setEntity(new StringEntity(store(mid)));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            post = new HttpPost(storeNotifyUri(cluster, queueId2) + SLOW);
            post.setEntity(new StringEntity(store(mid)));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            final String expected =
                luceneCheckBody(
                    luceneCheckEntry(mid, FID1, queueId2),
                    luceneCheckEntry(mid, FID1, queueId2));

            lucene.checkSearch(
                luceneCheckUri(mid),
                new JsonChecker(expected));

            post = new HttpPost(storeNotifyUri(cluster, queueId1) + SLOW);
            post.setEntity(new StringEntity(store(mid)));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            lucene.checkSearch(
                luceneCheckUri(mid),
                new JsonChecker(expected));

            checkNoLocks(cluster.kamaji());
        }
    }

    @Test
    public void testSlowIndexFailed() throws Exception {
        // test case when everything is ok, until update itself
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(null, TESTCONF);
             AsyncClient client = new AsyncClient(
                 cluster.kamaji().reactor(),
                 Configs.targetConfig()))
        {
            client.start();
            cluster.blackbox().add(
                BLACKBOX_URI,
                blackboxResponse(UID, TO));

            String mid = "4357";
            cluster.filterSearch().add(
                filterSearchUri(mid),
                filterSearch("3123560", mid));

            cluster.tikaite().add(TIKAITE_URI, TIKAITE_RESPONSE);

            cluster.slowIndexerProxy().register(
                new Pattern<>("", true),
                new StaticHttpItem(HttpStatus.SC_OK));

            cluster.kamaji().start();

            final String updateUriBase = "/update?uid=" + UID;

            cluster.backend().add(
                "/search*",
                new StaticHttpResource(new ProxyHandler(lucene.searchPort())));
            cluster.backend().add(
                "/delete*",
                new StaticHttpResource(new ProxyHandler(lucene.indexerPort())));
            cluster.backend().add(
                updateUriBase + "&fast&mid=" + mid,
                new ProxyHandler(lucene.indexerPort()));

            final String queueId1 = "4136024";
            final String queueId2 = "4136025";
            BasicAsyncRequestProducerGenerator fastGenerator =
                new BasicAsyncRequestProducerGenerator(
                    storeNotifyUri(cluster, queueId1), store(mid));

            HttpResponse fastResponse = client.execute(
                cluster.kamaji().host(),
                fastGenerator,
                EmptyFutureCallback.INSTANCE).get();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, fastResponse);

            // fast, done now slow
            // test acquire lock that already holded
            FastSlowLock localLock =
                new FastSlowLock(
                    Long.parseLong(queueId2),
                    UID + "_m" + mid);
            final LockStorage<String, FastSlowLock> storage =
                cluster.kamaji().lockManager();

            cluster.backend().add(
                updateUriBase + "&mid=" + mid,
                (req, resp, con) -> {
                    storage.acquire(localLock.key(), localLock);
                    resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                },
                new ProxyHandler(lucene.indexerPort()));

            try {
                client.execute(
                    cluster.kamaji().host(),
                    new BasicAsyncRequestProducerGenerator(
                        storeNotifyUri(cluster, queueId1) + SLOW,
                        store(mid)),
                    EmptyFutureCallback.INSTANCE).get();
                Assert.fail();
            } catch (ExecutionException ee) {
                YandexAssert.assertInstanceOf(
                    BadResponseException.class,
                    ee.getCause());
            }

            FastSlowLock lock = storage.acquire(localLock.key(), localLock);

            Assert.assertEquals(
                "Lock not free",
                FastSlowLock.LockStatus.FREE,
                lock.status());

            storage.release(lock.key());
            storage.release(lock.key());
            checkNoLocks(cluster.kamaji());
        }
    }

    private void checkNoLocks(final Kamaji kamaji) throws Exception {
        checkNoLocks(
            kamaji,
            "No holding locks expected, but actually found locks ");
    }

    private void checkNoLocks(
        final Kamaji kamaji,
        final String message)
        throws Exception
    {
        LockStorage<String, FastSlowLock> storage = kamaji.lockManager();
        Field field = LockStorage.class.getDeclaredField("storage");
        field.setAccessible(true);
        Object mapObj = field.get(storage);
        Map<?, ?> map = ValueUtils.asMap(mapObj);
        Assert.assertEquals(message + map.size(), 0, map.size());
    }

    private static final class WaitingCallback
        implements FutureCallback<Object>
    {
        private volatile Status status;

        private WaitingCallback() {
        }

        private Status waitResponse(final long timeout) throws Exception {
            long start = System.currentTimeMillis();
            long passed = 0;
            final long sleep = 50;
            while (status == null && passed < timeout) {
                Thread.sleep(sleep);

                passed = System.currentTimeMillis() - start;
            }

            if (status == null) {
                throw new TimeoutException("Request timeout");
            }

            return status;
        }

        @Override
        public void completed(final Object o) {
            status = Status.OK;
        }

        @Override
        public void failed(final Exception e) {
            e.printStackTrace();
            status = Status.FAILED;
        }

        @Override
        public void cancelled() {
            status = Status.CANCELLED;
        }
    }

    private static class PeachEmulator
        implements HttpRequestHandler, FutureCallback<Object>,
        GenericAutoCloseable<IOException>
    {
        private final AtomicInteger requestCount = new AtomicInteger();
        private final AtomicInteger onAir = new AtomicInteger();
        private final Kamaji kamaji;
        private final int queueSize;
        private volatile Exception fail;
        private final SharedConnectingIOReactor reactor;
        private final AsyncClient client;
        private List<BasicAsyncRequestProducerGenerator> queue =
            new ArrayList<>();

        private PeachEmulator(final Kamaji kamaji) throws Exception {
            this(kamaji, 1);
        }

        private PeachEmulator(
            final Kamaji kamaji,
            final int queueSize)
            throws Exception
        {
            this.kamaji = kamaji;
            this.queueSize = queueSize;
            reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            client = new AsyncClient(
                reactor,
                new HttpTargetConfigBuilder(Configs.targetConfig())
                    .httpRetries(
                        new RetriesConfigBuilder()
                            .count(1)
                            .interval(WAIT_REQUEST_TIMEOUT))
                    .connections(queueSize)
                    .build());
            reactor.start();
            client.start();
        }

        @Override
        public void close() throws IOException {
            client.close();
            reactor.close();
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            requestCount.incrementAndGet();
            onAir.incrementAndGet();

            RequestLine requestLine = request.getRequestLine();
            HttpEntity entity =
                ((HttpEntityEnclosingRequest) request).getEntity();
            ByteArrayEntity nextEntity = CharsetUtils.toDecodable(entity)
                .processWith(ByteArrayEntityFactory.INSTANCE);
            nextEntity.setContentType(entity.getContentType());
            nextEntity.setContentEncoding(entity.getContentEncoding());
            BasicHttpEntityEnclosingRequest nextRequest =
                new BasicHttpEntityEnclosingRequest(requestLine);
            nextRequest.setEntity(nextEntity);

            queue.add(
                new BasicAsyncRequestProducerGenerator(
                requestLine.getUri(),
                nextEntity));
            final long sleep = 50;
            try {
                if (queue.size() >= queueSize) {
                    for (int i = queue.size() - 1; i >= 0; i--) {
                        client.execute(
                            kamaji.host(),
                            queue.get(i),
                            EmptyAsyncConsumerFactory.ANY_GOOD,
                            this);

                        Thread.sleep(sleep);
                    }

                    queue.clear();
                }
            } catch (InterruptedException ie) {
                throw new ServerException(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    ie);
            }

            response.setStatusCode(HttpStatus.SC_OK);
        }

        public synchronized void reset() {
            onAir.set(0);
            fail = null;
            requestCount.set(0);
        }

        @Override
        public synchronized void completed(final Object o) {
            onAir.decrementAndGet();
        }

        @Override
        public synchronized void failed(final Exception e) {
            e.printStackTrace();
            onAir.decrementAndGet();
            fail = e;
        }

        @Override
        public synchronized void cancelled() {
            onAir.decrementAndGet();
        }

        public void waitRequests(final long timeout) throws Exception {
            waitRequests(-1, timeout);
        }

        public void waitRequests(
            final int count,
            final long timeout)
            throws Exception
        {
            long start = System.currentTimeMillis();
            final long sleep = 10;
            while (true) {
                synchronized (this) {
                    if (requestCount.get() >= count && onAir.get() <= 0) {
                        break;
                    }
                }
                Thread.sleep(sleep);
                if (System.currentTimeMillis() - start > timeout) {
                    throw new TimeoutException(
                        "Request count " + requestCount.get()
                            + " on air " + onAir.get());
                }
            }

            if (fail != null) {
                throw fail;
            }
        }
    }
}
