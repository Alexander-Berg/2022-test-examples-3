package ru.yandex.chemodan.app.djfs.albums;

import java.util.UUID;

import org.bson.types.ObjectId;
import org.joda.time.Instant;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.djfs.core.album.Album;
import ru.yandex.chemodan.app.djfs.core.album.AlbumDelta;
import ru.yandex.chemodan.app.djfs.core.album.AlbumDeltaChange;
import ru.yandex.chemodan.app.djfs.core.album.AlbumDeltaChangeType;
import ru.yandex.chemodan.app.djfs.core.album.AlbumDeltaRaw;
import ru.yandex.chemodan.app.djfs.core.album.AlbumFaceClustersDao;
import ru.yandex.chemodan.app.djfs.core.album.AlbumItem;
import ru.yandex.chemodan.app.djfs.core.album.AlbumItemFaceCoordinates;
import ru.yandex.chemodan.app.djfs.core.album.AlbumMergeManager;
import ru.yandex.chemodan.app.djfs.core.album.AlbumType;
import ru.yandex.chemodan.app.djfs.core.album.AlbumTypeMismatchException;
import ru.yandex.chemodan.app.djfs.core.album.AlbumsNotFoundException;
import ru.yandex.chemodan.app.djfs.core.album.CollectionId;
import ru.yandex.chemodan.app.djfs.core.album.DjfsAlbumsTestBase;
import ru.yandex.chemodan.app.djfs.core.album.FaceInfo;
import ru.yandex.chemodan.app.djfs.core.album.FacesAlbumManager;
import ru.yandex.chemodan.app.djfs.core.album.FeatureUnavailableInUserCountryException;
import ru.yandex.chemodan.app.djfs.core.album.operation.albummerge.AlbumMergeTask;
import ru.yandex.chemodan.app.djfs.core.client.FacesDelta;
import ru.yandex.chemodan.app.djfs.core.client.MockBlackbox2;
import ru.yandex.chemodan.app.djfs.core.db.EntityAlreadyExistsException;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.AntiVirusScanStatus;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.albumremove.AlbumRemoveCallbacks;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.albumsetattr.AlbumSetAttrCallbacks;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.core.operations.OperationDao;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.FacesIndexingState;
import ru.yandex.chemodan.queller.celery.job.CeleryJob;
import ru.yandex.chemodan.queller.worker.CeleryTaskManager;
import ru.yandex.commune.bazinga.BazingaStopExecuteException;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.serialize.JsonParser;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = {
        DjfsAlbumsTestContextConfiguration.class,
})
public class FacesAlbumManagerTest extends DjfsAlbumsTestBase {
    @Autowired
    private AlbumFaceClustersDao albumFaceClustersDao;
    @Autowired
    private FacesAlbumManager facesAlbumManager;
    @Autowired
    private AlbumMergeManager albumMergeManager;

    @Autowired
    private CeleryTaskManager celeryTaskManager;

    private FileDjfsResource makeResource(DjfsResource parent, String fileId, String path) {
        return FileDjfsResource.builder()
                .antiVirusScanStatus(AntiVirusScanStatus.CLEAN)
                .creationTime(Instant.now())
                .uploadTime(Instant.now())
                .digestStid("fake stid")
                .fileStid("fake stid")
                .hid("deadf00ddeadf00ddeadf00ddeadf00d")
                .md5("deadf00ddeadf00ddeadf00ddeadf00d")
                .modificationTime(Instant.now())
                .parentId(parent.getId())
                .sha256("deadf00d")
                .version(0)
                .fileId(DjfsFileId.cons(fileId))
                .id(UUID.randomUUID())
                .path(DjfsResourcePath.cons(parent.getUid(), path))
                .build();
    }

