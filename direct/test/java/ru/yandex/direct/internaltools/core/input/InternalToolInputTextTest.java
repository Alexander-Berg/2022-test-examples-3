package ru.yandex.direct.internaltools.core.input;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.Text;
import ru.yandex.direct.internaltools.core.annotations.input.TextArea;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.direct.internaltools.core.bootstrap.InternalToolInputBootstrap.COLUMNS_ARG;
import static ru.yandex.direct.internaltools.core.bootstrap.InternalToolInputBootstrap.FIELD_LEN_ARG;
import static ru.yandex.direct.internaltools.core.bootstrap.InternalToolInputBootstrap.MAX_LEN_ARG;
import static ru.yandex.direct.internaltools.core.bootstrap.InternalToolInputBootstrap.ROWS_ARG;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на парсинг описаний полей со строковыми значениями
 */
public class InternalToolInputTextTest {
    private static final String TEST_LABEL = "Some date";
    private static final String TEST_VALUE = "1234";
    private static final int TEST_NUM_ONE = 50;
    private static final int TEST_NUM_TWO = 150;

    public static class TestClass extends InternalToolParameter {
        @Input(label = TEST_LABEL)
        public String one;

        @Text(defaultValue = TEST_VALUE, fieldLen = TEST_NUM_ONE, valueMaxLen = TEST_NUM_TWO)
        @Input(label = TEST_LABEL)
        public String two;

        @TextArea(defaultValue = TEST_VALUE, columns = TEST_NUM_ONE, rows = TEST_NUM_TWO)
        @Input(label = TEST_LABEL)
        public String three;

        @Text(defaultValue = TEST_VALUE)
        @TextArea(defaultValue = TEST_VALUE)
        @Input(label = TEST_LABEL)
        public Long notString;
    }

    /**
     * Проверяем, что умеем работать со строкой без аннотации Text
     */
    @Test
    public void testStringWithoutAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "one");

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

    /**
     * Проверяем, что умеем работать со строкой с аннотацией Text
     */
    @Test
    public void testStringWithTextAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "two");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.TEXT)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(String.valueOf(TEST_VALUE));
        soft.assertThat(input.getArgs())
                .containsOnly(entry(FIELD_LEN_ARG, TEST_NUM_ONE), entry(MAX_LEN_ARG, TEST_NUM_TWO));
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать со строкой с аннотацией Text
     */
    @Test
    public void testStringWithTextAreaAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "three");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.TEXTAREA)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(String.valueOf(TEST_VALUE));
        soft.assertThat(input.getArgs())
                .containsOnly(entry(COLUMNS_ARG, TEST_NUM_ONE), entry(ROWS_ARG, TEST_NUM_TWO));
        soft.assertAll();
    }

    /**
     * Проверяем, что не обращаем внимания на аннотации Text и TextArea, если они не на строке
     */
    @Test
    public void testNotStringWithAnnotation() throws NoSuchFieldException {
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
