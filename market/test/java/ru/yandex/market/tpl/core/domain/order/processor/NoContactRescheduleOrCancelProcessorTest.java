package ru.yandex.market.tpl.core.domain.order.processor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.NO_CONTACT;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class NoContactRescheduleOrCancelProcessorTest {
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandService userShiftCommandService;
    private final NoContactRescheduleOrCancelProcessor noContactRescheduleOrCancelProcessor;

    public Order order;
    public User userFirst;

    @Value("${tpl.reschedule.max.count.attempt:3}")
    private long rescheduleMaxCountAttempt;

    @BeforeEach
    void init() {
        userFirst = testUserHelper.findOrCreateUser(1L);

        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        UserShift userShift1 = userShiftRepository
                .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), userFirst));

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("231432")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("09:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        userShiftReassignManager.assign(userShift1, order);
        testUserHelper.checkinAndFinishPickup(userShift1);
    }

    @Test
    void testCancelOrderAfterRescheduleMaxCountAttempt() {
        //фейлим таску на доставку у первого курьера
        UserShift userShift = userShiftRepository.findCurrentShift(userFirst).get();
        userShiftCommandService.failDeliveryTask(userFirst,
                new UserShiftCommand.FailOrderDeliveryTask(
                        userShift.getId(),
                        userShift.streamDeliveryRoutePoints().findFirst().get().getId(),
                        userShift.streamOrderDeliveryTasks().findFirst().get().getId(),
                        new OrderDeliveryFailReason(NO_CONTACT, "", null, Source.COURIER)
                ));

        for (int i = 0; i < rescheduleMaxCountAttempt; i++) {
            //переназначаем на курьера, который отменит таску по причине НЕ ДОЗВОНИЛСЯ
            User user1 = testUserHelper.findOrCreateUser(i + 5);
            userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user1.getId());
            userShift = userShiftRepository.findCurrentShift(user1).get();
            testUserHelper.checkinAndFinishPickup(userShift);
            userShiftCommandService.failDeliveryTask(user1,
                    new UserShiftCommand.FailOrderDeliveryTask(
                            userShift.getId(),
                            userShift.streamDeliveryRoutePoints().findFirst().get().getId(),
                            userShift.streamOrderDeliveryTasks().findFirst().get().getId(),
                            new OrderDeliveryFailReason(
                                    NO_CONTACT, "", null, Source.COURIER
                            )
                    ));

            //переназначаем просто на курьера, чтобы таска сфейлилась с таким статусом -> COURIER_REASSIGNED
            User user2 = testUserHelper.findOrCreateUser(i + 1000);
            userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());
            userShift = userShiftRepository.findCurrentShift(user2).get();
            testUserHelper.checkinAndFinishPickup(userShift);

            assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
        }

        noContactRescheduleOrCancelProcessor.rescheduleOrCancel(order.getId(), userShift.getShift().getShiftDate());
        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
    }
}
