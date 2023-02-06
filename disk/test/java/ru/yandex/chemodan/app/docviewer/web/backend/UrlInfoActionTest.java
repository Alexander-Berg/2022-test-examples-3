package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;

public class UrlInfoActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private TestManager testManager;

    @Test
    public void testWord() {
        testWordBase(false);
    }

    @Test
    public void testWordMobile() {
        testWordBase(true);
    }

    private void testWordBase(boolean mobile) {
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(),
                UriUtils.toUrlString(TestResources.Microsoft_Word_97_001p), getConvertTargetMobileIncluded(mobile));

        HttpGet httpGet = new HttpGet("http://localhost:32405/urlinfo?uid=0&url="
                + UrlUtils.urlEncode(UriUtils.toUrlString(TestResources.Microsoft_Word_97_001p))
                + getMobileParameter(mobile));

        String response = ApacheHttpClientUtils.executeReadString(httpGet, Timeout.seconds(30));

        Assert.assertContains(response, "<file-id>" + fileId + "</file-id>");
        Assert.assertContains(response, "<state>COPIED</state>");
        Assert.assertContains(response, "<convert-state target=\"" + getConvertTargetMobileIncluded(mobile)
                + "\"><result-type>ZIPPED_HTML");
        Assert.assertContains(response, "<state>AVAILABLE</state>");
        Assert.assertContains(response, "<pages count=\"1\"/>");
        Assert.assertContains(response, "<size>12288</size>");
    }

    @Test
    public void testJPEG() {
        testJPEGBase(false);
    }

    @Test
    public void testJPEGMobile() {
        testJPEGBase(true);
    }

    private void testJPEGBase(boolean mobile) {
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(),
                UriUtils.toUrlString(TestResources.JPEG), getConvertTargetMobileIncluded(mobile));

        HttpGet httpGet = new HttpGet("http://localhost:32405/urlinfo?uid=0&url="
                + UrlUtils.urlEncode(UriUtils.toUrlString(TestResources.JPEG))
                + getMobileParameter(mobile));

        String response = ApacheHttpClientUtils.executeReadString(httpGet, Timeout.seconds(30));

        Assert.assertContains(response, "<file-id>" + fileId + "</file-id>");
        Assert.assertContains(response, "<state>COPIED</state>");
        Assert.assertContains(response, "<convert-state target=\"" + getConvertTargetMobileIncluded(mobile)
                + "\"><result-type>ZIPPED_HTML");
        Assert.assertContains(response, "<state>AVAILABLE</state>");
        Assert.assertContains(response, "<pages count=\"1\">");
        Assert.assertContains(response,
                "<page index=\"1\" width=\"400\" height=\"400\" height-to-width=\"1\"/><");
        Assert.assertContains(response, "<size>89807</size>");
    }
}
