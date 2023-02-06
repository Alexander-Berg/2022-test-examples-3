package ru.yandex.market.ydb.integration.query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.DatabaseModel;
import ru.yandex.market.ydb.integration.ServiceTestBase;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.model.YdbField;
import ru.yandex.market.ydb.integration.table.Primary;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

public class YdbInsertTest extends ServiceTestBase {

    @Autowired
    private PromoYdbTable promoYdbTable;

    @Test
    void shouldConstructInsertQuery() {
        var qb = YdbInsert.insert(promoYdbTable, promoYdbTable.id, promoYdbTable.name, promoYdbTable.promoId)
                .row("some id", "some name", "some promo id").toQuery();

        assertThat(qb.text(),
                is("insert into `/local/" + PromoYdbTable.TABLE_NAME + "`  " +
                        "select * from as_table($batch_insert_cases_0)"));

        assertThat(qb.params(), aMapWithSize(1));
        assertThat(qb.params().keySet(), hasItems(
                "$batch_insert_cases_0"
        ));
    }

    @Test
    void shouldConstructUpsertQuery() {
        var qb = YdbInsert.upsert(promoYdbTable, promoYdbTable.id, promoYdbTable.name, promoYdbTable.promoId)
                .row("some id", "some name", "some promo id").toQuery();

        assertThat(qb.text(),
                is("upsert into `/local/" + PromoYdbTable.TABLE_NAME + "`  " +
                        "select * from as_table($batch_insert_cases_0)"));

        assertThat(qb.params(), aMapWithSize(1));
        assertThat(qb.params().keySet(), hasItems(
                "$batch_insert_cases_0"
        ));
    }

    @Test
    void shouldConstructReplaceQuery() {
        var qb = YdbInsert.replace(promoYdbTable, promoYdbTable.id, promoYdbTable.name, promoYdbTable.promoId)
                .row("some id", "some name", "some promo id").toQuery();

        assertThat(qb.text(),
                is("replace into `/local/" + PromoYdbTable.TABLE_NAME + "`  " +
                        "select * from as_table($batch_insert_cases_0)"));

        assertThat(qb.params(), aMapWithSize(1));
        assertThat(qb.params().keySet(), hasItems(
                "$batch_insert_cases_0"
        ));
    }

    @DatabaseModel(value = PromoYdbTable.TABLE_NAME, alias = "t")
    public static class PromoYdbTable extends YdbTableDescription {

        public static final String TABLE_NAME = "insert_cases";

        @Primary
        private final YdbField<String> id = text("id");
        private final YdbField<String> promoId = text("promo_id");
        private final YdbField<String> name = text("name");

        public YdbField<String> getId() {
            return id;
        }

        public YdbField<String> getPromoId() {
            return promoId;
        }

        public YdbField<String> getName() {
            return name;
        }
    }
}
