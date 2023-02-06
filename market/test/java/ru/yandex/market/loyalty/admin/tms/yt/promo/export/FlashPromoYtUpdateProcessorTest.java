package ru.yandex.market.loyalty.admin.tms.yt.promo.export;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.PromoYtUpdateProcessor;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueFlashDSL;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.PromoDetailsDSL;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.flash.FlashPromoDao;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoStatus;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.flash.FlashPromoService;
import ru.yandex.market.loyalty.test.TestFor;
import Market.Promo.Promo.PromoDetails;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Set;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.blueFlashDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.promoDetails;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.ends;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.flashDescription;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.source;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.starts;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.status;

@TestFor(PromoYtUpdateProcessor.class)
public class FlashPromoYtUpdateProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SHOP_PROMO_ID = "some promo";
    private static final String SOME_PROMO_KEY = "some promo";
    private static final String ANOTHER_PROMO_KEY = "another promo";
    private static final String ANOTHER_ONE_PROMO_KEY = "another one promo";
    private static final String SSKU_1 = "primary ssku 1";
    private static final String SSKU_2 = "primary ssku 2";
    private static final String SSKU_3 = "primary ssku 3";
    private static final String SSKU_4 = "primary ssku 4";
    private static final long FEED_ID = 1234;
    private static final long ANOTHER_FEED_ID = 1235;
    private static final long ANOTHER_ONE_FEED_ID = 1236;

    @Autowired
    private PromoYtUpdateProcessor ytUpdateProcessor;
    @Autowired
    private FlashPromoService flashPromoService;
    @Autowired
    private FlashPromoDao flashPromoDao;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

    private PromoDetails.Builder flashPromoDetails(long feedId) {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        return promoDetails(
                PromoDetailsDSL.shopPromoId(SHOP_PROMO_ID),
                PromoDetailsDSL.starts(current),
                PromoDetailsDSL.ends(current.plusDays(1)),

                blueFlashDetails(
                        BlueFlashDSL.item(
                                BlueFlashDSL.offer(feedId, SSKU_1),
                                BlueFlashDSL.fixedPrice(120),
                                BlueFlashDSL.quantityBudget(5),
                                BlueFlashDSL.moneyBudget(200)
                        ),
                        BlueFlashDSL.item(
                                BlueFlashDSL.offer(feedId, SSKU_2),
                                BlueFlashDSL.fixedPrice(220),
                                BlueFlashDSL.quantityBudget(5),
                                BlueFlashDSL.moneyBudget(200)
                        ),
                        BlueFlashDSL.item(
                                BlueFlashDSL.offer(feedId, SSKU_3),
                                BlueFlashDSL.fixedPrice(320),
                                BlueFlashDSL.quantityBudget(5),
                                BlueFlashDSL.moneyBudget(200)
                        ),
                        BlueFlashDSL.item(
                                BlueFlashDSL.offer(feedId, SSKU_4),
                                BlueFlashDSL.fixedPrice(420),
                                BlueFlashDSL.quantityBudget(5),
                                BlueFlashDSL.moneyBudget(200)
                        )
                )

        );
    }

    private void prepareData(PromoYtTestHelper.YtreeDataBuilder dataBuilder) {
        promoBundlesTestHelper.addNullRecord(dataBuilder);
        dataBuilder
                .promo(FEED_ID, SOME_PROMO_KEY, flashPromoDetails(FEED_ID).setShopPromoId(randomString()))
                .promo(ANOTHER_FEED_ID, ANOTHER_PROMO_KEY, flashPromoDetails(ANOTHER_FEED_ID).setShopPromoId(randomString()))
                .promo(ANOTHER_ONE_FEED_ID, ANOTHER_ONE_PROMO_KEY,
                        flashPromoDetails(ANOTHER_ONE_FEED_ID).setShopPromoId(randomString()));
    }

    @Test
    public void shouldUpdateFlashPromoFromYt() {
        promoBundlesTestHelper.usingMock(this::prepareData, ytUpdateProcessor::updatePromoFromYt);

        Set<String> persisted = flashPromoService.getActivePromoKeys();
        assertThat(persisted, hasSize(3));
        assertThat(persisted, hasItems(
                SOME_PROMO_KEY,
                ANOTHER_PROMO_KEY,
                ANOTHER_ONE_PROMO_KEY
        ));
    }

    @Test
    // disabled inactive logic for flash promos https://st.yandex-team.ru/MARKETDISCOUNT-6937
    public void shouldNotDeactivateWhenDeactivatePromoBundles() {
        FlashPromoDescription expectedDescription = flashPromoService.createPromo(flashDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey("some expired promo"),
                shopPromoId("some expired promo"),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1))
        ));

        assertThat(expectedDescription, hasProperty("status", is(FlashPromoStatus.ACTIVE)));
        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        promoBundlesTestHelper.usingMock(this::prepareData, ytUpdateProcessor::updatePromoFromYt);

        assertThat(
                flashPromoService.getActivePromosByPromoKey(Collections.singleton("some expired promo")),
                hasItem(allOf(
                        hasProperty("id", is(expectedDescription.getId())),
                        hasProperty("status", is(FlashPromoStatus.ACTIVE))
                ))
        );

        assertThat(
                flashPromoDao.get(expectedDescription.getId()),
                hasProperty("status", is(FlashPromoStatus.ACTIVE))
        );

        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );
    }

    @Test
    public void shouldReactivatePromoBundles() {
        FlashPromoDescription expectedDescription = flashPromoService.createPromo(flashDescription(
                promoSource(LOYALTY_VALUE),
                status(FlashPromoStatus.INACTIVE),
                feedId(12345),
                promoKey("some inactive promo"),
                shopPromoId(SHOP_PROMO_ID),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1))
        ));

        assertThat(expectedDescription, hasProperty("status", is(FlashPromoStatus.INACTIVE)));
        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.INACTIVE))
        );

        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(12345, "some activated promo", flashPromoDetails(12345));
                },
                ytUpdateProcessor::updatePromoFromYt
        );

        assertThat(
                flashPromoService.getActivePromosByPromoKey(Collections.singleton("some inactive promo")),
                empty()
        );

        assertThat(
                flashPromoService.getActivePromosByPromoKey(Collections.singleton("some activated promo")),
                hasItem(allOf(
                        hasProperty("id", is(expectedDescription.getId())),
                        hasProperty("status", is(FlashPromoStatus.ACTIVE))
                ))
        );

        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );
    }

    @Test
    public void shouldNotDeactivateSnapshotWhenPromoKeyWasChanged() {
        // Arrange
        final long feedId = 12345L;
        FlashPromoDescription oldDescription = flashPromoService.createPromo(flashDescription(
                promoSource(LOYALTY_VALUE),
                feedId(12345),
                promoKey("old_promo_key"),
                shopPromoId(SHOP_PROMO_ID),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1))
        ));

        assertThat(oldDescription, hasProperty("status", is(FlashPromoStatus.ACTIVE)));
        assertThat(oldDescription, hasProperty("promoKey", is("old_promo_key")));
        assertThat(
                promoService.getPromo(oldDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        // Act
        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(feedId, "new_promo_key", flashPromoDetails(feedId));
                },
                ytUpdateProcessor::updatePromoFromYt
        );

        // Assert
        assertThat(
                flashPromoService.getActivePromosByPromoKey(Collections.singleton("new_promo_key")),
                hasItem(allOf(
                        hasProperty("id", is(oldDescription.getId())),
                        hasProperty("status", is(FlashPromoStatus.ACTIVE))
                ))
        );

        assertThat(
                promoService.getPromo(oldDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        // Должен появиться активный фолбэк со старым promo_key
        assertThat(
                flashPromoService.getActivePromosByPromoKey(Collections.singleton("old_promo_key")),
                hasItem(allOf(
                        hasProperty("id", not(oldDescription.getId())),
                        hasProperty("status", is(FlashPromoStatus.ACTIVE)),
                        hasProperty("snapshotVersion", is(true)),
                        hasProperty("deactivationTime", nullValue())
                ))
        );
    }


    @Test
    public void shouldSavePromoFlashLandingUrl() {
        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "PROMO_KEY", flashPromoDetails(FEED_ID).setLandingUrl("created_landing_url"));
                },
                ytUpdateProcessor::updatePromoFromYt
        );

        Set<FlashPromoDescription> promos = flashPromoService.getActivePromosByPromoKey(Collections.singleton(
                "PROMO_KEY"));

        assertThat(promos, hasSize(1));
        FlashPromoDescription promo = promos.iterator().next();
        assertThat(promo.getPromoId(), notNullValue());

        assertThat(
                promoService.getPromoParamValue(promo.getPromoId(), PromoParameterName.LANDING_URL).orElse(""),
                is("created_landing_url"));
    }

    @Test
    public void shouldUpdatePromoFlashLandingUrl() {
        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "PROMO_KEY", flashPromoDetails(FEED_ID).setLandingUrl("old_landing_url"));
                },
                ytUpdateProcessor::updatePromoFromYt
        );

        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "PROMO_KEY", flashPromoDetails(FEED_ID).setLandingUrl("new_landing_url"));
                },
                ytUpdateProcessor::updatePromoFromYt
        );

        Set<FlashPromoDescription> promos = flashPromoService.getActivePromosByPromoKey(Collections.singleton("PROMO_KEY"));

        assertThat(promos, hasSize(1));
        FlashPromoDescription promo = promos.iterator().next();
        assertThat(promo.getPromoId(), notNullValue());

        assertThat(
                promoService.getPromoParamValue(promo.getPromoId(), PromoParameterName.LANDING_URL).orElse(""),
                is("new_landing_url"));
    }
}
