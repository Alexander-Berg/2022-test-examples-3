package ru.yandex.market.tpl.api.controller.api.task;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnCreateDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.ScannedPlaceDto;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.juggler.JugglerPushClient;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("ALL")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OrderReturnTaskControllerClientReturnIntTest extends BaseApiIntTest {

    public static final String CLIENT_RETURN_EXTERNAL_ID_1 = "clientReturn1";
    public static final String CLIENT_RETURN_BARCODE_EXTERNAL_CREATED_1 = "VOZVRAT_SF_1";
    public static final String EXTERNAL_ORDER_ID = "123";
    private final UserPropertyService userPropertyService;
    private final LockerDeliveryService lockerDeliveryService;

    private final UserShiftCommandService commandService;
    private final TestUserHelper testUserHelper;
    private final JugglerPushClient mockedJugglerPushClient;
    private final TransactionTemplate transactionTemplate;
    private final ClientReturnService clientReturnService;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandDataHelper helper;
    private final ClientReturnRepository clientReturnRepository;

    private UserShift userShift;
    private long userShiftId;
    private long lockerTaskId;
    private long pickupTaskId;
    private long lockerTaskRoutePointId;
    private User user;

    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 123, DeliveryService.DEFAULT_DS_ID)
        );

        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId(EXTERNAL_ORDER_ID)
                .pickupPoint(pickupPoint)
                .build());
        UserShiftCommand.Create createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(helper.taskLockerDelivery(order.getId(), pickupPoint.getId()))
                .build();

        userShiftId = transactionTemplate.execute(status -> commandService.createUserShift(createCommand));

        pickupTaskId = transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            return userShift.streamPickupTasks().findFirst().orElseThrow().getId();
        });

        userShift = transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            testUserHelper.openShift(user, userShift.getId());
            testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            var pickupTask = userShift.streamPickupRoutePoints()
                    .findFirst().orElseThrow().streamPickupTasks().findFirst().orElseThrow();
            commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                    userShift.getId(), userShift.getCurrentRoutePoint().getId(), pickupTask.getId()
            ));
            finishPickupScan();
            finishPickupLoading();

            return userShift;
        });

        lockerTaskId = transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            LockerDeliveryTask lockerDeliveryTask = userShift.streamLockerDeliveryTasks().findFirst().orElseThrow();
            return lockerDeliveryTask.getId();
        });

        lockerTaskRoutePointId = transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            LockerDeliveryTask lockerDeliveryTask = userShift.streamLockerDeliveryTasks().findFirst().orElseThrow();
            return lockerDeliveryTask.getRoutePoint().getId();
        });

        ClientReturnCreateDto clientReturnCreateDto = ClientReturnCreateDto.builder()
                .returnId(CLIENT_RETURN_EXTERNAL_ID_1)
                .barcode(CLIENT_RETURN_BARCODE_EXTERNAL_CREATED_1)
                .pickupPointId(pickupPoint.getId())
                .logisticPointId(123L)
                .build();
        clientReturnService.create(clientReturnCreateDto);
        clientReturnService.receiveOnPvz(CLIENT_RETURN_EXTERNAL_ID_1);


    }

    @SneakyThrows
    @Test
    void shouldReturnClientReturn() {
        //given
        init();

        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            return null;
        });
        //finish loading
        finishLoadingLockerTask();
        finishUnLoadingLockerTask();
        //finish locker task

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
                        .completedOrders(List.of())
                        .scannedOutsidePlaces(Set.of(
                                ScannedPlaceDto.builder()
                                        .orderExternalId(CLIENT_RETURN_BARCODE_EXTERNAL_CREATED_1)
                                        .build()
                        ))
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());

        finishReturnTask(returnTask);

        finishUserShift();

        //then

        ClientReturnStatus clientReturnStatus =
                clientReturnRepository.findByBarcode(CLIENT_RETURN_BARCODE_EXTERNAL_CREATED_1).orElseThrow().getStatus();
        assertThat(clientReturnStatus).isEqualTo(ClientReturnStatus.DELIVERED_TO_SC);
    }


    @SneakyThrows

    private void finishReturnTask(OrderReturnTask returnTask) {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-return/{task-id}/task/finish",
                        returnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());
    }

    private void finishLoadingLockerTask() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-load",
                        lockerTaskRoutePointId,
                        lockerTaskId)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of())
                        .skippedOrders(List.of())
                        .scannedOutsidePlaces(Set.of())
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());
    }

    @SneakyThrows
    private void finishUnLoadingLockerTask() {
        OrderScanTaskDto.OrderForScanDto scanDto = new OrderScanTaskDto.OrderForScanDto();
        scanDto.setExternalOrderId(CLIENT_RETURN_BARCODE_EXTERNAL_CREATED_1);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/route-points/{routePointId}/tasks/locker-delivery/{taskId" +
                                "}/finish-unload",
                        lockerTaskRoutePointId,
                        lockerTaskId)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of(scanDto))
                        .build()))

        ).andExpect(status().isOk());
    }

    @SneakyThrows
    private void finishPickupScan() {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{taskId" +
                                "}/finish",
                        pickupTaskId)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of())
                        .skippedOrders(List.of())
                        .scannedOutsidePlaces(Set.of(
                                ScannedPlaceDto.builder()
                                        .orderExternalId(EXTERNAL_ORDER_ID)
                                        .barcode(EXTERNAL_ORDER_ID)
                                        .build()
                        ))
                        .comment("Not enough capacity")
                        .build()))

        ).andExpect(status().isOk());
    }

    @SneakyThrows
    private void finishPickupLoading() {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks/order-pickup/{taskId" +
                                "}/finish-loading",
                        pickupTaskId)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk());
    }

    @SneakyThrows
    private void finishUserShift() {
        commandService.finishUserShift(new UserShiftCommand.Finish(userShiftId));
    }

    private OrderReturnTask getOrderReturnTask(UserShift us) {
        return us.streamReturnRoutePoints().findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
    }

}
