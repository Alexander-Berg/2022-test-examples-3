package ru.yandex.market.tpl.core.domain.clientreturn;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnCreateDto;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnSystemCreated;
import ru.yandex.market.tpl.api.model.order.clientreturn.PartnerClientReturnDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.partner.OrderGeneralizedStatus;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderParamsDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.task.LockerSubtaskType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.order.PartnerReportOrderService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType.CLIENT_RETURN;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.FINISHED;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.NOT_STARTED;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.ON_TASK;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.SHIFT_CLOSED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CANCEL_ORDER;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.CANCELLED;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.READY_FOR_RECEIVED;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.RECEIVED;

@RequiredArgsConstructor
public class ClientReturnExternalSourceCreatedTest extends TplAbstractTest {

    private static final String CLIENT_RETURN_EXTERNAL_ID_1 = "EXTERNAL_RETURN_ID_1";


    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandService userShiftCommandService;
    private final ClientReturnService clientReturnService;
    private final ClientReturnRepository clientReturnRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftQueryService userShiftQueryService;
    private final PartnerReportOrderService partnerReportOrderService;
    private final ClientReturnCommandService clientReturnCommandService;
    private final ClientReturnQueryService clientReturnQueryService;
    private final TransactionTemplate transactionTemplate;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    private User user;
    private Shift shift;
    private UserShift userShift;
    private PickupPoint pickupPoint;
    private Order order;
    private LockerDeliveryTask lockerDeliveryTask;
    private RoutePoint routePoint;
    private String clientReturnBarcodeExternalCreated1;


