package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.msku;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MetadataProviderMock;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmBooleanAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmEnumAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmInt64AttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmNumericAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStringAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStructAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.entity.BmdmEntityToParamValuesConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverterImpl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.MdmEntity;

public class BmdmEntityToCommonMskuConverterTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmParamCache mdmParamCache;

    private MetadataProviderMock metadataProviderMock;
    private BmdmEntityToCommonMskuConverter converter;

    @Before
    public void setUp() throws Exception {
        metadataProviderMock = new MetadataProviderMock();
        metadataProviderMock.addEntityType(TestBmdmUtils.FLAT_GOLD_MSKU_ENTITY_TYPE);
        metadataProviderMock.addEntityType(TestBmdmUtils.GOLDEN_MSKU_ENTITY_TYPE);
        metadataProviderMock.addEntityType(TestBmdmUtils.VGH_ENTITY_TYPE);
        metadataProviderMock.addEntityType(TestBmdmUtils.TIME_ENTITY_TYPE);
        metadataProviderMock.addExternalReferences(TestBmdmUtils.GOLD_MSKU_EXTERNAL_REFERENCES);
        metadataProviderMock.addExternalReferences(TestBmdmUtils.FLAT_GOLD_MSKU_EXTERNAL_REFERENCES);
        converter = createConverter();
    }


    @Test
    public void testConvertNullToNull() {
        Assertions.assertThat(converter.commonMskuToEntity(null, 7, 10)).isNull();
        Assertions.assertThat(converter.entityToCommonMsku(null, 7, 10)).isNull();
    }

    @Test
    public void whenTryToConvertInconsistentCommonMskuShouldFail() {
        // в самой CommonMsku один  msku id, а на ParamValues другой
        var inconsistentMsku = new CommonMsku(
            10,
            List.of(new MskuParamValue().setMskuId(11))
        );
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> converter.commonMskuToEntity(inconsistentMsku, 12, 10))
            .withMessageStartingWith("Given common msku have inconsistent msku ids on values:");
    }

    @Test
    public void testUnlimitedConversion() {
        // Используются разные способы хранения неограниченных сроков
        // В старом мдм специальная еденица измерения
        // В новом флажок
        long mskuId = 115L;
        long entityId = 42L;
        Instant updatedTs = Instant.now().plus(2, ChronoUnit.MILLIS);
        MdmEntity mdmEntity = MdmEntity.newBuilder()
            .setMdmId(entityId)
            .setMdmEntityTypeId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
            .putMdmAttributeValues(
                TestBmdmUtils.GOLD_MSKU_ID_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleInt64Value(
                    TestBmdmUtils.GOLD_MSKU_ID_ATTRIBUTE_ID, mskuId, "AUTO", "", 0))
            .putMdmAttributeValues(
                TestBmdmUtils.GOLD_MSKU_SHELF_LIFE_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleStructValue(
                    TestBmdmUtils.GOLD_MSKU_SHELF_LIFE_ATTRIBUTE_ID,
                    MdmEntity.newBuilder()
                        .setMdmEntityTypeId(TestBmdmUtils.TIME_ENTITY_TYPE_ID)
                        .putMdmAttributeValues(
                            TestBmdmUtils.TIME_UNLIMITED_ATTRIBUTE_ID,
                            TestBmdmUtils.createSingleBooleanValue(
                                TestBmdmUtils.TIME_UNLIMITED_ATTRIBUTE_ID, true, "AUTO", "vasya", 2))
                        .build()))
            .putMdmAttributeValues(
                TestBmdmUtils.GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleStructValue(
                    TestBmdmUtils.GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID,
                    MdmEntity.newBuilder()
                        .setMdmEntityTypeId(TestBmdmUtils.TIME_ENTITY_TYPE_ID)
                        .putMdmAttributeValues(
                            TestBmdmUtils.TIME_UNLIMITED_ATTRIBUTE_ID,
                            TestBmdmUtils.createSingleBooleanValue(
                                TestBmdmUtils.TIME_UNLIMITED_ATTRIBUTE_ID, true, "AUTO", "vasya", 2))
                        .build()))
            .build();
        mdmEntity = mdmEntity.toBuilder().setMdmUpdateMeta(
            MdmBase.MdmUpdateMeta.newBuilder()
                .setFrom(updatedTs.toEpochMilli()).build()
        ).build();

        MskuParamValue shelfLifeValue = new MskuParamValue().setMskuId(mskuId);
        shelfLifeValue.setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE).getXslName())
            .setNumeric(BigDecimal.ONE)
            .setMasterDataSourceType(MasterDataSourceType.AUTO)
            .setMasterDataSourceId("vasya")
            .setUpdatedTs(Instant.EPOCH.plus(2, ChronoUnit.MILLIS))
            .setSourceUpdatedTs(null);
        MskuParamValue shelfLifeUnit = new MskuParamValue().setMskuId(mskuId);
        shelfLifeUnit.setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName())
            .setOption(new MdmParamOption(
                KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.UNLIMITED)))
            .setMasterDataSourceType(MasterDataSourceType.AUTO)
            .setMasterDataSourceId("vasya")
            .setUpdatedTs(Instant.EPOCH.plus(2, ChronoUnit.MILLIS))
            .setSourceUpdatedTs(null);
        MskuParamValue lifeTimeValue = new MskuParamValue().setMskuId(mskuId);
        lifeTimeValue.setMdmParamId(KnownMdmParams.LIFE_TIME)
            .setXslName(mdmParamCache.get(KnownMdmParams.LIFE_TIME).getXslName())
            .setString("1")
            .setMasterDataSourceType(MasterDataSourceType.AUTO)
            .setMasterDataSourceId("vasya")
            .setUpdatedTs(Instant.EPOCH.plus(2, ChronoUnit.MILLIS))
            .setSourceUpdatedTs(null);
        MskuParamValue lifeTimeUnit = new MskuParamValue().setMskuId(mskuId);
        lifeTimeUnit.setMdmParamId(KnownMdmParams.LIFE_TIME_UNIT)
            .setXslName(mdmParamCache.get(KnownMdmParams.LIFE_TIME_UNIT).getXslName())
            .setOption(new MdmParamOption(
                KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.UNLIMITED)))
            .setMasterDataSourceType(MasterDataSourceType.AUTO)
            .setMasterDataSourceId("vasya")
            .setUpdatedTs(Instant.EPOCH.plus(2, ChronoUnit.MILLIS))
            .setSourceUpdatedTs(null);
        MskuParamValue version = new MskuParamValue().setMskuId(mskuId);
        version.setMdmParamId(KnownMdmParams.BMDM_ID)
            .setXslName(mdmParamCache.get(KnownMdmParams.BMDM_ID).getXslName())
            .setNumeric(BigDecimal.valueOf(entityId))
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN)
            .setUpdatedTs(updatedTs)
            .setSourceUpdatedTs(null);
        CommonMsku commonMsku =
            new CommonMsku(mskuId, List.of(shelfLifeValue, shelfLifeUnit, lifeTimeValue, lifeTimeUnit, version));

        testGoldMskuConversion(mdmEntity, commonMsku);
    }

    @Test
    public void testFlatGoldMskuEntityConversion() {
        long mskuId = 115L;
        long entityId = 42L;
        MskuParamValue heavyGood = new MskuParamValue().setMskuId(mskuId);
        heavyGood.setMdmParamId(KnownMdmParams.HEAVY_GOOD)
            .setXslName(mdmParamCache.get(KnownMdmParams.HEAVY_GOOD).getXslName())
            .setBool(true)
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN)
            .setUpdatedTs(Instant.EPOCH.plus(75, ChronoUnit.HOURS));
        MskuParamValue value = new MskuParamValue().setMskuId(mskuId);
        value.setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE).getXslName())
            .setNumeric(BigDecimal.ONE)
            .setMasterDataSourceType(MasterDataSourceType.AUTO)
            .setMasterDataSourceId("vasya")
            .setUpdatedTs(Instant.EPOCH.plus(2, ChronoUnit.MILLIS));
        MskuParamValue unit = new MskuParamValue().setMskuId(mskuId);
        unit.setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName())
            .setOption(new MdmParamOption(
                KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.UNLIMITED)))
            .setMasterDataSourceType(MasterDataSourceType.AUTO)
            .setMasterDataSourceId("vasya")
            .setUpdatedTs(Instant.EPOCH.plus(2, ChronoUnit.MILLIS));
        MskuParamValue version = new MskuParamValue().setMskuId(mskuId);
        version.setMdmParamId(KnownMdmParams.BMDM_ID)
            .setXslName(mdmParamCache.get(KnownMdmParams.BMDM_ID).getXslName())
            .setNumeric(BigDecimal.valueOf(entityId))
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN)
            .setUpdatedTs(Instant.EPOCH.plus(2, ChronoUnit.MILLIS));
        CommonMsku commonMsku = new CommonMsku(mskuId, List.of(value, unit, version));
        MdmEntity mdmEntity = converter.commonMskuToFlatGoldMskuEntity(commonMsku);
        Assertions.assertThat(converter.flatGoldMskuEntityToCommonMsku(mdmEntity))
            .isEqualTo(commonMsku);
    }

    private BmdmEntityToCommonMskuConverter createConverter() {
        BmdmAttributeToMdmParamConverter bmdmAttributeToMdmParamConverter =
            new BmdmAttributeToMdmParamConverterImpl(metadataProviderMock);
        BmdmEntityToParamValuesConverterImpl universalConverter =
            new BmdmEntityToParamValuesConverterImpl(metadataProviderMock);
        universalConverter.updateAttributeConverters(List.of(
            new BmdmStructAttributeValuesToParamValuesConverter(universalConverter),
            new BmdmEnumAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmStringAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmBooleanAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmInt64AttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmNumericAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter)
        ));
        return new BmdmEntityToCommonMskuConverterImpl(universalConverter, mdmParamCache);
    }

    private void testGoldMskuConversion(MdmEntity mdmEntity, CommonMsku commonMsku) {
        testMskuConversion(
            mdmEntity,
            commonMsku,
            converter::goldMskuEntityToCommonMsku,
            converter::commonMskuToGoldMskuEntity
        );
    }

    private void testFlatGoldMskuConversion(MdmEntity mdmEntity, CommonMsku commonMsku) {
        testMskuConversion(
            mdmEntity,
            commonMsku,
            converter::flatGoldMskuEntityToCommonMsku,
            converter::commonMskuToFlatGoldMskuEntity
        );
    }

    private void testMskuConversion(MdmEntity mdmEntity,
                                    CommonMsku commonMsku,
                                    Function<MdmEntity, CommonMsku> entity2CommonMsku,
                                    Function<CommonMsku, MdmEntity> commonMsku2Entity) {
        MdmEntity entityFromCommonMsku = commonMsku2Entity.apply(commonMsku);
        Assertions.assertThat(entityFromCommonMsku).isEqualTo(mdmEntity);
        Assertions.assertThat(entity2CommonMsku.apply(entityFromCommonMsku)).isEqualTo(commonMsku);

        CommonMsku commonMskuFromEntity = entity2CommonMsku.apply(mdmEntity);
        Assertions.assertThat(commonMskuFromEntity).isEqualTo(commonMsku);
        Assertions.assertThat(commonMsku2Entity.apply(commonMskuFromEntity)).isEqualTo(mdmEntity);
    }
}
