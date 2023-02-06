package ru.yandex.iex.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.TestBase;

public class RealUpdateHandlerTest extends TestBase {
    private static final String TRUSTED_UID = "204757941";
    private static final String UNTRUSTED_UID = "588355978";
    private static final String LID = "98";
    private static final String NONRELEVANT_LID = "100";
    private static final String LIDS = "40,43,51,52,64,74";
    private static final String TRUSTED_MID = "171418260816791154";
    private static final String UNTRUSTED_MID = "166914661189419402";
    private static final String MID_PARAM = "\"mid\": ";
    private static final String FID_PARAM = "\"fid\": ";
    private static final String LIDS_PARAM = "\"lids\": [";
    private static final String LIDS_ADD = "lids_add";
    private static final String LIDS_DEL = "lids_del";

    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String AXIS_URI = "/v1/facts/store_batch?client_id=extractors";
    private static final String ENTITY_RETURNED_MSG = "Entity returned:\n";
    private static final String FILTER_SEARCH_URI = "/filter_search?order=default&full_folders_and_labels=1&uid=";
    private static final String LABELS_URI = "/labels?caller=msearch&mdb=pg&uid=";
    private static final String COWORKERS_SELECTION_URI = "/coworkers-selection?uid=";

    private static final String PATH = "real-update-handler/";
    private static final String STOREFS_TRUSTED_JSON = "storefs_trusted.json";
    private static final String STOREFS_TRUSTED_LABELED_JSON = "storefs_trusted_labeled.json";
    private static final String STOREFS_TRUSTED_LABELED_NONRELEVANT_JSON = "storefs_trusted_labeled_nonrelevant.json";
    private static final String STOREFS_UNTRUSTED_JSON = "storefs_untrusted.json";
    private static final String STOREFS_UNTRUSTED_LABELED_JSON = "storefs_untrusted_labeled.json";
    private static final String STOREFS_UNTRUSTED_LABELED_NONRELEVANT_JSON =
            "storefs_untrusted_labeled_nonrelevant.json";
    private static final String LABELS_JSON = "labels.json";

    private static final String UPDATE = "update";

    protected IexProxyCluster cluster;

