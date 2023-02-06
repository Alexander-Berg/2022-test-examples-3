package ru.yandex.market.abo.core.dynamic;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.report.Const;
import ru.yandex.common.report.graph.model.GraphReportColumnFilter;
import ru.yandex.common.report.tabular.AbstractReportFactory;
import ru.yandex.common.report.tabular.model.Cell;
import ru.yandex.common.report.tabular.model.Report;
import ru.yandex.common.report.tabular.model.Row;
import ru.yandex.common.util.StringUtils;
import ru.yandex.common.util.parameters.ParametersSource;
import ru.yandex.market.statface.StatfaceData;
import ru.yandex.market.statface.StatfacePeriod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.statface.dashboard.DashboardConsts.STATFACE_ABO_FOLDER;

/**
 * @author artemmz
 * @date 04.10.17.
 */
public class StatfaceCommonReportAdapterTest {
    private static final String REPORT_NAME = "reportMe";
    private static final int WINDOW_WIDTH = 1;
    private static final String QUERY_PARAM = "?report_name=dbReport&query_name=admin:uc_problem_approve_by_date&graph_report__period=WW";

    @Mock
    private AbstractReportFactory reportFactory;
    @Mock
    private Report report;
    @Mock
    private Row row;
    @Mock
    private Cell cell;
    @Mock
    private GraphReportColumnFilter columnFilter;
    @Mock
    private GraphReportColumnFilter.ColumnInfo columnInfo;

    private StatfaceCommonReportAdapter statfaceReport;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        statfaceReport = new StatfaceCommonReportAdapter(REPORT_NAME, WINDOW_WIDTH, QUERY_PARAM, reportFactory);
    }

    @Test
    public void prepareRequest() throws Exception {
        ParametersSource request = statfaceReport.prepareRequest(new Date(), new Date());
        assertFalse(StringUtils.isEmpty(request.getParam(Const.REPORT_NAME)));
        assertFalse(StringUtils.isEmpty(request.getParam(Const.QUERY_NAME_PARAM)));
        assertFalse(StringUtils.isEmpty(request.getParam(Const.GRAPH_REPORT__FROM)));
        assertFalse(StringUtils.isEmpty(request.getParam(Const.GRAPH_REPORT__TO)));
        assertFalse(StringUtils.isEmpty(request.getParam(Const.GRAPH_REPORT__GENERATING)));
        assertFalse(StringUtils.isEmpty(request.getParam(Const.GRAPH_REPORT__PERIOD)));
    }

    @Test
    public void fillWithReportData() throws Exception {
        when(report.getTableHeaders()).thenReturn(Arrays.asList("x", "y1", "y2", "not graph column"));
        when(report.getRows()).thenReturn(Collections.singletonList(row));
        when(row.getCells()).thenReturn(Arrays.asList(cell, cell, cell));
        when(cell.getStringValue()).thenReturn("1970-01-01 00:00:00").thenReturn("foo").thenReturn("bar");

        when(report.getGraphReportColumnFilter()).thenReturn(columnFilter);
        when(columnFilter.getYColumnInfoList()).thenReturn(Arrays.asList(columnInfo, columnInfo));
        when(columnInfo.getName()).thenReturn("y1").thenReturn("y2");

        StatfaceData statfaceData = statfaceReport.fillWithReportData(report);

        assertEquals(STATFACE_ABO_FOLDER + REPORT_NAME, statfaceData.getReportName());
        assertEquals(StatfacePeriod.weekly, statfaceData.getPeriod());
        assertEquals(statfaceData.toJson(), "{\"values\":[{\"fielddate\":\"1970-01-01 00:00:00\",\"y1\":\"foo\",\"y2\":\"bar\"}]}");
    }

    @Test
    public void mustHaveXColumn() throws Exception {
        when(report.getTableHeaders()).thenReturn(Arrays.asList("y0", "y1", "y2"));
        assertThrows(RuntimeException.class, () ->
                statfaceReport.fillWithReportData(report));
    }
}
