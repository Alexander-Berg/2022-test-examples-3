package ru.yandex.chemodan.app.djfs.migrator;

import java.util.concurrent.Semaphore;

import lombok.SneakyThrows;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.trashappend.TrashAppendOperation;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationPlan;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationUtil;
import ru.yandex.chemodan.app.djfs.migrator.test.BaseDjfsMigratorTest;
import ru.yandex.misc.test.Assert;

public class DjfsMigratorOperationsCheckTest extends BaseDjfsMigratorTest {
    @Override
    @Before
    public void setUp() {
        super.setUp();
        loadUserForMigrationData();
        loadSampleUsers();
    }

    @SneakyThrows
    @Test
    public void mustFailIfExistsOperation() {
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID, "/disk/file.txt");
        FileDjfsResource file = filesystem.createFile(DjfsPrincipal.cons(UID), filePath);

        Semaphore semaphore = new Semaphore(0);

        Thread insertOperation = new Thread(() -> DjfsMigrationUtil.doInTransaction(srcShard().getDataSource(), () -> {
            Operation operation = TrashAppendOperation.create(UID, filePath, file, "", 123);
            operationDao.insert(operation);
            semaphore.release();
            sleep();
        }));
        insertOperation.start();

        try {
            semaphore.acquire();
            DjfsMigrator.CopyResult result =
                migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations
                );
            Assert.equals(DjfsMigrationState.USER_IS_ACTIVE, result.getState());
            Assert.equals("user has active operations", result.getMessage());
        } finally {
            insertOperation.join();
        }
    }

    @Test
    public void mustFailIfExistsResentOperations() {
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID, "/disk/file.txt");
        FileDjfsResource file = filesystem.createFile(DjfsPrincipal.cons(UID), filePath);

        Operation operation = TrashAppendOperation.create(UID, filePath, file, "", 123);
        operationDao.insert(operation);
        operationDao.changeState(UID, operation.getId(), Operation.State.WAITING, Operation.State.COMPLETED);
        operationDao.setDtime(UID, operation.getId(), Instant.now());

        DjfsMigrator.CopyResult result =
            migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);
        Assert.equals(DjfsMigrationState.USER_IS_ACTIVE, result.getState());
        Assert.equals("user has recent active operations", result.getMessage());

        Duration spendTime = DjfsMigrator.RESENT_OPERATIONS_FAIL_DURATION.multipliedBy(2);
        DateTimeUtils.setCurrentMillisFixed(Instant.now().plus(spendTime).getMillis());

        Assert.equals(
            DjfsMigrationState.COPY_SUCCESS,
            migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations).getState()
        );
    }

    @Test
    public void mustIgnoreUserActiveOperationIfForceFlag() {
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID, "/disk/file.txt");
        FileDjfsResource file = filesystem.createFile(DjfsPrincipal.cons(UID), filePath);

        Operation operation = TrashAppendOperation.create(UID, filePath, file, "", 123);
        operationDao.insert(operation);
        operationDao.changeState(UID, operation.getId(), Operation.State.WAITING, Operation.State.COMPLETED);
        operationDao.setDtime(UID, operation.getId(), Instant.now());

        Assert.equals(
            DjfsMigrationState.COPY_SUCCESS,
            migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, true, DjfsMigrationPlan.allTablesMigrations).getState()
        );
    }

    @SneakyThrows
    private void sleep() {
        Thread.sleep(100);
    }
}
