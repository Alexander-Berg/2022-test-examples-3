package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.function.Function;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;

public class DimensionsBlockTest {
    private static final long MSKU_ID = 0L;
    private final MdmParamCache paramCache =
        TestMdmParamUtils.createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());

    @Before
    public void setUp() {
    }

    @Test
    public void tesConvertingToMskuParamValues() {
        MdmParam lengthParam = paramCache.get(KnownMdmParams.LENGTH);
        MdmParam widthParam = paramCache.get(KnownMdmParams.WIDTH);
        MdmParam heightParam = paramCache.get(KnownMdmParams.HEIGHT);
        MdmParam weightGrossParam = paramCache.get(KnownMdmParams.WEIGHT_GROSS);

        MdmParamValue length = new MdmParamValue()
            .setMdmParamId(KnownMdmParams.LENGTH)
            .setNumeric(new BigDecimal("12"))
            .setMasterDataSourceId("from_vasya_pupkin_with_love")
            .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT);
        MdmParamValue width = new MdmParamValue()
            .setMdmParamId(KnownMdmParams.WIDTH)
            .setNumeric(new BigDecimal("13"))
            .setUpdatedTs(Instant.EPOCH.plusSeconds(1_000_000_002L))
            .setSourceUpdatedTs(Instant.EPOCH.plusSeconds(999_999_998L))
            .setMasterDataSourceId("from_super_supplier")
            .setMasterDataSourceType(MasterDataSourceType.SUPPLIER);
        MdmParamValue height = new MdmParamValue()
            .setMdmParamId(KnownMdmParams.HEIGHT)
            .setNumeric(new BigDecimal("14"))
            .setUpdatedTs(Instant.EPOCH.plusSeconds(1_000_000_003L))
            .setSourceUpdatedTs(Instant.EPOCH.plusSeconds(999_999_997L))
            .setMasterDataSourceId("from_super_warehouse")
            .setMasterDataSourceType(MasterDataSourceType.WAREHOUSE);
        MdmParamValue weightGross = new MdmParamValue()
            .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
            .setNumeric(new BigDecimal("314"))
            .setUpdatedTs(Instant.EPOCH.plusSeconds(1_000_000_004L))
            .setSourceUpdatedTs(Instant.EPOCH.plusSeconds(999_999_996L))
            .setMasterDataSourceId("from_super_supplier")
            .setMasterDataSourceType(MasterDataSourceType.SUPPLIER);

        List<MskuParamValue> expectedMskuParamValues = List.of(
            fromMdmParamValue(length, lengthParam),
            fromMdmParamValue(width, widthParam),
            fromMdmParamValue(height, heightParam),
            fromMdmParamValue(weightGross, weightGrossParam)
        );

        DimensionsBlock block = new DimensionsBlock(length, width, height, weightGross);
        List<MskuParamValue> mskuParamValues =
            block.toMskuParamValues(MSKU_ID, lengthParam, widthParam, heightParam, weightGrossParam);

        assertEqualsWithTimestamps(mskuParamValues, expectedMskuParamValues);

        //Для параметра length мы не задали UpdatedTs, но он всё равно не должен быть нулевым
        Assertions.assertThat(block.getBoxLengthUm().getUpdatedTs()).isNotZero();
    }

    private void assertEqualsWithTimestamps(List<MskuParamValue> actual, List<MskuParamValue> expected) {
        Assertions.assertThat(actual).containsExactlyElementsOf(expected);
        Assertions.assertThat(extractFields(actual, MskuParamValue::getSourceUpdatedTs))
            .containsExactlyElementsOf(extractFields(expected, MskuParamValue::getSourceUpdatedTs));
        Assertions.assertThat(extractFields(actual, MskuParamValue::getUpdatedTs))
            .containsExactlyElementsOf(extractFields(expected, MskuParamValue::getUpdatedTs));
    }

    private <T> List<T> extractFields(List<MskuParamValue> mskuParamValues, Function<MskuParamValue, T> extractor) {
        return mskuParamValues.stream().map(extractor).collect(Collectors.toList());
    }

    private MskuParamValue fromMdmParamValue(MdmParamValue paramValue, MdmParam mdmParam) {
        MskuParamValue result = new MskuParamValue().setMskuId(MSKU_ID);
        result.setMdmParamId(paramValue.getMdmParamId())
            .setNumeric(paramValue.getNumeric().orElse(null))
            .setUpdatedTs(paramValue.getUpdatedTs())
            .setSourceUpdatedTs(paramValue.getSourceUpdatedTs())
            .setMasterDataSourceId(paramValue.getMasterDataSourceId())
            .setMasterDataSourceType(paramValue.getMasterDataSourceType())
            .setXslName(mdmParam.getXslName());
        return result;
    }
}
