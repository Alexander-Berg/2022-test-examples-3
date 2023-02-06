package ru.yandex.chemodan.uploader.web;

import org.apache.http.client.methods.HttpPut;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.uploader.UploaderPorts;
import ru.yandex.chemodan.util.http.HttpClientUtils;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

/**
 * @author akirakozov
 */
public class WebSyncUploadTest extends AbstractWebTestSupport {

    @Autowired
    protected UploaderPorts uploaderHttpPorts;

    @Test
    public void syncUpload() {
        String hello = "hello";
        MapF<String, Object> params = Cf.<String, Object>map("api", "0.2").plus1("uid", "123")
                .plus1("file-id", "fakeid").plus1("path", "/disk/fake");

        String baseUrl = "http://localhost:" + uploaderHttpPorts.getDataPort() + "/sync-upload/disk";
        String url = UrlUtils.addParameters(baseUrl, params);
        HttpPut put = HttpClientUtils.httpPut(url, hello.getBytes());
        ApacheHttpClientUtils.executeReadString(put);
    }
}
