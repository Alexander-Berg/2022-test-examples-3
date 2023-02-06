package ru.yandex.market.tpl.core.domain.routing.publish;


import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.RoutingRequestCreator;
import ru.yandex.market.tpl.core.domain.routing.events.ShiftRoutingResultReceivedEvent;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.publish.PublishUserShiftManager;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestTplRoutingFactory;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor
public class PublishRoutingResultForCouriersWithoutScheduleTest extends TplAbstractTest {

    private final TransactionTemplate transactionTemplate;
    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();

    private final UserShiftRepository userShiftRepository;

    private final RoutingRequestCreator routingRequestCreator;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final ShiftManager shiftManager;
    private final PublishUserShiftManager publishUserShiftManager;
    private final Clock clock;
    private final TestTplRoutingFactory testTplRoutingFactory;

    private Shift shift;
    private User user;
    private Order order1;
    private Order order2;
    private RoutingRequest routingRequest;

    @BeforeEach
    void init() {
        long sortingCenterId = 47819L;

        LocalDate shiftDate = LocalDate.now(clock);

        shift = shiftManager.findOrCreate(shiftDate, sortingCenterId);

        user = userHelper.findOrCreateUserWithoutSchedule(824126L);

        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shiftDate)
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.CREATED)
                .build());

        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shiftDate.minusDays(3))
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.CREATED)
                .build());

        routingRequest = transactionTemplate.execute(tt -> {
            List<UserScheduleRule> userScheduleRules = List.of(
                    publishUserShiftManager.mapUserToDefaultUserScheduleRule(shift, user)
            );

            Map<Long, RoutingCourier> couriersById =
                    createShiftRoutingRequestCommandFactory.mapCouriersFromUserSchedules(
                            userScheduleRules,
                            false,
                            Map.of(),
                            Map.of()
                    );


            CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                    .data(CreateShiftRoutingRequestCommandData.builder()
                            .routeDate(shift.getShiftDate())
                            .sortingCenter(shift.getSortingCenter())
                            .couriers(new HashSet<>(couriersById.values()))
                            .orders(List.of(order1, order2))
                            .movements(List.of())
                            .build()
                    )
                    .createdAt(clock.instant())
                    .mockType(RoutingMockType.REAL)
                    .build();
            return routingRequestCreator.createShiftRoutingRequest(command);
        });
    }

    @Test
    void shouldCreateUserShifts() {
        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);
        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);

        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

        transactionTemplate.execute(tt -> {
            List<UserShift> userShifts = userShiftRepository.findAllByShiftId(shift.getId());
            assertThat(userShifts).hasSize(1);

            UserShift userShift = userShifts.iterator().next();
            assertThat(userShift.getUser().getId()).isEqualTo(user.getId());

            assertThat(userShift.streamOrderDeliveryTasks().count()).isEqualTo(2);

            Set<Long> orderIds = userShift.streamOrderDeliveryTasks()
                    .map(OrderDeliveryTask::getOrderId)
                    .toSet();

            assertThat(orderIds).containsExactlyInAnyOrder(order1.getId(), order2.getId());
            return null;
        });
    }

}
