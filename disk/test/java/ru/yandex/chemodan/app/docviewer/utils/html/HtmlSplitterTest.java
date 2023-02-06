package ru.yandex.chemodan.app.docviewer.utils.html;

import java.util.List;

import javax.annotation.Resource;

import org.dom4j.Document;
import org.joda.time.Instant;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.utils.ByteArrayOutputStreamSource;
import ru.yandex.chemodan.app.docviewer.utils.DimensionO;
import ru.yandex.chemodan.app.docviewer.utils.FileList;
import ru.yandex.chemodan.app.docviewer.utils.XmlSerializer;
import ru.yandex.chemodan.app.docviewer.utils.XmlUtils2;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.StringReaderSource;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.TimeUtils;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author akirakozov
 * @author ssytnik
 * @author vlsergey
 */
public class HtmlSplitterTest extends DocviewerSpringTestBase {

    private static final Logger logger = LoggerFactory.getLogger(HtmlSplitterTest.class);

    @Autowired
    private HtmlSplitter htmlSplitter;

    @Autowired
    private HtmlPostprocessor htmlPostprocessor;

    // Mock, only for testing
    private static class File2Mock extends File2 {
        private final ListF<File2> children;

        public File2Mock(String path, ListF<File2> childs) {
            super(path);
            this.children = childs;
        }

        @Override
        public ListF<File2> listRegularFiles() {
            return children;
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public boolean isRegular() {
            return false;
        }
    }

    private static FileList getFolderMock() {
        // virtual folder with images
        return new FileList(new File2Mock("folder", Cf.list(new File2("image 1.jpeg"), new File2("image2.jpeg"))));
    }

    @Test
    public void testImagesWithForbiddenName() {
        Document source = Dom4jUtils.read(new StringReaderSource(
                "<html><body><img src=\"image 1.jpeg\"/></body></html>"));
        List<byte[]> result = htmlSplitter.splitHtml(source, true, Option.of(getFolderMock()), false);

        Assert.assertEquals(1, result.size());
        String result0 = new String(Dom4jUtils.write(source));
        String result1 = new String(result.get(0));

        Assert.assertContains(result0, "<img/>");
        Assert.assertContains(result1, "<img/>");

        Document toOutput = source;
        htmlPostprocessor.preprocessOutput(toOutput, Option.of("somefileid"), DimensionO.WIDTH_1024);
        String result2 = new String(Dom4jUtils.write(source));
        Assert.assertContains(result2, "<img/>");
    }

    @Test
    public void testLocalImage() {
        Document source = Dom4jUtils.read(new StringReaderSource(
                "<html><body><img src=\"image2.jpeg\"/></body></html>"));
        List<byte[]> result = htmlSplitter.splitHtml(source, true, Option.of(getFolderMock()), false);

        Assert.assertEquals(1, result.size());
        String result0 = new String(Dom4jUtils.write(source));
        String result1 = new String(result.get(0));

        Assert.assertContains(result0, "<img x-docviewer-img-local-src=\"image2.jpeg\"/>");
        Assert.assertContains(result1, "<img x-docviewer-img-local-src=\"image2.jpeg\"/>");

        Document toOutput = source;
        htmlPostprocessor.preprocessOutput(toOutput, Option.of("somefileid"), DimensionO.WIDTH_1024);
        String result2 = new String(Dom4jUtils.write(source));
        Assert.assertContains(result2,
                "<img src=\"./htmlimage?id=somefileid&amp;name=image2.jpeg\"/>");
    }