    @BeforeEach
    void init() {
        transactionTemplate.execute(ts -> {
            clientReturnBarcodeExternalCreated1 = barcodePrefixRepository.findBarcodePrefixByName(
                    "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
            configurationServiceAdapter.insertValue(ConfigurationProperties.REOPEN_ORDER_RETURN_TASK_ENABLED, true);
            user = testUserHelper.findOrCreateUser(1L);
            shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                    sortingCenterService.findSortCenterForDs(239).getId());
            PickupPoint pickupPoint = pickupPointRepository.save(
                    testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
            GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

            userShift = userShiftRepository
                    .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
            order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .externalOrderId("111")
                    .deliveryDate(LocalDate.now(clock))
                    .deliveryServiceId(239L)
                    .pickupPoint(pickupPoint)
                    .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                            .geoPoint(geoPoint)
                            .build())
                    .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                    .build());
            userShiftReassignManager.assign(userShift, order);

            testUserHelper.checkinAndFinishPickup(userShift);
            routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
            lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            assertThat(lockerDeliveryTask.getSubtasks())
                    .hasSize(1);

            LockerDeliveryTaskDto lockerDeliveryTaskDto = getLockerDeliveryTaskFirst();
            assertThat(lockerDeliveryTaskDto.getClientReturns()).hasSize(0);
            assertThat(lockerDeliveryTaskDto.getCompletedClientReturns()).hasSize(0);

            ClientReturnCreateDto clientReturnCreateDto = new ClientReturnCreateDto();
            clientReturnCreateDto.setReturnId(CLIENT_RETURN_EXTERNAL_ID_1);
            clientReturnCreateDto.setBarcode(clientReturnBarcodeExternalCreated1);
            clientReturnCreateDto.setPickupPointId(pickupPoint.getId());
            clientReturnCreateDto.setLogisticPointId(pickupPoint.getLogisticPointId());
            clientReturnCreateDto.setSystemCreated(ClientReturnSystemCreated.LRM);
            clientReturnService.create(clientReturnCreateDto);
            clientReturnService.receiveOnPvz(CLIENT_RETURN_EXTERNAL_ID_1);
            finishLoadingLocker();
            return null;
        });
    }

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

    @Test
    void testGetPartnerClientReturnDto() {
        PartnerClientReturnDto clientReturnInfo =
                clientReturnQueryService.getClientReturnInfo(CLIENT_RETURN_EXTERNAL_ID_1);

        assertThat(clientReturnInfo.getDetails().getExternalReturnId()).isEqualTo(CLIENT_RETURN_EXTERNAL_ID_1);
        assertThat(clientReturnInfo.getDetails().getBarcode()).isEqualTo(clientReturnBarcodeExternalCreated1);
        assertThat(clientReturnInfo.getDetails().getStatus()).isNotBlank();
        assertThat(clientReturnInfo.getDetails().getType()).isNotBlank();
    }

    @Test
    @Transactional
    void scanAndReceiveOneExternalCreatedClientReturn() {
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()));

        receiveClientReturns(unloadedOrders);

        assertThat(lockerDeliveryTask.getSubtasks()).hasSize(2);

        lockerDeliveryTask.getSubtasks().stream()
                .filter(lockerSubtask -> lockerSubtask.getType() == LockerSubtaskType.PICKUP_CLIENT_RETURN)
                .forEach(lockerSubtask -> {
                    assertThat(lockerSubtask.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
                    assertThat(lockerSubtask.getFinishedAt()).isNotNull();
                });

        List<ClientReturn> clientReturns = clientReturnRepository.findAllByBarcodeIn(List.of(
                clientReturnBarcodeExternalCreated1
        ));

        assertThat(clientReturns).hasSize(1);
        clientReturns.forEach(clientReturn -> assertThat(clientReturn.getStatus()).isEqualTo(RECEIVED));
    }

    @Test
    @Transactional
    void testUserShiftClosed() {
        scanAndReceiveOneExternalCreatedClientReturn();

        RoutePoint returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        testUserHelper.arriveAtRoutePoint(returnRoutePoint);

        OrderReturnTask orderReturnTask = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        assertThat(orderReturnTask.getStatus()).isEqualTo(OrderReturnTaskStatus.NOT_STARTED);

        userShiftCommandService.startOrderReturn(
                user,
                new UserShiftCommand.StartScan(
                        userShift.getId(),
                        returnRoutePoint.getId(),
                        orderReturnTask.getId()));

        userShiftCommandService.finishReturnOrders(
                user,
                new UserShiftCommand.FinishScan(
                        userShift.getId(),
                        returnRoutePoint.getId(),
                        orderReturnTask.getId(),
                        ScanRequest.builder()
                                .successfullyScannedClientReturns(clientReturnRepository.findAllByBarcodeIn(List.of(
                                        clientReturnBarcodeExternalCreated1
                                )).stream().map(ClientReturn::getId).collect(Collectors.toList()))
                                .build()
                )
        );

        userShiftCommandService.finishReturnTask(
                userShift.getUser(),
                new UserShiftCommand.FinishReturnTask(
                        userShift.getId(),
                        returnRoutePoint.getId(),
                        orderReturnTask.getId()));

        assertThat(userShift.getStatus()).isEqualTo(SHIFT_CLOSED);
        assertThat(userShift.streamReturnRoutePoints().collect(Collectors.toList())).hasSize(1);
        userShift.streamReturnRoutePoints()
                .forEach(routePoint -> assertThat(routePoint.getStatus()).isEqualTo(FINISHED));
        userShift.streamReturnRoutePoints()
                .forEach(routePoint ->
                        routePoint.streamReturnTasks()
                                .forEach(orderReturnTask1 ->
                                        assertThat(orderReturnTask1.getStatus()).isEqualTo(OrderReturnTaskStatus.FINISHED)));

    }

    @Test
    @Transactional
    void whenReopenDeliveryTaskThanReopenReturnOrderTask() {
        testUserShiftClosed();

        userShiftCommandService.reopenDeliveryTask(null, new UserShiftCommand.ReopenOrderDeliveryTask(
                lockerDeliveryTask.getRoutePoint().getUserShift().getId(),
                lockerDeliveryTask.getRoutePoint().getId(),
                lockerDeliveryTask.getId(),
                Source.COURIER
        ));

        assertThat(userShift.getStatus()).isEqualTo(ON_TASK);
        assertThat(userShift.streamReturnRoutePoints().collect(Collectors.toList())).hasSize(1);
        userShift.streamReturnRoutePoints()
                .forEach(routePoint -> assertThat(routePoint.getStatus()).isEqualTo(NOT_STARTED));
        userShift.streamReturnRoutePoints()
                .forEach(routePoint ->
                        routePoint.streamReturnTasks()
                                .forEach(orderReturnTask1 ->
                                        assertThat(orderReturnTask1.getStatus())
                                                .isEqualTo(OrderReturnTaskStatus.NOT_STARTED)));
        assertThat(userShift.getCurrentRoutePoint().getId()).isEqualTo(routePoint.getId());
    }

    @Test
    @Transactional
    void cancelExternalCreatedClientReturn() {
        clientReturnService.cancelLockerClientReturn(CLIENT_RETURN_EXTERNAL_ID_1);

        List<ClientReturn> clientReturns =
                clientReturnRepository.findAllByBarcodeIn(List.of(clientReturnBarcodeExternalCreated1));

        assertThat(clientReturns).hasSize(1);
        clientReturns.forEach(clientReturn -> assertThat(clientReturn.getStatus()).isEqualTo(CANCELLED));
        assertThat(lockerDeliveryTask.getSubtasks()).hasSize(1);
    }

    @Test
    void throwTryCancelWhenAlreadyReceivedClientReturn() {
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()));

        receiveClientReturns(unloadedOrders);

        assertThrows(CommandFailedException.class,
                () -> clientReturnService.cancelLockerClientReturn(CLIENT_RETURN_EXTERNAL_ID_1));
    }

    @Test
    void reopenLockerTask() {
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()));

        receiveClientReturns(unloadedOrders);

        List<ClientReturn> clientReturns = clientReturnRepository.findAllByBarcodeIn(
                List.of(clientReturnBarcodeExternalCreated1)
        );
        assertThat(clientReturns).hasSize(1);
        clientReturns.forEach(clientReturn -> assertThat(clientReturn.getStatus()).isEqualTo(RECEIVED));

        userShiftCommandService.reopenDeliveryTask(null, new UserShiftCommand.ReopenOrderDeliveryTask(
                lockerDeliveryTask.getRoutePoint().getUserShift().getId(),
                lockerDeliveryTask.getRoutePoint().getId(),
                lockerDeliveryTask.getId(),
                Source.COURIER
        ));

        clientReturns = clientReturnRepository.findAllByBarcodeIn(
                List.of(clientReturnBarcodeExternalCreated1)
        );

        assertThat(clientReturns).hasSize(1);
        clientReturns.forEach(clientReturn -> assertThat(clientReturn.getStatus()).isEqualTo(READY_FOR_RECEIVED));
    }

    @Test
    @Transactional
    void failLockerDeliveryTask() {
        userShiftCommandService.failDeliveryTask(user,
                new UserShiftCommand.FailOrderDeliveryTask(
                        userShift.getId(), routePoint.getId(), lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(CANCEL_ORDER, "")));

        assertThat(lockerDeliveryTask.getSubtasks()).hasSize(1);
        ClientReturn clientReturn =
                clientReturnRepository.findByExternalReturnIdOrThrow(CLIENT_RETURN_EXTERNAL_ID_1);

        assertThat(clientReturn.getStatus()).isEqualTo(READY_FOR_RECEIVED);
    }

    @Test
    void findClientReturnByStatus() {
        assertThat(clientReturnRepository.findByStatus(READY_FOR_RECEIVED)).hasSize(1);
    }

    @Test
    void viewClientReturnOnOrdersTable() {
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()));
        receiveClientReturns(unloadedOrders);

        var params = new PartnerReportOrderParamsDto();
        params.setOrderId(clientReturnBarcodeExternalCreated1);
        params.setOrderTypes(Set.of(CLIENT_RETURN));
        List<PartnerReportOrderDto> partnerReportOrderDtos = partnerReportOrderService.findAll(params);

        assertThat(partnerReportOrderDtos).hasSize(1);
        PartnerReportOrderDto partnerReportOrderDto = partnerReportOrderDtos.get(0);

        assertThat(partnerReportOrderDto.getOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(partnerReportOrderDto.getGeneralizedTaskStatus()).isEqualTo("DONE");
        assertThat(partnerReportOrderDto.getGeneralizedTaskStatusLocalize())
                .isEqualTo(OrderGeneralizedStatus.DONE.getDescription());
    }

    @Test
    void viewClientReturnOnOrdersTable_skipEmptyFilterProperties() {
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()));

        receiveClientReturns(unloadedOrders);
        //given
        var params = new PartnerReportOrderParamsDto();
        params.setOrderId(clientReturnBarcodeExternalCreated1);
        params.setMultiOrderId("");
        params.setPlaceBarcode("");

        //when
        List<PartnerReportOrderDto> partnerReportOrderDtos = partnerReportOrderService.findAll(params);

        //then
        assertThat(partnerReportOrderDtos).hasSize(1);
        assertThat(partnerReportOrderDtos.get(0).getTaskStatus()).isNotBlank();
    }

    @Test
    @Transactional
    void whenCourierSkipClientReturnAssignedToIt() {
        Set<UnloadedOrder> unloadedOrders = Set.of();
        ClientReturn clientReturn =
                clientReturnRepository.findByBarcode(clientReturnBarcodeExternalCreated1).get();
        assertThat(clientReturn.getStatus()).isEqualTo(READY_FOR_RECEIVED);

        receiveClientReturns(unloadedOrders);

        assertThat(lockerDeliveryTask.getSubtasks()).hasSize(1);
        assertThat(lockerDeliveryTask.streamClientReturnSubtasks().collect(Collectors.toList())).hasSize(0);
        lockerDeliveryTask.streamClientReturnSubtasks()
                .forEach(lockerSubtask ->
                        assertThat(lockerSubtask.getStatus())
                                .isEqualTo(LockerDeliverySubtaskStatus.FINISHED)
                );
        assertThat(clientReturn.getStatus()).isEqualTo(READY_FOR_RECEIVED);
    }

    @Test
    @Transactional
    void findClientReturnWithoutSubtaskAndTryReassign() {
        ClientReturn clientReturn = createClientReturnWithoutSubtask();
        clientReturn.readyForReceived(ClientReturnCommand.ReadyForReceived.builder().build());
        clientReturn.assignedToCourier(ClientReturnCommand.AssignedToCourier.builder().build());

        List<ClientReturn> clientReturnsAssignToCourierAndNotExistsSubtask =
                clientReturnRepository.findAllWhereClientReturnAssignToCourierAndNotExistsSubtask();
        assertThat(clientReturnsAssignToCourierAndNotExistsSubtask).hasSize(1);

        clientReturnService.reassignClientReturnsWithoutSubtasks();

        assertThat(clientReturnRepository.findAllWhereClientReturnAssignToCourierAndNotExistsSubtask()).hasSize(0);
        assertThat(clientReturnRepository.findByIdOrThrow(clientReturn.getId()).getStatus()).isEqualTo(READY_FOR_RECEIVED);
    }

    @Test
    void createClientReturnWithIncorrectBarcodeMask() {
        ClientReturnCreateDto clientReturnCreateDto = new ClientReturnCreateDto();
        clientReturnCreateDto.setReturnId("some_return_id_1");
        clientReturnCreateDto.setBarcode("some_incorrect_mask_1");
        clientReturnCreateDto.setPickupPointId(31L);
        clientReturnCreateDto.setLogisticPointId(111L);

        assertThatThrownBy(() -> clientReturnService.create(clientReturnCreateDto))
                .isInstanceOf(TplInvalidParameterException.class)
                .hasMessageContaining("Client return has incorrect barcode mask:");
    }

    private ClientReturn createClientReturnWithoutSubtask() {
        return clientReturnCommandService.create(ClientReturnCommand.Create.builder()
                .barcode("barcode")
                .pickupPoint(pickupPoint)
                .returnId("returnId")
                .createdSource(CreatedSource.EXTERNAL)
                .source(Source.SYSTEM)
                .build()
        );
    }

    private void finishLoadingLocker() {
        testUserHelper.arriveAtRoutePoint(routePoint);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "help me!"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of(order.getId()))
                                .build()));
    }

    private void receiveClientReturns(Set<UnloadedOrder> unloadedOrders) {
        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        unloadedOrders
                ));
    }

    private List<LockerDeliveryTaskDto> getLockerDeliveryTasks() {
        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, routePoint.getId());

        return routePointInfo.getTasks().stream()
                .filter(taskDto -> taskDto instanceof LockerDeliveryTaskDto)
                .map(taskDto -> (LockerDeliveryTaskDto) taskDto)
                .collect(Collectors.toList());
    }

    private LockerDeliveryTaskDto getLockerDeliveryTaskFirst() {
        List<LockerDeliveryTaskDto> lockerDeliveryTasks = getLockerDeliveryTasks();
        assertThat(lockerDeliveryTasks).hasSize(1);
        return lockerDeliveryTasks.get(0);
    }
}
