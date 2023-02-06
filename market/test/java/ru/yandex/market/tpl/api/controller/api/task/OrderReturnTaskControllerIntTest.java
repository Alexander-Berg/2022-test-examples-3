package ru.yandex.market.tpl.api.controller.api.task;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.ScannedPlaceDto;
import ru.yandex.market.tpl.api.model.task.locker.delivery.LockerDeliveryCancelRequestDto;
import ru.yandex.market.tpl.common.web.monitoring.juggler.model.JugglerStatus;
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
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.juggler.JugglerPushClient;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DROPOFF_RETURN_SUPPORT_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.SKIP_DIRECT_DROPOFF_SCAN_ON_SC_ENABLED;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OrderReturnTaskControllerIntTest extends BaseApiIntTest {

    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final UserPropertyService userPropertyService;
    private final LockerDeliveryService lockerDeliveryService;

    private final DropoffCargoRepository dropoffCargoRepository;
    private final UserShiftCommandService commandService;
    private final TestDropOffCourierFlowFactory testFlowFactory;
    private final TestUserHelper testUserHelper;
    private final JugglerPushClient mockedJugglerPushClient;
    private final TransactionTemplate transactionTemplate;
    private final MovementRepository movementRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;

    private UserShift userShift;
    private LockerDeliveryTask lockerTask;
    private Movement movementDropoffReturn;
    private User user;
    private List<DropoffCargo> dropoffCargos = List.of();


    void init(boolean skipDirectCargoScan) {
        setSkipProperty(skipDirectCargoScan);
        TestDropOffCourierFlowFactory.CreatedEntityDto createdEntityDto =
                testFlowFactory.initReturnFlowLockerTaskState();
        userShift = createdEntityDto.getUserShift();
        movementDropoffReturn = createdEntityDto.getMovementDropoffReturn();
        user = createdEntityDto.getUser();
        lockerTask = createdEntityDto.getLockerTask();
        dropoffCargos = createdEntityDto.getSucceedScannedReturnCargos();

        when(configurationProviderAdapter.isBooleanEnabled(DROPOFF_RETURN_SUPPORT_ENABLED)).thenReturn(true);

        Mockito.reset(mockedJugglerPushClient);
    }

    void initWithoutArrivingPvz(boolean skipDirectCargoScan) {
        setSkipProperty(skipDirectCargoScan);
        TestDropOffCourierFlowFactory.CreatedEntityDto createdEntityDto =
                testFlowFactory.initReturnFlowPickupTaskState();
        testFlowFactory.finishPickupProcess(createdEntityDto);
        dropoffCargos = createdEntityDto.getSucceedScannedReturnCargos();
        userShift = createdEntityDto.getUserShift();
        lockerTask = transactionTemplate.execute(status ->
                userShift.streamLockerDeliveryTasks()
                        .findFirst().orElseThrow());
        movementDropoffReturn = createdEntityDto.getMovementDropoffReturn();
        user = createdEntityDto.getUser();

        when(configurationProviderAdapter.isBooleanEnabled(DROPOFF_RETURN_SUPPORT_ENABLED)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(mockedJugglerPushClient);
        sortingCenterPropertyService.deletePropertyFromSortingCenter(
                sortingCenterRepository.findByIdOrThrow(userShift.getSortingCenter().getId()),
                SKIP_DIRECT_DROPOFF_SCAN_ON_SC_ENABLED
        );
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnCargo(boolean skipDirectCargoScan) {
        //given
        init(skipDirectCargoScan);
        //finish loading
        finishLoadingLockerTask();
        //finish locker task
        lockerDeliveryService.finishTask(lockerTask.getId(), null, user);


        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/finish",
                        returnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of(
                                        OrderScanTaskDto.OrderForScanDto.builder()
                                                .externalOrderId(dropoffCargos.get(1).getBarcode())
                                                .build()
                                )
                        )
                        .scannedOutsidePlaces(Set.of(
                                ScannedPlaceDto.builder()
                                        .orderExternalId(dropoffCargos.get(1).getBarcode())
                                        .build()
                        ))
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        finishReturnTask(returnTask);

        //then
        DropoffCargo dropoffCargo =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(dropoffCargos.get(0).getBarcode()).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.DELIVERED_TO_LOGISTIC_POINT, dropoffCargo.getStatus());

        DropoffCargo dropoffCargo2 =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(dropoffCargos.get(1).getBarcode()).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.CREATED, dropoffCargo2.getStatus());

        verify(mockedJugglerPushClient, never()).courierEvent(any(), any(), eq(JugglerStatus.CRIT));
    }


    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldNotifySkipped_whenReturn(boolean skipDirectCargoScan) {
        //given
        init(skipDirectCargoScan);

        //finish loading
        finishLoadingLockerTask();
        //finish locker task
        lockerDeliveryService.finishTask(lockerTask.getId(), null, user);


        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/finish",
                        returnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(dropoffCargos.get(1).getBarcode())
                                        .build()
                        ))
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        finishReturnTask(returnTask);

        //then
        DropoffCargo dropoffCargo =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(dropoffCargos.get(0).getBarcode()).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.DELIVERED_TO_LOGISTIC_POINT, dropoffCargo.getStatus());

        DropoffCargo dropoffCargo2 =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(dropoffCargos.get(1).getBarcode()).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.CREATED, dropoffCargo2.getStatus());

        Movement updatedMovement = movementRepository.findByIdOrThrow(movementDropoffReturn.getId());
        assertEquals(MovementStatus.DELIVERED, updatedMovement.getStatus());

        verify(mockedJugglerPushClient, times(1)).courierEvent(any(), any(), eq(JugglerStatus.CRIT));

    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCanceledMovement_whenReturn(boolean skipDirectCargoScan) {
        //given
        initWithoutArrivingPvz(skipDirectCargoScan);

        //cancell LockerTask without arriving
        cancelLockerTask();

        var returnTask = getOrderReturnTask(userShift);
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
        commandService.startOrderReturn(user,
                new UserShiftCommand.StartScan(userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                        returnTask.getId()));

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/finish",
                        returnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(dropoffCargos.get(1).getBarcode())
                                        .build()
                        ))
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        finishReturnTask(returnTask);

        //then
        Movement updatedMovement = movementRepository.findByIdOrThrow(movementDropoffReturn.getId());
        assertEquals(MovementStatus.CANCELLED, updatedMovement.getStatus());
    }

    private void finishReturnTask(OrderReturnTask returnTask) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/task/finish",
                        returnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());
    }

    private void finishLoadingLockerTask() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of(
                                        OrderScanTaskDto.OrderForScanDto.builder()
                                                .externalOrderId(dropoffCargos.get(0).getBarcode())
                                                .build()
                                )
                        )
                        .skippedOrders(List.of(
                                OrderScanTaskDto.OrderForScanDto.builder()
                                        .externalOrderId(dropoffCargos.get(1).getBarcode())
                                        .build()
                        ))
                        .scannedOutsidePlaces(Set.of(
                                ScannedPlaceDto.builder()
                                        .orderExternalId(dropoffCargos.get(0).getBarcode())
                                        .build()
                        ))
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());
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
