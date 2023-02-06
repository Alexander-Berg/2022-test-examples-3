package ru.yandex.market.stat.dicts.metrics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.records.TestDictionaryLoader;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stat.dicts.services.YtClusters;
import ru.yandex.market.stat.dicts.services.YtDictionaryStorage;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.stat.dicts.metrics.LoadStatisticsMetricsSet.TIME_BEFORE_BB;

@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@Slf4j
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)
public class LoadStatisticsMetricsSetTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Autowired
    private MetadataService metadataService;
    @Mock
    private YtDictionaryStorage ytStorage;
    @Mock
    private YtClusters ytClusters;

    private LoadStatisticsMetricsSet metricsSet;

    private LocalDateTime dayOne = LocalDate.now().minusDays(1).atStartOfDay();
    private List<DictionaryLoadersHolder> dictionaryLoadersHolders;
    private TestDictionaryLoader loaderDaily;
    private TestDictionaryLoader loaderHourly;
    private TestDictionaryLoader loaderDefault;

    @Before
    public void setUp() {
        when(ytClusters.getNativeCluster()).thenReturn(DEFAULT_CLUSTER);
        loaderDefault = new TestDictionaryLoader(ytStorage, Arrays.asList("1", "2", "3"),
                LoaderScale.DEFAULT);
        loaderDaily = new TestDictionaryLoader(ytStorage, Arrays.asList("4", "5", "6"),
                LoaderScale.DAYLY);
        loaderHourly = new TestDictionaryLoader(ytStorage, Arrays.asList("7", "8", "9"),
                LoaderScale.HOURLY);
        dictionaryLoadersHolders = Collections.singletonList(new DictionaryLoadersHolder(Arrays.asList(loaderDefault,
                loaderHourly, loaderDaily)));
        metricsSet = new LoadStatisticsMetricsSet(dictionaryLoadersHolders, metadataService, ytClusters);
    }

    @Test
    public void testMetadataCalled() {

        givenDataInMetabase();

        Map<String, Metric> res = metricsSet.getMetrics();

        thenMetricsMatchExpected(res);

        whenNewLoadAdded();
        res = metricsSet.getMetrics();
        //ничего не поменялось, тк берем из кэша
        thenMetricsMatchExpected(res);
    }

    private Long getValue(Map<String, Metric> res, String s) {
        return ((Gauge<Long>) res.get(s)).getValue();
    }


    private void givenDataInMetabase() {
        long id = metadataService.save(DEFAULT_CLUSTER, loaderDaily.getDictionary(), dayOne, 5L, 10L, 1000L,
                LocalDate.now().atStartOfDay().plusMinutes(3), 2L, 100L);
        metadataService.publish(DEFAULT_CLUSTER, id, loaderDaily.getDictionary().getName());
        metadataService.save(DEFAULT_CLUSTER, loaderDaily.getDictionary(), dayOne, 5L, 10L, 2000L, LocalDateTime.now(), 15L, 200L);

        metadataService.save(DEFAULT_CLUSTER, loaderHourly.getDictionary(), dayOne, 10L, 11L, 20L, LocalDateTime.now(), 0L, 300L);

        metadataService.save(DEFAULT_CLUSTER, loaderDefault.getDictionary(), dayOne.minusDays(5), 5L, 10L, 3000L, LocalDateTime.now(),
                5L, 400L);
        metadataService.save(DEFAULT_CLUSTER, loaderDefault.getDictionary(), dayOne.minusDays(6), 5L, 10L, 4000L,
                LocalDateTime.now().minusHours(4), 25L, 500L);
    }

    private void whenNewLoadAdded() {
        metadataService.save(DEFAULT_CLUSTER, loaderDaily.getDictionary(), dayOne, 5L, 10L, 70000L, LocalDateTime.now(), 150L,
                500L);

    }

    private void thenMetricsMatchExpected(Map<String, Metric> res) {
        assertThat(res.size(), Matchers.is(10));
        assertTrue(res.containsKey("loads_by_type.TINY.test_dictionary.size"));
        assertTrue(res.containsKey("loads_by_type.TINY.test_dictionary-1d.size"));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.size"), Matchers.is(400L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.size"), Matchers.is(200L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.queue_minutes"), Matchers.is(5L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.queue_minutes"), Matchers.is(15L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.duration"), Matchers.is(3L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.duration"), Matchers.is(2L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.time_ready"), Matchers.is(TIME_BEFORE_BB));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.time_ready"), Matchers.greaterThan(0L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.hahn_publish"), Matchers.is(TIME_BEFORE_BB));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.hahn_publish"), Matchers.greaterThan(0L));
    }

    private void thenMetricsAreAnother(Map<String, Metric> res) {
        assertThat(res.size(), Matchers.is(10));
        assertTrue(res.containsKey("loads_by_type.TINY.test_dictionary.size"));
        assertTrue(res.containsKey("loads_by_type.TINY.test_dictionary-1d.size"));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.size"), Matchers.is(400L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.size"), Matchers.is(500L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.queue_minutes"), Matchers.is(5L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.queue_minutes"), Matchers.is(150L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.duration"), Matchers.is(3L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.duration"), Matchers.is(70L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.time_ready"), Matchers.is(TIME_BEFORE_BB));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.time_ready"), Matchers.greaterThan(0L));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary.hahn_publish"), Matchers.is(TIME_BEFORE_BB));
        assertThat(getValue(res, "loads_by_type.TINY.test_dictionary-1d.hahn_publish"), Matchers.greaterThan(0L));
    }
}
