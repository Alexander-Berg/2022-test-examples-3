package ru.yandex.market.ydb.integration.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.yandex.ydb.table.values.PrimitiveValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.DatabaseModel;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.model.Field;
import ru.yandex.market.ydb.integration.model.YdbField;
import ru.yandex.market.ydb.integration.model.YdbTuple2;
import ru.yandex.market.ydb.integration.table.Primary;
import ru.yandex.market.ydb.integration.ServiceTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

public class YdbSelectTest extends ServiceTestBase {

    @Autowired
    private PromoYdbTable promoYdbTable;
    @Autowired
    private PromoCategoriesYdbTable promoCategoriesYdbTable;
    @Autowired
    private CachedAssortmentYdbTable cachedAssortmentYdbTable;

    @Test
    void shouldConstructSelectFromClauseWithAliases() {
        var qb = YdbSelect.select(QSelect.of(promoCategoriesYdbTable.fields())
                .from(QFrom.table(promoCategoriesYdbTable))).toQuery();

        assertThat(qb.text(),
                is("select pc.category_id as pc_category_id, pc.discount as pc_discount, " +
                        "pc.promo_id as pc_promo_id " +
                        "from `/local/promo_categories` as pc"));

        assertThat(qb.params(), anEmptyMap());
    }

    @Test
    void shouldConstructSelectFromDifferentTables() {
        var qb = YdbSelect.select(QSelect.of(
                promoYdbTable.getId(),
                promoYdbTable.getName(),
                promoCategoriesYdbTable.getCategoryId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId()))))).toQuery();

        assertThat(qb.text(),
                is("select prm.id as prm_id, prm.name as prm_name, pc.category_id as " +
                        "pc_category_id " +
                        "from `/local/promos` as prm " +
                        "join `/local/promo_categories` as pc on prm.id = pc.promo_id"));

        assertThat(qb.params(), anEmptyMap());
    }

    @Test
    void shouldConstructAggregationCountQuery() {
        var qb = YdbSelect.select(QSelect.of(
                YdbTableDescription.count(promoYdbTable.getId()),
                promoCategoriesYdbTable.getCategoryId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .groupBy(promoCategoriesYdbTable.getCategoryId()).toQuery();

        assertThat(qb.text(), is("select count(distinct prm.id) as count_id, pc.category_id as pc_category_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "group by pc.category_id"));

        assertThat(qb.params(), anEmptyMap());
    }

    @Test
    void shouldConstructAggregationCountWithoutDistinctQuery() {
        var qb = YdbSelect.select(QSelect.of(
                YdbTableDescription.count(promoYdbTable.getId(), false),
                promoCategoriesYdbTable.getCategoryId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .groupBy(promoCategoriesYdbTable.getCategoryId()).toQuery();

        assertThat(qb.text(), is("select count(prm.id) as count_id, pc.category_id as pc_category_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "group by pc.category_id"));

        assertThat(qb.params(), anEmptyMap());
    }

    @Test
    void shouldConstructAggregationMaxQuery() {
        var qb = YdbSelect.select(QSelect.of(
                YdbTableDescription.max(promoCategoriesYdbTable.getCategoryId()),
                promoYdbTable.getPromoId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .groupBy(promoYdbTable.getPromoId()).toQuery();

        assertThat(qb.text(), is("select max(pc.category_id) as max_category_id, prm.promo_id as prm_promo_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "group by prm.promo_id"));

        assertThat(qb.params(), anEmptyMap());
    }

    @Test
    void shouldConstructAggregationMinQuery() {
        var qb = YdbSelect.select(QSelect.of(
                YdbTableDescription.min(promoCategoriesYdbTable.getCategoryId()),
                promoYdbTable.getPromoId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .groupBy(promoYdbTable.getPromoId()).toQuery();

        assertThat(qb.text(), is("select min(pc.category_id) as min_category_id, prm.promo_id as prm_promo_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "group by prm.promo_id"));

        assertThat(qb.params(), anEmptyMap());
    }

    @Test
    void shouldConstructAggregationAvgQuery() {
        var qb = YdbSelect.select(QSelect.of(
                YdbTableDescription.avg(promoCategoriesYdbTable.getCategoryId()),
                promoYdbTable.getPromoId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .groupBy(promoYdbTable.getPromoId()).toQuery();

        assertThat(qb.text(), is("select avg(pc.category_id) as avg_category_id, prm.promo_id as prm_promo_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "group by prm.promo_id"));

        assertThat(qb.params(), anEmptyMap());
    }

    @Test
    void shouldConstructQueryWithOrder() {
        var qb = YdbSelect.select(QSelect.of(
                YdbTableDescription.count(promoYdbTable.getId()),
                promoCategoriesYdbTable.getCategoryId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .orderBy(QOrder.by(QOrder.OrderField.asc(promoCategoriesYdbTable.getCategoryId()))).toQuery();

        assertThat(qb.text(), is("select count(distinct prm.id) as count_id, pc.category_id as pc_category_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "order by pc_category_id asc"));

        assertThat(qb.params(), anEmptyMap());
    }

    @Test
    void shouldConstructConditionalQueryWithJoins() {
        var qb = YdbSelect.select(QSelect.of(
                promoYdbTable.getId(),
                promoCategoriesYdbTable.getCategoryId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .where(QCondition.or(
                        promoYdbTable.getPromoId().eq("some"),
                        promoCategoriesYdbTable.getCategoryId().eq(123L)
                ))
                .toQuery();

        assertThat(qb.text(), is("select prm.id as prm_id, pc.category_id as pc_category_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "where ((prm.promo_id = $prm_promo_id_" +
                Operator.EQ.ordinal() + "_0) or (pc.category_id = $pc_category_id_" +
                Operator.EQ.ordinal() + "_0))"));

        assertThat(qb.params(), aMapWithSize(2));
        assertThat(qb.params().keySet(), hasItems(
                "$prm_promo_id_" + Operator.EQ.ordinal() + "_0",
                "$pc_category_id_" + Operator.EQ.ordinal() + "_0"
        ));
    }

    @Test
    void shouldConstructConditionalQueryWithLimits() {
        var qb = YdbSelect.select(QSelect.of(
                promoYdbTable.getId(),
                promoCategoriesYdbTable.getCategoryId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .where(QCondition.or(
                        promoYdbTable.getPromoId().eq("some"),
                        promoCategoriesYdbTable.getCategoryId().eq(123L)
                ))
                .limit(100)
                .toQuery();

        assertThat(qb.text(), is("select prm.id as prm_id, pc.category_id as pc_category_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "where ((prm.promo_id = $prm_promo_id_" +
                Operator.EQ.ordinal() + "_0) or (pc.category_id = $pc_category_id_" +
                Operator.EQ.ordinal() + "_0)) limit $limit"));

        assertThat(qb.params(), aMapWithSize(3));
        assertThat(qb.params().keySet(), hasItems(
                "$prm_promo_id_" + Operator.EQ.ordinal() + "_0",
                "$pc_category_id_" + Operator.EQ.ordinal() + "_0",
                "$limit"
        ));
    }

    @Test
    void shouldConstructConditionalQueryWithLimitsAndOffset() {
        var qb = YdbSelect.select(QSelect.of(
                promoYdbTable.getId(),
                promoCategoriesYdbTable.getCategoryId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .where(QCondition.or(
                        promoYdbTable.getPromoId().eq("some"),
                        promoCategoriesYdbTable.getCategoryId().eq(123L)
                ))
                .limit(100)
                .offset(1L)
                .toQuery();

        assertThat(qb.text(), is("select prm.id as prm_id, pc.category_id as pc_category_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "where ((prm.promo_id = $prm_promo_id_" +
                Operator.EQ.ordinal() + "_0) or (pc.category_id = $pc_category_id_" +
                Operator.EQ.ordinal() + "_0)) limit $limit offset $offset"));

        assertThat(qb.params(), aMapWithSize(4));
        assertThat(qb.params().keySet(), hasItems(
                "$prm_promo_id_" + Operator.EQ.ordinal() + "_0",
                "$pc_category_id_" + Operator.EQ.ordinal() + "_0",
                "$limit",
                "$offset"
        ));
    }

    @Test
    void shouldConstructConditionalQueryWithCorteges() {

        var promoId = UUID.randomUUID().toString();
        var qb = YdbSelect.select(QSelect.of(
                promoYdbTable.getId(),
                promoCategoriesYdbTable.getCategoryId()
        )
                .from(QFrom.join(promoYdbTable, promoCategoriesYdbTable,
                        QFrom.OnClause.on(promoYdbTable.getId().eq(promoCategoriesYdbTable.getPromoId())))).select())
                .where(YdbTuple2.tuple(promoYdbTable.getId(), promoCategoriesYdbTable.getCategoryId(), Map.Entry.class)
                        .eq(Map.entry(promoId, 123L)))
                .toQuery();

        assertThat(qb.text(), is("select prm.id as prm_id, pc.category_id as pc_category_id " +
                "from `/local/promos` as prm " +
                "join `/local/promo_categories` as pc on prm.id = pc.promo_id " +
                "where ((prm.id,pc.category_id) = $id_category_id_" + Operator.EQ.ordinal() + "_0)"));

        assertThat(qb.params(), aMapWithSize(1));
        assertThat(qb.params().keySet(), hasItems(
                "$id_category_id_" + Operator.EQ.ordinal() + "_0"
        ));
    }

    @Test
    void shouldConstructConditionalQueryWithJsonValue() {
        var qb = YdbSelect.select(QSelect.of(cachedAssortmentYdbTable.getOfferId())
                .from(QFrom.table(cachedAssortmentYdbTable)))
                .where(cachedAssortmentYdbTable.stockByWarehouse.jsonValueGreaterZero("123")).toQuery();

        assertThat(qb.text(), is("select ca.offer_id as ca_offer_id from `/local/cached_assortment` as ca " +
                "where (json_value(ca.stocks_by_warehouse, '$.\"123\"' returning Uint64 default 0 on empty default 0 on error) > 0)"
        ));

        var qbNotNull = YdbSelect.select(QSelect.of(cachedAssortmentYdbTable.getOfferId())
                .from(QFrom.table(cachedAssortmentYdbTable)))
                .where(cachedAssortmentYdbTable.stockByWarehouse.jsonValueIsNotNull("123")).toQuery();

        assertThat(qbNotNull.text(), is("select ca.offer_id as ca_offer_id from `/local/cached_assortment` as ca " +
                "where (json_value(ca.stocks_by_warehouse, '$.\"123\"') is not null)"
        ));
    }

    @DatabaseModel(value = PromoYdbTable.TABLE_NAME, alias = "prm")
    public static class PromoYdbTable extends YdbTableDescription {

        public static final String TABLE_NAME = "promos";

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

        public static final String TABLE_NAME = "promo_categories";

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

    @DatabaseModel(value = CachedAssortmentYdbTable.TABLE_NAME, alias = "ca")
    public static class CachedAssortmentYdbTable extends YdbTableDescription {

        public static final String TABLE_NAME = "cached_assortment";

        @Primary
        private final YdbField<String> offerId = text("offer_id");
        @Primary(order = 1)
        private final YdbField<String> promoId = text("promo_id");
        private final YdbField<String> name = text("name");
        private final YdbField<Map<Long, Long>> stockByWarehouse = json("stocks_by_warehouse");

        public YdbField<String> getOfferId() {
            return offerId;
        }

        public YdbField<String> getPromoId() {
            return promoId;
        }

        public YdbField<Map<Long, Long>> getStockByWarehouse() {
            return stockByWarehouse;
        }
    }
}