    @Test
    public void testHttpLinkImage() {
        Document source = Dom4jUtils.read(new StringReaderSource(
                "<html><body><img src=\"http://localhost/image3.jpeg\" /></body></html>"));
        List<byte[]> result = htmlSplitter.splitHtml(source, true, Option.of(getFolderMock()), false);

        Assert.assertEquals(1, result.size());
        String result0 = new String(Dom4jUtils.write(source));
        String result1 = new String(result.get(0));

        Assert.assertContains(result0,
                "<img src=\"http://h.yandex.net/?http%3A%2F%2Flocalhost%2Fimage3.jpeg\"/>");
        Assert.assertContains(result1,
                "<img src=\"http://h.yandex.net/?http%3A%2F%2Flocalhost%2Fimage3.jpeg\"/>");

        Document toOutput = source;
        htmlPostprocessor.preprocessOutput(toOutput, Option.of("somefileid"), DimensionO.WIDTH_1024);
        String result2 = new String(Dom4jUtils.write(source));
        Assert.assertContains(result2,
                "<img src=\"http://h.yandex.net/?http%3A%2F%2Flocalhost%2Fimage3.jpeg\"/>");
    }

    @Test
    public void testImagePlaceHolder() {
        Document source = Dom4jUtils.read(new StringReaderSource(
                "<html><body><img src=\"image4.jpeg\" /></body></html>"));
        List<byte[]> result = htmlSplitter.splitHtml(source, true, Option.of(getFolderMock()), false);

        Assert.assertEquals(1, result.size());
        String result0 = new String(Dom4jUtils.write(source));
        String result1 = new String(result.get(0));

        Assert.assertContains(result0, "<img x-docviewer-img-placeholder=\"true\"/>"); // image4.jpeg
        Assert.assertContains(result1, "<img x-docviewer-img-placeholder=\"true\"/>");

        Document toOutput = source;
        htmlPostprocessor.preprocessOutput(toOutput, Option.of("somefileid"), DimensionO.WIDTH_1024);
        String result2 = new String(Dom4jUtils.write(source));
        Assert.assertContains(result2, "<img src=\"./htmlimage?placeholder=true\"/>");
    }

    private void checkAnchors(String source2) {
        String source = "<p><a href=\"#section1\">link</a></p>";
        List<byte[]> result = HtmlSplitterWorker.processAnchors(
                Cf.list(source.getBytes(), source2.getBytes()), Dom4jUtils.createPrettyFormat());
        Assert.assertEquals(2, result.size());
        String part1 = new String(result.get(0));

        Assert.assertContains(part1, "<a href=\"#section1\" x-docviewer-page-num-ref=\"2\">link</a>");
    }

    @Test
    public void processAnchors() {
        checkAnchors("<p><h2 id=\"section1\">Frist</h2></p>");
    }

    @Test
    public void processAnchors2() {
        checkAnchors("<p><h2><a name=\"section1\">Frist</a></h2></p>");
    }

    @Test
    public void testMsxmlOpenoffice() throws Exception {
        Document doc = XmlUtils2.parseHtmlToDom4j(new UrlInputStreamSource(
                TestResources.HTML_MSXML_OPENOFFICE));

        List<byte[]> results = htmlSplitter.splitHtml(doc, true, Option.empty(), false);
        String xml = new String(results.get(0), "utf-8");
        Assert.assertContains(xml, "<TD align=\"LEFT\" colspan=\"3\">№ 53482338</TD>");

        ByteArrayOutputStreamSource htmlSerialized = new ByteArrayOutputStreamSource();
        Document pageHtml = Dom4jUtils.read(new ByteArrayInputStreamSource(results.get(0)));
        xmlSerializer.serializeToXml(pageHtml, htmlSerialized);
        String html = new String(htmlSerialized.getByteArray(), "utf-8");
        Assert.assertContains(html, "<TD align=\"LEFT\" colspan=\"3\">№ 53482338</TD>");
    }

    @Resource
    private XmlSerializer xmlSerializer;

    @Test
    public void testSpeed() {
        logger.debug("Hotting...");
        for (int i = 0; i < 3; i++) {
            Document document = Dom4jUtils.read(new UrlInputStreamSource(
                    TestResources.HTML_FLAT));
            htmlSplitter.splitHtml(document, false, Option.empty(), false);
        }

        Document document = Dom4jUtils.read(new UrlInputStreamSource(
                TestResources.HTML_FLAT));

        Instant start = TimeUtils.now();
        htmlSplitter.splitHtml(document, false, Option.empty(), false);
        logger.debug("done in {} s", TimeUtils.secondsStringToNow(start));
    }

}
