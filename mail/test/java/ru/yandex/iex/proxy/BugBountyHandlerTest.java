package ru.yandex.iex.proxy;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class BugBountyHandlerTest extends TestBase {
    private static final String MID = "159314836818238392";
    private static final String UID = "588355978";
    private static final String MID_PARAM = "\"mid\": ";
    private static final String FID_PARAM = "\"fid\": ";

    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String ENTITY_RETURNED_MSG = "Entity returned:\n";

    private static final String STORE = "store";

    protected IexProxyCluster cluster;

    @Before
    public void initializeCluster()
        throws Exception
    {
        String extraConfig =
            "[extrasettings]\n"
            + "bugbounty-secret = cxHybNsoqgB8mmcjqtRCRg\n"
            + "[entities_rcpt_email]\n"
            + "hirthwork@yandex$ru = contentline\n"
            + "[postprocess_rcpt_email]\n"
            + "hirthwork@yandex$ru = _VOID:http://localhost:"
            + IexProxyCluster.IPORT + "/sobb\n";

        cluster = new IexProxyCluster(this, null, extraConfig);
        cluster.iexproxy().start();

        // blackbox response mock
        cluster.blackbox().add(
            "/blackbox*",
            IexProxyCluster.blackboxResponse(
                Long.parseLong(UID),
                "klimiky@yandex.ru"));

        // online handler
        cluster.onlineDB().add(
            "/online?uid=" + UID,
            new StaticHttpResource(new OnlineHandler(true)));

        // enlarge response mock
        cluster.msearch().add("/api/async/enlarge/your?uid=" + UID, "");

        cluster.producerAsyncClient().register(
            new Pattern<>("/add", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);

        cluster.gettext().start();

        cluster.cokemulatorIexlib().add(
            "/process?*",
            "{\"contentline\":{\"weight\":1,\"text\":\"\"}}");
    }

    @Test
    public void testBugBounty() throws Exception {
        IexProxyTestMocks.filterSearchMock(cluster, "storefs.json", Long.parseUnsignedLong(UID), MID);
        String producerUri = "/update?prefix=4001517835&service=change_log";
        cluster.producerAsyncClient().add(
            producerUri,
             new ExpectingHttpItem(
                 new JsonChecker("{\n"
                     + "\"prefix\": 4001517835,\n"
                     + "\"AddIfNotExists\": true,\n"
                     + "\"docs\": [\n"
                     + "  {\n"
                     + "    \"url\": \"sobb_record_4001517835_" + UID + "\",\n"
                     + "    \"sobb_inbox\": {\n"
                     + "      \"function\": \"inc\"\n"
                     + "    }\n"
                     + "  }\n"
                     + "]\n"
                     + "}")));
        cluster.gettext().add(
            "/get-text?mid=" + MID + "&uid=" + UID,
            "Pong Qh_XyW53xCl4VXSBVRRdafg");

        String changed = '{' + MID_PARAM + MID + ',' + FID_PARAM + "1}";
        sendNotify(changed);

        Assert.assertEquals(
            1,
            cluster.producerAsyncClient().accessCount(producerUri));

        // Wrong token
        cluster.gettext().add(
            "/get-text?mid=" + MID + "&uid=" + UID,
            "Pong Qh_Wrong_Cl4VXSBVRRdafg");

        sendNotify(changed);

        // No more changes expected
        Assert.assertEquals(
            1,
            cluster.producerAsyncClient().accessCount(producerUri));
    }

    private void sendNotify(final String changed)
        throws IOException, HttpException
    {
        String uri = "/notify?mdb=pg&service=change_log"
            + "&zoo-queue-id=7799292&change-type=" + STORE + "&uid=" + UID;
        String body = '{'
            + "\"uid\": " + UID + ','
            + "\"change_type\": \"" + STORE + "\","
            + "\"operation_date\": 1580828198,"
            + "\"mdb\": \"pg\","
            + "\"changed\": [" + changed + "]}";
        HttpPost post = new HttpPost(HTTP_LOCALHOST
            + cluster.iexproxy().port() + uri);
        post.setEntity(new StringEntity(body));
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info(ENTITY_RETURNED_MSG + entityString);
            }
        }
    }
}
