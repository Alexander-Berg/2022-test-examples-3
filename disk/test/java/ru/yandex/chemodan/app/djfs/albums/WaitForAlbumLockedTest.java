package ru.yandex.chemodan.app.djfs.albums;

import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.djfs.core.album.DjfsAlbumsTestBase;
import ru.yandex.chemodan.app.djfs.core.album.GeoInfo;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.inside.geobase.RegionType;
import ru.yandex.misc.concurrent.CountDownLatches;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = {
        DjfsAlbumsTestContextConfiguration.class,
})
public class WaitForAlbumLockedTest extends DjfsAlbumsTestBase {

    @Test
    public void FailureOnWaiting() {
        addGeoPhoto();

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> transactionUtils.executeInNewOrCurrentTransaction(UID, () -> {
            albumDeltaDao.getCurrentRevisionWithLock(UID);
            CountDownLatches.await(latch);
        })).start();

        String url = "/api/v1/albums/exclude_photo_from_geo_album";
        try {
            a3TestHelper.post(url + "?uid=" + UID.asString() + "&path=/disk/image.jpg");
        } catch (Exception e) {
            Assert.isInstance(e.getCause(), SocketTimeoutException.class);
        } finally {
            latch.countDown();
        }
    }

    @Test
    public void ToWaitingIsOK() {
        addGeoPhoto();

        new Thread(() -> {
            transactionUtils.executeInNewOrCurrentTransaction(UID, () -> {
                albumDeltaDao.getCurrentRevisionWithLock(UID);
                try {
                    Thread.sleep(10000);
                } catch (Exception e) { }
            });
        }).start();

        String url = "/api/v1/albums/exclude_photo_from_geo_album";
        HttpResponse response = a3TestHelper.post(url + "?uid=" + UID.asString() + "&path=/disk/image.jpg");

        Assert.equals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
    }

    private void initAlbumsRevision() {
        albumDeltaDao.tryInitializeCurrentRevision(UID);
        diskInfoManager.disableGeoAlbumsGenerationInProgress(UID);
    }

    private void addGeoPhoto() {
        initAlbumsRevision();

        geobase.addRegion(FEDERAL_SUBJECT_REGION_ID, RegionType.FEDERAL_SUBJECT, Cf.list());
        geobase.addRegion(CITY_REGION_ID, RegionType.CITY, Cf.list(FEDERAL_SUBJECT_REGION_ID));
        geobase.setRegionForCoordinates(POINT_COORDINATES, 1, CITY_REGION_ID);
        geobase.setRegionForCoordinates(OTHER_POINT_COORDINATES, 1, FEDERAL_SUBJECT_REGION_ID);

        FileDjfsResource file1 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(100));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);
        geoAlbumManager.addPhotoToAlbum(file1, info);

        geoAlbumManager.postProcessFiles(Cf.list(DjfsResourceId.cons(UID, file1.getFileId().get())));
    }

    @Value("${a3.port}")
    private int port;

    private A3TestHelper a3TestHelper;

    @PostConstruct
    public void startServers() {
        this.a3TestHelper = new A3TestHelper(port);
        this.a3TestHelper.startServers(applicationContext);
    }

}
