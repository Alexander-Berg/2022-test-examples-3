package ru.yandex.market.ff.grid.validation.rule.custom;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.validation.rule.concrete.ValidationRuleTest;

/**
 * Unit тесты для {@link BarcodesValidationRule}.
 *
 * @author avetokhin 20/09/17.
 */
public class BarcodesValidationRuleTest extends ValidationRuleTest {

    private static BarcodesValidationRule createRule(boolean isMandatory) {
        return new BarcodesValidationRule(isMandatory, 3, "", "", "");
    }

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(createCell("1"), true, createRule(true)),
                Arguments.of(createCell("123ABCDE"), true, createRule(true)),
                Arguments.of(createCell("''123ABCDE"), true, createRule(true)),
                Arguments.of(createCell("''123'\\ABC\\\\DE"), true, createRule(true)),
                Arguments.of(createCell("1,2,3"), true, createRule(true)),
                Arguments.of(createCell("1,2,abcd"), true, createRule(true)),
                Arguments.of(createCell("1234,12-a"), true, createRule(true)),
                Arguments.of(createCell("1234,12_a"), false, createRule(true)),
                Arguments.of(createCell(",1,2"), false, createRule(true)),
                Arguments.of(createCell(",1,2,"), false, createRule(true)),
                Arguments.of(createCell(" , "), false, createRule(true)),
                Arguments.of(createCell(" , ,"), false, createRule(true)),
                Arguments.of(createCell("123, 1234"), true, createRule(true)),
                Arguments.of(createCell("~"), true, createRule(true)),
                Arguments.of(createCell(""), false, createRule(true)),
                Arguments.of(createCell("DMN83/BBD43"), true, createRule(true)),
                Arguments.of(createCell("!\"#$%&'()*+,-./:<=>?@[\\]^`{|}~"), true, createRule(true)),
                Arguments.of(createCell("АБС"), false, createRule(true)),
                Arguments.of(createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaksjdalks" +
                    "djlakdjs9203842903840923jakjsdlkajsdlakjsdlkasjd29342901"), true, createRule(true)),
                Arguments.of(createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaksjdalks" +
                    "djlakdjs9203842903840923jakjsdlkajsdlakjsdlkasjd29342901,1"), true, createRule(true)),
                Arguments.of(createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaks" +
                    "jdalksdjlakdjs9203842903840923jakjsdlkajsdlakjsdlkasjd293429012"), false, createRule(true)),
                Arguments.of(createCell("1,2,3,4"), false, createRule(true)),

                Arguments.of(createCell("1"), true, createRule(false)),
                Arguments.of(createCell("123ABCDE"), true, createRule(false)),
                Arguments.of(createCell("''123ABCDE"), true, createRule(false)),
                Arguments.of(createCell("''123'\\ABC\\\\DE"), true, createRule(false)),
                Arguments.of(createCell("1,2,3"), true, createRule(false)),
                Arguments.of(createCell("1,2,abcd"), true, createRule(false)),
                Arguments.of(createCell("1234,12-a"), true, createRule(false)),
                Arguments.of(createCell("1234,12_a"), false, createRule(false)),
                Arguments.of(createCell(",1,2"), false, createRule(false)),
                Arguments.of(createCell(",1,2,"), false, createRule(false)),
                Arguments.of(createCell(" , "), false, createRule(false)),
                Arguments.of(createCell(" , ,"), false, createRule(false)),
                Arguments.of(createCell("123, 1234"), true, createRule(false)),
                Arguments.of(createCell("~"), true, createRule(false)),
                Arguments.of(createCell(""), true, createRule(false)),
                Arguments.of(createCell("DMN83/BBD43"), true, createRule(false)),
                Arguments.of(createCell("!\"#$%&'()*+,-./:<=>?@[\\]^`{|}~"), true, createRule(false)),
                Arguments.of(createCell("АБС"), false, createRule(false)),
                Arguments.of(createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaksjdalks" +
                    "djlakdjs9203842903840923jakjsdlkajsdlakjsdlkasjd29342901"), true, createRule(false)),
                Arguments.of(createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaksjdalks" +
                    "djlakdjs9203842903840923jakjsdlkajsdlakjsdlkasjd29342901,1"), true, createRule(false)),
                Arguments.of(createCell("12398sdhasdoajsdajsd9038230498kjalksdjalskdj902384230948klasjdlaksjdalks" +
                    "djlakdjs9203842903840923jakjsdlkajsdlakjsdlkasjd293429012"), false, createRule(false)),
                Arguments.of(createCell("1,2,3,4"), false, createRule(false)),

                Arguments.of(createCell(null), true, createRule(false)),
                Arguments.of(createCell(""), false, createRule(true))
        );
    }
}
