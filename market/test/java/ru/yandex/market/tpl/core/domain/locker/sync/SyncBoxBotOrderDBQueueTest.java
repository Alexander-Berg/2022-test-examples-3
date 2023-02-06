package ru.yandex.market.tpl.core.domain.locker.sync;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.api.model.locker.boxbot.BoxBotActorType;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.BoxBotOrderStatusDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.BoxBotOrdersStatusDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.db.queue.log.QueueLog;
import ru.yandex.market.tpl.common.db.queue.log.QueueLogEvent;
import ru.yandex.market.tpl.common.db.queue.log.QueueLogRepository;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.boxbot.LockerApi;
import ru.yandex.market.tpl.core.service.user.assignment.UserAssignmentEventRepository;
import ru.yandex.market.tpl.core.service.user.assignment.UserAssignmentEventType;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus.FAILED;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus.FINISHED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SYNC_BOXBOT_STATUSES;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class SyncBoxBotOrderDBQueueTest extends TplAbstractTest {

    private final UserAssignmentEventRepository userAssignmentEventRepository;
    private final BoxBotOrderSyncDataRepository boxBotOrderSyncDataRepository;
    private final BoxBotOrderSyncDataService boxBotOrderSyncDataService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandService userShiftCommandService;
    private final PickupPointRepository pickupPointRepository;
    private final ConfigurationService configurationService;
    private final OrderGenerateService orderGenerateService;
    private final SortingCenterService sortingCenterService;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftRepository userShiftRepository;
    private final OrderCommandService orderCommandService;
    private final QueueLogRepository queueLogRepository;
    private final TestDataFactory testDataFactory;
    private final OrderRepository orderRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TestUserHelper testUserHelper;
    private final JdbcTemplate jdbcTemplate;
    private final TestableClock clock;
    private final LockerApi lockerApi;
    private final LockerSubtaskRepository lockerSubtaskRepository;

    private LockerDeliveryTask lockerDeliveryTask;
    private LockerSubtask subtask;
    private RoutePoint routePoint;
    private UserShift userShift;
    private long subtaskId;
    private Order order;
    private Shift shift;
    private User user;

    @BeforeEach
    void setup() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NonNull TransactionStatus status) {
                configurationService.mergeValue(SYNC_BOXBOT_STATUSES.name(), true);
                user = testUserHelper.findOrCreateUser(12345L);
                shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                        sortingCenterService.findSortCenterForDs(239).getId());
                var userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
                userShift = userShiftRepository.findByIdOrThrow(userShiftId);
                var pickupPoint = pickupPointRepository.save(
                        testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
                var geoPoint = GeoPointGenerator.generateLonLat();

                order = orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .externalOrderId("12345678")
                                .deliveryDate(LocalDate.now(clock))
                                .deliveryServiceId(239L)
                                .pickupPoint(pickupPoint)
                                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                        .geoPoint(geoPoint)
                                        .build())
                                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                                .build()
                );

                userShiftReassignManager.assign(userShift, order);

                testUserHelper.checkinAndFinishPickup(userShift);

                routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
                lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
                subtask = lockerDeliveryTask.streamLockerDeliverySubtasks().findFirst().orElseThrow();
                subtaskId = subtask.getId();
                testUserHelper.arriveAtRoutePoint(routePoint);
            }
        });
    }

    @AfterEach
    void afterEach() {
    }

    /**
     * Проверка успешной обработки задачи с выполнением синхронизации заказа. Если 45/50 чекпоинт не получен, задача
     * должна остаться в очереди, после получения 45/50 успешно завершиться, перевести заказ в статус
     * DELIVERED_TO_PICKUP_POINT и сохранить данные синхронизации
     */
    @Test
    void processSuccessTest() {
        var updatedAt = order.getUpdatedAt();
        assertOrderStatusValid(order.getId(), false, updatedAt);
        assertThat(boxBotOrderSyncDataRepository.findById(subtaskId)).isEmpty();

        cancelTask();
        checkSyncStatus(BoxBotOrderSyncStatus.IN_PROGRESS, null);

        // Первое выполнение - чекпоинт не 45 или 50
        mockBoxBotResponse(order.getExternalOrderId(), 30);
        assertQueueHasSize(1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.SYNC_BOXBOT_ORDER_STATUS);

        // Заказ остался неизмененным, синхронизация в IN_PROGRESS, таска в очереди
        assertOrderStatusValid(order.getId(), false, updatedAt);
        checkSyncStatus(BoxBotOrderSyncStatus.IN_PROGRESS, null);
        assertQueueHasSize(1);
        var lSubtask = lockerSubtaskRepository.findByIdOrThrow(subtaskId);
        assertThat(lSubtask.getStatus()).isEqualTo(FAILED);

        // Второе выполнение - чекпоинт 45
        mockBoxBotResponse(order.getExternalOrderId(), 45);
        pushTasks();
        dbQueueTestUtil.executeAllQueueItems(QueueType.SYNC_BOXBOT_ORDER_STATUS);

        // Заказ прошел синхронизацию, статусы изменились, синхронизация завершена, таски в очереди нет
        assertOrderStatusValid(order.getId(), true, updatedAt);
        checkSyncStatus(BoxBotOrderSyncStatus.FINISHED, null);
        assertQueueHasSize(0);
        // Статус таски перешел в finished
        lSubtask = lockerSubtaskRepository.findByIdOrThrow(subtaskId);
        assertThat(lSubtask.getStatus()).isEqualTo(FINISHED);
        assertThat(lSubtask.getFailReason()).isNull();

        // Заказ снимается с курьера
        var orderUnassigned = userAssignmentEventRepository.findAll().stream()
                .anyMatch(event -> event.getUserId().equals(user.getId())
                        && event.getOrderId().equals(order.getId())
                        && event.getEventType() == UserAssignmentEventType.UNASSIGNED);
        assertThat(orderUnassigned).isTrue();

        // Задача завершена успешно
        assertQueueHasSize(0);
        assertQueueLogValid(
                order.getId(),
                Set.of(QueueLogEvent.EXECUTE, QueueLogEvent.SUCCESSFUL),
                Set.of(QueueLogEvent.FAILED)
        );
    }

    /**
     * Проверка отправки задачи на ретрай, если боксбот не вернул требуемый статус заказа.
     */
    @Test
    void reEnqueueTest() {
        var updatedAt = order.getUpdatedAt();
        assertOrderStatusValid(order.getId(), false, updatedAt);
        mockBoxBotResponse(order.getExternalOrderId(), 10);

        cancelTask();
        checkSyncStatus(BoxBotOrderSyncStatus.IN_PROGRESS, null);

        assertQueueHasSize(1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.SYNC_BOXBOT_ORDER_STATUS);

        // Статус заказа не поменялся
        assertOrderStatusValid(order.getId(), false, updatedAt);

        // Статус синхронизации не поменялся
        checkSyncStatus(BoxBotOrderSyncStatus.IN_PROGRESS, null);

        // Задача перенесена (выполнена успешно и создана новая)
        assertQueueLogValid(
                order.getId(),
                Set.of(QueueLogEvent.EXECUTE, QueueLogEvent.SUCCESSFUL),
                Set.of(QueueLogEvent.FAILED)
        );
        assertQueueHasSize(1);
    }

    /**
     * Проверка обработки задачи при закрытой смене. Если смена закрыта задача должна завершиться без повторов.
     */
    @Test
    void failUserShiftFinishedTest() {
        var updatedAt = order.getUpdatedAt();
        assertOrderStatusValid(order.getId(), false, updatedAt);
        cancelTask();
        userShiftCommandService.closeShift(new UserShiftCommand.Close(userShift.getId()));

        assertQueueHasSize(1);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.SYNC_BOXBOT_ORDER_STATUS);

        // Статус заказа не поменялся
        assertOrderStatusValid(order.getId(), false, updatedAt);

        // Синхронизация завершена с ошибкой
        checkSyncStatus(BoxBotOrderSyncStatus.FAILED, BoxBotOrderSyncFailReason.USER_SHIFT_CLOSED);

        // Задача завершена с ошибкой
        assertQueueHasSize(0);
        assertQueueLogValid(
                order.getId(),
                Set.of(QueueLogEvent.EXECUTE, QueueLogEvent.FAILED),
                Set.of(QueueLogEvent.SUCCESSFUL)
        );
    }

    /**
     * Проверка обработки задачи при измененном статусе заказа. Если из ББ получили 45/50 чекпоинт, а в МК
     * статус заказа не может быть переведен из текущего в доставленный, синхронизация должна завершиться с ошибкой
     */
    @Test
    void failWrongOrderStatusTest() {
        var updatedAt = order.getUpdatedAt();
        assertOrderStatusValid(order.getId(), false, updatedAt);
        cancelTask();

        orderCommandService.cancelOrReturn(new OrderCommand.CancelOrReturn(
                order.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_WAS_LOST, null),
                user,
                false
        ));
        updatedAt = orderRepository.findByIdOrThrow(order.getId()).getUpdatedAt();

        mockBoxBotResponse(order.getExternalOrderId(), 45);
        assertQueueHasSize(1);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.SYNC_BOXBOT_ORDER_STATUS);

        // Статус заказа не поменялся, синхронизация завершена с ошибкой
        assertOrderStatusValid(order.getId(), false, updatedAt);
        checkSyncStatus(BoxBotOrderSyncStatus.FAILED, BoxBotOrderSyncFailReason.WRONG_ORDER_STATUS);

        // Задача завершена с ошибкой
        assertQueueHasSize(0);
        assertQueueLogValid(
                order.getId(),
                Set.of(QueueLogEvent.EXECUTE, QueueLogEvent.FAILED),
                Set.of(QueueLogEvent.SUCCESSFUL)
        );
    }

    /**
     * Проверка обработки задачи при измененном статусе заказа. Если из ББ получили 45/50 чекпоинт, а в МК
     * статус заказа уже доставлен, то синхроизация должна завершиться успешно и не менять заказ
     */
    @Test
    void processDeliveredStatusAlreadySetTest() {
        assertOrderStatusValid(order.getId(), false, order.getUpdatedAt());
        assertThat(boxBotOrderSyncDataRepository.findById(subtaskId)).isEmpty();

        cancelTask();
        checkSyncStatus(BoxBotOrderSyncStatus.IN_PROGRESS, null);
        assertQueueHasSize(1);

        orderCommandService.deliverToPickupPoint(
                new OrderCommand.DeliverToPickupPoint(order.getId(), userShift.getUser(), Source.SYSTEM)
        );
        var updatedAt = orderRepository.findByIdOrThrow(order.getId()).getUpdatedAt();
        mockBoxBotResponse(order.getExternalOrderId(), 45);
        dbQueueTestUtil.executeAllQueueItems(QueueType.SYNC_BOXBOT_ORDER_STATUS);

        // Задача завершена успешно, синхронизация завершена, заказ не изменялся
        assertOrderStatusValid(order.getId(), false, updatedAt);
        checkSyncStatus(BoxBotOrderSyncStatus.FINISHED, null);
        assertQueueHasSize(0);

        // Задача завершена успешно
        assertQueueHasSize(0);
        assertQueueLogValid(
                order.getId(),
                Set.of(QueueLogEvent.EXECUTE, QueueLogEvent.SUCCESSFUL),
                Set.of(QueueLogEvent.FAILED)
        );
    }


    /**
     * Проверка повторного запуска синхронизации.
     */
    @Test
    void cancelReopenedTaskTest() {
        transactionTemplate.execute(status -> {
            boxBotOrderSyncDataService.startSync(subtask);
            orderCommandService.deliverToPickupPoint(new OrderCommand.DeliverToPickupPoint(
                    order.getId(), user, Source.SYSTEM
            ));
            boxBotOrderSyncDataService.finishSync(subtaskId, 45, Source.COURIER);
            return null;
        });
        var syncData = transactionTemplate.execute(status -> {
            boxBotOrderSyncDataService.startSync(subtask);
            return boxBotOrderSyncDataRepository.findByIdOrThrow(subtask.getId());
        });
        assertThat(syncData.getStatus()).isEqualTo(BoxBotOrderSyncStatus.IN_PROGRESS);
    }

    private void cancelTask() {
        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), routePoint.getId(), lockerDeliveryTask.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.FINISHED_BY_SUPPORT, "Не работает!!")
        ));
    }

    private void mockBoxBotResponse(String externalOrderId, int statusCode) {
        var response = BoxBotOrdersStatusDto.builder()
                .orders(List.of(BoxBotOrderStatusDto.builder()
                        .orderId(externalOrderId)
                        .statusCode(statusCode)
                        .updatedBy(BoxBotActorType.COURIER)
                        .build()))
                .build();
        doReturn(response).when(lockerApi).getOrdersStatus(eq(Set.of(externalOrderId)));
    }

    private void assertQueueLogValid(long orderId, Set<QueueLogEvent> requiredEvents,
                                     Set<QueueLogEvent> forbiddenEvents) {
        var logEvents = queueLogRepository.findAll().stream()
                .filter(log -> log.getQueueName().equals(QueueType.SYNC_BOXBOT_ORDER_STATUS.name()))
                .filter(log -> log.getEntityId().equals(Long.toString(orderId)))
                .map(QueueLog::getEvent)
                .collect(Collectors.toSet());
        requiredEvents.forEach(event -> assertThat(logEvents.contains(event)).isTrue());
        forbiddenEvents.forEach(event -> assertThat(logEvents.contains(event)).isFalse());
    }

    private void assertOrderStatusValid(long orderId, boolean changeExpected, Instant previousUpdateTime) {
        var order = orderRepository.findByIdOrThrow(orderId);
        if (changeExpected) {
            assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
            assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.DELIVERED);
            assertThat(order.getUpdatedAt().isAfter(previousUpdateTime)).isTrue();
        } else {
            assertThat(order.getUpdatedAt()).isEqualTo(previousUpdateTime);
        }
    }

    private void assertQueueHasSize(int expectedCount) {
        var queueSize = jdbcTemplate.queryForObject(
                "select count(*) " +
                        "from queue_task where queue_name = 'SYNC_BOXBOT_ORDER_STATUS'",
                Integer.class
        );
        assertThat(queueSize).isEqualTo(expectedCount);
    }

    private void pushTasks() {
        jdbcTemplate.update("update queue_task set process_time = now()-interval'10 minute' " +
                "where queue_name='SYNC_BOXBOT_ORDER_STATUS'");
    }

    private void checkSyncStatus(BoxBotOrderSyncStatus expectedStatus, BoxBotOrderSyncFailReason reason) {
        transactionTemplate.execute(status -> {
            var syncData = boxBotOrderSyncDataRepository.findByIdOrThrow(subtaskId);
            assertThat(syncData.getStatus()).isEqualTo(expectedStatus);
            assertThat(syncData.getFailReason()).isEqualTo(reason);
            return null;
        });
    }
}
