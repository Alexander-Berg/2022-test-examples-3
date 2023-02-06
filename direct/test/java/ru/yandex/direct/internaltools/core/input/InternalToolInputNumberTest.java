package ru.yandex.direct.internaltools.core.input;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.Number;
import ru.yandex.direct.internaltools.core.annotations.input.NumericId;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.exception.InternalToolInitialisationException;
import ru.yandex.direct.validation.builder.ItemValidationBuilder;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.direct.internaltools.core.bootstrap.InternalToolInputBootstrap.MAX_NUM_VALUE;
import static ru.yandex.direct.internaltools.core.bootstrap.InternalToolInputBootstrap.MIN_NUM_VALUE;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на парсинг описаний полей с численными значениями
 */
public class InternalToolInputNumberTest {
    private static final String TEST_LABEL = "Some date";
    private static final long TEST_VALUE = 15L;
    private static final long MIN_VALUE = -15L;
    private static final long MAX_VALUE = 25L;
    private static final long TEST_VALUE_ID = 16L;

    public static class TestClass extends InternalToolParameter {
        @Input(label = TEST_LABEL)
        public long one;

        @Number(defaultValue = TEST_VALUE)
        @NumericId(defaultValue = TEST_VALUE_ID)
        @Input(label = TEST_LABEL)
        public Long two;

        @Number(defaultValue = TEST_VALUE, minValue = MIN_VALUE, maxValue = MAX_VALUE)
        @Input(label = TEST_LABEL)
        public Long three;

        @Number(defaultValue = TEST_VALUE, minValue = MAX_VALUE, maxValue = MIN_VALUE)
        @Input(label = TEST_LABEL)
        public Long four;

        @NumericId(defaultValue = TEST_VALUE_ID)
        @Input(label = TEST_LABEL)
        public Long idOne;

        @NumericId(defaultValue = TEST_VALUE_ID, canBeZero = true)
        @Input(label = TEST_LABEL)
        public Long idTwo;

        @Number(defaultValue = TEST_VALUE)
        @Input(label = TEST_LABEL)
        public String notNumber;

        @NumericId(defaultValue = TEST_VALUE_ID)
        @Input(label = TEST_LABEL)
        public String notNumberId;
    }

    private ItemValidationBuilder<TestClass, Defect> validationBuilder;
    private TestClass param;

    @Before
    public void before() {
        param = new TestClass();
        validationBuilder = ItemValidationBuilder.of(param);
    }

    /**
     * Проверяем, что умеем работать с числом без аннотации Number
     */
    @Test
    public void testNumberWithoutAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "one");

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

    /**
     * Проверяем, что умеем работать с числом с аннотацией Number
     */
    @Test
    public void testNumberWithAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "two");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.NUMBER)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(TEST_VALUE);
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с числом с аннотацией Number
     */
    @Test
    public void testNumberWithAnnotationAndLimits() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "three");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.NUMBER)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(TEST_VALUE);
        soft.assertThat(input.getArgs())
                .containsOnly(
                        entry(MIN_NUM_VALUE, MIN_VALUE),
                        entry(MAX_NUM_VALUE, MAX_VALUE)
                );
        soft.assertAll();
    }

    @Test
    public void testNumberWithAnnotationAndLimitsValidationLess() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "three");

        param.three = MIN_VALUE - 10;
        input.addValidation(validationBuilder, param);

        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isTrue();
    }

    @Test
    public void testNumberWithAnnotationAndLimitsValidationMore() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "three");

        param.three = MAX_VALUE + 1;
        input.addValidation(validationBuilder, param);

        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isTrue();
    }

    @Test
    public void testNumberWithAnnotationAndLimitsValidationIn() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "three");

        param.three = (MAX_VALUE + MIN_VALUE) / 2;
        input.addValidation(validationBuilder, param);

        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }

    /**
     * Проверяем, что умеем работать с числом с аннотацией Number
     */
    @Test(expected = InternalToolInitialisationException.class)
    public void testNumberWithAnnotationAndLimitsIncorrect() throws NoSuchFieldException {
        getInput(TestClass.class, "four");
    }

    /**
     * Проверяем, что умеем работать с числом с аннотацией NumberId
     */
    @Test
    public void testNumberIdWithAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "idOne");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.NUMBER)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(TEST_VALUE_ID);
        soft.assertThat(input.getArgs())
                .containsOnly(entry(MIN_NUM_VALUE, 1L));
        soft.assertAll();
    }

    @Test
    public void testNumberIdWithAnnotationValidationLess() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "idOne");

        param.idOne = 0L;
        input.addValidation(validationBuilder, param);

        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isTrue();
    }

    @Test
    public void testNumberIdWithAnnotationValidationIn() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "idOne");

        param.idOne = TEST_VALUE_ID;
        input.addValidation(validationBuilder, param);

        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }

    @Test
    public void testNumberIdWithAnnotationValidationZeroIn() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "idTwo");

        param.idTwo = 0L;
        input.addValidation(validationBuilder, param);

        assertThat(validationBuilder.getResult().hasAnyErrors())
                .isFalse();
    }

    /**
     * Проверяем, что умеем работать с числом с аннотацией Number
     */
    @Test
    public void testNumberIdWithAnnotationCanBeZero() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "idTwo");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.NUMBER)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(TEST_VALUE_ID);
        soft.assertThat(input.getArgs())
                .containsOnly(entry(MIN_NUM_VALUE, 0L));
        soft.assertAll();
    }

    /**
     * Проверяем, что не обращаем внимания на аннотацию, если она не на числе
     */
    @Test
    public void testNotNumberWithAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "notNumber");

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
     * Проверяем, что не обращаем внимания на аннотацию, если она не на числе
     */
    @Test
    public void testNotNumberIdWithAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "notNumberId");

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
