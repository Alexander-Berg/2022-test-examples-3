package ru.yandex.chemodan.app.djfs.migrator;

import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.djfs.core.db.DjfsShardInfo;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationPlan;
import ru.yandex.chemodan.app.djfs.migrator.test.BaseDjfsMigratorTest;
import ru.yandex.misc.test.Assert;


/**
 * @author yappo
 */
public class DjfsMigratorTest extends BaseDjfsMigratorTest {
    @Override
    @Before
    public void setUp() {
        super.setUp();
        loadUserForMigrationData();
        loadSampleUsers();
    }

    @Test
    public void shouldReturnSpecialResultIfUserAlreadyOnDestinationShard() {
        DjfsMigrator.CopyResult result =
                migrator.copyDataAndSwitchShard(UID, DST_PG_SHARD_ID, SRC_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);
        Assert.equals(DjfsMigrationState.ALREADY_MIGRATED, result.getState());
    }

    @Test
    public void shouldNotCopyFromShardThatNotCurrentForUser() {
        Assert.assertThrows(
                () -> migrator.copyDataAndSwitchShard(UID, 123, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations),
                AssertionError.class
        );
    }

    @Test
    public void shouldNotCleanDataFromShardThatCurrentForUser() {
        Assert.assertThrows(
                () -> migrator.cleanData(UID, SRC_PG_SHARD_ID, Cf.list(), DjfsMigrationPlan.allTablesMigrations),
                AssertionError.class
        );
    }

    @Test
    public void shouldNotCopyOnSameShard() {
        Assert.assertThrows(
                () -> migrator.copyDataAndSwitchShard(
                    UID, DST_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations
                ),
                AssertionError.class
        );
    }

    @Test
    public void shouldNotMigrateIfUserOnSourceShardIsLockedForMigration() {
        lockManager.lockForMigration(UID, new DjfsShardInfo.Pg(SRC_PG_SHARD_ID), Duration.standardHours(2), "");
        DjfsMigrator.CopyResult result = migrator.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations
        );
        Assert.equals(DjfsMigrationState.USER_ALREADY_LOCKED_FOR_MIGRATION, result.getState());

        Assert.isTrue(lockManager.isLockedForMigration(UID, new DjfsShardInfo.Pg(SRC_PG_SHARD_ID)));
        DateTimeUtils.setCurrentMillisFixed(Instant.now().plus(Duration.standardHours(1)).getMillis());
        Assert.isTrue(lockManager.isLockedForMigration(UID, new DjfsShardInfo.Pg(SRC_PG_SHARD_ID)));
    }

    @Test
    public void shouldNotMigrateIfUserOnDestinationShardIsLockedForMigration() {
        lockManager.lockForMigration(UID, new DjfsShardInfo.Pg(DST_PG_SHARD_ID), Duration.standardHours(2), "");
        DjfsMigrator.CopyResult result = migrator.copyDataAndSwitchShard(
                UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations
        );
        Assert.equals(DjfsMigrationState.EXCEPTION_ON_COPYING, result.getState());

        Assert.isFalse(lockManager.isLockedForMigration(UID, new DjfsShardInfo.Pg(SRC_PG_SHARD_ID)));
        Assert.isTrue(lockManager.isLockedForMigration(UID, new DjfsShardInfo.Pg(DST_PG_SHARD_ID)));
        DateTimeUtils.setCurrentMillisFixed(Instant.now().plus(Duration.standardHours(1)).getMillis());
        Assert.isTrue(lockManager.isLockedForMigration(UID, new DjfsShardInfo.Pg(DST_PG_SHARD_ID)));
    }

    @Test
    public void testSwitchShard() {
        migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);
        Assert.equals(DST_PG_SHARD_ID, sharpeiShardResolver.shardByUid(UID).get().getShardId());
    }
}
