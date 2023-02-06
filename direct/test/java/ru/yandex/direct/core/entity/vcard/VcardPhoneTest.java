package ru.yandex.direct.core.entity.vcard;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@ParametersAreNonnullByDefault
public class VcardPhoneTest {
    private static final VcardPhone FULL_PHONE = VcardPhone.fromEncodedString("+7#123#456-78-90#1234");
    private static final VcardPhone EMPTY_PHONE = VcardPhone.fromEncodedString("###");

    @Test
    public void fromEncodedStringFullPhoneCountryCodeTest() {
        assertEquals("получен правильный код страны", "+7", FULL_PHONE.getCountryCode());
    }

    @Test
    public void fromEncodedStringFullPhoneCityCodeTest() {
        assertEquals("получен правильный код города", "123", FULL_PHONE.getCityCode());
    }

    @Test
    public void fromEncodedStringFullPhoneNumberTest() {
        assertEquals("получен правильный номер телефона", "456-78-90", FULL_PHONE.getPhone());
    }

    @Test
    public void fromEncodedStringFullPhoneExtensionTest() {
        assertEquals("получен правильный ext", "1234", FULL_PHONE.getExt());
    }

    @Test
    public void fromEncodedStringEmptyCountryCodeTest() {
        assertEquals("получен правильный код страны", "", EMPTY_PHONE.getCountryCode());
    }

    @Test
    public void fromEncodedStringEmptyCityCodeTest() {
        assertEquals("получен правильный код города", "", EMPTY_PHONE.getCityCode());
    }

    @Test
    public void fromEncodedStringEmptyPhoneNumberTest() {
        assertEquals("получен правильный номер телефона", "", EMPTY_PHONE.getPhone());
    }

    @Test
    public void fromEncodedStringEmptyExtensionTest() {
        assertEquals("получен правильный ext", "", EMPTY_PHONE.getExt());
    }

    @Test
    public void fromEncodedStringEmptyTest() {
        VcardPhone emptyPhone = new VcardPhone();

        assertEquals(emptyPhone, VcardPhone.fromEncodedString(""));
    }

    @Test
    public void fromEncodedStringNullTest() {
        VcardPhone emptyPhone = new VcardPhone();

        assertEquals(emptyPhone, VcardPhone.fromEncodedString(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromEncodedStringInvalidPhoneTest() {
        VcardPhone.fromEncodedString("0#123#");
    }
}
