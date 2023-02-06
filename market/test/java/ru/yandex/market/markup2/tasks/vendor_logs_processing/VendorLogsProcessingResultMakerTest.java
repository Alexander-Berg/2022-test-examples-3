package ru.yandex.market.markup2.tasks.vendor_logs_processing;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.entries.group.ApplyResults;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.utils.ModelTestUtils;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.resultMaker.ResultMakerContext;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author galaev@yandex-team.ru
 * @since 23/01/2018.
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class VendorLogsProcessingResultMakerTest {

    private static final int CATEGORY_ID = 1;
    private static final long VENDOR_ID = 2;
    private static final int MODELS_COUNT = 5;
    private static final long ATTACH_TO_MODEL_ID = 11L;
    private static final Random RANDOM = new Random(1);

    @Mock
    private ModelStorageService modelStorageService;

    @Captor
    private ArgumentCaptor<List<ModelStorage.Model>> modelCaptor;

    private List<ModelStorage.Model> models;
    private VendorLogsProcessingResultMaker resultMaker;
    private ResultMakerContext
        <VendorLogsProcessingIdentity, VendorLogsProcessingPayload, VendorLogsProcessingResponse> resultMakerContext;


    @Before
    public void setUp() {
        models = createModels();
        ModelTestUtils.mockModelStorageGetModels(modelStorageService, models);
        ModelTestUtils.mockModelStorageSaveModels(modelStorageService, Sets.newHashSet(1L, 2L, 3L, 4L));

        resultMaker = new VendorLogsProcessingResultMaker();
        resultMaker.setModelStorageService(modelStorageService);
        TaskInfo taskInfo = Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, MODELS_COUNT,
            Collections.singletonMap(ParameterType.APPLY_RESULTS, ApplyResults.TRUE));

        resultMakerContext = Markup2TestUtils.createResultMakerContext(taskInfo);
        resultMakerContext.addDataItems(createDataItems());
    }

    private List<ModelStorage.Model> createModels() {
        List<ModelStorage.Model> myModels = new ArrayList<>();
        for (long i = 1; i <= MODELS_COUNT; ++i) {
            long sourceModelId = i;
            long targetModelId = i + 10;
            ModelStorage.Model sourceModel = ModelTestUtils.createModel(
                CATEGORY_ID, VENDOR_ID, sourceModelId, "VENDOR", true)
                .toBuilder()
                .addRelations(ModelStorage.Relation.newBuilder()
                    .setCategoryId(CATEGORY_ID)
                    .setType(ModelStorage.RelationType.SYNC_TARGET)
                    .setId(targetModelId)
                    .build())
                .build();
            ModelStorage.Model targetModel = ModelTestUtils.createModel(
                CATEGORY_ID, VENDOR_ID, targetModelId, "GURU", true)
                .toBuilder()
                .addRelations(ModelStorage.Relation.newBuilder()
                    .setCategoryId(CATEGORY_ID)
                    .setType(ModelStorage.RelationType.SYNC_SOURCE)
                    .setId(sourceModelId)
                    .build())
                .build();
            myModels.add(sourceModel);
            myModels.add(targetModel);
        }
        return myModels;
    }

    @Test
    public void makeResult() {
        resultMaker.makeResults(resultMakerContext);

        Mockito.verify(modelStorageService, Mockito.times(5)).saveModels(modelCaptor.capture());
        Map<Long, ModelStorage.Model> savedModels = new HashMap<>();
        for (Collection<ModelStorage.Model> col : modelCaptor.getAllValues()) {
            col.forEach(model -> savedModels.put(model.getId(), model));
        }

        Assert.assertEquals(7, savedModels.size());

        // assert deleted relations
        asssertHasNoRelations(savedModels.get(4L));
        asssertHasNoRelations(savedModels.get(5L));
        asssertHasNoRelations(savedModels.get(14L));
        asssertHasNoRelations(savedModels.get(15L));

        // assert updated relation
        assertHasRelation(savedModels.get(3L), 11L);
        assertHasRelation(savedModels.get(11L), 3L);
        asssertHasNoRelations(savedModels.get(13L));
    }

    private void asssertHasNoRelations(ModelStorage.Model model) {
        Assert.assertTrue(model.getRelationsList().isEmpty());
    }

    private void assertHasRelation(ModelStorage.Model model, long relatedModelId) {
        Assert.assertEquals(relatedModelId, model.getRelations(0).getId());
    }

    private Collection<TaskDataItem<VendorLogsProcessingPayload, VendorLogsProcessingResponse>> createDataItems() {
        List<TaskDataItem<VendorLogsProcessingPayload, VendorLogsProcessingResponse>> dataItems = new ArrayList<>();
        long modelId = 0;
        dataItems.add(createDataItem(VendorLogsProcessingStatus.OK, ++modelId));
        dataItems.add(createDataItem(VendorLogsProcessingStatus.CANNOT, ++modelId));
        dataItems.add(createDataItem(VendorLogsProcessingStatus.MATCH_CARD, ++modelId));
        dataItems.add(createDataItem(VendorLogsProcessingStatus.OTHER_CAT, ++modelId));
        dataItems.add(createDataItem(VendorLogsProcessingStatus.NEW_CARD, ++modelId));
        dataItems.add(createDataItem(VendorLogsProcessingStatus.WRONG_VENDOR, ++modelId));
        return dataItems;
    }

    private TaskDataItem<VendorLogsProcessingPayload, VendorLogsProcessingResponse>
    createDataItem(VendorLogsProcessingStatus status, long modelId) {
        VendorLogsProcessingIdentity identifier = new VendorLogsProcessingIdentity(CATEGORY_ID, modelId);
        TaskDataItem<VendorLogsProcessingPayload, VendorLogsProcessingResponse> dataItem =
            new TaskDataItem<>(RANDOM.nextLong(), new VendorLogsProcessingPayload(identifier, null));
        dataItem.setResponseInfo(
            new VendorLogsProcessingResponse(RANDOM.nextLong(), status.toString(), CATEGORY_ID, ATTACH_TO_MODEL_ID));
        return dataItem;
    }
}
