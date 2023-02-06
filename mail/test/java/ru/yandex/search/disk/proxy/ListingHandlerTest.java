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
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class ListingHandlerTest extends TestBase {
    private static final String URI = "/listing?uid=0";
    private static final String GET_URI = "/get?uid=0";
    private static final String KEY = "&key=";
    private static final String ID = "&id=";
    private static final String MIMETYPE = "&mimetype=";
    private static final String MEDIATYPE = "&mediatype=";
    private static final String GET_ID = "&get=id";
    private static final String SORT_ID = "&sort=id";
    private static final String LENGTH0 = "&length=0";
    private static final String FROM = "&from-etime=";
    private static final String TO = "&to-etime=";
    private static final String SORT_ETIME = "&sort=etime";
    private static final String ASC = "&asc";
    private static final String MERGED = "\",\"merged_docs_count\":";
    private static final String ROOT = "/disk/";
    private static final String FOLDER1 = "/disk/folder in root/";
    private static final String FOLDER2 = "/disk/another/";
    private static final String SUBFOLDER1 = "/disk/folder in root/sub1/";
    private static final String SUBFOLDER2 = "/disk/folder in root/sub2/";
    private static final String PHOTOUNLIM = "/photounlim/";
    private static final String TEXT = "text/plain";
    private static final String HTML = "text/html";
    private static final String JPG = "image/jpeg";
    private static final String PNG = "image/png";
    private static final String AVI = "video/avi";
    // docs
    private static final String DOC0 = "\"id\":\"0\"";
    private static final String DOC1 = "\"id\":\"1\"";
    private static final String DOC2 = "\"id\":\"2\"";
    private static final String DOC3 = "\"id\":\"3\"";
    private static final String DOC4 = "\"id\":\"4\"";
    private static final String DOC5 = "\"id\":\"5\"";
    private static final String DOC6 = "\"id\":\"6\"";
    private static final String DOC7 = "\"id\":\"7\"";
    private static final String DOC8 = "\"id\":\"8\"";
    private static final String DOC9 = "\"id\":\"9\"";
    private static final String DOC10 = "\"id\":\"10\"";
    // noon 01 June 1986 MSK
    private static final int START_TIMESTAMP = 517996800;
    private static final int SECONDS_PER_DAY = 86400;

    private int docId = 0;

    // CSOFF: ParameterNumber
    private String doc(
        final String folder,
        final String mimetype,
        final int mediatype,
        final int etime)
    {
        return "\"version\":0,\"type\":\"file\",\"id\":" + docId++
            + ",\"key\":\"" + folder + "\",\"mimetype\":\"" + mimetype
            + "\",\"mediatype\":" + mediatype + ",\"etime\":" + etime
            + ",\"aux_folder\":\""
            + folder.substring(1, folder.indexOf('/', 2)) + '"';
    }
    // CSON: ParameterNumber

    // CSOFF: MagicNumber
    private void prepareIndex(final ProxyCluster cluster) throws IOException {
        docId = 0;
        cluster.backend().add(
            doc(ROOT, TEXT, 0, START_TIMESTAMP) + ",\"size\":10",
            doc(FOLDER1, JPG, 1, START_TIMESTAMP + SECONDS_PER_DAY)
            + ",\"size\":15",
            doc(FOLDER1, PNG, 1, START_TIMESTAMP + SECONDS_PER_DAY + 1)
            + ",\"size\":17",
            doc(SUBFOLDER1, JPG, 1, START_TIMESTAMP + SECONDS_PER_DAY * 2)
            + ",\"size\":20",
            doc(SUBFOLDER1, JPG, 1, START_TIMESTAMP + SECONDS_PER_DAY * 2 + 1)
            + ",\"size\":2",
            doc(SUBFOLDER2, PNG, 1, START_TIMESTAMP + SECONDS_PER_DAY * 2 + 2)
            + ",\"size\":22",
            doc(FOLDER2, HTML, 0, START_TIMESTAMP + SECONDS_PER_DAY * 3)
            + ",\"size\":13",
            doc(FOLDER2, HTML, 0, START_TIMESTAMP + SECONDS_PER_DAY * 3 + 1)
            + ",\"size\":5",
            doc(FOLDER2, AVI, 2, START_TIMESTAMP + SECONDS_PER_DAY * 4)
            + ",\"size\":55",
            doc(PHOTOUNLIM + "/hot-rod.avi", AVI, 2, START_TIMESTAMP)
            + ",\"size\":56,\"name\":\"hot-rod.avi\",\"ext\":\"avi\"",
            doc(PHOTOUNLIM + "/VIDEO_0001.MP4", AVI, 2, START_TIMESTAMP)
            + ",\"size\":57,\"name\":\"VIDEO_0001.MP4\",\"ext\":\"MP4\"");
    }

    @Test
    public void test() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + KEY + ROOT + LENGTH0)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult(9)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + KEY + ROOT + LENGTH0
                        + MIMETYPE + JPG + MIMETYPE + PNG)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult(5)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI
                        + KEY + FOLDER1.replace(' ', '+')
                        + MIMETYPE + JPG + GET_ID + "&sort=id&asc")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(DOC1, DOC3, DOC4)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI
                        + KEY + SUBFOLDER1.replace(' ', '+')
                        + GET_ID + SORT_ID)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult(DOC4, DOC3)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI
                        + KEY + SUBFOLDER2.replace(' ', '+') + GET_ID
                        + MIMETYPE + JPG + MIMETYPE + PNG)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult(DOC5)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + KEY + ROOT
                        + "&group=mimetype&merge_func=count&get=mimetype"
                        + "&sort=mimetype&asc")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            "\"mimetype\":\"image/jpeg" + MERGED + 2,
                            "\"mimetype\":\"image/png" + MERGED + 1,
                            "\"mimetype\":\"text/html" + MERGED + 1,
                            "\"mimetype\":\"text/plain\"",
                            "\"mimetype\":\"video/avi\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI
                        + KEY + FOLDER1.replace(' ', '+')
                        + "&group=mimetype&merge_func=none&get=mimetype"
                        + "&sort=mimetype&type=file")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            "\"mimetype\":\"image/png\"",
                            "\"mimetype\":\"image/jpeg\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI
                        + KEY + FOLDER1.replace(' ', '+')
                        + "&group=mimetype&merge_func=none&get=mimetype&"
                        + "sort=mimetype&type=dir")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult()),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + KEY + ROOT
                        + MEDIATYPE + 0 + MEDIATYPE + 2 + GET_ID + SORT_ID)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(DOC8, DOC7, DOC6, DOC0)),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Add etime filters
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + KEY + ROOT
                        + MEDIATYPE + 0 + MEDIATYPE + 2 + GET_ID + SORT_ID
                        + FROM + (START_TIMESTAMP + SECONDS_PER_DAY * 3 + 1))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult(DOC8, DOC7)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + KEY + ROOT
                        + MEDIATYPE + 0 + MEDIATYPE + 2 + GET_ID + SORT_ID
                        + TO + (START_TIMESTAMP + SECONDS_PER_DAY * 3))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult(DOC6, DOC0)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testEtime() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);

            // List some files
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + GET_ID + SORT_ETIME
                        + FROM + (START_TIMESTAMP + SECONDS_PER_DAY + 1)
                        + TO + (START_TIMESTAMP + SECONDS_PER_DAY * 3))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            DOC6,
                            DOC5,
                            DOC4,
                            DOC3,
                            DOC2)),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Exclude ones that are not images
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + GET_ID + SORT_ETIME
                        + FROM + (START_TIMESTAMP + SECONDS_PER_DAY + 1)
                        + TO + (START_TIMESTAMP + SECONDS_PER_DAY * 3)
                        + MEDIATYPE + 1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(DOC5, DOC4, DOC3, DOC2)),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Leave only jpegs
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + GET_ID + SORT_ETIME
                        + FROM + (START_TIMESTAMP + SECONDS_PER_DAY + 1)
                        + TO + (START_TIMESTAMP + SECONDS_PER_DAY * 3)
                        + MEDIATYPE + 1 + MIMETYPE + JPG + ASC)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(DOC3, DOC4)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCalcSize() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI + KEY + ROOT
                        + "&group=mediatype&aggregate=sum(size)+total_size"
                        + "&merge_func=count&get=mediatype,total_size")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            "\"mediatype\":\"0\",\"merged_docs_count\":2,"
                            + "\"total_size\":\"28\"",
                            "\"mediatype\":\"1\",\"merged_docs_count\":4,"
                            + "\"total_size\":\"76\"",
                            "\"mediatype\":\"2\",\"total_size\":\"55\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testGet() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + GET_URI
                        + ID + 2 + ID + 0 + ID + 1 + GET_ID + SORT_ID)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(DOC2, DOC1, DOC0)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPhotosliceFilter() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            String uri =
                cluster.proxy().host() + GET_URI + GET_ID + SORT_ID + ASC
                + ID + 10   // allowed in photoslice and search results
                + ID + 0    // allowed in search results
                + ID + 1    // allowed in photoslice and search results
                + ID + 9    // not allowed anywhere because of extension
                + ID + 6;   // allowed in search results
            // Request docs without filter
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            DOC0,
                            DOC1,
                            DOC10,
                            DOC6,
                            DOC9)),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Request docs with photoslice filter
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(uri + "&apply-photoslice-filter")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(DOC1, DOC10)),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Request docs with size limit 50
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(uri + "&postfilter=size+%3c=+50")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(DOC0, DOC1, DOC6)),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Test listing with photoslice filter
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + URI + "&apply-photoslice-filter&length=0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult(4)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI
                        + "&apply-photoslice-filter&length=0&"
                        + "postfilter=size+%3c%3d+50")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult(3)),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Test listing with photoslice filter and grouping
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + URI
                        + "&group=mediatype&merge_func=count&get=mediatype"
                        + "&sort=mediatype&asc&apply-photoslice-filter")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            "\"mediatype\":\"1" + MERGED + 2,
                            "\"mediatype\":\"2\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    // CSON: MagicNumber
}

