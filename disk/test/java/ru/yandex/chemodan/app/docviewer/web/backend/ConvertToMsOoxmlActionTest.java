package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.test.handlers.ReadContentTypeHandler;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author metal
 */
public class ConvertToMsOoxmlActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private TestManager testManager;
    @Autowired
    private Copier copier;

    @Test
    public void convertDocToDocxAndCheckResult() {
        testManager.cleanupUri(PassportUidOrZero.zero(), TestResources.Microsoft_Word_97_001p.toString());
        byte[] converted = ApacheHttpClientUtils.download(
                "http://localhost:32405/convert-to-ms-ooxml?uid=0&content-type=application/msword&url="
                        + TestResources.Microsoft_Word_97_001p);
        Assert.gt(converted.length, 0);
    }

    @Test
    @Ignore("Required remote resource")
    public void convertDocMailAttachToDocxAndCheckResult() {
        boolean oldValue = copier.isEnableNativeUrlFetching();
        try {
            copier.setEnableNativeUrlFetching(true);

            String url = "ya-mail://162411061562048528/1.1";
            testManager.cleanupUri(TestUser.TEST.uid, url);
            byte[] converted = ApacheHttpClientUtils.download(
                    "http://localhost:32405/convert-to-ms-ooxml?uid=" + TestUser.TEST.uid.toString() +
                            "&content-type=application/msword" +
                            "&url=" + url);
            Assert.gt(converted.length, 0);
        } catch (Exception e) {
            copier.setEnableNativeUrlFetching(oldValue);
        }
    }

    @Test
    public void convertEmptyXlsTreatedLikeXlsxAndCheckResult() {
        testManager.cleanupUri(PassportUidOrZero.zero(), TestResources.Microsoft_Excel_empty_ooxml.toString());
        byte[] converted = ApacheHttpClientUtils.download(
                "http://localhost:32405/convert-to-ms-ooxml?uid=0&content-type=application/vnd.ms-excel&url="
                        + TestResources.Microsoft_Excel_empty_ooxml);
        Assert.gt(converted.length, 0);
    }

    @Test
    public void convertDocToDocxAndCheckResultMimeType() {
        testManager.cleanupUri(PassportUidOrZero.zero(), TestResources.Microsoft_Word_97_001p.toString());
        String contentType = ApacheHttpClientUtils.execute(
                new HttpGet("http://localhost:32405/convert-to-ms-ooxml?uid=0&url="
                        + TestResources.Microsoft_Word_97_001p),
                new ReadContentTypeHandler());
        Assert.equals(MimeTypes.MIME_MICROSOFT_OOXML_WORD, contentType);
    }
}
