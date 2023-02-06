package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.delivery.mdbapp.AbstractContextualTest;
import ru.yandex.market.delivery.mdbapp.configuration.integration.persistence.JdbcMetadataStore;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.sc.internal.client.ScIntClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class IdempotentReceiverOrderEventsProcessorDecoratorTest extends AbstractContextualTest {

    @MockBean
    @SuppressWarnings("unused")
    private PechkinHttpClient pechkinHttpClient;

    @MockBean
    @SuppressWarnings("unused")
    private LMSClient lmsClient;

    @MockBean
    @SuppressWarnings("unused")
    private ScIntClient scIntClient;

    @Autowired
    @Qualifier("orderEventsJdbcMetadataStore")
    private JdbcMetadataStore orderEventsJdbcMetadataStore;

    private Exception thrownException;

    @Mock
    private OrderEventsProcessor delegate;
    private OrderEventsProcessor orderEventsProcessor;

    @Before
    public void setUp() {
        orderEventsProcessor = new IdempotentReceiverOrderEventsProcessorDecorator(
            delegate,
            orderEventsJdbcMetadataStore
        );
    }

    @Test
    @Sql("/data/clean-order-events.sql")
    public void processEvent() throws Exception {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(1000000L);

        Thread thread1 = createThread(orderEventsProcessor, orderHistoryEvent);
        Thread thread2 = createThread(orderEventsProcessor, orderHistoryEvent);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        verify(delegate).processEvent(eq(orderHistoryEvent));

        softly.assertThat(thrownException)
            .as("Exception should be thrown")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Reject duplicate event: 1000000");
    }

    private Thread createThread(OrderEventsProcessor orderEventsProcessor, OrderHistoryEvent orderHistoryEvent) {
        return new Thread(() -> {
            try {
                orderEventsProcessor.processEvent(orderHistoryEvent);
            } catch (Exception e) {
                thrownException = e;
            }
        });
    }
}
