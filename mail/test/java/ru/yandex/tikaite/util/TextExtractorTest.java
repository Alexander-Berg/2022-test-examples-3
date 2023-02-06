package ru.yandex.tikaite.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.mime.MediaType;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.test.util.TestBase;

public class TextExtractorTest extends TestBase {
    private static final String APPLICATION_XML = "application/xml";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TEXT_HTML = "text/html";
    private static final String BASE64 = "V2FyIGlzIHBlYWNl";
    private static final String QUOTED_PRINTABLE =
        "=3Chtml><body>Freedom is slavery =C2=A9 <i>1984</i></body></html>";
    private static final String XML_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final int BIG_BAD_XML_ELEMENTS = 1024;

    public TextExtractorTest() {
        super(false, 0L);
    }

    private static DetectionResult detectStreamType(final InputStream is)
        throws IOException
    {
        return TextExtractor.INSTANCE.detectStreamType(
            TikaInputStream.get(is),
            new TextExtractOptions());
    }

    @Test
    public void testXhtml() throws IOException {
        StringWriter writer = new StringWriter();
        try (DetectionResult dr = detectStreamType(
                getClass().getResourceAsStream("xhtml.txt")))
        {
            Assert.assertEquals(
                TEXT_HTML,
                dr.mediaType().getBaseType().toString());
            Assert.assertNull(
                TextExtractor.INSTANCE.extractText(
                    dr,
                    writer,
                    new TextExtractOptions()).cause());
            Assert.assertEquals("Hello, world", writer.toString());
        }
    }

    @Test
    public void testOctetStream() throws IOException {
        StringWriter writer = new StringWriter();
        try (DetectionResult dr = detectStreamType(
                getClass().getResourceAsStream("random.txt")))
        {
            Assert.assertSame(MediaType.OCTET_STREAM, dr.mediaType());
            Assert.assertNull(
                TextExtractor.INSTANCE.extractText(
                    dr,
                    writer,
                    new TextExtractOptions()).cause());
            Assert.assertEquals("", writer.toString());
        }
    }

    @Test
    public void testBadXml() throws IOException {
        StringWriter writer = new StringWriter();
        try (DetectionResult dr =
                detectStreamType(
                    new ByteArrayInputStream(
                        (XML_HEADER + "<root><el/><el></root>")
                            .getBytes(StandardCharsets.UTF_8))))
        {
            Assert.assertEquals(APPLICATION_XML, dr.mediaType().toString());
            TextExtractResult result = TextExtractor.INSTANCE.extractText(
                dr,
                writer,
                new TextExtractOptions());
            Assert.assertEquals("", writer.toString());
            Assert.assertEquals(-1, result.truncated());
            Assert.assertNotNull(result.cause());
        }
    }

    @Test
    public void testBigBadXml() throws IOException {
        StringWriter writer = new StringWriter();
        StringBuilder sb = new StringBuilder(XML_HEADER);
        sb.append("<root>");
        for (int i = 0; i < BIG_BAD_XML_ELEMENTS; ++i) {
            sb.append("<element>line #" + i + "</element>\n");
        }
        sb.append("<element></root>");
        try (DetectionResult dr =
                detectStreamType(
                    new ByteArrayInputStream(
                        sb.toString().getBytes(StandardCharsets.UTF_8))))
        {
            Assert.assertEquals(APPLICATION_XML, dr.mediaType().toString());
            TextExtractResult result = TextExtractor.INSTANCE.extractText(
                dr,
                writer,
                new TextExtractOptions());
            Assert.assertEquals(
                "line #0",
                new BufferedReader(
                    new StringReader(writer.toString())).readLine());
            Assert.assertEquals(-1, result.truncated());
            Assert.assertNotNull(result.cause());
        }
    }

    @Test
    public void testBadRtf() throws IOException {
        StringWriter writer = new StringWriter();
        try (DetectionResult dr =
                detectStreamType(
                    new ByteArrayInputStream(
                        "{\\rtf1\\pard\\plain\\ltrpar\\bin10}"
                            .getBytes(StandardCharsets.UTF_8))))
        {
            Assert.assertEquals("application/rtf", dr.mediaType().toString());
            Assert.assertNotNull(
                TextExtractor.INSTANCE.extractText(
                    dr,
                    writer,
                    new TextExtractOptions()).cause());
        }
    }

