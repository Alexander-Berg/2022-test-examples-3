package ru.yandex.iex.proxy;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class IexProxyPkpassTest extends TestBase {
    private static final String PATH = "pkpass/";
    //private static final long UID_VALUE = 1130000013896717L;
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String FILTER_SEARCH =
            "/filter_search?order=default&full_folders_and_labels=1&uid=";
    private static boolean rewriteExpectedJson = false;
    private static final String PKPASS_FS_ANS_2 = "pkpass_FS_ans_2.json";
    private static final String CONFIG_EXTRA =
        "postprocess.default2 = events:http://localhost:"
        + IexProxyCluster.IPORT + "/events\n";

    @Test
    public void testPkpass1() throws Exception {
        genericPkpassTest(
            "pkpass_raw_iexlib_1.json",
            "pkpass_returnedby_iexproxy_1.json");
    }

    @Test
    public void testPkpass2() throws Exception {
        genericPkpassTest(
            "pkpass_raw_iexlib_2.json",
            "pkpass_returnedby_iexproxy_2.json");
    }

    @Test
    public void testPkpass3() throws Exception {
        genericPkpassTest(
            "pkpass_raw_iexlib_3.json",
            "pkpass_returnedby_iexproxy_3.json");
    }

    @Test
    public void testPkpass4() throws Exception {
        genericPkpassTest(
            "pkpass_raw_iexlib_4.json",
            "pkpass_returnedby_iexproxy_4.json");
    }

    @Test
    public void testPkpass5() throws Exception {
        genericPkpassTest(
            "pkpass_raw_iexlib_5.json",
            "pkpass_returnedby_iexproxy_5.json");
    }

    @Test
    public void testPkpass6() throws Exception {
        genericPkpassTest(
            "pkpass_raw_iexlib_6.json",
            "pkpass_returnedby_iexproxy_6.json");
    }

    @Test
    public void testPkpass7() throws Exception {
        genericPkpassTest(
            "pkpass_raw_iexlib_7.json",
            "pkpass_returnedby_iexproxy_7.json");
    }

    @Test
    public void testPkpassThroughFacts() throws Exception {
        genericPkpassTestThroughFactshandle(
            "pkpass_raw_iexlib_8.json",
            "pkpass_tikaite_response_8.json",
            "pkpass_FS_ans_1.json",
            "pkpass_returnedby_iexproxy_8.json");
    }

    @Test
    public void testMicroPkpassThroughFacts() throws Exception {
        genericPkpassTestThroughFactshandle(
            "pkpass_raw_iexlib_9.json",
            "pkpass_tikaite_response_9.json",
            PKPASS_FS_ANS_2,
            "pkpass_returnedby_iexproxy_9.json");
    }

    @Test
    public void testMicro2PkpassThroughFacts() throws Exception {
        genericPkpassTestThroughFactshandle(
            "pkpass_raw_iexlib_micro.json",
            "pkpass_tikaite_response_micro.json",
            PKPASS_FS_ANS_2,
            "pkpass_returnedby_iexproxy_micro.json");
    }

    //CSOFF: ParameterNumber
    void genericPkpassTestThroughFactshandle(
        final String iexlibFile,
        final String tikaiteFile,
        final String fsFile,
        final String postResFile)
        throws Exception
    {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            HttpGet get = createHttpGet(cluster);
            setEnvironmentForFactsHandle(
                cluster,
                PATH + fsFile,
                PATH + iexlibFile,
                PATH + tikaiteFile);
            cluster.iexproxy().start();
            try (CloseableHttpResponse response = client.execute(get)) {
                String returned =
                    CharsetUtils.toString(response.getEntity());
                cluster.compareJson(
                    PATH + postResFile,
                    returned,
                    rewriteExpectedJson);
            }
        }
    }

    void setEnvironmentForFactsHandle(
        final IexProxyCluster cluster,
        final String fsFile,
        final String iexlibFile,
        final String tikaiteFile)
        throws Exception
    {
        final long uid = 123;
        //1. Statistic - Golovan
        cluster.producer().add("/_status*", "localhost");
        //2. BB
        String bbUri = "/blackbox/?*";
        String bbResponse =
            IexProxyCluster.blackboxResponse(uid, "a@b", "80019");
        bbResponse = bbResponse.substring(0, bbResponse.length() - (2 + 2))
            + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
        cluster.blackbox().add(
            bbUri,
            new StaticHttpItem(bbResponse),
            new StaticHttpItem(bbResponse));
        //3. FS
        FileEntity entity = new FileEntity(
            new File(getClass().getResource(fsFile).toURI()),
            ContentType.APPLICATION_JSON);
        String fsUri = FILTER_SEARCH + uid + '*';
        cluster.filterSearch().add(
            fsUri,
            new StaticHttpItem(HttpStatus.SC_OK, entity),
            new StaticHttpItem(HttpStatus.SC_OK, entity),
            new StaticHttpItem(HttpStatus.SC_OK, entity),
            new StaticHttpItem(HttpStatus.SC_OK, entity));
        //4. Iexlib
        FileEntity entityFromCoke = new FileEntity(
            new File(getClass().
                getResource(iexlibFile).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.cokemulatorIexlib().add(
            "/process?*",
            new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke),
            new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
        if (tikaiteFile != null) {
            cluster.tikaite().add(
                "/tikaite?json-type=dollar&stid=*",
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    new FileEntity(
                        new File(getClass().getResource(tikaiteFile).toURI()),
                        ContentType.APPLICATION_JSON)));
        }
        //5. Other than that
        cluster.producerAsyncClient().add(
            "/modify*",
            new ProxyHandler(cluster.testLucene().indexerPort()));
        cluster.producerAsyncClient().add(
            "/add*",
            new ProxyHandler(cluster.testLucene().indexerPort()));
    }
    //CSON: ParameterNumber

    void genericPkpassTest(
        final String inputCokeSolution,
        final String outputExpectedSolution) throws Exception
    {
        try (IexProxyCluster cluster = new IexProxyCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            HttpPost post = createHttpPost(cluster, PATH + inputCokeSolution);
            cluster.iexproxy().start();
            try (CloseableHttpResponse response = client.execute(post)) {
                String returned =
                    CharsetUtils.toString(response.getEntity());
                cluster.compareJson(
                    PATH + outputExpectedSolution,
                    returned,
                    rewriteExpectedJson);
            }
        }
    }

    HttpPost createHttpPost(final IexProxyCluster cluster, final String file)
        throws Exception
    {
        HttpPost post = new HttpPost(
            HTTP_LOCALHOST + cluster.iexproxy().port() + "/pkpass?"
            + "subject=subjecthere&uid=4002720415&mid=158470411888099553"
            + "&email=ren.prs@yandex.ru"
            + "&received_date=1231231213&domain=s7.ru");
        post.setEntity(
            new FileEntity(
                new File(getClass()
                    .getResource(file).toURI()),
                ContentType.APPLICATION_JSON));
        return post;
    }

    HttpGet createHttpGet(final IexProxyCluster cluster)
        throws Exception
    {
        HttpGet get = new HttpGet(
            HTTP_LOCALHOST + cluster.iexproxy().port()
            + "/facts?uid=26521462&mid=158751886864839104&ignore_cache");
        return get;
    }

    @SuppressWarnings("unused")
    String readExpectedJson(final String file) throws Exception {
        Path path = Paths.get(
            getClass().getResource(
                file).toURI());
        String expected = java.nio.file.Files.readString(path);
        return expected;
    }

    @SuppressWarnings("unused")
    void compareJson(final String returned, final String expected) {
        String result = new JsonChecker(expected)
            .check(returned);
        if (result != null) {
            Assert.fail(result);
        }
    }
}
