package ru.yandex.direct.internaltools.core.input;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.Select;
import ru.yandex.direct.internaltools.core.annotations.input.ShardSelect;
import ru.yandex.direct.internaltools.core.annotations.input.Text;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.validation.builder.ItemValidationBuilder;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на базовую валидации сгенерированных полей
 */
public class InternalToolInputValidationTest {
    private enum TestEnum {
        ONE,
        TWO;
    }

    public static class TestClass extends InternalToolParameter {
        @Input(label = "not required/no value", required = false)
        public String nrnv;
        @Input(label = "required/no value")
        public Long rnv;
        @Input(label = "not required/value", required = false)
        public LocalDateTime nrv = LocalDateTime.now();
        @Input(label = "required/value empty")
        public String rve = "";
        @Input(label = "required/value")
        public LocalDate rv = LocalDate.now();
        @Input(label = "string/max size exceeded", required = false)
        @Text(valueMaxLen = 10)
        private String longString = "123456789012345";
        @Input(label = "string/max size")
        @Text(valueMaxLen = 10)
        private String correctString = "1234567890";
        @Input(label = "enum", required = false)
        public TestEnum enumValue;
        @Select(choices = {"ONE", "TWO"})
        @Input(label = "select", required = false)
        public String select = "THREE";
        @Select(choices = {"ONE", "TWO"})
        @Input(label = "select", required = false)
        public String selectCorrect = "TWO";
        @ShardSelect
        @Input(label = "shard select")
        public Integer shardSelect = -1;
        @ShardSelect
        @Input(label = "shard select")
        public Integer shardSelectCorrect = 1;

        public String getLongString() {
            return longString;
        }

        public String getCorrectString() {
            return correctString;
        }
    }

    private ItemValidationBuilder<TestClass, Defect> validationBuilder;
    private TestClass param;

    @Before
    public void before() {
        param = new TestClass();
        validationBuilder = ItemValidationBuilder.of(param);
    }

    /**
     * Проверяем, что необязательно поле без значения успешно валидируется
     */
    @Test
    public void testNotRequiredNoValueNoError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "nrnv");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }

    /**
     * Проверяем, что обязательно поле без значения валидируется с ошибками
     */
    @Test
    public void testRequiredNoValueError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "rnv");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isTrue();
    }

    /**
     * Проверяем, что необязательно поле со значением успешно валидируется
     */
    @Test
    public void testNotRequiredValueNoError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "nrv");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }

    /**
     * Проверяем, что обязательное поле с пустым значением валидируется с ошибками
     */
    @Test
    public void testRequiredValueEmptyError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "rve");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isTrue();
    }

    /**
     * Проверяем, что обязательное поле со значением успешно валидируется
     */
    @Test
    public void testRequiredValueNoError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "rv");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }

    /**
     * Проверяем, что ограничение длины строки работает
     */
    @Test
    public void testStringMaxSizeError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "longString");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isTrue();
    }

    /**
     * Проверяем, что ограничение длины строки работает
     */
    @Test
    public void testStringMaxSize() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "correctString");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }

    /**
     * Проверяем, что пустой необязательный енам работает
     */
    @Test
    public void testEmptyEnum() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "enumValue");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }

    /**
     * Проверяем, что селект строк валидирует входящие значения
     */
    @Test
    public void testSelectStringError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "select");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isTrue();
    }

    /**
     * Проверяем, что селект строк валидирует входящие значения
     */
    @Test
    public void testSelectStringNoError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "selectCorrect");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }

    /**
     * Проверяем, что селект строк валидирует входящие значения
     */
    @Test
    public void testShardSelectError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "shardSelect");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isTrue();
    }

    /**
     * Проверяем, что селект строк валидирует входящие значения
     */
    @Test
    public void testShardSelectNoError() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "shardSelectCorrect");
        assertThat(input).isNotNull();

        input.addValidation(validationBuilder, param);
        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }
}
