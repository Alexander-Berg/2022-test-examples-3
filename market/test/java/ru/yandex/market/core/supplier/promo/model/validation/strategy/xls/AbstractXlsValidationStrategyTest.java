package ru.yandex.market.core.supplier.promo.model.validation.strategy.xls;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferPromos;
import NMarketIndexer.Common.Common;
import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.datacamp.DataCampUtil;

public class AbstractXlsValidationStrategyTest extends FunctionalTest {
    protected final DataCampOffer.Offer dcOffer = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setOfferId("shop-sku-1")
                    .build())
            .setPromos(
                    DataCampOfferPromos.OfferPromos.newBuilder()
                            .setAnaplanPromos(
                                    createMarketPromos(
                                            Collections.emptyList(),
                                            Collections.singletonList(
                                                    createPromo("promo-id", null, 2000L)
                                            )
                                    )
                            )
            )
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                            .setCategoryId(1)
                            .build())
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                            .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                    .setMarketSkuId(1L)
                            )
                    )
                    .build())
            .build();

    protected final DataCampOffer.Offer dcOfferWithActivePromo = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setOfferId("shop-sku-1")
                    .setWarehouseId(123)
                    .setShopId(321)
                    .build())
            .setPromos(
                    DataCampOfferPromos.OfferPromos.newBuilder()
                            .setAnaplanPromos(
                                    createMarketPromos(
                                            Collections.singletonList(
                                                    createPromo("promo-id-1", null, null)
                                            ),
                                            Arrays.asList(
                                                    createPromo("promo-id", null, 2000L),
                                                    createPromo("promo-id-1", null, null)
                                            )
                                    )
                            )
            )
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                            .setCategoryId(1)
                            .build()
                    )
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                            .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                    .setMarketSkuId(1L)
                            )
                    )
                    .build()
            )
            .build();

    protected Map<String, DataCampOffer.Offer> datacampOffers = new HashMap<>() {{
        put("shop-sku-1", dcOffer);
    }};

    protected DataCampOfferPromos.Promo createPromo(String id, Long price, Long oldPrice) {
        DataCampOfferPromos.Promo.Builder promoBuilder = DataCampOfferPromos.Promo.newBuilder()
                .setId(id);
        if (price != null) {
            promoBuilder.setDirectDiscount(DataCampOfferPromos.Promo.DirectDiscount.newBuilder()
                    .setPrice(Common.PriceExpression.newBuilder()
                            .setPrice(DataCampUtil.powToIdx(BigDecimal.valueOf(price)))));
        }
        if (oldPrice != null) {
            promoBuilder.setDirectDiscount(DataCampOfferPromos.Promo.DirectDiscount.newBuilder()
                    .setBasePrice(Common.PriceExpression.newBuilder()
                            .setPrice(DataCampUtil.powToIdx(BigDecimal.valueOf(oldPrice)))));
        }
        return promoBuilder.build();
    }

    protected final DataCampOffer.Offer flashDcOffer = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setOfferId("shop-sku-1")
            )
            .setPromos(
                    DataCampOfferPromos.OfferPromos.newBuilder()
                            .setAnaplanPromos(
                                    createMarketPromos(
                                            Collections.emptyList(),
                                            Collections.singletonList(
                                                    DataCampOfferPromos.Promo.newBuilder()
                                                            .setId("blue-flash-promo-id")
                                                            .setFlash(DataCampOfferPromos.Promo.BlueFlash.newBuilder()
                                                                    .setBasePrice(Common.PriceExpression.newBuilder()
                                                                            .setPrice(DataCampUtil.powToIdx(BigDecimal.valueOf(2000L))))
                                                                    .build())
                                                            .build()
                                            )
                                    )
                            )
            )
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                            .setCategoryId(1)
                            .build()
                    )
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                            .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                    .setMarketSkuId(1L)
                            )
                    )
                    .build())
            .build();

    protected final DataCampOffer.Offer flashDcOfferWOBasePrice = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setOfferId("shop-sku-1")
            )
            .setPromos(
                    DataCampOfferPromos.OfferPromos.newBuilder()
                            .setAnaplanPromos(
                                    createMarketPromos(
                                            Collections.emptyList(),
                                            Collections.singletonList(
                                                    DataCampOfferPromos.Promo.newBuilder()
                                                            .setId("blue-flash-promo-id")
                                                            .build()
                                            )
                                    )
                            )
            )
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                            .setCategoryId(1)
                            .build()
                    )
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                            .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                    .setMarketSkuId(1L)
                            )
                    )
                    .build())
            .build();

    protected DataCampOfferPromos.MarketPromos createMarketPromos(
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
