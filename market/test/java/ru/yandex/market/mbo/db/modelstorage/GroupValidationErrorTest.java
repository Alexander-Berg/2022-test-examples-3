package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.validation.MandatoryParametersValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationContextStub;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.dump.DumpValidationService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterValueBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorSubtype.MISSING_MANDATORY;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType.MANDATORY_PARAM_EMPTY;
import static ru.yandex.market.mbo.db.modelstorage.validation.ValidationErrorUtils.assertValidationError;
import static ru.yandex.market.mbo.http.ModelStorage.ErrorParamName.PARAMETER_NAME;
import static ru.yandex.market.mbo.http.ModelStorage.ErrorParamName.PARAM_ID;
import static ru.yandex.market.mbo.http.ModelStorage.ErrorParamName.PARAM_XSL_NAME;

/**
 * @author dmserebr
 * @date 19/04/2019
 */
@SuppressWarnings("checkstyle:magicNumber")
public class GroupValidationErrorTest extends BaseGroupStorageUpdatesTest {

    private CommonModel model = CommonModelBuilder.newBuilder(1L, 10L, 20L)
        .currentType(CommonModel.Source.GURU)
        .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
        .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
        .endModel();

    private CommonModel sku1 = CommonModelBuilder.newBuilder(2L, 10L, 20L)
        .currentType(CommonModel.Source.SKU)
        .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
        .endModel();

    private CommonModel sku2 = CommonModelBuilder.newBuilder(3L, 10L, 20L)
        .currentType(CommonModel.Source.SKU)
        .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
        .endModel();

    private CommonModel model2 = CommonModelBuilder.newBuilder(4L, 10L, 20L)
        .currentType(CommonModel.Source.GURU)
        .endModel();

