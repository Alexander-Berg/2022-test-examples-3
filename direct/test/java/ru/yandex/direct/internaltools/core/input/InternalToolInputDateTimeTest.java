package ru.yandex.direct.internaltools.core.input;

import java.time.LocalDateTime;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.DateTime;
import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.exception.InternalToolInitialisationException;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на парсинг описаний полей с датой и временем
 */
public class InternalToolInputDateTimeTest {
    private static final String TEST_LABEL = "Some datetime";
    private static final String TEST_DATE = "2017-01-01T05:13:34";

    public static class TestClass extends InternalToolParameter {
        @Input(label = TEST_LABEL)
        public LocalDateTime dateOne;

        @DateTime(defaultValue = TEST_DATE)
        @Input(label = TEST_LABEL)
        public LocalDateTime dateTwo;

        @DateTime(now = true)
        @Input(label = TEST_LABEL)
        public LocalDateTime dateThree;

        @DateTime(defaultValue = TEST_DATE, now = true)
        @Input(label = TEST_LABEL)
        public LocalDateTime dateFour;

        @DateTime(defaultValue = TEST_DATE + "12345")
        @Input(label = TEST_LABEL)
        public LocalDateTime dateFive;

        @DateTime
        @Input(label = TEST_LABEL)
        public LocalDateTime dateSix;

        @DateTime(defaultValue = TEST_DATE)
        @Input(label = TEST_LABEL)
        public String notDate;
    }

    /**
     * Проверяем, что умеем работать с датой без аннотации DateTime
     */
    @Test
    public void testDateWithoutAnnotation() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "dateOne");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.DATETIME)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с датой с пустой аннотацией DateTime
     */
    @Test
    public void testDateWithAnnotationWithoutParams() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "dateSix");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.DATETIME)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isNull();
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с датой c аннотацией DateTime и заданным значением по-умолчанию
     */
    @Test
    public void testDateWithAnnotationDefaultSet() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "dateTwo");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.DATETIME)
                .as("Тип контрола корректен");
        soft.assertThat(input.getDefaultValue())
                .isEqualTo(LocalDateTime.parse(TEST_DATE));
        soft.assertThat(input.getArgs())
                .isEmpty();
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать с датой c аннотацией DateTime и заданным значением now
     */
    @Test
    public void testDateWithAnnotationTodaySet() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "dateThree");

        assertThat(input).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(input.getInputType())
                .isEqualTo(InternalToolInputType.DATETIME)
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

        LocalDateTime before = LocalDateTime.now();
        input = input.applyPreProcessors();
        assertThat((LocalDateTime) input.getDefaultValue())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(LocalDateTime.now());
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
