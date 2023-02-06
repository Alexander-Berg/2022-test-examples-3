package ru.yandex.search.shakur;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.TestBase;

public class ShakurTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (ShakurCluster cluster = new ShakurCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {

            cluster.addStatus();
            HttpAssert.assertJsonResponse(
                client,
                cluster.proxy().host() + "/api/shakur/checkpassword?sha1=xxxx",
                "{\"found\": false, \"passwords\": []}");
            cluster.searchBackend().add(
                new LongPrefix(0),
                "\"id\":\"sha1_D80A2DE0126B5F475DA37E1556EED9A77BB0338E" +
                "\",\"freq\":\"1\",\"source\":\"haveibeenpwned\"," +
                "\"sha1\":\"D80A2DE0126B5F475DA37E1556EED9A77BB0338E\"");

            HttpAssert.assertJsonResponse(
                client,
                cluster.proxy().host()
                    + "/api/shakur/checkpassword?sha1=D80A2DE0126B5F475DA37E1556EED9A77BB0338E",
                "{\"found\": true, \"passwords\": [\n" +
                    "{\n" +
                    "   \"freq\": 1,\n" +
                    "   \"sha1\": \"D80A2DE0126B5F475DA37E1556EED9A77BB0338E\",\n" +
                    "   \"source\": \"haveibeenpwned\" \n" +
                    "}]}");


            //check passport min freq
            HttpGet get = new HttpGet(cluster.proxy().host() + "/api/shakur/checkpassword?sha1=D80A2DE0");
            get.addHeader(HttpHeaders.USER_AGENT, "python-requests/2.25.1");
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"found\": false, \"passwords\": []}");

            cluster.searchBackend().add(
                new LongPrefix(0),
                "\"id\":\"sha1_D80A2DE0126B5F475DA37E1556EED9A77BB0338F" +
                "\",\"freq\":\"10\",\"source\":\"haveibeenpwned\"," +
                "\"sha1\":\"D80A2DE0126B5F475DA37E1556EED9A77BB0338F\"");
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"found\": true, \"passwords\": [\n" +
                    "{\n" +
                    "   \"freq\": 10,\n" +
                    "   \"sha1\": \"D80A2DE0126B5F475DA37E1556EED9A77BB0338F\",\n" +
                    "   \"source\": \"haveibeenpwned\" \n" +
                    "}]}");
            //end min freq


            cluster.searchBackend().add(
                new LongPrefix(0),
            "\"id\":\"sha1_CD77E05F8405E46E2D448BDD47C59399DF500B23\"," +
                "\"freq\":\"1\",\"source\":\"haveibeenpwned\"," +
                "\"sha1\":\"CD77E05F8405E46E2D448BDD47C59399DF500B23\"");

            HttpPost post = new HttpPost(cluster.proxy().host() + "/api/shakur/checkpassword?");
            post.setEntity(new StringEntity("{\"sha1\":[\"CD77E05F8405E46E2D448BDD47C59399DF500B23\", \"D80A2DE0126B5F475DA37E1556EED9A77BB0338E\"]}"));
            HttpAssert.assertJsonResponse(
                client,
                post,
                "{\"found\": true, \"passwords\": [\n" +
                    "{\n" +
                    "   \"freq\": 1,\n" +
                    "   \"sha1\": \"CD77E05F8405E46E2D448BDD47C59399DF500B23\",\n" +
                    "   \"source\": \"haveibeenpwned\" \n" +
                    "}, {" +
                    "   \"freq\": 1,\n" +
                    "   \"sha1\": \"D80A2DE0126B5F475DA37E1556EED9A77BB0338E\",\n" +
                    "   \"source\": \"haveibeenpwned\" \n" +
                    "}]}");

            post = new HttpPost(cluster.proxy().host() + "/api/shakur/checkpassword?");
            post.setEntity(new StringEntity("{\"sha1\":[\"cd77e05f8405e46e2d448bdd47c59399df500b23\", \"D80A2DE0126B5F475DA37E1556EED9A77BB0338E\"]}"));
            HttpAssert.assertJsonResponse(
                client,
                post,
                "{\"found\": true, \"passwords\": [\n" +
                    "{\n" +
                    "   \"freq\": 1,\n" +
                    "   \"sha1\": \"CD77E05F8405E46E2D448BDD47C59399DF500B23\",\n" +
                    "   \"source\": \"haveibeenpwned\" \n" +
                    "}, {" +
                    "   \"freq\": 1,\n" +
                    "   \"sha1\": \"D80A2DE0126B5F475DA37E1556EED9A77BB0338E\",\n" +
                    "   \"source\": \"haveibeenpwned\" \n" +
                    "}]}");
        }
    }
}
