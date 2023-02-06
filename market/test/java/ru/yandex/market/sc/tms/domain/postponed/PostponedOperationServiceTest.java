package ru.yandex.market.sc.tms.domain.postponed;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.postponed.model.OperationType;
import ru.yandex.market.sc.core.domain.postponed.repository.PostponedOperation;
import ru.yandex.market.sc.core.domain.postponed.repository.PostponedOperationRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_CANCELLED_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTmsTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PostponedOperationServiceTest {

    private final TestFactory testFactory;
    private final OrderCommandService orderCommandService;
    private final PostponedOperationRepository postponedOperationRepository;
    private final ScOrderRepository scOrderRepository;
    private final PostponedOperationService postponedOperationService;
    private final JdbcTemplate jdbcTemplate;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 123L);
        testFactory.setupMockClock(clock);
    }

    @Test
    void applyPostponedCancel() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);
        assertThat(order.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(1);
        assertThat(operations.iterator().next().getOrderId()).isEqualTo(order.getId());
        assertThat(operations.iterator().next().getOperationType()).isEqualTo(OperationType.CANCEL);

        testFactory.shipOrderRoute(order);
        var shippedOrder = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(shippedOrder.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);

        postponedOperationService.applyPostponedCancel();

        var cancelOrder = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(cancelOrder.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        var appliedOperations = postponedOperationRepository.findAll();
        assertThat(appliedOperations.iterator().next().getIsApplied()).isTrue();
    }

    @Test
    void applyPostponedDamageMark() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, true);
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        orderCommandService.markOrderAsDamaged(order.getId(), true, user);
        assertFalse(order.isDamaged());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(1);
        var operation = operations.get(0);
        assertThat(operation.getOrderId()).isEqualTo(order.getId());
        assertThat(operation.getOperationType()).isEqualTo(OperationType.MARK_DAMAGED);

        testFactory.shipOrderRoute(order);
        var shippedOrder = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(shippedOrder.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);

        postponedOperationService.applyPostponedDamageMark();

        var damagedOrder = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(damagedOrder.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        var appliedOperations = postponedOperationRepository.findAll();
        assertThat(appliedOperations).allMatch(PostponedOperation::getIsApplied);
    }

    @Test
    void applyPostponedRevertDamageMark() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, true);
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().markOrderAsDamaged().sort().get();
        assertTrue(order.isDamaged());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);


        // Из текущего статуса откатить пометку от поврежденности нельзя
        orderCommandService.revertMarkOrderAsDamaged(order.getId(), true, user);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(1);
        var operation = operations.get(0);
        assertThat(operation.getOrderId()).isEqualTo(order.getId());
        assertThat(operation.getOperationType()).isEqualTo(OperationType.REVERT_MARK_DAMAGED);

        // Проверяем, что в заказе ничего не изменилось
        order = testFactory.getOrder(order.getId());
        assertTrue(order.isDamaged());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        // Выставляем статус, из которого можно отменить пометку о поврежденности
        jdbcTemplate.update("UPDATE orders SET ff_status = ? WHERE id = ?", RETURNED_ORDER_AT_SO_WAREHOUSE.name(),
                order.getId());

        postponedOperationService.applyPostponedRevertDamageMark();

        var revertedDamagedOrder = scOrderRepository.findByIdOrThrow(order.getId());
        assertFalse(revertedDamagedOrder.isDamaged());
        assertThat(revertedDamagedOrder.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
        var appliedOperations = postponedOperationRepository.findAll();
        assertThat(appliedOperations).allMatch(PostponedOperation::getIsApplied);
    }

    @Test
    void dontThrowExceptionWhenCantApplyCancel() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);
        assertThat(order.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(1);
        assertThat(operations.iterator().next().getOrderId()).isEqualTo(order.getId());
        assertThat(operations.iterator().next().getOperationType()).isEqualTo(OperationType.CANCEL);

        assertDoesNotThrow(postponedOperationService::applyPostponedCancel);

        var dontCancelOrder = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(dontCancelOrder.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var appliedOperations = postponedOperationRepository.findAll();
        assertThat(appliedOperations.iterator().next().getIsApplied()).isFalse();
    }

    @Test
    void dontApplyCancelToSortedToLotOrders() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var order = testFactory.createForToday(
                        order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).places("p1", "p2").build()
                )
                .acceptPlaces("p1").get();
        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow();
        var cell = testFactory.findRouteCell(route, order).orElseThrow();
        var lot1 = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var lot2 = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var orderPlaces = testFactory.orderPlaces(order.getId());
        var p1 = orderPlaces.stream().filter(p -> p.getMainPartnerCode().equals("p1")).findAny().orElseThrow();
        var p2 = orderPlaces.stream().filter(p -> p.getMainPartnerCode().equals("p2")).findAny().orElseThrow();
        testFactory.sortPlaceToLot(p1, lot1, user);
        testFactory.prepareToShipLot(lot1);
        Long routeId = testFactory.findOutgoingCourierRoute(order).orElseThrow().allowNextRead().getId();
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(routeId), sortingCenter);
        testFactory.acceptPlace(order, "p2");
        testFactory.sortPlaceToLot(p2, lot2, user);

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);
        postponedOperationService.applyPostponedCancel();
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
    }

    @Test
    void dontThrowExceptionPostponedCancelForAlreadyReturnedOrders() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().cancelWithPosponed().ship().cancel().accept().sort().ship().get();
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(1);
        assertThat(operations.iterator().next().getOrderId()).isEqualTo(order.getId());
        assertThat(operations.iterator().next().getOperationType()).isEqualTo(OperationType.CANCEL);

        assertDoesNotThrow(postponedOperationService::applyPostponedCancel);

        var dontCancelOrder = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(dontCancelOrder.getFfStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        var appliedOperations = postponedOperationRepository.findAll();
        assertThat(appliedOperations.iterator().next().getIsApplied()).isTrue();
    }

    @Test
    void dontThrowExceptionPostponedCancelForAlreadyCancelOrders() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).cancelWithPosponed().get();
        postponedOperationRepository.saveAndFlush(new PostponedOperation(order, OperationType.CANCEL));
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(1);
        assertThat(operations.iterator().next().getOrderId()).isEqualTo(order.getId());
        assertThat(operations.iterator().next().getOperationType()).isEqualTo(OperationType.CANCEL);

        assertDoesNotThrow(postponedOperationService::applyPostponedCancel);

        var alreadyCancelOrder = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(alreadyCancelOrder.getFfStatus()).isEqualTo(ORDER_CANCELLED_FF);
        var appliedOperations = postponedOperationRepository.findAll();
        assertThat(appliedOperations.iterator().next().getIsApplied()).isTrue();
    }
}
