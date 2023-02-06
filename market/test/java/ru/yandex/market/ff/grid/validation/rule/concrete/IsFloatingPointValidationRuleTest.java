package ru.yandex.market.ff.grid.validation.rule.concrete;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;

/**
 * @author kotovdv 01/08/2017.
 */
public class IsFloatingPointValidationRuleTest extends ValidationRuleTest {

    private final ColumnValidationRule rule = new IsFloatingPointValidationRule(false, "");

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(new DefaultGridCell(0, 0, null), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1.0"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1.1"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1,0"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1,1"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1-1"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "a1"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "-A"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "5+3"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "true"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "false"), false, rule)
        );
    }
}
