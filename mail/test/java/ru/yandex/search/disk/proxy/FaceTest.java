package ru.yandex.search.disk.proxy;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.TestBase;

public class FaceTest extends TestBase {

    @Test
    public void testClusterPhotos() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.start();
            JsonList docs =
                TypesafeValueContentHandler.parse(loadResourceAsString("face_index_1.json")).asList();

            HttpPost post = new HttpPost(cluster.backend().indexerUri() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":\"100500\", \"docs\":" + JsonType.NORMAL.toString(docs) + "}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            cluster.backend().flush();

            String uri = cluster.proxy().host()
                + "/api/faces/cluster-photos?&uid=100500&cluster_id=1_21_0&resource_get=";

            HttpAssert.assertJsonResponse(
                client,
                uri + "width%2Cheight%2Ccost_disk_aethetic_0%2Cresource_id",
                "{\n" +
                    "  \"cluster_id\": \"1_21_0\",\n" +
                    "  \"items\": [\n" +
                    "    {\n" +
                    "      \"face_id\": \"abcd101_0\",\n" +
                    "      \"face_cluster_id\": \"1_21_0\",\n" +
                    "      \"face_coord_x\": \"0.5208011328\",\n" +
                    "      \"face_coord_y\": \"0.4226217831\",\n" +
                    "      \"face_width\": \"0.1004859793\",\n" +
                    "      \"face_height\": \"0.1625525905\",\n" +
                    "      \"width\": \"100\",\n" +
                    "      \"height\": \"200\",\n" +
                    "      \"resource_id\": \"abcd101\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"face_id\": \"abcd102_0\",\n" +
                    "      \"face_cluster_id\": \"1_21_0\",\n" +
                    "      \"face_coord_x\": \"0.5208011328\",\n" +
                    "      \"face_coord_y\": \"0.4226217831\",\n" +
                    "      \"face_width\": \"0.1004859793\",\n" +
                    "      \"face_height\": \"0.1625525905\",\n" +
                    "      \"width\": \"100\",\n" +
                    "      \"height\": \"200\",\n" +
                    "      \"cost_disk_aethetic_0\": \"0.12\",\n" +
                    "      \"resource_id\": \"abcd102\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}");
        }
    }
}
