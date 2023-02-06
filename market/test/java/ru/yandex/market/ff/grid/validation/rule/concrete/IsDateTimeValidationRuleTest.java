package ru.yandex.market.ff.grid.validation.rule.concrete;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;

public class IsDateTimeValidationRuleTest extends ValidationRuleTest {

    private final ColumnValidationRule rule = new IsDateTimeValidationRule(false, "", "");

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(new DefaultGridCell(0, 0, null), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, ""), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "   "), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "2021-09-13 12:00:00"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "true"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "false"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "123456"), false, rule)
        );
    }

}
