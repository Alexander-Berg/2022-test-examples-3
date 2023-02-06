package ru.yandex.market.mbo.db.modelstorage.group;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Тестируем автоматическое сохранение связанных моделей.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class ModelRelationsGroupStorageTest extends BaseGroupStorageUpdatesTest {

    @Test
    public void testLoadRelatedParentModel() throws Exception {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, 1, 1)
                .startModelRelation()
                    .id(2).categoryId(2).type(ModelRelation.RelationType.SKU_MODEL)
                .endModelRelation()
                .startModelRelation()
                    .id(3).categoryId(3).type(ModelRelation.RelationType.SYNC_SOURCE)
                .endModelRelation()
            .getModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(2, 2, 2)
                .startModelRelation()
                    .id(1).categoryId(1).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .endModelRelation()
            .getModel();
        CommonModel model3 = CommonModelBuilder.newBuilder(3, 3, 3)
                .startModelRelation()
                    .id(1).categoryId(1).type(ModelRelation.RelationType.SYNC_TARGET)
                .endModelRelation()
            .getModel();
        putToStorage(model1, model2, model3);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model2, model3);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        assertThat(groupOperationStatus.getRequestedModelStatuses()).hasSize(2);
        assertThat(groupOperationStatus.getAdditionalModelStatues()).isEmpty();
        CommonModel baseModelForSku = saveGroup.getAdditionalModels().get(0);
        assertRelations(baseModelForSku,
            2, 2, ModelRelation.RelationType.SKU_MODEL,
            3, 3, ModelRelation.RelationType.SYNC_SOURCE);
    }

    @Test
    public void testLoadRelatedParentModel2() throws Exception {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, 1, 1)
                .startModelRelation()
                    .id(3).categoryId(3).type(ModelRelation.RelationType.SKU_MODEL)
                .endModelRelation()
            .getModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(2, 2, 2)
                .startModelRelation()
                    .id(4).categoryId(4).type(ModelRelation.RelationType.EXPERIMENTAL_MODEL)
                .endModelRelation()
            .getModel();
        CommonModel model3 = CommonModelBuilder.newBuilder(3, 3, 3)
                .startModelRelation()
                    .id(1).categoryId(1).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .endModelRelation()
            .getModel();
        CommonModel model4 = CommonModelBuilder.newBuilder(4, 4, 4)
                .startModelRelation()
                    .id(2).categoryId(2).type(ModelRelation.RelationType.EXPERIMENTAL_BASE_MODEL)
                .endModelRelation()
            .getModel();
        putToStorage(model1, model2, model3, model4);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model3, model4);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        assertThat(groupOperationStatus.getRequestedModelStatuses()).hasSize(2);
        assertThat(groupOperationStatus.getAdditionalModelStatues()).isEmpty();
        CommonModel baseModelForSku = saveGroup.getAdditionalModels().get(0);
        assertRelations(baseModelForSku, 3, 3, ModelRelation.RelationType.SKU_MODEL);
    }

    @Test
    public void testDeleteModelWouldAlsoDeleteRelationsFromOtherModelsToThem() throws Exception {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, 1, 1)
                .startModelRelation()
                    .id(3).categoryId(3).type(ModelRelation.RelationType.SYNC_TARGET)
                .endModelRelation()
            .getModel();

        CommonModel model2 = CommonModelBuilder.newBuilder(2, 2, 2)
                .startModelRelation()
                    .id(3).categoryId(3).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .endModelRelation()
            .getModel();

        CommonModel model5 = CommonModelBuilder.newBuilder(5, 3, 3)
            .getModel();

        CommonModel model3 = CommonModelBuilder.newBuilder(3, 3, 3)
                .parentModelId(5)
                .startModelRelation()
                    .id(1).categoryId(1).type(ModelRelation.RelationType.SYNC_SOURCE)
                .endModelRelation()
                .startModelRelation()
                    .id(2).categoryId(2).type(ModelRelation.RelationType.SKU_MODEL)
                .endModelRelation()
                .startModelRelation()
                    .id(4).categoryId(4).type(ModelRelation.RelationType.SYNC_SOURCE)
                .endModelRelation()
            .getModel();
        putToStorage(model1, model2, model3, model5);

        model1.setDeleted(true);
        model2.setDeleted(true);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1, model2);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model3New = groupOperationStatus.getAdditionalModelStatues().get(0).getModel();
        assertRelations(model3New, 4, 4, ModelRelation.RelationType.SYNC_SOURCE);
        assertThat(model3New)
            .extracting(CommonModel::isDeleted)
            .isEqualTo(false);
    }

    @Test
    public void testRelatedModelsDeletedInGroup() throws Exception {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.VENDOR)
            .startModelRelation()
            .id(2).categoryId(2).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .getModel();

        CommonModel model2 = CommonModelBuilder.newBuilder(2, 2, 2)
            .currentType(CommonModel.Source.GENERATED_SKU)
            .startModelRelation()
            .id(1).categoryId(1).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();

        putToStorage(model1, model2);

        model1.setDeleted(true);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1, model2);

        storage.saveModels(saveGroup, context);

        CommonModel model2Updated = storage.getModel(2, 2, context.getStats().getReadStats()).get();
        assertThat(model2Updated)
            .extracting(CommonModel::isDeleted)
            .isEqualTo(true);
        assertThat(model2Updated)
            .extracting(CommonModel::getDeletedDate)
            .isNotNull();
    }

    @Test
    public void testRelatedModelsDeletedOutOfGroup() throws Exception {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.VENDOR)
            .startModelRelation()
            .id(2).categoryId(2).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .getModel();

        CommonModel model2 = CommonModelBuilder.newBuilder(2, 2, 2)
            .currentType(CommonModel.Source.GENERATED_SKU)
            .startModelRelation()
            .id(1).categoryId(1).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();

        putToStorage(model1, model2);

        model1.setDeleted(true);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);

        storage.saveModels(saveGroup, context);

        CommonModel model2Updated = storage.getModel(2, 2, context.getStats().getReadStats()).get();
        assertThat(model2Updated)
            .extracting(CommonModel::isDeleted)
            .isEqualTo(true);
        assertThat(model2Updated)
            .extracting(CommonModel::getDeletedDate)
            .isNotNull();
    }

    @Test
    public void testRelatedModelsNotDeletedBecauesOfType() throws Exception {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .startModelRelation()
            .id(2).categoryId(2).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .getModel();

        CommonModel model2 = CommonModelBuilder.newBuilder(2, 2, 2)
            .currentType(CommonModel.Source.SKU)
            .startModelRelation()
            .id(1).categoryId(1).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();

        putToStorage(model1, model2);

        model1.setDeleted(true);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);

        storage.saveModels(saveGroup, context);

        CommonModel model2Updated = storage.getModel(2, 2, context.getStats().getReadStats()).get();
        assertThat(model2Updated)
            .extracting(CommonModel::isDeleted)
            .isEqualTo(false);
        assertThat(model2Updated)
            .extracting(CommonModel::getDeletedDate)
            .isNull();
    }

    @Test
    public void testDeleteWithRelationToNonexistentModel() {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, 1, 1)
            .startModelRelation()
                .id(101).categoryId(1).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .startModelRelation()
                .id(101).categoryId(1).type(ModelRelation.RelationType.SYNC_SOURCE)
            .endModelRelation()
            .getModel();
        putToStorage(model1);

        model1.setDeleted(true);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

       assertEquals(OperationStatusType.OK, groupOperationStatus.getStatus());
    }
}
