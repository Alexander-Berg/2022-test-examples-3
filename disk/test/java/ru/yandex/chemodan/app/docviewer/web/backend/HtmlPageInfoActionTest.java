package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.utils.Digester;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.chemodan.app.docviewer.web.framework.WebSecurityManager;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;

public class HtmlPageInfoActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private Digester digester;

    @Autowired
    private WebSecurityManager webSecurityManager;

    @Autowired
    private TestManager testManager;

    private void test(final TargetType targetType, final String actionPath, final boolean isXmlOutput) {
        test(targetType, actionPath, isXmlOutput, false);
    }

    private void test(final TargetType targetType, final String actionPath, final boolean isXmlOutput, final boolean mobile) {
        final PassportUidOrZero fakeUid = PassportUidOrZero.fromUid(666);

        final String url = UriUtils.toUrlString(TestResources.Microsoft_Word_97_001p);
        final String fileId = digester.calculateDigestId(new UrlInputStreamSource(
                TestResources.Microsoft_Word_97_001p));

        testManager.waitUriToComplete(fakeUid, url, targetType);
        webSecurityManager.validateFileRightUsingUid(fakeUid, fileId);
        HttpGet httpGet = new HttpGet("http://localhost:32405/" + actionPath + "?uid=666&id=" + fileId + "&page=1" +
                (isXmlOutput ? "" : "&json=true") + getMobileParameter(mobile));

        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(30));

        if (isXmlOutput) {
            Assert.assertContains(response,
                    "<page current-page=\"1\" total-pages=\"1\" width-sensitive=\"false\" extract-text-info=\"false\">");
            Assert.assertContains(response, "<page-html>");
        } else {
            Assert.assertContains(response, "\"page\"");
            Assert.assertContains(response, "\"current-page\":1");
            Assert.assertContains(response, "\"total-pages\":1");
            Assert.assertContains(response, "\"width-sensitive\":false");
            Assert.assertContains(response, "\"extract-text-info\":false");
            Assert.assertContains(response, "\"page-body\"");
            Assert.assertContains(response, "\"page-style\"");
        }
    }

    @Test
    public void testDom4jBugWorkarounds() {
        final String fileId = testManager.makeAvailable(PassportUidOrZero.zero(), UriUtils.toUrlString(TestResources.Microsoft_SpreadsheetML), TargetType.HTML_WITH_IMAGES);
        byte[] result = ApacheHttpClientUtils
                .download("http://localhost:32405/htmlwithimagespageinfo?uid=0&id=" + fileId
                        + "&page=1");
        String resultString = new String(result);
        Assert.assertContains(resultString,
                "<page current-page=\"1\" total-pages=\"1\" width-sensitive=\"false\" extract-text-info=\"false\">");
    }

    @Test
    @Ignore("flaky test")
    public void testHtmlOnly() {
        test(TargetType.HTML_ONLY, "htmlonlypageinfo", true);
    }

    @Test
    public void testHtmlOnlyJson() {
        test(TargetType.HTML_ONLY, "htmlonlypageinfo", false);
    }

    @Test
    public void testHtmlWithImages() {
        test(TargetType.HTML_WITH_IMAGES, "htmlwithimagespageinfo", true);
    }

    @Test
    public void testHtmlWithImagesForMobile() {
        test(TargetType.HTML_WITH_IMAGES_FOR_MOBILE, "htmlwithimagespageinfo", true, true);
    }

    @Test
    public void testHtmlWithImagesJson() {
        test(TargetType.HTML_WITH_IMAGES, "htmlwithimagespageinfo", false);
    }

    @Test
    public void testHtmlWithImagesJsonForMobile() {
        test(TargetType.HTML_WITH_IMAGES_FOR_MOBILE, "htmlwithimagespageinfo", false, true);
    }
}
