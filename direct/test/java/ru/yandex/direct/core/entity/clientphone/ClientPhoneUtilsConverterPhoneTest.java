package ru.yandex.direct.core.entity.clientphone;

import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.entity.vcard.model.Phone;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class ClientPhoneUtilsConverterPhoneTest {

    public static List<Object[]> params_convertPhone() {
        return List.of(new Object[][]{
                {
                        "ru mobile phone 7 without first plus",
                        new PhoneNumber().withPhone("79991234567"),
                        new Phone().withCountryCode("+7")
                                .withCityCode("999")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "ru mobile phone 8 without first plus",
                        new PhoneNumber().withPhone("89991234567"),
                        new Phone().withCountryCode("+8")
                                .withCityCode("999")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "ru mobile phone",
                        new PhoneNumber().withPhone("+79991234567"),
                        new Phone().withCountryCode("+7")
                                .withCityCode("999")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "ru mobile phone with extension",
                        new PhoneNumber().withPhone("+79991234567").withExtension(123L),
                        new Phone().withCountryCode("+7")
                                .withCityCode("999")
                                .withPhoneNumber("123-45-67")
                                .withExtension("123")
                },
                {
                        "ru phone with 3 city code",
                        new PhoneNumber().withPhone("+74951234567"),
                        new Phone().withCountryCode("+7")
                                .withCityCode("495")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "ru phone with 4 city code",
                        new PhoneNumber().withPhone("+73812234567"),
                        new Phone().withCountryCode("+7")
                                .withCityCode("3812")
                                .withPhoneNumber("23-45-67")
                                .withExtension(null)
                },
                {
                        // https://ru.wikipedia.org/wiki/Обнинск Только для 48439 и 48458 будет 3-х буквенный код
                        "ru phone with Obninsk 48439 city code",
                        new PhoneNumber().withPhone("+74843912345"),
                        new Phone().withCountryCode("+7")
                                .withCityCode("484")
                                .withPhoneNumber("391-23-45")
                                .withExtension(null)
                },
                {
                        "ru phone with not Obninsk city code",
                        new PhoneNumber().withPhone("+74842908220"),
                        new Phone().withCountryCode("+7")
                                .withCityCode("4842")
                                .withPhoneNumber("90-82-20")
                                .withExtension(null)
                },
                {
                        "ru mobile phone 7 800",
                        new PhoneNumber().withPhone("78001234567"),
                        new Phone().withCountryCode("+7")
                                .withCityCode("800")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "by phone",
                        new PhoneNumber().withPhone("+375291234567"),
                        new Phone().withCountryCode("+375")
                                .withCityCode("29")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "ua phone",
                        new PhoneNumber().withPhone("+380951234567"),
                        new Phone().withCountryCode("+380")
                                .withCityCode("95")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "kz phone",
                        new PhoneNumber().withPhone("+77211234567"),
                        new Phone().withCountryCode("+7")
                                .withCityCode("721")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "tr phone",
                        new PhoneNumber().withPhone("+902421234567"),
                        new Phone().withCountryCode("+90")
                                .withCityCode("242")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "usa phone",
                        new PhoneNumber().withPhone("+19511234567"),
                        new Phone().withCountryCode("+1")
                                .withCityCode("951")
                                .withPhoneNumber("123-45-67")
                                .withExtension(null)
                },
                {
                        "japan phone",
                        new PhoneNumber().withPhone("+81994123456"),
                        new Phone().withCountryCode("+81")
                                .withCityCode("994")
                                .withPhoneNumber("12-34-56")
                                .withExtension(null)
                },
                {
                        "uz phone",
                        new PhoneNumber().withPhone("+998909277848"),
                        new Phone().withCountryCode("+998")
                                .withCityCode("90")
                                .withPhoneNumber("927-78-48")
                                .withExtension(null)
                },

        });
    }

    @Test
    @Parameters(method = "params_convertPhone")
    @TestCaseName("{0}")
    public void convertPhone(
            @SuppressWarnings("unused") String description,
            PhoneNumber phoneNumber,
            Phone phone
    ) {
        var actualPhone = ClientPhoneUtils.toPhone(phoneNumber);
        assertThat(actualPhone).isEqualTo(phone);
    }
}
