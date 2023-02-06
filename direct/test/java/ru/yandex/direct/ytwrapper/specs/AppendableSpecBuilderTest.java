package ru.yandex.direct.ytwrapper.specs;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.direct.ytwrapper.model.YtField;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.direct.ytwrapper.model.YtTableRow;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AppendableSpecBuilderTest {
    private static final YtField<Long> field1 = new YtField<>("field1", Long.class);
    private static final YtField<Long> field2 = new YtField<>("field2", Long.class);

    private static final YtTable inputTable = new YtTable("//tmp/t1");
    private static final YtTableRow inputTableRow = new YtTableRow(Arrays.asList(field1, field2));
    private static final YtTable outputTable = new YtTable("//tmp/t2");

    private class TestSpecBuilder extends AppendableSpecBuilder {
        private OperationSpec operationSpec;

        TestSpecBuilder() {
            this(mock(OperationSpec.class));
        }

        TestSpecBuilder(OperationSpec operationSpec) {
            this.operationSpec = operationSpec;
        }

        @Override
        public void validateCurrent() {
        }

        @Override
        protected OperationSpec buildCurrent() {
            return operationSpec;
        }
    }

    @Test
    public void testValidateSingle() {
        AppendableSpecBuilder builder = new TestSpecBuilder()
                .addInputTable(inputTable, inputTableRow)
                .addOutputTable(outputTable);
        builder.validateCurrent();
    }

    @Test
    public void testValidateDouble() {
        AppendableSpecBuilder builder = new TestSpecBuilder()
                .addInputTable(inputTable, inputTableRow);

        AppendableSpecBuilder nextBuilder = new TestSpecBuilder()
                .addOutputTable(outputTable);
        builder.append(nextBuilder);

        builder.build();

        assertThat("Таблица вывода сгенерирована", builder.getOutputTables().size(), equalTo(1));
        assertThat("Таблица ввода сгенерирована", nextBuilder.getInputTables().size(), equalTo(1));
        assertThat("Таблицы ввода и вывода равны", builder.getOutputTables().get(0),
                equalTo(nextBuilder.getInputTables().get(0).getTable()));
    }

    @Test
    public void testBuildSingle() {
        OperationSpec expectedSpec = mock(OperationSpec.class);
        AppendableSpecBuilder builder = new TestSpecBuilder(expectedSpec)
                .addInputTable(inputTable, inputTableRow)
                .addOutputTable(outputTable);
        OperationSpec spec = builder.build();

        assertThat("Вернули незавернутый ожидаемый спек", spec, equalTo(expectedSpec));
    }

    @Test
    public void testBuildDouble() {
        OperationSpec expectedSpecOne = mock(OperationSpec.class);
        AppendableSpecBuilder builder = new TestSpecBuilder(expectedSpecOne)
                .addInputTable(inputTable, inputTableRow);

        OperationSpec expectedSpecTwo = mock(OperationSpec.class);
        AppendableSpecBuilder nextBuilder = new TestSpecBuilder(expectedSpecTwo)
                .addOutputTable(outputTable);
        builder.append(nextBuilder);

        OperationSpec spec = builder.build();

        assertThat("Вернули транзакционный спек", spec, instanceOf(TransactionalOperationSpec.class));

        TransactionalOperationSpec trSpec = (TransactionalOperationSpec) spec;
        assertThat("Количество задач в транзакции верно", trSpec.getOperationSpecs().size(),
                equalTo(3)); // две наши + удаление временной таблицы
        assertThat("Задачи идут в ожидаемом порядке", trSpec.getOperationSpecs().get(0), equalTo(expectedSpecOne));
        assertThat("Задачи идут в ожидаемом порядке", trSpec.getOperationSpecs().get(1), equalTo(expectedSpecTwo));
        assertThat("Задачи идут в ожидаемом порядке", trSpec.getOperationSpecs().get(2),
                instanceOf(DeleteNodeOperationSpec.class));
    }

    @Test
    public void testBuildThree() {
        OperationSpec expectedSpecOne = mock(OperationSpec.class);
        AppendableSpecBuilder builder = new TestSpecBuilder(expectedSpecOne)
                .addInputTable(inputTable, inputTableRow);

        OperationSpec expectedSpecTwo = mock(OperationSpec.class);
        AppendableSpecBuilder nextBuilder = new TestSpecBuilder(expectedSpecTwo);
        builder.append(nextBuilder);

        OperationSpec expectedSpecThree = mock(OperationSpec.class);
        AppendableSpecBuilder lastBuilder = new TestSpecBuilder(expectedSpecThree)
                .addOutputTable(outputTable);
        builder.append(lastBuilder);

        OperationSpec spec = builder.build();

        assertThat("Вернули транзакционный спек", spec, instanceOf(TransactionalOperationSpec.class));

        TransactionalOperationSpec trSpec = (TransactionalOperationSpec) spec;
        assertThat("Количество задач в транзакции верно", trSpec.getOperationSpecs().size(),
                equalTo(5)); // три наши + удаление временных таблиц
        assertThat("Задачи идут в ожидаемом порядке", trSpec.getOperationSpecs().get(0), equalTo(expectedSpecOne));
        assertThat("Задачи идут в ожидаемом порядке", trSpec.getOperationSpecs().get(1), equalTo(expectedSpecTwo));
        assertThat("Задачи идут в ожидаемом порядке", trSpec.getOperationSpecs().get(2), equalTo(expectedSpecThree));
        assertThat("Задачи идут в ожидаемом порядке", trSpec.getOperationSpecs().get(3),
                instanceOf(DeleteNodeOperationSpec.class));
        assertThat("Задачи идут в ожидаемом порядке", trSpec.getOperationSpecs().get(4),
                instanceOf(DeleteNodeOperationSpec.class));
    }
}
