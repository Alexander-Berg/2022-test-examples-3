package ru.yandex.search.mop.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.search.mop.common.parsers.SearchMapJsonParser;
import ru.yandex.search.mop.common.searchmap.HostGroup;
import ru.yandex.search.mop.common.searchmap.Metashard;
import ru.yandex.search.mop.common.searchmap.Queue;
import ru.yandex.search.mop.common.searchmap.SearchMap;
import ru.yandex.test.util.TestBase;

public class GetSearchMapTest extends TestBase {
    @Test
    public void testGetSearchMap() throws Exception {
        try (MopServerCluster cluster = new MopServerCluster();
            CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.apply("searchmap.sql");
            cluster.start();

            String uri = cluster.mopServer().host().toString() +
                "/get-searchmap?";


            // format = string, version = 0 -> full string searchmap

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                cluster.checkResponse(response, "searchmap.txt", null);
            }


            // format = string, version = 6 -> full string searchmap

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "version=6")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                cluster.checkResponse(response, "searchmap.txt", null);
            }


            // format = string, version = 7 -> empty string

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "version=7")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals("", CharsetUtils.toString(response.getEntity()));
            }


            // format = json, version = 7 -> empty json

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "version=7&format=json")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals("{}", CharsetUtils.toString(response.getEntity()));
            }


            // format = json, version = 0 -> full json searchmap

            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    this.getClass().getResourceAsStream("searchmap.json"),
                    StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
            }

            SearchMap expected =
                SearchMapJsonParser.INSTANCE.parse(sb.toString());

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "version=0&format=json")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                String responseStr = CharsetUtils.toString(response.getEntity());
                SearchMap actual =
                    SearchMapJsonParser.INSTANCE.parse(responseStr);

                // ignore arrays order

                Assert.assertEquals(expected.version(), actual.version());

                Assert.assertEquals(expected.queues().size(), actual.queues().size());
                Set<Queue> expectedQueues = new HashSet<>(expected.queues());
                Set<Queue> actualQueues = new HashSet<>(actual.queues());
                Assert.assertEquals(expectedQueues, actualQueues);

                Assert.assertEquals(expected.hostGroups().size(), actual.hostGroups().size());
                Set<HostGroup> expectedHosts = new HashSet<>(expected.hostGroups());
                Set<HostGroup> actualHosts = new HashSet<>(actual.hostGroups());
                Assert.assertEquals(expectedHosts, actualHosts);

                Assert.assertEquals(expected.metashards().size(), actual.metashards().size());
                Set<Metashard> expectedMetashards = new HashSet<>(expected.metashards());
                Set<Metashard> actualMetashards = new HashSet<>(actual.metashards());
                Assert.assertEquals(expectedMetashards, actualMetashards);
            }
        }
    }
}
