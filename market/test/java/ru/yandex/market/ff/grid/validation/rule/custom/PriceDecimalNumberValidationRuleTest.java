package ru.yandex.market.ff.grid.validation.rule.custom;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.validation.rule.concrete.ValidationRuleTest;

/**
 * Unit тесты для {@link PriceDecimalNumberValidationRule}.
 *
 * @author avetokhin 13/12/17.
 */
public class PriceDecimalNumberValidationRuleTest extends ValidationRuleTest {
    private static PriceDecimalNumberValidationRule createRule(boolean isMandatory) {
        return new PriceDecimalNumberValidationRule(isMandatory, "", "");
    }

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(createCell("1"), true, createRule(true)),
                Arguments.of(createCell("1"), true, createRule(false)),

                Arguments.of(createCell("1.12"), true, createRule(true)),
                Arguments.of(createCell("1,12"), true, createRule(false)),

                Arguments.of(createCell("13123123123121.12"), true, createRule(true)),
                Arguments.of(createCell("34435432423421,12"), true, createRule(false)),

                Arguments.of(createCell("-1"), true, createRule(true)),
                Arguments.of(createCell("-1"), true, createRule(false)),

                Arguments.of(createCell("-1.000"), true, createRule(true)),
                Arguments.of(createCell("-1,000"), true, createRule(false)),

                Arguments.of(createCell("-13123123123121.12"), true, createRule(true)),
                Arguments.of(createCell("-34435432423421,12"), true, createRule(false)),

                Arguments.of(createCell("-0"), true, createRule(true)),
                Arguments.of(createCell("0"), true, createRule(false)),

                Arguments.of(createCell(""), false, createRule(true)),
                Arguments.of(createCell(""), true, createRule(false)),
                Arguments.of(createCell(null), false, createRule(true)),
                Arguments.of(createCell(null), true, createRule(false)),

                Arguments.of(createCell("1.121"), false, createRule(true)),
                Arguments.of(createCell("1,122"), false, createRule(false)),
                Arguments.of(createCell("-13123123123121.122"), false, createRule(true)),
                Arguments.of(createCell("-34435432423421,122"), false, createRule(false))
        );
    }
}
