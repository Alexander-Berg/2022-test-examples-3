package ru.yandex.chemodan.app.docviewer.web.backend;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.utils.pdf.image.PdfRenderTargetTypeHolder;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;

public class HtmlInfoActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private TestManager testManager;

    @Autowired
    PdfRenderTargetTypeHolder pdfRenderTargetTypeHolder;

    @Test
    public void testHtmlOnlyInfo() {
        Assert.assertContains(getHtmlOnlyInfo(true), "Hello");
    }

    @Test
    public void testHtmlOnlyInfoJson() {
        String resultString = getHtmlOnlyInfo(false);

        Assert.assertContains(resultString, "\"doc-body\"");
        Assert.assertContains(resultString, "\"doc-style\"");
        Assert.assertContains(resultString, "\"title\":\"Microsoft Word - Hello.rtf\"");
    }

    @Test
    public void testHtmlWithImagesInfo() {
        testHtmlWithImagesInfoBase(true, false);
    }

    @Test
    public void testHtmlWithImagesInfoMobile() {
        testHtmlWithImagesInfoBase(true, true);
    }

    @Test
    public void testHtmlWithImagesInfoJson() {
        testHtmlWithImagesInfoBase(false, false);
    }

    @Test
    public void testHtmlWithImagesInfoJsonMobile() {
        testHtmlWithImagesInfoBase(false, true);
    }

    private void testHtmlWithImagesInfoBase(boolean isXmlOutput, boolean mobile) {
        String fileId = prepareFileId(getConvertTargetMobileIncluded(mobile));
        String resultString = getHtmlWithImagesInfo(fileId, isXmlOutput, mobile);
        String expectedUrl = constructExpectedUrl(fileId);

        if (isXmlOutput) {
            Assert.assertContains(resultString, "<img height=\"2898\" src=\"" + expectedUrl + "\" width=\"2048\"/>");
        } else {
            Assert.assertContains(resultString, "<img height=\\\"2898\\\" src=\\\"" + expectedUrl + "\\\" width=\\\"2048\\\"/>");
        }

        if (isXmlOutput) {
            Assert.assertContains(resultString, "Hello");
        } else {
            Assert.assertContains(resultString, "\"doc-body\"");
            Assert.assertContains(resultString, "\"doc-style\"");
            Assert.assertContains(resultString, "\"title\":\"Microsoft Word - Hello.rtf\"");
        }
    }

    private String getHtmlWithImagesInfo(String fileId, boolean isXmlOutput, boolean mobile) {
        byte[] result = ApacheHttpClientUtils.download("http://localhost:32405/htmlwithimagesinfo?uid=0&id="
                + fileId + "&width=2048" + getXmlOutputParameter(isXmlOutput) + getMobileParameter(mobile));
        return new String(result);
    }

    private String getHtmlOnlyInfo(boolean isXmlOutput) {
        prepareFileId(TargetType.HTML_ONLY);

        byte[] result = ApacheHttpClientUtils.download("http://localhost:32405/htmlonlyinfo?uid=0"
                + "&id=b2z-gidupcm1c5s25gg6uv7uo7e0svmmt6hg48nbpfw4twjqhx8fefusroe2hr4hkemcsqu5b632fmu9dc9017i04g17ujk5jsuw3tb"
                + "&width=2048" + getXmlOutputParameter(isXmlOutput));

        return new String(result);
    }

    private String constructExpectedUrl(String fileId) {
        String ext = pdfRenderTargetTypeHolder.getTargetType().value();
        return "./htmlimage?id=" + fileId + "&amp;width=2048&amp;name=bg-0." + ext;
    }

    private String prepareFileId(TargetType htmlWithImages) {
        return testManager.makeAvailable(PassportUidOrZero.zero(), TestResources.Adobe_Acrobat_1_3_001p, htmlWithImages);
    }

    private String getXmlOutputParameter(boolean isXmlOutput) {
        return isXmlOutput ? "" : "&json=true";
    }
}
