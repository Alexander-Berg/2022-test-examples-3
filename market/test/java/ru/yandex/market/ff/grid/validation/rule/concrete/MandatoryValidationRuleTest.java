package ru.yandex.market.ff.grid.validation.rule.concrete;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;

/**
 * @author kotovdv 01/08/2017.
 */
public class MandatoryValidationRuleTest extends ValidationRuleTest {

    private final ColumnValidationRule rule = new IsStringValidationRule(true, "Violation message", "", 10);

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(new DefaultGridCell(0, 0, null), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, ""), false, rule)
        );
    }
}
