package ru.yandex.market.pers.address.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UtilsTest {

    @Test
    public void testGetPhoneNumbers1() {
        testGetPhoneNumbers("+7(3452) 666-740", "+73452666740");
    }

    @Test
    public void testGetPhoneNumbers2() {
        testGetPhoneNumbers("+7 (495) 661-79-99", "+74956617999");
    }

    @Test
    public void testGetPhoneNumbers3() {
        testGetPhoneNumbers("+7 495 542-67-30", "+74955426730");
    }

    @Test
    public void testGetPhoneNumbers4() {
        testGetPhoneNumbers("8 800 700-58-29", "+78007005829");
    }

    @Test
    public void testGetPhoneNumbers5() {
        testGetPhoneNumbers("+7495 108-05-27", "+74951080527");
    }

    @Test
    public void testGetPhoneNumbers6() {
        testGetPhoneNumbers("8 (831) 260-16-50", "+78312601650");
    }

    @Test
    public void testGetPhoneNumbers7() {
        testGetPhoneNumbers("+79228682818", "+79228682818");
    }

    @Test
    public void testGetPhoneNumbers8() {
        testGetPhoneNumbers("+7 (812)6005020", "+78126005020");
    }

    @Test
    public void testGetPhoneNumbers9() {
        testGetPhoneNumbers("84993943763", "+74993943763");
    }

    @Test
    public void testGetPhoneNumbers10() {
        testGetPhoneNumbers("+73452 500-686", "+73452500686");
    }

    @Test
    public void testGetPhoneNumbers11() {
        testGetPhoneNumbers("+7(831) 213-88-99", "+78312138899");
    }

    @Test
    public void testGetPhoneNumbers12() {
        testGetPhoneNumbers("+7(495)203-58-76", "+74952035876");
    }

    @Test
    public void testGetPhoneNumbers13() {
        testGetPhoneNumbers("+7(495)5855679", "+74955855679");
    }

    @Test
    public void testGetPhoneNumbers14() {
        testGetPhoneNumbers("8 800 700-2820", "+78007002820");
    }

    @Test
    public void testGetPhoneNumbers15() {
        testGetPhoneNumbers("800 700-2820", "+78007002820");
    }

    @Test
    public void testGetPhoneNumbers16() {
        testGetPhoneNumbers("(831) 213-88-99", "+78312138899");
    }

    @Test
    public void testGetPhoneNumbers17() {
        testGetPhoneNumbers("+7 4952589018 (212)", "+74952589018,212");
    }

    private void testGetPhoneNumbers(String sourceNumber, String expectedshopPhoneForCall) {
        String phoneNumbers = Utils.normalizePhoneNumber(sourceNumber);
        assertEquals(expectedshopPhoneForCall, phoneNumbers);
    }

    @Test
    public void testValidPlatformSuccess() {
        assertEquals("ios", Utils.validatePlatform("ios"));
        assertEquals("android", Utils.validatePlatform("android"));
        assertEquals("go_ios", Utils.validatePlatform("go_ios"));
        assertEquals("go_android", Utils.validatePlatform("go_android"));
        assertEquals("touch", Utils.validatePlatform("touch"));
        assertEquals("api", Utils.validatePlatform("api"));
        assertEquals("platform_valid--123A", Utils.validatePlatform("platform_valid--123A"));
        String string50length = Stream.generate(() -> "a").limit(50).collect(Collectors.joining());
        assertEquals(string50length, Utils.validatePlatform(string50length));
    }

    @Test
    public void testValidPlatformFail() {
        assertNull(Utils.validatePlatform(Stream.generate(() -> "a").limit(51).collect(Collectors.joining())));
        assertNull(Utils.validatePlatform("{123}"));
        assertNull(Utils.validatePlatform("@as"));
        assertNull(Utils.validatePlatform("?123"));
        assertNull(Utils.validatePlatform("~"));
        assertNull(Utils.validatePlatform("\\"));
    }
}
