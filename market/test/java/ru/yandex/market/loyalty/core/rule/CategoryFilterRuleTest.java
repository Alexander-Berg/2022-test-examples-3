package ru.yandex.market.loyalty.core.rule;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.order.Item;
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
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.FIRST_CHILD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PARENT_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;

/**
 * @author dinyat
 * 08/06/2017
 */
public class CategoryFilterRuleTest extends MarketLoyaltyCoreMockedDbTestBase {

    private static final int FIRST_CATEGORY_ID = 12644434;
    private static final int ANOTHER_CATEGORY_ID = 12312;

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
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .addPromoRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(FIRST_CATEGORY_ID, 456))
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
    public void testFilterItemsByCategoryWithExistedOffer() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .addPromoRule(CATEGORY_FILTER_RULE, CATEGORY_ID, (ImmutableSet.of(FIRST_CATEGORY_ID, 456)))
                //.addPromoRule(EXCLUDED_OFFERS_FILTER_RULE, EXCLUDED_OFFERS_TYPE, ExcludedOffersType.NONE)
        );
        /*Promo promo1 = promoManager.createPromocodePromo(PromoUtils.accrualPromoBuilder()
                .addCoinRule(EXCLUDED_OFFERS_FILTER_RULE, EXCLUDED_OFFERS_TYPE, ExcludedOffersType.NONE)
        );*/

        List<Item> items = OrderItemsConverter.constructItemsFromRequest(
                orderRequestBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                categoryId(FIRST_CATEGORY_ID),
                                supplier(12345L)
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                categoryId(ANOTHER_CATEGORY_ID)
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

    @Test
    public void testFilterItemsByCategoryWithExistedOfferByParentCategory() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .addPromoRule(CATEGORY_FILTER_RULE, CATEGORY_ID, (ImmutableSet.of(PARENT_CATEGORY_ID, 456)))
        );

        List<Item> items = OrderItemsConverter.constructItemsFromRequest(
                orderRequestBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                categoryId(FIRST_CHILD_CATEGORY_ID)
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                categoryId(ANOTHER_CATEGORY_ID)
                        )
                        .build()
                        .getItems()
        );

        List<Item> result = itemsFilter.filterItems(
                items, promo.getRulesContainer(), ExcludedOffersType.PRICE_DISCOUNT, discountUtils.getRulesPayload())
                .map(ItemFilteringResult::getItem)
                .collect(Collectors.toList());

        assertThat(result, contains(hasProperty("itemKey", equalTo(DEFAULT_ITEM_KEY))));
    }
}