    @Before
    public void initializeCluster() throws Exception {
        org.junit.Assume.assumeTrue(MailStorageCluster.iexUrl() != null);
        cluster = new IexProxyCluster(this, true, true);
        cluster.iexproxy().start();

        // blackbox response mock
        cluster.blackbox().add(
            "/blackbox*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(Long.parseLong(TRUSTED_UID), "klimiky@yandex.ru"))));
        cluster.blackbox().add(
            "/blackbox*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(
                    Long.parseLong(UNTRUSTED_UID), "marija.smolina28test@yandex.ru"))));

        // online handler
        cluster.onlineDB().add("/online?uid=" + TRUSTED_UID, new StaticHttpResource(new OnlineHandler(true)));
        cluster.onlineDB().add("/online?uid=" + UNTRUSTED_UID, new StaticHttpResource(new OnlineHandler(true)));

        // enlarge response mock
        cluster.msearch().add("/api/async/enlarge/your?uid=" + TRUSTED_UID,
                new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));
        cluster.msearch().add("/api/async/enlarge/your?uid=" + UNTRUSTED_UID,
                new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));

        // status response mock
        cluster.producer().add(
            "/_status*", new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("[{\"localhost\":-1}]")));

        // axis response mock
        cluster.axis().add(AXIS_URI, HttpStatus.SC_OK);

        // labels mock
        FileEntity entity = new FileEntity(
            new File(getClass().getResource(PATH + LABELS_JSON).toURI()), ContentType.APPLICATION_JSON);
        cluster.labels().add(LABELS_URI + TRUSTED_UID, new StaticHttpResource(HttpStatus.SC_OK, entity));
        cluster.labels().add(LABELS_URI + UNTRUSTED_UID, new StaticHttpResource(HttpStatus.SC_OK, entity));

        // coworkers selection's handler response mock
        cluster.complaintsCoworkersSelection().add(COWORKERS_SELECTION_URI + TRUSTED_UID + "&*", HttpStatus.SC_OK);
        cluster.complaintsCoworkersSelection().add(COWORKERS_SELECTION_URI + UNTRUSTED_UID + "&*", HttpStatus.SC_OK);

        // producerAsyncClient add requests handlers
        cluster.producerAsyncClient().add("/notify*", new ProxyHandler(cluster.iexproxy().port()));
        cluster.producerAsyncClient().add(
                "/add*",
                new ExpectingHeaderHttpItem(
                        new ProxyHandler(cluster.testLucene().indexerPort()),
                        YandexHeaders.X_INDEX_OPERATION_QUEUE,
                        cluster.iexproxy().xIndexOperationQueueNameUpdate())
        );
    }

    private void testOneMsg(
            final String storefsJson,
            final String mid,
            final String uid,
            final String lid,
            final boolean label,
            final int labelsCnt,
            final int coworkersSelectionCnt)
            throws Exception
    {
        String lidsDel = "";
        String lidsAdd = "";
        String lids = LIDS;
        if (label) {
            lidsAdd = lid;
            lids += ',' + lid;
        } else {
            lidsDel = lid;
        }
        // add label to msg
        filterSearchMock(storefsJson, true, uid, mid);
        String arguments = "\"deleted\":null,\"" + LIDS_DEL + "\":[" + lidsDel + "],\"recent\":null,\""
                + LIDS_ADD + "\":[" + lidsAdd + "],\"seen\":false";
        String changed = '{' + MID_PARAM + mid + ',' + FID_PARAM + "1,\"deleted\":false,\"tab\":\"news\","
                + "\"recent\":true," + LIDS_PARAM + lids + "],\"seen\":true,\"tid\":" + mid + "}";
        sendNotify(changed, arguments, UPDATE, uid);
        cluster.waitProducerRequests(cluster.labels(), LABELS_URI + uid, labelsCnt);
        cluster.waitProducerRequests(
                cluster.complaintsCoworkersSelection(), COWORKERS_SELECTION_URI + uid + "&*", coworkersSelectionCnt);
    }

    @Test
    public void testUntrustedUserRelevantLabelOneMsg() throws Exception {
        testOneMsg(STOREFS_UNTRUSTED_LABELED_JSON, UNTRUSTED_MID, UNTRUSTED_UID, LID, true, 0, 0);
    }

    @Test
    public void testUntrustedUserNonRelevantLabelOneMsg() throws Exception {
        testOneMsg(STOREFS_UNTRUSTED_LABELED_NONRELEVANT_JSON, UNTRUSTED_MID, UNTRUSTED_UID, NONRELEVANT_LID, true,
                0, 0);
    }

    @Test
    public void testUntrustedUserRelevantUnlabelOneMsg() throws Exception {
        testOneMsg(STOREFS_UNTRUSTED_JSON, UNTRUSTED_MID, UNTRUSTED_UID, LID, false, 0, 0);
    }

    @Test
    public void testUntrustedUserNonRelevantUnlabelOneMsg() throws Exception {
        testOneMsg(STOREFS_UNTRUSTED_JSON, UNTRUSTED_MID, UNTRUSTED_UID, NONRELEVANT_LID, false, 0, 0);
    }

    @Test
    public void testTrustedUserRelevantLabelOneMsg() throws Exception {
        testOneMsg(STOREFS_TRUSTED_LABELED_JSON, TRUSTED_MID, TRUSTED_UID, LID, true, 1, 1);
    }

    @Test
    public void testTrustedUserNonRelevantLabelOneMsg() throws Exception {
        testOneMsg(STOREFS_TRUSTED_LABELED_NONRELEVANT_JSON, TRUSTED_MID, TRUSTED_UID, NONRELEVANT_LID, true, 1, 0);
    }

    @Test
    public void testTrustedUserRelevantUnlabelOneMsg() throws Exception {
        testOneMsg(STOREFS_TRUSTED_JSON, TRUSTED_MID, TRUSTED_UID, LID, false, 1, 1);
    }

    @Test
    public void testTrustedUserNonRelevantUnlabelOneMsg() throws Exception {
        testOneMsg(STOREFS_TRUSTED_JSON, TRUSTED_MID, TRUSTED_UID, NONRELEVANT_LID, false, 1, 0);
    }

    private void sendNotify(final String changed, final String arguments, final String changeType, final String uid)
            throws IOException, HttpException
    {
        // /notify?mdb=pg&pgshard=2708&operation-id=108277845&operation-date=1580828198.306575&uid=204757941
        // &change-type=update&changed-size=1&batch-size=1&salo-worker=pg2708:7&transfer-timestamp=1580828198427
        // &zoo-queue-id=10759297&deadline=1580828208548
        //
        // {"select_date":"1580828198.422","uid":"204757941","pgshard":"2708","lcn":"5492","fresh_count":"0",
        //  "operation_date":"1580828198.306575","operation_id":"108277845","change_type":"update","arguments":
        //  {"deleted":null,"lids_del":[],"recent":null,"lids_add":[98],"seen":null},"useful_new_messages":"0",
        //  "changed":[{"fid":1,"deleted":false,"tab":"news","mid":171418260816791154,"recent":true,
        //      "lids":[40,43,51,52,64,74,98],"seen":true,"tid":171418260816791154}]}
        String uri = "/notify?mdb=pg&pgshard=2708&operation-id=108277845&operation-date=1580828198.306575"
                + "&zoo-queue-id=10759297&changed-size=1&batch-size=1&change-type=" + changeType + "&uid=" + uid
                + "&salo-worker=pg2708:7&transfer-timestamp=1580828198427&deadline=1580828208548";
        String body = "{\"select_date\":\"1580828198.422\",\"uid\": " + uid + ",\"pgshard\":\"2708\","
                + "\"lcn\":\"5492\",\"fresh_count\":\"0\",\"operation_date\":1580828198.306575,\"operation_id"
                + "\":\"108277845\",\"change_type\":\"" + changeType + "\",\"arguments\":{" + arguments + "},"
                + "\"useful_new_messages\":\"0\",\"changed\":[" + changed + "],\"db_user\":\"mops\","
                + "\"session_key\":\"LIZA-12345678-1234567891011\"}";
        logger.info("sendNotify BODY: " + body);
        HttpPost post = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + uri);
        post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        post.addHeader(YandexHeaders.ZOO_QUEUE, changeType);
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                String entityString = CharsetUtils.toString(response.getEntity());
                logger.info(ENTITY_RETURNED_MSG + entityString);
            }
        }
    }

    private void filterSearchMock(final String file, final boolean mdb, final String uid, final String... mids)
            throws URISyntaxException, BadRequestException
    {
        FileEntity entity = new FileEntity(
            new File(getClass().getResource(PATH + file).toURI()), ContentType.APPLICATION_JSON);
        QueryConstructor uri = new QueryConstructor(FILTER_SEARCH_URI + uid);
        if (mdb) {
            uri.append("mdb", "pg");
        }
        for (String mid : mids) {
            uri.append("mids", mid);
        }
        cluster.filterSearch().add(uri.toString(), new StaticHttpResource(HttpStatus.SC_OK, entity));
    }
}
