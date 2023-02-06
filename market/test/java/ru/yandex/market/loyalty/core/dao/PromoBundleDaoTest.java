package ru.yandex.market.loyalty.core.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleStatus;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static java.util.function.Function.identity;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao.activeOn;
import static ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao.withKeys;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.PROMO_KEY;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.changeDescriptionOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.changeItems;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.fixedPrice;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictReturn;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictions;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;

public class PromoBundleDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final String BUNDLE = "some bundle";
    private static final String ANOTHER_BUNDLE = "another promo";
    private static final String EXPIRED_BY_TIME_BUNDLE = "expired by time bundle";
    private static final String INACTIVE_BUNDLE = "inactive bundle";
    private static final String PROMO_ITEM_SSKU = "some promo offer";
    private static final String ONE_PROMO_ITEM_SSKU = "one promo offer";
    private static final String TWO_PROMO_ITEM_SSKU = "two promo offer";
    private static final String GIFT_ITEM_SSKU = "some gift offer";
    private static final String ANOTHER_GIFT_ITEM_SSKU = "another gift offer";

    @Autowired
    private PromoBundleService bundleService;
    @Autowired
    private PromoBundleDao bundleDao;

    @Before
    public void prepare() {
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
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(ANOTHER_BUNDLE),
                        shopPromoId(ANOTHER_BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(1)),
                        item(
                                condition(giftWithPurchase(FEED_ID, ONE_PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, TWO_PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, ANOTHER_GIFT_ITEM_SSKU))
                        )
                )
        );

        bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(EXPIRED_BY_TIME_BUNDLE),
                        shopPromoId(EXPIRED_BY_TIME_BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime().minusYears(1)),
                        ends(clock.dateTime().minusDays(10)),
                        item(
                                condition(giftWithPurchase(FEED_ID, ONE_PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, TWO_PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, ANOTHER_GIFT_ITEM_SSKU))
                        )
                )
        );

        bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(INACTIVE_BUNDLE),
                        shopPromoId(INACTIVE_BUNDLE),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        starts(clock.dateTime().minusYears(1)),
                        ends(clock.dateTime().minusDays(10)),
                        item(
                                condition(giftWithPurchase(FEED_ID, ONE_PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, TWO_PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, ANOTHER_GIFT_ITEM_SSKU))
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateNewPromoBundle() {
        PromoBundleDescription expectedDescription = createRandomBundle();
        Set<PromoBundleDescription> promos = bundleDao.select(
                activeOn(clock.dateTime()),
                withKeys(
                        expectedDescription.getPromoKey()
                )
        );

        Map<String, PromoBundleDescription> promoMap = asMap(promos, PromoBundleDescription::getPromoKey);

        assertThat(promoMap, hasKey(expectedDescription.getPromoKey()));

        assertThat(promoMap.get(expectedDescription.getPromoKey()), allOf(
                hasProperty("promoBundlesStrategyType", is(GIFT_WITH_PURCHASE)),
                hasProperty("status", is(PromoBundleStatus.ACTIVE)),
                hasProperty("restrictions", allOf(
                        hasProperty("bundleId", is(promoMap.get(expectedDescription.getPromoKey()).getId())),
                        hasProperty("minPurchasesPrice", nullValue()),
                        hasProperty("maxPurchasesPrice", nullValue()),
                        hasProperty("minQuantity", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("maxQuantity", nullValue())
                )),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("bundleId", is(promoMap.get(expectedDescription.getPromoKey()).getId())),
                                hasProperty("quantityInBundle", comparesEqualTo(BigDecimal.ONE)),
                                hasProperty("primary", comparesEqualTo(true)),
                                hasProperty("condition", allOf(
                                        hasProperty("feedId", is(FEED_ID)),
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", is(PROMO_ITEM_SSKU))
                                        ))
                                )),
                                hasProperty("restrictions", allOf(
                                        hasProperty("bundleId",
                                                is(promoMap.get(expectedDescription.getPromoKey()).getId())),
                                        hasProperty("minPurchasesPrice", nullValue()),
                                        hasProperty("maxPurchasesPrice", nullValue()),
                                        hasProperty("minQuantity", comparesEqualTo(BigDecimal.ONE)),
                                        hasProperty("maxQuantity", nullValue())
                                ))
                        ),
                        allOf(
                                hasProperty("bundleId", is(promoMap.get(expectedDescription.getPromoKey()).getId())),
                                hasProperty("quantityInBundle", comparesEqualTo(BigDecimal.ONE)),
                                hasProperty("primary", comparesEqualTo(false)),
                                hasProperty("condition", allOf(
                                        hasProperty("feedId", is(FEED_ID)),
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", is(GIFT_ITEM_SSKU))
                                        ))
                                )),
                                hasProperty("restrictions", allOf(
                                        hasProperty("bundleId",
                                                is(promoMap.get(expectedDescription.getPromoKey()).getId())),
                                        hasProperty("minPurchasesPrice", nullValue()),
                                        hasProperty("maxPurchasesPrice", nullValue()),
                                        hasProperty("minQuantity", comparesEqualTo(BigDecimal.ONE)),
                                        hasProperty("maxQuantity", nullValue())
                                ))
                        )
                ))
        ));
    }

    @Test
    public void shouldUpdatePromoBundle() {
        PromoBundleDescription expectedDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(randomString()),
                        shopPromoId(randomString()),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(10)),
                        item(
                                condition(giftWithPurchase(FEED_ID, ONE_PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, ANOTHER_GIFT_ITEM_SSKU))
                        )
                )
        );

        Set<PromoBundleDescription> promos = bundleDao.select(
                activeOn(clock.dateTime()),
                withKeys(
                        expectedDescription.getPromoKey()
                )
        );

        Map<String, PromoBundleDescription> promoMap = asMap(promos, PromoBundleDescription::getPromoKey);

        assertThat(promoMap, hasKey(expectedDescription.getPromoKey()));

        Long bundleId = bundleDao.update(promoMap.get(expectedDescription.getPromoKey()).toBuilder().status(
                PromoBundleStatus.INACTIVE).build()).getId();

        assertThat(bundleDao.getPromoBundleDescriptionById(bundleId), hasProperty("status",
                is(PromoBundleStatus.INACTIVE)));
    }

    @Test
    public void shouldUpdateExistingPromoBundle() {
        final PromoBundleDescription savedBundle = createRandomBundle();
        assertNotNull(savedBundle.getId());
        assertThat(bundleDao.getActiveSnapshotIdsByPromoKey(savedBundle.getPromoKey()), hasSize(0));

        final PromoBundleDescription newBundle = bundleService.createPromoBundle(savedBundle.toBuilder()
                .id(null)
                .promoKey(randomString())
                .startTime(savedBundle.getStartTime().minusYears(1L))
                .endTime(savedBundle.getStartTime().plusYears(2L))
                .build());
        assertEquals(savedBundle.getId(), newBundle.getId());
        assertNotEquals(savedBundle.getPromoKey(), newBundle.getPromoKey());
        assertThat(bundleDao.getActiveSnapshotIdsByPromoKey(savedBundle.getPromoKey()), hasSize(1));

        final Set<PromoBundleDescription> snapshots = bundleService.getActivePromoBundles(
                Collections.singleton(savedBundle.getPromoKey()));
        assertThat(snapshots, hasSize(1));
        assertTrue(snapshots.iterator().next().isSnapshotVersion());
    }

    @Test
    public void shouldBeAbleToCreateSeveralSnapshots() {
        final PromoBundleDescription firstBundle = createRandomBundle();
        assertNotNull(firstBundle.getId());
        assertThat(bundleDao.getActiveSnapshotIdsByPromoKey(firstBundle.getPromoKey()), hasSize(0));

        final PromoBundleDescription secondBundle = bundleService.createPromoBundle(firstBundle.toBuilder()
                .id(null)
                .promoKey(randomString())
                .startTime(firstBundle.getStartTime().minusYears(1L))
                .endTime(firstBundle.getStartTime().plusYears(2L))
                .build());
        assertEquals(firstBundle.getId(), secondBundle.getId());
        assertNotEquals(firstBundle.getPromoKey(), secondBundle.getPromoKey());
        assertThat(bundleDao.getActiveSnapshotIdsByPromoKey(firstBundle.getPromoKey()), hasSize(1));

        final PromoBundleDescription thirdBundle = bundleService.createPromoBundle(secondBundle.toBuilder()
                .id(null)
                .promoKey(randomString())
                .startTime(secondBundle.getStartTime().minusYears(1L))
                .endTime(secondBundle.getStartTime().plusYears(2L))
                .build());
        assertEquals(firstBundle.getId(), thirdBundle.getId());
        assertNotEquals(firstBundle.getPromoKey(), thirdBundle.getPromoKey());
        assertNotEquals(secondBundle.getPromoKey(), thirdBundle.getPromoKey());

        final Set<PromoBundleDescription> snapshots = bundleService.getActivePromoBundles(
                new HashSet<>(Arrays.asList(firstBundle.getPromoKey(), secondBundle.getPromoKey())));
        assertThat(snapshots, hasSize(2));
        snapshots.forEach(snapshot -> assertTrue(snapshot.isSnapshotVersion()));
    }

    @Test
    public void shouldBeAbleToRevertToInitialStateWithSeveralSnapshots() {
        final PromoBundleDescription initialState = createRandomBundle();
        assertNotNull(initialState.getId());
        assertThat(bundleDao.getActiveSnapshotIdsByPromoKey(initialState.getPromoKey()), hasSize(0));

        final PromoBundleDescription secondState = bundleService.createPromoBundle(initialState.toBuilder()
                .id(null)
                .promoKey(randomString())
                .startTime(initialState.getStartTime().minusYears(1L))
                .endTime(initialState.getStartTime().plusYears(2L))
                .build());
        assertEquals(initialState.getId(), secondState.getId());
        assertNotEquals(initialState.getPromoKey(), secondState.getPromoKey());
        assertThat(bundleDao.getActiveSnapshotIdsByPromoKey(initialState.getPromoKey()), hasSize(1));

        final PromoBundleDescription thirdState = bundleService.createPromoBundle(secondState.toBuilder()
                .id(null)
                .promoKey(randomString())
                .startTime(secondState.getStartTime().minusYears(1L))
                .endTime(secondState.getStartTime().plusYears(2L))
                .build());
        assertEquals(initialState.getId(), thirdState.getId());
        assertNotEquals(initialState.getPromoKey(), thirdState.getPromoKey());
        assertNotEquals(secondState.getPromoKey(), thirdState.getPromoKey());

        final PromoBundleDescription fourthStateEqualsToInitial = bundleService.createPromoBundle(
                initialState.toBuilder()
                        .id(null)
                        .build());
        assertEquals(initialState.getId(), fourthStateEqualsToInitial.getId());
        assertEquals(initialState.getPromoKey(), fourthStateEqualsToInitial.getPromoKey());
        assertFalse(fourthStateEqualsToInitial.isSnapshotVersion());
        assertThat(bundleDao.getActiveSnapshotIdsByPromoKey(initialState.getPromoKey()), hasSize(0));

        final Set<PromoBundleDescription> activePromoBundles = bundleService.getActivePromoBundles(
                new HashSet<>(
                        Arrays.asList(initialState.getPromoKey(), secondState.getPromoKey(),
                                thirdState.getPromoKey())));
        assertThat(activePromoBundles, hasSize(3));
        final Set<PromoBundleDescription> snapshots = activePromoBundles.stream()
                .filter(PromoBundleDescription::isSnapshotVersion)
                .collect(Collectors.toSet());
        assertEquals(2, snapshots.size());
    }

    @Test
    public void shouldGetNonePromosOnEmptyRequest() {
        Set<PromoBundleDescription> promos = bundleDao.select(
                activeOn(clock.dateTime()),
                PromoBundleDao.withKeys(List.of())
        );

        assertThat("no promos should get", promos, empty());
    }

    @Test
    public void shouldGetActivePromoBundleActions() {
        Set<PromoBundleDescription> promos = bundleDao.select(
                activeOn(clock.dateTime()),
                withKeys(
                        BUNDLE,
                        ANOTHER_BUNDLE
                )
        );

        assertThat("only 2 promos should get", promos, not(empty()));
        assertThat("only 2 promos should get", promos, hasSize(2));

        Map<String, PromoBundleDescription> promoMap = asMap(promos, PromoBundleDescription::getPromoKey);

        assertThat(promoMap, hasKey(BUNDLE));
        assertThat(promoMap, hasKey(ANOTHER_BUNDLE));

        assertThat(promoMap.get(BUNDLE), allOf(
                hasProperty("status", is(PromoBundleStatus.ACTIVE)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("bundleId", is(promoMap.get(BUNDLE).getId())),
                        hasProperty("quantityInBundle", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("primary", comparesEqualTo(true)),
                        hasProperty("condition", allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("mappings", hasItem(
                                        hasProperty("ssku", is(PROMO_ITEM_SSKU))
                                ))
                        ))
                ), allOf(
                        hasProperty("bundleId", is(promoMap.get(BUNDLE).getId())),
                        hasProperty("quantityInBundle", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("primary", comparesEqualTo(false)),
                        hasProperty("condition", allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("mappings", hasItem(
                                        hasProperty("ssku", is(GIFT_ITEM_SSKU))
                                ))
                        ))
                )))
        ));

        assertThat(promoMap.get(ANOTHER_BUNDLE), allOf(
                hasProperty("status", is(PromoBundleStatus.ACTIVE)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("bundleId", is(promoMap.get(ANOTHER_BUNDLE).getId())),
                        hasProperty("quantityInBundle", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("primary", comparesEqualTo(true)),
                        hasProperty("condition", allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("mappings", hasItem(
                                        hasProperty("ssku", is(ONE_PROMO_ITEM_SSKU))
                                ))
                        ))
                ), allOf(
                        hasProperty("bundleId", is(promoMap.get(ANOTHER_BUNDLE).getId())),
                        hasProperty("quantityInBundle", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("primary", comparesEqualTo(true)),
                        hasProperty("condition", allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("mappings", hasItem(
                                        hasProperty("ssku", is(TWO_PROMO_ITEM_SSKU))
                                ))
                        ))
                ), allOf(
                        hasProperty("bundleId", is(promoMap.get(ANOTHER_BUNDLE).getId())),
                        hasProperty("quantityInBundle", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("primary", comparesEqualTo(false)),
                        hasProperty("condition", allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("mappings", hasItem(
                                        hasProperty("ssku", is(ANOTHER_GIFT_ITEM_SSKU))
                                ))
                        ))
                )))
        ));
    }

    @Test
    public void shouldGetPromoBundleByFilter() {
        Set<PromoBundleDescription> promos = bundleDao.select(PROMO_KEY.eqTo(BUNDLE));

        assertThat("at least 1 promos should get", promos, not(emptyCollectionOf(PromoBundleDescription.class)));
        assertThat("at least 1 promos should get", promos, hasSize(1));

        Map<String, PromoBundleDescription> promoMap = asMap(promos, PromoBundleDescription::getPromoKey);

        assertThat(promoMap, hasKey(BUNDLE));
    }

    @Test
    public void shouldSaveFixedPrice() {
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey("some promo key"),
                shopPromoId("some promo"),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, ONE_PROMO_ITEM_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        when(ONE_PROMO_ITEM_SSKU),
                        then(ANOTHER_GIFT_ITEM_SSKU),
                        fixedPrice(1000)
                ))
        ));

        assertThat(bundleDescription.getItems(), notNullValue());
        assertThat(bundleDescription.getItems(), hasItem(hasProperty(
                "condition",
                hasProperty(
                        "mappings",
                        hasItem(
                                hasProperty("fixedPrice", comparesEqualTo(BigDecimal.valueOf(1000)))
                        )
                )
        )));
    }

    @Test
    public void shouldUpdateFixedPrice() {
        PromoBundleDescription promo = bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey("some promo key"),
                shopPromoId("some promo"),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, ONE_PROMO_ITEM_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        when(ONE_PROMO_ITEM_SSKU),
                        then(ANOTHER_GIFT_ITEM_SSKU),
                        fixedPrice(123)
                ))
        );

        bundleService.createPromoBundle(promo);

        PromoBundleDescription changed = bundleService.createPromoBundle(changeDescriptionOf(promo, changeItems(
                primaryItem(FEED_ID, ONE_PROMO_ITEM_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        when(ONE_PROMO_ITEM_SSKU),
                        then(ANOTHER_GIFT_ITEM_SSKU),
                        fixedPrice(12345)
                ))
        )));

        assertThat(changed.getItems(), notNullValue());
        assertThat(changed.getItems(), hasItem(hasProperty(
                "condition",
                hasProperty(
                        "mappings",
                        hasItem(
                                hasProperty("fixedPrice", comparesEqualTo(BigDecimal.valueOf(12345)))
                        )
                )
        )));
    }

    @Test
    public void shouldSaveDiscountProportion() {
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey("some promo key"),
                shopPromoId("some promo"),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, ONE_PROMO_ITEM_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        when(PROMO_ITEM_SSKU),
                        then(ANOTHER_GIFT_ITEM_SSKU),
                        proportion(9.9)
                ))
        ));

        assertThat(bundleDescription.getItems(), notNullValue());
        assertThat(bundleDescription.getItems(), hasItems(
                allOf(
                        hasProperty("primary", is(false)),
                        hasProperty(
                                "condition",
                                hasProperty(
                                        "mappings",
                                        hasItem(
                                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(9.9)))
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void shouldUpdateDiscountProportion() {
        PromoBundleDescription promo = bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey("some promo key"),
                shopPromoId("some promo"),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, ONE_PROMO_ITEM_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        when(PROMO_ITEM_SSKU),
                        then(ANOTHER_GIFT_ITEM_SSKU),
                        proportion(9.9)
                ))
        );

        bundleService.createPromoBundle(promo);

        PromoBundleDescription changed = bundleService.createPromoBundle(changeDescriptionOf(promo, changeItems(
                primaryItem(FEED_ID, ONE_PROMO_ITEM_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        when(PROMO_ITEM_SSKU),
                        then(ANOTHER_GIFT_ITEM_SSKU),
                        proportion(23.5)
                ))
        )));

        assertThat(changed.getItems(), notNullValue());
        assertThat(changed.getItems(), hasItems(
                allOf(
                        hasProperty("primary", is(false)),
                        hasProperty(
                                "condition",
                                hasProperty(
                                        "mappings",
                                        hasItem(
                                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(23.5)))
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void shouldSaveReturnRestriction() {
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey("some promo key"),
                shopPromoId("some promo"),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, ONE_PROMO_ITEM_SSKU)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, ANOTHER_GIFT_ITEM_SSKU))
                ),
                restrictions(
                        restrictReturn()
                )
        ));

        assertThat(bundleDescription.getRestrictions(), notNullValue());
        assertThat(bundleDescription.getRestrictions().isRestrictReturn(), is(true));
    }

    @Test
    public void shouldUpdateReturnRestriction() {
        PromoBundleDescription promo = bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey("some promo key"),
                shopPromoId("some promo"),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, ONE_PROMO_ITEM_SSKU)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, ANOTHER_GIFT_ITEM_SSKU))
                ),
                restrictions(
                        restrictReturn()
                )
        );

        bundleService.createPromoBundle(promo);

        PromoBundleDescription changed = bundleService.createPromoBundle(changeDescriptionOf(promo, restrictions()));

        assertThat(changed.getRestrictions(), notNullValue());
        assertThat(changed.getRestrictions().isRestrictReturn(), is(false));
    }

    private static <T> Map<String, T> asMap(Collection<T> collection, Function<T, String> method) {
        return collection.stream().collect(Collectors.toMap(method, identity()));
    }

    private PromoBundleDescription createRandomBundle() {
        return bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(randomString()),
                        shopPromoId(randomString()),
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
                )
        );
    }
}
