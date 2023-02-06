package ru.yandex.direct.ytcore.entity.statistics.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.ytcomponents.service.StatsDynContextProvider;
import ru.yandex.direct.ytcomponents.statistics.model.DateRange;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsRequest;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsResponse;
import ru.yandex.direct.ytcomponents.statistics.model.RetargetingStatisticsRequest;
import ru.yandex.direct.ytcomponents.statistics.model.ShowConditionStatisticsRequest;
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.types.Unsigned.ulong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RecentStatisticsRepositoryTest {

    private static final TableSchema RESULT_SCHEMA = new TableSchema.Builder()
            .setUniqueKeys(false)
            .addKey("ExportID", ColumnValueType.INT64)
            .addKey("GroupExportID", ColumnValueType.INT64)
            .addKey("DirectBannerID", ColumnValueType.INT64)
            .addKey("PhraseExportID", ColumnValueType.INT64)
            .addKey("PhraseID", ColumnValueType.INT64)
            .addKey("GoalContextID", ColumnValueType.INT64)
            .addKey("IsMobile", ColumnValueType.BOOLEAN)
            .addKey("IsFlat", ColumnValueType.BOOLEAN)
            .addKey("NetworkShows", ColumnValueType.INT64)
            .addKey("NetworkClicks", ColumnValueType.INT64)
            .addKey("NetworkEshows", ColumnValueType.DOUBLE)
            .addKey("SearchShows", ColumnValueType.INT64)
            .addKey("SearchClicks", ColumnValueType.INT64)
            .addKey("SearchEshows", ColumnValueType.DOUBLE)
            .addKey("YaSearchShows", ColumnValueType.INT64)
            .addKey("YaSearchClicks", ColumnValueType.INT64)
            .addKey("YaSearchEshows", ColumnValueType.DOUBLE)
            .build();

    private RecentStatisticsRepository recentStatisticsRepository;
    private YtDynamicContext ytDynamicContext;

    @Before
    public void setUp() {
        UnversionedRowset emptyRowset = new UnversionedRowset(RESULT_SCHEMA, Collections.emptyList());

        StatsDynContextProvider ytDynContextProvider = mock(StatsDynContextProvider.class);
        ytDynamicContext = mock(YtDynamicContext.class);
        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(emptyRowset);
        when(ytDynContextProvider.getContext()).thenReturn(ytDynamicContext);
        recentStatisticsRepository = new RecentStatisticsRepository(ytDynContextProvider);
    }

    @Test
    public void getPhraseStatistics_success_onEmptyResponse() {
        DateRange dateRange = getDateRange();
        Collection<PhraseStatisticsRequest> requests = singletonList(new PhraseStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withBsPhraseId(ulong(4L))
                .build()
        );
        List<PhraseStatisticsResponse> recentPhraseStatistics =
                recentStatisticsRepository.getPhraseStatistics(requests, dateRange);
        assertThat(recentPhraseStatistics).isEmpty();
    }

    @Test
    public void getShowConditionStatistics_success_onEmptyResponse() {
        DateRange dateRange = getDateRange();
        Collection<ShowConditionStatisticsRequest> requests = singletonList(new ShowConditionStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withShowConditionId(4L)
                .build()
        );
        List<PhraseStatisticsResponse> recentPhraseStatistics =
                recentStatisticsRepository.getShowConditionStatistics(requests, dateRange);
        assertThat(recentPhraseStatistics).isEmpty();
    }

    @Test
    public void getRetargetingStatistics_success_onEmptyResponse() {
        DateRange dateRange = getDateRange();
        Collection<RetargetingStatisticsRequest> requests = singletonList(new RetargetingStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withRetargetingConditionId(4L)
                .build()
        );
        List<PhraseStatisticsResponse> recentPhraseStatistics =
                recentStatisticsRepository.getRetargetingStatistics(requests, dateRange);
        assertThat(recentPhraseStatistics).isEmpty();
    }

    @Test
    public void getPhraseStatistics_success_onNotEmptyResponse() {
        UnversionedRowset rowset = new UnversionedRowset(RESULT_SCHEMA, singletonList(
                row(1L, 2L, 3L, 4L, 5L, false, false, 100L, 10L, 50.5)
        ));
        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(rowset);

        DateRange dateRange = getDateRange();
        Collection<PhraseStatisticsRequest> requests = singletonList(new PhraseStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withBsPhraseId(ulong(4L))
                .build()
        );
        List<PhraseStatisticsResponse> recentPhraseStatistics =
                recentStatisticsRepository.getPhraseStatistics(requests, dateRange);
        assertThat(recentPhraseStatistics)
                .hasSize(1)
                .first().isEqualToComparingFieldByField(
                new PhraseStatisticsResponse()
                        .withCampaignId(1L)
                        .withAdGroupId(2L)
                        .withBannerId(3L)
                        .withPhraseId(4L)
                        .withBsPhraseId(ulong(5L))
                        .withGoalContextId(ulong(5L))
                        .withMobile(false)
                        .withNetworkShows(100L)
                        .withNetworkClicks(10L)
                        .withNetworkEshows(50.5)
                        .withSearchShows(100L)
                        .withSearchClicks(10L)
                        .withSearchEshows(50.5)
                        .withYaSearchShows(100L)
                        .withYaSearchClicks(10L)
                        .withYaSearchEshows(50.5)
        );
    }

    @Test
    public void getShowConditionStatistics_success_onNotEmptyResponse() {
        UnversionedRowset rowset = new UnversionedRowset(RESULT_SCHEMA, singletonList(
                row(1L, 2L, 3L, 4L, 5L, false, false, 100L, 10L, 50.5)
        ));
        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(rowset);

        DateRange dateRange = getDateRange();
        Collection<ShowConditionStatisticsRequest> requests = singletonList(new ShowConditionStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withShowConditionId(4L)
                .build()
        );
        List<PhraseStatisticsResponse> recentPhraseStatistics =
                recentStatisticsRepository.getShowConditionStatistics(requests, dateRange);
        assertThat(recentPhraseStatistics)
                .hasSize(1)
                .first().isEqualToComparingFieldByField(
                new PhraseStatisticsResponse()
                        .withCampaignId(1L)
                        .withAdGroupId(2L)
                        .withBannerId(3L)
                        .withPhraseId(4L)
                        .withBsPhraseId(ulong(5L))
                        .withGoalContextId(ulong(5L))
                        .withMobile(false)
                        .withNetworkShows(100L)
                        .withNetworkClicks(10L)
                        .withNetworkEshows(50.5)
                        .withSearchShows(100L)
                        .withSearchClicks(10L)
                        .withSearchEshows(50.5)
                        .withYaSearchShows(100L)
                        .withYaSearchClicks(10L)
                        .withYaSearchEshows(50.5)
        );
    }

    @Test
    public void getRetargetingStatistics_success_onNotEmptyResponse() {
        UnversionedRowset rowset = new UnversionedRowset(RESULT_SCHEMA, singletonList(
                row(1L, 2L, 3L, 4L, 5L, false, false, 100L, 10L, 50.5)
        ));
        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(rowset);
        DateRange dateRange = getDateRange();
        Collection<RetargetingStatisticsRequest> requests = singletonList(new RetargetingStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withRetargetingConditionId(4L)
                .build()
        );
        List<PhraseStatisticsResponse> recentPhraseStatistics =
                recentStatisticsRepository.getRetargetingStatistics(requests, dateRange);

        assertThat(recentPhraseStatistics)
                .hasSize(1)
                .first().isEqualToComparingFieldByField(
                new PhraseStatisticsResponse()
                        .withCampaignId(1L)
                        .withAdGroupId(2L)
                        .withBannerId(3L)
                        .withPhraseId(4L)
                        .withBsPhraseId(ulong(5L))
                        .withGoalContextId(ulong(5L))
                        .withMobile(false)
                        .withNetworkShows(100L)
                        .withNetworkClicks(10L)
                        .withNetworkEshows(50.5)
                        .withSearchShows(100L)
                        .withSearchClicks(10L)
                        .withSearchEshows(50.5)
                        .withYaSearchShows(100L)
                        .withYaSearchClicks(10L)
                        .withYaSearchEshows(50.5)
        );
    }

    @Test
    public void getShowConditionStatistics_correctSelect_onNotEmptyResponse() {
        DateRange dateRange = getDateRange();
        Collection<ShowConditionStatisticsRequest> requests = singletonList(new ShowConditionStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withShowConditionId(4L)
                .build()
        );

        recentStatisticsRepository.getShowConditionStatistics(requests, dateRange);

        ArgumentCaptor<Select> captor = ArgumentCaptor.forClass(Select.class);
        verify(ytDynamicContext).executeSelect(captor.capture());
        assertThat(captor.getValue().toString())
                .describedAs("Select should contain condition with PhraseExportID")
                .contains("(ExportID, GroupExportID, PhraseExportID) IN ((1, 2, 4))");
    }

    @Test
    public void getRetargetingStatistics_correctSelect_onNotEmptyResponse() {
        DateRange dateRange = getDateRange();
        Collection<RetargetingStatisticsRequest> requests = singletonList(new RetargetingStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withRetargetingConditionId(4L)
                .build()
        );

        recentStatisticsRepository.getRetargetingStatistics(requests, dateRange);

        ArgumentCaptor<Select> captor = ArgumentCaptor.forClass(Select.class);
        verify(ytDynamicContext).executeSelect(captor.capture());
        assertThat(captor.getValue().toString())
                .describedAs("Select should contain condition with GoalContextID")
                .contains("(ExportID, GroupExportID, GoalContextID) IN ((1, 2, 4))");
    }

    @Test
    public void getPhraseStatistics_success_forRequestWithBannerId() {
        DateRange dateRange = getDateRange();
        Collection<PhraseStatisticsRequest> requests = singletonList(new PhraseStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withBannerId(3L)
                .withBsPhraseId(ulong(4L))
                .build()
        );

        recentStatisticsRepository.getPhraseStatistics(requests, dateRange);

        ArgumentCaptor<Select> captor = ArgumentCaptor.forClass(Select.class);
        verify(ytDynamicContext).executeSelect(captor.capture());
        assertThat(captor.getValue().toString())
                .describedAs("Select should contain condition with DirectBannerID")
                .contains("(ExportID, GroupExportID, DirectBannerID, PhraseID) IN ((1, 2, 3, 4u))");
    }

    @Test
    public void getPhraseStatistics_success_forRequestWithoutBannerId() {
        DateRange dateRange = getDateRange();
        Collection<PhraseStatisticsRequest> requests = singletonList(new PhraseStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                /*.withBannerId(3L)*/
                .withBsPhraseId(ulong(4L))
                .build()
        );

        recentStatisticsRepository.getPhraseStatistics(requests, dateRange);

        ArgumentCaptor<Select> captor = ArgumentCaptor.forClass(Select.class);
        verify(ytDynamicContext).executeSelect(captor.capture());
        assertThat(captor.getValue().toString())
                .describedAs("Select should not contain condition with DirectBannerID")
                .contains("(ExportID, GroupExportID, PhraseID) IN ((1, 2, 4u))");
    }

    @Test
    public void getPhraseStatistics_success_forRequestWithAndWithoutBannerId() {
        DateRange dateRange = getDateRange();
        Collection<PhraseStatisticsRequest> requests = asList(
                new PhraseStatisticsRequest.Builder()
                        .withCampaignId(1L)
                        .withAdGroupId(2L)
                        .withBannerId(3L)
                        .withBsPhraseId(ulong(4L))
                        .build(),
                new PhraseStatisticsRequest.Builder()
                        .withCampaignId(10L)
                        .withAdGroupId(20L)
                        /*.withBannerId(30L)*/
                        .withBsPhraseId(ulong(40L))
                        .build()
        );

        recentStatisticsRepository.getPhraseStatistics(requests, dateRange);

        ArgumentCaptor<Select> captor = ArgumentCaptor.forClass(Select.class);
        verify(ytDynamicContext, times(2)).executeSelect(captor.capture());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(captor.getAllValues()).anySatisfy(select -> assertThat(select.toString())
                    .describedAs("One of selects should contain condition without DirectBannerID ")
                    .contains("(ExportID, GroupExportID, PhraseID) IN ((10, 20, 40u))"));
            softly.assertThat(captor.getAllValues()).anySatisfy(select -> assertThat(select.toString())
                    .describedAs("One of selects should contain condition with DirectBannerID ")
                    .contains("(ExportID, GroupExportID, DirectBannerID, PhraseID) IN ((1, 2, 3, 4u))"));
        });
    }

    private DateRange getDateRange() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(28);
        return new DateRange()
                .withFromInclusive(startDate)
                .withToInclusive(endDate);
    }

    @SuppressWarnings("SameParameterValue")
    private static UnversionedRow row(Long campaignId, Long adGroupId, Long bannerId, Long phraseId, Long bsPhraseId,
                                      Boolean isFlat, Boolean isMobile, Long shows, Long clicks, Double eshows) {
        return new UnversionedRow(asList(longValue(0, campaignId),
                longValue(1, adGroupId),
                longValue(2, bannerId),
                longValue(3, phraseId),
                longValue(4, bsPhraseId),
                // GoalContextID равен PhraseID
                longValue(5, bsPhraseId),
                booleanValue(6, isMobile),
                booleanValue(7, isFlat),
                // Network* колонки
                longValue(8, shows),
                longValue(9, clicks),
                doubleValue(10, eshows),
                // В Search* колонки записываем то же самое
                longValue(11, shows),
                longValue(12, clicks),
                doubleValue(13, eshows),
                // В YaSearch* колонки записываем то же самое
                longValue(14, shows),
                longValue(15, clicks),
                doubleValue(16, eshows)));
    }

    private static UnversionedValue longValue(Integer id, Long value) {
        return new UnversionedValue(id, ColumnValueType.INT64, false, value);
    }

    private static UnversionedValue doubleValue(Integer id, Double value) {
        return new UnversionedValue(id, ColumnValueType.DOUBLE, false, value);
    }

    private static UnversionedValue booleanValue(Integer id, Boolean value) {
        return new UnversionedValue(id, ColumnValueType.BOOLEAN, false, value);
    }
}
