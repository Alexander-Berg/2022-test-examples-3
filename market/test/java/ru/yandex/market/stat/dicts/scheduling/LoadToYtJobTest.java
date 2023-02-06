package ru.yandex.market.stat.dicts.scheduling;

import com.codahale.metrics.MetricRegistry;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.records.TestDictionary;
import ru.yandex.market.stat.dicts.services.DictionaryYtService;
import ru.yandex.market.stat.dicts.services.JugglerEventsSender;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stat.dicts.services.YtClusters;
import ru.yandex.market.stat.utils.DateUtil;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class LoadToYtJobTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    private static final Clock TEST_CLOCK = DateUtil.fixedClock("2018-05-01T05:30:00");

    private static final LocalDateTime TODAY = LocalDate.parse("2018-05-01").atStartOfDay();
    private static final LocalDateTime YESTERDAY = TODAY.minusDays(1);
    private static final LocalDateTime BEFORE_YESTERDAY = YESTERDAY.minusDays(1);
    private static final LocalDateTime TWO_MONTHS_AGO = YESTERDAY.minusMonths(2);

    @Mock
    private DictionaryLoader loader;

    @Mock
    private MetadataService metadataService;

    @Mock
    private DictionaryYtService ytService;

    @Mock
    private YtClusters ytClusters;

    @Mock
    private JugglerEventsSender jugglerEventsSender;

    @Mock
    private MetricRegistry metricRegistry;

    private LoadToYtJob loadToYtJob;
    private Dictionary<TestDictionary> dictionary;

    @Before
    public void setUp() {
        LoadToYtJob.clock = TEST_CLOCK;
        loadToYtJob = new LoadToYtJob(loader, metadataService, ytClusters, jugglerEventsSender, metricRegistry);

        dictionary = Dictionary.fromClass(TestDictionary.class);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);
        Mockito.when(loader.allowEmpty()).thenReturn(false);
    }

    @Test
    public void getLoadDayWithFirstLoadCase() throws IOException {
        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(YESTERDAY));
    }

    @Test
    public void getLoadDayWithOnlyNeedLoad() throws IOException {
        // Given
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(BEFORE_YESTERDAY.toLocalDate())
                .loadTime(BEFORE_YESTERDAY)
                .loadPartition(dictionary.tablePartition(BEFORE_YESTERDAY))
                .build()
        );
        Mockito.when(loader.onlyByNeedLoad()).thenReturn(true);
        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, BEFORE_YESTERDAY, YESTERDAY)).thenReturn(true);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(YESTERDAY));
    }

    @Test
    public void getLoadDayWithOnlyNeedLoadNegative() throws IOException {
        // Given
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(BEFORE_YESTERDAY.toLocalDate())
                .loadTime(BEFORE_YESTERDAY)
                .loadPartition(dictionary.tablePartition(BEFORE_YESTERDAY))
                .build()
        );
        Mockito.when(loader.onlyByNeedLoad()).thenReturn(true);
        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, BEFORE_YESTERDAY, YESTERDAY)).thenReturn(false);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test
    public void getLoadDayWithNewDayAndPathNotExists() throws IOException {
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(YESTERDAY.toLocalDate())
                .loadTime(TODAY.minusSeconds(1))
                .loadPartition(dictionary.tablePartition(YESTERDAY))
                .build()
        );
        Mockito.when(loader.sourceExistsForDay(DEFAULT_CLUSTER, YESTERDAY)).thenReturn(false);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test
    public void getLoadDayWithNewDayAndPathExists() throws IOException {
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(YESTERDAY.toLocalDate())
                .loadTime(TODAY.minusSeconds(1))
                .loadPartition(dictionary.tablePartition(YESTERDAY))
                .build()
        );
        Mockito.when(loader.sourceExistsForDay(DEFAULT_CLUSTER, YESTERDAY)).thenReturn(true);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(YESTERDAY));
    }

    @Test
    public void getLoadHourlyNextHour() throws IOException {
        dictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.HOURLY);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);

        assertThat(loader.getDictionary().getScale(), equalTo(LoaderScale.HOURLY));

        LocalDateTime lastLoadTime = LocalDateTime.now(TEST_CLOCK).minusHours(1).minusMinutes(20); //2018-05-01T04:30:00
        LocalDateTime lastPartition = DateUtil.atHourStart(lastLoadTime.minusHours(1)); //2018-05-01T03:00:00
        Mockito.when(loader.sourceExistsForDay(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(lastPartition.toLocalDate())
                .loadTime(lastLoadTime)
                .scale(dictionary.getScale().getName())
                .loadPartition(dictionary.tablePartition(lastPartition))
                .build()
        );
        Mockito.when(loader.onlyByNeedLoad()).thenReturn(false);
        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(TODAY.plusHours(4))); //2018-05-01T04:00:00
    }

    @Test
    public void getLoadHourlyNextHourByNeedLoad() throws IOException {
        dictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.HOURLY);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);

        assertThat(loader.getDictionary().getScale(), equalTo(LoaderScale.HOURLY));

        LocalDateTime lastLoadTime = LocalDateTime.now(TEST_CLOCK).minusHours(1).minusMinutes(20); //2018-05-01T04:30:00
        LocalDateTime lastPartition = DateUtil.atHourStart(lastLoadTime.minusHours(1)); //2018-05-01T03:00:00
        Mockito.when(loader.onlyByNeedLoad()).thenReturn(true);
        Mockito.when(loader.needLoad(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
                MetadataService.DictionaryLoadInfo.builder()
                        .day(lastPartition.toLocalDate())
                        .loadTime(lastLoadTime)
                        .scale(dictionary.getScale().getName())
                        .loadPartition(dictionary.tablePartition(lastPartition))
                        .build()
        );

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));

        Mockito.when(loader.needLoad(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);
        assertThat(result.isPresent(), equalTo(true));
    }

    @Test
    public void getLoadHourlyThisHour() throws IOException {
        dictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.HOURLY);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);
        assertThat(loader.getDictionary().getScale(), equalTo(LoaderScale.HOURLY));

        LocalDateTime lastLoadTime = LocalDateTime.now(TEST_CLOCK).minusMinutes(20);
        LocalDateTime lastPartition = DateUtil.atHourStart(lastLoadTime.minusHours(1));
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(lastPartition.toLocalDate())
                .loadTime(lastLoadTime)
                .scale(dictionary.getScale().getName())
                .loadPartition(dictionary.tablePartition(lastPartition))
                .build()
        );
        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));
    }


    @Test
    public void getLoadMonthlyLastLoadYesterday() throws IOException {
        dictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.DEFAULT_MONTH);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);
        assertThat(loader.getDictionary().getScale(), equalTo(LoaderScale.DEFAULT_MONTH));

        LocalDateTime lastLoadTime = YESTERDAY.plusHours(23).plusMinutes(20); //2018-04-30T23:20:00
        LocalDateTime lastPartition = YESTERDAY; //2018-04-30T00:00:00
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(lastPartition.toLocalDate())
                .loadTime(lastLoadTime)
                .scale(dictionary.getScale().getName())
                .loadPartition(dictionary.tablePartition(lastPartition))
                .build()
        );
        Mockito.when(loader.sourceExistsForDay(DEFAULT_CLUSTER, TODAY)).thenReturn(true);
        assertThat(metadataService.getLast(dictionary, false).loadPartitionAsDt(),
                                           equalTo(LocalDate.parse("2018-04-01").atStartOfDay()));

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(TODAY));
    }

    @Test
    public void getLoadMonthlyLastLoadTodayNeedReload() throws IOException {
        dictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.DEFAULT_MONTH);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);
        assertThat(loader.getDictionary().getScale(), equalTo(LoaderScale.DEFAULT_MONTH));

        LocalDateTime lastLoadTime = TODAY.plusHours(3).plusMinutes(20); //2018-05-01T03:20:00
        LocalDateTime lastPartition = TODAY; //2018-05-01T00:00:00
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(lastPartition.toLocalDate())
                .loadTime(lastLoadTime)
                .scale(dictionary.getScale().getName())
                .loadPartition(dictionary.tablePartition(lastPartition))
                .build()
        );
        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, lastLoadTime, TODAY)).thenReturn(true);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(TODAY));
    }

    @Test
    public void getLoadMonthlyLastLoadTodayReloadNotNeeded() throws IOException {
        dictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.DEFAULT_MONTH);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);
        assertThat(loader.getDictionary().getScale(), equalTo(LoaderScale.DEFAULT_MONTH));

        LocalDateTime lastLoadTime = TODAY.plusHours(3).plusMinutes(20); //2018-05-01T03:20:00
        LocalDateTime lastPartition = TODAY; //2018-05-01T00:00:00
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(lastPartition.toLocalDate())
                .loadTime(lastLoadTime)
                .scale(dictionary.getScale().getName())
                .loadPartition(dictionary.tablePartition(lastPartition))
                .build()
        );

        assertThat(metadataService.getLast(dictionary, false).loadPartitionAsDt(),
                                                         equalTo(LocalDate.parse("2018-05-01").atStartOfDay()));

        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, lastLoadTime, TODAY)).thenReturn(false);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test
    public void getLoadDayWithLoadDayBeforeBeforeYesterdayAndPathNotExists() throws IOException {
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(BEFORE_YESTERDAY.minusDays(1).toLocalDate())
                .loadTime(YESTERDAY)
                .loadPartition(dictionary.tablePartition(BEFORE_YESTERDAY.minusDays(1)))
                .build()
        );
        Mockito.when(loader.sourceExistsForDay(DEFAULT_CLUSTER, BEFORE_YESTERDAY)).thenReturn(false);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test
    public void getLoadDayWithLoadDayBeforeBeforeYesterdayAndPathExists() throws IOException {
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(BEFORE_YESTERDAY.minusDays(1).toLocalDate())
                .loadTime(YESTERDAY)
                .loadPartition(dictionary.tablePartition(BEFORE_YESTERDAY.minusDays(1)))
                .build()
        );
        Mockito.when(loader.sourceExistsForDay(DEFAULT_CLUSTER, BEFORE_YESTERDAY)).thenReturn(true);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(BEFORE_YESTERDAY));
    }

    @Test
    public void getLoadDayWithLoadDayBeforeYesterdayAndPathNotExists() throws IOException {
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(BEFORE_YESTERDAY.toLocalDate())
                .loadTime(TODAY)
                .loadPartition(dictionary.tablePartition(BEFORE_YESTERDAY))
                .build()
        );
        Mockito.when(loader.sourceExistsForDay(DEFAULT_CLUSTER, YESTERDAY)).thenReturn(false);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test
    public void getLoadDayWithLoadDayBeforeYesterdayAndPathExists() throws IOException {
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(BEFORE_YESTERDAY.toLocalDate())
                .loadTime(TODAY)
                .loadPartition(dictionary.tablePartition(BEFORE_YESTERDAY))
                .build()
        );
        Mockito.when(loader.sourceExistsForDay(DEFAULT_CLUSTER, YESTERDAY)).thenReturn(true);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(YESTERDAY));
    }

    @Test
    public void getLoadDayWithReloadTodayAndPathExists() throws IOException {

        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(YESTERDAY.toLocalDate())
                .loadTime(TODAY)
                .loadPartition(dictionary.tablePartition(YESTERDAY))
                .build()
        );

        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, TODAY, TODAY)).thenReturn(true);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(TODAY));
    }

    @Test
    public void getLoadDayWithEmptyHistoryStart() throws IOException {

        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(TODAY.toLocalDate())
                .loadTime(TODAY)
                .loadPartition(dictionary.tablePartition(TODAY))
                .build()
        );

        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, TODAY, TODAY)).thenReturn(false);
        Mockito.when(loader.historyStart()).thenReturn(null);

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test
    public void getLoadDayWithHistoryStartAfterOldest() throws IOException {

        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(TODAY.toLocalDate())
                .loadTime(TODAY)
                .loadPartition(dictionary.tablePartition(TODAY))
                .build()
        );

        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, TODAY, TODAY)).thenReturn(false);
        Mockito.when(loader.historyStart()).thenReturn(BEFORE_YESTERDAY.toLocalDate());

        Mockito.when(metadataService.getOldest(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(BEFORE_YESTERDAY.toLocalDate())
                .loadTime(BEFORE_YESTERDAY)
                .loadPartition(dictionary.tablePartition(BEFORE_YESTERDAY))
                .build()
        );

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test
    public void getLoadDayWithHistoryStartBeforeOldest() throws IOException {

        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(TODAY.toLocalDate())
                .loadTime(TODAY)
                .loadPartition(dictionary.tablePartition(TODAY))
                .build()
        );

        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, TODAY, TODAY)).thenReturn(false);
        Mockito.when(loader.historyStart()).thenReturn(BEFORE_YESTERDAY.toLocalDate());

        Mockito.when(metadataService.getOldest(dictionary, false)).thenReturn(
            MetadataService.DictionaryLoadInfo.builder()
                .day(YESTERDAY.toLocalDate())
                .loadTime(YESTERDAY)
                .loadPartition(dictionary.tablePartition(YESTERDAY))
                .build()
        );

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(BEFORE_YESTERDAY));
    }

    @Test
    public void getLoadDayWithHistoryStartBeforeOldestForMonthly() throws IOException {

        dictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.DEFAULT_MONTH);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);

        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
                MetadataService.DictionaryLoadInfo.builder()
                        .day(TODAY.toLocalDate())
                        .loadTime(TODAY)
                        .loadPartition(dictionary.tablePartition(TODAY))
                        .build()
        );

        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, TODAY, TODAY)).thenReturn(false);
        Mockito.when(loader.historyStart()).thenReturn(TWO_MONTHS_AGO.toLocalDate());

        Mockito.when(metadataService.getOldest(dictionary, false)).thenReturn(
                MetadataService.DictionaryLoadInfo.builder()
                        .day(YESTERDAY.toLocalDate())
                        .loadTime(YESTERDAY)
                        .loadPartition(dictionary.tablePartition(YESTERDAY))
                        .build()
        );

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(YESTERDAY.withDayOfMonth(1).minusDays(1)));
    }

    @Test
    public void getLoadDayWithHistoryStartAfterOldestForMonthly() throws IOException {

        dictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.DEFAULT_MONTH);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);

        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
                MetadataService.DictionaryLoadInfo.builder()
                        .day(TODAY.toLocalDate())
                        .loadTime(TODAY)
                        .loadPartition(dictionary.tablePartition(TODAY))
                        .build()
        );

        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, TODAY, TODAY)).thenReturn(false);
        Mockito.when(loader.historyStart()).thenReturn(YESTERDAY.toLocalDate());

        Mockito.when(metadataService.getOldest(dictionary, false)).thenReturn(
                MetadataService.DictionaryLoadInfo.builder()
                        .day(YESTERDAY.toLocalDate())
                        .loadTime(YESTERDAY)
                        .loadPartition(dictionary.tablePartition(YESTERDAY))
                        .build()
        );

        // When
        Optional<LocalDateTime> result = loadToYtJob.getLoadPartition(DEFAULT_CLUSTER);

        // Then
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test
    @UseDataProvider("combinations")
    public void getPartitionToReloadForScales() throws IOException {

        //1е число месяца
        checkPartitionsForReload("2018-05-01T05:30:00", LoaderScale.DEFAULT_MONTH,
                "2018-05-01", equalTo(LocalDate.parse("2018-04-30").atStartOfDay()));

        //2е число месяца
        checkPartitionsForReload("2018-05-02T05:30:00", LoaderScale.DEFAULT_MONTH,
                "2018-05-02", equalTo(LocalDate.parse("2018-04-30").atStartOfDay()));

        //3е число месяца
        checkPartitionsForReload("2018-05-03T05:30:00", LoaderScale.DEFAULT_MONTH,
                "2018-05-03", Matchers.nullValue());

        //1е число месяца, но мы и так перевыгружаем за вчера
        checkPartitionsForReload("2018-05-01T05:30:00", LoaderScale.DEFAULT_MONTH,
                "2018-04-30", Matchers.nullValue());

        //1е число месяца, но мы и перевыгружаем  какую-то древность
        checkPartitionsForReload("2018-05-01T05:30:00", LoaderScale.DEFAULT_MONTH,
                "2018-02-28", Matchers.nullValue());

        //не актуально для дефолтового скейла
        checkPartitionsForReload("2018-05-01T05:30:00", LoaderScale.DEFAULT,
                "2018-05-01", Matchers.nullValue());

        //не актуально для дневного скейла
        checkPartitionsForReload("2018-05-01T05:30:00", LoaderScale.DAYLY,
                "2018-05-01", Matchers.nullValue());

        //не актуально для часового скейла
        checkPartitionsForReload("2018-05-01T05:30:00", LoaderScale.HOURLY,
                "2018-05-01", Matchers.nullValue());
    }

    private void checkPartitionsForReload(String currentTime, LoaderScale scale, String suggestedPartition, Matcher matcher) throws IOException {
        LoadToYtJob.clock = DateUtil.fixedClock(currentTime);
        LocalDateTime suggested = LocalDate.parse(suggestedPartition).atStartOfDay();
        loadToYtJob = new LoadToYtJob(loader, metadataService, ytClusters, jugglerEventsSender, metricRegistry);

        dictionary = Dictionary.fromClass(TestDictionary.class, scale);
        Mockito.when(loader.getDictionary()).thenReturn(dictionary);
        Mockito.when(loader.getScale()).thenReturn(scale);
        Mockito.when(metadataService.getLast(dictionary, false)).thenReturn(
                MetadataService.DictionaryLoadInfo.builder()
                        .day(suggested.minusDays(1).toLocalDate())
                        .loadTime(suggested.minusDays(1))
                        .loadPartition(dictionary.tablePartition(suggested.minusDays(1)))
                        .build()
        );

        Mockito.when(loader.needLoad(DEFAULT_CLUSTER, suggested, suggested)).thenReturn(true);
        assertThat(loadToYtJob.getPartitionsToReload(suggested), matcher);
    }

}
