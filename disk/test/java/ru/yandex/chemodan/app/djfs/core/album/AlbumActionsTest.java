package ru.yandex.chemodan.app.djfs.core.album;

import org.bson.types.ObjectId;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple3;
import ru.yandex.chemodan.app.djfs.core.album.pojo.AlbumDeltaItemsPojo;
import ru.yandex.chemodan.app.djfs.core.album.pojo.AlbumDeltasPojo;
import ru.yandex.chemodan.app.djfs.core.album.pojo.AlbumSnapshotPojo;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.share.Group;
import ru.yandex.chemodan.app.djfs.core.share.GroupLink;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.inside.geobase.RegionType;
import ru.yandex.misc.bender.BenderMapper;
import ru.yandex.misc.bender.MembersToBind;
import ru.yandex.misc.bender.config.BenderConfiguration;
import ru.yandex.misc.bender.config.CustomMarshallerUnmarshallerFactoryBuilder;
import ru.yandex.misc.lang.CharsetUtils;
import ru.yandex.misc.test.Assert;

public class AlbumActionsTest extends DjfsAlbumsTestBase {
    @Autowired
    protected AlbumActions albumActions;

    @Test
    public void getSnapshot() {
        Album album = createGeoAlbum(UID, "My lovely album");
        albumDao.insert(album);

        albumDeltaDao.tryInitializeCurrentRevision(UID);

        AlbumSnapshotPojo snapshot = albumActions.snapshot(UID.asString(), Cf.list(AlbumType.GEO), Cf.list("index"));
        Assert.equals(1, snapshot.recordsCount);
    }

    @Test
    public void getMultiTypeSnapshot() {
        Album geoAlbum = createGeoAlbum(UID, "My lovely geo album");
        Album personalAlbum = createAlbum(UID, "My lovely personal album");

        albumDao.insert(geoAlbum);
        albumDao.insert(personalAlbum);

        albumDeltaDao.tryInitializeCurrentRevision(UID);

        AlbumSnapshotPojo snapshot = albumActions.snapshot(UID.asString(),
                Cf.list(AlbumType.GEO, AlbumType.PERSONAL), Cf.list("index"));

        Assert.equals(2, snapshot.recordsCount);
    }

    @Test
    public void getSnapshotForEmptyAlbum() {
        Album album = createGeoAlbum(UID, "My lovely album");
        albumDao.insert(album);

        albumDeltaDao.tryInitializeCurrentRevision(UID);

        AlbumSnapshotPojo snapshot = albumActions.snapshot(UID.asString(), Cf.list(AlbumType.GEO),
                Cf.list(album.getId().toHexString()));
        Assert.equals(0, snapshot.recordsCount);
    }

    @Test(expected = AlbumsNotFoundException.class)
    public void getSnapshotForUserWithoutGeoAlbums() {
        albumActions.snapshot(UID.asString(), Cf.list(AlbumType.GEO), Cf.list("index"));
    }


    @Test
    public void rawDeltaSerializeCheck() {
        Album album = createGeoAlbum(UID, "My lovely album");
        AlbumItem albumItem1 = createItem(album);
        AlbumItem albumItem2 = createItem(album);
        albumDao.insert(album);
        albumItemDao.insert(albumItem1);
        albumItemDao.insert(albumItem2);

        long curRevision = 100500;
        AlbumDelta delta1 = AlbumUtils.createInsertAlbumItemDelta(album, albumItem1, curRevision);
        albumDeltaDao.insert(delta1);

        long prevRevision = curRevision - 1;
        AlbumDelta delta2 = AlbumUtils.createInsertAlbumItemDelta(album, albumItem2, prevRevision);
        albumDeltaDao.insert(delta2);

        albumDao.updateAlbumRevision(album, curRevision);
        albumDeltaDao.tryInitializeCurrentRevision(UID);
        albumDeltaDao.updateCurrentRevision(UID, curRevision);

        AlbumDeltasPojo deltas = albumActions.deltas(UID.asString(), prevRevision, Option.empty(), Cf.list(album.getType()));

        BenderMapper mapper = new BenderMapper(BenderConfiguration.cons(
                MembersToBind.WITH_ANNOTATIONS, false,
                CustomMarshallerUnmarshallerFactoryBuilder.cons()
                        .add(AlbumItemFieldValue.class, AlbumItemFieldValueJsonSerializers.consMarshaller())
                        .add(RawJsonValue.class, RawJsonValueSerializer.consMarshaller())
                        .build()
        ));

        String serializedResult = CharsetUtils.decodeUtf8(mapper.serializeJson(deltas));
        JsonObject parsedResult = JsonObject.parseObject(serializedResult);

        JsonArray items = (JsonArray) parsedResult.get("items");
        Assert.sizeIs(1, items.getArray());

        JsonObject item = (JsonObject) items.getArray().get(0);
        JsonArray changes = (JsonArray)item.get("changes");
        Assert.sizeIs(1, changes.getArray());

        JsonObject change = (JsonObject) changes.getArray().get(0);
        JsonString changeType = (JsonString) change.get("change_type");
        Assert.equals("insert", changeType.getString());
    }

