package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.test.Assert;

public class FileInfoActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private TestManager testManager;

    @Test
    public void test() {
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(), UriUtils.toUrlString(TestResources.Microsoft_Word_97_001p), TargetType.PLAIN_TEXT);

        HttpGet httpGet = new HttpGet("http://localhost:32405/fileinfo?uid=0&id=" + fileId);
        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(30));

        Assert.assertContains(response,
                "<file-id>9hc-an2gjny6dx5ctizd7rkvnwxokkaljkt12r07gbnt461j1arb2o3fx8pu1p40hu5mrb3gy0bqhi8hr0lb8rwpac5k6xlenkxu9ek</file-id>");
        Assert.assertContains(response, "<state>COPIED</state>");
        Assert.assertContains(response, "<convert-state target=\"PLAIN_TEXT\">");
        Assert.assertContains(response, "<state>AVAILABLE</state>");
    }
}
