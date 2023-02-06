package ru.yandex.market.ff.grid.validation.rule.concrete;

import java.util.stream.Stream;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.model.cell.GridCell;
import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kotovdv 01/08/2017.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ValidationRuleTest {


    protected abstract Stream<Arguments> params();

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("params")
    void testRuleEvaluation(GridCell cell, boolean expectedResult, ColumnValidationRule rule) {
        assertThat(rule.applyToSingleCell(cell).isValid())
            .describedAs("Rule [" + rule.getClass().getSimpleName() + " evaluation test")
            .isEqualTo(expectedResult);
    }

    protected static DefaultGridCell createCell(String value) {
        return new DefaultGridCell(0, 0, value);
    }

}
