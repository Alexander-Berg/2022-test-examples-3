package ru.yandex.market.ff.grid.validation.rule.concrete;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;

/**
 * @author kotovdv 01/08/2017.
 */
public class IsIntegerValidationRuleTest extends ValidationRuleTest {

    private final ColumnValidationRule rule = new IsIntegerValidationRule(false, "");

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(new DefaultGridCell(0, 0, null), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, ""), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, " "), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "0"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1.0"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1.1"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1,0"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1,1"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "sdas"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "true"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "false"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "-2147483648"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "-2147483649"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "2147483647"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "2147483648"), false, rule)
        );
    }
}
