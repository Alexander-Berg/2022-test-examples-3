package ru.yandex.zora.proxy;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.MultiFutureCallback;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class ZoraProxyTest extends TestBase {
    private static final String URL = "url";
    private static final String IMAGE_ROUTE = "/image?";
    private static final String IEX_SOURCE = "ps_mail_iex";
    private static final byte[] SAMPLE_IMAGE =
        Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6"
                + "QAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAA"
                + "Ad0SU1FB9gBEg8MMpr32/cAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3a"
                + "XRoIEdJTVBXgQ4XAAAAD0lEQVQY02NgGAUMDAwMAAEIAAHEOhmJAAAAA"
                + "ElFTkSuQmCC");

    // CSOFF: MagicNumber
    @Test
    public void testHttps() throws Exception {
        final String url = "https://02.img.avito.st/https_image.jpg";

        try (ZoraCluster cluster = new ZoraCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.zoraServer().add(
                url,
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new ByteArrayEntity(SAMPLE_IMAGE)),
                        ZoraScheduler.ZORA_HTTPS_HEADER_KEY,
                        "true"));

            QueryConstructor qc = new QueryConstructor(IMAGE_ROUTE);
            qc.append(URL, url);

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[{\"height\":8,\"width\":8}]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void waitTest() throws Exception {
        final String url = "http://img.avito.st/1.jpg";

        try (ZoraCluster cluster = new ZoraCluster();
             CloseableHttpAsyncClient client = HttpAsyncClients.createDefault())
        {
            cluster.zoraServer().add(
                url,
                new SlowpokeHttpItem(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new ByteArrayEntity(SAMPLE_IMAGE)),
                        ZoraScheduler.ZORA_SOURCE_HEADER,
                        IEX_SOURCE),
                    200));

            client.start();

            QueryConstructor qc =
                new QueryConstructor(
                    "http://" + cluster.proxy().host().toHostString()
                        + IMAGE_ROUTE);

            qc.append(URL, url);

            final WaitingCallback cb = new WaitingCallback();
            MultiFutureCallback<HttpResponse> mfCalback =
                new MultiFutureCallback<>(cb);

            HttpGet request = new HttpGet(qc.toString());
            client.execute(request, mfCalback.newCallback());
            client.execute(request, mfCalback.newCallback());
            client.execute(request, mfCalback.newCallback());
            client.execute(request, mfCalback.newCallback());

            mfCalback.done();

            synchronized (cb) {
                cb.wait(1100);
            }

            if (cb.exception != null) {
                throw cb.exception;
            }

            Assert.assertNotNull(cb.result);
            String expected = "[{ \"height\": 8, \"width\": 8}]";

            for (HttpResponse response: cb.result) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private static final class WaitingCallback
        implements FutureCallback<List<HttpResponse>>
    {
        private List<HttpResponse> result;
        private Exception exception;

        @Override
        public synchronized void completed(
            final List<HttpResponse> responses)
        {
            System.out.println("Completed");
            this.result = responses;
            this.notifyAll();
        }

        @Override
        public synchronized void failed(final Exception e) {
            System.out.println("Failed");
            this.exception = e;
            e.printStackTrace();
            this.notifyAll();
        }

        @Override
        public synchronized void cancelled() {
            System.out.println("Cancelled");
            this.notifyAll();
        }
    }

    @Test
    public void nowaitTest() throws Exception {
        final String simpleImageURL1 =
            "http://02.img.avito.st/100x75/2718517802.jpg";
        final String simpleImageURL2 =
            "http://02.img.avito.st/8x8/image.jpg";

        try (ZoraCluster cluster = new ZoraCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.zoraServer().add(
                simpleImageURL1,
                new SlowpokeHttpItem(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(""),
                        ZoraScheduler.ZORA_SOURCE_HEADER,
                        IEX_SOURCE),
                    200));

            cluster.zoraServer().add(
                simpleImageURL2,
                new SlowpokeHttpItem(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new ByteArrayEntity(SAMPLE_IMAGE)),
                        ZoraScheduler.ZORA_SOURCE_HEADER,
                        IEX_SOURCE),
                    200));

            QueryConstructor qc = new QueryConstructor(IMAGE_ROUTE);
            qc.append(URL, simpleImageURL1);
            String baseRequest = qc.toString();

            final String nowait = "&nowait";
            String noWaitRequest = baseRequest + nowait;
            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(noWaitRequest)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_ACCEPTED, response);
            }

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(noWaitRequest)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_LOCKED, response);
            }

            String expected = "[{\"width\": -1, \"height\": -1}]";

            Thread.sleep(250);

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(noWaitRequest)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(noWaitRequest)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            //test valid png
            qc = new QueryConstructor(IMAGE_ROUTE);
            qc.append(URL, simpleImageURL2);
            baseRequest = qc.toString();

            noWaitRequest = baseRequest + nowait;
            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(noWaitRequest)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_ACCEPTED, response);
            }

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(noWaitRequest)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_LOCKED, response);
            }

            Thread.sleep(250);

            expected = "[{\"width\": 8, \"height\": 8}]";

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(noWaitRequest)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSuspicious() throws Exception {
        final String imageUrl1 =
            "http://02.img.avito.st/afbadfb1.jpg";
        //"http://02.img.avito.st/?afbadfb=1.jpg";
        final String imageUrl2 =
            "http://02.img.avito.st/afbadfb2.jpg";
        //"http://02.img.avito.st/?afbadfb=2.jpg";
        final String imageUrl3 =
            "http://02.img.avito.st/afbadfb3.jpg";

        try (ZoraCluster cluster = new ZoraCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.zoraServer().add(
                imageUrl1,
                new SlowpokeHttpItem(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new ByteArrayEntity(SAMPLE_IMAGE)),
                        ZoraScheduler.ZORA_SOURCE_HEADER,
                        IEX_SOURCE),
                    200));
            cluster.zoraServer().add(
                imageUrl2,
                new SlowpokeHttpItem(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new ByteArrayEntity(SAMPLE_IMAGE)),
                        ZoraScheduler.ZORA_SOURCE_HEADER,
                        IEX_SOURCE),
                    200));
            cluster.zoraServer().add(
                imageUrl3,
                new SlowpokeHttpItem(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new ByteArrayEntity(SAMPLE_IMAGE)),
                        ZoraScheduler.ZORA_SOURCE_HEADER,
                        IEX_SOURCE),
                    200));

            QueryConstructor qc = new QueryConstructor(IMAGE_ROUTE);
            qc.append(URL, imageUrl1);
            qc.append(URL, imageUrl2);
            qc.append(URL, imageUrl3);

            String sizes = "{\"height\":8,\"width\":8}";
            String expected =
                " [ "
                    + String.join(",", Arrays.asList(sizes, sizes, sizes))
                    + " ] ";
            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMultipleUrls() throws Exception {
        final String simpleImageURL1 =
            "http://02.img.avito.st/1.jpg";
        final String simpleImageURL2 =
            "http://02.img.avito.st/2.jpg";
        final String simpleImageURL3 =
            "http://02.img.avito.st/3.jpg";

        try (ZoraCluster cluster = new ZoraCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.zoraServer().add(
                simpleImageURL1,
                new SlowpokeHttpItem(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(""),
                        ZoraScheduler.ZORA_SOURCE_HEADER,
                        IEX_SOURCE),
                    200));

            cluster.zoraServer().add(
                simpleImageURL2,
                new SlowpokeHttpItem(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new ByteArrayEntity(SAMPLE_IMAGE)),
                        ZoraScheduler.ZORA_SOURCE_HEADER,
                        IEX_SOURCE),
                    200));
            cluster.zoraServer().add(
                simpleImageURL3,
                new SlowpokeHttpItem(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new ByteArrayEntity(SAMPLE_IMAGE)),
                        ZoraScheduler.ZORA_SOURCE_HEADER,
                        IEX_SOURCE),
                    200));

            QueryConstructor qc = new QueryConstructor(IMAGE_ROUTE);
            qc.append(URL, simpleImageURL2);
            qc.append(URL, simpleImageURL3);

            String sizes = "{\"height\": 8, \"width\": 8}";
            String expected = "[ " + sizes + ',' + sizes + " ]";
            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    // CSON: MagicNumber
}
