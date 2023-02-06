package ru.yandex.market.delivery.mdbapp.components.storage.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import steps.utils.TestableClock;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.dto.capacity.CapacityHealthCheckDto;

@Sql(
    value = "/data/repository/partnerCapacity/truncate-capacity.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    value = "/data/repository/partnerCapacity/capacity-counters_overflow.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class CapacityCounterRepositoryTest extends MockContextualTest {
    @Autowired
    CapacityCounterRepository capacityCounterRepository;

    @Autowired
    private TestableClock clock;

    @Test
    public void testFindOverflowCounters() {
        clock.setFixed(
            LocalDateTime.of(2019, 1, 1, 15, 0, 0)
                .toInstant(OffsetDateTime.now().getOffset()),
            ZoneId.systemDefault()
        );

        var overflows = capacityCounterRepository.getTopOverflowCountersAtInterval(
            LocalDate.now(clock),
            LocalDate.now(clock).plusDays(5),
            50L,
            PageRequest.of(0, 10)
        );

        softly.assertThat(overflows.size()).isEqualTo(2);
        softly.assertThat(CapacityHealthCheckDto.getOverflow(overflows.get(0))).isEqualTo(100);
        softly.assertThat(CapacityHealthCheckDto.getOverflow(overflows.get(1))).isEqualTo(50);
    }

    @Test
    public void testFindOverflowCountersPagination() {
        clock.setFixed(
            LocalDateTime.of(2019, 1, 1, 15, 0, 0)
                .toInstant(OffsetDateTime.now().getOffset()),
            ZoneId.systemDefault()
        );

        var overflows = capacityCounterRepository.getTopOverflowCountersAtInterval(
            LocalDate.now(clock),
            LocalDate.now(clock).plusDays(5),
            50L,
            PageRequest.of(0, 1)
        );

        softly.assertThat(overflows.size()).isEqualTo(1);
        softly.assertThat(CapacityHealthCheckDto.getOverflow(overflows.get(0))).isEqualTo(100);
    }
}