    @Override
    public void setUp() {
        super.setUp();

        final DjfsResource disk = djfsResourceDao.find(DjfsResourcePath.cons(UID, "/disk")).getOrThrow("No disk?");
        djfsResourceDao.insert(
                UID,
                makeResource(disk, "27ccb2a4bc8d274186fb8b006e6a0690745b5f7a788de909c53f204b928c7767", "/disk/foo.jpg"),
                makeResource(disk, "927daf4d8b10336d27bcf14d487ca781e1cbfe40f8517b6d7b07d36d43a45b71", "/disk/bar.jpg"),
                makeResource(disk, "e81455a5fe5879dbc8b10ef7cb7b56d56ab4d5e67f81e3fd2987721f74c86b84", "/disk/baz.jpg"),
                makeResource(disk, "12f673d26f83d3de8372e672e2e7ae2ee77e6077154a8b752773b40bad9f7428", "/disk/qux.jpg"),
                makeResource(disk, "d92fefe5cac46ffd345129d726d28c0ec030f65e121ec0caa3a21c4712c05ff0", "/disk/bat.jpg"),
                makeResource(disk, "30ba54e4948b0f7ad9a8abbe73502c5eda4de0466234a809452846e8a2bdfb15", "/disk/imp.jpg"),
                makeResource(disk, "cab43abb3ae65359313949b3a146c2f9129fb1c4a3c115eefe672a21890f3fa3", "/disk/pay.jpg"),
                makeResource(disk, "600472fbb70465775102ddd6ee53b99bf14d7bbfbd4c215536a13e2138a1a891", "/disk/xan.jpg")
        );

        final DjfsResource babeDisk = djfsResourceDao.find(DjfsResourcePath.cons(UID_BABE, "/disk")).getOrThrow("No disk?");
        djfsResourceDao.insert(
                UID_BABE,
                makeResource(babeDisk, "927daf4d8b10336d27bcf14d487ca781e1cbfe40f8517b6d7b07d36d43a45b71", "/disk/uxf.jpg")
        );
    }

    private Tuple2<Album, ListF<FileDjfsResource>> createFacesAlbumWithPhotos(DjfsPrincipal principal,
                                                                              DjfsResourcePath folder, int photosNum)
    {
        Album album = createAlbum(principal.getUid(), "test", a -> a.type(AlbumType.FACES));
        albumDao.insert(album);
        // TODO: На самом деле профанация, надо из фикстуры сделать

        ListF<FileDjfsResource> files = Cf.arrayList();
        for (int i = 0; i < photosNum; i++) {
            AlbumItem item = createItem(album);

            FileDjfsResource file = filesystem.createFile(
                    principal,
                    folder.getChildPath("image-" + i + ".jpg"),
                    f -> f.fileId(DjfsFileId.cons(item.getObjectId()))
            );
            albumItemDao.insert(item);

            files.add(file);
        }
        return Tuple2.tuple(album, files);
    }

    @Test
    public void initializeFromClusters() {
        // not initialized
        Assert.none(userDao.getFaceClustersVersion(UID));
        Assert.equals(FacesIndexingState.NOT_INDEXED, userDao.getFacesIndexingState(UID)._1);

        final Instant before = Instant.now();
        facesAlbumManager.initializeAlbumsFromClusters(UID);
        final Instant after = Instant.now();

        Assert.assertThrows(
                () -> facesAlbumManager.initializeAlbumsFromClusters(UID),
                EntityAlreadyExistsException.class,
                e -> {
                    Assert.equals(
                            "Face clusters already initialized, uid = 31337, known version = 1, new version = 1",
                            e.getMessage()
                    );
                    return true;
                }
        );

        // initialized
        Assert.some(1L, userDao.getFaceClustersVersion(UID));
        final Tuple2<FacesIndexingState, Instant> state = userDao.getFacesIndexingState(UID);
        Assert.equals(FacesIndexingState.REINDEXED, state._1);
        if (state._2.isAfter(after) || state._2.isBefore(before))
            throw new AssertionError(
                "Assertion failed: before == " + before + " <= " + state._2 + " <= " + after + " == after"
            );

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.FACES);
        Assert.sizeIs(2, albums);

        Album album = albums.sortedBy(Album::getId).get(1);
        Assert.equals(AlbumType.FACES, album.getType());
        Assert.equals(Cf.list("31337_1_1"), albumFaceClustersDao.getClusters(UID, album.getId()));
        Assert.isTrue(album.getCoverId().isPresent());

        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(3, items);
        Assert.some(items.get(2).getId(), album.getCoverId());

        AlbumItem item2 = items.get(2);
        Assert.isTrue(item2.getFaceInfo().isPresent());
        if (!item2.getFaceInfo().get().serializeJson().contains("\"id\": \"face_31337"))
            throw new AssertionError(item2.getFaceInfo().get().serializeJson());

