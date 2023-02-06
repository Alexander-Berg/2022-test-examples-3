package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MdmParamValueBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ShelfLifeBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistantImpl;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class MskuSilverItemPreProcessorExpirDateTest {
    private static final Long CATEGORY_ID = 10L;
    private static final Instant TIMESTAMP = TimestampUtil.toInstant(12345L);

    private MskuSilverItemPreProcessor preProcessor;
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    private MdmParamCache mdmParamCache;

    @Before
    public void before() {
        mdmParamCache = TestMdmParamUtils.createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());
        MdmLmsCargoTypeCacheMock ctCache = TestMdmParamUtils.createCargoTypeCacheMock(new ArrayList<>());
        featureSwitchingAssistant = new FeatureSwitchingAssistantImpl(new StorageKeyValueServiceMock());
        featureSwitchingAssistant.enableFeature(MdmProperties.WRITE_MSKU_EXPIR_DATE_COMPUTED_FROM_SSKUS_SHELF_LIVES);
        preProcessor =
            new MskuSilverItemPreProcessor(mdmParamCache, ctCache, featureSwitchingAssistant);
    }

    @Test
    public void whenHaveOnlyCategoryExpireDateRequiredBlockShouldCreateTrueBlock() {
        testOnlyCategory(
            KnownMdmParams.EXPIRATION_DATES_REQUIRED_OPTION,
            true
        );
    }

    @Test
    public void whenHaveOnlyCategoryExpireDateMayUseBlockShouldCreateFalseBlock() {
        testOnlyCategory(
            KnownMdmParams.EXPIRATION_DATES_MAY_USE_OPTION,
            false
        );
    }

    @Test
    public void whenHaveOnlyCategoryExpireDateNotAllowedBlockShouldCreateFalseBlock() {
        testOnlyCategory(
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            false
        );
    }

    @Test
    public void whenHaveEnoughShelfBlocksCreateTrueBlock() {
        List<Pair<TimeInUnits, Integer>> shelfLives = new ArrayList<>();
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION; i++) {
            shelfLives.add(Pair.of(new TimeInUnits(12), 10 + i));
        }
        testWithShelfLives(
            shelfLives,
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            true,
            MasterDataSourceType.EXPIR_DATE_FROM_SHELF_LIVES_SOURCE_ID
        );
    }

    @Test
    public void whenHaveNotEnoughShelfBlocksWithSameTimeCreateBlockFromCategory() {
        List<Pair<TimeInUnits, Integer>> shelfLives1 = new ArrayList<>();
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION; i++) {
            shelfLives1.add(Pair.of(new TimeInUnits(i, TimeInUnits.TimeUnit.YEAR), 10 + i));
        }
        testWithShelfLives(
            shelfLives1,
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            false,
            MasterDataSourceType.EXPIR_DATE_FROM_CATEGORY_SETTINGS_SOURCE_ID
        );

        List<Pair<TimeInUnits, Integer>> shelfLives2 = new ArrayList<>();
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION - 1; i++) {
            shelfLives2.add(Pair.of(new TimeInUnits(5, TimeInUnits.TimeUnit.YEAR), 10 + i));
        }
        testWithShelfLives(
            shelfLives2,
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            false,
            MasterDataSourceType.EXPIR_DATE_FROM_CATEGORY_SETTINGS_SOURCE_ID
        );
    }

    @Test
    public void whenHaveNotEnoughShelfBlocksWithUniqSuppliersCreateBlockFromCategory() {
        List<Pair<TimeInUnits, Integer>> shelfLives = new ArrayList<>();
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION; i++) {
            shelfLives.add(Pair.of(new TimeInUnits(911), 112));
        }
        testWithShelfLives(
            shelfLives,
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            false,
            MasterDataSourceType.EXPIR_DATE_FROM_CATEGORY_SETTINGS_SOURCE_ID
        );
    }

    @Test
    public void whenHaveEnoughShelfBlocksWithDifferentButSimilarTimesCreateTrueBlock() {
        List<Pair<TimeInUnits, Integer>> shelfLives1 = new ArrayList<>();
        shelfLives1.add(Pair.of(new TimeInUnits(360, TimeInUnits.TimeUnit.DAY), 1));
        shelfLives1.add(Pair.of(new TimeInUnits(365, TimeInUnits.TimeUnit.DAY), 2));
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION - 2; i++) {
            shelfLives1.add(Pair.of(new TimeInUnits(1, TimeInUnits.TimeUnit.YEAR), 3 + i));
        }
        testWithShelfLives(
            shelfLives1,
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            true,
            MasterDataSourceType.EXPIR_DATE_FROM_SHELF_LIVES_SOURCE_ID
        );

        List<Pair<TimeInUnits, Integer>> shelfLives2 = new ArrayList<>();
        shelfLives2.add(Pair.of(new TimeInUnits(24, TimeInUnits.TimeUnit.MONTH), 1));
        shelfLives2.add(Pair.of(new TimeInUnits(720, TimeInUnits.TimeUnit.DAY), 2));
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION - 2; i++) {
            shelfLives2.add(Pair.of(new TimeInUnits(2, TimeInUnits.TimeUnit.YEAR), 3 + i));
        }
        testWithShelfLives(
            shelfLives2,
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            true,
            MasterDataSourceType.EXPIR_DATE_FROM_SHELF_LIVES_SOURCE_ID
        );
    }

    @Test
    public void whenCreationFromShelfLivesDisabledGlobalCreateBlockFromCategory() {
        List<Pair<TimeInUnits, Integer>> shelfLives = new ArrayList<>();
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION; i++) {
            shelfLives.add(Pair.of(new TimeInUnits(12), 10 + i));
        }
        testWithShelfLives(
            shelfLives,
            false,
            List.of(),
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            false,
            MasterDataSourceType.EXPIR_DATE_FROM_CATEGORY_SETTINGS_SOURCE_ID
        );
    }

    @Test
    public void whenCreationFromShelfLivesEnabledInCategoryDoIt() {
        List<Pair<TimeInUnits, Integer>> shelfLives = new ArrayList<>();
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION; i++) {
            shelfLives.add(Pair.of(new TimeInUnits(12), 10 + i));
        }
        testWithShelfLives(
            shelfLives,
            false,
            List.of(CATEGORY_ID, Long.MAX_VALUE, 4567L, 89853L),
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            true,
            MasterDataSourceType.EXPIR_DATE_FROM_SHELF_LIVES_SOURCE_ID
        );
    }

    private void testOnlyCategory(MdmParamOption paramOption, boolean expectedValue) {
        testOnlyCategory(paramOption, expectedValue, List.of());
        testOnlyCategory(paramOption, expectedValue, List.of(createExpirDateBlock(true, "any_source")));
    }

    private void testOnlyCategory(MdmParamOption option, boolean expectedValue,
                                  List<ItemBlock> existingExpirDateBlocks) {
        proceedPreProcessing(
            List.of(),
            false,
            List.of(),
            option,
            expectedValue,
            MasterDataSourceType.EXPIR_DATE_FROM_CATEGORY_SETTINGS_SOURCE_ID,
            existingExpirDateBlocks
        );
    }

    private void testWithShelfLives(
        List<Pair<TimeInUnits, Integer>> shelfLives,
        MdmParamOption categoryOption,
        boolean expectedValue,
        String expectedSourceId
    ) {
        testWithShelfLives(
            shelfLives,
            true,
            List.of(),
            categoryOption,
            expectedValue,
            expectedSourceId
        );
    }

    private void testWithShelfLives(
        List<Pair<TimeInUnits, Integer>> shelfLives,
        boolean fromShelfLivesGlobal,
        List<Long> fromShelfLivesCategories,
        MdmParamOption categoryOption,
        boolean expectedValue,
        String expectedSourceId
    ) {
        proceedPreProcessing(
            shelfLives,
            fromShelfLivesGlobal,
            fromShelfLivesCategories,
            categoryOption,
            expectedValue,
            expectedSourceId,
            List.of()
        );
        proceedPreProcessing(
            shelfLives,
            fromShelfLivesGlobal,
            fromShelfLivesCategories,
            categoryOption,
            expectedValue,
            expectedSourceId,
            List.of(createExpirDateBlock(true, "any_source"))
        );
    }

    private void proceedPreProcessing(
        List<Pair<TimeInUnits, Integer>> shelfLives,
        boolean fromShelfLivesGlobal,
        List<Long> fromShelfLivesCategories,
        MdmParamOption categoryOption,
        boolean expectedValue,
        String expectedSourceId,
        List<ItemBlock> existingExpirDateBlocks
    ) {
        featureSwitchingAssistant.switchFeature(MdmProperties.COMPUTE_MSKU_EXPIR_DATE_FROM_SSKUS_SHELF_LIVES_GLOBAL,
            fromShelfLivesGlobal);
        featureSwitchingAssistant.updateFeatureEnabledCategories(
            MdmProperties.COMPUTE_MSKU_EXPIR_DATE_FROM_SSKUS_SHELF_LIVES_CATEGORIES, fromShelfLivesCategories);
        Map<ItemBlock.BlockType, List<ItemBlock>> silverBlocksByType = new HashMap<>();

        GoldComputationContext context = createContext(categoryOption);

        silverBlocksByType.put(ItemBlock.BlockType.EXPIR_DATE, new ArrayList<>(existingExpirDateBlocks));

        List<ItemBlock> shelfLifeBlocks = shelfLives.stream()
            .map(pair -> createShelfLifeBlock(pair.first, pair.second))
            .collect(Collectors.toList());
        silverBlocksByType.put(ItemBlock.BlockType.SHELF_LIFE, shelfLifeBlocks);

        ItemBlock expectedBlock =
            createExpirDateBlock(expectedValue, expectedSourceId);
        List<ItemBlock> expectedBlocks = new ArrayList<>(existingExpirDateBlocks);
        expectedBlocks.add(expectedBlock);

        preProcessor.preProcess(silverBlocksByType, TIMESTAMP, context);

        Assertions.assertThat(silverBlocksByType.get(ItemBlock.BlockType.EXPIR_DATE))
            .containsExactlyElementsOf(expectedBlocks);
    }

    private GoldComputationContext createContext(MdmParamOption option) {
        CategoryParamValue categoryParamValue = new CategoryParamValue();
        categoryParamValue.setCategoryId(CATEGORY_ID);
        categoryParamValue.setOption(option);
        return new GoldComputationContext(
            CATEGORY_ID,
            Map.of(KnownMdmParams.EXPIRATION_DATES_APPLY, categoryParamValue)
        );
    }

    private ItemBlock createExpirDateBlock(boolean value, String sourceId) {
        MdmParamValueBlock<Boolean> expirDateBlock =
            new MdmParamValueBlock<Boolean>(ItemBlock.BlockType.EXPIR_DATE, mdmParamCache
                .get(KnownMdmParams.EXPIR_DATE))
                .fromSskuMasterData(value, TIMESTAMP);
        expirDateBlock.getMdmParamValue().ifPresent(mdmParamValue -> mdmParamValue.setMasterDataSourceId(sourceId));
        return expirDateBlock;
    }

    private ItemBlock createShelfLifeBlock(TimeInUnits time, int supplierId) {
        MdmParam valueParam = mdmParamCache.get(KnownMdmParams.SHELF_LIFE);
        MdmParam unitParam = mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT);
        MdmParam commentParam = mdmParamCache.get(KnownMdmParams.SHELF_LIFE_COMMENT);
        return new ShelfLifeBlock(valueParam, unitParam, commentParam, new ShopSkuKey(supplierId, "shopSku"))
            .fromSskuMasterData(time, "comment", TIMESTAMP);
    }
}
