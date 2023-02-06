package ru.yandex.market.tpl.common.util.phone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberTest {
    @Test
    void testPhoneNormalization() {
        assertEquals("1234567890", PhoneNumber.of("1234567890").getShortNumber());
        assertEquals("71234567890", PhoneNumber.of("1234567890").getFullNumber());
        assertEquals("1234567890", PhoneNumber.of("81234567890").getShortNumber());
        assertEquals("81234567890", PhoneNumber.of("81234567890").getFullNumber());
        assertEquals("1234567890", PhoneNumber.of("123 456 78 90").getShortNumber());
        assertEquals("1234567890", PhoneNumber.of("(123) 4567890").getShortNumber());
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of(""));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of(null));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of("1234"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of(".>/"));
    }
}
