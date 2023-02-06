package ru.yandex.chemodan.uploader.social;

import org.junit.Test;

import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;

/**
 * @author akirakozov
 */
public class SocialProxyClientTest extends TestBase {

    private static final String host = "host.name";

    @Test
    public void checkUploadPhotoRequestUrl() {
        SocialProxyClient spc = new SocialProxyClient(host, ApacheHttpClientUtils.singleConnectionClient(Timeout.seconds(10)));
        Assert.equals(
                "http://host.name/proxy2/profile/prof_abc/photo_post/get_request?aid=alb_def",
                spc.getUploadPhotoRequestUrl("prof_abc", "alb_def"));
    }

    @Test
    public void checkUploadPhotoCommitUrl() {
        SocialProxyClient spc = new SocialProxyClient(host, ApacheHttpClientUtils.singleConnectionClient(Timeout.seconds(10)));
        Assert.equals(
                "http://host.name/proxy2/profile/prof_abc/photo_post/commit",
                spc.getUploadPhotoCommitUrl("prof_abc"));
    }

}
