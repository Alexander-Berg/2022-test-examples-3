package ru.yandex.market.tpl.core.domain.routing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.LockerSubtaskType;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.events.ShiftRoutingResultReceivedEvent;
import ru.yandex.market.tpl.core.domain.routing.logistic_request.Routable;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestCollector;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.additional_data.UserShiftAdditionalDataRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.publish.PublishUserShiftManager;
import ru.yandex.market.tpl.core.domain.usershift.publish.PublishUserShiftsMerger;
import ru.yandex.market.tpl.core.domain.usershift.publish.RawUserShift;
import ru.yandex.market.tpl.core.domain.usershift.publish.RawUserShiftRepository;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourierVehicleType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultShift;
import ru.yandex.market.tpl.core.external.routing.api.RoutingScheduleData;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleData;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkRepository;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestClientReturnFactory;
import ru.yandex.market.tpl.core.test.factory.TestTplRoutingFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.PUBLISH_ROUTING_BY_USER_SHIFTS_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.PUBLISH_WAVE_VALIDATION_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED;
import static ru.yandex.market.tpl.core.domain.usershift.publish.UserShiftPublishStatus.CREATED;
import static ru.yandex.market.tpl.core.domain.usershift.publish.UserShiftPublishStatus.ERROR;
import static ru.yandex.market.tpl.core.domain.usershift.publish.UserShiftPublishStatus.IN_PROGRESS;
import static ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType.GROUP;
import static ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType.PARTIAL;

@RequiredArgsConstructor
public class UserShiftPublishTest extends TplAbstractTest {
    private static final long LOGISTICPOINT_ID_FOR_RETURN_DROPOFF = 123456789L;
    private static final String PARTIAL_PROCESSING_ID = "partial-processing-id";
    private static final String GROUP_PROCESSING_ID = "group-processing-id";

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();

    private final UserScheduleRuleRepository scheduleRuleRepository;
    private final Clock clock = Clock.systemDefaultZone();

    private final RoutingRequestCreator routingRequestCreator;
    private final ShiftManager shiftManager;
    private final PublishUserShiftManager publishUserShiftManager;
    private final TransactionTemplate transactionTemplate;
    private final RawUserShiftRepository rawUserShiftRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final PublishUserShiftsMerger merger;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftRepository userShiftRepository;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final VehicleGenerateService vehicleGenerateService;
    private final UserShiftAdditionalDataRepository userShiftAdditionalDataRepository;
    private final TestClientReturnFactory testClientReturnFactory;
    private final SortingCenterRepository sortingCenterRepository;
    private final DsZoneOffsetCachingService dsZoneOffsetCachingService;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final SpecialRequestCollector specialRequestCollector;
    private final LogisticRequestLinkRepository logisticRequestLinkRepository;
    private final TestTplRoutingFactory testTplRoutingFactory;
    User user1;
    User user2;
    User user3;
    User user4;
    User user5;
    User user6;
    User user7;
    User user8;
    User user9;
    User user10;
    private Shift shift;
    private Order order1;
    private Order order2;
    private Order lockerOrder;
    private User user;
    private Movement dropoffMovement;
    private Order orderInDropoffPvz;
    private ClientReturn clientReturn;
    private SpecialRequest specialRequest;
    private Routable routable1;

    private RoutingRequest routingRequest;

