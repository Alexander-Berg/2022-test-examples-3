package ru.yandex.market.logistics.lom.converter.lgw;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.entity.embedded.Cost;

class OrderCostConverterTest {

    private OrderCostConverter orderCostConverter = new OrderCostConverter();

    @Test
    @DisplayName("Тариф с переводом отображается переведённым")
    void tariffIdToTariffCode() {
        Cost cost = new Cost().setTariffId(2L).setTariffCode("Tariff Name");
        Assertions.assertThat(orderCostConverter.getTariffCode(cost)).isEqualTo("Tariff Name");
    }

    @Test
    @DisplayName("Тариф без перевода приводится к строке")
    void tariffIdToString() {
        Cost cost = new Cost().setTariffId(2L).setTariffCode(null);
        Assertions.assertThat(orderCostConverter.getTariffCode(cost)).isEqualTo("2");
    }
}
