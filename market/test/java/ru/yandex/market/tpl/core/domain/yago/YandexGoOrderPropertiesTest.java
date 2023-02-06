package ru.yandex.market.tpl.core.domain.yago;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YandexGoOrderPropertiesTest {

    public static final Long DELIVERY_SERVICE_ID = 123L;

    @InjectMocks
    YandexGoOrderProperties props;

    @Mock
    ConfigurationProviderAdapter configurationProviderAdapter;

    @Test
    void shouldReturnExpectedValue_whenGetDeliveryService_ifValueExists() {
        // given
        when(configurationProviderAdapter.getValue(ConfigurationProperties.YANDEX_GO_DELIVERY_SERVICE_ID))
                .thenReturn(Optional.of(String.valueOf(DELIVERY_SERVICE_ID)));

        // when
        Optional<Long> deliveryServiceId = props.getDeliveryServiceId();

        // then
        assertThat(deliveryServiceId).isEqualTo(Optional.of(DELIVERY_SERVICE_ID));
    }

    @Test
    void shouldReturnEmptyValue_whenGetDeliveryService_ifValueDoesNotExist() {
        // given
        when(configurationProviderAdapter.getValue(ConfigurationProperties.YANDEX_GO_DELIVERY_SERVICE_ID))
                .thenReturn(Optional.empty());

        // when
        Optional<Long> deliveryServiceId = props.getDeliveryServiceId();

        // then
        assertThat(deliveryServiceId).isEqualTo(Optional.empty());
    }

    @Test
    void shouldReturnExpectedValue_whenRequiredDeliveryService_ifValueExists() {
        // given
        when(configurationProviderAdapter.getValue(ConfigurationProperties.YANDEX_GO_DELIVERY_SERVICE_ID))
                .thenReturn(Optional.of(String.valueOf(DELIVERY_SERVICE_ID)));

        // when
        Long deliveryServiceId = props.requireDeliveryServiceId();

        // then
        assertThat(deliveryServiceId).isEqualTo(DELIVERY_SERVICE_ID);
    }

    @Test
    void shouldThrowException_whenRequireDeliveryService_ifValueDoesNotExist() {
        // given
        when(configurationProviderAdapter.getValue(ConfigurationProperties.YANDEX_GO_DELIVERY_SERVICE_ID))
                .thenReturn(Optional.empty());

        // when
        assertThrows(RuntimeException.class, () -> {
            props.requireDeliveryServiceId();
        });
    }
}
