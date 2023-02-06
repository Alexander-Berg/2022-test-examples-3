package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.ParameterValueValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.processing.EraseBrokenParamValuesProcessor;
import ru.yandex.market.mbo.db.modelstorage.validation.processing.EraseInvalidParamOptionIdErrorProcessor;
import ru.yandex.market.mbo.db.modelstorage.validation.processing.OutOfBoundsParamValueErrorProcessor;
import ru.yandex.market.mbo.db.modelstorage.validation.processing.ValidationErrorProcessingService;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author york
 */
public class EraseBrokenParamsStorageTest extends BaseGroupStorageUpdatesTest {
    private static final long CORRECT_ID = 1L;
    private static final long INCORRECT_ID = 2L;

    @Override
    protected ModelValidationContext createModelValidationContext() {
        ModelValidationContext ctx = Mockito.mock(ModelValidationContext.class);
        Mockito.when(ctx.parameterMatchesDefinition(anyLong(), any())).thenAnswer(invocation -> {
            ParameterValue value = invocation.getArgument(1);
            return value.getParamId() != INCORRECT_ID;
        });
        Mockito.when(ctx.getOptionNames(anyLong(), anyLong()))
            .thenReturn(Collections.singletonMap(1L, "option_name"));
        Mockito.when(ctx.getReadableParameterName(anyLong(), anyLong()))
            .thenAnswer(invocation -> invocation.getArgument(1).toString());
        Mockito.when(ctx.getStats()).thenReturn(new OperationStats());
        return ctx;
    }

    @Override
    protected ModelValidationService createModelValidationService() {
        ModelValidationService baseService = super.createModelValidationService(
            Collections.singletonList(new ParameterValueValidator()));
        return baseService;
    }


    @Override
    protected ValidationErrorProcessingService createValidationErrorProcessingService() {
        return new ValidationErrorProcessingService(Arrays.asList(
            new EraseBrokenParamValuesProcessor(),
            new OutOfBoundsParamValueErrorProcessor(),
            new EraseInvalidParamOptionIdErrorProcessor()
        ));
    }

    @Test
    public void testAllSuccessFailed() {
        CommonModel model1 = CommonModelBuilder.newBuilder(CommonModel.NO_ID, 1L, 1)
            .parameterValues(CORRECT_ID, "correct", 1L)
            .getModel();
        updateModificationSource(model1);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        ModelSaveContext context2 = new ModelSaveContext(USER_ID);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context2);

        assertThat(groupOperationStatus.getStatus()).isEqualTo(OperationStatusType.OK);

        Optional<CommonModel> modelAfterSave =
            storage.getModel(model1.getCategoryId(), model1.getId(), new ReadStats());
        assertThat(modelAfterSave.isPresent()).isEqualTo(true);
        assertThat(modelAfterSave.get().getParameterValues(CORRECT_ID)).isNotNull();
    }

    @Test
    public void testEraseBrokenParamsWontChangeModelOnCorrectParams() {
        CommonModel model1 = CommonModelBuilder.newBuilder(CommonModel.NO_ID, 1, 1)
            .parameterValues(CORRECT_ID, "correct", 1L)
            .getModel();
        updateModificationSource(model1);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        ModelSaveContext context2 = new ModelSaveContext(USER_ID).setEraseBrokenParams(true);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context2);

        assertThat(groupOperationStatus.getStatus()).isEqualTo(OperationStatusType.OK);

        Optional<CommonModel> modelAfterSave =
            storage.getModel(model1.getCategoryId(), model1.getId(), new ReadStats());
        assertThat(modelAfterSave.isPresent()).isEqualTo(true);
        assertThat(modelAfterSave.get().getParameterValues(CORRECT_ID)).isNotNull();
    }

    @Test
    public void testIncorrectParamValidationFailed() {
        CommonModel model1 = CommonModelBuilder.newBuilder(CommonModel.NO_ID, 1, 1)
            .parameterValues(INCORRECT_ID, "incorrrect", 1)
            .getModel();
        updateModificationSource(model1);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        ModelSaveContext context2 = new ModelSaveContext(USER_ID);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context2);

        assertThat(groupOperationStatus.getStatus()).isEqualTo(OperationStatusType.VALIDATION_ERROR);
    }

    @Test
    public void testIncorrectParamValidationCorrected() {
        CommonModel model1 = CommonModelBuilder.newBuilder(CommonModel.NO_ID, 1, 1)
            .parameterValues(INCORRECT_ID, "incorrrect", 1)
            .getModel();
        updateModificationSource(model1);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        ModelSaveContext context2 = new ModelSaveContext(USER_ID).setEraseBrokenParams(true);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context2);

        assertThat(groupOperationStatus.getStatus()).isEqualTo(OperationStatusType.OK);

        Optional<CommonModel> modelAfterSave =
            storage.getModel(model1.getCategoryId(), model1.getId(), new ReadStats());
        assertThat(modelAfterSave.isPresent()).isEqualTo(true);
        assertThat(modelAfterSave.get().getParameterValues(INCORRECT_ID)).isNull();
    }

    @Test
    public void testIncorrectParamOptionIdValidationFailed() {
        CommonModel model1 = CommonModelBuilder.newBuilder(CommonModel.NO_ID, 1, 1)
            .parameterValues(CORRECT_ID, "corrrect", 2)
            .getModel();
        updateModificationSource(model1);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        ModelSaveContext context2 = new ModelSaveContext(USER_ID);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context2);

        assertThat(groupOperationStatus.getStatus()).isEqualTo(OperationStatusType.VALIDATION_ERROR);
    }

    @Test
    public void testIncorrectParamOptionIdValidationCorrected() {
        CommonModel model1 = CommonModelBuilder.newBuilder(CommonModel.NO_ID, 1, 1)
            .parameterValues(CORRECT_ID, "corrrect", 2)
            .getModel();
        updateModificationSource(model1);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        ModelSaveContext context2 = new ModelSaveContext(USER_ID).setEraseParamsWithInvalidOptionIds(true);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context2);

        assertThat(groupOperationStatus.getStatus()).isEqualTo(OperationStatusType.OK);
    }


    private void  updateModificationSource(CommonModel model) {
        model.getParameterValues().stream().flatMap(ParameterValues::stream)
            .forEach(pv -> pv.setModificationSource(ModificationSource.OPERATOR_FILLED));
    }
}
