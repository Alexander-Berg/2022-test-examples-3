package ru.yandex.direct.ytwrapper.specs;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ytwrapper.exceptions.SpecGenerationException;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.direct.ytwrapper.model.attributes.OptimizeForAttr;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class SetTableAttributesSpecBuilderTest {
    private static final YtTable inputTable = new YtTable("//tmp/t1");
    private static final YtTable outputInputTable = new YtTable("//tmp/t2");
    private static final YtTable outputTable = new YtTable("//tmp/t2");

    private SetTableAttributesSpecBuilder builder;

    @Before
    public void before() {
        builder = new SetTableAttributesSpecBuilder();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoInput() {
        builder.setOptimizeFor(OptimizeForAttr.SCAN)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoOutput() {
        builder.setOptimizeFor(OptimizeForAttr.SCAN)
                .addInputTable(inputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoAttrs() {
        builder.addInputTable(inputTable)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateOutputInInput() {
        builder.addInputTable(inputTable)
                .addInputTable(outputInputTable)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test
    public void testValidateCorrect() {
        builder.setOptimizeFor(OptimizeForAttr.SCAN)
                .addInputTable(inputTable)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateTooMuchOutput() {
        builder.setOptimizeFor(OptimizeForAttr.SCAN)
                .addInputTable(inputTable)
                .addOutputTable(outputTable).addOutputTable(new YtTable("//tmp/test"));
        builder.validateCurrent();
    }

    @Test()
    public void testBuild() {
        builder.setOptimizeFor(OptimizeForAttr.SCAN)
                .addInputTable(inputTable)
                .addOutputTable(outputTable);

        OperationSpec spec = builder.build();
        assertThat("Спек ожидаемого класса", spec, instanceOf(SetTableAttributesOperationSpec.class));
        assertThat("Вложенный спек ожидаемого класса", ((SetTableAttributesOperationSpec) spec).getYPath(),
                equalTo(YPath.simple("//tmp/t2")));
    }
}
