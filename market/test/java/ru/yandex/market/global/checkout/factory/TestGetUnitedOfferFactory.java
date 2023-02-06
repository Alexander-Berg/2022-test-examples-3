package ru.yandex.market.global.checkout.factory;

import java.util.Map;
import java.util.function.Supplier;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPictures;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import NMarketIndexer.Common.Common;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.test.RandomUtil;

@Deprecated
public class TestGetUnitedOfferFactory {

    private static final long SEED = RandomUtil.seed(TestGetUnitedOfferFactory.class);

    public static final long SHOP_ID = 40;
    public static final long BUSINESS_ID = 41;
    public static final String OFFER_ID = "OFFER_ID";
    public static final long PRICE = 123L;


    protected EnhancedRandom getRandom(Randomizer<SyncGetOffer.GetUnitedOffersResponse> randomizer) {
        return RandomDataGenerator.dataRandom(SEED)
                .apply(SEED, (seed, builder) -> builder.randomize(
                        SyncGetOffer.GetUnitedOffersResponse.class, randomizer
                ))
                .build();
    }

    public SyncGetOffer.GetUnitedOffersResponse buildWithOfferParams(
            Supplier<Map<String, String>> offerParamsSupplier
    ) {
        return getRandom(getUnitedOffersResponseRandomizer(BUSINESS_ID, OFFER_ID, SHOP_ID, PRICE, false,
                offerParamsSupplier)).nextObject(SyncGetOffer.GetUnitedOffersResponse.class);
    }

    public SyncGetOffer.GetUnitedOffersResponse build(
            long price,
            boolean isAdult,
            long businessId,
            long shopId,
            String offerId
    ) {
        return getRandom(getUnitedOffersResponseRandomizer(businessId, offerId, shopId, price, isAdult, Map::of))
                .nextObject(SyncGetOffer.GetUnitedOffersResponse.class);
    }

    public SyncGetOffer.GetUnitedOffersResponse build(long price) {
        return build(price, false, BUSINESS_ID, SHOP_ID, OFFER_ID);
    }

    public SyncGetOffer.GetUnitedOffersResponse build(long price, boolean isAdult) {
        return build(price, isAdult, BUSINESS_ID, SHOP_ID, OFFER_ID);
    }

    public SyncGetOffer.GetUnitedOffersResponse build() {
        return build(PRICE);
    }

    private DataCampOfferContent.ProductYmlParams buildOfferParams(Map<String, String> params) {
        DataCampOfferContent.ProductYmlParams.Builder offerParamsBuilder
                = DataCampOfferContent.ProductYmlParams.newBuilder();
        params.forEach((key, value) -> offerParamsBuilder.addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                .setName(key)
                .setValue(value)
                .setUnit("")
                .build()));
        return offerParamsBuilder.build();
    }

    protected Randomizer<SyncGetOffer.GetUnitedOffersResponse> getUnitedOffersResponseRandomizer(
            long businessId,
            String offerId,
            long shopId,
            long price,
            boolean isAdult,
            Supplier<Map<String, String>> offerParamsSupplier
    ) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(SEED)
                    .build();

            @Override
            public SyncGetOffer.GetUnitedOffersResponse getRandomValue() {
                return SyncGetOffer.GetUnitedOffersResponse.newBuilder()
                        .addOffers(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(DataCampOffer.Offer.newBuilder()
                                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                .setOfferId(offerId)
                                                .setBusinessId((int) businessId)
                                                .setShopId((int) shopId)
                                                .build())
                                        .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                                                .setName(DataCampOfferMeta.StringValue.newBuilder()
                                                                        .setValue(random.nextObject(String.class))
                                                                        .build())
                                                                .setDescription(DataCampOfferMeta.StringValue.newBuilder()
                                                                        .setValue(random.nextObject(String.class))
                                                                        .build())
                                                                .setVendor(DataCampOfferMeta.StringValue.newBuilder()
                                                                        .setValue(random.nextObject(String.class))
                                                                        .build())
                                                                .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                                                        .setId(random.nextLong())
                                                                        .build())
                                                                .setAdult(DataCampOfferMeta.Flag.newBuilder()
                                                                        .setFlag(isAdult)
                                                                        .build())
                                                                .setOfferParams(buildOfferParams(offerParamsSupplier.get())))
                                                        .build())
                                                .build())
                                        .setPictures(DataCampOfferPictures.OfferPictures.newBuilder()
                                                .setPartner(DataCampOfferPictures.PartnerPictures.newBuilder()
                                                        .putActual(
                                                                random.nextObject(String.class),
                                                                DataCampOfferPictures.MarketPicture.newBuilder()
                                                                        .setOriginal(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                                                                                .setUrl(random.nextObject(String.class))
                                                                                .build())
                                                                        .build())
                                                        .build())
                                                .build())
                                        .build())
                                .putService((int) shopId, DataCampOffer.Offer.newBuilder()
                                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                .setBusinessId((int) businessId)
                                                .setShopId((int) shopId)
                                                .setOfferId(offerId)
                                                .buildPartial())
                                        .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                                                .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                                        .setBinaryPrice(Common.PriceExpression.newBuilder()
                                                                .setPrice(price * 100_000)
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                        ).build();
            }
        };
    }
}
