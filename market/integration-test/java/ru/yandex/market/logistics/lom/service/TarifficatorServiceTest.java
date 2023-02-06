package ru.yandex.market.logistics.lom.service;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.service.tarifficator.TarifficatorService;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;

import static org.mockito.Mockito.when;

public class TarifficatorServiceTest extends AbstractContextualTest {

    @Autowired
    private TarifficatorClient tarifficatorClient;

    @Autowired
    private TarifficatorService tarifficatorService;

    @Test
    @DisplayName("Тариф найден в Тарификаторе")
    public void found() {
        when(tarifficatorClient.getOptionalTariff(1))
            .thenReturn(Optional.of(TariffDto.builder().id(1L).code("code").build()));

        Optional<TariffDto> optionalTariff = tarifficatorService.getTariff(1L);
        softly.assertThat(optionalTariff).hasValueSatisfying(
            tariff -> {
                softly.assertThat(tariff.getCode()).isEqualTo("code");
                softly.assertThat(tariff.getId()).isEqualTo(1L);
            }
        );
    }

    @Test
    @DisplayName("Тариф не найден в Тарификаторе")
    void notFound() {
        Optional<TariffDto> tariff = tarifficatorService.getTariff(2L);
        softly.assertThat(tariff).isEmpty();
    }
}
