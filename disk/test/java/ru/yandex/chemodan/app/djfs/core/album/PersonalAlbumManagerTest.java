package ru.yandex.chemodan.app.djfs.core.album;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.albumappend.AlbumAppendCallbacks;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.albumitemremove.AlbumItemRemoveCallbacks;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.albumremove.AlbumRemoveCallbacks;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.albumsetattr.AlbumSetAttrCallbacks;
import ru.yandex.chemodan.app.djfs.core.share.Group;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.serialize.JsonParser;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

public class PersonalAlbumManagerTest extends DjfsAlbumsTestBase {
    private Tuple2<Album, ListF<FileDjfsResource>> createPersonalAlbumWithPhotos(DjfsPrincipal principal,
        DjfsResourcePath folder, int photosNum)
    {
        Album album = createAlbum(principal.getUid(), "test", a -> a.type(AlbumType.PERSONAL));
        albumDao.insert(album);

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
    public void addPhotoToAlbum() {
        int photosNum = 1;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
                PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), photosNum);
        Album album = personalAlbumWithPhotos._1;

        Instant resourceEtime = Instant.now().minus(Duration.standardDays(100500));
        FileDjfsResource fileA = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/img-2.jpg"), x->x.exifTime(resourceEtime));
        FileDjfsResource fileB = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/img-3.jpg"), x->x.exifTime(resourceEtime));
        ListF<DjfsResource> resources = Cf.arrayList();
        resources.add(fileA);
        resources.add(fileB);
        mockEventHistoryLogger.messageData.clear();
        personalAlbumManager.addPhotosToAlbum(resources, album, true, AlbumAppendCallbacks.defaultWithLogging(mockEventHistoryLogger), false, true);
        Assert.sizeIs(3, albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100));
        Assert.sizeIs(2, mockEventHistoryLogger.messageData.filter(
                x -> x.getOrElse("event_type", "").equals("album-items-append"))
        );
        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        ListF<JsonObject> updateDeltas = getDeltasByType(allDeltas, "update");
        Assert.sizeIs(1, updateDeltas);
        Assert.sizeIs(3, allDeltas);
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
    public void removePhotoFromAlbum() {
        int photosNum = 1;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
                PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), photosNum);
        Album album = personalAlbumWithPhotos._1;

        Instant resourceEtime = Instant.now().minus(Duration.standardDays(100500));
        FileDjfsResource fileA = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/img-2.jpg"), x->x.exifTime(resourceEtime));
        FileDjfsResource fileB = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/img-3.jpg"), x->x.exifTime(resourceEtime));
        ListF<DjfsResource> resources = Cf.arrayList();
        resources.add(fileA);
        resources.add(fileB);
        personalAlbumManager.addPhotosToAlbum(resources, album, true, AlbumAppendCallbacks.empty(), false, true);
        ListF<AlbumItem> res = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(3, res);
        mockEventHistoryLogger.messageData.clear();
        personalAlbumManager.removePhotoFromAlbum(UID, album, res.first().getId().toString(),
                AlbumItemRemoveCallbacks.defaultWithLogging(mockEventHistoryLogger), true);
        Assert.sizeIs(2, albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100));
        Assert.sizeIs(1, mockEventHistoryLogger.messageData.filter(
                x -> x.getOrElse("event_type", "").equals("album-items-remove"))
        );
    }

    @Test
    public void removeAlbum() {
        int photosNum = 3;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
                PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), photosNum);
        Album album = personalAlbumWithPhotos._1;
        mockEventHistoryLogger.messageData.clear();
        personalAlbumManager.removeAlbum(UID, album, AlbumRemoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        ListF<JsonObject> deleteDeltas = getDeltasByType(allDeltas, "delete");
        Assert.sizeIs(1, deleteDeltas);
        Assert.sizeIs(1, mockEventHistoryLogger.messageData.filter(
                x -> x.getOrElse("event_type", "").equals("album-remove"))
        );
    }

    @Test
    public void setAttrsRename() {
        int photosNum = 1;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
                PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), photosNum);
        Album album = personalAlbumWithPhotos._1;
        mockEventHistoryLogger.messageData.clear();
        personalAlbumManager.setAttrs(
                UID, album, Option.of("sweet one"), Option.empty(), Option.empty(), Option.empty(),
                Option.of("The best album you have ever seen"), Option.empty(), Option.of(12345689098765L),
                AlbumSetAttrCallbacks.defaultWithLogging(mockEventHistoryLogger));

        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        ListF<JsonObject> updateDeltas = getDeltasByType(allDeltas, "update");
        Assert.sizeIs(1, updateDeltas);

        ListF<MapF<String, String>> historyChangeTitle = mockEventHistoryLogger.messageData.filter(
                x -> x.getOrElse("event_type", "").equals("album-change-title"));
        Assert.sizeIs(1, historyChangeTitle);
        Album albumAfterSetAttr = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals("sweet one", albumAfterSetAttr.getTitle());
        Assert.equals("sweet one", historyChangeTitle.get(0).getO("album_title").get());
        Assert.equals(Option.of("The best album you have ever seen"), albumAfterSetAttr.getDescription());
        Assert.equals(Option.of(12345689098765L), albumAfterSetAttr.getFotkiAlbumId());
    }

    @Test
    public void setAttrsChangeCover() {
        int photosNum = 5;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
                PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), photosNum);
        Album album = personalAlbumWithPhotos._1;
        mockEventHistoryLogger.messageData.clear();
        personalAlbumManager.setAttrs(
                UID, album, Option.empty(), Option.of("2"), Option.of(51.6875), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(),
                AlbumSetAttrCallbacks.defaultWithLogging(mockEventHistoryLogger));

        ListF<AlbumDeltaRaw> allDeltas = albumDeltaDao.findRaw(UID);
        ListF<JsonObject> updateDeltas = getDeltasByType(allDeltas, "update");
        Assert.sizeIs(1, updateDeltas);
        Assert.sizeIs(1, mockEventHistoryLogger.messageData.filter(
                x -> x.getOrElse("event_type", "").equals("album-change-cover"))
        );
        Assert.sizeIs(1, mockEventHistoryLogger.messageData.filter(
                x -> x.getOrElse("event_type", "").equals("album-change-cover-offset"))
        );
        Album albumAfterSetAttr = albumDao.findAlbum(UID, album.getId()).get();
        // TODO Надо вынуть файл по номеру и сравнить ID как это сделать так чтоб на опираться на метод решения хз
        // assert albumAfterSetAttr.getCoverId().equals();
        Assert.equals(Option.of(51.6875), albumAfterSetAttr.getCoverOffsetY());
    }

    @Test
    public void checkSyncFilesDeletion() {
        int photosNum = 10;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
            PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), photosNum);
        Album album = personalAlbumWithPhotos._1;
        ListF<FileDjfsResource> files = personalAlbumWithPhotos._2;

        Assert.sizeIs(photosNum, albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100));

        int removeCount = 4;
        ListF<FileDjfsResource> filesToRemove = files.take(removeCount);
        DjfsResource trash = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.TRASH)).get();
        filesToRemove.forEach(f -> djfsResourceDao.changeParent(UID, f.getId(), trash.getId()));

        ListF<DjfsResourceId> resourceIds = filesToRemove.flatMap(DjfsResource::getResourceId);
        personalAlbumManager.postProcessFiles(resourceIds);

        ListF<AlbumItem> leftItems = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(photosNum - removeCount, leftItems);
        Assert.sizeIs(0,
            leftItems.map(AlbumItem::getObjectId).filter(i -> resourceIds.containsTs(DjfsResourceId.cons(UID, i)))
        );
    }

    @Test
    public void checkSyncFilesDeletionFromSeveralAlbums() {
        FileDjfsResource fileAB = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/img-1.jpg"));
        FileDjfsResource fileA = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/img-2.jpg"));
        FileDjfsResource fileB = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/img-3.jpg"));
        FileDjfsResource fileC = filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/img-4.jpg"));

        Album albumA = createAlbum(UID, "A", a -> a.type(AlbumType.PERSONAL));
        Album albumB = createAlbum(UID, "B", a -> a.type(AlbumType.PERSONAL));
        Album albumC = createAlbum(UID, "C", a -> a.type(AlbumType.PERSONAL));

        albumDao.insert(albumA);
        albumDao.insert(albumB);
        albumDao.insert(albumC);

        AlbumItem item = createItem(albumA, i -> i.objectId(fileAB.getFileId().get().getValue()));
        albumItemDao.insert(item);
        item = createItem(albumB, i -> i.objectId(fileAB.getFileId().get().getValue()));
        albumItemDao.insert(item);
        item = createItem(albumA, i -> i.objectId(fileA.getFileId().get().getValue()));
        albumItemDao.insert(item);
        item = createItem(albumB, i -> i.objectId(fileB.getFileId().get().getValue()));
        albumItemDao.insert(item);
        item = createItem(albumC, i -> i.objectId(fileC.getFileId().get().getValue()));
        albumItemDao.insert(item);

        Assert.sizeIs(2, albumItemDao.getAllAlbumItems(UID, Cf.list(albumA.getId()), 100));
        Assert.sizeIs(2, albumItemDao.getAllAlbumItems(UID, Cf.list(albumB.getId()), 100));
        Assert.sizeIs(1, albumItemDao.getAllAlbumItems(UID, Cf.list(albumC.getId()), 100));

        DjfsResource trash = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.TRASH)).get();
        djfsResourceDao.changeParent(UID, fileAB.getId(), trash.getId());

        personalAlbumManager.postProcessFiles(Cf.list(fileAB.getResourceId().get()));

        ListF<AlbumItem> itemsA = albumItemDao.getAllAlbumItems(UID, Cf.list(albumA.getId()), 100);
        ListF<AlbumItem> itemsB = albumItemDao.getAllAlbumItems(UID, Cf.list(albumB.getId()), 100);
        ListF<AlbumItem> itemsC = albumItemDao.getAllAlbumItems(UID, Cf.list(albumC.getId()), 100);

        Assert.sizeIs(1, itemsA);
        Assert.sizeIs(1, itemsB);
        Assert.sizeIs(1, itemsC);

        Assert.equals(fileA.getFileId().get().getValue(), itemsA.first().getObjectId());
        Assert.equals(fileB.getFileId().get().getValue(), itemsB.first().getObjectId());
        Assert.equals(fileC.getFileId().get().getValue(), itemsC.first().getObjectId());
    }

    @Test
    public void checkRevisionUpdateAfterSync() {
        int photosNum = 10;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
            PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), photosNum);
        Album album = personalAlbumWithPhotos._1;
        ListF<FileDjfsResource> files = personalAlbumWithPhotos._2;
        long previousRevision = 42;
        albumDao.updateAlbumRevision(album, previousRevision);

        Assert.sizeIs(photosNum, albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100));

        FileDjfsResource fileToRemove = files.first();
        DjfsResource trash = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.TRASH)).get();
        djfsResourceDao.changeParent(UID, fileToRemove.getId(), trash.getId());

        personalAlbumManager.postProcessFiles(Cf.list(fileToRemove.getResourceId().get()));

        album = albumDao.findAlbums(UID, Cf.list(album.getId())).first();
        Assert.gt(album.getRevision().get(), previousRevision);
    }

    @Test
    public void checkRevisionUpdateIfThereWasNullRevisionField() {
        int photosNum = 10;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
                PRINCIPAL, DjfsResourcePath.cons(UID, "/disk"), photosNum);
        Album album = personalAlbumWithPhotos._1;
        ListF<FileDjfsResource> files = personalAlbumWithPhotos._2;

        JdbcTemplate3 shard = pgShardResolver.resolve(UID);
        String q = "UPDATE disk.albums SET revision=NULL WHERE uid = ? AND id = ?";
        shard.update(q, UID, album.getId().toByteArray());

        Assert.sizeIs(photosNum, albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100));

        FileDjfsResource fileToRemove = files.first();
        DjfsResource trash = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.TRASH)).get();
        djfsResourceDao.changeParent(UID, fileToRemove.getId(), trash.getId());

        album = albumDao.findAlbums(UID, Cf.list(album.getId())).first();
        Assert.isFalse(album.getRevision().isPresent());

        personalAlbumManager.postProcessFiles(Cf.list(fileToRemove.getResourceId().get()));

        album = albumDao.findAlbums(UID, Cf.list(album.getId())).first();
        Assert.gt(album.getRevision().get(), 0L);
    }

    @Test
    public void checkSyncFilesDeletionForSharedFolderOwner() {
        DjfsResourcePath ownerPath = DjfsResourcePath.cons(UID, "/disk/owner_path");
        filesystem.createFolder(PRINCIPAL, ownerPath);
        Group group = shareManager.createGroup(ownerPath);

        final DjfsUid PARTICIPANT_UID = DjfsUid.cons(10050042);
        final DjfsPrincipal PARTICIPANT_PRINCIPAL = DjfsPrincipal.cons(PARTICIPANT_UID);
        initializeUser(PARTICIPANT_UID, 1);
        DjfsResourcePath participantPath = DjfsResourcePath.cons(PARTICIPANT_UID, "/disk/folder/participant_path");
        util.fs.createFolderRecursively(PARTICIPANT_PRINCIPAL, participantPath);
        shareManager.createLink(group.getId(), participantPath, SharePermissions.READ_WRITE);

        int photosNum = 10;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
            PRINCIPAL, ownerPath, photosNum);
        Album album = personalAlbumWithPhotos._1;
        ListF<FileDjfsResource> files = personalAlbumWithPhotos._2;

        int removeCount = 4;
        ListF<FileDjfsResource> filesToRemove = files.take(removeCount);
        DjfsResource trash = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.TRASH)).get();
        filesToRemove.forEach(f -> djfsResourceDao.changeParent(UID, f.getId(), trash.getId()));

        ListF<DjfsResourceId> resourceIds = filesToRemove.flatMap(DjfsResource::getResourceId);
        personalAlbumManager.postProcessFiles(resourceIds);

        ListF<AlbumItem> leftItems = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(photosNum - removeCount, leftItems);
        Assert.sizeIs(0,
            leftItems.map(AlbumItem::getObjectId).filter(i -> resourceIds.containsTs(DjfsResourceId.cons(UID, i)))
        );
    }

    @Test
    public void checkSyncFilesDeletionForSharedFolderParticipant() {
        // IMPORTANT: we do not sync album items for participants now

        DjfsResourcePath ownerPath = DjfsResourcePath.cons(UID, "/disk/owner_path");
        filesystem.createFolder(PRINCIPAL, ownerPath);
        Group group = shareManager.createGroup(ownerPath);

        final DjfsUid PARTICIPANT_UID = DjfsUid.cons(10050042);
        final DjfsPrincipal PARTICIPANT_PRINCIPAL = DjfsPrincipal.cons(PARTICIPANT_UID);
        initializeUser(PARTICIPANT_UID, 1);
        DjfsResourcePath participantPath = DjfsResourcePath.cons(PARTICIPANT_UID, "/disk/folder/participant_path");
        util.fs.createFolderRecursively(PARTICIPANT_PRINCIPAL, participantPath);
        shareManager.createLink(group.getId(), participantPath, SharePermissions.READ_WRITE);

        int photosNum = 10;
        Tuple2<Album, ListF<FileDjfsResource>> personalAlbumWithPhotos = createPersonalAlbumWithPhotos(
            PARTICIPANT_PRINCIPAL, participantPath, photosNum);
        Album album = personalAlbumWithPhotos._1;
        ListF<FileDjfsResource> files = personalAlbumWithPhotos._2;

        int removeCount = 4;
        ListF<FileDjfsResource> filesToRemove = files.take(removeCount);
        DjfsResource trash = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.TRASH)).get();
        filesToRemove.forEach(f -> djfsResourceDao.changeParent(UID, f.getId(), trash.getId()));

        ListF<DjfsResourceId> resourceIds = filesToRemove.flatMap(DjfsResource::getResourceId);
        personalAlbumManager.postProcessFiles(resourceIds);

        ListF<AlbumItem> leftItems = albumItemDao.getAllAlbumItems(PARTICIPANT_UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(photosNum, leftItems);
        Assert.sizeIs(removeCount,
            leftItems.map(AlbumItem::getObjectId).filter(i -> resourceIds.containsTs(DjfsResourceId.cons(UID, i)))
        );
    }
}