    @AfterEach
    void after() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMART_CONSOLIDATION_ORDERS_FOR_LAVKA, false);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SortingCenterProperties.SPECIAL_REQUEST_ADD_TO_MVRP_REQUEST_ENABLED,
                false
        );
    }

    void init(boolean logisticRequestsEnabled) {

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SortingCenterProperties.SPECIAL_REQUEST_ADD_TO_MVRP_REQUEST_ENABLED,
                logisticRequestsEnabled
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SortingCenterProperties.ROUTABLE_ROUTING_ENABLED,
                logisticRequestsEnabled
        );

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMART_CONSOLIDATION_ORDERS_FOR_LAVKA, true);

        LocalDate shiftDate = LocalDate.now(clock);

        shift = shiftManager.findOrCreate(shiftDate, SortingCenter.DEFAULT_SC_ID);

        user = userHelper.findOrCreateUser(424126L, LocalDate.now(clock));


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

        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LAVKA, LOGISTICPOINT_ID_FOR_RETURN_DROPOFF, 1L));

        GeoPoint geoPoint3 = GeoPointGenerator.generateLonLat();
        orderInDropoffPvz = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(order1.getDelivery().getDeliveryDateAtDefaultTimeZone().plusDays(2L))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(100500)
                        .geoPoint(geoPoint3)
                        .build())
                .pickupPoint(pickupPoint)
                .recipientPhone("phone2")
                .deliveryInterval(LocalTimeInterval.valueOf("12:00-16:00"))
                .build());

        dropoffMovement =
                testDataFactory.buildDropOffReturnMovement(String.valueOf(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF));

        clientReturn =
                testClientReturnFactory.buildAndSave(shift.getSortingCenter().getDeliveryServices().get(0).getId(),
                        shift.getShiftDate().atStartOfDay().minusDays(1L),
                        shift.getShiftDate().atStartOfDay().minusDays(1L).plusHours(4),
                        "+700",
                        geoPoint1.getLatitude(),
                        geoPoint1.getLongitude()
                );

        var lockerPickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 12L, 1L));
        lockerOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(lockerPickupPoint)
                .build());
        specialRequest = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(lockerPickupPoint.getId())
                        .build());
        routable1 = specialRequestCollector.convertToRoutable(specialRequest);

        List<UserScheduleRule> usersSchedules = scheduleRuleRepository.findAllWorkingRulesForDate(
                shift.getShiftDate(),
                shift.getSortingCenter().getId());

        Map<Long, RoutingCourier> couriersById =
                createShiftRoutingRequestCommandFactory.mapCouriersFromUserSchedules(
                        usersSchedules,
                        false,
                        Map.of(),
                        Map.of()
                );


        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(new HashSet<>(couriersById.values()))
                        .orders(List.of(order1, order2, orderInDropoffPvz, lockerOrder))
                        .movements(List.of(dropoffMovement))
                        .clientReturns(List.of(clientReturn))
                        .routableList(List.of(routable1))
                        .build()
                )
                .createdAt(clock.instant())
                .mockType(RoutingMockType.REAL)
                .build();
        routingRequest = routingRequestCreator.createShiftRoutingRequest(command);
        configurationServiceAdapter.insertValue(PUBLISH_ROUTING_BY_USER_SHIFTS_ENABLED, true);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.PUBLISH_ROUTING_BY_USER_SHIFTS_ENABLED, true);
    }

    @Test
    void shouldPublishSpecialRequestWithLockerTask() {
        transactionTemplate.execute(status -> {
            init(true);
            var routingResult = routingApiDataHelper.mockResult(routingRequest, false);
            shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));
            publishUserShiftManager.publishUserShift(shift.getId(), user.getId());
            return null;
        });

        transactionTemplate.execute(status -> {
            var userShifts = userShiftRepository.findAllByShiftId(shift.getId());
            var flowTasks = userShifts.stream()
                    .flatMap(UserShift::streamRoutePoints)
                    .flatMap(RoutePoint::streamFlowTasks)
                    .collect(Collectors.toList());
            assertThat(flowTasks).hasSize(1);
            var links = logisticRequestLinkRepository.findLinksForTask(flowTasks.get(0).getId());
            assertThat(links).hasSize(1);
            assertThat(links.get(0).getLogisticRequestId()).isEqualTo(specialRequest.getId());

            var lockerTask = userShifts.stream()
                    .flatMap(UserShift::streamRoutePoints)
                    .flatMap(RoutePoint::streamLockerDeliveryTasks)
                    .flatMap(LockerDeliveryTask::streamSubtask)
                    .filter(ldt -> Objects.equals(ldt.getOrderId(), lockerOrder.getId()))
                    .findFirst()
                    .map(LockerSubtask::getTask)
                    .orElseThrow();

            assertThat(flowTasks.get(0).getRoutePoint().getId()).isEqualTo(lockerTask.getRoutePoint().getId());

            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCreateTaskInRawUserShift(boolean logisticRequestEnabled) {
        transactionTemplate.execute((a) -> {
            init(logisticRequestEnabled);
            RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);
            routingResult.getProfileType();


            shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));


            assertThat(rawUserShiftRepository.findAll().size()).isEqualTo(1);

            var rawUserShiftResult = rawUserShiftRepository.findAll().get(0);
            assertThat(rawUserShiftResult.getShiftId()).isEqualTo(shift.getId());
            assertThat(rawUserShiftResult.getUserId()).isEqualTo(user.getId());
            assertThat(rawUserShiftResult.getRawSource()).isNotEmpty();
            return 1;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPublishUserShift(boolean logisticRequestEnabled) {
        transactionTemplate.execute((a) -> {
            init(logisticRequestEnabled);
            RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);
            routingResult.getProfileType();

            shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));


            assertThat(rawUserShiftRepository.findAll().size()).isEqualTo(1);
            publishUserShiftManager.publishUserShift(shift.getId(), user.getId());

            var rawUserShiftResult = rawUserShiftRepository.findAll().get(0);
            assertThat(rawUserShiftResult.getOrders()).isNotEmpty();
            var orders = StreamEx.of(rawUserShiftResult.getOrders().split(",")).map(Long::valueOf).toSet();
            assertThat(orders).contains(order1.getId());
            assertThat(orders).contains(order2.getId());
            assertThat(rawUserShiftResult.getStatus()).isEqualTo(CREATED);
            return 1;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldDeleteUserShiftWithAdditionalData(boolean logisticRequestsEnabled) {
        transactionTemplate.execute((a) -> {
            init(logisticRequestsEnabled);
            return 1;
        });

        var vehicle = vehicleGenerateService.generateVehicle();
        vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                .users(List.of(user))
                .registrationNumber("A000AA")
                .registrationNumberRegion("111")
                .vehicle(vehicle)
                .build());

        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);

        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

        assertThat(rawUserShiftRepository.findAll().size()).isEqualTo(1);
        publishUserShiftManager.publishUserShift(shift.getId(), user.getId());

        var rawUserShiftResult = rawUserShiftRepository.findAll().get(0);
        assertThat(rawUserShiftResult.getOrders()).isNotEmpty();
        assertThat(rawUserShiftResult.getStatus()).isEqualTo(CREATED);
        var userShift = userShiftRepository.findByShiftIdAndUserId(shift.getId(), user.getId()).orElseThrow();
        userShiftAdditionalDataRepository.findByUserShiftId(userShift.getId()).orElseThrow();


        assertDoesNotThrow(() -> publishUserShiftManager.publishUserShift(shift.getId(), user.getId()));
        assertThat(userShiftAdditionalDataRepository.findByUserShiftId(userShift.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldIncrementAttemptCount(boolean logisticRequestsEnabled) {
        transactionTemplate.execute((a) -> {
            init(logisticRequestsEnabled);
            var routingResult = routingApiDataHelper.mockResult(routingRequest, false);

            shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));
            publishUserShiftManager.publishUserShift(shift.getId(), user.getId());

            var rawUserShiftResult = rawUserShiftRepository.findAll().get(0);
            assertThat(rawUserShiftResult.getSequentialErrorAttemptCount()).isEqualTo(0L);
            return 1;
        });

        var rawUserShiftResult = rawUserShiftRepository.findAll().get(0);

//            Делаем невалидным чтобы кинулось исключение
        var validRawSource = rawUserShiftResult.getRawSource();
        rawUserShiftResult.setRawSource("INVALID_SOURCE");
        rawUserShiftRepository.save(rawUserShiftResult);
        assertThrows(RuntimeException.class, () -> publishUserShiftManager.publishUserShift(shift.getId(),
                user.getId()));
        rawUserShiftResult = rawUserShiftRepository.findAll().get(0);
        assertThat(rawUserShiftResult.getSequentialErrorAttemptCount()).isEqualTo(1L);

        assertThrows(RuntimeException.class, () -> publishUserShiftManager.publishUserShift(shift.getId(),
                user.getId()));
        rawUserShiftResult = rawUserShiftRepository.findAll().get(0);
        assertThat(rawUserShiftResult.getSequentialErrorAttemptCount()).isEqualTo(2L);

        rawUserShiftResult.setRawSource(validRawSource);
        rawUserShiftRepository.save(rawUserShiftResult);
        publishUserShiftManager.publishUserShift(shift.getId(), user.getId());

        rawUserShiftResult = rawUserShiftRepository.findAll().get(0);
        assertThat(rawUserShiftResult.getSequentialErrorAttemptCount()).isEqualTo(0L);

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPublishUserShift_withMixedFlow(boolean logisticRequestsEnabled) {
        //given
        transactionTemplate.execute((a) -> {
            init(logisticRequestsEnabled);
            RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);

            shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));
            return 1;
        });

        //when
        publishUserShiftManager.publishUserShift(shift.getId(), user.getId());


        //then
        transactionTemplate.execute(st -> {
            List<LockerDeliveryTask> ldts = userShiftRepository.findAll()
                    .get(0)
                    .streamLockerDeliveryTasks().collect(Collectors.toList());

            assertThat(ldts).hasSize(2);

            List<LockerSubtask> dropoffSubTasks =
                    ldts.get(0).streamDropOffReturnSubtasks().collect(Collectors.toList());
            assertThat(dropoffSubTasks).hasSize(1);
            assertThat(dropoffSubTasks.get(0).getLockerSubtaskDropOff().getMovementId()).isEqualTo(dropoffMovement.getId());

            List<LockerSubtask> orderSubTasks = ldts.get(0).streamLockerDeliverySubtasks()
                    .filter(ldt -> ldt.getType() == LockerSubtaskType.DELIVERY)
                    .collect(Collectors.toList());
            assertThat(orderSubTasks).hasSize(1);
            assertThat(orderSubTasks.get(0).getLockerSubtaskDropOff()).isNull();
            assertThat(orderSubTasks.get(0).getOrderId()).isEqualTo(orderInDropoffPvz.getId());

            return 1;
        });
    }

    @Test
    void shouldValidateOrdersList() {
        configurationServiceAdapter.insertValue(PUBLISH_WAVE_VALIDATION_ENABLED, true);
        RawUserShift rawUserShift = new RawUserShift();
        rawUserShift.setOrders("1,2,3,4");
        publishUserShiftManager.validateOrderList(rawUserShift, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));

        rawUserShift.setOrders(null);
        publishUserShiftManager.validateOrderList(rawUserShift, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));

        rawUserShift.setOrders("");
        publishUserShiftManager.validateOrderList(rawUserShift, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));

        rawUserShift.setOrders("        ");
        publishUserShiftManager.validateOrderList(rawUserShift, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));
    }

    @Test
    void shouldThrowExceptionWhenValidateException() {
        configurationServiceAdapter.insertValue(PUBLISH_WAVE_VALIDATION_ENABLED, true);
        RawUserShift rawUserShift = new RawUserShift();
        rawUserShift.setOrders("1,2,3,4");

        Exception exception = assertThrows(RuntimeException.class, () ->
                publishUserShiftManager.validateOrderList(rawUserShift, List.of(1L, 2L, 3L, 5L, 6L, 7L, 8L))
        );

        String expectedMessage = "Can't find orders from first wave in second wave";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage.contains(expectedMessage)).isTrue();
    }

    private void initRawUserShifts(long shiftId) {
        user1 = userHelper.findOrCreateUser(24126L, LocalDate.now(clock));
        user2 = userHelper.findOrCreateUser(124126L, LocalDate.now(clock));
        user3 = userHelper.findOrCreateUser(224126L, LocalDate.now(clock));
        user4 = userHelper.findOrCreateUser(324126L, LocalDate.now(clock));
        user5 = userHelper.findOrCreateUser(44126L, LocalDate.now(clock));
        user6 = userHelper.findOrCreateUser(4126L, LocalDate.now(clock));
        user7 = userHelper.findOrCreateUser(41262345L, LocalDate.now(clock));
        user8 = userHelper.findOrCreateUser(4123344426L, LocalDate.now(clock));
        user9 = userHelper.findOrCreateUser(100500L, LocalDate.now(clock));
        user10 = userHelper.findOrCreateUser(100510L, LocalDate.now(clock));
        List<RawUserShift> rawUserShifts = List.of(
                new RawUserShift(1L, shiftId, user1.getId(), "", CREATED, null, GROUP, "", 0L, GROUP_PROCESSING_ID),
                new RawUserShift(2L, shiftId, user2.getId(), "", ERROR, null, GROUP, "", 0L, GROUP_PROCESSING_ID),
                new RawUserShift(3L, shiftId, user3.getId(), "", IN_PROGRESS, null, GROUP, "", 0L,
                        GROUP_PROCESSING_ID),
                new RawUserShift(4L, shiftId, user4.getId(), "", CREATED, null, PARTIAL, "1,2,3,4", 0L,
                        PARTIAL_PROCESSING_ID),
                new RawUserShift(5L, shiftId, user5.getId(), "", CREATED, null, GROUP, "", 0L, GROUP_PROCESSING_ID),
                new RawUserShift(6L, shiftId, user6.getId(), "", CREATED, null, PARTIAL, "5,6,7", null,
                        PARTIAL_PROCESSING_ID),
                new RawUserShift(7L, shiftId, user7.getId(), "", CREATED, null, PARTIAL, "", null,
                        PARTIAL_PROCESSING_ID),
                new RawUserShift(8L, shiftId, user8.getId(), "", CREATED, null, PARTIAL, null, null,
                        PARTIAL_PROCESSING_ID),
                new RawUserShift(9L, shiftId, user9.getId(), "", CREATED, null, PARTIAL, "8,9", null,
                        "not-default-processing-id"),
                new RawUserShift(10L, shiftId, user10.getId(), "", CREATED, null, PARTIAL, "10", null, null)
        );
        rawUserShiftRepository.saveAll(rawUserShifts);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnCompletedInPartialRouting(boolean logisticRequestsEnabled) {
        transactionTemplate.execute((a) -> {
            init(logisticRequestsEnabled);
            initRawUserShifts(shift.getId());
            assertThat(merger.isCompleted(shift.getId(), PARTIAL, PARTIAL_PROCESSING_ID)).isTrue();
            return 1;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnNotCompletedInGroupRouting(boolean logisticRequestsEnabled) {
        transactionTemplate.execute((a) -> {
            init(logisticRequestsEnabled);
            initRawUserShifts(shift.getId());
            assertThat(merger.isCompleted(shift.getId(), GROUP, GROUP_PROCESSING_ID)).isFalse();
            return 1;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnOrdersInPartialRouting(boolean logisticRequestsEnabled) {
        transactionTemplate.execute((a) -> {
            init(logisticRequestsEnabled);
            initRawUserShifts(shift.getId());
            var createdPartial = rawUserShiftRepository.findCreatedUserShifts(
                    shift.getId(), PARTIAL_PROCESSING_ID, PARTIAL.name());
            var ordersByUsers = merger.getOrderIdsByUserId(createdPartial);
            assertThat(ordersByUsers.get(user4.getId())).asList().containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
            assertThat(ordersByUsers.get(user6.getId())).asList().containsExactlyInAnyOrder(5L, 6L, 7L);
            assertThat(ordersByUsers.get(user10.getId())).asList().containsExactlyInAnyOrder(10L);
            List.of(user1, user2, user3, user5, user7, user8, user9).forEach(user ->
                    assertThat(ordersByUsers.containsKey(user.getId())).isFalse()
            );
            return 1;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createShiftIfNotFoundSchedule(boolean logisticRequestsEnabled) {
        transactionTemplate.execute((a) -> {
            init(logisticRequestsEnabled);
            User userWithoutSchedule = userHelper.findOrCreateUserWithoutSchedule(7564866L);
            UserScheduleData defaultSchedule = publishUserShiftManager
                    .mapUserToDefaultUserScheduleRule(shift, userWithoutSchedule).getScheduleData();

            RoutingCourier courier = RoutingCourier.builder()
                    .id(userWithoutSchedule.getId())
                    .ref("ref")
                    .depotId(SortingCenter.DEFAULT_SC_ID)
                    .additionalTags(Set.of(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode()))
                    .excludedTags(Set.of())
                    .taskIdsWithFixedOrder(List.of())
                    .plannedTaskIds(List.of())
                    .build();

            CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                    .data(CreateShiftRoutingRequestCommandData.builder()
                            .routeDate(shift.getShiftDate())
                            .sortingCenter(shift.getSortingCenter())
                            .couriers(Set.of(courier))
                            .orders(List.of(order1))
                            .movements(List.of(dropoffMovement))
                            .build()
                    )
                    .createdAt(clock.instant())
                    .mockType(RoutingMockType.REAL)
                    .build();
            routingRequest = routingRequestCreator.createShiftRoutingRequest(command);
            RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);

            shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

            publishUserShiftManager.publishUserShift(shift.getId(), userWithoutSchedule.getId());


            Optional<UserShift> userShift =
                    userShiftRepository.findByShiftIdAndUserId(shift.getId(), userWithoutSchedule.getId());
            assertThat(userShift.isPresent()).isTrue();
            assertThat(userShift.get().getScheduleData())
                    .isEqualTo(defaultSchedule);

            return 1;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createShiftWithClientReturn_newVersion(boolean logisticRequestsEnabled) {
        var routingResult = transactionTemplate.execute(ts -> {
            enableNewMultiApi(true);
            init(logisticRequestsEnabled);
            return routingApiDataHelper.mockResult(routingRequest, false);
        });
        //when
        Optional<UserShift> userShiftO = transactionTemplate.execute((a) -> {


            shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));
            publishUserShiftManager.publishUserShift(shift.getId(), user.getId());
            return userShiftRepository.findByShiftIdAndUserId(shift.getId(), user.getId());
        });

        //then
        Assertions.assertThat(userShiftO.isPresent()).isTrue();
        UserShift userShift = userShiftO.get();

        OrderDeliveryTask clientReturnTask = userShift.streamOrderDeliveryTasks()
                .filter(OrderDeliveryTask::isClientReturn)
                .findFirst()
                .orElseThrow();


        ZoneOffset zoneOffset = dsZoneOffsetCachingService.getOffsetForDs(clientReturn.getDeliveryServiceId());
        Assertions.assertThat(clientReturnTask.getClientReturnId()).isEqualTo(clientReturn.getId());
        Assertions.assertThat(clientReturnTask.getExpectedDeliveryTime()).isNotEqualTo(clientReturn.getArriveIntervalTo().toInstant(zoneOffset));
        Assertions.assertThat(clientReturnTask.getExpectedDeliveryTime()).isNotEqualTo(clientReturn.getArriveIntervalFrom().toInstant(zoneOffset));
        Assertions.assertThat(clientReturnTask.getRoutePoint().getExpectedDateTime()).isNotEqualTo(clientReturn.getArriveIntervalTo().toInstant(zoneOffset));
        Assertions.assertThat(clientReturnTask.getRoutePoint().getExpectedDateTime()).isNotEqualTo(clientReturn.getArriveIntervalFrom().toInstant(zoneOffset));
        var rps =
                routingResult.getShiftsByUserId().values().stream()
                        .map(RoutingResultShift::getRoutePoints)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
        assertThat(rps.stream().anyMatch(rp -> rp.getExpectedArrivalTime().equals(clientReturnTask.getRoutePoint().getExpectedDateTime()))).isTrue();
        assertThat(rps.stream().anyMatch(rp -> rp.getExpectedFinishTime().equals(clientReturnTask.getExpectedDeliveryTime()))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForClientReturnOldVersionTest")
    void createShiftWithClientReturn_oldVersion(boolean enabled, boolean logisticRequestEnabled) {
        //when
        transactionTemplate.execute((a) -> {
            enableNewMultiApi(true);
            init(logisticRequestEnabled);
            enableSplitPublish(enabled);
            RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);
            testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);

            shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));
            if (enabled) {
                publishUserShiftManager.publishUserShift(shift.getId(), user.getId());
            }
            Optional<UserShift> userShiftO = userShiftRepository.findByShiftIdAndUserId(shift.getId(), user.getId());

            //then
            Assertions.assertThat(userShiftO.isPresent()).isTrue();
            UserShift userShift = userShiftO.get();

            OrderDeliveryTask clientReturnTask = userShift.streamOrderDeliveryTasks()
                    .filter(OrderDeliveryTask::isClientReturn)
                    .findFirst()
                    .orElseThrow();
            ZoneOffset zoneOffset = dsZoneOffsetCachingService.getOffsetForDs(clientReturn.getDeliveryServiceId());

            Assertions.assertThat(clientReturnTask.getClientReturnId()).isEqualTo(clientReturn.getId());
            Assertions.assertThat(clientReturnTask.getClientReturnId()).isEqualTo(clientReturn.getId());
            Assertions.assertThat(clientReturnTask.getExpectedDeliveryTime()).isNotEqualTo(clientReturn.getArriveIntervalTo().toInstant(zoneOffset));
            Assertions.assertThat(clientReturnTask.getExpectedDeliveryTime()).isNotEqualTo(clientReturn.getArriveIntervalFrom().toInstant(zoneOffset));
            Assertions.assertThat(clientReturnTask.getRoutePoint().getExpectedDateTime()).isNotEqualTo(clientReturn.getArriveIntervalTo().toInstant(zoneOffset));
            Assertions.assertThat(clientReturnTask.getRoutePoint().getExpectedDateTime()).isNotEqualTo(clientReturn.getArriveIntervalFrom().toInstant(zoneOffset));

            var rps =
                    routingResult.getShiftsByUserId().values().stream()
                            .map(RoutingResultShift::getRoutePoints)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());
            assertThat(rps.stream().anyMatch(rp -> rp.getExpectedArrivalTime().equals(clientReturnTask.getRoutePoint().getExpectedDateTime()))).isTrue();
            assertThat(rps.stream().anyMatch(rp -> rp.getExpectedFinishTime().equals(clientReturnTask.getExpectedDeliveryTime()))).isTrue();

            return 1;
        });
    }

    private static Stream<Arguments> getArgumentsForClientReturnOldVersionTest() {
        return Stream.of(
                Arguments.of(false, false),
                Arguments.of(false, true),
                Arguments.of(true, false),
                Arguments.of(true, true)
        );
    }

    private void enableNewMultiApi(boolean enableNewVerMultiApi) {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.getById(SortingCenter.DEFAULT_SC_ID),
                CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED, enableNewVerMultiApi
        );
    }

    private void enableSplitPublish(boolean enabled) {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.getById(SortingCenter.DEFAULT_SC_ID),
                SortingCenterProperties.PUBLISH_ROUTING_BY_USER_SHIFTS_ENABLED, enabled
        );
    }

}
