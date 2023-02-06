package ru.yandex.market.delivery.mdbapp.components.consumer;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.OrderEventsService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Тест сохранения событий чекаутера во внутреннюю очередь")
class OrderHistoryEventConsumerMediumIntegrationTest extends AbstractMediumContextualTest {

    @Autowired
    private OrderEventsService eventsService;

    @Autowired
    private CheckouterServiceClient checkouterServiceClient;

    @Test
    @ExpectedDatabase(
        value = "/components/consumer/saved_event.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @DisplayName("Успех")
    public void testSuccess() {
        OrderHistoryEventConsumer orderHistoryEventConsumer = new OrderHistoryEventConsumer(
            checkouterServiceClient,
            eventsService
        );
        OrderHistoryEvent event = createEvent();
        orderHistoryEventConsumer.accept(List.of(event));
    }

    @Nonnull
    private OrderHistoryEvent createEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(1L);
        event.setType(HistoryEventType.NEW_ORDER);
        Order orderAfter = new Order();
        orderAfter.setId(123L);
        Delivery delivery = new Delivery();
        DeliveryDates deliveryDates = new DeliveryDates();
        deliveryDates.setFromDate(Date.from(Instant.parse("2022-06-17T00:00:00Z")));
        delivery.setDeliveryDates(deliveryDates);
        orderAfter.setDelivery(delivery);
        event.setOrderAfter(orderAfter);
        return event;
    }
}
