package ru.yandex.chemodan.app.djfs.core.filesystem.operation;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mongodb.ReadPreference;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.OperationCallbackHandler;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.move.MoveOperation;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.move.MoveOperationData;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.move.MoveOperationHandler;
import ru.yandex.chemodan.app.djfs.core.operations.MpfsOperationHandler;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSinglePostgresUserTestBase;
import ru.yandex.misc.test.Assert;


public class MoveOperationHandlerTest extends DjfsSinglePostgresUserTestBase {

    @Autowired
    private MoveOperationHandler handler;

    @Test
    public void commonFolderMove() {
        makeQuickMoveUser(UID);

        DjfsResourcePath srcFolder = DjfsResourcePath.cons(UID, "/disk/folder");
        DjfsResourcePath dstFolder = DjfsResourcePath.cons(UID, "/disk/new");
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, srcFolder);

        Operation operation = MoveOperation.create(UID, srcFolder, folder, dstFolder, false, "",
                123);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = handler.handle(operation, new AtomicBoolean());
        Assert.assertEquals(MpfsOperationHandler.Status.DONE, status);

        Option<DjfsResource> source = filesystem.find(PRINCIPAL, srcFolder, Option.of(ReadPreference.primary()));
        Option<DjfsResource> destination = filesystem.find(PRINCIPAL, dstFolder, Option.of(ReadPreference.primary()));
        Assert.assertEmpty(source);
        Assert.some(destination);
    }

    @Test
    public void testAsyncMoveWithCallback() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        FolderDjfsResource srcFolder = filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/new");

        String callbackUrl = "http://your-ad-here.nowhere";

        Operation operation = MoveOperation.create(UID, folderPath, srcFolder, destinationPath, false,
                callbackUrl, 123);
        operationDao.insert(operation);

        handler.handle(operation, new AtomicBoolean());

        Assert.sizeIs(1,
                mockCeleryTaskManager.submitted.filter(x -> x.task == OperationCallbackHandler.CALL_URL_TASK_ID));
    }

    @Test
    public void exceptionInsideMoveFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        FolderDjfsResource srcFolder = filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/existing");
        filesystem.createFolder(PRINCIPAL, destinationPath);

        Operation operation = MoveOperation.create(UID, folderPath, srcFolder, destinationPath, false,
                "", 123);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = handler.handle(operation, new AtomicBoolean());

        Assert.assertEquals(MpfsOperationHandler.Status.FAIL, status);

        Option<Operation> operationO = operationDao.find(UID, operation.getId());
        Assert.some(operationO);
        Operation failedOperation = operationO.get();

        MoveOperationData data = failedOperation.getData(MoveOperationData.B);
        Assert.some(data.getError());
    }
}
