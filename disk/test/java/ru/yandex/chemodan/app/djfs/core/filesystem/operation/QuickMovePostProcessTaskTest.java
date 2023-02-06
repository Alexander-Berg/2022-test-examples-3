package ru.yandex.chemodan.app.djfs.core.filesystem.operation;

import java.util.concurrent.ThreadLocalRandom;

import com.mongodb.ReadPreference;
import org.joda.time.Instant;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.MediaType;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.postprocess.QuickMovePostProcessTask;
import ru.yandex.chemodan.app.djfs.core.globalgallery.DeletionLogEntry;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSinglePostgresUserTestBase;
import ru.yandex.chemodan.app.djfs.core.test.RandomFailingInvocationHandler;
import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.misc.test.Assert;

public class QuickMovePostProcessTaskTest extends DjfsSinglePostgresUserTestBase {

    @Autowired
    private QuickMovePostProcessTask handler;

    private void setParameters(DjfsResourcePath path) {
        QuickMovePostProcessTask task = new QuickMovePostProcessTask(path);
        handler.setParameters(task.getParameters());
    }

    private ExecutionContext mockExecutionContext() {
        ExecutionContext context = Mockito.mock(ExecutionContext.class);
        Mockito.when(context.getExecutionInfo()).thenReturn(Option.empty());
        return context;
    }

    @Test
    public void trashAppendQuotaCalculation() throws Exception {
        DjfsResourcePath folderInTrash = DjfsResourcePath.cons(UID, "/trash/folder");
        filesystem.createFolder(PRINCIPAL, folderInTrash,
                x -> x.trashAppendTime(Instant.now()).trashAppendOriginalPath(
                        folderInTrash.changeArea(DjfsResourceArea.DISK).getPath()));

        int filesCount = 5;
        long totalSize = 0;
        for (int i = 0; i < filesCount; i += 1) {
            DjfsResourcePath filePath = folderInTrash.getChildPath("file-" + i + ".txt");
            int fileSize = ThreadLocalRandom.current().nextInt(0, 1024);
            filesystem.createFile(PRINCIPAL, filePath, x -> x.size(fileSize));
            totalSize += fileSize;
        }

        setParameters(folderInTrash);
        ExecutionContext context = mockExecutionContext();
        handler.execute(context);

        Assert.equals(totalSize, quotaManager.getTrashUsed(UID));
    }

    @Test
    public void trashAppendQuotaCalculationWithDuplicateFileId() throws Exception {
        DjfsResourcePath folderInTrash = DjfsResourcePath.cons(UID, "/trash/folder");
        DjfsResourcePath folderInDisk = DjfsResourcePath.cons(UID, "/disk/folder");

        DjfsFileId folderFileId = DjfsFileId.random();

        filesystem.createFolder(PRINCIPAL, folderInTrash,
                x -> x.trashAppendTime(Instant.now())
                        .trashAppendOriginalPath(folderInDisk.getPath())
                        .fileId(folderFileId));
        filesystem.createFolder(PRINCIPAL, folderInDisk,
                x -> x.fileId(folderFileId));

        int filesCount = 5;
        long totalSize = 0;
        for (int i = 0; i < filesCount; i += 1) {
            DjfsResourcePath filePathInTrash = folderInTrash.getChildPath("file-" + i + ".txt");
            DjfsResourcePath filePathInDisk = folderInDisk.getChildPath("file-" + i + ".txt");

            int fileSize = ThreadLocalRandom.current().nextInt(0, 1024);
            DjfsFileId fileId = DjfsFileId.random();

            filesystem.createFile(PRINCIPAL, filePathInDisk, x -> x.size(fileSize).fileId(fileId));
            filesystem.createFile(PRINCIPAL, filePathInTrash, x -> x.size(fileSize).fileId(fileId));

            totalSize += fileSize;
        }

        setParameters(folderInTrash);
        ExecutionContext context = mockExecutionContext();
        handler.execute(context);

        Assert.equals(totalSize, quotaManager.getTrashUsed(UID));
    }