    @Test(expected = RevisionNotFoundException.class)
    public void deltaWithRequestedRevisionNotFound() {
        long currentRevision = 100500;

        Album album = createAlbum(UID, "My lovely album", x -> x.type(AlbumType.GEO).revision(Option.of(currentRevision)));
        AlbumItem albumItem = createItem(album);
        albumDao.insert(album);
        albumItemDao.insert(albumItem);

        AlbumDelta delta = createInsertDelta(album, albumItem, currentRevision);
        albumDeltaDao.insert(delta);

        albumDeltaDao.tryInitializeCurrentRevision(UID);
        albumDeltaDao.updateCurrentRevision(UID, currentRevision);

        long notExistingRevision = 0;
        albumActions.deltas(UID.asString(), notExistingRevision, Option.empty(), Cf.list(album.getType()));
    }

    @Test
    public void deltasWithCurrentRevisionAndDeletedDeltas() {
        long currentRevision = 100500;

        createAlbum(UID, "My lovely album", x -> x.type(AlbumType.GEO).revision(Option.of(currentRevision)));
        albumDeltaDao.tryInitializeCurrentRevision(UID);
        albumDeltaDao.updateCurrentRevision(UID, currentRevision);

        AlbumDeltasPojo deltas = albumActions.deltas(UID.asString(), currentRevision, Option.empty(), Option.empty());
        Assert.sizeIs(0, deltas.items);
    }

    @Test(expected = AlbumsNotFoundException.class)
    public void deltasForUserWithoutAlbums() {
        albumActions.deltas(UID.asString(), 0, Option.empty(), Cf.list());
    }

    @Test
    public void personalAlbumsSnapshot() {
        Album album = createAlbum(UID, "Personal 1", x -> x.type(AlbumType.PERSONAL));
        albumDao.insert(album);

        AlbumSnapshotPojo snapshot = albumActions.snapshot(
                UID.asString(), Cf.list(AlbumType.PERSONAL), Cf.list("index"));
        Assert.equals(1, snapshot.recordsCount);
        Assert.equals(0L, snapshot.revision);
    }

    @Test
    public void deltasRevisionCheck() {
        albumDeltaDao.tryInitializeCurrentRevision(UID);
        Album album = createAlbum(UID, "Personal 1", x -> x.type(AlbumType.PERSONAL));
        albumDao.insert(album);

        for (int i = 0; i < 100; i++) {
            DjfsResourcePath imagePath = DjfsResourcePath.cons(UID, "/disk/image-" + i + ".jpg");
            DjfsFileId fileId = DjfsFileId.random();
            FileDjfsResource file = filesystem.createFile(PRINCIPAL, imagePath,
                    x -> x.fileId(fileId).coordinates(POINT_COORDINATES).exifTime(Instant.now()));
            GeoInfo info = new GeoInfo(CITY_REGION_ID, CITY_NAME, RegionType.CITY);

            geoAlbumManager.addPhotoToAlbum(file, info);
        }

        long baseRevision = 1;

        AlbumDeltasPojo geoDeltas = albumActions.deltas(UID.asString(), baseRevision, Option.empty(), Cf.list(AlbumType.GEO));
        AlbumDeltasPojo personalDeltas = albumActions.deltas(UID.asString(), baseRevision, Option.empty(), Cf.list(AlbumType.PERSONAL));

        ListF<Tuple3<Long, Long, RawJsonValue>> deltaGeoRevisions = geoDeltas.items.map(x -> Tuple3.tuple(x.baseRevision, x.revision, x.changes));
        ListF<Tuple3<Long, Long, RawJsonValue>> deltaPersonalRevisions = personalDeltas.items.map(x -> Tuple3.tuple(x.baseRevision, x.revision, x.changes));
        assert deltaGeoRevisions.length() > 0;
        assert deltaGeoRevisions.length() == deltaPersonalRevisions.length();
        for (int i = 0; i < deltaGeoRevisions.length(); i++) {
            Assert.equals(deltaGeoRevisions.get(i)._2, deltaGeoRevisions.get(i)._1 + 1);
            Assert.equals(deltaPersonalRevisions.get(i)._2, deltaPersonalRevisions.get(i)._1 + 1);
            if (i > 0) {
                Assert.equals(deltaGeoRevisions.get(i)._2, deltaGeoRevisions.get(i - 1)._2 + 1);
                Assert.equals(deltaPersonalRevisions.get(i)._2, deltaPersonalRevisions.get(i - 1)._2 + 1);
            } else {
                Assert.equals(deltaGeoRevisions.get(i)._2, baseRevision + 1);
                Assert.equals(deltaPersonalRevisions.get(i)._2, baseRevision + 1);
            }

//            If album delta type don't correspond required type changes must be empty list
            Assert.notEquals(deltaGeoRevisions.get(i)._3, deltaPersonalRevisions.get(i)._3);
            assert (deltaGeoRevisions.get(i)._3.equals(RawJsonValue.emptyRawValueFromString()) ||
                    deltaPersonalRevisions.get(i)._3.equals(RawJsonValue.emptyRawValueFromString()));
        }
    }

