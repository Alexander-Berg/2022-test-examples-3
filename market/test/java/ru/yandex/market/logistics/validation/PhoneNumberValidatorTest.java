package ru.yandex.market.logistics.validation;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Валидация номера телефона")
public class PhoneNumberValidatorTest extends AbstractTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успешная валидация")
    void validationSuccess(String caseName, String value) {
        softly.assertThat(validator.validate(new PhoneHolder(value))).isEmpty();
    }

    @Nonnull
    static Stream<Arguments> validationSuccess() {
        return Stream.of(
            Arguments.of("Номер телефона null", null),
            Arguments.of("Без кода страны", "4994905572"),
            Arguments.of("С кодом страны", "+74994905572"),
            Arguments.of("С пробелами", "+7 499490 55     72  "),
            Arguments.of("С префиксом", "тел. +74994905572"),
            Arguments.of("С постфиксом", "+74994905572 доб 193994"),
            Arguments.of("Со скобками", "+7(499)4905572"),
            Arguments.of("С дефисами", "+7-499-490-55-72"),
            Arguments.of("Код страны 8", "84994905572"),
            Arguments.of("Чилийский номер", "+56-42-2 123456")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Неуспешная валидация")
    void validationError(String caseName, String phone) {
        Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(new PhoneHolder(phone));
        softly.assertThat(violations.size()).isEqualTo(1);

        ConstraintViolation<PhoneHolder> violation = violations.stream().findFirst().orElseThrow();
        softly.assertThat(violation.getMessage()).isEqualTo("invalid phone");
        softly.assertThat(violation.getPropertyPath().toString()).isEqualTo("phone");
    }

    @Nonnull
    static Stream<Arguments> validationError() {
        return Stream.of(
            Arguments.of("Пустая строка", ""),
            Arguments.of("Не цифры", "Номер телефона отсутствует"),
            Arguments.of("Больше 11 цифр", "1234567890123456789")
        );
    }

    static class PhoneHolder {
        @ValidPhoneNumber
        private final String phone;

        PhoneHolder(String phone) {
            this.phone = phone;
        }
    }
}
