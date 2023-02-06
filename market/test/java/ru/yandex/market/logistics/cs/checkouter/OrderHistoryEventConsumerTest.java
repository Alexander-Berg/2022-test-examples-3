package ru.yandex.market.logistics.cs.checkouter;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistics.cs.AbstractTest;
import ru.yandex.market.logistics.cs.config.FeaturePropertiesConfiguration;
import ru.yandex.market.logistics.cs.dbqueue.logbroker.checkouter.LogbrokerCheckouterConsumptionProducer;
import ru.yandex.market.logistics.cs.logbroker.EventProcessor;
import ru.yandex.market.logistics.cs.logbroker.checkouter.OrderHistoryEventConsumer;
import ru.yandex.market.logistics.cs.util.TestDtoFactory;
import ru.yandex.market.logistics.cs.util.TestDtoFactory.SingleServiceOrderHistoryEvent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.REFUND;

@DisplayName("Консьюмер событий чекаутера")
class OrderHistoryEventConsumerTest extends AbstractTest {

    @Mock
    private LogbrokerCheckouterConsumptionProducer logbrokerCheckouterConsumptionProducer;

    @Mock
    private EventProcessor<OrderHistoryEvent> checkouterEventsProcessor;

    @Autowired
    private FeaturePropertiesConfiguration featurePropertiesConfiguration;

    private OrderHistoryEventConsumer consumer;

    @BeforeEach
    void setup() {
        consumer = new OrderHistoryEventConsumer(
            logbrokerCheckouterConsumptionProducer,
            checkouterEventsProcessor,
            featurePropertiesConfiguration
        );
    }

    @Test
    void smokeTest() {
        assertDoesNotThrow(() -> consumer.consume(List.of()));
    }

    private static Stream<Arguments> filteredEventTypeMappingProvider() {
        return Stream.of(
            Arguments.of(REFUND, null),
            Arguments.of(ORDER_STATUS_UPDATED, OrderStatus.DELIVERED)
        );
    }

    @DisplayName(value = "Фильтрация события с набором состояний (historyEvent.type, orderAfter.status)")
    @ParameterizedTest(name = "({0}, {1})")
    @MethodSource("filteredEventTypeMappingProvider")
    void singleEventTypeFiltering(HistoryEventType historyEventType, OrderStatus orderStatus) {
        SingleServiceOrderHistoryEvent testData =
            TestDtoFactory.singleServiceWithParcelOrder(historyEventType, orderStatus);

        List<OrderHistoryEvent> events = List.of(testData.getEvent());
        consumer.consume(events);

        verifyNoMoreInteractions(logbrokerCheckouterConsumptionProducer);
        verifyNoMoreInteractions(checkouterEventsProcessor);
    }
}
