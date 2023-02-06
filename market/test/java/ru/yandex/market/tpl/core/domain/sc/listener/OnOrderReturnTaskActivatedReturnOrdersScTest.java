package ru.yandex.market.tpl.core.domain.sc.listener;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrderRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.external.delivery.sc.ScLgwClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author valter
 */
@Disabled("https://st.yandex-team.ru/MARKETTPL-993")
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OnOrderReturnTaskActivatedReturnOrdersScTest {

    private final ScManager scManager;
    private final SortingCenterService sortingCenterService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final ScOrderRepository scOrderRepository;
    private final Clock clock;

    @MockBean
    private ScLgwClient scLgwClient;

    @Test
    @SneakyThrows
    void returnOrdersInBeruHub() {
        long beruHubId = 48805L;
        var order = singleOrderScenario(beruHubId, true);
        verify(scLgwClient).createReturnRegister(
                argThat(r -> Objects.equals(
                        List.of(order.getExternalOrderId()),
                        r.getOrdersId().stream().map(ResourceId::getYandexId).collect(Collectors.toList())
                )),
                eq(beruHubId));
    }

    @Test
    @SneakyThrows
    void doNotReturnReshedules() {
        long beruHubId = 48805L;
        singleOrderScenario(beruHubId, false);
        verify(scLgwClient, never()).createReturnRegister(any(), anyLong());
    }

    @Test
    @SneakyThrows
    void doNotReturnOrdersInBetaPro() {
        singleOrderScenario(47951L, true);
        verify(scLgwClient, never()).createReturnRegister(any(), anyLong());
    }

    private Order singleOrderScenario(long scId, boolean failByCustomer) {
        var order = createAndAcceptAtScOrder(scId);
        var userShift = createUserShift(order);
        updateOrderFromSc(order, userShift.getShift().getSortingCenter());
        failOrdersAtFirstRoutePoint(userShift, failByCustomer);
        sendLgwRequestsToSc();
        return order;
    }

    private Order createAndAcceptAtScOrder(long scId) {
        return orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(sortingCenterService.findDsForSortingCenter(scId).get(0).getId())
                        .deliveryDate(LocalDate.now(clock))
                        .flowStatus(OrderFlowStatus.CREATED)
                        .build()
        );
    }

    private UserShift createUserShift(Order order) {
        var user = testUserHelper.findOrCreateUser(123L, LocalDate.now(clock));
        return testUserHelper.createShiftWithDeliveryTask(user, UserShiftStatus.SHIFT_CREATED, order);
    }

    private void updateOrderFromSc(Order order, SortingCenter sortingCenter) {
        scManager.createOrders(Instant.now().plus(1, ChronoUnit.SECONDS));
        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_ORDER);
        var instant = Instant.now(clock);

        scManager.updateOrderStatuses(order.getExternalOrderId(), sortingCenter.getId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), instant),
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(), instant),
                        new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(), instant)
                )
        );
    }

    private void failOrdersAtFirstRoutePoint(UserShift userShift, boolean byCustomer) {
        testUserHelper.checkinAndFinishPickup(userShift);
        var routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        testUserHelper.finishDelivery(routePoint,
                byCustomer
                        ? OrderDeliveryTaskFailReasonType.CLIENT_REFUSED
                        : OrderDeliveryTaskFailReasonType.NO_CONTACT
        );
    }

    private void sendLgwRequestsToSc() {
        dbQueueTestUtil.executeAllQueueItems(QueueType.RETURN_ORDER);
    }

}
