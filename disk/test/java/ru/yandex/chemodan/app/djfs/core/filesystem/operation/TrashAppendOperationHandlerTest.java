package ru.yandex.chemodan.app.djfs.core.filesystem.operation;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mongodb.ReadPreference;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.OperationCallbackHandler;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.trashappend.TrashAppendOperation;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.trashappend.TrashAppendOperationData;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.trashappend.TrashAppendOperationHandler;
import ru.yandex.chemodan.app.djfs.core.operations.MpfsOperationHandler;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSinglePostgresUserTestBase;
import ru.yandex.misc.test.Assert;


public class TrashAppendOperationHandlerTest extends DjfsSinglePostgresUserTestBase {

    @Autowired
    private TrashAppendOperationHandler handler;

    @Test
    public void trashAppendFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, folderPath);

        Operation operation = TrashAppendOperation.create(UID, folderPath, folder, "", 123);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = handler.handle(operation, new AtomicBoolean());
        Assert.assertEquals(MpfsOperationHandler.Status.DONE, status);

        Option<DjfsResource> source = filesystem.find(PRINCIPAL, folderPath, Option.of(ReadPreference.primary()));
        Assert.assertEmpty(source);
    }

    @Test
    public void testTrashAppendWithCallback() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, folderPath);

        String callbackUrl = "http://your-ad-here.nowhere";

        Operation operation = TrashAppendOperation.create(UID, folderPath, folder, callbackUrl, 123);
        operationDao.insert(operation);

        handler.handle(operation, new AtomicBoolean());

        Assert.sizeIs(1,
                mockCeleryTaskManager.submitted.filter(x -> x.task == OperationCallbackHandler.CALL_URL_TASK_ID));
    }

    @Test
    public void exceptionInsideTrashAppend() {
        makeQuickMoveUser(UID);

        DjfsResourcePath filePath = DjfsResourcePath.cons(UID, "/disk/file.txt");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, filePath);

        Operation operation = TrashAppendOperation.create(UID, filePath, file, "", 123);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = handler.handle(operation, new AtomicBoolean());

        Assert.assertEquals(MpfsOperationHandler.Status.FAIL, status);

        Option<Operation> operationO = operationDao.find(UID, operation.getId());
        Assert.some(operationO);
        Operation failedOperation = operationO.get();

        TrashAppendOperationData data = failedOperation.getData(TrashAppendOperationData.B);
        Assert.some(data.getError());
    }
}
