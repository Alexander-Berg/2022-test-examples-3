package ru.yandex.market.mbo.db.modelstorage.data.group;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.OperationStatusTestUtils;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class GroupOperationStatusTest extends BaseModelSaveGroupTest {

    @Test
    public void testDefaultStatusFailedInGroup() {
        CommonModel model1 = createAndSaveModel();
        CommonModel model2 = createAndSaveModel();
        model2.setDeleted(true);
        List<CommonModel> requestedModels = Collections.singletonList(model1);
        List<CommonModel> additionalModels = Collections.singletonList(model2);

        ModelSaveGroup group = ModelSaveGroupFactory.create(requestedModels, additionalModels);
        group.addBeforeModels(storageService.searchByIds(Arrays.asList(model1.getId(), model2.getId())));

        GroupOperationStatus status = group.generateOverallStatus();
        OperationStatusTestUtils.assertGroupOperationStatus(status, OperationStatusType.FAILED_MODEL_IN_GROUP,
            OperationStatusType.FAILED_MODEL_IN_GROUP.getDefaultStatusMessage(),
            Collections.singletonList(model1.getId()), 1, 0);

        OperationStatus requestedOperationStatus = status.getRequestedModelStatuses().get(0);
        // FAILED_MODEL_IN_GROUP is filtered from additional statuses. Noone is interested in it.
        assertThat(status.getAdditionalModelStatues()).hasSize(0);

        OperationStatusTestUtils.assertOperationStatus(requestedOperationStatus, OperationType.CHANGE,
            OperationStatusType.FAILED_MODEL_IN_GROUP,
            OperationStatusType.FAILED_MODEL_IN_GROUP.getDefaultStatusMessage(),
            model1.getId(), null, null);
    }

    @Test
    public void testFailedStatus() {
        CommonModel model1 = createAndSaveModel();
        CommonModel model2 = createAndSaveModel();
        model2.setDeleted(true);
        List<CommonModel> requestedModels = Collections.singletonList(model1);
        List<CommonModel> additionalModels = Collections.singletonList(model2);

        ModelSaveGroup group = ModelSaveGroupFactory.create(requestedModels, additionalModels);
        group.addBeforeModels(storageService.searchByIds(Arrays.asList(model1.getId(), model2.getId())));

        group.setStatusToAllModels(OperationStatusType.INTERNAL_ERROR, "test test");

        GroupOperationStatus status = group.generateOverallStatus();
        OperationStatusTestUtils.assertGroupOperationStatus(status,
            OperationStatusType.INTERNAL_ERROR, "test test", Arrays.asList(model1.getId(), model2.getId()), 1, 1);

        OperationStatus requestedOperationStatus = status.getRequestedModelStatuses().get(0);
        OperationStatus additionalOperationStatus = status.getAdditionalModelStatues().get(0);

        OperationStatusTestUtils.assertOperationStatus(requestedOperationStatus, OperationType.CHANGE,
            OperationStatusType.INTERNAL_ERROR, "test test",
            model1.getId(), null, null);
        OperationStatusTestUtils.assertOperationStatus(additionalOperationStatus, OperationType.REMOVE,
            OperationStatusType.INTERNAL_ERROR, "test test",
            model2.getId(), null, null);
    }

    @Test
    public void testSuccessfulStatusWhenModelIdChanged() {
        CommonModel model = createAndSaveModel();
        long modelId = model.getId();

        ModelSaveGroup group = ModelSaveGroup.fromModels(model);
        group.addBeforeModels(storageService.searchByIds(Collections.singletonList(modelId)));
        group.setStatusToAllModels(OperationStatusType.OK,
            "Айм э барби гёрл ин э барби вёрлд, лайф ин пластик - итс фантастик");

        // change id
        model.setId(100);

        GroupOperationStatus status = group.generateOverallStatus();

        OperationStatusTestUtils.assertGroupOperationStatus(status, OperationStatusType.OK,
            "Айм э барби гёрл ин э барби вёрлд, лайф ин пластик - итс фантастик",
            Collections.emptyList(), 1, 0);

        OperationStatus requestedOperationStatus = status.getRequestedModelStatuses().get(0);

        OperationStatusTestUtils.assertOperationStatus(requestedOperationStatus, OperationType.CHANGE,
            OperationStatusType.OK, "Айм э барби гёрл ин э барби вёрлд, лайф ин пластик - итс фантастик",
            modelId, model.getId(), model);
    }

    @Test
    public void testGroupOperationStatusOrderingWontChange() {
        List<CommonModel> requestedModels = Arrays.asList(createdModel1, updatedModel1, deletedModel1);
        List<CommonModel> additionalModels = Arrays.asList(createdModel2, updatedModel2, deletedModel2);

        ModelSaveGroup group = ModelSaveGroupFactory.create(requestedModels, additionalModels);
        group.setStatus(createdModel2, OperationStatusType.OK);
        group.setStatus(updatedModel2, OperationStatusType.OK);
        group.setStatus(deletedModel2, OperationStatusType.OK);
        createdModel1.setId(100);
        createdModel2.setId(200);

        List<Long> expectedRequestedModelIds = Arrays.asList(
            createdModel1.getId(), updatedModel1.getId(), deletedModel1.getId());
        List<Long> expectedAdditionalModelIds = Arrays.asList(
            createdModel2.getId(), updatedModel2.getId(), deletedModel2.getId());

        List<OperationType> expectedRequestedOperations = Arrays.asList(OperationType.CREATE, OperationType.CHANGE,
            OperationType.REMOVE);
        List<OperationType> expectedAdditionalOperations = Arrays.asList(OperationType.CREATE, OperationType.CHANGE,
            OperationType.REMOVE);

        Assert.assertEquals(expectedRequestedModelIds, getRequestedModelIds(group));
        Assert.assertEquals(expectedAdditionalModelIds, getAdditionalModelIds(group));
        Assert.assertEquals(expectedRequestedOperations, getRequestedOperationTypes(group));
        Assert.assertEquals(expectedAdditionalOperations, getAdditionalOperationTypes(group));
    }
}
