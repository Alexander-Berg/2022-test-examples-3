package ru.yandex.market.tpl.core.adapter;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class ConfigurationServiceAdapterTest {
    @Mock
    private final ConfigurationService configurationService;

    private ConfigurationServiceAdapter configurationServiceAdapter;

    @BeforeEach
    void init() {
        configurationServiceAdapter = new ConfigurationServiceAdapter(configurationService);
    }

    @Test
    void getValue() {
        configurationServiceAdapter.getValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES, String.class);
        verify(configurationService, times(1)).getValue(any(String.class), any());
    }

    @Test
    void tryGetOrMergeVal() {
        configurationServiceAdapter.tryGetOrMergeVal(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES,
                String.class, "Something");
        verify(configurationService, times(1)).tryGetOrMergeVal(any(String.class), eq(String.class), eq("Something"));

    }

    @Test
    void updateValue() {
        configurationServiceAdapter.updateValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES, 50);
        verify(configurationService, atLeast(1)).updateValue(any(String.class), any(Integer.class));
    }

    @Test
    void updateValueInstant() {
        configurationServiceAdapter.updateValueInstant(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES,
                Instant.MAX);
        verify(configurationService, times(1)).updateValueInstant(any(String.class), any(Instant.class));
    }

    @Test
    void updateValueString() {
        configurationServiceAdapter.updateValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES, "Something");
        verify(configurationService, times(1)).updateValue(any(String.class), any(String.class));
    }

    @Test
    void insertValue() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES, 50);
        verify(configurationService, times(1)).insertValue(any(String.class), any(Integer.class));
    }

    @Test
    void insertValueInstant() {
        configurationServiceAdapter.insertValueInstant(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES,
                Instant.MAX);
        verify(configurationService, times(1)).insertValueInstant(any(String.class), any(Instant.class));
    }

    @Test
    void insertValueString() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES, "Something");
        verify(configurationService, times(1)).insertValue(any(String.class), any(String.class));
    }

    @Test
    void mergeValueString() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES, "Something");
        verify(configurationService, times(1)).mergeValue(any(String.class), any(String.class));
    }

    @Test
    void mergeValue() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES, 50);
        verify(configurationService, times(1)).mergeValue(any(String.class), any(Integer.class));
    }

    @Test
    void mergeValueAsInstant() {
        configurationServiceAdapter.mergeValueAsInstant(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES,
                Instant.MAX);
        verify(configurationService, times(1)).mergeValueAsInstant(any(String.class), any(Instant.class));
    }

    @Test
    void deleteValue() {
        configurationServiceAdapter.deleteValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES);
        verify(configurationService, times(1)).deleteValue(any(String.class));
    }

    @Test
    void getValuesLike() {
        configurationServiceAdapter.getValuesLike("Something");
        verify(configurationService, times(1)).getValuesLike(any(String.class));
    }

    @Test
    void getCount() {
        configurationServiceAdapter.getCount();
        verify(configurationService, times(1)).getCount();
    }

    @Test
    void getCountLikePrefix() {
        configurationServiceAdapter.getCountLikePrefix("Something");
        verify(configurationService, times(1)).getCountLikePrefix(any(String.class));
    }

    @Test
    void getAll() {
        Pageable pageable = mock(Pageable.class);
        Mockito.when(pageable.getPageSize()).thenReturn(10);
        Mockito.when(pageable.getOffset()).thenReturn(5L);
        configurationServiceAdapter.getAll(pageable);
        verify(configurationService, times(1)).getAll(any(Integer.class), any(Long.class));
    }

    @Test
    void getAllLikePrefix() {
        Pageable pageable = mock(Pageable.class);
        Mockito.when(pageable.getPageSize()).thenReturn(10);
        Mockito.when(pageable.getOffset()).thenReturn(5L);
        configurationServiceAdapter.getAllLikePrefix("Something", pageable);
        verify(configurationService, times(1)).getAllLikePrefix(any(String.class), any(Integer.class), any(Long.class));
    }

    @Test
    void getById() {
        configurationServiceAdapter.getById(1L);
        verify(configurationService, times(1)).getById(any(Long.class));
    }

    @Test
    void updateValueByKey() {
        configurationServiceAdapter.updateValueByKey(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES,
                "Something");
        verify(configurationService, times(1)).updateValueByKey(any(String.class), any(String.class));
    }

    @Test
    void insertKeyAndValue() {
        configurationServiceAdapter.insertKeyAndValue(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES,
                "Something");
        verify(configurationService, times(1)).insertKeyAndValue(any(String.class), any(String.class));
    }

    @Test
    void deleteById() {
        configurationServiceAdapter.deleteById(1L);
        verify(configurationService, times(1)).deleteById(any(Long.class));
    }

}
