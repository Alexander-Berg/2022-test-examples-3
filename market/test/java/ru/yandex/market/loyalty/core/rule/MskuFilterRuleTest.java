package ru.yandex.market.loyalty.core.rule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.promo.ExcludedOffersType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
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
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;

/**
 * @author dinyat
 * 09/06/2017
 */
public class MskuFilterRuleTest extends MarketLoyaltyCoreMockedDbTestBase {

    private static final String MSKU_ID = "123";
    private static final String ANOTHER_MSKU_ID = "234";
    private Promo promo;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ItemsFilter itemsFilter;
    @Autowired
    private DiscountUtils discountUtils;

    @Override
    protected boolean shouldCheckConsistence() {
        return false;
    }

    @Test
    public void testFilterItemsWithNotExistedOffer() {
        promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .addPromoRule(MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, ImmutableSet.of(MSKU_ID,
                                ANOTHER_MSKU_ID))
        );

        List<Item> items = OrderItemsConverter.constructItemsFromRequest(
                orderRequestBuilder()
                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                        .build()
                        .getItems()
        );

        List<Item> result = itemsFilter.filterItems(items, promo.getRulesContainer(),
                ExcludedOffersType.PRICE_DISCOUNT, discountUtils.getRulesPayload())
                .map(ItemFilteringResult::getItem)
                .collect(Collectors.toList());

        assertThat(result, is(empty()));
    }

    @Test
    public void testFilterItemsByMskuWithExistedOffer() {
        promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .addPromoRule(MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton(MSKU_ID))
        );

        List<Item> items = OrderItemsConverter.constructItemsFromRequest(
                orderRequestBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                msku(MSKU_ID)
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                msku(ANOTHER_MSKU_ID)
                        )
                        .build()
                        .getItems()
        );

        List<Item> result = itemsFilter.filterItems(items, promo.getRulesContainer(),
                ExcludedOffersType.PRICE_DISCOUNT, discountUtils.getRulesPayload())
                .map(ItemFilteringResult::getItem)
                .collect(Collectors.toList());

        assertThat(result, contains(hasProperty("itemKey", equalTo(DEFAULT_ITEM_KEY))));
    }
}
