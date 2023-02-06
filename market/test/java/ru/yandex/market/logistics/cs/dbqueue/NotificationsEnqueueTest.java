package ru.yandex.market.logistics.cs.dbqueue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.cs.dbqueue.notifications.counter.CounterNotificationsProducer;
import ru.yandex.market.logistics.cs.dbqueue.notifications.dayoff.DayOffNotificationsProducer;
import ru.yandex.market.logistics.cs.domain.dto.NotifyMessageDto;
import ru.yandex.market.logistics.cs.domain.dto.PartnerDto;
import ru.yandex.market.logistics.cs.domain.entity.CapacityCounterNotification;
import ru.yandex.market.logistics.cs.domain.entity.CapacityValueCounter;
import ru.yandex.market.logistics.cs.domain.enumeration.CapacityValueWarnPercent;
import ru.yandex.market.logistics.cs.domain.enumeration.NotificationType;
import ru.yandex.market.logistics.cs.notifications.counter.ICapacityCounterNotifiable;
import ru.yandex.market.logistics.cs.notifications.dayoff.IDayOffNotifiable;
import ru.yandex.market.logistics.cs.repository.CapacityValueCounterRepository;
import ru.yandex.market.logistics.cs.repository.EventRepository;
import ru.yandex.market.logistics.cs.service.CapacityNotificationService;
import ru.yandex.market.logistics.cs.service.NotificationPreparingService;
import ru.yandex.market.logistics.cs.service.impl.NotificationPreparingServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Распределение нотификаций по очередям")
public class NotificationsEnqueueTest {

    private static final String MESSAGE_TEXT = "some text";
    private static final List<NotificationType> COUNTER_TYPES = List.of(
        NotificationType.COUNTER_HALF,
        NotificationType.COUNTER_LOW,
        NotificationType.COUNTER_OVERFLOW,
        NotificationType.COUNTER_ALREADY_OVERFLOWED,
        NotificationType.COUNTER_LESS_20
    );
    private static final List<NotificationType> DAY_OFF_TYPES = List.of(
        NotificationType.DAY_OFF_SET,
        NotificationType.DAY_OFF_UNSET
    );

    private final List<ICapacityCounterNotifiable> capacityCounterNotifications = createCounterNotifications();
    private final List<IDayOffNotifiable> dayOffNotifications = createDayOffNotifications();

    private final CapacityNotificationService capacityNotificationService = Mockito.mock(
        CapacityNotificationService.class
    );
    private final CapacityValueCounterRepository counterRepository = Mockito.mock(CapacityValueCounterRepository.class);
    private final EventRepository eventRepository = Mockito.mock(EventRepository.class);
    private final EntityManager entityManager = Mockito.mock(EntityManager.class);

    private final DayOffNotificationsProducer dayOffProducer = Mockito.mock(DayOffNotificationsProducer.class);
    private final CounterNotificationsProducer counterProducer = Mockito.mock(CounterNotificationsProducer.class);

    private final NotificationPreparingService notificationPreparingService =
        new NotificationPreparingServiceImpl(
            capacityCounterNotifications,
            dayOffNotifications,
            capacityNotificationService,
            counterRepository,
            eventRepository,
            entityManager,
            dayOffProducer,
            counterProducer,
            new TestableClock()
        );

    @BeforeEach
    void init() {
        Mockito.when(capacityNotificationService.find(anyLong())).thenReturn(Optional.empty());
        Mockito.when(counterRepository.findAllById(any())).thenReturn(createCounters());
    }

    @Test
    @DisplayName("Нотификации о счетчике попадают только в очередь counter queue.")
    void testCounterEnqueue() {
        notificationPreparingService.enqueueCapacityCounterNotifications(
            CapacityValueCounter.builder().id(1L).build(),
            0L
        );
        verify(counterProducer, times(1)).enqueue(any(), eq(COUNTER_TYPES));
        verifyNoMoreInteractions(counterProducer);
        verifyNoInteractions(dayOffProducer);
    }

    @Test
    @DisplayName("Нотификации о счетчике попадают только в очередь day off queue.")
    void testPossibleFlows() {
        List<CapacityValueCounter> counters = createCounters();
        notificationPreparingService.enqueueDayOffNotifications(
            counters.stream()
                .map(CapacityValueCounter::getId)
                .collect(Collectors.toList())
        );
        verify(dayOffProducer, times(counters.size())).enqueue(any(), eq(DAY_OFF_TYPES));
        verifyNoMoreInteractions(dayOffProducer);
        verifyNoInteractions(counterProducer);
    }

    private static List<CapacityValueCounter> createCounters() {
        return IntStream.range(0, 5).mapToObj(i ->
            CapacityValueCounter.builder()
                .id((long) i)
                .build()
        ).collect(Collectors.toList());
    }

    private static List<ICapacityCounterNotifiable> createCounterNotifications() {
        return COUNTER_TYPES.stream().map(notificationType -> new ICapacityCounterNotifiable() {

            @Override
            public boolean match(
                @Nonnull CapacityValueCounter capacityValueCounter,
                @Nonnull Optional<CapacityCounterNotification> notificationDto
            ) {
                return true;
            }

            @NotNull
            @Override
            public CapacityValueWarnPercent getCapacityValueWarnPercent() {
                return CapacityValueWarnPercent.OVERFLOW;
            }

            @Override
            public void markNotificationAsSend(@Nonnull CapacityCounterNotification capacityCounterNotification) {
            }

            @Override
            public NotificationType getNotificationType() {
                return notificationType;
            }

            @Override
            @Nonnull
            public String getMessageText(@Nonnull NotifyMessageDto dto) {
                return MESSAGE_TEXT;
            }

            @Override
            public boolean validatePartnerType(@Nonnull PartnerDto partner) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    private static List<IDayOffNotifiable> createDayOffNotifications() {
        return DAY_OFF_TYPES.stream().map(notificationType -> new IDayOffNotifiable() {
            @Override
            public boolean match(@Nonnull CapacityValueCounter counter) {
                return true;
            }

            @Override
            @Nonnull
            public String getMessageText(@Nonnull NotifyMessageDto dto) {
                return MESSAGE_TEXT;
            }

            @Override
            public NotificationType getNotificationType() {
                return notificationType;
            }

            @Override
            public boolean validatePartnerType(@Nonnull PartnerDto partner) {
                return true;
            }
        }).collect(Collectors.toList());
    }
}
