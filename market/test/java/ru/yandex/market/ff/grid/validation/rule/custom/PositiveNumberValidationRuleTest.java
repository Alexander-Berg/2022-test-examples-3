package ru.yandex.market.ff.grid.validation.rule.custom;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.validation.rule.concrete.ValidationRuleTest;

/**
 * Unit тесты для {@link PositiveNumberValidationRule}.
 *
 * @author avetokhin 13/12/17.
 */
public class PositiveNumberValidationRuleTest extends ValidationRuleTest {
    private static PositiveNumberValidationRule createRule(boolean isMandatory) {
        return new PositiveNumberValidationRule(isMandatory, "", "");
    }

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(createCell("1"), true, createRule(true)),
                Arguments.of(createCell("1"), true, createRule(false)),

                Arguments.of(createCell("1.123"), true, createRule(true)),
                Arguments.of(createCell("1,123"), true, createRule(false)),

                Arguments.of(createCell("13123123123121.123"), true, createRule(true)),
                Arguments.of(createCell("34435432423421,123"), true, createRule(false)),

                Arguments.of(createCell("-1"), false, createRule(true)),
                Arguments.of(createCell("-1"), false, createRule(false)),

                Arguments.of(createCell("-1.123"), false, createRule(true)),
                Arguments.of(createCell("-1,123"), false, createRule(false)),

                Arguments.of(createCell("-13123123123121.123"), false, createRule(true)),
                Arguments.of(createCell("-34435432423421,123"), false, createRule(false)),

                Arguments.of(createCell("-0"), false, createRule(true)),
                Arguments.of(createCell("0"), false, createRule(false)),

                Arguments.of(createCell(""), false, createRule(true)),
                Arguments.of(createCell(""), true, createRule(false)),
                Arguments.of(createCell(null), false, createRule(true)),
                Arguments.of(createCell(null), true, createRule(false))
        );
    }
}
