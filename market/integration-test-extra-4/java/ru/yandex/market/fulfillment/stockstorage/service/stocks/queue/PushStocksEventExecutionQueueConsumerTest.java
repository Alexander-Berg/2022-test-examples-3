package ru.yandex.market.fulfillment.stockstorage.service.stocks.queue;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.StockUpdatingStrategyProvider;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.when;

class PushStocksEventExecutionQueueConsumerTest extends AbstractContextualTest {

    @Autowired
    private PushStocksEventExecutionQueueConsumer subject;
    @SpyBean
    private StockUpdatingStrategyProvider stockUpdatingStrategyProvider;

    @Test
    @DatabaseSetup("classpath:database/states/push_stocks_event_execution_queue_consumer/before-queue-with-items.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/push_stocks_event_execution_queue_consumer/after-success.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void consumeSuccessfully() {
        subject.consume();
    }

    @Test
    @DatabaseSetup("classpath:database/states/push_stocks_event_execution_queue_consumer/before-queue-with-items.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/push_stocks_event_execution_queue_consumer/after-failed-message.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void consumeReturnsFailedMessages() {
        when(stockUpdatingStrategyProvider.provide(2)).thenThrow(new RuntimeException("boom"));

        subject.consume();
    }

}
