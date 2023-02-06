package ru.yandex.chemodan.app.djfs.migrator.migrations;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.djfs.core.db.DjfsShardInfo;
import ru.yandex.chemodan.app.djfs.migrator.test.BaseDjfsMigratorTest;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

public class DjfsMigrationPlanTest extends BaseDjfsMigratorTest {
    public static final String getAllTablesQuery =
            "SELECT table_name FROM information_schema.tables WHERE table_schema='disk'";

    private static final String getAllForeignKeyRelations = ""
            + "SELECT table_name, array_agg(foreign_table_name) as dependencies "
            + "FROM ( "
            + "    SELECT "
            + "         distinct split_part(r.conrelid::regclass::text, '.', 2) AS table_name , "
            + "         split_part(r.confrelid::regclass::text, '.', 2) AS foreign_table_name"
            + "    FROM pg_catalog.pg_constraint r"
            + "    WHERE r.contype = 'f'"
            + ") relations "
            + "GROUP BY table_name";

    private MapF<String, ListF<String>> getTableDependencies() {
        JdbcTemplate3 shard = pgShardResolver.resolve(new DjfsShardInfo.Pg(PG_SHARD_1));
        return Cf.toMap(shard.query(
                getAllForeignKeyRelations,
                (rs, rowNum) -> new Tuple2<>(
                        rs.getString("table_name"),
                        Cf.arrayList((String[]) rs.getArray("dependencies").getArray())
                )
        ));
    }

    @Override
    @Before
    public void setUp() {
        super.setUp();
        loadUserForMigrationData();
    }

    private ListF<String> checkTableDependencies(
            DjfsMigrationPlan migrationPlan,
            ListF<Tuple2<String, String>> exclusions
    ) {
        MapF<String, ListF<String>> exclusionsMap = exclusions.groupByMapValues(Tuple2::get1, Tuple2::get2);

        ListF<String> tableOrder = migrationPlan.getTablesOrder();
        MapF<String, ListF<String>> tableDependencies = getTableDependencies();

        ListF<String> errors = Cf.list();
        for (int i = 0; i < tableOrder.size(); i++) {
            String table = tableOrder.get(i);
            SetF<String> dependsOn = tableDependencies.getOrElse(table, Cf.list()).unique().minus1(table);
            for (String dependsOnTable : dependsOn) {
                if (!tableOrder.subList(0, i).containsTs(dependsOnTable)) {
                    if (!exclusionsMap.getOrElse(table, Cf.list()).containsTs(dependsOnTable)) {
                        errors = errors.plus(table + " must be after " + dependsOnTable + " because of foreign key");
                    }
                }
            }
        }
        return errors;
    }

    @Test
    public void checkAllMigratedTablesHaveSampleData() {
        SetF<String> migratedTables = DjfsMigrationPlan.allTablesMigrations.getTablesOrder().unique();

        JdbcTemplate3 shard = pgShardResolver.resolve(UID);
        SetF<String> tablesWithoutSampleData = migratedTables
                .filter(table -> shard.queryForObject("SELECT NOT exists(SELECT * FROM disk." + table + ")", Boolean.class));

        Assert.isEmpty(tablesWithoutSampleData, "see sample_database.sql, use real data please, it's important");
    }

    @Test
    public void checkAllTablesInMigrations() {
        SetF<String> migratedTables = DjfsMigrationPlan.allTablesMigrations.getTablesOrder().unique();
        SetF<String> ignoredTables = DjfsMigrationPlan.allTablesMigrations.getIgnore().unique();
        SetF<String> tablesInConfig = migratedTables.plus(ignoredTables);

        JdbcTemplate3 shard = pgShardResolver.resolve(new DjfsShardInfo.Pg(PG_SHARD_1));
        SetF<String> tablesInDb = shard.query(getAllTablesQuery, (rs, rowNum) -> rs.getString("table_name")).unique();
        Assert.notNull(tablesInConfig);
        Assert.notNull(tablesInDb);
        Assert.isEmpty(Cf.toSet(tablesInDb).minus(tablesInConfig), "see DjfsMigrationPlan.allTablesMigrations");
    }

    @Test
    public void checkAllTablesOrderAccordingToDependencies() {
        Assert.isEmpty(checkTableDependencies(
                DjfsMigrationPlan.allTablesMigrations,
                Cf.list(Tuple2.tuple("album_items", "albums"))
        ));
    }

    @Test
    public void checkFailIfDependenciesInWrongOrder() {
        Assert.assertContains(
                checkTableDependencies(DjfsMigrationPlan.builder()
                        .simple("user_activity_info")
                        .simple("user_index")
                        .build(),
                        Cf.list()
                ),
                "user_activity_info must be after user_index because of foreign key"
        );
    }

    @Test
    public void checkAllSelfReferenceHandled() {
        SetF<String> tablesWithSelfReferenceMigration = DjfsMigrationPlan.allTablesMigrations.getSelfReference().unique();
        SetF<String> ignoredTables = DjfsMigrationPlan.allTablesMigrations.getIgnore().unique();


        SetF<String> tablesWithSelfReference = getTableDependencies()
                .filter((table, dependencies) -> dependencies.containsTs(table))
                .keys().unique();

        Assert.isEmpty(tablesWithSelfReference.minus(tablesWithSelfReferenceMigration).minus(ignoredTables));
    }

}
