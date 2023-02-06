package ru.yandex.market.tpl.core.domain.routing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.logistic_request.Routable;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTagRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestCollector;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCommonRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.external.routing.api.AdditionalTag;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.CreateUserShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.MultiClientReturn;
import ru.yandex.market.tpl.core.external.routing.api.MultiOrder;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourierVehicleType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingScheduleData;
import ru.yandex.market.tpl.core.service.order.collector.OrderRoutableCollector;
import ru.yandex.market.tpl.core.service.pickup_point_survey.PickupPointSurveyGeneratorService;
import ru.yandex.market.tpl.core.service.routing.MultiClientReturnPackagerService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;
import ru.yandex.market.tpl.core.service.user.transport.TransportTypeRepository;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestClientReturnFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.REROUTE_FIXED_ITEMS_ADD_MOVEMENTS;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.REROUTE_FIX_ORDER_IN_MINUTES;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SURVEY_TASK_ADDITIONAL_TIME_ON_RP_SECONDS;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.YANDEX_GO_COMPANIES_IDS;
import static ru.yandex.market.tpl.core.domain.order.TplOrderGenerateConstants.DEFAULT_DS_ID;
import static ru.yandex.market.tpl.core.domain.routing.MultiOrderMapper.GEO_POINT_SCALE;
import static ru.yandex.market.tpl.core.domain.routing.movement.MovementsRequestItemsCollector.DROPOFF_RETURN_REF_PREFIX;
import static ru.yandex.market.tpl.core.domain.routing.movement.MovementsRequestItemsCollector.DROPSHIPS_REF_PREFIX;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ROUTABLE_ROUTING_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ROUTING_ORDERS_AS_ROUTABLE_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.SPECIAL_REQUEST_ADD_TO_MVRP_REQUEST_ENABLED;
import static ru.yandex.market.tpl.core.test.TestDataFactory.PICKUP_POINT_CODE_TEST;

@RequiredArgsConstructor
public class RoutingRequestCreatorTest extends TplAbstractTest {

    public static final String DROPOFF_CARGO_RETURN_TAG = "dropoff_cargo_return";
    public static final String LOGISTICPOINT_ID_FOR_RETURN_DROPOFF = "1234567";
    private static final long DEFAULT_SURVEY_TASK_ADDITIONAL_TIME_ON_RP_SECONDS = 300;

    private final TestUserHelper userHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final SpecialRequestGenerateService specialRequestGenerateService;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;
    private final UserScheduleRuleRepository scheduleRuleRepository;

    private final RoutingRequestCreator routingRequestCreator;
    private final UserShiftRoutingRequestCreator userShiftRoutingRequestCreator;
    private final SpecialRequestCollector specialRequestCollector;
    private final OrderRoutableCollector orderRoutableCollector;

    private final UserRepository userRepository;
    private final TransportTypeRepository transportTypeRepository;
    private final OrderRepository orderRepository;
    private final MultiOrderMapper multiOrderMapper;
    private final RoutingOrderTagRepository routingOrderTagRepository;
    private final PickupPointSurveyGeneratorService pickupPointSurveyGeneratorService;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final TestDataFactory testDataFactory;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final PartnerRepository<DeliveryService> deliveryServicweRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final MultiClientReturnPackagerService multiClientReturnPackagerService;

    private final TransactionTemplate transactionTemplate;

    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftTestHelper userShiftTestHelper;
    private final MovementGenerator movementGenerator;
    private final ConfigurationProviderAdapter configurationProvider;

    private final PickupPointRepository pickupPointRepository;
    private final TestClientReturnFactory testClientReturnFactory;

    private final JdbcTemplate jdbcTemplate;

