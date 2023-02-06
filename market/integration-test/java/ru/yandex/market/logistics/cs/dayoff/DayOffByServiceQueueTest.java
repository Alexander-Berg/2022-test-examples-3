package ru.yandex.market.logistics.cs.dayoff;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistics.cs.dbqueue.dayoff.byservice.DayOffByServiceProducer;
import ru.yandex.market.logistics.cs.dbqueue.dayoff.byservice.ServiceDaysOffPayload;
import ru.yandex.market.logistics.cs.domain.entity.ServiceDayOff;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueConsumer;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Очередь update_day_off_by_service")
class DayOffByServiceQueueTest extends AbstractDayOffTest {

    private static final Long DELETED_CAPACITY_ID = null;

    @Autowired
    @InjectMocks
    private DayOffByServiceProducer producer;

    @Autowired
    private QueueConsumer<ServiceDaysOffPayload> dayOffByServiceQueueConsumer;

    @SpyBean
    private QueueProducer<ServiceDaysOffPayload> queueProducer;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(service, queueProducer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/dayoff/after/days_off_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Добавляем дэйоффы для сервисов")
    void addDaysOffByService() {
        dayOffByServiceQueueConsumer.execute(createTask(SERVICE_ID_10, CAPACITY_ID_1L));
        List<ServiceDayOff> expectedDayoffsFor10thService = List.of(
            dayOff(SERVICE_ID_10, TWENTIETH_OF_MAY),
            dayOff(SERVICE_ID_10, TENTH_OF_MAY)
        );
        softly.assertThat(dayOffDayRepository.findAll())
            .hasSize(2)
            .usingElementComparatorIgnoringFields("id")
            .usingComparatorForElementFieldsWithType(LOCAL_DATE_TIME_COMPARATOR, LocalDateTime.class)
            .hasSameElementsAs(expectedDayoffsFor10thService);

        dayOffByServiceQueueConsumer.execute(createTask(SERVICE_ID_11, CAPACITY_ID_1L));

        verify(service).removeDayOffs(SERVICE_ID_10);
        verify(service).updateDayOffs(SERVICE_ID_10, CAPACITY_ID_1L);
        verify(service).removeDayOffs(SERVICE_ID_11);
        verify(service).updateDayOffs(SERVICE_ID_11, CAPACITY_ID_1L);
    }

    @Test
    @DisplayName("Удаляем дейоффы для сервисов")
    @DatabaseSetup("/repository/dayoff/after/days_off_updated.xml")
    void deleteDaysOffByService() {
        dayOffByServiceQueueConsumer.execute(createTask(SERVICE_ID_10, DELETED_CAPACITY_ID));
        softly.assertThat(dayOffDayRepository.findAll()).hasSize(2).allMatch(it -> it.getServiceId() == SERVICE_ID_11);

        dayOffByServiceQueueConsumer.execute(createTask(SERVICE_ID_11, DELETED_CAPACITY_ID));
        softly.assertThat(dayOffDayRepository.findAll()).isEmpty();

        verify(service).removeDayOffs(SERVICE_ID_10);
        verify(service).removeDayOffs(SERVICE_ID_11);
    }

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Вызывается продюсер для проставления в очередь")
    void taskWasQueued(Long serviceId, @Nullable Long newCapacityId) {
        doReturn(0L).when(queueProducer).enqueue(any());
        producer.enqueue(serviceId, newCapacityId);
        verify(queueProducer).enqueue(eq(EnqueueParams.create(new ServiceDaysOffPayload(
            serviceId,
            newCapacityId
        ))));
    }

    private static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(SERVICE_ID_10, DELETED_CAPACITY_ID),
            Arguments.of(SERVICE_ID_10, CAPACITY_ID_1L),
            Arguments.of(SERVICE_ID_11, DELETED_CAPACITY_ID),
            Arguments.of(SERVICE_ID_11, CAPACITY_ID_1L)
        );
    }

    @Nonnull
    private Task<ServiceDaysOffPayload> createTask(Long serviceId, @Nullable Long newCapacityId) {
        return createTask(new ServiceDaysOffPayload(serviceId, newCapacityId));
    }
}
