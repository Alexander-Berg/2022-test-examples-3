package ru.yandex.market.ydb.integration.query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.DatabaseModel;
import ru.yandex.market.ydb.integration.ServiceTestBase;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.model.YdbField;
import ru.yandex.market.ydb.integration.table.Primary;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class YdbUpdateTest extends ServiceTestBase {

    @Autowired
    private PromoYdbTable promoYdbTable;

    @Test
    void shouldConstructUpdateQuery() {
        var qb = YdbUpdate.update(promoYdbTable)
                .set(promoYdbTable.getName(), "new name")
                .where(promoYdbTable.id.eq("some id"));

        assertThat(qb.toQuery().text(QContext.NO_ALIASES),
                is("update `/local/" + PromoYdbTable.TABLE_NAME + "`  " +
                        "set name = $upd_name_0 " +
                        "where (id = $t_id_" + Operator.EQ.ordinal() + "_0)"));
    }

    @DatabaseModel(value = PromoYdbTable.TABLE_NAME, alias = "t")
    public static class PromoYdbTable extends YdbTableDescription {

        public static final String TABLE_NAME = "update_cases";

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
