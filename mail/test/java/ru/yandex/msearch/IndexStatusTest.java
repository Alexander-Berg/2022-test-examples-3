package ru.yandex.msearch;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class IndexStatusTest extends TestBase {
    @Test
    public void testIndexStatus() throws Exception {
        File root = Files.createTempDirectory("testIndexStatus").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            //plain text
            final String plainTextResponse = 
                "shard: 0 index-status: RW index-status-description: Read-write\n"
                + "shard: 1 index-status: RW index-status-description: Read-write\n"
                + "shard: 2 index-status: RW index-status-description: Read-write\n"
                + "shard: 3 index-status: RW index-status-description: Read-write\n"
                + "shard: 4 index-status: RW index-status-description: Read-write\n"
                + "shard: 5 index-status: RW index-status-description: Read-write\n"
                + "shard: 6 index-status: RW index-status-description: Read-write\n"
                + "shard: 7 index-status: RW index-status-description: Read-write\n"
                + "shard: 8 index-status: RW index-status-description: Read-write\n"
                + "shard: 9 index-status: RW index-status-description: Read-write\n";

            //indexer server
            HttpGet get = new HttpGet("http://localhost:"
                + daemon.jsonServerPort() + "/index-status");
            HttpResponse response = client.execute(get);
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(plainTextResponse, text);

            //search server
            get = new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/index-status");
            response = client.execute(get);
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(plainTextResponse, text);


            //json 
            final String jsonResponse =
                "{\"shards\":["
                + "{\"shard\":0,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + ",{\"shard\":1,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + ",{\"shard\":2,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + ",{\"shard\":3,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + ",{\"shard\":4,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + ",{\"shard\":5,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + ",{\"shard\":6,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + ",{\"shard\":7,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + ",{\"shard\":8,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + ",{\"shard\":9,\"index-status\":\"RW\","
                    + "\"index-status-description\":\"Read-write\"}"
                + "]}";
            //indexer server
            get = new HttpGet("http://localhost:"
                + daemon.jsonServerPort() + "/index-status?json-type=normal");
            response = client.execute(get);
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(jsonResponse, text);

            //search server
            get = new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/index-status?json-type=normal");
            response = client.execute(get);
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(jsonResponse, text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }
}
