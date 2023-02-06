package ru.yandex.market.logistics.cs.util;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistics.cs.logbroker.checkouter.SimpleCombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.DeliveryRoute;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.DeliveryService;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.Point;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.PointIds;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.ProcessedItem;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.Timestamp;

import static io.github.benas.randombeans.FieldPredicates.inClass;
import static io.github.benas.randombeans.FieldPredicates.named;
import static io.github.benas.randombeans.FieldPredicates.ofType;
import static java.util.stream.Collectors.toCollection;

@UtilityClass
public class TestDtoFactory {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final EnhancedRandom RANDOMIZER = EnhancedRandomBuilder
        .aNewEnhancedRandomBuilder()
        .collectionSizeRange(2, 4)
        .excludeType(clazz -> clazz.isAssignableFrom(JsonNode.class))
        .excludeType(clazz -> clazz.isAssignableFrom(Multimap.class))
        .randomize(CombinatorRoute.Date.class, (Randomizer<CombinatorRoute.Date>) TestDtoFactory::nextDate)
        .randomize(Timestamp.class, (Randomizer<Timestamp>) TestDtoFactory::nextTimestamp)
        .randomize(
            named("id").and(ofType(long.class)).and(inClass(DeliveryService.class)),
            new StatefulIdGenerator()
        )
        .randomize(
            named("quantity").and(ofType(int.class)).and(inClass(ProcessedItem.class)),
            new IntegerRangeRandomizer(1, 10)
        )
        .objectPoolSize(1 << 10)
        .build();

    public IndexedOrderHistoryEvent randomHistoryEventWithRoute(
        HistoryEventType historyEventType,
        @Nullable OrderStatus orderBeforeStatus,
        @Nullable OrderStatus orderAfterStatus
    ) {
        return randomHistoryEventWithRoute(MAPPER, historyEventType, orderBeforeStatus, orderAfterStatus, false);
    }

    public IndexedOrderHistoryEvent randomHistoryEventWithRoute(
        ObjectMapper mapper,
        HistoryEventType historyEventType,
        @Nullable OrderStatus orderBeforeStatus,
        @Nullable OrderStatus orderAfterStatus,
        boolean withLogisticDate
    ) {
        DeliveryRoute route = RANDOMIZER.nextObject(DeliveryRoute.class);
        if (!withLogisticDate) {
            route.getPoints().stream()
                .map(Point::getServices)
                .flatMap(Collection::stream)
                .forEach(service -> service.setLogisticDate(null));
        }

        SimpleCombinatorRoute combinatorRoute = new SimpleCombinatorRoute(route, null);
        JsonNode routeNode = mapper.valueToTree(combinatorRoute);

        Parcel parcel = parcel();
        parcel.setRoute(routeNode);

        Delivery delivery = new Delivery();
        delivery.setParcels(List.of(parcel));

        OrderHistoryEvent event = TestDtoFactory.orderHistoryEvent(historyEventType);

        if (orderBeforeStatus != null) {
            Order order = TestDtoFactory.order(orderBeforeStatus);
            event.setOrderBefore(order);
        }

        if (orderAfterStatus != null) {
            Order order = TestDtoFactory.order(orderAfterStatus);
            order.setDelivery(delivery);
            event.setOrderAfter(order);
        }

        List<DeliveryService> serviceList = route.getPoints().stream()
            .map(Point::getServices)
            .flatMap(Collection::stream)
            .collect(toCollection(LinkedList::new));
        return new IndexedOrderHistoryEvent(event, serviceList, routeNode);
    }

