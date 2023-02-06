package ru.yandex.direct.internaltools.core.input;

import java.time.LocalDate;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Date;
import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.exception.InternalToolInitialisationException;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на парсинг описаний полей с датой
 */
public class InternalToolInputDateTest {
    private static final String TEST_LABEL = "Some date";
    private static final String TEST_DATE = "2017-01-01";

    public static class TestClass extends InternalToolParameter {
        @Input(label = TEST_LABEL)
        public LocalDate dateOne;

        @Date(defaultValue = TEST_DATE)
        @Input(label = TEST_LABEL)
        public LocalDate dateTwo;

        @Date(today = true)
        @Input(label = TEST_LABEL)
        public LocalDate dateThree;

        @Date(defaultValue = TEST_DATE, today = true)
        @Input(label = TEST_LABEL)
        public LocalDate dateFour;

        @Date(defaultValue = TEST_DATE + "12345")
        @Input(label = TEST_LABEL)
        public LocalDate dateFive;

        @Date
        @Input(label = TEST_LABEL)
        public LocalDate dateSix;

        @Date(defaultValue = TEST_DATE)
        @Input(label = TEST_LABEL)
        public String notDate;
    }

    /**
     * Проверяем, что умеем работать с датой без аннотации Date
     */
    @Test
    public void testDateWithoutAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "dateOne");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.DATE)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с датой с пустой аннотацией Date
     */
    @Test
    public void testDateWithAnnotationWithoutParams() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "dateSix");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.DATE)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с датой c аннотацией Date и заданным значением по-умолчанию
     */
    @Test
    public void testDateWithAnnotationDefaultSet() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "dateTwo");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.DATE)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(LocalDate.parse(TEST_DATE));
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с датой c аннотацией Date и заданным значением today
     */
    @Test
    public void testDateWithAnnotationTodaySet() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "dateThree");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.DATE)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getPreProcessors())
                .size().isGreaterThanOrEqualTo(1);
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с датой c аннотацией Date и заданным значением today
     */
    @Test
    public void testDateWithAnnotationTodaySetDefault() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "dateThree");

        assertThat(input).isNotNull()
                .as("Получен не null");

        LocalDate before = LocalDate.now();
        input = input.applyPreProcessors();
        assertThat((LocalDate) input.getDefaultValue())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(LocalDate.now());
    }

    /**
     * Проверяем, что возбуждаем исключение если в аннотации задано два параметра
     */
    @Test(expected = InternalToolInitialisationException.class)
    public void testDateWithAnnotationTodayAndDefaultSet() throws NoSuchFieldException {
        getInput(TestClass.class, "dateFour");
    }

    /**
     * Проверяем, что возбуждаем исключение если дефолнтное значение не дата
     */
    @Test(expected = InternalToolInitialisationException.class)
    public void testDateWithAnnotationUnparsableDefaultSet() throws NoSuchFieldException {
        getInput(TestClass.class, "dateFive");
    }

    /**
     * Проверяем, что аннотацией Date на другом поле не работает
     */
    @Test()
    public void testStringWithDateAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "notDate");

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
