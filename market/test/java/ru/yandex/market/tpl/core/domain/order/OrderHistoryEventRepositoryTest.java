package ru.yandex.market.tpl.core.domain.order;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.difference.AddressOrderDifference;
import ru.yandex.market.tpl.core.domain.order.difference.RecipientDataOrderDifference;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderHistoryEventRepositoryTest {
    private static final String externalOrderId = "424242orderID!";
    private static final int itemsCount = 3;

    private static final String address = "address 123";
    private static final String name = "Default Name";
    private static final String phone = "+79251231212";

    private final TestDataFactory testDataFactory;
    private final OrderHistoryEventRepository orderHistoryEventRepository;

    @Test
    public void findHistoricalEventsInRightOrder() {
        var orderId = createOrder();
        var date = Instant.now();
        testDataFactory.createEvent(orderId, OrderEventType.RECIPIENT_DATA_CHANGED,
                new RecipientDataOrderDifference(name, phone), date.plusSeconds(1));
        testDataFactory.createEvent(orderId, OrderEventType.ADDRESS_CHANGED,
                new AddressOrderDifference(address, address), date);
        testDataFactory.createEvent(orderId, OrderEventType.ADDRESS_CHANGED,
                new AddressOrderDifference(address, address), date.minusSeconds(1));

        var resultEvents = orderHistoryEventRepository.findHistoricalEventsForOrders(List.of(orderId));

        assertThat(resultEvents).hasSize(3);
        var event = resultEvents.get(0);
        assertThat(event.getType()).isEqualTo(OrderEventType.ADDRESS_CHANGED);
        assertThat(event.getDifference()).isInstanceOf(AddressOrderDifference.class);
        event = resultEvents.get(1);
        assertThat(event.getType()).isEqualTo(OrderEventType.ADDRESS_CHANGED);
        assertThat(event.getDifference()).isInstanceOf(AddressOrderDifference.class);
        event = resultEvents.get(2);
        assertThat(event.getType()).isEqualTo(OrderEventType.RECIPIENT_DATA_CHANGED);
        assertThat(event.getDifference()).isInstanceOf(RecipientDataOrderDifference.class);
    }

    @Test
    public void findOnlyHistoricalEvents() {
        var orderId = createOrder();
        testDataFactory.createEvent(orderId, OrderEventType.ORDER_FLOW_STATUS_CHANGED,
                new RecipientDataOrderDifference(name, phone));
        testDataFactory.createEvent(orderId, OrderEventType.RECIPIENT_DATA_CHANGED,
                new RecipientDataOrderDifference(name, phone));
        testDataFactory.createEvent(orderId, OrderEventType.ADDRESS_CHANGED,
                new AddressOrderDifference(address, address));
        testDataFactory.createEvent(orderId, OrderEventType.TRANSMISSION_REVERTED,
                new RecipientDataOrderDifference(name, phone));

        var resultEvents = orderHistoryEventRepository.findHistoricalEventsForOrders(List.of(orderId));

        assertThat(resultEvents).hasSize(2);
    }

    @Test
    public void findHistoricalEventsWithNotNullDiff() {
        var orderId = createOrder();
        testDataFactory.createEvent(orderId, OrderEventType.RECIPIENT_DATA_CHANGED,
                null);
        testDataFactory.createEvent(orderId, OrderEventType.RECIPIENT_DATA_CHANGED,
                new RecipientDataOrderDifference(name, phone));
        testDataFactory.createEvent(orderId, OrderEventType.ADDRESS_CHANGED,
                null);
        testDataFactory.createEvent(orderId, OrderEventType.ADDRESS_CHANGED,
                new AddressOrderDifference(address, address));

        var resultEvents = orderHistoryEventRepository.findHistoricalEventsForOrders(List.of(orderId));

        assertThat(resultEvents).hasSize(2);
    }

    @Test
    public void saveDiff() {
        var orderId = createOrder();
        var date = Instant.now();
        testDataFactory.createEvent(orderId, OrderEventType.RECIPIENT_DATA_CHANGED,
                new RecipientDataOrderDifference(name, phone), date);
        testDataFactory.createEvent(orderId, OrderEventType.ADDRESS_CHANGED,
                new AddressOrderDifference(address, address), date.plusSeconds(1));

        var resultEvents = orderHistoryEventRepository.findHistoricalEventsForOrders(List.of(orderId));

        assertThat(resultEvents).hasSize(2);
        var recipientDataDiff = (RecipientDataOrderDifference) resultEvents.get(0).getDifference();
        assertThat(recipientDataDiff).isNotNull();
        assertThat(recipientDataDiff.getName()).isEqualTo(name);
        assertThat(recipientDataDiff.getPhone()).isEqualTo(phone);
        var addressDiff = (AddressOrderDifference) resultEvents.get(1).getDifference();
        assertThat(addressDiff).isNotNull();
        assertThat(addressDiff.getAddress()).isEqualTo(address);
    }

    private Long createOrder() {
        Long orderId = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId(externalOrderId)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(itemsCount)
                        .build())
                .build()).getId();
        assertThat(orderId).isNotNull();
        testDataFactory.flushAndClear();
        return orderId;
    }

}
