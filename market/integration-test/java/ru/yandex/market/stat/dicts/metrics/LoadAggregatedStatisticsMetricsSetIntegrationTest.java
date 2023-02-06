package ru.yandex.market.stat.dicts.metrics;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.common.DictionaryTypeBySize;
import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.records.TestDictionary;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;

import static java.lang.String.valueOf;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static ru.yandex.market.stat.dicts.common.DictionaryTypeBySize.BIG;
import static ru.yandex.market.stat.dicts.common.DictionaryTypeBySize.MEDIUM;
import static ru.yandex.market.stat.dicts.common.DictionaryTypeBySize.SLA;
import static ru.yandex.market.stat.dicts.common.DictionaryTypeBySize.SPECIAL;
import static ru.yandex.market.stat.dicts.common.DictionaryTypeBySize.TINY;
import static ru.yandex.market.stat.dicts.loaders.LoaderScale.DAYLY;

@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@Slf4j
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)
public class LoadAggregatedStatisticsMetricsSetIntegrationTest {

    private final static String DEFAULT_CLUSTER = "hahn";

    private final static long REVISION = 10L;
    private final static long MILLION = 1_000_000L;

    private final static long BIG_BYTES = 6L * 1024L * 1024L * 1024L;
    private final static long MEDIUM_BYTES = 512L * 1024L * 1024L;
    private final static long TINY_BYTES = 512L * 1024L;

    @Autowired
    private MetadataService metadataService;
    @Autowired
    @Qualifier("metadataTemplate")
    NamedParameterJdbcTemplate metadataTemplate;

    private LoadAggregatedStatisticsMetricsSet metricsSet;

    private LocalDateTime dayOne = LocalDate.now().minusDays(1).atStartOfDay();

    @Before
    public void setUp() {
        metricsSet = new LoadAggregatedStatisticsMetricsSet(metadataService);
        metadataTemplate.update("truncate table loads", new HashMap<>());
    }

