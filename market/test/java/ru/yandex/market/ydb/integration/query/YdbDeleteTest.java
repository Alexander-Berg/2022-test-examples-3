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

public class YdbDeleteTest extends ServiceTestBase {

    @Autowired
    private PromoYdbTable promoYdbTable;

    @Test
    void shouldConstructDeleteQuery() {
        var qb = YdbDelete.deleteFrom(promoYdbTable)
                .where(promoYdbTable.id.eq("some id")).toQuery();

        assertThat(qb.text(),
                is("delete from `/local/" + PromoYdbTable.TABLE_NAME + "`  " +
                        "where (id = $t_id_" + Operator.EQ.ordinal() + "_0)"));

        assertThat(qb.params(), aMapWithSize(1));
        assertThat(qb.params().keySet(), hasItems(
                "$t_id_" + Operator.EQ.ordinal() + "_0"
        ));
    }

    @DatabaseModel(value = PromoYdbTable.TABLE_NAME, alias = "t")
    public static class PromoYdbTable extends YdbTableDescription {

        public static final String TABLE_NAME = "delete_cases";

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
