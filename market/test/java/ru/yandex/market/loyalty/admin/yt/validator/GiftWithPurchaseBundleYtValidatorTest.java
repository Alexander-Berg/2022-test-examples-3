package ru.yandex.market.loyalty.admin.yt.validator;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.model.PromoYtDescription;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.GenericBundle.BundleContent;
import Market.Promo.Promo.PromoDetails.GenericBundle.PromoItem;
import Market.Promo.Promo.PromoDetails.GenericBundle.SecondaryItem;
import Market.Promo.Promo.PromoDetails.Restrictions;

import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static NMarket.Common.Promo.Promo.EPromoType;

public class GiftWithPurchaseBundleYtValidatorTest extends MarketLoyaltyAdminMockedDbTest {

    private static final long FEED_ID = 1234;
    private static final String SOME_PROMO_KEY = "some promo";


    @Autowired
    private GiftWithPurchaseBundleYtValidator validator;

    @Test
    public void shouldLogCapacityLimitViolation() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        assertThat(validator.validate(new PromoYtDescription(
                0,
                FEED_ID,
                SOME_PROMO_KEY,
                ReportPromoType.BLUE_FLASH,
                YtSource.FIRST_PARTY_PIPELINE,
                PromoDetails.newBuilder()
                        .setStartDate(current.toEpochSecond())
                        .setEndDate(current.plusDays(1).toEpochSecond())
                        .setShopPromoId(SOME_PROMO_KEY)
                        .setType(GIFT_WITH_PURCHASE.getReportCode())
                        .setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                                .addAllBundlesContent(
                                        generateBundlesContent(
                                                configurationService.genericBundleCapacityMax() + 2))
                                .build())
                        .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                .addRestrictedPromoTypes(EPromoType.MarketBonus)
                                .build())
                        .build()
        )).toString(), containsString("items summary count should be less then"));
    }

    @Test
    public void shouldPassWithoutCapacityLimitViolation() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        assertThat(validator.validate(new PromoYtDescription(
                0,
                FEED_ID,
                SOME_PROMO_KEY,
                ReportPromoType.BLUE_FLASH,
                YtSource.FIRST_PARTY_PIPELINE,
                PromoDetails.newBuilder()
                        .setStartDate(current.toEpochSecond())
                        .setEndDate(current.plusDays(1).toEpochSecond())
                        .setShopPromoId(SOME_PROMO_KEY)
                        .setType(GIFT_WITH_PURCHASE.getReportCode())
                        .setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                                .addAllBundlesContent(
                                        generateBundlesContent(
                                                configurationService.genericBundleCapacityMax()))
                                .build())
                        .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                .addRestrictedPromoTypes(EPromoType.MarketBonus)
                                .build())
                        .build()
        )).toString(), not(containsString("items summary count should be less then")));
    }

    private Iterable<? extends BundleContent> generateBundlesContent(
            int genericBundleCapacityMax
    ) {
        return IntStream.range(0, genericBundleCapacityMax / 2)
                .mapToObj(i -> BundleContent.newBuilder()
                        .setPrimaryItem(PromoItem.newBuilder()
                                .setCount(1)
                                .setOfferId(randomString())
                                .build())
                        .setSecondaryItem(SecondaryItem.newBuilder()
                                .setItem(PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId(randomString())
                                        .build())
                                .setDiscountPrice(PromoDetails.Money.newBuilder()
                                        .setCurrency(Currency.RUR.name())
                                        .setValue(100)
                                        .build())
                                .build())
                        .setSpreadDiscount(10)
                        .build())
                .collect(toList());
    }
}
