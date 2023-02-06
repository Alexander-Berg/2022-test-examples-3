package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.DimensionsBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.TestBlockCreationUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MdmParamValueBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ShelfLifeBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistantImpl;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class MskuGoldenBlocksPostProcessorTest {
    private static final long MSKU_ID = 27182818284L;
    private static final long CATEGORY_ID = 314151926535L;
    private static final ModelKey MODEL_KEY = new ModelKey(CATEGORY_ID, MSKU_ID);
    private static final GoldComputationContext CONTEXT_WITH_HEAVY_GOOD_TO_TRUE_OVERRIDES = new GoldComputationContext(
        0L, List.of(),
        // настройки false
        Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, false,
            KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID, false),
        // оверрайды в true
        Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true,
            KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID, true),
        Map.of(), Map.of(), Map.of(), Set.of());
    private static final GoldComputationContext CONTEXT_WITH_HEAVY_GOOD_FALSE_SETTINGS = new GoldComputationContext(
        0L, List.of(),
        Map.of(
            KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, false,
            KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID, false
        ), Map.of(), Map.of(), Map.of(), Map.of(), Set.of());

    MskuGoldenBlocksPostProcessor postProcessor;
    FeatureSwitchingAssistant featureSwitchingAssistant;
    MdmParamCache mdmParamCache;
    StorageKeyValueService keyValueService;

    @Before
    public void setUp() {
        keyValueService = new StorageKeyValueServiceMock();
        mdmParamCache = TestMdmParamUtils.createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());
        featureSwitchingAssistant = new FeatureSwitchingAssistantImpl(keyValueService);
        CustomsCommCodeMarkupService markupService = Mockito.mock(CustomsCommCodeMarkupServiceImpl.class);
        Mockito.when(markupService.generateMskuParamValues(Mockito.anyLong(), Mockito.anyString()))
            .thenReturn(List.of());
        postProcessor =
            new MskuGoldenBlocksPostProcessor(featureSwitchingAssistant, mdmParamCache, markupService, keyValueService);
    }

    @Test
    public void whenShelfLifeUnlimitedSetExpirDateFalse() {
        featureSwitchingAssistant.enableFeature(MdmProperties.SET_EXPIR_DATE_FALSE_FOR_UNLIMITED);

        ValueCommentBlock shelfLifeBlock = createShelfLifeBlock(TimeInUnits.UNLIMITED);
        MdmParamValueBlock<Boolean> expirDateBefore =
            createExpirDateBlock(true, MasterDataSourceType.MDM_OPERATOR, "some_source_id", "some_guy");
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore = createBlocksMap(shelfLifeBlock, expirDateBefore);


        MdmParamValueBlock<Boolean> expirDateExpected = createExpirDateBlock(false, MasterDataSourceType.AUTO,
            MasterDataSourceType.EXPIR_DATE_BY_UNLIMITED_UNIT_SOURCE_ID, null);
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksExpected = createBlocksMap(shelfLifeBlock, expirDateExpected);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);
        assertEquals(blocksAfter, blocksExpected);
    }

    @Test
    public void whenNoShelfLifeDoNothing() {
        featureSwitchingAssistant.enableFeature(MdmProperties.SET_EXPIR_DATE_FALSE_FOR_UNLIMITED);

        MdmParamValueBlock<Boolean> expirDateBefore =
            createExpirDateBlock(true, MasterDataSourceType.MDM_OPERATOR, "some_source_id", "some_guy");
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore = createBlocksMap(expirDateBefore);
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);

        assertEquals(blocksAfter, blocksBefore);
    }

    @Test
    public void whenHaveManualOldHeavyGoodKeepIt() {

        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_HEAVY_GOOD_FROM_MSKU_DIMENSIONS_GLOBAL);
        MdmParamValueBlock<Boolean> heavyGoodBefore =
            createHeavyGoodBlock(false, MasterDataSourceType.MDM_OPERATOR, "some_source_id", "some_guy");
        MdmParamValueBlock<Boolean> heavyGood20Before =
            createHeavyGood20Block(false, MasterDataSourceType.MDM_OPERATOR, "some_source_id", "some_guy");
        DimensionsBlock dimensionsBlock = createDimensionsBlock(
            new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("31"));
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(heavyGoodBefore, heavyGood20Before, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter = postProcessor.postProcess(
            new HashMap<>(blocksBefore), MODEL_KEY, CONTEXT_WITH_HEAVY_GOOD_TO_TRUE_OVERRIDES
        );

        assertEquals(blocksAfter, blocksBefore);
    }

    @Test
    public void whenHavedHeavyGoodByMboOperatorKeepIt() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_HEAVY_GOOD_FROM_MSKU_DIMENSIONS_GLOBAL);
        MdmParamValueBlock<Boolean> heavyGoodBefore =
            createHeavyGoodBlock(true, MasterDataSourceType.MBO_OPERATOR, "some_source_id", "some_guy");
        MdmParamValueBlock<Boolean> heavyGood20Before =
            createHeavyGood20Block(true, MasterDataSourceType.MBO_OPERATOR, "some_source_id", "some_guy");
        DimensionsBlock dimensionsBlock = createDimensionsBlock(
            new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("15"));
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(heavyGoodBefore, heavyGood20Before, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter = postProcessor.postProcess(
            new HashMap<>(blocksBefore), MODEL_KEY, CONTEXT_WITH_HEAVY_GOOD_FALSE_SETTINGS
        );

        assertEquals(blocksAfter, blocksBefore);
    }

    @Test
    public void whenNotHaveManualOldHeavyGoodUseCategoryOverrides() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_HEAVY_GOOD_FROM_MSKU_DIMENSIONS_GLOBAL);

        MdmParamValueBlock<Boolean> heavyGoodBefore =
            createHeavyGoodBlock(false, MasterDataSourceType.AUTO, "some_source_id", "some_guy");
        MdmParamValueBlock<Boolean> heavyGood20Before =
            createHeavyGood20Block(false, MasterDataSourceType.AUTO, "some_source_id", "some_guy");
        DimensionsBlock dimensionsBlock = createDimensionsBlock(
            new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("31"));
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(heavyGoodBefore, heavyGood20Before, dimensionsBlock);

        MdmParamValueBlock<Boolean> heavyGoodExpected = createHeavyGoodBlock(true, MasterDataSourceType.AUTO,
            MasterDataSourceType.HEAVY_GOOD_BY_CATEGORY_OVERRIDE, null);
        MdmParamValueBlock<Boolean> heavyGood20Expected = createHeavyGood20Block(true, MasterDataSourceType.AUTO,
            MasterDataSourceType.HEAVY_GOOD_BY_CATEGORY_OVERRIDE, null);
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksExpected =
            createBlocksMap(heavyGoodExpected, heavyGood20Expected, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter = postProcessor.postProcess(
            new HashMap<>(blocksBefore), MODEL_KEY, CONTEXT_WITH_HEAVY_GOOD_TO_TRUE_OVERRIDES
        );

        assertEquals(blocksAfter, blocksExpected);
    }

    @Test
    public void whenNotHaveManualOldAndCategoryOverridesComputeFromDimensions() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_HEAVY_GOOD_FROM_MSKU_DIMENSIONS_GLOBAL);

        MdmParamValueBlock<Boolean> heavyGoodBefore =
            createHeavyGoodBlock(true, MasterDataSourceType.AUTO, "some_source_id", "some_guy");
        MdmParamValueBlock<Boolean> heavyGood20Before =
            createHeavyGood20Block(false, MasterDataSourceType.AUTO, "some_source_id", "some_guy");
        DimensionsBlock dimensionsBlock = createDimensionsBlock(
            new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("29.9999999999999"));
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(heavyGoodBefore, heavyGood20Before, dimensionsBlock);

        MdmParamValueBlock<Boolean> heavyGoodExpected = createHeavyGoodBlock(false, MasterDataSourceType.AUTO,
            MasterDataSourceType.HEAVY_GOOD_BY_MSKU_DIMENSIONS, null);
        MdmParamValueBlock<Boolean> heavyGood20Expected = createHeavyGood20Block(true, MasterDataSourceType.AUTO,
            MasterDataSourceType.HEAVY_GOOD_BY_MSKU_DIMENSIONS, null);
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksExpected =
            createBlocksMap(heavyGoodExpected, heavyGood20Expected, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY, CONTEXT_WITH_HEAVY_GOOD_FALSE_SETTINGS);

        assertEquals(blocksAfter, blocksExpected);
    }

    @Test
    public void whenNotHaveManualOldAndCategoryOverridesAndCanNotComputeFromDimensionsUseCategorySettings() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_HEAVY_GOOD_FROM_MSKU_DIMENSIONS_GLOBAL);

        MdmParamValueBlock<Boolean> heavyGoodBefore =
            createHeavyGoodBlock(true, MasterDataSourceType.AUTO, "some_source_id", "some_guy");
        MdmParamValueBlock<Boolean> heavyGood20Before =
            createHeavyGood20Block(true, MasterDataSourceType.AUTO, "some_source_id", "some_guy");
        DimensionsBlock dimensionsBlock = createDimensionsBlock(
            new BigDecimal("60"), new BigDecimal("60"), new BigDecimal("60"), null);
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(heavyGoodBefore, heavyGood20Before, dimensionsBlock);

        MdmParamValueBlock<Boolean> heavyGoodExpected = createHeavyGoodBlock(false, MasterDataSourceType.AUTO,
            MasterDataSourceType.HEAVY_GOOD_BY_CATEGORY_SETTING, null);
        MdmParamValueBlock<Boolean> heavyGood20Expected = createHeavyGood20Block(false, MasterDataSourceType.AUTO,
            MasterDataSourceType.HEAVY_GOOD_BY_CATEGORY_SETTING, null);
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksExpected =
            createBlocksMap(heavyGoodExpected, heavyGood20Expected, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY, CONTEXT_WITH_HEAVY_GOOD_FALSE_SETTINGS);

        assertEquals(blocksAfter, blocksExpected);
    }

    @Test
    public void whenNoHeavyGoodFromTrustedSourcesRemoveHeavyGoodParam() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_HEAVY_GOOD_FROM_MSKU_DIMENSIONS_GLOBAL);

        MdmParamValueBlock<Boolean> heavyGoodBefore =
            createHeavyGoodBlock(true, MasterDataSourceType.AUTO, "some_source_id", "some_guy");
        MdmParamValueBlock<Boolean> heavyGood20Before =
            createHeavyGood20Block(true, MasterDataSourceType.AUTO, "some_source_id", "some_guy");
        DimensionsBlock dimensionsBlock = createDimensionsBlock(new BigDecimal("20"), null, null, null);
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(heavyGoodBefore, heavyGood20Before, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksExpected = createBlocksMap(dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);

        assertEquals(blocksAfter, blocksExpected);
    }

    @Test
    public void whenComputedPreciousGoodIsTrueCreateAppropriateBlock() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);

        MdmParamValueBlock<BigDecimal> priceBlock = TestBlockCreationUtil.createPriceBlock(new BigDecimal("20000"));
        DimensionsBlock dimensionsBlock =
            createDimensionsBlock(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ONE);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(priceBlock, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksExpected = createBlocksMap(
            dimensionsBlock, priceBlock,
            TestBlockCreationUtil.createPreciousGoodBlock(true, MasterDataSource.DEFAULT_AUTO_SOURCE, Instant.now())
        );

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);

        assertEquals(blocksAfter, blocksExpected);
    }

    @Test
    public void whenComputedPreciousGoodIsFalseCreateBlock() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);

        MdmParamValueBlock<BigDecimal> priceBlock = TestBlockCreationUtil.createPriceBlock(new BigDecimal("2"));
        DimensionsBlock dimensionsBlock =
            createDimensionsBlock(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ONE);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(priceBlock, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksExpected = createBlocksMap(
            dimensionsBlock, priceBlock,
            TestBlockCreationUtil.createPreciousGoodBlock(false, MasterDataSource.DEFAULT_AUTO_SOURCE, Instant.now())
        );

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);

        assertEquals(blocksAfter, blocksExpected);
    }

    @Test
    public void whenPreciousGoodCoefficientSetUseItInComputation() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);

        MdmParamValueBlock<BigDecimal> priceBlock = TestBlockCreationUtil.createPriceBlock(new BigDecimal("20000"));
        DimensionsBlock dimensionsBlock =
            createDimensionsBlock(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ONE);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(priceBlock, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksExpected1 = createBlocksMap(
            dimensionsBlock, priceBlock,
            TestBlockCreationUtil.createPreciousGoodBlock(true, MasterDataSource.DEFAULT_AUTO_SOURCE, Instant.now())
        );

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksExpected2 = createBlocksMap(
            dimensionsBlock, priceBlock,
            TestBlockCreationUtil.createPreciousGoodBlock(false, MasterDataSource.DEFAULT_AUTO_SOURCE, Instant.now())
        );

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter1 =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);

        keyValueService.putValue(MdmProperties.PRECIOUS_GOOD_COEFFICIENT, "21");
        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter2 =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);

        assertEquals(blocksAfter1, blocksExpected1);
        assertEquals(blocksAfter2, blocksExpected2);
    }

    @Test
    public void whenNoVolumeShouldNotComputePreciousGood() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);

        MdmParamValueBlock<BigDecimal> priceBlock = TestBlockCreationUtil.createPriceBlock(new BigDecimal("20000"));

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(priceBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);

        assertEquals(blocksAfter, blocksBefore);
    }

    @Test
    public void whenNoPriceShouldNotComputePreciousGood() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);

        DimensionsBlock dimensionsBlock =
            createDimensionsBlock(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ONE);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);

        assertEquals(blocksAfter, blocksBefore);
    }

    @Test
    public void whenFeatureDisabledShouldNotComputePreciousGood() {
        MdmParamValueBlock<BigDecimal> priceBlock = TestBlockCreationUtil.createPriceBlock(new BigDecimal("20000"));
        DimensionsBlock dimensionsBlock =
            createDimensionsBlock(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ONE);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksBefore =
            createBlocksMap(priceBlock, dimensionsBlock);

        Map<ItemBlock.BlockType, List<ItemBlock>> blocksAfter =
            postProcessor.postProcess(new HashMap<>(blocksBefore), MODEL_KEY);

        assertEquals(blocksAfter, blocksBefore);
    }

    private DimensionsBlock createDimensionsBlock(BigDecimal length, BigDecimal width, BigDecimal height,
                                                  BigDecimal weight) {
        return new DimensionsBlock(
            length != null ? new MdmParamValue().setMdmParamId(KnownMdmParams.LENGTH).setNumeric(length) : null,
            width != null ? new MdmParamValue().setMdmParamId(KnownMdmParams.WIDTH).setNumeric(width) : null,
            height != null ? new MdmParamValue().setMdmParamId(KnownMdmParams.HEIGHT).setNumeric(height) : null,
            weight != null ? new MdmParamValue().setMdmParamId(KnownMdmParams.WEIGHT_GROSS).setNumeric(weight) : null
        );
    }

    private ShelfLifeBlock createShelfLifeBlock(TimeInUnits timeInUnits) {
        MdmParam valueParam = mdmParamCache.get(KnownMdmParams.SHELF_LIFE);
        MdmParam unitParam = mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT);
        MdmParam commentParam = mdmParamCache.get(KnownMdmParams.SHELF_LIFE_COMMENT);
        ShelfLifeBlock shelfLifeBlock = new ShelfLifeBlock(valueParam, unitParam, commentParam);
        shelfLifeBlock.fromSskuMasterData(timeInUnits, "", Instant.EPOCH);
        return shelfLifeBlock;
    }

    private MdmParamValueBlock<Boolean> createHeavyGoodBlock(boolean boolValue, MasterDataSourceType sourceType,
                                                             String sourceId, String updatedByLogin) {
        return TestBlockCreationUtil.createBoolParamValueBlock(
            ItemBlock.BlockType.HEAVY_GOOD, KnownMdmParams.HEAVY_GOOD, boolValue, sourceType, sourceId, updatedByLogin
        );
    }

    private MdmParamValueBlock<Boolean> createHeavyGood20Block(boolean boolValue, MasterDataSourceType sourceType,
                                                               String sourceId, String updatedByLogin) {
        return TestBlockCreationUtil.createBoolParamValueBlock(
            ItemBlock.BlockType.HEAVY_GOOD_20, KnownMdmParams.HEAVY_GOOD_20, boolValue, sourceType, sourceId,
            updatedByLogin
        );
    }

    private MdmParamValueBlock<Boolean> createExpirDateBlock(boolean boolValue, MasterDataSourceType sourceType,
                                                             String sourceId, String updatedByLogin) {
        return TestBlockCreationUtil.createBoolParamValueBlock(
            ItemBlock.BlockType.EXPIR_DATE, KnownMdmParams.EXPIR_DATE, boolValue, sourceType, sourceId, updatedByLogin
        );
    }

    private Map<ItemBlock.BlockType, List<ItemBlock>> createBlocksMap(ItemBlock... itemBlocks) {
        return Arrays.stream(itemBlocks)
            .collect(Collectors.toMap(ItemBlock::getBlockType, List::of));
    }

    private void assertEquals(Map<ItemBlock.BlockType, List<ItemBlock>> actual,
                              Map<ItemBlock.BlockType, List<ItemBlock>> expected) {
        var actualCopy = new HashMap<>(actual);
        var expectedCopy = new HashMap<>(expected);
        removeEmpty(actualCopy);
        removeEmpty(expectedCopy);
        Assertions.assertThat(actualCopy).isEqualTo(expectedCopy);
    }

    private void removeEmpty(Map<ItemBlock.BlockType, List<ItemBlock>> blocksMap) {
        Set<ItemBlock.BlockType> blockTypes = new HashSet<>(blocksMap.keySet());
        for (ItemBlock.BlockType type : blockTypes) {
            if (blocksMap.get(type).isEmpty()) {
                blocksMap.remove(type);
            }
        }
    }
}
