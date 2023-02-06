package ru.yandex.market.tpl.core.service.tracking;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.tracking.OrderDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.Task;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.tracking.PreviouslyConfirmationDeliveryStatus.NO_CONFIRMATION_NEEDED;

/**
 * @author aostrikov
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@CoreTest
class TrackingCreationTest {

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;

    private final UserShiftCommandService commandService;

    private final UserShiftRepository shiftRepository;
    private final TrackingRepository trackingRepository;

    private final TrackingService trackingService;
    private final OrderGenerateService orderGenerateService;

    private UserShift userShift;
    private ru.yandex.market.tpl.core.domain.order.Order order;

    private Tracking tracking;

    @BeforeEach
    void createShift() {
        LocalDate date = LocalDate.now();
        User user = testUserHelper.findOrCreateUser(824125L, date);
        order = orderGenerateService.createOrder();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(testUserHelper.findOrCreateOpenShift(date).getId())
                .routePoint(helper.taskPrepaid("to mister Boris", 12, order.getId()))
                .build();

        long shiftId = commandService.createUserShift(createCommand);
        userShift = shiftRepository.findById(shiftId).orElseThrow();
        tracking = trackingRepository.findByUserShiftId(userShift.getId()).get(0);

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
    }

    @Test
    @Order(5)
    void shouldShowTrackingAfterShiftStart() {
        testUserHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);
        TrackingDto trackingDto = trackingService.getTrackingDto(tracking.getId());

        OrderDto trackingOrder = trackingDto.getOrder();
        assertThat(trackingOrder.getId()).isEqualTo(order.getExternalOrderId());
        assertThat(trackingOrder.getPaymentStatus()).isEqualTo(order.getPaymentStatus());
        assertThat(trackingOrder.getItems().size()).isEqualTo(order.getItems().size());

        assertThat(trackingDto.getDelivery().getStatus()).isEqualTo(TrackingDeliveryStatus.IN_PROGRESS);

        assertThat(trackingDto.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
    }

    @Test
    @Order(10)
    void shouldShowCorrectStatusAfterCancellation() {
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        RoutePoint routePoint = userShift.streamDeliveryRoutePoints().findAny().orElseThrow();
        Task<?> task = routePoint.streamTasks().findAny().orElseThrow();

        commandService.failDeliveryTask(null,
                new UserShiftCommand.FailOrderDeliveryTask(userShift.getId(),
                        routePoint.getId(), task.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, null, null, Source.DELIVERY)
                ));

        TrackingDto trackingDto = trackingService.getTrackingDto(tracking.getId());

        assertThat(trackingDto.getDelivery().getStatus()).isEqualTo(TrackingDeliveryStatus.NOT_DELIVERED);

        assertThat(trackingDto.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
    }

}
