package ru.yandex.direct.internaltools.core.input;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Hidden;
import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на парсинг описаний спрятанных полей
 */
public class InternalToolInputHiddenTest {
    private static final String TEST_LABEL = "Some date";
    private static final String TEST_VALUE = "value";

    public static class TestClass extends InternalToolParameter {
        @Hidden
        @Input(label = TEST_LABEL)
        public Integer one;

        @Hidden(defaultValue = TEST_VALUE)
        @Input(label = TEST_LABEL)
        public byte two;
    }

    /**
     * Проверяем, что умеем работать c пустой аннотацией Hidden
     */
    @Test
    public void testEmptyAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "one");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.HIDDEN)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать c аннотацией Hidden с заданным значением
     */
    @Test
    public void testNonEmptyAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "two");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.HIDDEN)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(TEST_VALUE);
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }
}
