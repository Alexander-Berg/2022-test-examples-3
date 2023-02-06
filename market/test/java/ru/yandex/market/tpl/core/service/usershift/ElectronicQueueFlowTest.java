package ru.yandex.market.tpl.core.service.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.yard.client.YardClientApi;
import ru.yandex.market.logistics.yard.client.dto.registration.RegisterYardClientDto;
import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerEqueueDto;
import ru.yandex.market.tpl.api.model.task.EqueueCodeDto;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.user.UserType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.partner.equeue.PartnerEqueueService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.IN_PROGRESS;
import static ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus.MISSED_ARRIVAL;
import static ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus.WAITING_ARRIVAL;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.YARD_FOR_EQUEUE_DISABLED;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.EQUEUE_SKIP_QUEUE;

@RequiredArgsConstructor
public class ElectronicQueueFlowTest extends TplAbstractTest {

    private final Clock clock;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final ArriveAtRoutePointService arriveAtRoutePointService;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final RoutePointRepository routePointRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ElectronicQueueService electronicQueueService;
    private final PartnerEqueueService partnerEqueueService;
    private final ElectronicQueueLatecomersService electronicQueueLatecomersService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserPropertyService userPropertyService;
    private final YardClientApi yardClientApi;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    private User user;
    private Long userShiftId;
    private Long pickupRoutePointId;

