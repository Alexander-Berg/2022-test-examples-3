package ru.yandex.market.tpl.carrier.tms.executor.shift;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.PushCarrierNotificationRepository;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierEvent;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierNotification;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierNotificationStatus;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierPayload;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.xiva.send.XivaSendTvmClient;
import ru.yandex.market.tpl.common.xiva.send.model.PushCarrierSendRequest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@TmsIntTest
@Sql("classpath:/mockPartner/defaultDeliveryServices.sql")
public class PushLostCarrierDriverExecutorTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final RunGenerator runGenerator;
    private final PushLostCarrierDriverExecutor executor;
    private final XivaSendTvmClient xivaSendTvmClient;

    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;
    private final Clock clock;

    private final DbQueueTestUtil dbQueueTestUtil;

    private final PushCarrierNotificationRepository pushCarrierNotificationRepository;
    private final JdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<PushCarrierSendRequest> sendRequestArgumentCaptor;

    private User user;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(1L);
        Transport transport = testUserHelper.findOrCreateTransport();
        var run = runGenerator.generate();
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());
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
    void shouldNotSendPushIfHasRecentNotification() {
        Instant createdAt = clock.instant().minus(10, ChronoUnit.MINUTES);
        PushCarrierNotification notification = pushCarrierNotificationRepository.save(new PushCarrierNotification(
                null,
                null,
                null,
                String.valueOf(user.getUid()),
                PushCarrierEvent.DRIVER_LOST,
                PushCarrierNotificationStatus.CREATED,
                300,
                PushCarrierPayload.builder().build(),
                0,
                createdAt,
                null
        ));

        jdbcTemplate.update("update push_carrier_notification set created_at = ? where id = ?", Timestamp.from(createdAt), notification.getId());

        executor.doRealJob(null);

        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 0);

        Mockito.verifyNoInteractions(xivaSendTvmClient);
    }

    @Test
    void shouldSendPushIfHasOldNotification() {
        Instant createdAt = clock.instant().minus(1, ChronoUnit.HOURS);

        PushCarrierNotification notification = pushCarrierNotificationRepository.saveAndFlush(new PushCarrierNotification(
                null,
                null,
                null,
                String.valueOf(user.getUid()),
                PushCarrierEvent.DRIVER_LOST,
                PushCarrierNotificationStatus.CREATED,
                300,
                PushCarrierPayload.builder().build(),
                0,
                createdAt,
                null
        ));

        jdbcTemplate.update("update push_carrier_notification set created_at = ? where id = ?", Timestamp.from(createdAt), notification.getId());

        executor.doRealJob(null);

        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PUSH_CARRIER_NOTIFICATION);

        Mockito.verify(xivaSendTvmClient).send(sendRequestArgumentCaptor.capture());

        List<PushCarrierSendRequest> values = sendRequestArgumentCaptor.getAllValues();
        Assertions.assertThat(values).hasSize(1);
    }

}
