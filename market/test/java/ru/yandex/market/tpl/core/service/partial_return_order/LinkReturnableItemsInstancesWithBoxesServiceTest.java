package ru.yandex.market.tpl.core.service.partial_return_order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusRequestDto;
import ru.yandex.market.tpl.api.model.partial_return_order.LinkReturnableItemsInstancesWithBoxesRequestDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderItemInstance;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrder;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderBox;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderQueryService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.partial_return.LinkReturnableItemsInstancesWithBoxesService;
import ru.yandex.market.tpl.core.service.partial_return.UpdateItemsInstancesPurchaseStatusService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.PACK_RETURN_BOXES_AND_READY_FOR_RETURN;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.RETURNED_ORDER_IS_DELIVERED_TO_SENDER;
import static ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus.NOT_STARTED;
import static ru.yandex.market.tpl.core.test.TestTplApiRequestFactory.buildUpdateOrderItemRequest;

@RequiredArgsConstructor
public class LinkReturnableItemsInstancesWithBoxesServiceTest extends TplAbstractTest {
    private final OrderGenerateService orderGenerateService;
    private final UpdateItemsInstancesPurchaseStatusService updateItemsInstancesPurchaseStatusService;
    private final LinkReturnableItemsInstancesWithBoxesService linkReturnableItemsInstancesWithBoxesService;
    private final PartialReturnOrderQueryService partialReturnOrderQueryService;
    private final TransactionTemplate transactionTemplate;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final Clock clock;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandService userShiftCommandService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final UserShiftManager userShiftManager;
    private final OrderRepository orderRepository;

    private Order order1;
    private Order order2;
    private User user;
    private UserShift userShift;
    private OrderDeliveryTask orderDeliveryTask1;
    private OrderDeliveryTask orderDeliveryTask2;
    private OrderReturnTask orderReturnTask;

