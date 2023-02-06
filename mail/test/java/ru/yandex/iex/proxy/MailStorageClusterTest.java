package ru.yandex.iex.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class MailStorageClusterTest extends TestBase {
    private static final int BUFFER_SIZE = 65536;
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String PING_STID = "1.11.111";
    private static final String PING_EML = "ping.eml";
    private static final String PONG_JSON = "pong.json";
    private static final String PING_URI = "/get/" + PING_STID;

    public static String streamToString(final InputStream is)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();
        char[] cbuf = new char[BUFFER_SIZE];
        try (InputStreamReader reader =
                new InputStreamReader(is, StandardCharsets.UTF_8))
        {
            int read;
            while ((read = reader.read(cbuf)) != -1) {
                sb.append(cbuf, 0, read);
            }
        }
        return sb.toString();
    }

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue(MailStorageCluster.iexUrl() != null);
    }

    @Test
    public void test() throws Exception {
        try (CloseableHttpClient client = Configs.createDefaultClient();
            MailStorageCluster storage = new MailStorageCluster(this))
        {
            String body =
                streamToString(getClass().getResourceAsStream(PING_EML));
            storage.put(PING_STID, this.getClass().getResource(PING_EML));
            storage.start();
            HttpGet get =
                new HttpGet(HTTP_LOCALHOST + storage.lenulcaPort() + PING_URI);

            HttpResponse response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                body,
                EntityUtils.toString(response.getEntity()));

            get = new HttpGet(
                storage.tikaite().host()
                + "/tikaite?json-type=dollar&stid=" + PING_STID);
            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

            JsonChecker json = new JsonChecker(
                streamToString(getClass().getResourceAsStream(PONG_JSON)));

            YandexAssert.check(
                json,
                EntityUtils.toString(response.getEntity()));
        }
    }

    @Ignore
    @Test
    public void cokeIexTest() throws Exception {
        try (CloseableHttpClient client = Configs.createDefaultClient();
            MailStorageCluster storage = new MailStorageCluster(this))
        {
            storage.put(PING_STID, this.getClass().getResource(PING_EML));
            storage.start();
            HttpGet get = new HttpGet(HTTP_LOCALHOST + storage.cokemulatorPort()
                    + "/process?stid=" + PING_STID
                    + "&time=1477568584&domain=yandex.ru&e=getbody&hid=1");

            HttpResponse response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                "{\"getbody\":{\"text\":\"PONG\"}}",
                EntityUtils.toString(response.getEntity()));

            get = new HttpGet(HTTP_LOCALHOST + storage.cokemulatorPort()
                    + "/process?qwe&stid=" + PING_STID
                    + "&time=1477568584&domain=yandex.ru&e=snippet&hid=1");

            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                "{\"snippet\":{\"text_html\":\" PONG \",\"text\":\"PONG\"}}",
                EntityUtils.toString(response.getEntity()).trim());
/*
            JsonChecker json = new JsonChecker(
                streamToString(getClass().getResourceAsStream(PONG_JSON)));

            YandexAssert.check(
                json,
                EntityUtils.toString(response.getEntity()));
*/
        }
    }
}