    @Test
    public void noDataForToday() {
        Map<String, Metric> metrics = metricsSet.getMetrics();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(metrics).hasSize(15);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.queue_minutes.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.duration_secs.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.ready_time.pp95")).isEqualTo(0);

            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.queue_minutes.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.duration_secs.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.ready_time.pp95")).isEqualTo(0);

            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.queue_minutes.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.duration_secs.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.ready_time.pp95")).isEqualTo(0);

            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.queue_minutes.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.duration_secs.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.ready_time.pp95")).isEqualTo(0);

            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.queue_minutes.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.duration_secs.pp95")).isEqualTo(0);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.ready_time.pp95")).isEqualTo(0);
        });
    }

    @Test
    public void fewDictsAllBelowPercentile() {

        prepareForSizeQueueAndLoad(SLA, 1, 1 * 60 * 1000, 1);
        prepareForSizeQueueAndLoad(BIG, 2, 2 * 60 * 1000, 2);
        prepareForSizeQueueAndLoad(MEDIUM, 3, 3 * 60 * 1000, 3);
        prepareForSizeQueueAndLoad(TINY, 4, 4 * 60 * 1000, 4);
        prepareForSizeQueueAndLoad(SPECIAL, 5, 5 * 60 * 1000, 5);

        Map<String, Metric> metrics = metricsSet.getMetrics();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(metrics).hasSize(15);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.queue_minutes.pp95")).isEqualTo(1);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.duration_secs.pp95")).isEqualTo(1 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.ready_time.pp95"))
                    .isLessThan(now().minus(30, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.ready_time.pp95"))
                    .isGreaterThan(now().minus(90, SECONDS).toLocalTime().toSecondOfDay());

            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.queue_minutes.pp95")).isEqualTo(2);
            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.duration_secs.pp95")).isEqualTo(2 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.ready_time.pp95"))
                    .isLessThan(now().minus(90, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.ready_time.pp95"))
                    .isGreaterThan(now().minus(150, SECONDS).toLocalTime().toSecondOfDay());

            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.queue_minutes.pp95")).isEqualTo(3);
            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.duration_secs.pp95")).isEqualTo(3 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.ready_time.pp95"))
                    .isLessThan(now().minus(150, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.ready_time.pp95"))
                    .isGreaterThan(now().minus(210, SECONDS).toLocalTime().toSecondOfDay());

            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.queue_minutes.pp95")).isEqualTo(4);
            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.duration_secs.pp95")).isEqualTo(4 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.ready_time.pp95"))
                    .isLessThan(now().minus(210, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.ready_time.pp95"))
                    .isGreaterThan(now().minus(270, SECONDS).toLocalTime().toSecondOfDay());

            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.queue_minutes.pp95")).isEqualTo(5);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.duration_secs.pp95")).isEqualTo(5 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.ready_time.pp95"))
                    .isLessThan(now().minus(270, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.ready_time.pp95"))
                    .isGreaterThan(now().minus(330, SECONDS).toLocalTime().toSecondOfDay());
        });

        prepareForSizeQueueAndLoad(BIG, 1000000L, 1000000L, 0);

        Map<String, Metric> afterMetrics = metricsSet.getMetrics();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(getValue(afterMetrics, "aggregated_by_type.BIG.queue_minutes.pp95")).isEqualTo(2);
            softly.assertThat(getValue(afterMetrics, "aggregated_by_type.BIG.duration_secs.pp95")).isEqualTo(2 * 60);
        });
    }

    @Test
    public void fewDictsOnLinearIncrease() {
        for (int i = 1; i <= 100; i++) {
            prepareForSizeQueueAndLoad(SLA, i, i * 60 * 1000, i);
            prepareForSizeQueueAndLoad(BIG, i * 2, i * 2 * 60 * 1000, i * 2);
            prepareForSizeQueueAndLoad(MEDIUM, i * 3, i * 3 * 60 * 1000, i * 3);
            prepareForSizeQueueAndLoad(TINY, i * 4, i * 4 * 60 * 1000, i * 4);
            prepareForSizeQueueAndLoad(SPECIAL, i * 5, i * 5 * 60 * 1000, i * 5);
        }

        Map<String, Metric> metrics = metricsSet.getMetrics();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(metrics).hasSize(15);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.queue_minutes.pp95")).isEqualTo(95);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.duration_secs.pp95")).isEqualTo(95 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.ready_time.pp95"))
                    .isLessThan(now().minus(5 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.SLA.ready_time.pp95"))
                    .isGreaterThan(now().minus(6 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());

            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.queue_minutes.pp95")).isEqualTo(95 * 2);
            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.duration_secs.pp95")).isEqualTo(95 * 2 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.ready_time.pp95"))
                    .isLessThan(now().minus(11 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.BIG.ready_time.pp95"))
                    .isGreaterThan(now().minus(12 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());

            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.queue_minutes.pp95")).isEqualTo(95 * 3);
            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.duration_secs.pp95")).isEqualTo(95 * 3 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.ready_time.pp95"))
                    .isLessThan(now().minus(17 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.MEDIUM.ready_time.pp95"))
                    .isGreaterThan(now().minus(18 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());

            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.queue_minutes.pp95")).isEqualTo(95 * 4);
            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.duration_secs.pp95")).isEqualTo(95 * 4 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.ready_time.pp95"))
                    .isLessThan(now().minus(23 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.TINY.ready_time.pp95"))
                    .isGreaterThan(now().minus(24 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());

            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.queue_minutes.pp95")).isEqualTo(95 * 5);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.duration_secs.pp95")).isEqualTo(95 * 5 * 60);
            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.ready_time.pp95"))
                    .isLessThan(now().minus(29 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());
            softly.assertThat(getValue(metrics, "aggregated_by_type.SPECIAL.ready_time.pp95"))
                    .isGreaterThan(now().minus(30 * 60 + 30, SECONDS).toLocalTime().toSecondOfDay());
        });
    }

    private void prepareForSizeQueueAndLoad(DictionaryTypeBySize size,
                                            long minutesInQueue,
                                            long millisLoading,
                                            long minutesAgo) {
        switch (size) {
            case SLA:
                prepareSLA(minutesInQueue, millisLoading, minutesAgo);
                break;
            case BIG:
                save(minutesInQueue, millisLoading, BIG_BYTES, minutesAgo);
                break;
            case MEDIUM:
                save(minutesInQueue, millisLoading, MEDIUM_BYTES, minutesAgo);
                break;
            case TINY:
                save(minutesInQueue, millisLoading, TINY_BYTES, minutesAgo);
                break;
            case SPECIAL:
                prepareSpecial(minutesInQueue, millisLoading, minutesAgo);
                break;
            case UNKNOWN:
        }
    }

    private void prepareSLA(long minutesInQueue, long millisLoading, long minutesAgo) {
        RandomDictionary dictionary = getTestDictionary("");
        dictionary.setSla(true);
        metadataService.save(DEFAULT_CLUSTER, dictionary, dayOne, 5L, 10L, millisLoading, now(), minutesInQueue, 200L);
        hackHahnTimestamp(minutesAgo, dictionary.getName());
    }

    private void prepareSpecial(long minutesInQueue, long millisLoading, long minutesAgo) {
        RandomDictionary dict = getTestDictionary("axapta_inventtrans_open");
        metadataService.save(DEFAULT_CLUSTER, dict, dayOne, 5L, 10L, millisLoading, now(), minutesInQueue, 200L);
        hackHahnTimestamp(minutesAgo, dict.getName());
    }

    private void save(long minutesInQueue, long millisLoading, long size, long minutesAgo) {
        RandomDictionary dict = getTestDictionary("");
        metadataService.save(DEFAULT_CLUSTER, dict, dayOne, MILLION, REVISION, millisLoading, now(), minutesInQueue, size);
        hackHahnTimestamp(minutesAgo, dict.getName());
    }

    private void hackHahnTimestamp(long minusMinutes, String name) {
        metadataTemplate.update(
                "update loads set hahn = now() - interval '" + minusMinutes + " minutes' " +
                        "where dictionary = :dict",
                ImmutableMap.of("minutes", valueOf(minusMinutes), "dict", name));
    }

    private RandomDictionary getTestDictionary(String namePrefix) {
        String id = UUID.randomUUID().toString();
        return new RandomDictionary(
                namePrefix + id, "/tmp/path/" + id, TestDictionary.class, new HashMap<>(), DAYLY
        );
    }

    private static class RandomDictionary extends Dictionary<TestDictionary> {

        protected RandomDictionary(String name,
                                   String relativePath,
                                   Class<TestDictionary> recordClass,
                                   Map<String, DictionaryField> columns,
                                   LoaderScale scale) {
            super(name, relativePath, recordClass, columns, scale);
        }
    }

    private Long getValue(Map<String, Metric> res, String s) {
        return Optional.ofNullable((Gauge<Long>) res.get(s)).map(Gauge::getValue).orElse(null);
    }
}
