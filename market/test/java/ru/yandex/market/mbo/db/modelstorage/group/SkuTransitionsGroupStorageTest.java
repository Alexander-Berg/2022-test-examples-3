package ru.yandex.market.mbo.db.modelstorage.group;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionReason;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Тестируем сохранение переездов SKU.
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class SkuTransitionsGroupStorageTest extends BaseGroupStorageUpdatesTest {
    private CommonModel baseModel;
    private CommonModel skuModel;
    private ModelTransition fromSkuTransition;
    private ModelTransition wrongSkuTransition;

    @Before
    public void before() {
        super.before();

        baseModel = CommonModelBuilder.newBuilder(1L, 300L, 1)
            .published(false)
            .startModelRelation()
            .id(2L).categoryId(300L).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .getModel();

        skuModel = CommonModelBuilder.newBuilder(2L, 300L, 1)
            .currentType(CommonModel.Source.SKU)
            .published(false)
            .startModelRelation()
            .id(1L).categoryId(300L).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();

        fromSkuTransition = new ModelTransition()
            .setOldEntityId(2L)
            .setNewEntityId(3L);

        wrongSkuTransition = new ModelTransition()
            .setOldEntityId(4L)
            .setNewEntityId(3L);

        super.putToStorage(baseModel, skuModel);
    }

    @Test
    public void testDeleteSkuWithTransitions() {
        skuModel.setDeleted(true);
        ModelSaveGroup saveGroup = ModelSaveGroup.from(
            Collections.singletonList(skuModel), Collections.singletonList(fromSkuTransition));
        GroupOperationStatus statuses = storage.saveModels(saveGroup, context);

        assertEquals(OperationStatusType.OK, statuses.getStatus());
        assertEquals(2, statuses.getAllModelStatuses().size());
        assertTrue(storage.searchById(skuModel.getId(), new ReadStats()).isDeleted());

        assertEquals(1, modelTransitionRepositoryStub.getTransitionsCount());
        ModelTransition savedTransition = modelTransitionRepositoryStub.getTransitions().get(0);
        assertEquals(2L, savedTransition.getOldEntityId().longValue());
        assertEquals(3L, savedTransition.getNewEntityId().longValue());
    }

    @Test
    public void testDeleteSkuWithWrongTransitions() {
        skuModel.setDeleted(true);
        ModelSaveGroup saveGroup = ModelSaveGroup.from(
            Collections.singletonList(skuModel), Collections.singletonList(wrongSkuTransition));
        GroupOperationStatus statuses = storage.saveModels(saveGroup, context);

        assertEquals(OperationStatusType.MODEL_NOT_FOUND, statuses.getStatus());
        assertEquals(2, statuses.getAllModelStatuses().size());
        assertFalse(storage.searchById(skuModel.getId(), new ReadStats()).isDeleted());

        assertEquals(0, modelTransitionRepositoryStub.getTransitionsCount());
    }

    @Test
    public void testRemoveSku() {
        CommonModel skuModel1 = CommonModelBuilder.newBuilder(3L, 300L, 1)
            .currentType(CommonModel.Source.SKU)
            .published(false)
            .startModelRelation()
            .id(1L).categoryId(300L).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .getModel();
        putToStorage(skuModel1);

        ModelTransition transition = new ModelTransition()
            .setEntityType(EntityType.SKU)
            .setType(ModelTransitionType.DUPLICATE)
            .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
            .setOldEntityDeleted(true)
            .setOldEntityId(2L)
            .setNewEntityId(3L)
            .setPrimaryTransition(true);
        List<ModelTransition> transitions = Collections.singletonList(transition);
        GroupOperationStatus statuses = storage.deleteModel(skuModel, context, transitions);

        assertEquals(OperationStatusType.OK, statuses.getStatus());
        assertEquals(2, statuses.getAllModelStatuses().size());
        assertTrue(storage.searchById(skuModel.getId(), new ReadStats()).isDeleted());

        assertEquals(1, modelTransitionRepositoryStub.getTransitionsCount());
        ModelTransition savedTransition = modelTransitionRepositoryStub.getTransitions().get(0);
        Assertions.assertThat(savedTransition)
            .isEqualToIgnoringGivenFields(transition, "id", "actionId", "date");
    }
}
