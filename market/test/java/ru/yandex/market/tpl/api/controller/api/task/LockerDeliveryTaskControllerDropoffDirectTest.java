package ru.yandex.market.tpl.api.controller.api.task;

import java.util.List;
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
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType;
import ru.yandex.market.tpl.api.model.scanner.ScannerOrderDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.api.model.task.locker.delivery.LockerDeliveryCancelRequestDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.usershift.params.UserShiftParamsDto;
import ru.yandex.market.tpl.common.util.TplObjectMappers;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoFlowStatus;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
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
import static ru.yandex.market.tpl.api.controller.api.task.TestDropOffCourierFlowFactory.LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2;
import static ru.yandex.market.tpl.api.controller.api.task.TestDropOffCourierFlowFactory.LOGISTICPOINT_ID_SC;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.ORDERS_LOADED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CARGO_DROPOFF_DIRECT_FLOW_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DROPOFF_RETURN_SUPPORT_ENABLED;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LockerDeliveryTaskControllerDropoffDirectTest extends BaseApiIntTest {

    private final UserShiftRepository userShiftRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private final DropoffCargoRepository dropoffCargoRepository;
    private final TestDropOffCourierFlowFactory testFlowFactory;
    private final MovementHistoryEventRepository movementHistoryEventRepository;
    private final TestUserHelper testUserHelper;

    private UserShift userShift;
    private LockerDeliveryTask lockerTask;
    private Movement movementDropoff;
    private User user;
    private List<DropoffCargo> dropoffCargos = List.of();
    private final TransactionTemplate transactionTemplate;


    @BeforeEach
    void setUp() {
        when(configurationProviderAdapter.isBooleanEnabled(DROPOFF_RETURN_SUPPORT_ENABLED)).thenReturn(true);
        when(configurationProviderAdapter.isBooleanEnabled(CARGO_DROPOFF_DIRECT_FLOW_ENABLED)).thenReturn(true);
    }

    void init() {
        TestDropOffCourierFlowFactory.CreatedEntityDto createdEntityDto =
                testFlowFactory.initDirectFlowLockerTaskState();
        userShift = createdEntityDto.getUserShift();
        movementDropoff = createdEntityDto.getMovementDropoffDirect();
        user = createdEntityDto.getUser();
        lockerTask = createdEntityDto.getLockerTask();
        dropoffCargos = createdEntityDto.getSucceedScannedDirectCargos();


    }

    @SneakyThrows
    @Test
    void shouldCorrectCancel_whenSeveralDropoff_beforeUnloading() {
        //given
        var createdEntityDto = testFlowFactory.initDirectSeveralFlowLockerTaskState();

        DropoffCargo dropoff2 = testFlowFactory.addDropoffCargo("DROPOFF_2", LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2,
                LOGISTICPOINT_ID_SC);
        //when
        LockerDeliveryTaskDto taskDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/tasks/locker-delivery/{task-id}",
                                        createdEntityDto.getLockerTask().getId().toString())
                                .param("shouldShowAllOrders", "true")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), LockerDeliveryTaskDto.class);

        assertThat(taskDto.getOrders()).isNotEmpty();
        assertThat(taskDto.getLocker()).isNotNull();
        assertThat(taskDto.getId()).isNotNull();
        assertThat(taskDto.getName()).isNotNull();

        TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/shifts/{id}/params",
                                        createdEntityDto.getUserShift().getId().toString())
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), UserShiftParamsDto.class);


        lockerTask = createdEntityDto.getLockerTask();


        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/locker-delivery/{taskId}/cancel",
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(
                        new LockerDeliveryCancelRequestDto(OrderDeliveryTaskFailReasonType.PVZ_CLOSED, "", List.of())))

        ).andExpect(status().isOk());

        //then
        userShift = userShiftRepository.findByIdOrThrow(createdEntityDto.getUserShift().getId());
        assertThat(userShift.streamLockerDeliveryTasks()
                .filter(ldt -> ldt.getId().equals(lockerTask.getId()))
                .anyMatch(LockerDeliveryTask::isFailed)).isTrue();

        //Текущая точка - следующий ПВЗ
        assertThat(userShift.getCurrentRoutePoint().streamLockerDeliveryTasks()
                .anyMatch(task -> !task.isFailed() && task.getType() == TaskType.LOCKER_DELIVERY))
                .isTrue();
    }

    @SneakyThrows
    @Test
    void shouldCorrectReopen_whenSeveralDropoff() {
        //given
        var createdEntityDto = testFlowFactory.initDirectSeveralFlowLockerTaskState();

        DropoffCargo dropoff2 = testFlowFactory.addDropoffCargo("DROPOFF_2", LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2,
                LOGISTICPOINT_ID_SC);
        //when
        LockerDeliveryTaskDto taskDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/tasks/locker-delivery/{task-id}",
                                        createdEntityDto.getLockerTask().getId().toString())
                                .param("shouldShowAllOrders", "true")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), LockerDeliveryTaskDto.class);

        assertThat(taskDto.getOrders()).isNotEmpty();
        assertThat(taskDto.getLocker()).isNotNull();
        assertThat(taskDto.getId()).isNotNull();
        assertThat(taskDto.getName()).isNotNull();

        TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/shifts/{id}/params",
                                        createdEntityDto.getUserShift().getId().toString())
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), UserShiftParamsDto.class);


        lockerTask = createdEntityDto.getLockerTask();


        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/locker-delivery/{taskId}/cancel",
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(
                        new LockerDeliveryCancelRequestDto(OrderDeliveryTaskFailReasonType.PVZ_CLOSED, "", List.of())))

        ).andExpect(status().isOk());
        //when

        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/locker-delivery/{taskId}/reopen",
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());


        //then
        userShift = userShiftRepository.findByIdOrThrow(createdEntityDto.getUserShift().getId());

        //Текущая точка - следующий ПВЗ
        var lockerTask2 = userShift.getCurrentRoutePoint().streamLockerDeliveryTasks().findFirst().orElseThrow();
        //отмена второго пвз - едем на первый пвз
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/locker-delivery/{taskId}/cancel",
                        lockerTask2.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(
                        new LockerDeliveryCancelRequestDto(OrderDeliveryTaskFailReasonType.PVZ_CLOSED, "", List.of())))

        ).andExpect(status().isOk());

        //Возврат на возобновленный ПВЗ
        testUserHelper.arriveAtRoutePoint(lockerTask.getRoutePoint());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .build()))

        ).andExpect(status().isOk());


        List<DropoffCargo> succeedScannedDirectCargos = createdEntityDto.getSucceedScannedDirectCargos();
        assertThat(succeedScannedDirectCargos).hasSize(2);
        var barcodes = succeedScannedDirectCargos.stream().map(DropoffCargo::getBarcode).collect(Collectors.toList());

        ScannerOrderDto scannerOrderDto0 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes", barcodes.get(0), "ggg", "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);

        ScannerOrderDto scannerOrderDto1 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes", barcodes.get(1), "ggg", "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);

        //приемка
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

        //then
        List<LockerSubtask> dropOffReturnsSubTasks =
                transactionTemplate.execute(st -> userShiftRepository.findByIdOrThrow(userShift.getId())
                        .streamLockerDeliveryTasks()
                        .filter(task -> task.getId().equals(lockerTask.getId()))
                        .flatMap(LockerDeliveryTask::streamDropoffCargoSubtasks)
                        .collect(Collectors.toList()));
        assertThat(dropOffReturnsSubTasks).hasSize(2);
    }

    @SneakyThrows
    @Test
    void shouldCorrectReopen_whenReceivedSeveralDropoff() {
        //given
        var createdEntityDto = testFlowFactory.initDirectSeveralFlowLockerTaskState();

        DropoffCargo dropoff2 = testFlowFactory.addDropoffCargo("DROPOFF_2", LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2,
                LOGISTICPOINT_ID_SC);
        //when
        LockerDeliveryTaskDto taskDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/tasks/locker-delivery/{task-id}",
                                        createdEntityDto.getLockerTask().getId().toString())
                                .param("shouldShowAllOrders", "true")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), LockerDeliveryTaskDto.class);

        assertThat(taskDto.getOrders()).isNotEmpty();
        assertThat(taskDto.getLocker()).isNotNull();
        assertThat(taskDto.getId()).isNotNull();
        assertThat(taskDto.getName()).isNotNull();

        TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/shifts/{id}/params",
                                        createdEntityDto.getUserShift().getId().toString())
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), UserShiftParamsDto.class);


        lockerTask = createdEntityDto.getLockerTask();

        //Возврат на возобновленный ПВЗ
        testUserHelper.arriveAtRoutePoint(lockerTask.getRoutePoint());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .build()))

        ).andExpect(status().isOk());

        List<DropoffCargo> succeedScannedDirectCargos = createdEntityDto.getSucceedScannedDirectCargos();
        assertThat(succeedScannedDirectCargos).hasSize(2);
        var barcodes = succeedScannedDirectCargos.stream().map(DropoffCargo::getBarcode).collect(Collectors.toList());


        ScannerOrderDto scannerOrderDto0 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes", barcodes.get(0), "ggg", "hfghfg")
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
                        .completedOrders(List.of(scannerOrderDto0))
                        .build()))

        ).andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/locker-delivery/{taskId}/reopen",
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());


        //then
        userShift = userShiftRepository.findByIdOrThrow(createdEntityDto.getUserShift().getId());

        //Текущая точка - следующий ПВЗ
        var lockerTask2 = userShift.getCurrentRoutePoint().streamLockerDeliveryTasks().findFirst().orElseThrow();
        //отмена второго пвз - едем на первый пвз
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/locker-delivery/{taskId}/cancel",
                        lockerTask2.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(
                        new LockerDeliveryCancelRequestDto(OrderDeliveryTaskFailReasonType.PVZ_CLOSED, "", List.of())))

        ).andExpect(status().isOk());

        //Возврат на возобновленный ПВЗ
        testUserHelper.arriveAtRoutePoint(lockerTask.getRoutePoint());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .build()))

        ).andExpect(status().isOk());


        ScannerOrderDto scannerOrderDto1 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes", barcodes.get(1), "ggg", "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);

        //приемка
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-unload",
                        lockerTask.getRoutePoint().getId(),
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(PickupPointScanTaskRequestDto.builder()
                        .completedOrders(List.of(scannerOrderDto1))
                        .build()))

        ).andExpect(status().isOk());

        //then
        List<LockerSubtask> dropOffReturnsSubTasks =
                transactionTemplate.execute(st -> userShiftRepository.findByIdOrThrow(userShift.getId())
                        .streamLockerDeliveryTasks()
                        .filter(task -> task.getId().equals(lockerTask.getId()))
                        .flatMap(LockerDeliveryTask::streamDropoffCargoSubtasks)
                        .collect(Collectors.toList()));
        assertThat(dropOffReturnsSubTasks).hasSize(2);
    }

    @SneakyThrows
    @Test
    void shouldHaveStatusOrdersLoaded() {
        //given
        var createdEntityDto = testFlowFactory.initDirectSeveralFlowLockerTaskState();

        DropoffCargo dropoff2 = testFlowFactory.addDropoffCargo("DROPOFF_2", LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2,
                LOGISTICPOINT_ID_SC);
        //when
        LockerDeliveryTaskDto taskDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/tasks/locker-delivery/{task-id}",
                                        createdEntityDto.getLockerTask().getId().toString())
                                .param("shouldShowAllOrders", "true")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), LockerDeliveryTaskDto.class);

        assertThat(taskDto.getOrders()).isNotEmpty();
        assertThat(taskDto.getLocker()).isNotNull();
        assertThat(taskDto.getId()).isNotNull();
        assertThat(taskDto.getName()).isNotNull();

        TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/shifts/{id}/params",
                                        createdEntityDto.getUserShift().getId().toString())
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), UserShiftParamsDto.class);


        lockerTask = createdEntityDto.getLockerTask();

        assertThat(lockerTask.getStatus()).isNotEqualTo(ORDERS_LOADED);
        //Возврат на возобновленный ПВЗ
        testUserHelper.arriveAtRoutePoint(lockerTask.getRoutePoint());

        assertThat(lockerTask.getStatus()).isEqualTo(ORDERS_LOADED);
    }

    @SneakyThrows
    @Test
    void shouldCorrectCancel_beforeUnload() {
        //when
        init();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/locker-delivery/{taskId}/cancel",
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(
                        new LockerDeliveryCancelRequestDto(OrderDeliveryTaskFailReasonType.PVZ_CLOSED, "", List.of())))

        ).andExpect(status().isOk());

        //then
        List<LockerSubtask> dropOffSubTasks =
                transactionTemplate.execute(st -> userShiftRepository.findByIdOrThrow(userShift.getId())
                        .streamLockerDeliveryTasks()
                        .flatMap(LockerDeliveryTask::streamDropoffCargoSubtasks)
                        .collect(Collectors.toList()));
        assertThat(dropOffSubTasks).hasSize(1);

        assertThat(dropOffSubTasks.iterator().next().isFailed()).isTrue();

        assertThat(movementHistoryEventRepository.findByMovementId(movementDropoff.getId(), Pageable.unpaged())
                .stream()
                .anyMatch(event -> event.getType() == MovementHistoryEventType.MOVEMENT_CANCELLED)).isFalse();
    }

    @SneakyThrows
    @Test
    void shouldCorrectTasks_whenCancel() {

        init();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/locker-delivery/{taskId}/cancel",
                        lockerTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(
                        new LockerDeliveryCancelRequestDto(OrderDeliveryTaskFailReasonType.PVZ_CLOSED, "", List.of())))

        ).andExpect(status().isOk());

        //when
        OrderDeliveryTasksDto orderDeliveryTasksDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/tasks")
                                .param("terminal", "true")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)


                        ).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), OrderDeliveryTasksDto.class
        );

        //then
        List<LockerSubtask> dropOffSubTasks =
                transactionTemplate.execute(st -> userShiftRepository.findByIdOrThrow(userShift.getId())
                        .streamLockerDeliveryTasks()
                        .flatMap(LockerDeliveryTask::streamDropoffCargoSubtasks)
                        .collect(Collectors.toList()));
        assertThat(orderDeliveryTasksDto.getTasks()).hasSize(1);

        OrderSummaryDto summaryDto = orderDeliveryTasksDto.getTasks().iterator().next();

        assertThat(summaryDto.getType()).isEqualTo(TaskType.LOCKER_DELIVERY);
        assertThat(summaryDto.getOrderType()).isEqualTo(OrderType.PVZ);
        assertThat(summaryDto.getTaskStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
    }

    @SneakyThrows
    @Test
    void shouldUnloadingCargos() {
        //given
        init();
        List<String> barcodes = List.of("barcode1", "barcode2");

        //load cargo in pvz
        //Важно для перехода статуса
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
                                .param("barcodes", barcodes.get(0), "ggg", "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);

        ScannerOrderDto scannerOrderDto1 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/scanner/v2")
                                .param("barcodes", barcodes.get(1), "ggg", "hfghfg")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), ScannerOrderDto.class);


        //when
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

        //then
        List<LockerSubtask> dropOffReturnsSubTasks =
                transactionTemplate.execute(st -> userShiftRepository.findByIdOrThrow(userShift.getId())
                        .streamLockerDeliveryTasks()
                        .flatMap(LockerDeliveryTask::streamDropoffCargoSubtasks)
                        .collect(Collectors.toList()));
        assertThat(dropOffReturnsSubTasks).hasSize(2);

        //статусы после завершения сканирования забора посылок с ПВЗ
        Long dropoffCargoId0 = dropOffReturnsSubTasks.get(0).getLockerSubtaskDropOff().getDropoffCargoId();
        DropoffCargo dropoffCargo = dropoffCargoRepository.findByIdOrThrow(dropoffCargoId0);
        assertEquals(DropoffCargoFlowStatus.ISSUED_FOR_CARRIAGE, dropoffCargo.getStatus());

        Long dropoffCargoId1 = dropOffReturnsSubTasks.get(0).getLockerSubtaskDropOff().getDropoffCargoId();
        DropoffCargo dropoffCargo2 = dropoffCargoRepository.findByIdOrThrow(dropoffCargoId1);
        assertEquals(DropoffCargoFlowStatus.ISSUED_FOR_CARRIAGE, dropoffCargo2.getStatus());

        //Проверка истории
        movementHistoryEventRepository.findByMovementId(movementDropoff.getId(), Pageable.unpaged())
                .stream()
                .filter(event -> event.getType() == MovementHistoryEventType.CARGO_RECEIVED)
                .filter(event -> event.getMovementId().equals(movementDropoff.getId()))
                .findFirst()
                .orElseThrow();
    }
}