        facesAlbumManager.deleteAlbums(UID);

        // removed
        Assert.none(userDao.getFaceClustersVersion(UID));

        ListF<Album> albumsAfterDelete = albumDao.getAlbums(UID, AlbumType.FACES);
        Assert.sizeIs(0, albumsAfterDelete);

        Assert.equals(Cf.list(), albumFaceClustersDao.getClusters(UID, album.getId()));
    }

    @Test
    public void removeAlbum() {
        int photosNum = 3;
        Tuple2<Album, ListF<FileDjfsResource>> facesAlbumWithPhotos = createFacesAlbumWithPhotos(
                PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), photosNum);
        Album album = facesAlbumWithPhotos._1;
        mockEventHistoryLogger.messageData.clear();
        facesAlbumManager.removeAlbum(UID, album, AlbumRemoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
        ListF<AlbumDeltaRaw> allDeltas =  albumDeltaDao.findRaw(UID);
        ListF<JsonObject> deleteDeltas = getDeltasByType(allDeltas, "delete");
        ListF<JsonObject> updateDeltas = getDeltasByType(allDeltas, "update");
        // Проверяем что вместо удаления скрываем альбом
        Assert.sizeIs(0, deleteDeltas);
        Option<Album> albumAfterDelete = albumDao.findAlbum(UID, album.getId());
        Assert.equals(albumAfterDelete.get().isHidden(), true);
        Assert.sizeIs(1, updateDeltas);
        Assert.sizeIs(1, mockEventHistoryLogger.messageData.filter(
                x -> x.getOrElse("event_type", "").equals("album-remove"))
        );
    }

    @Test
    public void mergeOperationSuccess() {
        facesAlbumManager.initializeAlbumsFromClusters(UID);
        Album srcAlbum = albumDao.getAlbums(UID, AlbumType.FACES).get(1);
        Album dstAlbum = createFacesAlbumWithPhotos(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), 2)._1;

        // check operation creation and task param serialization
        String operationId = albumMergeManager.mergeFaceAlbumsAsync(UID, srcAlbum.getId(), dstAlbum.getId()).getId();
        Assert.equals(Operation.State.WAITING, operationDao.find(UID, operationId).get().getState());

        AlbumMigrateTaskForTest task = new AlbumMigrateTaskForTest(albumMergeManager, operationDao);
        task.execute(UID, operationId, srcAlbum.getId(), dstAlbum.getId(), AlbumType.FACES);
        // check operation status after
        Assert.equals(Operation.State.COMPLETED, operationDao.find(UID, operationId).get().getState());
    }

    @Test(expected = AlbumsNotFoundException.class)
    public void missingAlbum() {
        albumMergeManager.mergeFaceAlbumsAsync(UID, new ObjectId(), new ObjectId());
    }

    @Test(expected = AlbumTypeMismatchException.class)
    public void wrongTypeAlbum () {
        Album facesAlbum = createAlbum(UID, "faces", a -> a.type(AlbumType.FACES));
        Album geoAlbum = createAlbum(UID, "geo", a -> a.type(AlbumType.GEO));
        albumDao.insert(facesAlbum);
        albumDao.insert(geoAlbum);
        albumMergeManager.mergeFaceAlbumsAsync(UID, facesAlbum.getId(), geoAlbum.getId());
    }

    @Test
    public void mergeOperationFailure() {
        facesAlbumManager.initializeAlbumsFromClusters(UID);
        Album srcAlbum = albumDao.getAlbums(UID, AlbumType.FACES).get(1);
        Album dstAlbum = createFacesAlbumWithPhotos(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), 2)._1;

        // check operation creation and task param serialization
        String operationId = albumMergeManager.mergeFaceAlbumsAsync(UID, srcAlbum.getId(), dstAlbum.getId()).getId();
        Assert.equals(Operation.State.WAITING, operationDao.find(UID, operationId).get().getState());

        // giving null for NPE while task execution
        AlbumMigrateTaskForTest task = new AlbumMigrateTaskForTest(null, operationDao);
        try {
            task.execute(UID, operationId, srcAlbum.getId(), dstAlbum.getId(), AlbumType.FACES);
        } catch (BazingaStopExecuteException ignore) {
        }
        // check operation status after
        Assert.equals(Operation.State.FAILED, operationDao.find(UID, operationId).get().getState());
    }

    @Test
    public void mergeAlbums() {
        facesAlbumManager.initializeAlbumsFromClusters(UID);
        Album srcAlbum = albumDao.getAlbums(UID, AlbumType.FACES).sortedBy(Album::getId).get(1);
        Album dstAlbum = createFacesAlbumWithPhotos(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), 2)._1;

        ListF<String> srcItemIds = getItemIds(srcAlbum.getId());
        Assert.hasSize(3, srcItemIds);
        ListF<String> dstItemIds = getItemIds(dstAlbum.getId());
        Assert.hasSize(2, dstItemIds);

        ListF<String> srcClusters = albumFaceClustersDao.getClusters(UID, srcAlbum.getId());
        ListF<String> dstClusters = albumFaceClustersDao.getClusters(UID, dstAlbum.getId());

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        facesAlbumManager.mergeAlbums(UID, srcAlbum.getId(), dstAlbum.getId());

        // assert deltas
        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        Assert.hasSize(1, allDeltas);
        Assert.equals(2L, allDeltas.single().getRevision());
        Assert.some(2L, albumDeltaDao.getCurrentRevisionWithoutLock(UID));

        ListF<AlbumDeltaChange> changes = AlbumDelta.B.getParser().parseListJson(allDeltas.single().changes);

        Assert.hasSize(9, changes);

        srcItemIds.zip(changes.subList(0, 3)).forEach((albumItemId, change) -> {
            Assert.equals(AlbumDeltaChangeType.INSERT, change.getChangeType());
            Assert.equals(dstAlbum.getId().toHexString(), change.getCollectionId());
            Assert.equals(albumItemId, change.getRecordId());
        });

        assertAlbumFieldSet(changes.get(3), dstAlbum, "items_count");

        srcItemIds.zip(changes.subList(4, 7)).forEach((albumItemId, change) -> {
            Assert.equals(AlbumDeltaChangeType.DELETE, change.getChangeType());
            Assert.equals(srcAlbum.getId().toHexString(), change.getCollectionId());
            Assert.equals(albumItemId, change.getRecordId());
        });

        AlbumDeltaChange deleteChange = changes.get(7);
        Assert.equals(AlbumDeltaChangeType.DELETE, deleteChange.getChangeType());
        Assert.equals(CollectionId.ALBUM_LIST_COLLECTION_ID, deleteChange.getCollectionId());
        Assert.equals(srcAlbum.getId().toHexString(), deleteChange.getRecordId());

        assertAlbumFieldSet(changes.get(8), dstAlbum, "modified");

        // assert album states
        Assert.isEmpty(albumDao.findAlbum(UID, srcAlbum.getId()));
        Album actualDstAlbum = albumDao.findAlbum(UID, dstAlbum.getId()).get();
        Assert.some(2L, actualDstAlbum.getRevision());
        Assert.isTrue(actualDstAlbum.getDateModified().get().isAfter(dstAlbum.getDateModified().get()));

        // assert album items states
        Assert.hasSize(0, getItemIds(srcAlbum.getId()));
        SetF<String> expectedDstItemIds = srcItemIds.plus(dstItemIds).unique();
        SetF<String> actualDstItemIds = getItemIds(dstAlbum.getId()).unique();
        Assert.equals(expectedDstItemIds, actualDstItemIds);

        // clusters
        SetF<String> expectedDstClusters = srcClusters.plus(dstClusters).unique();
        SetF<String> actualDstClusters = albumFaceClustersDao.getClusters(UID, dstAlbum.getId()).unique();
        Assert.equals(expectedDstClusters, actualDstClusters);

        // assert nothing changes on retry
        facesAlbumManager.mergeAlbums(UID, srcAlbum.getId(), dstAlbum.getId());
        Assert.hasSize(1, albumDeltaDao.findRaw(UID));
        Assert.some(2L, albumDao.findAlbum(UID, dstAlbum.getId()).filterMap(Album::getRevision));

        // assert pushes
        ListF<CeleryJob> celeryJobs = Cf.x(jobCaptor.getAllValues());
        Assert.hasSize(2, celeryJobs);

        // assert revision changed push sent
        Assert.equals(JsonString.valueOf("album_deltas_updated"), celeryJobs.get(0).getKwargs().getTs("class"));
        Assert.equals("{ \"t\": \"album_deltas_updated\", \"uid\": \"31337\", \"revision\": 2 }",
                celeryJobs.get(0).getKwargs().getTs("xiva_data").serialize());

        // assert albums merged push sent
        Assert.equals(JsonString.valueOf("albums"), celeryJobs.get(1).getKwargs().getTs("class"));
        Assert.equals("{ \"root\": { \"tag\": \"album\", \"parameters\": { " +
                        "\"src_id\": \"" + srcAlbum.getId().toHexString() + "\", " +
                        "\"dst_id\": \"" + dstAlbum.getId().toHexString() + "\", \"type\": \"albums_merged\" } } }",
                celeryJobs.get(1).getKwargs().getTs("xiva_data").serialize());
    }

    @Test
    public void mergeAlbumsWithBlankDstTitle() {
        facesAlbumManager.initializeAlbumsFromClusters(UID);
        Album srcAlbum = albumDao.getAlbums(UID, AlbumType.FACES).sortedBy(Album::getId).get(1);
        Album dstAlbum = createFacesAlbumWithPhotos(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), 2)._1;

        albumDao.setTitle(srcAlbum, "My lovely face");
        albumDao.setTitle(dstAlbum, "");

        ListF<String> srcItemIds = getItemIds(srcAlbum.getId());
        Assert.hasSize(3, srcItemIds);
        ListF<String> dstItemIds = getItemIds(dstAlbum.getId());
        Assert.hasSize(2, dstItemIds);

        facesAlbumManager.mergeAlbums(UID, srcAlbum.getId(), dstAlbum.getId());

        // assert deltas
        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        Assert.hasSize(1, allDeltas);
        Assert.equals(2L, allDeltas.single().getRevision());
        Assert.some(2L, albumDeltaDao.getCurrentRevisionWithoutLock(UID));

        ListF<AlbumDeltaChange> changes = AlbumDelta.B.getParser().parseListJson(allDeltas.single().changes);

        Assert.hasSize(10, changes);
        assertAlbumFieldSet(changes.get(4), dstAlbum, "title");
        Assert.equals("My lovely face", albumDao.findAlbum(UID, dstAlbum.getId()).get().getTitle());
    }

    @Test
    public void mergeAlbumsWithIntersections() {
        facesAlbumManager.initializeAlbumsFromClusters(UID);
        Album srcAlbum = albumDao.getAlbums(UID, AlbumType.FACES).sortedBy(Album::getId).get(1);
        Album dstAlbum = createFacesAlbumWithPhotos(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), 2)._1;

        //create artificial intersection
        AlbumItem srcItem = albumItemDao.getAllAlbumItems(UID, srcAlbum.getId()).first();
        AlbumItem dstItem = AlbumItem.builder()
                .id(new ObjectId()).uid(srcItem.getUid()).albumId(dstAlbum.getId()).objectId(srcItem.getObjectId())
                .objectType(srcItem.getObjectType()).description(srcItem.getDescription())
                .orderIndex(srcItem.getOrderIndex()).groupId(srcItem.getGroupId()).faceInfo(srcItem.getFaceInfo())
                .dateCreated(srcItem.getDateCreated()).build();

        albumItemDao.insert(dstItem);

        ListF<String> srcItemIds = getItemIds(srcAlbum.getId());
        Assert.hasSize(3, srcItemIds);
        ListF<String> dstItemIds = getItemIds(dstAlbum.getId());
        Assert.hasSize(3, dstItemIds);

        facesAlbumManager.mergeAlbums(UID, srcAlbum.getId(), dstAlbum.getId());

        // assert deltas
        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        Assert.hasSize(1, allDeltas);
        Assert.equals(2L, allDeltas.single().getRevision());
        Assert.some(2L, albumDeltaDao.getCurrentRevisionWithoutLock(UID));

        ListF<AlbumDeltaChange> changes = AlbumDelta.B.getParser().parseListJson(allDeltas.single().changes);

        Assert.hasSize(8, changes);

        srcItemIds.drop(1).zip(changes.subList(0, 2)).forEach((albumItemId, change) -> {
            Assert.equals(AlbumDeltaChangeType.INSERT, change.getChangeType());
            Assert.equals(dstAlbum.getId().toHexString(), change.getCollectionId());
            Assert.equals(albumItemId, change.getRecordId());
        });

        assertAlbumFieldSet(changes.get(2), dstAlbum, "items_count");

        srcItemIds.zip(changes.subList(3, 6)).forEach((albumItemId, change) -> {
            Assert.equals(AlbumDeltaChangeType.DELETE, change.getChangeType());
            Assert.equals(srcAlbum.getId().toHexString(), change.getCollectionId());
            Assert.equals(albumItemId, change.getRecordId());
        });

        // assert album items states
        Assert.hasSize(0, getItemIds(srcAlbum.getId()));
        Assert.hasSize(5, getItemIds(dstAlbum.getId()));
    }

    private void assertAlbumFieldSet(AlbumDeltaChange change, Album album, String fieldId) {
        Assert.equals(AlbumDeltaChangeType.UPDATE, change.getChangeType());
        Assert.equals(CollectionId.ALBUM_LIST_COLLECTION_ID, change.getCollectionId());
        Assert.equals(album.getId().toHexString(), change.getRecordId());
        Assert.equals(fieldId, change.changes.single().fieldId);
    }

    private ListF<String> getItemIds(ObjectId albumId) {
        return albumItemDao.getAllAlbumItems(UID, albumId).map(i -> i.getId().toHexString());
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

    private static class AlbumMigrateTaskForTest extends AlbumMergeTask {
        public AlbumMigrateTaskForTest(AlbumMergeManager albumMergeManager, OperationDao operationDao) {
            super(albumMergeManager, operationDao);
        }

        public void execute(DjfsUid uid, String oid, ObjectId source, ObjectId destination, AlbumType type) {
            try {
                execute(new Parameters(uid, oid, source, destination, type), null);
            } catch (Exception e) {
                throw ExceptionUtils.translate(e);
            }
        }
    }

    private FacesDelta itemAdded(String clusterId, String resourceId, int size) {
        final String sizeS = Integer.toString(size);
        return new FacesDelta(
                FacesDelta.Type.ITEM_ADDED,
                clusterId,
                Option.of(Cf.toMap(Tuple2List.fromPairs(
                        "resource_id", resourceId,
                        "height", sizeS,
                        "width", sizeS,
                        "face_coord_x", "0.1",
                        "face_coord_y", "0.2",
                        "face_height", "0.3",
                        "face_width", "0.4"
                ))),
                null
        );
    }

    private FacesDelta itemDeleted(String clusterId, String resourceId) {
        return new FacesDelta(FacesDelta.Type.ITEM_DELETED, clusterId, null, Option.of(resourceId));
    }

    private Album clusterUpdateSetup() {
        userDao.setFacesIndexingState(UID, FacesIndexingState.REINDEXED);

        // before update
        Assert.sizeIs(0, albumDao.getAlbums(UID, AlbumType.FACES));
        Assert.none(albumDeltaDao.getCurrentRevisionWithoutLock(UID));

        diskSearchHttpClient.setFacesDeltas(
                142,
                itemAdded(
                        "31337_91_11",
                        "31337:30ba54e4948b0f7ad9a8abbe73502c5eda4de0466234a809452846e8a2bdfb15",
                        100
                )
        );
        Assert.isTrue(facesAlbumManager.updateAlbumsFromClusters(UID));

        final Album album1 = albumDao.getAlbums(UID, AlbumType.FACES).single();
        Assert.sizeIs(1, albumItemDao.getAllAlbumItems(UID, album1.getId()));
        Assert.isFalse(album1.isHidden());
        Assert.isTrue(album1.isCoverAuto());

        Assert.some(142L, userDao.getFaceClustersVersion(UID));
        Assert.some(1L, albumDeltaDao.getCurrentRevisionWithoutLock(UID));
        // starting from 0, added 1 delta here

        return album1;
    }

    @Test
    public void clusterUpdateWithGreaterResolutionLeadsToCoverAutoChange() {
        final Album album1 = clusterUpdateSetup();

        diskSearchHttpClient.setFacesDeltas(
                176,
                itemAdded(
                        "31337_91_11",
                        "31337:600472fbb70465775102ddd6ee53b99bf14d7bbfbd4c215536a13e2138a1a891",
                        200 // resolution is greater than previous
                )
        );
        Assert.isTrue(facesAlbumManager.updateAlbumsFromClusters(UID));

        final Album album2 = albumDao.getAlbums(UID, AlbumType.FACES).single();
        Assert.equals(album1.getId(), album2.getId());
        Assert.sizeIs(2, albumItemDao.getAllAlbumItems(UID, album2.getId())); // item added
        Assert.isFalse(album2.isHidden());
        Assert.notEquals(album1.getCoverId().get(), album2.getCoverId().get());

        Assert.some(176L, userDao.getFaceClustersVersion(UID));
        Assert.some(3L, albumDeltaDao.getCurrentRevisionWithoutLock(UID));
        // adding two deltas here: for the item and for the album
    }

    private Album clusterUpdateSetupUserCover() {
        final Album album1 = clusterUpdateSetup();
        final ObjectId album1Cover = album1.getCoverId().get();

        // set cover manually
        facesAlbumManager.setAttrs(
                UID,
                album1,
                Option.empty(),
                Option.of(album1Cover.toHexString()),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                AlbumSetAttrCallbacks.empty()
        );

        final Album album2 = albumDao.findAlbum(UID, album1.getId()).get();
        final ObjectId album2Cover = album2.getCoverId().get();
        Assert.equals(album1Cover, album2Cover);
        Assert.isFalse(album2.isCoverAuto()); // changed to manual

        Assert.some(2L, albumDeltaDao.getCurrentRevisionWithoutLock(UID)); // cover change

        return album2;
    }

    @Test
    public void clusterUpdateWithGreaterResolutionLeavesUserCoverUnchanged() {
        final Album album1 = clusterUpdateSetupUserCover();
        final ObjectId album1Cover = album1.getCoverId().get();

        diskSearchHttpClient.setFacesDeltas(
                176,
                itemAdded(
                        "31337_91_11",
                        "31337:600472fbb70465775102ddd6ee53b99bf14d7bbfbd4c215536a13e2138a1a891",
                        200 // resolution is greater than previous
                )
        );
        Assert.isTrue(facesAlbumManager.updateAlbumsFromClusters(UID));

        // after update
        final Album album2 = albumDao.getAlbums(UID, AlbumType.FACES).single();
        Assert.equals(album1.getId(), album2.getId());
        Assert.sizeIs(2, albumItemDao.getAllAlbumItems(UID, album2.getId())); // item added
        Assert.isFalse(album2.isHidden());

        final ObjectId album2Cover = album2.getCoverId().get();
        Assert.equals(album1Cover, album2Cover);

        Assert.some(176L, userDao.getFaceClustersVersion(UID));
        Assert.some(4L, albumDeltaDao.getCurrentRevisionWithoutLock(UID));
        // adding two deltas here: for the item and for the album
    }

    @Test
    public void clusterUpdateWithUserCoverDeletedResetsCoverToAuto() {
        final Album album1 = clusterUpdateSetupUserCover();

        diskSearchHttpClient.setFacesDeltas(
                244,
                itemDeleted(
                        "31337_91_11",
                        "31337:30ba54e4948b0f7ad9a8abbe73502c5eda4de0466234a809452846e8a2bdfb15"
                )
        );
        Assert.isTrue(facesAlbumManager.updateAlbumsFromClusters(UID));

        final Album album2 = albumDao.getAlbums(UID, AlbumType.FACES).single();
        Assert.equals(album1.getId(), album2.getId());
        Assert.sizeIs(0, albumItemDao.getAllAlbumItems(UID, album2.getId()));
        Assert.isFalse(album2.isHidden());
        Assert.isTrue(album2.isCoverAuto()); // automatic again

        Assert.some(244L, userDao.getFaceClustersVersion(UID));
        Assert.some(4L, albumDeltaDao.getCurrentRevisionWithoutLock(UID));
        // adding two deltas here: for the item and for the album
    }

    @Test
    public void countryRestriction() {
        {
            final DjfsUid uid = UID;

            blackbox2.add(uid, MockBlackbox2.userBuilder().country("RU").build());
            facesAlbumManager.startIndexing(uid, false);
            Assert.equals(FacesIndexingState.RUNNING, userDao.getFacesIndexingState(uid)._1);
        }

        {
            final DjfsUid uid = UID_BABE;

            blackbox2.add(uid, MockBlackbox2.userBuilder().country("US").build());
            Assert.assertThrows(
                    () -> facesAlbumManager.startIndexing(uid, false), FeatureUnavailableInUserCountryException.class
            );
            Assert.equals(FacesIndexingState.COUNTRY_RESTRICTION, userDao.getFacesIndexingState(uid)._1);
        }
    }

    @Test
    public void reindexWithForce() {
        // initial index
        Assert.equals(true, facesAlbumManager.startIndexing(UID, false));
        Tuple2<FacesIndexingState, Instant> lastState = userDao.getFacesIndexingState(UID);
        Assert.equals(FacesIndexingState.RUNNING, lastState._1);

        // reindex without force during indexing
        Assert.equals(false, facesAlbumManager.startIndexing(UID, false));
        Assert.equals(lastState, userDao.getFacesIndexingState(UID)); // not modified

        // reindex with force
        Assert.equals(true, facesAlbumManager.startIndexing(UID, true));
        final Tuple2<FacesIndexingState, Instant> state2 = userDao.getFacesIndexingState(UID);
        Assert.lt(lastState._2, state2._2); // state is modified
        lastState = state2;
        Assert.equals(FacesIndexingState.RUNNING, lastState._1);

        // index completed
        facesAlbumManager.initializeAlbumsFromClusters(UID);
        lastState = userDao.getFacesIndexingState(UID);

        // reindex without force after full index
        Assert.equals(false, facesAlbumManager.startIndexing(UID, false));
        Assert.equals(lastState, userDao.getFacesIndexingState(UID)); // not modified

        // reindex with force
        Assert.equals(true, facesAlbumManager.startIndexing(UID, true));
        final Tuple2<FacesIndexingState, Instant> state3 = userDao.getFacesIndexingState(UID);
        Assert.lt(state2._2, state3._2); // state is modified
        lastState = state3;
        Assert.equals(FacesIndexingState.RUNNING, lastState._1);
    }

    private AlbumItemFaceCoordinates getCover(String faceCoordX, String faceCoordY, String faceHeight,
                                              String faceWidth, String height, String width) {
        MapF<String, String> data = Cf.map("face_coord_x", faceCoordX)
                .plus1("face_coord_y", faceCoordY)
                .plus1("face_height", faceHeight)
                .plus1("face_width", faceWidth)
                .plus1("height", height)
                .plus1("width", width)
                .plus1("resource_id", "id");
        FaceInfo faceInfo = new FaceInfo(data);
        return new AlbumItemFaceCoordinates(faceInfo);
    }
    private boolean validateBorders(AlbumItemFaceCoordinates cover) {
        return cover.getLeftTopAngle().getX() >= 0
                && cover.getLeftTopAngle().getY() >= 0
                && cover.getRightBottomAngle().getX() <= 1
                && cover.getRightBottomAngle().getY() <= 1
                && cover.getLeftTopAngle().getX() <= cover.getRightBottomAngle().getX()
                && cover.getLeftTopAngle().getY() <= cover.getRightBottomAngle().getY();
    }

    private void assertCorrectCoordinates(AlbumItemFaceCoordinates cover, double left, double top, double right, double bottom) {
        final double eps = 0.000001;
        Assert.isTrue(validateBorders(cover));
        Assert.equals(cover.getLeftTopAngle().getX(), left, eps);
        Assert.equals(cover.getLeftTopAngle().getY(), top, eps);
        Assert.equals(cover.getRightBottomAngle().getX(), right, eps);
        Assert.equals(cover.getRightBottomAngle().getY(), bottom, eps);
    }

    @Test
    public void checkCoverCoordinates() {
        AlbumItemFaceCoordinates cover = getCover("0.242184", "0.313399", "0.402529","0.436081", "2048", "1536");
        assertCorrectCoordinates(cover, 0d, 0.188977, 1d, 0.751477);

        cover = getCover("0.05", "0.1", "0.7","0.5", "1000", "2000");
        assertCorrectCoordinates(cover, 0d, 0d, 0.666666, 1d);

        cover = getCover("0", "0", "1","1", "750", "1000");
        assertCorrectCoordinates(cover, 0d, 0d, 1d, 1d);

        cover = getCover("0.3296781301", "0", "0.923567009","0.5361372471", "1920", "2560");
        assertCorrectCoordinates(cover, 0d, 0d, 1d, 1d);
    }
}
