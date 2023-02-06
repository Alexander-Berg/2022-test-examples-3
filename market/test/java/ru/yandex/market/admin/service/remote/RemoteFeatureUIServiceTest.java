package ru.yandex.market.admin.service.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.common.report.tabular.model.Cell;
import ru.yandex.common.report.tabular.model.PartReportMetaData;
import ru.yandex.common.report.tabular.model.QueryResultInfo;
import ru.yandex.common.report.tabular.model.Report;
import ru.yandex.common.report.tabular.model.ReportQueryInfo;
import ru.yandex.common.report.tabular.model.ReportQueryParamInfo;
import ru.yandex.common.report.tabular.model.Row;
import ru.yandex.common.report.tabular.model.StringCell;
import ru.yandex.market.admin.model.convert.UniConverter;
import ru.yandex.market.admin.ui.model.feature.UIFeatureCutoff;
import ru.yandex.market.admin.ui.model.feature.UIFeatureCutoffType;
import ru.yandex.market.admin.ui.model.feature.UIFeatureType;
import ru.yandex.market.admin.ui.model.report.UIReport;
import ru.yandex.market.admin.ui.model.report.UIReportQueryInfo;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(locations = "classpath:admin/admin-models.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class RemoteFeatureUIServiceTest {
    private static final long START_DATE_TS_1 = 1517581276453L;
    private static final long START_DATE_TS_2 = 1517581295114L;

    @Autowired
    private UniConverter uniConverter;

    private FeatureService featureService;

    private RemoteFeatureUIService remoteFeatureUIService;

    @Before
    public void setUp() {
        featureService = mock(FeatureService.class);
        remoteFeatureUIService = new RemoteFeatureUIService(featureService, uniConverter);
    }

    /**
     * Тест проверяет, что результат вызова {@link RemoteFeatureUIService#getFeatureCutoffs(long, ru.yandex.market.admin.ui.model.feature.UIFeatureType)}
     * успешно конвертирует бизнес модель в UI и возвращает отклбючения в обратном порядке по дате открытия.
     */
    @Test
    public void testGetFeatureCutoffsConversionToUIModel() {
        FeatureCutoffInfo info1 = new FeatureCutoffInfo.Builder()
                .setId(1L)
                .setDatasourceId(101L)
                .setFeatureCutoffType(FeatureCutoffType.PRECONDITION)
                .setStartDate(new Date(START_DATE_TS_1))
                .setFeatureType(FeatureType.FULFILLMENT)
                .setComment("test_comment")
                .build();
        FeatureCutoffInfo info2 = new FeatureCutoffInfo.Builder()
                .setId(2L)
                .setDatasourceId(101L)
                .setFeatureCutoffType(FeatureCutoffType.MANAGER)
                .setStartDate(new Date(START_DATE_TS_2))
                .setFeatureType(FeatureType.FULFILLMENT)
                .setComment("test comment 2")
                .build();
        when(featureService.getCutoffs(anyLong(), any())).thenReturn(Arrays.asList(info1, info2));

        ArrayList<UIFeatureCutoff> uiCutoffs =
                remoteFeatureUIService.getFeatureCutoffs(101L, UIFeatureType.FULFILLMENT);

        assertThat(uiCutoffs, contains(uiModelMatcher(info2), uiModelMatcher(info1)));
    }

    /**
     * Тест проверят, что все {@link FeatureCutoffType} успешно конвертируются в {@link UIFeatureCutoffType}.
     */
    @Test
    public void testAllFeatureCutoffTypesAreConvertible() {
        for (FeatureCustomCutoffType coreType : FeatureCutoffType.values()) {
            UIFeatureCutoffType uiType = uniConverter.fromCoreToUI(coreType);

            assertThat("Expected value: " + coreType, uiType, notNullValue());
            assertThat(uiType.getStringId(), equalTo(coreType.name()));
        }
    }

    /**
     * Тест проверят, что все {@link FeatureType} успешно конвертируются в {@link UIFeatureType}.
     */
    @Test
    public void testAllFeatureTypesAreConvertible() {
        for (FeatureType coreType : FeatureType.values()) {
            UIFeatureType uiType = uniConverter.fromCoreToUI(coreType);

            assertThat("Expected value: " + coreType, uiType, notNullValue());
            assertThat(uiType.getStringId(), equalTo(coreType.name()));
        }
    }

    /**
     * Тест проверят, что {@link ru.yandex.common.report.tabular.model.Report} успешно конвертируются в {@link ru.yandex.market.admin.ui.model.report.UIReport}.
     */
    @Test
    public void testLibraryReportsMapsToUiReport() {
        List<Cell> cells = List.of(new StringCell("test"));
        var rows = List.of(new Row(cells));
        var report = new Report(new PartReportMetaData(10, 0, 10), rows);
        UIReport uiReport = uniConverter.fromCoreToUI(report);
        assertThat(uiReport.getMetaData().getTotalRowCount(), equalTo(10));
        assertThat(uiReport.getRows().size(), equalTo(1));
        assertThat(uiReport.getRows().get(0).getCells().get(0).getStringValue(), equalTo("test"));
    }

    /**
     * Тест проверят, что {@link ru.yandex.common.report.tabular.model.ReportQueryInfo} успешно конвертируются в {@link ru.yandex.market.admin.ui.model.report.UIReportQueryInfo}.
     */
    @Test
    public void testLibraryReportQueryInfoMapsToUiReportQueryInfo() {
        var tst = new TreeMap<String, ReportQueryInfo>();
        var reportQueryInfo = new ReportQueryInfo();
        tst.put("1", reportQueryInfo);
        var result = new QueryResultInfo("title");
        var reportQueryParamInfo = new ReportQueryParamInfo();
        reportQueryParamInfo.setName("name");
        reportQueryParamInfo.setType(1);
        var paramNames = new TreeMap<String, String>();
        paramNames.put("1", "test");
        reportQueryInfo.setParamInfo(Map.of("1", reportQueryParamInfo));
        reportQueryInfo.setResultInfoList(List.of(result));
        reportQueryInfo.setParamNames(paramNames);
        List<UIReportQueryInfo> queryInfos = uniConverter.fromCoreToUI(new ArrayList<>(tst.values()));
        assertThat(queryInfos.get(0).getTableHeaders().get(0).getTitle(), equalTo("title"));
        assertThat(queryInfos.get(0).getParameters().get("1"), equalTo("test"));
    }

    private Matcher<UIFeatureCutoff> uiModelMatcher(final FeatureCutoffInfo featureCutoffInfo) {
        return new TypeSafeMatcher<UIFeatureCutoff>() {
            @Override
            protected boolean matchesSafely(UIFeatureCutoff item) {
                return Objects.equals(item.getLongField(UIFeatureCutoff.ID), featureCutoffInfo.getId())
                        && Objects.equals(
                        item.getDateField(UIFeatureCutoff.START_DATE), featureCutoffInfo.getStartDate())
                        && Objects.equals(
                        item.getField(UIFeatureCutoff.FEATURE_CUTOFF_TYPE),
                        uniConverter.fromCoreToUI(featureCutoffInfo.getFeatureCutoffType()))
                        && Objects.equals(
                        item.getStringField(UIFeatureCutoff.COMMENT), featureCutoffInfo.getComment());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(featureCutoffInfo);
            }
        };
    }
}
