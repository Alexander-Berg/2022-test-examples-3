package ru.yandex.market.logistics.lom.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;

public class SolomonMetricsConverterTest extends AbstractTest {

    private final SolomonMetricsConverter converter = new SolomonMetricsConverter();

    @Test
    public void prepareLabelValue() {
        String testStringWithoutChange = "A1B2C D_2-d";
        softly.assertThat(converter.prepareLabelValue(testStringWithoutChange))
            .isEqualTo(testStringWithoutChange);
        softly.assertThat(converter.prepareLabelValue("АБВГДЕЯ")).isEqualTo("ABVGDEYA");
        softly.assertThat(converter.prepareLabelValue("абвгдея")).isEqualTo("abvgdeya");
        softly.assertThat(converter.prepareLabelValue("МК Восток-1")).isEqualTo("MK Vostok-1");
        softly.assertThat(converter.prepareLabelValue("DropShip_Терракот")).isEqualTo("DropShip_Terrakot");
        softly.assertThat(converter.prepareLabelValue("А*Б?В\"Г'D`1")).isEqualTo("A_B_V_G_D_1");
    }
}
