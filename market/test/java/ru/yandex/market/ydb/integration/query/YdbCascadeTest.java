package ru.yandex.market.ydb.integration.query;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

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

public class YdbCascadeTest extends ServiceTestBase {

    @Autowired
    private PromoYdbTable promoYdbTable;
    @Autowired
    private PromoCategoriesYdbTable promoCategoriesYdbTable;

    @Test
    void shouldConstructCascadeQueryWithInserts() {
        var qb1 = YdbInsert.insert(promoYdbTable,
                promoYdbTable.id, promoYdbTable.name, promoYdbTable.promoId)
                .row("some id", "some name", "some promo id");
        var qb2 = YdbInsert.insert(promoCategoriesYdbTable,
                promoCategoriesYdbTable.promoId, promoCategoriesYdbTable.categoryId, promoCategoriesYdbTable.discount)
                .row("some promo id", 123L, BigDecimal.TEN);
        var qb = YdbCascade.cascadeOf(qb1.toQuery(), qb2.toQuery());

        assertThat(qb.text(),
                is("insert into `/local/" + PromoYdbTable.TABLE_NAME + "`  " +
                        "select * from as_table($batch_cascade_cases_0) ;\n" +
                        "insert into `/local/" + PromoCategoriesYdbTable.TABLE_NAME + "`  " +
                        "select * from as_table($batch_cascade_cases2_1)"));

        assertThat(qb.params(), aMapWithSize(2));
        assertThat(qb.params().keySet(), hasItems(
                "$batch_cascade_cases_0",
                "$batch_cascade_cases2_1"
        ));
    }

    @Test
    void shouldConstructCascadeQueryWithUpdates() {
        var qb1 = YdbInsert.upsert(promoYdbTable,
                promoYdbTable.id, promoYdbTable.name, promoYdbTable.promoId)
                .row("some id", "some name", "some promo id");
        var qb2 = YdbUpdate.update(promoCategoriesYdbTable)
                .set(promoCategoriesYdbTable.discount, BigDecimal.valueOf(123))
                .where(promoCategoriesYdbTable.promoId.eq("some promo id"));
        var qb3 = YdbUpdate.update(promoCategoriesYdbTable)
                .set(promoCategoriesYdbTable.discount, BigDecimal.valueOf(124))
                .where(promoCategoriesYdbTable.promoId.eq("some another id"));
        var qb = YdbCascade.cascadeOf(qb1.toQuery(), qb2.toQuery(), qb3.toQuery());

        assertThat(qb.text(),
                is("upsert into `/local/" + PromoYdbTable.TABLE_NAME + "`  " +
                        "select * from as_table($batch_cascade_cases_0) ;\n" +
                        "update `/local/" + PromoCategoriesYdbTable.TABLE_NAME + "`  " +
                        "set discount = $upd_discount_1 " +
                        "where (promo_id = $pc_promo_id_" + Operator.EQ.ordinal() + "_1);\n" +
                        "update `/local/" + PromoCategoriesYdbTable.TABLE_NAME + "`  " +
                        "set discount = $upd_discount_2 " +
                        "where (promo_id = $pc_promo_id_" + Operator.EQ.ordinal() + "_2)"
                ));

        assertThat(qb.params(), aMapWithSize(5));
        assertThat(qb.params().keySet(), hasItems(
                "$batch_cascade_cases_0",
                "$upd_discount_1",
                "$pc_promo_id_" + Operator.EQ.ordinal() + "_1",
                "$upd_discount_2",
                "$pc_promo_id_" + Operator.EQ.ordinal() + "_2"
        ));
    }

    @Test
    void shouldConstructCascadeQueryWithDeletes() {
        var qb1 = YdbDelete.deleteFrom(promoCategoriesYdbTable)
                .where(promoCategoriesYdbTable.promoId.eq("some promo id"));
        var qb2 = YdbDelete.deleteFrom(promoCategoriesYdbTable)
                .where(promoCategoriesYdbTable.promoId.eq("some another id"));
        var qb = YdbCascade.cascadeOf(qb1.toQuery(), qb2.toQuery());

        assertThat(qb.text(),
                is(
                        "delete from `/local/" + PromoCategoriesYdbTable.TABLE_NAME + "`  " +
                        "where (promo_id = $pc_promo_id_" + Operator.EQ.ordinal() + "_0);\n" +
                        "delete from `/local/" + PromoCategoriesYdbTable.TABLE_NAME + "`  " +
                        "where (promo_id = $pc_promo_id_" + Operator.EQ.ordinal() + "_1)"
                ));

        assertThat(qb.params(), aMapWithSize(2));
        assertThat(qb.params().keySet(), hasItems(
                "$pc_promo_id_" + Operator.EQ.ordinal() + "_0",
                "$pc_promo_id_" + Operator.EQ.ordinal() + "_1"
        ));
    }

    @DatabaseModel(value = PromoYdbTable.TABLE_NAME, alias = "t")
    public static class PromoYdbTable extends YdbTableDescription {

        public static final String TABLE_NAME = "cascade_cases";

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

    @DatabaseModel(value = PromoCategoriesYdbTable.TABLE_NAME, alias = "pc")
    public static class PromoCategoriesYdbTable extends YdbTableDescription {

        public static final String TABLE_NAME = "cascade_cases2";

        @Primary
        private final YdbField<String> promoId = text("promo_id");
        @Primary(order = 1)
        private final YdbField<Long> categoryId = bigint("category_id");
        private final YdbField<BigDecimal> discount = numeric("discount");

        @Nonnull
        public YdbField<String> getPromoId() {
            return promoId;
        }

        @Nonnull
        public YdbField<Long> getCategoryId() {
            return categoryId;
        }

        @Nonnull
        public YdbField<BigDecimal> getDiscount() {
            return discount;
        }
    }
}
