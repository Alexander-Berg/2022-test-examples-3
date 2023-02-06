package ru.yandex.market.fulfillment.stockstorage.service.stocks.queue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.StocksState;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueItem;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.service.execution.ExecutionQueueType;
import ru.yandex.market.fulfillment.stockstorage.service.helper.IdGenerator;
import ru.yandex.market.fulfillment.stockstorage.service.queue.ExecutionQueueService;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyIntegerKey.TASK_WRITE_PUSH_STOCKS_BATCH_SIZE;

@ExtendWith(SpringExtension.class)
class PushStocksEventExecutionQueueProducerTest {

    @InjectMocks
    private PushStocksEventExecutionQueueProducer subject;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private ExecutionQueueService executionQueueService;
    @Mock
    private SystemPropertyService systemPropertyService;
    private LocalDateTime dateTimeFixed = LocalDateTime.now();
    @Spy
    private Clock clock = Clock.fixed(dateTimeFixed.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private String id = "d720999c-19f7-495f-915e-067f910ffdb5";


    @Test
    void pushSuccessful() {
        when(idGenerator.get()).thenReturn(id);
        when(systemPropertyService.getIntegerProperty(TASK_WRITE_PUSH_STOCKS_BATCH_SIZE)).thenReturn(50);

        List<StocksState> stocks = List.of(getStocksState(), getStocksState());

        PushStocksExecutionQueuePayload payload = new PushStocksExecutionQueuePayload(id, stocks.stream()
                .map(PushStockPayload::new)
                .collect(Collectors.toList()));

        subject.push(stocks);

        ExecutionQueueItem<?> itemsExpected = ExecutionQueueItem.of(
                        dateTimeFixed,
                        dateTimeFixed,
                        ExecutionQueueType.PUSH_STOCKS_EVENT,
                        payload);

        verify(executionQueueService, times(1)).pushUnchecked(List.of(itemsExpected));
    }

    @Test
    void pushProxiesExceptions() {
        List<StocksState> stocks = List.of(getStocksState(), getStocksState());

        doThrow(new RuntimeException("test")).when(executionQueueService).pushUnchecked(any(Collection.class));

        assertThrows(RuntimeException.class, () -> {
            subject.push(stocks);
        });
    }

    private StocksState getStocksState() {
        return new StocksState(
                new UnitId("some-sku", 1L, 1),
                Map.of(),
                0
        );
    }

}
