package ru.yandex.market.loyalty.admin.yt.mapper;

import org.junit.Test;

import ru.yandex.market.loyalty.admin.yt.mapper.importer.concrete.GiftWithPurchaseYtMapper;
import ru.yandex.market.loyalty.admin.yt.model.PromoYtDescription;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.GenericBundle;
import Market.Promo.Promo.PromoDetails.GenericBundle.BundleContent;
import Market.Promo.Promo.PromoDetails.GenericBundle.PromoItem;
import Market.Promo.Promo.PromoDetails.GenericBundle.SecondaryItem;
import Market.Promo.Promo.PromoDetails.Money;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;

public class GiftWithPurchaseYtMapperTest {
    private static final long FEED_ID = 1234;
    private static final String OFFER_1 = "offer 1";
    private static final String OFFER_2 = "offer 2";

    private final GiftWithPurchaseYtMapper mapper = new GiftWithPurchaseYtMapper();

    @Test
    public void shouldMapFixedDiscountDescription() {
        PromoBundleDescription description = mapper.mapDescription(new PromoYtDescription(
                0,
                FEED_ID,
                randomString(),
                ReportPromoType.GENERIC_BUNDLE,
                YtSource.FIRST_PARTY_PIPELINE,
                PromoDetails.newBuilder()
                        .setGenericBundle(GenericBundle.newBuilder()
                                .addBundlesContent(BundleContent.newBuilder()
                                        .setPrimaryItem(PromoItem.newBuilder()
                                                .setOfferId(OFFER_1)
                                                .setCount(1)
                                                .build())
                                        .setSecondaryItem(SecondaryItem.newBuilder()
                                                .setItem(PromoItem.newBuilder()
                                                        .setOfferId(OFFER_2)
                                                        .setCount(1)
                                                        .build())
                                                .setDiscountPrice(Money.newBuilder()
                                                        .setCurrency("RUB")
                                                        .setValue(100)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build()
        ));

        assertThat(description.getItems(), hasSize(2));
        assertThat(description.getItems(), hasItems(allOf(
                hasProperty("primary", is(false)),
                hasProperty("condition", hasProperty("mappings", hasItem(allOf(
                        hasProperty("ssku", is(OFFER_2)),
                        hasProperty("proportion", nullValue()),
                        hasProperty("fixedPrice", comparesEqualTo(BigDecimal.ONE))
                ))))
        ), allOf(
                hasProperty("primary", is(true)),
                hasProperty("condition", hasProperty("mappings", hasItem(allOf(
                        hasProperty("ssku", is(OFFER_1)),
                        hasProperty("proportion", nullValue()),
                        hasProperty("fixedPrice", nullValue())
                ))))
        )));
    }

    @Test
    public void shouldMapProportionalDiscountDescription() {
        PromoBundleDescription description = mapper.mapDescription(new PromoYtDescription(
                0,
                FEED_ID,
                randomString(),
                ReportPromoType.GENERIC_BUNDLE,
                YtSource.FIRST_PARTY_PIPELINE,
                PromoDetails.newBuilder()
                        .setGenericBundle(GenericBundle.newBuilder()
                                .addBundlesContent(BundleContent.newBuilder()
                                        .setPrimaryItem(PromoItem.newBuilder()
                                                .setOfferId(OFFER_1)
                                                .setCount(1)
                                                .build())
                                        .setSecondaryItem(SecondaryItem.newBuilder()
                                                .setItem(PromoItem.newBuilder()
                                                        .setOfferId(OFFER_2)
                                                        .setCount(1)
                                                        .build())
                                                .setDiscountPrice(Money.newBuilder()
                                                        .setCurrency("RUB")
                                                        .setValue(0)
                                                        .build())
                                                .build())
                                        .setSpreadDiscount(30.)
                                        .build()))
                        .build()
        ));

        assertThat(description.getItems(), hasSize(2));
        assertThat(description.getItems(), hasItems(allOf(
                hasProperty("primary", is(false)),
                hasProperty("condition", hasProperty("mappings", hasItem(allOf(
                        hasProperty("ssku", is(OFFER_2)),
                        hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(70))),
                        hasProperty("fixedPrice", nullValue())
                ))))
        ), allOf(
                hasProperty("primary", is(true)),
                hasProperty("condition", hasProperty("mappings", hasItem(allOf(
                        hasProperty("ssku", is(OFFER_1)),
                        hasProperty("proportion", nullValue()),
                        hasProperty("fixedPrice", nullValue())
                ))))
        )));
    }
}
