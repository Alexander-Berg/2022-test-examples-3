package ru.yandex.chemodan.app.djfs.migrator.migrations;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.migrator.DjfsCopyConfiguration;
import ru.yandex.chemodan.app.djfs.migrator.PgSchema;
import ru.yandex.chemodan.app.djfs.migrator.test.BaseDjfsMigratorTest;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

abstract public class AbstractMigrationTypeTest extends BaseDjfsMigratorTest {
    @Override
    @Before
    public void setUp() {
        super.setUp();
        loadUserForMigrationData();
        loadSampleUsers();
    }

    abstract protected DjfsMigrationPlan migrationPlan();

    @Test
    public void testFailCheckAllCopiedBeforeMigration() {
        Assert.assertThrows(this::checkAllCopied, AssertionError.class);
    }

    @Test
    abstract public void testAllTableDataCleaned();

    @Test
    abstract public void testCopyWithMinimalBatchSize();

    @Test
    abstract public void testCheckCopiedWillFailIfSomeRecordIsChanged();

    private void checkAllCopied() {
        DjfsCopyConfiguration migratorConfiguration = migratorConfigFactory
            .copyConfigBuilder(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID)
            .baseBatchSize(1)
            .build();
        PgSchema pgSchema = PgSchema.build(migratorConfiguration.srcShardJdbcTemplate());

        for (DjfsTableMigration migration : migrationPlan().getMigrations()) {
            migration.checkAllCopied(migratorConfiguration, pgSchema);
        }
    }

    protected void testCleanData(Consumer<JdbcTemplate3> check) {
        runCopying(UID);

        cleanData(dstShard());

        check.accept(dstShard());
    }

    private void cleanData(JdbcTemplate3 shard) {
        for (DjfsTableMigration migration : migrationPlan().getMigrations().reverse()) {
            migration.cleanData(shard, UID, 1);
        }
    }

    @SafeVarargs
    protected final void checkAllCopiedMustFailIfChangeSomeRecord(Consumer<JdbcTemplate3>... changes) {
        for (Consumer<JdbcTemplate3> change : changes) {
            runCopying(UID);

            change.accept(dstShard());

            Assert.assertThrows(this::checkAllCopied, AssertionError.class);

            cleanData(dstShard());
        }
    }

    protected void runCopying(DjfsUid uid) {
        runCopying(uid, Function.identityF());
    }

    protected void runCopying(DjfsUid uid,
        Function<DjfsCopyConfiguration.DjfsCopyConfigurationBuilder, DjfsCopyConfiguration.DjfsCopyConfigurationBuilder> configCustomizer) {
        DjfsCopyConfiguration.DjfsCopyConfigurationBuilder builder = migratorConfigFactory
            .copyConfigBuilder(uid, SRC_PG_SHARD_ID, DST_PG_SHARD_ID);
        builder = configCustomizer.apply(builder);
        DjfsCopyConfiguration migratorConfiguration = builder.build();

        PgSchema pgSchema = PgSchema.build(migratorConfiguration.srcShardJdbcTemplate());
        for (DjfsTableMigration migration : migrationPlan().getMigrations()) {
            migration.runCopying(migratorConfiguration, pgSchema, () -> {});
        }
    }
}
