package ru.yandex.direct.internaltools.core.input;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.CheckBox;
import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на парсинг описаний булевых полей
 */
public class InternalToolInputBooleanTest {
    private static final String TEST_LABEL_ONE = "someBoolean";
    private static final String TEST_NAME_ONE = "booleanValueP";
    private static final String TEST_LABEL_TWO = "someBooleanTwo";
    private static final String TEST_NAME_TWO = "booleanValue";
    private static final String TEST_LABEL_THREE = "someBooleanThree";
    private static final String TEST_NAME_THREE = "booleanValueWithCb";

    public static class TestClass extends InternalToolParameter {
        @Input(label = TEST_LABEL_ONE)
        private boolean booleanValueP;

        @Input(label = TEST_LABEL_TWO)
        private Boolean booleanValue;

        @Input(label = TEST_LABEL_THREE)
        @CheckBox(checked = true)
        private Boolean booleanValueWithCb;

        @Input(label = TEST_LABEL_THREE)
        @CheckBox(checked = true)
        public String notBoolean;

        public boolean isBooleanValueP() {
            return booleanValueP;
        }

        public Boolean getBooleanValue() {
            return booleanValue;
        }

        public Boolean getBooleanValueWithCb() {
            return booleanValueWithCb;
        }
    }

    /**
     * Проверяем, что умеем работать с примитивным типом
     */
    @Test
    public void testPrimitiveBoolean() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, TEST_NAME_ONE);

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.CHECKBOX)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с boxed-типом
     */
    @Test
    public void testBoxedBoolean() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, TEST_NAME_TWO);

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.CHECKBOX)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с аннотацией @CheckBox
     */
    @Test
    public void testBooleanWithCheckBoxAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, TEST_NAME_THREE);

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.CHECKBOX)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(true);
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что аннотация @CheckBox не работает на не boolean
     */
    @Test
    public void testStringWithCheckBoxAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "notBoolean");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.TEXT)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }
}
