package ru.yandex.market.tpl.common.covid;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.common.covid.external.TplCovidExternalService;
import ru.yandex.market.tpl.common.covid.external.TplCovidExternalServiceImpl;
import ru.yandex.market.tpl.common.covid.external.TplVaccinationInfo;
import ru.yandex.market.tpl.common.covid.external.mapper.TplVaccinationInfoMapper;
import ru.yandex.market.tpl.common.covid.external.processors.TplGosuslugiApiProcessor;
import ru.yandex.market.tpl.common.covid.external.processors.TplMosCovidApiProcessor;
import ru.yandex.market.tpl.common.covid.gosuslugi.CertAttr;
import ru.yandex.market.tpl.common.covid.gosuslugi.CertAttrType;
import ru.yandex.market.tpl.common.covid.gosuslugi.CertInfo;
import ru.yandex.market.tpl.common.covid.gosuslugi.CertItem;
import ru.yandex.market.tpl.common.covid.gosuslugi.GosuslugiVaccinationClient;
import ru.yandex.market.tpl.common.covid.gosuslugi.GosuslugiVaccinationInfo;
import ru.yandex.market.tpl.common.covid.gosuslugi.GosuslugiVaccinationInfoValidator;
import ru.yandex.market.tpl.common.covid.mosru.ImmuneMosRuVaccinationInfo;
import ru.yandex.market.tpl.common.covid.mosru.MosRuVaccinationClient;
import ru.yandex.market.tpl.common.covid.mosru.MosRuVaccinationInfoValidator;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.common.covid.TplVaccinationUtil.ERROR_MESSAGE;

@ExtendWith(SpringExtension.class)
class VaccinationValidatorV2Test {

    private static final PersonalDataVaccinationInfo PERSONAL_INFO = PersonalDataVaccinationInfo.builder()
            .firstName("Александр")
            .lastName("Кержаков")
            .birthdayDate(LocalDate.of(1982, 11, 27))
            .passport("4516 889911")
            .build();

    @Mock
    private GosuslugiVaccinationClient gosuslugiVaccinationClient;

    @Mock
    private MosRuVaccinationClient mosRuVaccinationClient;

    @Mock
    private GosuslugiVaccinationInfoValidator gosuslugiVaccinationInfoValidator;

    @Mock
    private MosRuVaccinationInfoValidator mosRuVaccinationInfoValidator;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TplVaccinationInfoMapper vaccinationInfoMapper = new TplVaccinationInfoMapper();

    @InjectMocks
    private TplGosuslugiApiProcessor tplGosuslugiApiProcessor;

    @InjectMocks
    private TplMosCovidApiProcessor tplMosCovidApiProcessor;


    private TplCovidExternalService covidExternalService;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private VaccinationValidator validator;

    @BeforeEach
    void beforeAll() {
        covidExternalService = new TplCovidExternalServiceImpl(List.of(
                tplGosuslugiApiProcessor,
                tplMosCovidApiProcessor
        ));
        Mockito.reset();
    }

