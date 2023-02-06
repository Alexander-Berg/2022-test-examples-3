package ru.yandex.market.loyalty.core.rule;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.promo.ExcludedOffersType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.ItemFilteringInfo.ItemFilteringResult;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.discount.OrderItemsConverter;
import ru.yandex.market.loyalty.core.service.discount.SpendMode;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.RulePayloads;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.rule.RuleType.VENDOR_FILTER_RULE;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.vendor;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class PayloadAccessTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long VENDOR_ID = 123L;
    private static final long ANOTHER_VENDOR_ID = 234L;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ItemsFilter itemsFilter;

    @Override
    protected boolean shouldCheckConsistence() {
        return false;
    }

    @Test
    public void shouldPayloadSupplierBeOneTimeAccessed() {
        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .addPromoRule(VENDOR_FILTER_RULE, RuleParameterName.VENDOR_ID, Set.of(VENDOR_ID,
                                ANOTHER_VENDOR_ID))
        );

        List<Item> items = OrderItemsConverter.constructItemsFromRequest(
                orderRequestBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                vendor(VENDOR_ID)
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                vendor(ANOTHER_VENDOR_ID)
                        )
                        .build()
                        .getItems()
        );

        RulePayloads<?> rulePayloads = RulePayloads.builder(SpendMode.SPEND)
                .putCached(PromoApplicabilityPolicy.class, new CheckOneTimeSupplier())
                .build();

        List<Item> result = itemsFilter.filterItems(items, promo.getRulesContainer(),
                ExcludedOffersType.PRICE_DISCOUNT, rulePayloads)
                .map(ItemFilteringResult::getItem)
                .collect(Collectors.toList());

        assertThat(result, containsInAnyOrder(
                hasProperty("itemKey", equalTo(DEFAULT_ITEM_KEY)),
                hasProperty("itemKey", equalTo(ANOTHER_ITEM_KEY))
        ));
    }

    private static class CheckOneTimeSupplier implements Supplier<PromoApplicabilityPolicy> {
        private final AtomicBoolean isAccessed = new AtomicBoolean(false);

        @Override
        public PromoApplicabilityPolicy get() {
            assertTrue(isAccessed.compareAndSet(false, true));
            return PromoApplicabilityPolicy.ANY;
        }
    }
}
