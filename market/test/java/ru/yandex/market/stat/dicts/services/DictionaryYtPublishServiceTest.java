package ru.yandex.market.stat.dicts.services;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.TestLoader;
import ru.yandex.market.stat.dicts.records.TestDictionary;
import ru.yandex.market.stat.dicts.services.YtClusters;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.stat.dicts.config.TestConstants.ANOTHER_TEST_CLUSTER;
import static ru.yandex.market.stat.dicts.config.TestConstants.TEST_CLUSTER;

/**
 * Created by kateleb on 03.05.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class DictionaryYtPublishServiceTest {
    private static final String DEFAULT_CLUSTER = "hahn";
    private static final String ANOTHER_CLUSTER = "arnold";

    private final LocalDateTime todayDt = LocalDate.now().atStartOfDay();

    private final LocalDateTime yesterdayDt = todayDt.minusDays(1);

    private final LocalDateTime beforeYesterdayDt = todayDt.minusDays(2);
    private final LocalDate today = todayDt.toLocalDate();
    private final LocalDate yesterday = yesterdayDt.toLocalDate();
    private final LocalDate beforeYesterday = beforeYesterdayDt.toLocalDate();

    private Dictionary dictionary = Dictionary.fromClass(TestDictionary.class);

    @Mock
    private YtClusters ytClusters;

    @Mock
    private DictionaryYtService ytService;

    @Mock
    private DictionaryYtService anotherYtService;

    @Mock
    private MetadataService metadataService;

    @Mock
    private StepEventsSender stepEventsSender;
    @Mock
    private ReactorArtifactsSender reactorArtifactsSender;

    private List<DictionaryLoadersHolder> loadersHolders;

    private DictionaryYtPublishService publisher;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        loadersHolders = new ArrayList<>();
        loadersHolders.add(new DictionaryLoadersHolder(Arrays.asList(
                new TestLoader(LoaderScale.DEFAULT),
                new TestLoader(LoaderScale.DAYLY),
                new TestLoader(LoaderScale.HOURLY))));
        publisher = new DictionaryYtPublishService(
                metadataService, stepEventsSender, reactorArtifactsSender,
                loadersHolders, ytClusters, "production"
        );
        when(ytClusters.getYtService(DEFAULT_CLUSTER)).thenReturn(ytService);
        when(ytClusters.getYtService(ANOTHER_CLUSTER)).thenReturn(anotherYtService);
    }

    @Test
    public void testAllPublished() {
        when(metadataService.getUnpublished(DEFAULT_CLUSTER, dictionary, false)).thenReturn(Collections.emptyList());
        publisher.publish(DEFAULT_CLUSTER, dictionary);
        verify(metadataService).getUnpublished(DEFAULT_CLUSTER, dictionary, false);
        verify(ytClusters).getYtService(any());
        verifyNoMoreInteractions(ytClusters);
        verifyNoMoreInteractions(metadataService);
        verifyNoMoreInteractions(stepEventsSender);
        verify(ytService, Mockito.never()).setTtl(any(), anyLong());
    }

    @Test
    public void testAllPublishedOnAnotherCluster() {
        when(metadataService.getUnpublished(ANOTHER_CLUSTER, dictionary, false)).thenReturn(Collections.emptyList());
        publisher.publish(ANOTHER_CLUSTER, dictionary);
        verify(metadataService).getUnpublished(ANOTHER_CLUSTER, dictionary, false);
        verify(ytClusters).getYtService(any());
        verifyNoMoreInteractions(ytClusters);
        verifyNoMoreInteractions(metadataService);
        verifyNoMoreInteractions(stepEventsSender);
        verify(anotherYtService, Mockito.never()).setTtl(any(), anyLong());
    }

    @Test
    public void testUnpublishedOldDaysWhenLatestPublished() {
        LoaderScale scale = dictionary.getScale();
        when(metadataService.getUnpublished(DEFAULT_CLUSTER, dictionary, false)).thenReturn(Arrays.asList(
            new MetadataService.DictionaryLoadInfo(
                1, dictionary.getName(), 15, 123, yesterday, LocalDateTime.now(), false,
                scale.getName(), dictionary.tablePartition(yesterdayDt), null, null),
            new MetadataService.DictionaryLoadInfo(
                2, dictionary.getName(), 15, 456, beforeYesterday, LocalDateTime.now().minusDays(1), false,
                scale.getName(), dictionary.tablePartition(beforeYesterdayDt), null, null)
        ));
        when(metadataService.getLastPublished(DEFAULT_CLUSTER, dictionary, false))
            .thenReturn(new MetadataService.DictionaryLoadInfo(
                3, dictionary.getName(), 13, -1, today, LocalDateTime.now(), false,
                scale.getName(), dictionary.tablePartition(todayDt), null, null));
        when(metadataService.publishedExists(DEFAULT_CLUSTER, dictionary, yesterdayDt,false)).thenReturn(false);
        when(metadataService.publishedExists(DEFAULT_CLUSTER, dictionary, beforeYesterdayDt, false)).thenReturn(false);

        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(yesterdayDt))).thenReturn(tablepath(dictionary, yesterdayDt));
        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(beforeYesterdayDt))).thenReturn(tablepath(dictionary, beforeYesterdayDt));
        when(ytService.getLoadRevision(tablepath(dictionary, yesterdayDt))).thenReturn(123L);
        when(ytService.getLoadRevision(tablepath(dictionary, beforeYesterdayDt))).thenReturn(456L);
        when(ytService.partitionExists(any(),any())).thenReturn(true);

        dictionary.setTtlDays(2L);

        publisher.publish(DEFAULT_CLUSTER, dictionary);

        verify(metadataService).getUnpublished(DEFAULT_CLUSTER, dictionary, false);
        verify(metadataService).getLastPublished(DEFAULT_CLUSTER, dictionary, false);
        verify(metadataService, times(2)).publishedExists(eq(DEFAULT_CLUSTER), any(), any(), any());
        verify(metadataService).publish(DEFAULT_CLUSTER, 1, dictionary.getName());
        verify(metadataService).publish(DEFAULT_CLUSTER, 2, dictionary.getName());

        verify(ytService, Mockito.times(1)).setTtl(tablepath(dictionary, yesterdayDt), 172800L);
        verify(ytService, Mockito.times(1)).setTtl(tablepath(dictionary, beforeYesterdayDt), 172800L);
        verify(ytService, Mockito.never()).setTtl(tablepath(dictionary, todayDt), 172800L);
        verify(anotherYtService, Mockito.never()).setTtl(any(), anyLong());

        verify(stepEventsSender).sendEvent(dictionary.getName(), yesterdayDt, DEFAULT_CLUSTER, tablepath(dictionary, yesterdayDt), scale, false);
        verify(stepEventsSender).sendEvent(dictionary.getName(), beforeYesterdayDt, DEFAULT_CLUSTER, tablepath(dictionary, beforeYesterdayDt), scale, false);
        verify(ytService, times(2)).dictionaryPartitionTable(any(), any(String.class));
        verify(ytService).getLoadRevision(tablepath(dictionary, yesterdayDt));
        verify(ytService).getLoadRevision(tablepath(dictionary, beforeYesterdayDt));
        verify(ytService, times(2)).partitionExists(any(), any());
        verifyNoMoreInteractions(metadataService);
        verifyNoMoreInteractions(ytService);
        verifyNoMoreInteractions(stepEventsSender);
        verifyNoMoreInteractions(anotherYtService);
    }

    @Test
    public void testUnpublishedForAlreadyPublishedDays() {
        LoaderScale scale = dictionary.getScale();
        when(metadataService.getUnpublished(DEFAULT_CLUSTER, dictionary, false)).thenReturn(Arrays.asList(
            new MetadataService.DictionaryLoadInfo(
                1, dictionary.getName(), 15, 123, yesterday, LocalDateTime.now(), false,
                dictionary.getScale().getName(), dictionary.tablePartition(yesterdayDt), null, null),
            new MetadataService.DictionaryLoadInfo(
                2, dictionary.getName(), 15, 456, beforeYesterday, LocalDateTime.now().minusDays(1), false,
                dictionary.getScale().getName(), dictionary.tablePartition(beforeYesterdayDt), null, null)
        ));
        when(metadataService.getLastPublished(DEFAULT_CLUSTER, dictionary, false))
            .thenReturn(new MetadataService.DictionaryLoadInfo(
                3, dictionary.getName(), 13, -1, today, LocalDateTime.now(), false,
                dictionary.getScale().getName(), dictionary.tablePartition(todayDt), null, null));
        when(metadataService.publishedExists(DEFAULT_CLUSTER, dictionary, yesterdayDt, false)).thenReturn(true);
        when(metadataService.publishedExists(DEFAULT_CLUSTER, dictionary, beforeYesterdayDt, false)).thenReturn(true);

        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(yesterdayDt))).thenReturn(tablepath(dictionary, yesterdayDt));
        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(beforeYesterdayDt))).thenReturn(tablepath(dictionary, beforeYesterdayDt));
        when(ytService.getLoadRevision(tablepath(dictionary, yesterdayDt))).thenReturn(123L);
        when(ytService.getLoadRevision(tablepath(dictionary, beforeYesterdayDt))).thenReturn(456L);
        when(ytService.partitionExists(any(), any())).thenReturn(true);

        dictionary.setTtlDays(2L);

        publisher.publish(DEFAULT_CLUSTER, dictionary);
        verify(metadataService).getUnpublished(DEFAULT_CLUSTER, dictionary, false);
        verify(metadataService).getLastPublished(DEFAULT_CLUSTER, dictionary, false);

        verify(ytService).getLoadRevision(tablepath(dictionary, yesterdayDt));
        verify(ytService).getLoadRevision(tablepath(dictionary, beforeYesterdayDt));
        verify(metadataService, times(2)).publishedExists(eq(DEFAULT_CLUSTER), any(), any(), any());
        verify(metadataService).publish(DEFAULT_CLUSTER, 1, dictionary.getName());
        verify(metadataService).publish(DEFAULT_CLUSTER, 2, dictionary.getName());

        verify(ytService, Mockito.times(1)).setTtl(tablepath(dictionary, yesterdayDt), 172800L);
        verify(ytService, Mockito.times(1)).setTtl(tablepath(dictionary, beforeYesterdayDt), 172800L);
        verify(ytService, Mockito.never()).setTtl(tablepath(dictionary, todayDt), 172800L);
        verify(anotherYtService, Mockito.never()).setTtl(any(), anyLong());

        verify(stepEventsSender).sendEvent(dictionary.getName(), yesterdayDt, DEFAULT_CLUSTER, tablepath(dictionary, yesterdayDt), scale,true);
        verify(stepEventsSender).sendEvent(dictionary.getName(), beforeYesterdayDt, DEFAULT_CLUSTER, tablepath(dictionary, beforeYesterdayDt), scale, true);
        verify(ytService, times(2)).dictionaryPartitionTable(any(), any(String.class));
        verify(ytService, times(2)).partitionExists(any(), any());

        verifyNoMoreInteractions(metadataService);
        verifyNoMoreInteractions(ytService);
        verifyNoMoreInteractions(stepEventsSender);
        verifyNoMoreInteractions(anotherYtService);
    }

    @Test
    public void testUnpublishedSeveral() {
        LoaderScale scale = dictionary.getScale();
        when(metadataService.getUnpublished(DEFAULT_CLUSTER, dictionary, false))
            .thenReturn(Arrays.asList(
                new MetadataService.DictionaryLoadInfo(
                    3, dictionary.getName(), 13, 123, today, LocalDateTime.now(), false,
                    dictionary.getScale().getName(), dictionary.tablePartition(todayDt), null, null),
                new MetadataService.DictionaryLoadInfo(
                    2, dictionary.getName(), 15, 456, yesterday, LocalDateTime.now().minusHours(6), false,
                    dictionary.getScale().getName(), dictionary.tablePartition(yesterdayDt), null, null),
                new MetadataService.DictionaryLoadInfo(
                    1, dictionary.getName(), 3, 789, beforeYesterday, LocalDateTime.now().minusHours(26), false,
                    dictionary.getScale().getName(), dictionary.tablePartition(beforeYesterdayDt), null, null
            )));
        when(metadataService.getLastPublished(DEFAULT_CLUSTER, dictionary, false))
            .thenReturn(
                new MetadataService.DictionaryLoadInfo(
                    3, dictionary.getName(), 13, 456, beforeYesterday, LocalDateTime.now().minusHours(6), false,
                    dictionary.getScale().getName(), dictionary.tablePartition(beforeYesterdayDt), null, null
            ));
        when(metadataService.publishedExists(DEFAULT_CLUSTER, dictionary, yesterdayDt, false)).thenReturn(false);

        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(yesterdayDt))).thenReturn(tablepath(dictionary, yesterdayDt));
        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(todayDt))).thenReturn(tablepath(dictionary, todayDt));
        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(beforeYesterdayDt))).thenReturn(tablepath(dictionary, beforeYesterdayDt));
        when(ytService.getLoadRevision(tablepath(dictionary, todayDt))).thenReturn(111L);
        when(ytService.getLoadRevision(tablepath(dictionary, yesterdayDt))).thenReturn(456L);
        when(ytService.getLoadRevision(tablepath(dictionary, beforeYesterdayDt))).thenReturn(777L);
        when(ytService.partitionExists(any(), any())).thenReturn(true);

        dictionary.setTtlDays(2L);

        publisher.publish(DEFAULT_CLUSTER, dictionary);

        verify(metadataService).getUnpublished(DEFAULT_CLUSTER, dictionary, false);
        verify(metadataService).getLastPublished(DEFAULT_CLUSTER, dictionary, false);
        verify(metadataService).publishedExists(eq(DEFAULT_CLUSTER), any(), any(), any());
        verify(ytService).getLoadRevision(tablepath(dictionary, todayDt));
        verify(ytService).getLoadRevision(tablepath(dictionary, yesterdayDt));
        verify(ytService).getLoadRevision(tablepath(dictionary, beforeYesterdayDt));
        verify(metadataService).markUnpublished(DEFAULT_CLUSTER, 1, dictionary.getName());

        verify(ytService, Mockito.times(1)).setTtl(tablepath(dictionary, yesterdayDt), 172800L);
        verify(ytService, Mockito.never()).setTtl(tablepath(dictionary, todayDt), 172800L);
        verify(ytService, Mockito.never()).setTtl(tablepath(dictionary, beforeYesterdayDt), 172800L);
        verify(anotherYtService, Mockito.never()).setTtl(any(), anyLong());

        verify(stepEventsSender).sendEvent(dictionary.getName(), yesterdayDt, DEFAULT_CLUSTER, tablepath(dictionary, yesterdayDt), scale, false);
        verify(metadataService).publish(DEFAULT_CLUSTER, 2, dictionary.getName());
        verify(metadataService).markUnpublished(DEFAULT_CLUSTER, 3, dictionary.getName());
        verify(ytService).markAsLatest(tablepath(dictionary, yesterdayDt));
        verifyNoMoreInteractions(metadataService);
        verifyNoMoreInteractions(stepEventsSender);
    }

    @Test
    public void testUnpublishedSeveralOnAnotherCluster() {
        LoaderScale scale = dictionary.getScale();
        when(metadataService.getUnpublished(ANOTHER_CLUSTER, dictionary, false))
                .thenReturn(Arrays.asList(
                        new MetadataService.DictionaryLoadInfo(
                                3, dictionary.getName(), 13, 123, today, LocalDateTime.now(), false,
                                dictionary.getScale().getName(), dictionary.tablePartition(todayDt), null, null),
                        new MetadataService.DictionaryLoadInfo(
                                2, dictionary.getName(), 15, 456, yesterday, LocalDateTime.now().minusHours(6), false,
                                dictionary.getScale().getName(), dictionary.tablePartition(yesterdayDt), null, null),
                        new MetadataService.DictionaryLoadInfo(
                                1, dictionary.getName(), 3, 789, beforeYesterday, LocalDateTime.now().minusHours(26),
                                false,
                                dictionary.getScale().getName(), dictionary.tablePartition(beforeYesterdayDt), null,
                                null
                        )));
        when(metadataService.getLastPublished(ANOTHER_CLUSTER, dictionary, false))
                .thenReturn(
                        new MetadataService.DictionaryLoadInfo(
                                3, dictionary.getName(), 13, 456, beforeYesterday, LocalDateTime.now().minusHours(6),
                                false,
                                dictionary.getScale().getName(), dictionary.tablePartition(beforeYesterdayDt), null,
                                null
                        ));
        when(metadataService.publishedExists(ANOTHER_CLUSTER, dictionary, yesterdayDt, false)).thenReturn(false);

        when(anotherYtService.dictionaryPartitionTable(dictionary, scale.formatPartition(yesterdayDt))).thenReturn(tablepath(dictionary, yesterdayDt));
        when(anotherYtService.dictionaryPartitionTable(dictionary, scale.formatPartition(todayDt))).thenReturn(tablepath(dictionary, todayDt));
        when(anotherYtService.dictionaryPartitionTable(dictionary, scale.formatPartition(beforeYesterdayDt))).thenReturn(tablepath(dictionary, beforeYesterdayDt));
        when(anotherYtService.getLoadRevision(tablepath(dictionary, todayDt))).thenReturn(111L);
        when(anotherYtService.getLoadRevision(tablepath(dictionary, yesterdayDt))).thenReturn(456L);
        when(anotherYtService.getLoadRevision(tablepath(dictionary, beforeYesterdayDt))).thenReturn(777L);
        when(anotherYtService.partitionExists(any(), any())).thenReturn(true);

        dictionary.setTtlDays(2L);

        publisher.publish(ANOTHER_CLUSTER, dictionary);

        verify(metadataService).getUnpublished(ANOTHER_CLUSTER, dictionary, false);
        verify(metadataService).getLastPublished(ANOTHER_CLUSTER, dictionary, false);
        verify(metadataService).publishedExists(eq(ANOTHER_CLUSTER), any(), any(), any());
        verify(anotherYtService).getLoadRevision(tablepath(dictionary, todayDt));
        verify(anotherYtService).getLoadRevision(tablepath(dictionary, yesterdayDt));
        verify(anotherYtService).getLoadRevision(tablepath(dictionary, beforeYesterdayDt));
        verify(metadataService).markUnpublished(ANOTHER_CLUSTER, 1, dictionary.getName());

        verify(anotherYtService, Mockito.times(1)).setTtl(tablepath(dictionary, yesterdayDt), 172800L);
        verify(anotherYtService, Mockito.never()).setTtl(tablepath(dictionary, todayDt), 172800L);
        verify(anotherYtService, Mockito.never()).setTtl(tablepath(dictionary, beforeYesterdayDt), 172800L);
        verify(ytService, Mockito.never()).setTtl(any(), anyLong());

        verify(stepEventsSender).sendEvent(dictionary.getName(), yesterdayDt, ANOTHER_CLUSTER, tablepath(dictionary,
                yesterdayDt), scale, false);
        verify(metadataService).publish(ANOTHER_CLUSTER, 2, dictionary.getName());
        verify(metadataService).markUnpublished(ANOTHER_CLUSTER, 3, dictionary.getName());
        verify(anotherYtService).markAsLatest(tablepath(dictionary, yesterdayDt));
        verify(anotherYtService, times(3)).dictionaryPartitionTable(any(), anyString());
        verify(anotherYtService, times(3)).partitionExists(any(), any());
        verifyNoMoreInteractions(metadataService);
        verifyNoMoreInteractions(stepEventsSender);
        verifyNoMoreInteractions(anotherYtService);
        verifyNoMoreInteractions(ytService);
    }

    @Test
    public void testUnpublishedNoData() {
        LoaderScale scale = dictionary.getScale();
        when(metadataService.getUnpublished(DEFAULT_CLUSTER, dictionary, false))
            .thenReturn(Arrays.asList(
                new MetadataService.DictionaryLoadInfo(
                    3, dictionary.getName(), 13, 123, today, LocalDateTime.now(), false,
                    dictionary.getScale().getName(), dictionary.tablePartition(todayDt), null, null),
                new MetadataService.DictionaryLoadInfo(
                    2, dictionary.getName(), 15, 456, yesterday, LocalDateTime.now().minusHours(6), false,
                    dictionary.getScale().getName(), dictionary.tablePartition(yesterdayDt), null, null),
                new MetadataService.DictionaryLoadInfo(
                    1, dictionary.getName(), 3, 789, beforeYesterday, LocalDateTime.now().minusHours(26), false,
                    dictionary.getScale().getName(), dictionary.tablePartition(beforeYesterdayDt), null, null)
            ));
        when(metadataService.getLastPublished(DEFAULT_CLUSTER, dictionary, false)).thenReturn(null);
        when(ytService.getLoadRevision(tablepath(dictionary, todayDt))).thenReturn(111L);
        when(ytService.getLoadRevision(tablepath(dictionary, yesterdayDt))).thenReturn(444L);
        when(ytService.getLoadRevision(tablepath(dictionary, beforeYesterdayDt))).thenReturn(777L);
        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(yesterdayDt))).thenReturn(tablepath(dictionary, yesterdayDt));
        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(todayDt))).thenReturn(tablepath(dictionary, todayDt));
        when(ytService.dictionaryPartitionTable(dictionary, scale.formatPartition(beforeYesterdayDt))).thenReturn(tablepath(dictionary, beforeYesterdayDt));
        when(ytService.partitionExists(any(), any())).thenReturn(true);

        publisher.publish(DEFAULT_CLUSTER, dictionary);

        verify(metadataService).getUnpublished(DEFAULT_CLUSTER,dictionary, false);
        verify(metadataService).getLastPublished(DEFAULT_CLUSTER, dictionary, false);
        verify(ytService).getLoadRevision(tablepath(dictionary, yesterdayDt));
        verify(ytService).getLoadRevision(tablepath(dictionary, todayDt));
        verify(ytService).getLoadRevision(tablepath(dictionary, beforeYesterdayDt));
        verify(metadataService).markUnpublished(DEFAULT_CLUSTER, 1, dictionary.getName());
        verify(metadataService).markUnpublished(DEFAULT_CLUSTER, 2, dictionary.getName());
        verify(metadataService).markUnpublished(DEFAULT_CLUSTER, 3, dictionary.getName());
        verify(metadataService).getLastPublished(DEFAULT_CLUSTER, dictionary, false);
        verify(ytService, times(0)).markAsLatest(any());

        verify(ytService, Mockito.never()).setTtl(any(), anyLong());
        verify(anotherYtService, Mockito.never()).setTtl(any(), anyLong());

        verifyNoMoreInteractions(metadataService);
        verifyNoMoreInteractions(stepEventsSender);
    }

    @Test
    public void testUnpublishedSeveralDaily() {
        Dictionary dailyDictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.DAYLY);
        LoaderScale scale = dailyDictionary.getScale();
        when(metadataService.getUnpublished(DEFAULT_CLUSTER, dailyDictionary, false))
            .thenReturn(Arrays.asList(
                new MetadataService.DictionaryLoadInfo(
                    3, dailyDictionary.getName(), 13, 123, today, LocalDateTime.now(), false,
                    dailyDictionary.getScale().getName(), dailyDictionary.tablePartition(todayDt), null, null),
                new MetadataService.DictionaryLoadInfo(
                    2, dailyDictionary.getName(), 15, 456, yesterday, LocalDateTime.now().minusHours(6), false,
                    dailyDictionary.getScale().getName(), dailyDictionary.tablePartition(yesterdayDt), null, null),
                new MetadataService.DictionaryLoadInfo(
                    1, dailyDictionary.getName(), 3, 789, beforeYesterday, LocalDateTime.now().minusHours(26), false,
                    dailyDictionary.getScale().getName(), dailyDictionary.tablePartition(beforeYesterdayDt), null, null
                )));
        when(metadataService.getLastPublished(DEFAULT_CLUSTER, dailyDictionary, false))
            .thenReturn(
                new MetadataService.DictionaryLoadInfo(
                    3, dailyDictionary.getName(), 13, 456, beforeYesterday, LocalDateTime.now().minusHours(6), false,
                    dailyDictionary.getScale().getName(), dailyDictionary.tablePartition(beforeYesterdayDt), null, null
                ));
        when(metadataService.publishedExists(DEFAULT_CLUSTER, dailyDictionary, yesterdayDt, false)).thenReturn(false);

        when(ytService.dictionaryPartitionTable(dailyDictionary, scale.formatPartition(yesterdayDt))).thenReturn(tablepath(dailyDictionary, yesterdayDt));
        when(ytService.dictionaryPartitionTable(dailyDictionary, scale.formatPartition(todayDt))).thenReturn(tablepath(dailyDictionary, todayDt));
        when(ytService.dictionaryPartitionTable(dailyDictionary, scale.formatPartition(beforeYesterdayDt))).thenReturn(tablepath(dailyDictionary, beforeYesterdayDt));
        when(ytService.getLoadRevision(tablepath(dailyDictionary, todayDt))).thenReturn(111L);
        when(ytService.getLoadRevision(tablepath(dailyDictionary, yesterdayDt))).thenReturn(456L);
        when(ytService.getLoadRevision(tablepath(dailyDictionary, beforeYesterdayDt))).thenReturn(777L);
        when(ytService.partitionExists(any(), any())).thenReturn(true);

        assertThat("Wrong partition", tablepath(dailyDictionary, todayDt).toString(), containsString("/1d/20"));

        dailyDictionary.setTtlDays(2L);
        publisher.publish(DEFAULT_CLUSTER, dailyDictionary);

        verify(metadataService).getUnpublished(DEFAULT_CLUSTER, dailyDictionary, false);
        verify(metadataService).getLastPublished(DEFAULT_CLUSTER, dailyDictionary, false);
        verify(metadataService).publishedExists(eq(DEFAULT_CLUSTER), any(), any(), any());
        verify(ytService).getLoadRevision(tablepath(dailyDictionary, todayDt));
        verify(ytService).getLoadRevision(tablepath(dailyDictionary, yesterdayDt));
        verify(ytService).getLoadRevision(tablepath(dailyDictionary, beforeYesterdayDt));
        verify(metadataService).markUnpublished(DEFAULT_CLUSTER, 1, dailyDictionary.getName());

        verify(ytService, Mockito.times(1)).setTtl(tablepath(dailyDictionary, yesterdayDt), 172800L);
        verify(ytService, Mockito.never()).setTtl(tablepath(dailyDictionary, todayDt), 172800L);
        verify(ytService, Mockito.never()).setTtl(tablepath(dailyDictionary, beforeYesterdayDt), 172800L);
        verify(anotherYtService, Mockito.never()).setTtl(any(), anyLong());

        verify(stepEventsSender).sendEvent(dailyDictionary.getName(), yesterdayDt, DEFAULT_CLUSTER, tablepath(dailyDictionary, yesterdayDt), scale, false);
        verify(metadataService).publish(DEFAULT_CLUSTER, 2, dailyDictionary.getName());
        verify(metadataService).markUnpublished(DEFAULT_CLUSTER, 3, dailyDictionary.getName());
        verify(ytService).markAsLatest(tablepath(dailyDictionary, yesterdayDt));
        verifyNoMoreInteractions(metadataService);
        verifyNoMoreInteractions(stepEventsSender);
    }


    @Test
    public void testUnpublishedSeveralHourly() {
        LocalDate day = LocalDate.parse("2019-04-01");
        LocalDateTime firstPartition = day.atStartOfDay().plusHours(7);
        LocalDateTime secondPartition = day.atStartOfDay().plusHours(9);
        LocalDateTime lastPartition = day.atStartOfDay().plusHours(10);
        Dictionary hourlyDict = Dictionary.fromClass(TestDictionary.class, LoaderScale.HOURLY);
        LoaderScale scale = hourlyDict.getScale();
        when(metadataService.getUnpublished(DEFAULT_CLUSTER, hourlyDict, false))
            .thenReturn(Arrays.asList(
                new MetadataService.DictionaryLoadInfo(
                    3, hourlyDict.getName(), 13, 123, day, LocalDateTime.now(), false,
                    hourlyDict.getScale().getName(), hourlyDict.tablePartition(lastPartition), null, null),
                new MetadataService.DictionaryLoadInfo(
                    2, hourlyDict.getName(), 15, 456, day, LocalDateTime.now().minusHours(1), false,
                    hourlyDict.getScale().getName(), hourlyDict.tablePartition(secondPartition), null, null),
                new MetadataService.DictionaryLoadInfo(
                    1, hourlyDict.getName(), 3, 789, day, LocalDateTime.now().minusHours(2), false,
                    hourlyDict.getScale().getName(), hourlyDict.tablePartition(firstPartition), null, null
                )));
        when(metadataService.getLastPublished(DEFAULT_CLUSTER, hourlyDict, false))
            .thenReturn(
                new MetadataService.DictionaryLoadInfo(
                    3, hourlyDict.getName(), 13, 456, day, LocalDateTime.now().minusHours(6), false,
                    hourlyDict.getScale().getName(), hourlyDict.tablePartition(firstPartition), null, null
                ));
        when(metadataService.publishedExists(DEFAULT_CLUSTER, hourlyDict, secondPartition, false)).thenReturn(false);

        when(ytService.dictionaryPartitionTable(hourlyDict, scale.formatPartition(secondPartition))).thenReturn(tablepath(hourlyDict, secondPartition));
        when(ytService.dictionaryPartitionTable(hourlyDict, scale.formatPartition(lastPartition))).thenReturn(tablepath(hourlyDict, lastPartition));
        when(ytService.dictionaryPartitionTable(hourlyDict, scale.formatPartition(firstPartition))).thenReturn(tablepath(hourlyDict, firstPartition));
        when(ytService.getLoadRevision(tablepath(hourlyDict, lastPartition))).thenReturn(111L);
        when(ytService.getLoadRevision(tablepath(hourlyDict, secondPartition))).thenReturn(456L);
        when(ytService.getLoadRevision(tablepath(hourlyDict, firstPartition))).thenReturn(777L);
        when(ytService.partitionExists(any(), any())).thenReturn(true);
        assertThat("Wrong partition", tablepath(hourlyDict, lastPartition).toString(),
            is("//somepath/" + hourlyDict.getRelativePath() + "/1h/2019-04-01T10:00:00"));

        publisher.publish(DEFAULT_CLUSTER, hourlyDict);

        verify(metadataService).getUnpublished(DEFAULT_CLUSTER, hourlyDict, false);
        verify(metadataService).getLastPublished(DEFAULT_CLUSTER, hourlyDict, false);
        verify(metadataService).publishedExists(eq(DEFAULT_CLUSTER), any(), any(), any());
        verify(ytService).getLoadRevision(tablepath(hourlyDict, lastPartition));
        verify(ytService).getLoadRevision(tablepath(hourlyDict, secondPartition));
        verify(ytService).getLoadRevision(tablepath(hourlyDict, firstPartition));
        verify(metadataService).markUnpublished(DEFAULT_CLUSTER, 1, hourlyDict.getName());

        verify(ytService, Mockito.times(1)).setTtl(tablepath(hourlyDict, secondPartition), 259200L);
        verify(ytService, Mockito.never()).setTtl(tablepath(hourlyDict, firstPartition), 259200L);
        verify(ytService, Mockito.never()).setTtl(tablepath(hourlyDict, lastPartition), 259200L);
        verify(anotherYtService, Mockito.never()).setTtl(any(), anyLong());

        verify(stepEventsSender).sendEvent(hourlyDict.getName(), secondPartition, DEFAULT_CLUSTER, tablepath(hourlyDict, secondPartition), scale,false);
        verify(metadataService).publish(DEFAULT_CLUSTER, 2, hourlyDict.getName());
        verify(metadataService).markUnpublished(DEFAULT_CLUSTER, 3, hourlyDict.getName());
        verify(ytService).markAsLatest(tablepath(hourlyDict, secondPartition));
        verifyNoMoreInteractions(metadataService);
        verifyNoMoreInteractions(stepEventsSender);
    }

    @Test
    public void testLoadersByNameNotEmpty() {
        Map<String, DictionaryLoader> loaderMap = publisher.getLoadersByName();
        assertEquals(loaderMap.size(), 3);
    }

    @Test
    public void mustPassPublishedFromResultSet() throws SQLException {
        ResultSet rs = mockCommonFields();
        Mockito.when(rs.getTimestamp("hahn_published")).thenReturn(null);
        Mockito.when(rs.getTimestamp("arnold_published")).thenReturn(null);

        MetadataService.DictionaryLoadInfo dictionaryLoadInfo = MetadataService.DictionaryLoadInfo.from(rs);

        assertNull(dictionaryLoadInfo.getHahnPublished());
        assertNull(dictionaryLoadInfo.getArnoldPublished());
        assertTrue(dictionaryLoadInfo.isFirstPublishForLoad());
        assertTrue(publisher.needToRunPostProcess(dictionaryLoadInfo, "anaplan__promo_data-1d"));
    }

    @Test
    public void mustNotPassPublishedFromResultSetNullNullArgs() throws SQLException {
        ResultSet rs = mockCommonFields();
        Mockito.when(rs.getTimestamp("hahn_published")).thenReturn(null);
        Mockito.when(rs.getTimestamp("arnold_published")).thenReturn(null);

        MetadataService.DictionaryLoadInfo dictionaryLoadInfo = MetadataService.DictionaryLoadInfo.from(rs);

        assertNull(dictionaryLoadInfo.getHahnPublished());
        assertNull(dictionaryLoadInfo.getArnoldPublished());
        assertTrue(dictionaryLoadInfo.isFirstPublishForLoad());
        assertFalse(publisher.needToRunPostProcess(dictionaryLoadInfo, "mbiDict"));
    }

    @Test
    public void mustNotPassPublishedFromResultSetNullNotNullArgs() throws SQLException {
        ResultSet rs = mockCommonFields();
        Mockito.when(rs.getTimestamp("hahn_published")).thenReturn(null);
        Mockito.when(rs.getTimestamp("arnold_published")).thenReturn(Timestamp.valueOf("2020-06-04 16:54:00"));

        MetadataService.DictionaryLoadInfo dictionaryLoadInfo = MetadataService.DictionaryLoadInfo.from(rs);

        assertNull(dictionaryLoadInfo.getHahnPublished());
        assertEquals(dictionaryLoadInfo.getArnoldPublished(),
                LocalDateTime.of(2020, 6, 4, 16, 54, 0));
        assertFalse(dictionaryLoadInfo.isFirstPublishForLoad());
        assertFalse(publisher.needToRunPostProcess(dictionaryLoadInfo, "anaplan__"));
    }

    @Test
    public void mustNotPassPublishedFromResultSetNotNullNullArgs() throws SQLException {
        ResultSet rs = mockCommonFields();
        Mockito.when(rs.getTimestamp("hahn_published")).thenReturn(Timestamp.valueOf("2020-08-24 08:01:15"));
        Mockito.when(rs.getTimestamp("arnold_published")).thenReturn(null);

        MetadataService.DictionaryLoadInfo dictionaryLoadInfo = MetadataService.DictionaryLoadInfo.from(rs);

        assertEquals(dictionaryLoadInfo.getHahnPublished(),
                LocalDateTime.of(2020, 8, 24, 8, 1, 15));
        assertNull(dictionaryLoadInfo.getArnoldPublished());
        assertFalse(dictionaryLoadInfo.isFirstPublishForLoad());
        assertFalse(publisher.needToRunPostProcess(dictionaryLoadInfo, "mbiDict"));
    }

    @Test
    public void mustNotPassPublishedFromResultSetNotNullNotNullArgs() throws SQLException {
        ResultSet rs = mockCommonFields();
        Mockito.when(rs.getTimestamp("hahn_published")).thenReturn(Timestamp.valueOf("2015-09-14 04:03:14"));
        Mockito.when(rs.getTimestamp("arnold_published")).thenReturn(Timestamp.valueOf("2008-03-13 05:05:12"));

        MetadataService.DictionaryLoadInfo dictionaryLoadInfo = MetadataService.DictionaryLoadInfo.from(rs);

        assertEquals(dictionaryLoadInfo.getHahnPublished(),
                LocalDateTime.of(2015, 9, 14, 4, 3, 14));
        assertEquals(dictionaryLoadInfo.getArnoldPublished(),
                LocalDateTime.of(2008, 3, 13, 5, 5, 12));
        assertFalse(dictionaryLoadInfo.isFirstPublishForLoad());
        assertFalse(publisher.needToRunPostProcess(dictionaryLoadInfo, "anaplan__"));
    }

    private ResultSet mockCommonFields() throws SQLException {
        ResultSet rs = Mockito.mock(ResultSet.class);
        Mockito.when(rs.getLong("id")).thenReturn(1L);
        Mockito.when(rs.getString("dictionary")).thenReturn("magic");
        Mockito.when(rs.getLong("records")).thenReturn(10L);
        Mockito.when(rs.getLong("revision")).thenReturn(8L);
        Mockito.when(rs.getDate("load_day")).thenReturn(Date.valueOf("2019-04-10"));
        Mockito.when(rs.getTimestamp("last_load_time")).thenReturn(Timestamp.valueOf("2019-04-10 14:54:00.081"));
        Mockito.when(rs.getBoolean("uploaded")).thenReturn(true);
        Mockito.when(rs.getString("load_partition")).thenReturn("2019-04-10");
        Mockito.when(rs.getString("scale")).thenReturn("2019-04-10");

        return rs;
    }


    @NotNull
    private YPath tablepath(Dictionary dict, LocalDateTime now) {
        return YPath.simple("//somepath/" + dict.getRelativePath() + "/" + dict.tablePartition(now));
    }
}

