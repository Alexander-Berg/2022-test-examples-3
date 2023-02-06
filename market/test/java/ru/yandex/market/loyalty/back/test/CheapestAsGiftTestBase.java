package ru.yandex.market.loyalty.back.test;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemResponse;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.NoSuchElementException;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.md5;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.cheapestAsGift;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.quantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.withQuantityInBundle;

public abstract class CheapestAsGiftTestBase extends MarketLoyaltyBackMockedDbTestBase {

    protected static final long FEED_ID = 123;
    protected static final String BUNDLE_PROMO_KEY = "some promo bundle";
    protected static final String FIRST_ITEM_SSKU = "some promo offer";
    protected static final String SECOND_ITEM_SSKU = "another promo offer";
    protected static final String THIRD_ITEM_SSKU = "third promo offer";

    @Autowired
    protected PromoBundleService bundleService;

    @Before
    public final void prepareBundle() {
        createBundle();
    }

    @Nonnull
    protected static BundledOrderItemResponse getItemByShopSKU(
            @Nonnull OrderWithBundlesResponse bundlesResponse,
            @Nonnull String shopSku
    ) {
        return bundlesResponse.getItems().stream()
                .filter(item -> item.getOfferId().equals(md5(shopSku)))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    @Nonnull
    protected static ItemPromoResponse getPromo(
            @Nonnull List<ItemPromoResponse> promos,
            @Nonnull PromoType promoType
    ) {
        return promos.stream()
                .filter(p -> p.getPromoType() == promoType)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    private void createBundle() {
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(randomString()),
                strategy(CHEAPEST_AS_GIFT),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                withQuantityInBundle(3),
                item(
                        condition(cheapestAsGift(
                                FeedSskuSet.of(FEED_ID, List.of(FIRST_ITEM_SSKU, SECOND_ITEM_SSKU, THIRD_ITEM_SSKU)))),
                        quantityInBundle(3),
                        primary()
                )
        ));
    }
}
