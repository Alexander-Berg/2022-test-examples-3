package ru.yandex.chemodan.app.djfs.core.album;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.mongodb.ReadPreference;
import lombok.SneakyThrows;
import org.bson.types.ObjectId;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonBoolean;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.serialize.JsonParser;
import ru.yandex.inside.geobase.RegionType;
import ru.yandex.misc.test.Assert;

public class GeoAlbumGenerationTest extends DjfsAlbumsTestBase {

    @Test
    public void newAlbumGeneration() {
        initAlbumsRevision();
        DjfsResourcePath imagePath = DjfsResourcePath.cons(UID, "/disk/image.jpg");

        DjfsFileId fileId = DjfsFileId.random();
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, imagePath,
                x -> x.fileId(fileId).coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        Assert.sizeIs(0, albumDao.getAlbums(UID, AlbumType.GEO));

        geoAlbumManager.addPhotoToAlbum(file, info);

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(1, albums);

        Album album = albums.get(0);
        Assert.equals(CITY_NAME, album.getTitle());
        Assert.some((long) CITY_REGION_ID, album.getGeoId());

        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(1, items);
        AlbumItem albumItem = items.get(0);
        Assert.equals(fileId.getValue(), albumItem.getObjectId());

        ListF<AlbumDeltaRaw> deltas = albumDeltaDao.findRaw(UID);
        Assert.sizeIs(3, deltas);
        // deltas:
        // 1. insert album
        // 2. insert item
        // 3. update album items count + album cover

        ListF<Long> deltaRevisions = deltas.map(x -> x.revision);
        for (int i = 1; i < deltaRevisions.length(); i++) {
            Assert.lt(deltaRevisions.get(i - 1), deltaRevisions.get(i));
        }

        Assert.some(deltas.last().revision, album.getRevision());
    }

    @Test
    public void appendToExistingAlbum() {
        initAlbumsRevision();
        Album album = createAlbum(UID, CITY_NAME, x -> x.type(AlbumType.GEO).geoId(Option.of((long) CITY_REGION_ID)));
        albumDao.insert(album);

        DjfsResourcePath imagePath = DjfsResourcePath.cons(UID, "/disk/image.jpg");
        DjfsFileId fileId = DjfsFileId.random();
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, imagePath,
                x -> x.fileId(fileId).coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file, info);

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(1, albums);

        Assert.equals(CITY_NAME, albums.get(0).getTitle());
        Assert.some((long) CITY_REGION_ID, albums.get(0).getGeoId());

        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(1, items);
        AlbumItem albumItem = items.get(0);
        Assert.equals(fileId.getValue(), albumItem.getObjectId());

        ListF<AlbumDeltaRaw> deltas = albumDeltaDao.findRaw(UID);
        Assert.sizeIs(2, deltas);  // insert item + update items count delta

