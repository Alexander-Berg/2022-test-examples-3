package ru.yandex.market.ydb.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.migration.YdbTableCreate;
import ru.yandex.market.ydb.integration.model.YdbField;
import ru.yandex.market.ydb.integration.table.Index;
import ru.yandex.market.ydb.integration.table.Primary;
import ru.yandex.market.ydb.integration.ServiceTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

public class YdbTableDescriptionTest extends ServiceTestBase {

    private static final String INDEX = "some index";
    private static final String PRIMARY_FIRST = "primary_first";
    private static final String PRIMARY_SECOND = "primary_second";
    private static final String INDEX_FIRST = "index_first";
    private static final String INDEX_SECOND = "index_second";

    @Autowired
    protected TestYdbTableDescription tableDescription;

    @Test
    void shouldSortPrimaryColumns() {
        YdbTableCreate tableCreate = tableDescription.toCreate();

        assertThat(tableCreate.toTableDescription().getPrimaryKeys(), contains(
                PRIMARY_FIRST,
                PRIMARY_SECOND
        ));
    }

    @Test
    void shouldSortIndexColumns() {
        YdbTableCreate tableCreate = tableDescription.toCreate();

        assertThat(tableCreate.toTableDescription().getIndexes(), hasItem(allOf(
                hasProperty("name", is(INDEX)),
                hasProperty("columns", contains(
                        INDEX_FIRST,
                        INDEX_SECOND
                ))
        )));
    }

    @DatabaseModel(value = "test_table", alias = "tst")
    public static class TestYdbTableDescription extends YdbTableDescription {

        @Primary(order = 1)
        private final YdbField<String> firstPrimary = text(PRIMARY_FIRST);
        @Primary(order = 2)
        private final YdbField<String> secondPrimary = text(PRIMARY_SECOND);
        @Index(value = INDEX, order = 1)
        private final YdbField<String> indexFirst = text(INDEX_FIRST);
        @Index(value = INDEX, order = 2)
        private final YdbField<String> indexSecond = text(INDEX_SECOND);

        public YdbField<String> getFirstPrimary() {
            return firstPrimary;
        }

        public YdbField<String> getSecondPrimary() {
            return secondPrimary;
        }

        public YdbField<String> getIndexFirst() {
            return indexFirst;
        }

        public YdbField<String> getIndexSecond() {
            return indexSecond;
        }
    }
}
