package ru.yandex.market.loyalty.core.rule;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.loyalty.core.rule.RuleType.WITHOUT_SHOP_DISCOUNT_FILTER_RULE;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;

/**
 * @author dinyat
 * 09/06/2017
 */
public class WithoutShopDiscountFilterRuleTest extends MarketLoyaltyCoreMockedDbTestBase {

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


    @Before
    public void setUp() {
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .addPromoRule(WITHOUT_SHOP_DISCOUNT_FILTER_RULE)
        );
    }

    @Test
    public void itemsWithDiscount() {
        List<Item> items = OrderItemsConverter.constructItemsFromRequest(
                orderRequestBuilder()
                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                        .withOrderItem(itemKey(ANOTHER_ITEM_KEY), b -> b.setOldMinPrice(BigDecimal.TEN))
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
