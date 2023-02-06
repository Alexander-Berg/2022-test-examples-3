package ru.yandex.market.loyalty.admin.yt.mapper;

import org.junit.Test;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.mapper.importer.concrete.BlueFlashPromoYtMapper;
import ru.yandex.market.loyalty.admin.yt.model.PromoYtDescription;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import Market.Promo.Promo.PromoDetails;

import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static NMarket.Common.Promo.Promo.EPromoType;

public class BlueFlashPromoYtMapperTest extends MarketLoyaltyAdminMockedDbTest {
    private static final long FEED_ID = 1234;
    private static final String OFFER_1 = "offer 1";
    private static final String URL = "some url";
    private static final String ANAPLAN_ID = "anaplan promo Id";
    private static final String SHOP_PROMO_ID = "some shop promo Id";

    @Test
    public void shouldMapBlueFlashPromoFromYt() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());
        BlueFlashPromoYtMapper mapper = new BlueFlashPromoYtMapper();

        FlashPromoDescription description = (FlashPromoDescription) mapper.mapDescription(new PromoYtDescription(
                0,
                FEED_ID,
                randomString(),
                ReportPromoType.GENERIC_BUNDLE,
                YtSource.FIRST_PARTY_PIPELINE,
                PromoDetails.newBuilder()
                        .setUrl(URL)
                        .setShopPromoId(SHOP_PROMO_ID)
                        .setAnaplanPromoId(ANAPLAN_ID)
                        .setStartDate(current.toEpochSecond())
                        .setEndDate(current.plusDays(1L).toEpochSecond())
                        .setBlueFlash(PromoDetails.BlueFlash.newBuilder()
                                .addItems(PromoDetails.BlueFlash.FlashItem.newBuilder()
                                        .setOffer(PromoDetails.FeedOfferId.newBuilder()
                                                .setFeedId((int) FEED_ID)
                                                .setOfferId(OFFER_1)
                                                .build())
                                        .setPrice(PromoDetails.Money.newBuilder().setValue(10L).build())
                                        .setBudgetLimit(PromoDetails.Money.newBuilder().setValue(1000).build())
                                        .setQuantityLimit(3)
                                        .build())
                        )
                        .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                .addRestrictedPromoTypes(EPromoType.PromoCode)
                                .build())
                        .build()
        ));

        assertThat(description.getAnaplanId(), is(ANAPLAN_ID));
        assertThat(description.getShopPromoId(), is(SHOP_PROMO_ID));
        assertThat(description.getUrl(), is(URL));

    }

    @Test
    public void shouldMapBlueFlashPromoFromYtWithoutItems() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());
        BlueFlashPromoYtMapper mapper = new BlueFlashPromoYtMapper();

        FlashPromoDescription description = (FlashPromoDescription) mapper.mapDescription(new PromoYtDescription(
                0,
                FEED_ID,
                randomString(),
                ReportPromoType.GENERIC_BUNDLE,
                YtSource.FIRST_PARTY_PIPELINE,
                PromoDetails.newBuilder()
                        .setUrl(URL)
                        .setShopPromoId(SHOP_PROMO_ID)
                        .setAnaplanPromoId(ANAPLAN_ID)
                        .setStartDate(current.toEpochSecond())
                        .setEndDate(current.plusDays(1L).toEpochSecond())
                        .setBlueFlash(PromoDetails.BlueFlash.newBuilder()
                                .addItems(PromoDetails.BlueFlash.FlashItem.newBuilder()
                                        .setOffer(PromoDetails.FeedOfferId.newBuilder()
                                                .setFeedId((int) FEED_ID)
                                                .setOfferId(OFFER_1)
                                                .build())
                                        .setPrice(PromoDetails.Money.newBuilder().setValue(10L).build())
                                        .setBudgetLimit(PromoDetails.Money.newBuilder().setValue(1000).build())
                                        .setQuantityLimit(3)
                                        .build())
                        )
                        .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                .addRestrictedPromoTypes(EPromoType.PromoCode)
                                .build())
                        .build()
        ));

        assertThat(description.getAnaplanId(), is(ANAPLAN_ID));
        assertThat(description.getShopPromoId(), is(SHOP_PROMO_ID));
        assertThat(description.getUrl(), is(URL));

    }

}
