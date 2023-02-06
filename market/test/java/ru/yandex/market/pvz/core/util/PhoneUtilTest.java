package ru.yandex.market.pvz.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneUtilTest {

    private static final String FORMATTED_PHONE = "+79992281488";

    @Test
    void testPhoneIsFormattedCorrectly() {
        assertThat(PhoneUtil.format("9992281488")).isEqualTo(FORMATTED_PHONE);
        assertThat(PhoneUtil.format("89992281488")).isEqualTo(FORMATTED_PHONE);
        assertThat(PhoneUtil.format("79992281488")).isEqualTo(FORMATTED_PHONE);
        assertThat(PhoneUtil.format("+79992281488")).isEqualTo(FORMATTED_PHONE);
        assertThat(PhoneUtil.format("+7 999 228-14-88")).isEqualTo(FORMATTED_PHONE);
        assertThat(PhoneUtil.format("+7 (999) 228-14-88")).isEqualTo(FORMATTED_PHONE);
        assertThat(PhoneUtil.format("+7 (999) 228-14-88")).isEqualTo(FORMATTED_PHONE);
    }

}
