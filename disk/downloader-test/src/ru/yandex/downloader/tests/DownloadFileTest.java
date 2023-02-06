package ru.yandex.downloader.tests;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import ru.yandex.downloader.TestData;
import ru.yandex.downloader.url.BaseUrlParams;
import ru.yandex.downloader.url.Disposition;
import ru.yandex.downloader.url.MulcaTargetId;
import ru.yandex.downloader.url.UrlCreator;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class DownloadFileTest extends DownloaderTestBase {

    @Test
    public void downloadSmallFile() {
        String url = createBaseUrl(TestData.SMALL_TXT_FILE_STID);
        String result = ApacheHttpClientUtils.executeReadString(new HttpGet(url), true);
        Assert.equals("hello", result);
    }

    @Test
    public void downloadBigFile() {
        // It's not really big file, we just set fsize parameter, which used in zaberun
        String url = createBaseUrl(TestData.SMALL_TXT_FILE_STID);
        url = UrlUtils.addParameter(url, "fsize", "10000000");
        String result = ApacheHttpClientUtils.executeReadString(new HttpGet(url), true);
        Assert.equals("hello", result);
    }

    private String createBaseUrl(MulcaId mulcaId) {
        BaseUrlParams params = new BaseUrlParams();
        params.fileName = "hello.txt";
        params.contentType = "text/plain";
        params.targetRef = new MulcaTargetId(mulcaId);
        params.disposition = Disposition.ATTACHMENT;

        return UrlCreator.createDiskUrl(params);
    }
}
