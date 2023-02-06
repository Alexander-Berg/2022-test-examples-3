package ru.yandex.market.delivery.mdbapp.integration.service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.assertj.core.api.JUnitSoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.common.util.region.CustomRegionAttribute;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Delivery;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.OrderRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.OrderRequestStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.RequestCondition;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.godd.DeliveryRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.godd.OrderRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.godd.OrderRequestRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.godd.RequestConditionRepository;

import static org.mockito.Mockito.doReturn;

@Sql("/data/clean-get-delivery-date-order-store.sql")
public class OrderDeliveryDateHandlerIntegrationTest extends MockContextualTest {
    private static final Long REGION_ID = 1L;
    public static final ZoneOffset TZ_OFFSET = ZoneOffset.ofHours(8);
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Autowired
    private OrderDeliveryDateHandler orderDeliveryDateHandler;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderRequestRepository orderRequestRepository;

    @Autowired
    private RequestConditionRepository requestConditionRepository;

    @MockBean
    private RegionService regionService;

    private Delivery delivery;
    private RequestCondition condition;

    Instant now = LocalDate.of(2020, 7, 15).atStartOfDay().toInstant(ZoneOffset.UTC);

    @Before
    public void setUp() throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        RegionTree regionTree = Mockito.mock(RegionTree.class);
        Region region = new Region(REGION_ID.intValue(), "Region", RegionType.CONTINENT, null);
        region.setCustomAttributeValue(CustomRegionAttribute.TIMEZONE_OFFSET,
            Integer.toString(TZ_OFFSET.getTotalSeconds()));

        doReturn(regionTree).when(regionService).getRegionTree();
        doReturn(region).when(regionTree).getRegion(REGION_ID.intValue());

        condition = requestConditionRepository.save(
            new RequestCondition()
                .setId(1L)
                .setRequestTime(LocalTime.of(10, 0))
                .setDescription("aaa")
        );

        delivery = new Delivery()
            .setId(1L)
            .setEnabled(false)
            .setMaxRequestedOrders(Integer.MAX_VALUE);

        Field f = Delivery.class.getDeclaredField("requestConditions");
        f.setAccessible(true);
        f.set(delivery, Set.of(condition));