    @BeforeEach
    void init() {
        transactionTemplate.execute(ts -> {
                    user = testUserHelper.findOrCreateUser(1L);
                    var shift = testUserHelper.findOrCreateOpenShiftForSc(
                            LocalDate.now(clock),
                            sortingCenterService.findSortCenterForDs(239).getId()
                    );
                    userShift = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(),
                            user));
                    AddressGenerator.AddressGenerateParam addressGenerateParam =
                            AddressGenerator.AddressGenerateParam.builder()
                                    .geoPoint(GeoPointGenerator.generateLonLat())
                                    .build();

                    order1 = orderGenerateService.createOrder(
                            OrderGenerateService.OrderGenerateParam.builder()
                                    .items(
                                            OrderGenerateService.OrderGenerateParam.Items.builder()
                                                    .isFashion(true)
                                                    .itemsCount(2)
                                                    .itemsItemCount(2)
                                                    .itemsPrice(BigDecimal.valueOf(120))
                                                    .build()
                                    )
                                    .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                                    .addressGenerateParam(addressGenerateParam)
                                    .externalOrderId("451233244")
                                    .deliveryDate(LocalDate.now(clock))
                                    .deliveryServiceId(239L)
                                    .build()
                    );

                    order2 = orderGenerateService.createOrder(
                            OrderGenerateService.OrderGenerateParam.builder()
                                    .items(
                                            OrderGenerateService.OrderGenerateParam.Items.builder()
                                                    .isFashion(true)
                                                    .itemsCount(1)
                                                    .itemsItemCount(3)
                                                    .itemsPrice(BigDecimal.valueOf(150))
                                                    .build()
                                    )
                                    .deliveryPrice(BigDecimal.ONE)
                                    .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                                    .addressGenerateParam(addressGenerateParam)
                                    .externalOrderId("451233243244")
                                    .deliveryDate(LocalDate.now(clock))
                                    .deliveryServiceId(239L)
                                    .build()
                    );

                    userShiftReassignManager.assign(userShift, order1);
                    userShiftReassignManager.assign(userShift, order2);

                    orderDeliveryTask1 = userShift.streamOrderDeliveryTasks()
                            .filter(t -> Objects.equals(t.getOrderId(), order1.getId()))
                            .findFirst().orElseThrow();

                    orderDeliveryTask2 = userShift.streamOrderDeliveryTasks()
                            .filter(t -> Objects.equals(t.getOrderId(), order2.getId()))
                            .findFirst().orElseThrow();

                    orderReturnTask = userShift.streamReturnRoutePoints().findFirst().get().getOrderReturnTask();

                    testUserHelper.checkinAndFinishPickup(userShift);
                    return null;
                }
        );
    }

    @Test
    void linkWhenAllReturnableItemsInstancesWithBoxesWhenReturnNeededBoxes() {
        linkWhenAllReturnableItemsInstancesWithBoxes();
        var returnRoutePointId = orderReturnTask.getRoutePoint().getId();
        List<Long> allReturnBoxes = transactionTemplate.execute(ts ->
                StreamEx.of(partialReturnOrderQueryService.findAllByOrderIds(List.of(order1.getId(), order2.getId())))
                        .flatMap(PartialReturnOrder::streamBoxes)
                        .map(PartialReturnOrderBox::getId)
                        .collect(Collectors.toList())
        );

        userShiftCommandService.arriveAtRoutePoint(userShift.getUser(), new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), returnRoutePointId, new LocationDto(BigDecimal.ONE, BigDecimal.ONE), false));

        userShiftCommandService.startOrderReturn(userShift.getUser(),
                new UserShiftCommand.StartScan(userShift.getId(), returnRoutePointId, orderReturnTask.getId()));

        userShiftCommandService.finishReturnOrders(userShift.getUser(),
                new UserShiftCommand.FinishScan(userShift.getId(), returnRoutePointId, orderReturnTask.getId(),
                        ScanRequest.builder()
                                .successfullyScannedPartialReturnBoxes(allReturnBoxes)
                                .comment("DUTY_COMMENT")
                                .finishedAt(Instant.now(clock))
                                .build()));

        userShiftCommandService.finishReturnTask(userShift.getUser(), new UserShiftCommand.FinishReturnTask(
                userShift.getId(), returnRoutePointId, orderReturnTask.getId()));

        testUserHelper.finishUserShift(userShift.getId());
        transactionTemplate.execute(ts -> {
            var order1 = orderRepository.findByIdOrThrow(this.order1.getId());
            assertThat(order1.getOrderFlowStatus()).isEqualTo(RETURNED_ORDER_IS_DELIVERED_TO_SENDER);

            var order2 = orderRepository.findByIdOrThrow(this.order2.getId());
            assertThat(order2.getOrderFlowStatus()).isEqualTo(RETURNED_ORDER_IS_DELIVERED_TO_SENDER);
            return null;
        });
    }

    @Test
    void linkWhenAllReturnableItemsInstancesWithBoxesWhenNotReturnNeededBoxes() {
        linkWhenAllReturnableItemsInstancesWithBoxes();

        userShiftManager.returnOrders(userShift.getId());
        testUserHelper.finishUserShift(userShift.getId());

        //нет возврата по 2 заказам, потому 2
        dbQueueTestUtil.assertQueueHasSize(QueueType.PARTIAL_RETURN_BOXES_CREATE_TRACKER_ISSUE, 2);
    }

    void linkWhenAllReturnableItemsInstancesWithBoxes() {
        testUserHelper.arriveAtRoutePoint(orderDeliveryTask1.getRoutePoint());
        List<OrderItemInstance> instO1 = order1.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());

        List<OrderItemInstance> instO2 = order2.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());
        updateAllItemsInstancesPurchaseStatusToReturn(instO1, instO2);

        LinkReturnableItemsInstancesWithBoxesRequestDto request =
                LinkReturnableItemsInstancesWithBoxesRequestDto.builder()
                        .orders(List.of(
                                LinkReturnableItemsInstancesWithBoxesRequestDto.LinkReturnableItemsInstancesWithBoxesByOrderRequestDto.builder()
                                        .boxBarcodes(List.of("FSN_RET_123", "FSN_RET_456"))
                                        .uits(instO1.stream().map(OrderItemInstance::getUit).collect(Collectors.toList()))
                                        .externalOrderId(order1.getExternalOrderId())
                                        .taskId(orderDeliveryTask1.getId())
                                        .build()
                                ,
                                LinkReturnableItemsInstancesWithBoxesRequestDto.LinkReturnableItemsInstancesWithBoxesByOrderRequestDto.builder()
                                        .boxBarcodes(List.of("FSN_RET_789"))
                                        .uits(instO2.stream().map(OrderItemInstance::getUit).collect(Collectors.toList()))
                                        .externalOrderId(order2.getExternalOrderId())
                                        .taskId(orderDeliveryTask2.getId())
                                        .build()
                        ))
                        .build();
        linkReturnableItemsInstancesWithBoxesService.link(request, user);

        transactionTemplate.execute(ts -> {
            var userShift = userShiftRepository.findByIdOrThrow(this.userShift.getId());
            userShift.streamOrderDeliveryTasks().forEach(
                    task -> assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED)
            );
            PartialReturnOrder partialReturnOrder1 = partialReturnOrderQueryService.findByOrderOrElseThrow(order1);
            assertThat(partialReturnOrder1.getOrder().getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);

            assertThat(partialReturnOrder1.getOrder().getOrderFlowStatus()).isEqualTo(PACK_RETURN_BOXES_AND_READY_FOR_RETURN);

            PartialReturnOrder partialReturnOrder2 = partialReturnOrderQueryService.findByOrderOrElseThrow(order2);
            assertThat(partialReturnOrder2.getOrder().getOrderFlowStatus()).isEqualTo(PACK_RETURN_BOXES_AND_READY_FOR_RETURN);
            assertThat(partialReturnOrder2.getOrder().getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);

            assertThat(partialReturnOrder2.streamBoxes()).hasSize(1);
            assertThat(dbQueueTestUtil.getQueue(QueueType.PROCESS_PARTIAL_RETURN_ORDER)).hasSize(4);
            return null;
        });

    }

    @Test
    void linkReturnableItemsInstancesWithBoxesHappyTest() {
        testUserHelper.arriveAtRoutePoint(orderDeliveryTask1.getRoutePoint());

        List<OrderItemInstance> instO1 = order1.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());

        List<OrderItemInstance> instO2 = order2.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());
        updateItemsInstancesPurchaseStatus(instO1, instO2);

        LinkReturnableItemsInstancesWithBoxesRequestDto request =
                LinkReturnableItemsInstancesWithBoxesRequestDto.builder()
                        .orders(List.of(
                                LinkReturnableItemsInstancesWithBoxesRequestDto.LinkReturnableItemsInstancesWithBoxesByOrderRequestDto.builder()
                                        .boxBarcodes(List.of("FSN_RET_123", "FSN_RET_456"))
                                        .uits(List.of(instO1.get(2).getUit(), instO1.get(3).getUit()))
                                        .externalOrderId(order1.getExternalOrderId())
                                        .taskId(orderDeliveryTask1.getId())
                                        .build()
                                ,
                                LinkReturnableItemsInstancesWithBoxesRequestDto.LinkReturnableItemsInstancesWithBoxesByOrderRequestDto.builder()
                                        .boxBarcodes(List.of("FSN_RET_789"))
                                        .uits(instO2.stream().map(OrderItemInstance::getUit).collect(Collectors.toList()))
                                        .externalOrderId(order2.getExternalOrderId())
                                        .taskId(orderDeliveryTask2.getId())
                                        .build()
                        ))
                        .build();

        linkReturnableItemsInstancesWithBoxesService.link(request, user);

        transactionTemplate.execute(ts -> {
            PartialReturnOrder partialReturnOrder1 = partialReturnOrderQueryService.findByOrderOrElseThrow(order1);
            assertThat(partialReturnOrder1.streamBoxes()).hasSize(2);

            PartialReturnOrder partialReturnOrder2 = partialReturnOrderQueryService.findByOrderOrElseThrow(order2);
            assertThat(partialReturnOrder2.streamBoxes()).hasSize(1);

            var userShift = userShiftRepository.findByIdOrThrow(this.userShift.getId());
            userShift.streamOrderDeliveryTasks().forEach(
                    task -> assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED)
            );
            return null;
        });

        userShiftCommandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), orderReturnTask.getRoutePoint().getId(), new LocationDto(BigDecimal.ONE,
                BigDecimal.ONE)
        ));

        transactionTemplate.execute(ts -> {
            var userShift = userShiftRepository.findByIdOrThrow(this.userShift.getId());

            userShift.streamReturnRoutePoints().flatMap(RoutePoint::streamReturnTasks)
                    .forEach(task -> assertThat(task.getStatus()).isEqualTo(NOT_STARTED));
            return null;
        });

    }

    @Test
    void linkReturnableItemsInstancesWithBoxesInvalidBarcodes() {
        testUserHelper.arriveAtRoutePoint(orderDeliveryTask1.getRoutePoint());

        List<OrderItemInstance> instO1 = order1.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());

        List<OrderItemInstance> instO2 = order2.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());
        updateItemsInstancesPurchaseStatus(instO1, instO2);

        LinkReturnableItemsInstancesWithBoxesRequestDto request =
                LinkReturnableItemsInstancesWithBoxesRequestDto.builder()
                        .orders(List.of(
                                LinkReturnableItemsInstancesWithBoxesRequestDto.LinkReturnableItemsInstancesWithBoxesByOrderRequestDto.builder()
                                        .boxBarcodes(List.of("barcode1 ", "barcode2"))
                                        .uits(List.of(instO1.get(2).getUit(), instO1.get(3).getUit()))
                                        .externalOrderId(order1.getExternalOrderId())
                                        .taskId(orderDeliveryTask1.getId())
                                        .build()
                                ,
                                LinkReturnableItemsInstancesWithBoxesRequestDto.LinkReturnableItemsInstancesWithBoxesByOrderRequestDto.builder()
                                        .boxBarcodes(List.of("barcode3\n", "barcode4", "barcode 5"))
                                        .uits(instO2.stream().map(OrderItemInstance::getUit).collect(Collectors.toList()))
                                        .externalOrderId(order2.getExternalOrderId())
                                        .taskId(orderDeliveryTask2.getId())
                                        .build()
                        ))
                        .build();

        var exception = assertThrows(TplIllegalArgumentException.class, () ->
                linkReturnableItemsInstancesWithBoxesService.link(request, user));
        assertThat(exception.getMessage()).contains(
                "Вы отсканировали некорректные штрих-коды! Попробуйте отсканировать их еще раз",
                "barcode1 ",
                "barcode 5",
                "barcode3"
        );
    }

    private void updateAllItemsInstancesPurchaseStatusToReturn(List<OrderItemInstance> instO1,
                                                               List<OrderItemInstance> instO2) {
        UpdateItemsInstancesPurchaseStatusRequestDto request = UpdateItemsInstancesPurchaseStatusRequestDto.builder()
                .orders(List.of(
                        buildUpdateOrderItemRequest(order1.getExternalOrderId(),
                                List.of(),
                                List.of(instO1.get(0).getUit(), instO1.get(1).getUit(), instO1.get(2).getUit()),
                                List.of(instO1.get(3).getUit()))
                        ,
                        buildUpdateOrderItemRequest(order2.getExternalOrderId(),
                                null,
                                List.of(instO2.get(2).getUit()),
                                List.of(instO2.get(0).getUit(), instO2.get(1).getUit()))
                ))
                .build();

        updateItemsInstancesPurchaseStatusService.updateItemsInstancesPurchaseStatus(request, user);
    }

    private void updateItemsInstancesPurchaseStatus(List<OrderItemInstance> instO1, List<OrderItemInstance> instO2) {
        UpdateItemsInstancesPurchaseStatusRequestDto request = UpdateItemsInstancesPurchaseStatusRequestDto.builder()
                .orders(List.of(
                        buildUpdateOrderItemRequest(order1.getExternalOrderId(),
                                List.of(instO1.get(0).getUit(), instO1.get(1).getUit()),
                                List.of(instO1.get(2).getUit()),
                                List.of(instO1.get(3).getUit()))
                        ,
                        buildUpdateOrderItemRequest(order2.getExternalOrderId(),
                                null,
                                List.of(instO2.get(2).getUit()),
                                List.of(instO2.get(0).getUit(), instO2.get(1).getUit()))
                ))
                .build();

        updateItemsInstancesPurchaseStatusService.updateItemsInstancesPurchaseStatus(request, user);
    }
}
