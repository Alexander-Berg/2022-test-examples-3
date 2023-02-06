package ru.yandex.market.ydb.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.migration.YdbTableChanges;
import ru.yandex.market.ydb.integration.model.YdbField;
import ru.yandex.market.ydb.integration.table.Primary;
import ru.yandex.market.ydb.integration.ServiceTestBase;

public class YdbTemplateSchemaChangeTest extends ServiceTestBase {

    @Autowired
    private YdbTemplate ydbTemplate;
    @Autowired
    private TestYdbTableDescription tableDescription;

    @BeforeEach
    void configure() {
        ydbTemplate.createTable(tableDescription.toCreate());
    }

    @AfterEach
    void clean() {
        ydbTemplate.dropTable(tableDescription.tableName());
    }

    @Test
    void shouldAddColumnIntoTable() {
        ydbTemplate.changeTable(YdbTableChanges.addColumn(tableDescription, tableDescription.text("newColumn")));
    }

    @Test
    void shouldDropColumnIntoTable() {
        ydbTemplate.changeTable(YdbTableChanges.dropColumn(tableDescription, "name"));
    }

    @DatabaseModel(value = "test_table_1", alias = "tst1")
    public static class TestYdbTableDescription extends YdbTableDescription {

        @Primary
        private final YdbField<String> id = text("id");
        private final YdbField<String> name = text("name");

        public YdbField<String> getId() {
            return id;
        }

        public YdbField<String> getName() {
            return name;
        }
    }
}