    @Test
    public void testGlobalDeletionLogAppendForImagesAndVideos() throws Exception {
        DjfsResourcePath folderInTrash = DjfsResourcePath.cons(UID, "/trash/folder");
        DjfsResourcePath subfolderInTrash = folderInTrash.getChildPath("subfolder");

        DjfsResourcePath file1Path = folderInTrash.getChildPath("file-1.txt");
        DjfsResourcePath file2Path = subfolderInTrash.getChildPath("file-2.txt");

        DjfsFileId file1Id = DjfsFileId.random();
        DjfsFileId file2Id = DjfsFileId.random();

        filesystem.createFolder(PRINCIPAL, folderInTrash,
                x -> x.trashAppendTime(Instant.now()).trashAppendOriginalPath(
                        folderInTrash.changeArea(DjfsResourceArea.DISK).getPath()));
        filesystem.createFolder(PRINCIPAL, subfolderInTrash);
        filesystem.createFile(PRINCIPAL, file1Path, x -> x.mediaType(MediaType.IMAGE).fileId(file1Id));
        filesystem.createFile(PRINCIPAL, file2Path, x -> x.mediaType(MediaType.VIDEO).fileId(file2Id));

        setParameters(folderInTrash);
        ExecutionContext context = mockExecutionContext();
        handler.execute(context);

        ListF<DeletionLogEntry> entries = deletionLogDao.findAll(UID);
        Assert.sizeIs(2, entries);
        Assert.sizeIs(2, entries.filter(x -> x.getFileId().equals(file1Id) || x.getFileId().equals(file2Id)));
    }

    @Test
    public void testGlobalDeletionLogAppendForNotImages() throws Exception {
        DjfsResourcePath folderInTrash = DjfsResourcePath.cons(UID, "/trash/folder");
        DjfsResourcePath file1Path = folderInTrash.getChildPath("file-1.txt");

        filesystem.createFolder(PRINCIPAL, folderInTrash,
                x -> x.trashAppendTime(Instant.now()).trashAppendOriginalPath(
                        folderInTrash.changeArea(DjfsResourceArea.DISK).getPath()));
        filesystem.createFile(PRINCIPAL, file1Path, x -> x.mediaType(MediaType.DATA));

        setParameters(folderInTrash);
        ExecutionContext context = mockExecutionContext();
        handler.execute(context);

        ListF<DeletionLogEntry> entries = deletionLogDao.findAll(UID);
        Assert.sizeIs(0, entries);
    }

    @Test
    public void testPublicFields() throws Exception {
        DjfsResourcePath folderInTrash = DjfsResourcePath.cons(UID, "/trash/folder");
        DjfsResourcePath file1Path = folderInTrash.getChildPath("file-1.txt");

        filesystem.createFolder(PRINCIPAL, folderInTrash,
                x -> x.trashAppendTime(Instant.now()).trashAppendOriginalPath(
                        folderInTrash.changeArea(DjfsResourceArea.DISK).getPath()));
        filesystem.createFile(PRINCIPAL, file1Path, x -> x.isPublic(true).downloadCounter(100500));

        setParameters(folderInTrash);
        ExecutionContext context = mockExecutionContext();
        handler.execute(context);

        Option<DjfsResource> fileInTrash = filesystem.find(PRINCIPAL, file1Path, Option.of(ReadPreference.primary()));
        Assert.some(fileInTrash);
        Assert.isTrue(fileInTrash.get().isPublished());
        Assert.isFalse(fileInTrash.get().isPublic());
        Assert.isFalse(fileInTrash.get().getDownloadCounter().isPresent());
    }

    @Test
    public void checkSkipFilesWithTargetArea() throws Exception {
        DjfsResourcePath folderInTrash = DjfsResourcePath.cons(UID, "/trash/folder");
        filesystem.createFolder(PRINCIPAL, folderInTrash,
                x -> x.trashAppendTime(Instant.now()).trashAppendOriginalPath(
                        folderInTrash.changeArea(DjfsResourceArea.DISK).getPath()));

        int filesCount = 5;
        long checkSize = 0;
        for (int i = 0; i < filesCount; i += 1) {
            DjfsResourcePath filePath = folderInTrash.getChildPath("file-" + i + ".txt");
            int fileSize = ThreadLocalRandom.current().nextInt(0, 1024);
            filesystem.createFile(PRINCIPAL, filePath, x -> x.size(fileSize));
            checkSize += fileSize;
        }
        for (int i = 0; i < filesCount; i += 1) {
            DjfsResourcePath filePath = folderInTrash.getChildPath("file-" + i + ".jpg");
            int fileSize = ThreadLocalRandom.current().nextInt(0, 1024);
            filesystem.createFile(PRINCIPAL, filePath, x -> x.size(fileSize).area(DjfsResourceArea.TRASH));
        }

        setParameters(folderInTrash);
        ExecutionContext context = mockExecutionContext();
        handler.execute(context);

        Assert.equals(checkSize, quotaManager.getTrashUsed(UID));
    }

