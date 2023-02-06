package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.DimensionsBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ExpirDateBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MdmParamValueBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamMetaType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistantImpl;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author dmserebr
 * @date 09/06/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MskuSilverItemPreProcessorHeavyGoodTest extends MdmBaseDbTestClass {
    private static final Long CATEGORY_ID = 10L;
    private static final Instant TIMESTAMP = TimestampUtil.toInstant(12345L);
    private static final GoldComputationContext CONTEXT_WITH_HEAVY_GOOD_SETTING =
        new GoldComputationContext(CATEGORY_ID, List.of(), Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true),
            Map.of(), Map.of(), Map.of(), Map.of(), Set.of());
    private static final GoldComputationContext CONTEXT_WITH_HEAVY_GOOD_SETTING_AND_OVERRIDE =
        new GoldComputationContext(CATEGORY_ID, List.of(),
            Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true),
            Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true), Map.of(), Map.of(), Map.of(), Set.of());

    private MskuSilverItemPreProcessor preProcessor;

    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MdmLmsCargoTypeCache mdmLmsCargoTypeCache;

    @Before
    public void before() {
        FeatureSwitchingAssistant featureSwitchingAssistant =
            new FeatureSwitchingAssistantImpl(new StorageKeyValueServiceMock());
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_HEAVY_GOOD_IN_MSKU_GOLDEN_SERVICE_KEY);
        preProcessor = new MskuSilverItemPreProcessor(mdmParamCache, mdmLmsCargoTypeCache, featureSwitchingAssistant);
    }

    @Test
    public void testPreprocessEmptyMap() {
        Assertions.assertThat(preProcessor.preProcess(Map.of(), TIMESTAMP, GoldComputationContext.EMPTY_CONTEXT))
            .isEmpty();
    }

    @Test
    public void testPreprocessNoWeightDimensionsAndCategorySettings() {
        List<ItemBlock> expirDateBlocks = List.of(
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()),
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()));
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksByType = new HashMap<>();
        blocksByType.put(ItemBlock.BlockType.EXPIR_DATE, expirDateBlocks);

        Map<ItemBlock.BlockType, List<ItemBlock>> result =
            preProcessor.preProcess(blocksByType, TIMESTAMP, GoldComputationContext.EMPTY_CONTEXT);

        Assertions.assertThat(result.keySet()).containsOnly(ItemBlock.BlockType.EXPIR_DATE);
        Assertions.assertThat(result.get(ItemBlock.BlockType.EXPIR_DATE))
            .isEqualTo(expirDateBlocks);
    }

    @Test
    public void testPreprocessNoWeightDimensionsButCategorySettingsProvided() {
        List<ItemBlock> expirDateBlocks = List.of(
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()),
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()));

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksByType = new HashMap<>();
        blocksByType.put(ItemBlock.BlockType.EXPIR_DATE, expirDateBlocks);

        Map<ItemBlock.BlockType, List<ItemBlock>> result =
            preProcessor.preProcess(blocksByType, TIMESTAMP, CONTEXT_WITH_HEAVY_GOOD_SETTING);

        Assertions.assertThat(result.keySet()).containsOnlyOnce(
            ItemBlock.BlockType.EXPIR_DATE,
            ItemBlock.BlockType.HEAVY_GOOD);
        Assertions.assertThat(result.get(ItemBlock.BlockType.EXPIR_DATE))
            .isEqualTo(expirDateBlocks);
        MdmParam heavyGoodParam = mdmParamCache.get(KnownMdmParams.HEAVY_GOOD);
        Assertions.assertThat(result.get(ItemBlock.BlockType.HEAVY_GOOD))
            .isEqualTo(List.of(new MdmParamValueBlock<Boolean>(ItemBlock.BlockType.HEAVY_GOOD, heavyGoodParam)
                .fromSskuMasterData(true, TIMESTAMP)));
    }

    @Test
    public void testPreprocessWithWeightDimensionsAndCategorySettingsProvided() {
        List<ItemBlock> expirDateBlocks = List.of(
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()),
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()));
        ItemBlock dimensionsBlock1 = new DimensionsBlock(
            createParamValue(10), createParamValue(10), createParamValue(20), createParamValue(1)); // not heavy
        ItemBlock dimensionsBlock2 = new DimensionsBlock(
            createParamValue(10), createParamValue(10), createParamValue(40), createParamValue(40)); // heavy
        ItemBlock dimensionsBlock3 = new DimensionsBlock(
            createParamValue(10), createParamValue(70), createParamValue(20), createParamValue(10)); // not heavy

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksByType = new HashMap<>();
        blocksByType.put(ItemBlock.BlockType.EXPIR_DATE, expirDateBlocks);
        blocksByType.put(ItemBlock.BlockType.DIMENSIONS, List.of(dimensionsBlock1, dimensionsBlock2, dimensionsBlock3));

        Map<ItemBlock.BlockType, List<ItemBlock>> result =
            preProcessor.preProcess(blocksByType, TIMESTAMP, CONTEXT_WITH_HEAVY_GOOD_SETTING);

        Assertions.assertThat(result.keySet()).containsOnlyOnce(
            ItemBlock.BlockType.EXPIR_DATE,
            ItemBlock.BlockType.DIMENSIONS,
            ItemBlock.BlockType.HEAVY_GOOD);
        MdmParam heavyGoodParam = mdmParamCache.get(KnownMdmParams.HEAVY_GOOD);
        Assertions.assertThat(result.get(ItemBlock.BlockType.HEAVY_GOOD))
            .isEqualTo(List.of(new MdmParamValueBlock<Boolean>(ItemBlock.BlockType.HEAVY_GOOD, heavyGoodParam)
                .fromSskuMasterData(false, TIMESTAMP)));
    }

    @Test
    public void testIfNoQuorumThenTakeCategorySettings() {
        List<ItemBlock> expirDateBlocks = List.of(
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()),
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()));
        ItemBlock dimensionsBlock1 = new DimensionsBlock(
            createParamValue(10), createParamValue(10), createParamValue(20), createParamValue(1)); // not heavy
        ItemBlock dimensionsBlock2 = new DimensionsBlock(
            createParamValue(10), createParamValue(10), createParamValue(40), createParamValue(40)); // heavy

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksByType = new HashMap<>();
        blocksByType.put(ItemBlock.BlockType.EXPIR_DATE, expirDateBlocks);
        blocksByType.put(ItemBlock.BlockType.DIMENSIONS, List.of(dimensionsBlock1, dimensionsBlock2));

        Map<ItemBlock.BlockType, List<ItemBlock>> result =
            preProcessor.preProcess(blocksByType, TIMESTAMP, CONTEXT_WITH_HEAVY_GOOD_SETTING);

        Assertions.assertThat(result.keySet()).containsOnlyOnce(
            ItemBlock.BlockType.EXPIR_DATE,
            ItemBlock.BlockType.DIMENSIONS,
            ItemBlock.BlockType.HEAVY_GOOD);
        MdmParam heavyGoodParam = mdmParamCache.get(KnownMdmParams.HEAVY_GOOD);
        Assertions.assertThat(result.get(ItemBlock.BlockType.HEAVY_GOOD))
            .isEqualTo(List.of(new MdmParamValueBlock<Boolean>(ItemBlock.BlockType.HEAVY_GOOD, heavyGoodParam)
                .fromSskuMasterData(true, TIMESTAMP)));
    }

    @Test
    public void testTakeCategoryOverrideIfExists() {
        List<ItemBlock> expirDateBlocks = List.of(
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()),
            generateExpirApplyBlock().fromSskuMasterData(false, Instant.now()));
        ItemBlock dimensionsBlock1 = new DimensionsBlock(
            createParamValue(10), createParamValue(10), createParamValue(20), createParamValue(1)); // not heavy

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksByType = new HashMap<>();
        blocksByType.put(ItemBlock.BlockType.EXPIR_DATE, expirDateBlocks);
        blocksByType.put(ItemBlock.BlockType.DIMENSIONS, List.of(dimensionsBlock1));

        Map<ItemBlock.BlockType, List<ItemBlock>> result
            = preProcessor.preProcess(blocksByType, TIMESTAMP, CONTEXT_WITH_HEAVY_GOOD_SETTING_AND_OVERRIDE);

        Assertions.assertThat(result.keySet()).containsOnlyOnce(
            ItemBlock.BlockType.EXPIR_DATE,
            ItemBlock.BlockType.DIMENSIONS,
            ItemBlock.BlockType.HEAVY_GOOD);
        MdmParam heavyGoodParam = mdmParamCache.get(KnownMdmParams.HEAVY_GOOD);
        Assertions.assertThat(result.get(ItemBlock.BlockType.HEAVY_GOOD))
            .isEqualTo(List.of(new MdmParamValueBlock<Boolean>(ItemBlock.BlockType.HEAVY_GOOD, heavyGoodParam)
                .fromSskuMasterData(true, TIMESTAMP)));
    }

    private ExpirDateBlock generateExpirApplyBlock() {
        MdmParam param = new MdmParam()
            .setId(KnownMdmParams.EXPIR_DATE)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setValueType(MdmParamValueType.MBO_BOOL);
        return new ExpirDateBlock(param);
    }

    private MdmParamValue createParamValue(int value) {
        var pv = new MdmParamValue();
        pv.setNumeric(new BigDecimal(value));
        pv.setUpdatedTs(TIMESTAMP);
        return pv;
    }
}
