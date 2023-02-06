package ru.yandex.direct.internaltools.core.input;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.MultipleSelect;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на парсинг описаний полей с выбором нескольких элементов
 */
public class InternalToolInputMultipleSelectTest {

    private static final String TEST_LABEL = "Test label";

    private static final String ENUM_CHOICE_ONE = "ONE"; //TestEnum.ONE
    private static final String ENUM_CHOICE_THREE = "THREE"; //TestEnum.THREE
    private static final String STRING_CHOICE_ONE = "test1";
    private static final String STRING_CHOICE_TWO = "test2";
    private static final String LONG_CHOICE_ONE = "10";
    private static final String LONG_CHOICE_TWO = "20";

    public enum TestEnum {
        ONE,
        TWO,
        THREE
    }

    public static class TestClass extends InternalToolParameter {

        @MultipleSelect(defaultValues = {ENUM_CHOICE_ONE, ENUM_CHOICE_THREE})
        @Input(label = TEST_LABEL)
        public Set<TestEnum> enumMultipleSelect;

        @MultipleSelect
        @Input(label = TEST_LABEL)
        public Set<TestEnum> enumMultipleSelectWithoutDefaultValues;

        @MultipleSelect(choices = {STRING_CHOICE_ONE, STRING_CHOICE_TWO}, defaultValues = STRING_CHOICE_TWO)
        @Input(label = TEST_LABEL)
        public Set<String> stringMultipleSelect;

        @MultipleSelect(choices = {LONG_CHOICE_ONE, LONG_CHOICE_TWO},
                defaultValues = {LONG_CHOICE_ONE, LONG_CHOICE_TWO})
        @Input(label = TEST_LABEL)
        public Set<Long> longMultipleSelect;

        @MultipleSelect(preprocessed = true)
        @Input(label = TEST_LABEL)
        public Set<String> multipleSelectWithPreprocessed;

    }

    private SoftAssertions getCommonSoftAssertions(InternalToolInput<TestClass, ?> input) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.MULTIPLE_SELECT)
                .as("Тип контрола корректен");
        soft.assertThat(input.getArgs())
                .isEmpty();
        return soft;
    }

    /**
     * Проверяем, что умеем работать с аннотацией MultipleSelect для Set<Enum>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testMultipleSelectAnnotationForEnum() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "enumMultipleSelect");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = getCommonSoftAssertions(input);

        soft.assertThat((String[]) input.getDefaultValue())
                .containsExactly(ENUM_CHOICE_ONE, ENUM_CHOICE_THREE);
        soft.assertThat((List<TestEnum>) input.getAllowedValues())
                .containsExactly(TestEnum.values());
        soft.assertThat(input.getValidators())
                .isNotEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с аннотацией MultipleSelect для Set<Enum>'а без дефолтных значений
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testMultipleSelectAnnotationWithoutDefaultValues() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "enumMultipleSelectWithoutDefaultValues");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = getCommonSoftAssertions(input);

        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat((List<TestEnum>) input.getAllowedValues())
                .containsExactly(TestEnum.values());
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с аннотацией MultipleSelect для Set<String>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testMultipleSelectAnnotationForString() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "stringMultipleSelect");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = getCommonSoftAssertions(input);

        soft.assertThat((String[]) input.getDefaultValue())
                .containsExactly(STRING_CHOICE_TWO);
        soft.assertThat((List<String>) input.getAllowedValues())
                .containsExactly(STRING_CHOICE_ONE, STRING_CHOICE_TWO);
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с аннотацией MultipleSelect c preprocessed=true
     */
    @Test
    public void testMultipleSelectAnnotationWithPreprocessed() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "multipleSelectWithPreprocessed");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = getCommonSoftAssertions(input);

        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getAllowedValues())
                .isNull();
        soft.assertThat(input.getValidators())
                .isEmpty();
        soft.assertAll();
    }
}