    @Test
    public void continueAfterFailure() throws Exception {
        DjfsResourcePath folderInTrash = DjfsResourcePath.cons(UID, "/trash/folder");
        filesystem.createFolder(PRINCIPAL, folderInTrash,
                x -> x.trashAppendTime(Instant.now()).trashAppendOriginalPath(
                        folderInTrash.changeArea(DjfsResourceArea.DISK).getPath()));

        int filesCount = 100;
        long totalSize = 0;
        for (int i = 0; i < filesCount; i += 1) {
            DjfsResourcePath filePath = folderInTrash.getChildPath("file-" + i + ".jpg");
            int fileSize = ThreadLocalRandom.current().nextInt(0, 1024);
            filesystem.createFile(PRINCIPAL, filePath, x -> x.mediaType(MediaType.IMAGE).size(fileSize));
            totalSize += fileSize;
        }

        randomFailuresProbabilitySource.setFailureProbability(0.1);

        setParameters(folderInTrash);
        for (int attemptNumber = 0; attemptNumber < 20000; ++attemptNumber) {
            try {
                ExecutionContext context = mockExecutionContext();
                handler.execute(context);
            } catch (RandomFailingInvocationHandler.RandomFailureException ignore) {
                continue;
            }

            break;
        }

        randomFailuresProbabilitySource.setFailureProbability(0);
        ListF<DjfsResource> children = djfsResourceDao.find2ImmediateChildren(folderInTrash, filesCount);
        Assert.sizeIs(
                filesCount,
                children.filter(x -> x.getArea().isPresent() && x.getArea().get() == DjfsResourceArea.TRASH)
        );
        Assert.equals(totalSize, quotaManager.getTrashUsed(UID));
    }

    @Test
    public void trashCounterAfterRestoreAndTrashAppend() throws Exception {
        // Сценарий:
        // 1. удаляем в корзину с randomFailuresProbabilitySource, ждем ошибку
        // 2. когда ошибка случается (в середине пост обработки), восстанавливаем папку целиком из корзины
        // 3. дожидаемся, пока таск завершится
        // 4. опять удаляем в корзину
        // 5. проверяем корректные счетчики по корзине

        DjfsResourcePath folderInTrash = DjfsResourcePath.cons(UID, "/trash/folder");
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, folderInTrash, x -> x
                .trashAppendTime(Instant.now())
                .trashAppendOriginalPath(folderInTrash.changeArea(DjfsResourceArea.DISK).getPath())
        );

        int filesCount = 100;
        long totalSize = 0;
        for (int i = 0; i < filesCount; i += 1) {
            DjfsResourcePath filePath = folderInTrash.getChildPath("file-" + i + ".txt");
            int fileSize = ThreadLocalRandom.current().nextInt(0, 1024);
            filesystem.createFile(PRINCIPAL, filePath, x -> x.size(fileSize));
            totalSize += fileSize;
        }

        randomFailuresProbabilitySource.setFailureProbability(0.02);

        setParameters(folderInTrash);

        for (int attemptNumber = 0; attemptNumber < 20000; ++attemptNumber) {
            try {
                ExecutionContext context = mockExecutionContext();
                handler.execute(context);
            } catch (RandomFailingInvocationHandler.RandomFailureException ignore) {
                randomFailuresProbabilitySource.setFailureProbability(0);
                break;
            }

            Assert.fail("RandomFailureException expected here");
        }

        Assert.gt(totalSize, quotaManager.getTrashUsed(UID));

        DjfsResource diskFolder = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK)).get();
        djfsResourceDao.changeParent(UID, folder.getId(), diskFolder.getId());

        ExecutionContext context = mockExecutionContext();
        handler.execute(context);

        DjfsResource trashFolder = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.TRASH)).get();
        djfsResourceDao.changeParent(UID, folder.getId(), trashFolder.getId());

        setParameters(folderInTrash);
        handler.execute(context);

        Assert.equals(totalSize, quotaManager.getTrashUsed(UID));
    }
}
