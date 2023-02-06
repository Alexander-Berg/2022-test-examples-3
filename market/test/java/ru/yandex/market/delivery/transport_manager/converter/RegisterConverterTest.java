package ru.yandex.market.delivery.transport_manager.converter;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.dto.RegisterOrdersCountDto;

class RegisterConverterTest {
    @Test
    void convertRegisterOrdersCount() {
        List<ru.yandex.market.delivery.transport_manager.model.dto.RegisterOrdersCountDto> actual =
            RegisterConverter.convertRegisterOrdersCount(List.of(
                new RegisterOrdersCountDto(1L, 10L),
                new RegisterOrdersCountDto(2L, 20L)
            ));

        Assertions.assertThat(actual)
            .isEqualTo(List.of(
                new ru.yandex.market.delivery.transport_manager.model.dto.RegisterOrdersCountDto(1L, 10L),
                new ru.yandex.market.delivery.transport_manager.model.dto.RegisterOrdersCountDto(2L, 20L)
            ));
    }
}
