package ru.yandex.market.logistics.validation;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Валидация ИНН")
public class InnValidatorTest extends AbstractTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успешная валидация ИНН")
    void validationSuccess(String caseName, String inn) {
        softly.assertThat(validator.validate(new InnHolder(inn))).isEmpty();
    }

    @Nonnull
    static Stream<Arguments> validationSuccess() {
        return Stream.of(
            Arguments.of("ИНН null", null),
            Arguments.of("Валидный ИНН ИП", "027204637820"),
            Arguments.of("Валидный ИНН организации", "7736207543")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Неуспешная валидация")
    void validationError(String caseName, String inn) {
        Set<ConstraintViolation<InnHolder>> violations = validator.validate(new InnHolder(inn));
        softly.assertThat(violations.size()).isEqualTo(1);

        ConstraintViolation<InnHolder> violation = violations.stream().findFirst().orElseThrow();
        softly.assertThat(violation.getMessage()).isEqualTo("invalid inn");
        softly.assertThat(violation.getPropertyPath().toString()).isEqualTo("inn");
    }

    @Nonnull
    static Stream<Arguments> validationError() {
        return Stream.of(
            Arguments.of("Не цифры", "AAAAAAAAAA"),
            Arguments.of("Не 10 или 12 цифр", "027204637"),
            Arguments.of("Неправильная контрольная сумма ИНН ИП", "027204637831"),
            Arguments.of("Неправильная контрольная сумма ИНН организации", "7736207541")
        );
    }

    static class InnHolder {
        @ValidInn
        private final String inn;

        InnHolder(String inn) {
            this.inn = inn;
        }
    }
}
