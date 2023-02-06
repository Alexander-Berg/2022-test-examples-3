package ru.yandex.market.tpl.core.domain.order.delayed_order;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistory;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistoryRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerOrderDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointType.LOCKER_DELIVERY;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor
public class DelayedOrderPickupStatusTest extends TplAbstractTest {

    private final Clock clock;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService commandService;
    private final TestDataFactory testDataFactory;
    private final UserShiftReassignManager userShiftReassignManager;
    private final LockerOrderDataHelper lockerOrderDataHelper;
    private final PickupPointRepository pickupPointRepository;
    private final TransactionTemplate tt;
    private final SortingCenterService sortingCenterService;
    private final DelayedOrderStatusService delayedOrderStatusService;
    private final OrderFlowStatusHistoryRepository historyRepository;

    private UserShift userShift;
    private User user;
    private Shift shift;
    private Order pickupOrder;


    @BeforeEach
    void init() {
        tt.execute(a -> {
            ClockUtil.initFixed(clock);
            user = testUserHelper.findOrCreateUser(35236L);
            shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                    sortingCenterService.findSortCenterForDs(DELIVERY_SERVICE_ID).getId());
            var userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
            userShift = userShiftRepository.findByIdOrThrow(userShiftId);


            PickupPoint pickupPoint = pickupPointRepository.save(
                    testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, DELIVERY_SERVICE_ID));
            GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

            pickupOrder = lockerOrderDataHelper.getPickupOrder(
                    shift, "EXTERNAL_ORDER_ID_1", pickupPoint, geoPoint,
                    OrderFlowStatus.SORTING_CENTER_PREPARED, 2
            );

            userShiftReassignManager.assign(userShift, pickupOrder);

            testUserHelper.checkinAndFinishPickup(userShift);
            return 0;
        });
    }

    @Test
    void shouldChangePickupOrderHistoryStatus() {
        var routePoint = userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getType() == LOCKER_DELIVERY)
                .findFirst().get();

        var deliveryTask = routePoint
                .streamDeliveryTasks()
                .findFirst().get();

        var failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                "Source.COURIER"
        );

        var payload = new DelayedOrderStatusPayload(
                "requestId",
                userShift.getId(),
                deliveryTask.getId(),
                failReason,
                Instant.now(clock)
        );

        commandService.failDeliveryTask(
                user,
                new UserShiftCommand.FailOrderDeliveryTask(
                        userShift.getId(),
                        deliveryTask.getRoutePoint().getId(),
                        deliveryTask.getId(),
                        failReason
                ));
        delayedOrderStatusService.processPayload(payload);
        List<OrderFlowStatusHistory> history = historyRepository
                .findByExternalOrderIdHistory(pickupOrder.getExternalOrderId());
        long countReschedule = history.stream()
                .filter(h -> h.getOrderFlowStatusAfter() == OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP)
                .count();

        assertThat(countReschedule).isEqualTo(1L);
        commandService.closeShift(new UserShiftCommand.Close(userShift.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        history = historyRepository
                .findByExternalOrderIdHistory(pickupOrder.getExternalOrderId());

        countReschedule = history.stream()
                .filter(h -> h.getOrderFlowStatusAfter() == OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP)
                .count();

        assertThat(countReschedule).isEqualTo(1L);
    }

}
