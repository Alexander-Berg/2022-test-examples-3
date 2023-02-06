package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param;

import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamMetaType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.reference.BmdmExternalReferenceProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.reference.ConstantBmdmExternalReferenceProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.BmdmPathKeeper;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mdm.http.MdmBase;

public class BmdmAttributeToMdmParamConverterTest {
    private static final BmdmExternalReferenceProvider EXTERNAL_REFERENCE_PROVIDER =
        new ConstantBmdmExternalReferenceProvider(TestBmdmUtils.GOLD_MSKU_EXTERNAL_REFERENCES);
    private static final BmdmAttributeToMdmParamConverter CONVERTER =
        new BmdmAttributeToMdmParamConverterImpl(EXTERNAL_REFERENCE_PROVIDER);

    @Test
    public void whenConvertSameAttributeWithDifferentPathsShouldGetDifferentParams() {
        MdmBase.MdmPath goldMskuShelfLifeValuePath = MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.GOLD_MSKU_SHELF_LIFE_ATTRIBUTE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.TIME_ENTITY_TYPE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.TIME_VALUE_ATTRIBUTE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .build())
            .build();
        MdmBase.MdmPath goldMskuLifeTimeValuePath = MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.TIME_ENTITY_TYPE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.TIME_VALUE_ATTRIBUTE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .build())
            .build();

        Optional<MdmParam> shelfLifeParam =
            CONVERTER.createMdmParam(TestBmdmUtils.TIME_ENTITY_VALUE_ATTRIBUTE, goldMskuShelfLifeValuePath);
        Optional<MdmParam> lifeTimeParam =
            CONVERTER.createMdmParam(TestBmdmUtils.TIME_ENTITY_VALUE_ATTRIBUTE, goldMskuLifeTimeValuePath);

        Assertions.assertThat(shelfLifeParam)
            .hasValueSatisfying(param -> {
                Assertions.assertThat(param.getId()).isEqualTo(KnownMdmParams.SHELF_LIFE);
                Assertions.assertThat(param.getXslName()).isEqualTo("LifeShelf");
                Assertions.assertThat(param.getExternals().getMboParamId())
                    .isEqualTo(KnownMdmMboParams.LIFE_SHELF_PARAM_ID);
                Assertions.assertThat(param.getValueType()).isEqualTo(MdmParamValueType.NUMERIC);
            });
        Assertions.assertThat(lifeTimeParam)
            .hasValueSatisfying(param -> {
                Assertions.assertThat(param.getId()).isEqualTo(KnownMdmParams.LIFE_TIME);
                Assertions.assertThat(param.getXslName()).isEqualTo("ShelfService");
                Assertions.assertThat(param.getExternals().getMboParamId())
                    .isEqualTo(KnownMdmMboParams.LIFE_TIME_PARAM_ID);
                Assertions.assertThat(param.getValueType()).isEqualTo(MdmParamValueType.STRING);
            });
    }

    @Test
    public void whenHaveMultipleAttributeReferencesShouldThrowException() {
        // Срок годности золотой MSKU - структура из нескольких аттрибутов, ссылающихся на несколько разных парамов.
        MdmBase.MdmPath goldMskuShelfLifeStructurePath = MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.GOLD_MSKU_SHELF_LIFE_ATTRIBUTE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .build())
            .build();
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() ->
                CONVERTER.createMdmParam(TestBmdmUtils.GOLD_MSKU_SHELF_LIFE_ATTRIBUTE, goldMskuShelfLifeStructurePath))
            .withMessageStartingWith("Found duplicate attribute external references for system OLD_MDM.");
    }

    @Test
    public void testBooleanReferencesConversion() {
        MdmBase.MdmPath hideGuaranteePeriodPath = MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.GOLD_MSKU_GUARANTEE_PERIOD_ATTRIBUTE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.TIME_ENTITY_TYPE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.TIME_HIDDEN_ATTRIBUTE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .build())
            .build();

        MdmParam hideGuaranteePeriodParam =
            CONVERTER.createMdmParam(TestBmdmUtils.TIME_HIDDEN_ATTRIBUTE, hideGuaranteePeriodPath)
                .orElseThrow();
        Assertions.assertThat(hideGuaranteePeriodParam.getId()).isEqualTo(KnownMdmParams.HIDE_GUARANTEE_PERIOD);
        Assertions.assertThat(hideGuaranteePeriodParam.getXslName()).isEqualTo("hide_warranty_period");
        Assertions.assertThat(hideGuaranteePeriodParam.getExternals().getMboParamId())
            .isEqualTo(KnownMdmMboParams.HIDE_GUARANTEE_PERIOD_PARAM_ID);
        Assertions.assertThat(hideGuaranteePeriodParam.getValueType()).isEqualTo(MdmParamValueType.MBO_BOOL);
        Assertions.assertThat(hideGuaranteePeriodParam.getExternals().getBoolBindings().get(true))
            .isEqualTo(KnownMdmMboParams.HIDE_GUARANTEE_PERIOD_TRUE_OPTION_ID);
        Assertions.assertThat(hideGuaranteePeriodParam.getExternals().getBoolBindings().get(false))
            .isEqualTo(KnownMdmMboParams.HIDE_GUARANTEE_PERIOD_FALSE_OPTION_ID);
    }

    @Test
    public void testOptionConversion() {
        MdmBase.MdmPath lifeTimeUnitPath = MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.TIME_ENTITY_TYPE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .build())
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(TestBmdmUtils.TIME_UNIT_ATTRIBUTE_ID)
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .build())
            .build();

        MdmParam lifeTimeUnitParam =
            CONVERTER.createMdmParam(TestBmdmUtils.TIME_UNIT_ATTRIBUTE, lifeTimeUnitPath)
                .orElseThrow();
        Assertions.assertThat(lifeTimeUnitParam.getId()).isEqualTo(KnownMdmParams.LIFE_TIME_UNIT);
        Assertions.assertThat(lifeTimeUnitParam.getXslName()).isEqualTo("ShelfService_Unit");
        Assertions.assertThat(lifeTimeUnitParam.getExternals().getMboParamId())
            .isEqualTo(KnownMdmMboParams.LIFE_TIME_UNIT_PARAM_ID);
        Assertions.assertThat(lifeTimeUnitParam.getValueType()).isEqualTo(MdmParamValueType.MBO_ENUM);
        Assertions.assertThat(lifeTimeUnitParam.getOptions())
            .containsExactly(
                new MdmParamOption()
                    .setId(1)
                    .setRenderedValue("часы"),
                new MdmParamOption()
                    .setId(2)
                    .setRenderedValue("годы"),
                new MdmParamOption()
                    .setId(3)
                    .setRenderedValue("дни"),
                new MdmParamOption()
                    .setId(4)
                    .setRenderedValue("месяцы"),
                new MdmParamOption()
                    .setId(5)
                    .setRenderedValue("недели"),
                new MdmParamOption()
                    .setId(6)
                    .setRenderedValue("не ограничен")
            );
        Assertions.assertThat(lifeTimeUnitParam.getExternals().getOptionBindings())
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                1L, KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.HOUR),
                2L, KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.YEAR),
                3L, KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.DAY),
                4L, KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.MONTH),
                5L, KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.WEEK),
                6L, KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.UNLIMITED)
            ));
    }

    @Test
    public void whenHaveMboReferenceShouldSetMboParamMetaType() {
        // given
        MdmBase.MdmPath lifeTimeUnitPath = new BmdmPathKeeper()
            .addBmdmEntity(TestBmdmUtils.GOLDEN_MSKU_ENTITY_TYPE)
            .addBmdmAttribute(TestBmdmUtils.GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID)
            .addBmdmEntity(TestBmdmUtils.TIME_ENTITY_TYPE)
            .addBmdmAttribute(TestBmdmUtils.TIME_UNIT_ATTRIBUTE_ID)
            .getPath();

        // when
        MdmParam lifeTimeUnitParam =
            CONVERTER.createMdmParam(TestBmdmUtils.TIME_UNIT_ATTRIBUTE, lifeTimeUnitPath).orElseThrow();

        //then
        Assertions.assertThat(lifeTimeUnitParam.getMetaType()).isEqualTo(MdmParamMetaType.MBO_PARAM);
    }

    @Test
    public void whenHaveNoMboReferenceShouldSetMdmSettingMetaType() {
        // given
        MdmBase.MdmPath lifeTimeUnlimitedPath = new BmdmPathKeeper()
            .addBmdmEntity(TestBmdmUtils.GOLDEN_MSKU_ENTITY_TYPE)
            .addBmdmAttribute(TestBmdmUtils.GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID)
            .addBmdmEntity(TestBmdmUtils.TIME_ENTITY_TYPE)
            .addBmdmAttribute(TestBmdmUtils.TIME_UNLIMITED_ATTRIBUTE_ID)
            .getPath();

        // when
        MdmParam lifeTimeUnitParam =
            CONVERTER.createMdmParam(TestBmdmUtils.TIME_UNLIMITED_ATTRIBUTE, lifeTimeUnlimitedPath).orElseThrow();

        //then
        Assertions.assertThat(lifeTimeUnitParam.getMetaType()).isEqualTo(MdmParamMetaType.MDM_SETTING);
    }

    @Test
    public void whenLastSegmentOfGivenPathNotSatisfyGivenAttributeThrowException() {
        MdmBase.MdmPath lifeTimeUnlimitedPath = new BmdmPathKeeper()
            .addBmdmEntity(TestBmdmUtils.GOLDEN_MSKU_ENTITY_TYPE)
            .addBmdmAttribute(TestBmdmUtils.GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID)
            .addBmdmEntity(TestBmdmUtils.TIME_ENTITY_TYPE)
            .addBmdmAttribute(TestBmdmUtils.TIME_UNLIMITED_ATTRIBUTE_ID)
            .getPath();

        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CONVERTER.createMdmParam(TestBmdmUtils.TIME_HIDDEN_ATTRIBUTE, lifeTimeUnlimitedPath))
            .withMessageStartingWith("Given path doesn't lead to given attribute.");
    }
}
