//package ru.yandex.market.markup2.tasks.fill_param_values_metric.etalon;
//
//import com.google.common.base.Functions;
//import com.google.common.collect.Lists;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.junit.MockitoJUnitRunner;
//import ru.yandex.market.markup2.entries.group.ModelIds;
//import ru.yandex.market.markup2.entries.group.ModelTypeValue;
//import ru.yandex.market.markup2.entries.group.ParameterType;
//import ru.yandex.market.markup2.entries.group.PublishingValue;
//import ru.yandex.market.markup2.entries.task.TaskInfo;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesIdentity;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesRequestGeneratorTestBase;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
//import ru.yandex.market.markup2.utils.Markup2TestUtils;
//import ru.yandex.market.markup2.workflow.general.TaskDataItem;
//import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
//import ru.yandex.market.mbo.http.ModelStorage;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//import static org.mockito.ArgumentMatchers.anyCollection;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.when;
///**
// * @author anmalysh
// */
//@SuppressWarnings("checkstyle:MagicNumber")
//@RunWith(MockitoJUnitRunner.class)
//public class EtalonParamValuesRequestGeneratorTest extends EtalonParamValuesTestBase {
//
//    @Before
//    public void setup() {
//        super.setup(true);
//    }
//
//    @Test
//    public void generateByIds() throws InterruptedException {
//        int countToGenerate = 10;
//        setModelStorageMock();
//        ArrayList<Long> modelIdsParam = Lists.newArrayList(1L, 2L, 3L);
//        Map<ParameterType, Object> params = createParams(ModelTypeValue.GURU,
//                                                         PublishingValue.ALL,
//                                                         new ModelIds(modelIdsParam));
//        TaskInfo basicTaskInfo =
//            Markup2TestUtils.createBasicTaskInfo(FillParamValuesRequestGeneratorTestBase.CATEGORY_ID,
//                                                 countToGenerate,
//                                                 params);
//        RequestGeneratorContext<FillParamValuesIdentity,
//            EtalonParamValuesDataItemPayload,
//            FillParamValuesResponse> ctx = runGenerateRequests(basicTaskInfo);
//        assertTasks(ctx.getTaskDataItems(), Arrays.asList(model1, model2));
//    }
//
//    @Test
//    public void generateGuruAll() throws InterruptedException {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests(10, ModelTypeValue.GURU, PublishingValue.ALL);
//
//        assertTasks(requests, Arrays.asList(model1, model2, model5));
//    }
//
//    @Test
//    public void generateGuruUnpublished() throws InterruptedException {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests(10, ModelTypeValue.GURU, PublishingValue.UNPUBLISHED);
//
//        assertTasks(requests, Arrays.asList(model1));
//    }
//
//    @Test
//    public void generateGuruPublished() throws InterruptedException {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests(10, ModelTypeValue.GURU, PublishingValue.PUBLISHED);
//
//        assertTasks(requests, Arrays.asList(model2, model5));
//    }
//
//    @Test
//    public void generateClustersAll() throws InterruptedException {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests(10, ModelTypeValue.CLUSTERS, PublishingValue.ALL);
//
//        assertTasks(requests, Arrays.asList(cluster1, cluster2));
//    }
//
//    @Test
//    public void generateClustersUnpublished() throws InterruptedException {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests(10, ModelTypeValue.CLUSTERS, PublishingValue.UNPUBLISHED);
//
//        assertTasks(requests, Arrays.asList(cluster1));
//    }
//
//    @Test
//    public void generateClustersPublished() throws InterruptedException {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests(10, ModelTypeValue.CLUSTERS, PublishingValue.PUBLISHED);
//
//        assertTasks(requests, Arrays.asList(cluster2));
//    }
//
//    @Test
//    public void generateAllAll() throws InterruptedException {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests(10, ModelTypeValue.ALL, PublishingValue.ALL);
//
//        assertTasks(requests, Arrays.asList(model1, model2, model5, cluster1, cluster2));
//    }
//
//    @Test
//    public void generateAllUnpublished() throws InterruptedException {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests(10, ModelTypeValue.ALL, PublishingValue.UNPUBLISHED);
//
//        assertTasks(requests, Arrays.asList(model1, cluster1));
//    }
//
//    @Test
//    public void generateAllPublished() throws InterruptedException {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requests =
//            generateRequests(10, ModelTypeValue.ALL, PublishingValue.PUBLISHED);
//
//        assertTasks(requests, Arrays.asList(model2, model5, cluster2));
//    }
//
//    private void assertTasks(Collection<TaskDataItem<EtalonParamValuesDataItemPayload,
//            FillParamValuesResponse>> requests,
//                             Collection<ModelStorage.Model> models) {
//        assertEquals(models.size(), requests.size());
//        Map<Long, TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> requestsById = requests
//            .stream()
//            .collect(Collectors.toMap(r -> r.getInputData().getModelId(), Functions.identity()));
//        for (ModelStorage.Model model : models) {
//            TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse> request =
//                requestsById.get(model.getId());
//            if (request == null) {
//                fail("Task item not created for model " + model.getId());
//            }
//
//            assertTask(request, model);
//        }
//    }
//
//    private void setModelStorageMock() {
//        when(modelStorageService.getModels(anyLong(), anyCollection())).thenAnswer(i -> {
//            List<Long> modelIds = i.getArgument(1);
//            return models.stream().filter(model -> modelIds.contains(model.getId())).collect(Collectors.toList());
//        });
//    }
//}
