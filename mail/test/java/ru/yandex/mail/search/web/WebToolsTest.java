package ru.yandex.mail.search.web;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.mail.search.web.info.InfoExtractorFactory;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class WebToolsTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (WebToolsCluster cluster = new WebToolsCluster();
             CloseableHttpClient client = HttpClients.createDefault()) {
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(cluster.webApi().host().toString() + "/ping"));

            Thread.sleep(3000);

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.webApi().host()
                                 + "/mail_search_prod/peach-skip?host=sas2-0806.search.yandex.net&shard=0&name=default")))
            {
                System.out.println(CharsetUtils.toString(response.getEntity()));
                //Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.webApi().host()
                                 + "/disk_search_prod/health/info?request=")))
            {
                System.out.println(CharsetUtils.toString(response.getEntity()));
            }

            return;
        }
    }

    @Ignore
    public void testDiskSearch() throws Exception {
        try (WebToolsCluster cluster = new WebToolsCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.diskProxy().add(
                "/?&kps=5598601&text=house&only=id,stid"
                    + "&source=mail_search_webtools&old-cv-only",
                new String(
                    IOStreamUtils.consume(
                        this.getClass().getResourceAsStream("oldcv.json"))
                        .toByteArray(),
                    StandardCharsets.UTF_8));
            cluster.diskProxy().add(
                "/?&kps=5598601&text=house&only=id,stid&source="
                    + "mail_search_webtools&i2t-only",
                new String(
                    IOStreamUtils.consume(
                        this.getClass().getResourceAsStream("i2t.json"))
                        .toByteArray(),
                    StandardCharsets.UTF_8));
            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.webApi().host().toString()
                                 + "/disk/search?kps=5598601&request=house")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        new String(
                            IOStreamUtils.consume(
                                this.getClass().getResourceAsStream(
                                    "expected.json"))
                            .toByteArray(),
                        StandardCharsets.UTF_8)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testQueueExtractor() throws Exception {
        Assert.assertNotNull(
            InfoExtractorFactory.UID_EXTRACT_CHAIN.extract(
                "uid=1234&mid=1234"));
        Assert.assertNotNull(
            InfoExtractorFactory.UID_EXTRACT_CHAIN.extract(
                "&service=2134&uid=1234&mid=1234"));
        Assert.assertNull(
            InfoExtractorFactory.UID_EXTRACT_CHAIN.extract(
                "&service=2134&mid=1234"));
        Assert.assertEquals(
            "1234",
            InfoExtractorFactory.UID_EXTRACT_CHAIN.extract(
                "&service=21rre34uid=1234&mid=1234"));
        Assert.assertEquals(
            "165788761282808950",
            InfoExtractorFactory.MID_EXTRACT_CHAIN.extract(
                "https://mail.yandex-team.ru/?uid=1120000000040290"
                    + "&login=vonidu#message/165788761282808950"));
    }
}
