package ru.yandex.market.tpl.core.query.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointListDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.user.UserMode.DEFAULT_MODE;
import static ru.yandex.market.tpl.api.model.user.UserMode.SOFT_MODE;
import static ru.yandex.market.tpl.api.model.user.UserMode.STRICT_MODE;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
@Slf4j
public class UserShiftQueryServiceUserModeTest {
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftQueryService queryService;
    private final Clock clock;
    private final TestUserHelper userHelper;
    private final UserPropertyService userPropertyService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private User user;
    private UserShift userShift;
    private Order order1;
    private Order order2;
    private CallToRecipientTask callTask;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());
        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());

        Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();
        RoutePointAddress myAddress = new RoutePointAddress("my_address", geoPoint);

        NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order1, false, false)
                .build();
        GeoPoint geoPoint2 = GeoPointGenerator.generateLonLat();
        RoutePointAddress myAddress2 = new RoutePointAddress("my_address_2", geoPoint2);

        NewDeliveryRoutePointData delivery2 = NewDeliveryRoutePointData.builder()
                .address(myAddress2)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name_2")
                .withOrderReferenceFromOrder(order2, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery1)
                .routePoint(delivery2)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        callTask = userShift.streamCallTasks().findFirst().orElseThrow();
    }

    @Test
    void getTasksInfoDifferentUserMode() {
        OrderDeliveryTasksDto tasksInfo = queryService.getTasksInfo(user, false);

        assertThat(tasksInfo.getTasks()).hasSize(2);

        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, STRICT_MODE.name());
        testUserHelper.clearUserPropertiesCache();

        tasksInfo = queryService.getTasksInfo(user, false);

        assertThat(tasksInfo.getTasks()).hasSize(0);

        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, SOFT_MODE.name());
        testUserHelper.clearUserPropertiesCache();

        tasksInfo = queryService.getTasksInfo(user, false);

        assertThat(tasksInfo.getTasks()).hasSize(2);

        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, DEFAULT_MODE.name());
        testUserHelper.clearUserPropertiesCache();

        tasksInfo = queryService.getTasksInfo(user, false);

        assertThat(tasksInfo.getTasks()).hasSize(2);

    }

    @Test
    void getRoutePointsSummaries() {

        RoutePointListDto routePointListDto = queryService.getRoutePointsSummaries(user);
        assertThat(routePointListDto.getRoutePoints()).hasSize(4);
        assertThat(routePointListDto.getCount()).isEqualTo(2);

        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, STRICT_MODE.name());
        testUserHelper.clearUserPropertiesCache();
        routePointListDto = queryService.getRoutePointsSummaries(user);
        assertThat(routePointListDto.getRoutePoints()).hasSize(0);
        assertThat(routePointListDto.getCount()).isEqualTo(2);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);
        routePointListDto = queryService.getRoutePointsSummaries(user);
        assertThat(routePointListDto.getRoutePoints()).hasSize(2);
        assertThat(routePointListDto.getCount()).isEqualTo(2);

        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, DEFAULT_MODE.name());
        testUserHelper.clearUserPropertiesCache();
        routePointListDto = queryService.getRoutePointsSummaries(user);
        assertThat(routePointListDto.getRoutePoints()).hasSize(4);
        assertThat(routePointListDto.getCount()).isEqualTo(2);


    }

    @Test
    void getRemainingTasksInfo() {
        //Смена не начата и не проставлен user_mode
        RemainingOrderDeliveryTasksDto remainingTasksInfo = queryService.getRemainingTasksInfo(user);
        assertThat(remainingTasksInfo.getIsCanBeOpenedOutOfTurn()).isFalse();

        //Смена не начата и user_mode = SOFT_MODE
        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, SOFT_MODE.name());
        remainingTasksInfo = queryService.getRemainingTasksInfo(user);
        assertThat(remainingTasksInfo.getIsCanBeOpenedOutOfTurn()).isFalse();

        //Смена не начата, user_mode = SOFT_MODE, забор не закончен
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        remainingTasksInfo = queryService.getRemainingTasksInfo(user);
        assertThat(remainingTasksInfo.getIsCanBeOpenedOutOfTurn()).isFalse();

        //Смена не начата, user_mode = SOFT_MODE, забор закончен
        userHelper.finishPickupAtStartOfTheDay(userShift);
        remainingTasksInfo = queryService.getRemainingTasksInfo(user);
        assertThat(remainingTasksInfo.getIsCanBeOpenedOutOfTurn()).isTrue();

        //Смена не начата, user_mode = STRICT_MODE, забор закончен
        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, STRICT_MODE.name());
        testUserHelper.clearUserPropertiesCache();
        remainingTasksInfo = queryService.getRemainingTasksInfo(user);
        assertThat(remainingTasksInfo.getIsCanBeOpenedOutOfTurn()).isFalse();
    }
}
