//package ru.yandex.market.markup2.tasks.fill_param_values_metric.metric;
//
//import com.google.common.base.Functions;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.junit.MockitoJUnitRunner;
//import ru.yandex.market.markup2.entries.group.ModelTypeValue;
//import ru.yandex.market.markup2.entries.group.PublishingValue;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesIdentity;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
//import ru.yandex.market.markup2.utils.Markup2TestUtils;
//import ru.yandex.market.markup2.workflow.general.TaskDataItem;
//import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
//import ru.yandex.market.mbo.http.ModelStorage;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.when;
//
///**
// * @author anmalysh
// */
//@SuppressWarnings("checkstyle:MagicNumber")
//@RunWith(MockitoJUnitRunner.class)
//public class FillParamValuesMetricRequestGeneratorTest extends FillParamValuesMetricTestBase {
//
//    @Before
//    public void setup() {
//        super.setup();
//        when(headTaskProgress.getTaskDataItemsByStates(any())).thenAnswer(i -> headItems);
//        when(headTaskInfo.getProgress()).thenReturn(headTaskProgress);
//    }
//
//    private Collection<TaskDataItem<FillParamValuesMetricDataItemPayload, FillParamValuesResponse>>
//    generateRequests() {
//        headItems = new ArrayList<>(generateRequests(10, ModelTypeValue.ALL, PublishingValue.ALL));
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
//        return spyContext.getTaskDataItems();
//    }
//
//    @Test
//    public void testGenerate() {
//        Collection<TaskDataItem<FillParamValuesMetricDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests();
//
//        assertTasks(requests, Arrays.asList(model1, model2, model5, cluster1, cluster2));
//    }
//
//    protected void assertTasks(Collection<TaskDataItem<FillParamValuesMetricDataItemPayload,
//            FillParamValuesResponse>> requests,
//                               Collection<ModelStorage.Model> models) {
//        assertEquals(models.size(), requests.size());
//        Map<Long, TaskDataItem<FillParamValuesMetricDataItemPayload, FillParamValuesResponse>> requestsById = requests
//            .stream()
//            .collect(Collectors.toMap(r -> r.getInputData().getModelId(), Functions.identity()));
//        for (ModelStorage.Model model : models) {
//            TaskDataItem<FillParamValuesMetricDataItemPayload, FillParamValuesResponse> request =
//                requestsById.get(model.getId());
//            if (request == null) {
//                fail("Task item not created for model " + model.getId());
//            }
//
//            assertTask(request, model);
//        }
//    }
//
//}