    private CommonModel newSku1 = CommonModelBuilder.newBuilder(0L, 10L, 20L)
        .currentType(CommonModel.Source.SKU)
        .title("model1")
        .modelRelation(4L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
        .endModel();

    private CommonModel newSku2 = CommonModelBuilder.newBuilder(0L, 10L, 20L)
        .currentType(CommonModel.Source.SKU)
        .title("model2")
        .modelRelation(4L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
        .endModel();

    private CommonModel newSkuWithUniqueId = CommonModelBuilder.newBuilder(-5L, 10L, 20L)
        .currentType(CommonModel.Source.SKU)
        .modelRelation(4L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
        .endModel();

    private ModelValidationContextStub ctx;

    private static ModelSaveContext saveContext = new ModelSaveContext(12345L);

    @Before
    public void before() {
        super.before();
        ctx = new ModelValidationContextStub(mock(DumpValidationService.class));
        ctx.setStatsModelStorageService(storage);
    }

    @Override
    protected ModelValidationContext createModelValidationContext() {
        return ctx;
    }

    @Override
    protected ModelValidationService createModelValidationService() {
        ModelValidationService baseService = super.createModelValidationService(
            Collections.singletonList(new MandatoryParametersValidator()));
        return baseService;
    }

    @Test
    public void testNoValidationErrorsOnOneModel() {
        ModelSaveGroup group = ModelSaveGroup.fromModels(model);
        storage.putToStorage(model);

        GroupOperationStatus status = storage.saveModels(group, saveContext);

        assertThat(status.getStatus()).isEqualTo(OperationStatusType.OK);
        assertThat(status.getAllModelStatuses().stream().allMatch(OperationStatus::isOk));
    }

    @Test
    public void testValidationErrorsInSingleModel() {
        ctx.addParam(100L, "testParam", true, SkuParameterMode.SKU_NONE);

        ModelSaveGroup group = ModelSaveGroup.fromModels(model);

        CommonModel r1 = CommonModelBuilder.newBuilder(2L, 10L).getModel();
        CommonModel r2 = CommonModelBuilder.newBuilder(3L, 10L).getModel();

        group.addIfAbsent(r1);
        group.addIfAbsent(r2);

        storage.putToStorage(this.model);
        // Add related models to storage
        storage.putToStorage(r1, r2);

        GroupOperationStatus status = storage.saveModels(group, saveContext);

        assertThat(status.getStatus()).isEqualTo(OperationStatusType.OK); // not critical error
        assertThat(status.getAllModelStatuses()).hasSize(3);
        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(0),
            OperationType.CHANGE, OperationStatusType.NO_OP,
            "Operation is not performed", 1L, null, this.model);
        OperationStatusTestUtils.assertOperationStatus(status.getAdditionalModelStatues().get(0),
            OperationType.CHANGE, OperationStatusType.OK,
            "Operation completed successfully", 2L, null, r1);
        OperationStatusTestUtils.assertOperationStatus(status.getAdditionalModelStatues().get(1),
            OperationType.CHANGE, OperationStatusType.OK,
            "Operation completed successfully", 3L, null, r2);

        List<ModelValidationError> validationErrors = status.getRequestedModelStatuses().get(0).getValidationErrors();
        assertThat(validationErrors).hasSize(1);
        assertValidationError(validationErrors.get(0), 1L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, false);
    }

    @Test
    public void testValidationErrorsInModelWhichHasSkus() {
        ctx.addParam(100L, "testParam", true, SkuParameterMode.SKU_NONE);

        // model is missing testParam value; skus have everything
        sku1.addParameterValue(ParameterValueBuilder.newBuilder()
            .paramId(100L).xslName("testParam").type(Param.Type.BOOLEAN).booleanValue(true, 124L)
            .build());
        sku2.addParameterValue(ParameterValueBuilder.newBuilder()
            .paramId(100L).xslName("testParam").type(Param.Type.BOOLEAN).booleanValue(true, 122L)
            .build());

        ModelSaveGroup group = ModelSaveGroup.fromModels(model);
        storage.putToStorage(model, sku1, sku2);

        GroupOperationStatus status = storage.saveModels(group, saveContext);

        assertThat(status.getStatus()).isEqualTo(OperationStatusType.OK);
        assertThat(status.getRequestedModelStatuses()).hasSize(1);
        assertThat(status.getAdditionalModelStatues()).isEmpty();

        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(0),
            OperationType.CHANGE, OperationStatusType.NO_OP,
            "Operation is not performed", 1L, null, model);

        List<ModelValidationError> validationErrors =
            status.getRequestedModelStatuses().get(0).getValidationErrors();
        assertThat(validationErrors).hasSize(1);
        assertValidationError(validationErrors.get(0), 1L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, false);
    }

    @Test
    public void testValidationErrorsInSku() {
        // mandatory parameter validation errors of SKU are contained in SKU's OperationStatus
        ctx.addParam(100L, "testParam", true, SkuParameterMode.SKU_INFORMATIONAL);

        // sku1 is missing param value, sku2 has the value
        sku2.addParameterValue(ParameterValueBuilder.newBuilder()
            .paramId(100L).xslName("testParam").type(Param.Type.BOOLEAN).booleanValue(true, 123L)
            .build());

        ModelSaveGroup group = ModelSaveGroup.fromModels(model);

        storage.putToStorage(model, sku1, sku2);

        GroupOperationStatus status = storage.saveModels(group, saveContext);

        assertThat(status.getStatus()).isEqualTo(OperationStatusType.OK);
        assertThat(status.getRequestedModelStatuses()).hasSize(1);
        assertThat(status.getAdditionalModelStatues()).hasSize(0);

        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(0),
            OperationType.CHANGE, OperationStatusType.NO_OP,
            "Operation is not performed", 1L, null, model);

        List<ModelValidationError> validationErrors = status.getValidationErrors();
        assertThat(validationErrors).hasSize(1);
        assertValidationError(validationErrors.get(0), 2L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, false);
    }

    @Test
    public void testValidationErrorsInBothParentAndSku() {
        ctx.addParam(100L, "testParam", true, SkuParameterMode.SKU_NONE);
        ctx.addParam(101L, "testParam2", true, SkuParameterMode.SKU_DEFINING);

        // model is missing testParam value; sku1 is missing testParam2 value; sku2 has everything
        sku1.addParameterValue(ParameterValueBuilder.newBuilder()
            .paramId(100L).xslName("testParam").type(Param.Type.BOOLEAN).booleanValue(true, 124L)
            .build());
        sku2.addParameterValue(ParameterValueBuilder.newBuilder()
            .paramId(100L).xslName("testParam").type(Param.Type.BOOLEAN).booleanValue(true, 122L)
            .paramId(101L).xslName("testParam2").type(Param.Type.BOOLEAN).booleanValue(true, 123L)
            .build());

        ModelSaveGroup group = ModelSaveGroup.fromModels(model);
        storage.putToStorage(model, sku1, sku2);

        GroupOperationStatus status = storage.saveModels(group, saveContext);

        assertThat(status.getStatus()).isEqualTo(OperationStatusType.OK);
        assertThat(status.getRequestedModelStatuses()).hasSize(1);
        assertThat(status.getAdditionalModelStatues()).hasSize(0);

        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(0),
            OperationType.CHANGE, OperationStatusType.NO_OP,
            "Operation is not performed", 1L, null, model);

        Map<Long, List<ModelValidationError>> requestedValidationErrorsById =
            status.getRequestedModelStatuses().get(0).getValidationErrors().stream()
                .collect(Collectors.groupingBy(ModelValidationError::getModelId));

