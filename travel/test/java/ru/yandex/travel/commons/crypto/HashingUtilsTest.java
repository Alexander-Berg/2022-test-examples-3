package ru.yandex.travel.commons.crypto;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class HashingUtilsTest {

    @Test
    public void testPhoneHashing() {
        assertThat(HashingUtils.hashPhone("123-45-67")).isEqualTo(HashingUtils.hashPhone("+12-34 567"));
        assertThat(HashingUtils.hashPhone("+7-123-45-67")).isEqualTo(HashingUtils.hashPhone("8-1234567"));
        assertThat(HashingUtils.hashPhone("007-123-45-67")).isEqualTo(HashingUtils.hashPhone("8-1234567"));
        assertThat(HashingUtils.hashPhone("7-123-45-67")).isEqualTo(HashingUtils.hashPhone("8-1234567"));
        assertThat(HashingUtils.hashPhone("1-123-45-67")).isNotEqualTo(HashingUtils.hashPhone("8-1234567"));
    }

    @Test
    public void testEmailHashing() {
        assertThat(HashingUtils.hashEmail("test@example.com")).isEqualTo(HashingUtils.hashEmail("Test@Example.Com"));
    }
}
