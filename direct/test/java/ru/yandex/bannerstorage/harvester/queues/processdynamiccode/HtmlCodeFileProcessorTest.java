package ru.yandex.bannerstorage.harvester.queues.processdynamiccode;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ru.yandex.bannerstorage.harvester.queues.processdynamiccode.exceptions.ExternalUriForbiddenException;

import static org.testng.Assert.assertEquals;

/**
 * @author egorovmv
 */
public class HtmlCodeFileProcessorTest extends ProcessDynamicCodeTest {
    private Document createValidDocument(String imageUrl) {
        return Jsoup.parse(
                String.format(
                        "<html>" +
                                "<head>" +
                                "<title>title</title>" +
                                "<style>.style{background:url(%1$s)}</style>" +
                                "<style type=\"text/css\">.style{background:url(%1$s)}</style>" +
                                "<style type=\"text\">.style{background:url(http://localhost/source.jpg)}</style>" +
                                "</head>" +
                                "<body>" +
                                "<img src='%1$s' />" +
                                "<div style='background:url(%1$s)'></div>" +
                                "<script type=\"text/javascript\"></script>" +
                                "<script type=\"text/javascript\">" +
                                "var s = '%1$s';" +
                                "</script>" +
                                "<script type=\"text/vbscript\">" +
                                "var s = 'http://localhost/source.jpg';" +
                                "</script>" +
                                "</body>" +
                                "</html>",
                        imageUrl));
    }

    @DataProvider
    private Object[][] testValidDocumentDataProvider() {
        return getValidImageExtensions()
                .stream()
                .map(e -> new Object[]{e})
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "testValidDocumentDataProvider")
    public void testValidDocument(String imageExtension) {
        Document testValue = createValidDocument(
                SOURCE_IMG_URL + imageExtension);
        new HtmlCodeFileProcessor().processDocument(
                testValue,
                createImageUploader(DEST_IMG_URL + imageExtension));
        assertEquals(
                testValue.toString(),
                createValidDocument(DEST_IMG_URL + imageExtension).toString());
    }

    private Document createDocumentWithElementWithForbiddenAttribute() {
        return Jsoup.parse(
                "<html>" +
                        "<head>" +
                        "<title>title</title>" +
                        "<link href=\"a.css\" />" +
                        "</head>" +
                        "<body>" +
                        "</body>" +
                        "</html>");
    }

    @Test(expectedExceptions = ExternalUriForbiddenException.class)
    public void testDocumentWithElementWithForbiddenAttribute() {
        Document testValue = createDocumentWithElementWithForbiddenAttribute();
        new HtmlCodeFileProcessor().processDocument(
                testValue,
                createImageUploader(null));
    }
}
