package ru.yandex.search.disk.proxy;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class FolderSizeHandlerTest extends TestBase {
    private static final String URI = "/folder-size?uid=0";
    private static final String KEY = "&key=";
    private static final String ROOT = "/disk/";
    private static final String FOLDER = "/disk/Ёлка и Алёна/";
    private static final String SUBFOLDER1 = "/disk/Ёлка и Алёна/sub1/";
    private static final String SUBFOLDER2 = "/disk/Ёлка и Алёна/sub2/";

    private int docId = 0;

    private String doc(final String folder, final long size) {
        return "\"version\":0,\"type\":\"file\",\"id\":" + docId++
            + ",\"key\":\"" + folder + "\",\"size\":" + size;
    }

    // CSOFF: MagicNumber
    private void prepareIndex(final ProxyCluster cluster) throws IOException {
        docId = 0;
        cluster.backend().add(
            doc(ROOT, 10),
            doc(FOLDER, 20),
            doc(FOLDER, 21),
            doc(SUBFOLDER1, 5),
            doc(SUBFOLDER1, 3),
            doc(SUBFOLDER2, 5));
    }
    // CSON: MagicNumber

    @Test
    public void test() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + URI + KEY + ROOT)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"/disk/\":{\"size\":64,\"count\":6}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI
                        + KEY + FOLDER.replace(' ', '+')
                        + KEY + SUBFOLDER1.replace(' ', '+')
                        + KEY + SUBFOLDER2.replace(' ', '+')
                        + KEY + "abracadabra")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"/disk/Ёлка и Алёна/\":{\"size\":54,\"count\":5},"
                        + "\"/disk/Ёлка и Алёна/sub1/\":"
                        + "{\"size\":8,\"count\":2},"
                        + "\"/disk/Ёлка и Алёна/sub2/\":"
                        + "{\"size\":5,\"count\":1}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_REQUEST,
                client,
                new HttpGet(cluster.proxy().host() + URI));
        }
    }
}

