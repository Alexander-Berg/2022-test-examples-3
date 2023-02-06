package ru.yandex.market.mbo.db.modelstorage.group;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author york
 * @since 04.04.2018
 */
public class ParentsLoadingTest extends BaseGroupStorageUpdatesTest {

    private Map<Long, ParentInfo> parentsInfos;
    private Map<Long, ParentInfo> beforeParentsInfos;
    private static final long CATEGORY_HID = 1000L;

    @Before
    public void init() {
        parentsInfos = new HashMap<>();
        beforeParentsInfos = new HashMap<>();
    }

    @Override
    protected ModelValidationService createModelValidationService() {
        ModelValidationService service = super.createModelValidationService(
            Collections.singletonList(new ModelValidator() {
            @Override
            public List<CommonModel.Source> getSupportedModelTypes() {
                return Arrays.asList(CommonModel.Source.values());
            }

            @Override
            public List<ModelChanges.Operation> getSupportedOperations() {
                return Arrays.asList(ModelChanges.Operation.values());
            }

            @Nonnull
            @Override
            public ChangeHandlingMode getChangeHandlingMode() {
                return ChangeHandlingMode.VALIDATE_ALWAYS;
            }

            @Override
            public List<ModelValidationError> validate(ModelValidationContext context,
                                                       ModelChanges modelChanges,
                                                       Collection<CommonModel> updatedModels) {
                saveParentInfo(modelChanges.getAfter(), parentsInfos);
                saveParentInfo(modelChanges.getBefore(), beforeParentsInfos);
                return Collections.emptyList();
            }
        }));
        return service;
    }

    private void saveParentInfo(CommonModel m, Map<Long, ParentInfo> parentsInfo) {
        CommonModel parent = m.getParentModel();
        ParentInfo info = parent == null ? null :
            new ParentInfo(parent.getId(), parent.getModificationDate().getTime());
        parentsInfo.put(m.getId(), info);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testParentsLoading() {
        int i = 1;
        CommonModel model = createGuruModel(i++, CATEGORY_HID, 1, b -> { });
        model.setModificationDate(new Date(10L));
        CommonModel model1 = createGuruModel(i++, CATEGORY_HID, 1, b -> { });
        model1.setModificationDate(new Date(15L));

        CommonModel modification = createGuruModel(i++, CATEGORY_HID, 1, b -> { });
        modification.setParentModelId(model.getId());
        CommonModel modification1 = createGuruModel(i++, CATEGORY_HID, 1, b -> { });
        modification1.setParentModelId(model1.getId());

        putToStorage(model, model1, modification, modification1);

        model = new CommonModel(model); //copy
        model.setModificationDate(new Date(50L));

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(model, modification, modification1);
        GroupOperationStatus groupOperationStatus = storage.saveModels(modelSaveGroup, context);

        Assert.assertFalse(groupOperationStatus.isFailure());
        Assert.assertNull(parentsInfos.get(model.getId()));
        Assert.assertNull(parentsInfos.get(model1.getId()));

        ParentInfo forModel = parentsInfos.get(modification.getId());
        Assert.assertNotNull(forModel);
        Assert.assertEquals(model.getId(), forModel.id);
        Assert.assertEquals(50, forModel.timestamp); //taken from saving

        ParentInfo forModel1 = parentsInfos.get(modification1.getId());
        Assert.assertNotNull(forModel1);
        Assert.assertEquals(model1.getId(), forModel1.id);
        Assert.assertEquals(15, forModel1.timestamp);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testBeforeParentsLoading() {
        int i = 1;
        CommonModel model = createGuruModel(i++, CATEGORY_HID, 1, b -> { });
        model.setModificationDate(new Date(10L));

        CommonModel modification = createGuruModel(i++, CATEGORY_HID, 1, b -> { });
        modification.setParentModelId(model.getId());

        putToStorage(model, modification);

        CommonModel modelCopy = new CommonModel(model); //copy
        modelCopy.setModificationDate(new Date(50L));
        CommonModel modificationCopy = new CommonModel(modification); // copy

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(modelCopy, modificationCopy);
        // set before modification without parent
        modelSaveGroup.addBeforeModels(Lists.newArrayList(model, modification));

        GroupOperationStatus groupOperationStatus = storage.saveModels(modelSaveGroup, context);

        Assert.assertFalse(groupOperationStatus.isFailure());
        Assert.assertNull(beforeParentsInfos.get(model.getId()));

        ParentInfo beforeModel = beforeParentsInfos.get(modification.getId());
        Assert.assertNotNull(beforeModel);
        Assert.assertEquals(model.getId(), beforeModel.id);
        Assert.assertEquals(10, beforeModel.timestamp); // before saving
    }

    private class ParentInfo {
        final long id;
        final long timestamp;

        ParentInfo(long id, long timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }
    }
}
