package ru.yandex.tikaite.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentProducer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;

// CSOFF: MagicNumber
public class PdfClownTest extends TestBase {
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String CONTENT_TYPE_META = "Content-Type:";
    private static final String PDFCLOWN = "\nparser:pdfclown";
    private static final String TRUNCATED = "truncated";
    private static final int TIMEOUT = 20000;

    public PdfClownTest() {
        super(false, 0L);
    }

    @Test
    public void testPlanPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains(
                "Жилой дом со встроенными помещениями"),
            true,
            null,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_PDF + PDFCLOWN));
        Map<String, Object> root = json.root();
        root.put(CommonFields.PAGES, 1L);
        root.put(CommonFields.PRODUCER, "PDFTron PDFNet");
        root.put(CommonFields.TOOL, "ArchiCAD");
        String plan = "plan.pdf";
        DiskHandlerTest.testJson(plan, json);
        root.put(TRUNCATED, 2048L);
        DiskHandlerTest.testJson(plan, json, "&limit=2048");
    }

    @Test
    public void testProgressPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.AllOf(
                new Json.Contains("01.02.2012"),
                new Json.Contains(
                    "ознакомиться с инструкцией по эксплуатации,")),
            true,
            null,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_PDF + PDFCLOWN));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1327898426L);
        root.put(CommonFields.MODIFIED, 1332925932L);
        root.put(CommonFields.PAGES, 4L);
        root.put(CommonFields.PRODUCER, "Solid Converter PDF  (7.1.934.0)");
        root.put(CommonFields.TOOL, "Microsoft Office Word");
        DiskHandlerTest.testJson("progress.pdf", json);
    }

    @Test
    public void testMonarchPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains(
                "\nInformation is correct at time of entry into "
                + "website but may be subject to alteration without notice."),
            true,
            null,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_PDF + PDFCLOWN));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "lex");
        root.put(CommonFields.CREATED, 1377589963L);
        root.put(CommonFields.MODIFIED, 1377589963L);
        root.put(CommonFields.PAGES, 4L);
        root.put(CommonFields.PRODUCER, "GPL Ghostscript 9.07");
        root.put(
            CommonFields.TITLE,
            "Monarch - Book Flights Online and Buy Cheap Flight Tickets");
        root.put(CommonFields.TOOL, "PScript5.dll Version 5.2.2");
        String plan = "monarch.pdf";
        DiskHandlerTest.testJson(plan, json);
        root.put(TRUNCATED, 1000L);
        DiskHandlerTest.testJson(plan, json, "&limit=1000");
    }

    @Test
    public void testControlPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains(
                "Использование ПИД регулятора\nОписание\n"),
            true,
            Json.ANY_VALUE,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_PDF + PDFCLOWN));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "ELENA");
        root.put(CommonFields.CREATED, 1192432941L);
        root.put(CommonFields.MODIFIED, 1192429422L);
        root.put(CommonFields.PAGES, 253L);
        root.put(CommonFields.PRODUCER, "Acrobat Distiller 5.0 (Windows)");
        root.put(CommonFields.TITLE, "ATV_21_obl.p65");
        root.put(CommonFields.TOOL, "PageMaker 6.5");
        DiskHandlerTest.testJson("control.pdf", json);
    }

    @Test
    public void testUshakinPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.AllOf(
                new Json.Contains(
                    "понять, каким образом достигается"),
                new Json.Contains(
                    "По мнению Драгомирова, военное дело "
                    + "одновременно основывалось на "
                    + "двух противоречивых требованиях:"),
                new Json.Contains(
                    "максимальное число женщин противника")),
            true,
            null,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_PDF + PDFCLOWN));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1312504220L);
        root.put(CommonFields.MODIFIED, 1336923445L);
        root.put(CommonFields.PAGES, 719L);
        DiskHandlerTest.testJson("ushakin.pdf", json);
    }

    @Test
    public void testAeroflot() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains("Если в течение 20 минут после отправки SMS"),
            true,
            null,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_PDF + PDFCLOWN));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1415387898L);
        root.put(CommonFields.MODIFIED, 1415387898L);
        root.put(CommonFields.PAGES, 1L);
        root.put(CommonFields.PRODUCER, "iText 2.1.7 by 1T3XT");
        root.put(
            CommonFields.TOOL,
            "BIRT Report Engine /usr/local/tomcat72/webapps/birt-server/WEB-IN"
            + "F/lib/org.eclipse.birt.runtime_3.7.1.v20110913-1734.jar using i"
            + "Text /usr/local/tomcat72/webapps/birt-server/WEB-INF/lib/org.ec"
            + "lipse.birt.runtime_3.7.1.v20110913-1734.jar.");
        DiskHandlerTest.testJson("aeroflot.pdf", json);
    }

    @Test
    public void testBadPdfInput() throws Exception {
        ContentProducer producer = new ContentProducer() {
            @Override
            public void writeTo(final OutputStream out) throws IOException {
                byte[] buf = new byte[TIMEOUT];
                try (InputStream in = new FileInputStream(
                        Paths.getSandboxResourcesRoot() + "/control.pdf"))
                {
                    final int toRead = 2000000;
                    int left = toRead;
                    while (left > 0) {
                        int read = in.read(buf, 0, Math.min(left, buf.length));
                        out.write(buf, 0, read);
                        left -= read;
                        out.flush();
                    }
                    try {
                        Thread.sleep(TIMEOUT);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(
                ServerTest.getConfig(
                    backend.port(),
                    "\nserver.fragment-size-hint = 0")))
        {
            backend.add("/get/control.pdf?raw", producer);
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    "http://localhost:" + server.port()
                    + "/get/control.pdf?name=disk")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                DiskHandlerTest.assertInvalidJson(response);
            }
        }
    }

    @Test
    public void testTablePdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains("Статистический шаг интервала"),
            true,
            Json.ANY_VALUE,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_PDF + PDFCLOWN));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "днс");
        root.put(CommonFields.CREATED, 1543067257L);
        root.put(CommonFields.MODIFIED, 1543067257L);
        root.put(CommonFields.PAGES, 1L);
        root.put(CommonFields.PRODUCER, "Adobe PDF Library 15.0");
        root.put(CommonFields.TOOL, "Acrobat PDFMaker 18 для Excel");
        DiskHandlerTest.testJson("pirson.pdf", json);
    }

    @Test
    public void testRandomPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains("Дисперсия – характеристика случайной величины"),
            true,
            Json.ANY_VALUE,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_PDF + PDFCLOWN));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "днс");
        root.put(CommonFields.CREATED, 1543067043L);
        root.put(CommonFields.MODIFIED, 1543067046L);
        root.put(CommonFields.PAGES, 1L);
        root.put(CommonFields.PRODUCER, "Adobe PDF Library 15.0");
        root.put(CommonFields.TOOL, "Acrobat PDFMaker 18 для Excel");
        DiskHandlerTest.testJson("random.pdf", json);
    }

    @Test
    public void testIronstarPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains("Дата и время закрытия соревнований"),
            true,
            null,
            new Json.Headers(CONTENT_TYPE_META + APPLICATION_PDF + PDFCLOWN));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1491160491L);
        root.put(CommonFields.MODIFIED, 1491223474L);
        root.put(CommonFields.PAGES, 7L);
        root.put(CommonFields.PRODUCER, "Skia/PDF m59");
        DiskHandlerTest.testJson("ironstar.pdf", json);
    }
}
// CSON: MagicNumber

