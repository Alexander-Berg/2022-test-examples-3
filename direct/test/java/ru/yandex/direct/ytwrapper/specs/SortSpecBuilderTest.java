package ru.yandex.direct.ytwrapper.specs;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ytwrapper.exceptions.SpecGenerationException;
import ru.yandex.direct.ytwrapper.model.YtField;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.inside.yt.kosher.operations.specs.SortSpec;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class SortSpecBuilderTest {
    private static final YtTable inputTable = new YtTable("//tmp/t1");
    private static final YtTable outputTable = new YtTable("//tmp/t2");
    private static final YtField<Long> sortByField = new YtField<>("sort", Long.class);

    private SortSpecBuilder builder;

    @Before
    public void before() {
        builder = new SortSpecBuilder();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoInput() {
        builder.addSortByField(sortByField).setOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoOutput() {
        builder.addSortByField(sortByField).addInputTable(inputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateTooMuchOutput() {
        builder.addSortByField(sortByField).addInputTable(inputTable).addOutputTable(outputTable)
                .addOutputTable(inputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoSortField() {
        builder.addInputTable(inputTable).setOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test()
    public void testValidate() {
        builder.addSortByField(sortByField).addInputTable(inputTable).setOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test()
    public void testBuild() {
        builder.addSortByField(sortByField).addInputTable(inputTable).setOutputTable(outputTable);
        OperationSpec spec = builder.build();
        assertThat("Спек ожидаемого класса", spec, instanceOf(SingleOperationSpec.class));
        assertThat("Вложенный спек ожидаемого класса", ((SingleOperationSpec) spec).getSpec(),
                instanceOf(SortSpec.class));
    }
}
