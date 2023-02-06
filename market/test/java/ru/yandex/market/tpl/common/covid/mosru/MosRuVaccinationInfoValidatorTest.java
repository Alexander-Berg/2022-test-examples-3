package ru.yandex.market.tpl.common.covid.mosru;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.tpl.common.covid.PersonalDataVaccinationInfo;
import ru.yandex.market.tpl.common.covid.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.covid.ValidationError.BIRTHDAY_VALIDATION_ERROR;
import static ru.yandex.market.tpl.common.covid.ValidationError.FIO_VALIDATION_ERROR;

class MosRuVaccinationInfoValidatorTest {

    @Test
    void validCertificateTest() {
        var vaccinationInfo = new ImmuneMosRuVaccinationInfo(
                new ImmuneMosRuVaccinationInfo.Result(
                        "25PXP589R1", "916e6c25", "EMIAS_QUEUE", "2021-06-24T14:11:35.019+03:00", "iVBORw",
                        new ImmuneMosRuVaccinationInfo.Certificate("VACCINATED", "2021-06-24T00:00:00.000+03:00",
                                "2022-06-24T00:00:00.000+03:00", "Р**** А**** П*******", LocalDate.of(1994, 2, 5),
                                "B89900FAC41852D7171CC5A9EFF43263", "EMIAS"),
                        new ImmuneMosRuVaccinationInfo.Vaccinated(true))
        );

        var validator = new MosRuVaccinationInfoValidator();

        var actual = validator.validate(vaccinationInfo, PersonalDataVaccinationInfo.builder()
                .firstName("Артем")
                .lastName("Рылов")
                .birthdayDate(LocalDate.of(1994, 2, 5))
                .passport("6112456230")
                .build());
        ValidationResult expected = new ValidationResult(true, List.of());

        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("invalidCertificateTestInput")
    void invalidCertificateTest(
            String name,
            LocalDate birthDate,
            ValidationResult expectedResult
    ) {
        var vaccinationInfo = new ImmuneMosRuVaccinationInfo(
                new ImmuneMosRuVaccinationInfo.Result(
                        "25PXP589R1", "916e6c25", "EMIAS_QUEUE", "2021-06-24T14:11:35.019+03:00", "iVBORw",
                        new ImmuneMosRuVaccinationInfo.Certificate("VACCINATED", "2021-06-24T00:00:00.000+03:00",
                                "2022-06-24T00:00:00.000+03:00", name, birthDate,
                                "B89900FAC41852D7171CC5A9EFF43263", "EMIAS"),
                        new ImmuneMosRuVaccinationInfo.Vaccinated(true))
        );

        var validator = new MosRuVaccinationInfoValidator();

        var actual = validator.validate(vaccinationInfo, PersonalDataVaccinationInfo.builder()
                .firstName("Артем")
                .lastName("Рылов")
                .birthdayDate(LocalDate.of(1994, 2, 5))
                .passport("6112456230")
                .build());

        assertThat(actual).isEqualTo(expectedResult);
    }

    private static List<Arguments> invalidCertificateTestInput() {
        return List.of(
                Arguments.of("А**** Р**** Д*******", LocalDate.of(1994, 2, 5),
                        new ValidationResult(false, List.of(FIO_VALIDATION_ERROR))),
                Arguments.of("Р**** А**** П*******", LocalDate.of(1994, 2, 6),
                        new ValidationResult(false, List.of(BIRTHDAY_VALIDATION_ERROR))),
                Arguments.of("А**** Р**** Д*******", LocalDate.of(1994, 2, 6),
                        new ValidationResult(false, List.of(FIO_VALIDATION_ERROR, BIRTHDAY_VALIDATION_ERROR)))
        );
    }
}
