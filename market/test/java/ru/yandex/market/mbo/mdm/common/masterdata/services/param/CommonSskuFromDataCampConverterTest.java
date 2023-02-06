package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampPartnerInfo;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.protobuf.Timestamp;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.FlatSsku;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static ru.yandex.market.mbo.mdm.common.datacamp.DatacampOffersTestUtil.createBusinessOfferBuilder;
import static ru.yandex.market.mbo.mdm.common.datacamp.DatacampOffersTestUtil.createServiceOfferBuilder;
import static ru.yandex.market.mbo.mdm.common.datacamp.DatacampOffersTestUtil.createUnitedOffer;
import static ru.yandex.market.mbo.mdm.common.datacamp.DatacampOffersTestUtil.offerStatus;

public class CommonSskuFromDataCampConverterTest extends MdmBaseDbTestClass {
    private static final int BUSINESS_ID = 777;
    private static final int SHOP_ID = 888;
    private static final String SHOP_SKU = "newValue";
    @Autowired
    private MdmFromDatacampConverter fromDatacampConverter;

    @Test
    public void canWritePartnerContentToPojoNoService() {

        DataCampOfferMeta.UpdateMeta meta = DataCampOfferMeta.UpdateMeta.newBuilder()
            .setTimestamp(Timestamp.newBuilder().setSeconds(9000).build()).build();

        DataCampOfferContent.OriginalTerms.Builder originalTermsBuilder =
            DataCampOfferContent.OriginalTerms.newBuilder()
                .setBoxCount(DataCampOfferMeta.Ui32Value.newBuilder().setValue(1).build())
                .setQuantityInPack(DataCampOfferMeta.Ui32Value.newBuilder().setValue(2).build())
                .setTransportUnitSize(DataCampOfferMeta.Ui32Value.newBuilder().setValue(1).build())
                .setPartnerDeliveryTime(DataCampOfferMeta.Ui32Value.newBuilder().setValue(15).build())
                .setQuantity(DataCampOfferContent.Quantity.newBuilder().setMin(11).setStep(11).setMeta(meta))
                .setSellerWarranty(DataCampOfferContent.Warranty.newBuilder().setCommentWarranty("commentWarranty")
                    .setMeta(meta)
                    .setWarrantyPeriod(DataCampOfferMeta.Duration.newBuilder().setDays(13)))
                .setSupplyWeekdays(DataCampOfferContent.SupplySchedule.newBuilder()
                    .setMeta(meta));

        DataCampOfferContent.OriginalSpecification.Builder originalSpecBuilder =
            DataCampOfferContent.OriginalSpecification.newBuilder()
                .setAnimalProducts(DataCampOfferMeta.Flag.newBuilder().setFlag(false).build())
                .setExpiry(DataCampOfferContent.Expiration.newBuilder()
                    .setDatetime(Timestamp.newBuilder().setSeconds(900).build())
                    .setValidityComment("abc")
                    .setValidityPeriod(DataCampOfferMeta.Duration.newBuilder().setDays(13).build())
                    .setMeta(meta).build())
                .setLifespan(DataCampOfferContent.Lifespan.newBuilder()
                    .setServiceLifeComment("abc")
                    .setServiceLifePeriod(DataCampOfferMeta.Duration.newBuilder().setDays(13).build()).build())
                .setManufacturer(DataCampOfferMeta.StringValue.newBuilder().setValue("ООО Вектор").build())
                // both grams and value_mg should work
                .setWeight(DataCampOfferContent.PreciseWeight.newBuilder().setValueMg(12000000).build())
                .setWeightNet(DataCampOfferContent.PreciseWeight.newBuilder().setGrams(12000).build())
                .setDimensions(DataCampOfferContent.PreciseDimensions.newBuilder()
                    .setHeightMkm(80000)
                    .setLengthMkm(110000)
                    .setWidthMkm(120000).build());

        DataCampOfferContent.PartnerContent.Builder partnerContent = DataCampOfferContent.PartnerContent.newBuilder()
            .setOriginal(originalSpecBuilder.build())
            .setOriginalTerms(originalTermsBuilder.build());

        DataCampOfferContent.OfferContent.Builder offerContentBuilder = DataCampOfferContent.OfferContent.newBuilder()
            .setPartner(partnerContent);

        DataCampOfferPrice.OfferPrice price = DataCampOfferPrice.OfferPrice.newBuilder()
            .setOriginalPriceFields(DataCampOfferPrice.OriginalPriceFields.newBuilder()
                .setVat(DataCampOfferPrice.VatValue.newBuilder()
                    .setValue(DataCampOfferPrice.Vat.VAT_10)
                    .setMeta(meta)))
            .build();

        DataCampOfferIdentifiers.OfferIdentifiers.Builder identifiers =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setOfferId(SHOP_SKU);

        DataCampOffer.Offer.Builder basicOffer = DataCampOffer.Offer.newBuilder()
            .setContent(offerContentBuilder.build())
            .setPrice(price)
            .setIdentifiers(identifiers);

        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer)
            .build();

