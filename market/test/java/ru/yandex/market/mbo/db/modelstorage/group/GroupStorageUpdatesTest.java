package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тесты, которые проверяют корректность подшагов.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class GroupStorageUpdatesTest extends BaseGroupStorageUpdatesTest {
    private CommonModel newModel;
    private CommonModel updatedModel;
    private CommonModel deletedModel;
    private CommonModel modelWithModifications;
    private CommonModel modification;
    private CommonModel baseModel;
    private CommonModel skuModel;
    private CommonModel newSkuModel;

    @Before
    public void setUp() throws Exception {
        newModel = CommonModelBuilder.newBuilder(0, 1, 1)
            .getModel();
        updatedModel = CommonModelBuilder.newBuilder(idGenerator.getId(), 1, 1)
            .published(false)
            .getModel();
        deletedModel = CommonModelBuilder.newBuilder(idGenerator.getId(), 1, 1)
            .published(false)
            .getModel();
        putToStorage(updatedModel, deletedModel);
        deletedModel.setDeleted(true);

        modelWithModifications = CommonModelBuilder.newBuilder(idGenerator.getId(), 1, 1)
            .published(false)
            .getModel();
        modification = CommonModelBuilder.newBuilder(idGenerator.getId(), 1, 1)
            .published(false)
            .parentModelId(modelWithModifications.getId()).getModel();
        putToStorage(modelWithModifications, modification);

        long baseModelId = idGenerator.getId();
        long baseCategoryId = 1;

        skuModel = CommonModelBuilder.newBuilder(generatedIdGenerator.getId(), 300, 1)
            .currentType(CommonModel.Source.SKU)
            .published(false)
            .startModelRelation()
                .id(baseModelId).categoryId(baseCategoryId).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();
        newSkuModel = CommonModelBuilder.newBuilder(0, 100, 1)
            .currentType(CommonModel.Source.SKU)
            .published(false)
            .startModelRelation()
                .id(baseModelId).categoryId(baseCategoryId).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();
        baseModel = CommonModelBuilder.newBuilder(baseModelId, baseCategoryId, 1)
            .published(false)
            .startModelRelation()
                .id(skuModel.getId()).categoryId(skuModel.getCategoryId()).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .getModel();
        putToStorage(baseModel, skuModel);
    }

    @Test
    public void testSimpleModelsSave() throws Exception {
        ModelSaveGroup group = ModelSaveGroup.fromModels(newModel, updatedModel, deletedModel,
            modelWithModifications, modification);

        GroupOperationStatus groupOperationStatus = storage.saveModels(group, context);
        Assert.assertEquals(OperationStatusType.OK, groupOperationStatus.getStatus());

        verify(modelAuditContextProvider, times(1)).createContext();
    }

    @Test
    public void testCreateNewRelationModel() throws Exception {
        ModelSaveGroup group = ModelSaveGroup.fromModels(newSkuModel, baseModel, skuModel);
        GroupOperationStatus status = storage.saveModels(group, context);

        CommonModel newSku = status.getRequestedModelStatuses().get(0).getModel();
        CommonModel model = status.getRequestedModelStatuses().get(1).getModel();
        Assert.assertFalse(newSku.isNewModel());
        assertRelations(newSku, baseModel.getId(), baseModel.getCategoryId(),
            ModelRelation.RelationType.SKU_PARENT_MODEL);
        assertRelations(model,
            skuModel.getId(), skuModel.getCategoryId(), ModelRelation.RelationType.SKU_MODEL,
            newSku.getId(), newSku.getCategoryId(), ModelRelation.RelationType.SKU_MODEL);
    }

    @Test
    public void saveUnknownModelShouldFail() throws Exception {
        CommonModel modelToUpdate = new CommonModel(updatedModel);
        modelToUpdate.setId(365L);

        ModelSaveGroup group = ModelSaveGroup.fromModels(modelToUpdate);
        GroupOperationStatus groupOperationStatus = storage.saveModels(group, context);

        Assert.assertEquals(OperationStatusType.MODEL_NOT_FOUND, groupOperationStatus.getStatus());
        Assert.assertEquals(Collections.singletonList(365L), groupOperationStatus.getFailedModelIds());
    }

    @Test
    public void saveAlreadyModifiedModelShouldFail() throws Exception {
        long id = updatedModel.getId();
        CommonModel modelToUpdate = new CommonModel(updatedModel);

        Date date = new Date();
        date.setTime(date.getTime() + 1000);
        updatedModel.setModificationDate(date);
        putToStorage(updatedModel);

        ModelSaveGroup group = ModelSaveGroup.fromModels(modelToUpdate);
        GroupOperationStatus groupOperationStatus = storage.saveModels(group, context);

        Assert.assertEquals(OperationStatusType.MODEL_MODIFIED, groupOperationStatus.getStatus());
        Assert.assertEquals(Collections.singletonList(id), groupOperationStatus.getFailedModelIds());
        Assert.assertEquals(1, groupOperationStatus.getRequestedModelStatuses().size());
        Assert.assertEquals(updatedModel.getModificationDate(),
            groupOperationStatus.getRequestedModelStatuses().get(0).getModel().getModificationDate());
    }
}
