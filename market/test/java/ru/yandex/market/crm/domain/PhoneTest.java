package ru.yandex.market.crm.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PhoneTest {

    @Test
    public void check_isEmpty() {
        Assertions.assertTrue(Phone.fromRaw(null).isEmpty());
        Assertions.assertTrue(Phone.fromRaw("").isEmpty());
        Assertions.assertTrue(Phone.empty().isEmpty());

        Assertions.assertFalse(Phone.fromRaw("-aa").isEmpty());
        Assertions.assertFalse(Phone.fromRaw("+7").isEmpty());

        Assertions.assertFalse(Phone.fromNormalized("+7").isEmpty());
    }

    @Test
    public void check_raw_equals() {
        Assertions.assertEquals(Phone.fromRaw("+79123456789").getNormalized(),
                Phone.fromRaw("+79123456789").getNormalized());
        Assertions.assertEquals(Phone.fromRaw("+79123456789").getNormalized(),
                Phone.fromRaw("+7 (912) 345-67-89").getNormalized());
        Assertions.assertEquals(Phone.fromRaw("+79123456789#123").getNormalized(), Phone.fromRaw("+7 (912) 345-67-89 " +
                "доб. 1-23").getNormalized());
        Assertions.assertNotEquals(Phone.fromRaw("+79123456789#123").getNormalized(), Phone.fromRaw("+7 (912) " +
                "345-67-89 доб. 1-24").getNormalized());
        Assertions.assertNotEquals(Phone.fromRaw("+79123456789#123").getNormalized(), Phone.fromRaw("+7 (000) " +
                "345-67-89 доб. 1-23").getNormalized());
        Assertions.assertEquals(Phone.fromRaw("+7 9(123) 456-789").getNormalized(), Phone.fromRaw("+7 (912) " +
                "345-67-89").getNormalized());
    }

    @Test
    public void check_normalized_equals() {
        Assertions.assertEquals(Phone.fromNormalized("+79123456789"), Phone.fromRaw("+79123456789"));
        Assertions.assertEquals(Phone.fromNormalized("+79123456789").getNormalized(), Phone.fromRaw("+7 (912) " +
                "345-67-89").getNormalized());
        Assertions.assertEquals(Phone.fromNormalized("+79123456789#123").getNormalized(), Phone.fromRaw("+7 (912) " +
                "345-67-89 доб. 1-23").getNormalized());
        Assertions.assertNotEquals(Phone.fromNormalized("+79123456789#123").getNormalized(), Phone.fromRaw("+7 (912) " +
                "345-67-89 доб. 1-24").getNormalized());
        Assertions.assertNotEquals(Phone.fromNormalized("+79123456789#123").getNormalized(), Phone.fromRaw("+7 (000) " +
                "345-67-89 доб. 1-23").getNormalized());
        // this method does not check format
        Assertions.assertNotEquals(Phone.fromNormalized("+7 9(123) 456-789").getNormalized(), Phone.fromRaw("+7 9" +
                "(123) 456-789").getNormalized());
        Assertions.assertNotEquals(Phone.fromNormalized("+7 9(123) 456-789").getNormalized(), Phone.fromRaw("+7 (912)" +
                " 345-67-89").getNormalized());
    }

    @Test
    public void check_getMain() {
        Assertions.assertEquals("", Phone.fromRaw(null).getMain());
        Assertions.assertEquals("", Phone.empty().getMain());
        Assertions.assertEquals("+79123456789", Phone.fromRaw("+79123456789").getMain());
        Assertions.assertEquals("+79123456789", Phone.fromRaw("+7 (912) 345-67-89").getMain());
        Assertions.assertEquals("+79123456789", Phone.fromRaw("+7 (912) 345-67-89#22").getMain());

        Assertions.assertEquals("+79123456789", Phone.fromNormalized("+79123456789").getMain());
        Assertions.assertEquals("+79123456789", Phone.fromNormalized("+79123456789#22").getMain());
    }

    @Test
    public void check_getMainWithoutPlus() {
        Assertions.assertEquals("", Phone.fromRaw(null).getMainWithoutPlus());
        Assertions.assertEquals("", Phone.empty().getMainWithoutPlus());
        Assertions.assertEquals("79123456789", Phone.fromRaw("+79123456789").getMainWithoutPlus());
        Assertions.assertEquals("79123456789", Phone.fromRaw("+7 (912) 345-67-89").getMainWithoutPlus());
        Assertions.assertEquals("79123456789", Phone.fromRaw("+7 (912) 345-67-89#22").getMainWithoutPlus());

        Assertions.assertEquals("79123456789", Phone.fromNormalized("+79123456789").getMainWithoutPlus());
        Assertions.assertEquals("79123456789", Phone.fromNormalized("+79123456789#22").getMainWithoutPlus());
    }

    @Test
    public void check_getRaw() {
        Assertions.assertNull(Phone.fromRaw(null).getRaw());
        Assertions.assertNull(Phone.fromRaw("").getRaw());
        Assertions.assertNull(Phone.empty().getRaw());
        Assertions.assertNull(Phone.fromRaw("+79123456789").getRaw());
        Assertions.assertEquals("+7 (912) 345-67-89", Phone.fromRaw("+7 (912) 345-67-89").getRaw());
        Assertions.assertEquals("+7 (912) 345-67-89#22", Phone.fromRaw("+7 (912) 345-67-89#22").getRaw());
        Assertions.assertNull(Phone.fromRaw("+79123456789#22").getRaw());

        Assertions.assertNull(Phone.fromNormalized("+79123456789").getRaw());
    }

    @Test
    public void check_getExt() {
        Assertions.assertNull(Phone.fromRaw(null).getExt());
        Assertions.assertNull(Phone.empty().getExt());
        Assertions.assertNull(Phone.fromRaw("+79123456789").getExt());
        Assertions.assertEquals("23", Phone.fromRaw("+79123456789#23").getExt());
        Assertions.assertNull(Phone.fromRaw("+7 (912) 345-67-89").getExt());
        Assertions.assertEquals("22", Phone.fromRaw("+7 (912) 345-67-89#22").getExt());

        Assertions.assertNull(Phone.fromNormalized("+79123456789").getExt());
        Assertions.assertEquals("23", Phone.fromNormalized("+79123456789#23").getExt());
    }

    @Test
    public void check_toString() {
        Assertions.assertEquals("", Phone.fromRaw(null).toString());
        Assertions.assertEquals("", Phone.fromRaw("").toString());
        Assertions.assertEquals("", Phone.empty().toString());
        Assertions.assertEquals("+79123456789", Phone.fromRaw("+79123456789").toString());
        Assertions.assertEquals("+79123456789", Phone.fromRaw("+7 (912) 345-67-89").toString());
        Assertions.assertEquals("+79123456789#22", Phone.fromRaw("+7 (912) 345-67-89#22").toString());

        Assertions.assertEquals("+79123456789#22", Phone.fromNormalized("+79123456789#22").toString());
    }

    @Test
    public void check_prettyFormat() {
        Assertions.assertEquals("", Phone.fromRaw(null).prettyFormat());
        Assertions.assertEquals("", Phone.fromRaw("").prettyFormat());
        Assertions.assertEquals("", Phone.empty().prettyFormat());
        Assertions.assertEquals("+79123456789", Phone.fromRaw("+79123456789").prettyFormat());
        Assertions.assertEquals("+79123456789", Phone.fromRaw("+7 (912) 345-67-89").prettyFormat());
        Assertions.assertEquals("+79123456789 доб. 22", Phone.fromRaw("+7 (912) 345-67-89#22").prettyFormat());

        Assertions.assertEquals("+79123456789", Phone.fromNormalized("+79123456789").prettyFormat());
        Assertions.assertEquals("+79123456789 доб. 22", Phone.fromRaw("+79123456789#22").prettyFormat());
    }

}
