package ru.yandex.chemodan.app.docviewer.web.backend;

import java.awt.image.RenderedImage;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.utils.DimensionO;
import ru.yandex.chemodan.app.docviewer.utils.ImageUtils;
import ru.yandex.chemodan.app.docviewer.utils.html.HtmlPostprocessor;
import ru.yandex.chemodan.app.docviewer.utils.pdf.image.PdfHelper;
import ru.yandex.chemodan.app.docviewer.utils.pdf.image.PdfRenderTargetTypeHolder;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;

public class HtmlImageActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private TestManager testManager;

    @Autowired
    PdfHelper pdfHelper;

    @Autowired
    PdfRenderTargetTypeHolder pdfRenderTargetTypeHolder;


    @Test
    public void testPdfBackgroundNoText() {
        int width = 1024;
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(),
                TestResources.Adobe_Acrobat_1_5_114p, TargetType.HTML_WITH_IMAGES);
        pdfHelper.removeHtmlBackground(fileId, 1, DimensionO.cons(width));

        String ext = pdfRenderTargetTypeHolder.getTargetType().value();
        byte[] firstPage = ApacheHttpClientUtils.download("http://localhost:32405" + HtmlImageAction.PATH
                + "?uid=0&id=" + fileId + "&name=bg-0." + ext + "&width=" + width);

        RenderedImage image = ImageUtils.read(new ByteArrayInputStreamSource(firstPage));
        Assert.assertEquals(width, image.getWidth());
    }

    @Test
    public void testPdfBackgroundWithImage() {
        testPdfBackgroundWithImageBase(false);
    }

    @Test
    public void testPdfBackgroundWithImageMobile() {
        testPdfBackgroundWithImageBase(true);
    }

    private void testPdfBackgroundWithImageBase(boolean mobile) {
        int width = 1024;
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(),
                TestResources.Adobe_Acrobat_1_5_114p, getConvertTargetMobileIncluded(mobile));
        pdfHelper.removeHtmlBackground(fileId, 25, DimensionO.cons(width));

        String ext = pdfRenderTargetTypeHolder.getTargetType().value();
        byte[] page25 = ApacheHttpClientUtils.download("http://localhost:32405" + HtmlImageAction.PATH
                + "?uid=0&id=" + fileId + "&name=bg-24." + ext + "&width=" + width + getMobileParameter(mobile));
        RenderedImage image25 = ImageUtils.read(new ByteArrayInputStreamSource(page25));
        Assert.assertEquals(width, image25.getWidth());
        // has text on image
        Assert.assertTrue(page25.length > 10000);
    }

    @Test
    public void getImagePlaceHolder() {
        byte[] placeholder = ApacheHttpClientUtils.download("http://localhost:32405" + HtmlImageAction.PATH + "?placeholder=true");
        byte[] expected = new UrlInputStreamSource(HtmlPostprocessor.IMAGE_PLACEHOLDER_URL).readBytes();

        Assert.arraysEquals(expected, placeholder);
    }
}
