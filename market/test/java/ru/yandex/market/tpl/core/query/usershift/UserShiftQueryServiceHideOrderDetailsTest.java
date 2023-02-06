package ru.yandex.market.tpl.core.query.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
@Slf4j
public class UserShiftQueryServiceHideOrderDetailsTest {
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftQueryService queryService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final Clock clock;

    private User user;

    void init(OrderFlowStatus orderFlowStatusOrderOne, OrderFlowStatus orderFlowStatusOrderTwo) {
        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var geoPoint = GeoPointGenerator.generateLonLat();
        var geoPoint2 = GeoPointGenerator.generateLonLat();
        var address = new RoutePointAddress("address", geoPoint);
        var address2 = new RoutePointAddress("my_address_2", geoPoint2);

        var order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(orderFlowStatusOrderOne)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());
        var order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(orderFlowStatusOrderTwo)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());

        Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();

        NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                .address(address)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order1, false, false)
                .build();

        NewDeliveryRoutePointData delivery2 = NewDeliveryRoutePointData.builder()
                .address(address2)
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

        commandService.createUserShift(createCommand);
    }


    @ParameterizedTest
    @MethodSource("provideTrueHidingParameters")
    void shouldHideOrderDetails_whenNewMapperEnabled(OrderFlowStatus orderFlowStatusOrderOne,
                                                     OrderFlowStatus orderFlowStatusOrderTwo) {
        init(orderFlowStatusOrderOne, orderFlowStatusOrderTwo);

        RemainingOrderDeliveryTasksDto remainingTasksInfo = queryService.getRemainingTasksInfo(user);
        assertThat(remainingTasksInfo.isShouldHideOrderDetails()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("provideFalseHidingParameters")
    void shouldNotHideOrderDetails(OrderFlowStatus orderFlowStatusOrderOne, OrderFlowStatus orderFlowStatusOrderTwo) {
        init(orderFlowStatusOrderOne, orderFlowStatusOrderTwo);

        RemainingOrderDeliveryTasksDto remainingTasksInfo = queryService.getRemainingTasksInfo(user);
        assertThat(remainingTasksInfo.isShouldHideOrderDetails()).isFalse();
    }

    private static Stream<Arguments> provideTrueHidingParameters() {
        return Stream.of(
                Arguments.of(OrderFlowStatus.CREATED, OrderFlowStatus.START_CREATE_SORTING_CENTER),
                Arguments.of(OrderFlowStatus.START_CREATE_SORTING_CENTER, OrderFlowStatus.SORTING_CENTER_CREATED),
                Arguments.of(OrderFlowStatus.SORTING_CENTER_CREATED, OrderFlowStatus.SORTING_CENTER_ARRIVED),
                Arguments.of(OrderFlowStatus.SORTING_CENTER_ARRIVED, OrderFlowStatus.SORTING_CENTER_PREPARED),
                Arguments.of(OrderFlowStatus.SORTING_CENTER_PREPARED, OrderFlowStatus.TRANSPORTATION_RECIPIENT),
                Arguments.of(OrderFlowStatus.START_CREATE_SORTING_CENTER, OrderFlowStatus.CANCELLED)
        );
    }

    private static Stream<Arguments> provideFalseHidingParameters() {
        return Stream.of(
                Arguments.of(OrderFlowStatus.DELIVERY_ATTEMPT_FAILED, OrderFlowStatus.READY_FOR_RETURN),
                Arguments.of(OrderFlowStatus.CANCELLED, OrderFlowStatus.TRANSMITTED_TO_RECIPIENT),
                Arguments.of(OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP,
                        OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP),
                Arguments.of(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT, OrderFlowStatus.TRANSPORTATION_RECIPIENT)
        );
    }
}
