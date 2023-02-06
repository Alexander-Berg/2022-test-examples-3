package ru.yandex.market.ff.grid.validation.rule.concrete;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;

/**
 * Unit тесты для  {@link IsBooleanValidationRuleTest}.
 *
 * @author avetokhin 15/08/2017.
 */
public class IsBooleanValidationRuleTest extends ValidationRuleTest {

    private final ColumnValidationRule rule = new IsBooleanValidationRule(false, "");

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(new DefaultGridCell(0, 0, null), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, ""), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "1"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "0"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "YeS"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "no"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "да"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "нЕт"), true, rule),
                Arguments.of(new DefaultGridCell(0, 0, "12345"), false, rule),
                Arguments.of(new DefaultGridCell(0, 0, "123456"), false, rule)
        );
    }
}
