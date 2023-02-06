package ru.yandex.market.tpl.core.adapter;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.common.util.configuration.Configurable;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class ConfigurationProviderAdapterTest {
    @Mock
    private final ConfigurationProvider configurationProvider;

    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void init() {
        configurationProviderAdapter = new ConfigurationProviderAdapter(configurationProvider);
    }

    @Test
    void getValue() {
        configurationProviderAdapter.getValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES, Integer.class);
        verify(configurationProvider, times(1)).getValue(any(String.class), any());
    }

    @Test
    void getValuesLike() {
        configurationProviderAdapter.getValuesLike("Something");
        verify(configurationProvider, times(1)).getValuesLike(any(String.class));

    }

    @Test
    void getValueAsInteger() {
        configurationProviderAdapter.getValueAsInteger(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsInteger(any(String.class));
    }

    @Test
    void getValueAsLong() {
        configurationProviderAdapter.getValueAsLong(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsLong(any(String.class));
    }

    @Test
    void getValueAsLongs() {
        configurationProviderAdapter.getValueAsLongs(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsLongs(any(String.class));
    }

    @Test
    void getValueAsStrings() {
        configurationProviderAdapter.getValueAsStrings(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsStrings(any(String.class));
    }

    @Test
    void getValueAsInstant() {
        configurationProviderAdapter.getValueAsInstant(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsInstant(any(String.class));
    }

    @Test
    void getValueAsLocalDate() {
        configurationProviderAdapter.getValueAsLocalDate(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsLocalDate(any(String.class));
    }

    @Test
    void getValueAsLocalTime() {
        configurationProviderAdapter.getValueAsLocalTime(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsLocalTime(any(String.class));
    }

    @Test
    void getValueAsLocalDateTime() {
        configurationProviderAdapter.getValueAsLocalDateTime(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsLocalDateTime(any(String.class));
    }

    @Test
    void getValueAsDuration() {
        configurationProviderAdapter.getValueAsDuration(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsDuration(any(String.class));
    }

    @Test
    void getValueWithoutType() {
        configurationProviderAdapter.getValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValue(any(String.class));
    }

    @Test
    void isBooleanEnabled() {
        configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).isBooleanEnabled(any(String.class));
    }

    @Test
    void getValueAsDouble() {
        configurationProviderAdapter.getValueAsDouble(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationProvider, times(1)).getValueAsDouble(any(String.class));
    }

    @Test
    void getValueСonfigurable() {
        configurationProviderAdapter.getValue(RoutingVehicleType.COMMON);
        verify(configurationProvider, times(1)).getValue(any(Configurable.class));
    }


}

