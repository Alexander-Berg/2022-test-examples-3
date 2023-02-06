package ru.yandex.market.tpl.core.domain.routing.publish;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.movement.MovementTestBuilder;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddressTestBuilder;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseTestBuilder;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.RoutingRequestCreator;
import ru.yandex.market.tpl.core.domain.routing.events.ShiftRoutingResultReceivedEvent;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTagRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.publish.PublishUserShiftManager;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;
import ru.yandex.market.tpl.core.service.user.transport.TransportTypeRepository;
import ru.yandex.market.tpl.core.service.user.transport.TransportTypeTestBuilder;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestTplRoutingFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class PublishRoutingResultForDropoffReturnTest extends TplAbstractTest {

    public static final String DROPOFF_CARGO_RETURN_TAG = "dropoff_cargo_return";

    private final ShiftManager shiftManager;
    private final PublishUserShiftManager publishUserShiftManager;
    private final RoutingRequestCreator routingRequestCreator;
    private final TestUserHelper userHelper;
    private final MovementRepository movementRepository;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftRepository userShiftRepository;
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final TransportTypeRepository transportTypeRepository;
    private final RoutingOrderTagRepository routingOrderTagRepository;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final PickupPointRepository pickupPointRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final RoutingLogDao routingLogDao;
    private final TestTplRoutingFactory testTplRoutingFactory;

    private Shift shift;
    private User user;

    @BeforeEach
    void init() {
        LocalDate shiftDate = LocalDate.now(clock);
        long sortingCenterId = 47819L;

        shift = userHelper.findOrCreateOpenShiftForSc(shiftDate, sortingCenterId);
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        RoutingOrderTag dropoffReturnTag = routingOrderTagRepository.findByName(DROPOFF_CARGO_RETURN_TAG).get();
        TransportType transportType = transportTypeRepository.saveAndFlush(
                TransportTypeTestBuilder.builder()
                        .routingOrderTags(Set.of(dropoffReturnTag))
                        .build().get()
        );
        clearAfterTest(transportType);
        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);
    }

    @AfterEach
    void tearDown() {
        sortingCenterPropertyService.deletePropertyFromSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED
        );
    }

    @Test
    void createsLockerDeliveryTasks() {
        var warehouseFrom = buildWareHouseFrom("1", 1);
        var warehouseTo1 = buildWareHouseFrom("2", 2);
        var warehouseTo2 = buildWareHouseFrom("3", 3);

        var pickupPoint1 = createPickUpPoint(warehouseTo1.getYandexId());
        var pickupPoint2 = createPickUpPoint(warehouseTo2.getYandexId());

        var movement1 = movementRepository.save(
                MovementTestBuilder.builder()
                        .id(1L)
                        .externalId("1")
                        .warehouse(warehouseFrom)
                        .warehouseTo(warehouseTo1)
                        .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                        .build().get()
        );
        var movement2 = movementRepository.save(
                MovementTestBuilder.builder()
                        .id(2L)
                        .externalId("2")
                        .warehouse(warehouseFrom)
                        .warehouseTo(warehouseTo2)
                        .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                        .build().get()
        );

        RoutingRequest routingRequest = buildRequest(List.of(movement1, movement2));

        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);
        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);

        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

        transactionTemplate.execute(status -> {
            List<UserShift> userShifts = userShiftRepository.findAll();
            assertThat(userShifts.size()).isEqualTo(1);

            var userShift = userShifts.get(0);

            assertThat(userShift.streamRoutePoints().count()).isEqualTo(4);
            assertThat(userShift.streamPickupTasks().count()).isEqualTo(1);
            assertThat(userShift.streamLockerDeliveryTasks().count()).isEqualTo(2);
            assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);

            assertLockerDeliveryTask(userShift, warehouseTo1, pickupPoint1, movement1);
            assertLockerDeliveryTask(userShift, warehouseTo2, pickupPoint2, movement2);

            return null;
        });
    }

    @Test
    void createsLockerDeliveryTasks_mixedWithDropshipsAndDropshipAfterReturn() {

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                true
        );

        var warehouseFrom = buildWareHouseFrom("1", 1);
        var warehouseTo1 = buildWareHouseFrom("2", 2);
        var warehouseTo2 = buildWareHouseFrom("3", 3);

        createPickUpPoint(warehouseTo1.getYandexId());
        createPickUpPoint(warehouseTo2.getYandexId());

        var movement1 = buildMovement(warehouseFrom, warehouseTo1, List.of(Movement.TAG_DROPOFF_CARGO_RETURN), 1L);
        var movement2 = buildMovement(warehouseFrom, warehouseTo2, List.of(Movement.TAG_DROPOFF_CARGO_RETURN), 2L);
        var movement3 = buildMovement(warehouseTo2, warehouseFrom, List.of(), 3L);

        RoutingRequest routingRequest = buildRequest(List.of(movement1, movement2, movement3));

        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);

        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);
        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

        transactionTemplate.execute(status -> {
            List<UserShift> userShifts = userShiftRepository.findAll();
            assertThat(userShifts.size()).isEqualTo(1);

            var userShift = userShifts.get(0);

            assertThat(userShift.streamRoutePoints().count()).isEqualTo(5);
            assertThat(userShift.streamLockerDeliveryTasks().count()).isEqualTo(3);
            assertThat(userShift.streamLockerDeliveryTasks().anyMatch(tld -> tld.getSubtasks().size() == 2)).isFalse();

            return null;
        });
    }


    @Test
    void createsLockerDeliveryTasks_mixedWithDropshipsAndDropshipBeforeReturn() {

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                true
        );

        var warehouseFrom = buildWareHouseFrom("1", 1);
        var warehouseTo1 = buildWareHouseFrom("2", 2);
        var warehouseTo2 = buildWareHouseFrom("3", 3);

        createPickUpPoint(warehouseTo1.getYandexId());
        createPickUpPoint(warehouseTo2.getYandexId());

        var movement1 = buildMovement(warehouseFrom, warehouseTo1, List.of(Movement.TAG_DROPOFF_CARGO_RETURN), 1L);
        var movement2 = buildMovement(warehouseFrom, warehouseTo2, List.of(Movement.TAG_DROPOFF_CARGO_RETURN), 2L);
        var movement3 = buildMovement(warehouseTo2, warehouseFrom, List.of(), 3L);

        RoutingRequest routingRequest = buildRequest(List.of(movement1, movement3, movement2));


        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);
        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);

        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

        transactionTemplate.execute(status -> {
            List<UserShift> userShifts = userShiftRepository.findAll();
            assertThat(userShifts.size()).isEqualTo(1);

            var userShift = userShifts.get(0);

            assertThat(userShift.streamLockerDeliveryTasks().count()).isEqualTo(2);

            LockerDeliveryTask mixedLockerDeliveryTask =
                    userShift.streamLockerDeliveryTasks().filter(tld -> tld.getSubtasks().size() == 2).findFirst().orElseThrow();

            assertThat(mixedLockerDeliveryTask.getSubtasks().stream().anyMatch(lst -> lst.getLockerSubtaskDropOff()
                    .getMovementId().equals(movement2.getId()))).isTrue();

            assertThat(mixedLockerDeliveryTask.getSubtasks().stream().anyMatch(lst -> lst.getLockerSubtaskDropOff()
                    .getMovementId().equals(movement3.getId()))).isTrue();


            return null;
        });
    }

    @NotNull
    private Movement buildMovement(OrderWarehouse warehouseFrom, OrderWarehouse warehouseTo, List<String> tags,
                                   Long id) {
        return movementRepository.save(
                MovementTestBuilder.builder()
                        .id(id)
                        .externalId(String.valueOf(id))
                        .warehouse(warehouseFrom)
                        .warehouseTo(warehouseTo)
                        .tags(tags)
                        .build().get()
        );
    }

    @Nullable
    private RoutingRequest buildRequest(List<Movement> movements) {
        return transactionTemplate.execute(tt -> {
            List<UserScheduleRule> userScheduleRules = List.of(
                    publishUserShiftManager.mapUserToDefaultUserScheduleRule(shift, user)
            );

            Map<Long, RoutingCourier> couriersById =
                    createShiftRoutingRequestCommandFactory.mapCouriersFromUserSchedules(
                            userScheduleRules,
                            false,
                            Map.of(),
                            Map.of()
                    );

            CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                    .data(CreateShiftRoutingRequestCommandData.builder()
                            .routeDate(shift.getShiftDate())
                            .sortingCenter(shift.getSortingCenter())
                            .couriers(new HashSet<>(couriersById.values()))
                            .orders(List.of())
                            .movements(movements)
                            .build()
                    )
                    .createdAt(clock.instant())
                    .mockType(RoutingMockType.REAL)
                    .profileType(RoutingProfileType.PARTIAL)
                    .build();
            return routingRequestCreator.createShiftRoutingRequest(command);
        });
    }

    @NotNull
    private OrderWarehouse buildWareHouseFrom(String yandexId, int val) {
        return orderWarehouseRepository.save(
                OrderWarehouseTestBuilder.builder()
                        .yandexId(yandexId)
                        .address(
                                OrderWarehouseAddressTestBuilder.builder()
                                        .longitude(BigDecimal.valueOf(val))
                                        .latitude(BigDecimal.valueOf(val))
                                        .build().get()
                        )
                        .build().get()
        );
    }

    private void assertLockerDeliveryTask(
            UserShift userShift,
            OrderWarehouse warehouse,
            PickupPoint pickupPoint,
            Movement movement1
    ) {
        LockerDeliveryTask lockerDeliveryTask1 = userShift.streamRoutePoints()
                .filter(it -> it.getType() == RoutePointType.LOCKER_DELIVERY)
                .filter(it ->
                        warehouse.getAddress().getLongitude().equals(
                                it.getRoutePointAddress().getLongitude().stripTrailingZeros()
                        )
                                && warehouse.getAddress().getLatitude().equals(
                                it.getRoutePointAddress().getLatitude().stripTrailingZeros()
                        )
                )
                .findFirst().get()
                .streamLockerDeliveryTasks().findFirst().get();
        assertThat(lockerDeliveryTask1.getPickupPointId()).isEqualTo(pickupPoint.getId());

        LockerSubtask lockerSubtask = lockerDeliveryTask1.getSubtasks().get(0);
        assertThat(lockerSubtask.getLockerSubtaskDropOff().getMovementId()).isEqualTo(movement1.getId());
        assertThat(lockerSubtask.getLockerSubtaskDropOff().getDropoffCargoId()).isNull();
    }

    private PickupPoint createPickUpPoint(String loginsticPointId) {
        PickupPoint pickupPoint = new PickupPoint();
        pickupPoint.setCode(loginsticPointId);
        pickupPoint.setDeliveryServiceId(1L);
        pickupPoint.setName("test");
        pickupPoint.setPartnerSubType(PartnerSubType.PVZ);
        pickupPoint.setType(PickupPointType.PVZ);
        pickupPoint.setLogisticPointId(Long.parseLong(loginsticPointId));
        return pickupPointRepository.saveAndFlush(pickupPoint);
    }
}
