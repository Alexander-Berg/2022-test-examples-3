package ru.yandex.market.logistics.cs.notifications;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.config.dbqueue.CounterNotificationsQueueConfiguration;
import ru.yandex.market.logistics.cs.config.dbqueue.DayOffNotificationsQueueConfiguration;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener;
import ru.yandex.market.logistics.cs.dbqueue.notifications.telegram.TelegramNotificationsProducer;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceCounterBatchPayload;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptor;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.facade.CapacityValueCounterFacade;
import ru.yandex.market.logistics.cs.util.DateTimeUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerCapacityFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.awaitility.Awaitility.await;
import static ru.yandex.market.logistics.cs.dbqueue.common.SingleQueueShardRouter.MASTER;
import static ru.yandex.market.logistics.management.entity.type.CapacityService.INBOUND;

@DatabaseSetup("/repository/notifications/common/before/before_notifications_base.xml")
public abstract class CommonCapacityCounterNotificationTest extends AbstractIntegrationTest {
    protected static final LocalDate DAY = DateTimeUtils.nowUtc().toLocalDate();
    protected static final Long PARTNER_ID = 111L;

    private static final AccountingTaskLifecycleListener.QueueCoordinates DAY_OFF_QUEUE_COORDINATES =
        new AccountingTaskLifecycleListener.QueueCoordinates(
            MASTER,
            DayOffNotificationsQueueConfiguration.QUEUE_LOCATION
        );
    private static final AccountingTaskLifecycleListener.QueueCoordinates COUNTER_QUEUE_COORDINATES =
        new AccountingTaskLifecycleListener.QueueCoordinates(
            MASTER,
            CounterNotificationsQueueConfiguration.QUEUE_LOCATION
        );

    @Autowired
    protected CapacityValueCounterFacade facade;

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected TestableClock clock;

    @Autowired
    private AccountingTaskLifecycleListener taskLifecycleListener;

    @Autowired
    protected TelegramNotificationsProducer telegramNotificationsProducer;

    @BeforeEach
    void initializeMocks() {
        mockLmsClientSearchCapacity(1L, PARTNER_ID);
        mockLmsClientSearchCapacity(2L, PARTNER_ID);
        mockLmsClientSearchCapacity(4L, PARTNER_ID);
        Mockito.when(lmsClient.getPartner(PARTNER_ID)).thenReturn(
            Optional.of(PartnerResponse.newBuilder()
                .partnerType(PartnerType.FULFILLMENT)
                .build()
            ));
    }

    @AfterEach
    void tearDown() {
        clock.clearFixed();
        refreshQueue();
    }

    protected void updateCountersWithAmountAndUnitType(int orderCount, int itemCount, LocalDate day) {
        facade.processServiceCounter(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(new ServiceDeliveryDescriptor(111L, day, itemCount)),
            orderCount,
            false
        ));
    }

    protected void mockLmsClientSearchCapacity(long capacityId, long partnerId) {
        Mockito.when(lmsClient.searchCapacity(
                PartnerCapacityFilter.newBuilder()
                    .ids(Set.of(capacityId))
                    .build()
            ))
            .thenReturn(List.of(
                PartnerCapacityDto.newBuilder()
                    .id(capacityId)
                    .partnerId(partnerId)
                    .capacityService(INBOUND)
                    .build()
            ));
    }

    protected void refreshQueue() {
        taskLifecycleListener.reset();
    }

    protected void waitUntilDayOffTasksFinished(int finishedTaskCount) {
        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(DAY_OFF_QUEUE_COORDINATES, finishedTaskCount));
    }

    protected void waitUntilCounterTasksFinished(int finishedTaskCount) {
        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(COUNTER_QUEUE_COORDINATES, finishedTaskCount));
    }

    private boolean containsTaskLifecycleEventOfType(
        AccountingTaskLifecycleListener.QueueCoordinates queueCoordinates,
        int finishedTaskCount
    ) {
        return taskLifecycleListener.getEvents(queueCoordinates).stream()
            .filter(event -> AccountingTaskLifecycleListener.LifecycleEventType.FINISHED.equals(event.getType()))
            .count() == finishedTaskCount;
    }
}
