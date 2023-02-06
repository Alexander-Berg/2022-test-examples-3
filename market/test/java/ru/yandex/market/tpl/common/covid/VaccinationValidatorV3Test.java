package ru.yandex.market.tpl.common.covid;

import java.io.IOException;
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
import ru.yandex.market.tpl.common.covid.gosuslugi.CertInfo;
import ru.yandex.market.tpl.common.covid.gosuslugi.GosuslugiVaccinationClient;
import ru.yandex.market.tpl.common.covid.gosuslugi.GosuslugiVaccinationInfoValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class VaccinationValidatorV3Test {

    private static final PersonalDataVaccinationInfo PERSONAL_INFO = PersonalDataVaccinationInfo.builder()
            .firstName("Андрей")
            .lastName("Субботин")
            .birthdayDate(LocalDate.of(1989, 5, 17))
            .passport("00** ***000")
            .build();

    @Mock
    private GosuslugiVaccinationClient gosuslugiVaccinationClient;

    @Mock
    private GosuslugiVaccinationInfoValidator gosuslugiVaccinationInfoValidator;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private final TplVaccinationInfoMapper vaccinationInfoMapper = new TplVaccinationInfoMapper();

    @InjectMocks
    private TplGosuslugiApiProcessor tplGosuslugiApiProcessor;

    private TplCovidExternalService covidExternalService;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private VaccinationValidator validator;

    @BeforeEach
    void beforeAll() {
        covidExternalService = new TplCovidExternalServiceImpl(List.of(
                tplGosuslugiApiProcessor
        ));
        Mockito.reset();
    }

    @Test
    void validGosuslugiV3Link_V3Test() throws IOException {
        String link = "https://www.gosuslugi.ru/covid-cert/verify/8761072131267813" +
                "?lang=ru&ck=f015ed643f63a61484fcae7a7e50863e";
        String certInfoString = "{\"items\":[{\"type\":\"ILLNESS_FACT\",\"unrz\":\"761072131267813\"," +
                "\"unrzFull\":\"8761072131267813\",\"attrs\":[{\"type\":\"date\",\"title\":\"Дата выздоровления\"," +
                "\"entitle\":\"Recovery date\",\"envalue\":\"20.08.2021\",\"value\":\"20.08.2021\",\"order\":1}," +
                "{\"type\":\"date\",\"title\":\"Действует до\",\"entitle\":\"Valid until\",\"envalue\":\"20.08" +
                ".2022\",\"value\":\"20.08.2022\",\"order\":2},{\"type\":\"fio\",\"title\":\"ФИО\",\"entitle\":\"Full" +
                " name\",\"envalue\":\"S******* A*****\",\"value\":\"С******* А***** А*********\",\"order\":3}," +
                "{\"type\":\"passport\",\"title\":\"Паспорт\",\"entitle\":\"National passport\",\"envalue\":\"00** " +
                "***000\",\"value\":\"00** ***000\",\"order\":4},{\"type\":\"enPassport\"," +
                "\"title\":\"Загранпаспорт\",\"entitle\":\"International passport\",\"envalue\":\"6* ****597\"," +
                "\"value\":\"6* ****597\",\"order\":5},{\"type\":\"birthDate\",\"title\":\"Дата рождения\"," +
                "\"entitle\":\"Date of birth\",\"envalue\":\"17.05.1989\",\"value\":\"17.05.1989\",\"order\":6}]," +
                "\"title\":\"Сведения о перенесенном заболевании COVID-19\",\"entitle\":\"Previous disease " +
                "COVID-19\",\"qr\":\"iVBORw0KGgoAAAANSUhEUgAAAPoAAAD6AQAAAACgl2eQAAADdElEQVR4Xu2WQY4bMQwExY9I" +
                "//9FniJ9REoVNQ4CI1jksOZpZ7H2jFQGGmSzNe18ff1q7ytv1w9wr" +
                "/8BZmuxx2yxdvDXeTw71hxuFAHrnLU7i4N1yJhtnDX7cKMKmMgb7PSrtCWMSDcqASTt1sbunapJDZlSIBl27Bwap2QpYE8ORhlZo8nH" +
                "7LHW3agB9Ob61/W3q9/38vo+wAtFilPYRC337L2GtwJgTANJrDjDSoaianyXAUu" +
                "/2qsuk47JsdExRUCqWfm1hPXM4Hv40yLA6Or9ET" +
                "isl3UixSjWKAKW0cnk9kZ6IM8wSYhUvZP1ecDiDOfEc+S4Z4qFgTL+GObDgN1hhefI/NxODc" +
                "/YtwygK9TI0U2bbmM9pqnKSBUB5xa" +
                "Jx5we/dolZ1atBmA6jPLQpZmdCEQ2H+1x1OcBJKVGV1vGiD2zduMWqgTgOZwUcwO7Ipo8GWnmIsAwb4QFdToeZGaaLzxY5gmxCqB" +
                "rW62iSxyhY4Sl5CJAm1KSaauYHtOEJz3rXNcAK4fWDN25j13VuvRQNqsAmMxMHiS4N40TecZdjUWABmWR0lgs/iN1a6Rzp" +
                "/vzQHYp" +
                "tEvnVdPgAL2THFWAZVGUDUNg2tVmrSxZEZAXPdMpS8dYJMeGjyIALdkXJXGL3pyiSPFVwJNcEOw4wH54p9oaAI8gEkF6F6PgX" +
                "/1D11" +
                "6m/Tyw9cc2yp/4olQWy9edKALskNmBOtYwzfFgo3GFAArtjd0anib85+xYrVvJAsAKuU9weqDiFiSHA/SEeQHQ7kW5hhXLBPE0Cc" +
                "+UGsAAtVA8HVvkD0K/HH9RA7gwkmpCnrIZIzfOagAI3jHtD27FOjp36RzhKsBSBedqS5nIo0J5v1aatgDYZueiT3d2kXn077qBX" +
                "gSE8xsIdXy917taWM9UATNlkR6WCWV3BeiatgBA2dK56NIwOTQv" +
                "+2ahCgBUbmNr3FPkqNUGDjtWBDik6gNax4rxTbnwywCtAaZVeU" +
                "xCdsybo3bM2tUAtkU9GiTfdDlVtulh2+oAysPCrYxvF75pcutqDTAfXaS58+sA5+xmhhUBK11iaFEpRbpF08JSVQG8XNwYGw4MUd" +
                "oyyj3sKoGV5fI9dzo+DLGk7q0DLJLVwqWUTeLYtGuYCuB61qGxT9N3TUMN04DWAJaIXf4oUSq2Ym4/IguAr68f4F7fAPwG/0/5bW" +
                "9ayD8AAAAASUVORK5CYII=\",\"status\":\"1\",\"order\":0,\"expiredAt\":\"20.08.2022\"," +
                "\"serviceUnavailable\":false}],\"hasNext\":false}";
        CertInfo certInfo = VaccinationBaseMapper.readValue(certInfoString, CertInfo.class);
        when(gosuslugiVaccinationClient.getVaccinationInfoFromGosuslugiV3(
                "8761072131267813", "lang=ru&ck=f015ed643f63a61484fcae7a7e50863e"))
                .thenReturn(certInfo);
        when(gosuslugiVaccinationInfoValidator.validate(certInfo, PERSONAL_INFO))
                .thenReturn(new ValidationResult(true, List.of()));

        TplVaccinationInfo vaccinationInfo = covidExternalService.getVaccinationInfo(link);
        ValidationResult actual = validator.validate(vaccinationInfo, PERSONAL_INFO);
        ValidationResult expected = new ValidationResult(true, List.of());

        assertThat(actual).isEqualTo(expected);
    }
}