    @Test
    public void getMuliDeltas() {
        Album geoAlbum = createGeoAlbum(UID, "My lovely geo album");
        Album personalAlbum = createAlbum(UID, "My lovely personal album");

        albumDao.insert(geoAlbum);
        albumDao.insert(personalAlbum);

        AlbumItem geoAlbumItem = createItem(geoAlbum);
        AlbumItem personalAlbumItem = createItem(personalAlbum);

        albumDeltaDao.tryInitializeCurrentRevision(UID);

        long baseRevision = 0;
        AlbumDelta delta1 = AlbumUtils.createInsertAlbumItemDelta(geoAlbum, geoAlbumItem, baseRevision + 1);
        albumDeltaDao.insert(delta1);
        AlbumDelta delta2 = AlbumUtils.createInsertAlbumItemDelta(personalAlbum, personalAlbumItem, baseRevision + 2);
        albumDeltaDao.insert(delta2);

        albumDeltaDao.updateCurrentRevision(UID, 2);

        SetF<AlbumDeltaItemsPojo> allDeltas = getUniqueNotEmptyChanges(
                albumActions.deltas(UID.asString(), baseRevision, Option.empty(),
                        Cf.list(AlbumType.GEO, AlbumType.PERSONAL)));

        SetF<AlbumDeltaItemsPojo> geoDeltas = getUniqueNotEmptyChanges(
                albumActions.deltas(UID.asString(), baseRevision, Option.empty(),
                        Cf.list(AlbumType.GEO)));
        SetF<AlbumDeltaItemsPojo> personalDeltas = getUniqueNotEmptyChanges(
                albumActions.deltas(UID.asString(), baseRevision, Option.empty(),
                        Cf.list(AlbumType.PERSONAL)));

        Assert.hasSize(2, allDeltas);
        Assert.hasSize(1, geoDeltas);
        Assert.hasSize(1, personalDeltas);

        Assert.equals(allDeltas, geoDeltas.plus(personalDeltas));
    }

