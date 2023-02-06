package ru.yandex.bannerstorage.harvester.queues.processdynamiccode.processors;

import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ru.yandex.bannerstorage.harvester.queues.processdynamiccode.ProcessDynamicCodeTest;
import ru.yandex.bannerstorage.harvester.queues.processdynamiccode.exceptions.ExternalUriForbiddenException;

import static org.testng.Assert.assertEquals;

/**
 * @author egorovmv
 */
public class ScriptElementProcessorTest extends ProcessDynamicCodeTest {
    private Element createScriptElementWithNotEmptySrcAttribute() {
        return Jsoup.parse(
                "<html>" +
                        "<head><title>title</title></head>" +
                        "<body>" +
                        "<script id=\"elem\" src=\"http://host/a.js\">" +
                        "</script>" +
                        "</body></html>")
                .select("#elem")
                .first();
    }

    @Test(expectedExceptions = ExternalUriForbiddenException.class)
    public void testElementWithNotEmptySrcAttribute() {
        new ScriptElementProcessor().processElement(
                createScriptElementWithNotEmptySrcAttribute(),
                createImageUploader(null));
    }

    @DataProvider
    private Object[][] getQuoteDotExtensionList() {
        List<String> quotes = Arrays.asList("'", "\"");
        List<String> extensions = getValidImageExtensions();
        return quotes.stream()
                .flatMap(q -> extensions.stream().map(e -> new Object[]{q, e}))
                .toArray(Object[][]::new);
    }

    private Element createScriptElementWithImageUrlEnclosedBy(String imageUrl, String quote) {
        return Jsoup.parse(
                "<html>" +
                        "<head><title>title</title></head>" +
                        "<body>" +
                        "<script id=\"elem\">" +
                        "var s=" + quote + imageUrl + quote +
                        "</script>" +
                        "</body></html>")
                .select("#elem")
                .first();
    }

    @Test(dataProvider = "getQuoteDotExtensionList")
    public void testElementWithAbsoluteUrl(String quote, String extension) {
        Element scriptElement = createScriptElementWithImageUrlEnclosedBy(SOURCE_IMG_URL + extension, quote);
        new ScriptElementProcessor().processElement(
                scriptElement, createImageUploader(DEST_IMG_URL + extension));
        assertEquals(
                scriptElement.toString(),
                createScriptElementWithImageUrlEnclosedBy(DEST_IMG_URL + extension, quote).toString());
    }

    @Test(dataProvider = "getQuoteDotExtensionList")
    public void testElementWithRelativeUrl(String quote, String extension) {
        Element scriptElement = createScriptElementWithImageUrlEnclosedBy(
                REL_SOURCE_IMG_URL_WITHOUT_EXT + extension, quote);
        new ScriptElementProcessor().processElement(
                scriptElement, createImageUploader(REL_DEST_IMG_URL_WITHOUT_EXT + extension));
        assertEquals(
                scriptElement.toString(),
                createScriptElementWithImageUrlEnclosedBy(REL_DEST_IMG_URL_WITHOUT_EXT + extension, quote).toString());
    }

    @DataProvider
    private Object[][] getHtmlEntityList() {
        return new Object[][]{
                {"&"},
                {"<"},
                {">"},
        };
    }

    private Element createScriptElementWithHtmlEntity(String imageUrl, String htmlEntity) {
        return Jsoup.parse(
                "<html>" +
                        "<head><title>title</title></head>" +
                        "<body>" +
                        "<script id=\"elem\">" +
                        "var s=\"" + imageUrl + "\";\n" +
                        "var q=\"" + htmlEntity + "\";\n" +
                        "</script>" +
                        "</body></html>")
                .select("#elem")
                .first();
    }

    @Test(dataProvider = "getHtmlEntityList")
    public void testElementWithHttpEntity(String httpEntity) {
        String extension = ".jpg";
        Element scriptElement = createScriptElementWithHtmlEntity(
                SOURCE_IMG_URL + extension, httpEntity);
        new ScriptElementProcessor().processElement(
                scriptElement, createImageUploader(DEST_IMG_URL + extension));
        assertEquals(
                scriptElement.toString(),
                createScriptElementWithHtmlEntity(DEST_IMG_URL + extension, httpEntity).toString());
    }
}
