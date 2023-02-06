package ru.yandex.market.tpl.internal.service.billing;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnCreateDto;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnSystemCreated;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.internal.TplIntAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.OTHER;


@RequiredArgsConstructor
public class BillingServiceClientReturnTest extends TplIntAbstractTest {

    public static final List<String> CLIENT_RETURN_BARCODES =
            List.of(ClientReturn.CLIENT_RETURN_AT_ADDRESS_BARCODE_PREFIX + "123456");

    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final UserShiftCommandService userShiftCommandService;
    private final ClientReturnGenerator clientReturnGenerator;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftTestHelper userShiftTestHelper;
    private final ClientReturnService clientReturnService;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final UserShiftReassignManager userShiftReassignManager;
    private final TestUserHelper testUserHelper;
    private final BillingService billingService;
    private final TestUserHelper userHelper;
    private final ReturnsApi returnsApi;
    private final Clock clock;

    private ClientReturn clientReturn;
    private Shift shift;
    private Order order;
    private User user;


    @BeforeEach
    void setUp() {
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );
        user = userHelper.findOrCreateUser(216L);
        clientReturn = clientReturnGenerator.generateReturnFromClient();
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        order = orderGenerateService.createOrder();
    }

    @Test
    @DisplayName("Проверка, что в в биллинге учитывается успешно забранный возврат")
    void takenOrderCount_WhenClientReturnPickedUp() {
        var command = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.clientReturn("addrRet", 14, clientReturn.getId()))
                .build();

        var userShiftId = userShiftTestHelper.start(command);

        var task = transactionTemplate.execute(
                cmd -> userShiftRepository.findByIdOrThrow(userShiftId).streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow()
        );

        //начинаем и завершаем смену
        userHelper.finishPickupAtStartOfTheDay(userShiftId, true);
        pickupClientReturn(clientReturn, user, task);
        userHelper.finishFullReturnAtEnd(userShiftId);
        userHelper.finishUserShift(userShiftId);

        //проверяем что биллинг получил правильную дто
        var billingShiftDto = billingService.findShifts(LocalDate.now(clock));
        assertThat(billingShiftDto.getUserShifts()).hasSize(1);

        var billingUs = billingShiftDto.getUserShifts().get(0);
        assertThat(billingUs.getTakenOrderCount()).isEqualTo(1);

        var billingOrderDto = billingService.findOrders(Set.of(userShiftId));
        assertThat(billingOrderDto.getOrders()).hasSize(1);

        assertThat(billingUs.getTakenOrderCount()).isEqualTo(1);

    }

    @Test
    @DisplayName("Проверка, что в в биллинге не учитывается возврат из постамата")
    void shouldNotCountLockerReturn() {
        transactionTemplate.execute(ts -> {
            var clientReturnBarcodeExternalCreated1 = barcodePrefixRepository.findBarcodePrefixByName(
                    "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
            PickupPoint pickupPoint = pickupPointRepository.save(
                    testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
            GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

           var userShift = userShiftRepository
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
            var routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
            var lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            assertThat(lockerDeliveryTask.getSubtasks())
                    .hasSize(1);

            ClientReturnCreateDto clientReturnCreateDto = new ClientReturnCreateDto();
            clientReturnCreateDto.setReturnId("1");
            clientReturnCreateDto.setBarcode(clientReturnBarcodeExternalCreated1);
            clientReturnCreateDto.setPickupPointId(pickupPoint.getId());
            clientReturnCreateDto.setLogisticPointId(pickupPoint.getLogisticPointId());
            clientReturnCreateDto.setSystemCreated(ClientReturnSystemCreated.LRM);
            clientReturnService.create(clientReturnCreateDto);
            clientReturnService.receiveOnPvz("1");
            testUserHelper.arriveAtRoutePoint(routePoint);

            userShiftCommandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(),
                            new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "help me!"),
                            ScanRequest.builder()
                                    .successfullyScannedOrders(List.of(order.getId()))
                                    .build()));

            Set<UnloadedOrder> unloadedOrders = Set.of(

                    new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()));

            userShiftCommandService.finishUnloadingLocker(user,
                    new UserShiftCommand.FinishUnloadingLocker(
                            routePoint.getUserShift().getId(),
                            routePoint.getId(),
                            lockerDeliveryTask.getId(),
                            unloadedOrders
                    ));

            var billingShiftDto = billingService.findShifts(LocalDate.now(clock));
            assertThat(billingShiftDto.getUserShifts()).hasSize(1);

            var billingOrderDto = billingService.findOrders(Set.of(userShift.getId()));
            assertThat(billingOrderDto.getOrders()).hasSize(1);
            return null;
        });

    }

    @DisplayName("Проверка, что при неуспешном возврате на точке, счетчик увеличивается в " +
            "зависиомтси от причины")
    @ParameterizedTest
    @MethodSource("failReasonToCount")
    void shiftCount_WhenClientReturnFailed(OrderDeliveryTaskFailReasonType failReasonType, int failedTaskCount) {
        var command = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.clientReturn("addrRet", 14, clientReturn.getId()))
                .build();

        var userShiftId = userShiftTestHelper.start(command);

        //начинаем и завершаем смену
        userHelper.finishPickupAtStartOfTheDay(userShiftId, true);
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
                    var task =
                            userShift.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow();
                    var currRp = userShift.getCurrentRoutePoint();
                    userShiftCommandService.arriveAtRoutePoint(user,
                            new UserShiftCommand.ArriveAtRoutePoint(
                                    userShift.getId(),
                                    currRp.getId(),
                                    userShiftCommandDataHelper.getLocationDto(userShift.getId())
                            ));
                    userShiftCommandService.failDeliveryTask(user,
                            new UserShiftCommand.FailOrderDeliveryTask(
                                    userShift.getId(), currRp.getId(), task.getId(),
                                    new OrderDeliveryFailReason(
                                            new OrderDeliveryFailReasonDto(failReasonType, "NOT_SUPPORTED_YET",
                                                    Source.SYSTEM),
                                            Source.SYSTEM
                                    )
                            ));
                }
        );

        userHelper.finishFullReturnAtEnd(userShiftId);
        userHelper.finishUserShift(userShiftId);

        //проверяем, что таска провалилась
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var us = userShiftRepository.findByIdOrThrow(userShiftId);
                    var crTask =
                            us.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow();
                    assertThat(crTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
                }
        );

        //проверяем что биллинг получил правильную дто
        var billingDto = billingService.findShifts(LocalDate.now(clock));
        assertThat(billingDto.getUserShifts()).hasSize(1);

        var billingUs = billingDto.getUserShifts().get(0);
        assertThat(billingUs.getTakenOrderCount()).isEqualTo(failedTaskCount);
    }

    @Test
    @DisplayName("Проверка, что при успешном возврате и заказе на одной точке, счетчик учитывает оба")
    void takenOrderCount_WhenOrderAndClientReturnSuccess() {
        var userShift = userHelper.createEmptyShift(user, shift);
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var orderTask = getOrderTask(userShift, clientReturn);
        getClientReturnTask(userShift, orderTask, clientReturn, deliveryTime);

        //начинаем и завершаем смену
        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var currentRp = userShiftRepository.findByIdOrThrow(userShift.getId()).getCurrentRoutePoint();
                    userHelper.finishDelivery(currentRp, false);
                }
        );
        userHelper.finishFullReturnAtEnd(userShift.getId());
        userHelper.finishUserShift(userShift);

        //проверяем что возврат и доставка в нужных статусах
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var us = userShiftRepository.findByIdOrThrow(userShift.getId());
                    var orderTaskDelivered =
                            us.streamOrderDeliveryTasks().remove(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow();
                    var crTask =
                            us.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow();

                    assertThat(orderTaskDelivered.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
                    assertThat(crTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
                }
        );

        //проверяем что биллинг получил правильную дто
        var billingDto = billingService.findShifts(LocalDate.now(clock));
        assertThat(billingDto.getUserShifts()).hasSize(1);

        var billingUs = billingDto.getUserShifts().get(0);
        assertThat(billingUs.getTakenOrderCount()).isEqualTo(2);
    }

    @DisplayName("Проверка, что при заказе и возврате на точке и при неуспешном возврате, счетчик увеличивается в " +
            "зависиомтси от причины")
    @ParameterizedTest
    @MethodSource("failReasonToCount")
    void test3(OrderDeliveryTaskFailReasonType failReasonType, int failedTaskCount) {
        var userShift = userHelper.createEmptyShift(user, shift);
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var orderTask = getOrderTask(userShift, clientReturn);
        var clientReturnTask = getClientReturnTask(userShift, orderTask, clientReturn, deliveryTime);

        //начинаем и завершаем смену
        userHelper.openShiftAndFinishPickupAtStartOfDay(user, userShift.getId());

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var us = userShiftRepository.findByIdOrThrow(userShift.getId());
                    var currentRp = userShiftRepository.findByIdOrThrow(userShift.getId()).getCurrentRoutePoint();
                    userHelper.arriveAtRpAndfinishOrderDeliveryTask(user, userShift.getId(), currentRp.getId(),
                            orderTask, order);

                    assertThat(us.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow().getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

                    userShiftCommandService.failDeliveryTask(user,
                            new UserShiftCommand.FailOrderDeliveryTask(
                                    userShift.getId(), currentRp.getId(), clientReturnTask.getId(),
                                    new OrderDeliveryFailReason(
                                            new OrderDeliveryFailReasonDto(failReasonType, "NOT_SUPPORTED_YET",
                                                    Source.SYSTEM),
                                            Source.SYSTEM
                                    )
                            ));
                }
        );

        userHelper.finishFullReturnAtEnd(userShift.getId());
        userHelper.finishUserShift(userShift);

        //проверяем что возврат и доставка в нужных статусах
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var us = userShiftRepository.findByIdOrThrow(userShift.getId());
                    var orderTaskDelivered =
                            us.streamOrderDeliveryTasks().remove(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow();
                    var crTask =
                            us.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow();

                    assertThat(orderTaskDelivered.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
                    assertThat(crTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
                }
        );

        //проверяем что биллинг получил правильную дто
        var billingDto = billingService.findShifts(LocalDate.now(clock));
        assertThat(billingDto.getUserShifts()).hasSize(1);

        var billingUs = billingDto.getUserShifts().get(0);
        assertThat(billingUs.getTakenOrderCount()).isEqualTo(1 + failedTaskCount);
    }

    private OrderDeliveryTask getClientReturnTask(UserShift userShift, DeliveryTask orderTask,
                                                  ClientReturn clientReturn, Instant deliveryTime) {
        return userShiftCommandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), orderTask.getRoutePoint().getId(), clientReturn.getId(), deliveryTime
                )
        );
    }

    private void pickupClientReturn(ClientReturn clientReturn, User user, OrderDeliveryTask task) {
        clientReturnService.assignBarcodeAndFinishTask(CLIENT_RETURN_BARCODES, Map.of(),
                clientReturn.getExternalReturnId(), user,
                task.getId()
        );
    }

    private DeliveryTask getOrderTask(UserShift userShift, ClientReturn clientReturn) {
        return userShiftCommandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(
                        userShift.getId(),
                        userShiftCommandDataHelper.taskPrepaid(clientReturn.getLogisticRequestPointFrom().getAddress(), 15, order.getId()),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                ));
    }

    private static Stream<Arguments> failReasonToCount() {
        return Stream.of(
                Arguments.of(ORDER_NOT_ACCEPTED, 1),
                Arguments.of(OTHER, 1)
        );
    }
}
