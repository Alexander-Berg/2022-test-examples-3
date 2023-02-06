package ru.yandex.direct.ytwrapper.specs;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ytwrapper.exceptions.SpecGenerationException;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class DeleteTableSpecBuilderTest {
    private static final YtTable inputTable = new YtTable("//tmp/t1");

    private DeleteTableSpecBuilder builder;

    @Before
    public void before() {
        builder = new DeleteTableSpecBuilder();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateNoInput() {
        builder.validateCurrent();
    }

    @Test(expected = SpecGenerationException.class)
    public void testValidateTooMuchInput() {
        builder.addInputTable(inputTable).addInputTable(new YtTable("//tmp/test"));
        builder.validateCurrent();
    }

    @Test()
    public void testValidate() {
        builder.setInputTable(inputTable);
        builder.validateCurrent();
    }

    @Test()
    public void testBuild() {
        builder.setInputTable(inputTable);
        OperationSpec spec = builder.build();
        assertThat("Спек ожидаемого класса", spec, instanceOf(DeleteNodeOperationSpec.class));
        assertThat("Вложенный спек ожидаемого класса", ((DeleteNodeOperationSpec) spec).getYPath(),
                equalTo(YPath.simple("//tmp/t1")));
    }
}
