package ru.yandex.market.pers.notify.util;

import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author semin-serg
 */
public class PhoneNumberHelperTest {

    @Test
    public void testGetPhoneNumbers1() {
        testGetPhoneNumbers("+7(3452) 666-740", "tel:+73452666740");
    }

    @Test
    public void testGetPhoneNumbers2() {
        testGetPhoneNumbers("+7 (495) 661-79-99", "tel:+74956617999");
    }

    @Test
    public void testGetPhoneNumbers3() {
        testGetPhoneNumbers("+7 495 542-67-30", "tel:+74955426730");
    }

    @Test
    public void testGetPhoneNumbers4() {
        testGetPhoneNumbers("8 800 700-58-29", "tel:+78007005829");
    }

    @Test
    public void testGetPhoneNumbers5() {
        testGetPhoneNumbers("+7495 108-05-27", "tel:+74951080527");
    }

    @Test
    public void testGetPhoneNumbers6() {
        testGetPhoneNumbers("8 (831) 260-16-50", "tel:+78312601650");
    }

    @Test
    public void testGetPhoneNumbers7() {
        testGetPhoneNumbers("+79228682818", "tel:+79228682818");
    }

    @Test
    public void testGetPhoneNumbers8() {
        testGetPhoneNumbers("+7 (812)6005020", "tel:+78126005020");
    }

    @Test
    public void testGetPhoneNumbers9() {
        testGetPhoneNumbers("84993943763", "tel:+74993943763");
    }

    @Test
    public void testGetPhoneNumbers10() {
        testGetPhoneNumbers("+73452 500-686", "tel:+73452500686");
    }

    @Test
    public void testGetPhoneNumbers11() {
        testGetPhoneNumbers("+7(831) 213-88-99", "tel:+78312138899");
    }

    @Test
    public void testGetPhoneNumbers12() {
        testGetPhoneNumbers("+7(495)203-58-76", "tel:+74952035876");
    }

    @Test
    public void testGetPhoneNumbers13() {
        testGetPhoneNumbers("+7(495)5855679", "tel:+74955855679");
    }

    @Test
    public void testGetPhoneNumbers14() {
        testGetPhoneNumbers("8 800 700-2820", "tel:+78007002820");
    }

    @Test
    public void testGetPhoneNumbers15() {
        testGetPhoneNumbers("800 700-2820", "tel:+78007002820");
    }

    @Test
    public void testGetPhoneNumbers16() {
        testGetPhoneNumbers("(831) 213-88-99", "tel:+78312138899");
    }

    @Test
    public void testGetPhoneNumbers17() {
        testGetPhoneNumbers("+7 4952589018 (212)", "tel:+74952589018,212");
    }

    @Test
    public void testGetPhoneNumbers18() {
        testGetPhoneNumbers("", "tel:");
    }

    @Test
    public void testPreparePhoneForCall1() {
        testPreparePhoneForCall("8", "800", "100-0503", null, "tel:+78001000503");
    }

    @Test
    public void testPreparePhoneForCall2() {
        testPreparePhoneForCall("8", "812", "407-2024", null, "tel:+78124072024");
    }

    @Test
    public void testPreparePhoneForCall3() {
        testPreparePhoneForCall("7", "800", "2228000", null, "tel:+78002228000");
    }

    @Test
    public void testPreparePhoneForCall4() {
        testPreparePhoneForCall("7", "495", "215-2070", null, "tel:+74952152070");
    }

    @Test
    public void testPreparePhoneForCall5() {
        testPreparePhoneForCall("7", "495", "7397000", "71740", "tel:+74957397000,71740");
    }

    @Test
    public void testPreparePhoneForCall6() {
        testPreparePhoneForCall("8", "495", "7397000", "71740", "tel:+74957397000,71740");
    }

    private void testGetPhoneNumbers(String sourceNumber, String expectedshopPhoneForCall) {
        Map<String, Object> phoneNumbers = PhoneNumberHelper.getPhoneNumbers(sourceNumber);
        String shopPhone = (String) phoneNumbers.get("shop_phone");
        assertEquals(sourceNumber, shopPhone);
        String shopPhoneForCall = (String) phoneNumbers.get("shop_phone_for_call");
        assertEquals(expectedshopPhoneForCall, shopPhoneForCall);
    }

    private void testPreparePhoneForCall(String countryCode, String cityCode, String number, String extNumber,
                                         String expectedNormalizedNumber) {
        ShopOutletPhone shopOutletPhone = new ShopOutletPhone(countryCode, cityCode, number, extNumber);
        String actualNormalizedNumber = PhoneNumberHelper.preparePhoneForCall(shopOutletPhone);
        assertEquals(expectedNormalizedNumber, actualNormalizedNumber);
    }

}
