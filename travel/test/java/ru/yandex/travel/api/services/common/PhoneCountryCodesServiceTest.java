package ru.yandex.travel.api.services.common;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PhoneCountryCodesServiceTest {
    private final PhoneCountryCodesService service = new PhoneCountryCodesService();

    @Test
    public void testCountryCode() {
        assertThat(service.getCountryCodeSafe("79161234567")).isEqualTo(7);
        assertThat(service.getCountryCodeSafe("19161234567")).isEqualTo(1);
        assertThat(service.getCountryCodeSafe("375123456789")).isEqualTo(375);
        assertThat(service.getCountryCodeSafe("4912345678")).isEqualTo(49);
        assertThat(service.getCountryCodeSafe("9991234567890")).isEqualTo(999);
    }
}
