package ru.yandex.market.loyalty.admin.tms.yt.promo.export;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.PromoYtUpdateProcessor;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.PromoDetailsDSL;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleStatus;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.test.TestFor;
import Market.Promo.Promo.PromoDetails;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Set;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL.count;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL.discountProportion;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL.set;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL.ssku;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.blueSetDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.promoDetails;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
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

@TestFor(PromoYtUpdateProcessor.class)
public class BlueSetPromoYtUpdateProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SOME_PROMO_KEY = "some promo";
    private static final String ANOTHER_PROMO_KEY = "another promo";
    private static final String ANOTHER_ONE_PROMO_KEY = "another one promo";
    private static final String SSKU_1 = "ssku 1";
    private static final String SSKU_2 = "ssku 2";
    private static final String SSKU_3 = "ssku 3";
    private static final String SSKU_4 = "ssku 4";
    private static final long FEED_ID = 1234;
    private static final long ANOTHER_FEED_ID = 1235;
    private static final long ANOTHER_ONE_FEED_ID = 1236;

    @Autowired
    private PromoYtUpdateProcessor bundlesUpdateProcessor;
    @Autowired
    private PromoBundleService promoBundleService;
    @Autowired
    private PromoBundleDao promoBundleDao;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

    private String shopPromoId;
    private PromoDetails.Builder typicalDetails;

    @Before
    public void configure() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());
        shopPromoId = randomString();

        typicalDetails = promoDetails(
                PromoDetailsDSL.shopPromoId(shopPromoId),
                PromoDetailsDSL.starts(current),
                PromoDetailsDSL.ends(current.plusDays(1)),

                blueSetDetails(
                        set(
                                BlueSetDSL.item(
                                        ssku(SSKU_1),
                                        count(1),
                                        discountProportion(30)
                                ),
                                BlueSetDSL.item(
                                        ssku(SSKU_2),
                                        count(1),
                                        discountProportion(30)
                                ),
                                BlueSetDSL.item(
                                        ssku(SSKU_3),
                                        count(1),
                                        discountProportion(30)
                                )
                        ),
                        set(
                                BlueSetDSL.item(
                                        ssku(SSKU_4),
                                        count(1),
                                        discountProportion(20)
                                ),
                                BlueSetDSL.item(
                                        ssku(SSKU_3),
                                        count(1),
                                        discountProportion(20)
                                )
                        )
                )
        );
    }

    private void prepareData(PromoYtTestHelper.YtreeDataBuilder dataBuilder) {
        promoBundlesTestHelper.addNullRecord(dataBuilder);
        dataBuilder
                .promo(FEED_ID, SOME_PROMO_KEY, typicalDetails.clone())
                .promo(ANOTHER_FEED_ID, ANOTHER_PROMO_KEY, typicalDetails.clone().setShopPromoId(randomString()))
                .promo(ANOTHER_ONE_FEED_ID, ANOTHER_ONE_PROMO_KEY, typicalDetails.clone().setShopPromoId(randomString()));
    }

    @Test
    public void shouldSavePromosFromYt() {
        Set<String> persisted = promoBundleService.getActivePromoKeys();

        assertThat(persisted, empty());

        promoBundlesTestHelper.usingMock(this::prepareData, bundlesUpdateProcessor::updatePromoFromYt);

        persisted = promoBundleService.getActivePromoKeys();
        assertThat(persisted, hasSize(3));
        assertThat(persisted, hasItems(
                SOME_PROMO_KEY,
                ANOTHER_PROMO_KEY,
                ANOTHER_ONE_PROMO_KEY
        ));
    }

    @Test
    public void shouldSavePreviousSnapshotOnUpdatePromosFromYt() {
        PromoBundleDescription expectedDescription = promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                shopPromoId(shopPromoId),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                promoKey("some expired promo"),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(SSKU_1, 30),
                                proportion(SSKU_2, 30)
                        )),
                        primary()
                )
        ));

        assertThat(expectedDescription, notNullValue());

        promoBundlesTestHelper.usingMock(db -> {
            promoBundlesTestHelper.addNullRecord(db);
            db.promo(FEED_ID, SOME_PROMO_KEY, typicalDetails.clone());
        }, bundlesUpdateProcessor::updatePromoFromYt);

        Set<String> persisted = promoBundleService.getActivePromoKeys();
        assertThat(persisted, hasSize(2));
        assertThat(persisted, hasItems(
                SOME_PROMO_KEY,
                "some expired promo"
        ));
    }

    @Test
    public void shouldDeactivatePromos() {
        PromoBundleDescription expectedDescription = promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                promoKey("some expired promo"),
                shopPromoId("some expired promo"),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(SSKU_1, 30),
                                proportion(SSKU_2, 30)
                        )),
                        primary()
                )
        ));

        assertThat(expectedDescription, hasProperty("status", is(PromoBundleStatus.ACTIVE)));
        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        promoBundlesTestHelper.usingMock(this::prepareData, bundlesUpdateProcessor::updatePromoFromYt);

        assertThat(
                promoBundleService.getActivePromoBundles(Collections.singleton("some expired promo")), empty()
        );

        assertThat(
                promoBundleDao.getPromoBundleDescriptionById(expectedDescription.getId()),
                hasProperty("status", is(PromoBundleStatus.INACTIVE))
        );

        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.INACTIVE))
        );
    }

    @Test
    public void shouldNotDeactivateSnapshotWhenPromoKeyWasChanged() {
        PromoBundleDescription oldDescription = promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                shopPromoId(shopPromoId),
                promoKey("old_promo_key"),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(SSKU_1, 30),
                                proportion(SSKU_2, 30),
                                proportion(SSKU_3, 30)
                        )),
                        primary()
                )
        ));

        assertThat(oldDescription, hasProperty("status", is(PromoBundleStatus.ACTIVE)));
        assertThat(oldDescription, hasProperty("promoKey", is("old_promo_key")));
        assertThat(
                promoService.getPromo(oldDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "new_promo_key", typicalDetails.clone());
                },
                bundlesUpdateProcessor::updatePromoFromYt
        );

        assertThat(
                promoBundleService.getActivePromoBundles(Collections.singleton("new_promo_key")),
                hasItem(allOf(
                        hasProperty("id", is(oldDescription.getId())),
                        hasProperty("status", is(PromoBundleStatus.ACTIVE))
                ))
        );

        assertThat(
                promoService.getPromo(oldDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        assertThat(
                promoBundleService.getActivePromoBundles(Collections.singleton("old_promo_key")),
                hasItem(allOf(
                        hasProperty("id", not(oldDescription.getId())),
                        hasProperty("status", is(PromoBundleStatus.ACTIVE)),
                        hasProperty("snapshotVersion", is(true)),
                        hasProperty("deactivationTime", nullValue())
                ))
        );
    }

    @Test
    public void shouldSavePreviousSnapshotsOnDifferentChangesFromYt() {
        promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                shopPromoId(shopPromoId),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                promoKey(SOME_PROMO_KEY),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(SSKU_1, 30),
                                proportion(SSKU_2, 30)
                        )),
                        primary()
                )
        ));

        promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                shopPromoId(shopPromoId),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                promoKey(ANOTHER_PROMO_KEY),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(SSKU_1, 30),
                                proportion(SSKU_2, 30),
                                proportion(SSKU_3, 30)
                        )),
                        primary()
                )
        ));

        promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                shopPromoId(shopPromoId),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                promoKey(ANOTHER_ONE_PROMO_KEY),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(SSKU_1, 30),
                                proportion(SSKU_2, 30),
                                proportion(SSKU_3, 30),
                                proportion(SSKU_4, 30)
                        )),
                        primary()
                )
        ));

        Set<String> persisted = promoBundleService.getActivePromoKeys();
        assertThat(persisted, hasSize(3));
        assertThat(persisted, hasItems(
                SOME_PROMO_KEY,
                ANOTHER_PROMO_KEY,
                ANOTHER_ONE_PROMO_KEY
        ));
    }
}
