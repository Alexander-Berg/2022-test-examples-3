package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.List;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMO_YT_SOURCE_ENABLE_ANAPLAN_ID_PREFIX;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.anaplanId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.blueSet;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.source;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;

@TestFor(DiscountController.class)
@RunWith(Parameterized.class)
public class DiscountControllerAnaplanIdVisibilityTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final String ANAPLAN_ID = "anaplan id";
    private static final String SOURCE = "some";
    private static final String FIRST_ITEM_SSKU = "first promo offer";
    private static final String SECOND_ITEM_SSKU = "second promo offer";

    @Parameterized.Parameters
    public static Iterable<?> eventTypes() {
        return List.of(true, false);
    }

    @ClassRule
    public static final SpringClassRule SCR = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Autowired
    private PromoBundleService bundleService;

    @Autowired
    private ConfigurationService configurationService;

    private PromoBundleDescription expectedDescription;
    private boolean visibility;

    public DiscountControllerAnaplanIdVisibilityTest(boolean visibility) {
        this.visibility = visibility;
    }

    @Before
    public void prepare() {
        expectedDescription = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                shopPromoId(randomString()),
                promoKey(randomString()),
                anaplanId(ANAPLAN_ID),
                source(SOURCE),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_ITEM_SSKU, 30),
                                proportion(SECOND_ITEM_SSKU, 30)
                        )),
                        primary()
                )
        ));

        configurationService.set(PROMO_YT_SOURCE_ENABLE_ANAPLAN_ID_PREFIX + SOURCE, visibility);
    }

    @Test
    public void shouldSendAnaplanIdDependsConfiguration() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_ITEM_SSKU),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(expectedDescription.getPromoKey()),
                        price(7485)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_ITEM_SSKU),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(expectedDescription.getPromoKey()),
                        price(2538)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(1));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), empty());

        assertThat(firstOrderOf(discountResponse).getItems(), everyItem(
                hasProperty(
                        "promos",
                        hasItem(
                                hasProperty("anaplanId", visibility ? is(ANAPLAN_ID) : nullValue())
                        )
                )));
    }
}
