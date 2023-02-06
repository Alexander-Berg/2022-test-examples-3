package ru.yandex.direct.jobs.advq.offline.processing;

import java.util.Arrays;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.direct.ytwrapper.model.YtTableRow;
import ru.yandex.direct.ytwrapper.model.YtYield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.jobs.advq.offline.processing.OfflineAdvqTestUtils.bidsRow;
import static ru.yandex.direct.jobs.advq.offline.processing.OfflineAdvqTestUtils.phrasesRow;
import static ru.yandex.direct.jobs.advq.offline.processing.OfflineAdvqTestUtils.preProcessingRow;
import static ru.yandex.direct.jobs.advq.offline.processing.OfflineAdvqTestUtils.resultRow;

class OfflineAdvqProcessingReducerTest {
    private static final long TEST_PID = 1234L;
    private static final long TEST_ID = 6543L;
    private static final long TEST_SHARD = 12;
    private static final String TEST_KEYWORD = "keyword one";
    private static final String TEST_KEYWORD_OTHER = "keyword two";

    @Mock
    private YtYield ytYield;

    @Captor
    private ArgumentCaptor<YtTableRow> captor;

    private OfflineAdvqProcessingReducer reducer;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);

        reducer = new OfflineAdvqProcessingReducer(ytYield);
    }

    @Test
    void testPercentValue() {
        SoftAssertions soft = new SoftAssertions();
        Offset<Double> offset = Offset.offset(1e-3);

        soft.assertThat(reducer.percentChange(110L, 100L))
                .isCloseTo(10L, offset);
        soft.assertThat(reducer.percentChange(0L, 129L))
                .isCloseTo(100L, offset);
        soft.assertThat(reducer.percentChange(111111L, 111112L))
                .isCloseTo(0L, offset);
        soft.assertThat(reducer.percentChange(15L, 15L))
                .isCloseTo(0L, offset);

        soft.assertAll();
    }

    @Test
    void testDecisionValue() {
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(reducer.forecastDiffers(100L + OfflineAdvqProcessingReducer.BORDER_DIFF_VALUE + 1, 100L))
                .isTrue();
        soft.assertThat(reducer.forecastDiffers(100L + OfflineAdvqProcessingReducer.BORDER_DIFF_VALUE - 1, 100L))
                .isFalse();

        soft.assertThat(reducer.forecastDiffers(111L, 100L))
                .isTrue();
        soft.assertThat(reducer.forecastDiffers(0L, 129L))
                .isTrue();
        soft.assertThat(reducer.forecastDiffers(111111L, 111112L))
                .isFalse();
        soft.assertThat(reducer.forecastDiffers(111111L, 211112L))
                .isTrue();
        soft.assertThat(reducer.forecastDiffers(15L, 15L))
                .isFalse();

        soft.assertAll();
    }

    @Test
    void testKey() {
        assertThat(reducer.key(phrasesRow(TEST_PID, "4321")))
                .isEqualTo(TEST_PID);
    }

    @Test
    void testReduce_ForecastPartlyNeedsUpdate() {
        long forecast = 10500;
        String geo = "4321";

        IteratorF<YtTableRow> iteratorF = Cf.wrap(Arrays.asList(
                phrasesRow(TEST_PID, geo),
                preProcessingRow(TEST_PID, TEST_SHARD),
                bidsRow(TEST_PID, TEST_ID, TEST_KEYWORD, forecast * 2),
                bidsRow(TEST_PID, TEST_ID + 1, TEST_KEYWORD, forecast * 2),
                bidsRow(TEST_PID, TEST_ID + 2, TEST_KEYWORD, forecast),
                resultRow(TEST_PID, TEST_ID, geo, TEST_KEYWORD, forecast),
                resultRow(TEST_PID, TEST_ID + 1, geo, TEST_KEYWORD_OTHER, forecast),
                resultRow(TEST_PID, TEST_ID + 2, geo, TEST_KEYWORD, forecast),
                resultRow(TEST_PID, TEST_ID + 3, geo, TEST_KEYWORD, forecast)
        )).iterator();

        reducer.reduce(TEST_PID, iteratorF);

        verify(ytYield).yield(eq((int) TEST_SHARD - 1), captor.capture());

        YtTableRow row = captor.getValue();

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.GROUP_ID))
                .isEqualTo(TEST_PID);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.ID))
                .isEqualTo(TEST_ID);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.ORIGINAL_KEYWORD))
                .isEqualTo(TEST_KEYWORD);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.GEO))
                .isEqualTo(geo);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.FORECAST))
                .isEqualTo(forecast);

        soft.assertAll();
    }

    @Test
    void testReduce_ForecastNeedsUpdate() {
        long forecast = 10500;
        String geo = "4321";

        IteratorF<YtTableRow> iteratorF = Cf.wrap(Arrays.asList(
                phrasesRow(TEST_PID, geo),
                preProcessingRow(TEST_PID, TEST_SHARD),
                bidsRow(TEST_PID, TEST_ID, TEST_KEYWORD, forecast * 2),
                resultRow(TEST_PID, TEST_ID, geo, TEST_KEYWORD, forecast)
        )).iterator();

        reducer.reduce(TEST_PID, iteratorF);

        verify(ytYield).yield(eq((int) TEST_SHARD - 1), captor.capture());

        YtTableRow row = captor.getValue();

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.GROUP_ID))
                .isEqualTo(TEST_PID);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.ID))
                .isEqualTo(TEST_ID);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.ORIGINAL_KEYWORD))
                .isEqualTo(TEST_KEYWORD);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.GEO))
                .isEqualTo(geo);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.FORECAST))
                .isEqualTo(forecast);

        soft.assertAll();
    }

    @Test
    void testReduce_ForecastNotChanged() {
        long forecast = 10500;
        String geo = "4321";

        IteratorF<YtTableRow> iteratorF = Cf.wrap(Arrays.asList(
                phrasesRow(TEST_PID, geo),
                preProcessingRow(TEST_PID, TEST_SHARD),
                bidsRow(TEST_PID, TEST_ID, TEST_KEYWORD, forecast),
                resultRow(TEST_PID, TEST_ID, geo, TEST_KEYWORD, forecast)
        )).iterator();

        reducer.reduce(TEST_PID, iteratorF);

        verify(ytYield).yield(eq((int) TEST_SHARD - 1), captor.capture());

        YtTableRow row = captor.getValue();

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.GROUP_ID))
                .isEqualTo(TEST_PID);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.ID))
                .isEqualTo(0L);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.ORIGINAL_KEYWORD))
                .isEqualTo("");
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.GEO))
                .isEqualTo(geo);
        soft.assertThat(row.valueOf(OfflineAdvqProcessingBaseTableRow.FORECAST))
                .isEqualTo(0L);

        soft.assertAll();
    }

    @Test
    void testReduce_geoChanged() {
        long forecast = 10500;

        IteratorF<YtTableRow> iteratorF = Cf.wrap(Arrays.asList(
                phrasesRow(TEST_PID, "1234"),
                preProcessingRow(TEST_PID, TEST_SHARD),
                bidsRow(TEST_PID, TEST_ID, TEST_KEYWORD, forecast * 2),
                resultRow(TEST_PID, TEST_ID, "4321", TEST_KEYWORD, forecast)
        )).iterator();

        reducer.reduce(TEST_PID, iteratorF);

        verify(ytYield, never()).yield(anyInt(), any());
    }

    @Test
    void testReduce_phraseChanged() {
        long forecast = 10500;
        String geo = "4321";

        IteratorF<YtTableRow> iteratorF = Cf.wrap(Arrays.asList(
                phrasesRow(TEST_PID, geo),
                preProcessingRow(TEST_PID, TEST_SHARD),
                bidsRow(TEST_PID, TEST_ID, TEST_KEYWORD, forecast * 2),
                resultRow(TEST_PID, TEST_ID, geo, TEST_KEYWORD_OTHER, forecast)
        )).iterator();

        reducer.reduce(TEST_PID, iteratorF);

        verify(ytYield, never()).yield(anyInt(), any());
    }

    @Test
    void testReduce_noBidsData() {
        long forecast = 10500;
        String geo = "4321";

        IteratorF<YtTableRow> iteratorF = Cf.wrap(Arrays.asList(
                phrasesRow(TEST_PID, geo),
                preProcessingRow(TEST_PID, TEST_SHARD),
                resultRow(TEST_PID, TEST_ID, geo, TEST_KEYWORD_OTHER, forecast)
        )).iterator();

        reducer.reduce(TEST_PID, iteratorF);

        verify(ytYield, never()).yield(anyInt(), any());
    }

    @Test
    void testReduce_noPhrasesData() {
        long forecast = 10500;
        String geo = "4321";

        IteratorF<YtTableRow> iteratorF = Cf.wrap(Arrays.asList(
                preProcessingRow(TEST_PID, TEST_SHARD),
                bidsRow(TEST_PID, TEST_ID, TEST_KEYWORD, forecast * 2),
                resultRow(TEST_PID, TEST_ID, geo, TEST_KEYWORD, forecast)
        )).iterator();

        reducer.reduce(TEST_PID, iteratorF);

        verify(ytYield, never()).yield(anyInt(), any());
    }

    @Test
    void testReduce_noShardData() {
        long forecast = 10500;
        String geo = "4321";

        IteratorF<YtTableRow> iteratorF = Cf.wrap(Arrays.asList(
                phrasesRow(TEST_PID, geo),
                bidsRow(TEST_PID, TEST_ID, TEST_KEYWORD, forecast * 2),
                resultRow(TEST_PID, TEST_ID, geo, TEST_KEYWORD, forecast)
        )).iterator();

        reducer.reduce(TEST_PID, iteratorF);

        verify(ytYield, never()).yield(anyInt(), any());
    }

    @Test
    void testReduce_noForecastData() {
        long forecast = 10500;
        String geo = "4321";

        IteratorF<YtTableRow> iteratorF = Cf.wrap(Arrays.asList(
                phrasesRow(TEST_PID, geo),
                preProcessingRow(TEST_PID, TEST_SHARD),
                bidsRow(TEST_PID, TEST_ID, TEST_KEYWORD, forecast)
        )).iterator();

        reducer.reduce(TEST_PID, iteratorF);

        verify(ytYield, never()).yield(anyInt(), any());
    }
}
