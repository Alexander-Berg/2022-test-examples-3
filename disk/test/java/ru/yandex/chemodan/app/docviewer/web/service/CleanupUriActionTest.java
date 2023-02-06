package ru.yandex.chemodan.app.docviewer.web.service;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;

/**
 * @author akirakozov
 */
public class CleanupUriActionTest extends DocviewerWebSpringTestBase {

    @Test
    public void test() {
        // just check params parsing
        HttpGet httpGet = new HttpGet("http://localhost:32401/cleanupuri?uid=1260145&url=ya-disk%3A%2F%2F%2Fdisk%2Fsicp.pdf");
        ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(10));
    }

}
