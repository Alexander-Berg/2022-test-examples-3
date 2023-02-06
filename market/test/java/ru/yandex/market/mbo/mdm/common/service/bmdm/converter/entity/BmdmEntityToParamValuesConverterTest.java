package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MetadataProviderMock;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmBooleanAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmEnumAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmInt64AttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmNumericAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStringAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStructAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmEntityTypeToParamsConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmEntityTypeToParamsConverterImpl;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.MdmEntity;

public class BmdmEntityToParamValuesConverterTest {
    private static final long BASE_TEST_TS = Instant.parse("2020-11-24T09:00:00.00Z").toEpochMilli();
    private static final long TEST_MSKU_ID = 1024L;
    private MetadataProviderMock bmdmMetadataProviderMock;
    private BmdmEntityToParamValuesConverter converter;
    private BmdmEntityTypeToParamsConverter paramsConverter;

    private static BmdmEntityToParamValuesConverter createConverter(MetadataProvider bmdmMetadataProvider) {
        BmdmAttributeToMdmParamConverter attributeToMdmParamConverter =
            new BmdmAttributeToMdmParamConverterImpl(bmdmMetadataProvider);
        BmdmEntityToParamValuesConverterImpl converter =
            new BmdmEntityToParamValuesConverterImpl(bmdmMetadataProvider);
        converter.updateAttributeConverters(List.of(
            new BmdmStructAttributeValuesToParamValuesConverter(converter),
            new BmdmEnumAttributeValuesToParamValuesConverter(attributeToMdmParamConverter),
            new BmdmStringAttributeValuesToParamValuesConverter(attributeToMdmParamConverter),
            new BmdmBooleanAttributeValuesToParamValuesConverter(attributeToMdmParamConverter),
            new BmdmInt64AttributeValuesToParamValuesConverter(attributeToMdmParamConverter),
            new BmdmNumericAttributeValuesToParamValuesConverter(attributeToMdmParamConverter)
        ));
        return converter;
    }

    //<editor-fold desc="strict equals" defaultstate="collapsed">
    private static boolean equalsWithTs(Map<Long, MdmParamValue> paramValues1, Map<Long, MdmParamValue> paramValues2) {
        if (paramValues1.size() != paramValues2.size()) {
            return false;
        }
        if (!Objects.equals(paramValues1.keySet(), paramValues2.keySet())) {
            return false;
        }
        for (Long paramId : paramValues1.keySet()) {
            if (!equalsWithTs(paramValues1.get(paramId), paramValues2.get(paramId))) {
                return false;
            }
        }
        return true;
    }

    private static boolean equalsWithTs(MdmParamValue paramValue1, MdmParamValue paramValue2) {
        return paramValue1 == paramValue2
            || Objects.equals(paramValue1, paramValue2)
            && Objects.equals(paramValue1.getUpdatedTs(), paramValue2.getUpdatedTs());
    }
    //</editor-fold>

    //<editor-fold desc="default test data generators" defaultstate="collapsed">
    private static MdmEntity testVghEntity() {
        return MdmEntity.newBuilder()
            .setMdmEntityTypeId(TestBmdmUtils.VGH_ENTITY_TYPE_ID)
            .putMdmAttributeValues(
                TestBmdmUtils.LENGTH_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleNumericValue(
                    TestBmdmUtils.LENGTH_ATTRIBUTE_ID, "25.03", "MDM_ADMIN", "winterfir", BASE_TEST_TS))
            .putMdmAttributeValues(
                TestBmdmUtils.WIDTH_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleNumericValue(
                    TestBmdmUtils.WIDTH_ATTRIBUTE_ID, "8.04", "TOOL", "cloudcat", BASE_TEST_TS + 1))
            .putMdmAttributeValues(
                TestBmdmUtils.HEIGHT_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleNumericValue(
                    TestBmdmUtils.HEIGHT_ATTRIBUTE_ID, "16.04", "MDM_OPERATOR", "amaslak", BASE_TEST_TS + 2))
            .putMdmAttributeValues(
                TestBmdmUtils.WEIGHT_GROSS_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleNumericValue(
                    TestBmdmUtils.WEIGHT_GROSS_ATTRIBUTE_ID, "20.11", "MDM_GLOBAL", "maxkilin", BASE_TEST_TS + 3))
            .putMdmAttributeValues(
                TestBmdmUtils.WEIGHT_NET_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleNumericValue(
                    TestBmdmUtils.WEIGHT_NET_ATTRIBUTE_ID, "4.10", "MEASUREMENT", "albina-gima", BASE_TEST_TS + 4))
            .putMdmAttributeValues(
                TestBmdmUtils.WEIGHT_TARE_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleNumericValue(
                    TestBmdmUtils.WEIGHT_TARE_ATTRIBUTE_ID, "10.11", "SUPPLIER", "sany-der", BASE_TEST_TS + 5))
            .build();
    }

