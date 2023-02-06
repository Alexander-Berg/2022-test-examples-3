package ru.yandex.market.mbi.audit.service.yt.dao;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.mbi.audit.service.yt.model.LogEntityHistory;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtClientProxySource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class EntityHistoryRepositoryTest {
    private EntityHistoryRepository repository;
    private YtClientProxy mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(YtClientProxy.class);
        repository = new EntityHistoryRepository(mockClient, mock(YtClientProxySource.class),
                mock(BindingTable.class),
                new RetryTemplate());
    }

    @Test
    void saveEntityHistoryAll_emptyList() {
        List<LogEntityHistory> emptyLogsList = new ArrayList<>();

        repository.saveEntityHistoryAll(emptyLogsList);

        verify(mockClient, times(0)).insertRows(anyString(), any(), any());
    }
}
