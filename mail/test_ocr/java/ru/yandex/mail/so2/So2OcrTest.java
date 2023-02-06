package ru.yandex.mail.so2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.junit.Assume;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.JsonSubsetChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class So2OcrTest extends TestBase {
    private static final int OCRAAS_INSTANCES = 3;

    public So2OcrTest() {
        super(false, 0L);
        System.setProperty("ADDITIONAL_CONFIG", "ocr-base.conf");
        System.setProperty("SELENIUM_INSTANCES", "1");
        System.setProperty(
            "OCRAAS_INSTANCES",
            Integer.toString(OCRAAS_INSTANCES));
        System.setProperty("QUEUE_OCR_WORKERS", "true");
        System.setProperty("OCRAAS_MAX_PROCESS_TIME", "0");
    }

    @Test
    public void testOcr() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/geckodriver").exists());
        try (So2Cluster cluster = new So2Cluster(
                this,
                "mail/so/daemons/so2/so2_config/files/ocr.conf"))
        {
            cluster.start();

            cluster.check(
                "/?extractor-name=ocr&output-format=ocr",
                "Content-Type: text/html; charset=utf-8\r\n\r\n"
                + "<h1>Hello, world</h1>\n"
                + "<p>прuвет</p><p><u>чудо</u> мир &lt;=)</p>\n"
                + "<br><a href=\"https://yandex.ru/#fragment\">Ссылка</a>",
                "{\"ocr_text\":"
                + "\"Hello, world\nпривет\nчудо мир <=)\nСсылка\","
                + "\"deobfuscated_ocr_text\":"
                + "\"hello world\nпривет\nчудо мир\nссылка\","
                + "\"plain_text\":"
                + "\"Hello, world\nпрuвет\nчудо\nмир <=)\nСсылка\","
                + "\"deobfuscated_plain_text\":"
                + "\"hello world\nпривет\nчудо\nмир\nссылка\","
                + "\"html_text\":\"<h1>Hello, world</h1>\n"
                + "<p>прuвет</p><p><u>чудо</u> мир &lt;=)</p>\n<br />"
                + "<a href=\\\"https://yandex.ru/#fragment\\\">"
                + "Ссылка</a>\"}");
        }
    }

    @Test
    public void testBlockquote1() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/geckodriver").exists());
        try (So2Cluster cluster = new So2Cluster(
                this,
                "mail/so/daemons/so2/so2_config/files/ocr.conf"))
        {
            cluster.start();

            cluster.check(
                "/?extractor-name=ocr&output-format=ocr",
                "Content-Type: multipart/mixed; boundary=boundary\r\n\r\n"
                + "--boundary\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n\r\n"
                + "<h1>Hello, world</h1>\n"
                + "<blockquote>Quotation here</blockquote>\n"
                + "<p>After quotation</p>\n"
                + "<img src=\"cid:apng\">\r\n"
                + "--boundary\r\n"
                + "Content-Type: image/png;name=a.png\r\n"
                + "Content-ID: apng\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + "iVBORw0KGgoAAAANSUhEUgAAABIAAAAaCAYAAAC6nQw6AAAAgElEQVQ"
                + "4y72TSw7AIAhEnYn3vzJd1bRGLD/LTkPgzQAQEWmLAPB6K2kj2IqCFh"
                + "rt7z+i44V28uj1Ikz0NXZToVURjfiM2Vo3i7wQ0aohsya7iebCMxWz+"
                + "xPyaCeXGX+eKrr3prZEWX9KNvuG6BF/XAvpnR4r/AFw6Po99zXnlhFd"
                + "Kq4oQ+f7uAoAAAAASUVORK5CYII=\r\n"
                + "--boundary--",
                "{\"ocr_text\":"
                + "\"Hello, world\nAfter quotation\nA\","
                + "\"deobfuscated_ocr_text\":"
                + "\"hello world\n%FirstName% quotation\","
                + "\"plain_text\":"
                + "\"Hello, world\nAfter quotation\","
                + "\"deobfuscated_plain_text\":"
                + "\"hello world\n%FirstName% quotation\","
                + "\"html_text\":\"<h1>Hello, world</h1>\n\n"
                + "<p>After quotation</p>\n"
                + "<img src=\\\"data:image/png;base64,"
                + "iVBORw0KGgoAAAANSUhEUgAAABIAAAAaCAYAAAC6nQw6AAAAgEl"
                + "EQVQ4y72TSw7AIAhEnYn3vzJd1bRGLD/LTkPgzQAQEWmLAPB6K2"
                + "kj2IqCFhrt7z+i44V28uj1Ikz0NXZToVURjfiM2Vo3i7wQ0aohs"
                + "ya7iebCMxWz+xPyaCeXGX+eKrr3prZEWX9KNvuG6BF/XAvpnR4r"
                + "/AFw6Po99zXnlhFdKq4oQ+f7uAoAAAAASUVORK5CYII=\\\" />"
                + "\"}");
        }
    }

    @Test
    public void testBlockquote2() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/geckodriver").exists());
        try (So2Cluster cluster = new So2Cluster(
                this,
                "mail/so/daemons/so2/so2_config/files/ocr.conf"))
        {
            cluster.start();

            cluster.check(
                "/?extractor-name=ocr&output-format=ocr",
                "Content-Type: text/html; charset=utf-8\r\n\r\n"
                + "<h1>Hello, world</h1>"
                + "<blockquote>Цитирование №1</blockquote>"
                // latin `o'
                + "<p>Пoсле цитирoвания</p>"
                + "<blockquote>Цитирование №2</blockquote>",
                "{\"ocr_text\":"
                + "\"Hello, world\nЦитирование №1\nПосле цитирования\n"
                + "Цитирование №2\","
                + "\"deobfuscated_ocr_text\":"
                + "\"hello world\nцитирование 1\nцитирования\n"
                + "цитирование 2\","
                + "\"plain_text\":"
                // latin `o'
                + "\"Hello, world\nЦитирование №1\nПoсле цитирoвания\n"
                + "Цитирование №2\","
                + "\"deobfuscated_plain_text\":"
                + "\"hello world\nцитирование 1\nцитирования\n"
                + "цитирование 2\","
                + "\"html_text\":\"<h1>Hello, world</h1>"
                + "<blockquote>Цитирование №1</blockquote>"
                // latin `o'
                + "<p>Пoсле цитирoвания</p>"
                + "<blockquote>Цитирование №2</blockquote>\"}");
        }
    }

    @Test
    public void testQueueing() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/geckodriver").exists());
        try (So2Cluster cluster = new So2Cluster(
                this,
                "mail/so/daemons/so2/so2_config/files/ocr.conf");
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            cluster.start();
            reactor.start();
            client.start();

            String uri = "/?extractor-name=ocr&output-format=ocr&only-so2";
            int count = 10;
            List<Future<HttpResponse>> futures = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                String body =
                    "Content-Type: text/html; charset=utf-8\r\n\r\n"
                    + "<h1>Hello, world</h1>\n"
                    + "<p>прuвет</p><p><u>чудо</u> мир &lt;=)</p>\n"
                    + "<br><a href=\"https://yandex.ru/#fragment\">Ссылка "
                    + i + "</a>";
                futures.add(
                    client.execute(
                        cluster.so2().host(),
                        new BasicAsyncRequestProducerGenerator(
                            uri + "&reqid=" + i,
                            body),
                        BasicAsyncResponseConsumerFactory.OK,
                        EmptyFutureCallback.INSTANCE));
            }

            for (int i = 0; i < count; ++i) {
                YandexAssert.check(
                    new JsonChecker(
                        "{\"ocr_text\":"
                        + "\"Hello, world\nпривет\nчудо мир <=)\nСсылка " + i
                        + "\",\"deobfuscated_ocr_text\":"
                        + "\"hello world\nпривет\nчудо мир\nссылка " + i
                        + "\",\"plain_text\":"
                        + "\"Hello, world\nпрuвет\nчудо\nмир <=)\nСсылка " + i
                        + "\",\"deobfuscated_plain_text\":"
                        + "\"hello world\nпривет\nчудо\nмир\nссылка " + i
                        + "\",\"html_text\":\"<h1>Hello, world</h1>\n"
                        + "<p>прuвет</p><p><u>чудо</u> мир &lt;=)</p>\n<br />"
                        + "<a href=\\\"https://yandex.ru/#fragment\\\">"
                        + "Ссылка " + i + "</a>\"}"),
                    CharsetUtils.toString(futures.get(i).get().getEntity()));
            }
        }
    }

    @Test
    public void testNarrowing() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/geckodriver").exists());
        System.setProperty("QUEUE_OCR_WORKERS", "false");
        try (So2Cluster cluster = new So2Cluster(this);
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            cluster.start();
            reactor.start();
            client.start();

            String uri = "/?hr";
            String body =
                "Content-Type: text/html; charset=utf-8\r\n\r\n"
                + "<h1>Hello, world</h1>\n"
                + "<p>прuвет</p><p><u>чудо</u> мир &lt;=)</p>\n"
                + "<br><a href=\"https://yandex.ru/#fragment\">Ссылка</a>";
            JsonSubsetChecker responseOcrChecker =
                new JsonSubsetChecker(
                    "{\"ocr_text\":"
                    + "\"Hello, world\nпривет\nчудо мир <=)\nСсылка\","
                    + "\"deobfuscated_ocr_text\":"
                    + "\"hello world\nпривет\nчудо мир\nссылка\","
                    + "\"plain_text\":"
                    + "\"Hello, world\nпрuвет\nчудо\nмир <=)\nСсылка\","
                    + "\"deobfuscated_plain_text\":"
                    + "\"hello world\nпривет\nчудо\nмир\nссылка\","
                    + "\"html_text\":\"<h1>Hello, world</h1>\n"
                    + "<p>прuвет</p><p><u>чудо</u> мир &lt;=)</p>\n<br />"
                    + "<a href=\\\"https://yandex.ru/#fragment\\\">"
                    + "Ссылка</a>\"}");
            int count = 20;
            List<Future<HttpResponse>> futures = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                futures.add(
                    client.execute(
                        cluster.so2().host(),
                        new BasicAsyncRequestProducerGenerator(
                            uri + "&extractor-name=ocr&only-so2&reqid=" + i,
                            body),
                        BasicAsyncResponseConsumerFactory.OK,
                        EmptyFutureCallback.INSTANCE));
                if (i == 0) {
                    // forcefully wait for warmup
                    futures.get(0).get();
                } else {
                    Thread.sleep(100L);
                }
            }

            int ocrDone = 0;
            for (Future<HttpResponse> future: futures) {
                String responseBody =
                    CharsetUtils.toString(future.get().getEntity());
                try {
                    YandexAssert.check(responseOcrChecker, responseBody);
                    ++ocrDone;
                } catch (AssertionError e) {
                }
            }
            logger.info("OCRs done: " + ocrDone);
            if (ocrDone <= OCRAAS_INSTANCES) {
                throw new AssertionError("Not enough successfull OCRs");
            }
            if (ocrDone == count) {
                throw new AssertionError("Too many successful OCRs");
            }
            // Check duplicate charts
            HttpAssert.golovanPanel(cluster.so2().host());
        } finally {
            System.setProperty("QUEUE_OCR_WORKERS", "true");
        }
    }

    @Test
    public void testExplicitImageSize() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/geckodriver").exists());
        try (So2Cluster cluster = new So2Cluster(
                this,
                "mail/so/daemons/so2/so2_config/files/ocr.conf"))
        {
            cluster.start();

            cluster.check(
                "/?extractor-name=ocr&output-format=ocr",
                "Content-Type: multipart/mixed; boundary=boundary\r\n\r\n"
                + "--boundary\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n\r\n"
                + "<img src=\"https://example.com/i.png\" width=1920>\n"
                + "<img src=\"https://example.com/i.png\" "
                + "style=\"width:1920px;height:auto\">\n"
                + "<img src=\"https://example.com/i.png\" "
                + "width=10 height=20>\n"
                + "<h1>Hello, world</h1>\n"
                + "<img src=\"cid:apng\" width=1920>\n"
                + "<h1>Hello again</h2>\n"
                + "--boundary\r\n"
                + "Content-Type: image/png;name=a.png\r\n"
                + "Content-ID: apng\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + "iVBORw0KGgoAAAANSUhEUgAAABIAAAAaCAYAAAC6nQw6AAAAgElEQVQ"
                + "4y72TSw7AIAhEnYn3vzJd1bRGLD/LTkPgzQAQEWmLAPB6K2kj2IqCFh"
                + "rt7z+i44V28uj1Ikz0NXZToVURjfiM2Vo3i7wQ0aohsya7iebCMxWz+"
                + "xPyaCeXGX+eKrr3prZEWX9KNvuG6BF/XAvpnR4r/AFw6Po99zXnlhFd"
                + "Kq4oQ+f7uAoAAAAASUVORK5CYII=\r\n"
                + "--boundary--",
                "{\"ocr_text\":"
                + "\"Hello, world\","
                + "\"deobfuscated_ocr_text\":"
                + "\"hello world\","
                + "\"plain_text\":"
                + "\"Hello, world\nHello again\","
                + "\"deobfuscated_plain_text\":"
                + "\"hello world\nhello again\","
                + "\"html_text\":\""
                + "<img height=\\\"0\\\" src=\\\"data:image/png;base64"
                + ",iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAADk"
                + "lEQVQIHQEDAPz/AAAAAAMAAW7VSZoAAAAASUVORK5CYII=\\\" "
                + "width=\\\"1920\\\" />\n"
                + "<img src=\\\"data:image/png;base64"
                + ",iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAADk"
                + "lEQVQIHQEDAPz/AAAAAAMAAW7VSZoAAAAASUVORK5CYII=\\\" "
                + "style=\\\"height:0px;width:1920px\\\" />\n"
                + "<img height=\\\"20\\\" "
                + "src=\\\"data:image/png;base64"
                + ",iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAADk"
                + "lEQVQIHQEDAPz/AAAAAAMAAW7VSZoAAAAASUVORK5CYII=\\\" "
                + "width=\\\"10\\\" />\n"
                + "<h1>Hello, world</h1>\n"
                + "<img src=\\\"data:image/png;base64,"
                + "iVBORw0KGgoAAAANSUhEUgAAABIAAAAaCAYAAAC6nQw6AAAAgEl"
                + "EQVQ4y72TSw7AIAhEnYn3vzJd1bRGLD/LTkPgzQAQEWmLAPB6K2"
                + "kj2IqCFhrt7z+i44V28uj1Ikz0NXZToVURjfiM2Vo3i7wQ0aohs"
                + "ya7iebCMxWz+xPyaCeXGX+eKrr3prZEWX9KNvuG6BF/XAvpnR4r"
                + "/AFw6Po99zXnlhFdKq4oQ+f7uAoAAAAASUVORK5CYII=\\\" "
                + "width=\\\"1920\\\" />\n"
                + "<h1>Hello again</h1>\"}");
        }
    }
}

