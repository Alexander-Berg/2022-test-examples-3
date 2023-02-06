package ru.yandex.direct.core.entity.clientphone;

import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.organizations.swagger.model.CompanyPhone;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.clientphone.ClientPhoneUtils.toPhoneNumber;

@RunWith(JUnitParamsRunner.class)
public class ClientPhoneUtilsTest {

    public static List<Object[]> parametersForToPhoneNumber_test() {
        return asList(new Object[][]{
                {
                        "простой случай, без добавочного",
                        new CompanyPhone().countryCode("7").regionCode("800").number("1234567"),
                        new PhoneNumber().withPhone("+78001234567")
                },
                {
                        "с добавочным",
                        new CompanyPhone().countryCode("7").regionCode("800").number("1234567").ext("123"),
                        new PhoneNumber().withPhone("+78001234567").withExtension(123L)
                },
                {
                        "текст в добавочном",
                        new CompanyPhone().countryCode("7").regionCode("800").number("1234567").ext("менеджер"),
                        new PhoneNumber().withPhone("+78001234567").withExtension(null)
                },
        });
    }

    @Test
    @Parameters(method = "parametersForToPhoneNumber_test")
    @TestCaseName("{0}")
    public void toPhoneNumber_test(
            @SuppressWarnings("unused") String name,
            CompanyPhone input, PhoneNumber expected) {
        PhoneNumber actual = toPhoneNumber(input);
        assertThat(actual)
                .isEqualToComparingFieldByField(expected);
    }

    public static List<Object[]> parametersForParseExtension_test() {
        return List.of(new Object[][]{
                {
                        "Числовой добавочный номер",
                        "123456",
                        123456L
                },
                {
                        "Строковый добавочный номер",
                        "Менеджер",
                        null
                },
                {
                        "Слишком длинный добавочный номер",
                        "1234567",
                        null
                },
                {
                        "Отрицательный добавочный номер",
                        "-12345",
                        null
                },
        });
    }

    @Test
    @Parameters(method = "parametersForParseExtension_test")
    @TestCaseName("{0}")
    public void parseExtension_test(@SuppressWarnings("unused") String name, String originExt, Long parsedExt) {
        Long actualParsedExtension = ClientPhoneUtils.parseExtension(originExt);
        assertThat(actualParsedExtension).isEqualTo(parsedExt);
    }

    public static List<Object[]> parametersForGetRegionCode_test() {
        return List.of(new Object[][]{
                {
                        "Телефон c плюсом",
                        "+79874564345",
                        "987"
                },
                {
                        "Телефон без плюса",
                        "79874564345",
                        "987"
                },
        });
    }

    @Test
    @Parameters(method = "parametersForGetRegionCode_test")
    @TestCaseName("{0}")
    public void getRegionCode_test(@SuppressWarnings("unused") String name, String phoneNumber, String regionCode) {
        assertThat(ClientPhoneUtils.getRegionCode(phoneNumber)).isEqualTo(regionCode);
    }
}
