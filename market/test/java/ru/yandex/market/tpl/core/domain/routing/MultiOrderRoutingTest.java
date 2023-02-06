package ru.yandex.market.tpl.core.domain.routing;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.lms.routingschedule.RoutingScheduleRuleCreateDto;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.events.ShiftRoutingResultReceivedEvent;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingScheduleService;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.test.factory.TestTplRoutingFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class MultiOrderRoutingTest {

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();

    private final UserScheduleRuleRepository scheduleRuleRepository;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock = Clock.systemDefaultZone(); // TODO: пофиксить RoutingRequest.createdAt = Instant.now();

    private final RoutingRequestCreator routingRequestCreator;
    private final ShiftManager shiftManager;
    private final RoutingScheduleService routingScheduleService;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final TestTplRoutingFactory testTplRoutingFactory;

    private Shift shift;
    private String multiOrderId1;
    private String multiOrderId2;
    private String multiOrderId3;
    private RoutingRequest routingRequest;

    @BeforeEach
    void init() {
        LocalDate shiftDate = LocalDate.now(clock);

        shift = shiftManager.findOrCreate(shiftDate, SortingCenter.DEFAULT_SC_ID);

        userHelper.findOrCreateUser(82412901L, LocalDate.now(clock));


        GeoPoint geoPoint1 = GeoPointGenerator.generateLonLat();
        // первый мультизаказ
        AddressGenerator.AddressGenerateParam addressGenerateParam1 = AddressGenerator.AddressGenerateParam.builder()
                .regionId(117065)
                .geoPoint(geoPoint1)
                .street("Колотушкина")
                .house("2")
                .build();
        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(addressGenerateParam1)
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(addressGenerateParam1)
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        multiOrderId1 = String.format("m_%d_%d", order1.getId(), order2.getId());

        // просто заказ на тот же адрес
        Order order3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(addressGenerateParam1)
                .recipientPhone("phone2")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        multiOrderId2 = String.valueOf(order3.getId());

        // второй мультизаказ на другой адрес, c разными интервалами
        GeoPoint geoPoint2 = GeoPointGenerator.generateLonLat();
        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .regionId(100500)
                .geoPoint(geoPoint2)
                .street("Колотушкина")
                .house("1")
                .build();
        Order order4 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(addressGenerateParam)
                .recipientPhone("phone2")
                .deliveryInterval(LocalTimeInterval.valueOf("12:00-16:00"))
                .build());

        Order order5 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(addressGenerateParam)
                .recipientPhone("phone2")
                .deliveryInterval(LocalTimeInterval.valueOf("14:00-18:00"))
                .build());

        multiOrderId3 = String.format("m_%d_%d", order4.getId(), order5.getId());

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
        routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

    }

    @Test
    void routingRequestWithMultiOrders() {
        Map<String, RelativeTimeInterval> intervalByMultiOrderId = routingRequest.getItems().stream()
                .collect(Collectors.toMap(RoutingRequestItem::getTaskId, RoutingRequestItem::getInterval));

        assertThat(intervalByMultiOrderId).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        multiOrderId1, RelativeTimeInterval.valueOf("10:00-14:00"),
                        multiOrderId2, RelativeTimeInterval.valueOf("10:00-14:00"),
                        multiOrderId3, RelativeTimeInterval.valueOf("14:00-16:00")
                )
        );
    }

    @Test
    void applyRoutingResponseWithMultiOrders() {
        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);

        saveRoutingSchedule(true);

        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);

        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

        List<UserShift> userShifts = userShiftRepository.findAllByShiftId(shift.getId());

        assertThat(userShifts).hasSize(1);

        UserShift userShift = userShifts.iterator().next();

        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.COURIERS_ASSIGNED);
        assertThat(userShift.streamOrderDeliveryTasks().count()).isEqualTo(5);

        Set<String> multiOrderIds = userShift.streamOrderDeliveryTasks()
                .map(OrderDeliveryTask::getMultiOrderId)
                .toSet();

        assertThat(multiOrderIds).containsExactlyInAnyOrder(multiOrderId1, multiOrderId2, multiOrderId3);
    }

    @Test
    void applyRoutingResponseWithMultiOrdersLastWaveNotStarted() {
        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);
        saveRoutingSchedule(false);
        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);
        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

        List<UserShift> userShifts = userShiftRepository.findAllByShiftId(shift.getId());
        assertThat(userShifts).hasSize(1);
        UserShift userShift = userShifts.iterator().next();
        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.CREATED);
        assertThat(userShift.streamOrderDeliveryTasks().count()).isEqualTo(5);
        Set<String> multiOrderIds = userShift.streamOrderDeliveryTasks()
                .map(OrderDeliveryTask::getMultiOrderId)
                .toSet();
        assertThat(multiOrderIds).containsExactlyInAnyOrder(multiOrderId1, multiOrderId2, multiOrderId3);
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
