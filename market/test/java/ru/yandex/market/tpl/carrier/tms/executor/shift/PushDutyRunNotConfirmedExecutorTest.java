package ru.yandex.market.tpl.carrier.tms.executor.shift;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.PushCarrierNotificationRepository;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierEvent;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierNotification;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.xiva.send.XivaSendTvmClient;
import ru.yandex.market.tpl.common.xiva.send.model.PushCarrierSendRequest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@TmsIntTest
public class PushDutyRunNotConfirmedExecutorTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final DutyGenerator dutyGenerator;
    private final PushDutyRunNotConfirmedExecutor executor;
    private final XivaSendTvmClient xivaSendTvmClient;

    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunHelper runHelper;
    private final Clock clock;

    private final DbQueueTestUtil dbQueueTestUtil;

    private final PushCarrierNotificationRepository pushCarrierNotificationRepository;
    private final JdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<PushCarrierSendRequest> sendRequestArgumentCaptor;

    private User user;
    private Duty duty;

    @BeforeEach
    void setUp() {
        Company company = testUserHelper.findOrCreateCompany("test_123");
        DeliveryService deliveryService = testUserHelper.deliveryService(1234L,
                Set.of(company));
        user = testUserHelper.findOrCreateUser(1L, company.getName());
        Transport transport = testUserHelper.findOrCreateTransport("test_tr", company.getName());
        duty = dutyGenerator.generate(db -> db.deliveryServiceId(deliveryService.getId()));
        UserShift userShift = runHelper.assignUserAndTransport(duty.getRun(), user, transport);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.arriveAtRoutePoint(userShift.getFirstRoutePoint());
        runHelper.createDutyRun(orderWarehouseGenerator.generateWarehouse(),
                orderWarehouseGenerator.generateWarehouse(), duty, duty.getRun());
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(xivaSendTvmClient);
    }

    @Test
    void shouldSendPushToLostCarrierDrivers() {
        executor.doRealJob(null);

        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PUSH_CARRIER_NOTIFICATION);

        Mockito.verify(xivaSendTvmClient).send(sendRequestArgumentCaptor.capture());

        List<PushCarrierSendRequest> values = sendRequestArgumentCaptor.getAllValues();
        Assertions.assertThat(values).hasSize(1);
    }

    @Test
    @Transactional
    void shouldNotSendPushIfHasRecentNotification() {
        Instant createdAt = clock.instant().minus(3, ChronoUnit.MINUTES);
        PushCarrierNotification notification =
                pushCarrierNotificationRepository.findTopByXivaUserIdAndEventOrderByIdDesc("1",
                        PushCarrierEvent.DUTY_RUN_CREATED).orElseThrow();


        jdbcTemplate.update("update push_carrier_notification set created_at = ? where id = ?",
                Timestamp.from(createdAt), notification.getId());

        executor.doRealJob(null);

        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 1);

    }

    @Test
    @Transactional
    void shouldSendPushIfHasOldNotification() {

        Instant createdAt = clock.instant().minus(1, ChronoUnit.HOURS);

        PushCarrierNotification notification =
                pushCarrierNotificationRepository.findTopByXivaUserIdAndEventOrderByIdDesc("1",
                        PushCarrierEvent.DUTY_RUN_CREATED).orElseThrow();

        jdbcTemplate.update("update push_carrier_notification set created_at = ? where id = ?",
                Timestamp.from(createdAt), notification.getId());

        executor.doRealJob(null);

        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PUSH_CARRIER_NOTIFICATION);

        Mockito.verify(xivaSendTvmClient).send(sendRequestArgumentCaptor.capture());

        List<PushCarrierSendRequest> values = sendRequestArgumentCaptor.getAllValues();
        Assertions.assertThat(values).hasSize(1);
    }

}
