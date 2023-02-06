package ru.yandex.chemodan.app.djfs.migrator;

import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.db.DjfsShardInfo;
import ru.yandex.chemodan.app.djfs.core.db.pg.SharpeiShardResolver;
import ru.yandex.chemodan.app.djfs.core.lock.LockManager;
import ru.yandex.chemodan.app.djfs.core.operations.OperationDao;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationPlan;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsTableMigration;
import ru.yandex.chemodan.app.djfs.migrator.test.BaseDjfsMigratorTest;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;


/**
 * @author yappo
 */
public class DjfsMigratorLocksTest extends BaseDjfsMigratorTest {
    private DjfsMigrator migratorWithMocks;
    private LockManager mockedLockManager;

    @Autowired
    private SharpeiShardResolver sharpeiShardResolver;
    @Autowired
    private OperationDao operationDao;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        loadUserForMigrationData();
        mockedLockManager = Mockito.mock(LockManager.class);
        migratorWithMocks = new DjfsMigrator(
                migratorConfigFactory,
                mockedLockManager,
                sharpeiClient,
                sharpeiShardResolver,
                operationDao
        );
    }

    @Test
    public void testLocksOnSourceUserOnSuccessMigration() {
        migratorWithMocks.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID,
                false, DjfsMigrationPlan.allTablesMigrations
        );

        Mockito.verify(mockedLockManager).lockForMigration(Mockito.eq(UID), src(), Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());
        Mockito.verify(mockedLockManager).updateLockForMigration(Mockito.eq(UID), src(), Mockito.eq(DjfsMigrator.MigrationLock.LOCK_SOURCE_SHARD_AFTER_MIGRATION_FOR),
                Mockito.anyString());
    }

    @Test
    public void testLocksOnDestinationUserOnSuccessMigration() {
        migratorWithMocks.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID,
                false, DjfsMigrationPlan.allTablesMigrations
        );

        Mockito.verify(mockedLockManager).lockForMigration(Mockito.eq(UID), dst(), Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());
        Mockito.verify(mockedLockManager).unlockForMigration(Mockito.eq(UID), dst(), Mockito.anyString());
    }

    @Test
    public void testUnlockUserOnSourceOnCopyFail() {
        DjfsMigrator.CopyResult result = migratorWithMocks.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID,
                false, DjfsMigrationPlan.builder().migration(new FakeMigration() {
                    @Override
                    public void runCopying(DjfsCopyConfiguration migrationConf, PgSchema databaseSchema,
                            Runnable callback)
                    {
                        throw new IllegalStateException();
                    }
                }).build());
        Assert.equals(DjfsMigrationState.EXCEPTION_ON_COPYING, result.getState());

        Mockito.verify(mockedLockManager).lockForMigration(Mockito.eq(UID), src(),  Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());
        Mockito.verify(mockedLockManager).unlockForMigration(Mockito.eq(UID), src(), Mockito.anyString());
    }

    @Test
    public void testUnlockUserOnDestinationOnCopyFail() {
        DjfsMigrator.CopyResult result = migratorWithMocks.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID,
                false, DjfsMigrationPlan.builder().migration(new FakeMigration() {
                    @Override
                    public void runCopying(DjfsCopyConfiguration migrationConf, PgSchema databaseSchema,
                            Runnable callback)
                    {
                        throw new IllegalStateException();
                    }
                }).build());

        Assert.equals(DjfsMigrationState.EXCEPTION_ON_COPYING, result.getState());

        Mockito.verify(mockedLockManager).lockForMigration(Mockito.eq(UID), dst(),  Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());
        Mockito.verify(mockedLockManager).unlockForMigration(Mockito.eq(UID), dst(), Mockito.anyString());
    }

    @Test
    public void testUnlockUserOnCheckDataFail() {
        DjfsMigrator.CopyResult result = migratorWithMocks.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID,
                false, DjfsMigrationPlan.builder().migration(new FakeMigration() {
                    @Override
                    public void checkAllCopied(DjfsCopyConfiguration migrationConf,
                            PgSchema sourceSchema) {
                        throw new IllegalStateException();
                    }
                }).build());
        Assert.equals(DjfsMigrationState.EXCEPTION_ON_COPYING, result.getState());

        Mockito.verify(mockedLockManager).lockForMigration(Mockito.eq(UID), src(), Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());
        Mockito.verify(mockedLockManager).unlockForMigration(Mockito.eq(UID), src(), Mockito.anyString());
    }

    @Test
    public void testNotLockUserOnCleanDestinationFail() {
        DjfsMigrator.CopyResult result = migratorWithMocks.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID,
                false, DjfsMigrationPlan.builder().migration(new FakeMigration() {
                    @Override
                    public void cleanData(JdbcTemplate3 shard, DjfsUid uid, int batchSize) {
                        throw new IllegalStateException();
                    }
                }).build());
        Assert.equals(DjfsMigrationState.EXCEPTION_ON_COPYING, result.getState());

        Mockito.verify(mockedLockManager, Mockito.never()).lockForMigration(Mockito.eq(UID), src(), Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());
        Mockito.verify(mockedLockManager, Mockito.never()).unlockForMigration(Mockito.eq(UID), src(), Mockito.anyString());
    }

    @Test
    public void testUpdateLockOnCopying() {
        migratorWithMocks.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID,
                false, DjfsMigrationPlan.builder().migration(new FakeMigration(){
                    @Override
                    public void runCopying(DjfsCopyConfiguration migrationConf, PgSchema databaseSchema,
                            Runnable callback) {
                        DateTimeUtils.setCurrentMillisFixed(
                                Instant.now()
                                        .plus(DjfsMigrator.MigrationLock.UPDATE_LOCK_AFTER)
                                        .plus(Duration.standardSeconds(1))
                                        .getMillis()
                        );
                        callback.run();
                    }
                }).build()
        );

        Mockito.verify(mockedLockManager).updateLockForMigration(Mockito.eq(UID), src(), Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());
        Mockito.verify(mockedLockManager).updateLockForMigration(Mockito.eq(UID), dst(), Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());
    }

    @Test
    public void testNotUpdateLockOnShortCopying() {
        migratorWithMocks.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID,
                false, DjfsMigrationPlan.builder().migration(new FakeMigration(){
                    @Override
                    public void runCopying(DjfsCopyConfiguration migrationConf, PgSchema databaseSchema,
                            Runnable callback) {
                        DateTimeUtils.setCurrentMillisFixed(
                                Instant.now()
                                        .plus(Duration.standardSeconds(1))
                                        .getMillis()
                        );
                        callback.run();
                    }
                }).build()
        );

        Mockito.verify(mockedLockManager, Mockito.never()).updateLockForMigration(Mockito.eq(UID), src(), Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());

        Mockito.verify(mockedLockManager, Mockito.never()).updateLockForMigration(Mockito.eq(UID), dst(), Mockito.eq(DjfsMigrator.MigrationLock.LOCK_FOR),
                Mockito.anyString());
    }

    private static DjfsShardInfo.Pg dst() {
        return Mockito.eq(new DjfsShardInfo.Pg(DST_PG_SHARD_ID));
    }

    private static DjfsShardInfo.Pg src() {
        return Mockito.eq(new DjfsShardInfo.Pg(SRC_PG_SHARD_ID));
    }

    private abstract static class FakeMigration implements DjfsTableMigration {
        @Override
        public void runCopying(DjfsCopyConfiguration migrationConf, PgSchema databaseSchema,
                Runnable callback) {
        }

        @Override
        public void checkAllCopied(DjfsCopyConfiguration migrationConf,
                PgSchema sourceSchema) {
        }

        @Override
        public void cleanData(JdbcTemplate3 shard, DjfsUid uid, int batchSize) {
        }

        @Override
        public ListF<String> tables() {
            return DjfsMigrationPlan.allTablesMigrations.getTablesOrder().plus(DjfsMigrationPlan.allTablesMigrations.getIgnore());
        }
    }
}
