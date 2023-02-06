package ru.yandex.market.mbo.db.modelstorage.group;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author shadoff
 * created on 11/5/20
 */
public class ConcurrentModificationAndValidationTest extends BaseGroupStorageUpdatesTest {
    private CommonModel model;

    @Before
    public void setUp() throws Exception {
        model = CommonModelBuilder.newBuilder(idGenerator.getId(), 1, 1)
            .published(false)
            .getModel();
    }

    @Override
    protected ModelValidationService createModelValidationService() {
        return super.createModelValidationService(
            ImmutableList.of(new AlwaysFailedValidator()));
    }

    private static class AlwaysFailedValidator implements ModelValidator {
        @Override
        public List<CommonModel.Source> getSupportedModelTypes() {
            return Arrays.asList(CommonModel.Source.values());
        }

        @Override
        public List<ModelChanges.Operation> getSupportedOperations() {
            return Arrays.asList(ModelChanges.Operation.values());
        }

        @Override
        @Nonnull
        public ChangeHandlingMode getChangeHandlingMode() {
            return ChangeHandlingMode.VALIDATE_ALWAYS;
        }

        @Override
        public List<ModelValidationError> validate(ModelValidationContext context,
                                                   ModelChanges modelChanges,
                                                   Collection<CommonModel> updatedModels) {
            return Collections.singletonList(new ModelValidationError(modelChanges.getAfter().getId(),
                ModelValidationError.ErrorType.UNKNOWN_ERROR, true)
                .addLocalizedMessagePattern("Все сломалось"));
        }
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void shouldHaveConcurrentModificationBeforeValidationError() {
        long id = model.getId();
        CommonModel modelToUpdate = new CommonModel(model);
        modelToUpdate.setModificationDate(new Date(1));
        model.setModificationDate(new Date(1000));
        putToStorage(model);

        ModelSaveGroup group = ModelSaveGroup.fromModels(modelToUpdate);
        GroupOperationStatus groupOperationStatus = storage.saveModels(group, context);

        Assert.assertEquals(OperationStatusType.MODEL_MODIFIED, groupOperationStatus.getStatus());
        Assert.assertEquals(Collections.singletonList(id), groupOperationStatus.getFailedModelIds());
        Assert.assertEquals(1, groupOperationStatus.getRequestedModelStatuses().size());
        Assert.assertEquals(model.getModificationDate(),
            groupOperationStatus.getRequestedModelStatuses().get(0).getModel().getModificationDate());

        List<ModelValidationError> errors = groupOperationStatus.getValidationErrors();
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void testConcurrentModificationFailsGroup() {
        long id = model.getId();
        CommonModel modelToUpdate = new CommonModel(model);
        modelToUpdate.setModificationDate(new Date(1));
        model.setModificationDate(new Date(1000));
        putToStorage(model);

        CommonModel otherModel = CommonModelBuilder.newBuilder(idGenerator.getId(), 1, 1)
            .published(false)
            .getModel();
        long otherId = otherModel.getId();
        putToStorage(otherModel);

        ModelSaveGroup group = ModelSaveGroup.fromModels(modelToUpdate, otherModel);
        GroupOperationStatus groupOperationStatus = storage.saveModels(group, context);

        Assert.assertEquals(OperationStatusType.MODEL_MODIFIED, groupOperationStatus.getStatus());
        Assert.assertEquals(ImmutableList.of(id, otherId), groupOperationStatus.getFailedModelIds());
        Assert.assertEquals(2, groupOperationStatus.getRequestedModelStatuses().size());
        Assertions.assertThat(groupOperationStatus.getRequestedModelStatuses()).extracting(OperationStatus::getStatus)
            .containsExactlyInAnyOrder(OperationStatusType.MODEL_MODIFIED, OperationStatusType.FAILED_MODEL_IN_GROUP);

        List<ModelValidationError> errors = groupOperationStatus.getValidationErrors();
        Assertions.assertThat(errors).isEmpty();
    }
}