    @BeforeEach
    void init() {
        configurationServiceAdapter.mergeValue(YARD_FOR_EQUEUE_DISABLED, false);
        user = userHelper.findOrCreateUser(35236L);
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShiftId = commandService.createUserShift(createCommand);
        commandService.switchActiveUserShift(user, userShiftId);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getShift().getSortingCenter(),
                SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                true
        );
        pickupRoutePointId = userShift.getCurrentRoutePoint().getId();
    }

    @Test
    void arriveAtPickupRoutePoint() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        arriveAtRoutePoint(userShift);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.IN_QUEUE);
        assertThat(pickupTask.getArrivalAttempt()).isEqualTo(1);

        dbQueueTestUtil.assertQueueHasSize(QueueType.REGISTER_COURIER_IN_YARD, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.REGISTER_COURIER_IN_YARD);

        SortingCenter sortingCenter = userShift.getSortingCenter();
        var offset = sortingCenter.getZoneOffset();
        verify(yardClientApi, times(1)).registerClient(new RegisterYardClientDto(
                "tpl/" + userShift.getUser().getId() + "/" + pickupTask.getArrivalAttempt(),
                userShift.getSortingCenterId(),
                user.getName(),
                null,
                any(),
                LocalDateTime.of(LocalDate.now(offset), userShift.getScheduleData().getShiftStart()).atZone(offset)
        ));
    }

    @Test
    void arriveAtPickupRoutePointWithDisabledYard() {
        configurationServiceAdapter.mergeValue(YARD_FOR_EQUEUE_DISABLED, true);

        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        arriveAtRoutePoint(userShift);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.READY_TO_SCAN);
    }

    @Test
    void arriveAtPickupRoutePointForSelfEmployed() {
        transactionTemplate.execute((status) -> {
            var userForUpdate = userRepository.getById(user.getId());
            userForUpdate.setUserType(UserType.SELF_EMPLOYED);
            userRepository.save(userForUpdate);
            return null;
        });

        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        arriveAtRoutePoint(userShift);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.NOT_STARTED);
    }

    @Test
    void arriveAtPickupRoutePointWithDisabledEqueueForUser() {
        transactionTemplate.execute((status) -> {
            userPropertyService.addPropertyToUser(user, EQUEUE_SKIP_QUEUE, true);
            return null;
        });

        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        arriveAtRoutePoint(userShift);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.READY_TO_SCAN);
    }

    @Test
    void arriveAtPickupRoutePointAfterMissArrival() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        arriveAtRoutePoint(userShift);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupRoutePoint.getStatus()).isEqualTo(IN_PROGRESS);
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.IN_QUEUE);
        assertThat(pickupTask.getArrivalAttempt()).isEqualTo(1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.REGISTER_COURIER_IN_YARD, 1);

        setPickupTaskStatusToMissedArrival(pickupTask.getId());
        pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.MISSED_ARRIVAL);

        arriveAtRoutePointService.arrivedAtRoutePoint(
                userShift.getCurrentRoutePoint().getId(),
                new LocationDto(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "myDevice",
                        userShift.getId()
                ),
                user);

        pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupRoutePoint.getStatus()).isEqualTo(IN_PROGRESS);
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.IN_QUEUE);
        assertThat(pickupTask.getArrivalAttempt()).isEqualTo(2);
        dbQueueTestUtil.assertQueueHasSize(QueueType.REGISTER_COURIER_IN_YARD, 2);
    }

    @Test
    void sendEnqueueCode_inTime() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        arriveAtRoutePoint(userShift);
        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        inviteToLoading(pickupRoutePoint, Instant.now(clock).plusSeconds(60));

        arriveAtSortingCenter(userShift, pickupRoutePoint);

        pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.READY_TO_SCAN);
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    void sendEnqueueCode_late() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        arriveAtRoutePoint(userShift);
        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        inviteToLoading(pickupRoutePoint, Instant.now(clock).minusSeconds(60));

        arriveAtSortingCenter(userShift, pickupRoutePoint);

        pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(MISSED_ARRIVAL);
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    void missArrival_afterSuccessfulArrival() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        arriveAtRoutePointService.arrivedAtRoutePoint(
                userShift.getCurrentRoutePoint().getId(),
                new LocationDto(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "myDevice",
                        userShift.getId()
                ),
                user);
        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        inviteToLoading(pickupRoutePoint, Instant.now(clock).plusSeconds(60));
        arriveAtSortingCenter(userShift, pickupRoutePoint);

        commandService.missArrival(
                user,
                new UserShiftCommand.MissArrival(
                        userShiftId,
                        pickupRoutePointId,
                        pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow().getId(),
                        true,
                        Source.COURIER
                )
        );

        pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.READY_TO_SCAN);
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    void missArrival_afterTimeout() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        arriveAtRoutePointService.arrivedAtRoutePoint(
                userShift.getCurrentRoutePoint().getId(),
                new LocationDto(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "myDevice",
                        userShift.getId()
                ),
                user);
        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        inviteToLoading(pickupRoutePoint, Instant.now(clock).minusSeconds(60));

        commandService.missArrival(
                user,
                new UserShiftCommand.MissArrival(
                        userShiftId,
                        pickupRoutePointId,
                        pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow().getId(),
                        true,
                        Source.COURIER
                )
        );

        pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(MISSED_ARRIVAL);
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    void leaveTheService() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        arriveAtRoutePoint(userShift);
        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        inviteToLoading(pickupRoutePoint, Instant.now(clock).plusSeconds(60));
        arriveAtSortingCenter(userShift, pickupRoutePoint);
        startOrderPickup(pickupTask);
        pickupOrders(pickupTask);
        finishLoading(pickupTask);

        pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();

        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.FINISHED);
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 3);
    }

    @Test
    void partnerGetEqueue() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        arriveAtRoutePoint(userShift);
        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        inviteToLoading(pickupRoutePoint, Instant.now(clock).plusSeconds(60));

        var partnerEqueue = getPartnerEqueue(userShift);

        assertThat(partnerEqueue.getTotalElements()).isEqualTo(1L);
        var equeueDto = partnerEqueue.getContent().iterator().next();
        assertThat(equeueDto.getUserShiftId()).isEqualTo(userShiftId);
        assertThat(equeueDto.getCourierName()).isEqualTo(userShift.getUser().getName());
        assertThat(equeueDto.getProvisionalLoadingStartTime())
                .isEqualTo(userShift.getScheduleData().getLoadingStartTime());
        assertThat(equeueDto.getStatus()).isEqualTo(WAITING_ARRIVAL);
        assertThat(equeueDto.getTransportType()).isNotNull();
        assertThat(equeueDto.getCourierId()).isEqualTo(user.getId());
    }

    @Test
    public void removeFromEqueue() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        arriveAtRoutePoint(userShift);

        partnerEqueueService.removeCourierFromEqueue(userShiftId);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.NOT_STARTED);
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    void removeLatecomersFromQueue() {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        var shift = userShift.getShift();
        updateShiftDate(shift);
        arriveAtRoutePoint(userShift);
        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        inviteToLoading(pickupRoutePoint, Instant.now(clock).minusSeconds(60));

        electronicQueueLatecomersService.removeLateComersFromEqueue(
                userShift.getSortingCenter()
        );

        pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.getStatus()).isEqualTo(MISSED_ARRIVAL);
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 1);
    }

    private void arriveAtRoutePoint(UserShift userShift) {
        arriveAtRoutePointService.arrivedAtRoutePoint(
                userShift.getCurrentRoutePoint().getId(),
                new LocationDto(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "myDevice",
                        userShift.getId()
                ),
                user);
    }

    private void inviteToLoading(RoutePoint pickupRoutePoint, Instant instant) {
        commandService.inviteToLoading(user, new UserShiftCommand.InviteToLoading(
                userShiftId,
                pickupRoutePointId,
                pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow().getId(),
                instant
        ));
    }

    private Page<PartnerEqueueDto> getPartnerEqueue(UserShift userShift) {
        return partnerEqueueService.getEqueueForSortingCenter(
                userShift.getShift().getSortingCenter().getId(),
                userShift.getShift().getShiftDate(),
                Pageable.unpaged()
        );
    }

    private void arriveAtSortingCenter(UserShift userShift, RoutePoint pickupRoutePoint) {
        var qrCode = electronicQueueService.generateQrCodeContentForElectronicQueue(
                userShift.getShift().getSortingCenter().getId(), userShift.getShift().getShiftDate());
        commandService.arriveAtSortingCenter(user, new UserShiftCommand.ArriveAtSortingCenter(
                userShiftId,
                pickupRoutePointId,
                pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow().getId(),
                Optional.of(new EqueueCodeDto(qrCode))
        ));
    }

    private void startOrderPickup(OrderPickupTask pickupTask) {
        commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                userShiftId,
                pickupRoutePointId,
                pickupTask.getId()
        ));
    }

    private void pickupOrders(OrderPickupTask pickupTask) {
        commandService.pickupOrders(user, new UserShiftCommand.FinishScan(
                userShiftId,
                pickupRoutePointId,
                pickupTask.getId(),
                ScanRequest.builder().build()
        ));
    }

    private void finishLoading(OrderPickupTask pickupTask) {
        commandService.finishLoading(user, new UserShiftCommand.FinishLoading(
                userShiftId,
                pickupRoutePointId,
                pickupTask.getId()
        ));
    }

    private void setPickupTaskStatusToMissedArrival(Long taskId) {
        jdbcTemplate.update("UPDATE task SET status = ? WHERE id = ?", MISSED_ARRIVAL.toString(), taskId);
    }

    private void updateShiftDate(Shift shift) {
        var date = LocalDate.now(shift.getSortingCenter().getZoneOffset());
        jdbcTemplate.update("UPDATE shift SET shift_date = ? WHERE id = ?", date, shift.getId());
    }

}
