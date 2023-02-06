package ru.yandex.market.loyalty.admin.yt.mapper;

import org.junit.Test;

import ru.yandex.market.loyalty.admin.yt.mapper.importer.concrete.BlueSetPromoYtMapper;
import ru.yandex.market.loyalty.admin.yt.model.PromoYtDescription;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.Restrictions;

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
import static NMarket.Common.Promo.Promo.EPromoType;

public class BlueSetPromoYtMapperTest {
    private static final long FEED_ID = 1234;
    private static final String OFFER_1 = "offer 1";
    private static final String OFFER_2 = "offer 2";

    private final BlueSetPromoYtMapper mapper = new BlueSetPromoYtMapper();

    @Test
    public void shouldMapProportionalDiscountDescription() {
        PromoBundleDescription description = mapper.mapDescription(new PromoYtDescription(
                0,
                FEED_ID,
                randomString(),
                ReportPromoType.GENERIC_BUNDLE,
                YtSource.FIRST_PARTY_PIPELINE,
                PromoDetails.newBuilder()
                        .setBlueSet(PromoDetails.BlueSet.newBuilder()
                                .addSetsContent(PromoDetails.BlueSet.SetContent.newBuilder()
                                        .addItems(PromoDetails.BlueSet.SetContent.SetItem.newBuilder()
                                                .setOfferId(OFFER_1)
                                                .setDiscount(30.0)
                                                .setCount(1)
                                                .build())
                                        .addItems(PromoDetails.BlueSet.SetContent.SetItem.newBuilder()
                                                .setOfferId(OFFER_2)
                                                .setDiscount(30.0)
                                                .setCount(1)
                                                .build())
                                        .build())
                                .build())
                        .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                .addRestrictedPromoTypes(EPromoType.PromoCode)
                                .build())
                        .build()
        ));

        assertThat(description.getItems(), hasSize(1));
        assertThat(description.getItems(), hasItems(allOf(
                hasProperty("primary", is(true)),
                hasProperty("condition", hasProperty("conditions", hasItem(allOf(
                        hasProperty("ssku", is(OFFER_2)),
                        hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(30))),
                        hasProperty("fixedPrice", nullValue())
                ))))
        ), allOf(
                hasProperty("primary", is(true)),
                hasProperty("condition", hasProperty("conditions", hasItem(allOf(
                        hasProperty("ssku", is(OFFER_1)),
                        hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(30))),
                        hasProperty("fixedPrice", nullValue())
                ))))
        )));
    }
}
