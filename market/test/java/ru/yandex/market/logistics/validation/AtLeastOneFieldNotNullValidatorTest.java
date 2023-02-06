package ru.yandex.market.logistics.validation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Валидация, что хотя бы одно поле из списка not null")
public class AtLeastOneFieldNotNullValidatorTest extends AbstractTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успешная валидация")
    void validationSuccess(String caseName, Object value) {
        softly.assertThat(validator.validate(value)).isEmpty();
    }

    @Test
    @DisplayName("Неуспешная валидация, оба обязательных поля не заполнены")
    void validationError() {
        Set<ConstraintViolation<FirstTestClass>> violations = validator.validate(
            new FirstTestClass(null, null, new Object())
        );

        List<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
        softly.assertThat(messages).containsExactlyInAnyOrder(
            "at least one field must not be null: [someString, someInt]"
        );
    }

    @Test
    @DisplayName("Неуспешная валидация, несколько валидаций, все поля не заполнены")
    void validationErrorMultiple() {
        Set<ConstraintViolation<SecondTestClass>> violations = validator.validate(
            new SecondTestClass(null, null, null, null)
        );

        List<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
        softly.assertThat(messages).containsExactlyInAnyOrder(
            "at least one field must not be null: [field1, field2]",
            "at least one field must not be null: [field3, field4]"
        );
    }

    @Nonnull
    static Stream<Arguments> validationSuccess() {
        return Stream.of(
            Arguments.of("Первое из двух обязательных полей не заполнено", new FirstTestClass(null, 0, null)),
            Arguments.of("Второе из двух обязательных полей не заполнено", new FirstTestClass("", null, null)),
            Arguments.of("Оба обязательных поля заполнены", new FirstTestClass("", 0, null)),
            Arguments.of("Две аннотации, из каждой заполнено по одному полю", new SecondTestClass("", null, null, "")),
            Arguments.of("Несколько аннотаций, все поля заполнены", new SecondTestClass("", "", "", ""))
        );
    }

    @AtLeastOneFieldNotNull(fieldNames = {"someString", "someInt"})
    static class FirstTestClass {
        String someString;
        Integer someInt;
        Object someObject;

        FirstTestClass(String someString, Integer someInt, Object someObject) {
            this.someString = someString;
            this.someInt = someInt;
            this.someObject = someObject;
        }
    }

    @AtLeastOneFieldNotNull(fieldNames = {"field1", "field2"})
    @AtLeastOneFieldNotNull(fieldNames = {"field3", "field4"})
    static class SecondTestClass {
        Object field1;
        Object field2;
        Object field3;
        Object field4;

        SecondTestClass(Object field1, Object field2, Object field3, Object field4) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
            this.field4 = field4;
        }
    }
}
