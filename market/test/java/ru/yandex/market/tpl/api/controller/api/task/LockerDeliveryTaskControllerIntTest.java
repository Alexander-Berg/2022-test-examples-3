package ru.yandex.market.tpl.api.controller.api.task;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType;
import ru.yandex.market.tpl.api.model.scanner.ScannerOrderDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.ScannedPlaceDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointReturnReason;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointScanTaskRequestDto;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoFlowStatus;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DROPOFF_RETURN_SUPPORT_ENABLED;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LockerDeliveryTaskControllerIntTest extends BaseApiIntTest {

    private final UserShiftRepository userShiftRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private final DropoffCargoRepository dropoffCargoRepository;
    private final MovementRepository movementRepository;
    private final MovementHistoryEventRepository movementHistoryEventRepository;
    private final TestDropOffCourierFlowFactory testFlowFactory;

    private UserShift userShift;
    private LockerDeliveryTask lockerTask;
    private Movement movementDropoffReturn;
    private User user;
    private List<DropoffCargo> dropoffCargos = List.of();


    @BeforeEach
    void init() {

        TestDropOffCourierFlowFactory.CreatedEntityDto createdEntityDto =
                testFlowFactory.initReturnFlowLockerTaskState();
        userShift = createdEntityDto.getUserShift();
        movementDropoffReturn = createdEntityDto.getMovementDropoffReturn();
        user = createdEntityDto.getUser();
        lockerTask = createdEntityDto.getLockerTask();
        dropoffCargos = createdEntityDto.getSucceedScannedReturnCargos();

        when(configurationProviderAdapter.isBooleanEnabled(DROPOFF_RETURN_SUPPORT_ENABLED)).thenReturn(true);

    }

    @SneakyThrows
    @Test
    void shouldAcceptScannedCargos() {
        //given

        List<String> barcodes = List.of("barcode1", "barcode2");


        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
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
                                        .build()
                        ))
                        .scannedOutsidePlaces(Set.of(ScannedPlaceDto.builder()
                                .orderExternalId(barcodes.get(0))
                                .build()))
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        //then
        UserShift updatedUserShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        List<LockerSubtask> dropOffReturnsSubTasks = updatedUserShift
                .streamLockerDeliveryTasks()
                .flatMap(LockerDeliveryTask::streamDropOffReturnSubtasks)
                .collect(Collectors.toList());
        assertThat(dropOffReturnsSubTasks).hasSize(2);

        //статусы после завершения сканирования
        DropoffCargo dropoffCargo =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(barcodes.get(0)).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.DELIVERED_TO_LOGISTIC_POINT, dropoffCargo.getStatus());

        DropoffCargo dropoffCargo2 =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(barcodes.get(1)).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.ISSUED_FOR_CARRIAGE, dropoffCargo2.getStatus());

        Movement updatedMovement = movementRepository.findByIdOrThrow(movementDropoffReturn.getId());
        assertEquals(MovementStatus.DELIVERED, updatedMovement.getStatus());

        //Проверка истории
        MovementHistoryEvent movementHistoryEvent =
                movementHistoryEventRepository.findByMovementId(updatedMovement.getId(), Pageable.unpaged())
                        .stream()
                        .filter(event -> event.getType() == MovementHistoryEventType.CARGO_RECEIVED)
                        .filter(event -> event.getMovementId().equals(updatedMovement.getId()))
                        .findFirst()
                        .orElseThrow();

        assertThat(movementHistoryEvent.getContext()).contains(barcodes);
    }

    @SneakyThrows
    @Test
    void shouldUnloadingCargos() {
        //given
        List<String> barcodes = List.of("barcode1", "barcode2");

        //load cargo in pvz
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
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
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-unload",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(PickupPointScanTaskRequestDto.builder()
                        .completedOrders(List.of(
                                ScannerOrderDto.builder()
                                        .returnReason(PickupPointReturnReason.CELL_DID_NOT_OPEN)
                                        .externalOrderId(barcodes.get(0))
                                        .build(),
                                ScannerOrderDto.builder()
                                        .returnReason(PickupPointReturnReason.DIMENSIONS_EXCEEDS_LOCKER)
                                        .externalOrderId(barcodes.get(1))
                                        .build()
                        ))
                        .build()))

        ).andExpect(status().isOk());

        //then
        UserShift updatedUserShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        List<LockerSubtask> dropOffReturnsSubTasks = updatedUserShift
                .streamLockerDeliveryTasks()
                .flatMap(LockerDeliveryTask::streamDropOffReturnSubtasks)
                .collect(Collectors.toList());
        assertThat(dropOffReturnsSubTasks).hasSize(2);

        //статусы после завершения сканирования забора посылок с ПВЗ
        DropoffCargo dropoffCargo =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(barcodes.get(0)).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.ISSUED_FOR_CARRIAGE, dropoffCargo.getStatus());

        DropoffCargo dropoffCargo2 =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(barcodes.get(1)).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.ISSUED_FOR_CARRIAGE, dropoffCargo2.getStatus());
    }
}
