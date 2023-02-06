package ru.yandex.travel.api.services.common;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CountryNameServiceTests {
    private CountryNameService service;

    @Before
    public void setUp() throws Exception {
        service = new CountryNameService();
        service.afterPropertiesSet();
    }

    @Test
    public void testCommonCodes() {
        assertThat(service.resolveCountryCode("RU")).isEqualTo("Россия");
        assertThat(service.resolveCountryCode("UA")).isEqualTo("Украина");
        assertThat(service.resolveCountryCode("TR")).isEqualTo("Турция");
        assertThat(service.resolveCountryCode("BY")).isEqualTo("Беларусь");
        assertThat(service.resolveCountryCode("KZ")).isEqualTo("Казахстан");
    }

    @Test
    public void testDefault() {
        assertThat(service.resolveCountryCode("XYZ")).isEqualTo("XYZ");
    }
}
