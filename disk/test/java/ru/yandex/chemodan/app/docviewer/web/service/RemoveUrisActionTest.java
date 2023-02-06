package ru.yandex.chemodan.app.docviewer.web.service;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.test.Assert;

public class RemoveUrisActionTest extends DocviewerWebSpringTestBase {

    @Test
    public void test() {
        HttpGet httpGet = new HttpGet("http://localhost:32401/removeuris");
        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(10));
        Assert.A.equals("ok", response);
    }

}
