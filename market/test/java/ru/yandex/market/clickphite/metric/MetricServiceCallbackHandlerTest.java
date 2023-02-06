package ru.yandex.market.clickphite.metric;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.clickhouse.HttpResultRow;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;
import ru.yandex.market.health.configs.clickphite.metric.MetricServiceContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricServiceCallbackHandlerTest {

    private static final int MAX_ROWS_PER_PERIOD = 10;
    private static final int FIRST_TIMESTAMP = 1;
    private static final int SECOND_TIMESTAMP = 2;

    @Mock
    private MetricServiceContext metricServiceContext;

    @Mock
    private MetricContextGroup metricContextGroup;

    @Captor
    private ArgumentCaptor<MetricServiceContext> metricServiceContextCaptor;

    private List<List<HttpResultRow>> sentRows;

    private MetricServiceCallbackHandler callbackHandler;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        sentRows = new ArrayList<>();
        callbackHandler = new MetricServiceCallbackHandler(metricServiceContext, metricContextGroup, "some_query",
            MAX_ROWS_PER_PERIOD, MAX_ROWS_PER_PERIOD / 2);
        setupTimestamp(FIRST_TIMESTAMP);
        //тестируемый код сразу очищает список после его передачи в sendMetrics, поэтому приходится сохранять
        //результат в отдельное поле сразу при вызове, чтобы не потерять переданные данные для дальнейшей проверки
        when(metricContextGroup.sendMetrics(any(), any(), any())).thenAnswer(invocation -> {
            List<HttpResultRow> currentSentRows = invocation.getArgument(0);
            sentRows.add(new ArrayList<>(currentSentRows));
            return null;
        });
    }

    @Test
    public void noMetricsSentWhenNoRowsInResult() throws Exception {
        callbackHandler.flush();
        verifyNoMetricsWasSent();
        checkProcessedAndIgnoredMetrics(0, 0);
    }

    @Test
    public void testNoOverflow() throws Exception {
        int callCount = MAX_ROWS_PER_PERIOD - 1;
        final List<HttpResultRow> mockedRows = oneTimestampTest(callCount, callCount, 0);
        verifyMetricsWasSend(mockedRows);
    }

    @Test
    public void testOverflow1() throws Exception {
        testOverflow(MAX_ROWS_PER_PERIOD);
    }

    @Test
    public void testOverflow2() throws Exception {
        testOverflow(MAX_ROWS_PER_PERIOD + 1);
    }

    @Test
    public void overflowAndNormalCaseCompositionTest() throws Exception {
        final int rowsInFirstTimestamp = MAX_ROWS_PER_PERIOD + 10;
        final int rowsInSecondTimestamp = MAX_ROWS_PER_PERIOD - 2;

        processRows(rowsInFirstTimestamp);
        setupTimestamp(SECOND_TIMESTAMP);
        final List<HttpResultRow> mockedRows = processRows(rowsInSecondTimestamp);
        verifyNoMetricsWasSent();

        callbackHandler.flush();
        verifyMetricsWasSend(mockedRows);

        checkProcessedAndIgnoredMetrics(rowsInSecondTimestamp, rowsInFirstTimestamp);
    }

    private void testOverflow(int callCount) throws Exception {
        oneTimestampTest(callCount, 0, callCount);
        verifyNoMetricsWasSent();
    }

    private List<HttpResultRow> oneTimestampTest(int callCount, int expectedProcessedRows, int expectedIgnoredRows)
        throws Exception {
        final List<HttpResultRow> mockedRows = processRows(callCount);
        callbackHandler.flush();
        checkProcessedAndIgnoredMetrics(expectedProcessedRows, expectedIgnoredRows);
        return mockedRows;
    }

    private List<HttpResultRow> processRows(int rowsCount) throws Exception {
        List<HttpResultRow> mockedRows = new ArrayList<>();
        for (int i = 0; i < rowsCount; i++) {
            final HttpResultRow mockedRow = mock(HttpResultRow.class);
            callbackHandler.processRow(mockedRow);
            mockedRows.add(mockedRow);
        }
        return mockedRows;
    }

    private void setupTimestamp(int timestamp) {
        when(metricServiceContext.getRowTimestampSeconds(any())).thenReturn(timestamp);
    }

    private void checkProcessedAndIgnoredMetrics(int expectedProcessedRows, int expectedIgnoredRows) {
        assertEquals(expectedProcessedRows, callbackHandler.getProcessedRows());
        assertEquals(expectedIgnoredRows, callbackHandler.getIgnoredRows());
    }

    private void verifyMetricsWasSend(List<HttpResultRow> expectedRows) throws Exception {
        verify(metricContextGroup).sendMetrics(any(), metricServiceContextCaptor.capture(), any());
        assertSame(metricServiceContext, metricServiceContextCaptor.getValue());
        List<HttpResultRow> actualRows = sentRows.get(0);
        assertEquals(expectedRows.size(), actualRows.size());
        for (int i = 0; i < expectedRows.size(); i++) {
            assertSame(expectedRows.get(i), actualRows.get(i));
        }
    }

    private void verifyNoMetricsWasSent() throws Exception {
        verify(metricContextGroup, never()).sendMetrics(any(), any(), any());
    }
}
