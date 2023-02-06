package ru.yandex.chemodan.app.djfs.core.filesystem;


import org.joda.time.Instant;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.DjfsNotImplementedException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.legacy.AsyncOperationResultPojo;
import ru.yandex.chemodan.app.djfs.core.legacy.LegacyFilesystemActions;
import ru.yandex.chemodan.app.djfs.core.legacy.exception.LegacyMd5UnsupportedException;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSinglePostgresUserTestBase;
import ru.yandex.chemodan.app.djfs.core.web.JsonStringResult;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = LegacyFilesystemActionsTestContextConfiguration.class)
public class LegacyFilesystemActionsTest extends DjfsSinglePostgresUserTestBase {
    @Autowired
    private LegacyFilesystemActions legacyFilesystemActions;
    @Autowired
    private FilesystemActions filesystemActions;
    @Autowired
    private SupportDao supportDao;

    @Test
    public void testAsyncMove() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/new");

        AsyncOperationResultPojo result = legacyFilesystemActions.asyncMove(
                UID.asString(),
                folderPath.getFullPath(),
                destinationPath.getFullPath(),
                Option.empty(),
                Option.empty(),
                createRequestWithUserObj(userDao.find(UID).get())
        );

        Option<Operation> operation = operationDao.find(UID, result.operationId);
        Assert.some(operation);
        Assert.equals(Operation.State.WAITING, operation.get().getState());
    }

    @Test
    public void testTrashAppendTrashCleanTask() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        legacyFilesystemActions.trashAppend(UID.asString(), folderPath.getFullPath(), "");

        ListF<TrashCleanQueueTask> trashCleanTasks = pgTrashCleanQueueDao.findAll(UID);
        Assert.sizeIs(1, trashCleanTasks);
        Assert.equals(UID, trashCleanTasks.get(0).getUid());
        Assert.gt(86400000L, Instant.now().toDate().getTime() - trashCleanTasks.get(0).getDate().getTime());
    }

    @Test(expected = LegacyMd5UnsupportedException.class)
    public void trashAppendWithMd5ForFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, filePath);

        legacyFilesystemActions.asyncTrashAppend(UID.asString(), folderPath.getFullPath(),
                Option.of(file.getMd5()), Option.empty(), createRequestWithUserObj(userDao.find(UID).get()));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void trashAppendWithMd5ForFile() {
        makeQuickMoveUser(UID);

        DjfsResourcePath filePath = DjfsResourcePath.cons(UID, "/disk/file.txt");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, filePath);

        legacyFilesystemActions.asyncTrashAppend(UID.asString(), filePath.getFullPath(),
                Option.of(file.getMd5()), Option.empty(), createRequestWithUserObj(userDao.find(UID).get()));
    }

    @Test
    public void testBlocking() {
        Mockito.when(supportDao.getAllBlockedStids()).thenReturn(Cf.set(SupportDaoTest.STID_1, SupportDaoTest.STID_2));
        JsonStringResult result = filesystemActions.blocking();
        Assert.assertContains(result.getResult(), SupportDaoTest.SUPPORT_COMMENT.getDataStids().first());
        Assert.assertContains(result.getResult(), SupportDaoTest.SUPPORT_COMMENT.getDataStids().last());
        Mockito.verify(supportDao).getAllBlockedStids();

        // Second get from cache
        JsonStringResult result1 = filesystemActions.blocking();
        Assert.equals(result, result1);
        Mockito.verify(supportDao, Mockito.times(1)).getAllBlockedStids();
    }
}
