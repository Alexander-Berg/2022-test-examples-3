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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.TestBase;

public class IexProxyCbkRegisterEmlTest extends TestBase {
    private static final String MID = "159314836818238392";
    private static final String UID = "1130000018785180";
    //private static final String STID = "1.632123143.7594801846142779115218810981";
    private static final String MID_PARAM = "\"mid\": ";
    private static final String FID_PARAM = "\"fid\": ";

    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String ENTITY_RETURNED_MSG = "Entity returned:\n";
    private static final String FILTER_SEARCH_URI = "/filter_search?"
        + "order=default&full_folders_and_labels=1&uid=";

    private static final String STORE = "store";

    private StaticServer forwardProxy;
    protected IexProxyCluster cluster;

    @Before
    public void initializeCluster()
        throws Exception
    {
        forwardProxy = new StaticServer(Configs.baseConfig("forward_proxy"));
        forwardProxy.start();
        String extraConfig =
            "[postprocess_rcpt_uid]\n"
                + UID + " = _VOID:" + HTTP_LOCALHOST + IexProxyCluster.IPORT
                + "/cbk_register\n" +
            "[entities_rcpt_uid]\n"
                 + UID + " = entity_for_empty_response\n";

        cluster = new IexProxyCluster(this, null, extraConfig, true, false);
        cluster.iexproxy().start();
        forwardProxy.add("/eml*",
            new ProxyHandler(cluster.iexproxy().host()));

        cluster.blackbox().add(
            "/blackbox*",
            IexProxyCluster.blackboxResponse(
                Long.parseLong(UID),
                "atugaenko@yandex.ru"));

        cluster.onlineDB().add("/online?uid=" + UID, new OnlineHandler(true));
        cluster.msearch().add("/api/async/enlarge/your?uid=" + UID, "");
        cluster.producer().add("/_status*","[{\"localhost\":-1}]");

        cluster.producerAsyncClient().add(
            "/add*",
            new ProxyHandler(cluster.testLucene().indexerPort()));

        cluster.gettext().start();
    }

    @Test
    public void testForwardEml() throws Exception {
        String corovaneerUri = "/eml";

        FileEntity cokeJson = new FileEntity(
            new File(getClass().getResource("coke_null_snippet_response.json").
                toURI()), ContentType.APPLICATION_JSON);

        cluster.cokemulatorIexlib().add(
            "/process?stid=1*",
            new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

        FileEntity eml = new FileEntity(
            new File(
                getClass().getResource("mock_mail.eml").
                    toURI()), ContentType.MULTIPART_FORM_DATA);

        cluster.mulcaGate().add(
            "/*",
            new StaticHttpItem(HttpStatus.SC_OK, eml));

        cluster.corovaneer().add(
                corovaneerUri,
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));
        cluster.gatemail().start();

        cluster.refund().add(
            "/*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("OK")));

        filterSearchMock("storefs.json", MID);
        String changed = '{' + MID_PARAM + MID + ',' + FID_PARAM + "1}";
        sendNotify(changed);

        Assert.assertEquals(1,
            cluster.refund().accessCount("/request-register"));
    }

    @Test
    public void testForwardEmlEmptyBody() throws Exception {
        String corovaneerUri = "/eml";

        FileEntity cokeJson = new FileEntity(
            new File(getClass().getResource("coke_empty_response.json").
                toURI()), ContentType.APPLICATION_JSON);

        cluster.cokemulatorIexlib().add(
            "/process?stid=1*",
            new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

        FileEntity eml = new FileEntity(
            new File(
                getClass().getResource("mock_mail.eml").
                    toURI()), ContentType.MULTIPART_FORM_DATA);

        cluster.mulcaGate().add(
            "/*",
            new StaticHttpItem(HttpStatus.SC_OK, eml));

        cluster.corovaneer().add(
                corovaneerUri,
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));
        cluster.gatemail().start();

        cluster.refund().add(
            "/*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("OK")));

        filterSearchMock("storefs.json", MID);
        String changed = '{' + MID_PARAM + MID + ',' + FID_PARAM + "1}";
        sendNotify(changed);

        Assert.assertEquals(1,
            cluster.refund().accessCount("/request-register"));
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
                System.out.println(ENTITY_RETURNED_MSG + entityString);
            }
        }
    }

    private void filterSearchMock(
        final String file,
        final String... mids)
        throws URISyntaxException, BadRequestException
    {
        FileEntity entity = new FileEntity(
            new File(getClass().getResource(file).toURI()),
            ContentType.APPLICATION_JSON);
        QueryConstructor uri = new QueryConstructor(FILTER_SEARCH_URI + UID + "&mdb=pg");
        for (String mid : mids) {
            uri.append("mids", mid);
        }
        cluster.filterSearch().add(uri.toString(), entity);
    }

}
