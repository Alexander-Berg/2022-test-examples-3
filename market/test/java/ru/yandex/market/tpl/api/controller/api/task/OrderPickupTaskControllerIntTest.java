package ru.yandex.market.tpl.api.controller.api.task;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointListDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointSummaryDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.ScannedPlaceDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoFlowStatus;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtaskDropOff;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.api.controller.api.task.TestDropOffCourierFlowFactory.LOGISTICPOINT_ID_FOR_RETURN_DROPOFF2;
import static ru.yandex.market.tpl.api.controller.api.task.TestDropOffCourierFlowFactory.LOGISTICPOINT_ID_SC;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DROPOFF_RETURN_SUPPORT_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.MIGRATION_TO_NEW_ALGO_PICKUP_DROPOFF_RETURN_CARGOS_ENABLED;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OrderPickupTaskControllerIntTest extends BaseApiIntTest {

    private final TestDataFactory testDataFactory;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final UserPropertyService userPropertyService;

    private final DropoffCargoRepository dropoffCargoRepository;
    private final MovementRepository movementRepository;
    private final MovementHistoryEventRepository movementHistoryEventRepository;
    private final TestDropOffCourierFlowFactory testFlowFactory;
    private final TransferApi mockedTransferApi;
    private final UserShiftCommandService userShiftCommandService;

    private UserShift userShift;
    private OrderPickupTask pickupTask;
    private Movement movementDropoffReturn;
    private User user;
    private Order lockerOrder;
    TestDropOffCourierFlowFactory.CreatedEntityDto createdEntityDto;

    @BeforeEach
    void setUp() {
        when(configurationProviderAdapter.isBooleanEnabled(MIGRATION_TO_NEW_ALGO_PICKUP_DROPOFF_RETURN_CARGOS_ENABLED))
                .thenReturn(false);
        when(configurationProviderAdapter.isBooleanEnabled(DROPOFF_RETURN_SUPPORT_ENABLED))
                .thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        when(configurationProviderAdapter.isBooleanEnabled(MIGRATION_TO_NEW_ALGO_PICKUP_DROPOFF_RETURN_CARGOS_ENABLED))
                .thenReturn(false);
        when(configurationProviderAdapter.isBooleanEnabled(DROPOFF_RETURN_SUPPORT_ENABLED))
                .thenReturn(false);
    }

    void initOnePvz() {
        createdEntityDto = testFlowFactory.initReturnFlowPickupTaskState();
        updateFields();

    }

    void initSeveralPvz() {
        createdEntityDto = testFlowFactory.initReturnSeveralFlowPickupTaskState();
        updateFields();
    }

    @SneakyThrows
    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void shouldAcceptScannedCargos(boolean enablePickupAlgoMigration) {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(MIGRATION_TO_NEW_ALGO_PICKUP_DROPOFF_RETURN_CARGOS_ENABLED))
                .thenReturn(enablePickupAlgoMigration);
        initOnePvz();
        List<String> barcodes = List.of("barcode1", "barcode2");
        barcodes.forEach(testFlowFactory::addDropoffCargoReturn);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{task-id}/finish",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of(
                                        OrderScanTaskDto.OrderForScanDto.builder()
                                                .externalOrderId(barcodes.get(0))
                                                .build()
                                )
                        )
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(barcodes.get(1))
                                        .build(),
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(lockerOrder.getExternalOrderId())
                                        .build()
                        ))
                        .scannedOutsidePlaces(
                                Set.of(ScannedPlaceDto.builder()
                                        .orderExternalId(barcodes.get(0))
                                        .build())
                        )
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        RoutePointListDto routePointListDto = requestRoutPoints();
        //then
        UserShift updatedUserShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        List<LockerSubtask> dropOffReturnsSubTasks = updatedUserShift
                .streamLockerDeliveryTasks()
                .flatMap(LockerDeliveryTask::streamDropOffReturnSubtasks)
                .collect(Collectors.toList());
        assertThat(dropOffReturnsSubTasks).hasSize(1);

        LockerSubtaskDropOff lockerSubtaskDropOffReturn =
                dropOffReturnsSubTasks.iterator().next().getLockerSubtaskDropOff();

        DropoffCargo dropoffCargo =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(barcodes.get(0)).orElseThrow();

        assertNotNull(lockerSubtaskDropOffReturn);
        assertEquals(dropoffCargo.getId(), lockerSubtaskDropOffReturn.getDropoffCargoId());
        assertEquals(DropoffCargoFlowStatus.ISSUED_FOR_CARRIAGE, dropoffCargo.getStatus());

        Movement updatedMovement = movementRepository.findByIdOrThrow(movementDropoffReturn.getId());
        assertEquals(MovementStatus.INITIALLY_CONFIRMED, updatedMovement.getStatus());

        Integer successPlaces = getSuccessPlaces(routePointListDto);
        assertEquals(1, successPlaces);
    }


    @SneakyThrows
    @Test
    void shouldAcceptScannedCargos_whenTransferActCancell() {
        //given
        initSeveralPvz();
        when(configurationProviderAdapter.isBooleanEnabled(MIGRATION_TO_NEW_ALGO_PICKUP_DROPOFF_RETURN_CARGOS_ENABLED))
                .thenReturn(true);
        userPropertyService.addPropertyToUser(user, UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, true);
        when(mockedTransferApi.transferIdDeleteWithHttpInfo(any())).thenReturn(ResponseEntity.ok().build());

        List<String> barcodes = List.of("barcode1", "barcode2");
        barcodes.forEach(testFlowFactory::addDropoffCargoReturn);

        testFlowFactory.addDropoffCargo("barcode2_1", LOGISTICPOINT_ID_SC, LOGISTICPOINT_ID_FOR_RETURN_DROPOFF2);
        testFlowFactory.addDropoffCargo("barcode2_2", LOGISTICPOINT_ID_SC, LOGISTICPOINT_ID_FOR_RETURN_DROPOFF2);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{task-id}/finish",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of(
                                        OrderScanTaskDto.OrderForScanDto.builder()
                                                .externalOrderId(barcodes.get(0))
                                                .build()

                                )
                        )
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(barcodes.get(1))
                                        .build(),
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(lockerOrder.getExternalOrderId())
                                        .build()
                        ))
                        .scannedOutsidePlaces(
                                Set.of(ScannedPlaceDto.builder()
                                        .orderExternalId(barcodes.get(0))
                                        .build())
                        )
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        //Ожидание результата АПП
        userShiftRepository.findByIdOrThrow(userShift.getId())
                .streamPickupTasks()
                .findFirst()
                .ifPresent(pickupTask -> pickupTask.getTransferActs().iterator().next().waitForSignature(
                        "transferExternalId"));


        //Отмена АПП
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/tasks/order-pickup/{task-id}/transfer-acts",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());


        //Досканирование
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{task-id}/finish",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of(
                                        OrderScanTaskDto.OrderForScanDto.builder()
                                                .externalOrderId(barcodes.get(0))
                                                .build()
                                )
                        )
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(barcodes.get(1))
                                        .build(),
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(lockerOrder.getExternalOrderId())
                                        .build()
                        ))
                        .scannedOutsidePlaces(
                                Set.of(ScannedPlaceDto.builder()
                                        .orderExternalId(barcodes.get(0))
                                        .build())
                        )
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        //Ожидание ответа АПП
        userShiftRepository.findByIdOrThrow(userShift.getId())
                .streamPickupTasks()
                .findFirst()
                .ifPresent(pickupTask -> pickupTask.getTransferActs().iterator().next().waitForSignature(
                        "transferExternalId2"));

        //подпись АПП (тригерит завершение процесса сканирования)
        userShiftCommandService.signTransferAct(user,
                new UserShiftCommand.SignTransferAct(
                        userShift.getId(),
                        userShift.getCurrentRoutePoint().getId(),
                        pickupTask.getId(),
                        pickupTask.getTransferActs().get(0).getId()
                ));

        //then
        UserShift updatedUserShift = userShiftRepository.findByIdOrThrow(userShift.getId());


        //Check LockerTask2 for cancell.
        RoutePoint routePointPvz2 = updatedUserShift
                .streamDeliveryRoutePoints()
                .filter(rp -> rp.streamLockerDeliveryTasks().anyMatch(ldt -> ldt.getPickupPointId()
                        .equals(createdEntityDto.getPickupPointId2())))
                .findFirst().orElseThrow();
        assertThat(routePointPvz2.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        LockerDeliveryTask lockerTask2 = routePointPvz2.streamTasks(LockerDeliveryTask.class).findAny().orElseThrow();
        assertThat(lockerTask2.getStatus()).isEqualTo(LockerDeliveryTaskStatus.CANCELLED);
        assertThat(lockerTask2.getSubtasks()).hasSize(1);
        LockerSubtask lockerSubtask2 = lockerTask2.getSubtasks().get(0);
        assertThat(lockerSubtask2.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(lockerSubtask2.getLockerSubtaskDropOff().getMovementId()).isEqualTo(createdEntityDto
                .getMovementDropoffReturn2().getId());


        //Check LockerTask1 is not cancell.
        RoutePoint routePointPvz1 = updatedUserShift
                .streamDeliveryRoutePoints()
                .filter(rp -> rp.streamLockerDeliveryTasks().anyMatch(ldt -> ldt.getPickupPointId()
                        .equals(createdEntityDto.getPickupPointId())))
                .findFirst().orElseThrow();
        assertThat(routePointPvz1.getStatus()).isEqualTo(RoutePointStatus.NOT_STARTED);
        LockerDeliveryTask lockerTask1 = routePointPvz1.streamTasks(LockerDeliveryTask.class).findAny().orElseThrow();
        assertThat(lockerTask1.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        assertThat(lockerTask1.getSubtasks()).hasSize(1);
        LockerSubtask lockerSubtask1 = lockerTask1.getSubtasks().get(0);
        assertThat(lockerSubtask1.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);
        assertThat(lockerSubtask1.getLockerSubtaskDropOff().getMovementId()).isEqualTo(createdEntityDto
                .getMovementDropoffReturn().getId());
    }

    @SneakyThrows
    @Test
    void shouldAcceptScannedCargos_whenTransferActCancell_withScanAddition() {
        //given
        initSeveralPvz();
        when(configurationProviderAdapter.isBooleanEnabled(MIGRATION_TO_NEW_ALGO_PICKUP_DROPOFF_RETURN_CARGOS_ENABLED)).thenReturn(true);
        userPropertyService.addPropertyToUser(user, UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, true);
        when(mockedTransferApi.transferIdDeleteWithHttpInfo(any())).thenReturn(ResponseEntity.ok().build());

        List<String> barcodes = List.of("barcode1", "barcode2");
        barcodes.forEach(testFlowFactory::addDropoffCargoReturn);

        String barcode21 = "barcode2_1";
        testFlowFactory.addDropoffCargo(barcode21, LOGISTICPOINT_ID_SC, LOGISTICPOINT_ID_FOR_RETURN_DROPOFF2);


        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{task-id}/finish",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of(
                                        OrderScanTaskDto.OrderForScanDto.builder()
                                                .externalOrderId(barcodes.get(0))
                                                .build()

                                )
                        )
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(barcodes.get(1))
                                        .build(),
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(lockerOrder.getExternalOrderId())
                                        .build()
                        ))
                        .scannedOutsidePlaces(
                                Set.of(ScannedPlaceDto.builder()
                                        .orderExternalId(barcodes.get(0))
                                        .build())
                        )
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        userShiftRepository.findByIdOrThrow(userShift.getId())
                .streamPickupTasks()
                .findFirst()
                .ifPresent(pickupTask -> pickupTask.getTransferActs().iterator().next().waitForSignature(
                        "transferExternalId"));


        mockMvc.perform(MockMvcRequestBuilders.delete("/api/tasks/order-pickup/{task-id}/transfer-acts",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());


        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{task-id}/finish",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of(
                                        OrderScanTaskDto.OrderForScanDto.builder()
                                                .externalOrderId(barcodes.get(0))
                                                .build()
                                )
                        )
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(barcodes.get(1))
                                        .build(),
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(lockerOrder.getExternalOrderId())
                                        .build()
                        ))
                        .scannedOutsidePlaces(
                                Set.of(ScannedPlaceDto.builder()
                                                .orderExternalId(barcodes.get(0))
                                                .build(),
                                        ScannedPlaceDto.builder()
                                                .orderExternalId(barcode21)
                                                .build()
                                )
                        )
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        userShiftRepository.findByIdOrThrow(userShift.getId())
                .streamPickupTasks()
                .findFirst()
                .ifPresent(pickupTask -> pickupTask.getTransferActs().iterator().next().waitForSignature(
                        "transferExternalId2"));

        userShiftCommandService.signTransferAct(user,
                new UserShiftCommand.SignTransferAct(
                        userShift.getId(),
                        userShift.getCurrentRoutePoint().getId(),
                        pickupTask.getId(),
                        pickupTask.getTransferActs().get(0).getId()
                ));

        //then
        UserShift updatedUserShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        //Check both not cancell.
        Set.of(Pair.of(createdEntityDto.getPickupPointId(), createdEntityDto.getMovementDropoffReturn()),
                        Pair.of(createdEntityDto.getPickupPointId2(), createdEntityDto.getMovementDropoffReturn2()))
                .forEach(pair -> {
                    var routePointPvz = updatedUserShift
                            .streamDeliveryRoutePoints()
                            .filter(rp -> rp.streamLockerDeliveryTasks().anyMatch(ldt -> ldt.getPickupPointId()
                                    .equals(pair.getLeft())))
                            .findFirst().orElseThrow();
                    assertThat(routePointPvz.getStatus()).isEqualTo(RoutePointStatus.NOT_STARTED);

                    LockerDeliveryTask lockerTask =
                            routePointPvz.streamTasks(LockerDeliveryTask.class).findAny().orElseThrow();
                    assertThat(lockerTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
                    assertThat(lockerTask.getSubtasks()).hasSize(1);

                    LockerSubtask lockerSubtask = lockerTask.getSubtasks().get(0);
                    assertThat(lockerSubtask.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);
                    assertThat(lockerSubtask.getLockerSubtaskDropOff().getMovementId()).isEqualTo(pair.getRight().getId());
                });
    }

    @SneakyThrows
    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void shouldUpdateMovementStatus_whenPartlyOrderAccept(boolean enablePickupAlgoMigration) {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(MIGRATION_TO_NEW_ALGO_PICKUP_DROPOFF_RETURN_CARGOS_ENABLED))
                .thenReturn(enablePickupAlgoMigration);
        initOnePvz();
        List<String> barcodes = List.of("barcode1", "barcode2");
        Set<Long> cargos =
                barcodes.stream().map(testFlowFactory::addDropoffCargoReturn)
                        .map(DropoffCargo::getId).collect(Collectors.toSet());

        var order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        var pickupOrdersCommand = new UserShiftCommand.FinishScan(
                userShift.getId(),
                pickupTask.getRoutePoint().getId(),
                pickupTask.getId(),
                ScanRequest.builder()
                        .successfullyScannedDropoffCargos(cargos)
                        .successfullyScannedOrders(List.of())
                        .skippedOrders(List.of(order.getId()))
                        .comment("")
                        .finishedAt(Instant.now(clock))
                        .build()
        );
        commandService.pickupOrders(user, pickupOrdersCommand);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{task-id}/finish-loading",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());


        //then
        Movement updatedMovement = movementRepository.findByIdOrThrow(movementDropoffReturn.getId());
        assertEquals(MovementStatus.TRANSPORTATION, updatedMovement.getStatus());
    }

    @SneakyThrows
    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void shouldAcceptAllScannedCargos_checkMovementStaus(boolean enablePickupAlgoMigration) {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(MIGRATION_TO_NEW_ALGO_PICKUP_DROPOFF_RETURN_CARGOS_ENABLED))
                .thenReturn(enablePickupAlgoMigration);
        initOnePvz();
        List<String> barcodes = List.of("barcode1", "barcode2");
        barcodes.forEach(testFlowFactory::addDropoffCargoReturn);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{task-id}/finish",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of(
                                        OrderScanTaskDto.OrderForScanDto.builder()
                                                .externalOrderId(barcodes.get(0))
                                                .build(),
                                        OrderScanTaskDto.OrderForScanDto.builder()
                                                .externalOrderId(barcodes.get(1))
                                                .build()
                                )
                        )
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(lockerOrder.getExternalOrderId())
                                        .build()))
                        .scannedOutsidePlaces(Set.of(
                                        ScannedPlaceDto.builder()
                                                .orderExternalId(barcodes.get(0))
                                                .build(),
                                        ScannedPlaceDto.builder()
                                                .orderExternalId(barcodes.get(1))
                                                .build()
                                )
                        )
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        RoutePointListDto routePointListDto = requestRoutPoints();

        commandService.finishLoading(
                user,
                new UserShiftCommand.FinishLoading(
                        userShift.getId(),
                        pickupTask.getRoutePoint().getId(),
                        pickupTask.getId()
                )
        );
        //then
        Movement updatedMovement = movementRepository.findByIdOrThrow(movementDropoffReturn.getId());
        assertEquals(MovementStatus.TRANSPORTATION, updatedMovement.getStatus());

        MovementHistoryEvent movementHistoryEvent =
                movementHistoryEventRepository.findByMovementId(updatedMovement.getId(), Pageable.unpaged())
                        .stream()
                        .filter(event -> event.getType() == MovementHistoryEventType.CARGO_RECEIVED)
                        .filter(event -> event.getMovementId().equals(updatedMovement.getId()))
                        .findFirst()
                        .orElseThrow();

        assertEquals(barcodes.get(0) + ", " + barcodes.get(1), movementHistoryEvent.getContext());

        Integer successPlaces = getSuccessPlaces(routePointListDto);
        assertEquals(2, successPlaces);
    }

    private RoutePointListDto requestRoutPoints() throws Exception {
        return ObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/route-points")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
                        .andReturn().getResponse().getContentAsString(),
                RoutePointListDto.class);
    }

    private Integer getSuccessPlaces(RoutePointListDto routePointListDto) {
        return routePointListDto
                .getRoutePoints()
                .stream()
                .filter(dto -> dto.getType().equals(RoutePointType.ORDER_PICKUP))
                .findFirst()
                .map(RoutePointSummaryDto::getItemCount)
                .map(RoutePointSummaryDto.ItemCount::getSuccess)
                .orElseThrow();
    }

    @SneakyThrows
    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void shouldFailAllTasksAndFinishRp_whenAllSkip(boolean enablePickupAlgoMigration) {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(MIGRATION_TO_NEW_ALGO_PICKUP_DROPOFF_RETURN_CARGOS_ENABLED))
                .thenReturn(enablePickupAlgoMigration);
        initOnePvz();
        List<String> barcodes = List.of("barcode1", "barcode2");
        barcodes.forEach(testFlowFactory::addDropoffCargoReturn);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{task-id}/finish",
                        pickupTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of())
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(barcodes.get(0))
                                        .build(),
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(barcodes.get(1))
                                        .build(),
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(lockerOrder.getExternalOrderId())
                                        .build()
                        ))
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        RoutePointListDto routePointListDto = requestRoutPoints();

        //then
        UserShift updatedUserShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        List<LockerSubtask> dropOffReturnsSubTasks = updatedUserShift
                .streamLockerDeliveryTasks()
                .flatMap(LockerDeliveryTask::streamDropOffReturnSubtasks)
                .collect(Collectors.toList());

        LockerSubtask lockerSubtaskDropOffReturn =
                dropOffReturnsSubTasks.iterator().next();
        assertThat(dropOffReturnsSubTasks).hasSize(1);

        assertEquals(LockerDeliverySubtaskStatus.FAILED, lockerSubtaskDropOffReturn.getStatus());
        assertEquals(LockerDeliveryTaskStatus.CANCELLED, lockerSubtaskDropOffReturn.getTask().getStatus());
        assertEquals(RoutePointStatus.FINISHED, lockerSubtaskDropOffReturn.getTask().getRoutePoint().getStatus());

        Movement updatedMovement = movementRepository.findByIdOrThrow(movementDropoffReturn.getId());
        assertEquals(MovementStatus.CANCELLED, updatedMovement.getStatus());

        Integer successPlaces = getSuccessPlaces(routePointListDto);
        assertEquals(0, successPlaces);
    }

    private void updateFields() {
        userShift = createdEntityDto.getUserShift();
        pickupTask = createdEntityDto.getPickupTask();
        movementDropoffReturn = createdEntityDto.getMovementDropoffReturn();
        user = createdEntityDto.getUser();
        lockerOrder = createdEntityDto.getLockerOrder();
    }
}
