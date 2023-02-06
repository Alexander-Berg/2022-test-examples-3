package ru.yandex.market.tpl.tms.service.equeue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.equeue.EqueuePushCouriersToScProducer;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterUtil;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.util.CacheTestUtil;
import ru.yandex.market.tpl.core.util.DbTestUtil;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DBQUEUE_EXECUTION_DELAY_IN_SECONDS_FOR_JOBS_THAT_DEPEND_ON_CONF_PROPS_CACHE;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.YARD_FOR_EQUEUE_DISABLED;

@RequiredArgsConstructor
class EqueuePushCouriersToScProcessingServiceTest extends TplTmsAbstractTest {

    private final EqueuePushCouriersToScProducer producer;
    private final SortingCenterRepository sortingCenterRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftCommandService commandService;
    private final UserShiftTestHelper userShiftTestHelper;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final TransactionTemplate transactionTemplate;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final List<CacheManager> cacheManagers;
    private final DataSource dataSource;

    @BeforeEach
    void beforeEach() {
        DbTestUtil.truncateTables(dataSource);
        configurationServiceAdapter.mergeValue(YARD_FOR_EQUEUE_DISABLED, false);
    }

    @Test
    void pushAllCouriers() {
        UserShift userShiftNotStarted = transactionTemplate.execute(
                status -> createUserShiftAndArriveAtPickup(876585L)
        );

        UserShift userShiftInQueue = transactionTemplate.execute(status -> {
            var result = createUserShiftAndArriveAtPickup(876586L);
            userShiftTestHelper.arriveAtRoutePoint(result);
            return result;
        });

        UserShift userShiftWaitingArrival = transactionTemplate.execute(status -> {
            var result = createUserShiftAndArriveAtPickup(876587L);
            userShiftTestHelper.arriveAtRoutePoint(result);
            inviteToLoading(result);
            return result;
        });

        UserShift userShiftMissedArrival = transactionTemplate.execute(status -> {
            var result = createUserShiftAndArriveAtPickup(876588L);
            userShiftTestHelper.arriveAtRoutePoint(result);
            inviteToLoading(result);
            missArrival(result);
            return result;
        });

        configurationServiceAdapter.mergeValue(YARD_FOR_EQUEUE_DISABLED, true);

        configurationServiceAdapter.mergeValue(
                DBQUEUE_EXECUTION_DELAY_IN_SECONDS_FOR_JOBS_THAT_DEPEND_ON_CONF_PROPS_CACHE, 0
        );
        CacheTestUtil.clear(cacheManagers);

        producer.produce();
        dbQueueTestUtil.executeAllQueueItems(QueueType.EQUEUE_PUSH_COURIERS_TO_SC);

        transactionTemplate.execute(status -> {
            UserShift userShiftNotStartedResult = userShiftRepository.findById(
                    userShiftNotStarted.getId()
            ).get();
            assertThat(getOrderPickupTask(userShiftNotStartedResult).getStatus())
                    .isEqualTo(OrderPickupTaskStatus.NOT_STARTED);

            UserShift userShiftInQueueResult = userShiftRepository.findById(
                    userShiftInQueue.getId()
            ).get();
            assertThat(getOrderPickupTask(userShiftInQueueResult).getStatus())
                    .isEqualTo(OrderPickupTaskStatus.READY_TO_SCAN);

            UserShift userShiftWaitingArrivalResult = userShiftRepository.findById(
                    userShiftWaitingArrival.getId()
            ).get();
            assertThat(getOrderPickupTask(userShiftWaitingArrivalResult).getStatus())
                    .isEqualTo(OrderPickupTaskStatus.READY_TO_SCAN);

            UserShift userShiftMissedArrivalResult = userShiftRepository.findById(
                    userShiftMissedArrival.getId()
            ).get();
            assertThat(getOrderPickupTask(userShiftMissedArrivalResult).getStatus())
                    .isEqualTo(OrderPickupTaskStatus.READY_TO_SCAN);

            return null;
        });
    }

    @Test
    void pushCouriersForSc() {
        UserShift userShiftInQueue = transactionTemplate.execute(status -> {
            var result = createUserShiftAndArriveAtPickup(876589L);
            userShiftTestHelper.arriveAtRoutePoint(result);
            sortingCenterPropertyService.upsertPropertyToSortingCenter(
                    result.getShift().getSortingCenter(),
                    SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                    false
            );
            return result;
        });
        Long scId = userShiftInQueue.getShift().getSortingCenter().getId();

        configurationServiceAdapter.mergeValue(
                DBQUEUE_EXECUTION_DELAY_IN_SECONDS_FOR_JOBS_THAT_DEPEND_ON_CONF_PROPS_CACHE, 0
        );
        CacheTestUtil.clear(cacheManagers);

        producer.produce(scId);
        dbQueueTestUtil.executeAllQueueItems(QueueType.EQUEUE_PUSH_COURIERS_TO_SC);

        transactionTemplate.execute(status -> {
            UserShift userShiftInQueueResult = userShiftRepository.findById(
                    userShiftInQueue.getId()
            ).get();
            assertThat(getOrderPickupTask(userShiftInQueueResult).getStatus())
                    .isEqualTo(OrderPickupTaskStatus.READY_TO_SCAN);

            return null;
        });
    }

    private UserShift createUserShiftAndArriveAtPickup(long uin) {
        long scId = 47820L;
        sortingCenterRepository.save(SortingCenterUtil.sortingCenter(scId));
        User user = userHelper.findOrCreateUserForSc(uin, LocalDate.now(clock), scId);
        Shift shift = userHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), scId);
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();
        long userShiftId = commandService.createUserShift(createCommand);

        UserShift userShift = userShiftRepository.findById(userShiftId).get();

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getShift().getSortingCenter(),
                SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                true
        );
        CacheTestUtil.clear(cacheManagers);

        commandService.switchActiveUserShift(user, userShiftId);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        RoutePoint routePoint = userShift.getCurrentRoutePoint();
        Long routePointId = routePoint.getId();

        commandService.arriveAtRoutePoint(
                user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShiftId, routePointId,
                        new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShiftId)
                )
        );

        return userShift;
    }

    private void inviteToLoading(UserShift userShift) {
        OrderPickupTask orderPickupTask = getOrderPickupTask(userShift);
        commandService.inviteToLoading(
                userShift.getUser(),
                new UserShiftCommand.InviteToLoading(
                        userShift.getId(),
                        orderPickupTask.getRoutePoint().getId(),
                        orderPickupTask.getId(),
                        Instant.now(clock).plus(5, ChronoUnit.MINUTES)
                )
        );
    }

    private void missArrival(UserShift userShift) {
        OrderPickupTask orderPickupTask = getOrderPickupTask(userShift);
        commandService.missArrival(
                userShift.getUser(),
                new UserShiftCommand.MissArrival(
                        userShift.getId(),
                        orderPickupTask.getRoutePoint().getId(),
                        orderPickupTask.getId(),
                        false,
                        Source.SYSTEM
                )
        );
    }

    private OrderPickupTask getOrderPickupTask(UserShift userShift) {
        return userShift.streamPickupTasks().findFirst()
                .orElseThrow(() -> new RuntimeException("OrderPickupTask not found"));
    }

}
