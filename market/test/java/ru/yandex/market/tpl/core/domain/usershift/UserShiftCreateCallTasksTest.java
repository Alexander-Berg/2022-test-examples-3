package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.MultiOrder;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class UserShiftCreateCallTasksTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;

    private User user;
    private Shift shift;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
    }

    @Test
    void shouldCreateShiftAndMergeMultiOrderDeliveryTaskToOneCallTask() {
        LocalDate now = LocalDate.now(clock);
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(now)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .recipientPhone("79998765432")
                        .build());

        Order anotherOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(now)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .recipientPhone("79998765432")
                        .build());

        MultiOrder multiOrder = MultiOrder.builder()
                .orders(List.of(order, anotherOrder))
                .interval(DateTimeUtil.DEFAULT_RELATIVE_INTERVAL)
                .build();

        var taskToMerge1 = helper.taskUnpaid("addr1", 12, order.getId());
        var taskToMerge2 = helper.cloneTask(taskToMerge1,
                taskToMerge1.getExpectedDeliveryTime().plus(4, ChronoUnit.MINUTES), anotherOrder.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(taskToMerge1)
                .routePoint(taskToMerge2) // will be merged
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        List<RoutePoint> deliveryRoutePoints = deliveryRoutePoints(userShift);
        assertThat(deliveryRoutePoints).isNotNull().hasSize(1);
        assertThat(deliveryRoutePoints.get(0).getTasks()).hasSize(2);

        List<CallToRecipientTask> callToRecipientTasks = userShift.getCallToRecipientTasks();
        assertThat(callToRecipientTasks).isNotNull();
        assertThat(callToRecipientTasks).hasSize(1);

        CallToRecipientTask callTask = callToRecipientTasks.iterator().next();

        for (OrderDeliveryTask orderDeliveryTask : callTask.getOrderDeliveryTasks()) {
            assertThat(orderDeliveryTask.getParentId()).isEqualTo(String.valueOf(callTask.getId()));
        }
    }

    private List<RoutePoint> deliveryRoutePoints(UserShift userShift) {
        return userShift.streamDeliveryRoutePoints()
                .collect(Collectors.toList());
    }

}
