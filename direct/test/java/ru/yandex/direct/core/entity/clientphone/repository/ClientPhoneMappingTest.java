package ru.yandex.direct.core.entity.clientphone.repository;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class ClientPhoneMappingTest {

    @Test
    @Parameters(method = "phoneNumbers")
    public void phoneNumberToDb(PhoneNumber phoneNumber, String expected) {
        String actual = ClientPhoneMapping.phoneNumberToDb(phoneNumber);

        assertThat(actual).isEqualTo(expected);
    }

    public static Object[] phoneNumbers() {
        return new Object[][] {
                {phone("+749512345", null), "+7#495#1-23-45#"},
                {phone("+7495123456", null), "+7#495#12-34-56#"},
                {phone("+74951234567", null), "+7#495#123-45-67#"},
                {phone("+7495123456789", null), "+7#495#123-45-67-89#"},
                {phone("+749523-45-6-78", null), "+7#495#234-56-78#"},
                {phone("+74951234567", 123L), "+7#495#123-45-67#123"},
                {phone("74951234567", 1L), "+7#495#123-45-67#1"}
        };
    }

    @Test
    @Parameters(method = "phonesFromDb")
    public void phoneNumberFromDb(PhoneNumber expected, String phoneNumber) {
        PhoneNumber actual = ClientPhoneMapping.phoneNumberFromDb(phoneNumber);

        assertThat(actual).isEqualTo(expected);
    }

    public static Object[] phonesFromDb() {
        return new Object[][] {
                {phone("+749512345", null), "+7#495#1-23-45#"},
                {phone("+7495123456", null), "+7#495#12-34-56#"},
                {phone("+74951234567", null), "+7#495#123-45-67#"},
                {phone("+7495123456789", null), "+7#495#123-45-67-89#"},
                {phone("+74951234567", 123L), "+7#495#123-45-67#123"},
        };
    }

    public static PhoneNumber phone(String phone, Long ext) {
        return new PhoneNumber()
                .withPhone(phone)
                .withExtension(ext);
    }

}
