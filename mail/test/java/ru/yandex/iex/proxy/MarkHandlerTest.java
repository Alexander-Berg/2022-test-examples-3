package ru.yandex.iex.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.TestBase;

public class MarkHandlerTest extends TestBase {
    private static final long TIMEOUT = 2000;

    protected IexProxyCluster cluster;

    @Before
    public void setupCluster() throws Exception {
        cluster =  new IexProxyCluster(this, true);
        cluster.iexproxy().start();

        // status response mock
        cluster.producer().add(
            "/_status*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("[{\"localhost\":-1}]")));
    }


    @Test
    public void testCgi() throws IOException, URISyntaxException, BadRequestException {
        mockFilterSearch("storefs-1.json", "154", "120001");
        MockedHandle labelHandle = new MockedHandle(cluster.mops(),
                "/label?lids=88&uid=154&mids=120001");
        MockedHandle spamHandle = new MockedHandle(cluster.mops(),
                "/spam?&uid=154&mids=120001");
        mockLabel(154, 88);

        URIBuilder uri = new URIBuilder(cluster.iexproxy().httpHost().toString());
        uri.setPath("/mark");
        uri.addParameter("uid", "154");
        uri.addParameter("mid", "120001");
        uri.addParameter("spam", null);
        uri.addParameter("phishing", null);
        request(new HttpGet(uri.toString()), "1 1");

        labelHandle.waitAndCheckAccess(1);
        spamHandle.waitAndCheckAccess(1);

    }

    @Test
    public void testPost() throws IOException, URISyntaxException, BadRequestException {
        mockLucene(130, 130006, "a3dsf-31csqs", null);
        mockLucene(130, 130007, null, "<msg:id_here>");
        mockFilterSearch("storefs-1-2.json", "154", "120001", "120002");
        mockFilterSearch("storefs-1-2.json", "154", "120002", "120001");
        mockFilterSearch("storefs-6-7.json", "130", "130006", "130007");
        mockFilterSearch("storefs-6-7.json", "130", "130007", "130006");
        MockedHandle uid130 = new MockedHandle(cluster.mops(),
                "/unspam?&uid=130&mids=130006");
        MockedHandle uid154 = new MockedHandle(cluster.mops(),
                "/unspam?&uid=154&mids=120002");

        URIBuilder uri = new URIBuilder(cluster.iexproxy().httpHost().toString());
        uri.setPath("/mark");
        uri.addParameter("spam", "false");
        HttpPost post = new HttpPost(uri.toString());
        post.setEntity(new StringEntity(
            "154 120001\n"
            + "154 120002\n\n"
            + "130 a3dsf-31csqs\n"
            + "130 wrong-smtpid\n"
            + "130 <msg:id_here>\n"));

        request(post, "2 4");

        uid130.waitAndCheckAccess(1);
        uid154.waitAndCheckAccess(1);
    }

    void request(HttpUriRequest request, String expected) throws IOException {
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(request)) {
                Assert.assertEquals(HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(expected,
                    EntityUtils.toString(response.getEntity())) ;
            }
        }
    }

    void mockLucene(
        final long uid,
        final long mid,
        final String queueId,
        final String msgId)
        throws IOException
    {
        StringBuilder sb = new StringBuilder("\"url\":\"");
        sb.append(uid);
        sb.append('_');
        sb.append(mid);
        sb.append("\",\"uid\":");
        sb.append(uid);
        sb.append(",\"mid\":");
        sb.append(mid);
        if (queueId == null) {
            sb.append(",\"msg_id\":\"");
            sb.append(msgId);
            sb.append('"');
        } else {
            sb.append(",\"smtp_id\":\"");
            sb.append(queueId);
            sb.append("\",\"all_smtp_ids\":\"");
            sb.append(queueId);
            sb.append('"');
        }
        cluster.testLucene().add(new LongPrefix(uid), new String(sb));
    }


    private void mockFilterSearch(
        final String file,
        final String uid,
        final String... mids) throws URISyntaxException, BadRequestException
    {
        QueryConstructor uri = new QueryConstructor(
            "/filter_search?order=default&full_folders_and_labels=1");
        uri.append("uid", uid);
        for (String mid : mids) {
            uri.append("mids", mid);
        }
        cluster.filterSearch().add(
            uri.toString(),
            new File(getClass().getResource("mark-handler/" + file).toURI()),
            ContentType.APPLICATION_JSON);
    }

    private void mockLabel(long uid, long lid) {
        cluster.mops().add(
            "/labels/create?uid="+ uid + "&name=73&type=so&strict=0",
            "{\"lid\" : \"" + lid + "\"}");
    }

    static class MockedHandle {
        private final StaticServer server;
        private final String handle;

        MockedHandle(StaticServer server, String handle) {
            this.server = server;
            this.handle = handle;
            server.add(handle, HttpStatus.SC_OK);
        }

        void waitAndCheckAccess(int target) {
            long start = System.currentTimeMillis();
            while (true) {
                try {
                    Assert.assertEquals("Handle " + handle + " access count",
                            target, server.accessCount(handle));
                    break;
                } catch (AssertionError e) {
                    if (System.currentTimeMillis() - start > TIMEOUT) {
                        throw e;
                    }
                }
            }
        }
    }
}
