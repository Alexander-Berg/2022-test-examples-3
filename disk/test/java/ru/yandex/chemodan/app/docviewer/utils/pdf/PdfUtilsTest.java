package ru.yandex.chemodan.app.docviewer.utils.pdf;

import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.states.PagesInfoHelper;
import ru.yandex.chemodan.app.docviewer.utils.DimensionO;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;

public class PdfUtilsTest {

    @Test
    public void guessFirstPageSize() {
        Cf.<URL, DimensionO>map()
                .plus1(TestResources.Adobe_Acrobat_Docviewer_1277, DimensionO.cons(794, 1235))
                .plus1(TestResources.Adobe_Acrobat_1_4_004p_landscape, DimensionO.cons(842, 595))
                .plus1(TestResources.Adobe_Acrobat_Wider_Than_1280, DimensionO.cons(1280, 720))
                .forEach((url, size) -> Assert.A.equals(size, PdfUtils.getRenderedPageSize(PdfUtils.withExistingDocument(
                        new UrlInputStreamSource(url), true, PagesInfoHelper::toPagesInfo), 0),
                        url + " size differs"));
    }

    @Test
    @Ignore("Nobody remembered what this test was about")
    public void testCharsNoUnicodeMapping() {
        String supported = PdfUtils.withExistingDocument(new UrlInputStreamSource(
                TestResources.Adobe_Acrobat_1_3_001p_2columns), true, PdfUtils
                .filterTextOperatorsHandler(PdfUtils.preserveInHtmlHandler(false))
                .asFunctionReturnParam().andThen(PdfUtils::stripText));

        Assert.assertTrue(supported.contains("Documents"));
        Assert.assertTrue(supported.contains("Stack"));
        Assert.assertFalse(supported.contains("?????????"));

        String unsupported = PdfUtils.withExistingDocument(new UrlInputStreamSource(
                TestResources.Adobe_Acrobat_1_3_001p_2columns), true, PdfUtils
                .filterTextOperatorsHandler(PdfUtils.preserveInBackgroundHandler(false))
                .asFunctionReturnParam().andThen(PdfUtils::stripText));

        Assert.assertFalse(unsupported.contains("Documents"));
        Assert.assertFalse(unsupported.contains("Stack"));
        Assert.assertTrue(unsupported.contains("?????????"));
    }

    @Test
    public void testMath() {
        String supported = PdfUtils.withExistingDocument(new UrlInputStreamSource(
                TestResources.Adobe_Acrobat_1_5_114p), true, a -> {
                    PdfUtils.filterTextOperators(a, PdfUtils.preserveInHtmlHandler(true));
                    return PdfUtils.stripText(a);
                });

        Assert.assertFalse(supported.contains("\u239b"));
        Assert.assertFalse(supported.contains("\u239c"));
        Assert.assertFalse(supported.contains("\u239d"));
        Assert.assertFalse(supported.contains("\u239e"));
        Assert.assertFalse(supported.contains("\u239f"));
        Assert.assertFalse(supported.contains("\u23a0"));

        Assert.assertFalse(supported.contains("\ufe02"));

        String unsupported = PdfUtils.withExistingDocument(new UrlInputStreamSource(
                TestResources.Adobe_Acrobat_1_5_114p), true,
                PdfUtils.filterTextOperatorsHandler(PdfUtils.preserveInBackgroundHandler(true))
                        .asFunctionReturnParam().andThen(PdfUtils::stripText));

        Assert.assertTrue(unsupported.contains("\u239b"));
        Assert.assertTrue(unsupported.contains("\u239c"));
        Assert.assertTrue(unsupported.contains("\u239d"));
        Assert.assertTrue(unsupported.contains("\u239e"));
        Assert.assertTrue(unsupported.contains("\u239f"));
        Assert.assertTrue(unsupported.contains("\u23a0"));

        Assert.assertTrue(unsupported.contains("("));
        Assert.assertTrue(unsupported.contains("\ufe02"));
        Assert.assertTrue(unsupported.contains(")"));
    }

    @Test
    public void testNoWarnings() {
        PdfUtils.withExistingDocument(
                new UrlInputStreamSource(TestResources.Adobe_Acrobat_1_5_114p), true,
                PdfUtils::stripText);
    }

}
