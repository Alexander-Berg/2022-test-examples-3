package ru.yandex.market.abo.cpa.order;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.cpa.order.delivery.OrderDelivery;
import ru.yandex.market.abo.cpa.order.delivery.OrderDeliveryRepo;
import ru.yandex.market.abo.cpa.order.model.CpaOrderStat;
import ru.yandex.market.abo.cpa.order.service.CpaOrderStatRepo;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;

@ContextConfiguration("classpath:WEB-INF/checkouter-serialization.xml")
public class OrderEventLoaderTest extends EmptyTest {
    private static final Long SHOP_ID = 5645678478L;
    private static final Long ORDER_ID = 234235L;
    private static final Map<StatusOrSubstatusProvider, Function<CpaOrderStat, Date>> STATUS_HISTORY_MAP = Map.ofEntries(
            entry(new StatusOrSubstatusProvider(PROCESSING), CpaOrderStat::getProcessing),
            entry(new StatusOrSubstatusProvider(OrderSubstatus.STARTED), CpaOrderStat::getProcessing),
            entry(new StatusOrSubstatusProvider(OrderSubstatus.READY_TO_SHIP), CpaOrderStat::getReadyToShip),
            entry(new StatusOrSubstatusProvider(OrderSubstatus.SHIPPED), CpaOrderStat::getShipped),

            entry(new StatusOrSubstatusProvider(DELIVERY), CpaOrderStat::getDelivery),
            entry(new StatusOrSubstatusProvider(PICKUP), CpaOrderStat::getPickup),
            entry(new StatusOrSubstatusProvider(DELIVERED), CpaOrderStat::getDelivered),
            entry(new StatusOrSubstatusProvider(CANCELLED), CpaOrderStat::getCancelled),

            entry(new StatusOrSubstatusProvider(PENDING), CpaOrderStat::getPending)
    );

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private OrderStatCreator orderStatCreator;
    @Autowired
    private CpaOrderStatRepo cpaOrderStatRepo;
    @Autowired
    private OrderDeliveryRepo orderDeliveryRepo;
    @Autowired
    private ObjectMapper checkouterAnnotationObjectMapper;

    @Test
    public void testCreateCpaOrderStat() {
        Order order = TestHelper.generateOrder(1L, 774L, 1L, false);
        orderStatCreator.processBatch(List.of(createOrderHistoryEvent(order)));

        CpaOrderStat cpaOrderStat = cpaOrderStatRepo.findByIdOrNull(order.getId());

        assertEquals(order.getId(), Long.valueOf(cpaOrderStat.getOrderId()));
        assertEquals(order.getShopId(), Long.valueOf(cpaOrderStat.getShopId()));
        assertEquals(order.getShopOrderId(), cpaOrderStat.getShopOrderId());
        assertEquals(order.getCreationDate(), cpaOrderStat.getCreationDate());
        assertEquals(order.getRgb(), cpaOrderStat.getRgb());
        assertEquals(order.getDelivery().getType(), cpaOrderStat.getDeliveryType());
        OrderDelivery orderDelivery = orderDeliveryRepo.getOne(order.getId());
        assertEquals(order.getDelivery().getDeliveryDates().getToDate(), orderDelivery.getByOrder());
        assertEquals(order.getDelivery().getParcels().get(0).getShipmentDate(),
                DateUtil.asLocalDate(orderDelivery.getByShipment()));
    }

    @Test
    void testSaveStatusDates() {
        STATUS_HISTORY_MAP.forEach(((provider, checkDateFunc) -> {
            Order order = TestHelper.generateOrder(RND.nextLong(), 774L, RND.nextLong(), false);
            Date eventDate = new Date();
            order.setStatus(provider.getOrderStatus());
            order.setSubstatus(provider.getOrderSubstatus());
            OrderHistoryEvent historyEvent = createOrderHistoryEvent(order, RND.nextInt(100500), eventDate);

            orderStatCreator.processBatch(List.of(historyEvent));

            CpaOrderStat statFromDb = cpaOrderStatRepo.findByIdOrNull(order.getId());
            Date statusDate = checkDateFunc.apply(statFromDb);
            assertEquals(historyEvent.getFromDate(), statusDate, "failed to save date for " + provider);
        }));
    }

