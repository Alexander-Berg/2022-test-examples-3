package ru.yandex.direct.internaltools.core.input;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.ShardSelect;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на парсинг описаний полей для ввода шардов
 */
public class InternalToolInputShardSelectTest {
    private static final String TEST_LABEL = "Some date";

    public static class TestClass extends InternalToolParameter {
        @ShardSelect
        @Input(label = TEST_LABEL)
        public Integer one;

        @ShardSelect
        @Input(label = TEST_LABEL)
        public String notInt;
    }

    /**
     * Проверяем, что умеем работать с аннотацией ShardSelect
     */
    @Test
    public void testShardSelect() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "one");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.SELECT)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(1);
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertThat((List<Integer>) input.getAllowedValues())
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем аннотация ShardSelect работает только с интами
     */
    @Test
    public void testShardSelectOnString() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "notInt");

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
