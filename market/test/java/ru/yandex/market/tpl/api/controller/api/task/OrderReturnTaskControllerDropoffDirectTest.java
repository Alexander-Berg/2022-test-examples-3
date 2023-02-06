package ru.yandex.market.tpl.api.controller.api.task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.destination.ScanTaskDestinationDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.scanner.ScannerOrderDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.ScannedPlaceDto;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.task.locker.delivery.LockerDeliveryCancelRequestDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointScanTaskRequestDto;
import ru.yandex.market.tpl.common.util.TplObjectMappers;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoFlowStatus;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.juggler.JugglerPushClient;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.api.controller.api.task.TestDropOffCourierFlowFactory.LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2;
import static ru.yandex.market.tpl.api.controller.api.task.TestDropOffCourierFlowFactory.LOGISTICPOINT_ID_SC;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CARGO_DROPOFF_DIRECT_FLOW_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DROPOFF_RETURN_SUPPORT_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.SKIP_DIRECT_DROPOFF_SCAN_ON_SC_ENABLED;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OrderReturnTaskControllerDropoffDirectTest extends BaseApiIntTest {

    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final LockerDeliveryService lockerDeliveryService;

    private final UserShiftCommandService commandService;
    private final TestDropOffCourierFlowFactory testFlowFactory;
    private final TestUserHelper testUserHelper;
    private final JugglerPushClient mockedJugglerPushClient;
    private final DropoffCargoRepository dropoffCargoRepository;
    private final MovementRepository movementRepository;
    private final UserShiftRepository userShiftRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;

    private UserShift userShift;
    private LockerDeliveryTask lockerTask;
    private User user;
    private List<DropoffCargo> dropoffCargos = List.of();
    private TestDropOffCourierFlowFactory.CreatedEntityDto createdEntityDto;


    @BeforeEach
    void setUp() {
        when(configurationProviderAdapter.isBooleanEnabled(DROPOFF_RETURN_SUPPORT_ENABLED)).thenReturn(true);
        when(configurationProviderAdapter.isBooleanEnabled(CARGO_DROPOFF_DIRECT_FLOW_ENABLED)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(mockedJugglerPushClient, configurationProviderAdapter);
        sortingCenterPropertyService.deletePropertyFromSortingCenter(
                sortingCenterRepository.findByIdOrThrow(userShift.getSortingCenter().getId()),
                SKIP_DIRECT_DROPOFF_SCAN_ON_SC_ENABLED
        );
    }

    void init(boolean skipDirectCargoScan) {
        setSkipProperty(skipDirectCargoScan);
        createdEntityDto = testFlowFactory.initDirectFlowLockerTaskState();
        userShift = createdEntityDto.getUserShift();
        user = createdEntityDto.getUser();
        lockerTask = createdEntityDto.getLockerTask();
        dropoffCargos = createdEntityDto.getSucceedScannedDirectCargos();
    }

    void initMixed(boolean skipDirectCargoScan) {
        setSkipProperty(skipDirectCargoScan);
        createdEntityDto = testFlowFactory.initDirectFlowLockerTaskMixed();
        userShift = createdEntityDto.getUserShift();
        user = createdEntityDto.getUser();
        lockerTask = createdEntityDto.getLockerTask();
        dropoffCargos = createdEntityDto.getSucceedScannedDirectCargos();
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnCorrectInfoDto(boolean skipDirectCargoScan) {
        //given
        init(skipDirectCargoScan);

        //finish loading
        finishUnLoadingLockerTask();
        //finish locker task
        lockerDeliveryService.finishTask(lockerTask.getId(), null, user);


        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        //when
        RoutePointDto routePointDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/route-points/{id}",
                                        userShift.getCurrentRoutePoint().getId()).
                                header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), RoutePointDto.class);

        //then

        List<TaskDto> tasks = routePointDto.getTasks();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0)).isInstanceOf(OrderReturnTaskDto.class);

        var taskDto = (OrderReturnTaskDto) tasks.get(0);

        if (skipDirectCargoScan) {
            assertThat(taskDto.getOrders()).hasSize(0);
            assertThat(taskDto.getDestinations()).hasSize(0);
        } else {
            assertThat(taskDto.getOrders()).hasSize(2);
            assertThat(taskDto.getOrders().stream().map(OrderScanTaskDto.OrderForScanDto::getExternalOrderId).collect(Collectors.toSet()))
                    .containsExactlyInAnyOrder(dropoffCargos.get(0).getBarcode(), dropoffCargos.get(1).getBarcode());
            assertThat(taskDto.getDestinations()).hasSize(1);
            assertThat(taskDto.getDestinations().get(0).getOutsideOrders()).hasSize(2);
            assertThat(taskDto.getDestinations().get(0).getType()).isEqualTo(OrderType.PVZ);
        }
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnCorrectInfoDto_whenSeveral(boolean skipDirectCargoScan) {
        //given
        setSkipProperty(skipDirectCargoScan);
        var createdEntity = testFlowFactory.initDirectSeveralFlowLockerTaskState();

        DropoffCargo dropoff2 = testFlowFactory.addDropoffCargo("DROPOFF_2", LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2,
                LOGISTICPOINT_ID_SC);

        //when
        lockerTask = createdEntity.getLockerTask();
        userShift = userShiftRepository.findByIdOrThrow(createdEntity.getUserShift().getId());
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .build()))

        ).andExpect(status().isOk());


        ScannerOrderDto scannerOrderDto0 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes",
                                        createdEntity.getSucceedScannedDirectCargos().get(0).getBarcode(), "ggg",
                                        "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);

        ScannerOrderDto scannerOrderDto1 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes",
                                        createdEntity.getSucceedScannedDirectCargos().get(1).getBarcode(), "ggg",
                                        "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);


        //Первый ПВЗ
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-unload",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(PickupPointScanTaskRequestDto.builder()
                        .completedOrders(List.of(scannerOrderDto0, scannerOrderDto1))
                        .build()))

        ).andExpect(status().isOk());


        userShift = userShiftRepository.findByIdOrThrow(createdEntity.getUserShift().getId());
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());

        LockerDeliveryTask lockerDeliveryTask2 =
                userShift.getCurrentRoutePoint().streamLockerDeliveryTasks().findFirst().orElseThrow();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerDeliveryTask2.getRoutePoint().getId(),
                        lockerDeliveryTask2.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .build()))

        ).andExpect(status().isOk());


        ScannerOrderDto scannerOrderDto3 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes", dropoff2.getBarcode(), "ggg", "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);

        //Второй ПВЗ
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-unload",
                        lockerDeliveryTask2.getRoutePoint().getId(),
                        lockerDeliveryTask2.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(PickupPointScanTaskRequestDto.builder()
                        .completedOrders(List.of(scannerOrderDto3))
                        .build()))

        ).andExpect(status().isOk());


        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        //when
        RoutePointDto routePointDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/route-points/{id}",
                                        userShift.getCurrentRoutePoint().getId()).
                                header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), RoutePointDto.class);

        //then
        List<TaskDto> tasks = routePointDto.getTasks();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0)).isInstanceOf(OrderReturnTaskDto.class);

        var taskDto = (OrderReturnTaskDto) tasks.get(0);

        if (skipDirectCargoScan) {
            assertThat(taskDto.getOrders()).hasSize(0);
            assertThat(taskDto.getDestinations()).hasSize(0);
        } else {
            assertThat(taskDto.getOrders()).hasSize(3);
            assertThat(taskDto.getOrders().stream().map(OrderScanTaskDto.OrderForScanDto::getExternalOrderId).collect(Collectors.toSet()))
                    .containsExactlyInAnyOrder(createdEntity.getSucceedScannedDirectCargos().get(0).getBarcode(),
                            createdEntity.getSucceedScannedDirectCargos().get(1).getBarcode(),
                            dropoff2.getBarcode());
            assertThat(taskDto.getDestinations()).hasSize(2);
            assertThat(taskDto.getDestinations().stream().map(ScanTaskDestinationDto::getType)
                    .anyMatch(type -> type != OrderType.PVZ)).isFalse();
        }
    }


    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCorrectFinishScanning_happyPath(boolean skipDirectCargoScan) {
        //given
        init(skipDirectCargoScan);
        //finish loading
        finishUnLoadingLockerTask();
        //finish locker task
        lockerDeliveryService.finishTask(lockerTask.getId(), null, user);


        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        RoutePointDto routePointDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/route-points/{id}",
                                        userShift.getCurrentRoutePoint().getId()).
                                header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), RoutePointDto.class);

        OrderReturnTaskDto orderReturnTaskDto = (OrderReturnTaskDto) routePointDto.getTasks().get(0);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/finish",
                        returnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(orderReturnTaskDto.getOrders())
                        .scannedOutsidePlaces(orderReturnTaskDto.getOrders()
                                .stream()
                                .map(orderScanDto -> ScannedPlaceDto
                                        .builder()
                                        .orderExternalId(orderScanDto.getExternalOrderId())
                                        .barcode(orderScanDto.getExternalOrderId())
                                        .build())
                                .collect(Collectors.toSet())

                        ).build()))

        ).andExpect(status().isOk());

        OrderReturnTaskDto returnTaskDto = finishReturnTask(returnTask);
        //then

        if (skipDirectCargoScan) {
            assertThat(returnTaskDto.getOrders()).hasSize(0);
            assertThat(returnTaskDto.getDestinations()).hasSize(0);
        } else {
            assertThat(returnTaskDto.getOrders()).hasSize(2);
        }
        assertThat(createdEntityDto.getSucceedScannedDirectCargos()
                .stream()
                .map(DropoffCargo::getId)
                .map(dropoffCargoRepository::findByIdOrThrow)
                .map(DropoffCargo::getStatus)
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(DropoffCargoFlowStatus.DELIVERED_TO_LOGISTIC_POINT);

        Movement updatedMovement =
                movementRepository.findByIdOrThrow(createdEntityDto.getMovementDropoffDirect().getId());
        assertEquals(MovementStatus.DELIVERED, updatedMovement.getStatus());
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCorrectFinishScanning_happyPath_Mixed(boolean skipDirectCargoScan) {
        //given
        initMixed(skipDirectCargoScan);
        //finish loading
        finishUnLoadingLockerTask();
        //finish locker task
        lockerDeliveryService.finishTask(lockerTask.getId(), null, user);


        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        RoutePointDto routePointDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/route-points/{id}",
                                        userShift.getCurrentRoutePoint().getId()).
                                header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), RoutePointDto.class);

        OrderReturnTaskDto orderReturnTaskDto = (OrderReturnTaskDto) routePointDto.getTasks().get(0);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/finish",
                        returnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(orderReturnTaskDto.getOrders())
                        .scannedOutsidePlaces(orderReturnTaskDto.getOrders()
                                .stream()
                                .map(orderScanDto -> ScannedPlaceDto
                                        .builder()
                                        .orderExternalId(orderScanDto.getExternalOrderId())
                                        .barcode(orderScanDto.getExternalOrderId())
                                        .build())
                                .collect(Collectors.toSet())

                        ).build()))

        ).andExpect(status().isOk());

        OrderReturnTaskDto returnTaskDto = finishReturnTask(returnTask);
        //then
        assertThat(createdEntityDto.getSucceedScannedDirectCargos()
                .stream()
                .map(DropoffCargo::getId)
                .map(dropoffCargoRepository::findByIdOrThrow)
                .map(DropoffCargo::getStatus)
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(DropoffCargoFlowStatus.DELIVERED_TO_LOGISTIC_POINT);

        assertThat(createdEntityDto.getSucceedScannedReturnCargos()
                .stream()
                .map(DropoffCargo::getId)
                .map(dropoffCargoRepository::findByIdOrThrow)
                .map(DropoffCargo::getStatus)
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(DropoffCargoFlowStatus.CREATED);

        Movement updatedMovement =
                movementRepository.findByIdOrThrow(createdEntityDto.getMovementDropoffDirect().getId());
        assertEquals(MovementStatus.DELIVERED, updatedMovement.getStatus());
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCorrectFinishScanning_happyPath_Mixed_Fail_whenUnloading(boolean skipDirectCargoScan) {
        //given
        initMixed(skipDirectCargoScan);
        //finish loading
        finishUnLoadingLockerTaskEmpty();
        //finish locker task
        lockerDeliveryService.finishTask(lockerTask.getId(), null, user);


        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        RoutePointDto routePointDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/route-points/{id}",
                                        userShift.getCurrentRoutePoint().getId()).
                                header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), RoutePointDto.class);

        OrderReturnTaskDto orderReturnTaskDto = (OrderReturnTaskDto) routePointDto.getTasks().get(0);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/finish",
                        returnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(orderReturnTaskDto.getOrders())
                        .scannedOutsidePlaces(orderReturnTaskDto.getOrders()
                                .stream()
                                .map(orderScanDto -> ScannedPlaceDto
                                        .builder()
                                        .orderExternalId(orderScanDto.getExternalOrderId())
                                        .barcode(orderScanDto.getExternalOrderId())
                                        .build())
                                .collect(Collectors.toSet())

                        ).build()))

        ).andExpect(status().isOk());

        OrderReturnTaskDto returnTaskDto = finishReturnTask(returnTask);
        //then

        //Заявка (Movement) на "прямой поток" выполнена, если доставлен хотя бы один лот и отменена в противном случае.
        Movement updatedMovement =
                movementRepository.findByIdOrThrow(createdEntityDto.getMovementDropoffDirect().getId());
        assertEquals(MovementStatus.CANCELLED, updatedMovement.getStatus());
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCorrectFinishScanning_happyPath_Mixed_Fail_pvzClosed(boolean skipDirectCargoScan) {
        //given
        initMixed(skipDirectCargoScan);
        //finish loading
        cancelLockerTask();
        //finish locker task
        lockerDeliveryService.finishTask(lockerTask.getId(), null, user);


        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        RoutePointDto routePointDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/route-points/{id}",
                                        userShift.getCurrentRoutePoint().getId()).
                                header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), RoutePointDto.class);

        OrderReturnTaskDto orderReturnTaskDto = (OrderReturnTaskDto) routePointDto.getTasks().get(0);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/finish",
                        returnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(orderReturnTaskDto.getOrders())
                        .scannedOutsidePlaces(orderReturnTaskDto.getOrders()
                                .stream()
                                .map(orderScanDto -> ScannedPlaceDto
                                        .builder()
                                        .orderExternalId(orderScanDto.getExternalOrderId())
                                        .barcode(orderScanDto.getExternalOrderId())
                                        .build())
                                .collect(Collectors.toSet())

                        ).build()))

        ).andExpect(status().isOk());

        OrderReturnTaskDto returnTaskDto = finishReturnTask(returnTask);
        //then

        //Заявка (Movement) на "прямой поток" выполнена, если доставлен хотя бы один лот и отменена в противном случае.
        Movement updatedMovement =
                movementRepository.findByIdOrThrow(createdEntityDto.getMovementDropoffDirect().getId());
        assertEquals(MovementStatus.CANCELLED, updatedMovement.getStatus());
    }

    private void cancelLockerTask() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/locker-delivery/{taskId}/cancel",
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(
                        new LockerDeliveryCancelRequestDto(OrderDeliveryTaskFailReasonType.PVZ_CLOSED,
                                "Not enough capacity", List.of())
                ))

        ).andExpect(status().isOk());
    }


    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnCorrectInfoDto_whenMixedCargos(boolean skipDirectCargoScan) {
        //given
        initMixed(skipDirectCargoScan);
        //finish loading
        finishUnLoadingLockerTask();
        //finish locker task
        lockerDeliveryService.finishTask(lockerTask.getId(), null, user);


        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        //when
        RoutePointDto routePointDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/route-points/{id}",
                                        userShift.getCurrentRoutePoint().getId()).
                                header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), RoutePointDto.class);

        //then

        List<TaskDto> tasks = routePointDto.getTasks();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0)).isInstanceOf(OrderReturnTaskDto.class);

        var taskDto = (OrderReturnTaskDto) tasks.get(0);

        if (skipDirectCargoScan) {
            assertThat(taskDto.getOrders()).hasSize(2);
            assertThat(taskDto.getDestinations().get(0).getOutsideOrders()).hasSize(2);
            assertThat(taskDto.getDestinations().get(0).getType()).isEqualTo(OrderType.PVZ);
            assertThat(taskDto.getOrders().stream().map(OrderScanTaskDto.OrderForScanDto::getExternalOrderId).collect(Collectors.toSet()))
                    .containsExactlyInAnyOrderElementsOf(
                            createdEntityDto.getSucceedScannedReturnCargos()
                                    .stream().map(DropoffCargo::getBarcode)
                                    .collect(Collectors.toSet()));
        } else {
            assertThat(taskDto.getOrders()).hasSize(4);
            var allCargos = new ArrayList<>(createdEntityDto.getSucceedScannedReturnCargos());
            allCargos.addAll(createdEntityDto.getSucceedScannedDirectCargos());
            assertThat(taskDto.getOrders().stream().map(OrderScanTaskDto.OrderForScanDto::getExternalOrderId).collect(Collectors.toSet()))
                    .containsExactlyInAnyOrderElementsOf(allCargos.stream().map(DropoffCargo::getBarcode)
                            .collect(Collectors.toSet()));
            assertThat(taskDto.getDestinations()).hasSize(2);
            assertThat(taskDto.getDestinations().get(0).getOutsideOrders()).hasSize(2);
            assertThat(taskDto.getDestinations().get(0).getType()).isEqualTo(OrderType.PVZ);
            assertThat(taskDto.getDestinations().get(1).getOutsideOrders()).hasSize(2);
            assertThat(taskDto.getDestinations().get(1).getType()).isEqualTo(OrderType.PVZ);
        }
    }

    private void finishUnLoadingLockerTask() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .comment("Комментарий, если были возвраты и их не выгрузили")
                        .build()))

        ).andExpect(status().isOk());

        ScannerOrderDto scannerOrderDto0 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes", dropoffCargos.get(0).getBarcode(), "ggg", "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);

        ScannerOrderDto scannerOrderDto1 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes", dropoffCargos.get(1).getBarcode(), "ggg", "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-unload",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(PickupPointScanTaskRequestDto.builder()
                        .completedOrders(List.of(scannerOrderDto0, scannerOrderDto1))
                        .build()))

        ).andExpect(status().isOk());
    }

    private void finishUnLoadingLockerTaskEmpty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .comment("Комментарий, если были возвраты и их не выгрузили")
                        .build()))

        ).andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-unload",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(PickupPointScanTaskRequestDto.builder()
                        .completedOrders(List.of())

                        .build()))

        ).andExpect(status().isOk());
    }

    private OrderReturnTaskDto finishReturnTask(OrderReturnTask returnTask) throws Exception {
        return TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/task/finish",
                                returnTask.getId())
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), OrderReturnTaskDto.class);
    }

    private OrderReturnTask getOrderReturnTask(UserShift us) {
        return us.streamReturnRoutePoints().findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
    }

    private void setSkipProperty(boolean skipValue) {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SKIP_DIRECT_DROPOFF_SCAN_ON_SC_ENABLED,
                skipValue
        );
    }
}
