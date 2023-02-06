package ru.yandex.direct.internaltools.core.input;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.Select;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на парсинг описаний полей с выбором элементов
 */
public class InternalToolInputSelectTest {
    private static final String TEST_LABEL = "Some date";
    private static final String CHOICE_ONE = "test1";
    private static final String CHOICE_TWO = "test2";

    public enum TestEnum {
        ONE,
        TWO,
        THREE,
        ;
    }

    public static class TestClass extends InternalToolParameter {
        @Select(choices = {CHOICE_ONE, CHOICE_TWO}, defaultValue = CHOICE_TWO)
        @Input(label = TEST_LABEL)
        public String one;

        @Input(label = TEST_LABEL)
        public TestEnum two;

        @Select(choices = {CHOICE_ONE, CHOICE_TWO})
        @Input(label = TEST_LABEL)
        public String three;

        @Select(preprocessed = true)
        @Input(label = TEST_LABEL, required = false)
        public String four;

        @Select(choices = {CHOICE_ONE, CHOICE_TWO}, defaultValue = CHOICE_TWO)
        @Input(label = TEST_LABEL)
        public Long notString;
    }

    /**
     * Проверяем, что умеем работать с аннотацией Select
     */
    @Test
    public void testSelectAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "one");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.SELECT)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(CHOICE_TWO);
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertThat((List<String>) input.getAllowedValues())
                .containsExactly(CHOICE_ONE, CHOICE_TWO);
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с аннотацией Select без дефолта
     */
    @Test
    public void testSelectAnnotationNoDefault() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "three");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.SELECT)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(CHOICE_ONE);
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertThat((List<String>) input.getAllowedValues())
                .containsExactly(CHOICE_ONE, CHOICE_TWO);
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с енамами
     */
    @Test
    public void testSelectEnum() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "two");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.SELECT)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(TestEnum.ONE);
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertThat((List<TestEnum>) input.getAllowedValues())
                .containsExactly(TestEnum.ONE, TestEnum.TWO, TestEnum.THREE);
        soft.assertAll();
    }

    /**
     * Проверяем, что аннотация Select работает только со строками
     */
    @Test
    public void testSelectPreprocessed() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "four");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.SELECT)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertThat(input.getAllowedValues())
                .isNull();
        soft.assertThat(input.getValidators())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что аннотация Select работает только со строками
     */
    @Test
    public void testSelectOnLong() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "notString");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.NUMBER)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }
}
