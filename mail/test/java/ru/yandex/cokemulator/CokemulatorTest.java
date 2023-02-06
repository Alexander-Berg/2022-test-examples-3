package ru.yandex.cokemulator;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.jniwrapper.JniWrapperConfigBuilder;
import ru.yandex.jniwrapper.JniWrapperException;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.storage.C2CDataExtractor;
import ru.yandex.util.storage.DataExtractorConfigBuilder;

public class CokemulatorTest extends TestBase {
    private static final String COPY_FUNC = "jniwrapper_test_copy";
    private static final String COPY_META = "jniwrapper_test_copy_meta";
    private static final String SUM_FUNC = "jniwrapper_test_sum";
    private static final String BAD_FUNC = "jniwrapper_test_bad";
    private static final String HTML = "html.eml";
    private static final String RAW = "?raw";
    private static final String GET_HTML = "/get/" + HTML + RAW;
    private static final String PROCESS_HTML = "/process?stid=" + HTML;
    private static final String HTML_META = "text/html; charset=utf-8";
    private static final String HTML_CONTENT =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 "
        + "Transitional//EN\">\n"
        + "<HTML>\n"
        + "  <head>\n"
        + "    <title>some title</title>\n"
        + "  </head>\n"
        + "  <!-- some comment here-->\n"
        + "  <BODY>\n"
        + "    <style type=\"text/css\">img "
        + "{border:0;display:block;}</style>\n"
        + "    <p>some body</p>\n"
        + "    <blockquote>quoted text</blockquote>\n"
        + "    after quotation\n"
        + "  </body>\n"
        + "</html>\n";
    private static final String TRUTH_BEAUTY =
        "If you believe that truth=beauty, then surely mathematics"
        + " is the most beautiful branch of philosophy.\r\n";
    private static final long TVM_RENEWAL_INTERVAL = 60000L;

