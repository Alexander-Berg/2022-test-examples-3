package ru.yandex.market.loyalty.admin.tms.yt.promo.export;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.PromoYtUpdateProcessor;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper.ThrowableConsumer;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper.YtreeDataBuilder;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.test.TestFor;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.Builder;
import Market.Promo.Promo.PromoDetails.GenericBundle;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

@TestFor(PromoYtUpdateProcessor.class)
public class PromoBundlesYtUpdateProcessorFallbackSpecificTest extends MarketLoyaltyAdminMockedDbTest {

    private static final String DEFAULT_SHOP_PROMO_ID = "altuninf_test2";

    @Autowired
    private PromoYtUpdateProcessor bundlesUpdateProcessor;
    @Autowired
    private PromoBundleService promoBundleService;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

    private Builder typicalDetails;

    @Before
    public void configure() {
        clock.setDate(toDate(1566680400L, TimeUnit.SECONDS));

        typicalDetails = PromoDetails.newBuilder()
                .setStartDate(1561928400)
                .setEndDate(1596229199)
                .setType(GIFT_WITH_PURCHASE.getReportCode())
                .setShopPromoId(DEFAULT_SHOP_PROMO_ID)
                .setGenericBundle(GenericBundle.newBuilder()
                        .addBundlesContent(GenericBundle.BundleContent.newBuilder()
                                .setPrimaryItem(GenericBundle.PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId("00065.00026.100126179060")
                                        .build())
                                .setSecondaryItem(GenericBundle.SecondaryItem.newBuilder()
                                        .setItem(GenericBundle.PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126179112")
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .addBundlesContent(GenericBundle.BundleContent.newBuilder()
                                .setPrimaryItem(GenericBundle.PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId("00065.00026.100126178009")
                                        .build())
                                .setSecondaryItem(GenericBundle.SecondaryItem.newBuilder()
                                        .setItem(GenericBundle.PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126179112")
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .addBundlesContent(GenericBundle.BundleContent.newBuilder()
                                .setPrimaryItem(GenericBundle.PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId("00065.00026.100126178007")
                                        .build())
                                .setSecondaryItem(GenericBundle.SecondaryItem.newBuilder()
                                        .setItem(GenericBundle.PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126179112")
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .addBundlesContent(GenericBundle.BundleContent.newBuilder()
                                .setPrimaryItem(GenericBundle.PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId("00065.00026.100126178011")
                                        .build())
                                .setSecondaryItem(GenericBundle.SecondaryItem.newBuilder()
                                        .setItem(GenericBundle.PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126179112")
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .addBundlesContent(GenericBundle.BundleContent.newBuilder()
                                .setPrimaryItem(GenericBundle.PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId("00065.00009.УЦУАЦУ")
                                        .build())
                                .setSecondaryItem(GenericBundle.SecondaryItem.newBuilder()
                                        .setItem(GenericBundle.PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126179112")
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .build());
    }

    @SafeVarargs
    private final ThrowableConsumer<YtreeDataBuilder> prepareData(
            Consumer<GenericBundle.Builder> recordCustomizer, Pair<Long, String>... params
    ) {
        return dataBuilder -> {
            promoBundlesTestHelper.addNullRecord(dataBuilder);
            Builder customized = typicalDetails.clone();
            GenericBundle.Builder builder = GenericBundle.newBuilder(customized.getGenericBundle());
            customized.setGenericBundle(builder);
            recordCustomizer.accept(builder);
            customized.setGenericBundle(builder.build());
            Arrays.stream(params).forEach(pair ->
                    dataBuilder.promo(pair.getKey(), pair.getValue(), customized.clone()));
        };
    }

    private final ThrowableConsumer<PromoYtTestHelper.YtreeDataPromoStorageBuilder> prepareDataPromoStorage() {
        return dataBuilder -> {
            promoBundlesTestHelper.addNullPromoStorageRecord(dataBuilder);
        };
    }

    @Test
    public void shouldImportDataWithCorrectFallbacks() {
        clock.setDate(toDate(1566680400L, TimeUnit.SECONDS));

        promoBundlesTestHelper.usingMock(prepareData(bb ->
                        bb.setAllowBerubonus(true),
                Pair.of(200369699L, "GSJsrLSb/4kMVAwbmbhm7w=="),
                Pair.of(200396944L, "XniP0ct/9pGyoiGf0EQ0fQ=="),
                Pair.of(200396943L, "0IIAWVjZaeoDVP42DHDmWg=="),
                Pair.of(200344511L, "slyk0mkRSE+jcqunXnUhXQ==")

        ), bundlesUpdateProcessor::updatePromoFromYt);

        Set<String> persisted = promoBundleService.getActivePromoKeys();
        assertThat(persisted, hasSize(4));
        assertThat(persisted, hasItems(
                "GSJsrLSb/4kMVAwbmbhm7w==",
                "XniP0ct/9pGyoiGf0EQ0fQ==",
                "0IIAWVjZaeoDVP42DHDmWg==",
                "slyk0mkRSE+jcqunXnUhXQ=="
        ));

        promoBundlesTestHelper.usingMock(prepareData(bb ->
                        bb.setAllowBerubonus(false),
                Pair.of(200369699L, "RxMbxJJourshDodEqZX8eA=="),
                Pair.of(200396944L, "nVMWhGOklducuCbkh5hwXw=="),
                Pair.of(200396943L, "AYbvJW+zlc7AqxxAi3kJ+g=="),
                Pair.of(200344511L, "2hbau45CIvxJafbtpXBbLA==")

        ), bundlesUpdateProcessor::updatePromoFromYt);

        persisted = promoBundleService.getActivePromoKeys();
        assertThat(persisted, hasSize(8));
        assertThat(persisted, hasItems(
                "RxMbxJJourshDodEqZX8eA==",
                "nVMWhGOklducuCbkh5hwXw==",
                "AYbvJW+zlc7AqxxAi3kJ+g==",
                "2hbau45CIvxJafbtpXBbLA==",

                "GSJsrLSb/4kMVAwbmbhm7w==",
                "XniP0ct/9pGyoiGf0EQ0fQ==",
                "0IIAWVjZaeoDVP42DHDmWg==",
                "slyk0mkRSE+jcqunXnUhXQ=="
        ));

        promoBundlesTestHelper.usingMock(prepareData(bb ->
                        bb.setAllowBerubonus(true),
                Pair.of(200369699L, "GSJsrLSb/4kMVAwbmbhm7w=="),
                Pair.of(200396944L, "XniP0ct/9pGyoiGf0EQ0fQ=="),
                Pair.of(200396943L, "0IIAWVjZaeoDVP42DHDmWg=="),
                Pair.of(200344511L, "slyk0mkRSE+jcqunXnUhXQ==")

        ), bundlesUpdateProcessor::updatePromoFromYt);

        persisted = promoBundleService.getActivePromoKeys();
        assertThat(persisted, hasSize(8));
        assertThat(persisted, hasItems(
                "RxMbxJJourshDodEqZX8eA==",
                "nVMWhGOklducuCbkh5hwXw==",
                "AYbvJW+zlc7AqxxAi3kJ+g==",
                "2hbau45CIvxJafbtpXBbLA==",

                "GSJsrLSb/4kMVAwbmbhm7w==",
                "XniP0ct/9pGyoiGf0EQ0fQ==",
                "0IIAWVjZaeoDVP42DHDmWg==",
                "slyk0mkRSE+jcqunXnUhXQ=="
        ));

        promoBundlesTestHelper.usingPromoStorageMock(prepareDataPromoStorage(),
                bundlesUpdateProcessor::updateFromPromoStorage);
    }
}