    private static Map<Long, MdmParamValue> testVghParamValues() {
        return Stream.of(
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.LENGTH)
                .setXslName("mdm_length")
                .setNumeric(new BigDecimal("25.03"))
                .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN)
                .setMasterDataSourceId("winterfir")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.WIDTH)
                .setXslName("mdm_width")
                .setNumeric(new BigDecimal("8.04"))
                .setMasterDataSourceType(MasterDataSourceType.TOOL)
                .setMasterDataSourceId("cloudcat")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 1))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.HEIGHT)
                .setXslName("mdm_height")
                .setNumeric(new BigDecimal("16.04"))
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
                .setMasterDataSourceId("amaslak")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 2))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
                .setXslName("mdm_weight_gross")
                .setNumeric(new BigDecimal("20.11"))
                .setMasterDataSourceType(MasterDataSourceType.MDM_GLOBAL)
                .setMasterDataSourceId("maxkilin")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 3))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.WEIGHT_NET)
                .setXslName("mdm_weight_net")
                .setNumeric(new BigDecimal("4.10"))
                .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
                .setMasterDataSourceId("albina-gima")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 4))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.WEIGHT_TARE)
                .setXslName("mdm_weight_tare")
                .setNumeric(new BigDecimal("10.11"))
                .setMasterDataSourceType(MasterDataSourceType.SUPPLIER)
                .setMasterDataSourceId("sany-der")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 5))
                .setSourceUpdatedTs(null)
        ).collect(Collectors.toMap(MdmParamValue::getMdmParamId, Function.identity()));
    }

    private static MdmEntity testShelfLifeEntity() {
        return testTimeEntity("120", TestBmdmUtils.TIME_UNIT_HOUR_OPTION_ID, "shelfLifeComment", false, false);
    }

    private static MdmEntity testLifeTimeEntity() {
        return testTimeEntity(null, null, "lifeTimeComment", true, false);
    }

    private static MdmEntity testGuaranteePeriodEntity() {
        return testTimeEntity("50", TestBmdmUtils.TIME_UNIT_YEAR_OPTION_ID, "guaranteePeriodComment", false, true);
    }

    private static MdmEntity testTimeEntity(String value,
                                            Long unitOptionId,
                                            String comment,
                                            Boolean unlimited,
                                            Boolean hidden) {
        MdmEntity.Builder builder = MdmEntity.newBuilder()
            .setMdmEntityTypeId(TestBmdmUtils.TIME_ENTITY_TYPE_ID);
        if (value != null) {
            builder.putMdmAttributeValues(
                TestBmdmUtils.TIME_VALUE_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleNumericValue(
                    TestBmdmUtils.TIME_VALUE_ATTRIBUTE_ID, value, "MDM_ADMIN", "winterfir", BASE_TEST_TS + 6));
        }
        if (unitOptionId != null) {
            builder.putMdmAttributeValues(
                TestBmdmUtils.TIME_UNIT_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleEnumValue(
                    TestBmdmUtils.TIME_UNIT_ATTRIBUTE_ID, unitOptionId, "TOOL", "cloudcat", BASE_TEST_TS + 7));
        }
        if (comment != null) {
            builder.putMdmAttributeValues(
                TestBmdmUtils.TIME_COMMENT_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleStringValue(
                    TestBmdmUtils.TIME_COMMENT_ATTRIBUTE_ID, comment, "MDM_OPERATOR", "amaslak", BASE_TEST_TS + 8));
        }
        if (unlimited != null) {
            builder.putMdmAttributeValues(
                TestBmdmUtils.TIME_UNLIMITED_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleBooleanValue(
                    TestBmdmUtils.TIME_UNLIMITED_ATTRIBUTE_ID, unlimited, "MDM_GLOBAL", "maxkilin", BASE_TEST_TS + 9));
        }
        if (hidden != null) {
            builder.putMdmAttributeValues(
                TestBmdmUtils.TIME_HIDDEN_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleBooleanValue(
                    TestBmdmUtils.TIME_HIDDEN_ATTRIBUTE_ID, hidden, "MEASUREMENT", "albina-gima", BASE_TEST_TS + 10));
        }
        return builder.build();
    }

    private static Map<Long, MdmParamValue> testShelfLifeParamValues() {
        return Stream.of(
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.SHELF_LIFE)
                .setXslName("LifeShelf")
                .setNumeric(new BigDecimal("120"))
                .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN)
                .setMasterDataSourceId("winterfir")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 6))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
                .setXslName("ShelfLife_Unit")
                .setOption(
                    new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.HOUR))
                )
                .setMasterDataSourceType(MasterDataSourceType.TOOL)
                .setMasterDataSourceId("cloudcat")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 7))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.SHELF_LIFE_COMMENT)
                .setXslName("ShelfLife_Comment")
                .setString("shelfLifeComment")
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
                .setMasterDataSourceId("amaslak")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 8))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNLIMITED)
                .setXslName("mdm_shelf_life_unlimited")
                .setBool(false)
                .setMasterDataSourceType(MasterDataSourceType.MDM_GLOBAL)
                .setMasterDataSourceId("maxkilin")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 9))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.HIDE_SHELF_LIFE)
                .setXslName("hide_shelf_life")
                .setBool(false)
                .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
                .setMasterDataSourceId("albina-gima")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 10))
                .setSourceUpdatedTs(null)
        ).collect(Collectors.toMap(MdmParamValue::getMdmParamId, Function.identity()));
    }

    private static Map<Long, MdmParamValue> testLifeTimeParamValues() {
        return Stream.of(
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.LIFE_TIME_COMMENT)
                .setXslName("ShelfService_Comment")
                .setString("lifeTimeComment")
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
                .setMasterDataSourceId("amaslak")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 8))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.LIFE_TIME_UNLIMITED)
                .setXslName("mdm_life_time_unlimited")
                .setBool(true)
                .setMasterDataSourceType(MasterDataSourceType.MDM_GLOBAL)
                .setMasterDataSourceId("maxkilin")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 9))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.HIDE_LIFE_TIME)
                .setXslName("hide_life_time")
                .setBool(false)
                .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
                .setMasterDataSourceId("albina-gima")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 10))
                .setSourceUpdatedTs(null)
        ).collect(Collectors.toMap(MdmParamValue::getMdmParamId, Function.identity()));
    }

    private static Map<Long, MdmParamValue> testGuaranteePeriodParamValues() {
        return Stream.of(
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD)
                .setXslName("WarrantyPeriod")
                .setString("50")
                .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN)
                .setMasterDataSourceId("winterfir")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 6))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD_UNIT)
                .setXslName("WarrantyPeriod_Unit")
                .setOption(
                    new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR))
                )
                .setMasterDataSourceType(MasterDataSourceType.TOOL)
                .setMasterDataSourceId("cloudcat")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 7))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD_COMMENT)
                .setXslName("WarrantyPeriod_Comment")
                .setString("guaranteePeriodComment")
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
                .setMasterDataSourceId("amaslak")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 8))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD_UNLIMITED)
                .setXslName("mdm_guarantee_period_unlimited")
                .setBool(false)
                .setMasterDataSourceType(MasterDataSourceType.MDM_GLOBAL)
                .setMasterDataSourceId("maxkilin")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 9))
                .setSourceUpdatedTs(null),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.HIDE_GUARANTEE_PERIOD)
                .setXslName("hide_warranty_period")
                .setBool(true)
                .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
                .setMasterDataSourceId("albina-gima")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 10))
                .setSourceUpdatedTs(null)
        ).collect(Collectors.toMap(MdmParamValue::getMdmParamId, Function.identity()));
    }

    private static MdmEntity testMskuEntity() {
        return MdmEntity.newBuilder()
            .setMdmEntityTypeId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
            .putMdmAttributeValues(TestBmdmUtils.GOLD_MSKU_ID_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleInt64Value(
                    TestBmdmUtils.GOLD_MSKU_ID_ATTRIBUTE_ID,
                    TEST_MSKU_ID,
                    "MDM_UNKNOWN",
                    "anonymous",
                    BASE_TEST_TS + 1024))
            .putMdmAttributeValues(TestBmdmUtils.GOLD_MSKU_VGH_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleStructValue(TestBmdmUtils.GOLD_MSKU_VGH_ATTRIBUTE_ID, testVghEntity()))
            .putMdmAttributeValues(TestBmdmUtils.GOLD_MSKU_SHELF_LIFE_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleStructValue(
                    TestBmdmUtils.GOLD_MSKU_SHELF_LIFE_ATTRIBUTE_ID, testShelfLifeEntity()))
            .putMdmAttributeValues(TestBmdmUtils.GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleStructValue(
                    TestBmdmUtils.GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID, testLifeTimeEntity()))
            .putMdmAttributeValues(TestBmdmUtils.GOLD_MSKU_GUARANTEE_PERIOD_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleStructValue(TestBmdmUtils.GOLD_MSKU_GUARANTEE_PERIOD_ATTRIBUTE_ID,
                    testGuaranteePeriodEntity()))
            .build();
    }

    private static Map<Long, MdmParamValue> testMskuParamValues() {
        LinkedHashMap<Long, MdmParamValue> result = new LinkedHashMap<>();
        result.put(
            KnownMdmParams.MSKU_ID_REFERENCE,
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.MSKU_ID_REFERENCE)
                .setXslName("mskuIdReference")
                .setNumeric(BigDecimal.valueOf(TEST_MSKU_ID))
                .setMasterDataSourceType(MasterDataSourceType.MDM_UNKNOWN)
                .setMasterDataSourceId("anonymous")
                .setUpdatedTs(Instant.ofEpochMilli(BASE_TEST_TS + 1024))
                .setSourceUpdatedTs(null)
        );
        result.putAll(testVghParamValues());
        result.putAll(testShelfLifeParamValues());
        result.putAll(testLifeTimeParamValues());
        result.putAll(testGuaranteePeriodParamValues());
        return result;
    }

    //</editor-fold>

    @Before
    public void setUp() {
        bmdmMetadataProviderMock = new MetadataProviderMock();
        converter = createConverter(bmdmMetadataProviderMock);
        paramsConverter = new BmdmEntityTypeToParamsConverterImpl(
            bmdmMetadataProviderMock,
            new BmdmAttributeToMdmParamConverterImpl(bmdmMetadataProviderMock)
        );
    }

    @Test
    public void whenUnknownEntityTypeShouldFail() {
        MdmEntity entity = MdmEntity.newBuilder().setMdmEntityTypeId(1447).build();
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> converter.parseParamValues(entity, 100))
            .withMessage("Can't find entity type 1447");
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> converter.buildBmdmEntityFromParamValues(1448, Map.of(), 100))
            .withMessage("Can't find entity type 1448");
    }

    @Test
    public void whenEntityTypeIsInconsistentShouldFail() {
        // У entity type чужой аттрибут
        var entityType = MdmBase.MdmEntityType.newBuilder()
            .setMdmId(108)
            .addAttributes(
                MdmBase.MdmAttribute.newBuilder()
                    .setMdmId(50)
                    .setMdmEntityTypeId(109)
            )
            .build();
        bmdmMetadataProviderMock.addEntityType(entityType);

        MdmEntity entity = MdmEntity.newBuilder().setMdmEntityTypeId(entityType.getMdmId()).build();
        Assertions.assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> converter.parseParamValues(entity, 100))
            .withMessage("Entity type 108 is inconsistent");
        Assertions.assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> converter.buildBmdmEntityFromParamValues(entityType.getMdmId(), Map.of(), 100))
            .withMessage("Entity type 108 is inconsistent");
    }

    @Test
    public void whenEntityStructureNotConsistentShouldFail() {
        var attribute1 = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(50)
            .setMdmEntityTypeId(108)
            .build();
        var entityType1 = MdmBase.MdmEntityType.newBuilder()
            .setMdmId(attribute1.getMdmEntityTypeId())
            .addAttributes(attribute1)
            .build();
        bmdmMetadataProviderMock.addEntityType(entityType1);

        MdmEntity entity1 = MdmEntity.newBuilder()
            .setMdmId(146)
            .setMdmEntityTypeId(entityType1.getMdmId())
            .putMdmAttributeValues(
                attribute1.getMdmId(),
                MdmAttributeValues.newBuilder().setMdmAttributeId(attribute1.getMdmId() + 1).build())
            .build();
        Assertions.assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> converter.parseParamValues(entity1, 100))
            .withMessage("In entity 146 of type 108 attribute 51 is stored by key 50");

        var attribute2 = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(2050)
            .setMdmEntityTypeId(2108)
            .build();
        var entityType2 = MdmBase.MdmEntityType.newBuilder()
            .setMdmId(attribute2.getMdmEntityTypeId())
            .addAttributes(attribute2)
            .build();
        bmdmMetadataProviderMock.addEntityType(entityType2);

        MdmEntity entity2 = MdmEntity.newBuilder()
            .setMdmId(2146)
            .setMdmEntityTypeId(entityType2.getMdmId())
            .putMdmAttributeValues(
                attribute2.getMdmId() + 1,
                MdmAttributeValues.newBuilder().setMdmAttributeId(attribute2.getMdmId() + 1).build())
            .build();
        Assertions.assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> converter.parseParamValues(entity2, 100))
            .withMessage("Unexpected attribute 2051 in entity 2146 of type 2108");
    }

    @Test
    public void testGoldMskuConversion() {
        bmdmMetadataProviderMock.addEntityType(TestBmdmUtils.VGH_ENTITY_TYPE);
        bmdmMetadataProviderMock.addEntityType(TestBmdmUtils.TIME_ENTITY_TYPE);
        bmdmMetadataProviderMock.addEntityType(TestBmdmUtils.GOLDEN_MSKU_ENTITY_TYPE);
        bmdmMetadataProviderMock.addExternalReferences(TestBmdmUtils.GOLD_MSKU_EXTERNAL_REFERENCES);
        testConversion(testMskuEntity(), testMskuParamValues());
    }

    @Test
    public void whenRecursionLimitExceededShouldThrowException() {
        bmdmMetadataProviderMock.addEntityType(TestBmdmUtils.VGH_ENTITY_TYPE);
        bmdmMetadataProviderMock.addEntityType(TestBmdmUtils.TIME_ENTITY_TYPE);
        bmdmMetadataProviderMock.addEntityType(TestBmdmUtils.GOLDEN_MSKU_ENTITY_TYPE);
        bmdmMetadataProviderMock.addExternalReferences(TestBmdmUtils.GOLD_MSKU_EXTERNAL_REFERENCES);
        Assertions.assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> converter.parseParamValues(testMskuEntity(), 3))
            .withMessageStartingWith("Out of depth limit. Depth limit: 3. Path length: 4.");
        Assertions.assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> converter.buildBmdmEntityFromParamValues(
                KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID, testMskuParamValues(), 3))
            .withMessageStartingWith("Out of depth limit. Depth limit: 3. Path length: 4.");
        Assertions.assertThatNoException()
            .isThrownBy(() -> converter.parseParamValues(testMskuEntity(), 4));
        Assertions.assertThatNoException()
            .isThrownBy(() -> converter.buildBmdmEntityFromParamValues(
                KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID, testMskuParamValues(), 4));
    }

    @Test
    public void testFlatGoldMskuConversion() {
        Random random = new Random(Objects.hash("I hate BMDM."));
        bmdmMetadataProviderMock.addEntityType(TestBmdmUtils.FLAT_GOLD_MSKU_ENTITY_TYPE);
        bmdmMetadataProviderMock.addExternalReferences(TestBmdmUtils.FLAT_GOLD_MSKU_EXTERNAL_REFERENCES);
        MdmEntity mdmEntity = TestBmdmUtils.createFullRandomEntity(
            KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID, random, bmdmMetadataProviderMock)
            .orElseThrow();
        Map<Long, MdmParamValue> paramValuesFromEntity = converter.parseParamValues(mdmEntity, 2);
        Assertions.assertThat(converter.buildBmdmEntityFromParamValues(
            KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID, paramValuesFromEntity, 2))
            .contains(mdmEntity);

        Map<Long, MdmParamValue> paramValues1 = testMskuParamValues();
        Optional<MdmEntity> entityFromParamValues1 =
            converter.buildBmdmEntityFromParamValues(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID, paramValues1, 2);
        Assertions.assertThat(converter.parseParamValues(entityFromParamValues1.orElseThrow(), 2))
            .isEqualTo(paramValues1);

        Set<MdmParam> flatGoldMskuParams = paramsConverter.extractParams(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID);
        Map<Long, MdmParamValue> paramValues2 =
            TestMdmParamUtils.createRandomMdmParamValues(random, flatGoldMskuParams);
        Optional<MdmEntity> entityFromParamValues2 =
            converter.buildBmdmEntityFromParamValues(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID, paramValues2, 2);
        Assertions.assertThat(converter.parseParamValues(entityFromParamValues2.orElseThrow(), 2))
            .isEqualTo(paramValues2);
    }


    private void testConversion(MdmEntity mdmEntity, Map<Long, MdmParamValue> paramValues) {
        MdmEntity entityFromParamValues =
            converter.buildBmdmEntityFromParamValues(mdmEntity.getMdmEntityTypeId(), paramValues, 10)
                .orElse(null);
        Assertions.assertThat(entityFromParamValues).isEqualTo(mdmEntity);
        Assertions.assertThat(equalsWithTs(converter.parseParamValues(entityFromParamValues, 10), paramValues))
            .isTrue();

        Map<Long, MdmParamValue> paramValuesFromEntity = converter.parseParamValues(mdmEntity, 10);
        Assertions.assertThat(equalsWithTs(paramValues, paramValuesFromEntity)).isTrue();
        Assertions.assertThat(
            converter.buildBmdmEntityFromParamValues(mdmEntity.getMdmEntityTypeId(), paramValuesFromEntity, 10))
            .contains(mdmEntity);
    }
}
