package ru.yandex.market.ff.grid.validation.rule.custom;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.validation.rule.concrete.ValidationRuleTest;

/**
 * Unit тесты для {@link SkuValidationRule}.
 *
 * @author avetokhin 20/09/17.
 */
public class SkuValidationRuleTest extends ValidationRuleTest {
    private static SkuValidationRule createRule(boolean isMandatory) {
        return new SkuValidationRule(isMandatory, "", "");
    }

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(createCell("1"), true, createRule(true)),
                Arguments.of(createCell("123ABCDE"), true, createRule(true)),
                Arguments.of(createCell("12-a-b-.3"), true, createRule(true)),
                Arguments.of(createCell("~"), false, createRule(true)),
                Arguments.of(createCell("@"), false, createRule(true)),
                Arguments.of(createCell("ё"), false, createRule(true)),
                Arguments.of(createCell("12_a"), false, createRule(true)),
                Arguments.of(createCell("1;2"), false, createRule(true)),
                Arguments.of(createCell(" ; "), false, createRule(true)),
                Arguments.of(createCell(" ; ;"), false, createRule(true)),
                Arguments.of(createCell("123; 1234"), false, createRule(true)),
                Arguments.of(createCell(""), false, createRule(true)),
                Arguments.of(createCell("DMN83/BBD43"), true, createRule(true)),
                Arguments.of(createCell(".,\\/()[]"), true, createRule(true)),
                Arguments.of(createCell("АБс.YY-"), true, createRule(true)),
                Arguments.of(
                    createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaksjdalksdjlakdjk"),
                    true, createRule(true)),
                Arguments.of(
                    createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaksjdalksdjlakdjk1"),
                    false, createRule(true)),

                Arguments.of(createCell("1"), true, createRule(false)),
                Arguments.of(createCell("123ABCDE"), true, createRule(false)),
                Arguments.of(createCell("12-a-b-.3"), true, createRule(false)),
                Arguments.of(createCell("~"), false, createRule(false)),
                Arguments.of(createCell("@"), false, createRule(false)),
                Arguments.of(createCell("ё"), false, createRule(false)),
                Arguments.of(createCell("12-a"), true, createRule(false)),
                Arguments.of(createCell("1;2"), false, createRule(false)),
                Arguments.of(createCell(" ; "), false, createRule(false)),
                Arguments.of(createCell(" ; ;"), false, createRule(false)),
                Arguments.of(createCell("123; 1234"), false, createRule(false)),
                Arguments.of(createCell(""), true, createRule(false)),
                Arguments.of(createCell("DMN83/BBD43"), true, createRule(false)),
                Arguments.of(createCell(".,\\/()[]"), true, createRule(false)),
                Arguments.of(createCell("АБс.YY-"), true, createRule(false)),
                Arguments.of(
                    createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaksjdalksdjlakdjk"),
                    true, createRule(false)),
                Arguments.of(
                    createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaksjdalksdjlakdjk1"),
                    false, createRule(false)),

                Arguments.of(createCell(null), true, createRule(false)),
                Arguments.of(createCell(""), false, createRule(true))
        );
    }
}
