package ru.yandex.chemodan.uploader.web;

import org.apache.http.client.methods.HttpPost;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.http.CommonHeaders;
import ru.yandex.chemodan.uploader.UploaderPorts;
import ru.yandex.misc.io.http.HttpException;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class WebUploadFromOfficeTest extends AbstractWebTestSupport {

    @Autowired
    private UploaderPorts uploaderPorts;

    @Test
    public void checkForbiddenError() {
        String url = "http://localhost:" + uploaderPorts.getDataPort() + ApiPrivateUrls.UPLOAD_TARGET_FROM_OFFICE;
        url = UrlUtils.addParameter(url,
                ApiArgs.ACCESS_TOKEN, "token",
                ApiArgs.ACCESS_TOKEN_TTL, "ttl",
                ApiArgs.RESOURCE_ID, "id");

        try {
            HttpPost post = new HttpPost(url);
            post.addHeader(CommonHeaders.YANDEX_CLOUD_REQUEST_ID, "yandex-cloud-123231-123");
            ApacheHttpClientUtils.executeReadString(post);
            Assert.fail();
        } catch (HttpException e) {
            Assert.some(HttpStatus.SC_401_UNAUTHORIZED, e.getStatusCode());
        }
    }
}