        Assert.some(deltas.last().revision, albums.get(0).getRevision());
    }

    @Test
    public void appendSameElementTwice() {
        initAlbumsRevision();
        DjfsResourcePath imagePath = DjfsResourcePath.cons(UID, "/disk/image.jpg");
        DjfsFileId fileId = DjfsFileId.random();
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, imagePath,
                x -> x.fileId(fileId).coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file, info);
        geoAlbumManager.addPhotoToAlbum(file, info);

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(1, albums);

        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(albums.get(0).getId()), 100);
        Assert.sizeIs(1, items);
    }

    @Test
    public void unhiddenAlbum() {
        initAlbumsRevision();
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        for (int i = 0; i < 29; i++) {
            DjfsResourcePath imagePath = DjfsResourcePath.cons(UID, "/disk/image-" + i + ".jpg");
            DjfsFileId fileId = DjfsFileId.random();
            FileDjfsResource file = filesystem.createFile(PRINCIPAL, imagePath,
                    x -> x.fileId(fileId).coordinates(POINT_COORDINATES).exifTime(Instant.now()));
            geoAlbumManager.addPhotoToAlbum(file, info);

            ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.GEO);
            Assert.sizeIs(1, albums);
            Album album = albums.get(0);
            Assert.isTrue(album.isHidden());
        }

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(1, albums);
        Album album = albums.get(0);
        Long revisionBeforeLastAdd = album.getRevision().get();

        DjfsResourcePath imagePath = DjfsResourcePath.cons(UID, "/disk/image-new.jpg");
        DjfsFileId fileId = DjfsFileId.random();
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, imagePath,
                x -> x.fileId(fileId).coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        geoAlbumManager.addPhotoToAlbum(file, info);

        albums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(1, albums);
        album = albums.get(0);

        Assert.isFalse(album.isHidden());
        ListF<AlbumDeltaRaw> deltas = albumDeltaDao.findRaw(UID, revisionBeforeLastAdd, album.getRevision().get(), 100);

        Assert.sizeIs(2, deltas);  // new item insert delta + update delta (items count + visibility)

        ListF<JsonObject> updateDeltas = getDeltasByType(deltas, "update");
        ListF<JsonObject> isVisibleDeltas = updateDeltas.map(x -> getField(x, "is_visible")).filter(
                Option::isPresent).map(Option::get);
        Assert.sizeIs(1, isVisibleDeltas);

        Option<JsonBoolean> isVisibleValue = isVisibleDeltas.first().getByPathO("value", "boolean").filterByType(
                JsonBoolean.class);
        Assert.some(JsonBoolean.TRUE, isVisibleValue);
    }

    private Option<JsonObject> getField(JsonObject delta, String fieldId) {
        return delta.getByPathO("changes").filterByType(JsonArray.class).flatMap(JsonArray::getArray)
                .filterByType(JsonObject.class).filter(
                x -> x.getByPathO("field_id")
                        .filterByType(JsonString.class)
                        .filter(y -> y.getString().equals(fieldId))
                        .isNotEmpty()
        ).firstO();
    }

    private ListF<JsonObject> getDeltasByType(ListF<AlbumDeltaRaw> deltas, String type) {
        return deltas.map(x -> JsonParser.getInstance().parseArray(x.changes))
                .flatMap(JsonArray::getArray).filterByType(JsonObject.class)
                .filter(
                        x -> x.getByPathO("change_type")
                                .filterByType(JsonString.class)
                                .filter(y -> y.getString().equals(type))
                                .isNotEmpty()
                );
    }

    @Test
    @SneakyThrows
    public void testSimultaneousPhotoAdd() {
        initAlbumsRevision();
        Album album = createAlbum(UID, CITY_NAME, x -> x.type(AlbumType.GEO).geoId(Option.of((long) CITY_REGION_ID)));
        albumDao.insert(album);

        ListF<Tuple2<FileDjfsResource, GeoInfo>> photos = Cf.arrayList();

        for (int i = 0; i < 100; i++) {
            DjfsResourcePath imagePath = DjfsResourcePath.cons(UID, "/disk/image-" + i + ".jpg");
            DjfsFileId fileId = DjfsFileId.random();
            FileDjfsResource file = filesystem.createFile(PRINCIPAL, imagePath,
                    x -> x.fileId(fileId).coordinates(POINT_COORDINATES).exifTime(Instant.now()));
            GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

            photos.add(Tuple2.tuple(file, info));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(photos.size());
        ListF<Future<Boolean>> futures = Cf.arrayList();
        for (Tuple2<FileDjfsResource, GeoInfo> photo : photos) {
            futures.add(executorService.submit(
                    () -> {
                        SynchronizedHandlerHolder.set(true);
                        geoAlbumManager.addPhotoToAlbum(photo._1, photo._2);
                        return true;
                    }
            ));
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        for (Future<Boolean> f : futures) {
            Assert.isTrue(f.get());
        }

        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        for (int i = 0; i < allDeltas.size(); i++) {
            Assert.equals((long)(i + 1), allDeltas.get(i).revision);
        }
        Assert.some(allDeltas.last().revision, albumDeltaDao.getCurrentRevisionWithoutLock(UID));
    }

    @Test
    @SneakyThrows
    public void testSimultaneousPhotoAddWithNoAlbumCreateOnlyOneAlbum() {
        initAlbumsRevision();
        ListF<Tuple2<FileDjfsResource, GeoInfo>> photos = Cf.arrayList();

        for (int i = 0; i < 100; i++) {
            DjfsResourcePath imagePath = DjfsResourcePath.cons(UID, "/disk/image-" + i + ".jpg");
            DjfsFileId fileId = DjfsFileId.random();
            FileDjfsResource file = filesystem.createFile(PRINCIPAL, imagePath,
                    x -> x.fileId(fileId).coordinates(POINT_COORDINATES).exifTime(Instant.now()));
            GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

            photos.add(Tuple2.tuple(file, info));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(photos.size());
        ListF<Future<Boolean>> futures = Cf.arrayList();
        for (Tuple2<FileDjfsResource, GeoInfo> photo : photos) {
            futures.add(executorService.submit(
                    () -> {
                        SynchronizedHandlerHolder.set(true);
                        geoAlbumManager.addPhotoToAlbum(photo._1, photo._2);
                        return true;
                    }
            ));
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        for (Future<Boolean> f : futures) {
            Assert.isTrue(f.get());
        }

        ListF<Album> geoAlbums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(1, geoAlbums);
    }

    @Test
    public void dateModifyAlbumChangeForPhotoWithGreaterEtime() {
        initAlbumsRevision();
        Instant initialDateModified = Instant.now().minus(Duration.standardMinutes(100500));
        Instant photoExifTime = initialDateModified.plus(Duration.standardMinutes(42));

        Album album = createAlbum(UID, CITY_NAME,
                x -> x.type(AlbumType.GEO)
                        .geoId(Option.of((long) CITY_REGION_ID))
                        .dateModified(Option.of(initialDateModified))
        );
        albumDao.insert(album);

        FileDjfsResource file = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(photoExifTime));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file, info);

        album = albumDao.findGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(photoExifTime, album.getDateModified());

        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        ListF<JsonObject> updateDeltas = getDeltasByType(allDeltas, "update");
        ListF<JsonObject> modifiedChanges = updateDeltas.map(x -> getField(x, "modified")).filter(
                Option::isPresent).map(Option::get);
        Assert.sizeIs(1, modifiedChanges);
    }

    @Test
    public void dateModifyAlbumNotChangeForPhotoWithLessEtime() {
        Instant initialDateModified = Instant.now();
        Instant photoExifTime = initialDateModified.minus(100500);

        Album album = createAlbum(UID, CITY_NAME,
                x -> x.type(AlbumType.GEO)
                        .geoId(Option.of((long) CITY_REGION_ID))
                        .dateModified(Option.of(initialDateModified))
        );
        albumDao.insert(album);

        FileDjfsResource file = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(photoExifTime));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file, info);

        album = albumDao.findGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(initialDateModified, album.getDateModified());
    }

    @Test
    public void dateModifyAlbumSetForAlbumWithoutMtime() {
        initAlbumsRevision();
        Instant photoExifTime = Instant.now();

        Album album = createAlbum(UID, CITY_NAME,
                x -> x.type(AlbumType.GEO)
                        .geoId(Option.of((long) CITY_REGION_ID))
                        .dateModified(Option.empty())
        );
        albumDao.insert(album);

        FileDjfsResource file = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(photoExifTime));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file, info);

        album = albumDao.findGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(photoExifTime, album.getDateModified());

        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        ListF<JsonObject> updateDeltas = getDeltasByType(allDeltas, "update");
        ListF<JsonObject> modifiedChanges = updateDeltas.map(x -> getField(x, "modified")).filter(
                Option::isPresent).map(Option::get);
        Assert.sizeIs(1, modifiedChanges);
    }

    @Test
    public void testExcludeFromAlbum() {
        initAlbumsRevision();
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        Album album = createAlbum(UID, CITY_NAME,
                x -> x.type(AlbumType.GEO).geoId(Option.of((long) CITY_REGION_ID)));
        albumDao.insert(album);

        geoAlbumManager.addPhotoToAlbum(file, info);

        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(1, items);

        geoAlbumManager.removeFromGeoAlbum(file, info.regionId);

        items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(0, items);

        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        ListF<JsonObject> deleteDeltas = getDeltasByType(allDeltas, "delete");
        Assert.sizeIs(1, deleteDeltas);
    }

    @Test
    public void testCoverExcludeFromAlbum() {
        initAlbumsRevision();
        FileDjfsResource file1 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image1.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(100));
        FileDjfsResource file2 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image2.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(200));
        FileDjfsResource file3 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image3.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(300));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file3, info);
        geoAlbumManager.addPhotoToAlbum(file2, info);
        geoAlbumManager.addPhotoToAlbum(file1, info);

        Album album = albumDao.getAlbums(UID, AlbumType.GEO).first();
        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(3, items);

        geoAlbumManager.removeFromGeoAlbum(file3, info.regionId);

        items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(2, items);

        album = albumDao.getAlbums(UID, AlbumType.GEO).first();
        ObjectId expectedNewCoverId = items.filter(x -> x.getObjectId().equals(file2.getFileId().get().getValue()))
                .first().getId();
        Assert.some(expectedNewCoverId, album.getCoverId());
    }

    @Test
    public void testCoverUpdateIfMoreBeautifulOnSeparateFunction() {
        initAlbumsRevision();
        FileDjfsResource file1 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image1.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(100));
        FileDjfsResource file2 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image2.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(Option.empty()));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file1, info);
        ExtendedAlbum extendedAlbum = albumDao.findExtendedGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(file1.getResourceId().get(), extendedAlbum.coverResourceId);

        geoAlbumManager.addPhotoToAlbum(file2, info);
        extendedAlbum = albumDao.findExtendedGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(file1.getResourceId().get(), extendedAlbum.coverResourceId);  // should be the same

        albumDeltaDao.deleteAll(UID);

        filesystem.setAesthetics(PRINCIPAL, file2.getResourceId().get(), 200, false);
        file2 = (FileDjfsResource) filesystem.find(PRINCIPAL, file2.getPath(), Option.of(ReadPreference.primary())).get();
        FileDjfsResource finalFile2 = file2;

        geoAlbumManager.updateCover(file2, info);
        extendedAlbum = albumDao.findExtendedGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(file2.getResourceId().get(), extendedAlbum.coverResourceId);  // update on different callback

        ListF<AlbumDeltaRaw> deltas = albumDeltaDao.findRaw(UID);

        // check deltas
        ListF<JsonObject> updateDeltas = getDeltasByType(deltas, "update");

        // cover_resource_id delta
        ListF<JsonObject> coverResourceIdDeltas = updateDeltas.map(x -> getField(x, "cover_resource_id")).filter(
                Option::isPresent).map(Option::get);
        Assert.sizeIs(1, coverResourceIdDeltas);

        Option<JsonString> coverResourceIdValue = coverResourceIdDeltas.first()
                .getByPathO("value", "string").filterByType(JsonString.class);
        Assert.equals(file2.getResourceId().get().getValue(), coverResourceIdValue.get().getString());

        // cover_id delta
        ListF<JsonObject> coverIdDeltas = updateDeltas.map(x -> getField(x, "cover_id")).filter(
                Option::isPresent).map(Option::get);
        Assert.sizeIs(1, coverIdDeltas);

        Option<JsonString> coverIdValue = coverIdDeltas.first().getByPathO("value", "string")
                .filterByType(JsonString.class);
        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(extendedAlbum.album.getId()), 100);
        ObjectId coverId =
                items.find(x -> x.getObjectId().equals(finalFile2.getFileId().get().getValue())).map(AlbumItem::getId)
                        .first();
        Assert.equals(coverId.toHexString(), coverIdValue.get().getString());
    }

    @Test
    public void testCoverUpdateIfMoreBeautifulAndAestheticsAlreadyPresent() {
        initAlbumsRevision();
        FileDjfsResource file1 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image1.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(100));
        FileDjfsResource file2 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image2.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(200));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file1, info);
        ExtendedAlbum extendedAlbum = albumDao.findExtendedGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(file1.getResourceId().get(), extendedAlbum.coverResourceId);

        albumDeltaDao.deleteAll(UID);

        geoAlbumManager.addPhotoToAlbum(file2, info);
        extendedAlbum = albumDao.findExtendedGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(file2.getResourceId().get(), extendedAlbum.coverResourceId);  // should be new

        ListF<AlbumDeltaRaw> deltas = albumDeltaDao.findRaw(UID);

        // check deltas
        ListF<JsonObject> updateDeltas = getDeltasByType(deltas, "update");

        // cover_resource_id delta
        ListF<JsonObject> coverResourceIdDeltas = updateDeltas.map(x -> getField(x, "cover_resource_id")).filter(
                Option::isPresent).map(Option::get);
        Assert.sizeIs(1, coverResourceIdDeltas);

        Option<JsonString> coverResourceIdValue = coverResourceIdDeltas.first()
                .getByPathO("value", "string").filterByType(JsonString.class);
        Assert.equals(file2.getResourceId().get().getValue(), coverResourceIdValue.get().getString());

        // cover_id delta
        ListF<JsonObject> coverIdDeltas = updateDeltas.map(x -> getField(x, "cover_id")).filter(
                Option::isPresent).map(Option::get);
        Assert.sizeIs(1, coverIdDeltas);

        Option<JsonString> coverIdValue = coverIdDeltas.first().getByPathO("value", "string")
                .filterByType(JsonString.class);
        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(extendedAlbum.album.getId()), 100);
        ObjectId coverId =
                items.find(x -> x.getObjectId().equals(file2.getFileId().get().getValue())).map(AlbumItem::getId)
                        .first();
        Assert.equals(coverId.toHexString(), coverIdValue.get().getString());
    }

    @Test
    public void testCoverNotUpdatedIfLessBeautiful() {
        initAlbumsRevision();
        FileDjfsResource file1 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image1.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(100));
        FileDjfsResource file2 = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image2.jpg"),
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).aesthetics(50));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file1, info);
        ExtendedAlbum extendedAlbum = albumDao.findExtendedGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(file1.getResourceId().get(), extendedAlbum.coverResourceId);

        geoAlbumManager.addPhotoToAlbum(file2, info);
        extendedAlbum = albumDao.findExtendedGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(file1.getResourceId().get(), extendedAlbum.coverResourceId);  // should be the same

        albumDeltaDao.deleteAll(UID);

        geoAlbumManager.updateCover(file2, info);
        extendedAlbum = albumDao.findExtendedGeoAlbum(UID, CITY_REGION_ID).get();
        Assert.some(file1.getResourceId().get(), extendedAlbum.coverResourceId);  // not updated

        ListF<AlbumDeltaRaw> deltas = albumDeltaDao.findRaw(UID);
        Assert.sizeIs(0, deltas);
    }

    private void createGeoAlbumWithPhotos(int photoNum) {
        for (int i = 0; i < photoNum; i++) {
            DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-" + i + ".jpg");
            FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                    x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
            GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);
            geoAlbumManager.addPhotoToAlbum(file, info);
        }
    }

    private void createCityAndMicroDistrictAlbumsWithPhotos(int photoNum) {
        for (int i = 0; i < photoNum; i++) {
            DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-" + i + ".jpg");
            FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                    x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
            ListF<GeoInfo> infos = Cf.list(
                    new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY),
                    new GeoInfo(CITY_MICRODISTRICT_REGION_ID, CITY_MICRODISTRICT_NAME, RegionType.CITY_MICRODISTRICT)
            );
            infos.forEach(info -> geoAlbumManager.addPhotoToAlbum(file, info));
        }
    }

    @Test
    public void removeFromAlbumAfterTrashAppend() {
        initAlbumsRevision();
        int photoNum = 5;
        createGeoAlbumWithPhotos(photoNum);

        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-new.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);
        geoAlbumManager.addPhotoToAlbum(file, info);

        Album album = albumDao.getAlbums(UID, AlbumType.GEO).first();
        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(photoNum + 1, items);

        djfsResourceDao.changeParent(UID, file.getId(),
                djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.TRASH)).get().getId());
        Assert.isFalse(filesystem.find(PRINCIPAL, path, Option.of(ReadPreference.primary())).isPresent());

        GeoAlbumManager geoAlbumManagerStub = Mockito.spy(geoAlbumManager);
        Mockito.doReturn(Cf.list(info)).when(geoAlbumManagerStub).getResourceInfos(
                Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.list(info)).when(geoAlbumManagerStub).getResourceGeoIds(Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.map(RegionType.CITY, info.regionId)).when(geoAlbumManagerStub).getAllResourceGeoIds(Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getGenerationStrategies(Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getRemoveStrategies();

        geoAlbumManagerStub.postProcessFiles(Cf.list(file.getResourceId().get()));

        items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(photoNum, items);
        Assert.isFalse(items.map(AlbumItem::getObjectId).containsTs(file.getResourceId().get().getFileId().getValue()));
    }

    @Test
    public void restoreToAlbumAfterTrashRestore() {
        initAlbumsRevision();
        int photoNum = 5;
        createGeoAlbumWithPhotos(photoNum);

        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/trash/image-new.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        djfsResourceDao.changeParent(UID, file.getId(),
                djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK)).get().getId());
        Assert.isFalse(filesystem.find(PRINCIPAL, path, Option.of(ReadPreference.primary())).isPresent());

        GeoAlbumManager geoAlbumManagerStub = Mockito.spy(geoAlbumManager);
        Mockito.doReturn(Cf.list(info)).when(geoAlbumManagerStub).getResourceInfos(
                Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.list(info)).when(geoAlbumManagerStub).getResourceGeoIds(Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.map(RegionType.CITY, info.regionId)).when(geoAlbumManagerStub).getAllResourceGeoIds(Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getGenerationStrategies(Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getRemoveStrategies();

        geoAlbumManagerStub.postProcessFiles(Cf.list(file.getResourceId().get()));

        Album album = albumDao.getAlbums(UID, AlbumType.GEO).first();
        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(photoNum + 1, items);
        Assert.isTrue(items.map(AlbumItem::getObjectId).containsTs(file.getResourceId().get().getFileId().getValue()));
    }

    @Test
    public void testSkipPostProcessingForUsersWithoutGeoAlbums() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-new.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        GeoAlbumManager geoAlbumManagerStub = Mockito.spy(geoAlbumManager);
        Mockito.doReturn(Cf.list(info)).when(geoAlbumManagerStub).getResourceInfos(
                Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.list(info)).when(geoAlbumManagerStub).getResourceGeoIds(Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.map(RegionType.CITY, info.regionId)).when(geoAlbumManagerStub).getAllResourceGeoIds(Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getGenerationStrategies(Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getRemoveStrategies();

        geoAlbumManagerStub.postProcessFiles(Cf.list(file.getResourceId().get()));

        Assert.sizeIs(0, albumDao.getAlbums(UID, AlbumType.GEO));
        Assert.isFalse(geoAlbumManager.hasGeoAlbums(UID));
    }

    @Test
    public void addSamePhotoToSeveralAlbums() {
        initAlbumsRevision();
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-new.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        ListF<GeoInfo> infos = Cf.list(
                new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY),
                new GeoInfo(CITY_MICRODISTRICT_REGION_ID, CITY_MICRODISTRICT_NAME, RegionType.CITY_MICRODISTRICT)
        );
        infos.forEach(i -> geoAlbumManager.addPhotoToAlbum(file, i));

        ListF<Album> geoAlbums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(2, geoAlbums);

        for (Album album : geoAlbums) {
            ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
            Assert.sizeIs(1, items);
            Assert.equals(items.first().getObjectId(), file.getFileId().get().getValue());
        }
    }

    @Test
    public void removeFromSeveralAlbumsAfterTrashAppend() {
        initAlbumsRevision();
        int photoNum = 5;
        createCityAndMicroDistrictAlbumsWithPhotos(photoNum);

        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-new.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        ListF<GeoInfo> infos = Cf.list(
                new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY),
                new GeoInfo(CITY_MICRODISTRICT_REGION_ID, CITY_MICRODISTRICT_NAME, RegionType.CITY_MICRODISTRICT)
        );
        infos.forEach(i -> geoAlbumManager.addPhotoToAlbum(file, i));

        ListF<Album> geoAlbums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(2, geoAlbums);
        for (Album album : geoAlbums) {
            ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
            Assert.sizeIs(photoNum + 1, items);
        }

        djfsResourceDao.changeParent(UID, file.getId(),
                djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.TRASH)).get().getId());
        Assert.isFalse(filesystem.find(PRINCIPAL, path, Option.of(ReadPreference.primary())).isPresent());

        GeoAlbumManager geoAlbumManagerStub = Mockito.spy(geoAlbumManager);
        Mockito.doReturn(infos).when(geoAlbumManagerStub).getResourceInfos(
                Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(infos).when(geoAlbumManagerStub).getResourceGeoIds(Mockito.any(), Mockito.any());
        MapF<RegionType, Integer> allResourceGeoIds = Cf.map(
                RegionType.CITY, infos.first().regionId,
                RegionType.CITY_MICRODISTRICT, infos.last().regionId
        );
        Mockito.doReturn(allResourceGeoIds).when(geoAlbumManagerStub).getAllResourceGeoIds(Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getGenerationStrategies(Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getRemoveStrategies();

        geoAlbumManagerStub.postProcessFiles(Cf.list(file.getResourceId().get()));

        for (Album album : geoAlbums) {
            ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
            Assert.sizeIs(photoNum, items);
            Assert.isFalse(items.map(AlbumItem::getObjectId).containsTs(file.getResourceId().get().getFileId().getValue()));
        }
    }

    @Test
    public void restoreToSeveralAlbumsAfterTrashRestore() {
        int photoNum = 5;
        createCityAndMicroDistrictAlbumsWithPhotos(photoNum);

        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/trash/image-new.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
        ListF<GeoInfo> infos = Cf.list(
                new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY),
                new GeoInfo(CITY_MICRODISTRICT_REGION_ID, CITY_MICRODISTRICT_NAME, RegionType.CITY_MICRODISTRICT)
        );

        djfsResourceDao.changeParent(UID, file.getId(),
                djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK)).get().getId());
        Assert.isFalse(filesystem.find(PRINCIPAL, path, Option.of(ReadPreference.primary())).isPresent());

        GeoAlbumManager geoAlbumManagerStub = Mockito.spy(geoAlbumManager);
        Mockito.doReturn(infos).when(geoAlbumManagerStub).getResourceInfos(
                Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(infos).when(geoAlbumManagerStub).getResourceGeoIds(Mockito.any(), Mockito.any());
        MapF<RegionType, Integer> allResourceGeoIds = Cf.map(
                RegionType.CITY, infos.first().regionId,
                RegionType.CITY_MICRODISTRICT, infos.last().regionId
        );
        Mockito.doReturn(allResourceGeoIds).when(geoAlbumManagerStub).getAllResourceGeoIds(Mockito.any(), Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getGenerationStrategies(Mockito.any());
        Mockito.doReturn(Cf.list()).when(geoAlbumManagerStub).getRemoveStrategies();

        geoAlbumManagerStub.postProcessFiles(Cf.list(file.getResourceId().get()));

        ListF<Album> geoAlbums = albumDao.getAlbums(UID, AlbumType.GEO);
        for (Album album : geoAlbums) {
            ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
            Assert.sizeIs(photoNum + 1, items);
            Assert.isTrue(items.map(AlbumItem::getObjectId).containsTs(file.getResourceId().get().getFileId().getValue()));
        }
    }

    @Test
    public void newAlbumDateModifiedEqualsPhotoEtime() {
        initAlbumsRevision();
        Instant resourceEtime = Instant.now().minus(Duration.standardDays(100500));
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-new.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                x -> x.coordinates(POINT_COORDINATES).exifTime(resourceEtime));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);
        geoAlbumManager.addPhotoToAlbum(file, info);
        ListF<Album> geoAlbums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(1, geoAlbums);
        Assert.some(resourceEtime, geoAlbums.first().getDateModified());
    }

    @Test
    public void newAlbumDateModifiedWithPhotoEtimeInFuture() {
        initAlbumsRevision();
        int photoNum = 5;
        createGeoAlbumWithPhotos(photoNum);
        Instant now = Instant.now();

        Instant resourceEtime = Instant.now().plus(Duration.standardDays(100500));
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-new.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                x -> x.coordinates(POINT_COORDINATES).exifTime(resourceEtime));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file, info);

        ListF<Album> geoAlbums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(1, geoAlbums);

        Assert.some(geoAlbums.first().getDateModified());
        Assert.ge(geoAlbums.first().getDateModified().get(), now);
        Assert.le(geoAlbums.first().getDateModified().get(), Instant.now());
    }

    @Test
    public void doNotAddPhotoExcludedFromGeo() {
        initAlbumsRevision();
        int photoNum = 5;
        createGeoAlbumWithPhotos(photoNum);

        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-new.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()).albumsExclusion("geo"));
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        geoAlbumManager.addPhotoToAlbum(file, info);

        ListF<Album> geoAlbums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(1, geoAlbums);
        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(geoAlbums.first().getId()), 100);

        Assert.sizeIs(photoNum, items);
        Assert.isFalse(items.map(AlbumItem::getObjectId).containsTs(file.getFileId().get().getValue()));
    }

    @Test
    public void clearPhotosFromFederalSubjectAlbum() {
        initAlbumsRevision();

        geobase.addRegion(FEDERAL_SUBJECT_REGION_ID, RegionType.FEDERAL_SUBJECT, Cf.list());
        geobase.addRegion(CITY_REGION_ID, RegionType.CITY, Cf.list(FEDERAL_SUBJECT_REGION_ID));

        int visibilityThreshold = 30;
        for (int i = 0; i < visibilityThreshold - 1; i++) {
            DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-1-" + i + ".jpg");
            FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                    x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));

            GeoInfo cityInfo = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);
            geoAlbumManager.addPhotoToAlbum(file, cityInfo);

            GeoInfo federalSubjectInfo = new GeoInfo(FEDERAL_SUBJECT_REGION_ID, FEDERAL_SUBJECT_NAME,
                    RegionType.FEDERAL_SUBJECT);
            geoAlbumManager.addPhotoToAlbum(file, federalSubjectInfo);
        }

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(2, albums);
        Assert.forAll(albums, Album::isHidden);
        for (Album album : albums) {
            ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100500);
            Assert.sizeIs(visibilityThreshold - 1, items);
        }

        {
            DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/image-2.jpg");
            FileDjfsResource file = filesystem.createFile(PRINCIPAL, path,
                    x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));

            GeoInfo federalSubjectInfo = new GeoInfo(FEDERAL_SUBJECT_REGION_ID, FEDERAL_SUBJECT_NAME,
                    RegionType.FEDERAL_SUBJECT);
            geoAlbumManager.addPhotoToAlbum(file, federalSubjectInfo);

            GeoInfo cityInfo = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);
            geoAlbumManager.addPhotoToAlbum(file, cityInfo);
        }

        albums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.sizeIs(2, albums);

        Album federalAlbum = albums.first().getGeoId().get() == FEDERAL_SUBJECT_REGION_ID ?
                                    albums.first() : albums.last();
        Album cityAlbum = albums.first().getGeoId().get() == CITY_REGION_ID ? albums.first() : albums.last();

        ListF<AlbumItem> cityAlbumItems = albumItemDao.getAllAlbumItems(UID, Cf.list(cityAlbum.getId()), 100500);
        Assert.sizeIs(visibilityThreshold, cityAlbumItems);
        Assert.isFalse(cityAlbum.isHidden());

        ListF<AlbumItem> federalAlbumItems = albumItemDao.getAllAlbumItems(UID, Cf.list(federalAlbum.getId()), 100500);
        Assert.sizeIs(0, federalAlbumItems);
        Assert.isTrue(federalAlbum.isHidden());
    }

    @Test
    @SneakyThrows
    public void testSimultaneousPhotoRestoreToSeveralAlbums() {
        initAlbumsRevision();

        geobase.addRegion(FEDERAL_SUBJECT_REGION_ID, RegionType.FEDERAL_SUBJECT, Cf.list());
        geobase.addRegion(CITY_REGION_ID, RegionType.CITY, Cf.list(FEDERAL_SUBJECT_REGION_ID));
        geobase.setRegionForCoordinates(POINT_COORDINATES, 1, CITY_REGION_ID);
        geobase.setRegionForCoordinates(OTHER_POINT_COORDINATES, 1, FEDERAL_SUBJECT_REGION_ID);

        final int cityPhotosNum = 80;
        final int areaPhotosNum = 12;
        ListF<DjfsFileId> fileIds = Cf.arrayList();

        for (int i = 0; i < cityPhotosNum; i++) {
            FileDjfsResource file = filesystem.createFile(
                    PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image-" + i + ".jpg"),
                    f -> f.exifTime(Instant.now()).coordinates(POINT_COORDINATES)
            );
            fileIds.add(file.getFileId().get());
        }
        for (int i = 0; i < areaPhotosNum; i++) {
            FileDjfsResource file = filesystem.createFile(
                    PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/area_image-" + i + ".jpg"),
                    f -> f.exifTime(Instant.now()).coordinates(OTHER_POINT_COORDINATES)
            );
            fileIds.add(file.getFileId().get());
        }
        fileIds = fileIds.shuffle();

        ExecutorService executorService = Executors.newFixedThreadPool(fileIds.size());
        ListF<Future<Boolean>> futures = Cf.arrayList();
        for (DjfsFileId fileId : fileIds) {
            futures.add(executorService.submit(
                    () -> {
                        SynchronizedHandlerHolder.set(true);
                        geoAlbumManager.postProcessFiles(Cf.list(DjfsResourceId.cons(UID, fileId)));
                        return true;
                    }
            ));
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        for (Future<Boolean> f : futures) {
            Assert.isTrue(f.get());
        }

        ListF<ExtendedAlbum> albums = albumDao.getExtendedAlbums(UID, Cf.list(AlbumType.GEO));
        Assert.sizeIs(2, albums);

        ExtendedAlbum federalAlbum = albums.first().album.getGeoId().get() == FEDERAL_SUBJECT_REGION_ID ?
                             albums.first() : albums.last();
        ExtendedAlbum cityAlbum = albums.first().album.getGeoId().get() == CITY_REGION_ID ?
                             albums.first() : albums.last();

        Assert.equals(areaPhotosNum, federalAlbum.itemsCount);
        Assert.equals(cityPhotosNum, cityAlbum.itemsCount);
    }

    @Test
    @SneakyThrows
    public void testCorrectExclusionFromFederalSubjectAlbum() {
        initAlbumsRevision();

        geobase.addRegion(FEDERAL_SUBJECT_REGION_ID, RegionType.FEDERAL_SUBJECT, Cf.list());
        geobase.addRegion(CITY_REGION_ID, RegionType.CITY, Cf.list(FEDERAL_SUBJECT_REGION_ID));
        geobase.setRegionForCoordinates(POINT_COORDINATES, 1, CITY_REGION_ID);
        geobase.setRegionForCoordinates(OTHER_POINT_COORDINATES, 1, FEDERAL_SUBJECT_REGION_ID);

        final int photosNum = 100;
        ListF<DjfsFileId> fileIds = Cf.arrayList();
        int cityPhotosNum = 0;

        for (int i = 0; i < photosNum; i++) {
            boolean isInCityPhoto = ThreadLocalRandom.current().nextBoolean();
            if (isInCityPhoto) {
                cityPhotosNum += 1;
            }

            FileDjfsResource file = filesystem.createFile(
                    PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/image-" + i + ".jpg"),
                    f -> f.exifTime(Instant.now())
                            .coordinates(isInCityPhoto ? POINT_COORDINATES : OTHER_POINT_COORDINATES)
            );
            fileIds.add(file.getFileId().get());
        }

        for (DjfsFileId fileId : fileIds) {
            geoAlbumManager.postProcessFiles(Cf.list(DjfsResourceId.cons(UID, fileId)));
        }

        ListF<ExtendedAlbum> albums = albumDao.getExtendedAlbums(UID, Cf.list(AlbumType.GEO));
        Assert.sizeIs(2, albums);

        ExtendedAlbum federalAlbum = albums.first().album.getGeoId().get() == FEDERAL_SUBJECT_REGION_ID ?
                                     albums.first() : albums.last();
        ExtendedAlbum cityAlbum = albums.first().album.getGeoId().get() == CITY_REGION_ID ?
                                  albums.first() : albums.last();

        Assert.equals(photosNum - cityPhotosNum, federalAlbum.itemsCount);
        Assert.equals(cityPhotosNum, cityAlbum.itemsCount);
    }

    @Test
    public void testSetAlbumHiddenIfItBecomesEmpty() {
        initAlbumsRevision();

        Album album = createAlbum(UID, CITY_NAME,
                x -> x.type(AlbumType.GEO).geoId(Option.of((long) CITY_REGION_ID)));
        albumDao.insert(album);
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        int photosNum = 40;
        ListF<FileDjfsResource> files = Cf.arrayList();
        for (int i = 0; i < photosNum; i++) {
            FileDjfsResource file = filesystem.createFile(PRINCIPAL,
                    DjfsResourcePath.cons(UID, "/disk/image" + i + ".jpg"),
                    x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
            geoAlbumManager.addPhotoToAlbum(file, info);
            files.add(file);
        }
        album = albumDao.findAlbums(UID, Cf.list(album.getId())).first();
        Assert.isFalse(album.isHidden());

        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(photosNum, items);

        for (int i = 0; i < photosNum - 1; i++) {
            geoAlbumManager.removeFromGeoAlbum(files.get(i), info.regionId);
        }
        album = albumDao.findAlbums(UID, Cf.list(album.getId())).first();
        Assert.isFalse(album.isHidden());

        albumDeltaDao.deleteAll(UID);

        geoAlbumManager.removeFromGeoAlbum(files.last(), info.regionId);

        items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(0, items);

        album = albumDao.findAlbums(UID, Cf.list(album.getId())).first();
        Assert.isTrue(album.isHidden());

        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        ListF<JsonObject> updateDeltas = getDeltasByType(allDeltas, "update");
        ListF<JsonObject> isVisibleDeltas = updateDeltas.map(x -> getField(x, "is_visible")).filter(
                Option::isPresent).map(Option::get);
        Assert.sizeIs(1, isVisibleDeltas);

        Option<JsonBoolean> isVisibleValue = isVisibleDeltas.first().getByPathO("value", "boolean")
                .filterByType(JsonBoolean.class);
        Assert.some(JsonBoolean.FALSE, isVisibleValue);
    }

    @Test
    public void testVisibilityDeltaOnAlreadyVisibleAlbum() {
        initAlbumsRevision();

        Album album = createAlbum(UID, CITY_NAME,
                x -> x.type(AlbumType.GEO).geoId(Option.of((long) CITY_REGION_ID)).hidden(false));
        albumDao.insert(album);
        GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

        int photosNum = 40;
        ListF<FileDjfsResource> files = Cf.arrayList();
        for (int i = 0; i < photosNum; i++) {
            FileDjfsResource file = filesystem.createFile(PRINCIPAL,
                    DjfsResourcePath.cons(UID, "/disk/image" + i + ".jpg"),
                    x -> x.coordinates(POINT_COORDINATES).exifTime(Instant.now()));
            geoAlbumManager.addPhotoToAlbum(file, info);
            files.add(file);
        }
        album = albumDao.findAlbums(UID, Cf.list(album.getId())).first();
        Assert.isFalse(album.isHidden());

        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        ListF<JsonObject> updateDeltas = getDeltasByType(allDeltas, "update");
        ListF<JsonObject> isVisibleDeltas = updateDeltas.map(x -> getField(x, "is_visible")).filter(
                Option::isPresent).map(Option::get);
        Assert.sizeIs(0, isVisibleDeltas);
    }

    private void initAlbumsRevision() {
        albumDeltaDao.tryInitializeCurrentRevision(UID);
        diskInfoManager.disableGeoAlbumsGenerationInProgress(UID);
    }
}