        delivery = deliveryRepository.save(delivery);
    }

    @Test
    public void testInactiveDelivery() {
        OrderHistoryEvent evt = makeEvent(delivery);

        orderDeliveryDateHandler.handle(evt, now);

        softly.assertThat(orderRepository.findAll().stream()
            .map(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getId)
            .collect(Collectors.toList()))
            .isEqualTo(List.of(1L));
        softly.assertThat(orderRepository.findAll().stream()
            .map(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getActive)
            .collect(Collectors.toList()))
            .isEqualTo(List.of(false));

        softly.assertThat(orderRequestRepository.findAll().isEmpty()).isEqualTo(true);
    }

    @Test
    public void testCountersOkDelivery() {
        delivery.setEnabled(true);
        deliveryRepository.save(delivery);

        OrderHistoryEvent evt = makeEvent(delivery);

        orderDeliveryDateHandler.handle(evt, now);

        softly.assertThat(orderRepository.findAll().stream()
            .map(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getId)
            .collect(Collectors.toList()))
            .isEqualTo(List.of(1L));
        softly.assertThat(orderRepository.findAll().stream()
            .map(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getActive)
            .collect(Collectors.toList()))
            .isEqualTo(List.of(true));

        softly.assertThat(orderRequestRepository.findAll().size()).isEqualTo(1);
    }

    @Test
    public void testQuotaExceeded() {
        delivery.setEnabled(true);
        delivery.setMaxRequestedOrders(1);
        deliveryRepository.save(delivery);

        var order1 = orderRepository.save(new ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order()
            .setId(123L)
            .setFromDate(LocalDate.of(2020, 7, 15))
            .setToDate(LocalDate.of(2020, 7, 15))
            .setActive(true)
            .setTimezoneOffset(0)
            .setDelivery(delivery)
            .setDeliveryType("DELIVERY")
            .setWarehouseId(0L));
        orderRequestRepository.save(new OrderRequest()
            .setOrder(order1)
            .setId(1000L)
            .setProcessTime(LocalDate.of(2020, 7, 15).atStartOfDay().atOffset(ZoneOffset.UTC))
            .setRequestCondition(condition)
            .setStatus(OrderRequestStatus.NEW));


        OrderHistoryEvent evt = makeEvent(delivery);

        orderDeliveryDateHandler.handle(evt, now);

        softly.assertThat(orderRepository.findById(1L)
            .map(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getActive))
            .isEqualTo(Optional.of(false));

        softly
            .assertThat(orderRequestRepository.findAll().stream().noneMatch(r -> r.getOrder().getId().equals(1L)))
            .isEqualTo(true);
    }

    @Test
    public void testOrderToAnotherDelivery() {
        delivery.setEnabled(true);
        delivery.setMaxRequestedOrders(1);
        deliveryRepository.save(delivery);

        Delivery delivery2 = deliveryRepository.save(new Delivery()
            .setId(100L)
            .setEnabled(true)
            .setMaxRequestedOrders(10));
        ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order anotherDeliveryOrder =
            orderRepository.save(new ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order()
            .setId(123L)
            .setFromDate(LocalDate.of(2020, 7, 15))
            .setToDate(LocalDate.of(2020, 7, 15))
            .setActive(true)
            .setTimezoneOffset(0)
            .setDelivery(delivery2)
            .setDeliveryType("DELIVERY")
            .setWarehouseId(0L));
        orderRequestRepository.save(new OrderRequest()
            .setOrder(anotherDeliveryOrder)
            .setId(1000L)
            .setProcessTime(LocalDate.of(2020, 7, 15).atStartOfDay().atOffset(ZoneOffset.UTC))
            .setRequestCondition(condition)
            .setStatus(OrderRequestStatus.NEW));


        OrderHistoryEvent evt = makeEvent(this.delivery);

        orderDeliveryDateHandler.handle(evt, now);

        softly.assertThat(orderRepository.findById(1L)
            .map(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getActive))
            .isEqualTo(Optional.of(true));

        softly
            .assertThat(orderRequestRepository.findAll().stream().filter(r -> r.getOrder().getId().equals(1L)).count())
            .isEqualTo(1);
    }


    @Test
    public void testAnotherInactiveOrder() {
        delivery.setEnabled(true);
        delivery.setMaxRequestedOrders(1);
        deliveryRepository.save(delivery);

        orderRepository.save(new ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order()
            .setId(123L)
            .setFromDate(LocalDate.of(2020, 7, 15))
            .setToDate(LocalDate.of(2020, 7, 15))
            .setActive(false)
            .setTimezoneOffset(0)
            .setDelivery(delivery)
            .setDeliveryType("DELIVERY")
            .setWarehouseId(0L));

        OrderHistoryEvent evt = makeEvent(delivery);

        orderDeliveryDateHandler.handle(evt, now);

        softly.assertThat(orderRepository.findById(1L)
            .map(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getActive))
            .isEqualTo(Optional.of(true));

        softly.assertThat(orderRequestRepository.findAll().size()).isEqualTo(1);
    }

    @Test
    public void testCancelled() {
        doTestMarkDelivered(evt -> evt.getOrderAfter().setStatus(OrderStatus.CANCELLED));
    }

    @Test
    public void testDelivered() {
        doTestMarkDelivered(evt -> evt.getOrderAfter().setStatus(OrderStatus.DELIVERED));
    }

    @Test
    public void testPickUp() {
        doTestMarkDelivered(evt -> evt.getOrderAfter().setStatus(OrderStatus.PICKUP));
    }

    @Test
    public void testUser() {
        doTestMarkDelivered(evt -> {
            evt.setType(HistoryEventType.ORDER_SUBSTATUS_UPDATED);
            evt.getOrderAfter().setSubstatus(OrderSubstatus.USER_RECEIVED);
        });
    }

    private void doTestMarkDelivered(Consumer<OrderHistoryEvent> modifyEvent) {
        delivery.setEnabled(true);
        deliveryRepository.save(delivery);

        OrderHistoryEvent evt = makeEvent(delivery);
        // create
        orderDeliveryDateHandler.handle(evt, now);

        // delete
        softly.assertThat(orderRepository.findAll().stream().findFirst()
            .map(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getDelivered))
            .contains(false);

        modifyEvent.accept(evt);
        orderDeliveryDateHandler.handle(evt, now);

        softly.assertThat(orderRepository.findAll().stream().findFirst()
            .map(ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order::getDelivered))
            .contains(true);
    }

    @NotNull
    private OrderHistoryEvent makeEvent(Delivery delivery) {
        OrderHistoryEvent evt = new OrderHistoryEvent();
        evt.setType(HistoryEventType.ORDER_STATUS_UPDATED);

        Order orderBefore = new Order();
        orderBefore.setStatus(OrderStatus.PLACING);
        evt.setOrderBefore(orderBefore);

        Order orderAfter = new Order();
        orderAfter.setId(1L);
        Buyer buyer = new Buyer();
        buyer.setRegionId(REGION_ID);
        orderAfter.setBuyer(buyer);
        orderAfter.setStatus(OrderStatus.DELIVERY);
        ru.yandex.market.checkout.checkouter.delivery.Delivery d =
            new ru.yandex.market.checkout.checkouter.delivery.Delivery();
        d.setDeliveryServiceId(delivery.getId());
        d.setType(DeliveryType.DELIVERY);
        d.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        DeliveryDates deliveryDates = new DeliveryDates();
        deliveryDates.setFromDate(new Date());
        deliveryDates.setToDate(new Date());
        deliveryDates.setFromTime("10:00");
        deliveryDates.setToTime("20:00");
        d.setDeliveryDates(deliveryDates);
        orderAfter.setDelivery(d);
        evt.setOrderAfter(orderAfter);
        return evt;
    }
}
