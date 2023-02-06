package ru.yandex.chemodan.app.djfs.core.lock;

import java.util.UUID;

import org.joda.time.Duration;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.djfs.core.db.DjfsShardInfo;
import ru.yandex.chemodan.app.djfs.core.db.pg.SharpeiShardResolver;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class LockManagerTest extends DjfsSingleUserTestBase {
    @Autowired
    private FilesystemLockDao filesystemLockDao;

    @Autowired
    private LockManager lockManager;
    @Autowired
    private SharpeiShardResolver sharpeiShardResolver;

    private static DjfsResourcePath PATH = DjfsResourcePath.cons(UID, "/disk/random");
    private static DjfsResourcePath SIMILAR_PATH = DjfsResourcePath.cons(UID, "/disk/random2");

    private static DjfsResourcePath PARENT_PATH = DjfsResourcePath.cons(UID, "/disk/parent");
    private static DjfsResourcePath CHILD_PATH = DjfsResourcePath.cons(UID, "/disk/parent/child");

    @Test
    public void pgMigrationLock() {
        Assert.isFalse(lockManager.isLocked(UID));
        DjfsShardInfo.Pg userShard = new DjfsShardInfo.Pg(sharpeiShardResolver.shardByUid(UID).get().getShardId());
        String ownerMark = UUID.randomUUID().toString();
        lockManager.lockForMigration(UID, userShard, Duration.standardMinutes(5), ownerMark);
        Assert.isTrue(lockManager.isLocked(UID));
        lockManager.unlockForMigration(UID, userShard, ownerMark);
        Assert.isFalse(lockManager.isLocked(UID));
    }

    @Test
    public void canNotGetLockTwice() {
        Assert.isFalse(lockManager.isLocked(UID));
        DjfsShardInfo.Pg userShard = new DjfsShardInfo.Pg(sharpeiShardResolver.shardByUid(UID).get().getShardId());
        lockManager.lockForMigration(UID, userShard, Duration.standardMinutes(5), UUID.randomUUID().toString());
        Assert.assertThrows(
                () -> lockManager.lockForMigration(UID, userShard, Duration.standardMinutes(5), UUID.randomUUID().toString()),
                IllegalStateException.class,
                e -> e.getMessage().equals("user already locked for migration")
        );
    }

    @Test
    public void canNotUnlockNotOwnedLock() {
        Assert.isFalse(lockManager.isLocked(UID));
        DjfsShardInfo.Pg userShard = new DjfsShardInfo.Pg(sharpeiShardResolver.shardByUid(UID).get().getShardId());
        String owner = UUID.randomUUID().toString();
        lockManager.lockForMigration(UID, userShard, Duration.standardMinutes(5), owner);
        String anotherBackend = UUID.randomUUID().toString();
        Assert.assertThrows(
                () -> lockManager.unlockForMigration(UID, userShard, anotherBackend),
                IllegalStateException.class,
                e -> e.getMessage().equals("can not delete lock. Lock owner is different or lock expired")
        );
    }

    @Test
    public void lockForMigrationShouldRestrictUserActions() {
        DjfsShardInfo.Pg userShard = new DjfsShardInfo.Pg(sharpeiShardResolver.shardByUid(UID).get().getShardId());
        String ownerMark = UUID.randomUUID().toString();
        lockManager.lockForMigration(UID, userShard, Duration.standardMinutes(5), ownerMark);

        DjfsResourcePath filePath = DjfsResourcePath.cons(UID, "/disk/test.jpg");

        Assert.assertThrows(() -> filesystem.createFile(DjfsPrincipal.cons(UID), filePath), Exception.class);
    }

    @Test
    public void pathLock() {
        Assert.isFalse(lockManager.isLocked(PATH));
        lockManager.lock(PATH);
        Assert.isTrue(lockManager.isLocked(PATH));
        lockManager.unlock(PATH);
        Assert.isFalse(lockManager.isLocked(PATH));
    }

    @Test
    public void parentPathLock() {
        Assert.isFalse(lockManager.isLocked(CHILD_PATH));
        lockManager.lock(PARENT_PATH);
        Assert.isTrue(lockManager.isLocked(CHILD_PATH));
        lockManager.unlock(PARENT_PATH);
        Assert.isFalse(lockManager.isLocked(CHILD_PATH));
    }

    @Test
    public void childPathLock() {
        Assert.isFalse(lockManager.isLocked(PARENT_PATH));
        lockManager.lock(CHILD_PATH);
        Assert.isTrue(lockManager.isLocked(PARENT_PATH));
        lockManager.unlock(CHILD_PATH);
        Assert.isFalse(lockManager.isLocked(PARENT_PATH));
    }

    @Test
    public void similarPathLock() {
        Assert.isFalse(lockManager.isLocked(PATH));
        lockManager.lock(SIMILAR_PATH);
        Assert.isFalse(lockManager.isLocked(PATH));
    }

    @Test
    public void lockingSamePathTwiceThrowsException() {
        lockManager.lock(PATH);
        Assert.assertThrows(() -> lockManager.lock(PATH), ResourceLockedException.class);
    }

    @Test
    public void unlockSamePathTwice() {
        lockManager.unlock(PATH);
        lockManager.unlock(PATH);
    }

    @Test
    public void lockWithLockerId() {
        Assert.isTrue(lockManager.tryAcquireOrRenewLock("id", "oid", "op_type", PATH));
        Assert.isTrue(lockManager.isLocked(PATH));
        FilesystemLock lock = filesystemLockDao.find(PATH).get();
        Assert.some("id", lock.getLockerId());
        Assert.some("oid", lock.getOperationId());
        Assert.some("op_type", lock.getOperationType());
    }

    @Test
    public void lockSamePathWithSameLockerId() {
        Assert.isTrue(lockManager.tryAcquireOrRenewLock("id", "oid1", "op_type1", PATH));
        Assert.isTrue(lockManager.isLocked(PATH));
        Assert.isTrue(lockManager.tryAcquireOrRenewLock("id", "oid1", "op_type1", PATH));
        Assert.isTrue(lockManager.tryAcquireOrRenewLock("id", "oid1", "op_type1", PATH));
        Assert.isTrue(lockManager.tryAcquireOrRenewLock("id", "oid2", "op_type2", PATH));
        FilesystemLock lock = filesystemLockDao.find(PATH).get();
        Assert.some("id", lock.getLockerId());
        Assert.some("oid2", lock.getOperationId());
        Assert.some("op_type2", lock.getOperationType());
    }

    @Test
    public void lockSamePathWithDifferentLockerIdFails() {
        Assert.isTrue(lockManager.tryAcquireOrRenewLock("id", "oid", "op_type", PATH));
        Assert.isTrue(lockManager.isLocked(PATH));
        Assert.isFalse(lockManager.tryAcquireOrRenewLock("id1", "oid1", "op_type1", PATH));
        Assert.isFalse(lockManager.tryAcquireOrRenewLock("id2", "oid2", "op_type2", PATH));
    }

    @Test
    public void lockLockedPathWithLockerIdFails() {
        lockManager.lock(PATH);
        Assert.isTrue(lockManager.isLocked(PATH));
        Assert.isFalse(lockManager.tryAcquireOrRenewLock("id", "oid", "op_type", PATH));
    }

    @Test
    public void unlockLockedPathWithoutLockerIdFails() {
        lockManager.lock(PATH);
        Assert.isTrue(lockManager.isLocked(PATH));
        lockManager.unlock("id", PATH);
        Assert.isTrue(lockManager.isLocked(PATH));
    }

    @Test
    public void unlockLockedPathDifferentLockerIdFails() {
        lockManager.tryAcquireOrRenewLock("id1", "oid", "op_type", PATH);
        Assert.isTrue(lockManager.isLocked(PATH));
        lockManager.unlock("id", PATH);
        Assert.isTrue(lockManager.isLocked(PATH));
    }

    @Test
    public void unlockLockedPathWithSameLockerId() {
        lockManager.tryAcquireOrRenewLock("id", "oid", "op_type", PATH);
        Assert.isTrue(lockManager.isLocked(PATH));
        lockManager.unlock("id", PATH);
        Assert.isFalse(lockManager.isLocked(PATH));
    }
}
