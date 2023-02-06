package ru.yandex.market.mbi.logprocessor.storage.yt.dao;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.mbi.logprocessor.storage.yt.model.ApiLogEntity;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtClientProxySource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ApiLogRepositoryTest {

    private ApiLogRepository repository;
    private YtClientProxy mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(YtClientProxy.class);
        repository = new ApiLogRepository(mockClient, mock(YtClientProxySource.class),
                mock(BindingTable.class),
                new RetryTemplate());
    }

    @Test
    void saveAll_emptyList() {
        List<ApiLogEntity> emptyApiLogsList = new ArrayList<>();

        repository.saveAll(emptyApiLogsList);

        verify(mockClient, times(0)).insertRows(anyString(), any(), any());
    }
}
