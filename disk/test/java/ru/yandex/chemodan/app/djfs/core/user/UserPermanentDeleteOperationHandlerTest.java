package ru.yandex.chemodan.app.djfs.core.user;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mongodb.ReadPreference;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class UserPermanentDeleteOperationHandlerTest extends DjfsSingleUserTestBase {
    @Autowired
    private UserPermanentDeleteOperationHandler sut;

    private Operation getOperation() {
        Operation operation = Operation.builder()
                .id("id")
                .uid(UID)
                .type("type")
                .subtype("subtype")
                .state(Operation.State.WAITING)
                .ctime(Instant.now())
                .mtime(Instant.now())
                .dtime(Instant.now())
                .version(1)
                .jsonData(JsonObject.empty())
                .build();
        operationDao.insert(operation);
        return operation;
    }

    @Test
    public void handleForEmptyUser() {
        Operation operation = getOperation();
        sut.handle(operation, new AtomicBoolean());
    }

    @Test
    public void handleRootFolderWithoutFileId() {
        Operation operation = getOperation();

        djfsResourceDao.removeFileId(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK));

        sut.handle(operation, new AtomicBoolean());
    }

    @Test
    public void handle() {
        Operation operation = getOperation();

        util.fs.initialize(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK), 2, 2, 2);
        filesystem.createFolder(DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(UID, "/trash/rnd"));
        filesystem.createFolder(DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(UID, "/attach/YaFotki"));
        filesystem.createFile(DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(UID, "/attach/YaFotki/1.png"));

        sut.handle(operation, new AtomicBoolean());

        String hiddenRoot = "/hidden/" + operationDao.find(UID, "id").get()
                .getData(UserPermanentDeleteOperationHandler.Data.B).getHiddenRootFolderName().get();
        DjfsResourcePath hiddenRootPath = DjfsResourcePath.cons(UID, hiddenRoot);

        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath, Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/d0"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/d1"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("trash/rnd"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("attach/YaFotki"), Option.of(ReadPreference.primary())));

        ListF rms = mpfsClient.getCallParameters("rm");
        Assert.assertListsEqual(Cf.list(
                Tuple2.tuple(UID.asMpfsUser(), "/disk/d0/d0"),
                Tuple2.tuple(UID.asMpfsUser(), "/disk/d0/d1"),
                Tuple2.tuple(UID.asMpfsUser(), "/disk/d0"),
                Tuple2.tuple(UID.asMpfsUser(), "/disk/d1/d0"),
                Tuple2.tuple(UID.asMpfsUser(), "/disk/d1/d1"),
                Tuple2.tuple(UID.asMpfsUser(), "/disk/d1"),
                Tuple2.tuple(UID.asMpfsUser(), "/trash/rnd"),
                Tuple2.tuple(UID.asMpfsUser(), "/attach/YaFotki")
        ), rms);

        ListF moves = mpfsClient.getCallParameters("move");
        Assert.sizeIs(0, moves);
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/d0/f0"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/d0/f1"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/d1/f0"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/d1/f1"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/f0"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/f1"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("attach/YaFotki/1.png"), Option.of(ReadPreference.primary())));
    }

    @Test
    public void handleRestrictedFolders() {
        Operation operation = getOperation();

        FolderDjfsResource restrictedFolder_1 = FolderDjfsResource.cons(UID, "/disk/..");
        FolderDjfsResource folder = FolderDjfsResource.cons(UID, "/disk/folder");
        FolderDjfsResource restrictedFolder_2 = FolderDjfsResource.cons(folder, ".");
        FileDjfsResource file1 = FileDjfsResource.random(restrictedFolder_1, "file1");

        djfsResourceDao.insert(UID, restrictedFolder_1, folder, restrictedFolder_2);
        djfsResourceDao.insert(UID, file1);

        sut.handle(operation, new AtomicBoolean());

        String hiddenRoot = "/hidden/" + operationDao.find(UID, "id").get()
                .getData(UserPermanentDeleteOperationHandler.Data.B).getHiddenRootFolderName().get();
        DjfsResourcePath hiddenRootPath = DjfsResourcePath.cons(UID, hiddenRoot);

        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath, Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/.."), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/folder"), Option.of(ReadPreference.primary())));
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/folder/."), Option.of(ReadPreference.primary())));

        ListF<Tuple2> rms = mpfsClient.getCallParameters("rm");
        ListF<Tuple2> expectedFolderRms = Cf.list(
                Tuple2.tuple(UID.asMpfsUser(), "/disk/.."),
                Tuple2.tuple(UID.asMpfsUser(), "/disk/folder"),
                Tuple2.tuple(UID.asMpfsUser(), "/disk/folder/.")
        );
        Assert.assertListsEqual(expectedFolderRms, rms.sortedBy(Tuple2::get2));

        ListF moves = mpfsClient.getCallParameters("move");
        Assert.sizeIs(0, moves);
        Assert.some(filesystem.find(DjfsPrincipal.SYSTEM, hiddenRootPath.getMultipleChildPath("disk/../file1"), Option.of(ReadPreference.primary())));
    }
}
