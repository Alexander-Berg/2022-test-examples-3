package ru.yandex.market.delivery.mdbapp.scheduled;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.assertj.core.api.JUnitSoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.delivery.mdbapp.IntegrationTest;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.godd.OrderRepository;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGateway;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.sc.internal.client.ScIntClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static steps.orderSteps.OrderEventSteps.getOrderHistoryEvent;
import static steps.orderSteps.OrderSteps.getFilledOrder;

@RunWith(SpringRunner.class)
@MockBean({
    LMSClient.class,
    PechkinHttpClient.class,
    LomClient.class,
    ScIntClient.class,
})
@IntegrationTest
@Sql(value = {
    "/data/clean-get-delivery-date-order-store.sql",
    "/data/create-get-delivery-date-order-store.sql"
})
public class RemoveOrderDeliveryDateFlowOrderEventsTest {
    public static final LocalDate FROM_DATE = LocalDate.of(2020, 7, 14);
    private static final Long REGION_ID = 1L;
    private static final Long DELIVERY_SERVICE_ID = 2L;
    public static final ZoneOffset TZ_OFFSET = ZoneOffset.ofHours(8);
    public static final LocalDate TO_DATE = LocalDate.of(2020, 7, 15);
    public static final LocalTime FROM_TIME = LocalTime.of(10, 0);
    public static final LocalTime TO_TIME = LocalTime.of(20, 30);

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Autowired
    private OrderEventsGateway gateway;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private LomClient lomClient;

    @Test
    public void delivered() {
        doTestRemoved(e -> e.getOrderAfter().setStatus(OrderStatus.DELIVERED));
        assertNoOrder();
    }

    @Test
    public void cancelled() {
        when(lomClient.searchOrders(any(OrderSearchFilter.class), eq(Set.of()), any(Pageable.class), eq(true)))
            .thenReturn(PageResult.of(List.of(), 0, 1, 1));

        doTestRemoved(e -> {
            e.getOrderAfter().setStatus(OrderStatus.CANCELLED);
            e.getOrderAfter().setRgb(Color.BLUE);
            e.getOrderAfter().getDelivery().setParcels(Collections.emptyList());
        });
        assertNoOrder();
    }

    @Test
    public void pickup() {
        doTestRemoved(e -> e.getOrderAfter().setStatus(OrderStatus.PICKUP));
        assertNoOrder();
    }


    @Test
    public void userReceived() {
        doTestRemoved(e -> {
            e.setType(HistoryEventType.ORDER_SUBSTATUS_UPDATED);
            e.getOrderAfter().setSubstatus(OrderSubstatus.USER_RECEIVED);
        });
        assertNoOrder();
    }

    @Test
    public void wrongType1() {
        doTestRemoved(e -> {
            e.setType(HistoryEventType.ITEMS_UPDATED);
            e.getOrderAfter().setStatus(OrderStatus.DELIVERED);
        });
        assertOrder();
    }

    @Test
    public void wrongType2() {
        doTestRemoved(e -> e.getOrderAfter().setSubstatus(OrderSubstatus.USER_RECEIVED));
        assertOrder();
    }

    @Test
    public void wrongId() {
        doTestRemoved(e -> {
            e.getOrderAfter().setId(653L);
            e.getOrderAfter().setStatus(OrderStatus.DELIVERED);
        });
        assertOrder();
    }

    private void doTestRemoved(Consumer<OrderHistoryEvent> eventModification) {
        OrderHistoryEvent event = createEvent();
        eventModification.accept(event);
        gateway.processEvent(event);
    }

    private void assertNoOrder() {
        softly
            .assertThat(orderRepository.findAll().stream()
                .allMatch(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getDelivered))
            .isTrue();
    }

    private void assertOrder() {
        softly.assertThat(orderRepository.findAll()).isEqualTo(List.of(
            new ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order()
                .setId(OrderSteps.ID)

                .setDeliveryType(DeliveryType.DELIVERY.name())

                .setFromDate(FROM_DATE)
                .setToDate(TO_DATE)
                .setFromTime(FROM_TIME)
                .setToTime(TO_TIME)
                .setTimezoneOffset(TZ_OFFSET.getTotalSeconds())
                .setDelivery(new ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Delivery()
                    .setId(DELIVERY_SERVICE_ID).setEnabled(true).setMaxRequestedOrders(Integer.MAX_VALUE))
        ));
    }

    @NotNull
    private OrderHistoryEvent createEvent() {
        OrderHistoryEvent event = getOrderHistoryEvent();

        Order orderBefore = getFilledOrder();
        orderBefore.setStatus(OrderStatus.PROCESSING);
        event.setOrderBefore(orderBefore);

        Order orderAfter = getFilledOrder();
        orderAfter.setStatus(OrderStatus.DELIVERY);

        Buyer buyer = new Buyer();
        buyer.setRegionId(REGION_ID);
        orderAfter.setBuyer(buyer);

        Delivery delivery = new Delivery();
        DeliveryDates deliveryDates = new DeliveryDates();
        // Date is parsed by jackson in UTC
        deliveryDates.setFromDate(Date.from(FROM_DATE.atStartOfDay(ZoneOffset.UTC).toInstant()));
        deliveryDates.setToDate(Date.from(TO_DATE.atStartOfDay(ZoneOffset.UTC).toInstant()));
        deliveryDates.setFromTime(FROM_TIME);
        deliveryDates.setToTime(TO_TIME);
        delivery.setDeliveryDates(deliveryDates);
        delivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryPartnerType(YANDEX_MARKET);
        orderAfter.setDelivery(delivery);

        event.setOrderAfter(orderAfter);
        return event;
    }
}