    @Test
    void validGosuslugiCertificate_V2() {
        String certificateNumber = "0000659887469988";
        GosuslugiVaccinationInfo gosuslugiVaccinationInfo = new GosuslugiVaccinationInfo(
                null, LocalDate.of(1982, 11, 27),
                LocalDate.of(2022, 11, 27),
                "45** ***911", null, false, "2",
                "К******* А******** А**********", null, "ll-23232", ""
        );
        when(gosuslugiVaccinationClient.getVaccinationInfoFromGosuslugiV1(certificateNumber))
                .thenReturn(gosuslugiVaccinationInfo);
        when(gosuslugiVaccinationInfoValidator.validate(gosuslugiVaccinationInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(true, List.of()));


        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(certificateNumber);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);

        ValidationResult expected = new ValidationResult(true, List.of());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void validGosuslugiCertificateWithSpaces_v2() {
        String certificateNumber = "0000 6598 8746 9988";
        GosuslugiVaccinationInfo gosuslugiVaccinationInfo = new GosuslugiVaccinationInfo(
                null, LocalDate.of(1982, 11, 27),
                LocalDate.of(2022, 11, 27),
                "45** ***911", null, false, "2",
                "К******* А******** А**********", null, "ll-23232", ""
        );
        when(gosuslugiVaccinationClient.getVaccinationInfoFromGosuslugiV1("0000659887469988"))
                .thenReturn(gosuslugiVaccinationInfo);
        when(gosuslugiVaccinationInfoValidator.validate(gosuslugiVaccinationInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(true, List.of()));

        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(certificateNumber);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);

        ValidationResult expected = new ValidationResult(true, List.of());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void invalidGosuslugiCertificate_V2() {
        String certificateNumber = "0000659887469988";
        GosuslugiVaccinationInfo gosuslugiVaccinationInfo = new GosuslugiVaccinationInfo(
                null, LocalDate.of(1982, 11, 28),
                LocalDate.of(2022, 11, 27),
                "45** ***911", null, false, "2",
                "К******* А******** А**********", null, "ll-23232", ""
        );
        when(gosuslugiVaccinationClient.getVaccinationInfoFromGosuslugiV1(certificateNumber))
                .thenReturn(gosuslugiVaccinationInfo);
        when(gosuslugiVaccinationInfoValidator.validate(gosuslugiVaccinationInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR)));

        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(certificateNumber);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);

        ValidationResult expected =
                new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void validGosuslugiV1Link_V2() {
        String link = "https://www.gosuslugi.ru/api/vaccine/v1/cert/verify/9780000032520297";
        GosuslugiVaccinationInfo gosuslugiVaccinationInfo = new GosuslugiVaccinationInfo(
                null, LocalDate.of(1982, 11, 27),
                LocalDate.of(2022, 11, 27),
                "45** ***911", null, false, "2",
                "К******* А******** А**********", null, "ll-23232", ""
        );
        when(gosuslugiVaccinationClient.getVaccinationInfoFromGosuslugiV1("9780000032520297"))
                .thenReturn(gosuslugiVaccinationInfo);
        when(gosuslugiVaccinationInfoValidator.validate(gosuslugiVaccinationInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(true, List.of()));

        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);

        ValidationResult expected = new ValidationResult(true, List.of());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void invalidGosuslugiV1Link_V2() {
        String link = "https://www.gosuslugi.ru/api/vaccine/v1/cert/verify/9780000032520297";
        GosuslugiVaccinationInfo gosuslugiVaccinationInfo = new GosuslugiVaccinationInfo(
                null, LocalDate.of(1982, 11, 28),
                LocalDate.of(2022, 11, 27),
                "45** ***911", null, false, "2",
                "К******* А******** А**********", null, "ll-23232", ""
        );
        when(gosuslugiVaccinationClient.getVaccinationInfoFromGosuslugiV1("9780000032520297"))
                .thenReturn(gosuslugiVaccinationInfo);
        when(gosuslugiVaccinationInfoValidator.validate(gosuslugiVaccinationInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR)));

        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);


        ValidationResult expected =
                new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void validGosuslugiV2Link_V2() {
        String link = "https://www.gosuslugi.ru/covid-cert/verify/9780000032520297" +
                "?lang=ru&ck=e32de298c13b59dc872894fb92b0e5d6";
        CertInfo certInfo = new CertInfo(
                List.of(CertItem.builder()
                        .attrs(List.of(
                                CertAttr.builder()
                                        .type(CertAttrType.FIO)
                                        .value("К******* А******** А**********")
                                        .build(),
                                CertAttr.builder()
                                        .type(CertAttrType.PASSPORT)
                                        .value("45** ***911")
                                        .build(),
                                CertAttr.builder()
                                        .type(CertAttrType.BIRTH_DATE)
                                        .value("27.11.1982")
                                        .build()
                                )
                        ).build()
                )
        );
        when(gosuslugiVaccinationClient.getVaccinationInfoFromGosuslugiV3(
                "9780000032520297", "lang=ru&ck=e32de298c13b59dc872894fb92b0e5d6"))
                .thenReturn(certInfo);
        when(gosuslugiVaccinationInfoValidator.validate(certInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(true, List.of()));


        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);

        ValidationResult expected = new ValidationResult(true, List.of());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void validGosuslugiV2_CertStatus_V2() {
        //given
        String link = "https://www.gosuslugi.ru/covid-cert/status/11x1111x-2xx6-4xx8-8x39-44x7777xx555" +
                "?lang=ru";
        CertInfo certInfo = new CertInfo(
                List.of(CertItem.builder()
                        .attrs(List.of(
                                CertAttr.builder()
                                        .type(CertAttrType.FIO)
                                        .value("К******* А******** А**********")
                                        .build(),
                                CertAttr.builder()
                                        .type(CertAttrType.PASSPORT)
                                        .value("45** ***911")
                                        .build(),
                                CertAttr.builder()
                                        .type(CertAttrType.BIRTH_DATE)
                                        .value("27.11.1982")
                                        .build()
                                )
                        ).build()
                )
        );
        when(gosuslugiVaccinationClient.getVaccinationStatusFromGosuslugiV2(
                "11x1111x-2xx6-4xx8-8x39-44x7777xx555", "lang=ru"))
                .thenReturn(certInfo);
        when(gosuslugiVaccinationInfoValidator.validate(certInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(true, List.of()));


        //when
        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);

        //then
        ValidationResult expected = new ValidationResult(true, List.of());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void invalidGosuslugiV2Link_V2() {
        String link = "https://www.gosuslugi.ru/covid-cert/verify/9780000032520297" +
                "?lang=ru&ck=e32de298c13b59dc872894fb92b0e5d6";
        CertInfo certInfo = new CertInfo(
                List.of(CertItem.builder()
                        .attrs(List.of(
                                CertAttr.builder()
                                        .type(CertAttrType.FIO)
                                        .value("К******* А******** А**********")
                                        .build(),
                                CertAttr.builder()
                                        .type(CertAttrType.PASSPORT)
                                        .value("45** ***911")
                                        .build(),
                                CertAttr.builder()
                                        .type(CertAttrType.BIRTH_DATE)
                                        .value("28.11.1982")
                                        .build()
                                )
                        ).build()
                )
        );
        when(gosuslugiVaccinationClient.getVaccinationInfoFromGosuslugiV3(
                "9780000032520297", "lang=ru&ck=e32de298c13b59dc872894fb92b0e5d6"))
                .thenReturn(certInfo);
        when(gosuslugiVaccinationInfoValidator.validate(certInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR)));


        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);

        ValidationResult expected =
                new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void validGosuslugiUnrzLink_V2() {
        String link = "https://www.gosuslugi.ru/api/vaccine/v1/cert/verify/unrz/9780000032520297";
        GosuslugiVaccinationInfo gosuslugiVaccinationInfo = new GosuslugiVaccinationInfo(
                null, LocalDate.of(1982, 11, 27),
                LocalDate.of(2022, 11, 27),
                "45** ***911", null, false, "2",
                "К******* А******** А**********", null, "ll-23232", ""
        );
        when(gosuslugiVaccinationClient.getVaccinationInfoUnrz("9780000032520297"))
                .thenReturn(gosuslugiVaccinationInfo);
        when(gosuslugiVaccinationInfoValidator.validate(gosuslugiVaccinationInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(true, List.of()));

        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);

        ValidationResult expected = new ValidationResult(true, List.of());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void invalidGosuslugiUnrzLink() {
        String link = "https://www.gosuslugi.ru/api/vaccine/v1/cert/verify/unrz/9780000032520297";
        GosuslugiVaccinationInfo gosuslugiVaccinationInfo = new GosuslugiVaccinationInfo(
                null, LocalDate.of(1982, 11, 28),
                LocalDate.of(2022, 11, 27),
                "45** ***911", null, false, "2",
                "К******* А******** А**********", null, "ll-23232", ""
        );
        when(gosuslugiVaccinationClient.getVaccinationInfoUnrz("9780000032520297"))
                .thenReturn(gosuslugiVaccinationInfo);
        when(gosuslugiVaccinationInfoValidator.validate(gosuslugiVaccinationInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR)));


        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);

        ValidationResult expected =
                new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void validMosRuLink_V2() {
        String link = "https://immune.mos.ru/qr?id=12345QWERTY67890";
        var vaccinationInfo = new ImmuneMosRuVaccinationInfo(
                new ImmuneMosRuVaccinationInfo.Result(
                        "12345QWERTY67890", "916e6c25", "EMIAS_QUEUE", "2021-06-24T14:11:35.019+03:00", "iVBORw",
                        new ImmuneMosRuVaccinationInfo.Certificate("VACCINATED", "2021-06-24T00:00:00.000+03:00",
                                "2022-06-24T00:00:00.000+03:00", "К******* А******** А**********", LocalDate.of(1982,
                                11, 27),
                                "B89900FAC41852D7171CC5A9EFF43263", "EMIAS"),
                        new ImmuneMosRuVaccinationInfo.Vaccinated(true))
        );
        when(mosRuVaccinationClient.getVaccinationInfoFromMosRu("12345QWERTY67890"))
                .thenReturn(vaccinationInfo);
        when(mosRuVaccinationInfoValidator.validate(vaccinationInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(true, List.of()));

        TplVaccinationInfo tplVaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(tplVaccinationInfo, PERSONAL_INFO);

        ValidationResult expected =
                new ValidationResult(true, List.of());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void invalidMosRuLink_V2() {
        String link = "https://immune.mos.ru/qr?id=12345QWERTY67890";
        var vaccinationInfo = new ImmuneMosRuVaccinationInfo(
                new ImmuneMosRuVaccinationInfo.Result(
                        "12345QWERTY67890", "916e6c25", "EMIAS_QUEUE", "2021-06-24T14:11:35.019+03:00", "iVBORw",
                        new ImmuneMosRuVaccinationInfo.Certificate("VACCINATED", "2021-06-24T00:00:00.000+03:00",
                                "2022-06-24T00:00:00.000+03:00", "К******* А******** А**********", LocalDate.of(1982,
                                11, 28),
                                "B89900FAC41852D7171CC5A9EFF43263", "EMIAS"),
                        new ImmuneMosRuVaccinationInfo.Vaccinated(true))
        );
        when(mosRuVaccinationClient.getVaccinationInfoFromMosRu("12345QWERTY67890"))
                .thenReturn(vaccinationInfo);
        when(mosRuVaccinationInfoValidator.validate(vaccinationInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR)));

        TplVaccinationInfo tplVaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(tplVaccinationInfo, PERSONAL_INFO);

        ValidationResult expected =
                new ValidationResult(false, List.of(ValidationError.BIRTHDAY_VALIDATION_ERROR));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void unsupportedQuery_V2() {
        String query = "123456-certificate";

        var actual = assertThrows(TplInvalidParameterException.class, () -> covidExternalService.getVaccinationInfo(query));

        assertThat(actual.getMessage()).isEqualTo(ERROR_MESSAGE);
    }
}
