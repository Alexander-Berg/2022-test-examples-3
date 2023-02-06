package ru.yandex.market.tpl.common.covid;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class VaccinationInfoBaseValidatorTest {

    @ParameterizedTest
    @MethodSource("validateFioTestInput")
    void validateFioTest(String vaccinationFio, String personalLastName, String personalFirstName, boolean expected) {
        var validator = new VaccinationInfoBaseValidator();

        var actual = validator.validateFio(vaccinationFio, personalLastName, personalFirstName);
        assertThat(actual).isEqualTo(expected);
    }

    private static List<Arguments> validateFioTestInput() {
        return List.of(
                Arguments.of("С****** П*** Ф********", "Сидоров", "Петр", true),
                Arguments.of("С****** П*** Ф********", "Сидоров", "Павел", true),
                Arguments.of("С****** П*** Ф********", "Самойлов", "Петр", true),
                Arguments.of("С****** П*** Ф********", "сидоров", "петр", true),
                Arguments.of("С****** П*** Ф********", "Мойкин", "Петр", false),
                Arguments.of("С****** П*** Ф********", "Сидоров", "Михаил", false)
        );
    }

    @ParameterizedTest
    @MethodSource("validateBirthdayTestInput")
    void validateBirthdayTest(LocalDate vaccinationBirthday, LocalDate personalBirthday, boolean expected) {
        var validator = new VaccinationInfoBaseValidator();

        var actual = validator.validateBirthday(vaccinationBirthday, personalBirthday);
        assertThat(actual).isEqualTo(expected);
    }

    private static List<Arguments> validateBirthdayTestInput() {
        return List.of(
                Arguments.of(LocalDate.of(1994, 10, 28), LocalDate.of(1994, 10, 28), true),
                Arguments.of(LocalDate.of(1994, 10, 28), LocalDate.of(1994, 10, 29), false)
        );
    }

    @ParameterizedTest
    @MethodSource("validatePassportTestInput")
    void validatePassportTest(String vaccinationPassport, String personalPassport, boolean expected) {
        var validator = new VaccinationInfoBaseValidator();

        var actual = validator.validatePassport(vaccinationPassport, personalPassport);
        assertThat(actual).isEqualTo(expected);
    }

    private static List<Arguments> validatePassportTestInput() {
        return List.of(
                Arguments.of("45** ***430", "4510 222430", true),
                Arguments.of("45** ***430", "4510222430", true),
                Arguments.of("C025***** *", "C02517353", true),
                Arguments.of("C025***** *3", "C02517353", true),
                Arguments.of("* ****657", "3863657", true),
                Arguments.of("46** ***430", "4510 222430", false),
                Arguments.of("46** ***550", "4510 222430", false),
                Arguments.of("45** ***430", "5510 222430", false),
                Arguments.of("45** ***431", "4510 222430", false),
                Arguments.of(null, null, true)
        );
    }

}