        CommonSsku result = fromDatacampConverter.protoOriginalSpecificationToPojo(unitedOffer);

        Assert.assertEquals(0, result.getServiceSskus().size());
        Assert.assertEquals(new ShopSkuKey(BUSINESS_ID, SHOP_SKU), result.getKey());
        Map<Long, SskuParamValue> pvs = result.getBaseValuesByParamId();
        Assert.assertEquals(pvs.size(), 18);
        Assertions.assertThat(pvs.get(KnownMdmParams.WEIGHT_GROSS).getNumeric()).hasValue(decimal(12));
        Assertions.assertThat(pvs.get(KnownMdmParams.WIDTH).getNumeric()).hasValue(decimal(12));
        Assertions.assertThat(pvs.get(KnownMdmParams.HEIGHT).getNumeric()).hasValue(decimal(8));
        Assertions.assertThat(pvs.get(KnownMdmParams.LENGTH).getNumeric()).hasValue(decimal(11));
        Assertions.assertThat(pvs.get(KnownMdmParams.WEIGHT_NET).getNumeric()).hasValue(decimal(12));
        Assertions.assertThat(pvs.get(KnownMdmParams.MANUFACTURER).getString()).hasValue("ООО Вектор");
        Assertions.assertThat(pvs.get(KnownMdmParams.LIFE_TIME).getString()).hasValue("13");
        Assertions.assertThat(pvs.get(KnownMdmParams.LIFE_TIME_UNIT).getOption()).hasValue(opt(3));
        Assertions.assertThat(pvs.get(KnownMdmParams.LIFE_TIME_COMMENT).getString()).hasValue("abc");
        Assertions.assertThat(pvs.get(KnownMdmParams.SHELF_LIFE).getNumeric()).hasValue(decimal(13));
        Assertions.assertThat(pvs.get(KnownMdmParams.SHELF_LIFE_UNIT).getOption()).hasValue(opt(3));
        Assertions.assertThat(pvs.get(KnownMdmParams.SHELF_LIFE_COMMENT).getString()).hasValue("abc");
        Assertions.assertThat(pvs.get(KnownMdmParams.USE_IN_MERCURY).getBool()).hasValue(false);
        Assertions.assertThat(pvs.get(KnownMdmParams.GUARANTEE_PERIOD).getString()).hasValue("13");
        Assertions.assertThat(pvs.get(KnownMdmParams.GUARANTEE_PERIOD_UNIT).getOption()).hasValue(opt(3));
        Assertions.assertThat(pvs.get(KnownMdmParams.GUARANTEE_PERIOD_COMMENT).getString()).hasValue("commentWarranty");
        Assertions.assertThat(pvs.get(KnownMdmParams.BOX_COUNT).getNumeric()).hasValue(decimal(1));
        Assertions.assertThat(pvs.get(KnownMdmParams.VAT).getOption()).hasValue(KnownMdmParams.VAT_10_OPTION);
        Assert.assertNotNull(result.getBaseSsku());
    }

    @Test
    public void canCorrectConvertWeightLessThan500Gram() {
        DataCampOfferIdentifiers.OfferIdentifiers.Builder identifiers =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setOfferId(SHOP_SKU);

        DataCampOfferContent.OriginalSpecification.Builder originalSpecBuilder =
            DataCampOfferContent.OriginalSpecification.newBuilder()
                .setWeight(DataCampOfferContent.PreciseWeight.newBuilder().setValueMg(300_000).build())
                .setDimensions(DataCampOfferContent.PreciseDimensions.newBuilder()
                    .setHeightMkm(100_000)
                    .setLengthMkm(100_000)
                    .setWidthMkm(100_000).build());

        DataCampOfferContent.PartnerContent.Builder partnerContent = DataCampOfferContent.PartnerContent.newBuilder()
            .setOriginal(originalSpecBuilder.build());

        DataCampOfferContent.OfferContent.Builder offerContentBuilder = DataCampOfferContent.OfferContent.newBuilder()
            .setPartner(partnerContent);

        DataCampOffer.Offer.Builder offer = DataCampOffer.Offer.newBuilder()
            .setContent(offerContentBuilder.build())
            .setIdentifiers(identifiers);

        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(offer)
            .build();

        CommonSsku result = fromDatacampConverter.protoOriginalSpecificationToPojo(unitedOffer);
        Map<Long, SskuParamValue> pvs = result.getBaseValuesByParamId();
        Assertions.assertThat(pvs.get(KnownMdmParams.WEIGHT_GROSS).getNumeric()).hasValue(BigDecimal.valueOf(0.3));
    }

    @Test
    public void canCorrectConvertLengthLessThan5Millimeter() {
        DataCampOfferIdentifiers.OfferIdentifiers.Builder identifiers =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setOfferId(SHOP_SKU);

        DataCampOfferContent.OriginalSpecification.Builder originalSpecBuilder =
            DataCampOfferContent.OriginalSpecification.newBuilder()
                .setWeight(DataCampOfferContent.PreciseWeight.newBuilder().setValueMg(1_000_000).build())
                .setDimensions(DataCampOfferContent.PreciseDimensions.newBuilder()
                    .setHeightMkm(3_000)
                    .setLengthMkm(2_000)
                    .setWidthMkm(5_000).build());

        DataCampOfferContent.PartnerContent.Builder partnerContent = DataCampOfferContent.PartnerContent.newBuilder()
            .setOriginal(originalSpecBuilder.build());

        DataCampOfferContent.OfferContent.Builder offerContentBuilder = DataCampOfferContent.OfferContent.newBuilder()
            .setPartner(partnerContent);

        DataCampOffer.Offer.Builder offer = DataCampOffer.Offer.newBuilder()
            .setContent(offerContentBuilder.build())
            .setIdentifiers(identifiers);

        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(offer)
            .build();

        CommonSsku result = fromDatacampConverter.protoOriginalSpecificationToPojo(unitedOffer);
        Map<Long, SskuParamValue> pvs = result.getBaseValuesByParamId();
        Assertions.assertThat(pvs.get(KnownMdmParams.HEIGHT).getNumeric()).hasValue(BigDecimal.valueOf(0.3));
        Assertions.assertThat(pvs.get(KnownMdmParams.LENGTH).getNumeric()).hasValue(BigDecimal.valueOf(0.2));
        Assertions.assertThat(pvs.get(KnownMdmParams.WIDTH).getNumeric()).hasValue(BigDecimal.valueOf(0.5));
    }

    @Test
    public void testDsbsFlagConvertedOnServiceAndIfTrueOnly() {
        var partnerBaseInfo = DataCampPartnerInfo.PartnerInfo.newBuilder().setIsDsbs(true); // проигнорируется
        var partnerServiceTrueInfo = DataCampPartnerInfo.PartnerInfo.newBuilder().setIsDsbs(true); // возьмётся
        var partnerServiceFalseInfo = DataCampPartnerInfo.PartnerInfo.newBuilder().setIsDsbs(false); // проигнорируется

        var baseIdentifiers = DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
            .setBusinessId(BUSINESS_ID)
            .setOfferId(SHOP_SKU);

        var service1Identifiers =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setShopId(SHOP_ID)
                .setOfferId(SHOP_SKU);

        var service2Identifiers =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setShopId(SHOP_ID + 1)
                .setOfferId(SHOP_SKU);

        var baseOffer = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(baseIdentifiers)
            .setPartnerInfo(partnerBaseInfo);
        var serviceOffer1 = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(service1Identifiers)
            .setPartnerInfo(partnerServiceTrueInfo);
        var serviceOffer2 = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(service2Identifiers)
            .setPartnerInfo(partnerServiceFalseInfo);

        var unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(baseOffer)
            .putService(SHOP_ID, serviceOffer1.build())
            .putService(SHOP_ID + 1, serviceOffer2.build())
            .build();

        CommonSsku ssku = fromDatacampConverter.protoOriginalSpecificationToPojo(unitedOffer);

        Assertions.assertThat(ssku.getBaseValue(KnownMdmParams.IS_DBS)).isEmpty();

        Assertions.assertThat(ssku.getServiceSsku(SHOP_ID)).isPresent();
        Assertions.assertThat(ssku.getServiceSsku(SHOP_ID + 1)).isPresent();

        Assertions.assertThat(ssku.getServiceSsku(SHOP_ID).get().getParamValue(KnownMdmParams.IS_DBS)).isPresent();
        Assertions.assertThat(ssku.getServiceSsku(SHOP_ID).get().getParamValue(KnownMdmParams.IS_DBS).get().getBool())
            .isPresent().hasValue(true);
        Assertions.assertThat(ssku.getServiceSsku(SHOP_ID + 1).get().getParamValue(KnownMdmParams.IS_DBS)).isEmpty();
    }

    @Test
    public void canWriteBasicParamsToBasicPartAndServiceToServices() {
        DataCampOfferMeta.UpdateMeta meta = DataCampOfferMeta.UpdateMeta.newBuilder()
            .setTimestamp(Timestamp.newBuilder().setSeconds(9000).build()).build();

        DataCampOfferContent.OriginalTerms.Builder originalTermsBuilder =
            DataCampOfferContent.OriginalTerms.newBuilder()
                .setBoxCount(DataCampOfferMeta.Ui32Value.newBuilder().setValue(1).build())
                .setQuantityInPack(DataCampOfferMeta.Ui32Value.newBuilder().setValue(2).build())
                .setTransportUnitSize(DataCampOfferMeta.Ui32Value.newBuilder().setValue(1).build())
                .setPartnerDeliveryTime(DataCampOfferMeta.Ui32Value.newBuilder().setValue(15).build())
                .setQuantity(DataCampOfferContent.Quantity.newBuilder().setMin(11).setStep(11).setMeta(meta))
                .setSellerWarranty(DataCampOfferContent.Warranty.newBuilder().setCommentWarranty("commentWarranty")
                    .setMeta(meta)
                    .setWarrantyPeriod(DataCampOfferMeta.Duration.newBuilder().setDays(13)))
                .setSupplyWeekdays(DataCampOfferContent.SupplySchedule.newBuilder()
                    .setMeta(meta));

        DataCampOfferContent.OriginalSpecification.Builder originalSpecBuilder =
            DataCampOfferContent.OriginalSpecification.newBuilder()
                .setAnimalProducts(DataCampOfferMeta.Flag.newBuilder().setFlag(false).build())
                .setExpiry(DataCampOfferContent.Expiration.newBuilder()
                    .setDatetime(Timestamp.newBuilder().setSeconds(900).build())
                    .setValidityComment("abc")
                    .setValidityPeriod(DataCampOfferMeta.Duration.newBuilder().setDays(13).build())
                    .setMeta(meta).build())
                .setLifespan(DataCampOfferContent.Lifespan.newBuilder()
                    .setServiceLifeComment("abc")
                    .setServiceLifePeriod(DataCampOfferMeta.Duration.newBuilder().setDays(13).build()).build())
                .setManufacturer(DataCampOfferMeta.StringValue.newBuilder().setValue("ООО Вектор").build())
                // both grams and value_mg should work
                .setWeight(DataCampOfferContent.PreciseWeight.newBuilder().setValueMg(12000000).build())
                .setWeightNet(DataCampOfferContent.PreciseWeight.newBuilder().setGrams(12000).build())
                .setDimensions(DataCampOfferContent.PreciseDimensions.newBuilder()
                    .setHeightMkm(80000)
                    .setLengthMkm(110000)
                    .setWidthMkm(120000).build());

        DataCampOfferContent.PartnerContent.Builder partnerContent = DataCampOfferContent.PartnerContent.newBuilder()
            .setOriginal(originalSpecBuilder.build())
            .setOriginalTerms(originalTermsBuilder.build());

        DataCampOfferContent.OfferContent.Builder offerContentBuilder = DataCampOfferContent.OfferContent.newBuilder()
            .setPartner(partnerContent);

        DataCampOfferPrice.OfferPrice price = DataCampOfferPrice.OfferPrice.newBuilder()
            .setOriginalPriceFields(DataCampOfferPrice.OriginalPriceFields.newBuilder()
                .setVat(DataCampOfferPrice.VatValue.newBuilder()
                    .setValue(DataCampOfferPrice.Vat.VAT_10)
                    .setMeta(meta)))
            .build();

        DataCampOfferIdentifiers.OfferIdentifiers.Builder basicIdentifiers =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setOfferId(SHOP_SKU);

        DataCampOffer.Offer.Builder basicOffer = DataCampOffer.Offer.newBuilder()
            .setContent(offerContentBuilder.build())
            .setPrice(price)
            .setIdentifiers(basicIdentifiers);

        DataCampOfferIdentifiers.OfferIdentifiers.Builder serviceIdentifiers =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setShopId(SHOP_ID)
                .setOfferId(SHOP_SKU);

        DataCampOffer.Offer.Builder serviceOffer = DataCampOffer.Offer.newBuilder()
            .setContent(offerContentBuilder.build())
            .setIdentifiers(serviceIdentifiers);

        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer)
            .putService(SHOP_ID, serviceOffer.build())
            .build();

        CommonSsku result = fromDatacampConverter.protoOriginalSpecificationToPojo(unitedOffer);

        Assert.assertEquals(1, result.getServiceSskus().size());
        Assert.assertTrue(result.getServiceSskus().containsKey(SHOP_ID));
        Assert.assertEquals(new ShopSkuKey(BUSINESS_ID, SHOP_SKU), result.getKey());
        Map<Long, SskuParamValue> pvs = result.getBaseValuesByParamId();
        Assert.assertEquals(pvs.size(), 18);
        Assertions.assertThat(pvs.get(KnownMdmParams.WEIGHT_GROSS).getNumeric()).hasValue(decimal(12));
        Assertions.assertThat(pvs.get(KnownMdmParams.WIDTH).getNumeric()).hasValue(decimal(12));
        Assertions.assertThat(pvs.get(KnownMdmParams.HEIGHT).getNumeric()).hasValue(decimal(8));
        Assertions.assertThat(pvs.get(KnownMdmParams.LENGTH).getNumeric()).hasValue(decimal(11));
        Assertions.assertThat(pvs.get(KnownMdmParams.WEIGHT_NET).getNumeric()).hasValue(decimal(12));
        Assertions.assertThat(pvs.get(KnownMdmParams.MANUFACTURER).getString()).hasValue("ООО Вектор");
        Assertions.assertThat(pvs.get(KnownMdmParams.LIFE_TIME).getString()).hasValue("13");
        Assertions.assertThat(pvs.get(KnownMdmParams.LIFE_TIME_UNIT).getOption()).hasValue(opt(3));
        Assertions.assertThat(pvs.get(KnownMdmParams.LIFE_TIME_COMMENT).getString()).hasValue("abc");
        Assertions.assertThat(pvs.get(KnownMdmParams.SHELF_LIFE).getNumeric()).hasValue(decimal(13));
        Assertions.assertThat(pvs.get(KnownMdmParams.SHELF_LIFE_UNIT).getOption()).hasValue(opt(3));
        Assertions.assertThat(pvs.get(KnownMdmParams.SHELF_LIFE_COMMENT).getString()).hasValue("abc");
        Assertions.assertThat(pvs.get(KnownMdmParams.USE_IN_MERCURY).getBool()).hasValue(false);
        Assertions.assertThat(pvs.get(KnownMdmParams.GUARANTEE_PERIOD).getString()).hasValue("13");
        Assertions.assertThat(pvs.get(KnownMdmParams.GUARANTEE_PERIOD_UNIT).getOption()).hasValue(opt(3));
        Assertions.assertThat(pvs.get(KnownMdmParams.GUARANTEE_PERIOD_COMMENT).getString()).hasValue("commentWarranty");
        Assertions.assertThat(pvs.get(KnownMdmParams.BOX_COUNT).getNumeric()).hasValue(decimal(1));
        Assertions.assertThat(pvs.get(KnownMdmParams.VAT).getOption()).hasValue(KnownMdmParams.VAT_10_OPTION);
        Assert.assertNotNull(result.getBaseSsku());

        pvs = result.getServiceValues(SHOP_ID).stream()
            .collect(Collectors.toMap(SskuParamValue::getMdmParamId, Function.identity()));
        Assert.assertEquals(pvs.size(), 4);
        Assertions.assertThat(pvs.get(KnownMdmParams.DELIVERY_TIME).getNumeric()).hasValue(decimal(15));
        Assertions.assertThat(pvs.get(KnownMdmParams.TRANSPORT_UNIT_SIZE).getNumeric()).hasValue(decimal(1));
        Assertions.assertThat(pvs.get(KnownMdmParams.QUANTITY_IN_PACK).getNumeric()).hasValue(decimal(2));
        Assertions.assertThat(pvs.get(KnownMdmParams.SERVICE_EXISTS).getBool()).hasValue(true);
    }

    @Test
    public void canWriteRemoveFlagFromBusinessAndServiceParts() {
        // given
        var removedStatus = offerStatus(1000L)
            .setRemoved(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build());

        var businessOffer = createBusinessOfferBuilder(BUSINESS_ID, SHOP_SKU)
            .setStatus(removedStatus);
        var serviceOffer = createServiceOfferBuilder(SHOP_ID, SHOP_ID)
            .setStatus(removedStatus);
        var unitedOffer = createUnitedOffer(businessOffer, serviceOffer);

        // when
        CommonSsku result = fromDatacampConverter.protoOriginalSpecificationToPojo(unitedOffer);

        // then
        Map<Long, SskuParamValue> basePvs = result.getBaseValuesByParamId();
        Assertions.assertThat(basePvs.get(KnownMdmParams.IS_REMOVED).getBool()).hasValue(true);
        Map<Long, SskuParamValue> servicePvs = result.getServiceSsku(SHOP_ID)
            .map(FlatSsku::getValuesByParamId)
            .orElse(Map.of());
        Assertions.assertThat(servicePvs.get(KnownMdmParams.IS_REMOVED).getBool()).hasValue(true);
    }

    private static BigDecimal decimal(int number) {
        return BigDecimal.valueOf(number);
    }

    private static MdmParamOption opt(int id) {
        return new MdmParamOption(id);
    }
}
