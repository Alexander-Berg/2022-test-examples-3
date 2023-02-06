package ru.yandex.market.mboc.common.masterdata.services.msku;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamMetaType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.services.ShelfLifeBlockCalculationStrategies;

/**
 * @author albina-gima
 * @date 12/23/21
 */
public class ShelfLifeBlockCalculationStrategiesTest {
    private static final String SOURCE_ID_1 = "1";
    private static final String SOURCE_ID_2 = "2"; // banned source id
    private static final String SOURCE_ID_3 = "3";
    private static final String SOURCE_ID_4 = "4";
    private static final String SOURCE_ID_5 = "5";
    private static final String SOURCE_ID_6 = "6";

    private static final long HIGHEST_WAREHOUSE_PRIORITY = 300;
    private static final long MID_WAREHOUSE_PRIORITY = 200;
    private static final long LOWEST_WAREHOUSE_PRIORITY = 100;

    private static final Map<String, Long> WAREHOUSE_PRIORITIES = Map.of(
        SOURCE_ID_1, HIGHEST_WAREHOUSE_PRIORITY,
        SOURCE_ID_2, HIGHEST_WAREHOUSE_PRIORITY,
        SOURCE_ID_3, MID_WAREHOUSE_PRIORITY,
        SOURCE_ID_4, LOWEST_WAREHOUSE_PRIORITY,
        SOURCE_ID_5, LOWEST_WAREHOUSE_PRIORITY
    );
    private static final Set<String> BANNED_WAREHOUSES = Set.of(SOURCE_ID_2);

    @Test
    public void whenShouldSelectNewestBlockWithMdmAdminSource() {
        // given
        Instant updatedNowTs = Instant.now();
        Instant earlierUpdatedTs = updatedNowTs.minusSeconds(1000);

        ValueCommentBlock adminBlockNewest = generateShelfLifeBlock(1, "cookie",
            MasterDataSourceType.MDM_ADMIN, SOURCE_ID_6, updatedNowTs);

        ValueCommentBlock adminBlockOlder = generateShelfLifeBlock(1, "candy",
            MasterDataSourceType.MDM_ADMIN, SOURCE_ID_6, earlierUpdatedTs);

        ValueCommentBlock warehouseBlock1 = generateShelfLifeBlock(2, "pie",
            MasterDataSourceType.MEASUREMENT, SOURCE_ID_1, updatedNowTs);

        ValueCommentBlock warehouseBlock2 = generateShelfLifeBlock(3, "cake",
            MasterDataSourceType.MEASUREMENT, SOURCE_ID_4, updatedNowTs);

        GoldComputationContext context = new GoldComputationContext(0L, List.of(),
            Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true),
            Map.of(), Map.of(), Map.of(), WAREHOUSE_PRIORITIES, BANNED_WAREHOUSES);

        List<ValueCommentBlock> allBlocks = List.of(adminBlockNewest, adminBlockOlder,
            warehouseBlock1, warehouseBlock2);

        // when
        Optional<ValueCommentBlock> result = ShelfLifeBlockCalculationStrategies.compareShelfLifeBlocks(
            allBlocks, context);

