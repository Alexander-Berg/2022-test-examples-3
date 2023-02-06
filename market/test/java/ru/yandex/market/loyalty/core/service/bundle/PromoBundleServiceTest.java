package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.loyalty.core.dao.promo.Queries.PROMO_TYPE_FIELD;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStatus.INACTIVE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.model.promo.CorePromoType.GENERIC_BUNDLE;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;

public class PromoBundleServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final long ANOTHER_FEED_ID = 124;
    private static final String BUNDLE = "some bundle";
    private static final String ANOTHER_BUNDLE = "another bundle";
    private static final String PROMO_ITEM_SSKU = "some promo offer";
    private static final String ANOTHER_ITEM_SSKU = "another promo offer";
    private static final String GIFT_ITEM_SSKU = "some gift offer";

    @Autowired
    private PromoBundleService bundleService;
    @Autowired
    private PromoBundleDao bundleDao;
    @Autowired
    private PromoDao promoDao;

    @Before
    public void prepare() {
        PromoBundleUtils.enableAllBundleFeatures(configurationService);
    }

    @After
    public void after() {
        PromoBundleUtils.disableAllBundleFeatures(configurationService);
    }

    @Test
    public void shouldGetFuturePromoIfSet() {
        bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(BUNDLE),
                        shopPromoId(BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime().plusDays(2)),
                        ends(clock.dateTime().plusYears(1)),
                        primaryItem(FEED_ID, PROMO_ITEM_SSKU),
                        giftItem(FEED_ID, directionalMapping(
                                when(PROMO_ITEM_SSKU),
                                then(GIFT_ITEM_SSKU)
                        ))
                )
        );

        assertThat(bundleService.getActivePromoKeys(), hasItem(BUNDLE));
    }

    @Test
    public void shouldGetActivePromoKeys() {
        createFirstBundle();

        assertThat(bundleService.getActivePromoKeys(), hasItem(BUNDLE));
    }

    @Test
    public void shouldDeactivatePromoBundles() {
        createFirstBundle();
        bundleService.deactivatePromosByPromoKey(Collections.singleton(BUNDLE));

        assertThat(bundleService.getActivePromoKeys(), empty());
    }

    @Test
    public void shouldCreatPromoOnce() {
        int initialSize = promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)).size();

        PromoBundleDescription proto = bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(BUNDLE),
                shopPromoId(BUNDLE),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                )
        );

        PromoBundleDescription descriptionFirst = bundleService.createPromoBundle(proto);
        PromoBundleDescription descriptionLast = bundleService.createPromoBundle(proto);

        assertThat(descriptionFirst.getId(), equalTo(descriptionLast.getId()));
        assertThat(descriptionFirst.getPromoId(), equalTo(descriptionLast.getPromoId()));

        assertThat(initialSize + 1, comparesEqualTo(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)).size()));
    }

    @Ignore
    @Test
    public void shouldReactivateInactivePromo() {
        PromoBundleDescription promoToSave = bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(BUNDLE),
                shopPromoId(BUNDLE),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                )
        );

        PromoBundleDescription savedPromo = bundleService.createPromoBundle(promoToSave);

        bundleService.deactivatePromosByPromoKey(Collections.singleton(savedPromo.getPromoKey()));

        assertThat(bundleDao.getPromoBundleDescriptionById(savedPromo.getId()), hasProperty("status", is(INACTIVE)));
        assertThat(bundleService.createPromoBundle(promoToSave), allOf(
                hasProperty("id", is(savedPromo.getId())),
                hasProperty("status", is(ACTIVE))
        ));
    }

    @Test
    public void shouldGetActivePromos() {
        createFirstBundle();

        assertThat(
                bundleService.getActivePromoBundles(Set.of(BUNDLE)),
                hasItem(hasProperty("promoKey", equalTo(BUNDLE)))
        );
    }

    @Test
    public void shouldGetSeveralActivePromos() {
        createFirstBundle();
        createSecondBundle();
        bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(ANOTHER_FEED_ID + 1L),
                        promoKey("third_bundle"),
                        shopPromoId(BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(1)),
                        item(
                                condition(giftWithPurchase(ANOTHER_FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(ANOTHER_FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        Set<PromoBundleDescription> bundles = bundleService.getActivePromoBundles(Set.of(BUNDLE));
        assertThat(bundles, hasSize(1));
        assertThat(bundles, hasItem(hasProperty("promoKey", equalTo(BUNDLE))));

        bundles = bundleService.getActivePromoBundles(Set.of(BUNDLE, ANOTHER_BUNDLE));
        assertThat(bundles, hasSize(2));
        assertThat(bundles, hasItems(
                hasProperty("promoKey", equalTo(BUNDLE)),
                hasProperty("promoKey", equalTo(ANOTHER_BUNDLE))
        ));

        bundles = bundleService.getActivePromoBundles(Set.of(BUNDLE, ANOTHER_BUNDLE, "third_bundle"));
        assertThat(bundles, hasSize(3));
        assertThat(bundles, hasItems(
                hasProperty("promoKey", equalTo(BUNDLE)),
                hasProperty("promoKey", equalTo(ANOTHER_BUNDLE)),
                hasProperty("promoKey", equalTo("third_bundle"))
        ));
    }

    @Test
    public void shouldCreatePromoWithShopPromoIdForEachPromoKey() {
        PromoBundleDescription first = createFirstBundle();
        PromoBundleDescription second = createSecondBundle();

        assertThat(first.getShopPromoId(), is(BUNDLE));
        assertThat(first.getShopPromoId(), is(second.getShopPromoId()));
        assertThat(first.getId(), not(second.getId()));

        assertThat(first.getPromoKey(), is(BUNDLE));
        assertThat(second.getPromoKey(), is(ANOTHER_BUNDLE));
    }

    @Test
    public void shouldCreateFallbackVersion() {
        createFirstBundle();
        //update
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(ANOTHER_BUNDLE),
                        shopPromoId(BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(1)),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        Optional<PromoBundleDescription> fallback = bundleDao.selectFirst(
                PromoBundleDao.activeOn(clock.dateTime()),
                PromoBundleDescription.FEED_ID.eqTo(FEED_ID),
                PromoBundleDescription.SHOP_PROMO_ID.eqTo(BUNDLE),
                PromoBundleDescription.SNAPSHOT_VERSION.eqTo(true)
        );

        assertThat(fallback.isPresent(), is(true));
        assertThat(fallback.get().getId(), not(bundleDescription.getId()));
        assertThat(fallback.get().getPromoKey(), is(BUNDLE));
        assertThat(bundleService.getActivePromoKeys(), hasItems(BUNDLE, ANOTHER_BUNDLE));
    }

    @Test
    public void shouldDeactivateFallbackVersion() {
        createFirstBundle();
        //update
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(ANOTHER_BUNDLE),
                        shopPromoId(BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(1)),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        Optional<PromoBundleDescription> fallback = bundleDao.selectFirst(
                PromoBundleDao.activeOn(clock.dateTime()),
                PromoBundleDescription.FEED_ID.eqTo(FEED_ID),
                PromoBundleDescription.SHOP_PROMO_ID.eqTo(BUNDLE),
                PromoBundleDescription.SNAPSHOT_VERSION.eqTo(true)
        );

        assertThat(fallback.isPresent(), is(true));
        assertThat(fallback.get().getId(), not(bundleDescription.getId()));
        assertThat(fallback.get().getPromoKey(), is(BUNDLE));
        assertThat(bundleService.getActivePromoKeys(), hasItems(BUNDLE, ANOTHER_BUNDLE));
    }

    @Test
    public void shouldNotCreateFallbackForSamePromoKey() {
        createFirstBundle();
        //update
        bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(BUNDLE),
                        shopPromoId(BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(1)),
                        item(
                                condition(giftWithPurchase(FEED_ID, ANOTHER_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        Optional<PromoBundleDescription> fallback = bundleDao.selectFirst(
                PromoBundleDao.activeOn(clock.dateTime()),
                PromoBundleDescription.FEED_ID.eqTo(FEED_ID),
                PromoBundleDescription.SHOP_PROMO_ID.eqTo(BUNDLE),
                PromoBundleDescription.SNAPSHOT_VERSION.eqTo(true)
        );

        assertThat(fallback.isPresent(), is(false));
    }

    private PromoBundleDescription createFirstBundle() {
        return bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(BUNDLE),
                        shopPromoId(BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(1)),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );
    }

    private PromoBundleDescription createSecondBundle() {
        return bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(ANOTHER_FEED_ID),
                        promoKey(ANOTHER_BUNDLE),
                        shopPromoId(BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(1)),
                        item(
                                condition(giftWithPurchase(ANOTHER_FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(ANOTHER_FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );
    }
}
