package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferBids;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferDelivery;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampValidationResult;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictFeature;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictTestUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class MdmToDatacampConverterTest extends MdmBaseDbTestClass {
    private static final int BUSINESS_ID = 777;
    private static final int SERVICE_ID1 = 111;
    private static final int SERVICE_ID2 = 222;
    private static final String SHOP_SKU = "xxx";
    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);
    private static final Logger log = LoggerFactory.getLogger(MdmToDatacampConverterTest.class);

    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MdmToDatacampConverter toDatacampConverter;
    @Autowired
    private MdmFromDatacampConverter fromDatacampConverter;
    @Autowired
    private BeruId beruId;

    @Test
    public void whenOnlyBusinessValuesShouldBuildOnlyBaseOffer() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .build();
        CommonSsku converted = fromDatacampConverter.protoMarketMasterDataToPojo(
            toDatacampConverter.pojoToProtoMarketMasterData(ssku));
        Assertions.assertThat(converted).isEqualTo(ssku);
    }

    @Test
    public void whenSmallVghShouldConvertBackAndForthWithCorrectRoundingAndScaling() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, new BigDecimal("0.01"))
            .with(KnownMdmParams.WIDTH, new BigDecimal("0.015"))
            .with(KnownMdmParams.HEIGHT, new BigDecimal("1"))
            .with(KnownMdmParams.LENGTH, new BigDecimal("0.1"))
            .build();
        CommonSsku converted = fromDatacampConverter.protoMarketMasterDataToPojo(
            toDatacampConverter.pojoToProtoMarketMasterData(ssku));
        Assertions.assertThat(converted.getBaseValue(KnownMdmParams.WEIGHT_GROSS)
            .get().getNumeric().get().toPlainString()).isEqualTo("0.01");
        Assertions.assertThat(converted.getBaseValue(KnownMdmParams.WIDTH)
            .get().getNumeric().get().toPlainString()).isEqualTo("0.015");
        Assertions.assertThat(converted.getBaseValue(KnownMdmParams.HEIGHT)
            .get().getNumeric().get().toPlainString()).isEqualTo("1");
        Assertions.assertThat(converted.getBaseValue(KnownMdmParams.LENGTH)
            .get().getNumeric().get().toPlainString()).isEqualTo("0.1");
        Assertions.assertThat(converted).isEqualTo(ssku);
    }

    @Test
    public void whenServiceValuesArePresentShouldAddServiceOffers() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .startServiceValues(SERVICE_ID1)
                .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 3L)
                .with(KnownMdmParams.DELIVERY_TIME, 5L)
            .endServiceValues()
            .build();
        CommonSsku converted = fromDatacampConverter.protoMarketMasterDataToPojo(
            toDatacampConverter.pojoToProtoMarketMasterData(ssku));
        Assertions.assertThat(converted).isEqualTo(ssku);
    }

    @Test
    public void whenRslAndMeasurementArePresentShouldConvertProperly() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.HAS_MEASUREMENT_AFTER_INHERIT, true)
            .with(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_AFTER_INHERIT, 10009000L)
            .startServiceValues(SERVICE_ID1)
                .with(KnownMdmParams.RSL_IN_DAYS, 10L)
                .with(KnownMdmParams.RSL_OUT_DAYS, 5L)
                .with(KnownMdmParams.RSL_IN_PERCENTS, 50L)
                .with(KnownMdmParams.RSL_OUT_PERCENTS, 20L)
            .endServiceValues()
            .build();
        var proto = toDatacampConverter.pojoToProtoMarketMasterData(ssku);
        Assertions.assertThat(proto.getBasic().getContent().getMasterData().hasMeasurementExistence()).isTrue();
        var measurementExistence = proto.getBasic().getContent().getMasterData().getMeasurementExistence();
        Assertions.assertThat(measurementExistence.getHasMeasurement()).isTrue();
        Assertions.assertThat(measurementExistence.getLastMeasurementTs().getSeconds()).isEqualTo(10009L);
        Assertions.assertThat(measurementExistence.getLastMeasurementTs().getNanos()).isEqualTo(0L);

        Assertions.assertThat(
            proto.getServiceOrThrow(SERVICE_ID1).getContent().getMasterData().hasRemainingShelfLife()).isTrue();
        var rsl = proto.getServiceOrThrow(SERVICE_ID1).getContent().getMasterData().getRemainingShelfLife();

        Assertions.assertThat(rsl.getInDays()).isEqualTo(10);
        Assertions.assertThat(rsl.getOutDays()).isEqualTo(5);
        Assertions.assertThat(rsl.getInPercents()).isEqualTo(50);
        Assertions.assertThat(rsl.getOutPercents()).isEqualTo(20);
    }

    @Test
    public void whenRslAndMeasurementAreIncompleteShouldIgnore() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_AFTER_INHERIT, 10009000L)
            .startServiceValues(SERVICE_ID1)
                .with(KnownMdmParams.RSL_IN_DAYS, 10L)
                .with(KnownMdmParams.RSL_OUT_PERCENTS, 20L)
            .endServiceValues()
            .build();
        var proto = toDatacampConverter.pojoToProtoMarketMasterData(ssku);
        Assertions.assertThat(proto.getBasic().getContent().getMasterData().hasMeasurementExistence()).isFalse();
        Assertions.assertThat(
            proto.getServiceOrThrow(SERVICE_ID1).getContent().getMasterData().hasRemainingShelfLife()).isFalse();
    }

    @Test
    public void whenRslAndMeasurementArePartiallyCompleteShouldConvertProperly() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.HAS_MEASUREMENT_AFTER_INHERIT, false)
            .startServiceValues(SERVICE_ID1)
                .with(KnownMdmParams.RSL_IN_DAYS, 10L)
                .with(KnownMdmParams.RSL_OUT_DAYS, 5L)
            .endServiceValues()
            .build();
        var proto = toDatacampConverter.pojoToProtoMarketMasterData(ssku);
        Assertions.assertThat(proto.getBasic().getContent().getMasterData().hasMeasurementExistence()).isTrue();
        var measurementExistence = proto.getBasic().getContent().getMasterData().getMeasurementExistence();
        Assertions.assertThat(measurementExistence.getHasMeasurement()).isFalse();
        Assertions.assertThat(measurementExistence.hasLastMeasurementTs()).isFalse();

        Assertions.assertThat(
            proto.getServiceOrThrow(SERVICE_ID1).getContent().getMasterData().hasRemainingShelfLife()).isTrue();
        var rsl = proto.getServiceOrThrow(SERVICE_ID1).getContent().getMasterData().getRemainingShelfLife();

        Assertions.assertThat(rsl.getInDays()).isEqualTo(10);
        Assertions.assertThat(rsl.getOutDays()).isEqualTo(5);
        Assertions.assertThat(rsl.hasInPercents()).isFalse();
        Assertions.assertThat(rsl.hasOutPercents()).isFalse();
    }

    @Test
    public void whenTotallyFullBusinessProtoWithAbsolutelyEverythingShouldConvertItNicelyToBaseAndServiceCommonSsku() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .with(KnownMdmParams.WEIGHT_TARE, 6L)
            .with(KnownMdmParams.WEIGHT_NET, 66L)
            .withShelfLife(3, TimeInUnits.TimeUnit.DAY, "sl comment")
            .withLifeTime(4, TimeInUnits.TimeUnit.HOUR, "lt comment")
            .withGuaranteePeriod(5, TimeInUnits.TimeUnit.DAY, "gp comment")
            .with(KnownMdmParams.MANUFACTURER, "manufacturer")
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Нидерланды")
            .with(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID, "0064")
            .with(KnownMdmParams.EXPIR_DATE, true)
            .with(KnownMdmParams.BOX_COUNT, 100L)
            .with(KnownMdmParams.DOCUMENT_REG_NUMBER, "EC10.RU 00040728-00", "РОСС.6042 Р-21-615112", "WC3ROC WC3TFT")
            .with(KnownMdmParams.GTIN, "1234567", "803957387")
            .with(KnownMdmParams.VETIS_GUID, "gfdgdfgdf")
            .with(KnownMdmParams.USE_IN_MERCURY, false)
            .with(KnownMdmParams.IS_TRACEABLE, true)
            .startServiceValues(SERVICE_ID1)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 4L)
            .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 8L)
            .with(KnownMdmParams.DELIVERY_TIME, 10L)
            .with(KnownMdmParams.MIN_SHIPMENT, 33L)
            .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption(3), new MdmParamOption(6))
            .startServiceValues(SERVICE_ID2)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 1L)
            .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 5L)
            .with(KnownMdmParams.DELIVERY_TIME, 44L)
            .with(KnownMdmParams.MIN_SHIPMENT, 90L)
            .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption(1))
            .endServiceValues()
            .build();
        CommonSsku converted = fromDatacampConverter.protoMarketMasterDataToPojo(
            toDatacampConverter.pojoToProtoMarketMasterData(ssku));
        Assertions.assertThat(converted).isEqualTo(ssku);
    }

    @Test
    public void whenParameterIsAbsentShouldFillEmptyFieldWithMeta() {
        Instant metaTs = Instant.now().plusSeconds(100500);
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .with(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L)
            // нет веса тары
            .with(KnownMdmParams.WEIGHT_NET, 66L)
            .withShelfLife(3, TimeInUnits.TimeUnit.DAY, "sl comment")
            .withLifeTime(4, TimeInUnits.TimeUnit.HOUR, "lt comment")
            .withGuaranteePeriod(5, TimeInUnits.TimeUnit.WEEK, "gp comment")
            .with(KnownMdmParams.MANUFACTURER, "manufacturer")
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Нидерланды")
            .with(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID, "0064")
            .with(KnownMdmParams.EXPIR_DATE, true)
            // нет box_count
            .with(KnownMdmParams.DOCUMENT_REG_NUMBER, "EC10.RU 00040728-00", "РОСС.6042 Р-21-615112", "WC3ROC WC3TFT")
            .with(KnownMdmParams.GTIN, "1234567", "803957387")
            .with(KnownMdmParams.VETIS_GUID, "gfdgdfgdf")
            .with(KnownMdmParams.IS_TRACEABLE, true)
            .with(KnownMdmParams.USE_IN_MERCURY, false).customized(p -> p.setUpdatedTs(metaTs)) // зафиксируем TS
            .startServiceValues(SERVICE_ID1)
                .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 4L)
                .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 8L)
                .with(KnownMdmParams.DELIVERY_TIME, 10L)
                .with(KnownMdmParams.MIN_SHIPMENT, 33L)
                .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption(3), new MdmParamOption(6))
                .with(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 2L)
            .startServiceValues(SERVICE_ID2)
                .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 1L)
                .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 5L)
                .with(KnownMdmParams.DELIVERY_TIME, 44L)
                .with(KnownMdmParams.MIN_SHIPMENT, 90L)
                .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption(1))
                .with(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 3L)
            .endServiceValues()
            .build();
        DataCampUnitedOffer.UnitedOffer protoOffer = toDatacampConverter.pojoToProtoMarketMasterData(ssku);
        DataCampOfferContent.MarketMasterData protoMD = protoOffer.getBasic().getContent().getMasterData();
        var weightTare = protoMD.getWeightTare();
        var boxCount = protoMD.getBoxCount();
        var datacampMdVersion = protoMD.getVersion();

        Assertions.assertThat(weightTare.hasValueMg()).isFalse();
        Assertions.assertThat(weightTare.hasGrams()).isFalse();
        Assertions.assertThat(weightTare.getMeta().getTimestamp().getSeconds()).isEqualTo(metaTs.getEpochSecond());

        Assertions.assertThat(boxCount.hasValue()).isFalse();
        Assertions.assertThat(boxCount.getMeta().getTimestamp().getSeconds()).isEqualTo(metaTs.getEpochSecond());

        // должна сетиться максимальная версия мастер данных (контракт только для базовой части)
        Assertions.assertThat(datacampMdVersion.getValue().getCounter()).isEqualTo(3L);
    }

    @Test
    public void canWriteVerdictsForPersonalizedLogicToProto() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .startServiceValues(SERVICE_ID1)
            .with(KnownMdmParams.WIDTH, 3L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .endServiceValues()
            .build();

        List<ErrorInfo> errors = List.of(
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.SHELF_LIFE));
        var sskuVerdictResult = VerdictTestUtil.createForbiddingSskuVerdictResult(
            BUSINESS_KEY, Instant.EPOCH.plus(1, ChronoUnit.DAYS), VerdictFeature.UNSPECIFIED, errors);
        ssku.setBaseVerdict(sskuVerdictResult);
        ssku.getServiceSsku(SERVICE_ID1).get().setVerdict(sskuVerdictResult);

        DataCampUnitedOffer.UnitedOffer unitedOffer = toDatacampConverter.pojoToProtoMarketMasterData(ssku);
        Assertions.assertThat(unitedOffer.getBasic().getResolution().getBySourceList()).containsExactly(
            DataCampResolution.Verdicts.newBuilder()
                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                    .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(86400))
                    .setSource(DataCampOfferMeta.DataSource.MARKET_MDM))
                .addVerdict(DataCampResolution.Verdict.newBuilder()
                    .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                        .setIsBanned(false)
                        .addApplications(DataCampValidationResult.Feature.FULFILLMENT)
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setNamespace("mboc.ci.error")
                            .setCode("mboc.error.excel-value-is-required")
                            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                                .setName("header")
                                .setValue("Срок годности"))
                            .setText("Отсутствует значение для колонки 'Срок годности'")
                            .setLevel(DataCampExplanation.Explanation.Level.WARNING)
                            .setDetails("{\"header\":\"Срок годности\"}"))))
                .addVerdict(DataCampResolution.Verdict.newBuilder()
                    .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                        .setIsBanned(false)
                        .addApplications(DataCampValidationResult.Feature.CPA)
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setNamespace("mboc.ci.error")
                            .setCode("mboc.error.excel-value-is-required")
                            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                                .setName("header")
                                .setValue("Срок годности"))
                            .setText("Отсутствует значение для колонки 'Срок годности'")
                            .setLevel(DataCampExplanation.Explanation.Level.WARNING)
                            .setDetails("{\"header\":\"Срок годности\"}"))))
                .build());
    }

    @Test
    public void canWriteVerdictsForPersonalizedLogicForBeruIdToProto() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, new ShopSkuKey(beruId.getId(), "xxx"))
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .build();

        List<ErrorInfo> errors = List.of(
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.SHELF_LIFE));
        var sskuVerdictResult = VerdictTestUtil.createForbiddingSskuVerdictResult(
            BUSINESS_KEY, Instant.EPOCH.plus(1, ChronoUnit.DAYS), VerdictFeature.UNSPECIFIED, errors);
        ssku.setBaseVerdict(sskuVerdictResult);

        DataCampUnitedOffer.UnitedOffer unitedOffer = toDatacampConverter.pojoToProtoMarketMasterData(ssku);
        Assertions.assertThat(unitedOffer.getBasic().getResolution().getBySourceList()).hasSize(1);
        Assertions.assertThat(unitedOffer.getBasic().getResolution().getBySourceList().get(0)).isEqualTo(
            DataCampResolution.Verdicts.newBuilder()
                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                    .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(86400))
                    .setSource(DataCampOfferMeta.DataSource.MARKET_MDM))
                .addVerdict(DataCampResolution.Verdict.newBuilder()
                    .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                        .setIsBanned(false)
                        .addApplications(DataCampValidationResult.Feature.FULFILLMENT)
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setNamespace("mboc.ci.error")
                            .setCode("mboc.error.excel-value-is-required")
                            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                                .setName("header")
                                .setValue("Срок годности"))
                            .setText("Отсутствует значение для колонки 'Срок годности'")
                            .setLevel(DataCampExplanation.Explanation.Level.WARNING)
                            .setDetails("{\"header\":\"Срок годности\"}"))))
                .addVerdict(DataCampResolution.Verdict.newBuilder()
                    .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                        .setIsBanned(false)
                        .addApplications(DataCampValidationResult.Feature.CPA)
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setNamespace("mboc.ci.error")
                            .setCode("mboc.error.excel-value-is-required")
                            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                                .setName("header")
                                .setValue("Срок годности"))
                            .setText("Отсутствует значение для колонки 'Срок годности'")
                            .setLevel(DataCampExplanation.Explanation.Level.WARNING)
                            .setDetails("{\"header\":\"Срок годности\"}"))))
                .build());
    }

    @Test
    public void canWriteOnlyMergedBusinessRegNumbersToProto() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.DOCUMENT_REG_NUMBER, "regNumberBusiness1", "regNumberBusinsess2")
            .startServiceValues(SERVICE_ID1)
            .with(KnownMdmParams.DOCUMENT_REG_NUMBER, "regNumberService")
            .endServiceValues()
            .build();
        DataCampUnitedOffer.UnitedOffer offer =
            toDatacampConverter.pojoToProtoMarketMasterData(ssku);

        Assertions.assertThat(offer.getBasic().getContent().getMasterData().getCertificates().getValueCount())
            .isEqualTo(2);
        Assertions
            .assertThat(offer.getBasic().getContent().getMasterData().getCertificates().getValueList()
                .asByteStringList().stream()
                .map(ByteString::toStringUtf8)
                .collect(Collectors.toList()))
            .containsExactlyInAnyOrder("regNumberBusiness1", "regNumberBusinsess2");
        Assertions.assertThat(
                offer.getServiceMap().get(SERVICE_ID1).getContent().getMasterData().getCertificates().getValueCount())
            .isEqualTo(0);
    }

    @Test
    public void canCreateNewObjectWhenIncomingOfferIsNull() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .startServiceValues(SERVICE_ID1)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 3L)
            .with(KnownMdmParams.DELIVERY_TIME, 5L)
            .endServiceValues()
            .build();
        CommonSsku converted = fromDatacampConverter.protoMarketMasterDataToPojo(
            toDatacampConverter.enrichExistingOfferMdOrBuildNew(ssku, null));
        Assertions.assertThat(converted).isEqualTo(ssku);
    }

    @Test
    public void canEnrichExistingOfferWithNewInfo() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .startServiceValues(SERVICE_ID1)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 3L)
            .with(KnownMdmParams.DELIVERY_TIME, 5L)
            .endServiceValues()
            .build();
        DataCampUnitedOffer.UnitedOffer offer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BUSINESS_KEY.getSupplierId())
                    .setOfferId(BUSINESS_KEY.getShopSku()).build())
                .setDelivery(DataCampOfferDelivery.OfferDelivery.newBuilder().setCalculator(
                    DataCampOfferDelivery.DeliveryCalculatorOptions.newBuilder().setDeliveryCalcGeneration(7).build()))
                .setBids(DataCampOfferBids.OfferBids.getDefaultInstance()))
            .build();
        DataCampUnitedOffer.UnitedOffer enriched = toDatacampConverter.enrichExistingOfferMdOrBuildNew(ssku, offer);
        CommonSsku converted = fromDatacampConverter.protoMarketMasterDataToPojo(enriched);
        Assertions.assertThat(converted).isEqualTo(ssku);
        Assertions.assertThat(enriched.getBasic().getDelivery()).isEqualTo(offer.getBasic().getDelivery());
        Assertions.assertThat(enriched.getBasic().getBids()).isEqualTo(offer.getBasic().getBids());
    }

    @Test
    public void whenServiceValuesArePresentShouldAddServiceOffersToOriginalSpec() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .startServiceValues(SERVICE_ID1)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 3L)
            .with(KnownMdmParams.DELIVERY_TIME, 5L)
            .endServiceValues()
            .build();
        var updatedService = (ServiceSsku) ssku.getServiceSsku(SERVICE_ID1).get()
            .addParamValue(generateExistenceMarker(new ShopSkuKey(SERVICE_ID1, SHOP_SKU)));
        ssku.putServiceSsku(updatedService);
        CommonSsku converted = fromDatacampConverter.protoOriginalSpecificationToPojo(
            toDatacampConverter.enrichExistingOfferOriginalSpecificationOrBuildNew(ssku, null));
        Assertions.assertThat(converted).isEqualTo(ssku);
    }

    @Test
    public void whenOnlyBusinessValuesShouldBuildOnlyBaseOfferOriginalSpec() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .build();
        CommonSsku converted = fromDatacampConverter.protoOriginalSpecificationToPojo(
            toDatacampConverter.enrichExistingOfferOriginalSpecificationOrBuildNew(ssku, null));
        Assertions.assertThat(converted).isEqualTo(ssku);
    }

    @Test
    public void whenConvertingDefaultCommonSskuToPartnerDataShouldNotFail() {
        CommonSsku ssku = new CommonSsku(BUSINESS_KEY);
        var converted = fromDatacampConverter.protoOriginalSpecificationToPojo(
            toDatacampConverter.enrichExistingOfferOriginalSpecificationOrBuildNew(ssku, null));
        Assertions.assertThat(converted).isEqualTo(ssku);
    }

    @Test
    public void whenEmptyCommonSskuShouldNotFailWhileEnrichingPartnerData() {
        CommonSsku ssku = new CommonSsku(BUSINESS_KEY);
        ssku.clear();
        ssku.putServiceSsku(new ServiceSsku(new ShopSkuKey(SERVICE_ID1, SHOP_SKU)));
        var updatedService = (ServiceSsku) ssku.getServiceSsku(SERVICE_ID1).get()
            .addParamValue(generateExistenceMarker(new ShopSkuKey(SERVICE_ID1, SHOP_SKU)));
        ssku.putServiceSsku(updatedService);
        var converted = fromDatacampConverter.protoOriginalSpecificationToPojo(
            toDatacampConverter.enrichExistingOfferOriginalSpecificationOrBuildNew(ssku, null));
        Assertions.assertThat(converted).isEqualTo(ssku);
    }

    @Test
    public void shouldUseMaxVersionAsBaseDatacampVersion() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 70L)
            .startServiceValues(1)
                .with(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 65L)
            .endServiceValues()
            .startServiceValues(2)
                .with(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 72L)
            .endServiceValues()
            .startServiceValues(3)
                .with(KnownMdmParams.BOX_COUNT, 2L) // произвольный парам чисто заспавнить сервис
            .endServiceValues()
            .build();

        var unitedOffer = toDatacampConverter.pojoToProtoMarketMasterData(ssku);

        Assertions.assertThat(unitedOffer.getBasic().getContent().getMasterData().getVersion().getValue().getCounter())
            .isEqualTo(72L);
        var services = unitedOffer.getServiceMap();
        Assertions.assertThat(services.get(1).getContent().getMasterData().getVersion().getValue().getCounter())
            .isEqualTo(65L);
        Assertions.assertThat(services.get(2).getContent().getMasterData().getVersion().getValue().getCounter())
            .isEqualTo(72L);
        Assertions.assertThat(services.get(3).getContent().getMasterData().getVersion().getValue().getCounter())
            .isEqualTo(0L);
    }

    @Test
    public void whenTotallyFullBusinessProtoWithAbsolutelyEverythingShouldAddAllToPartnersData() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, BUSINESS_KEY)
            .with(KnownMdmParams.WEIGHT_GROSS, 16L)
            .with(KnownMdmParams.WIDTH, 1L)
            .with(KnownMdmParams.HEIGHT, 2L)
            .with(KnownMdmParams.LENGTH, 3L)
            .with(KnownMdmParams.WEIGHT_NET, 66L)
            .withShelfLife(3, TimeInUnits.TimeUnit.DAY, "sl comment")
            .withLifeTime(4, TimeInUnits.TimeUnit.HOUR, "lt comment")
            .withGuaranteePeriod(5, TimeInUnits.TimeUnit.DAY, "gp comment")
            .with(KnownMdmParams.MANUFACTURER, "manufacturer")
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Нидерланды")
            .with(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID, "0064")
            .with(KnownMdmParams.BOX_COUNT, 121L)
            .with(KnownMdmParams.DOCUMENT_REG_NUMBER, "EC10.RU 00040728-00", "РОСС.6042 Р-21-615112", "WC3ROC WC3TFT")
            .with(KnownMdmParams.VETIS_GUID, "gfdgdfgdf")
            .with(KnownMdmParams.USE_IN_MERCURY, false)
            .startServiceValues(SERVICE_ID1)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 4L)
            .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 8L)
            .with(KnownMdmParams.DELIVERY_TIME, 10L)
            .with(KnownMdmParams.MIN_SHIPMENT, 33L)
            .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption(3), new MdmParamOption(6))
            .startServiceValues(SERVICE_ID2)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 1L)
            .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 5L)
            .with(KnownMdmParams.DELIVERY_TIME, 44L)
            .with(KnownMdmParams.MIN_SHIPMENT, 90L)
            .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption(1))
            .endServiceValues()
            .build();
        var enriched = toDatacampConverter.enrichExistingOfferOriginalSpecificationOrBuildNew(ssku, null);
        log.info("basic partner info: " + enriched.getBasic().getContent().getPartner().toString());
        enriched.getServiceMap().forEach((id, offer) ->
            log.info("service " + id + " partner info: " + offer.getContent().getPartner().toString()));
        var updatedService1 = (ServiceSsku) ssku.getServiceSsku(SERVICE_ID1).get()
            .addParamValue(generateExistenceMarker(new ShopSkuKey(SERVICE_ID1, SHOP_SKU)));
        var updatedService2 = (ServiceSsku) ssku.getServiceSsku(SERVICE_ID2).get()
            .addParamValue(generateExistenceMarker(new ShopSkuKey(SERVICE_ID2, SHOP_SKU)));
        ssku.putServiceSsku(updatedService1).putServiceSsku(updatedService2);
        CommonSsku converted = fromDatacampConverter.protoOriginalSpecificationToPojo(enriched);
        compareCommonSskus(converted, ssku);
    }

    private SskuParamValue generateExistenceMarker(ShopSkuKey serviceKey) {
        return generateSskuBoolValue(serviceKey,
            KnownMdmParams.SERVICE_EXISTS,
            true,
            DataCampOfferMeta.UpdateMeta.newBuilder()
                .setTimestamp(Timestamp.newBuilder()
                    .setSeconds(Instant.now().toEpochMilli() / 1000)
                    .build()
                ).build()
        );
    }

    private SskuParamValue generateSskuBoolValue(ShopSkuKey key,
                                                 long mdmParamId,
                                                 boolean flag,
                                                 DataCampOfferMeta.UpdateMeta meta) {
        SskuParamValue value = new SskuParamValue().setShopSkuKey(key);
        value.setBool(flag);
        value.setMdmParamId(mdmParamId);
        value.setXslName(mdmParamCache.get(mdmParamId).getXslName());
        value.setMasterDataSourceType(MasterDataSourceType.AUTO);
        value.setSourceUpdatedTs(tsFromMeta(meta));
        return value;
    }

    private Instant tsFromMeta(DataCampOfferMeta.UpdateMeta meta) {
        long seconds = meta.getTimestamp().getSeconds();
        long nanos = meta.getTimestamp().getNanos();
        return Instant.ofEpochSecond(seconds, nanos);
    }

    private void compareCommonSskus(CommonSsku first, CommonSsku second) {
        Assertions.assertThat(first.getBaseValues()).containsAll(second.getBaseValues());
        first.getServiceSskus().forEach((id, ssku) ->
            Assertions.assertThat(first.getServiceValues(id)).containsAll(first.getServiceValues(id)));
    }
}
