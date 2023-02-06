package ru.yandex.market.mboc.common.offers.processing.counter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.offers.processing.counter.ProcessingCounterLogService.OLD_LOGS_CLEAN_BATCH_KEY;
import static ru.yandex.market.mboc.common.offers.processing.counter.ProcessingCounterLogService.OLD_LOGS_THRESHOLD_DAYS_KEY;
import static ru.yandex.market.mboc.common.offers.processing.counter.ProcessingCounterMetricsService.FREQUENT_CHANGES_THRESHOLD_KEY;
import static ru.yandex.market.mboc.common.offers.processing.counter.ProcessingCounterMetricsService.FREQUENT_CHANGES_TIMESPAN_DAYS_KEY;

public class ProcessingCounterLogRepositoryImplTest extends BaseDbTestClass {
    @Autowired
    private ProcessingCounterLogRepositoryImpl repository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() {
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void logChanges() {
        assertThat(repository.findAll()).isEmpty();

        var row0 = new ProcessingCounterChangeRow(
            0,
            10,
            LocalDateTime.now().minusHours(5),
            null,
            100,
            true
        );
        var row1 = new ProcessingCounterChangeRow(
            0,
            100,
            LocalDateTime.now().plusMonths(5),
            111,
            null,
            false
        );
        repository.logChanges(List.of(row0, row1));
        assertThat(repository.findAll()).hasSize(2).allMatch(row -> {
            if (row.getOfferId() == 10) {
                return row0.toBuilder().id(row.getId()).build().equals(row);
            } else if (row.getOfferId() == 100) {
                return row1.toBuilder().id(row.getId()).build().equals(row);
            } else {
                throw new RuntimeException();
            }
        });
    }

    @Test
    public void findOffersWithFrequentChangesCount() {
        final int threshold = 55;
        storageKeyValueService.putValue(FREQUENT_CHANGES_THRESHOLD_KEY, threshold);
        final Duration timespan = Duration.ofDays(80);
        storageKeyValueService.putValue(FREQUENT_CHANGES_TIMESPAN_DAYS_KEY, timespan.toDays());


        // Case 1: in timespan but not over threshold
        var case1 = new ProcessingCounterChangeRow(0, 100, LocalDateTime.now(), null, 1, false);
        repository.insertBatch(generateRows(case1).limit(threshold - 1).collect(Collectors.toList()));
        assertThat(repository.findOffersWithFrequentChangesCount(threshold, timespan)).isEqualTo(0);
        repository.deleteAll();
        // Case 2: in timespan and over threshold
        var case2 = new ProcessingCounterChangeRow(0, 200, LocalDateTime.now(), null, 1, false);
        repository.insertBatch(generateRows(case2).limit(threshold).collect(Collectors.toList()));
        assertThat(repository.findOffersWithFrequentChangesCount(threshold, timespan)).isEqualTo(1);
        repository.deleteAll();
        // Case 3: in timespan and over threshold but some are ignored
        var case3 = new ProcessingCounterChangeRow(0, 300, LocalDateTime.now(), null, 1, false);
        repository.insertBatch(generateRows(case3).limit(threshold - 1).collect(Collectors.toList()));
        repository.insertBatch(generateRows(case3.withIgnore(true)).limit(1).collect(Collectors.toList()));
        assertThat(repository.findOffersWithFrequentChangesCount(threshold, timespan)).isEqualTo(0);
        repository.deleteAll();
        // Case 4: not in timespan and over threshold
        var case4 = new ProcessingCounterChangeRow(0, 400, LocalDateTime.now(), null, 1, false);
        repository.insertBatch(generateRows(case4).limit(threshold - 1).collect(Collectors.toList()));
        repository.insertBatch(generateRows(case4.withTimestamp(LocalDateTime.now().minus(timespan).minusSeconds(1)))
            .limit(1).collect(Collectors.toList()));
        assertThat(repository.findOffersWithFrequentChangesCount(threshold, timespan)).isEqualTo(0);
        repository.deleteAll();
    }

    @Test
    public void deleteOldLogs() {
        final Duration threshold = Duration.ofDays(80);
        storageKeyValueService.putValue(OLD_LOGS_THRESHOLD_DAYS_KEY, threshold.toDays());
        final int batch = 35;
        storageKeyValueService.putValue(OLD_LOGS_CLEAN_BATCH_KEY, batch);

        var notOldEnough = new ProcessingCounterChangeRow(0, 50,
            LocalDateTime.now().minus(threshold).plusSeconds(1), null, 1, false);
        repository.insertBatch(generateRows(notOldEnough).limit(10).collect(Collectors.toList()));
        var oldEnough = new ProcessingCounterChangeRow(0, 100,
            LocalDateTime.now().minus(threshold).minusSeconds(1), null, 1, false);
        repository.insertBatch(generateRows(oldEnough).limit(20).collect(Collectors.toList()));
        var veryOld = new ProcessingCounterChangeRow(0, 200,
            LocalDateTime.now().minus(threshold).minus(threshold), null, 1, false);
        repository.insertBatch(generateRows(veryOld).limit(55).collect(Collectors.toList()));

        assertThat(repository.deleteOldLogs(threshold, batch)).isEqualTo(batch);
        assertThat(repository.findAll()).hasSize(50).filteredOn(it -> it.getOfferId() == 200).hasSize(20);
        assertThat(repository.deleteOldLogs(threshold, batch)).isEqualTo(batch);
        assertThat(repository.findAll()).hasSize(15).filteredOn(it -> it.getOfferId() == 200).isEmpty();
        assertThat(repository.deleteOldLogs(threshold, batch)).isEqualTo(5);
        assertThat(repository.findAll()).hasSize(10).allMatch(it -> it.getOfferId() == 50);
    }

    private Stream<ProcessingCounterChangeRow> generateRows(ProcessingCounterChangeRow base) {
        return generateRows(base, (r, __) -> r.withId(r.getId()));
    }

    private Stream<ProcessingCounterChangeRow> generateRows(
        ProcessingCounterChangeRow base,
        BiFunction<ProcessingCounterChangeRow, Integer, ProcessingCounterChangeRow> next
    ) {
        return IntStream.range(0, 1000000).mapToObj(i -> next.apply(base, i));
    }
}
