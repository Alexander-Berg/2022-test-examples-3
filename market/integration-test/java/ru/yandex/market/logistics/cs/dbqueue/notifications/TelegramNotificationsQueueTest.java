package ru.yandex.market.logistics.cs.dbqueue.notifications;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener.LifecycleEventType;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener.QueueCoordinates;
import ru.yandex.market.logistics.cs.dbqueue.notifications.telegram.TelegramNotificationsProducer;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceCounterBatchPayload;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptor;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.domain.enumeration.TelegramChannel;
import ru.yandex.market.logistics.cs.service.TelegramNotificationService;
import ru.yandex.market.logistics.cs.util.DateTimeUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerCapacityFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.money.common.dbqueue.api.QueueConsumer;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.cs.config.dbqueue.TelegramNotificationsQueueConfiguration.QUEUE_LOCATION;
import static ru.yandex.market.logistics.cs.dbqueue.common.SingleQueueShardRouter.MASTER;
import static ru.yandex.market.logistics.management.entity.type.CapacityService.INBOUND;
import static ru.yandex.market.logistics.management.entity.type.CapacityService.SHIPMENT;

@DisplayName("Очередь telegram_notifications")
class TelegramNotificationsQueueTest extends AbstractIntegrationTest {
    private static final QueueCoordinates QUEUE_COORDINATES = new QueueCoordinates(MASTER, QUEUE_LOCATION);

    private static final LocalDate DAY = DateTimeUtils.nowUtc().toLocalDate();

    @Autowired
    private QueueConsumer<ServiceCounterBatchPayload> consumer;

    @Autowired
    private AccountingTaskLifecycleListener taskLifecycleListener;

    @Autowired
    private TelegramNotificationsProducer telegramNotificationsProducer;

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setUp() {
        clock.setFixed(DAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

        Mockito.when(lmsClient.searchCapacity(PartnerCapacityFilter.newBuilder().ids(Set.of(1L)).build()))
            .thenReturn(List.of(
                PartnerCapacityDto.newBuilder().id(1L).partnerId(111L).capacityService(INBOUND).build())
            );
        Mockito.when(lmsClient.searchCapacity(PartnerCapacityFilter.newBuilder().ids(Set.of(2L)).build()))
            .thenReturn(List.of(
                PartnerCapacityDto.newBuilder().id(2L).partnerId(111L).capacityService(SHIPMENT).build())
            );
        Mockito.when(lmsClient.searchCapacity(PartnerCapacityFilter.newBuilder().ids(Set.of(4L)).build()))
            .thenReturn(List.of(
                PartnerCapacityDto.newBuilder().id(4L).partnerId(111L).capacityService(INBOUND).build())
            );
        Mockito.when(lmsClient.getPartner(111L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().partnerType(PartnerType.FULFILLMENT).build()));
    }

    @AfterEach
    void tearDown() {
        clock.clearFixed();
        taskLifecycleListener.reset();
    }

    @Test
    @DisplayName("Отправляются нотификации после изменения счетчиков при выполнении условий")
    @DatabaseSetup(value = "/repository/notifications/common/before/before_notifications_base.xml")
    void testProducer() {
        ServiceCounterBatchPayload serviceCounterBatchPayload = new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(new ServiceDeliveryDescriptor(111L, DAY, 600)),
            300,
            false
        );

        consumer.execute(createTask(serviceCounterBatchPayload));
        await().atMost(Duration.ofMinutes(1)).until(() -> containsTaskLifecycleEventOfType(11));

        verify(telegramNotificationsProducer, times(2))
            .enqueue(eq(TelegramChannel.CAPACITY_OVERFLOW), any(String.class));
        verify(telegramNotificationService, times(2))
            .send(eq(TelegramChannel.CAPACITY_OVERFLOW), any(String.class));
        verify(telegramNotificationsProducer, times(9))
            .enqueue(eq(TelegramChannel.WAREHOUSE), any(String.class));
        verify(telegramNotificationService, times(9))
            .send(eq(TelegramChannel.WAREHOUSE), any(String.class));
    }

    private boolean containsTaskLifecycleEventOfType(int finishedTaskCount) {
        return taskLifecycleListener.getEvents(QUEUE_COORDINATES).stream()
            .filter(event -> LifecycleEventType.FINISHED.equals(event.getType()))
            .count() == finishedTaskCount;
    }

    @Nonnull
    private Task<ServiceCounterBatchPayload> createTask(ServiceCounterBatchPayload payload) {
        return new Task<>(
            MASTER,
            payload,
            0,
            ZonedDateTime.now(ZoneId.of("UTC")),
            "traceInfo",
            "actor"
        );
    }
}