        // then
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get()).isEqualTo(adminBlockNewest);
    }

    @Test
    public void whenOnlyWarehouseBlocksGivenShouldSelectNewestBlockWithHighestWarehousePriority() {
        // given
        Instant updatedNowTs = Instant.now();
        Instant earlierUpdatedTs = updatedNowTs.minusSeconds(1000);

        ValueCommentBlock warehouseBlockNewestHighestPriority = generateShelfLifeBlock(2, "pie",
            MasterDataSourceType.MEASUREMENT, SOURCE_ID_1, updatedNowTs);

        ValueCommentBlock warehouseBlockHighestPriorityOlder = generateShelfLifeBlock(2, "candy",
            MasterDataSourceType.MEASUREMENT, SOURCE_ID_1, earlierUpdatedTs);

        ValueCommentBlock warehouseBlockMidPriority = generateShelfLifeBlock(3, "cake",
            MasterDataSourceType.MEASUREMENT, SOURCE_ID_4, updatedNowTs);

        GoldComputationContext context = new GoldComputationContext(0L, List.of(),
            Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true),
            Map.of(), Map.of(), Map.of(), WAREHOUSE_PRIORITIES, BANNED_WAREHOUSES);

        List<ValueCommentBlock> allBlocks = List.of(warehouseBlockHighestPriorityOlder, warehouseBlockMidPriority,
            warehouseBlockNewestHighestPriority);

        // when
        Optional<ValueCommentBlock> result = ShelfLifeBlockCalculationStrategies.compareShelfLifeBlocks(
            allBlocks, context);

        // then
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get()).isEqualTo(warehouseBlockNewestHighestPriority);
    }

    @Test
    public void whenShouldSelectSupplierBlockAsNewestOne() {
        // given
        Instant updatedNowTs = Instant.now();
        Instant earlierUpdatedTs = updatedNowTs.minusSeconds(5);
        Instant longTimeAgoUpdatedTs = updatedNowTs.minusSeconds(5000);

        ValueCommentBlock toolBlock = generateShelfLifeBlock(2, "pie",
            MasterDataSourceType.TOOL, SOURCE_ID_1, earlierUpdatedTs);

        ValueCommentBlock mdmOperatorBlock = generateShelfLifeBlock(2, "cookie",
            MasterDataSourceType.MDM_OPERATOR, SOURCE_ID_1, longTimeAgoUpdatedTs);

        ValueCommentBlock supplierBlockNewest = generateShelfLifeBlock(3, "cake",
            MasterDataSourceType.SUPPLIER, SOURCE_ID_4, updatedNowTs);

        GoldComputationContext context = new GoldComputationContext(0L, List.of(),
            Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true),
            Map.of(), Map.of(), Map.of(), WAREHOUSE_PRIORITIES, BANNED_WAREHOUSES);

        List<ValueCommentBlock> allBlocks = List.of(toolBlock, mdmOperatorBlock, supplierBlockNewest);

        // when
        Optional<ValueCommentBlock> result = ShelfLifeBlockCalculationStrategies.compareShelfLifeBlocks(
            allBlocks, context);

        // then
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get()).isEqualTo(supplierBlockNewest);
    }

    @Test
    public void whenMeasurementBlockIsBannedShouldSelectNewestBlockWithSupplierSource() {
        // given
        Instant updatedNowTs = Instant.now();

        ValueCommentBlock warehouseBlockBanned = generateShelfLifeBlock(2, "pie",
            MasterDataSourceType.MEASUREMENT, SOURCE_ID_2, updatedNowTs);

        ValueCommentBlock supplierBlockNewest = generateShelfLifeBlock(3, "cake",
            MasterDataSourceType.SUPPLIER, SOURCE_ID_4, updatedNowTs);

        GoldComputationContext context = new GoldComputationContext(0L, List.of(),
            Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true),
            Map.of(), Map.of(), Map.of(), WAREHOUSE_PRIORITIES, BANNED_WAREHOUSES);

        List<ValueCommentBlock> allBlocks = List.of(warehouseBlockBanned, supplierBlockNewest);

        // when
        Optional<ValueCommentBlock> result = ShelfLifeBlockCalculationStrategies.compareShelfLifeBlocks(
            allBlocks, context);

        // then
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get()).isEqualTo(supplierBlockNewest);
    }

    @Test
    public void whenAllBlockOfMeasurementTypeAreBannedShouldReturnEmptyResult() {
        // given
        Instant updatedNowTs = Instant.now();

        ValueCommentBlock warehouseBlockBanned = generateShelfLifeBlock(2, "pie",
            MasterDataSourceType.MEASUREMENT, SOURCE_ID_2, updatedNowTs);

        GoldComputationContext context = new GoldComputationContext(0L, List.of(),
            Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true),
            Map.of(), Map.of(), Map.of(), WAREHOUSE_PRIORITIES, BANNED_WAREHOUSES);

        List<ValueCommentBlock> allBlocks = List.of(warehouseBlockBanned);

        // when
        Optional<ValueCommentBlock> result = ShelfLifeBlockCalculationStrategies.compareShelfLifeBlocks(
            allBlocks, context);

        // then
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void shouldReturnEmptyResultIfAllBlocksHaveUnsupportedSourceType() {
        // given
        Instant updatedNowTs = Instant.now();
        Instant earlierUpdatedTs = updatedNowTs.minusSeconds(5);
        Instant longTimeAgoUpdatedTs = updatedNowTs.minusSeconds(1000);

        ValueCommentBlock unknownBlock = generateShelfLifeBlock(2, "pie",
            MasterDataSourceType.MDM_UNKNOWN, SOURCE_ID_1, earlierUpdatedTs);

        ValueCommentBlock autoBlock = generateShelfLifeBlock(2, "cookie",
            MasterDataSourceType.AUTO, SOURCE_ID_1, longTimeAgoUpdatedTs);

        ValueCommentBlock oldWarehouseBlock = generateShelfLifeBlock(3, "cake",
            MasterDataSourceType.WAREHOUSE, SOURCE_ID_4, updatedNowTs);

        GoldComputationContext context = new GoldComputationContext(0L, List.of(),
            Map.of(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true),
            Map.of(), Map.of(), Map.of(), WAREHOUSE_PRIORITIES, BANNED_WAREHOUSES);

        List<ValueCommentBlock> allBlocks = List.of(autoBlock, oldWarehouseBlock, unknownBlock);

        // when
        Optional<ValueCommentBlock> result = ShelfLifeBlockCalculationStrategies.compareShelfLifeBlocks(
            allBlocks, context);

        // then
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void whenYouAddNewSourceTypeDontForgetToPutItIntoShelfLifePriorities() {
        var prioritisedTypes = ShelfLifeBlockCalculationStrategies.MASTER_DATA_SOURCE_TYPES_PRIORITIES.keySet();
        Set<MasterDataSourceType> knownExceptions = Set.of(
            MasterDataSourceType.MDM_UNKNOWN,
            MasterDataSourceType.MSKU_INHERIT,
            MasterDataSourceType.MDM_GLOBAL,
            MasterDataSourceType.IRIS_UNRECOGNIZED,
            MasterDataSourceType.IRIS_MDM,
            MasterDataSourceType.WAREHOUSE,
            MasterDataSourceType.AUTO
        );
        Set<MasterDataSourceType> allTypes = Set.of(MasterDataSourceType.values());

        Assertions.assertThat(prioritisedTypes).containsExactlyInAnyOrderElementsOf(
            Sets.difference(allTypes, knownExceptions)
        );
    }

    @Test
    public void testMboOperatorBeatsMdmOperatorAndSupplier() {
        // given
        Instant updatedNowTs = Instant.now();
        Instant earlierUpdatedTs = updatedNowTs.minusSeconds(5);
        Instant longTimeAgoUpdatedTs = updatedNowTs.minusSeconds(1000);

        ValueCommentBlock mboOperatorBlock =
            generateShelfLifeBlock(2, "pie", MasterDataSourceType.MBO_OPERATOR, "krotkov", longTimeAgoUpdatedTs);

        ValueCommentBlock mdmOperatorBlock =
            generateShelfLifeBlock(2, "cookie", MasterDataSourceType.MDM_OPERATOR, "automarkup", earlierUpdatedTs);

        ValueCommentBlock supplierBlock =
            generateShelfLifeBlock(3, "cake", MasterDataSourceType.SUPPLIER, "ИП Пупкин", updatedNowTs);

        List<ValueCommentBlock> allBlocks = List.of(mdmOperatorBlock, supplierBlock, mboOperatorBlock);

        // when
        Optional<ValueCommentBlock> result =
            ShelfLifeBlockCalculationStrategies.compareShelfLifeBlocks(allBlocks, GoldComputationContext.EMPTY_CONTEXT);

        // then
        Assertions.assertThat(result.orElseThrow()).isEqualTo(mboOperatorBlock);
    }

    @Test
    public void testMeasurementFromValidWarehouseBeatsMboOperator() {
        // given
        Instant updatedNowTs = Instant.now();
        Instant earlierUpdatedTs = updatedNowTs.minusSeconds(5);
        Instant longTimeAgoUpdatedTs = updatedNowTs.minusSeconds(1000);

        ValueCommentBlock mboOperatorBlock =
            generateShelfLifeBlock(2, "pie", MasterDataSourceType.MBO_OPERATOR, "krotkov", updatedNowTs);

        ValueCommentBlock measurementBlock =
            generateShelfLifeBlock(3, "cookie", MasterDataSourceType.MEASUREMENT, SOURCE_ID_1, longTimeAgoUpdatedTs);

        GoldComputationContext context = new GoldComputationContext(
            0L,
            List.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            WAREHOUSE_PRIORITIES,
            BANNED_WAREHOUSES
        );

        List<ValueCommentBlock> allBlocks = List.of(measurementBlock, mboOperatorBlock);

        // when
        Optional<ValueCommentBlock> result =
            ShelfLifeBlockCalculationStrategies.compareShelfLifeBlocks(allBlocks, context);

        // then
        Assertions.assertThat(result.orElseThrow()).isEqualTo(measurementBlock);
    }

    private ValueCommentBlock generateShelfLifeBlock(int value, String comment,
                                                     MasterDataSourceType sourceType, String sourceId,
                                                     Instant updateTs) {
        var shelfLifePV = new MdmParamValue();
        shelfLifePV.setMdmParamId(KnownMdmParams.SHELF_LIFE);
        shelfLifePV.setNumeric(BigDecimal.valueOf(value));
        shelfLifePV.setModificationInfo(generateModificationInfo(sourceType, sourceId, updateTs));

        var shelfLifeCommentPV = new MdmParamValue();
        shelfLifeCommentPV.setMdmParamId(KnownMdmParams.SHELF_LIFE_COMMENT);
        shelfLifeCommentPV.setString(comment);
        shelfLifeCommentPV.setModificationInfo(generateModificationInfo(sourceType, sourceId, updateTs));

        var shelfLifeUnitPV = new MdmParamValue();
        shelfLifeUnitPV.setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT);
        shelfLifeUnitPV.setOption(
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.DAY)));
        shelfLifeUnitPV.setModificationInfo(generateModificationInfo(sourceType, sourceId, updateTs));

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

        return new ValueCommentBlock(ItemBlock.BlockType.SHELF_LIFE, shelfLife, shelfLifeUnit, shelfLifeComment)
            .fromMdmParamValues(shelfLifePV, shelfLifeUnitPV, shelfLifeCommentPV);
    }

    private MdmModificationInfo generateModificationInfo(MasterDataSourceType sourceType,
                                                         String sourceId,
                                                         Instant updateTs) {
        var modificationInfo = new MdmModificationInfo();
        modificationInfo.setMasterDataSourceType(sourceType)
            .setMasterDataSourceId(sourceId)
            .setUpdatedTs(updateTs)
            .setSourceUpdatedTs(Instant.EPOCH.plusSeconds(1000));
        return modificationInfo;
    }
}
