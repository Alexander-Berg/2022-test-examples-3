package ru.yandex.search.backpack.client;;


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

public class BackPackClientTest extends TestBase {
    private static final String ANSWER ="pong";
    public static final String URI = "/ping";

    @Test
    public void testOK() throws Exception {
        try (BackPackClientCluster cluster = new BackPackClientCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            HttpGet get = new HttpGet(
                    cluster.backpackClient().host().toString()
                    + URI);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                        ANSWER,
                        CharsetUtils.toString(response.getEntity()));
            }

            HttpGet get2 = new HttpGet(
                    cluster.searchBackend().indexerUri()
                            + URI);

            try (CloseableHttpResponse response = client.execute(get2)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                        ANSWER,
                        CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

