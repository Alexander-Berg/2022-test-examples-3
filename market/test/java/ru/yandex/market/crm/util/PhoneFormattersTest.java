package ru.yandex.market.crm.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PhoneFormattersTest {

    @Test
    public void check_normalizeMainPhone() {
        Assertions.assertEquals("", PhoneFormatters.normalizeMainPhone("--"));
        Assertions.assertEquals("+79221234567", PhoneFormatters.normalizeMainPhone("+7(922)1234567"));
        Assertions.assertEquals("+79221234567", PhoneFormatters.normalizeMainPhone("+7 922 123 45 67"));
        Assertions.assertEquals("+79221112233", PhoneFormatters.normalizeMainPhone("+7 922 111 22-33"));
        Assertions.assertEquals("+79221234567", PhoneFormatters.normalizeMainPhone("+ 7-922-123-45-67"));
        Assertions.assertEquals("+79221234567", PhoneFormatters.normalizeMainPhone("8(922) 123-45-67"));
        Assertions.assertEquals("+79221234567", PhoneFormatters.normalizeMainPhone("922 123-45-67"));
        Assertions.assertEquals("0074951112245", PhoneFormatters.normalizeMainPhone("00 7 495 111 22 45"));
        Assertions.assertEquals("+375171231234", PhoneFormatters.normalizeMainPhone("+375 (17) 123 12 34"));
        Assertions.assertEquals("+375171231234", PhoneFormatters.normalizeMainPhone("+ 375 (17) 123 12 34"));
        Assertions.assertEquals("39221112233", PhoneFormatters.normalizeMainPhone(" 3 922 111 22 33"));
        Assertions.assertEquals("+3111", PhoneFormatters.normalizeMainPhone("+3 11 1"));
        Assertions.assertEquals("+79221234567", PhoneFormatters.normalizeMainPhone("(922)1234567"));
        Assertions.assertEquals("+79221234567", PhoneFormatters.normalizeMainPhone("7(922)1234567"));
        Assertions.assertEquals("+79221112233", PhoneFormatters.normalizeMainPhone(" 7 922 111 22 33"));
    }

    @Test
    public void check_removeLeadingPlus() {
        Assertions.assertEquals("79221112233", PhoneFormatters.removeLeadingPlus("+79221112233"));
        Assertions.assertEquals("79221112233", PhoneFormatters.removeLeadingPlus("79221112233"));
        Assertions.assertEquals("39221112233", PhoneFormatters.removeLeadingPlus("+39221112233"));
        Assertions.assertEquals("39221112233", PhoneFormatters.removeLeadingPlus("39221112233"));
        Assertions.assertEquals("3111", PhoneFormatters.removeLeadingPlus("+3111"));
    }

    /**
     * Check normalization of numbers with extension on examples from
     * <a href="https://ru.wikipedia.org/wiki/%D0%A2%D0%B5%D0%BB%D0%B5%D1%84%D0%BE%D0%BD%D0%BD%D1%8B%D0%B9_%D0%BD%D0%BE%D0%BC%D0%B5%D1%80#%D0%92%D0%BD%D1%83%D1%82%D1%80%D0%B5%D0%BD%D0%BD%D0%B8%D0%B5_%D0%BD%D0%BE%D0%BC%D0%B5%D1%80%D0%B0">Wiki/Телефонный номер</a>
     */
    @Test
    public void check_normalizeMainPhone_of_ext() {
        Assertions.assertEquals("+71234567890", PhoneFormatters.normalizeMainPhone("(+7 123); 456-78-90 доб. 221"));
        Assertions.assertEquals("+71234567890", PhoneFormatters.normalizeMainPhone("(+7 123) 456-78-90 #221"));
        Assertions.assertEquals("+71234567890", PhoneFormatters.normalizeMainPhone("+71234567890 ext. 221"));
        Assertions.assertEquals("+71234567890", PhoneFormatters.normalizeMainPhone("+71234567890 ext. 22-11"));
    }

    @Test
    public void check_getExt() {
        Assertions.assertNull(PhoneFormatters.getExt("+71234567890"));
        Assertions.assertNull(PhoneFormatters.getExt("(+7 123); 456-78-90"));
        Assertions.assertEquals("221", PhoneFormatters.getExt("(+7 123); 456-78-90 доб. 221"));
        Assertions.assertEquals("221", PhoneFormatters.getExt("(+7 123); 456-78-90 вн. 221"));
        Assertions.assertEquals("221", PhoneFormatters.getExt("(+7 123) 456-78-90 #221"));
        Assertions.assertEquals("221", PhoneFormatters.getExt("+71234567890 ext. 221"));
        Assertions.assertEquals("2211", PhoneFormatters.getExt("+71234567890 ext. 22-11"));
    }

    @Test
    public void check_normalizePhoneSearchPrefix() {
        Assertions.assertEquals("+792212", PhoneFormatters.normalizePhoneSearchPrefix("+7(922)12"));
        Assertions.assertEquals("+7922", PhoneFormatters.normalizePhoneSearchPrefix("+7 922 "));
        Assertions.assertEquals("+7922", PhoneFormatters.normalizePhoneSearchPrefix("+ 7-922-"));
        Assertions.assertEquals("+792", PhoneFormatters.normalizePhoneSearchPrefix("8(92"));
        Assertions.assertEquals("+792", PhoneFormatters.normalizePhoneSearchPrefix("792"));
        Assertions.assertEquals("+7922123456", PhoneFormatters.normalizePhoneSearchPrefix("922 123-45-6"));
        Assertions.assertEquals("007495111", PhoneFormatters.normalizePhoneSearchPrefix("00 7 495 111"));
        Assertions.assertEquals("+375", PhoneFormatters.normalizePhoneSearchPrefix("+375"));
    }

}
