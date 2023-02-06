package ru.yandex.market.loyalty.core.rule;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.ExcludedOffersType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.ItemFilteringInfo.ItemFilteringResult;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.discount.OrderItemsConverter;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SUPPLIER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;

public class SuppliersFilterRuleTest extends MarketLoyaltyCoreMockedDbTestBase {

    long SUPPLIER = 12345L;
    long ANOTHER_SUPPLIER = 123456L;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ItemsFilter itemsFilter;
    @Autowired
    private DiscountUtils discountUtils;

    @Test
    public void testFilterItemsWithNotExistedOffer() {
        BigDecimal couponValue = BigDecimal.valueOf(80);
        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setCouponValue(couponValue, CoreCouponValueType.FIXED)
                        .addPromoRule(
                                RuleType.SUPPLIER_FILTER_RULE, SUPPLIER_ID, ImmutableSet.of(SUPPLIER)));

        List<Item> items = OrderItemsConverter.constructItemsFromRequest(
                orderRequestBuilder()
                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                        .build()
                        .getItems()
        );

        List<Item> result = itemsFilter.filterItems(items, promo.getRulesContainer(),
                ExcludedOffersType.PRICE_DISCOUNT, discountUtils.getRulesPayload())
                .filter(it -> !it.isZeroDiscount())
                .map(ItemFilteringResult::getItem)
                .collect(Collectors.toList());

        assertThat(result, is(empty()));
    }

    @Test
    public void testFilterItemsWithExistedOffer() {
        BigDecimal couponValue = BigDecimal.valueOf(80);
        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setCouponValue(couponValue, CoreCouponValueType.FIXED)
                        .addPromoRule(
                                RuleType.SUPPLIER_FILTER_RULE, SUPPLIER_ID, ImmutableSet.of(SUPPLIER)));

        List<Item> items = OrderItemsConverter.constructItemsFromRequest(
                orderRequestBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                supplier(SUPPLIER)
                        )
                        .build()
                        .getItems()
        );

        List<Item> result = itemsFilter.filterItems(items, promo.getRulesContainer(),
                ExcludedOffersType.PRICE_DISCOUNT, discountUtils.getRulesPayload())
                .filter(it -> !it.isZeroDiscount())
                .map(ItemFilteringResult::getItem)
                .collect(Collectors.toList());

        assertEquals(1, result.size());
        assertThat(result, contains(hasProperty("itemKey", equalTo(DEFAULT_ITEM_KEY))));
    }

    @Test
    public void testFilterItemsWithOneExistedOffer() {
        BigDecimal couponValue = BigDecimal.valueOf(80);
        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setCouponValue(couponValue, CoreCouponValueType.FIXED)
                        .addPromoRule(
                                RuleType.SUPPLIER_FILTER_RULE, SUPPLIER_ID, ImmutableSet.of(SUPPLIER)));

        List<Item> items = OrderItemsConverter.constructItemsFromRequest(
                orderRequestBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                supplier(SUPPLIER)
                        ).withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        supplier(ANOTHER_SUPPLIER)
                )
                        .build()
                        .getItems()
        );

        List<Item> result = itemsFilter.filterItems(
                items, promo.getRulesContainer(), ExcludedOffersType.PRICE_DISCOUNT, discountUtils.getRulesPayload())
                .filter(it -> !it.isZeroDiscount())
                .map(ItemFilteringResult::getItem)
                .collect(Collectors.toList());

        assertEquals(1, result.size());
        assertThat(result, contains(hasProperty("itemKey", equalTo(DEFAULT_ITEM_KEY))));
    }
}
