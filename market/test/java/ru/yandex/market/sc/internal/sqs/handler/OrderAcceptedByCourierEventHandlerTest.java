package ru.yandex.market.sc.internal.sqs.handler;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.les.CourierOrderEvent;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.order.OrderStatusService;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.route.RouteQueryService;
import ru.yandex.market.sc.core.domain.route.model.RouteDocumentType;
import ru.yandex.market.sc.core.domain.route.model.TransferActGetRequest;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.SqsEventFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.internal.sqs.SqsEventType.ORDER_ACCEPTED_BY_COURIER_EVENT;

@EmbeddedDbIntTest
public class OrderAcceptedByCourierEventHandlerTest {

    @Autowired
    private SqsEventFactory sqsEventFactory;
    @Autowired
    private OrderAcceptedByCourierEventHandler orderAcceptedByCourierEventHandler;
    @Autowired
    private Clock clock;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private TestFactory testFactory;
    @Autowired
    private OrderStatusService orderStatusService;
    @Autowired
    RouteQueryService routeQueryService;

    private SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @ParameterizedTest
    @ValueSource(classes = {Integer.class, Long.class, String.class})
    void shipOrderOnEvent(Class<?> courierIdClass) {
        // given
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();

        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);

        // when
        var event = sqsEventFactory.makeSqsEvent(ORDER_ACCEPTED_BY_COURIER_EVENT,
                clock.instant().plus(1, ChronoUnit.HOURS).toEpochMilli(), buildPayload(courierIdClass, order)
        );
        orderAcceptedByCourierEventHandler.handle(event);

        // then
        checkOrderAndPlacesToBeShipped(testFactory.getOrder(order.getId()));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void dontShipOrderOnEventIfOrderStatusWasUpdatedAfterEvent(int delayForEvent) {
        // given
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();

        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);

        // when
        var event = sqsEventFactory.makeSqsEvent(ORDER_ACCEPTED_BY_COURIER_EVENT,
                clock.instant().plus(delayForEvent, ChronoUnit.HOURS).toEpochMilli(), buildPayload(Long.class, order)
        );
        orderAcceptedByCourierEventHandler.handle(event);

