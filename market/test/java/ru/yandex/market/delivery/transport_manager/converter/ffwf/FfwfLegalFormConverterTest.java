package ru.yandex.market.delivery.transport_manager.converter.ffwf;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.ff.client.enums.LegalFormType;

import static org.assertj.core.api.Assertions.assertThat;

public class FfwfLegalFormConverterTest {
    private final FfwfLegalFormConverter legalFormConverter = new FfwfLegalFormConverter();

    @ParameterizedTest
    @ValueSource(strings = {"ip", "IP", "ИП", "ип"})
    void legalFormType_ip(String s) {
        assertThat(legalFormConverter.legalFormType(s)).isEqualTo(LegalFormType.IP);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "ABC", "3"})
    @NullSource
    void legalFormType_unknown(String s) {
        assertThat(legalFormConverter.legalFormType(s)).isEqualTo(LegalFormType.UNKNOWN);
    }
}
