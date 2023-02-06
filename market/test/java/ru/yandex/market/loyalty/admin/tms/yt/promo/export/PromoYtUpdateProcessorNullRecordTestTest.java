package ru.yandex.market.loyalty.admin.tms.yt.promo.export;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.PromoYtUpdateProcessor;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueFlashDSL;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.CheapestAsGiftDSL;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.GenericBundleDSL;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.PromoDetailsDSL;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.flash.FlashPromoService;
import ru.yandex.market.loyalty.test.TestFor;

import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.GenericBundleDSL.relation;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.blueFlashDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.blueSetDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.cheapestAsGiftDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.giftWithPurchaseDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.promoDetails;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;

@TestFor(PromoYtUpdateProcessor.class)
public class PromoYtUpdateProcessorNullRecordTestTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String GENERIC_BUNDLE_KEY = "generic bundle promo";
    private static final String CHEAPEST_AS_GIFT_KEY = "cheapest as gift promo";
    private static final String BLUE_SET_KEY = "blue set promo";
    private static final String BLUE_FLASH_KEY = "blue flash promo";
    private static final long FEED_ID = 1234;


    @Autowired
    private PromoYtUpdateProcessor bundlesUpdateProcessor;
    @Autowired
    private PromoBundleService promoBundleService;
    @Autowired
    private FlashPromoService flashPromoService;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

    private void prepareData(PromoYtTestHelper.YtreeDataBuilder dataBuilder) {
        configurationService.enable(ConfigurationService.PROMO_YT_IMPORT_NULL_CHECK);

        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        dataBuilder
                .promo(FEED_ID, GENERIC_BUNDLE_KEY, promoDetails(
                        PromoDetailsDSL.shopPromoId(randomString()),
                        PromoDetailsDSL.starts(current.minusDays(1)),
                        PromoDetailsDSL.ends(current.plusDays(1)),

                        giftWithPurchaseDetails(
                                relation(
                                        GenericBundleDSL.primary(
                                                GenericBundleDSL.ssku(randomString())
                                        ),
                                        GenericBundleDSL.gift(
                                                GenericBundleDSL.item(
                                                        GenericBundleDSL.ssku(randomString())
                                                ),
                                                GenericBundleDSL.fixedPrice(100)
                                        )
                                )
                        )
                ))
                .promo(FEED_ID, CHEAPEST_AS_GIFT_KEY, promoDetails(
                        PromoDetailsDSL.shopPromoId(randomString()),
                        PromoDetailsDSL.starts(current),
                        PromoDetailsDSL.ends(current.plusDays(1)),

                        cheapestAsGiftDetails(
                                CheapestAsGiftDSL.quantityInBundle(3),
                                CheapestAsGiftDSL.ssku(FEED_ID, randomString()),
                                CheapestAsGiftDSL.ssku(FEED_ID, randomString()),
                                CheapestAsGiftDSL.ssku(FEED_ID, randomString())
                        )
                ))
                .promo(FEED_ID, BLUE_SET_KEY, promoDetails(
                        PromoDetailsDSL.shopPromoId(randomString()),
                        PromoDetailsDSL.starts(current),
                        PromoDetailsDSL.ends(current.plusDays(1)),

                        blueSetDetails(
                                BlueSetDSL.set(
                                        BlueSetDSL.item(
                                                BlueSetDSL.ssku(randomString()),
                                                BlueSetDSL.count(1),
                                                BlueSetDSL.discountProportion(5)
                                        ),
                                        BlueSetDSL.item(
                                                BlueSetDSL.ssku(randomString()),
                                                BlueSetDSL.count(1),
                                                BlueSetDSL.discountProportion(15)
                                        ),
                                        BlueSetDSL.item(
                                                BlueSetDSL.ssku(randomString()),
                                                BlueSetDSL.count(1),
                                                BlueSetDSL.discountProportion(25)
                                        )
                                )
                        )
                ))
                .promo(FEED_ID, BLUE_FLASH_KEY, promoDetails(
                        PromoDetailsDSL.shopPromoId(randomString()),
                        PromoDetailsDSL.starts(current),
                        PromoDetailsDSL.ends(current.plusDays(1)),

                        blueFlashDetails(
                                BlueFlashDSL.item(
                                        BlueFlashDSL.offer(FEED_ID, randomString()),
                                        BlueFlashDSL.fixedPrice(120),
                                        BlueFlashDSL.quantityBudget(5),
                                        BlueFlashDSL.moneyBudget(200)
                                )
                        )
                ));
    }

    @Test
    @Ignore     //todo: MARKETDISCOUNT-7353
    public void shouldImportDataIfNullRecordExists() {
        promoBundlesTestHelper.usingMock(db -> {
            promoBundlesTestHelper.addNullRecord(db);
            prepareData(db);
        }, bundlesUpdateProcessor::updatePromoFromYt);

        assertThat(promoBundleService.getActivePromoKeys(), hasItems(
                GENERIC_BUNDLE_KEY,
                CHEAPEST_AS_GIFT_KEY,
                BLUE_SET_KEY
        ));

        assertThat(flashPromoService.getActivePromoKeys(), hasItems(
                BLUE_FLASH_KEY
        ));
    }

    @Test
    public void shouldNotImportDataIfNullRecordIsAbsent() {
        promoBundlesTestHelper.usingMock(this::prepareData, bundlesUpdateProcessor::updatePromoFromYt);

        assertThat(promoBundleService.getActivePromoKeys(), empty());
        assertThat(flashPromoService.getActivePromoKeys(), empty());
    }

    @Test
    @Ignore     //todo: MARKETDISCOUNT-7353
    public void shouldNotDisableDataIfNullRecordIsAbsent() {
        shouldImportDataIfNullRecordExists();

        promoBundlesTestHelper.usingMock(this::prepareData, bundlesUpdateProcessor::updatePromoFromYt);

        assertThat(promoBundleService.getActivePromoKeys(), hasItems(
                GENERIC_BUNDLE_KEY,
                CHEAPEST_AS_GIFT_KEY,
                BLUE_SET_KEY
        ));

        assertThat(flashPromoService.getActivePromoKeys(), hasItems(
                BLUE_FLASH_KEY
        ));
    }
}
