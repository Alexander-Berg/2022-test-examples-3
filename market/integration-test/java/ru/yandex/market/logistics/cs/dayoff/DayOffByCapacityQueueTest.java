package ru.yandex.market.logistics.cs.dayoff;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
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

import ru.yandex.market.logistics.cs.dbqueue.dayoff.bycapacity.CapacityDayPayload;
import ru.yandex.market.logistics.cs.dbqueue.dayoff.bycapacity.ServiceToDayByCapacityProducer;
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

@DisplayName("Очередь update_day_off_by_capacity")
@ParametersAreNonnullByDefault
class DayOffByCapacityQueueTest extends AbstractDayOffTest {

    @Autowired
    @InjectMocks
    private ServiceToDayByCapacityProducer producer;

    @Autowired
    private QueueConsumer<CapacityDayPayload> dayOffByCapacityQueueConsumer;

    @SpyBean
    private QueueProducer<CapacityDayPayload> queueProducer;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(service, queueProducer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/dayoff/after/days_off_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Дэйоффы добавляются по капасити, если хотя бы один из флагов day_off = true")
    void addDaysOffByService() {
        dayOffByCapacityQueueConsumer.execute(createTask(CAPACITY_ID_1L, TWENTIETH_OF_MAY));
        List<ServiceDayOff> expectedDayoffsFor10thService = List.of(
            dayOff(SERVICE_ID_10, TWENTIETH_OF_MAY),
            dayOff(SERVICE_ID_11, TWENTIETH_OF_MAY)
        );
        softly.assertThat(dayOffDayRepository.findAll())
            .hasSize(2)
            .usingElementComparatorIgnoringFields("id")
            .usingComparatorForElementFieldsWithType(LOCAL_DATE_TIME_COMPARATOR, LocalDateTime.class)
            .hasSameElementsAs(expectedDayoffsFor10thService);

        dayOffByCapacityQueueConsumer.execute(createTask(CAPACITY_ID_1L, TENTH_OF_MAY));

        verify(service).updateDayOffs(CAPACITY_ID_1L, TWENTIETH_OF_MAY);
        verify(service).updateDayOffs(CAPACITY_ID_1L, TENTH_OF_MAY);
    }

    @Test
    @DisplayName("Дейоффы удаляются, если флаг дэйоффа в этот день false")
    @DatabaseSetup(
        value = "/repository/dayoff/before/dayoff_for_remove.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/repository/dayoff/before/base_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removeDaysoffWithFalseDayoffFlag() {
        dayOffByCapacityQueueConsumer.execute(createTask(CAPACITY_ID_2L, TWENTIETH_OF_MAY));
        verify(service).updateDayOffs(CAPACITY_ID_2L, TWENTIETH_OF_MAY);
    }

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Вызывается продюсер для проставления в очередь")
    void taskQueuedTest(long capacityId, LocalDate day) {
        doReturn(0L).when(queueProducer).enqueue(any());
        producer.enqueue(capacityId, day);
        verify(queueProducer).enqueue(eq(EnqueueParams.create(new CapacityDayPayload(capacityId, day))));
    }

    private static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(CAPACITY_ID_1L, TENTH_OF_MAY),
            Arguments.of(CAPACITY_ID_2L, TWENTIETH_OF_MAY)
        );
    }

    @Nonnull
    private Task<CapacityDayPayload> createTask(long capacityId, LocalDate day) {
        return createTask(new CapacityDayPayload(capacityId, day));
    }
}