        assertThat(requestedValidationErrorsById).containsOnlyKeys(1L, 2L);
        assertThat(requestedValidationErrorsById.get(1L))
            .hasSize(1)
            .satisfies(ves -> assertValidationError(
                requestedValidationErrorsById.get(1L).get(0), 1L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, false));
        assertThat(requestedValidationErrorsById.get(2L))
            .hasSize(1)
            .satisfies(ves -> assertValidationError(
                requestedValidationErrorsById.get(2L).get(0), 2L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, false));

        Map<Long, List<ModelValidationError>> additionalValidationErrorsById =
            status.getValidationErrors().stream()
                .collect(Collectors.groupingBy(ModelValidationError::getModelId));
        assertThat(additionalValidationErrorsById).containsOnlyKeys(1L, 2L);
        assertThat(additionalValidationErrorsById.get(1L))
            .hasSize(1)
            .satisfies(ves -> assertValidationError(
                additionalValidationErrorsById.get(1L).get(0), 1L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, false));
        assertThat(additionalValidationErrorsById.get(2L))
            .hasSize(1)
            .satisfies(ves -> assertValidationError(
                additionalValidationErrorsById.get(2L).get(0), 2L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, false));
    }

    @Test
    public void testValidationErrorsInOneNewSku() {
        // for new SKUs, mandatory param validation errors are left in the base model
        ctx.addParam(100L, "testParam", true, SkuParameterMode.SKU_DEFINING);

        // sku1 is missing the param value, model and sku2 has all needed param values
        newSku2.addParameterValue(ParameterValueBuilder.newBuilder()
            .paramId(100L).xslName("testParam").type(Param.Type.BOOLEAN).booleanValue(true, 122L)
            .build());

        ModelSaveGroup group = ModelSaveGroup.fromModels(model2, newSku1, newSku2);
        storage.putToStorage(model2);

        GroupOperationStatus status = storage.saveModels(group, saveContext);

        assertThat(status.getStatus()).isEqualTo(OperationStatusType.VALIDATION_ERROR);
        assertThat(status.getRequestedModelStatuses()).hasSize(3);
        assertThat(status.getAdditionalModelStatues()).isEmpty();

        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(0),
            OperationType.CHANGE, OperationStatusType.VALIDATION_ERROR,
            "Validation error occurred", 4L, null, null);

        List<ModelValidationError> validationErrors = status.getRequestedModelStatuses().get(0).getValidationErrors();
        assertThat(validationErrors).hasSize(1);
        assertValidationError(validationErrors.get(0), -1L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, true);

        assertThat(status.getRequestedModelStatuses().get(1).getValidationErrors()).isEmpty();
        assertThat(status.getRequestedModelStatuses().get(2).getValidationErrors()).isEmpty();
    }

    @Test
    public void testValidationErrorsInNewSkus() {
        // for new SKUs, mandatory param validation errors are left in the base model
        ctx.addParam(100L, "testParam", true, SkuParameterMode.SKU_DEFINING);
        ctx.addParam(101L, "testParam2", true, SkuParameterMode.SKU_INFORMATIONAL);

        // sku1 and sku2 are missing the param value, model has all needed param values
        newSku2.addParameterValue(ParameterValueBuilder.newBuilder()
            .paramId(101L).xslName("testParam2").type(Param.Type.BOOLEAN).booleanValue(true, 123L)
            .build());

        ModelSaveGroup group = ModelSaveGroup.fromModels(model2, newSku1, newSku2);
        storage.putToStorage(model2);

        GroupOperationStatus status = storage.saveModels(group, saveContext);

        assertThat(status.getStatus()).isEqualTo(OperationStatusType.VALIDATION_ERROR);
        assertThat(status.getRequestedModelStatuses()).hasSize(3);
        assertThat(status.getAdditionalModelStatues()).isEmpty();

        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(0),
            OperationType.CHANGE, OperationStatusType.VALIDATION_ERROR,
            "Validation error occurred", 4L, null, null);

        List<ModelValidationError> validationErrors = status.getRequestedModelStatuses().get(0).getValidationErrors();
        assertThat(validationErrors)
            .containsExactlyInAnyOrder(
                new ModelValidationError(-1L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, true)
                    .addParam(PARAMETER_NAME, "Param100")
                    .addParam(PARAM_XSL_NAME, "testParam")
                    .addParam(PARAM_ID, "100")
                    .addLocalizedMessagePattern("Обязательный параметр '%{PARAMETER_NAME}'(%{PARAM_ID}) не заполнен."),
                new ModelValidationError(-1L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, true)
                    .addParam(PARAMETER_NAME, "Param101")
                    .addParam(PARAM_XSL_NAME, "testParam2")
                    .addParam(PARAM_ID, "101")
                    .addLocalizedMessagePattern("Обязательный параметр '%{PARAMETER_NAME}'(%{PARAM_ID}) не заполнен."),
                new ModelValidationError(-2L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, true)
                    .addParam(PARAMETER_NAME, "Param100")
                    .addParam(PARAM_XSL_NAME, "testParam")
                    .addParam(PARAM_ID, "100")
                    .addLocalizedMessagePattern("Обязательный параметр '%{PARAMETER_NAME}'(%{PARAM_ID}) не заполнен.")
            );

        assertThat(status.getRequestedModelStatuses().get(1).getValidationErrors()).isEmpty();
        assertThat(status.getRequestedModelStatuses().get(2).getValidationErrors()).isEmpty();
    }

    @Test
    public void testValidationErrorsInOneNewSkuWithUniqueId() {
        // for new SKUs with negative ids, mandatory param validation errors go to skus
        ctx.addParam(100L, "testParam", true, SkuParameterMode.SKU_DEFINING);

        // sku is missing the param value, model has all needed param values

        ModelSaveGroup group = ModelSaveGroup.fromModels(model2, newSkuWithUniqueId);
        storage.putToStorage(model2);

        GroupOperationStatus status = storage.saveModels(group, saveContext);

        assertThat(status.getStatus()).isEqualTo(OperationStatusType.VALIDATION_ERROR);
        assertThat(status.getRequestedModelStatuses()).hasSize(2);
        assertThat(status.getAdditionalModelStatues()).isEmpty();

        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(0),
            OperationType.CHANGE, OperationStatusType.FAILED_MODEL_IN_GROUP,
            "There are failed models in group. Failed models: -5", 4L, null, null);
        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(1),
            OperationType.CREATE, OperationStatusType.VALIDATION_ERROR,
            "Validation error occurred", -5L, null, null);

        assertThat(status.getRequestedModelStatuses().get(0).getValidationErrors()).isEmpty();

        List<ModelValidationError> validationErrors = status.getRequestedModelStatuses().get(1).getValidationErrors();
        assertThat(validationErrors).hasSize(1);
        assertValidationError(validationErrors.get(0), -5L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, true);
    }

    @Test
    public void testValidationErrorsInSkuWhenCategoryIdChanged() {
        // if category id is changed, errors are critical -> group status is validation error
        ctx.addParam(100L, "testParam", true, SkuParameterMode.SKU_INFORMATIONAL);

        // sku1 is missing param value, sku2 has the value
        sku2.addParameterValue(ParameterValueBuilder.newBuilder()
            .paramId(100L).xslName("testParam").type(Param.Type.BOOLEAN).booleanValue(true, 123L)
            .build());

        ModelSaveGroup group = ModelSaveGroup.fromModels(model, sku1, sku2);
        CommonModel oldModel = new CommonModel(model);
        oldModel.setCategoryId(9L);
        CommonModel oldSku1 = new CommonModel(sku1);
        oldSku1.setCategoryId(9L);
        CommonModel oldSku2 = new CommonModel(sku2);
        oldSku2.setCategoryId(9L);
        group.addBeforeModels(Arrays.asList(oldModel, oldSku1, oldSku2));

        storage.putToStorage(model, sku1, sku2);

        GroupOperationStatus status = storage.saveModels(group, saveContext);

        assertThat(status.getStatus()).isEqualTo(OperationStatusType.VALIDATION_ERROR);
        assertThat(status.getRequestedModelStatuses()).hasSize(3);
        assertThat(status.getAdditionalModelStatues()).isEmpty();

        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(0),
            OperationType.CHANGE, OperationStatusType.FAILED_MODEL_IN_GROUP,
            "There are failed models in group. Failed models: 2", 1L, null, null);

        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(1),
            OperationType.CHANGE, OperationStatusType.VALIDATION_ERROR,
            "Validation error occurred", 2L, null, null);

        OperationStatusTestUtils.assertOperationStatus(status.getRequestedModelStatuses().get(2),
            OperationType.CHANGE, OperationStatusType.FAILED_MODEL_IN_GROUP,
            "There are failed models in group. Failed models: 2", 3L, null, null);

        assertThat(status.getRequestedModelStatuses().get(0).getValidationErrors()).isEmpty();

        List<ModelValidationError> validationErrors = status.getRequestedModelStatuses().get(1).getValidationErrors();
        assertThat(validationErrors).hasSize(1);
        assertValidationError(validationErrors.get(0), 2L, MANDATORY_PARAM_EMPTY, MISSING_MANDATORY, true);

        assertThat(status.getRequestedModelStatuses().get(2).getValidationErrors()).isEmpty();
    }
}
