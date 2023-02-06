package ru.yandex.market.mboc.common.masterdata.services.msku;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.DimensionsBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ExpirDateBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ShelfLifeBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamMetaType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class ItemBlockCalculationStrategiesTest {

    @Test
    public void whenUseMostCommonStrategyShouldReturnMostCommon() {
        ExpirDateBlock[] requiredShelfLife = generateExpirApplyBlocks(3, block ->
            block.fromSskuMasterData(true, Instant.now()));
        ExpirDateBlock[] notRequiredShelfLife = generateExpirApplyBlocks(2, block ->
            block.fromSskuMasterData(false, Instant.now())
        );
        Optional<ExpirDateBlock> result = ItemBlockCalculationStrategies.mostCommonValue(
            join(requiredShelfLife, notRequiredShelfLife),
            ExpirDateBlock::getRawValue,
            QuorumType.LATEST
        );

        assertThat(result).isPresent();
        assertThat(result.get().getRawValue()).isTrue();
    }

    @Test
    public void whenUseMostCommonStrategyAndCountIsEqualShouldReturnMostRecent() {
        Instant ts = Instant.now();
        ExpirDateBlock[] requiredShelfLife = generateExpirApplyBlocks(3, block ->
            block.fromSskuMasterData(true, ts));
        ExpirDateBlock[] notRequiredShelfLife = generateExpirApplyBlocks(3, block ->
            block.fromSskuMasterData(false, ts.minusSeconds(3600L))
        );
        Optional<ExpirDateBlock> result = ItemBlockCalculationStrategies.mostCommonValue(
            join(requiredShelfLife, notRequiredShelfLife),
            ExpirDateBlock::getRawValue,
            QuorumType.LATEST
        );

        assertThat(result).isPresent();
        assertThat(result.get().getRawValue()).isTrue();
    }

    @Test
    public void whenUseMostRecentShouldReturnMostRecentByAverage() {
        Instant ts = Instant.now();
        ExpirDateBlock[] mostRecent = generateExpirApplyBlocks(1, block ->
            block.fromSskuMasterData(false, ts));
        ExpirDateBlock[] mostRecentMinusMinute = generateExpirApplyBlocks(1, block ->
            block.fromSskuMasterData(true, ts.minusSeconds(60L)));
        ExpirDateBlock[] mostRecentMinusTwoMinutes = generateExpirApplyBlocks(2, block ->
            block.fromSskuMasterData(true, ts.minusSeconds(120L)));
        ExpirDateBlock[] mostRecentMinusTenMinutes = generateExpirApplyBlocks(3, block ->
            block.fromSskuMasterData(false, ts.minusSeconds(600L)));

        Optional<Boolean> result = ItemBlockCalculationStrategies.borderValueByAverage(
            join(mostRecent, mostRecentMinusMinute, mostRecentMinusTwoMinutes, mostRecentMinusTenMinutes),
            ExpirDateBlock::getRawValue,
            true
        );

        assertThat(result).hasValue(true);
    }

    @Test
    public void whenParameterValueIsNullShouldReturnOptionalEmpty() {
        Instant ts = Instant.now();
        ExpirDateBlock[] data = generateExpirApplyBlocks(1, block ->
            block.fromSskuMasterData(null, ts));

        Optional<ExpirDateBlock> result = ItemBlockCalculationStrategies.mostCommonValue(
            join(data),
            ExpirDateBlock::getRawValue,
            QuorumType.LATEST
        );

        assertThat(result).isEmpty();
    }

    @Test
    public void whenSomeParameterValuesAreNullShouldProcessNotNull() {
        Instant ts = Instant.now();
        ExpirDateBlock[] emptyData = generateExpirApplyBlocks(10, block ->
            block.fromSskuMasterData(null, ts));

        ExpirDateBlock[] filledData = generateExpirApplyBlocks(1, block ->
            block.fromSskuMasterData(true, ts.minusSeconds(60L)));

        Optional<ExpirDateBlock> result = ItemBlockCalculationStrategies.mostCommonValue(
            join(emptyData, filledData),
            ExpirDateBlock::getRawValue,
            QuorumType.LATEST
        );

        assertThat(result).isPresent();
        assertThat(result.get().getRawValue()).isTrue();
    }

    @Test
    public void shouldChooseBlockWithFilledComment() {
        Instant now = Instant.now();
        TimeInUnits day = new TimeInUnits(1, TimeInUnits.TimeUnit.DAY);
        TimeInUnits twoDays = new TimeInUnits(2, TimeInUnits.TimeUnit.DAY);

        ShelfLifeBlock[] blocksWithComment = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(day, "comment", now));

        ShelfLifeBlock[] blocksWithoutComment = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(twoDays, null, now.minusSeconds(60)));

        Optional<ShelfLifeBlock> result = ItemBlockCalculationStrategies.mostRichOfTwo(
            join(blocksWithComment, blocksWithoutComment),
            ShelfLifeBlock::getValueWithUnit,
            ShelfLifeBlock::getValueComment
        );

        Optional<ShelfLifeBlock> resultComment = ItemBlockCalculationStrategies.mostRichOfTwo(
            join(blocksWithComment, blocksWithoutComment),
            ShelfLifeBlock::getValueComment,
            ShelfLifeBlock::getValueWithUnit
        );

        assertThat(result).isPresent();
        assertThat(result.get().getValueWithUnit()).isEqualTo(day);
        assertThat(resultComment).isPresent();
        assertThat(resultComment.get().getValueComment()).isEqualTo("comment");
    }

    @Test
    public void shouldChooseOldestBlockWithFilledComment() {
        Instant now = Instant.now();
        TimeInUnits day = new TimeInUnits(1, TimeInUnits.TimeUnit.DAY);
        TimeInUnits twoDays = new TimeInUnits(2, TimeInUnits.TimeUnit.DAY);

        ShelfLifeBlock[] blocksWithComment = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(day, "comment1", now));

        ShelfLifeBlock[] oldBlocksWithComment = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(twoDays, "comment2", now.minusSeconds(60)));

        ShelfLifeBlock[] oldBlocksWithoutComment = generateShelfLifeBlocks(5, block ->
            block.fromSskuMasterData(day, null, now.minusSeconds(60)));

        Optional<ShelfLifeBlock> result = ItemBlockCalculationStrategies.mostRichOfTwo(
            join(blocksWithComment, oldBlocksWithComment, oldBlocksWithoutComment),
            ShelfLifeBlock::getValueWithUnit,
            ShelfLifeBlock::getValueComment
        );

        Optional<ShelfLifeBlock> resultComment = ItemBlockCalculationStrategies.mostRichOfTwo(
            join(blocksWithComment, oldBlocksWithComment, oldBlocksWithoutComment),
            ShelfLifeBlock::getValueComment,
            ShelfLifeBlock::getValueWithUnit
        );

        assertThat(result).isPresent();
        assertThat(result.get().getValueWithUnit()).isEqualTo(twoDays);
        assertThat(resultComment).isPresent();
        assertThat(resultComment.get().getValueComment()).isEqualTo("comment2");
    }

    @Test
    public void shouldChooseOldestDateAndOldestComment() {
        Instant now = Instant.now();
        TimeInUnits day = new TimeInUnits(1, TimeInUnits.TimeUnit.DAY);
        TimeInUnits twoDays = new TimeInUnits(2, TimeInUnits.TimeUnit.DAY);

        ShelfLifeBlock[] blocksWithTime = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(day, null, now));

        ShelfLifeBlock[] blocksWithTime2 = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(twoDays, null, now.minusSeconds(60)));

        ShelfLifeBlock[] blocksWithComment = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(null, "comment1", now));

        ShelfLifeBlock[] blocksWithComment2 = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(null, "comment2", now.minusSeconds(60)));

        Optional<ShelfLifeBlock> result = ItemBlockCalculationStrategies.mostRichOfTwo(
            join(blocksWithComment, blocksWithTime, blocksWithTime2, blocksWithComment2),
            ShelfLifeBlock::getValueWithUnit,
            ShelfLifeBlock::getValueComment
        );

        Optional<ShelfLifeBlock> resultComment = ItemBlockCalculationStrategies.mostRichOfTwo(
            join(blocksWithComment, blocksWithTime, blocksWithTime2, blocksWithComment2),
            ShelfLifeBlock::getValueComment,
            ShelfLifeBlock::getValueWithUnit
        );

        assertThat(result).isPresent();
        assertThat(result.get().getValueWithUnit()).isEqualTo(twoDays);
        assertThat(resultComment).isPresent();
        assertThat(resultComment.get().getValueComment()).isEqualTo("comment2");
    }

    @Test
    public void shouldChooseEarliestByAverageMostCommonDate() {
        Instant now = Instant.now();
        TimeInUnits day = new TimeInUnits(1, TimeInUnits.TimeUnit.DAY);
        TimeInUnits twoDays = new TimeInUnits(2, TimeInUnits.TimeUnit.DAY);
        TimeInUnits threeDays = new TimeInUnits(3, TimeInUnits.TimeUnit.DAY);

        ShelfLifeBlock[] blocks1 = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(day, null, now.minusSeconds(1200)));

        ShelfLifeBlock[] blocks2 = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(twoDays, null, now));

        ShelfLifeBlock[] blocks3 = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(twoDays, null, now.minusSeconds(600)));

        ShelfLifeBlock[] blocks4 = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(threeDays, null, now.minusSeconds(480)));

        ShelfLifeBlock[] blocks5 = generateShelfLifeBlocks(1, block ->
            block.fromSskuMasterData(threeDays, null, now.minusSeconds(540)));

        Optional<ShelfLifeBlock> result = ItemBlockCalculationStrategies.mostRichOfTwo(
            join(blocks1, blocks2, blocks3, blocks4, blocks5),
            ShelfLifeBlock::getValueWithUnit,
            ShelfLifeBlock::getValueComment
        );

        assertThat(result).isPresent();
        assertThat(result.get().getValueWithUnit()).isEqualTo(threeDays);
    }

    @Test
    public void whenMasterDataIsEmptyShouldReturnOptionalEmpty() {
        Optional<ShelfLifeBlock> result = ItemBlockCalculationStrategies.mostCommonValue(
            emptyList(),
            ShelfLifeBlock::getValueWithUnit,
            QuorumType.LATEST
        );

        assertThat(result).isEmpty();
    }

    @Test
    public void testDefaultPriorityComparator() {
        Instant afterSupplierUpgrade =
            MdmProperties.DEFAULT_REPLACE_SUPPLIER_WITH_WAREHOUSE_FROM_TS.plus(1, ChronoUnit.DAYS);
        Instant beforeSupplierUpgrade =
            MdmProperties.DEFAULT_REPLACE_SUPPLIER_WITH_WAREHOUSE_FROM_TS.minus(1, ChronoUnit.DAYS);
        DimensionsBlock supplierBeforeUpgrade =
            generateDimensionBlock(MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        DimensionsBlock supplierAfterUpgrade =
            generateDimensionBlock(MasterDataSourceType.SUPPLIER, afterSupplierUpgrade);
        DimensionsBlock warehouse =
            generateDimensionBlock(MasterDataSourceType.WAREHOUSE, afterSupplierUpgrade);
        DimensionsBlock tool =
            generateDimensionBlock(MasterDataSourceType.TOOL, afterSupplierUpgrade);
        DimensionsBlock unknown =
            generateDimensionBlock(MasterDataSourceType.MDM_UNKNOWN, afterSupplierUpgrade);
        List<DimensionsBlock> beforeSort =
            List.of(tool, unknown, warehouse, supplierAfterUpgrade, supplierBeforeUpgrade);
        List<DimensionsBlock> sorted = new ArrayList<>(beforeSort);
        sorted.sort(ItemBlockCalculationStrategies.DEFAULT_PRIORITY_COMPARATOR);
        List<DimensionsBlock> expected = List.of(
            unknown, supplierBeforeUpgrade, warehouse, supplierAfterUpgrade, tool
        );
        Assertions.assertThat(sorted).containsExactlyElementsOf(expected);
    }

    @Test
    public void testPreferManualComparator() {
        Instant updatedTs = Instant.ofEpochSecond(1615889330);
        DimensionsBlock tool = generateDimensionBlock(MasterDataSourceType.TOOL, updatedTs);
        DimensionsBlock operator = generateDimensionBlock(MasterDataSourceType.MDM_OPERATOR, updatedTs);
        DimensionsBlock oldOperator =
            generateDimensionBlock(MasterDataSourceType.MDM_OPERATOR, updatedTs.minus(1, ChronoUnit.DAYS));
        DimensionsBlock admin = generateDimensionBlock(MasterDataSourceType.MDM_ADMIN, updatedTs);
        DimensionsBlock warehouse = generateDimensionBlock(MasterDataSourceType.WAREHOUSE, updatedTs);
        // PREFER_MANUAL: tool < operator
        Assertions.assertThat(ItemBlockCalculationStrategies.PREFER_MANUAL_COMPARATOR.compare(tool, operator))
            .isLessThan(0);
        // DEFAULT: tool > operator
        Assertions.assertThat(ItemBlockCalculationStrategies.DEFAULT_PRIORITY_COMPARATOR.compare(tool, operator))
            .isGreaterThan(0);
        // both manual, prefer with best default priority (admin > operator)
        Assertions.assertThat(ItemBlockCalculationStrategies.PREFER_MANUAL_COMPARATOR.compare(admin, operator))
            .isGreaterThan(0);
        // both operator, prefer latest
        Assertions.assertThat(ItemBlockCalculationStrategies.PREFER_MANUAL_COMPARATOR.compare(oldOperator, operator))
            .isLessThan(0);
        // retain all manual
        Assertions.assertThat(ItemBlockCalculationStrategies.ifHasManualIgnoreOthers(List.of(
            tool, operator, oldOperator, admin, warehouse
        ))).containsExactlyInAnyOrder(operator, oldOperator, admin);
        // if no manual is present, retain all
        Assertions.assertThat(ItemBlockCalculationStrategies.ifHasManualIgnoreOthers(List.of(
            tool, warehouse
        ))).containsExactlyInAnyOrder(tool, warehouse);
    }

    @Test
    public void testGetHighestPriorityBlocks() {
        Instant afterSupplierUpgrade =
            MdmProperties.DEFAULT_REPLACE_SUPPLIER_WITH_WAREHOUSE_FROM_TS.plus(1, ChronoUnit.DAYS);
        Instant beforeSupplierUpgrade =
            MdmProperties.DEFAULT_REPLACE_SUPPLIER_WITH_WAREHOUSE_FROM_TS.minus(1, ChronoUnit.DAYS);
        DimensionsBlock supplierBeforeUpgrade =
            generateDimensionBlock(MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        DimensionsBlock supplierAfterUpgrade =
            generateDimensionBlock(MasterDataSourceType.SUPPLIER, afterSupplierUpgrade);
        DimensionsBlock warehouse =
            generateDimensionBlock(MasterDataSourceType.WAREHOUSE, afterSupplierUpgrade);
        DimensionsBlock unknown =
            generateDimensionBlock(MasterDataSourceType.MDM_UNKNOWN, afterSupplierUpgrade);
        DimensionsBlock mdmDefault =
            generateDimensionBlock(MasterDataSourceType.MDM_DEFAULT, afterSupplierUpgrade);

        List<ItemBlock> blocks = List.of(warehouse, unknown, supplierAfterUpgrade, supplierBeforeUpgrade, mdmDefault);
        List<ItemBlock> expectedResult = List.of(warehouse, supplierAfterUpgrade);

        List<ItemBlock> result = ItemBlockCalculationStrategies.getHighestPriorityBlocks(blocks);
        Assertions.assertThat(result).containsExactlyElementsOf(expectedResult);
    }

    @Test
    public void testClosestToGeometricMean() {
        Instant beforeSupplierUpgrade =
            MdmProperties.DEFAULT_REPLACE_SUPPLIER_WITH_WAREHOUSE_FROM_TS.minus(1, ChronoUnit.DAYS);
        DimensionsBlock dimensionsBlock1 =
            generateDimensionBlock("10", "10", "10", "1", MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        DimensionsBlock dimensionsBlock2 =
            generateDimensionBlock("11", "11", "11", "1.35", MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        DimensionsBlock dimensionsBlock3 =
            generateDimensionBlock("20", "20", "20", "8", MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        DimensionsBlock lowPriorityBlock =
            generateDimensionBlock("2", "2", "2", "0.08", MasterDataSourceType.MDM_DEFAULT, beforeSupplierUpgrade);
        List<DimensionsBlock> blocks = List.of(dimensionsBlock1, dimensionsBlock2, dimensionsBlock3,
            lowPriorityBlock);

        Optional<DimensionsBlock> result = ItemBlockCalculationStrategies.closestToGeometricMean(blocks,
            GoldComputationContext.EMPTY_CONTEXT);

        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(dimensionsBlock2);
    }

    @Test
    public void whenTwoAreClosestToMeanShouldChooseOneThatSatisfyCategorySetting() {
        Instant beforeSupplierUpgrade =
            MdmProperties.DEFAULT_REPLACE_SUPPLIER_WITH_WAREHOUSE_FROM_TS.minus(1, ChronoUnit.DAYS);
        DimensionsBlock dimensionsBlock1 =
            generateDimensionBlock("10", "10", "10", "1", MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        DimensionsBlock dimensionsBlock2 =
            generateDimensionBlock("90", "90", "90", "10000", MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        Optional<DimensionsBlock> result = ItemBlockCalculationStrategies.closestToGeometricMean(
            List.of(dimensionsBlock1, dimensionsBlock2),
            new GoldComputationContext(0L, List.of(), Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true),
                Map.of(), Map.of(), Map.of(), Map.of(), Set.of())
        );
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(dimensionsBlock2);
    }

    @Test
    public void whenTwoAreClosestToGeometricMeanAndSatisfyCategorySettingShouldChooseLatest() {
        DimensionsBlock dimensionsBlock1 =
            generateDimensionBlock("10", "10", "10", "1", MasterDataSourceType.SUPPLIER, Instant.ofEpochSecond(100000));
        DimensionsBlock dimensionsBlock2 =
            generateDimensionBlock("10", "10", "10", "1", MasterDataSourceType.SUPPLIER, Instant.ofEpochSecond(200000));
        Optional<DimensionsBlock> result = ItemBlockCalculationStrategies.closestToGeometricMean(
            List.of(dimensionsBlock1, dimensionsBlock2),
            GoldComputationContext.EMPTY_CONTEXT
        );
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(dimensionsBlock2);
    }

    @Test
    public void whenClosestToGeometricMeanNotSignificantDifferentFromOldGoldKeepOldGold() {
        DimensionsBlock dimensionsBlock1 =
            generateDimensionBlock("10", "10", "10", "1", MasterDataSourceType.SUPPLIER, Instant.ofEpochSecond(30000));
        DimensionsBlock dimensionsBlock2 =
            generateDimensionBlock("20", "20", "20", "8", MasterDataSourceType.SUPPLIER, Instant.ofEpochSecond(20000));
        DimensionsBlock dimensionsBlock3 =
            generateDimensionBlock("10", "11", "9", "0.9", MasterDataSourceType.SUPPLIER, Instant.ofEpochSecond(20000));

        Optional<DimensionsBlock> resultWithoutOldGold = ItemBlockCalculationStrategies.closestToGeometricMean(
            List.of(dimensionsBlock1, dimensionsBlock2, dimensionsBlock3),
            GoldComputationContext.EMPTY_CONTEXT
        );
        Assertions.assertThat(resultWithoutOldGold).isPresent();
        Assertions.assertThat(resultWithoutOldGold.get()).isEqualTo(dimensionsBlock1);

        DimensionsBlock oldGold =
            generateDimensionBlock("9.5", "10.5", "10", "0.92", MasterDataSourceType.SUPPLIER, Instant.EPOCH);
        Optional<DimensionsBlock> result = ItemBlockCalculationStrategies.closestToGeometricMean(
            List.of(dimensionsBlock1, dimensionsBlock2, dimensionsBlock3),
            oldGold,
            GoldComputationContext.EMPTY_CONTEXT
        );
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(oldGold);
    }

    @Test
    public void whenSourceIsTrustedSkipGeometricMeanAlgoAndTakeMostRecentGold() {
        DimensionsBlock closeToMeanBlock1 =
            generateDimensionBlock("10", "10", "10", "1", MasterDataSourceType.MEASUREMENT, Instant.ofEpochSecond(20));
        DimensionsBlock latestBlock =
            generateDimensionBlock("30", "40", "50", "10", MasterDataSourceType.MEASUREMENT, Instant.ofEpochSecond(30));
        DimensionsBlock closeToMeanBlock2 =
            generateDimensionBlock("10", "11", "9", "0.9", MasterDataSourceType.MEASUREMENT, Instant.ofEpochSecond(20));

        Optional<DimensionsBlock> resultWithoutOldGold = ItemBlockCalculationStrategies.closestToGeometricMean(
            List.of(closeToMeanBlock1, latestBlock, closeToMeanBlock2),
            GoldComputationContext.EMPTY_CONTEXT
        );
        Assertions.assertThat(resultWithoutOldGold).isPresent();
        Assertions.assertThat(resultWithoutOldGold.get()).isEqualTo(latestBlock);

        DimensionsBlock oldGold =
            generateDimensionBlock("9.5", "10.5", "10", "0.92", MasterDataSourceType.MEASUREMENT, Instant.EPOCH);
        Optional<DimensionsBlock> result = ItemBlockCalculationStrategies.closestToGeometricMean(
            List.of(closeToMeanBlock1, latestBlock, closeToMeanBlock2),
            oldGold,
            GoldComputationContext.EMPTY_CONTEXT
        );
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(latestBlock);
    }

    @Test
    public void whenClosestToGeometricMeanNotSignificantDifferentFromOldGoldButHasHigherPriorityUseNewGold() {
        DimensionsBlock dimensionsBlock1 =
            generateDimensionBlock("10", "10", "10", "1", MasterDataSourceType.MDM_ADMIN, Instant.ofEpochSecond(30000));
        DimensionsBlock dimensionsBlock2 =
            generateDimensionBlock("20", "20", "20", "8", MasterDataSourceType.MDM_ADMIN, Instant.ofEpochSecond(2000));
        DimensionsBlock dimensionsBlock3 =
            generateDimensionBlock("10", "11", "9", "0.9", MasterDataSourceType.MDM_ADMIN, Instant.ofEpochSecond(2000));

        DimensionsBlock oldGold =
            generateDimensionBlock("9.5", "10.5", "10", "0.92", MasterDataSourceType.SUPPLIER, Instant.EPOCH);
        Optional<DimensionsBlock> result = ItemBlockCalculationStrategies.closestToGeometricMean(
            List.of(dimensionsBlock1, dimensionsBlock2, dimensionsBlock3),
            oldGold,
            GoldComputationContext.EMPTY_CONTEXT
        );
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(dimensionsBlock1);
    }

    @Test
    public void whenTwoAreClosestToMeanAndBothCategorySettingsPreferHeavyGood30ToHeavyGood20() {
        Instant beforeSupplierUpgrade =
            MdmProperties.DEFAULT_REPLACE_SUPPLIER_WITH_WAREHOUSE_FROM_TS.minus(1, ChronoUnit.DAYS);
        DimensionsBlock dimensionsBlock1 =
            generateDimensionBlock("20", "20", "20", "25", MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        DimensionsBlock dimensionsBlock2 =
            generateDimensionBlock("30", "30", "30", "35", MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        Optional<DimensionsBlock> result = ItemBlockCalculationStrategies.closestToGeometricMean(
            List.of(dimensionsBlock1, dimensionsBlock2),
            new GoldComputationContext(0L, List.of(), Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true,
                KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID, true),
                Map.of(), Map.of(), Map.of(), Map.of(), Set.of())
        );
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(dimensionsBlock2);
    }

    @Test
    public void whenTwoAreClosestToMeanAndOnlyHeavyGood20CategorySettingsPreferIt() {
        Instant beforeSupplierUpgrade =
            MdmProperties.DEFAULT_REPLACE_SUPPLIER_WITH_WAREHOUSE_FROM_TS.minus(1, ChronoUnit.DAYS);
        DimensionsBlock dimensionsBlock1 =
            generateDimensionBlock("20", "20", "20", "25", MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        DimensionsBlock dimensionsBlock2 =
            generateDimensionBlock("30", "30", "30", "35", MasterDataSourceType.SUPPLIER, beforeSupplierUpgrade);
        Optional<DimensionsBlock> result = ItemBlockCalculationStrategies.closestToGeometricMean(
            List.of(dimensionsBlock1, dimensionsBlock2),
            new GoldComputationContext(0L, List.of(), Map.of(KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID, true),
                Map.of(), Map.of(), Map.of(), Map.of(), Set.of())
        );
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(dimensionsBlock1);
    }

    @Test
    public void whenNoClosestToMeanCandidateShouldNotKeepOldGold() {
        DimensionsBlock oldGold =
            generateDimensionBlock("9.5", "10.5", "10", "100500", MasterDataSourceType.SUPPLIER, Instant.EPOCH);
        Optional<DimensionsBlock> result = ItemBlockCalculationStrategies.closestToGeometricMean(List.of(), oldGold,
            GoldComputationContext.EMPTY_CONTEXT);
        Assertions.assertThat(result).isEmpty();
    }

    @SafeVarargs
    private <T extends ItemBlock> List<T> join(T[]... data) {
        return Stream.of(data).flatMap(Stream::of).collect(Collectors.toList());
    }

    private ExpirDateBlock[] generateExpirApplyBlocks(
        int count,
        Consumer<ExpirDateBlock> processor
    ) {
        return IntStream.range(0, count)
            .mapToObj(i -> generateExpirApplyBlock())
            .peek(processor)
            .toArray(ExpirDateBlock[]::new);
    }

    private ShelfLifeBlock[] generateShelfLifeBlocks(int count, Consumer<ShelfLifeBlock> processor) {
        return IntStream.range(0, count)
            .mapToObj(i -> generateShelfLifeBlock())
            .peek(processor)
            .toArray(ShelfLifeBlock[]::new);
    }

    private ExpirDateBlock generateExpirApplyBlock() {
        MdmParam param = new MdmParam()
            .setId(KnownMdmParams.EXPIR_DATE)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setValueType(MdmParamValueType.MBO_BOOL);
        return new ExpirDateBlock(param);
    }

    private ShelfLifeBlock generateShelfLifeBlock() {
        MdmParam shelfLife = new MdmParam()
            .setId(KnownMdmParams.SHELF_LIFE)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam shelfLifeUnit = new MdmParam()
            .setId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setValueType(MdmParamValueType.MBO_ENUM);
        MdmParam shelfLifeComment = new MdmParam()
            .setId(KnownMdmParams.SHELF_LIFE_COMMENT)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setValueType(MdmParamValueType.STRING);
        return new ShelfLifeBlock(shelfLife, shelfLifeUnit, shelfLifeComment);
    }

    private DimensionsBlock generateDimensionBlock(MasterDataSourceType sourceType,
                                                   Instant updatedTS) {
        return generateDimensionBlock("10", "10", "10", "1", sourceType, updatedTS);
    }

    private DimensionsBlock generateDimensionBlock(String length,
                                                   String width,
                                                   String height,
                                                   String weight,
                                                   MasterDataSourceType sourceType,
                                                   Instant updatedTS) {
        MdmParamValue lengthParamValue = new MdmParamValue()
            .setNumeric(new BigDecimal(length))
            .setMasterDataSourceType(sourceType)
            .setUpdatedTs(updatedTS);
        MdmParamValue widthParamValue = new MdmParamValue()
            .setNumeric(new BigDecimal(width))
            .setMasterDataSourceType(sourceType)
            .setUpdatedTs(updatedTS);
        MdmParamValue heightParamValue = new MdmParamValue()
            .setNumeric(new BigDecimal(height))
            .setMasterDataSourceType(sourceType)
            .setUpdatedTs(updatedTS);
        MdmParamValue weightParamValue = new MdmParamValue()
            .setNumeric(new BigDecimal(weight))
            .setMasterDataSourceType(sourceType)
            .setUpdatedTs(updatedTS);
        return new DimensionsBlock(lengthParamValue, widthParamValue, heightParamValue, weightParamValue);
    }
}
