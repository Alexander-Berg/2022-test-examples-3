package ru.yandex.market.ff.grid.validation.rule.custom;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.validation.rule.concrete.ValidationRuleTest;

/**
 * Unit тесты для {@link NameValidationRule}.
 *
 * @author avetokhin 20/09/17.
 */
@SuppressWarnings("AvoidEscapedUnicodeCharacters")
public class NameValidationRuleTest extends ValidationRuleTest {

    private static NameValidationRule createRule(boolean isMandatory) {
        return new NameValidationRule(isMandatory, "", "");
    }

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(createCell("1"), true, createRule(true)),
                Arguments.of(createCell("123ABCDE"), true, createRule(true)),
                Arguments.of(createCell("\n\r\t!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"), true, createRule(true)),
                Arguments.of(createCell("СловоЗаСловоЁё"), true, createRule(true)),

                Arguments.of(createCell("1"), true, createRule(false)),
                Arguments.of(createCell("123ABCDE"), true, createRule(false)),
                Arguments.of(createCell("\n\r\t!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"), true, createRule(false)),
                Arguments.of(createCell("СловоЗаСловоЁё"), true, createRule(false)),

                Arguments.of(createCell(null), false, createRule(true)),
                Arguments.of(createCell(null), true, createRule(false)),
                Arguments.of(createCell(""), false, createRule(true)),
                Arguments.of(createCell(""), true, createRule(false)),

                Arguments.of(createCell("a\u0000b"), false, createRule(false)),
                Arguments.of(createCell("a\u0000b"), false, createRule(true)),
                Arguments.of(createCell("a\u0007b"), false, createRule(false)),
                Arguments.of(createCell("a\u0007b"), false, createRule(true)),
                Arguments.of(createCell("a\u0012b"), false, createRule(false)),
                Arguments.of(createCell("a\u0012b"), false, createRule(true)),
                Arguments.of(createCell("a\u007Fb"), false, createRule(false)),
                Arguments.of(createCell("a\u007Fb"), false, createRule(true))
        );
    }
}
