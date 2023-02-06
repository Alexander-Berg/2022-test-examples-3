package ru.yandex.market.ff.grid.validation.rule.provider.database.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsDateTimeValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsFloatingPointValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsIntegerValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsStringValidationRule;
import ru.yandex.market.ff.i18n.TemplateValidationMessages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author kotovdv 01/08/2017.
 */
class DataTypeTest {

    private final TemplateValidationMessages messages = mock(TemplateValidationMessages.class);

    private static Collection<Object[]> params() {
        Collection<Object[]> parameters = new ArrayList<>();

        addScenario(parameters, "Column1", "INTEGER", DataTypeTest::validateInteger);
        addScenario(parameters, "Column1", "integer", DataTypeTest::validateInteger);
        addScenario(parameters, "Column1", "integer   ", DataTypeTest::validateInteger);
        addScenario(parameters, "Column1", "   integer   ", DataTypeTest::validateInteger);

        addScenario(parameters, "Column2", "FLOAT", DataTypeTest::validateFloat);
        addScenario(parameters, "Column2", "float", DataTypeTest::validateFloat);
        addScenario(parameters, "Column2", "float   ", DataTypeTest::validateFloat);
        addScenario(parameters, "Column2", "   float   ", DataTypeTest::validateFloat);

        addScenario(parameters, "Column3", "STRING[10]", rule -> validateString(rule, 10));
        addScenario(parameters, "Column3", "string[10]", rule -> validateString(rule, 10));
        addScenario(parameters, "Column3", "string[10]   ",
                rule -> validateString(rule, 10));
        addScenario(parameters, "Column3", "   string[10]   ",
                rule -> validateString(rule, 10));
        addScenario(parameters, "Column4", "   DATE_TIME   ",
                DataTypeTest::validateDateTime);

        return parameters;
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("params")
    void testPlainDataTypeTransformation(String columnName, String dataType,
                                         Function<ColumnValidationRule, Boolean> validator) {
        ColumnValidationRule rule = DataType.createRuleFrom(columnName, dataType, true, messages);

        assertThat(validator.apply(rule))
                .overridingErrorMessage("Failed to transform [" + dataType + "] into valid rule")
                .isTrue();
    }

    private static void addScenario(Collection<Object[]> parameters,
                                    String columnName,
                                    String dataType,
                                    Function<? extends ColumnValidationRule, Boolean> validator) {
        parameters.add(new Object[]{columnName, dataType, validator});
    }


    private static boolean validateInteger(ColumnValidationRule rule) {
        return IsIntegerValidationRule.class.isAssignableFrom(rule.getClass());
    }

    private static boolean validateFloat(ColumnValidationRule rule) {
        return IsFloatingPointValidationRule.class.isAssignableFrom(rule.getClass());
    }

    private static boolean validateString(ColumnValidationRule rule, int maxLength) {
        return IsStringValidationRule.class.isAssignableFrom(rule.getClass()) &&
                IsStringValidationRule.class.cast(rule).getMaxLength() == maxLength;
    }

    private static boolean validateDateTime(ColumnValidationRule rule) {
        return IsDateTimeValidationRule.class.isAssignableFrom(rule.getClass());
    }

}