    @Test
    @DisplayName("save all statuses of order life cycle without overwriting with later time")
    void doNotOverwriteWithLaterTime() {
        Order firstOrderState = TestHelper.generateOrder(ORDER_ID, SHOP_ID, RND.nextLong(), false, DELIVERY);
        Order secondOrderState = TestHelper.generateOrder(ORDER_ID, SHOP_ID, RND.nextLong(), false, DELIVERY);
        secondOrderState.setSubstatus(OrderSubstatus.USER_RECEIVED);

        var first = createOrderHistoryEvent(firstOrderState, 3124134L, new Date(), HistoryEventType.ORDER_STATUS_UPDATED);
        var second = createOrderHistoryEvent(secondOrderState, first.getId() + 1,
                DateUtils.addHours(first.getFromDate(), 1), HistoryEventType.ORDER_SUBSTATUS_UPDATED);

        orderStatCreator.processBatch(List.of(first, second));

        CpaOrderStat saved = cpaOrderStatRepo.findByIdOrNull(ORDER_ID);
        assertEquals(saved.getDelivery().getTime(), first.getFromDate().getTime());
    }

    @Test
    void statusesFromJson() throws IOException, ParseException {
        long orderId = 18597422;

        InputStream in = OrderEventLoaderTest.class.getResourceAsStream("/cpa/orderHistoryEvents.json");
        OrderHistoryEvents orderHistoryEvents = checkouterAnnotationObjectMapper.readValue(in, OrderHistoryEvents.class);
        orderStatCreator.processBatch(orderHistoryEvents.getContent());
        flushAndClear();

        CpaOrderStat saved = cpaOrderStatRepo.findByIdOrNull(orderId);

        assertDateEquals("2020-05-03 17:39:49", saved.getCreationDate());
        assertDateEquals("2020-05-03 17:39:52", saved.getPending());
        assertDateEquals("2020-05-03 18:07:10", saved.getProcessing());
        assertDateEquals("2020-05-03 18:07:10", saved.getReadyToShip());
        assertDateEquals("2020-05-04 13:57:27", saved.getShipped());
    }

    @Test
    void deliveryTypesFromJson() throws IOException {
        long marketDeliveryOrderId = 18597422;
        long shopDeliveryOrderId = 18597423;
        long unknownDeliveryOrderId = 18597424;

        InputStream in = OrderEventLoaderTest.class.getResourceAsStream("/cpa/orderHistoryEvents.json");
        OrderHistoryEvents orderHistoryEvents = checkouterAnnotationObjectMapper.readValue(in, OrderHistoryEvents.class);
        orderStatCreator.processBatch(orderHistoryEvents.getContent());
        flushAndClear();

        assertEquals(
                orderDeliveryRepo.findByIdOrNull(shopDeliveryOrderId).getDeliveryPartnerType(),
                DeliveryPartnerType.SHOP
        );

        assertEquals(
                orderDeliveryRepo.findByIdOrNull(unknownDeliveryOrderId).getDeliveryPartnerType(),
                DeliveryPartnerType.UNKNOWN
        );

        assertEquals(
                orderDeliveryRepo.findByIdOrNull(marketDeliveryOrderId).getDeliveryPartnerType(),
                DeliveryPartnerType.YANDEX_MARKET
        );
    }

    @Test
    void deliveryExpressFromJson() throws IOException {
        long orderId = 18597424;
        InputStream in = OrderEventLoaderTest.class.getResourceAsStream("/cpa/orderHistoryEvents.json");
        OrderHistoryEvents orderHistoryEvents = checkouterAnnotationObjectMapper.readValue(in, OrderHistoryEvents.class);
        orderStatCreator.processBatch(orderHistoryEvents.getContent());

        assertTrue(orderDeliveryRepo.findByIdOrNull(orderId).getExpress());
    }

