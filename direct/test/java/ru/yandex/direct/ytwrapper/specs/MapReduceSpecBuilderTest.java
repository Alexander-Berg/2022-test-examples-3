package ru.yandex.direct.ytwrapper.specs;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ytwrapper.exceptions.SpecGenerationException;
import ru.yandex.direct.ytwrapper.model.YtField;
import ru.yandex.direct.ytwrapper.model.YtMapper;
import ru.yandex.direct.ytwrapper.model.YtReducer;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.direct.ytwrapper.model.YtTableRow;
import ru.yandex.inside.yt.kosher.operations.specs.MapReduceSpec;
import ru.yandex.misc.dataSize.DataSize;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.ytwrapper.specs.AppendableSpecBuilder.pair;

public class MapReduceSpecBuilderTest {
    private static final YtField<Long> field1 = new YtField<>("field1", Long.class);
    private static final YtField<Long> field2 = new YtField<>("field2", Long.class);

    private static final YtTable inputTable = new YtTable("//tmp/t1");
    private static final YtTableRow inputTableRow = new YtTableRow(Arrays.asList(field1, field2));
    private static final YtTable outputTable = new YtTable("//tmp/t2");

    private MapReduceSpecBuilder builder;

    @Before
    public void before() {
        builder = new MapReduceSpecBuilder();
    }

    @Test()
    public void testValidate() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addSortByField(field1)
                .addSortByField(field2)
                .addReduceByField(field1)
                .addReduceByField(field2)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .setPartitionCount(1234)
                .setPartitionJobCount(1234)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test()
    public void testValidateNoParCount() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addSortByField(field1)
                .addSortByField(field2)
                .addReduceByField(field1)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test()
    public void testValidateNoSortBy() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addReduceByField(field1)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoInput() {
        builder
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addSortByField(field1)
                .addSortByField(field2)
                .addReduceByField(field1)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .setPartitionCount(8192)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoOutput() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addSortByField(field1)
                .addSortByField(field2)
                .addReduceByField(field1)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .setPartitionCount(8192);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoMapper() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .addSortByField(field1)
                .addSortByField(field2)
                .addReduceByField(field1)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .setPartitionCount(8192)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoReducer() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addSortByField(field1)
                .addSortByField(field2)
                .addReduceByField(field1)
                .setPartitionCount(8192)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoReduceBy() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addSortByField(field1)
                .addSortByField(field2)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .setPartitionCount(8192)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateReduceSortDifferent() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addSortByField(field1)
                .addReduceByField(field2)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .setPartitionCount(8192)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateReduceNotFirstInSort() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addSortByField(field1)
                .addSortByField(field2)
                .addReduceByField(field2)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .setPartitionCount(8192)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test()
    public void testBuild() {
        builder
                .addInputTables(Collections.singletonList(pair(inputTable, inputTableRow)))
                .setMapper(mock(YtMapper.class)).setMapperMemoryLimit(DataSize.fromMegaBytes(500))
                .addSortByField(field1)
                .addSortByField(field2)
                .addReduceByField(field1)
                .addReduceByField(field2)
                .setReducer(mock(YtReducer.class)).setReducerMemoryLimit(DataSize.fromGigaBytes(1))
                .setPartitionCount(1234)
                .setPartitionJobCount(1234)
                .addOutputTable(outputTable);
        OperationSpec spec = builder.build();
        assertThat("Спек ожидаемого класса", spec, instanceOf(SingleOperationSpec.class));
        assertThat("Вложенный спек ожидаемого класса", ((SingleOperationSpec) spec).getSpec(),
                instanceOf(MapReduceSpec.class));
    }
}