    public CokemulatorTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .ctorName("jniwrapper_test_copy_config_ctor")
                            .dtorName("jniwrapper_test_dtor")
                            .mainName(SUM_FUNC)
                            .config("hello"))
                    .uriSuffix("my=param&another=param")))
        {
            cluster.storage().add(
                "/get/sum?raw&my=param&another=param",
                "abcd");
            cluster.start();
            for (int i = 0; i < 2; ++i) {
                try (CloseableHttpResponse response = client.execute(
                        new HttpGet(cluster.uri() + "/process?stid=sum")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals(
                        'a' + 'b' + 'c' + 'd' + 'h' + 'e' + 'l' + 'l' + 'o',
                        Integer.parseInt(
                            CharsetUtils.toString(response.getEntity())));
                }
            }
        }
        // Force ThreadLocalJniWrappers.Instance finalization
        System.gc();
    }

    @Test
    public void testCopy() throws Exception {
        final int limit = 21;
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataExtractorConfig(
                        new DataExtractorConfigBuilder()
                            .maxInputLength(limit)
                            .build())))
        {
            String data = "Привет, мир!";
            cluster.storage().add("/get/copy?raw", data);
            cluster.storage().add("/get/too-long?raw", "Не влезает жe");
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + "/process?stid=copy")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    data,
                    CharsetUtils.toString(response.getEntity()));
            }
            HttpAssert.assertStatusCode(
                HttpStatus.SC_REQUEST_TOO_LONG,
                client,
                new HttpGet(cluster.uri() + "/process?stid=too-long"));
        }
    }

    @Test
    public void testTruncate() throws Exception {
        final int limit = 5;
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataExtractorConfig(
                        new DataExtractorConfigBuilder()
                            .maxInputLength(limit)
                            .truncateLongInput(true)
                            .build())))
        {
            cluster.storage().add("/get/truncate?raw", "Hello, world!");
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + "/process?stid=truncate")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "Hello",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private void testLogrotate(final int workers) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .ctorName("jniwrapper_test_counter_ctor")
                            .mainName("jniwrapper_test_counter_to_string")
                            .logrotateName("jniwrapper_test_increment")
                            .workers(workers)
                            .queueSize(workers << 1))))
        {
            cluster.storage().add("/get/nothing-useful?raw", "Hi!");
            cluster.start();
            String uri = cluster.uri() + "/process?stid=nothing-useful";
            final long sleep = 200L;
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    Integer.toString(0),
                    CharsetUtils.toString(response.getEntity()));
            }
            for (int i = 0; i <= 2; ++i) {
                Thread.sleep(sleep);
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client,
                    new HttpGet(cluster.uri() + "/logrotate"));
            }
            Thread.sleep(sleep);
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "3",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testLogrotate() throws Exception {
        // It is assumed that all sequental requests will be processed by the
        // same thread
        testLogrotate(1);
        // thread safe case
        testLogrotate(0);
    }

    private void testReload(final int workers) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .ctorName("jniwrapper_test_counter_ctor")
                            .mainName("jniwrapper_test_counter_to_string")
                            .reloadName("jniwrapper_test_increment")
                            .workers(workers)
                            .queueSize(workers << 1))))
        {
            cluster.storage().add("/get/nothing-useful?raw", "Hi!");
            cluster.start();
            String uri = cluster.uri() + "/process?stid=nothing-useful";
            final long sleep = 200L;
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    Integer.toString(0),
                    CharsetUtils.toString(response.getEntity()));
            }
            for (int i = 0; i <= 2; ++i) {
                Thread.sleep(sleep);
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client,
                    new HttpGet(cluster.uri() + "/reload"));
            }
            Thread.sleep(sleep);
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "3",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testReload() throws Exception {
        // It is assumed that all sequental requests will be processed by the
        // same thread
        testReload(1);
        // thread safe case
        testReload(0);
    }

    @Test
    public void testFree() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .mainName(
                                "jniwrapper_test_global_counter_to_string")
                            .freeName(
                                "jniwrapper_test_increment_global_counter"))))
        {
            cluster.storage().add("/get/free?raw", "Let me out of here!");
            cluster.start();
            String uri = cluster.uri() + "/process?stid=free";
            for (int i = 0; i <= 2; ++i) {
                try (CloseableHttpResponse response =
                        client.execute(new HttpGet(uri)))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals(
                        Integer.toString(i),
                        CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }

    @Test
    public void testCopyUri() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .mainName("jniwrapper_test_copy_uri"))))
        {
            cluster.storage().add("/get/copy-uri?raw", "Useless");
            cluster.start();
            String uri = "/process?stid=copy-uri&params-here";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    uri,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testConcatUriMeta() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .mainName("jniwrapper_test_concat_uri_meta"))
                    .dataType(DataType.MAIL_TEXT)))
        {
            cluster.storage().add(
                "/get/concat-uri-meta?raw",
                new File(CokemulatorTest.class.getResource(HTML).toURI()));
            cluster.start();
            String uri = "/process?stid=concat-uri-meta&params-here";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "\uD83E\uDD11 сегодня \uD83E\uDD10: " + uri + HTML_META,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testConcatUriMeta16() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .main16Name("jniwrapper_test_concat_uri_meta16"))
                    .dataType(DataType.MAIL_TEXT)))
        {
            cluster.storage().add(
                "/get/concat-uri-meta16?raw",
                new File(CokemulatorTest.class.getResource(HTML).toURI()));
            cluster.start();
            String uri = "/process?stid=concat-uri-meta16&params-here";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "\uD83E\uDD10 уже \uD83E\uDD11: " + uri + HTML_META,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFail() throws Exception {
        try (CokemulatorCluster cluster =
                new CokemulatorCluster("jniwrapper_test_fail"))
        {
            cluster.storage().add("/get/fail?raw", "Error");
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_SERVICE_UNAVAILABLE,
                cluster.cokemulator().port(),
                "/process?stid=fail");
        }
    }

    @Test
    public void testOOM() throws Exception {
        try (CokemulatorCluster cluster =
                new CokemulatorCluster("jniwrapper_test_oom"))
        {
            cluster.storage().add("/get/oom?raw", "OOM");
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_SERVICE_UNAVAILABLE,
                cluster.cokemulator().port(),
                "/process?stid=oom");
        }
    }

    @Test
    public void testBad() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .mainName(BAD_FUNC)
                            .workers(0))))
        {
            cluster.storage().add("/get/bad?raw", "Very bad input");
            cluster.start();
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cokemulator().host()
                            + "/process?stid=bad")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                    response);
                YandexAssert.assertContains(
                    "Bad input",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBad16() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .main16Name(BAD_FUNC + 16)
                            .workers(0))))
        {
            cluster.storage().add("/get/bad?raw", "Very bad input");
            cluster.start();
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cokemulator().host()
                            + "/process?stid=bad")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                    response);
                YandexAssert.assertContains(
                    "Bad16 input",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testNotFound() throws Exception {
        try (CokemulatorCluster cluster = new CokemulatorCluster(COPY_FUNC)) {
            cluster.storage().add("/get/absent?raw", HttpStatus.SC_NOT_FOUND);
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_NOT_FOUND,
                cluster.cokemulator().port(),
                "/process?stid=absent");
        }
    }

    @Test
    public void testBadGateway() throws Exception {
        try (CokemulatorCluster cluster = new CokemulatorCluster(COPY_FUNC)) {
            cluster.storage().add("/get/forbid?raw", HttpStatus.SC_FORBIDDEN);
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_GATEWAY,
                cluster.cokemulator().port(),
                "/process?stid=forbid");
        }
    }

    @Test
    public void testStorageUnavailable() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(COPY_FUNC))
        {
            cluster.cokemulator().start();
            String uri =
                cluster.cokemulator().host() + "/process?stid=stopped";
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_GATEWAY_TIMEOUT,
                    response);
            }
        }
    }

    @Test
    public void testNonLoadable() throws Exception {
        try {
            new CokemulatorCluster("there is no such func");
            Assert.fail();
        } catch (ConfigException e) {
            YandexAssert.assertInstanceOf(
                JniWrapperException.class,
                e.getCause());
        }
    }

    @Test
    public void testBadCtor() throws Exception {
        try {
            new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder()
                            .ctorName("jniwrapper_test_bad_ctor")
                            .mainName(COPY_FUNC)
                            .config("test")));
            Assert.fail();
        } catch (ConfigException e) {
            YandexAssert.assertInstanceOf(
                JniWrapperException.class,
                e.getCause());
        }
    }

    @Test
    public void testMail() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataType(DataType.MAIL_TEXT)))
        {
            cluster.storage().add(
                GET_HTML,
                new File(CokemulatorTest.class.getResource(HTML).toURI()));
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + PROCESS_HTML)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    HTML_CONTENT,
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.storage().add(
                "/get/html-xmlless.eml?raw",
                new File(
                    CokemulatorTest.class.getResource("html-xmlless.eml")
                        .toURI()));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.uri() + "/process?stid=html-xmlless.eml")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    HTML_CONTENT,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testGetText() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))))
        {
            String text = "Plain text here";
            cluster.storage().add("/get/plaintext?raw", text);
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + "/get-text?stid=plaintext")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertNull(
                    response.getFirstHeader(C2CDataExtractor.METAINFO));
                Assert.assertEquals(
                    text,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMailGetText() throws Exception {
        CokemulatorConfigBuilder builder = new CokemulatorConfigBuilder()
            .jniWrapperConfig(
                new JniWrapperConfigBuilder().mainName(COPY_FUNC))
            .dataType(DataType.MAIL_TEXT);
        builder.dataExtractorConfig().charset(StandardCharsets.UTF_8);
        try (CokemulatorCluster cluster = new CokemulatorCluster(builder)) {
            cluster.storage().add(
                GET_HTML,
                new File(CokemulatorTest.class.getResource(HTML).toURI()));
            cluster.start();
            try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + "/get-text?stid=html.eml")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    C2CDataExtractor.METAINFO,
                    HTML_META,
                    response);
                Assert.assertEquals(
                    HTML_CONTENT,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testC2C() throws Exception {
        try (CokemulatorCluster cluster = new CokemulatorCluster(BAD_FUNC);
            StaticServer mulcagate =
                new StaticServer(Configs.baseConfig("Mulcagate")))
        {
            cluster.storage().add("/get/c2c?raw", "fdef");
            cluster.tikaite().add(
                "/text?encoding=auto&stid=c2c&hid=1&raw",
                "fuf");
            cluster.start();
            mulcagate.add(
                "/gate/dist-info/c2c?primary-only",
                "localhost\tfol\tprimary\n");
            mulcagate.start();
            CokemulatorConfigBuilder builder = new CokemulatorConfigBuilder()
                .name("C2C")
                .jniWrapperConfig(
                    new JniWrapperConfigBuilder().mainName(SUM_FUNC))
                .dataType(DataType.C2C)
                .mulcagateConfig(Configs.hostConfig(mulcagate))
                .cokemulatorPort(cluster.cokemulator().port());
            try (CokemulatorCluster c2c = new CokemulatorCluster(builder)) {
                c2c.start();
                try (CloseableHttpClient client = HttpClients.createDefault();
                    CloseableHttpResponse response = client.execute(
                        new HttpGet(c2c.uri() + "/process?stid=c2c")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals(
                        'f' + 'd' + 'e' + 'f',
                        Integer.parseInt(
                            CharsetUtils.toString(response.getEntity())));
                }
                try (CloseableHttpClient client = HttpClients.createDefault();
                    CloseableHttpResponse response = client.execute(
                        new HttpGet(c2c.uri() + "/process?stid=c2c&hid=1")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals(
                        'f' + 'u' + 'f',
                        Integer.parseInt(
                            CharsetUtils.toString(response.getEntity())));
                }
            }
        }
    }

    @Test
    public void testMailC2C() throws Exception {
        CokemulatorConfigBuilder builder = new CokemulatorConfigBuilder()
            .jniWrapperConfig(
                new JniWrapperConfigBuilder().mainName(COPY_FUNC))
            .dataType(DataType.MAIL_TEXT);
        try (CokemulatorCluster cluster = new CokemulatorCluster(builder);
            StaticServer mulcagate =
                new StaticServer(Configs.baseConfig("Mulcagate-Mail")))
        {
            cluster.storage().add(
                "/get/c2c-mail?raw",
                new File(CokemulatorTest.class.getResource(HTML).toURI()));
            cluster.start();
            mulcagate.add(
                "/gate/dist-info/c2c-mail?primary-only",
                "localhost\tsas\tprimary\n");
            mulcagate.start();
            builder = new CokemulatorConfigBuilder()
                .name("C2C-Mail")
                .jniWrapperConfig(
                    new JniWrapperConfigBuilder().mainName(COPY_META))
                .dataType(DataType.C2C)
                .uriSuffix("some-suffix")
                .mulcagateConfig(Configs.hostConfig(mulcagate))
                .cokemulatorPort(cluster.cokemulator().port());
            try (CokemulatorCluster c2c = new CokemulatorCluster(builder)) {
                c2c.start();
                try (CloseableHttpClient client = HttpClients.createDefault();
                    CloseableHttpResponse response = client.execute(
                        new HttpGet(c2c.uri() + "/process?stid=c2c-mail")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals(
                        HTML_META,
                        CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }

    @Test
    public void testMailC2Srw() throws Exception {
        CokemulatorConfigBuilder builder = new CokemulatorConfigBuilder()
            .jniWrapperConfig(
                new JniWrapperConfigBuilder().mainName(COPY_META))
            .dataType(DataType.MAIL_TEXT);
        try (CokemulatorCluster cluster = new CokemulatorCluster(builder);
            StaticServer tvm = new StaticServer(Configs.baseConfig("TVM")))
        {
            cluster.storage().add(
                "/get/c2srw-mail?raw",
                new File(CokemulatorTest.class.getResource(HTML).toURI()));
            cluster.start();
            tvm.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm.add(
                "/2/ticket/",
                "{\"4\":{\"ticket\":\"3:serv:COKEEPLn\"}"
                + ",\"5\":{\"ticket\":\"3:serv:UNISTORA\"}}");
            tvm.start();

            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm))
                .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret("1234567890123456789011");

            Tvm2ClientConfigBuilder tvm2ClientConfig =
                new Tvm2ClientConfigBuilder()
                    .destinationClientId("4,5")
                    .renewalInterval(TVM_RENEWAL_INTERVAL);

            builder = new CokemulatorConfigBuilder()
                .name("C2Srw-Mail")
                .jniWrapperConfig(
                    new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                .dataType(DataType.C2SRW)
                .storageConfig(Configs.hostConfig(cluster.cokemulator()))
                .tvm2ServiceConfig(tvm2ServiceConfig)
                .tvm2ClientConfig(tvm2ClientConfig)
                .apeClientId("4")
                .unistorageClientId("5");
            try (CokemulatorCluster c2srw = new CokemulatorCluster(builder)) {
                c2srw.start();
                try (CloseableHttpClient client = HttpClients.createDefault();
                    CloseableHttpResponse response = client.execute(
                        new HttpGet(c2srw.uri() + "/process?stid=c2srw-mail")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals(
                        HTML_CONTENT,
                        CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }

    @Test
    public void testMailTruncate() throws Exception {
        final int limit = 10;
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataType(DataType.MAIL_TEXT)
                    .dataExtractorConfig(
                        new DataExtractorConfigBuilder()
                            .maxInputLength(limit)
                            .truncateLongInput(true)
                            .build())
                    .uriSuffix("my=param")))
        {
            cluster.storage().add(
                GET_HTML + "&my=param",
                new File(CokemulatorTest.class.getResource(HTML).toURI()));
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + PROCESS_HTML)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "<!DOCTYPE ",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMailTruncateNotAllowed() throws Exception {
        final int limit = 10;
        try (CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataType(DataType.MAIL_TEXT)
                    .dataExtractorConfig(
                        new DataExtractorConfigBuilder()
                            .maxInputLength(limit)
                            .build())))
        {
            cluster.storage().add(
                GET_HTML,
                new File(CokemulatorTest.class.getResource(HTML).toURI()));
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                cluster.cokemulator().port(),
                PROCESS_HTML);
        }
    }

    @Test
    public void testMailMeta() throws Exception {
        CokemulatorConfigBuilder builder = new CokemulatorConfigBuilder()
            .jniWrapperConfig(
                new JniWrapperConfigBuilder().mainName(COPY_META))
            .dataType(DataType.MAIL_TEXT);
        builder.dataExtractorConfig().charset(StandardCharsets.UTF_8);
        try (CokemulatorCluster cluster = new CokemulatorCluster(builder)) {
            cluster.storage().add(
                GET_HTML,
                new File(CokemulatorTest.class.getResource(HTML).toURI()));
            cluster.start();
            try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + PROCESS_HTML)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    HTML_META,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMailBadEncoding() throws Exception {
        CokemulatorConfigBuilder builder = new CokemulatorConfigBuilder()
            .jniWrapperConfig(
                new JniWrapperConfigBuilder().mainName(COPY_FUNC))
            .dataType(DataType.MAIL_TEXT);
        builder.dataExtractorConfig().charset(StandardCharsets.UTF_8);
        try (CokemulatorCluster cluster = new CokemulatorCluster(builder)) {
            cluster.storage().add(
                "/get/bad-encoding.eml?raw",
                new File(
                    CokemulatorTest.class.getResource("bad-encoding.eml")
                        .toURI()));
            cluster.start();
            try (
                CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.uri() + "/process?stid=bad-encoding.eml")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "оПХБЕР",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMailQuotedPrintable() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataType(DataType.MAIL_TEXT)))
        {
            cluster.storage().add(
                "/get/quoted-printable.eml?raw",
                new File(
                    CokemulatorTest.class.getResource("quoted-printable.eml")
                        .toURI()));
            cluster.storage().add(
                "/get/quoted-printable-lowercase.eml?raw",
                new File(
                    CokemulatorTest.class.getResource(
                        "quoted-printable-lowercase.eml")
                        .toURI()));
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.uri() + "/process?stid=quoted-printable.eml")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    TRUTH_BEAUTY,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.uri()
                        + "/process?stid=quoted-printable-lowercase.eml")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    TRUTH_BEAUTY,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMailLongQuotedPrintable() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataType(DataType.MAIL_TEXT)))
        {
            cluster.storage().add(
                "/get/long-quoted-printable.eml?raw",
                new File(
                    CokemulatorTest.class
                        .getResource("long-quoted-printable.eml")
                        .toURI()));
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.uri()
                        + "/process?stid=long-quoted-printable.eml")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    TRUTH_BEAUTY,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMailAmazonQuotedPrintable() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataType(DataType.MAIL_TEXT)))
        {
            cluster.storage().add(
                "/get/amazon-quoted-printable.eml?raw",
                new File(
                    CokemulatorTest.class
                        .getResource("amazon-quoted-printable.eml")
                        .toURI()));
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.uri()
                        + "/process?stid=amazon-quoted-printable.eml")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = CharsetUtils.toString(response.getEntity());
                YandexAssert.assertContains(
                    "r.html?C=O5ID2HJ90BI3&K=A128GI7HR12MPZ&R=23CPJKB0P7T27",
                    body);
                // Check that CRLF is not collapsed to LF
                YandexAssert.assertContains("/> \r\n </body>", body);
            }
        }
    }

    @Test
    public void testMailBase64() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataType(DataType.MAIL_TEXT)))
        {
            cluster.storage().add(
                "/get/base64.eml?raw",
                new File(
                    CokemulatorTest.class.getResource("base64.eml").toURI()));
            String text = "Hello, world";
            cluster.tikaite().add(
                "/text?encoding=auto&stid=base64.eml&hid=1.3&raw",
                text);
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.uri() + "/process?stid=base64.eml")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "pleasure.ple",
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.uri() + "/process?stid=base64.eml&hid=1.3")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    text,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMailRfc822Attach() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataType(DataType.MAIL_TEXT)))
        {
            cluster.storage().add(
                "/get/rfc822.attach.eml?raw",
                new File(
                    CokemulatorTest.class.getResource("rfc822.attach.eml")
                        .toURI()));
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.uri() + "/process?stid=rfc822.attach.eml")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "forward test\r\n",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMailWithoutSkipMark() throws Exception {
        try (CokemulatorCluster cluster = new CokemulatorCluster(
                new CokemulatorConfigBuilder()
                    .jniWrapperConfig(
                        new JniWrapperConfigBuilder().mainName(COPY_FUNC))
                    .dataType(DataType.MAIL_TEXT)))
        {
            cluster.storage().add(GET_HTML, "Nothing useful here");
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                cluster.cokemulator().port(),
                PROCESS_HTML);
        }
    }

    @Test
    public void testPost() throws Exception {
        try (Cokemulator cokemulator =
                new Cokemulator(
                    new CokemulatorConfigBuilder()
                        .storageConfig(null)
                        .jniWrapperConfig(
                            new JniWrapperConfigBuilder()
                                .mainName(COPY_FUNC)
                                .libraryName(CokemulatorCluster.LIBRARY_NAME))
                        .port(0)
                        .connections(2)
                        .concurrency(1)
                        .build());
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cokemulator.start();
            HttpPost post = new HttpPost(cokemulator.host() + "/factextract/");
            String text = "Post entity test";
            post.setEntity(new StringEntity(text, ContentType.TEXT_PLAIN));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    text,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

