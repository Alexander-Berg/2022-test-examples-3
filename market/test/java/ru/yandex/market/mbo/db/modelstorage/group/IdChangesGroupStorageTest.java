package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

/**
 * Тесты проверяют корректность смены айдишника при сохранении моделей.
 *
 * @author s-ermakov
 */
@RunWith(MockitoJUnitRunner.class)
public class IdChangesGroupStorageTest extends BaseGroupStorageUpdatesTest {

    private CommonModel newGuruModel;
    private CommonModel guruModel;
    private CommonModel newClusterModel;
    private CommonModel clusterModel;
    private CommonModel skuModel;
    private CommonModel newSkuModel;
    private CommonModel baseModel;

    @Before
    public void setUp() throws Exception {
        newGuruModel = CommonModelBuilder.newBuilder(0, 1, 1)
            .published(false)
            .currentType(CommonModel.Source.GURU)
            .getModel();
        guruModel = CommonModelBuilder.newBuilder(idGenerator.getId(), 1, 1)
            .currentType(CommonModel.Source.GURU)
            .published(false)
            .getModel();
        newClusterModel = CommonModelBuilder.newBuilder(0, 1, 1)
            .published(false)
            .currentType(CommonModel.Source.CLUSTER)
            .getModel();
        clusterModel = CommonModelBuilder.newBuilder(generatedIdGenerator.getId(), 1, 1)
            .currentType(CommonModel.Source.CLUSTER)
            .published(false)
            .getModel();

        long baseModelId = idGenerator.getId();

        skuModel = CommonModelBuilder.newBuilder(generatedIdGenerator.getId(), 100, 1)
            .currentType(CommonModel.Source.SKU)
            .published(false)
            .startModelRelation()
                .id(baseModelId).categoryId(1).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();
        newSkuModel = CommonModelBuilder.newBuilder(0, 100, 1)
            .currentType(CommonModel.Source.SKU)
            .published(false)
            .startModelRelation()
                .id(baseModelId).categoryId(1).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();
        baseModel = CommonModelBuilder.newBuilder(baseModelId, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .published(false)
            .startModelRelation()
                .id(skuModel.getId()).categoryId(skuModel.getCategoryId()).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .getModel();
        putToStorage(baseModel, skuModel, guruModel, clusterModel);
    }

    // guru

    @Test
    public void testGuruWillCreatedWithSmallId() {
        GroupOperationStatus status = storage.saveModel(newGuruModel, context);

        Long modelId = status.getRequestedModelStatuses().get(0).getModelId();
        Assert.assertTrue("Model id: " + modelId, modelId < ModelStoreInterface.GENERATED_ID_MIN_VALUE);
    }

    @Test
    public void testPublishedGuruWillCreatedWithSmallId() {
        newGuruModel.setPublished(true);
        GroupOperationStatus status = storage.saveModel(newGuruModel, context);

        Long modelId = status.getRequestedModelStatuses().get(0).getModelId();
        Assert.assertTrue("Model id: " + modelId, modelId < ModelStoreInterface.GENERATED_ID_MIN_VALUE);
    }

    @Test
    public void testGuruIdWontChangeWhenModelBecomesPublished() throws Exception {
        Long modelId = guruModel.getId();
        guruModel.setPublished(true);

        ModelSaveGroup group = ModelSaveGroup.fromModels(guruModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        assertIdDontChange(operationStatus, modelId);
    }

    @Test
    public void testGuruModelIdWontChangeWhenModelBecomeUnpublishedAfterPublished() {
        guruModel.setPublished(true);
        putToStorage(guruModel);

        Long modelId = guruModel.getId();
        guruModel.setPublished(false);

        ModelSaveGroup group = ModelSaveGroup.fromModels(guruModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        assertIdDontChange(operationStatus, modelId);
    }

    @Test
    public void testIdWontChangeIfGuruDeleted() {
        Long modelId = guruModel.getId();
        guruModel.setDeleted(true);
        putToStorage(guruModel);

        ModelSaveGroup group = ModelSaveGroup.fromModels(guruModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        assertIdDontChange(operationStatus, modelId);
    }

    // cluster

    @Test
    public void testClusterWillCreatedWithBigId() {
        GroupOperationStatus status = storage.saveModel(newClusterModel, context);

        Long modelId = status.getRequestedModelStatuses().get(0).getModelId();
        Assert.assertTrue(modelId > ModelStoreInterface.GENERATED_ID_MIN_VALUE);
    }

    @Test
    public void testPublishedClusterWillCreatedWithSmallId() {
        newClusterModel.setPublished(true);
        GroupOperationStatus status = storage.saveModel(newClusterModel, context);

        Long modelId = status.getRequestedModelStatuses().get(0).getModelId();
        Assert.assertTrue("Model id: " + modelId, modelId < ModelStoreInterface.GENERATED_ID_MIN_VALUE);
    }

    @Test
    public void testClusterIdWillChangeWhenModelBecomesPublished() throws Exception {
        Long modelId = clusterModel.getId();
        clusterModel.setPublished(true);

        ModelSaveGroup group = ModelSaveGroup.fromModels(clusterModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        assertIdChange(operationStatus, modelId);
    }

    @Test
    public void testClusterIdWontChangeWhenModelBecomeUnpublishedAfterPublished() {
        Long modelId = idGenerator.getId();
        clusterModel.setId(modelId);
        clusterModel.setPublished(true);
        putToStorage(clusterModel);

        clusterModel.setPublished(false);

        ModelSaveGroup group = ModelSaveGroup.fromModels(clusterModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        Assert.assertEquals(modelId, operationStatus.getModelId());
        Assert.assertNull(operationStatus.getNewModelId());
        Assert.assertEquals(operationStatus.getModelId(), new Long(operationStatus.getModel().getId()));

        assertTransitionsCount(0);
    }

    @Test
    public void testIdWontChangeIfPublishedClusterBecomeDeleted() {
        Long modelId = idGenerator.getId();
        clusterModel.setId(modelId);
        clusterModel.setPublished(true);
        putToStorage(clusterModel);

        clusterModel.setDeleted(true);

        ModelSaveGroup group = ModelSaveGroup.fromModels(clusterModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        assertIdDontChange(operationStatus, modelId);
    }

    @Test
    public void testIdWontChangeIfNonPublishedClusterBecomeDeleted() {
        Long modelId = idGenerator.getId();
        clusterModel.setId(modelId);
        clusterModel.setPublished(false);
        putToStorage(clusterModel);

        clusterModel.setDeleted(true);

        ModelSaveGroup group = ModelSaveGroup.fromModels(clusterModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        assertIdDontChange(operationStatus, modelId);
    }

    // sku

    @Test
    public void testSkuWillCreatedWithBigId() {
        GroupOperationStatus status = storage.saveModel(newSkuModel, context);

        Long modelId = status.getRequestedModelStatuses().get(0).getModelId();
        Assert.assertTrue("Model id: " + modelId, modelId > ModelStoreInterface.GENERATED_ID_MIN_VALUE);
    }

    @Test
    public void testSkuIdWontChangeWhenModelBecomesPublished() throws Exception {
        Long modelId = skuModel.getId();
        skuModel.setPublished(true);

        ModelSaveGroup group = ModelSaveGroup.fromModels(skuModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        assertIdDontChange(operationStatus, modelId);
    }

    @Test
    public void testSkuModelIdWontChangeWhenModelBecomeUnpublishedAfterPublished() {
        skuModel.setPublished(true);
        putToStorage(skuModel);

        Long modelId = skuModel.getId();
        skuModel.setPublished(false);

        ModelSaveGroup group = ModelSaveGroup.fromModels(skuModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        assertIdDontChange(operationStatus, modelId);
    }

    @Test
    public void testSkuIdWontChangeIfSkuDeleted() {
        Long modelId = skuModel.getId();
        skuModel.setDeleted(true);
        putToStorage(skuModel);

        ModelSaveGroup group = ModelSaveGroup.fromModels(skuModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        OperationStatus operationStatus = status.getRequestedModelStatuses().get(0);
        assertIdDontChange(operationStatus, modelId);
    }

    private void assertIdChange(OperationStatus actualStatus, Long beforeId) {
        Assert.assertEquals(beforeId, actualStatus.getModelId());
        Assert.assertNotEquals(beforeId, actualStatus.getNewModelId());
        Assert.assertEquals(actualStatus.getNewModelId(), new Long(actualStatus.getModel().getId()));
        assertTransitionsCount(1);
    }

    private void assertIdDontChange(OperationStatus actualStatus, Long modelId) {
        Assert.assertEquals(modelId, actualStatus.getModelId());
        Assert.assertNull(actualStatus.getNewModelId());
        Assert.assertEquals(actualStatus.getModelId(), new Long(actualStatus.getModel().getId()));
        assertTransitionsCount(0);
    }
}
