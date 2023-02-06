package ru.yandex.market.delivery.transport_manager.converter;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.dto.LogisticPointAddress;

class LogisticPointConverterTest {
    @Test
    void convertAddresses() {
        List<ru.yandex.market.delivery.transport_manager.model.dto.LogisticPointAddress> actual =
            LogisticPointConverter.convertAddresses(List.of(
                new LogisticPointAddress(1L, BigDecimal.valueOf(37.65), BigDecimal.valueOf(51.1)),
                new LogisticPointAddress(2L, BigDecimal.valueOf(45.2), BigDecimal.valueOf(41.1))
            ));

        Assertions.assertThat(actual)
            .isEqualTo(List.of(
                new ru.yandex.market.delivery.transport_manager.model.dto.LogisticPointAddress(
                    1L, BigDecimal.valueOf(37.65), BigDecimal.valueOf(51.1)
                ),
                new ru.yandex.market.delivery.transport_manager.model.dto.LogisticPointAddress(
                    2L, BigDecimal.valueOf(45.2), BigDecimal.valueOf(41.1)
                )
            ));
    }
}
