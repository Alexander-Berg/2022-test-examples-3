package ru.yandex.market.ff.grid.validation.rule.concrete;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;

/**
 * @author kotovdv 01/08/2017.
 */
public class IsStringValidationRuleTest extends ValidationRuleTest {

    private final ColumnValidationRule rule = new IsStringValidationRule(false, "", "", 5);

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(new DefaultGridCell(0, 0, null), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, ""), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "   "), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "12345"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1.56"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "true"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "false"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "123456"), false, rule)
        );
    }
}
