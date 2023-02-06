package ru.yandex.downloader.tests;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import ru.yandex.bolts.collection.Option;
import ru.yandex.downloader.url.BaseUrlParams;
import ru.yandex.downloader.url.Disposition;
import ru.yandex.downloader.url.StringTargetId;
import ru.yandex.downloader.url.UrlCreator;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.io.http.apache.v4.ReadBytesResponseHandler;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class ZipAlbumTest extends DownloaderTestBase {
    public static final String ZIP_PUBLIC_ALBUM =
            "hrqswxCKCpMch9dN0L2lwjo/Ed1+yxRKWUhOsihA5uNgvahc2S7Hz8Eugb6VQ3rwq/J6bpmRyOJonT3VoXnDag==";

    @Test
    public void downloadZipFolder() {
        String url = createBaseUrl(ZIP_PUBLIC_ALBUM);
        byte[] result = ApacheHttpClientUtils.execute(
                new HttpGet(url), new ReadBytesResponseHandler(), Timeout.seconds(3), true);
        Assert.equals(327224, result.length);
    }

    private String createBaseUrl(String hash) {
        BaseUrlParams params = new BaseUrlParams();
        params.fileName = "album.zip";
        params.disposition = Disposition.ATTACHMENT;
        params.targetRef = new StringTargetId(hash);
        params.hash = Option.some(hash);
        params.contentType = "application/zip";

        return UrlCreator.createZipAlbumUrl(params);
    }

}
