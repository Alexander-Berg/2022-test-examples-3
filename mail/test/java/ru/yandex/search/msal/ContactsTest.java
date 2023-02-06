package ru.yandex.search.msal;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;

public class ContactsTest extends MsalTestBase {
    @Test
    public void testGetShardByUid() throws Exception {
        try (MsalCluster cluster = new MsalCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.sharpei().add(
                "/conninfo?format=json&uid=300&mode=read_write",
                "{\"id\":2095,\"name\":\"xdb305\","
                    + "\"databases\":[{\"address\":{\"host\":\"first-replica\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"MYT\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}},"
                    + "{\"address\":{\"host\":\"second-replica\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"SAS\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}},"
                    + "{\"address\":{\"host\":\"master-node\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"IVA\"},\"role\":\"master\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":0}}]}");
            cluster.sharpeiConnect().add(
                "/org_conninfo?format=json&org_id=400&mode=read_write",
                "{\"id\":3095,\"name\":\"xdb305\","
                    + "\"databases\":[{\"address\":{\"host\":\"first-replica\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"MYT\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}},"
                    + "{\"address\":{\"host\":\"second-replica\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"SAS\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}},"
                    + "{\"address\":{\"host\":\"master-node\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"IVA\"},\"role\":\"master\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":0}}]}");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.msal().host() + "/get-user-shard?uid=300")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals("2095", CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.msal().host() + "/get-user-shard?orgId=400")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals("3095", CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}
