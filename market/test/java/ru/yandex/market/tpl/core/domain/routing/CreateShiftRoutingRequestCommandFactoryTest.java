package ru.yandex.market.tpl.core.domain.routing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.routing.CustomRoutingParamDto;
import ru.yandex.market.tpl.api.model.routing.RoutingProfileType;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.common.util.exception.TplRoutingOrdersNotFoundException;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.routing.logistic_request.Routable;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingScheduleRule;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingScheduleRuleRepository;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingScheduleRuleUtil;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestCollector;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestClientReturnFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ROUTABLE_ROUTING_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ROUTING_ORDERS_AS_ROUTABLE_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.SPECIAL_REQUEST_ADD_TO_MVRP_REQUEST_ENABLED;

@RequiredArgsConstructor
public class CreateShiftRoutingRequestCommandFactoryTest extends TplAbstractTest {

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final Clock clock;
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final MovementRepository movementRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final RoutingScheduleRuleRepository routingScheduleRuleRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final TestClientReturnFactory clientReturnFactory;
    private final ClientReturnGenerator clientReturnGenerator;
    private final SpecialRequestCollector specialRequestCollector;
    private final RoutingCommandCreator routingCommandCreator;
    private final UserScheduleService userScheduleService;

    private Shift shift;
    private Order order;
    private Order orderWithPastDeliveryDate;
    private User user;
    private ClientReturn clientReturn;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(shift.getShiftDate())
                        .build()
        );

        clientReturn = clientReturnGenerator.generateReturnFromClient();

        orderWithPastDeliveryDate = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(shift.getShiftDate().minusDays(3L))
                        .build()
        );
        enableRoutable(true);
    }

    private void enableRoutable(boolean enable) {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(shift.getSortingCenter().getId()),
                ROUTING_ORDERS_AS_ROUTABLE_ENABLED,
                enable
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                ROUTABLE_ROUTING_ENABLED,
                true
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SPECIAL_REQUEST_ADD_TO_MVRP_REQUEST_ENABLED,
                true
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldLoadOrdersForShiftDateAndOrdersFromPast(boolean enableOrderRoutable) {
        enableRoutable(enableOrderRoutable);
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shift,
                RoutingMockType.REAL,
                null,
                Instant.now()
        );

        if (enableOrderRoutable) {
            assertThat(commandData.getRoutableList())
                    .extracting(Routable::getRef)
                    .containsOnly(order.getExternalOrderId(), orderWithPastDeliveryDate.getExternalOrderId());
        } else {
            assertThat(commandData.getOrders())
                    .extracting(Order::getExternalOrderId)
                    .containsOnly(order.getExternalOrderId(), orderWithPastDeliveryDate.getExternalOrderId());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldAddCurrentUnfinishedOrders_whenLastRealRequest(boolean enableOrderRoutable) {
        //given
        enableRoutable(enableOrderRoutable);
        Shift shiftForRoutingRequest = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(clock),
                SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);

        //add to DB unfinished order
        Order unFinishedOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(shiftForRoutingRequest.getShiftDate().minusDays(1L))
                        .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                        .build());

        //when
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shiftForRoutingRequest,
                RoutingMockType.REAL,
                null,
                Instant.now(clock)
        );

        //then
        if (enableOrderRoutable) {
            assertThat(commandData.getRoutableList())
                    .extracting(Routable::getRef)
                    .containsOnly(
                            order.getExternalOrderId(),
                            orderWithPastDeliveryDate.getExternalOrderId(),
                            unFinishedOrder.getExternalOrderId());
        } else {
            assertThat(commandData.getOrders())
                    .extracting(Order::getExternalOrderId)
                    .containsOnly(
                            order.getExternalOrderId(),
                            orderWithPastDeliveryDate.getExternalOrderId(),
                            unFinishedOrder.getExternalOrderId());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSkipCurrentUnfinishedOrders_whenNotLastRealRequest(boolean enableOrderRoutable) {
        //given
        enableRoutable(enableOrderRoutable);
        Shift shiftForRoutingRequest = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(clock),
                SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);

        //add to DB unfinished order
        orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(shiftForRoutingRequest.getShiftDate().minusDays(1L))
                        .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                        .build());


        RoutingScheduleRule routingScheduleRule = RoutingScheduleRuleUtil.routingScheduleRule(
                SortingCenter.DEFAULT_SC_ID,
                false,
                LocalTime.of(23, 0),
                LocalTime.of(23, 0)
        );
        routingScheduleRuleRepository.save(routingScheduleRule);

        //when
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shiftForRoutingRequest,
                RoutingMockType.REAL,
                null,
                Instant.now(clock).minus(3, ChronoUnit.HOURS)
        );

        //then
        if (enableOrderRoutable) {
            assertThat(commandData.getRoutableList())
                    .extracting(Routable::getRef)
                    .containsOnly(order.getExternalOrderId(), orderWithPastDeliveryDate.getExternalOrderId());
        } else {
            assertThat(commandData.getOrders())
                    .extracting(Order::getExternalOrderId)
                    .containsOnly(order.getExternalOrderId(), orderWithPastDeliveryDate.getExternalOrderId());
        }

        routingScheduleRuleRepository.delete(routingScheduleRule);
    }

    @Test
    void dropoffCargoReturns_add() {
        //given
        Shift shiftForRoutingRequest = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(clock),
                SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);

        var movements = createDropShipAndDropoffCargoReturnMovements();

        //when
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shiftForRoutingRequest,
                RoutingMockType.REAL,
                null,
                Instant.now(clock)
        );

        //then
        assertThat(commandData.getMovements()).containsAll(movements);
    }

    @Test
    void clientReturns_addIfEnabled() {
        //given
        Shift shiftForRoutingRequest = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(clock),
                SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED,
                true
        );

        ClientReturn expectedClientReturn = clientReturnFactory.buildAndSave(DeliveryService.DEFAULT_DS_ID,
                LocalDateTime.now(clock));
        ClientReturn notExpectedCauseDs = clientReturnFactory.buildAndSave(1234L,
                LocalDateTime.now(clock));
        ClientReturn notExpectedCauseDate = clientReturnFactory.buildAndSave(DeliveryService.DEFAULT_DS_ID,
                LocalDateTime.now(clock).minusDays(1));

        //when
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shiftForRoutingRequest,
                RoutingMockType.REAL,
                null,
                Instant.now(clock)
        );

        //then
        assertThat(commandData.getClientReturns()).containsOnly(expectedClientReturn);
    }

    @Test
    void errorWhenEmptyRequest() {
        //given
        LocalDate shiftDate = LocalDate.now(clock).plusDays(10);
        Shift shiftForRoutingRequest = userHelper.findOrCreateShiftForScWithStatus(shiftDate,
                SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);


        //then
        assertThrows(TplRoutingOrdersNotFoundException.class, () ->
                createShiftRoutingRequestCommandFactory.createCommandData(
                        shiftForRoutingRequest,
                        RoutingMockType.REAL,
                        null,
                        Instant.now(clock)
                ));
    }

    @Test
    void clientReturns_emptyIfDisabled() {
        //given
        Shift shiftForRoutingRequest = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(clock),
                SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED,
                false
        );

        //appropriate Client Return
        clientReturnFactory.buildAndSave(DeliveryService.DEFAULT_DS_ID, LocalDateTime.now(clock));

        //when
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shiftForRoutingRequest,
                RoutingMockType.REAL,
                null,
                Instant.now(clock)
        );

        //then
        assertThat(commandData.getClientReturns()).isEmpty();
    }


    @Test
    @DisplayName("Проверка, что при передаче клиентского возврата в дто, он появляется в комманде")
    void clientReturnsPresentInDto() {
        var customRoutingParamDto = CustomRoutingParamDto.builder()
                .shiftId(shift.getId())
                .externalOrderIds(Set.of(order.getExternalOrderId()))
                .strategy(RoutingProfileType.GROUP)
                .userIds(Set.of(user.getId()))
                .courierCount(1)
                .externalMovementIds(Set.of())
                .externalClientReturnIds(Set.of(clientReturn.getExternalReturnId()))
                .build();

        var commandDto = routingCommandCreator.createShiftRoutingCommand(customRoutingParamDto);
        assertThat(commandDto.getData().getClientReturns()).contains(clientReturn);
        assertThat(commandDto.getData().getOrders()).contains(order);
    }

    @Test
    @DisplayName("Проверка, что логистические заявки попадают в команду при домаршрутизации")
    void additionalRoutingTest_OrdersAndSpecialRequests() {
        var specialRequest = specialRequestGenerateService.createSpecialRequest();
        var customRoutingParamDto = CustomRoutingParamDto.builder()
                .shiftId(shift.getId())
                .logisticRequestIds(Set.of(order.getExternalOrderId(), specialRequest.getExternalId()))
                .strategy(RoutingProfileType.GROUP)
                .userIds(Set.of(user.getId()))
                .courierCount(1)
                .build();

        var commandDto = routingCommandCreator.createShiftRoutingCommand(customRoutingParamDto);
        assertThat(commandDto.getData().getOrders()).isEmpty();
        var routableList = commandDto.getData().getRoutableList();
        assertThat(routableList).hasSize(2);
        var routableIds = routableList.stream()
                .map(Routable::getEntityId)
                .collect(Collectors.toSet());
        assertThat(routableIds)
                .contains(order.getId())
                .contains(specialRequest.getId());
    }

    @Test
    @DisplayName("Проверка, что при отсуствии клиентского возврата, который ожидается по дто, пробрасывается ошибка")
    void throwsException_WhenClientReturnNonExistent() {
        String nonExistentId = clientReturn.getExternalReturnId() + "/doesntExist";
        var customRoutingParamDto = CustomRoutingParamDto.builder()
                .shiftId(shift.getId())
                .externalOrderIds(Set.of(order.getExternalOrderId()))
                .strategy(RoutingProfileType.GROUP)
                .userIds(Set.of(user.getId()))
                .courierCount(1)
                .externalMovementIds(Set.of())
                .externalClientReturnIds(Set.of(clientReturn.getExternalReturnId(), nonExistentId))
                .build();

        assertThrows(TplInvalidParameterException.class,
                () -> routingCommandCreator.createShiftRoutingCommand(customRoutingParamDto));
    }

    @Test
    void specialRequests_addIfEnabled() {
        //given
        Shift shiftForRoutingRequest = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(clock),
                SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SPECIAL_REQUEST_ADD_TO_MVRP_REQUEST_ENABLED,
                true
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                ROUTABLE_ROUTING_ENABLED,
                true
        );

        List<Routable> routableList = createSpecialRequestsReturnRoutables();

        //when
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shiftForRoutingRequest,
                RoutingMockType.REAL,
                null,
                Instant.now(clock)
        );

        //then
        assertThat(commandData.getRoutableList()).containsAll(routableList);

    }

    @Test
    void specialRequests_emptyIfEnabled() {
        //given
        Shift shiftForRoutingRequest = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(clock),
                SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SPECIAL_REQUEST_ADD_TO_MVRP_REQUEST_ENABLED,
                false
        );

        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(shiftForRoutingRequest.getShiftDate())
                        .build()
        );

        List<Routable> routableList = createSpecialRequestsReturnRoutables();

        //when
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shiftForRoutingRequest,
                RoutingMockType.REAL,
                null,
                Instant.now(clock)
        );

        var extIds = commandData.getRoutableList().stream()
                .map(Routable::getRef)
                .collect(Collectors.toSet());
        for (var routable : routableList) {
            assertThat(extIds).doesNotContain(routable.getRef());
        }
    }

    @Test
    @DisplayName("Когда у курьера есть исключение в расписании")
    void overrideScheduleRuleForCourier() {
        UserScheduleRuleDto dto = UserScheduleRuleDto.builder()
                .activeFrom(LocalDate.now(clock))
                .activeTo(LocalDate.now(clock))
                .scheduleType(UserScheduleType.OVERRIDE_WORK)
                .shiftStart(LocalTime.of(9, 0))
                .shiftEnd(LocalTime.of(23, 0))
                .sortingCenterId(SortingCenter.DEFAULT_SC_ID)
                .applyFrom(LocalDate.now(clock))
                .build();
        userScheduleService.createRule(user.getId(), dto, user.getCompany().isSuperCompany());
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shift,
                RoutingMockType.REAL,
                null,
                Instant.now()
        );
        assertThat(commandData.getCouriers().size()).isEqualTo(1);
        assertThat(commandData.getCouriers().iterator().next().getScheduleData().getTimeInterval().toISOString())
                .isEqualTo("09:00/23:00");
    }

    @Test
    @DisplayName("Смена у курьера без исключений, по базовому распианию")
    void defaultScheduleRule() {
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shift,
                RoutingMockType.REAL,
                null,
                Instant.now()
        );
        assertThat(commandData.getCouriers().size()).isEqualTo(1);
        assertThat(commandData.getCouriers().iterator().next().getScheduleData().getTimeInterval().toISOString())
                .isEqualTo("09:00/19:00");
    }

    @Test
    @DisplayName("Смена с тегом DROPSHIP, MAX_SHIFT_DROPSHIP_END_TIME возвращает значение больше," +
            " чем конец смены курьера по расписанию")
    void maxShiftDropshipEndTimeGreaterThanEndSchedule() {
        user = userHelper.createUserWithTransportTags(12398L, List.of(RequiredRoutingTag.DROPSHIP.getCode()));

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SortingCenterProperties.MAX_SHIFT_DROPSHIP_END_TIME,
                LocalTime.of(23, 30)
        );

        UserScheduleRuleDto dto = UserScheduleRuleDto.builder()
                .activeFrom(LocalDate.now(clock))
                .activeTo(LocalDate.now(clock))
                .scheduleType(UserScheduleType.OVERRIDE_WORK)
                .shiftStart(LocalTime.of(9, 0))
                .shiftEnd(LocalTime.of(23, 0))
                .sortingCenterId(SortingCenter.DEFAULT_SC_ID)
                .applyFrom(LocalDate.now(clock))
                .build();
        userScheduleService.createRule(user.getId(), dto, user.getCompany().isSuperCompany());
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shift,
                RoutingMockType.REAL,
                null,
                Instant.now()
        );

        clearAfterTest(user.getTransportType());

        List<RoutingCourier> couriers = new ArrayList<>(commandData.getCouriers());
        assertThat(couriers.size()).isEqualTo(2);
        assertThat(couriers.stream().filter(courier -> courier.getId() == user.getId()).findFirst().get()
                .getScheduleData().getTimeInterval().toISOString())
                .isEqualTo("09:00/23:00");
    }

    @Test
    @DisplayName("Смена с тегом DROPSHIP, MAX_SHIFT_DROPSHIP_END_TIME возвращает значение меньше," +
            " чем конец смены курьера по расписанию")
    void maxShiftDropshipEndTimeLessThanEndSchedule() {
        user = userHelper.createUserWithTransportTags(12398L, List.of(RequiredRoutingTag.DROPSHIP.getCode()));

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SortingCenterProperties.MAX_SHIFT_DROPSHIP_END_TIME,
                LocalTime.of(22, 30)
        );

        UserScheduleRuleDto dto = UserScheduleRuleDto.builder()
                .activeFrom(LocalDate.now(clock))
                .activeTo(LocalDate.now(clock))
                .scheduleType(UserScheduleType.OVERRIDE_WORK)
                .shiftStart(LocalTime.of(9, 0))
                .shiftEnd(LocalTime.of(23, 0))
                .sortingCenterId(SortingCenter.DEFAULT_SC_ID)
                .applyFrom(LocalDate.now(clock))
                .build();
        userScheduleService.createRule(user.getId(), dto, user.getCompany().isSuperCompany());
        CreateShiftRoutingRequestCommandData commandData = createShiftRoutingRequestCommandFactory.createCommandData(
                shift,
                RoutingMockType.REAL,
                null,
                Instant.now()
        );

        clearAfterTest(user.getTransportType());

        List<RoutingCourier> couriers = new ArrayList<>(commandData.getCouriers());
        assertThat(couriers.size()).isEqualTo(2);
        assertThat(couriers.stream().filter(courier -> courier.getId() == user.getId()).findAny().get()
                .getScheduleData().getTimeInterval().toISOString())
                .isEqualTo("09:00/22:30");
    }

    @NotNull
    private List<Routable> createSpecialRequestsReturnRoutables() {
        SpecialRequest specialRequest1 = specialRequestGenerateService.createSpecialRequest();
        SpecialRequest specialRequest2 = specialRequestGenerateService.createSpecialRequest();
        Routable routable1 = specialRequestCollector.convertToRoutable(specialRequest1);
        Routable routable2 = specialRequestCollector.convertToRoutable(specialRequest2);
        return List.of(routable1, routable2);
    }


    @NotNull
    private List<Movement> createDropShipAndDropoffCargoReturnMovements() {
        LocalDate today = LocalDate.now(clock);
        OrderWarehouse warehouse = createWarehouse("123");

        Movement movement = getMovement(
                warehouse,
                today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
                today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
                "1",
                MovementStatus.CREATED,
                198L
        );

        Movement movement2 = getMovement(
                warehouse,
                today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
                today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
                "2",
                MovementStatus.CREATED,
                198L
        );
        movement2.setTags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN));

        return movementRepository.saveAll(List.of(movement, movement2));
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
        movement.setDeliveryServiceId(deliveryServiceId);
        return movement;
    }

    private OrderWarehouse createWarehouse(String yandexId) {
        return orderWarehouseRepository.saveAndFlush(
                new OrderWarehouse(yandexId, "corp", new OrderWarehouseAddress(
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        "asdf",
                        1,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ), Map.of(), Collections.emptyList(), null, null));
    }

}
