package ru.yandex.market.fulfillment.stockstorage;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.repository.FFIntervalRepository;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.FFInterval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FFIntervalRepositoryTest extends AbstractContextualTest {

    @Autowired
    private FFIntervalRepository ffIntervalRepository;

    @Test
    public void syncSchedulerCounterReturnsNonNullPositiveUniqueValueEachCall() {
        int random = ThreadLocalRandom.current().nextInt(10, 31);
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < random; i++) {
            set.add(ffIntervalRepository.nextSyncSchedulerCounterValue());
        }

        Set<Integer> filteredSet = set.stream()
                .filter(Objects::nonNull)
                .filter(counter -> counter > 0)
                .collect(Collectors.toSet());

        assertEquals("Set must contains random number of elements", random, filteredSet.size());
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/repository/1/before.xml")
    public void shouldFindFFIntervalsByWarehouseIdsAndSyncJobName() {
        ImmutableSet<Integer> warehouseIds = ImmutableSet.of(1, 2);
        final String syncJobName = "job1";

        List<FFInterval> ffIntervals = ffIntervalRepository.findAllByWarehouseIdsAndSyncJobName(
                warehouseIds,
                List.of(syncJobName)
        );

        assertNotNull(ffIntervals);
        assertEquals(2, ffIntervals.size());
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/repository/2/before.xml")
    public void shouldNotFindFFIntervalsByWarehouseIdsAndSyncJobName() {
        ImmutableSet<Integer> warehouseIds = ImmutableSet.of(1, 2);
        final String syncJobName = "job1";

        List<FFInterval> ffIntervals = ffIntervalRepository.findAllByWarehouseIdsAndSyncJobName(
                warehouseIds,
                List.of(syncJobName)
        );

        assertNotNull(ffIntervals);
        assertEquals(0, ffIntervals.size());
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/repository/1/before.xml")
    public void shouldFindFFIntervalByWarehouseIdAndSyncJobName() {
        final String syncJobName = "job1";

        Optional<FFInterval> maybeInterval = ffIntervalRepository.findByWarehouseIdAndSyncJobName(1, syncJobName);
        assertTrue(maybeInterval.isPresent());
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/repository/2/before.xml")
    public void shouldNotFindFFIntervalByWarehouseIdsAndSyncJobName() {
        final String syncJobName = "job1";

        Optional<FFInterval> maybeInterval = ffIntervalRepository.findByWarehouseIdAndSyncJobName(1, syncJobName);

        assertFalse(maybeInterval.isPresent());
    }

}
