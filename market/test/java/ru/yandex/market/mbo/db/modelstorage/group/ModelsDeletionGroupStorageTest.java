package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Тестируем удаление моделей и модификаций.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.Silent.class)
public class ModelsDeletionGroupStorageTest extends BaseGroupStorageUpdatesTest {
    private CommonModel parent;
    private CommonModel child1;
    private CommonModel child2;
    private CommonModel otherModel;

    @Before
    public void before() {
        super.before();

        parent = createGuruModel(1);
        child1 = createGuruModel(2, b -> b.parentModelId(1));
        child2 = createGuruModel(3, b -> b.parentModelId(1));
        otherModel = createGuruModel(4);

        super.putToStorage(parent, child1, child2, otherModel);
    }

    @Test
    public void testDeleteModificationDoesntAffectOthers() {
        child2.setDeleted(true);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(child2);
        GroupOperationStatus statuses = storage.saveModels(saveGroup, context);

        assertEquals(OperationStatusType.OK, statuses.getStatus());
        assertEquals(1, statuses.getAllModelStatuses().size());
        assertFalse(searchById(parent.getId()).isDeleted());
        assertFalse(searchById(child1.getId()).isDeleted());
        assertFalse(searchById(otherModel.getId()).isDeleted());
    }

    @Test
    public void testDeleteModelsAlsoDeletesModifications() {
        parent.setDeleted(true);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parent);
        GroupOperationStatus statuses = storage.saveModels(saveGroup, context);

        assertEquals(OperationStatusType.OK, statuses.getStatus());

        assertEquals(3, statuses.getAllModelStatuses().size());
        assertTrue(searchById(parent.getId()).isDeleted());
        assertTrue(searchById(child1.getId()).isDeleted());
        assertTrue(searchById(child2.getId()).isDeleted());
        assertFalse(searchById(otherModel.getId()).isDeleted());
    }

    @Test
    public void testDeleteSimpleModel() {
        otherModel.setDeleted(true);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(otherModel);
        GroupOperationStatus statuses = storage.saveModels(saveGroup, context);

        assertEquals(OperationStatusType.OK, statuses.getStatus());
        assertEquals(1, statuses.getAllModelStatuses().size());
        assertTrue(searchById(otherModel.getId()).isDeleted());
    }

    @Test
    public void testOnlyLiveModificationsAreDeleted() {
        child1.setDeleted(true);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(child1);
        GroupOperationStatus statuses = storage.saveModels(saveGroup, context);

        assertEquals(OperationStatusType.OK, statuses.getStatus());
        assertEquals(1, statuses.getAllModelStatuses().size());
        assertTrue(searchById(child1.getId()).isDeleted());


        parent.setDeleted(true);
        saveGroup = ModelSaveGroup.fromModels(parent);
        statuses = storage.saveModels(saveGroup, context);

        assertEquals(OperationStatusType.OK, statuses.getStatus());

        assertEquals(2, statuses.getAllModelStatuses().size());
        assertTrue(searchById(parent.getId()).isDeleted());
        assertTrue(searchById(child2.getId()).isDeleted());
    }
}