        // then
        if (delayForEvent > 0) {
            checkOrderAndPlacesToBeShipped(testFactory.getOrder(order.getId()));
        } else {
            assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ORDER_ARRIVED_TO_SO_WAREHOUSE",
            "ORDER_AWAITING_CLARIFICATION_FF",
            "ORDER_READY_TO_BE_SEND_TO_SO_FF",
            "ORDER_PREPARED_TO_BE_SEND_TO_SO",
            "ORDER_SHIPPED_TO_SO_FF",
            "ORDER_CREATED_FF",
    })
    void dontShipOrderOnEventIfOrderStatusWasUpdatedAfterEvent(String orderFfStatusString) {
        // given
        var orderFfStatus = ScOrderFFStatus.valueOf(orderFfStatusString);

        final var order = createOrderInStatus(orderFfStatus);

        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(orderFfStatus);

        // when
        var event = sqsEventFactory.makeSqsEvent(ORDER_ACCEPTED_BY_COURIER_EVENT,
                clock.instant().plus(1, ChronoUnit.HOURS).toEpochMilli(), buildPayload(Integer.class, order)
        );
        orderAcceptedByCourierEventHandler.handle(event);

        // then
        if (orderStatusService.canShipDirectFlowOrderByFlowControlEvent(orderFfStatus)) {
            checkOrderAndPlacesToBeShipped(testFactory.getOrder(order.getId()));
        } else {
            assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(orderFfStatus);
        }
    }

    @Test
    void shipMultiplaceOrderOnEvent() {
        // given
        var order = testFactory.createForToday(
                order(sortingCenter, UUID.randomUUID().toString()).places("1", "2", "3").build()
        ).acceptPlaces().sortPlaces().get();

        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);

        // when
        var event = sqsEventFactory.makeSqsEvent(ORDER_ACCEPTED_BY_COURIER_EVENT,
                clock.instant().plus(1, ChronoUnit.HOURS).toEpochMilli(), buildPayload(Integer.class, order)
        );
        orderAcceptedByCourierEventHandler.handle(event);

        // then
        checkOrderAndPlacesToBeShipped(testFactory.getOrder(order.getId()));
    }

    @Test
    void shipOrderFromLotOnEvent() {
        // given
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        var order = testFactory.createForToday(order(sortingCenter, UUID.randomUUID().toString()).build())
                .accept().sort().sortToLot(lot.getLotId()).get();

        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);

        // when
        var event = sqsEventFactory.makeSqsEvent(ORDER_ACCEPTED_BY_COURIER_EVENT,
                clock.instant().plus(1, ChronoUnit.HOURS).toEpochMilli(), buildPayload(Integer.class, order)
        );
        orderAcceptedByCourierEventHandler.handle(event);

        // then
        checkOrderAndPlacesToBeShipped(testFactory.getOrder(order.getId()));
    }

    @Test
    @SneakyThrows
    void orderIsListedInTransferAct() {
        // given
        var orderShippedStage1 = testFactory.createForToday(
                order(sortingCenter, "o1").build()
        ).accept().sort().get();
        var orderShippedStage2 = testFactory.createForToday(
                order(sortingCenter, "o2").places("1", "2").build()
        ).accept().get();
        var orderShippedStage3 = testFactory.createForToday(
                order(sortingCenter, "o3").places("1", "2", "3").build()
        ).accept().sort().get();
        var routeOptional = testFactory.findOutgoingCourierRoute(orderShippedStage1);

        assertThat(testFactory.getOrder(orderShippedStage1.getId()).getFfStatus())
                .isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);

        // when - stage1: one order shipped by event
        var event = sqsEventFactory.makeSqsEvent(ORDER_ACCEPTED_BY_COURIER_EVENT,
                clock.instant().plus(1, ChronoUnit.HOURS).toEpochMilli(), buildPayload(Long.class, orderShippedStage1)
        );
        orderAcceptedByCourierEventHandler.handle(event);

        // then - stage1
        checkOrderAndPlacesToBeShipped(testFactory.getOrder(orderShippedStage1.getId()));
        assertThat(routeOptional).isPresent();
        Long routableId = testFactory.getRouteIdForSortableFlow(routeOptional.get().getId());
        transactionTemplate.execute(t -> {
                    var transferActDto = routeQueryService.getTransferAct(new TransferActGetRequest(
                            RouteDocumentType.ALL, sortingCenter, testFactory.getRoutable((routeOptional.get()))));
                    assertThat(transferActDto.getTotalItems()).isEqualTo(1L);
                    assertThat(transferActDto.getTotalPlaces()).isEqualTo(1L);
                    return null;
        });
        // when - stage2: second order shipped by event too
        event = sqsEventFactory.makeSqsEvent(ORDER_ACCEPTED_BY_COURIER_EVENT,
                clock.instant().plus(1, ChronoUnit.HOURS).toEpochMilli(), buildPayload(Long.class, orderShippedStage2)
        );
        orderAcceptedByCourierEventHandler.handle(event);

        //then - stage2
        transactionTemplate.execute(t -> {
                    var transferActDto = routeQueryService.getTransferAct(new TransferActGetRequest(
                            RouteDocumentType.ALL, sortingCenter, testFactory.getRoutable((routeOptional.get()))));
                    assertThat(transferActDto.getTotalItems()).isEqualTo(2L);
                    assertThat(transferActDto.getTotalPlaces()).isEqualTo(3L);
            return null;
        });
        // when - stage3: route shipped
        testFactory.shipOrderRoute(orderShippedStage3);

        //then - stage3
        transactionTemplate.execute(t -> {
            var transferActDto = routeQueryService.getTransferAct(new TransferActGetRequest(
                    RouteDocumentType.ALL, sortingCenter, testFactory.getRoutable(routeOptional.get())));
            assertThat(transferActDto.getTotalItems()).isEqualTo(3L);
            assertThat(transferActDto.getTotalPlaces()).isEqualTo(6L);
            return null;
        });
    }

    private void checkOrderAndPlacesToBeShipped(ScOrder order) {
        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
            testFactory.orderPlaces(order.getId()).forEach(
                    p -> assertThat(p.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT)
            );
    }

    private ScOrder createOrderInStatus(ScOrderFFStatus orderFfStatus) {
        return switch (orderFfStatus) {
            case ORDER_ARRIVED_TO_SO_WAREHOUSE -> testFactory.createOrderForToday(sortingCenter).accept().get();
            case ORDER_AWAITING_CLARIFICATION_FF -> testFactory.createOrderForToday(sortingCenter)
                    .accept()
                    .updateShipmentDate(LocalDate.now(clock).plusDays(3))
                    .keep()
                    .updateShipmentDate(LocalDate.now(clock))
                    .get();
            case ORDER_READY_TO_BE_SEND_TO_SO_FF -> testFactory.createOrderForToday(sortingCenter)
                    .accept().sort().get();
            case ORDER_PREPARED_TO_BE_SEND_TO_SO -> testFactory.createOrderForToday(sortingCenter)
                    .accept().sort().prepare().get();
            case ORDER_SHIPPED_TO_SO_FF -> testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
            case ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE -> testFactory.createForToday(
                            order(sortingCenter, UUID.randomUUID().toString()).places("1", "2").build())
                    .acceptPlaces(List.of("1", "2"))
                    .sortPlaces(List.of("1", "2"))
                    .shipPlace("1").get();
            case ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE -> testFactory.createForToday(
                            order(sortingCenter, UUID.randomUUID().toString()).places("1", "2").build())
                    .acceptPlace("1").get();
            case ORDER_CREATED_FF -> testFactory.createOrderForToday(sortingCenter).get();
            default -> throw new IllegalArgumentException();
        };
    }

    private CourierOrderEvent buildPayload(Class<?> courierIdClass, ScOrder order) {
        return new CourierOrderEvent(
                order.getExternalId(),
                "source-1",
                "",
                false,
                buildOrderAcceptedByCourierPayloadMap(order, courierIdClass)
        );
    }

    private Map<Object, Object> buildOrderAcceptedByCourierPayloadMap(ScOrder order, Class<?> courierIdClass) {
        assertThat(order.getCourierId()).isPresent();
        var courierIdLong = order.getCourierId().get();
        var orderAcceptedByCourierEventPayload = new LinkedHashMap<>();
        var courierId = switch (courierIdClass.getName()) {
            case "java.lang.Integer" -> courierIdLong.intValue();
            case "java.lang.Long" -> courierIdLong;
            case "java.lang.String" -> courierIdLong.toString();
            default -> throw new IllegalArgumentException();
        };
        orderAcceptedByCourierEventPayload.put("userUid", courierId);
        orderAcceptedByCourierEventPayload.put("scLogisticPointId", null);
        orderAcceptedByCourierEventPayload.put("scToken", sortingCenter.getToken());
        return orderAcceptedByCourierEventPayload;
    }

}
