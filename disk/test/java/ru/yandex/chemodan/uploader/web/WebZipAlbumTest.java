package ru.yandex.chemodan.uploader.web;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.mpfs.MpfsClientTest;
import ru.yandex.chemodan.uploader.registry.ApiVersion;
import ru.yandex.commune.archive.ArchiveEntry;
import ru.yandex.commune.archive.ArchiveListing;
import ru.yandex.commune.archive.ArchiveManager;
import ru.yandex.misc.io.InputStreamSourceUtils;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.io.http.apache.v4.ReadBytesResponseHandler;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class WebZipAlbumTest extends AbstractWebTestSupport {

    @Autowired
    private ArchiveManager archiveManager;

    @Test
    public void downloadZipAlbum() {
        String url = getPublicAlbumUrl(MpfsClientTest.ALBUM_PUBLIC_KEY);

        byte[] out = downloadZip(url);
        Assert.equals(327456, out.length);

        ArchiveListing listing = archiveManager.listArchive(InputStreamSourceUtils.bytes(out));
        Assert.equals(Cf.list("Новый альбом", "Новый альбом/apples.jpg", "Новый альбом/gus.jpg"),
                listing.getEntries().map(ArchiveEntry.getReadablePathF()).sorted());
        Assert.equals(0L, listing.getEntries().find(ArchiveEntry.pathEqualsToF("Новый альбом")).get().getSize().get());
        Assert.equals(202578L,
                listing.getEntries().find(ArchiveEntry.pathEqualsToF("Новый альбом/gus.jpg")).get().getSize().get());
        Assert.equals(124615L,
                listing.getEntries().find(ArchiveEntry.pathEqualsToF("Новый альбом/apples.jpg")).get().getSize().get());
    }

    private String getPublicAlbumUrl(String publicKey) {
        return UrlUtils.addParameter(
                "http://localhost:" + uploaderHttpPorts.getControlPort() + ApiUrls.ZIP_ALBUM_URL,
                ApiArgs.API_VERSION, ApiVersion.V_0_2.toSerializedString(),
                ApiArgs.ALBUM_PUBLIC_KEY, publicKey);
    }

    private byte[] downloadZip(String uri) {
        HttpUriRequest request = new HttpGet(uri);
        return ApacheHttpClientUtils.execute(request, new ReadBytesResponseHandler(), Timeout.seconds(20));
    }
}
