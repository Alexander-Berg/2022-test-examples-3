package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.CustomsCommCodeBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MdmParamValueBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

public class TestBlockCreationUtil {
    private static final MdmParamCache MDM_PARAM_CACHE = TestMdmParamUtils.createParamCacheMock();

    private TestBlockCreationUtil() {
    }

    public static MdmParamValueBlock<BigDecimal> createNumericMdmParamValueBlock(long mdmParamId, BigDecimal value) {
        return createNumericMdmParamValueBlock(
            ItemBlock.BlockType.MDM_PARAM, mdmParamId, value, MasterDataSource.DEFAULT_AUTO_SOURCE, Instant.EPOCH
        );
    }

    public static MdmParamValueBlock<BigDecimal> createNumericMdmParamValueBlock(ItemBlock.BlockType blockType,
                                                                                 long mdmParamId,
                                                                                 BigDecimal value,
                                                                                 MasterDataSource masterDataSource,
                                                                                 Instant updatedTs) {
        MdmParam mdmParam = MDM_PARAM_CACHE.get(mdmParamId);
        var block = new MdmParamValueBlock<BigDecimal>(blockType, mdmParam);
        block.fromSskuMasterData(value, updatedTs, masterDataSource);
        return block;
    }

    public static MdmParamValueBlock<String> createStringMdmParamValueBlock(long mdmParamId,
                                                                            String value,
                                                                            Instant updatedTs) {
        MdmParam mdmParam = MDM_PARAM_CACHE.get(mdmParamId);
        var block = new MdmParamValueBlock<String>(ItemBlock.BlockType.MDM_PARAM, mdmParam);
        block.fromSskuMasterData(value, updatedTs);
        return block;
    }

    public static MdmParamValueBlock<Boolean> createBoolParamValueBlock(ItemBlock.BlockType blockType,
                                                                         long mdmParamId,
                                                                         boolean boolValue,
                                                                         MasterDataSource source,
                                                                         Instant updatedTs,
                                                                         String updatedByLogin) {
        MdmParam param = MDM_PARAM_CACHE.get(mdmParamId);
        MdmParamValueBlock<Boolean> block = new MdmParamValueBlock<>(blockType, param);
        MdmParamValue paramValue = new MdmParamValue();
        paramValue.setBool(boolValue)
            .setMdmParamId(mdmParamId)
            .setXslName(param.getXslName())
            .setMasterDataSource(source)
            .setUpdatedByLogin(updatedByLogin)
            .setUpdatedTs(updatedTs);
        block.fromMdmParamValue(paramValue);
        return block;
    }

    public static MdmParamValueBlock<Boolean> createBoolParamValueBlock(ItemBlock.BlockType blockType,
                                                          long mdmParamId,
                                                          boolean boolValue,
                                                          MasterDataSourceType sourceType,
                                                          String sourceId,
                                                          String updatedByLogin) {
        return createBoolParamValueBlock(blockType, mdmParamId, boolValue, new MasterDataSource(sourceType, sourceId),
            DateTimeUtils.instantNow(), updatedByLogin);
    }

    public static MdmParamValueBlock<String> createStringsMdmParamValueBlock(long mdmParamId,
                                                                             List<String> values,
                                                                             Instant updatedTs) {
        MdmParam mdmParam = MDM_PARAM_CACHE.get(mdmParamId);
        MdmParamValue paramValue = new MdmParamValue()
            .setMdmParamId(mdmParam.getId())
            .setXslName(mdmParam.getXslName())
            .setStrings(values)
            .setMasterDataSource(MasterDataSource.DEFAULT_AUTO_SOURCE);
        return new MdmParamValueBlock<String>(ItemBlock.BlockType.MDM_PARAM, mdmParam).fromMdmParamValue(paramValue);
    }

    public static ValueCommentBlock createValueCommentBlock(
        ItemBlock.BlockType blockType,
        long valueParamId, long unitParamId, long commentParamId,
        TimeInUnits value, String comment,
        Instant updatedTs
    ) {
        MdmParam valueParam = MDM_PARAM_CACHE.get(valueParamId);
        MdmParam unitParam = MDM_PARAM_CACHE.get(unitParamId);
        MdmParam commentParam = MDM_PARAM_CACHE.get(commentParamId);
        ValueCommentBlock valueCommentBlock = new ValueCommentBlock(blockType, valueParam, unitParam, commentParam);
        valueCommentBlock.fromSskuMasterData(value, comment, updatedTs);
        return valueCommentBlock;
    }

    public static ValueCommentBlock createShelfLifeBlock(TimeInUnits value, String comment, Instant updatedTs) {
        return createValueCommentBlock(
            ItemBlock.BlockType.SHELF_LIFE,
            KnownMdmParams.SHELF_LIFE, KnownMdmParams.SHELF_LIFE_UNIT, KnownMdmParams.SHELF_LIFE_COMMENT,
            value, comment,
            updatedTs
        );
    }

    public static ValueCommentBlock createLifeTimeBlock(TimeInUnits value, String comment, Instant updatedTs) {
        return createValueCommentBlock(
            ItemBlock.BlockType.LIFE_TIME,
            KnownMdmParams.LIFE_TIME, KnownMdmParams.LIFE_TIME_UNIT, KnownMdmParams.LIFE_TIME_COMMENT,
            value, comment,
            updatedTs
        );
    }

    public static ValueCommentBlock createGuaranteePeriodBlock(TimeInUnits value, String comment, Instant updatedTs) {
        return createValueCommentBlock(
            ItemBlock.BlockType.GUARANTEE_PERIOD,
            KnownMdmParams.GUARANTEE_PERIOD,
            KnownMdmParams.GUARANTEE_PERIOD_UNIT,
            KnownMdmParams.GUARANTEE_PERIOD_COMMENT,
            value, comment,
            updatedTs
        );
    }

    public static CustomsCommCodeBlock createCustomsCommodityBlock(String customsCommodityCode,
                                                                   Instant updatedTs,
                                                                   Integer supplierId) {
        MdmParam mdmParam = MDM_PARAM_CACHE.get(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID);
        var result = new CustomsCommCodeBlock(mdmParam);
        result.fromSskuMasterData(customsCommodityCode, updatedTs);
        result.setSupplierId(supplierId);
        return result;
    }

    public static MdmParamValueBlock<BigDecimal> createPriceBlock(BigDecimal price) {
        return createNumericMdmParamValueBlock(
            ItemBlock.BlockType.PRICE, KnownMdmParams.PRICE, price, MasterDataSource.DEFAULT_AUTO_SOURCE,
            DateTimeUtils.instantNow()
        );
    }

    public static MdmParamValueBlock<Boolean> createPreciousGoodBlock(boolean value,
                                                                      MasterDataSource masterDataSource,
                                                                      Instant updatedTs) {
        return createBoolParamValueBlock(
            ItemBlock.BlockType.MDM_PARAM, KnownMdmParams.PRECIOUS_GOOD, value, masterDataSource, updatedTs, null
        );
    }
}
