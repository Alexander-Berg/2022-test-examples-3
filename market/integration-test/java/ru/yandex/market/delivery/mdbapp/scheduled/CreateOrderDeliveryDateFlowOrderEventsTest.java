package ru.yandex.market.delivery.mdbapp.scheduled;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.JUnitSoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import steps.orderSteps.OrderSteps;

import ru.yandex.common.util.region.CustomRegionAttribute;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.mdbapp.IntegrationTest;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.godd.OrderRepository;
import ru.yandex.market.delivery.mdbapp.components.util.DeliveryServices;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGateway;
import ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsByTypeRouter;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.sc.internal.client.ScIntClient;

import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static steps.orderSteps.OrderEventSteps.getOrderHistoryEvent;
import static steps.orderSteps.OrderSteps.getFilledOrder;

@RunWith(SpringRunner.class)
@MockBean({
    LMSClient.class,
    PechkinHttpClient.class,
    ScIntClient.class,
})
@IntegrationTest
@Sql(value = "/data/clean-get-delivery-date-order-store.sql")
public class CreateOrderDeliveryDateFlowOrderEventsTest {
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

    @MockBean
    private RegionService regionService;

    @Test
    public void blueDropShip() {
        doTestCreated(e -> e.getOrderAfter().setRgb(Color.BLUE));
        assertOrder();
    }

    @Test
    public void standard() {
        doTestCreated(e -> e.getOrderAfter().setFulfilment(true));
        assertOrder();
    }

    @Test
    public void crossDoc() {
        doTestCreated(e -> {
        });
        assertOrder();
    }

    @Test
    public void post1() {
        doTestCreated(e -> {
            e.getOrderAfter().getDelivery().setType(DeliveryType.POST);
            e.getOrderAfter().getDelivery().setDeliveryServiceId(DeliveryServices.MARSCHROUTE_POST_DELIVERY_SERVICE_ID);
            e.getOrderAfter().setRgb(Color.BLUE);
        });
        assertNoOrder();
    }

    @Test
    public void post2() {
        doTestCreated(e -> {
            e.getOrderAfter().getDelivery().setType(DeliveryType.POST);
            e.getOrderAfter().getDelivery()
                .setDeliveryServiceId(DeliveryServices.MARSCHROUTE_NEW_POST_DELIVERY_SERVICE_ID);
            e.getOrderAfter().setRgb(Color.BLUE);
        });
        assertNoOrder();
    }

    @Test
    public void post3() {
        doTestCreated(e -> {
            e.getOrderAfter().getDelivery().setType(DeliveryType.PICKUP);
            e.getOrderAfter().getDelivery().setDeliveryServiceId(DeliveryServices.MARSCHROUTE_POST_DELIVERY_SERVICE_ID);
            e.getOrderAfter().setRgb(Color.BLUE);
        });
        assertNoOrder();
    }

    @Test
    public void post4() {
        doTestCreated(e -> {
            e.getOrderAfter().getDelivery().setType(DeliveryType.PICKUP);
            e.getOrderAfter().getDelivery()
                .setDeliveryServiceId(DeliveryServices.MARSCHROUTE_NEW_POST_DELIVERY_SERVICE_ID);
            e.getOrderAfter().setRgb(Color.BLUE);
        });
        assertNoOrder();
    }

    @Test
    public void golden() {
        doTestCreated(e -> {
            e.getOrderAfter().getDelivery().setDeliveryServiceId(OrderEventsByTypeRouter.SELF_DELIVERY_SERVICE);
            e.getOrderAfter().setRgb(Color.BLUE);
        });
        assertNoOrder();
    }

    @Test
    public void wrongEvtType() {
        doTestCreated(e -> {
            e.setType(HistoryEventType.NEW_CASH_PAYMENT);
        });
        assertNoOrder();
    }

    private void doTestCreated(Consumer<OrderHistoryEvent> eventModification) {
        RegionTree regionTree = Mockito.mock(RegionTree.class);
        Region region = new Region(REGION_ID.intValue(), "Region", RegionType.CONTINENT, null);
        region.setCustomAttributeValue(
            CustomRegionAttribute.TIMEZONE_OFFSET,
            Integer.toString(TZ_OFFSET.getTotalSeconds())
        );

        doReturn(regionTree).when(regionService).getRegionTree();
        doReturn(region).when(regionTree).getRegion(REGION_ID.intValue());

        OrderHistoryEvent event = createEvent();
        eventModification.accept(event);
        gateway.processEvent(event);
    }

    private void assertNoOrder() {
        softly.assertThat(orderRepository.findAll()).isEqualTo(Collections.emptyList());
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
