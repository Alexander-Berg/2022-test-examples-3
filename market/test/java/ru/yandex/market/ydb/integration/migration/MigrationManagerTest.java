package ru.yandex.market.ydb.integration.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.DatabaseModel;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.YdbTemplate;
import ru.yandex.market.ydb.integration.model.YdbField;
import ru.yandex.market.ydb.integration.table.Primary;
import ru.yandex.market.ydb.integration.ServiceTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MigrationManagerTest extends ServiceTestBase {

    @Autowired
    private MigrationManager migrationManager;
    @Autowired
    private MigrationDao migrationDao;
    @Autowired
    private YdbTemplate ydbTemplate;
    @Autowired
    private TestYdbTableDescription tableDescription;

    @Test
    void shouldDoInitialMigration() {
        migrationManager.migrate(
                MigrationOperation.create(tableDescription)
        );

        assertThat(migrationDao.getInitialMigrationByTableName(tableDescription.tableName()).isPresent(),
                is(true));

        ydbTemplate.dropTable(tableDescription.tableName());
    }

    @Test
    void shouldDropTable() {
        MigrationOperation expected = MigrationOperation.drop(tableDescription.tableName());
        migrationManager.migrate(
                MigrationOperation.create(tableDescription),
                expected
        );

        assertThat(migrationDao.getMigrationByHash(expected.migrationDescription().hash()).isPresent(), is(true));
    }

    @Test
    void shouldAddColumn() {
        MigrationOperation expected = MigrationOperation.change(
                YdbTableChanges.addColumn(tableDescription, tableDescription.text("someNewColumn"))
        );
        migrationManager.migrate(
                MigrationOperation.create(tableDescription),
                expected
        );

        assertThat(migrationDao.getMigrationByHash(expected.migrationDescription().hash()).isPresent(), is(true));

        ydbTemplate.dropTable(tableDescription.tableName());
    }

    @Test
    void shouldDropColumn() {
        MigrationOperation expected =
                MigrationOperation.change(YdbTableChanges.dropColumn(tableDescription, "someNewColumn"));
        migrationManager.migrate(
                MigrationOperation.create(tableDescription),
                MigrationOperation.change(
                        YdbTableChanges.addColumn(
                                tableDescription,
                                tableDescription.text("someNewColumn")
                        )),
                expected
        );

        assertThat(migrationDao.getMigrationByHash(expected.migrationDescription().hash()).isPresent(), is(true));

        ydbTemplate.dropTable(tableDescription.tableName());
    }

    @DatabaseModel(value = "mm_table", alias = "mmt")
    static class TestYdbTableDescription extends YdbTableDescription {

        @Primary(order = 1)
        private final YdbField<String> id = text("id");
        private final YdbField<String> some = text("some");

        public YdbField<String> getId() {
            return id;
        }

        public YdbField<String> getSome() {
            return some;
        }
    }
}
