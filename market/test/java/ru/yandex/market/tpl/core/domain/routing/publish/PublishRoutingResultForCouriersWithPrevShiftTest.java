package ru.yandex.market.tpl.core.domain.routing.publish;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
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
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.publish.PublishUserShiftManager;
import ru.yandex.market.tpl.core.domain.usershift.publish.RawUserShiftRepository;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultShift;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestTplRoutingFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.CASH;

@RequiredArgsConstructor
public class PublishRoutingResultForCouriersWithPrevShiftTest extends TplAbstractTest {

    private final TransactionTemplate transactionTemplate;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();
    private final RawUserShiftRepository rawUserShiftRepository;
    private final UserShiftRepository userShiftRepository;

    private final UserShiftCommandService commandService;

    private final RoutingRequestCreator routingRequestCreator;
    private final ShiftManager shiftManager;
    private final PublishUserShiftManager publishUserShiftManager;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final Clock clock;
    private final TestTplRoutingFactory testTplRoutingFactory;

    private Shift shift;
    private User user;
    private UserShift userShift;
    private Order order1;
    private Order order2;


    @BeforeEach
    void init() {
        LocalDate shiftDate = LocalDate.now(clock);
        long sortingCenterId = 47819L;

        shift = userHelper.findOrCreateOpenShiftForSc(shiftDate, sortingCenterId);
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
    }

    @Test
    void shouldCreateAnotherUserShiftAfterCloseMainUserShift() {
        // курьер - ударник производства
        createAndFinishUserShift();

        publishPartialRoutingResult();

        transactionTemplate.execute(tt -> {
            List<UserShift> userShifts = StreamEx.of(userShiftRepository.findAllByShiftId(shift.getId()))
                    .filter(us -> us.getStatus().isNotClosedOrFinished())
                    .toList();
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


    @Test
    void shouldSaveTwoRawUserShift() {
        var user1 = userHelper.findOrCreateUser(89155555L, LocalDate.now(clock));
        var user2 = userHelper.findOrCreateUser(89155556L, LocalDate.now(clock));
        RoutingResult routingResult = RoutingResult.builder()
                .shiftsByUserId(Map.of(
                        user1.getId(), new RoutingResultShift(user1.getId(), List.of()),
                        user2.getId(), new RoutingResultShift(user2.getId(), List.of())
                ))
                .build();
        ShiftRoutingResultReceivedEvent event = new ShiftRoutingResultReceivedEvent(shift, routingResult);
        publishUserShiftManager.processByUserShifts(event, shift);
        assertThat(rawUserShiftRepository.findAll().size()).isEqualTo(2);
    }

    private void publishPartialRoutingResult() {
        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shift.getShiftDate())
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.CREATED)
                .build());

        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shift.getShiftDate())
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.CREATED)
                .build());

        RoutingRequest routingRequest = transactionTemplate.execute(tt -> {
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
                    .profileType(RoutingProfileType.PARTIAL)
                    .build();
            return routingRequestCreator.createShiftRoutingRequest(command);
        });


        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);
        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);
        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));
    }

    private void createAndStartUserShift() {
        var deliveryTask = helper.taskUnpaid(
                "addr1",
                12,
                orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .paymentStatus(OrderPaymentStatus.UNPAID)
                                .paymentType(CASH)
                                .build()
                ).getId()
        );

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(deliveryTask)
                .active(true)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        commandService.checkin(userShift.getUser(), new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, true);
    }

    private void createAndFinishUserShift() {
        createAndStartUserShift();
        userHelper.finishAllDeliveryTasks(userShift.getId());
        userHelper.finishFullReturnAtEnd(userShift.getId());
        userHelper.finishUserShift(userShift.getId());
    }

}

