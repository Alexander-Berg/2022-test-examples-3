package ru.yandex.market.logistics.utilizer.repo;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.domain.entity.UtilizationCycle;
import ru.yandex.market.logistics.utilizer.domain.enums.UtilizationCycleStatus;

public class UtilizationCycleRepositoryTest extends AbstractContextualTest {
    @Autowired
    UtilizationCycleJpaRepository utilizationCycleJpaRepository;

    @Test
    @ExpectedDatabase(value = "classpath:fixtures/repo/utilization-cycle/1/db-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void persistAndLoadTest() {
        UtilizationCycle newUtilizationCycle = UtilizationCycle.builder()
                .messageSentAt(LocalDateTime.now())
                .status(UtilizationCycleStatus.CREATED)
                .vendorId(100500L)
                .build();

        utilizationCycleJpaRepository.save(newUtilizationCycle);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/utilization-cycle/2/db-state.xml")
    public void findAllByStatusAndMessageSentAtLessThan() {
        List<Long> actualIds = utilizationCycleJpaRepository.findAllByStatusAndMessageSentAtLessThan(
                UtilizationCycleStatus.FINALIZED,
                LocalDateTime.of(2020, 12, 7, 11, 0)
        ).stream()
                .map(UtilizationCycle::getId)
                .collect(Collectors.toList());
        softly.assertThat(actualIds).containsExactlyInAnyOrder(2L);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/utilization-cycle/2/db-state.xml")
    public void findAllByStatusAndCreatedAtLessThan() {
        List<Long> actualIds = utilizationCycleJpaRepository.findAllByStatusAndCreatedAtLessThan(
                UtilizationCycleStatus.FINALIZED,
                LocalDateTime.of(2020, 3, 25, 12, 0)
        ).stream()
                .map(UtilizationCycle::getId)
                .collect(Collectors.toList());
        softly.assertThat(actualIds).containsExactlyInAnyOrder(2L);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/utilization-cycle/2/db-state.xml")
    public void findByVendorIdAndStatus() {
        List<Long> actualIds = utilizationCycleJpaRepository.findByVendorIdAndStatus(
                100500,
                UtilizationCycleStatus.FINALIZED
        ).stream()
                .map(UtilizationCycle::getId)
                .collect(Collectors.toList());
        softly.assertThat(actualIds).containsExactlyInAnyOrder(1L, 2L);

        List<UtilizationCycle> cycles =
                utilizationCycleJpaRepository.findByVendorIdAndStatus(100500, UtilizationCycleStatus.CREATED);
        softly.assertThat(cycles).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/utilization-cycle/2/db-state.xml")
    public void findAllByVendorIdAndStatusInOrderByIdAsc() {
        List<Long> actualIds = utilizationCycleJpaRepository.findAllByVendorIdAndStatusInOrderByIdAsc(
                100500,
                EnumSet.of(UtilizationCycleStatus.CREATED, UtilizationCycleStatus.FINALIZED)
        ).stream()
                .map(UtilizationCycle::getId)
                .collect(Collectors.toList());
        softly.assertThat(actualIds).containsExactly(1L, 2L);

        List<UtilizationCycle> cycles = utilizationCycleJpaRepository
                .findAllByVendorIdAndStatusInOrderByIdAsc(100500, EnumSet.of(UtilizationCycleStatus.CREATED));
        softly.assertThat(cycles).isEmpty();
    }
}
