package ru.yandex.market.logistics.lom.service.platform;

import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.service.flow.Strategy;
import ru.yandex.market.logistics.lom.service.flow.StrategyProvider;

public class FlowStrategyTest extends AbstractContextualTest {

    @Autowired
    private StrategyProvider provider;

    @Test
    @DisplayName("Выбор стратегий PlatformClient корректно обрабатывается")
    void allStrategiesSetForYandexDelivery() {
        softly.assertThat(provider.provide(PlatformClient.YANDEX_DELIVERY, YandexDeliveryStrategy.class).isPresent());

        softly.assertThatThrownBy(() ->
            provider.provideOrFail(PlatformClient.BERU, YandexDeliveryStrategy.class)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Component
    class YandexDeliveryStrategy implements Strategy<YandexDeliveryStrategy> {

        @Nonnull
        @Override
        public Set<PlatformClient> getSupportedFlow() {
            return Set.of(PlatformClient.YANDEX_DELIVERY);
        }

        @Nonnull
        @Override
        public Class<YandexDeliveryStrategy> getSupportedInterface() {
            return YandexDeliveryStrategy.class;
        }
    }
}
