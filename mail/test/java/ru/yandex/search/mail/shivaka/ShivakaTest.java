package ru.yandex.search.mail.shivaka;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.TestBase;

public class ShivakaTest extends TestBase {
    private static final String ANSWER =
            "[[search: http://sas1-0285.search.yandex.net:18074, "
            + "index: http://sas1-0285.search.yandex.net:18077]]";
    private static final String SHIVAKA_URI = "/test/?"
            + "service=change_log&"
            + "nanny-services=mail_search_prod,mail_search_prestable";

    @Test
    public void testOK() throws Exception {
        try (ShivakaCluster cluster = new ShivakaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            /*HttpGet get = new HttpGet(
                    cluster.queueServer().host()
                            + "/queuelen" )
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);*/
            // Test tags balancing
            //String request = "somepostdata";
            HttpGet get = new HttpGet(
                    cluster.shivaka().host().toString()
                    + SHIVAKA_URI);
            //post.setEntity(
            //        new StringEntity(request, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                /*HttpAssert.assertMultipart(
                        response.getEntity(),
                        new JsonChecker((Object) null),
                        new StringChecker("OK"));
                */
                Assert.assertEquals(
                        ANSWER,
                        CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

