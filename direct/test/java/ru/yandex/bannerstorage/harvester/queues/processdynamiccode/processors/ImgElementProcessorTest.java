package ru.yandex.bannerstorage.harvester.queues.processdynamiccode.processors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.testng.annotations.Test;

import ru.yandex.bannerstorage.harvester.queues.processdynamiccode.ProcessDynamicCodeTest;
import ru.yandex.bannerstorage.harvester.queues.processdynamiccode.exceptions.RelativeUriNotSupportedException;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

/**
 * @author egorovmv
 */
public class ImgElementProcessorTest extends ProcessDynamicCodeTest {
    private static Element createImgElement(String srcAttribute) {
        return Jsoup.parse(
                String.format(
                        "<html><head><title>title</title></head><body><img id=\"elem\" %s /></body></html>",
                        srcAttribute != null ? "src=\"" + srcAttribute + "\"" : ""))
                .select("#elem")
                .first();
    }

    @Test
    public void testElementWithoutSrcAttribute() {
        Element img = createImgElement(null);
        new ImgElementProcessor().processElement(img, createImageUploader(null));
        assertFalse(img.hasAttr("src"));
    }

    @Test
    public void testElementWithEmptySrcAttribute() {
        Element img = createImgElement("");
        new ImgElementProcessor().processElement(img, createImageUploader(null));
        assertEquals(img.attr("src"), "");
    }

    @Test
    public void testElementWithSrcLookLikeImgUrl() {
        Element img = createImgElement(SOURCE_IMG_URL);
        new ImgElementProcessor().processElement(img, createImageUploader(DEST_IMG_URL));
        assertEquals(img.attr("src"), DEST_IMG_URL);
    }

    @Test(expectedExceptions = RelativeUriNotSupportedException.class)
    public void testElementWithRelativeImgUrl() {
        Element img = createImgElement(REL_SOURCE_IMG_URL);
        new ImgElementProcessor().processElement(img, createImageUploader(REL_DEST_IMG_URL));
    }
}
