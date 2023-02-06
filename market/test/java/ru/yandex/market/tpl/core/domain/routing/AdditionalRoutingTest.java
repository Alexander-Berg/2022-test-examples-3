package ru.yandex.market.tpl.core.domain.routing;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.lms.routingschedule.RoutingScheduleRuleCreateDto;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.events.ShiftRoutingResultReceivedEvent;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingScheduleService;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.test.factory.TestTplRoutingFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType.PARTIAL;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class AdditionalRoutingTest {
    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();

    private final UserScheduleRuleRepository scheduleRuleRepository;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock = Clock.systemDefaultZone();

    private final RoutingRequestCreator routingRequestCreator;
    private final ShiftManager shiftManager;
    private final RoutingScheduleService routingScheduleService;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final OrderRepository orderRepository;
    private final TestTplRoutingFactory testTplRoutingFactory;


    private Shift shift;
    private Order order1;
    private Order order2;
    private User user1;
    private User user2;

    private RoutingRequest routingRequest;
    private RoutingRequest routingRequest2;

    @BeforeEach
    void init() {
        LocalDate shiftDate = LocalDate.now(clock);

        shift = shiftManager.findOrCreate(shiftDate, SortingCenter.DEFAULT_SC_ID);

        user1 = userHelper.findOrCreateUser(824126L, LocalDate.now(clock));
        user2 =  userHelper.findOrCreateUser(824127L, LocalDate.now(clock));


        GeoPoint geoPoint1 = GeoPointGenerator.generateLonLat();
        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(117065)
                        .geoPoint(geoPoint1)
                        .build())
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        GeoPoint geoPoint2 = GeoPointGenerator.generateLonLat();
        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(order1.getDelivery().getDeliveryDateAtDefaultTimeZone().plusDays(2L))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(100500)
                        .geoPoint(geoPoint2)
                        .build())
                .recipientPhone("phone2")
                .deliveryInterval(LocalTimeInterval.valueOf("12:00-16:00"))
                .build());


        List<UserScheduleRule> usersSchedules = scheduleRuleRepository.findAllWorkingRulesForDate(
                shift.getShiftDate(),
                shift.getSortingCenter().getId());
        assertThat(usersSchedules).hasSize(2);

        Map<Long, RoutingCourier> couriersById =
                createShiftRoutingRequestCommandFactory.mapCouriersFromUserSchedules(
                        usersSchedules,
                        false,
                        Map.of(),
                        Map.of()
                );

        CreateShiftRoutingRequestCommand<CreateShiftRoutingRequestCommandData> command = CreateShiftRoutingRequestCommand.<CreateShiftRoutingRequestCommandData>builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(couriersById.values().stream().filter(e -> e.getId() == user1.getId())
                                .collect(Collectors.toSet()))
                        .orders(List.of(order1))
                        .movements(List.of())
                        .build()
                )
                .createdAt(clock.instant())
                .mockType(RoutingMockType.REAL)
                .build();
        routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

        command = CreateShiftRoutingRequestCommand.<CreateShiftRoutingRequestCommandData>builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(couriersById.values().stream().filter(e -> e.getId() == user2.getId())
                                .collect(Collectors.toSet()))
                        .orders(List.of(order2))
                        .movements(List.of())
                        .build()
                )
                .profileType(PARTIAL)
                .createdAt(clock.instant())
                .mockType(RoutingMockType.REAL)
                .build();
        routingRequest2 = routingRequestCreator.createShiftRoutingRequest(command);

    }

    @Test
    void applyAdditionalRoutingResponseOrders() {
        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);

        saveRoutingSchedule(true);

        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);

        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

        List<UserShift> userShifts = userShiftRepository.findAllByShiftId(shift.getId());

        assertThat(userShifts).hasSize(1);

        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.COURIERS_ASSIGNED);
        Order orderForAdditionalRouting = orderRepository.findById(order2.getId()).get();

        assertThat(orderForAdditionalRouting.getDelivery().getDeliveryDateAtDefaultTimeZone())
                .isEqualTo(order1.getDelivery().getDeliveryDateAtDefaultTimeZone().plusDays(2L));

        RoutingResult routingResult2 = routingApiDataHelper.mockResult(routingRequest2, false);
        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult2));

        orderForAdditionalRouting = orderRepository.findById(order2.getId()).get();

        assertThat(orderForAdditionalRouting.getDelivery().getDeliveryDateAtDefaultTimeZone())
                .isEqualTo(shift.getShiftDate());
    }

    private void saveRoutingSchedule(boolean lastWaveAlreadyStarted) {
        LocalTime mainRoutingStartTime = lastWaveAlreadyStarted
                ? DateTimeUtil.toLocalTime(Instant.now(clock).minusSeconds(1000))
                : DateTimeUtil.toLocalTime(Instant.now(clock).plusSeconds(1000));
        RoutingScheduleRuleCreateDto routingScheduleRuleGridView = new RoutingScheduleRuleCreateDto(
                shift.getSortingCenter().getId(),
                null,
                mainRoutingStartTime,
                true
        );
        routingScheduleService.save(routingScheduleRuleGridView);
    }

}
