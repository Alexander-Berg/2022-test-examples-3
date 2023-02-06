package ru.yandex.market.tpl.core.domain.shift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.RoutingRequestCreator;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class WaveResultsMatchingValidatorTest {

    private final WaveResultsMatchingValidator validator;

    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();
    private final RoutingRequestCreator routingRequestCreator;
    private final ShiftManager shiftManager;
    private final Clock clock;
    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserScheduleRuleRepository scheduleRuleRepository;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;

    private RoutingRequest routingRequest1;
    private RoutingRequest routingRequest2;

    @BeforeEach
    void init() {
        LocalDate shiftDate = LocalDate.now(clock);
        Shift shift = shiftManager.findOrCreate(shiftDate, SortingCenter.DEFAULT_SC_ID);
        userHelper.findOrCreateUser(824126L, LocalDate.now(clock));
        GeoPoint geoPoint1 = GeoPointGenerator.generateLonLat();
        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(117065)
                        .geoPoint(geoPoint1)
                        .build())
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(117065)
                        .geoPoint(geoPoint1)
                        .build())
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        Order order3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(117065)
                        .geoPoint(geoPoint1)
                        .build())
                .recipientPhone("phone2")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        GeoPoint geoPoint2 = GeoPointGenerator.generateLonLat();
        Order order4 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(100500)
                        .geoPoint(geoPoint2)
                        .build())
                .recipientPhone("phone2")
                .deliveryInterval(LocalTimeInterval.valueOf("12:00-16:00"))
                .build());

        Order order5 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(100500)
                        .geoPoint(geoPoint2)
                        .build())
                .recipientPhone("phone2")
                .deliveryInterval(LocalTimeInterval.valueOf("14:00-18:00"))
                .build());

        List<UserScheduleRule> users = scheduleRuleRepository.findAllWorkingRulesForDate(
                shift.getShiftDate(),
                shift.getSortingCenter().getId());
        assertThat(users).hasSize(1);

        Map<Long, RoutingCourier> couriersById =
                createShiftRoutingRequestCommandFactory.mapCouriersFromUserSchedules(
                        users,
                        false,
                        Map.of(),
                        Map.of()
                );

        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(new HashSet<>(couriersById.values()))
                        .orders(List.of(order1, order2, order3, order4, order5))
                        .movements(List.of())
                        .build()
                )
                .createdAt(clock.instant())
                .mockType(RoutingMockType.REAL)
                .build();
        routingRequest1 = routingRequestCreator.createShiftRoutingRequest(command);

        Order order6 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(117065)
                        .geoPoint(geoPoint1)
                        .build())
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        Order order7 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(117065)
                        .geoPoint(geoPoint1)
                        .build())
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        Order order8 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(117065)
                        .geoPoint(geoPoint1)
                        .build())
                .recipientPhone("phone2")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(new HashSet<>(couriersById.values()))
                        .orders(List.of(order6, order7, order8))
                        .movements(List.of())
                        .build()
                )
                .createdAt(clock.instant())
                .mockType(RoutingMockType.REAL)
                .build();
        routingRequest2 = routingRequestCreator.createShiftRoutingRequest(command);
    }

    @Test
    void validateSameResponsesIsOk() {
        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest1, false);
        validator.validate(routingResult, routingResult);
    }

    @Test
    void validateDifferentResponsesThrowsException() {
        RoutingResult routingResult1 = routingApiDataHelper.mockResult(routingRequest1, false);
        RoutingResult routingResult2 = routingApiDataHelper.mockResult(routingRequest2, false);
        assertThrows(TplIllegalStateException.class, () -> validator.validate(routingResult1, routingResult2));
    }
}
