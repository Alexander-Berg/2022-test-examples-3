package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.CallRequirement;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.tvm.service.ServiceTicketRequest;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewOrderReturnRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.NOT_CALLED;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.SUCCESS;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class UserShiftReopenDeliveryTaskTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;

    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final TrackingRepository trackingRepository;
    private final TrackingService trackingService;

    private User user;
    private UserShift userShift;
    private RoutePoint routePoint;
    private OrderDeliveryTask deliveryTask;
    private Order order;

    @MockBean
    private Clock clock;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;
    private ServiceTicketRequest serviceTicket;

    void init() {
        init("");
    }

    void init(String recipientNotes) {
        ClockUtil.initFixed(clock);
        user = userHelper.findOrCreateUser(35236L);

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        order = getOrder(recipientNotes);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskPrepaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();

        userHelper.checkinAndFinishPickup(userShift);
        deliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();
        serviceTicket = new ServiceTicketRequest();
    }

    private Order getOrder(String recipientNotes) {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .recipientNotes(recipientNotes)
                .build());
    }

    @Test
    void testReopenDeliveryTaskWhenOrderReturnIsActive() {
        init();

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), routePoint.getId(), deliveryTask.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, "")
        ));
        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.ORDER_RETURN);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), deliveryTask.getRoutePoint().getId(), deliveryTask.getId(), Source.COURIER
        ));
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePoint);
    }

    @Test
    void testReopenDeliveryTaskAfterReturnWhenDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);
        init(DO_NOT_CALL_DELIVERY_PREFIX);

        commandService.addOrderReturnTask(user, new UserShiftCommand.AddOrderReturnTask(
                userShift.getId(), NewOrderReturnRoutePointData.build(userShift)));

        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), true);

        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.ORDER_RETURN);
        assertThat(userShift.getCallToRecipientTasks())
                .extracting(CallToRecipientTask::getStatus)
                .containsOnly(CLIENT_ASK_NOT_TO_CALL);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), deliveryTask.getRoutePoint().getId(), deliveryTask.getId(), Source.COURIER
        ));

        assertThat(userShift.getCallToRecipientTasks())
                .extracting(CallToRecipientTask::getStatus)
                .containsOnly(CLIENT_ASK_NOT_TO_CALL);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePoint);
    }

    @Test
    void testReopenDeliveryTaskAfterReturnWhenDoNotCallChanged() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);
        init(DO_NOT_CALL_DELIVERY_PREFIX);

        commandService.addOrderReturnTask(user, new UserShiftCommand.AddOrderReturnTask(
                userShift.getId(), NewOrderReturnRoutePointData.build(userShift)));

        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), true);

        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.ORDER_RETURN);
        assertThat(userShift.getCallToRecipientTasks())
                .extracting(CallToRecipientTask::getStatus)
                .containsOnly(CLIENT_ASK_NOT_TO_CALL);

        Optional<Tracking> trackingOpt = trackingRepository.findByOrderId(order.getId());
        trackingService.updateCallRequirement(trackingOpt.get().getId(), CallRequirement.CALL_REQUIRED, serviceTicket);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), deliveryTask.getRoutePoint().getId(), deliveryTask.getId(), Source.COURIER
        ));

        assertThat(userShift.getCallToRecipientTasks())
                .extracting(CallToRecipientTask::getStatus)
                .containsOnly(NOT_CALLED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePoint);
    }


    @Test
    void testReopenDeliveryTaskAfterReturnWhenDoNotCallChangedReverted() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);
        init();

        commandService.addOrderReturnTask(user, new UserShiftCommand.AddOrderReturnTask(
                userShift.getId(), NewOrderReturnRoutePointData.build(userShift)));

        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), true);

        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.ORDER_RETURN);
        var callToRecipientTasks = userShift.getCallToRecipientTasks();
        assertThat(callToRecipientTasks)
                .extracting(CallToRecipientTask::getStatus)
                .containsOnly(SUCCESS);

        var trackingOpt = trackingRepository.findByOrderId(order.getId());
        trackingService.updateCallRequirement(trackingOpt.get().getId(), CallRequirement.DO_NOT_CALL, serviceTicket);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), deliveryTask.getRoutePoint().getId(), deliveryTask.getId(), Source.COURIER
        ));

        callToRecipientTasks = userShift.getCallToRecipientTasks();
        assertThat(callToRecipientTasks)
                .extracting(CallToRecipientTask::getStatus)
                .containsOnly(NOT_CALLED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePoint);
    }
}
