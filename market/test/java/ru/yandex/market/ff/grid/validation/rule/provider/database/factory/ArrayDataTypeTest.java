package ru.yandex.market.ff.grid.validation.rule.provider.database.factory;

import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsArrayValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsFloatingPointValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsIntegerValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsStringValidationRule;
import ru.yandex.market.ff.i18n.TemplateValidationMessages;
import ru.yandex.market.ff.model.entity.DocumentTemplateColumn;

import static org.mockito.Mockito.mock;

/**
 * @author kotovdv 02/08/2017.
 */
class ArrayDataTypeTest {

    private final ColumnValidationRuleFactory ruleFactory =
            new ColumnValidationRuleFactory(mock(TemplateValidationMessages.class));

    private SoftAssertions softly;

    private static Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(createColumn("ARRAY[INTEGER][;]"), IsIntegerValidationRule.class, ";"),
                Arguments.of(createColumn("array[INTEGER][;]"), IsIntegerValidationRule.class, ";"),
                Arguments.of(createColumn("ARRAY[integer][;]"), IsIntegerValidationRule.class, ";"),
                Arguments.of(createColumn("array[integer][;]"), IsIntegerValidationRule.class, ";"),
                Arguments.of(createColumn("array[integer][;]   "), IsIntegerValidationRule.class, ";"),
                Arguments.of(createColumn("   array[integer][;]   "), IsIntegerValidationRule.class, ";"),

                Arguments.of(createColumn("ARRAY[FLOAT][;]"), IsFloatingPointValidationRule.class, ";"),
                Arguments.of(createColumn("array[FLOAT][;]"), IsFloatingPointValidationRule.class, ";"),
                Arguments.of(createColumn("ARRAY[float][;]"), IsFloatingPointValidationRule.class, ";"),
                Arguments.of(createColumn("array[float][;]"), IsFloatingPointValidationRule.class, ";"),
                Arguments.of(createColumn("array[float][;]   "), IsFloatingPointValidationRule.class, ";"),
                Arguments.of(createColumn("   array[float][;]   "), IsFloatingPointValidationRule.class, ";"),

                Arguments.of(createColumn("ARRAY[STRING[10]][;]"), IsStringValidationRule.class, ";"),
                Arguments.of(createColumn("array[STRING[10]][;]"), IsStringValidationRule.class, ";"),
                Arguments.of(createColumn("ARRAY[string[10]][;]"), IsStringValidationRule.class, ";"),
                Arguments.of(createColumn("array[string[10]][;]"), IsStringValidationRule.class, ";"),
                Arguments.of(createColumn("array[string[10]][;]    "), IsStringValidationRule.class, ";"),
                Arguments.of(createColumn("   array[string[10]][;]    "), IsStringValidationRule.class, ";"),

                Arguments.of(createColumn("ARRAY[INTEGER][,]"), IsIntegerValidationRule.class, ","),
                Arguments.of(createColumn("ARRAY[INTEGER][|]"), IsIntegerValidationRule.class, "|"),
                Arguments.of(createColumn("ARRAY[INTEGER][DELIM]"), IsIntegerValidationRule.class, "DELIM")
        );
    }

    @BeforeEach
    void beforeTest() {
        this.softly = new SoftAssertions();
    }

    @AfterEach
    void afterTest() {
        this.softly.assertAll();
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("params")
    void testArrayValidationRuleCreation(DocumentTemplateColumn templateColumn,
                                         Class<? extends ColumnValidationRule> elementRule,
                                         String delimiter) {
        ColumnValidationRule rule = ruleFactory.create(templateColumn);

        softly.assertThat(IsArrayValidationRule.class.isAssignableFrom(rule.getClass()))
                .as("Asserting that created rule =  [is array rule]")
                .isTrue();

        IsArrayValidationRule arrayValidationRule = IsArrayValidationRule.class.cast(rule);

        softly.assertThat(arrayValidationRule.getDelimiter())
                .as("Asserting array delimiter valid recognition")
                .isEqualTo(delimiter);

        softly.assertThat(arrayValidationRule.getElementValidationRule())
                .as("Asserting class of rule assigned to array elements")
                .isExactlyInstanceOf(elementRule);
    }

    private static DocumentTemplateColumn createColumn(String dataType) {
        return new DocumentTemplateColumn(0, "column", dataType, false, true);
    }
}
