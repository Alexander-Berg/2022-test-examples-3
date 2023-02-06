package ru.yandex.downloader.tests;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import ru.yandex.bolts.collection.Option;
import ru.yandex.downloader.url.*;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.io.http.apache.v4.ReadBytesResponseHandler;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class ZipFolderTest extends DownloaderTestBase {
    public static final String PUBLIC_FOLDER_HASH = "GpcUq1Qaeanz1AqYgfEEw3AQ0yvyA9pY9t7785z95j4=";

    @Test
    public void downloadZipFolder() {
        String url = createBaseUrl(PUBLIC_FOLDER_HASH);
        byte[] result = ApacheHttpClientUtils.execute(
                new HttpGet(url), new ReadBytesResponseHandler(), Timeout.seconds(3), true);
        Assert.equals(443037, result.length);
    }

    private String createBaseUrl(String hash) {
        BaseUrlParams params = new BaseUrlParams();
        params.fileName = "archive.zip";
        params.disposition = Disposition.ATTACHMENT;
        params.targetRef = new StringTargetId(hash);
        params.hash = Option.some(hash);
        params.contentType = "application/zip";

        return UrlCreator.createZipUrl(params);
    }
}