    @Test(expected = IOException.class)
    @SuppressWarnings("InputStreamSlowMultibyteRead")
    public void testBadInputStream() throws IOException {
        detectStreamType(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException();
            }
        });
    }

    @Test
    public void testBadClosingInputStream() throws IOException {
        StringWriter writer = new StringWriter();
        DetectionResult dr = detectStreamType(
            new ByteArrayInputStream(BASE64.getBytes(StandardCharsets.UTF_8)) {
                private boolean open = true;
                @Override
                public void close() throws IOException {
                    if (open) {
                        open = false;
                        throw new IOException();
                    }
                }
            });
        Assert.assertEquals(
            TEXT_PLAIN,
            dr.mediaType().getBaseType().toString());
        TextExtractResult result = TextExtractor.INSTANCE.extractText(
            dr,
            writer,
            new TextExtractOptions());
        Assert.assertEquals(BASE64, writer.toString());
        Assert.assertNull(result.cause());
        try {
            dr.close();
        } catch (IOException e) {
            return;
        }
        Assert.fail();
    }

    @Test(expected = IOException.class)
    public void testVeryBadClosingInputStream() throws IOException {
        detectStreamType(
            new ByteArrayInputStream(BASE64.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public void close() throws IOException {
                    throw new IOException();
                }
            }).close();
    }

    @Test
    public void testBadWriter() throws IOException {
        Writer writer = new Writer() {
            @Override
            public void close() {
            }

            @Override
            public void flush() {
            }

            @Override
            public void write(final char[] cbuf, final int off, final int len)
                throws IOException
            {
                throw new IOException();
            }
        };
        try (DetectionResult dr =
                detectStreamType(
                    new ByteArrayInputStream(
                        QUOTED_PRINTABLE.getBytes(StandardCharsets.UTF_8))))
        {
            Assert.assertEquals(
                TEXT_PLAIN,
                dr.mediaType().getBaseType().toString());
            try {
                TextExtractor.INSTANCE.extractText(
                    dr,
                    writer,
                    new TextExtractOptions());
                Assert.fail();
            } catch (IOException e) {
                return;
            }
        }
    }

    @Test
    public void testSSRF() throws Exception {
        String uri = "/uri";
        try (StaticServer server = new StaticServer(Configs.baseConfig())) {
            server.add(uri, "");
            server.start();
            for (int i = 0; i <= 1; ++i) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                    zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
                    String contentTypes =
                        loadResourceAsString("docx/Content_Types.xml");
                    if (i == 0) {
                        contentTypes =
                            contentTypes.replace(
                                "_HTTP_PORT_",
                                Integer.toString(server.port()));
                    } else {
                        contentTypes =
                            contentTypes.replace(
                                "<!DOCTYPE Types [<!ELEMENT Types ANY >\n"
                                + "<!ENTITY % sp SYSTEM \"http://localhost:"
                                + "_HTTP_PORT_/uri\">\n"
                                + "%sp;%param1;]>\n",
                                "");
                    }
                    zos.write(
                        contentTypes.getBytes(StandardCharsets.UTF_8));
                    zos.putNextEntry(new ZipEntry("_rels/"));
                    zos.putNextEntry(new ZipEntry("_rels/.rels"));
                    zos.write(
                        loadResourceAsString("docx/_rels/.rels")
                            .getBytes(StandardCharsets.UTF_8));
                    zos.putNextEntry(new ZipEntry("word/"));
                    zos.putNextEntry(new ZipEntry("word/document.xml"));
                    zos.write(
                        loadResourceAsString("docx/word/document.xml")
                            .getBytes(StandardCharsets.UTF_8));
                    zos.putNextEntry(new ZipEntry("word/_rels/"));
                    zos.putNextEntry(
                        new ZipEntry("word/_rels/document.xml.rels"));
                    zos.write(
                        loadResourceAsString(
                            "docx/word/_rels/document.xml.rels")
                            .getBytes(StandardCharsets.UTF_8));
                }
                try (DetectionResult dr = detectStreamType(
                    new ByteArrayInputStream(baos.toByteArray())))
                {
                    Assert.assertEquals(0, server.accessCount(uri));
                    StringWriter writer = new StringWriter();
                    TextExtractResult result = TextExtractor.INSTANCE.extractText(
                        dr,
                        writer,
                        new TextExtractOptions());
                    String expectedType;
                    String expectedText;
                    if (i == 0) {
                        expectedType = "application/x-tika-ooxml";
                        expectedText = "";
                    } else {
                        expectedType =
                            "application/vnd.openxmlformats-officedocument."
                            + "wordprocessingml.document";
                        expectedText = "test";
                    }
                    Assert.assertEquals(
                        expectedType,
                        dr.mediaType().getBaseType().toString());
                    Assert.assertEquals(expectedText, writer.toString());
                    if (i == 0) {
                        Assert.assertNotNull(result.cause());
                    } else {
                        Assert.assertNull(result.cause());
                    }
                }
            }
        }
    }
}

