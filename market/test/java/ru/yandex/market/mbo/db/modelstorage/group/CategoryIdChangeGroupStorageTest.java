package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.PicturesModelValidator;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author dmserebr
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class CategoryIdChangeGroupStorageTest extends BaseGroupStorageUpdatesTest {
    private static final long CATEGORY1 = 1L;
    private static final long CATEGORY2 = 2L;
    private static final long VENDOR1 = 1L;
    private static final long VENDOR2 = 2L;
    private static final long MODEL1 = 1L;
    private static final long MODEL2 = 2L;
    private static final long MODEL3 = 3L;
    private static final String NAMESPACE = "mpic-fake";
    private static final String AVATARS_HOST1 = "avatars.mdst-fake.yandex.net";

    @Override
    protected ModelValidationService createModelValidationService() {
        ModelValidationService baseService = super.createModelValidationService(
            Collections.singletonList(new PicturesModelValidator(
            Collections.singletonList(AVATARS_HOST1), NAMESPACE)));
        return baseService;
    }

    @Test
    public void testChangeCategoryIdSimpleModel() {
        CommonModel model1 = CommonModelBuilder.newBuilder(MODEL1, CATEGORY1, VENDOR1).getModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(MODEL2, CATEGORY1, VENDOR2).getModel();
        putToStorage(model1, model2);
        Date timestampBeforeUpdate = new Date();

        CommonModel model1changed = new CommonModel(model1);
        model1changed.setCategoryId(CATEGORY2);
        CommonModel model2changed = new CommonModel(model2);
        model2changed.setCategoryId(CATEGORY2);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1changed, model2changed);
        saveGroup.addBeforeModels(Arrays.asList(model1, model2));
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model1saved = groupOperationStatus.getRequestedModelStatuses().get(0).getModel();
        CommonModel model2saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        assertEquals(CATEGORY2, model1saved.getCategoryId());
        assertEquals(CATEGORY2, model2saved.getCategoryId());

        // assert modification date was set
        assertFalse(model1saved.getModificationDate().before(timestampBeforeUpdate));
        assertFalse(model2saved.getModificationDate().before(timestampBeforeUpdate));
    }

    @Test
    public void testChangeCategoryIdModelWithSku() {
        CommonModel model1 = CommonModelBuilder.newBuilder(MODEL1, CATEGORY1, VENDOR1)
            .startModelRelation()
            .id(MODEL2).categoryId(1).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .getModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(MODEL2, CATEGORY1, VENDOR2)
            .startModelRelation()
            .id(MODEL1).categoryId(CATEGORY1).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();
        putToStorage(model1, sku1);

        CommonModel model1changed = new CommonModel(model1);
        model1changed.setCategoryId(CATEGORY2);
        CommonModel sku1changed = new CommonModel(sku1);
        sku1changed.setCategoryId(CATEGORY2);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1changed, sku1changed);
        saveGroup.addBeforeModels(Arrays.asList(model1, sku1));
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model1saved = groupOperationStatus.getRequestedModelStatuses().get(0).getModel();
        CommonModel model2saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        assertEquals(CATEGORY2, model1saved.getCategoryId());
        assertEquals(CATEGORY2, model2saved.getCategoryId());
        assertRelations(model1saved, MODEL2, CATEGORY2, ModelRelation.RelationType.SKU_MODEL);
        assertRelations(model2saved, MODEL1, CATEGORY2, ModelRelation.RelationType.SKU_PARENT_MODEL);
    }

    @Test
    public void testChangeCategoryIdModelWithExperimental() {
        CommonModel model1 = CommonModelBuilder.newBuilder(MODEL1, CATEGORY1, VENDOR1)
            .startModelRelation()
            .id(MODEL2).categoryId(CATEGORY1).type(ModelRelation.RelationType.EXPERIMENTAL_MODEL)
            .endModelRelation()
            .startModelRelation()
            .id(MODEL3).categoryId(CATEGORY1).type(ModelRelation.RelationType.EXPERIMENTAL_MODEL)
            .endModelRelation()
            .getModel();
        CommonModel experimental1 = CommonModelBuilder.newBuilder(MODEL2, CATEGORY1, VENDOR2)
            .startModelRelation()
            .id(MODEL1).categoryId(CATEGORY1).type(ModelRelation.RelationType.EXPERIMENTAL_BASE_MODEL)
            .endModelRelation()
            .getModel();
        CommonModel experimental2 = CommonModelBuilder.newBuilder(MODEL3, CATEGORY1, VENDOR2)
            .startModelRelation()
            .id(MODEL1).categoryId(CATEGORY1).type(ModelRelation.RelationType.EXPERIMENTAL_BASE_MODEL)
            .endModelRelation()
            .getModel();
        putToStorage(model1, experimental1, experimental2);

        CommonModel model1changed = new CommonModel(model1);
        model1changed.setCategoryId(CATEGORY2);
        CommonModel experimental1changed = new CommonModel(experimental1);
        experimental1changed.setCategoryId(CATEGORY2);
        CommonModel experimental2changed = new CommonModel(experimental2);
        experimental2changed.setCategoryId(CATEGORY2);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1changed, experimental1changed, experimental2changed);
        saveGroup.addBeforeModels(Arrays.asList(model1, experimental1, experimental2));
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model1saved = groupOperationStatus.getRequestedModelStatuses().get(0).getModel();
        CommonModel experimental1saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        CommonModel experimental2saved = groupOperationStatus.getRequestedModelStatuses().get(2).getModel();
        assertTrue(experimental1saved.isDeleted());
        assertTrue(experimental2saved.isDeleted());
        assertEquals(CATEGORY2, model1saved.getCategoryId());
        assertEquals(CATEGORY2, experimental1saved.getCategoryId());
        assertEquals(CATEGORY2, experimental2saved.getCategoryId());
        assertEquals(0, model1saved.getRelations().size());
    }

    @Test
    public void testChangeCategoryIdModelWithModification() {
        CommonModel model1 = CommonModelBuilder.newBuilder(MODEL1, CATEGORY1, VENDOR1)
            .getModel();
        CommonModel modif1 = CommonModelBuilder.newBuilder(MODEL2, CATEGORY1, VENDOR1)
            .parentModelId(MODEL1)
            .pictureParam("XL-Picture", "http://url1", 300, 400, null, "http://url1")
            .getModel();
        CommonModel modif2 = CommonModelBuilder.newBuilder(MODEL3, CATEGORY1, VENDOR1)
            .parentModelId(MODEL1)
            .pictureParam("XL-Picture_2", "http://url2", 350, 450, null, "http://url2")
            .getModel();
        putToStorage(model1, modif1, modif2);
        Date timestampBeforeUpdate = new Date();

        CommonModel model1changed = new CommonModel(model1);
        model1changed.setCategoryId(CATEGORY2);
        CommonModel modif1changed = new CommonModel(modif1);
        modif1changed.setCategoryId(CATEGORY2);
        CommonModel modif2changed = new CommonModel(modif2);
        modif2changed.setCategoryId(CATEGORY2);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1changed, modif1changed, modif2changed);
        saveGroup.addBeforeModels(Arrays.asList(model1, modif1, modif2));
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model1saved = groupOperationStatus.getRequestedModelStatuses().get(0).getModel();
        CommonModel modif1saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        CommonModel modif2saved = groupOperationStatus.getRequestedModelStatuses().get(2).getModel();
        assertEquals(CATEGORY2, model1saved.getCategoryId());
        assertEquals(CATEGORY2, modif1saved.getCategoryId());
        assertEquals(CATEGORY2, modif2saved.getCategoryId());

        // assert modification date was updated
        assertTrue(model1saved.getModificationDate().after(timestampBeforeUpdate));
        assertTrue(modif1saved.getModificationDate().after(timestampBeforeUpdate));
        assertTrue(modif2saved.getModificationDate().after(timestampBeforeUpdate));
    }

    @Test
    public void testChangeCategoryIdSimpleModelWithoutBefore() {
        CommonModel model1 = CommonModelBuilder.newBuilder(MODEL1, CATEGORY1, VENDOR1).getModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(MODEL2, CATEGORY1, VENDOR2).getModel();
        putToStorage(model1, model2);
        Date timestampBeforeUpdate = new Date();

        CommonModel model1changed = new CommonModel(model1);
        model1changed.setCategoryId(CATEGORY2);
        CommonModel model2changed = new CommonModel(model2);
        model2changed.setCategoryId(CATEGORY2);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1changed, model2changed);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model1saved = groupOperationStatus.getRequestedModelStatuses().get(0).getModel();
        CommonModel model2saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        assertEquals(CATEGORY2, model1saved.getCategoryId());
        assertEquals(CATEGORY2, model2saved.getCategoryId());

        // assert modification date was set
        assertFalse(model1saved.getModificationDate().before(timestampBeforeUpdate));
        assertFalse(model2saved.getModificationDate().before(timestampBeforeUpdate));
    }

    @Test
    public void testChangeCategoryIdModelWithSkuWitoutBefore() {
        CommonModel model1 = CommonModelBuilder.newBuilder(MODEL1, CATEGORY1, VENDOR1)
            .startModelRelation()
            .id(MODEL2).categoryId(CATEGORY1).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .getModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(MODEL2, CATEGORY1, VENDOR2)
            .startModelRelation()
            .id(MODEL1).categoryId(CATEGORY1).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();
        putToStorage(model1, sku1);

        CommonModel model1changed = new CommonModel(model1);
        model1changed.setCategoryId(CATEGORY2);
        CommonModel sku1changed = new CommonModel(sku1);
        sku1changed.setCategoryId(CATEGORY2);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1changed, sku1changed);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model1saved = groupOperationStatus.getRequestedModelStatuses().get(0).getModel();
        CommonModel model2saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        assertEquals(CATEGORY2, model1saved.getCategoryId());
        assertEquals(CATEGORY2, model2saved.getCategoryId());
        assertRelations(model1saved, MODEL2, CATEGORY2, ModelRelation.RelationType.SKU_MODEL);
        assertRelations(model2saved, MODEL1, CATEGORY2, ModelRelation.RelationType.SKU_PARENT_MODEL);
    }

    @Test
    public void testChangeCategoryIdModelWithExperimentalWithoutBefore() {
        CommonModel model1 = CommonModelBuilder.newBuilder(MODEL1, CATEGORY1, VENDOR1)
            .startModelRelation()
            .id(MODEL2).categoryId(CATEGORY1).type(ModelRelation.RelationType.EXPERIMENTAL_MODEL)
            .endModelRelation()
            .startModelRelation()
            .id(MODEL3).categoryId(CATEGORY1).type(ModelRelation.RelationType.EXPERIMENTAL_MODEL)
            .endModelRelation()
            .getModel();
        CommonModel experimental1 = CommonModelBuilder.newBuilder(MODEL2, CATEGORY1, VENDOR2)
            .startModelRelation()
            .id(MODEL1).categoryId(CATEGORY1).type(ModelRelation.RelationType.EXPERIMENTAL_BASE_MODEL)
            .endModelRelation()
            .getModel();
        CommonModel experimental2 = CommonModelBuilder.newBuilder(MODEL3, CATEGORY1, VENDOR2)
            .startModelRelation()
            .id(MODEL1).categoryId(CATEGORY1).type(ModelRelation.RelationType.EXPERIMENTAL_BASE_MODEL)
            .endModelRelation()
            .getModel();
        putToStorage(model1, experimental1, experimental2);

        CommonModel model1changed = new CommonModel(model1);
        model1changed.setCategoryId(CATEGORY2);
        CommonModel experimental1changed = new CommonModel(experimental1);
        experimental1changed.setCategoryId(CATEGORY2);
        CommonModel experimental2changed = new CommonModel(experimental2);
        experimental2changed.setCategoryId(CATEGORY2);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1changed, experimental1changed, experimental2changed);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model1saved = groupOperationStatus.getRequestedModelStatuses().get(0).getModel();
        CommonModel experimental1saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        CommonModel experimental2saved = groupOperationStatus.getRequestedModelStatuses().get(2).getModel();
        assertTrue(experimental1saved.isDeleted());
        assertTrue(experimental2saved.isDeleted());
        assertEquals(CATEGORY2, model1saved.getCategoryId());
        assertEquals(CATEGORY2, experimental1saved.getCategoryId());
        assertEquals(CATEGORY2, experimental2saved.getCategoryId());
        assertEquals(0, model1saved.getRelations().size());
    }

    @Test
    public void testChangeCategoryIdModelWithModificationWithoutBefore() {
        CommonModel model1 = CommonModelBuilder.newBuilder(MODEL1, CATEGORY1, VENDOR1)
            .getModel();
        CommonModel modif1 = CommonModelBuilder.newBuilder(MODEL2, CATEGORY1, VENDOR1)
            .parentModelId(MODEL1)
            .pictureParam("XL-Picture", "http://url1", 300, 400, null, "http://url1")
            .getModel();
        CommonModel modif2 = CommonModelBuilder.newBuilder(MODEL3, CATEGORY1, VENDOR1)
            .parentModelId(MODEL1)
            .pictureParam("XL-Picture_2", "http://url2", 350, 450, null, "http://url2")
            .getModel();
        putToStorage(model1, modif1, modif2);
        Date timestampBeforeUpdate = new Date();

        CommonModel model1changed = new CommonModel(model1);
        model1changed.setCategoryId(CATEGORY2);
        CommonModel modif1changed = new CommonModel(modif1);
        modif1changed.setCategoryId(CATEGORY2);
        CommonModel modif2changed = new CommonModel(modif2);
        modif2changed.setCategoryId(CATEGORY2);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1changed, modif1changed, modif2changed);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model1saved = groupOperationStatus.getRequestedModelStatuses().get(0).getModel();
        CommonModel modif1saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        CommonModel modif2saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        assertEquals(CATEGORY2, model1saved.getCategoryId());
        assertEquals(CATEGORY2, modif1saved.getCategoryId());
        assertEquals(CATEGORY2, modif2saved.getCategoryId());

        // assert modification date was updated
        assertTrue(model1saved.getModificationDate().after(timestampBeforeUpdate));
        assertTrue(modif1saved.getModificationDate().after(timestampBeforeUpdate));
        assertTrue(modif2saved.getModificationDate().after(timestampBeforeUpdate));
    }
}
