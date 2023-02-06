package ru.yandex.chemodan.app.docviewer.utils.html;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;

public class HtmlMinWidthProcessorTest extends DocviewerWebSpringTestBase {

    @Autowired
    private TestManager testManager;

    @Test
    public void testExcel() {
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(),
                UriUtils.toUrlString(TestResources.Microsoft_Excel_97_001p), TargetType.HTML_WITH_IMAGES);

        String pageHtmlXml = ApacheHttpClientUtils.downloadString(
                "http://localhost:32405/htmlwithimagespageinfo?uid=0&id=" + fileId + "&page=1");
        Assert.assertContains(pageHtmlXml, "<body class=\"b1\" min-width=\"162\"");

        String allHtmlXml = ApacheHttpClientUtils.downloadString(
                "http://localhost:32405/htmlwithimagesinfo?uid=0&id=" + fileId + "&page=1");
        Assert.assertContains(allHtmlXml, "<body class=\"b1\" min-width=\"162\"");
    }

}