    private PickupPoint pickupPoint;
    private User user;
    private Shift shift;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        pickupPoint = pickupPointRepository.findByCode(PICKUP_POINT_CODE_TEST)
                .orElseGet(() -> pickupPointRepository.save(
                        testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                                Long.valueOf(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF), 1L)));
        sortingCenterPropertyService.deletePropertyFromSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.IS_DROP_FAR_ORDERS_FROM_ROUTING_ENABLED);
        sortingCenterPropertyService.deletePropertyFromSortingCenter(
                shift.getSortingCenter(), CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED);
        enableOrderRoutable(false);
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    public void shouldFixOrder(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableRerouteFixedItemsWithMovements(false);
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();

        var pickupTask = helper.taskOrderPickup(DateTimeUtil.todayAtHour(9, clock));
        var firstDeliveryTask = helper.taskUnpaid("addr1", 10, firstOrder.getId());
        var secondDeliveryTask = helper.taskUnpaid("addr2", 10, secondOrder.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(firstDeliveryTask)
                .routePoint(secondDeliveryTask)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(userShiftId).orElseThrow();

        List<String> taskIdsWithFixedOrder;
        Map<Long, String> multiOrderIdByOrderId = getMultiOrderIdByOrderId(userShift);

        taskIdsWithFixedOrder = transactionTemplate.execute(st ->
                userShiftRoutingRequestCreator.getTaskIdsWithFixedOrder(
                        repository.findById(userShiftId).orElseThrow().streamOrderDeliveryTasks()
                                .flatMap(DeliveryTask::streamDeliveryOrderSubtasks)
                                .toList(),
                        List.of(),
                        userShift,
                        multiOrderIdByOrderId,
                        Map.of(),
                        false
                )
        );

        assertThat(taskIdsWithFixedOrder)
                .containsExactly(String.valueOf(firstOrder.getId()), String.valueOf(secondOrder.getId()));
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    public void shouldFixOrderAndClientReturn(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableRerouteFixedItemsWithMovements(false);
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        ClientReturn firstClientReturn = clientReturnGenerator.generateReturnFromClient();
        ClientReturn secondClientReturn = clientReturnGenerator.generateReturnFromClient();
        var movementDropoffReturn = testDataFactory.buildDropOffReturnMovement(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF);
        var movementDropship = movementGenerator.generate(MovementCommand.Create.builder().build());

        var pickupTask = helper.taskOrderPickup(DateTimeUtil.todayAtHour(9, clock));
        var firstDeliveryTask = helper.taskUnpaid("addr1", 13, firstOrder.getId());
        var firstClientReturnTask = helper.clientReturn("addr2", 11, firstClientReturn.getId());
        var secondDeliveryTask = helper.taskUnpaid("addr2", 11, secondOrder.getId());
        var secondClientReturnTask = helper.clientReturn("addr2", 11, secondClientReturn.getId());
        var dropoffReturn = helper.taskDropOffReturn(movementDropoffReturn.getId());
        var collectDropship = helper.taskCollectDropship(LocalDate.now(), movementDropship);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(firstDeliveryTask)
                .routePoint(firstClientReturnTask)
                .routePoint(dropoffReturn)
                .routePoint(secondDeliveryTask)
                .routePoint(secondClientReturnTask)
                .routePoint(collectDropship)
                .build();

        long userShiftId =  commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(userShiftId).orElseThrow();

        Map<Long, String> multiOrderIdByOrderId = getMultiOrderIdByOrderId(userShift);

        Map<Long, ClientReturn> clientReturnsById = Map.of(firstClientReturn.getId(), firstClientReturn,
                secondClientReturn.getId(), secondClientReturn);
        List<MultiClientReturn> multiClientReturns = multiClientReturnPackagerService.pack(clientReturnsById.values());
        Map<Long, String> multiClientReturnsByClientReturnId = getMultiClientReturnsByClientReturnId(multiClientReturns);

        List<String> taskIdsWithFixedOrder = transactionTemplate.execute(st ->
                userShiftRoutingRequestCreator.getTaskIdsWithFixedOrder(
                        repository.findById(userShiftId).orElseThrow().streamOrderDeliveryTasks()
                                .flatMap(DeliveryTask::streamDeliveryOrderSubtasks)
                                .toList(),
                        List.of(),
                        userShift,
                        multiOrderIdByOrderId,
                        multiClientReturnsByClientReturnId,
                        false
                )
        );

        assertThat(taskIdsWithFixedOrder)
                .containsExactly(
                        String.valueOf(secondOrder.getId()),
                        String.valueOf(multiClientReturns.get(0).getRoutingTaskId()),
                        String.valueOf(firstOrder.getId()));
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    public void shouldFixOrderAndClientReturnWithMovements(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableRerouteFixedItemsWithMovements(true);
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        ClientReturn firstClientReturn = clientReturnGenerator.generateReturnFromClient();
        ClientReturn secondClientReturn = clientReturnGenerator.generateReturnFromClient();

        jdbcTemplate.execute("ALTER SEQUENCE seq_movement RESTART WITH 10000;");
        var movementDropoffReturn = testDataFactory.buildDropOffReturnMovement(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF);
        var movementDropship = movementGenerator.generate(MovementCommand.Create.builder().build());

        var pickupTask = helper.taskOrderPickup(DateTimeUtil.todayAtHour(9, clock));
        var firstDeliveryTask = helper.taskUnpaid("addr1", 13, firstOrder.getId());
        var firstClientReturnTask = helper.clientReturn("addr2", 11, firstClientReturn.getId());
        var secondDeliveryTask = helper.taskUnpaid("addr2", 11, secondOrder.getId());
        var secondClientReturnTask = helper.clientReturn("addr2", 11, secondClientReturn.getId());
        var dropoffReturn = helper.taskDropOffReturn(movementDropoffReturn.getId(), 12);
        var collectDropship = helper.taskCollectDropship(LocalDate.now(), movementDropship);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(firstDeliveryTask)
                .routePoint(firstClientReturnTask)
                .routePoint(dropoffReturn)
                .routePoint(secondDeliveryTask)
                .routePoint(secondClientReturnTask)
                .routePoint(collectDropship)
                .build();

        long userShiftId =  commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(userShiftId).orElseThrow();

        Map<Long, String> multiOrderIdByOrderId = getMultiOrderIdByOrderId(userShift);

        Map<Long, ClientReturn> clientReturnsById = Map.of(firstClientReturn.getId(), firstClientReturn,
                secondClientReturn.getId(), secondClientReturn);
        List<MultiClientReturn> multiClientReturns = multiClientReturnPackagerService.pack(clientReturnsById.values());
        Map<Long, String> multiClientReturnsByClientReturnId = getMultiClientReturnsByClientReturnId(multiClientReturns);

        List<String> taskIdsWithFixedOrder = transactionTemplate.execute(st -> {
            var userShiftPersisted = repository.findById(userShiftId).orElseThrow();
            return userShiftRoutingRequestCreator.getTaskIdsWithFixedOrder(
                    userShiftPersisted.streamOrderDeliveryTasks()
                            .flatMap(DeliveryTask::streamDeliveryOrderSubtasks)
                            .toList(),
                    List.of(
                            new UserShiftRoutingRequestCreator.RoutableItem(
                                    movementDropoffReturn.getId(),
                                    userShiftPersisted.streamLockerDeliveryTasks().findFirst().get().getRoutePoint(),
                                    dropoffReturn.getExpectedDeliveryTime()
                            ),
                            new UserShiftRoutingRequestCreator.RoutableItem(
                                    movementDropship.getId(),
                                    userShiftPersisted.streamCollectDropshipTasks().findFirst().get().getRoutePoint(),
                                    collectDropship.getExpectedArrivalTime()
                            )
                    ),
                    userShiftPersisted,
                    multiOrderIdByOrderId,
                    multiClientReturnsByClientReturnId,
                    false
            );
        });

        assertThat(taskIdsWithFixedOrder)
                .containsExactly(
                        String.valueOf(secondOrder.getId()),
                        String.valueOf(multiClientReturns.get(0).getRoutingTaskId()),
                        String.valueOf(movementDropoffReturn.getId()),
                        String.valueOf(firstOrder.getId()),
                        String.valueOf(movementDropship.getId())
                );
    }

    @NotNull
    private Map<Long, String> getMultiClientReturnsByClientReturnId(List<MultiClientReturn> multiClientReturns) {
        return StreamEx.of(multiClientReturns)
                .mapToEntry(MultiClientReturn::getRoutingTaskId,
                        cr -> cr.getItems().stream().map(ClientReturn::getId).collect(Collectors.toList()))
                .flatMapValues(Collection::stream)
                .invert()
                .toMap();
    }

    @NotNull
    private Map<Long, String> getMultiOrderIdByOrderId(UserShift userShift) {
        Map<Long, Order> ordersById = orderRepository.findMapForUserShift(userShift);
        List<MultiOrder> multiOrders = multiOrderMapper.mapForRoutingRequest(ordersById.values());
        return StreamEx.of(multiOrders)
                .mapToEntry(MultiOrder::getMultiOrderId,
                        o -> o.getOrders().stream().map(Order::getId).collect(Collectors.toList()))
                .flatMapValues(Collection::stream)
                .invert()
                .toMap();
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void createRoutingRequestWithDimensions(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        TransportType transportType = createTransportType();
        clearAfterTest(transportType);

        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);

        List<Order> orders = generateOrders(4);

        RoutingRequest routingRequest;
        if (enableOrderRoutable) {
            routingRequest = prepareRoutingRequestForRoutable(mapOrdersToRoutableItems(orders));
        } else {
            routingRequest = prepareRoutingRequest(orders, List.of());
        }
        assertThat(routingRequest.getItems()).extracting(RoutingRequestItem::getVolume).doesNotContainNull();
        assertThat(routingRequest.getUsers()).hasSize(1);
        assertThat(routingRequest.getUsers()).extracting(RoutingCourier::getVehicleCapacity).doesNotContainNull();
        routingRequest.getItems()
                .forEach(rri -> assertThat(rri.isUserShiftRoutingRequest()).isFalse());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void dropFarOrdersWhenEnabledForSc(boolean enableNewVerMultiApi) {
        //given
        enableNewMultiApi(enableNewVerMultiApi);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.IS_DROP_FAR_ORDERS_FROM_ROUTING_ENABLED, true);
        TransportType transportType = createTransportType();
        clearAfterTest(transportType);
        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);

        List<Order> orders = generateOrders(2);
        orders.forEach(order -> order.setIsAddressValid(false));


        //when
        RoutingRequest routingRequest = prepareRoutingRequest(orders, List.of());

        //then
        assertThat(routingRequest.getItems()).hasSize(0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void creationRequestWithClientReturn_enabled(boolean enableOrderRoutable) {
        //given
        enableNewMultiApi(true);
        enableOrderRoutable(enableOrderRoutable);

        LocalDateTime arriveFrom = LocalDate.now(clock).atStartOfDay();
        LocalDateTime arriveTo = LocalDate.now(clock).atStartOfDay().plusHours(4);
        String phone = "+7999999999";
        BigDecimal lat = BigDecimal.valueOf(55.5);
        BigDecimal lon = BigDecimal.valueOf(33.5);

        var clientReturn = testClientReturnFactory.buildAndSave(DEFAULT_DS_ID, arriveFrom, arriveTo, phone, lat, lon);

        MultiClientReturn multiClientReturn = MultiClientReturn.builder()
                .items(List.of(clientReturn))
                .build();

        //when
        RoutingRequest routingRequest = prepareRoutingRequest(List.of(),
                List.of(), List.of(), List.of(clientReturn), 0);

        //then
        assertThat(routingRequest.getItems()).hasSize(1);
        var routingRequestItem = routingRequest.getItems().get(0);
        assertThat(routingRequestItem.getType()).isEqualTo(RoutingRequestItemType.CLIENT_RETURN);
        assertThat(routingRequestItem.getTaskId()).isEqualTo(multiClientReturn.getRoutingTaskId());
        assertThat(routingRequestItem.getRef()).isEqualTo(multiClientReturn.getRoutingRef());
    }

    @Test
    void creationRequestWithSpecialRequestEnabled() {
        enableOrderRoutable(true);
        Routable routable = createRoutable();
        //when
        RoutingRequest routingRequest = prepareRoutingRequest(List.of(),
                List.of(), List.of(), List.of(), List.of(routable), 0);

        //then
        assertThat(routingRequest.getItems()).hasSize(1);
        var routingRequestItem = routingRequest.getItems().get(0);
        assertThat(routingRequestItem.getType()).isEqualTo(routable.getRoutingRequestItemType());
        assertThat(routingRequestItem.getTaskId()).isEqualTo(String.valueOf(routable.getEntityId()));
        assertThat(routingRequestItem.getRef()).isEqualTo(routable.getRef());
    }

    @Test
    void creationRequestWithOrderAndSpecialRequestSingleLocationTest() {
        enableOrderRoutable(true);

        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var specialRequest = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(pickupPoint.getId())
                        .build());

        var orderRoutable = orderRoutableCollector.map(order, false, false, false, 4);
        var specialRequestRoutable = specialRequestCollector.convertToRoutable(specialRequest);

        var routingRequest = prepareRoutingRequest(List.of(), List.of(), List.of(), List.of(),
                List.of(orderRoutable, specialRequestRoutable), 0);

        // сформирована одна локация
        assertThat(routingRequest.getItems()).hasSize(1);
        var item = routingRequest.getItems().get(0);

        var orderFirst = order.getExternalOrderId().compareTo(specialRequest.getExternalId()) < 0;
        var expectedRef = (orderFirst ? order.getExternalOrderId() : specialRequest.getExternalId()) +
                RoutableGroupPackager.MULTI_ITEM_ID_DELIMITER +
                (orderFirst ? specialRequest.getExternalId() : order.getExternalOrderId());
        var expectedId = RoutableGroupPackager.GROUP_PREFIX +
                specialRequestRoutable.getGroupIdPrefix() +
                RoutableGroupPackager.MULTI_ITEM_ID_DELIMITER +
                specialRequestRoutable.getId() +
                RoutableGroupPackager.MULTI_ITEM_ID_DELIMITER +
                orderRoutable.getGroupIdPrefix() +
                RoutableGroupPackager.MULTI_ITEM_ID_DELIMITER +
                orderRoutable.getId();

        // в одной локации находятся заказ и спецзадание
        assertThat(item.getRef()).isEqualTo(expectedRef);
        assertThat(item.getTaskId()).isEqualTo(expectedId);
        assertThat(item.getSubTaskCount()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void additionalTimeForSurveyWithLockerInventoryTest(boolean enableNewVerMultiApi) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(true);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(), ROUTING_ORDERS_AS_ROUTABLE_ENABLED, true
        );
        var transportType = createTransportType();
        clearAfterTest(transportType);
        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);
        var pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100L, 100L);
        pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);

        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        var specialRequest = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(pickupPoint.getId())
                        .build()
        );
        var routableItems = List.of(
                orderRoutableCollector.map(order, true, true, false, GeoPoint.GEO_POINT_SCALE),
                specialRequestCollector.convertToRoutable(specialRequest)
        );


        var pickupPointIdsWithActiveSurveyTasks =
                createShiftRoutingRequestCommandFactory.getPickupPointIdsWithActiveSurveyTasks();
        long additionalTimeForSurvey = createShiftRoutingRequestCommandFactory.getAdditionalTimeForSurvey();
        var timeForInventory = specialRequestCollector.getAdditionalTimeOnPoint(specialRequest);

        var routingRequest = prepareRoutingRequest(List.of(), List.of(),
                pickupPointIdsWithActiveSurveyTasks, List.of(), routableItems, additionalTimeForSurvey);

        assertThat(routingRequest.getUsers()).hasSize(1);
        assertThat(routingRequest.getItems()).hasSize(1);


        var routingItem = routingRequest.getItems().get(0);
        assertThat(routingItem.getSubTaskCount()).isEqualTo(2);
        assertThat(routingItem.getAdditionalTimeForSurvey()).isEqualTo(additionalTimeForSurvey + timeForInventory);
    }

    @NotNull
    private Routable createRoutable() {
        SpecialRequest specialRequest = specialRequestGenerateService.createSpecialRequest();
        return specialRequestCollector.convertToRoutable(specialRequest);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void creationRequestWithClientReturn_disabled(boolean enableOrderRoutable) {
        //given
        enableNewMultiApi(false);
        enableOrderRoutable(enableOrderRoutable);

        LocalDateTime arriveFrom = LocalDate.now(clock).atStartOfDay();
        LocalDateTime arriveTo = LocalDate.now(clock).atStartOfDay().plusHours(4);
        String phone = "+7999999999";
        BigDecimal lat = BigDecimal.valueOf(55.5);
        BigDecimal lon = BigDecimal.valueOf(33.5);

        var clientReturn = testClientReturnFactory.buildAndSave(DEFAULT_DS_ID, arriveFrom, arriveTo, phone, lat, lon);

        MultiClientReturn multiClientReturn = MultiClientReturn.builder()
                .items(List.of(clientReturn))
                .build();

        //when
        RoutingRequest routingRequest = prepareRoutingRequest(List.of(),
                List.of(), List.of(), List.of(clientReturn), 0);

        //then
        assertThat(routingRequest.getItems()).hasSize(0);
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void dropFarOrdersWhenDisabledForSc(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        //given
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.IS_DROP_FAR_ORDERS_FROM_ROUTING_ENABLED, false);
        TransportType transportType = createTransportType();
        clearAfterTest(transportType);
        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);

        List<Order> orders = generateOrders(1);
        orders.forEach(order -> order.setIsAddressValid(false));


        //when
        RoutingRequest routingRequest = prepareRoutingRequest(orders, List.of());

        //then
        assertThat(routingRequest.getItems()).hasSize(1);
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void dropFarOrdersWhenEnableddForSc_Mixed(boolean enableNewVerMultiApi) {
        //given
        enableNewMultiApi(enableNewVerMultiApi);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.IS_DROP_FAR_ORDERS_FROM_ROUTING_ENABLED, true);
        TransportType transportType = createTransportType();
        clearAfterTest(transportType);
        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);

        List<Order> orders = generateOrders(2);
        orders.get(0).setIsAddressValid(false);
        orders.get(1).setIsAddressValid(true);


        //when
        RoutingRequest routingRequest = prepareRoutingRequest(orders, List.of());

        //then
        assertThat(routingRequest.getItems()).hasSize(1);
        assertThat(routingRequest.getItems().get(0).getRef()).isEqualTo(orders.get(1).getExternalOrderId());
    }

    @NotNull
    private List<Order> generateOrders(int i2) {
        AtomicInteger count = new AtomicInteger(15);
        return Stream.generate(
                        () -> {
                            int i = count.incrementAndGet();
                            return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                                    .dimensions(new Dimensions(BigDecimal.valueOf(2.3), i, i, i))
                                    .build());
                        }).limit(i2)
                .collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void createRoutingRequestWithDimensions_withDropOff(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        //given
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        CreatedFlowResultDto createdFlowResultDto = prepareDropOffOnlyCourierFlow();
        Long userShiftId = createdFlowResultDto.getUserShiftId();

        CreateUserShiftRoutingRequestCommand command = buildUserShiftRerouteRequestCommand(userShiftId);

        //when
        RoutingRequest routingRequest = userShiftRoutingRequestCreator.createUserShiftRoutingRequest(command);

        //then
        assertsReroutingRequest(createdFlowResultDto, routingRequest);
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void createRoutingRequestWithDimensions_withOutDropOff(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        //given
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        CreatedFlowResultDto createdFlowResultDto = prepareWithoutDropOffCourierFlow();
        Long userShiftId = createdFlowResultDto.getUserShiftId();

        CreateUserShiftRoutingRequestCommand command = buildUserShiftRerouteRequestCommand(userShiftId);

        //when
        RoutingRequest routingRequest = userShiftRoutingRequestCreator.createUserShiftRoutingRequest(command);

        //then
        assertsReroutingRequest(createdFlowResultDto, routingRequest);
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void createRoutingRequestWithDimensions_MixedOrdersAndDropOff(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        //given
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        CreatedFlowResultDto createdFlowResultDto = prepareMixedCourierFlow();
        Long userShiftId = createdFlowResultDto.getUserShiftId();

        CreateUserShiftRoutingRequestCommand command = buildUserShiftRerouteRequestCommand(userShiftId);

        //when
        RoutingRequest routingRequest = userShiftRoutingRequestCreator.createUserShiftRoutingRequest(command);

        //then
        assertsReroutingRequest(createdFlowResultDto, routingRequest);
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void createRequestWithClientReturnsAndOrders(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        configurationServiceAdapter.mergeValue(REROUTE_FIX_ORDER_IN_MINUTES, 720L);

        //подготавливаем флоу в котором задаем курьеру даем 2 заказа, которые не мержатся изза разных координат
        //2 возврата, которые мержатся изза одинаковых координат + времени.
        CreatedFlowResultDto createdFlowResultDto = prepareClientReturnAndOrderFlow();
        Long userShiftId = createdFlowResultDto.getUserShiftId();

        CreateUserShiftRoutingRequestCommand command = buildUserShiftRerouteRequestCommand(userShiftId);

        RoutingRequest routingRequest = userShiftRoutingRequestCreator.createUserShiftRoutingRequest(command);

        assertThat(routingRequest.getUsers()).hasSize(1);
        var user = routingRequest.getUsers().stream().findFirst().orElseThrow();

        List<String> taskIds = new ArrayList<>();
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
                    var clientReturnTaskId =
                            StreamEx.of(userShift.streamOrderDeliveryTasks()).filter(OrderDeliveryTask::isClientReturn).map(OrderDeliveryTask::getClientReturnId).joining("_", "cr_", "");
                    var orderTasks =
                            StreamEx.of(userShift.streamOrderDeliveryTasks()).remove(OrderDeliveryTask::isClientReturn).toList();
                    assertThat(orderTasks).hasSize(2);
                    taskIds.add(String.valueOf(orderTasks.get(0).getOrderId()));
                    taskIds.add(String.valueOf(orderTasks.get(1).getOrderId()));
                    if (enableNewVerMultiApi) {
                        taskIds.add(clientReturnTaskId);
                    }
                    if (enableOrderRoutable) {
                        userShift.streamLockerDeliveryTasks()
                                .flatMap(LockerDeliveryTask::streamSubtask)
                                .filter(LockerSubtask::isCargoDeliverySubtask)
                                .map(st -> st.getLockerSubtaskDropOff().getMovementId())
                                .append(userShift.streamCollectDropshipTasks()
                                .map(CollectDropshipTask::getMovementId)
                                ).distinct()
                                .forEach(id -> taskIds.add(String.valueOf(id)));

                    }
                }
        );

        assertThat(user.getTaskIdsWithFixedOrder()).containsExactlyInAnyOrderElementsOf(taskIds);
        //по логике в хелперах для тестов заказы будут помещены в рутпоинты в первую очередь, потому если заказ и
        //возврат есть на одной точке, то первым будет идти заказ
        //тут нам важно, чтобы совпадали первые заказы, тк у них отличное от остальных время
        assertThat(user.getTaskIdsWithFixedOrder().get(0)).isEqualTo(taskIds.get(0));
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void lockerDeliveryTasksOnly(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        configurationServiceAdapter.mergeValue(REROUTE_FIX_ORDER_IN_MINUTES, 720L);

        //подготавливаем флоу в котором задаем курьеру даем 2 заказа, которые не мержатся изза разных координат
        //2 возврата, которые мержатся изза одинаковых координат + времени.
        CreatedFlowResultDto createdFlowResultDto = prepareLockerDeliveryFlow();
        Long userShiftId = createdFlowResultDto.getUserShiftId();

        CreateUserShiftRoutingRequestCommand command = buildUserShiftRerouteRequestCommand(userShiftId);

        RoutingRequest routingRequest = userShiftRoutingRequestCreator.createUserShiftRoutingRequest(command);

        assertThat(routingRequest.getUsers()).hasSize(1);

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
                    var lockerDeliveryTasks =
                            StreamEx.of(userShift.streamLockerDeliveryTasks()).toList();

                    assertThat(lockerDeliveryTasks).hasSize(2);
                }
        );
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void shouldAddMovementRegionIdTag(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        DeliveryService deliveryService = deliveryServicweRepository.findByIdOrThrow(DeliveryService.FAKE_DS_ID);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                deliveryService.getSortingCenter(),
                SortingCenterProperties.SAME_DAY_DROPOFF_ENABLED,
                Boolean.TRUE
        );

        LocalDate today = LocalDate.now(clock);
        OrderWarehouse warehouse = createWarehouse("123");
        warehouse.setRegionId(100500);
        OrderWarehouseAddress orderWarehouseAddress = warehouse.getAddress();
        orderWarehouseAddress.setLongitude(BigDecimal.ONE);
        orderWarehouseAddress.setLatitude(BigDecimal.ONE);
        Movement movement = getMovement(
                warehouse,
                today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
                today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
                "1",
                MovementStatus.CREATED,
                deliveryService.getId()
        );

        RoutingRequest routingRequest = prepareRoutingRequest(List.of(), List.of(movement));
        assertThat(routingRequest.getItems().stream().anyMatch(item -> item.getRegionId().equals(100500))).isTrue();

        RoutingRequestItem routingRequestItem =
                routingRequest.getItems().stream()
                        .filter(i -> i.getTaskId().equals(String.valueOf(movement.getId())))
                        .findFirst()
                        .orElseThrow();
        assertThat(routingRequestItem.getVolume()).isEqualByComparingTo(BigDecimal.ONE);
    }

    private void enableNewMultiApi(boolean enableNewVerMultiApi) {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(), CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED, enableNewVerMultiApi
        );
    }

    private void enableOrderRoutable(boolean enableOrderRoutable) {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(), ROUTING_ORDERS_AS_ROUTABLE_ENABLED, enableOrderRoutable
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(), ROUTABLE_ROUTING_ENABLED, enableOrderRoutable
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(), SPECIAL_REQUEST_ADD_TO_MVRP_REQUEST_ENABLED, enableOrderRoutable
        );
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void shouldAddUserTransportTypeTags_whenCreateRequest(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        RoutingOrderTag dropoffReturnTag = routingOrderTagRepository.findByName(DROPOFF_CARGO_RETURN_TAG).get();
        TransportType transportType = createTransportType(Set.of(dropoffReturnTag));
        clearAfterTest(transportType);
        //delete entity
        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);

        RoutingRequest routingRequest = prepareRoutingRequest(false);

        assertUserTags(
                routingRequest.getUsers().stream().findFirst().get(),
                List.of(DROPOFF_CARGO_RETURN_TAG),
                List.of(),
                List.of(),
                List.of()
        );
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void setPlannedTaskIdsForMovements(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        DeliveryService deliveryService = deliveryServicweRepository.findByIdOrThrow(DeliveryService.FAKE_DS_ID);
        LocalDate today = LocalDate.now(clock);
        OrderWarehouse warehouse = createWarehouse("123");
        warehouse.setRegionId(100500);
        OrderWarehouseAddress orderWarehouseAddress = warehouse.getAddress();
        orderWarehouseAddress.setLongitude(BigDecimal.ONE);
        orderWarehouseAddress.setLatitude(BigDecimal.ONE);

        String movementDroppoffReturnId = "1";
        Movement movementDroppoffReturn = getMovement(
                warehouse,
                today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
                today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
                movementDroppoffReturnId,
                MovementStatus.CREATED,
                deliveryService.getId()
        );
        movementDroppoffReturn.setId(100501L);
        movementDroppoffReturn.setWarehouseTo(
                createWarehouse("123-to", BigDecimal.valueOf(55), BigDecimal.valueOf(55))
        );
        movementDroppoffReturn.setTags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN));

        String movementDropshipId = "2";
        Movement movementDropship = getMovement(
                warehouse,
                today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
                today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
                movementDropshipId,
                MovementStatus.CREATED,
                deliveryService.getId()
        );
        movementDropship.setId(100502L);


        RoutingCourier routingCourier = RoutingCourier.builder()
                .id(user.getId())
                .ref(user.getFullName() + "-" + user.getUid())
                .depotId(shift.getSortingCenter().getId())
                .scheduleData(new RoutingScheduleData(
                        RoutingCourierVehicleType.CAR,
                        RelativeTimeInterval.valueOf("09:00-18:00")
                ))
                .routingTimeMultiplier(RoutingCourierMapper.DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .vehicleCapacity(BigDecimal.ONE)
                .servicedLocationType(RoutingLocationType.delivery)
                .priority(1)
                .vehicleId(123L)
                .partnerId(123L)
                .build();



        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(Set.of(routingCourier))
                        .orders(List.of())
                        .clientReturns(List.of())
                        .movements(List.of(movementDroppoffReturn, movementDropship))
                        .baseRoutingRequestUserIdToSubjectIds(
                                Map.of(
                                        routingCourier.getId(),
                                        List.of(
                                                List.of(movementDroppoffReturn.getId()),
                                                List.of(movementDropship.getId())
                                        )
                                )
                        )
                        .build())
                .mockType(RoutingMockType.MANUAL)
                .build();
        RoutingRequest routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

        RoutingCourier courier = routingRequest.getUsers().iterator().next();
        assertThat(courier.getPlannedTaskIds())
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                String.valueOf(movementDroppoffReturn.getId()),
                                String.valueOf(movementDropship.getId())
                        )
                );
    }

    @Test
    @DisplayName("Убираем заказ из планируемых заданий и итемов если у него нулевые координаты")
    void removeZeroCoordinatesOrderFromPlannedTasks() {
        enableNewMultiApi(true);

        GeoPoint geoPoint1 = GeoPoint.ofLatLon(0, 0);
        GeoPoint geoPoint2 = GeoPointGenerator.generateLonLat();

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint1)
                        .build())
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint2)
                        .build())
                .build());

        var orders = List.of(order1, order2);

        RoutingCourier routingCourier = RoutingCourier.builder()
                .id(user.getId())
                .ref(user.getFullName() + "-" + user.getUid())
                .depotId(shift.getSortingCenter().getId())
                .scheduleData(new RoutingScheduleData(
                        RoutingCourierVehicleType.CAR,
                        RelativeTimeInterval.valueOf("09:00-18:00")
                ))
                .routingTimeMultiplier(RoutingCourierMapper.DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .vehicleCapacity(BigDecimal.ONE)
                .servicedLocationType(RoutingLocationType.delivery)
                .priority(1)
                .vehicleId(123L)
                .partnerId(123L)
                .build();


        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(Set.of(routingCourier))
                        .orders(orders)
                        .clientReturns(List.of())
                        .movements(List.of())
                        .baseRoutingRequestUserIdToSubjectIds(
                                Map.of(
                                        routingCourier.getId(),
                                        List.of(
                                                List.of(order1.getId())
                                        )
                                )
                        )
                        .build())
                .mockType(RoutingMockType.MANUAL)
                .build();
        RoutingRequest routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

        RoutingCourier courier = routingRequest.getUsers().iterator().next();
        assertThat(courier.getPlannedTaskIds()).isEmpty();
        assertThat(routingRequest.getItems()).hasSize(1);
        assertThat(routingRequest.getItems().get(0).getSubTaskCount()).isEqualTo(1); // откинули невалидный заказ
    }

    @Test
    @DisplayName("Убираем заказ из планируемых заданий и итемов если у него нулевые координаты, но оставляем валидный заказ")
    void removeZeroCoordinatesOrderFromPlannedTasksAndLeaveGoodOrder() {
        enableNewMultiApi(true);

        GeoPoint geoPoint1 = GeoPoint.ofLatLon(0, 0);
        GeoPoint geoPoint2 = GeoPointGenerator.generateLonLat();

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint1)
                        .build())
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint2)
                        .build())
                .build());

        var orders = List.of(order1, order2);

        RoutingCourier routingCourier = RoutingCourier.builder()
                .id(user.getId())
                .ref(user.getFullName() + "-" + user.getUid())
                .depotId(shift.getSortingCenter().getId())
                .scheduleData(new RoutingScheduleData(
                        RoutingCourierVehicleType.CAR,
                        RelativeTimeInterval.valueOf("09:00-18:00")
                ))
                .routingTimeMultiplier(RoutingCourierMapper.DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .vehicleCapacity(BigDecimal.ONE)
                .servicedLocationType(RoutingLocationType.delivery)
                .priority(1)
                .vehicleId(123L)
                .partnerId(123L)
                .build();


        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(Set.of(routingCourier))
                        .orders(orders)
                        .clientReturns(List.of())
                        .movements(List.of())
                        .baseRoutingRequestUserIdToSubjectIds(
                                Map.of(
                                        routingCourier.getId(),
                                        List.of(
                                                List.of(order1.getId(), order2.getId())
                                        )
                                )
                        )
                        .build())
                .mockType(RoutingMockType.MANUAL)
                .build();
        RoutingRequest routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

        RoutingCourier courier = routingRequest.getUsers().iterator().next();
        assertThat(courier.getPlannedTaskIds()).hasSize(1);
        assertThat(courier.getPlannedTaskIds().get(0)).isEqualTo(order2.getId().toString());
        assertThat(routingRequest.getItems()).hasSize(1);
        assertThat(routingRequest.getItems().get(0).getSubTaskCount()).isEqualTo(1); // откинули невалидный заказ
    }

    @Test
    @DisplayName("Убираем заказ из итемов если у него нулевые координаты, но оставляем валидный заказ")
    void removeZeroCoordinatesOrderFromItems() {
        enableNewMultiApi(true);

        GeoPoint geoPoint1 = GeoPoint.ofLatLon(0, 0);
        GeoPoint geoPoint2 = GeoPointGenerator.generateLonLat();

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint1)
                        .build())
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint2)
                        .build())
                .build());

        var orders = List.of(order1, order2);

        RoutingCourier routingCourier = RoutingCourier.builder()
                .id(user.getId())
                .ref(user.getFullName() + "-" + user.getUid())
                .depotId(shift.getSortingCenter().getId())
                .scheduleData(new RoutingScheduleData(
                        RoutingCourierVehicleType.CAR,
                        RelativeTimeInterval.valueOf("09:00-18:00")
                ))
                .routingTimeMultiplier(RoutingCourierMapper.DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .vehicleCapacity(BigDecimal.ONE)
                .servicedLocationType(RoutingLocationType.delivery)
                .priority(1)
                .vehicleId(123L)
                .partnerId(123L)
                .build();


        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(Set.of(routingCourier))
                        .orders(orders)
                        .clientReturns(List.of())
                        .movements(List.of())
                        .baseRoutingRequestUserIdToSubjectIds(
                                Map.of(
                                        routingCourier.getId(),
                                        List.of(
                                                List.of(order1.getId())
                                        )
                                )
                        )
                        .build())
                .mockType(RoutingMockType.MANUAL)
                .build();
        RoutingRequest routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

        RoutingCourier courier = routingRequest.getUsers().iterator().next();
        assertThat(courier.getPlannedTaskIds()).isEmpty();
        assertThat(routingRequest.getItems()).hasSize(1);
        assertThat(routingRequest.getItems().get(0).getSubTaskCount()).isEqualTo(1); // откинули невалидный заказ
    }

    @Test
    @DisplayName("оставляем оба валидных заказа")
    void validOrdersShouldBeDisplayedInDto() {
        enableNewMultiApi(true);

        GeoPoint geoPoint1 = GeoPointGenerator.generateLonLat();
        GeoPoint geoPoint2 = GeoPointGenerator.generateLonLat();

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint1)
                        .build())
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint2)
                        .build())
                .build());

        var orders = List.of(order1, order2);

        RoutingCourier routingCourier = RoutingCourier.builder()
                .id(user.getId())
                .ref(user.getFullName() + "-" + user.getUid())
                .depotId(shift.getSortingCenter().getId())
                .scheduleData(new RoutingScheduleData(
                        RoutingCourierVehicleType.CAR,
                        RelativeTimeInterval.valueOf("09:00-18:00")
                ))
                .routingTimeMultiplier(RoutingCourierMapper.DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .vehicleCapacity(BigDecimal.ONE)
                .servicedLocationType(RoutingLocationType.delivery)
                .priority(1)
                .vehicleId(123L)
                .partnerId(123L)
                .build();


        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(Set.of(routingCourier))
                        .orders(orders)
                        .clientReturns(List.of())
                        .movements(List.of())
                        .baseRoutingRequestUserIdToSubjectIds(
                                Map.of(
                                        routingCourier.getId(),
                                        List.of(
                                                List.of(order1.getId())
                                        )
                                )
                        )
                        .build())
                .mockType(RoutingMockType.MANUAL)
                .build();
        RoutingRequest routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

        RoutingCourier courier = routingRequest.getUsers().iterator().next();
        assertThat(courier.getPlannedTaskIds()).hasSize(1);
        assertThat(routingRequest.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Должны убрать все невалидные заказы")
    void shouldRemoveAllInvalidOrders() {
        enableNewMultiApi(true);

        GeoPoint geoPoint1 = GeoPoint.ofLatLon(0,0);
        GeoPoint geoPoint2 = GeoPoint.ofLatLon(0,0);

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint1)
                        .build())
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint2)
                        .build())
                .build());

        var orders = List.of(order1, order2);

        RoutingCourier routingCourier = RoutingCourier.builder()
                .id(user.getId())
                .ref(user.getFullName() + "-" + user.getUid())
                .depotId(shift.getSortingCenter().getId())
                .scheduleData(new RoutingScheduleData(
                        RoutingCourierVehicleType.CAR,
                        RelativeTimeInterval.valueOf("09:00-18:00")
                ))
                .routingTimeMultiplier(RoutingCourierMapper.DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .vehicleCapacity(BigDecimal.ONE)
                .servicedLocationType(RoutingLocationType.delivery)
                .priority(1)
                .vehicleId(123L)
                .partnerId(123L)
                .build();


        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(Set.of(routingCourier))
                        .orders(orders)
                        .clientReturns(List.of())
                        .movements(List.of())
                        .baseRoutingRequestUserIdToSubjectIds(
                                Map.of(
                                        routingCourier.getId(),
                                        List.of(
                                                List.of(order1.getId())
                                        )
                                )
                        )
                        .build())
                .mockType(RoutingMockType.MANUAL)
                .build();
        RoutingRequest routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

        RoutingCourier courier = routingRequest.getUsers().iterator().next();
        assertThat(courier.getPlannedTaskIds()).hasSize(0);
        assertThat(routingRequest.getItems()).hasSize(0);
    }

    @Test
    @DisplayName("Проверяем тег JEWELRY на заказах, содержащих товар с карго типом JEWELRY")
    void orderWithJeweleryItemShouldHaveJeweleryTag() {
        enableNewMultiApi(true);

        GeoPoint geoPoint1 = GeoPointGenerator.generateLonLat();

        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .isJewelry(true)
                        .itemsCount(1)
                        .build())
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint1)
                        .build())
                .build());
        var orders = List.of(order);

        RoutingCourier routingCourier = RoutingCourier.builder()
                .id(user.getId())
                .ref(user.getFullName() + "-" + user.getUid())
                .depotId(shift.getSortingCenter().getId())
                .scheduleData(new RoutingScheduleData(
                        RoutingCourierVehicleType.CAR,
                        RelativeTimeInterval.valueOf("09:00-18:00")
                ))
                .routingTimeMultiplier(RoutingCourierMapper.DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .vehicleCapacity(BigDecimal.ONE)
                .servicedLocationType(RoutingLocationType.delivery)
                .priority(1)
                .vehicleId(123L)
                .partnerId(123L)
                .build();


        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(Set.of(routingCourier))
                        .orders(orders)
                        .clientReturns(List.of())
                        .movements(List.of())
                        .baseRoutingRequestUserIdToSubjectIds(
                                Map.of(
                                        routingCourier.getId(),
                                        List.of(
                                                List.of(order.getId())
                                        )
                                )
                        )
                        .build())
                .mockType(RoutingMockType.MANUAL)
                .build();
        RoutingRequest routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

        assertThat(routingRequest.getItems()).hasSize(1);
        assertThat(routingRequest.getItems().get(0).getAdditionalTags()).contains(AdditionalTag.JEWELRY.getCode());
    }

    @Test
    @DisplayName("Проверяем тег JEWELRY на заказах, не содержащих товар с карго типом JEWELRY")
    void orderWithoutJeweleryItemShouldNotHaveJeweleryTag() {

        enableNewMultiApi(true);

        GeoPoint geoPoint1 = GeoPointGenerator.generateLonLat();

        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(1)
                        .build())
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint1)
                        .build())
                .build());
        var orders = List.of(order);

        RoutingCourier routingCourier = RoutingCourier.builder()
                .id(user.getId())
                .ref(user.getFullName() + "-" + user.getUid())
                .depotId(shift.getSortingCenter().getId())
                .scheduleData(new RoutingScheduleData(
                        RoutingCourierVehicleType.CAR,
                        RelativeTimeInterval.valueOf("09:00-18:00")
                ))
                .routingTimeMultiplier(RoutingCourierMapper.DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .vehicleCapacity(BigDecimal.ONE)
                .servicedLocationType(RoutingLocationType.delivery)
                .priority(1)
                .vehicleId(123L)
                .partnerId(123L)
                .build();


        CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(Set.of(routingCourier))
                        .orders(orders)
                        .clientReturns(List.of())
                        .movements(List.of())
                        .baseRoutingRequestUserIdToSubjectIds(
                                Map.of(
                                        routingCourier.getId(),
                                        List.of(
                                                List.of(order.getId())
                                        )
                                )
                        )
                        .build())
                .mockType(RoutingMockType.MANUAL)
                .build();
        RoutingRequest routingRequest = routingRequestCreator.createShiftRoutingRequest(command);

        assertThat(routingRequest.getItems()).hasSize(1);
        assertThat(routingRequest.getItems().get(0).getAdditionalTags()).doesNotContain(AdditionalTag.JEWELRY.getCode());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void movementWithdropoffCargoReturnTag(boolean enableNewVerMultiApi) {
        enableNewMultiApi(enableNewVerMultiApi);
        DeliveryService deliveryService = deliveryServicweRepository.findByIdOrThrow(DeliveryService.FAKE_DS_ID);
        LocalDate today = LocalDate.now(clock);
        OrderWarehouse warehouse = createWarehouse("123");
        warehouse.setRegionId(100500);
        OrderWarehouseAddress orderWarehouseAddress = warehouse.getAddress();
        orderWarehouseAddress.setLongitude(BigDecimal.ONE);
        orderWarehouseAddress.setLatitude(BigDecimal.ONE);

        String movementDroppoffReturnId = "1";
        Movement movementDroppoffReturn = getMovement(
                warehouse,
                today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
                today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
                movementDroppoffReturnId,
                MovementStatus.CREATED,
                deliveryService.getId()
        );
        movementDroppoffReturn.setId(100501L);
        movementDroppoffReturn.setWarehouseTo(
                createWarehouse("123-to", BigDecimal.valueOf(55), BigDecimal.valueOf(55))
        );
        movementDroppoffReturn.setTags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN));

        String movementDropshipId = "2";
        Movement movementDropship = getMovement(
                warehouse,
                today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
                today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
                movementDropshipId,
                MovementStatus.CREATED,
                deliveryService.getId()
        );
        movementDropship.setId(100502L);

        RoutingRequest routingRequest = prepareRoutingRequest(
                List.of(), List.of(movementDroppoffReturn, movementDropship)
        );

        RoutingRequestItem routingRequestItemDropoff = routingRequest.getItems().stream()
                .filter(it -> it.getRef().equals(DROPOFF_RETURN_REF_PREFIX + movementDroppoffReturnId))
                .findFirst().get();
        assertThat(routingRequestItemDropoff.getType()).isEqualTo(RoutingRequestItemType.DROPOFF_CARGO_RETURN);
        assertThat(
                routingRequestItemDropoff.getTags().contains(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode())
        ).isTrue();
        assertThat(routingRequestItemDropoff.getAddress().getLatitude().stripTrailingZeros())
                .isEqualTo(movementDroppoffReturn.getWarehouseTo().getAddress().getLatitude().stripTrailingZeros());
        assertThat(routingRequestItemDropoff.getAddress().getLongitude().stripTrailingZeros())
                .isEqualTo(movementDroppoffReturn.getWarehouseTo().getAddress().getLongitude().stripTrailingZeros());

        RoutingRequestItem routingRequestItemDropship = routingRequest.getItems().stream()
                .filter(it -> it.getRef().equals(DROPSHIPS_REF_PREFIX + movementDropshipId))
                .findFirst().get();
        assertThat(routingRequestItemDropship.getType()).isEqualTo(RoutingRequestItemType.DROPSHIP);
        assertThat(
                routingRequestItemDropship.getTags().contains(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode())
        ).isFalse();
        assertThat(routingRequestItemDropship.getAddress().getLatitude().stripTrailingZeros())
                .isEqualTo(movementDropship.getWarehouse().getAddress().getLatitude().stripTrailingZeros());
        assertThat(routingRequestItemDropship.getAddress().getLongitude().stripTrailingZeros())
                .isEqualTo(movementDropship.getWarehouse().getAddress().getLongitude().stripTrailingZeros());
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void createRoutingRequestWithDefaultAdditionalTimeForSurvey(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        TransportType transportType = createTransportType();
        clearAfterTest(transportType);
        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);
        var pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100L, 100L);
        pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);

        var orders = List.of(
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pickupPoint)
                        .build())
        );

        var pickupPointIdsWithActiveSurveyTasks =
                createShiftRoutingRequestCommandFactory.getPickupPointIdsWithActiveSurveyTasks();
        long additionalTimeForSurvey = createShiftRoutingRequestCommandFactory.getAdditionalTimeForSurvey();

        RoutingRequest routingRequest = prepareRoutingRequest(orders, List.of(),
                pickupPointIdsWithActiveSurveyTasks, additionalTimeForSurvey);

        assertThat(routingRequest.getUsers()).hasSize(1);
        assertThat(routingRequest.getItems().get(0).getAdditionalTimeForSurvey()).isEqualTo(DEFAULT_SURVEY_TASK_ADDITIONAL_TIME_ON_RP_SECONDS);
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void createRoutingRequestWithAdditionalTimeForSurvey(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        configurationServiceAdapter.mergeValue(SURVEY_TASK_ADDITIONAL_TIME_ON_RP_SECONDS, 100L);
        TransportType transportType = createTransportType();
        clearAfterTest(transportType);
        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);
        var pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100L, 100L);
        pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);

        var orders = List.of(
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pickupPoint)
                        .build())
        );


        var pickupPointIdsWithActiveSurveyTasks =
                createShiftRoutingRequestCommandFactory.getPickupPointIdsWithActiveSurveyTasks();
        long additionalTimeForSurvey = createShiftRoutingRequestCommandFactory.getAdditionalTimeForSurvey();

        RoutingRequest routingRequest = prepareRoutingRequest(orders, List.of(),
                pickupPointIdsWithActiveSurveyTasks, additionalTimeForSurvey);

        assertThat(routingRequest.getUsers()).hasSize(1);


        assertThat(routingRequest.getItems().get(0).getAdditionalTimeForSurvey()).isEqualTo(100);
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void createRoutingRequestWithoutAdditionalTimeForSurvey(boolean enableNewVerMultiApi, boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        TransportType transportType = createTransportType();
        clearAfterTest(transportType);
        UserUtil.setTransportType(user, transportType);
        userRepository.saveAndFlush(user);

        var orders = List.of(
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .build())
        );

        var pickupPointIdsWithActiveSurveyTasks =
                createShiftRoutingRequestCommandFactory.getPickupPointIdsWithActiveSurveyTasks();
        long additionalTimeForSurvey = createShiftRoutingRequestCommandFactory.getAdditionalTimeForSurvey();

        RoutingRequest routingRequest = prepareRoutingRequest(orders, List.of(),
                pickupPointIdsWithActiveSurveyTasks, additionalTimeForSurvey);

        assertThat(routingRequest.getUsers()).hasSize(1);
        // 0 т.к. доставка не в ПВЗ
        assertThat(routingRequest.getItems().get(0).getAdditionalTimeForSurvey()).isEqualTo(0);
    }

    @DisplayName("Проверка, что ремаршрутизация объединяет логистические заявки разного типа в одну группу по общему ключу")
    @Test
    void rerouteMergeRoutableGroups() {
        enableOrderRoutable(true);

        var pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 555L);
        var order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var specialRequest = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(pickupPoint.getId())
                        .build());

        var clientReturn = clientReturnGenerator.generateReturnFromClient(LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusHours(1));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr4", 13, clientReturn.getId()))
                .routePoint(helper.taskLockerDelivery(order1.getId(), pickupPoint.getId()))
                .routePoint(helper.taskLockerDelivery(order2.getId(), pickupPoint.getId()))
                .build();

        long userShiftId = userShiftTestHelper.start(createCommand);

        commandService.addFlowTask(user, new UserShiftCommand.AddFlowTask(
                userShiftId,
                TaskFlowType.LOCKER_INVENTORY,
                NewCommonRoutePointData.builder()
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .address(new RoutePointAddress("my_address", GeoPointGenerator.generateLonLat()))
                        .expectedArrivalTime(Instant.now())
                        .name("my_name")
                        .pickupPointId(pickupPoint.getId())
                        .withLogisticRequests(List.of(specialRequest))
                        .build()
        ));

        var pickupPointIdsWithActiveSurveyTasks =
                createShiftRoutingRequestCommandFactory.getPickupPointIdsWithActiveSurveyTasks();
        var rerouteRequest = userShiftRoutingRequestCreator.createUserShiftRoutingRequest(
                CreateUserShiftRoutingRequestCommand.builder()
                        .userShiftId(userShiftId)
                        .additionalTimeForSurvey(10L)
                        .isAsyncReroute(false)
                        .pickupPointIdsWithActiveSurveyTasks(pickupPointIdsWithActiveSurveyTasks)
                        .shuffleInTransit(false)
                        .build()
        );

        assertThat(rerouteRequest.getItems()).hasSize(2);
        assertThat(rerouteRequest.getItems().get(0).getSubTaskCount()).isEqualTo(1);
        assertThat(rerouteRequest.getItems().get(1).getSubTaskCount()).isEqualTo(3);
    }

    private void disableYandexGoUser() {
        configurationServiceAdapter.deleteValue(YANDEX_GO_COMPANIES_IDS);
    }

    private void assertItemTags(RoutingRequestItem routingRequestItem, List<String> expectedItemRequiredTags,
                                List<String> unexpectedItemRequiredTags) {
        assertThat(routingRequestItem.getTags())
                .containsAll(expectedItemRequiredTags)
                .doesNotContainAnyElementsOf(unexpectedItemRequiredTags);
    }

    private void assertUserTags(
            RoutingCourier routingCourier,
            List<String> expectedTags,
            List<String> unexpectedTags,
            List<String> expectedExcludedTags,
            List<String> unexpectedExcludedTags) {
        if (!expectedTags.isEmpty()) {
            assertThat(routingCourier.getAdditionalTags())
                    .containsAll(expectedTags);
        }
        if (!unexpectedTags.isEmpty()) {
            assertThat(routingCourier.getAdditionalTags())
                    .doesNotContainAnyElementsOf(unexpectedTags);
        }
        if (!expectedExcludedTags.isEmpty()) {
            assertThat(routingCourier.getExcludedTags()).containsAll(expectedExcludedTags);
        }
        if (!unexpectedExcludedTags.isEmpty()) {
            assertThat(routingCourier.getExcludedTags()).doesNotContainAnyElementsOf(unexpectedExcludedTags);
        }
    }

    private RoutingRequest prepareRoutingRequest(boolean lockerOrder) {
        return prepareRoutingRequest(lockerOrder ? PartnerSubType.LOCKER : null);
    }

    private RoutingRequest prepareRoutingRequest(PartnerSubType subtype) {

        PickupPoint pickupPoint = null;
        if (subtype != null) {
            pickupPoint = testDataFactory.createPickupPoint(subtype, 1L, 1L);
        }

        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder().pickupPoint(pickupPoint).build());

        return prepareRoutingRequest(List.of(order), List.of(), List.of(), 0);
    }

    private RoutingRequest prepareRoutingRequest(List<Order> orders, List<Movement> movements) {
        return prepareRoutingRequest(orders, movements, List.of(), 0);
    }

    private RoutingRequest prepareRoutingRequest(List<Order> orders, List<Movement> movements,
                                                 List<Long> pickupPointIdsWithActiveSurveyTasks,
                                                 long additionalTimeForSurvey) {
        return prepareRoutingRequest(orders, movements, pickupPointIdsWithActiveSurveyTasks, List.of(),
                additionalTimeForSurvey);
    }

    private RoutingRequest prepareRoutingRequest(List<Order> orders, List<Movement> movements,
                                                 List<Long> pickupPointIdsWithActiveSurveyTasks,
                                                 List<ClientReturn> clientReturns,
                                                 long additionalTimeForSurvey) {
        return prepareRoutingRequest(
                orders, movements, pickupPointIdsWithActiveSurveyTasks,
                clientReturns, List.of(), additionalTimeForSurvey);
    }

    private RoutingRequest prepareRoutingRequestForRoutable(List<Routable> routableList) {
        return prepareRoutingRequest(List.of(), List.of(), List.of(), List.of(), routableList, 0);
    }

    private RoutingRequest prepareRoutingRequest(List<Order> orders, List<Movement> movements,
                                                 List<Long> pickupPointIdsWithActiveSurveyTasks,
                                                 List<ClientReturn> clientReturns,
                                                 List<Routable> routableList,
                                                 long additionalTimeForSurvey) {
        return transactionTemplate.execute(st -> {
            List<UserScheduleRule> users = scheduleRuleRepository.findAllWorkingRulesForDate(
                    shift.getShiftDate(),
                    shift.getSortingCenter().getId());

            Map<Long, RoutingCourier> couriersById =
                    createShiftRoutingRequestCommandFactory.mapCouriersFromUserSchedules(
                            users,
                            false,
                            Map.of(),
                            Map.of()
                    );

            var routableItems = routableList;
            var ordersForRequest = orders;
            if (sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                     ROUTING_ORDERS_AS_ROUTABLE_ENABLED, shift.getSortingCenter().getId())) {
                // Если флаг включен, переносим заказы
                routableItems = new ArrayList<>(routableList);
                routableItems.addAll(mapOrdersToRoutableItems(orders));
                ordersForRequest = Collections.emptyList();
            }

            CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                    .data(CreateShiftRoutingRequestCommandData.builder()
                            .routeDate(shift.getShiftDate())
                            .sortingCenter(shift.getSortingCenter())
                            .couriers(new HashSet<>(couriersById.values()))
                            .orders(ordersForRequest)
                            .clientReturns(clientReturns)
                            .routableList(routableItems)
                            .movements(movements)
                            .pickupPointIdsWithActiveSurveyTasks(pickupPointIdsWithActiveSurveyTasks)
                            .additionalTimeForSurvey(additionalTimeForSurvey)
                            .build())
                    .mockType(RoutingMockType.MANUAL)
                    .build();
            return routingRequestCreator.createShiftRoutingRequest(command);
        });
    }


    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void createRoutingRequestWithOrderPlaceFallbackToOrderDimensions(boolean enableNewVerMultiApi,
                                                                     boolean enableOrderRoutable) {
        enableNewMultiApi(enableNewVerMultiApi);
        enableOrderRoutable(enableOrderRoutable);
        AtomicInteger count = new AtomicInteger(15);
        List<Order> orders = Stream.generate(
                        () -> {
                            int i = count.incrementAndGet();

                            List<OrderPlaceDto> places = List.of(
                                    OrderPlaceDto.builder()
                                            .barcode(new OrderPlaceBarcode("172", "barcode" + i))
                                            .build());

                            return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                                    .dimensions(new Dimensions(BigDecimal.valueOf(2.3), i, i, i))
                                    .places(places)
                                    .build());
                        }).limit(4)
                .collect(Collectors.toList());


        RoutingRequest routingRequest = prepareRoutingRequest(orders, List.of());

        assertThat(routingRequest.getItems()).extracting(RoutingRequestItem::getVolume).doesNotContainNull();
        assertThat(routingRequest.getUsers()).hasSize(1);
        assertThat(routingRequest.getUsers()).extracting(RoutingCourier::getVehicleCapacity).doesNotContainNull();
    }

    private TransportType createTransportType() {
        return createTransportType(Set.of());
    }

    private TransportType createTransportType(Set<RoutingOrderTag> tags) {
        TransportType transportType = new TransportType();
        transportType.setCapacity(BigDecimal.valueOf(2.5));
        transportType.setName("someTransport");
        transportType.setRoutingPriority(100);
        transportType.setRoutingVehicleType(RoutingVehicleType.COMMON);
        transportType.setPalletsCapacity(0);
        transportType.setRoutingOrderTags(tags);
        return transportTypeRepository.saveAndFlush(transportType);
    }

    private DropoffCargo addDropoffCargo(String barcode) {
        return dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(barcode)
                        .logisticPointIdFrom("fakeIdFrom")
                        .logisticPointIdTo(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF)
                        .build());
    }

    private CreatedFlowResultDto prepareMixedCourierFlow() {

        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        var movementDropoffReturn = testDataFactory.buildDropOffReturnMovement(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF);
        var movementDropoShip = movementGenerator.generate(MovementCommand.Create.builder().build());


        var firstDeliveryTask = helper.taskUnpaid("addr1", 10, firstOrder.getId());
        var secondDeliveryTask = helper.taskUnpaid("addr2", 10, secondOrder.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(helper.taskDropOffReturn(movementDropoffReturn.getId()))
                .routePoint(firstDeliveryTask)
                .routePoint(secondDeliveryTask)
                .routePoint(helper.taskCollectDropship(LocalDate.now(), movementDropoShip))
                .build();

        Long userShiftId = prepareCourierFlowUserShift(createCommand, List.of(firstOrder.getId(), secondOrder.getId()));

        return CreatedFlowResultDto.builder()
                .dropOffMovements(List.of(movementDropoffReturn))
                .dropShipMovements(List.of(movementDropoShip))
                .orders(List.of(firstOrder, secondOrder))
                .userShiftId(userShiftId)
                .build();
    }

    private CreatedFlowResultDto prepareClientReturnAndOrderFlow() {
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();

        ClientReturn firstClientReturn = clientReturnGenerator.generateReturnFromClient();
        ClientReturn secondClientReturn = clientReturnGenerator.generateReturnFromClient();

        var movementDropoffReturn = testDataFactory.buildDropOffReturnMovement(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF);
        var movementDropoShip = movementGenerator.generate(MovementCommand.Create.builder().build());


        var firstClientReturnTask = helper.clientReturn("addr3", 12, firstClientReturn.getId());
        var secondDeliveryTask = helper.taskUnpaid("addr2", 12, secondOrder.getId());
        var firstDeliveryTask = helper.taskUnpaid("addr1", 10, firstOrder.getId());
        var secondClientReturnTask = helper.clientReturn("addr3", 12, secondClientReturn.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(helper.taskDropOffReturn(movementDropoffReturn.getId()))
                .routePoints(List.of(firstDeliveryTask, secondDeliveryTask, firstClientReturnTask,
                        secondClientReturnTask))
                .routePoint(helper.taskCollectDropship(LocalDate.now(), movementDropoShip))
                .build();

        Long userShiftId = userShiftTestHelper.start(createCommand);

        return CreatedFlowResultDto.builder()
                .dropOffMovements(List.of(movementDropoffReturn))
                .dropShipMovements(List.of(movementDropoShip))
                .orders(List.of(firstOrder, secondOrder))
                .clientReturns(List.of(firstClientReturn, secondClientReturn))
                .userShiftId(userShiftId)
                .build();
    }

    private CreatedFlowResultDto prepareLockerDeliveryFlow() {
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        var firstPickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100L, 100L);
        var secondPickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 101L, 100L);


        var secondDeliveryTask = helper.taskLockerDelivery(secondOrder.getId(), secondPickupPoint.getId());
        var firstDeliveryTask = helper.taskLockerDelivery(firstOrder.getId(), firstPickupPoint.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoints(List.of(firstDeliveryTask, secondDeliveryTask))
                .build();

        Long userShiftId = userShiftTestHelper.start(createCommand);

        return CreatedFlowResultDto.builder()
                .orders(List.of(firstOrder, secondOrder))
                .userShiftId(userShiftId)
                .build();
    }

    private CreatedFlowResultDto prepareDropOffOnlyCourierFlow() {

        var movementDropoffReturn = testDataFactory.buildDropOffReturnMovement(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(helper.taskDropOffReturn(movementDropoffReturn.getId(), pickupPoint.getId()))
                .build();

        Long userShiftId = prepareCourierFlowUserShift(createCommand, List.of());

        return CreatedFlowResultDto.builder()
                .dropOffMovements(List.of(movementDropoffReturn))
                .userShiftId(userShiftId)
                .build();
    }

    private CreatedFlowResultDto prepareWithoutDropOffCourierFlow() {

        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        var movementDropoShip = movementGenerator.generate(MovementCommand.Create.builder().build());

        var firstDeliveryTask = helper.taskUnpaid("addr1", 10, firstOrder.getId());
        var secondDeliveryTask = helper.taskUnpaid("addr2", 10, secondOrder.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(firstDeliveryTask)
                .routePoint(secondDeliveryTask)
                .routePoint(helper.taskCollectDropship(LocalDate.now(), movementDropoShip))
                .build();

        Long userShiftId = prepareCourierFlowUserShift(createCommand, List.of(firstOrder.getId(), secondOrder.getId()));

        return CreatedFlowResultDto.builder()
                .dropShipMovements(List.of(movementDropoShip))
                .orders(List.of(firstOrder, secondOrder))
                .userShiftId(userShiftId)
                .build();
    }

    private Long prepareCourierFlowUserShift(UserShiftCommand.Create createCommand,
                                             List<Long> successfullyScannedOrders) {

        Long userShiftId = transactionTemplate.execute(status -> commandService.createUserShift(createCommand));

        OrderPickupTask orderPickupTask = transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            userHelper.openShift(user, userShift.getId());
            userHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            var pickupTask = userShift.streamPickupRoutePoints()
                    .findFirst().orElseThrow().streamPickupTasks().findFirst().orElseThrow();
            commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                    userShift.getId(), userShift.getCurrentRoutePoint().getId(), pickupTask.getId()
            ));
            return pickupTask;
        });

        List<String> barcodes = List.of("barcode1", "barcode2");
        var dropoffCargos = barcodes
                .stream().map(this::addDropoffCargo)
                .collect(Collectors.toList());

        commandService.pickupOrders(user,
                new UserShiftCommand.FinishScan(userShiftId,
                        orderPickupTask.getRoutePoint().getId(),
                        orderPickupTask.getId(),
                        ScanRequest.builder()
                                .successfullyScannedDropoffCargos(Set.of(dropoffCargos.get(0).getId(),
                                        dropoffCargos.get(1).getId()))
                                .successfullyScannedOrders(successfullyScannedOrders)
                                .skippedDropoffCargos(Set.of())
                                .build()
                )
        );

        commandService.finishLoading(
                user,
                new UserShiftCommand.FinishLoading(
                        userShiftId,
                        orderPickupTask.getRoutePoint().getId(),
                        orderPickupTask.getId()
                )
        );
        return userShiftId;
    }

    private Movement getMovement(
            OrderWarehouse warehouse,
            Instant movementDeliveryIntervalFrom,
            Instant movementDeliveryIntervalTo,
            String externalId,
            MovementStatus created,
            long deliveryServiceId
    ) {
        Movement movement = new Movement();
        movement.setWarehouse(warehouse);
        movement.setDeliveryIntervalFrom(movementDeliveryIntervalFrom);
        movement.setDeliveryIntervalTo(movementDeliveryIntervalTo);
        movement.setExternalId(externalId);
        movement.setStatus(created);
        movement.setVolume(BigDecimal.ZERO);
        movement.setPredictedVolume(BigDecimal.ONE);
        movement.setDeliveryServiceId(deliveryServiceId);
        return movement;
    }

    private OrderWarehouse createWarehouse(String yandexId) {
        return createWarehouse(yandexId, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private OrderWarehouse createWarehouse(String yandexId, BigDecimal lon, BigDecimal lat) {
        return new OrderWarehouse(
                yandexId,
                "corp",
                new OrderWarehouseAddress(
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        1,
                        lon,
                        lat
                ),
                Map.of(),
                Collections.emptyList(),
                null,
                null);
    }

    private CreateUserShiftRoutingRequestCommand buildUserShiftRerouteRequestCommand(Long userShiftId) {
        var pickupPointIdsWithActiveSurveyTasks =
                createShiftRoutingRequestCommandFactory.getPickupPointIdsWithActiveSurveyTasks();

        CreateUserShiftRoutingRequestCommand command = CreateUserShiftRoutingRequestCommand.builder()
                .userShiftId(userShiftId)
                .shuffleInTransit(false)
                .pickupPointIdsWithActiveSurveyTasks(pickupPointIdsWithActiveSurveyTasks)
                .additionalTimeForSurvey(10L)
                .isAsyncReroute(false)
                .build();
        return command;
    }

    private void assertsReroutingRequest(CreatedFlowResultDto createdFlowResultDto, RoutingRequest routingRequest) {
        //assert DropOffs
        List<Movement> dropOffMovements = createdFlowResultDto.getDropOffMovements();
        List<RoutingRequestItem> dropOffitems = routingRequest.getItems()
                .stream()
                .filter(item -> item.getType() == RoutingRequestItemType.DROPOFF_CARGO_RETURN)
                .collect(Collectors.toList());
        assertThat(dropOffitems).hasSize(dropOffMovements.size());
        assertThat(dropOffitems.stream().map(RoutingRequestItem::getTaskId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(dropOffMovements
                        .stream().map(Movement::getId).map(String::valueOf).collect(Collectors.toList()));//assert
        // DropOffs

        //assert DropShips
        List<Movement> dropShipMovements = createdFlowResultDto.getDropShipMovements();
        List<RoutingRequestItem> dropShipItems = routingRequest.getItems()
                .stream()
                .filter(item -> item.getType() == RoutingRequestItemType.DROPSHIP)
                .collect(Collectors.toList());
        assertThat(dropShipItems).hasSize(dropShipMovements.size());
        assertThat(dropShipItems.stream().map(RoutingRequestItem::getTaskId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(dropShipMovements
                        .stream().map(Movement::getId).map(String::valueOf).collect(Collectors.toList()));

        //assert client orders
        List<Order> orders = createdFlowResultDto.getOrders();
        List<RoutingRequestItem> clientOrderItems = routingRequest.getItems()
                .stream()
                .filter(item -> item.getType() == RoutingRequestItemType.CLIENT)
                .collect(Collectors.toList());
        assertThat(clientOrderItems).hasSize(orders.size());
        assertThat(clientOrderItems.stream().map(RoutingRequestItem::getTaskId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(orders
                        .stream().map(Order::getId).map(String::valueOf).collect(Collectors.toList()));
    }

    private List<Routable> mapOrdersToRoutableItems(List<Order> orders) {
        var lavkaSmartConsolidationEnabled =
                configurationProvider.isBooleanEnabled(ConfigurationProperties
                        .SMART_CONSOLIDATION_ORDERS_FOR_LAVKA);
        var geoPointScale = configurationProvider.getValueAsInteger(ConfigurationProperties.GEO_POINT_SCALE)
                .orElse(GEO_POINT_SCALE);

        return orders.stream()
                .map(o -> orderRoutableCollector.map(o, true, lavkaSmartConsolidationEnabled, false, geoPointScale))
                .collect(Collectors.toList());
    }

    private void enableRerouteFixedItemsWithMovements(boolean enableRerouteFixedItemsWithMovements) {
        configurationServiceAdapter.mergeValue(REROUTE_FIXED_ITEMS_ADD_MOVEMENTS, enableRerouteFixedItemsWithMovements);
    }

    public static Stream<Arguments> twoBooleanArguments() {
        return Stream.of(
                Arguments.of(false, false),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(true, true)
        );
    }

    @Value
    @Builder
    public static class CreatedFlowResultDto {
        @Builder.Default
        List<Movement> dropOffMovements = List.of();
        @Builder.Default
        List<Movement> dropShipMovements = List.of();
        @Builder.Default
        List<Order> orders = List.of();
        @Builder.Default
        List<ClientReturn> clientReturns = List.of();
        Long userShiftId;
    }
}
