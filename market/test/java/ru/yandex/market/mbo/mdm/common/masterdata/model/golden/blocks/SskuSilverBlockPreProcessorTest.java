package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

public class SskuSilverBlockPreProcessorTest extends MdmBaseDbTestClass {

    private final SskuSilverBlockPreProcessor sskuSilverBlockPreProcessor = new SskuSilverBlockPreProcessor();

    @Test
    public void whenPreProcessorShouldAddMeasurementExistenceBlocks() {
        Map<ItemBlock.BlockType, List<ItemBlock>> silverBlocksByType = new HashMap<>();
        DimensionsBlock dimensionsBlock = createDimensionsBlockWithMeasurement();
        silverBlocksByType.put(ItemBlock.BlockType.DIMENSIONS, List.of(dimensionsBlock));
        sskuSilverBlockPreProcessor.preProcess(silverBlocksByType, Instant.now(), GoldComputationContext.EMPTY_CONTEXT);
        List<ItemBlock> measurementExistenceBlock = silverBlocksByType.get(ItemBlock.BlockType.MEASUREMENT_EXISTENCE);
        Assertions.assertThat(measurementExistenceBlock).isNotNull();
    }

    @Test
    public void whenPreProcessorShouldNotAddMeasurementExistenceBlocks() {
        Map<ItemBlock.BlockType, List<ItemBlock>> silverBlocksByType = new HashMap<>();
        DimensionsBlock dimensionsBlock = createDimensionsBlock();
        silverBlocksByType.put(ItemBlock.BlockType.DIMENSIONS, List.of(dimensionsBlock));
        sskuSilverBlockPreProcessor.preProcess(silverBlocksByType, Instant.now(), GoldComputationContext.EMPTY_CONTEXT);
        List<ItemBlock> measurementExistenceBlock = silverBlocksByType.get(ItemBlock.BlockType.MEASUREMENT_EXISTENCE);
        Assertions.assertThat(measurementExistenceBlock).isNullOrEmpty();
    }

    @Test
    public void whenPreProcessorWithNullContextShouldNotAddMeasurementExistenceBlocks() {
        Map<ItemBlock.BlockType, List<ItemBlock>> silverBlocksByType = new HashMap<>();
        DimensionsBlock dimensionsBlock = createDimensionsBlockWithMeasurement();
        silverBlocksByType.put(ItemBlock.BlockType.DIMENSIONS, List.of(dimensionsBlock));
        sskuSilverBlockPreProcessor.preProcess(silverBlocksByType, Instant.now(), null);
        List<ItemBlock> measurementExistenceBlock = silverBlocksByType.get(ItemBlock.BlockType.MEASUREMENT_EXISTENCE);
        Assertions.assertThat(measurementExistenceBlock).isNullOrEmpty();
    }

    @Test
    public void whenPreProcessorWithEmptySilverBlocksShouldNotAddMeasurementExistenceBlocks() {
        Map<ItemBlock.BlockType, List<ItemBlock>> silverBlocksByType = new HashMap<>();
        silverBlocksByType.put(ItemBlock.BlockType.DIMENSIONS, List.of());
        sskuSilverBlockPreProcessor.preProcess(silverBlocksByType, Instant.now(), null);
        List<ItemBlock> measurementExistenceBlock = silverBlocksByType.get(ItemBlock.BlockType.MEASUREMENT_EXISTENCE);
        Assertions.assertThat(measurementExistenceBlock).isNullOrEmpty();
    }

    private DimensionsBlock createDimensionsBlock() {
        return new DimensionsBlock(
            createMdmParamValue(1L),
            createMdmParamValue(2L),
            createMdmParamValue(3L),
            createMdmParamValue(4L)
        );
    }

    private DimensionsBlock createDimensionsBlockWithMeasurement() {
        return new DimensionsBlock(
            createMdmParamValue(1L).setMasterDataSourceType(MasterDataSourceType.MEASUREMENT),
            createMdmParamValue(2L).setMasterDataSourceType(MasterDataSourceType.MEASUREMENT),
            createMdmParamValue(3L).setMasterDataSourceType(MasterDataSourceType.MEASUREMENT),
            createMdmParamValue(4L).setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
        );
    }

    private MdmParamValue createMdmParamValue(long value) {
        var pv = new MdmParamValue();
        pv.setNumeric(BigDecimal.valueOf(value));
        return pv;
    }
}
