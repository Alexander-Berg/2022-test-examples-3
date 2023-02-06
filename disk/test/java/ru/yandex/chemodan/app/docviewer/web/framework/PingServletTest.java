package ru.yandex.chemodan.app.docviewer.web.framework;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.test.Assert;

public class PingServletTest extends DocviewerWebSpringTestBase {

    @Test
    public void testBackend() {
        HttpGet httpGet = new HttpGet("http://localhost:32405/ping");
        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(10));
        Assert.A.equals("pong", response);
    }

    @Test
    public void testService() {
        HttpGet httpGet = new HttpGet("http://localhost:32401/ping");
        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(10));
        Assert.A.equals("pong", response);
    }

}
