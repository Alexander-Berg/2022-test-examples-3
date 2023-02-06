package ru.yandex.bannerstorage.harvester.queues.processdynamiccode.processors;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ru.yandex.bannerstorage.harvester.queues.processdynamiccode.ProcessDynamicCodeTest;
import ru.yandex.bannerstorage.harvester.queues.processdynamiccode.exceptions.RelativeUriNotSupportedException;

import static org.testng.Assert.assertEquals;

/**
 * @author egorovmv
 */
public class ElementWithCssProcessorTest extends ProcessDynamicCodeTest {
    @DataProvider
    private static Object[][] getQuoteList() {
        return new Object[][]{
                {""},
                {"'"},
                {"\""},
        };
    }

    @Test(dataProvider = "getQuoteList")
    public void testSingleImgUrl(String quote) {
        String actual = ElementWithCssProcessor.processCss(
                " url ( " + quote + SOURCE_IMG_URL + quote + " ) ",
                createImageUploader(DEST_IMG_URL));
        assertEquals(actual, " url(" + quote + DEST_IMG_URL + quote + ") ");
    }

    @Test(dataProvider = "getQuoteList")
    public void singleImgUrlWithoutOpeningParenthesis(String quote) {
        String testValue = " url " + quote + SOURCE_IMG_URL + quote + " ) ";
        String actual = ElementWithCssProcessor.processCss(
                testValue, createImageUploader(DEST_IMG_URL));
        assertEquals(actual, testValue);
    }

    @Test(dataProvider = "getQuoteList")
    public void singleImgUrlWithoutClosingParenthesis(String quote) {
        String testValue = " url ( " + quote + SOURCE_IMG_URL + quote;
        String actual = ElementWithCssProcessor.processCss(
                testValue, createImageUploader(DEST_IMG_URL));
        assertEquals(actual, testValue);
    }

    @Test(dataProvider = "getQuoteList", expectedExceptions = RelativeUriNotSupportedException.class)
    public void singleRelativeImgUrl(String quote) {
        ElementWithCssProcessor.processCss(
                "url( " + quote + REL_SOURCE_IMG_URL + quote + " ) ",
                createImageUploader(REL_DEST_IMG_URL));
    }

    @Test(dataProvider = "getQuoteList")
    public void testMultipleImgUrls(String quote) {
        String actual = ElementWithCssProcessor.processCss(
                String.format(
                        "url ( %1$s%2$s%1$s ) %1$s%2$s%1$s url(%1$s%2$s%1$s)",
                        quote, SOURCE_IMG_URL),
                createImageUploader(DEST_IMG_URL));
        assertEquals(
                actual,
                String.format(
                        "url(%1$s%3$s%1$s) %1$s%2$s%1$s url(%1$s%3$s%1$s)",
                        quote, SOURCE_IMG_URL, DEST_IMG_URL));
    }
}
