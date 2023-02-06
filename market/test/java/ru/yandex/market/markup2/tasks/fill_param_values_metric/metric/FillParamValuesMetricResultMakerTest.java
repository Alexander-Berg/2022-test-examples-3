//package ru.yandex.market.markup2.tasks.fill_param_values_metric.metric;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.common.collect.ImmutableMap;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.junit.MockitoJUnitRunner;
//import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
//import ru.yandex.market.markup2.entries.group.ITaskGroupMetricsData;
//import ru.yandex.market.markup2.entries.group.ModelTypeValue;
//import ru.yandex.market.markup2.entries.group.ParameterType;
//import ru.yandex.market.markup2.entries.group.PublishingValue;
//import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
//import ru.yandex.market.markup2.entries.group.TaskGroupMetrics;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesIdentity;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
//import ru.yandex.market.markup2.utils.Markup2TestUtils;
//import ru.yandex.market.markup2.workflow.general.TaskDataItem;
//import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
//import ru.yandex.market.markup2.workflow.resultMaker.ResultMakerContext;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.Assert.assertEquals;
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.Matchers.any;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.when;
//
///**
// * @author anmalysh
// */
//@SuppressWarnings("checkstyle:MagicNumber")
//@RunWith(MockitoJUnitRunner.class)
//public class FillParamValuesMetricResultMakerTest extends FillParamValuesMetricTestBase {
//
//    private FillParamValuesMetricResultMaker resultMaker;
//
//    @Before
//    public void setup() {
//        super.setup();
//
//        when(paramUtils.getAllParams(anyInt())).thenReturn(categoryParameters);
//        when(headTaskProgress.getTaskDataItemsByStates(any())).thenAnswer(i -> headItems);
//        when(headTaskInfo.getProgress()).thenReturn(headTaskProgress);
//
//        TaskConfigGroupInfo groupInfo = mock(TaskConfigGroupInfo.class);
//        TaskConfigInfo info = mock(TaskConfigInfo.class);
//        when(groupInfo.getParameterValueOrDefault(any(ParameterType.class), any())).thenAnswer(i -> {
//            ParameterType type = i.getArgument(0);
//            if (ParameterType.MODEL_TYPE.equals(type)) {
//                return ModelTypeValue.ALL;
//            } else if (ParameterType.PUBLISHING.equals(type)) {
//                return PublishingValue.UNPUBLISHED;
//            }
//            return null;
//        });
//        when(info.getGroupInfo()).thenReturn(groupInfo);
//        when(headTaskInfo.getConfig()).thenReturn(info);
//
//        FillParamValuesMetricResultMaker collector = new FillParamValuesMetricResultMaker();
//        collector.setParamUtils(paramUtils);
//        resultMaker = spy(collector);
//        headItems = new ArrayList<>(generateRequests(10, ModelTypeValue.ALL, PublishingValue.UNPUBLISHED));
//    }
//
//    private ResultMakerContext<FillParamValuesIdentity, FillParamValuesMetricDataItemPayload, FillParamValuesResponse>
//    processResponses(Collection<FillParamValuesResponse> etalonResponses,
//                     Collection<FillParamValuesResponse> tolokaResponses) {
//        RequestGeneratorContext<FillParamValuesIdentity, FillParamValuesMetricDataItemPayload, FillParamValuesResponse>
//            contextImpl =
//            Markup2TestUtils.createGenerationContext(
//                Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, 10, Collections.emptyMap()),
//                Markup2TestUtils.createBasicUniqueContext(),
//                Markup2TestUtils.mockIdGenerator()
//            );
//
//        RequestGeneratorContext<FillParamValuesIdentity, FillParamValuesMetricDataItemPayload, FillParamValuesResponse>
//            spyContext = spy(contextImpl);
//
//        when(spyContext.getHeadTask()).thenReturn(headTaskInfo);
//
//        metricGenerator.generateRequests(spyContext);
//
//        Collection<TaskDataItem<FillParamValuesMetricDataItemPayload, FillParamValuesResponse>> tolokaItems =
//        spyContext.getTaskDataItems();
//
//        assertEquals(etalonResponses.size(), headItems.size());
//        assertEquals(tolokaResponses.size(), tolokaItems.size());
//
//        setResponses(etalonResponses, headItems);
//        setResponses(tolokaResponses, tolokaItems);
//
//        ResultMakerContext<FillParamValuesIdentity, FillParamValuesMetricDataItemPayload, FillParamValuesResponse>
//            resultMakerContextImpl = Markup2TestUtils.createResultMakerContext(
//            Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, 10, Collections.emptyMap())
//        );
//
//        resultMakerContextImpl.addDataItems(tolokaItems);
//
//        ResultMakerContext<FillParamValuesIdentity, FillParamValuesMetricDataItemPayload, FillParamValuesResponse>
//            resultMakerSpyContext = spy(resultMakerContextImpl);
//
//        when(resultMakerSpyContext.getHeadTask()).thenReturn(headTaskInfo);
//
//        resultMaker.makeResults(resultMakerSpyContext);
//
//        return resultMakerSpyContext;
//    }
//
//    @Test
//    public void testMetricCalculation() {
//        List<FillParamValuesResponse> etalonResponses = Arrays.asList(
//            createResponse(ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), 20)),
//            createResponse(ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), 10))
//        );
//        List<FillParamValuesResponse> tolokaResponses = Arrays.asList(
//            createResponse(ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), 10)),
//            createResponse(ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), 10))
//        );
//
//        ResultMakerContext<FillParamValuesIdentity, FillParamValuesMetricDataItemPayload,
//                FillParamValuesResponse> context =
//            processResponses(etalonResponses, tolokaResponses);
//        TaskGroupMetrics<ITaskGroupMetricsData> metrics = context.getGroupConfigInfo().getMetrics();
//        FillParamValuesMetricsData metricsData = (FillParamValuesMetricsData) metrics.getMetricsData();
//
//        assertEquals("all_unpublished", metricsData.getCardTypes());
//        assertEquals(311, metricsData.getCategoryId());
//        assertDouble(0.7777, metricsData.getF05());
//        assertDouble(0.5, metricsData.getNotIdealCards());
//        assertDouble(0.7625, metricsData.getAuc());
//        assertDouble(0.75, metricsData.getPrecision());
//        assertDouble(1, metricsData.getRecall());
//
//        assertDouble(0.4875, metricsData.getAucImportant());
//        assertDouble(1.0, metricsData.getRecallImportant());
//        assertDouble(0.5, metricsData.getPrecisionImportant());
//    }
//
//    @Test
//    public void testMetricSerialization() throws IOException {
//        FillParamValuesMetricsData metrics = new FillParamValuesMetricsData(1, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8,
//            "card_types");
//
//        Class<FillParamValuesMetricsData> clazz = FillParamValuesMetricsData.class;
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        String value = objectMapper.writeValueAsString(metrics);
//        System.out.println(value);
//        FillParamValuesMetricsData metricsCopy = objectMapper.readValue(value, clazz);
//
//        Assert.assertEquals(metrics.getCategoryId(), metricsCopy.getCategoryId());
//        Assert.assertEquals(metrics.getF05(), metricsCopy.getF05(), 0);
//        Assert.assertEquals(metrics.getNotIdealCards(), metricsCopy.getNotIdealCards(), 0);
//        Assert.assertEquals(metrics.getAuc(), metricsCopy.getAuc(), 0);
//        Assert.assertEquals(metrics.getPrecision(), metricsCopy.getPrecision(), 0);
//        Assert.assertEquals(metrics.getRecall(), metricsCopy.getRecall(), 0);
//        Assert.assertEquals(metrics.getAucImportant(), metricsCopy.getAucImportant(), 0);
//        Assert.assertEquals(metrics.getPrecisionImportant(), metricsCopy.getPrecisionImportant(), 0);
//        Assert.assertEquals(metrics.getRecallImportant(), metricsCopy.getRecallImportant(), 0);
//        Assert.assertEquals(metrics.getCardTypes(), metricsCopy.getCardTypes());
//    }
//
//    private void assertDouble(double expected, double actual) {
//        assertEquals(expected, actual, 0.0001);
//    }
//}