    private SetF<AlbumDeltaItemsPojo> getUniqueNotEmptyChanges(AlbumDeltasPojo deltas) {
        RawJsonValue emptyChanges = RawJsonValue.emptyRawValueFromString();
        return deltas.items.filterNot(item -> item.changes.equals(emptyChanges)).unique();
    }
    @Test
    public void testSnapshotForPersonalAlbumWithFilesFromSharedFolder() {
        DjfsUid ANOTHER_UID = DjfsUid.cons(223322223);
        DjfsPrincipal ANOTHER_PRINCIPAL = DjfsPrincipal.cons(ANOTHER_UID);
        initializePgUser(ANOTHER_UID, 1);

        DjfsResourcePath ownerPath = DjfsResourcePath.cons(UID, "/disk/owner_folder");
        DjfsResourcePath participantPath = DjfsResourcePath.cons(ANOTHER_UID, "/disk/folder/participant_folder");
        Tuple2<Group, GroupLink> groupWithLink =
                util.share.create(ownerPath, participantPath, SharePermissions.READ_WRITE);

        Album album = createAlbum(ANOTHER_UID, "My personal album", a -> a.type(AlbumType.PERSONAL));
        albumDao.insert(album);

        int numSharedFiles = 13;
        ListF<DjfsFileId> sharedFileIds = Cf.arrayList();
        for (int i = 0; i < numSharedFiles; i++) {
            FileDjfsResource file = filesystem.createFile(PRINCIPAL, ownerPath.getChildPath("image-" + i + ".jpg"));
            sharedFileIds.add(file.getFileId().get());

            AlbumItem item = createItem(album,
                    a -> a.groupId(Option.of(groupWithLink._1.getId())).objectType(AlbumItemType.SHARED_RESOURCE)
                    .objectId(file.getFileId().get().getValue())
            );
            albumItemDao.insert(item);
        }

        int numPersonalFiles = 42;
        ListF<DjfsFileId> personalFileIds = Cf.arrayList();
        for (int i = 0; i < numPersonalFiles; i++) {
            FileDjfsResource file = filesystem.createFile(ANOTHER_PRINCIPAL,
                    DjfsResourcePath.cons(ANOTHER_UID, "/disk/image-" + i + ".jpg"));
            personalFileIds.add(file.getFileId().get());

            AlbumItem item = createItem(album, a -> a.objectId(file.getFileId().get().getValue()));
            albumItemDao.insert(item);
        }

        AlbumSnapshotPojo snapshot = albumActions
                .snapshot(ANOTHER_UID.asString(), Cf.list(AlbumType.PERSONAL), Cf.list(album.getId().toHexString()));
        Assert.equals(numPersonalFiles + numSharedFiles, snapshot.recordsCount);
        Assert.sizeIs(numPersonalFiles + numSharedFiles, snapshot.records.items);
        ListF<DjfsResourceId> objResourceIds = snapshot.records.items.flatMap(i -> i.fields
                        .filter(f -> f.fieldId.equals("obj_resource_id"))
                        .map(f -> DjfsResourceId.cons((String)f.value.value)));

        SetF<DjfsUid> personalFilesOwner = objResourceIds
                .filter(o -> personalFileIds.containsTs(o.getFileId()))
                .map(DjfsResourceId::getUid)
                .unique();
        Assert.sizeIs(1, personalFilesOwner);
        Assert.equals(ANOTHER_UID, personalFilesOwner.single());

        SetF<DjfsUid> sharedFilesOwner = objResourceIds
                .filter(o -> sharedFileIds.containsTs(o.getFileId()))
                .map(DjfsResourceId::getUid)
                .unique();
        Assert.sizeIs(1, sharedFilesOwner);
        Assert.equals(UID, sharedFilesOwner.single());
    }

    @Test
    public void testYaPhotoAlbumsFiltrationFromSnapshot() {
        Album album1 = createAlbum(UID, "personal album 1", a -> a.type(AlbumType.PERSONAL));
        albumDao.insert(album1);
        for (int i = 0; i < 5; i++) {
            AlbumItem item = createItem(album1);
            albumItemDao.insert(item);
        }

        Album album2 = createAlbum(UID, "personal album 2", a -> a.type(AlbumType.PERSONAL));
        albumDao.insert(album2);
        for (int i = 0; i < 5; i++) {
            AlbumItem item = createItem(album2);
            albumItemDao.insert(item);
        }

        Album album3 = createAlbum(UID, "ya photo imported album", a -> a.type(AlbumType.PERSONAL)
                .fotkiAlbumId(Option.of(100500L)));
        albumDao.insert(album3);
        for (int i = 0; i < 5; i++) {
            AlbumItem item = createItem(album3);
            albumItemDao.insert(item);
        }

        AlbumSnapshotPojo snapshot = albumActions.snapshot(UID.asString(), Cf.list(AlbumType.PERSONAL),
                Cf.list("index"));

        Assert.equals(2, snapshot.recordsCount);
        Assert.sizeIs(2, snapshot.records.items);
        Assert.equals(Cf.set(album1, album2).map(Album::getId).map(ObjectId::toHexString).unique(),
                snapshot.records.items.map(i -> i.recordId).unique());
        Assert.equals(0L, snapshot.revision);
    }

    @Test
    public void fetchSnapshotRevisionAfterRevisionUpWithoutDeltas() {
        albumDeltaDao.tryInitializeCurrentRevision(UID);

        Album album = createAlbum(UID, "Moscow", a -> a.type(AlbumType.GEO).revision(Option.of(1L)));
        albumDao.insert(album);

        long updatedRevision = 100500;
        albumDeltaDao.updateCurrentRevision(UID, updatedRevision);

        AlbumSnapshotPojo snapshot = albumActions.snapshot(UID.asString(), Cf.list(AlbumType.GEO), Cf.list("index"));
        Assert.equals(updatedRevision, snapshot.revision);

        AlbumDeltasPojo deltas = albumActions.deltas(UID.asString(), updatedRevision, Option.empty(), Cf.list(album.getType()));
        Assert.sizeIs(0, deltas.items);
    }
}
