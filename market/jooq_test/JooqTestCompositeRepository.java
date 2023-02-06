package ru.yandex.market.mboc.common.services.jooq_test;

import java.util.Collection;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

import ru.yandex.market.mbo.jooq.repo.AscDesc;
import ru.yandex.market.mbo.jooq.repo.HasField;
import ru.yandex.market.mbo.jooq.repo.NaturalJooqRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.JooqTestComposite;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.records.JooqTestCompositeRecord;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.Tables.JOOQ_TEST_COMPOSITE;

public class JooqTestCompositeRepository extends NaturalJooqRepository<
    JooqTestComposite,
    JooqTestCompositeRecord,
    ShopSkuKey,
    JooqTestCompositeRepository.Filter,
    JooqTestCompositeRepository.SortBy> {

    public JooqTestCompositeRepository(DSLContext dslContext) {
        super(dslContext, JOOQ_TEST_COMPOSITE, JooqTestComposite.class,
            r -> new ShopSkuKey(r.get(JOOQ_TEST_COMPOSITE.SUPPLIER_ID), r.get(JOOQ_TEST_COMPOSITE.SHOP_SKU)));
    }

    @Override
    protected Condition createIdCondition(Collection<ShopSkuKey> shopSkuKeys) {
        return getShopSkuCondition(JOOQ_TEST_COMPOSITE, shopSkuKeys);
    }

    @Override
    protected Condition createCondition(Filter filter) {
        Condition condition = DSL.trueCondition();
        if (filter.getIds() != null) {
            condition = condition.and(getShopSkuCondition(JOOQ_TEST_COMPOSITE, filter.getIds()));
        }
        if (filter.isHasDescription()) {
            condition = condition.and(JOOQ_TEST_COMPOSITE.DESCRIPTION.isNotNull());
        }
        return condition;
    }

    @Getter
    @RequiredArgsConstructor
    public enum SortBy implements AscDesc<JooqTestCompositeRepository.SortBy>, HasField {
        SUPPLIER_ID(JOOQ_TEST_COMPOSITE.SUPPLIER_ID),
        DESCRIPTION(JOOQ_TEST_COMPOSITE.DESCRIPTION);
        private final Field<?> field;
    }

    @Data
    public static class Filter {
        private Collection<ShopSkuKey> ids;
        private boolean hasDescription = false;
    }

    public static Condition getShopSkuCondition(TableLike<?> table, Collection<ShopSkuKey> shopSkuKeys) {
        var supplierIdField = table.field("supplier_id", int.class);
        var shopSkuField = table.field("shop_sku", String.class);

        if (shopSkuKeys.isEmpty()) {
            return DSL.falseCondition();
        }

        var map = shopSkuKeys.stream()
            .collect(Collectors.groupingBy(ShopSkuKey::getSupplierId,
                Collectors.mapping(ShopSkuKey::getShopSku,
                    Collectors.toSet())));
        var conditions = map
            .entrySet().stream()
            .map(e -> supplierIdField.eq(e.getKey()).and(shopSkuField.in(e.getValue())))
            .collect(Collectors.toList());
        return DSL.or(conditions);
    }
}
