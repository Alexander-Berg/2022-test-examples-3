package ru.yandex.market.stat.dicts.scheduling;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.stat.dicts.common.DictionaryPartition;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.metrics.LoadStatisticsMetricsSet;
import ru.yandex.market.stat.dicts.records.TestDictionaryLoader;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stat.dicts.services.YtClusters;
import ru.yandex.market.stat.dicts.services.YtDictionaryStorage;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoadStatisticsTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Mock
    private MetadataService metadataService;
    @Mock
    private YtDictionaryStorage ytStorage;
    @Mock
    private YtClusters ytClusters;

    private LoadStatisticsMetricsSet metricsSet;

    private LocalDateTime dayOne = LocalDate.now().minusDays(1).atStartOfDay();
    private List<DictionaryLoadersHolder> dictionaryLoadersHolders;

    @Before
    public void setUp() {
        when(ytClusters.getNativeCluster()).thenReturn(DEFAULT_CLUSTER);
        TestDictionaryLoader loaderDefault = new TestDictionaryLoader(ytStorage, Arrays.asList("1", "2", "3"),
                LoaderScale.DEFAULT);
        TestDictionaryLoader loaderDaily = new TestDictionaryLoader(ytStorage, Arrays.asList("4", "5", "6"),
                LoaderScale.DAYLY);
        TestDictionaryLoader loaderHourly = new TestDictionaryLoader(ytStorage, Arrays.asList("7", "8", "9"),
                LoaderScale.HOURLY);
        dictionaryLoadersHolders = Collections.singletonList(new DictionaryLoadersHolder(Arrays.asList(loaderDefault,
                loaderHourly, loaderDaily)));
        metricsSet = new LoadStatisticsMetricsSet(dictionaryLoadersHolders, metadataService, ytClusters);
    }

    @Test
    public void testMetadataCalled() {
        when(metadataService.getLastLoadsFor3days(DEFAULT_CLUSTER)).thenReturn(getLatestLoads());
        when(metadataService.getFirstLoadForToday(DEFAULT_CLUSTER)).thenReturn(getTodayLoads());

        metricsSet.getMetrics();
        verify(metadataService).getLastLoadsFor3days(DEFAULT_CLUSTER);
        verify(metadataService).getFirstLoadForToday(DEFAULT_CLUSTER);
        Map<String, Metric> res = metricsSet.getMetrics();
        verify(ytClusters, times(3)).getNativeCluster();
        verifyNoMoreInteractions(metadataService);

        assertThat(res.size(), is(10));
        assertTrue(res.containsKey("loads_by_type.TINY.test_dictionary.size"));
        assertTrue(res.containsKey("loads_by_type.TINY.test_dictionary-1d.size"));

        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.size"), is(100L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.size"), is(200L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.queue_minutes"), is(5L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.queue_minutes"), is(2L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.duration"), is(1L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.duration"), is(2L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.time_ready"), is(600L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.time_ready"), is(-600L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.hahn_publish"), is(900L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.hahn_publish"), is(-600L));
    }

    private Long getValue(Map<String, Metric> res, String s) {
        return ((Gauge<Long>) res.get(s)).getValue();
    }


    private List<DictionaryPartition> getLatestLoads() {
        return Arrays.asList(
                DictionaryPartition.builder()
                        .dictionary("test_dictionary")
                        .scale(LoaderScale.DEFAULT)
                        .partition(LoaderScale.DEFAULT.formatPartition(dayOne))
                        .takeTimeMs(1000L)
                        .publishedTime(minutes(15L))
                        .readyTime(minutes(10L))
                        .tableSize(100)
                        .queueMinutes(5L)
                        .build(),

                DictionaryPartition.builder()
                        .dictionary("test_dictionary")
                        .scale(LoaderScale.DAYLY)
                        .partition(LoaderScale.DAYLY.formatPartition(dayOne.minusDays(1)))
                        .takeTimeMs(2000L)
                        .publishedTime(minutes(25L))
                        .readyTime(minutes(20L))
                        .queueMinutes(2L)
                        .tableSize(200)
                        .build(),

                DictionaryPartition.builder()
                        .dictionary("unknown_dictionary")
                        .scale(LoaderScale.DAYLY)
                        .partition(LoaderScale.DAYLY.formatPartition(dayOne))
                        .takeTimeMs(3000L)
                        .publishedTime(minutes(25L))
                        .readyTime(minutes(20L))
                        .queueMinutes(2L)
                        .tableSize(100)
                        .build()

        );
    }

    private List<DictionaryPartition> getTodayLoads() {
        return Arrays.asList(
                DictionaryPartition.builder()
                        .dictionary("test_dictionary")
                        .scale(LoaderScale.DEFAULT)
                        .partition(LoaderScale.DEFAULT.formatPartition(dayOne))
                        .takeTimeMs(1000L)
                        .publishedTime(minutes(15L))
                        .readyTime(minutes(10L))
                        .tableSize(100)
                        .queueMinutes(15L)
                        .build(),

                DictionaryPartition.builder()
                        .dictionary("unknown_dictionary")
                        .scale(LoaderScale.DAYLY)
                        .partition(LoaderScale.DAYLY.formatPartition(dayOne))
                        .takeTimeMs(3000L)
                        .publishedTime(minutes(25L))
                        .readyTime(minutes(20L))
                        .queueMinutes(2L)
                        .tableSize(100)
                        .build()
        );
    }

    private Time minutes(long min) {
        LocalTime localTime = LocalDate.now().atStartOfDay().plusMinutes(min).toLocalTime();
        return Time.valueOf(localTime);
    }

}
