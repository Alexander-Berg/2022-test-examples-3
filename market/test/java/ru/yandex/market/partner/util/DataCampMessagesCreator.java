package ru.yandex.market.partner.util;

import java.math.BigDecimal;
import java.util.List;

import Market.DataCamp.DataCampOfferPromos;
import NMarketIndexer.Common.Common;
import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.market.core.datacamp.DataCampUtil;

public class DataCampMessagesCreator {

    public static DataCampOfferPromos.Promo createDiscountPromo(String id, Long price, Long oldPrice) {
        DataCampOfferPromos.Promo.DirectDiscount.Builder directDiscountBuilder =
                DataCampOfferPromos.Promo.DirectDiscount.newBuilder();
        if (price != null) {
            directDiscountBuilder.setPrice(
                    Common.PriceExpression.newBuilder()
                            .setPrice(
                                    DataCampUtil.powToIdx(BigDecimal.valueOf(price))
                            )
                            .build()
            );
        }
        if (oldPrice != null) {
            directDiscountBuilder.setBasePrice(
                    Common.PriceExpression.newBuilder()
                            .setPrice(
                                    DataCampUtil.powToIdx(BigDecimal.valueOf(oldPrice))
                            )
                            .build()
            );
        }
        return DataCampOfferPromos.Promo.newBuilder()
                .setId(id)
                .setDirectDiscount(directDiscountBuilder)
                .build();
    }

    public static DataCampOfferPromos.Promo createBlueFlashPromo(String id, Long price, Long oldPrice) {
        DataCampOfferPromos.Promo.BlueFlash.Builder blueFlashBuilder =
                DataCampOfferPromos.Promo.BlueFlash.newBuilder();
        if (price != null) {
            blueFlashBuilder.setPrice(
                    Common.PriceExpression.newBuilder()
                            .setPrice(
                                    DataCampUtil.powToIdx(BigDecimal.valueOf(price))
                            )
                            .build()
            );
        }
        if (oldPrice != null) {
            blueFlashBuilder.setBasePrice(
                    Common.PriceExpression.newBuilder()
                            .setPrice(
                                    DataCampUtil.powToIdx(BigDecimal.valueOf(oldPrice))
                            )
                            .build()
            );
        }
        return DataCampOfferPromos.Promo.newBuilder()
                .setId(id)
                .setFlash(blueFlashBuilder)
                .build();
    }

    public static DataCampOfferPromos.Promo createSimplePromoWithIdOnly(String id) {
        return DataCampOfferPromos.Promo.newBuilder()
                .setId(id)
                .build();
    }

    public static DataCampOfferPromos.MarketPromos createMarketPromos(
            List<DataCampOfferPromos.Promo> activePromos,
            List<DataCampOfferPromos.Promo> allPromos
    ) {
        DataCampOfferPromos.MarketPromos.Builder anaplanPromosBuilder = DataCampOfferPromos.MarketPromos.newBuilder();
        if (CollectionUtils.isNotEmpty(activePromos)) {
            anaplanPromosBuilder.setActivePromos(
                    DataCampOfferPromos.Promos.newBuilder()
                            .addAllPromos(activePromos)
                            .build()
            );
        }
        if (CollectionUtils.isNotEmpty(allPromos)) {
            anaplanPromosBuilder.setAllPromos(
                    DataCampOfferPromos.Promos.newBuilder()
                            .addAllPromos(allPromos)
                            .build()
            );
        }
        return anaplanPromosBuilder.build();
    }
}
