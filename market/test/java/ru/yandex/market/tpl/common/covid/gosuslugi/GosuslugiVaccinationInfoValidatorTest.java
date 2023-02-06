package ru.yandex.market.tpl.common.covid.gosuslugi;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.tpl.common.covid.PersonalDataVaccinationInfo;
import ru.yandex.market.tpl.common.covid.ValidationError;
import ru.yandex.market.tpl.common.covid.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

class GosuslugiVaccinationInfoValidatorTest {

    @Test
    void validCertificateTest() {
        var certInfo = new CertInfo(
                List.of(
                        CertItem.builder()
                                .type(CertItemType.VACCINE_CERT)
                                .attrs(
                                        List.of(
                                                CertAttr.builder()
                                                        .type(CertAttrType.FIO)
                                                        .value("К***** П**** А*********")
                                                        .build(),
                                                CertAttr.builder()
                                                        .type(CertAttrType.PASSPORT)
                                                        .value("61** ***230")
                                                        .build(),
                                                CertAttr.builder()
                                                        .type(CertAttrType.BIRTH_DATE)
                                                        .value("23.06.1967")
                                                        .build()
                                        )
                                )
                                .order(0)
                                .build())
        );
        var validator = new GosuslugiVaccinationInfoValidator();
        var actual = validator.validate(
                certInfo,
                PersonalDataVaccinationInfo.builder()
                        .firstName("Пашка")
                        .lastName("Комаров")
                        .passport("6112456230")
                        .birthdayDate(LocalDate.of(1967, 6, 23))
                        .build());

        ValidationResult expected = new ValidationResult(true, List.of());
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("invalidCertificateTestInput")
    void invalidCertificateTest(
            String name,
            String birthDate,
            String passport,
            ValidationResult expectedResult
    ) {
        var validator = new GosuslugiVaccinationInfoValidator();
        var certInfo = new CertInfo(
                List.of(
                        CertItem.builder()
                                .type(CertItemType.VACCINE_CERT)
                                .attrs(
                                        List.of(
                                                CertAttr.builder()
                                                        .type(CertAttrType.FIO)
                                                        .value(name)
                                                        .build(),
                                                CertAttr.builder()
                                                        .type(CertAttrType.PASSPORT)
                                                        .value(passport)
                                                        .build(),
                                                CertAttr.builder()
                                                        .type(CertAttrType.BIRTH_DATE)
                                                        .value(birthDate)
                                                        .build()
                                        )
                                )
                                .order(0)
                                .build())
        );

        var actual = validator.validate(
                certInfo,
                PersonalDataVaccinationInfo.builder()
                        .firstName("Пашка")
                        .lastName("Комаров")
                        .passport("6112456230")
                        .birthdayDate(LocalDate.of(1967, 6, 23))
                        .build()
        );

        assertThat(actual).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> invalidCertificateTestInput() {
        return Stream.of(
                Arguments.of(
                        "К***** A**** А*********",
                        "23.06.1967",
                        "61** ***230",
                        new ValidationResult(false, List.of(ValidationError.FIO_VALIDATION_ERROR))
                ),
                Arguments.of(
                        "К***** П**** А*********",
                        "23.06.1967",
                        "61** ***231",
                        new ValidationResult(false, List.of(ValidationError.PASSPORT_VALIDATION_ERROR))
                ),
                Arguments.of(
                        "К***** П**** А*********",
                        "23.06.1968",
                        "61** ***230",
                        new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR))
                )
        );
    }
}
