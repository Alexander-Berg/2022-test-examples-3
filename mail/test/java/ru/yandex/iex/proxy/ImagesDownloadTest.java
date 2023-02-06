package ru.yandex.iex.proxy;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.iex.proxy.images.ImmutableZoraProxyClientConfig;
import ru.yandex.json.writer.JsonType;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.TestBase;

public class ImagesDownloadTest extends TestBase {
    private static final String ADDA = "/add*";
    private static final String AXIS_URI =
        "/v1/facts/store_batch?client_id=extractors";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String PG = "pg";
    private static final String UID = "&uid=";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SRC = "src";
    private static final String HUNDRED_PERCENT = "100%";

    // CSOFF: MultipleStringLiterals
    // CSOFF: MethodLength
    private static String zoraProxyURI(final String... urls) throws Exception {
        QueryConstructor qc = new QueryConstructor("/image?");
        for (String url: urls) {
            qc.append("url", url);
        }
        return qc.toString();
    }

    private static String zoraResponse(final String... sizes) {
        if (sizes.length == 2) {
            return "{\"width\":" + sizes[0] + ", \"height\": " + sizes[1] + '}';
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < sizes.length; i += 2) {
            if (sb.length() > 1) {
                sb.append(',');
            }

            sb.append("{\"width\":");
            sb.append(sizes[i]);
            sb.append(", \"height\": ");
            sb.append(sizes[i + 1]);
            sb.append("} ");
        }

        sb.append(']');
        return sb.toString();
    }

