package ru.yandex.market.loyalty.core.service.bundle;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDefinition;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.PromoBundleDescriptionBuilder;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDiscount;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.bundle.calculation.SuccessPromoBundle;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest.Cart;
import ru.yandex.market.loyalty.core.service.discount.PromoCalculationList;
import ru.yandex.market.loyalty.core.service.discount.SpendMode;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.core.utils.CartUtils;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static java.math.RoundingMode.HALF_UP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;

public abstract class AbstractBundleStrategyTest extends MarketLoyaltyCoreMockedDbTestBase {
    protected static final long FEED_ID = 123;
    protected static final int WAREHOUSE_ID = 123;
    protected static final int ANOTHER_WAREHOUSE_ID = 124;
    protected static final String PROMO_KEY = "some promo";
    protected static final String ANOTHER_PROMO_KEY = "another some promo";
    protected static final String BUNDLE_ID = "bundle";
    protected static final String FIRST_SSKU = "first ssku";
    protected static final String SECOND_SSKU = "second ssku";
    protected static final String THIRD_SSKU = "third ssku";
    protected static final String FOURTH_SSKU = "fourth ssku";
    protected static final String ANOTHER_SSKU = "another ssku";
    protected static final String GIFT_ITEM_SSKU = "gift ssku";
    protected static final String ANOTHER_GIFT_ITEM_SSKU = "another gift ssku";

    @Autowired
    protected PromoBundleService bundleService;
    @Autowired
    protected PromoBundleCombiner bundleCombiner;
    @Autowired
    protected PromoBundleDiscountCalculator discountCalculator;

    @Before
    public void prepareConfig() {
        PromoBundleUtils.enableAllBundleFeatures(configurationService);
    }

    @After
    public void cleanConfig() {
        PromoBundleUtils.disableAllBundleFeatures(configurationService);
    }

    @SafeVarargs
    protected final PromoBundleDescription typicalDescription(
            BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder>... customizers
    ) {
        return bundleService.createPromoBundle(bundleDescription(
                Stream.concat(Stream.of(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(PROMO_KEY),
                        shopPromoId(randomString()),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(10)),
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                ), Arrays.stream(customizers))
        ));
    }

    @SafeVarargs
    protected final PromoBundleDescription descriptionWithAnotherGift(
            BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder>... customizers
    ) {
        return bundleService.createPromoBundle(bundleDescription(
                Stream.concat(Stream.of(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(ANOTHER_PROMO_KEY),
                        shopPromoId(randomString()),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(10)),
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, ANOTHER_GIFT_ITEM_SSKU))
                        )
                ), Arrays.stream(customizers))
        ));
    }

    protected BuildCustomizer<BundledOrderItemRequest, OrderRequestUtils.OrderItemBuilder> someKey() {
        return itemKey(randomString());
    }

    protected BuildCustomizer<BundledOrderItemRequest, OrderRequestUtils.OrderItemBuilder> itemKey(String offerId) {
        return OrderRequestUtils.itemKey(FEED_ID, offerId);
    }

    protected PromoBundleCombineResult recombineCart(
            OrderWithBundlesRequest order, PromoBundleDescription description
    ) {
        return bundleCombiner.recombineCartsByBundles(
                Collections.singletonList(order),
                Collections.singleton(description)
        ).build(SpendMode.SPEND, PromoCalculationList.empty());
    }

    protected PromoBundleCombineResult recombineCart(
            OrderWithBundlesRequest order, Set<PromoBundleDescription> descriptions
    ) {
        return bundleCombiner.recombineCartsByBundles(
                Collections.singletonList(order),
                descriptions
        ).build(SpendMode.SPEND, PromoCalculationList.empty());
    }

    protected Collection<Item> itemsOf(
            PromoBundleCombineResult combineResult
    ) {
        return combineResult.getCarts().stream()
                .flatMap(c -> c.getItems().stream())
                .collect(Collectors.toSet());
    }

    protected Item itemOf(Collection<Item> items, ItemKey key) {
        return items.stream()
                .filter(item -> item.getItemKey().equals(key))
                .findFirst().orElse(null);
    }


    protected Collection<PromoBundleDefinition> bundlesOf(
            PromoBundleCombineResult combineResult
    ) {
        return combineResult.getBundles().values();
    }

    protected SuccessPromoBundle calculateDiscount(PromoBundleCombineResult combineResult, Cart cart) {
        assertThat(combineResult.getBundles(cart), not(empty()));

        return (SuccessPromoBundle) discountCalculator.calculateDiscountForCart(
                cart, PromoCalculationList.empty(), combineResult)
                .findFirst().orElseThrow(RuntimeException::new);
    }

    protected Collection<SuccessPromoBundle> calculateDiscounts(PromoBundleCombineResult combineResult, Cart cart) {
        assertThat(combineResult.getBundles(cart), not(empty()));

        return discountCalculator.calculateDiscountForCart(cart, PromoCalculationList.empty(), combineResult)
                .map(SuccessPromoBundle.class::cast)
                .collect(Collectors.toSet());
    }

    protected Cart firstCart(PromoBundleCombineResult combineResult) {
        assertThat(combineResult.getCarts(), not(empty()));

        return combineResult.getCarts().iterator().next();
    }

    protected PromoBundleDiscount firstBundle(SuccessPromoBundle successPromoBundle) {
        return successPromoBundle.getPromoBundleDiscounts().stream()
                .findFirst().orElseThrow(RuntimeException::new);
    }

    protected BigDecimal proportionOf(Number amount, Number devident, Number devider) {
        return BigDecimal.valueOf(amount.doubleValue())
                .multiply(BigDecimal.valueOf(devident.doubleValue()))
                .divide(BigDecimal.valueOf(devider.doubleValue()), 0, HALF_UP);
    }

    protected String generateBundleId(PromoBundleDescription bundleDescription, String... offerIds) {
        return CartUtils.generateBundleId(bundleDescription, FEED_ID, offerIds);
    }
}
