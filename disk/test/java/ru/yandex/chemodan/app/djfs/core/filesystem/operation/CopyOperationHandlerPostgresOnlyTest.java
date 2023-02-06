package ru.yandex.chemodan.app.djfs.core.filesystem.operation;

import java.util.concurrent.atomic.AtomicBoolean;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.PhotosliceAlbumType;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.copy.CopyOperation;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.copy.CopyOperationHandler;
import ru.yandex.chemodan.app.djfs.core.operations.MpfsOperationHandler;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSinglePostgresUserTestBase;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author yappo
 */
public class CopyOperationHandlerPostgresOnlyTest extends DjfsSinglePostgresUserTestBase {

    @Autowired
    private CopyOperationHandler sut;

    @Test
    public void copyPhotosliceAlbumType() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x -> x.photosliceAlbumType(PhotosliceAlbumType.CAMERA));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();
        Assert.equals(PhotosliceAlbumType.CAMERA, destination.getPhotosliceAlbumType().get());
    }
}
