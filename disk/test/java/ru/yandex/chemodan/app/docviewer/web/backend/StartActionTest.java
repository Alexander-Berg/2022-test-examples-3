package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.test.Assert;

public class StartActionTest extends DocviewerWebSpringTestBase {

    @Test
    public void testYaDisk() {
        HttpGet method = new HttpGet(
                "http://localhost:32405/start?uid=128729656&type=PLAIN_TEXT&url="
                        + UrlUtils.urlEncode("ya-disk:///disk/one.txt"));
        String response = ApacheHttpClient4Utils.executeReadString(method);
        Assert.A.equals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<ok/>", response);
    }

    @Test
    public void parseSkipableMimeTypes() {
        Assert.hasSize(0, StartAction.parseSkipableContentTypes(null));
        Assert.hasSize(0, StartAction.parseSkipableContentTypes(""));

        ListF<String> skipMimes = StartAction.parseSkipableContentTypes("application/pdf,application/msword,text/plain");
        Assert.equals(Cf.list("application/pdf", "application/msword", "text/plain"), skipMimes);
    }

}