    @ParameterizedTest
    @CsvSource({"DELAYED_DUE_EXTERNAL_CONDITIONS, 2021-08-05", "USER_MOVED_DELIVERY_DATES,"})
    void testRatingDeliveryDate(HistoryEventReason reason, LocalDate expected) throws IOException {
        var eventIdWithDeliveryChange = 243522769L;
        var orderId = 32802871L;
        Collection<OrderHistoryEvent> events;
        try (var is = OrderEventLoaderTest.class.getResourceAsStream("/cpa/orderHistoryEventsWithDeliveryChange.json");
             var bis = new BufferedInputStream(is)) {
            events = checkouterAnnotationObjectMapper.readValue(bis, PagedEvents.class).getItems();
        }
        var eventWithDeliveryChange = StreamEx.of(events)
                .findAny(event -> event.getId() == eventIdWithDeliveryChange)
                .orElseThrow();
        eventWithDeliveryChange.setReason(reason);

        orderStatCreator.processBatch(events);
        var ratingDeliveryDate = Optional.ofNullable(orderDeliveryRepo.findByIdOrNull(orderId).getRatingDeliveryDate())
                .map(DateUtil::asLocalDate)
                .orElse(null);
        assertEquals(expected, ratingDeliveryDate);
    }

    @ParameterizedTest
    @CsvSource({"DELAYED_DUE_EXTERNAL_CONDITIONS, 2021-08-04", "USER_MOVED_DELIVERY_DATES,"})
    void testRatingShipmentDate(HistoryEventReason reason, LocalDate expected) throws IOException {
        var eventIdWithShipmentChange = 243522769L;
        var orderId = 32802871L;
        Collection<OrderHistoryEvent> events;
        try (var is = OrderEventLoaderTest.class.getResourceAsStream("/cpa/orderHistoryEventsWithShipmentChange.json");
             var bis = new BufferedInputStream(is)) {
            events = checkouterAnnotationObjectMapper.readValue(bis, PagedEvents.class).getItems();
        }
        var eventWithShipmentChange = StreamEx.of(events)
            .findAny(event -> event.getId() == eventIdWithShipmentChange)
            .orElseThrow();
        eventWithShipmentChange.setReason(reason);

        orderStatCreator.processBatch(events);
        var ratingShipmentDate = Optional.ofNullable(orderDeliveryRepo.findByIdOrNull(orderId).getRatingShipmentDate())
            .map(DateUtil::asLocalDate)
            .orElse(null);
        assertEquals(expected, ratingShipmentDate);
    }

    private static void assertDateEquals(String expected, Date date) throws ParseException {
        assertNotNull(date);
        Date expectedDate = DATE_FORMAT.parse(expected);
        assertEquals(DateUtils.truncate(expectedDate, Calendar.SECOND), DateUtils.truncate(date, Calendar.SECOND));
    }


    private OrderHistoryEvent createOrderHistoryEvent(Order order) {
        return createOrderHistoryEvent(order, 1L, NOW);
    }

    private OrderHistoryEvent createOrderHistoryEvent(Order order, long eventId, Date eventDate) {
        return createOrderHistoryEvent(order, eventId, eventDate, HistoryEventType.ORDER_STATUS_UPDATED);
    }

    private OrderHistoryEvent createOrderHistoryEvent(Order order,
                                                      long eventId,
                                                      Date eventDate,
                                                      HistoryEventType eventType) {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(eventId);
        orderHistoryEvent.setType(eventType);
        orderHistoryEvent.setAuthor(new ClientInfo(ClientRole.SHOP, 0L));
        orderHistoryEvent.setFromDate(eventDate);
        orderHistoryEvent.setOrderAfter(order);
        return orderHistoryEvent;
    }

    private static class StatusOrSubstatusProvider {
        private final OrderStatus orderStatus;
        private final OrderSubstatus orderSubstatus;

        private StatusOrSubstatusProvider(OrderSubstatus substatus) {
            orderStatus = substatus.getStatus();
            orderSubstatus = substatus;
        }

        private StatusOrSubstatusProvider(OrderStatus status) {
            orderStatus = status;
            orderSubstatus = null;
        }

        OrderStatus getOrderStatus() {
            return orderStatus;
        }

        OrderSubstatus getOrderSubstatus() {
            return orderSubstatus;
        }

        @Override
        public String toString() {
            return "StatusOrSubstatusProvider{" +
                    "orderStatus=" + orderStatus +
                    ", orderSubstatus=" + orderSubstatus +
                    '}';
        }
    }
}
