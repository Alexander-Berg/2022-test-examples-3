package ru.yandex.chemodan.app.docviewer.web.backend;

import java.io.StringReader;

import org.apache.http.client.methods.HttpGet;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.YaDiskIntegrationTest;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author akirakozov
 */
public class ShareActionTest extends DocviewerWebSpringTestBase {
    @Autowired
    private TestManager testManager;

    @Autowired
    private Copier copier;

    @Before
    public void before() {
        copier.setEnableNativeUrlFetching(false);
    }

    @Test
    public void sharePublicFile() {
        testManager.makeAvailable(TestUser.TEST.uid,
                YaDiskIntegrationTest.TEST_URI_PUBLIC, TargetType.HTML_WITH_IMAGES);

        String url = UrlUtils.addParameter("http://localhost:32405/share",
                "uid", TestUser.TEST.uid.getUid(),
                "url", YaDiskIntegrationTest.TEST_URI_PUBLIC);

        String response = ApacheHttpClient4Utils.executeReadString(new HttpGet(url), Timeout.seconds(30));
        Element root = Dom4jUtils.readRootElement(new StringReader(response));
        Assert.equals("short-url", root.getName());
        Assert.equals("https://yadi.sk/i/GikQMsWr3PA3k8", root.getText());
    }

    @Test
    public void sharePublicNdaFile() {
        testManager.makeAvailableWithShowNda(TestUser.TEST_WITH_NDA.uid,
                YaDiskIntegrationTest.TEST_URI_PUBLIC_NDA, TargetType.PDF);

        String url = UrlUtils.addParameter("http://localhost:32405/share",
                "uid", TestUser.TEST_WITH_NDA.uid.getUid(),
                "url", YaDiskIntegrationTest.TEST_URI_PUBLIC_NDA,
                "show_nda", 1);

        ApacheHttpClientUtils.executeReadString(new HttpGet(url), Timeout.seconds(30));
    }
}
