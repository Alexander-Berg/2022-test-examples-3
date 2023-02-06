package ru.yandex.direct.grid.processing.util;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.direct.grid.core.entity.model.GdiOfferStats;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOffer;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOfferId;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.model.offer.GdOfferFilter;
import ru.yandex.direct.grid.processing.model.offer.GdOfferOrderBy;
import ru.yandex.direct.grid.processing.model.offer.GdOfferOrderByField;
import ru.yandex.direct.grid.processing.model.offer.GdOffersContainer;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultStatRequirements;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.ytwrapper.dynamic.dsl.YtMappingUtils.scaleMoney;
import static ru.yandex.direct.ytwrapper.dynamic.dsl.YtQueryUtil.DECIMAL_MULT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

public class OfferTestDataUtils {
    public static GdOffersContainer getDefaultGdOffersContainer() {
        return new GdOffersContainer()
                .withFilter(new GdOfferFilter())
                .withOrderBy(List.of(new GdOfferOrderBy()
                        .withField(GdOfferOrderByField.STAT_SHOWS)
                        .withOrder(Order.DESC))
                )
                .withStatRequirements(getDefaultStatRequirements())
                .withLimitOffset(getDefaultLimitOffset());
    }

    public static GdiOffer defaultGdiOffer() {
        return new GdiOffer()
                .withId(new GdiOfferId()
                        .withBusinessId(1L)
                        .withShopId(2L)
                        .withOfferYabsId(3L))
                .withUrl("https://example.com")
                .withName("Test Offer 01")
                .withImageUrl("https://example.com/test.jpg")
                .withPrice(scaleMoney(BigDecimal.valueOf(1024)))
                .withCurrencyIsoCode("RUR");
    }

    public static GdiOfferStats defaultGdiOfferStats() {
        return new GdiOfferStats()
                .withShows(BigDecimal.valueOf(50))
                .withClicks(BigDecimal.valueOf(18))
                .withCtr(scaleMoney(BigDecimal.valueOf(0.25)))
                .withCost(scaleMoney(BigDecimal.valueOf(1000)))
                .withCostWithTax(scaleMoney(BigDecimal.valueOf(1219.51)))
                .withRevenue(scaleMoney(BigDecimal.valueOf(1600)))
                .withCrr(scaleMoney(BigDecimal.valueOf(1.6)))
                .withCarts(BigDecimal.valueOf(13))
                .withPurchases(BigDecimal.valueOf(3))
                .withAvgClickCost(scaleMoney(BigDecimal.valueOf(55.55)))
                .withAvgProductPrice(scaleMoney(BigDecimal.valueOf(667)))
                .withAvgPurchaseRevenue(scaleMoney(BigDecimal.valueOf(200)))
                .withAutobudgetGoals(BigDecimal.valueOf(0))
                .withMeaningfulGoals(BigDecimal.valueOf(2));
    }

    public static GdiOfferStats noEcommerceGdiOfferStats() {
        return defaultGdiOfferStats()
                .withRevenue(scaleMoney(BigDecimal.ZERO))
                .withCrr(scaleMoney(BigDecimal.ZERO))
                .withCarts(BigDecimal.ZERO)
                .withPurchases(BigDecimal.ZERO)
                .withAvgProductPrice(scaleMoney(BigDecimal.ZERO))
                .withAvgPurchaseRevenue(scaleMoney(BigDecimal.ZERO));
    }

    public static UnversionedRowset toOffersRowset(List<GdiOffer> offers) {
        return toOffersRowset(offers, null);
    }

    public static UnversionedRowset toOffersRowset(List<GdiOffer> offers, @Nullable Long orderId) {
        RowsetBuilder rowsetBuilder = rowsetBuilder();
        offers.forEach(offer -> {
            RowBuilder rowBuilder = rowBuilder()
                    .withColValue("OA_BusinessId", offer.getId().getBusinessId())
                    .withColValue("OA_ShopId", offer.getId().getShopId())
                    .withColValue("OA_OfferYabsId", offer.getId().getOfferYabsId())
                    .withColValue("OA_Url", offer.getUrl())
                    .withColValue("OA_Name", offer.getName())
                    .withColValue("OA_PictureUrl", offer.getImageUrl())
                    .withColValue("OA_Price", ifNotNull(toMicros(offer.getPrice()), price -> price * 10))
                    .withColValue("OA_CurrencyName", offer.getCurrencyIsoCode())
                    .withColValue("OS_BusinessId", offer.getId().getBusinessId())
                    .withColValue("OS_ShopId", offer.getId().getShopId())
                    .withColValue("OS_OfferYabsId", offer.getId().getOfferYabsId())
                    .withColValue("OS_OrderID", orderId);
            if (offer.getStats() != null) {
                GdiOfferStats offerStats = offer.getStats();
                rowBuilder
                        .withColValue("shows", offerStats.getShows().longValue())
                        .withColValue("clicks", offerStats.getClicks().longValue())
                        .withColValue("ctr", toMicros(offerStats.getCtr()))
                        .withColValue("cost", toMicros(offerStats.getCost()))
                        .withColValue("costWithTax", toMicros(offerStats.getCostWithTax()))
                        .withColValue("revenue", toMicros(offerStats.getRevenue()))
                        .withColValue("crr", toMicros(offerStats.getCrr()))
                        .withColValue("carts", ifNotNull(offerStats.getCarts(), BigDecimal::longValue))
                        .withColValue("purchases", ifNotNull(offerStats.getPurchases(), BigDecimal::longValue))
                        .withColValue("avgClickCost", toMicros(offerStats.getAvgClickCost()))
                        .withColValue("avgProductPrice", toMicros(offerStats.getAvgProductPrice()))
                        .withColValue("avgPurchaseRevenue", toMicros(offerStats.getAvgPurchaseRevenue()))
                        .withColValue("autobudgetGoals", offerStats.getAutobudgetGoals().longValue())
                        .withColValue("meaningfulGoals", offerStats.getMeaningfulGoals().longValue());
            }
            rowsetBuilder.add(rowBuilder);
        });
        return rowsetBuilder.build();
    }

    @Nullable
    private static Long toMicros(@Nullable BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.multiply(DECIMAL_MULT).longValue();
    }
}