    @Test
    public void test() throws Exception {
        final String cfg = "zoraproxy.enabled-part = 1\n"
            + "entities.message-type-13 = news\n"
            + "postprocess.message-type-13 = news:http://localhost:"
            + IexProxyCluster.IPORT + "/news\n";

        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = "123";
            final String stid = "1.2.3";
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));

            String blackboxUri = IexProxyCluster.blackboxUri(UID + uid);
            cluster.blackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(uid, "a@b.c1"));

            String filterSearchUri = IexProxyTestMocks.
                filterSearchUri(uid, mid);
            cluster.filterSearch().add(
                filterSearchUri,
                IexProxyTestMocks.filterSearchPgResponse(mid, stid, "13"));

            String news = "news";
            String text1 = "First";
            String url0 =
                "http://www.glavbukh.ru/imgmail/e-daily/avtory/foot_3.jpg";
            String url1 =
                "http://www.glavbukh.ru/imgmail/e-daily/avtory/foot_2.jpg";
            String url2 = "http://www.glavbukh.ru/imgmail/e-daily/avtory/leg";
            String url4 = "selectedimg";

            SolutionBuilder sb =
                new SolutionBuilder(news)
                    .image(url0).image(url1).image(url2)
                    .selectedImage(url4, 1, 1);

            String cokeUri = "/process?stid=" + stid + '*';
            cluster.cokemulatorIexlib().add(
                cokeUri,
                new ChainedHttpResource(new StaticHttpItem(sb.build(text1))));

            cluster.tikaite().add(
                "/tikaite?json-type=dollar&stid=" + stid + '*',
                "{\"prefix\":" + uid
                    + ", \"docs\":[{\"headers\":\"x-yandex-rpop-id: 123\\n"
                    + "x-yandex-rpop-info: qweqwe@imap.yandex.ru\\n"
                    + "message-id: <weqwe@qeqwe>\"}]}");

            cluster.axis().add(AXIS_URI, HttpStatus.SC_OK);

            cluster.zoraProxy().add(
                zoraProxyURI(url0, url1),
                zoraResponse("100", "110", "200", "110"));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            Assert.assertEquals(
                1,
                cluster.zoraProxy().accessCount(zoraProxyURI(url0, url1)));
            Assert.assertEquals(
                0,
                cluster.zoraProxy().accessCount(zoraProxyURI(url2)));
            // url1 is ok, url2 is not downloadable
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            String url = "facts_" + uid + '_' + mid + "_news";
            String searchUri =
                "/search?prefix=1&text=url:" + url + "&get=fact_data&hr";
            cluster.testLucene().checkSearch(
                searchUri,
                sb.selectedImage(url1, "200", "110").expected(text1));

            String url3 = "http://www.glavbukh.ru/imgmail/e-daily/tmp.gifff";
            String url5 = "http://www.glavbukh.ru/imgmail/e-daily/tmp.jpeg";
            sb = new SolutionBuilder(news)
                    .image(url3).image(url5)
                    .selectedImage(url4, 1, -1);

            text1 = "Second";
            cluster.cokemulatorIexlib().add(
                cokeUri,
                new ChainedHttpResource(new StaticHttpItem(sb.build(text1))));

            cluster.zoraProxy().add(zoraProxyURI(url5),
                                    zoraResponse("200", "111"));

            cluster.zoraProxy().add(zoraProxyURI(url3, url5),
                zoraResponse("200", "110", "200", "111"));

            String deleteFacts = "\"url\":\"" + url + '\"';
            cluster.testLucene().delete(new LongPrefix(uid), deleteFacts);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            Assert.assertEquals(
                1,
                cluster.zoraProxy().accessCount(zoraProxyURI(url5)));
            Assert.assertEquals(
                0,
                cluster.zoraProxy().accessCount(zoraProxyURI(url3, url5)));

            cluster.testLucene().checkSearch(
                searchUri,
                sb.selectedImage(url5, "200", "111").expected(text1));

            cluster.testLucene().delete(new LongPrefix(uid), deleteFacts);
            String url6 = "http://www.glavbukh.ru/imgmail/e-daily/6.jpg";
            String url7 = "http://www.glavbukh.ru/imgmail/e-daily/7.jpg";
            String url8 = "http://www.glavbukh.ru/imgmail/e-daily/8.jpg";
            String url9 = "http://www.glavbukh.ru/imgmail/e-daily/9.jpg";
            String url10 = "http://www.glavbukh.ru/imgmail/e-daily/10.jpg";
            String url11 = "http://www.glavbukh.ru/imgmail/e-daily/11.jpg";

            sb = new SolutionBuilder(news)
                .image(url6)
                .image(url7).image(url8).image(url9)
                .image(url10).image(url11)
                .selectedImage(url4, String.valueOf(1), HUNDRED_PERCENT);

            text1 = "Third";
            cluster.cokemulatorIexlib().add(
                cokeUri,
                new ChainedHttpResource(new StaticHttpItem(sb.build(text1))));

            String[] sizes = new String[] {
                "99", "1000",
                "0", "5000",
                "0", "0",
                "100", "140",
                "500", "100",
                "-1", "-1"
            };

            cluster.zoraProxy().add(
                zoraProxyURI(url6, url7, url8, url9, url10, url11),
                zoraResponse(sizes));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            cluster.testLucene().checkSearch(
                searchUri,
                sb.selectedImage(url9, "100", "140").expected(text1));
        }
    }

    private static final class SolutionBuilder {
        private List<String> images = new ArrayList<>();
        private Map<String, Object> selectedImg = new HashMap<>();
        private final String name;

        private SolutionBuilder(final String name) {
            this.name = name;
        }

        public SolutionBuilder selectedImage(
            final String url,
            final String width,
            final String height)
        {
            selectedImg.clear();
            selectedImg.put(SRC, url);
            selectedImg.put(WIDTH, width);
            selectedImg.put(HEIGHT, height);

            return this;
        }

        public SolutionBuilder selectedImage(
            final String url,
            final int width,
            final int height)
        {
            return this.selectedImage(
                url,
                String.valueOf(width),
                String.valueOf(height));
        }

        public SolutionBuilder image(final String url) {
            this.images.add(url);

            return this;
        }

        public String expected(final String text) {
            return "{\"hitsCount\": 1, \"hitsArray\":[{\"fact_data\":"
                + "\"{\\\"total_img_count\\\":" + images.size()
                + ",\\\"imgs\\\":[],"
                + "\\\"subject\\\":\\\"Subject\\\","
                + "\\\"from\\\":\\\"login@d.c\\\","
                + "\\\"text\\\":\\\"" + text + "\\\",\\\""
                + "selected_img\\\":{\\\"src\\\":\\\""
                + selectedImg.get(SRC) + "\\\",\\"
                + "\"width\\\":\\\""
                + selectedImg.get(WIDTH) + "\\\",\\\"height\\\":\\\""
                + selectedImg.get(HEIGHT) + "\\\"}}\""
                + "}]}";
        }

        public String build(final String text) {
            return "{\"news\":"
                + "{\"total_img_count\":" + images.size() + ",\"imgs\":[],"
                + "\"selected_img\":" + JsonType.NORMAL.toString(selectedImg)
                + ",\"all_imgs\":" + JsonType.NORMAL.toString(images)
                + ",\"subject\":\"Subject\","
                + "\"from\":\"login@d.c\",\"text\":\"" + text + "\"}}";
        }
    }

    // CSON: MultipleStringLiterals
    // CSON: MethodLength

    @Test
    public void testDecentUrls() throws Exception {
        String url =
            "http://mfo-info.pro/showimg.php?src=1&i="
                + "chestnoe_slovo_zaem_do_10000/images/img-chs-3.jpg";

        Assert.assertTrue(
            ImmutableZoraProxyClientConfig.checkUrl(new URL(url)));

        url =
            "http://mfo-info.pro/showimg.php?src=1&i=slovo_"
                + "chestnoe_slovo_zaem_do_10000/images/img-chs-3.jpggg";

        Assert.assertFalse(
            ImmutableZoraProxyClientConfig.checkUrl(new URL(url)));

        url =
            "http://mfo-info.pro/showimg.php?&i="
                + "chestnoe_slovo_zaem_do_10000/images/img-chs-3";

        Assert.assertFalse(
            ImmutableZoraProxyClientConfig.checkUrl(new URL(url)));
    }
}
