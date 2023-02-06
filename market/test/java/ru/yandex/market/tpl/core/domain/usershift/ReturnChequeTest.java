package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeResponseDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeStatus;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.receipt.FiscalReceiptStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.user.UserMode;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptDataRepository;
import ru.yandex.market.tpl.core.domain.receipt.test.TestReceiptProcessor;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.lifepay.LifePayClient;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static ru.yandex.market.tpl.api.model.user.UserMode.SOFT_MODE;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

/**
 * @author kukabara
 */
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReturnChequeTest {

    private final UserShiftCommandService commandService;
    private final UserShiftQueryService queryService;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final OrderHistoryEventRepository orderHistoryEventRepository;

    private final UserShiftRepository userShiftRepository;
    private final UserShiftManager userShiftManager;
    private final ReceiptDataRepository receiptDataRepository;
    private final UserPropertyService userPropertyService;

    private final UserShiftCommandDataHelper helper;

    private final TestReceiptProcessor testReceiptProcessor;
    private final Clock clock;


    @MockBean
    private LifePayClient lifePayClient;

    private User user;
    private Shift shift;

    @BeforeEach
    void init() {
        LocalDate date = LocalDate.now();
        user = testUserHelper.findOrCreateUser(955L, date);
        shift = testUserHelper.findOrCreateOpenShift(date);
    }

    @Test
    void shouldReturnCheque() {
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();

        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .paymentType(OrderPaymentType.PREPAID)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());

        openShift(userShiftId);
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));

        RoutePointDto routePointDto = queryService.getRoutePointInfo(user, routePointId);
        for (TaskDto task : routePointDto.getTasks()) {
            if (task instanceof OrderDeliveryTaskDto) {
                OrderDeliveryTaskDto orderTask = (OrderDeliveryTaskDto) task;
                OrderDeliveryTaskStatus statusBefore = orderTask.getStatus();

                long taskId = orderTask.getId();
                OrderPaymentType paymentType = orderTask.getOrder().getPaymentType().isPrepaid()
                        ? OrderPaymentType.PREPAID : OrderPaymentType.CARD;

                deliverTask(userShiftId, routePointId, taskId, paymentType);
                returnTask(userShiftId, routePointId, taskId, paymentType, statusBefore);
                deliverTask(userShiftId, routePointId, taskId, paymentType);

                assertThat(queryService.getDeliveryTaskInfo(user, routePointId, taskId).getOrder().isHasReturn()).isTrue();
            }
        }

    }

    @Test
    void shouldPrintAndThenReturnDemoCheque() {
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(), SortingCenter.DEMO_SC_ID);
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();

        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .paymentType(OrderPaymentType.PREPAID)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());

        openShift(userShiftId);
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));

        RoutePointDto routePointDto = queryService.getRoutePointInfo(user, routePointId);
        for (TaskDto task : routePointDto.getTasks()) {
            if (task instanceof OrderDeliveryTaskDto) {
                OrderDeliveryTaskDto orderTask = (OrderDeliveryTaskDto) task;
                OrderDeliveryTaskStatus statusBefore = orderTask.getStatus();

                long taskId = orderTask.getId();
                OrderPaymentType paymentType = orderTask.getOrder().getPaymentType().isPrepaid()
                        ? OrderPaymentType.PREPAID : OrderPaymentType.CARD;

                deliverTask(userShiftId, routePointId, taskId, paymentType);
                returnTask(userShiftId, routePointId, taskId, paymentType, statusBefore);
                deliverTask(userShiftId, routePointId, taskId, paymentType);

                assertThat(queryService.getDeliveryTaskInfo(user, routePointId, taskId).getOrder().isHasReturn()).isTrue();
            }
        }

    }

    @Test
    void shouldSuccessPrintChequeOnDuplicateRequest() {
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();

        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .paymentType(OrderPaymentType.PREPAID)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());

        openShift(userShiftId);
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));

        RoutePointDto routePointDto = queryService.getRoutePointInfo(user, routePointId);
        for (TaskDto task : routePointDto.getTasks()) {
            if (task instanceof OrderDeliveryTaskDto) {
                OrderDeliveryTaskDto orderTask = (OrderDeliveryTaskDto) task;
                OrderDeliveryTaskStatus statusBefore = orderTask.getStatus();

                long taskId = orderTask.getId();

                OrderPaymentType paymentType = orderTask.getOrder().getPaymentType().isPrepaid() ?
                        OrderPaymentType.PREPAID : OrderPaymentType.CARD;

                deliverTask(userShiftId, routePointId, taskId, paymentType);

                // еще один запрос на печать чека не должен упасть
                UserShiftCommand.PrintOrReturnCheque command = new UserShiftCommand.PrintOrReturnCheque(
                        userShiftId, routePointId, taskId, helper.getChequeDto(paymentType), Instant.now(clock), false,
                        null, Optional.empty()
                );
                commandService.printCheque(user, command);

                returnTask(userShiftId, routePointId, taskId, paymentType, statusBefore);
                deliverTask(userShiftId, routePointId, taskId, paymentType);
            }
        }
    }

    @Test
    void shouldSuccessReturnChequeOnDuplicateRequest() {
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();

        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .paymentType(OrderPaymentType.PREPAID)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());

        openShift(userShiftId);
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));

        RoutePointDto routePointDto = queryService.getRoutePointInfo(user, routePointId);
        for (TaskDto task : routePointDto.getTasks()) {
            if (task instanceof OrderDeliveryTaskDto) {
                OrderDeliveryTaskDto orderTask = (OrderDeliveryTaskDto) task;
                OrderDeliveryTaskStatus statusBefore = orderTask.getStatus();

                long taskId = orderTask.getId();
                OrderPaymentType paymentType = orderTask.getOrder().getPaymentType().isPrepaid()
                        ? OrderPaymentType.PREPAID : OrderPaymentType.CARD;

                deliverTask(userShiftId, routePointId, taskId, paymentType);
                returnTask(userShiftId, routePointId, taskId, paymentType, statusBefore);

                commandService.returnCheque(
                        user,
                        new UserShiftCommand.PrintOrReturnCheque(
                                userShiftId, routePointId, taskId, helper.getChequeDto(paymentType),
                                Instant.now(clock), false, null, Optional.empty()));

                returnTask(userShiftId, routePointId, taskId, paymentType, statusBefore);
                deliverTask(userShiftId, routePointId, taskId, paymentType);

                assertThat(queryService.getDeliveryTaskInfo(user, routePointId, taskId).getOrder().isHasReturn()).isTrue();
            }
        }

    }

    @Test
    void shouldReturnChequeRemoteFiscalization() {
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();

        OrderDeliveryTask prepaidTask = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .paymentType(OrderPaymentType.PREPAID)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build());

        openShift(userShiftId);
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));

        RoutePointDto routePointDto = queryService.getRoutePointInfo(user, routePointId);
        for (TaskDto task : routePointDto.getTasks()) {
            if (task instanceof OrderDeliveryTaskDto) {
                OrderDeliveryTaskDto orderTask = (OrderDeliveryTaskDto) task;
                OrderDeliveryTaskStatus statusBefore = orderTask.getStatus();

                long taskId = orderTask.getId();
                OrderPaymentType paymentType = orderTask.getOrder().getPaymentType().isPrepaid()
                        ? OrderPaymentType.PREPAID : OrderPaymentType.CARD;

                deliverTaskRemoteFiscalization(userShiftId, routePointId, taskId, paymentType);
                returnTaskRemoteFiscalization(userShiftId, routePointId, taskId, statusBefore, paymentType);
                deliverTaskRemoteFiscalization(userShiftId, routePointId, taskId, paymentType);

                assertThat(queryService.getDeliveryTaskInfo(user, routePointId, taskId).getOrder().isHasReturn()).isTrue();

            }
        }
        allChequesPrinted(user, routePointDto);

        assertThat(orderHistoryEventRepository.findAllByOrderId(prepaidTask.getOrderId()))
                .extracting(OrderHistoryEvent::getType)
                .contains(OrderEventType.TRANSMISSION_REVERTED);
    }

    @Test
    void shouldReopenRoutePointWhenReturnCheque() {
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();
        OrderPaymentType paymentType = OrderPaymentType.PREPAID;
        long taskId = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .paymentType(paymentType)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build()).getId();
        openShift(userShiftId);
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));
        deliverAndAssertStatuses(userShiftId, routePointId, taskId, paymentType);

        returnTask(userShiftId, routePointId, taskId, paymentType, OrderDeliveryTaskStatus.NOT_DELIVERED);

        assertThat(queryService.getRoutePointInfo(user, routePointId).getStatus())
                .isEqualTo(RoutePointStatus.UNFINISHED);
        UserShiftDto userShiftDto = queryService.getUserShiftDto(user, userShiftId);
        assertThat(userShiftDto.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);

        deliverAndAssertStatuses(userShiftId, routePointId, taskId, paymentType);
    }

    @Test
    void shouldReopenRoutePointWhenReturnRemoteFiscalization() {
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();
        var paymentType = OrderPaymentType.CASH;
        long taskId = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(paymentType)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build()).getId();
        openShift(userShiftId);
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));
        deliverAndAssertStatusesRemoteFiscalization(userShiftId, routePointId, taskId, paymentType);

        returnTaskRemoteFiscalization(userShiftId, routePointId, taskId, OrderDeliveryTaskStatus.NOT_DELIVERED,
                paymentType);

        RoutePointDto routePointInfo = queryService.getRoutePointInfo(user, routePointId);
        assertThat(routePointInfo.getStatus())
                .isEqualTo(RoutePointStatus.UNFINISHED);
        UserShiftDto userShiftDto = queryService.getUserShiftDto(user, userShiftId);
        assertThat(userShiftDto.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        deliverAndAssertStatusesRemoteFiscalization(userShiftId, routePointId, taskId, paymentType);

        allChequesPrinted(user, routePointInfo);
    }

    @Test
    void shouldThrow404ForPrintChequeOnNotFoundTaskInCurrentShift() {
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();

        var taskId = testDataFactory.addDeliveryTaskManual(
                user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .paymentType(OrderPaymentType.PREPAID)
                        .build()
        ).getId();

        openShift(userShiftId);
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));

        assertThatThrownBy(() -> {
            UserShiftCommand.PrintOrReturnCheque command = new UserShiftCommand.PrintOrReturnCheque(
                    userShiftId,
                    routePointId,
                    taskId + 10000,
                    helper.getChequeDto(OrderPaymentType.PREPAID),
                    Instant.now(clock),
                    false,
                    null,
                    Optional.empty()
            );
            commandService.printCheque(user, command);
        }).isInstanceOf(TplEntityNotFoundException.class);
    }

    private void deliverAndAssertStatuses(long userShiftId, long routePointId, long taskId,
                                          OrderPaymentType paymentType) {
        deliverTask(userShiftId, routePointId, taskId, paymentType);
        assertThat(queryService.getRoutePointInfo(user, routePointId).getStatus())
                .isEqualTo(RoutePointStatus.FINISHED);
    }

    private void openShift(long userShiftId) {
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        UserShift userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
    }

    private void deliverTask(long userShiftId, Long routePointId, long taskId, OrderPaymentType paymentType) {
        if (paymentType != OrderPaymentType.PREPAID) {
            commandService.payOrder(user,
                    new UserShiftCommand.PayOrder(userShiftId, routePointId, taskId, paymentType, null)
            );
        }
        commandService.printCheque(user,
                new UserShiftCommand.PrintOrReturnCheque(
                        userShiftId, routePointId, taskId, helper.getChequeDto(paymentType), Instant.now(clock), false, null,
                        Optional.empty()
                )
        );

        OrderDeliveryTaskDto taskInfo = queryService.getDeliveryTaskInfo(user, routePointId, taskId);
        assertThat(taskInfo.getStatus())
                .isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
        assertThat(taskInfo.getOrder().getPaymentType())
                .describedAs("После оплаты не обновили paymentType в заказе")
                .isEqualTo(paymentType);
        assertThat(taskInfo.getOrder().getPaymentStatus())
                .isEqualTo(OrderPaymentStatus.PAID);
    }

    private void returnTask(long userShiftId, Long routePointId, long taskId,
                            OrderPaymentType paymentType, OrderDeliveryTaskStatus statusBefore) {
        commandService.returnCheque(user, new UserShiftCommand.PrintOrReturnCheque(userShiftId, routePointId, taskId,
                helper.getChequeDto(paymentType), Instant.now(clock), false, null, Optional.empty()));

        OrderDeliveryTaskDto taskInfo = queryService.getDeliveryTaskInfo(user, routePointId, taskId);
        assertThat(taskInfo.getStatus()).isEqualTo(statusBefore);
        assertThat(taskInfo.getOrder().getPaymentStatus())
                .isEqualTo(paymentType == OrderPaymentType.PREPAID ? OrderPaymentStatus.PAID :
                        OrderPaymentStatus.UNPAID);
    }

    private void deliverAndAssertStatusesRemoteFiscalization(long userShiftId, long routePointId, long taskId,
                                                             OrderPaymentType paymentType) {
        deliverTaskRemoteFiscalization(userShiftId, routePointId, taskId, paymentType);
        assertThat(queryService.getRoutePointInfo(user, routePointId).getStatus())
                .isEqualTo(RoutePointStatus.FINISHED);
    }

    private void deliverTaskRemoteFiscalization(long userShiftId, Long routePointId, long taskId,
                                                OrderPaymentType paymentType) {
        deliverTaskRemoteFiscalization(userShiftId, routePointId, taskId, paymentType, true);
    }

    private void deliverTaskRemoteFiscalization(long userShiftId, Long routePointId, long taskId,
                                                OrderPaymentType paymentType, boolean runFiscalization) {
        if (paymentType != OrderPaymentType.PREPAID) {
            commandService.payOrder(user,
                    new UserShiftCommand.PayOrder(userShiftId, routePointId, taskId, paymentType, null));
        }
        printChequeRemoteFiscalization(OrderChequeType.SELL, userShiftId, routePointId, taskId, paymentType);

        OrderDeliveryTaskDto taskInfo = queryService.getDeliveryTaskInfo(user, routePointId, taskId);
        assertThat(taskInfo.getStatus())
                .isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
        assertThat(taskInfo.getOrder().getPaymentType())
                .describedAs("После оплаты не обновили paymentType в заказе")
                .isEqualTo(paymentType);
        assertThat(taskInfo.getOrder().getPaymentStatus())
                .isEqualTo(OrderPaymentStatus.PAID);

        if (runFiscalization) {
            runFiscalization();
        }
    }

    private void returnTaskRemoteFiscalization(long userShiftId, Long routePointId, long taskId,
                                               OrderDeliveryTaskStatus statusBefore,
                                               OrderPaymentType paymentType) {
        printChequeRemoteFiscalization(OrderChequeType.RETURN, userShiftId, routePointId, taskId, paymentType);

        OrderDeliveryTaskDto taskInfo = queryService.getDeliveryTaskInfo(user, routePointId, taskId);
        assertThat(taskInfo.getStatus()).isEqualTo(statusBefore);
        assertThat(taskInfo.getOrder().getPaymentStatus())
                .isEqualTo(paymentType == OrderPaymentType.PREPAID
                        ? OrderPaymentStatus.PAID
                        : OrderPaymentStatus.UNPAID);
    }

    private void allChequesPrinted(User user, RoutePointDto routePointDto) {
        for (TaskDto task : routePointDto.getTasks()) {
            OrderDeliveryTaskDto taskInfo = queryService.getDeliveryTaskInfo(user, routePointDto.getId(), task.getId());
            boolean shouldBePrinted = taskInfo.getOrder().getPaymentType() != OrderPaymentType.PREPAID;
            if (shouldBePrinted) {
                assertThat(taskInfo.getOrder().getCheques())
                        .extracting(OrderChequeResponseDto::getStatus)
                        .containsOnly(OrderChequeStatus.PRINTED);
            } else {
                assertThat(taskInfo.getOrder().getCheques())
                        .extracting(OrderChequeResponseDto::getStatus)
                        .isEmpty();
            }
        }
    }

    @SneakyThrows
    private void printChequeRemoteFiscalization(OrderChequeType chequeType,
                                                long userShiftId, Long routePointId, long taskId,
                                                OrderPaymentType paymentType) {
        UserShiftCommand.PrintOrReturnCheque command = new UserShiftCommand.PrintOrReturnCheque(
                userShiftId, routePointId, taskId,
                new OrderChequeRemoteDto(paymentType, chequeType), Instant.now(clock),
                SOFT_MODE.equals(UserMode.valueOf(
                        userPropertyService.findPropertyForUser(UserProperties.USER_MODE, user))), null,
                Optional.empty()
        );
        if (chequeType == OrderChequeType.SELL) {
            commandService.printCheque(user, command);
        } else {
            commandService.returnCheque(user, command);
        }
    }

    private void runFiscalization() {
        receiptDataRepository.findAll().stream()
                .filter(rd -> rd.getStatus() == FiscalReceiptStatus.PROCESSING)
                .forEach(testReceiptProcessor::registerCheque);
    }

}
