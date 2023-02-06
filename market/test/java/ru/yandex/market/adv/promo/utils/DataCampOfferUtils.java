package ru.yandex.market.adv.promo.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import NMarketIndexer.Common.Common;
import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.market.adv.promo.utils.model.BasicAndServiceOffersPair;

import static ru.yandex.market.adv.promo.datacamp.utils.DatacampOffersUtils.powToIdx;

@ParametersAreNonnullByDefault
public final class DataCampOfferUtils {

    private DataCampOfferUtils() { }

    public static DataCampOffer.Offer createBasicOffer(
            String offerId,
            int shopId,
            int businessId
    ) {
        return createBasicOffer(offerId, shopId, businessId, null);
    }

    public static DataCampOffer.Offer createBasicOffer(
            String offerId,
            int shopId,
            int businessId,
            @Nullable Integer categoryId
    ) {
        return createBasicOffer(offerId, shopId, businessId, categoryId, null);
    }

    public static DataCampOffer.Offer createBasicOffer(
            String offerId,
            int shopId,
            int businessId,
            @Nullable Integer categoryId,
            @Nullable Integer vendorId

    ) {
        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId(offerId)
                        .setShopId(shopId)
                        .setBusinessId(businessId)
                );
        DataCampOfferContent.MarketContent.Builder marketContentBuilder =
                DataCampOfferContent.MarketContent.newBuilder();
        if (categoryId != null) {
            marketContentBuilder.setCategoryId(categoryId);
        }
        if (vendorId != null) {
            marketContentBuilder.setVendorId(vendorId);
        }
        offerBuilder.setContent(
                DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(marketContentBuilder.build())
        );
        return offerBuilder.build();
    }

    public static DataCampOffer.Offer createServiceOfferWithPromos(
            DataCampOfferIdentifiers.OfferIdentifiers identifiers,
            @Nullable List<DataCampOfferPromos.Promo> allAnaplanPromos,
            @Nullable List<DataCampOfferPromos.Promo> activeAnaplanPromos,
            @Nullable List<DataCampOfferPromos.Promo> partnerPromos,
            @Nullable List<DataCampOfferPromos.Promo> cashbackPromos
    ) {
        DataCampOfferPromos.OfferPromos.Builder offerPromosBuilder = DataCampOfferPromos.OfferPromos.newBuilder();
        if (CollectionUtils.isNotEmpty(allAnaplanPromos) || CollectionUtils.isNotEmpty(activeAnaplanPromos)) {
            DataCampOfferPromos.MarketPromos.Builder marketPromosBuilder =
                    DataCampOfferPromos.MarketPromos.newBuilder();
            if (CollectionUtils.isNotEmpty(allAnaplanPromos)) {
                marketPromosBuilder.setAllPromos(
                        DataCampOfferPromos.Promos.newBuilder()
                                .addAllPromos(allAnaplanPromos)
                );
            }
            if (CollectionUtils.isNotEmpty(activeAnaplanPromos)) {
                marketPromosBuilder.setActivePromos(
                        DataCampOfferPromos.Promos.newBuilder()
                                .addAllPromos(activeAnaplanPromos)
                );
            }
            offerPromosBuilder.setAnaplanPromos(marketPromosBuilder);
        }
        if (CollectionUtils.isNotEmpty(partnerPromos)) {
            offerPromosBuilder.setPartnerPromos(
                    DataCampOfferPromos.Promos.newBuilder()
                            .addAllPromos(partnerPromos)
            );
        }
        if (CollectionUtils.isNotEmpty(cashbackPromos)) {
            offerPromosBuilder.setPartnerCashbackPromos(
                    DataCampOfferPromos.Promos.newBuilder()
                            .addAllPromos(cashbackPromos)
            );
        }
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(identifiers)
                .setPromos(offerPromosBuilder)
                .build();
    }

    public static DataCampOfferPromos.Promo createPromo(String promoId) {
        return DataCampOfferPromos.Promo.newBuilder()
                .setId(promoId)
                .build();
    }

    public static DataCampOfferPromos.Promo createDirectDiscountPromo(
            String promoId,
            @Nullable Long discountPrice,
            @Nullable Long baseDiscountPrice
    ) {
        DataCampOfferPromos.Promo.DirectDiscount.Builder directDiscount =
                DataCampOfferPromos.Promo.DirectDiscount.newBuilder();
        if (discountPrice != null) {
            directDiscount.setPrice(createPrice(discountPrice));
        }
        if (baseDiscountPrice != null) {
            directDiscount.setBasePrice(createPrice(baseDiscountPrice));
        }
        return DataCampOfferPromos.Promo.newBuilder()
                .setId(promoId)
                .setDirectDiscount(directDiscount)
                .build();
    }

    public static DataCampOfferPromos.Promo createFlashPromo(
            String promoId,
            @Nullable Long discountPrice,
            @Nullable Long baseDiscountPrice
    ) {
        DataCampOfferPromos.Promo.BlueFlash.Builder flash =
                DataCampOfferPromos.Promo.BlueFlash.newBuilder();
        if (discountPrice != null) {
            flash.setPrice(createPrice(discountPrice));
        }
        if (baseDiscountPrice != null) {
            flash.setBasePrice(createPrice(baseDiscountPrice));
        }
        return DataCampOfferPromos.Promo.newBuilder()
                .setId(promoId)
                .setFlash(flash)
                .build();
    }

    private static Common.PriceExpression createPrice(long value) {
        return Common.PriceExpression.newBuilder()
                .setPrice(powToIdx(value))
                .build();
    }

    public static OffersBatch.UnitedOffersBatchResponse createUnitedOffersBatchResponseWithBasicInfo(
            int partnerId,
            Collection<DataCampOffer.Offer> basicOffers
    ) {
        return createUnitedOffersBatchResponse(
                partnerId,
                basicOffers.stream()
                        .map(basicOffer -> new BasicAndServiceOffersPair(basicOffer, null))
                        .collect(Collectors.toList())
        );
    }

    /**
     * @param basicAndServiceOfferPairs пары из базового и сервисного офферов.
     */
    public static OffersBatch.UnitedOffersBatchResponse createUnitedOffersBatchResponse(
            int partnerId,
            Collection<BasicAndServiceOffersPair> basicAndServiceOfferPairs
    ) {
        OffersBatch.UnitedOffersBatchResponse.Builder builder = OffersBatch.UnitedOffersBatchResponse.newBuilder();
        basicAndServiceOfferPairs.forEach(basicAndServiceOfferPair -> {
            builder.addEntries(
                    OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                            .setUnitedOffer(createUnitedOffer(basicAndServiceOfferPair, partnerId))
                            .build()
            );
        });
        return builder.build();
    }

    public static DataCampUnitedOffer.UnitedOffer createUnitedOffer(
            BasicAndServiceOffersPair basicAndServiceOfferPair,
            int partnerId
    ) {
        DataCampUnitedOffer.UnitedOffer.Builder offerBuilder =
                DataCampUnitedOffer.UnitedOffer.newBuilder()
                        .setBasic(basicAndServiceOfferPair.getBasicOffer())
                        .putActual(
                                partnerId,
                                DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicAndServiceOfferPair.getBasicOffer())
                                        .build()
                        );
        if (basicAndServiceOfferPair.getServiceOffer() != null) {
            offerBuilder.putService(partnerId, basicAndServiceOfferPair.getServiceOffer());
        }
        return offerBuilder.build();
    }
}