    public SingleServiceOrderHistoryEvent singleServiceWithParcelOrder(
        ObjectMapper mapper,
        HistoryEventType historyEventType,
        OrderStatus orderStatus
    ) {
        OrderHistoryEvent event = TestDtoFactory.orderHistoryEvent(historyEventType);

        DeliveryService service = TestDtoFactory.deliveryService();

        PointIds ids = new PointIds();
        ids.setPartnerId(123L);

        Point point = new Point();
        point.setIds(ids);
        point.setServices(List.of(service));

        DeliveryRoute route = new DeliveryRoute();
        route.setPoints(List.of(point));

        SimpleCombinatorRoute combinatorRoute = new SimpleCombinatorRoute(route, null);
        JsonNode routeNode = mapper.valueToTree(combinatorRoute);

        Order order = TestDtoFactory.order(orderStatus);

        Parcel parcel = parcel();
        parcel.setRoute(routeNode);

        Delivery delivery = new Delivery();
        delivery.setParcels(List.of(parcel));

        order.setDelivery(delivery);
        event.setOrderAfter(order);

        return new SingleServiceOrderHistoryEvent(event, order, service, routeNode);
    }

    public SingleServiceOrderHistoryEvent emptyRouteWithParcelOrder(
        HistoryEventType historyEventType,
        OrderStatus orderStatus
    ) {
        OrderHistoryEvent event = TestDtoFactory.orderHistoryEvent(historyEventType);

        Order order = TestDtoFactory.order(orderStatus);

        Parcel parcel = parcel();

        Delivery delivery = new Delivery();
        delivery.setParcels(List.of(parcel));

        order.setDelivery(delivery);
        event.setOrderAfter(order);

        return new SingleServiceOrderHistoryEvent(event, order, null, null);
    }

    public SingleServiceOrderHistoryEvent singleServiceWithParcelOrder(
        HistoryEventType historyEventType,
        OrderStatus orderStatus
    ) {
        return singleServiceWithParcelOrder(MAPPER, historyEventType, orderStatus);
    }

    public Parcel parcel() {
        Parcel parcel = new Parcel();
        parcel.setId(nextId());
        return parcel;
    }

    public long nextId() {
        return ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    }

    public OrderHistoryEvent orderHistoryEvent(HistoryEventType type) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(type);
        event.setId(nextId());
        event.setTranDate(new Date());
        return event;
    }

    public Order order(OrderStatus orderStatus) {
        Order order = new Order();
        order.setId(nextId());
        order.setStatus(orderStatus);
        return order;
    }

    private CombinatorRoute.Date nextDate() {
        CombinatorRoute.Date date = new CombinatorRoute.Date();
        date.setDay(ThreadLocalRandom.current().nextInt(1, 28));
        date.setMonth(ThreadLocalRandom.current().nextInt(1, 12));
        date.setYear(ThreadLocalRandom.current().nextInt(2000, 2021));
        return date;
    }

    private Timestamp nextTimestamp() {
        Timestamp timestamp = new Timestamp();
        timestamp.setSeconds(ThreadLocalRandom.current().nextLong(1000));
        timestamp.setNanos(ThreadLocalRandom.current().nextInt(1_000_000_000));
        return timestamp;
    }

    public DeliveryService deliveryService() {
        DeliveryService service = new DeliveryService();
        service.setStartTime(nextTimestamp());
        service.setId(nextId());
        return service;
    }

    @Getter
    @AllArgsConstructor
    public static class SingleServiceOrderHistoryEvent {
        private final OrderHistoryEvent event;
        private final Order order;
        private final DeliveryService service;
        private final JsonNode routeNode;
    }

    @Getter
    @AllArgsConstructor
    public static class IndexedOrderHistoryEvent {
        private final OrderHistoryEvent event;
        private final List<DeliveryService> serviceList;
        private final JsonNode routeNode;
    }

    private static class StatefulIdGenerator implements Randomizer<Long> {
        private final AtomicLong lastId;
        private final long jitter;

        private StatefulIdGenerator(long initialValue, long jitter) {
            Preconditions.checkArgument(initialValue >= 0);
            Preconditions.checkArgument(jitter >= 0);

            this.lastId = new AtomicLong(initialValue);
            this.jitter = jitter;
        }

        private StatefulIdGenerator() {
            this(0, 2);
        }

        @Override
        public Long getRandomValue() {
            return lastId.addAndGet(ThreadLocalRandom.current().nextLong(jitter) + 1);
        }
    }
}
