package ru.yandex.chemodan.app.djfs.core.filesystem;

import com.mongodb.ReadPreference;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.changelog.Changelog;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.move.MoveCallbacks;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.postprocess.QuickMovePostProcessTask;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSinglePostgresUserTestBase;
import ru.yandex.misc.test.Assert;

public class FilesystemTrashAppendTest extends DjfsSinglePostgresUserTestBase {
    @Test
    public void trashAppendFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        filesystem.trashAppendResource(PRINCIPAL, folderPath, MoveCallbacks.empty());

        Assert.some(filesystem.find(PRINCIPAL, folderPath.changeArea(DjfsResourceArea.TRASH), Option.of(ReadPreference.primary())));
    }

    @Test
    public void trashAppendFolderWithExistingTarget() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        DjfsResourcePath trashPath = DjfsResourcePath.cons(UID, "/trash/folder");
        filesystem.createFolder(PRINCIPAL, trashPath);
        Assert.some(filesystem.find(PRINCIPAL, folderPath.changeArea(DjfsResourceArea.TRASH), Option.of(ReadPreference.primary())));

        DjfsResource removedFolder = filesystem.trashAppendResource(PRINCIPAL, folderPath, MoveCallbacks.empty());
        Assert.equals(DjfsResourceArea.TRASH, removedFolder.getPath().getArea());
        Assert.some(filesystem.find(PRINCIPAL, removedFolder.getPath(), Option.of(ReadPreference.primary())));
    }

    @Test
    public void trashAppendDeepFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath subfolderPath = folderPath.getChildPath("subfolder");
        filesystem.createFolder(PRINCIPAL, subfolderPath);
        DjfsResourcePath filePath = subfolderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        DjfsResource removedFolder = filesystem.trashAppendResource(PRINCIPAL, folderPath, MoveCallbacks.empty());
        Assert.equals(DjfsResourceArea.TRASH, removedFolder.getPath().getArea());
        Assert.equals(2, removedFolder.getPath().getDepth());
        Assert.some(filesystem.find(PRINCIPAL, removedFolder.getPath(), Option.of(ReadPreference.primary())));
    }

    @Test
    public void trashAppendTaskForPostProcessing() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        bazingaStub.tasksWithParams.clear();

        filesystem.trashAppendResource(PRINCIPAL, folderPath, MoveCallbacks.empty());

        Assert.sizeIs(1, bazingaStub.tasksWithParams.filterByType(QuickMovePostProcessTask.class));
    }

    @Test
    public void trashAppendPublicFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath, x -> x.isPublic(true).downloadCounter(200));
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        filesystem.trashAppendResource(PRINCIPAL, folderPath, MoveCallbacks.empty());

        Option<DjfsResource> removedFolder = filesystem.find(PRINCIPAL, folderPath.changeArea(DjfsResourceArea.TRASH), Option.of(ReadPreference.primary()));
        Assert.some(removedFolder);
        Assert.isTrue(removedFolder.get().isPublished());
        Assert.isFalse(removedFolder.get().getDownloadCounter().isPresent());
    }

    @Test
    public void checkEventHistoryLog() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);

        mockEventHistoryLogger.messageData.clear();
        filesystem.trashAppendResource(PRINCIPAL, folderPath, MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
        Assert.equals(1, mockEventHistoryLogger.messageData.size());

        MapF<String, String> logItem = mockEventHistoryLogger.messageData.first();
        Assert.equals("fs-trash-append", logItem.getOrThrow("event_type"));
    }

    @Test
    public void checkCorrectChangelogEntryForSmallFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        for (int i = 0; i < 5; i++) {
            filesystem.createFile(PRINCIPAL, folderPath.getChildPath("file-" + i + ".txt"));
        }

        changelogDao.deleteAll(UID);

        filesystem.trashAppendResource(PRINCIPAL, folderPath, MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        ListF<Changelog> changelogs = changelogDao.findAll(UID);
        Assert.sizeIs(1, changelogs);

        Changelog entry = changelogs.first();
        Assert.equals(Changelog.OperationType.DELETED, entry.getOperationType());
        Assert.equals(folderPath, entry.getPath());
    }

    @Test
    public void checkCorrectChangelogEntryForBigFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        for (int i = 0; i < 800; i++) {
            filesystem.createFile(PRINCIPAL, folderPath.getChildPath("file-" + i + ".txt"));
        }

        changelogDao.deleteAll(UID);

        filesystem.trashAppendResource(PRINCIPAL, folderPath, MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        ListF<Changelog> changelogs = changelogDao.findAll(UID);
        Assert.sizeIs(1, changelogs);

        Changelog entry = changelogs.first();
        Assert.equals(Changelog.OperationType.DELETED, entry.getOperationType());
        Assert.equals(folderPath, entry.getPath());
    }
}
