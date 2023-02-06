//package ru.yandex.market.markup2.tasks.fill_param_values_metric.category;
//
//import com.google.common.collect.ImmutableMap;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.mockito.junit.MockitoJUnitRunner;
//import ru.yandex.market.markup2.entries.group.ITaskGroupMetricsData;
//import ru.yandex.market.markup2.entries.group.ModelTypeValue;
//import ru.yandex.market.markup2.entries.group.ParameterType;
//import ru.yandex.market.markup2.entries.group.PublishingValue;
//import ru.yandex.market.markup2.entries.group.TaskGroupMetrics;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
//import ru.yandex.market.markup2.tasks.fill_param_values_metric.etalon.EtalonParamValuesDataItemPayload;
//import ru.yandex.market.markup2.tasks.fill_param_values_metric.metric.FillParamValuesMetricTestBase;
//import ru.yandex.market.markup2.tasks.fill_param_values_metric.metric.FillParamValuesMetricsData;
//import ru.yandex.market.markup2.utils.Markup2TestUtils;
//import ru.yandex.market.markup2.utils.ModelTestUtils;
//import ru.yandex.market.markup2.workflow.general.TaskDataItem;
//import ru.yandex.market.markup2.workflow.resultMaker.ResultMakerContext;
//import ru.yandex.market.mbo.http.ModelStorage;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.Assert.assertEquals;
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.Mockito.when;
//
///**
// * @author V.Zaytsev (breezzo@yandex-team.ru)
// * @since 22.08.2017
// */
//@SuppressWarnings("checkstyle:MagicNumber")
//@RunWith(MockitoJUnitRunner.class)
//public class CategoryParamValuesMetricResultCollectorTest extends FillParamValuesMetricTestBase {
//
//    private CategoryParamValuesMetricResultMaker resultCollector;
//
//    @Before
//    public void setup() {
//        super.setup(false);
//
//        when(paramUtils.getAllParams(anyInt())).thenReturn(categoryParameters);
//
//        resultCollector = new CategoryParamValuesMetricResultMaker();
//        resultCollector.setModelStorageService(modelStorageService);
//        resultCollector.setParamUtils(paramUtils);
//    }
//
//    @Test
//    public void testMetricCalculation() {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> items =
//            generateRequests(2, ModelTypeValue.GURU, PublishingValue.PUBLISHED);
//
//        Assert.assertEquals(2, items.size());
//
//        mockGetModels(items);
//
//        List<FillParamValuesResponse> etalonResponses = Arrays.asList(
//            createResponse(ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), "Value1")),
//            createResponse(ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), "Value2"))
//        );
//
//        setResponses(etalonResponses, items);
//
//        Map<ParameterType, Object> parameters = ImmutableMap.of(
//            ParameterType.MODEL_TYPE, ModelTypeValue.GURU,
//            ParameterType.PUBLISHING, PublishingValue.PUBLISHED
//        );
//
//        ResultMakerContext resultContext =
//            Markup2TestUtils.createResultMakerContext(
//                Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, 2, parameters)
//            );
//
//        resultContext.addDataItems(items);
//
//        resultCollector.makeResults(resultContext);
//
//        TaskGroupMetrics<ITaskGroupMetricsData> metrics = resultContext.getGroupConfigInfo().getMetrics();
//        FillParamValuesMetricsData metricsData = (FillParamValuesMetricsData) metrics.getMetricsData();
//
//        assertEquals("guru_published", metricsData.getCardTypes());
//        assertEquals(CATEGORY_ID, metricsData.getCategoryId());
//        assertDouble(0.4166, metricsData.getF05());
//        assertDouble(0.5, metricsData.getNotIdealCards());
//        assertDouble(0.425, metricsData.getAuc());
//        assertDouble(0.5, metricsData.getPrecision());
//        assertDouble(0.5, metricsData.getRecall());
//
//        assertDouble(0.4875, metricsData.getAucImportant());
//        assertDouble(0.5, metricsData.getRecallImportant());
//        assertDouble(0.5, metricsData.getPrecisionImportant());
//    }
//
//    private static void assertDouble(double expected, double actual) {
//        assertEquals(expected, actual, 0.0001);
//    }
//
//    private void mockGetModels(
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> items) {
//
//        List<ModelStorage.Model> metricModels = new ArrayList<>();
//        Iterator<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> itemsIt = items.iterator();
//
//        long modelId = itemsIt.next().getInputData().getModelId();
//        ModelStorage.Model model = getModel(modelId);
//
//        metricModels.add(
//            model.toBuilder()
//                .addParameterValues(ModelTestUtils.createNumericValue(customParam1, "20"))
//                .build()
//        );
//
//        model = getModel(itemsIt.next().getInputData().getModelId());
//        metricModels.add(
//            model.toBuilder()
//                .addParameterValues(ModelTestUtils.createParameterValue(customParam2).setOptionId(20))
//                .build()
//        );
//
//        Mockito.doReturn(metricModels)
//            .when(modelStorageService)
//            .getModels(Mockito.anyLong(), Mockito.anyCollection());
//    }
//}
