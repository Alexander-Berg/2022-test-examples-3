package ru.yandex.market.pvz.core.domain.dbqueue.verify_covid_cert;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.pvz.core.domain.vaccination.VaccinationEmployeeCommandService;
import ru.yandex.market.tpl.common.covid.PersonalDataVaccinationInfo;
import ru.yandex.market.tpl.common.covid.VaccinationValidator;
import ru.yandex.market.tpl.common.covid.ValidationError;
import ru.yandex.market.tpl.common.covid.ValidationResult;
import ru.yandex.market.tpl.common.util.exception.TplExternalException;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.dbqueue.verify_covid_cert.VerifyCovidCertService.BIRTHDAY;
import static ru.yandex.market.pvz.core.domain.dbqueue.verify_covid_cert.VerifyCovidCertService.DELIMITER;
import static ru.yandex.market.pvz.core.domain.dbqueue.verify_covid_cert.VerifyCovidCertService.EXTERNAL_ERROR;
import static ru.yandex.market.pvz.core.domain.dbqueue.verify_covid_cert.VerifyCovidCertService.FIO;
import static ru.yandex.market.pvz.core.domain.dbqueue.verify_covid_cert.VerifyCovidCertService.PASSPORT;
import static ru.yandex.market.pvz.core.domain.dbqueue.verify_covid_cert.VerifyCovidCertService.VERIFY_ERROR_PREFIX;

@ExtendWith(SpringExtension.class)
class VerifyCovidCertServiceTest {

    private static final String QUERY = "https://www.gosuslugi.ru/covid-cert/verify/9780000032520297" +
            "?lang=ru&ck=e32de298c13b59dc872894fb92b0e5d6";
    private static final PersonalDataVaccinationInfo VACCINATION_INFO = PersonalDataVaccinationInfo.builder()
            .firstName("Макс")
            .lastName("Корж")
            .birthdayDate(LocalDate.of(1988, 11, 23))
            .passport("4312 567800")
            .build();

    @Mock
    private VaccinationValidator vaccinationValidator;

    @Mock
    private VaccinationEmployeeCommandService vaccinationEmployeeCommandService;

    private VerifyCovidCertService verifyCovidCertService;

    @BeforeEach
    void setUp() {
        verifyCovidCertService = new VerifyCovidCertService(vaccinationValidator, vaccinationEmployeeCommandService);
    }

    @Test
    void validCert() {
        when(vaccinationValidator.validate(QUERY, VACCINATION_INFO))
                .thenReturn(new ValidationResult(true, List.of()));

        var payload = new VerifyCovidCertPayload("1", 2L, QUERY, VACCINATION_INFO);
        verifyCovidCertService.processPayload(payload);

        verify(vaccinationEmployeeCommandService, times(1)).markCertificateValid(2L);
    }

    @Test
    void fullInvalidCert() {
        when(vaccinationValidator.validate(QUERY, VACCINATION_INFO))
                .thenReturn(new ValidationResult(false, List.of(
                        ValidationError.FIO_VALIDATION_ERROR,
                        ValidationError.BIRTHDAY_VALIDATION_ERROR,
                        ValidationError.PASSPORT_VALIDATION_ERROR)));

        var payload = new VerifyCovidCertPayload("1", 2L, QUERY, VACCINATION_INFO);
        verifyCovidCertService.processPayload(payload);

        verify(vaccinationEmployeeCommandService, times(1))
                .markCertificateInvalid(2L, VERIFY_ERROR_PREFIX + FIO + DELIMITER + BIRTHDAY + DELIMITER + PASSPORT);
    }

    @Test
    void externalGosuslugiError() {
        when(vaccinationValidator.validate(QUERY, VACCINATION_INFO))
                .thenThrow(TplExternalException.class);

        var payload = new VerifyCovidCertPayload("1", 2L, QUERY, VACCINATION_INFO);
        verifyCovidCertService.processPayload(payload);

        verify(vaccinationEmployeeCommandService, times(1))
                .markCertificateInvalid(2L, EXTERNAL_ERROR);
    }

    @Test
    void unexpectedVerifyCovidError() {
        when(vaccinationValidator.validate(QUERY, VACCINATION_INFO))
                .thenThrow(new RuntimeException("Unexpected error"));

        var payload = new VerifyCovidCertPayload("1", 2L, QUERY, VACCINATION_INFO);
        verifyCovidCertService.processPayload(payload);

        verify(vaccinationEmployeeCommandService, times(1))
                .markCertificateInvalid(2L, "Unexpected error");
    }
}
