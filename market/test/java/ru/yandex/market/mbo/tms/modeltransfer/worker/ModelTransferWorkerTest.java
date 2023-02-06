package ru.yandex.market.mbo.tms.modeltransfer.worker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.TovarTreeDaoMock;
import ru.yandex.market.mbo.db.TovarTreeProtoServiceMock;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.transfer.ChangeModelCategoryService;
import ru.yandex.market.mbo.db.transfer.ChangeModelCategoryServiceImpl;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelParameterLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfParametersConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ModelResultEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.tms.modeltransfer.ListOfModelsConfigBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ModelResultEntryBuilder;
import ru.yandex.market.mbo.user.AutoUser;

/**
 * @author dmserebr
 * @date 01.10.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelTransferWorkerTest {

    private static final long SOURCE_CATEGORY = 1L;
    private static final long TARGET_CATEGORY = 2L;
    private static final long ROOT_CATEGORY_ID = 1;
    private static final long CATEGORY_ID_1 = 101;
    private static final long CATEGORY_ID_2 = 102;

    @Mock
    TovarTreeDaoMock tovarTreeDao = new TovarTreeDaoMock();
    private Map<Long, CommonModel> modelsMap = new HashMap<>();
    private ParameterLoaderServiceStub parameterLoaderService;
    private ModelTransferWorker transferWorker;
    private ModelStorageServiceStub modelStorageService;
    private ChangeModelCategoryService changeModelCategoryService;

    @Before
    public void before() {
        modelStorageService = Mockito.spy(new ModelStorageServiceStub());
        parameterLoaderService = new ParameterLoaderServiceStub();
        fillNameAndVendorParams();
        tovarTreeDao
            .addCategory(new TovarCategory("Root", ROOT_CATEGORY_ID, 0))
            .addCategory(new TovarCategory("Category1", CATEGORY_ID_1, ROOT_CATEGORY_ID))
            .addCategory(new TovarCategory("Category2", CATEGORY_ID_2, ROOT_CATEGORY_ID));

        changeModelCategoryService = new ChangeModelCategoryServiceImpl(
            modelStorageService,
            parameterLoaderService,
            new AutoUser(100L),
            new TovarTreeProtoServiceMock(tovarTreeDao)
        );

        transferWorker = new ModelTransferWorker(
            changeModelCategoryService
        );

        modelStorageService.setModelsMap(modelsMap);
    }

    @Test
    public void testRemovingOfAutoModelsAndRelations() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameter(TARGET_CATEGORY));

        modelsMap.put(10L, CommonModelBuilder.newBuilder(10L, 1L, 1L)
            .title("vendor model 10")
            .currentType(CommonModel.Source.VENDOR)
            .getModel());

        CommonModel sku1 = CommonModelBuilder.newBuilder(4L, 1L, 1L)
            .title("SKU of guru model 3")
            .currentType(CommonModel.Source.SKU)
            .modelRelation(3L, 1L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .modelRelation(10L, 1L, ModelRelation.RelationType.SYNC_SOURCE)
            .getModel();

        CommonModel sku2 = CommonModelBuilder.newBuilder(5L, 1L, 1L)
            .title("SKU 2 of guru model 3")
            .currentType(CommonModel.Source.SKU)
            .modelRelation(3L, 1L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .withSkuRelations(sku1, sku2)
            .getModel());
        modelsMap.put(4L, sku1);
        modelsMap.put(5L, sku2);

        class MyModelStorageStub extends ModelStorageServiceStub {
            @Override
            public GroupOperationStatus saveModels(ModelSaveGroup group,
                                                   ModelSaveContext context) {
                List<OperationStatus> operationStatuses = new ArrayList<>();
                for (CommonModel model : group.getModels()) {
                    modelsMap.remove(model.getId());
                    modelsMap.put(model.getId(), model);
                    operationStatuses.add(new OperationStatus(OperationStatusType.OK, OperationType.CHANGE, model.getId()));
                }
                Assertions.assertThat(group.getModels().size()).isEqualTo(4);
                return new GroupOperationStatus(operationStatuses);
            }
        }

        ModelStorageServiceStub modelStorageService2 = new MyModelStorageStub();
        modelStorageService2.setModelsMap(modelsMap);

        Assertions.assertThat(modelsMap.get(10L).isDeleted()).isEqualTo(false);
        Assertions.assertThat(modelsMap.get(4L).getRelations().size()).isEqualTo(2);
        Assertions.assertThat(modelsMap.get(5L).getRelations().size()).isEqualTo(1);

        ChangeModelCategoryService changeModelCategoryService2 = new ChangeModelCategoryServiceImpl(
            modelStorageService2,
            parameterLoaderService,
            new AutoUser(100L),
            new TovarTreeProtoServiceMock(tovarTreeDao)
        );

        new ModelTransferWorker(
            changeModelCategoryService2
        ).doWork(config(3L), true);

        Assertions.assertThat(modelsMap.get(10L).isDeleted()).isEqualTo(true);
        Assertions.assertThat(modelsMap.get(4L).isDeleted()).isEqualTo(false);
        Assertions.assertThat(modelsMap.get(5L).isDeleted()).isEqualTo(false);
        Assertions.assertThat(modelsMap.get(4L).getRelations().size()).isEqualTo(1);
        Assertions.assertThat(modelsMap.get(5L).getRelations().size()).isEqualTo(1);
    }

    @Test
    public void testListOfModelsWithModificationPassesValidation() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameter(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, 1L, 1L)
            .title("Modification 4")
            .currentType(CommonModel.Source.GURU)
            .parentModelId(3L)
            .getModel());
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, 1L, 2L)
            .title("Guru model 5")
            .currentType(CommonModel.Source.GURU)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L, 5L), true);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #3 is successful")
                .build(),
            ModelResultEntryBuilder.newBuilder(4L, "Modification 4")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.MODIFICATION)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #4 is successful")
                .build(),
            ModelResultEntryBuilder.newBuilder(5L, "Guru model 5")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #5 is successful")
                .build()
        );
    }

    @Test
    public void testListOfModelsWithSkusPassesValidation() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameter(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .modelRelation(4L, 1L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(5L, 1L, ModelRelation.RelationType.SKU_MODEL)
            .getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, 1L, 1L)
            .title("SKU of guru model 3")
            .currentType(CommonModel.Source.SKU)
            .modelRelation(3L, 1L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel());
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, 1L, 1L)
            .title("SKU 2 of guru model 3")
            .currentType(CommonModel.Source.SKU)
            .modelRelation(3L, 1L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel());
        modelsMap.put(6L, CommonModelBuilder.newBuilder(6L, 1L, 2L)
            .title("Guru model 6")
            .currentType(CommonModel.Source.GURU)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L, 6L), true);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #3 is successful")
                .skuIds(4L, 5L)
                .build(),
            ModelResultEntryBuilder.newBuilder(4L, "SKU of guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.SKU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #4 is successful")
                .build(),
            ModelResultEntryBuilder.newBuilder(5L, "SKU 2 of guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.SKU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #5 is successful")
                .build(),
            ModelResultEntryBuilder.newBuilder(6L, "Guru model 6")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #6 is successful")
                .build()
        );
    }

    @Test
    public void testNoParamMatchInTargetCategory() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getAnotherTestParameter(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .parameterValues(50L, "testParam", 60L)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L), true);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.FAILURE)
                .statusMessage("Model mapper failure for model 3")
                .validationErrors("Failed to convert model 3 in group 3. ParameterValue (parameterId=50, " +
                    "xslName=testParam, valueId=60) doesn't have match in new category 2.")
                .build()
        );
    }

    @Test
    public void testNoParamMatchInTargetCategoryButParamsAreIgnored() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getNumericTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getAnotherTestParameter(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .parameterValues(50L, "testParam", 60L)
            .putParameterValue(new ParameterValue(52L, "testParam3", Param.Type.NUMERIC,
                new BigDecimal(10), null, null, null, null))
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(Arrays.asList(50L, 52L), 3L), true);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #3 is successful")
                .build()
        );
    }

    @Test
    public void testNoOptionMatchInTargetCategory() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameterWithAnotherOption(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .parameterValues(50L, "testParam", 61L)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L), true);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.FAILURE)
                .statusMessage("Model mapper failure for model 3")
                .validationErrors("Failed to convert model 3 in group 3. ParameterValue (parameterId=50, " +
                    "xslName=testParam, valueId=61) doesn't have match in new category 2.")
                .build()
        );
    }

    @Test
    public void testFirstModelPassesValidationSecondDoesnt() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameterWithAnotherOption(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .parameterValues(50L, "testParam", 60L)
            .getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, 1L, 2L)
            .title("Guru model 4")
            .currentType(CommonModel.Source.GURU)
            .parameterValues(50L, "testParam", 61L)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L, 4L), true);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #3 is successful")
                .build(),
            ModelResultEntryBuilder.newBuilder(4L, "Guru model 4")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.FAILURE)
                .statusMessage("Model mapper failure for model 4")
                .validationErrors("Failed to convert model 4 in group 4. ParameterValue (parameterId=50, " +
                    "xslName=testParam, valueId=61) doesn't have match in new category 2.")
                .build()
        );
    }

    @Test
    public void testDeletedModels() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameter(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, 1L, 1L)
            .title("Modification 4")
            .currentType(CommonModel.Source.GURU)
            .deleted(true)
            .parentModelId(3L)
            .getModel());
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, 1L, 2L)
            .title("Guru model 5")
            .currentType(CommonModel.Source.GURU)
            .deleted(true)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L, 5L), true);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #3 is successful")
                .build(),
            ModelResultEntryBuilder.newBuilder(5L, "Guru model 5")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.WARNING)
                .statusMessage("Model #5 is deleted")
                .build()
        );
    }

    @Test
    public void testMissingModels() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameter(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L, 6L), true);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #3 is successful")
                .build(),
            ModelResultEntryBuilder.newBuilder(6L, null)
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .status(ModelResultEntry.Status.FAILURE)
                .statusMessage("Model #6 is missing from storage")
                .validationErrors("Unable to find the requested model [id=6, categoryId=1] in storage")
                .build()
        );
    }

    @Test
    public void testModelsGroupSkipped() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameterWithAnotherOption(TARGET_CATEGORY));
        //will be successfull
        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 1")
            .currentType(CommonModel.Source.GURU)
            .getModel());
        //will fail group
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .getModel());
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, 1L, 1L)
            .title("Modification 4")
            .currentType(CommonModel.Source.GURU)
            .parameterValues(50L, "testParam", 61L)
            .parentModelId(4L)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L, 4L), false);

        List<ModelResultEntry> failed = result.stream()
            .filter(mre -> mre.isFailure())
            .collect(Collectors.toList());

        Assertions.assertThat(failed.size()).isEqualTo(1);
        Assertions.assertThat(failed.get(0).getModelId()).isEqualTo(5L);
        Assertions.assertThat(failed.get(0).getStatusMessage())
            .isEqualTo("Model mapper failure for model " + 5L);

        List<ModelResultEntry> warning = result.stream()
            .filter(mre -> mre.getStatus() == ResultEntry.Status.WARNING)
            .collect(Collectors.toList());

        Assertions.assertThat(warning.size()).isEqualTo(1);
        Assertions.assertThat(warning.get(0).getModelId()).isEqualTo(4L);
        Assertions.assertThat(warning.get(0).getStatusMessage())
            .isEqualTo("There was a mapper failure for another model in the group");

        Assertions.assertThat(modelStorageService.getModel(TARGET_CATEGORY, 3L).isPresent()).isEqualTo(true);
        Assertions.assertThat(modelStorageService.getModel(TARGET_CATEGORY, 4L).isPresent()).isEqualTo(false);
        Assertions.assertThat(modelStorageService.getModel(TARGET_CATEGORY, 5L).isPresent()).isEqualTo(false);
    }

    @Test
    public void testModelAndModificationTransferSuccessful() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameter(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, SOURCE_CATEGORY, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .parameterValues(50L, "testParam", 60L)
            .getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, SOURCE_CATEGORY, 1L)
            .title("Modification 4")
            .currentType(CommonModel.Source.GURU)
            .parentModelId(3L)
            .parameterValues(50L, "testParam", 61L)
            .getModel());
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, SOURCE_CATEGORY, 2L)
            .title("Guru model 5")
            .currentType(CommonModel.Source.GURU)
            .parameterValues(50L, "testParam", 60L)
            .parameterValues(50L, "testParam", 61L)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L, 5L), false);

        Assertions.assertThat(result).allMatch(singleModelResult ->
            singleModelResult.getStatus() == ResultEntry.Status.SUCCESS);

        List<CommonModel> modelsInSourceCategory = modelStorageService.getModels(SOURCE_CATEGORY,
            Arrays.asList(3L, 4L, 5L));
        List<CommonModel> modelsInTargetCategory = modelStorageService.getModels(TARGET_CATEGORY,
            Arrays.asList(3L, 4L, 5L));

        Assert.assertTrue(modelsInSourceCategory.isEmpty());
        Assert.assertEquals(3, modelsInTargetCategory.size());
        Assertions.assertThat(modelsInTargetCategory).containsExactlyInAnyOrder(
            CommonModelBuilder.newBuilder(3L, TARGET_CATEGORY, 1L)
                .title("Guru model 3")
                .currentType(CommonModel.Source.GURU)
                .parameterValues(50L, "testParam", 60L)
                .getModel(),
            CommonModelBuilder.newBuilder(4L, TARGET_CATEGORY, 1L)
                .title("Modification 4")
                .currentType(CommonModel.Source.GURU)
                .parentModelId(3L)
                .parameterValues(50L, "testParam", 61L)
                .getModel(),
            CommonModelBuilder.newBuilder(5L, TARGET_CATEGORY, 2L)
                .title("Guru model 5")
                .currentType(CommonModel.Source.GURU)
                .parameterValues(50L, "testParam", 60L)
                .parameterValues(50L, "testParam", 61L)
                .getModel()
        );
    }

    @Test
    public void testAlreadyMovedAndWrongCategoryModels() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameter(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 2L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .modelRelation(4L, 2L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(5L, 2L, ModelRelation.RelationType.SKU_MODEL)
            .getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, 2L, 1L)
            .title("SKU of guru model 3")
            .currentType(CommonModel.Source.SKU)
            .modelRelation(3L, 2L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel());
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, 2L, 1L)
            .title("SKU 2 of guru model 3")
            .currentType(CommonModel.Source.SKU)
            .modelRelation(3L, 2L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel());
        modelsMap.put(6L, CommonModelBuilder.newBuilder(6L, 3L, 2L)
            .title("Guru model 6")
            .currentType(CommonModel.Source.GURU)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L, 6L), false);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Model #3 is already moved")
                .skuIds(4L, 5L)
                .build(),
            ModelResultEntryBuilder.newBuilder(6L, "Guru model 6")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.FAILURE)
                .statusMessage("Model #6 is not in source category")
                .validationErrors("The requested model [id=6, categoryId=1] is currently in category 3")
                .build()
        );
    }

    @Test
    public void testStrictAndBrokenModels() {
        parameterLoaderService.addCategoryParam(getTestParameter(SOURCE_CATEGORY));
        parameterLoaderService.addCategoryParam(getTestParameter(TARGET_CATEGORY));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .title("Guru model 3")
            .currentType(CommonModel.Source.GURU)
            .getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, 1L, 1L)
            .title("Modification 4")
            .currentType(CommonModel.Source.GURU)
            .deleted(true)
            .broken(true)
            .strictChecksRequired(true)
            .parentModelId(3L)
            .getModel());
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, 1L, 2L)
            .title("Guru model 5")
            .currentType(CommonModel.Source.GURU)
            .deleted(true)
            .broken(true)
            .strictChecksRequired(true)
            .getModel());

        List<ModelResultEntry> result = transferWorker.doWork(config(3L, 5L), true);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ModelResultEntryBuilder.newBuilder(3L, "Guru model 3")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.SUCCESS)
                .statusMessage("Validation of model transfer for model #3 is successful")
                .build(),
            ModelResultEntryBuilder.newBuilder(5L, "Guru model 5")
                .sourceCategory(SOURCE_CATEGORY).targetCategory(TARGET_CATEGORY)
                .modelType(ModelResultEntry.ModelType.GURU)
                .status(ModelResultEntry.Status.WARNING)
                .statusMessage("Model #5 is deleted")
                .build()
        );
    }

    private void fillNameAndVendorParams() {
        CategoryParam nameParam = new Parameter();
        nameParam.setId(KnownIds.NAME_PARAM_ID);
        nameParam.setNames(WordUtil.defaultWords("Name"));
        nameParam.setXslName(XslNames.NAME);
        nameParam.setCategoryHid(SOURCE_CATEGORY);
        nameParam.setType(Param.Type.STRING);
        parameterLoaderService.addCategoryParam(nameParam);

        CategoryParam vendorParam = new Parameter();
        vendorParam.setId(KnownIds.VENDOR_PARAM_ID);
        vendorParam.setNames(WordUtil.defaultWords("Vendor"));
        vendorParam.setXslName(XslNames.VENDOR);
        vendorParam.setCategoryHid(SOURCE_CATEGORY);
        vendorParam.setType(Param.Type.ENUM);
        Option vendor1 = new OptionImpl();
        vendor1.setId(1L);
        vendor1.addName(WordUtil.defaultWord("Vendor1"));
        vendorParam.addOption(vendor1);
        Option vendor2 = new OptionImpl();
        vendor2.setId(2L);
        vendor2.addName(WordUtil.defaultWord("Vendor2"));
        vendorParam.addOption(vendor2);
        parameterLoaderService.addCategoryParam(vendorParam);

        //

        CategoryParam nameParamTarget = new Parameter();
        nameParamTarget.setId(KnownIds.NAME_PARAM_ID + 1);
        nameParamTarget.setNames(WordUtil.defaultWords("Name"));
        nameParamTarget.setXslName(XslNames.NAME);
        nameParamTarget.setCategoryHid(TARGET_CATEGORY);
        nameParamTarget.setType(Param.Type.STRING);
        parameterLoaderService.addCategoryParam(nameParamTarget);

        CategoryParam vendorParamTarget = new Parameter();
        vendorParamTarget.setId(KnownIds.VENDOR_PARAM_ID + 1);
        vendorParamTarget.setNames(WordUtil.defaultWords("Vendor"));
        vendorParamTarget.setXslName(XslNames.VENDOR);
        vendorParamTarget.setCategoryHid(TARGET_CATEGORY);
        vendorParamTarget.setType(Param.Type.ENUM);
        Option vendor1new = new OptionImpl();
        vendor1new.setId(3L);
        vendor1new.addName(WordUtil.defaultWord("Vendor1"));
        vendorParamTarget.addOption(vendor1new);
        Option vendor2new = new OptionImpl();
        vendor2new.setId(4L);
        vendor2new.addName(WordUtil.defaultWord("Vendor2"));
        vendorParamTarget.addOption(vendor2new);
        parameterLoaderService.addCategoryParam(vendorParamTarget);
    }

    private CategoryParam getTestParameter(long categoryId) {
        CategoryParam param = new Parameter();
        param.setId(50L);
        param.setNames(WordUtil.defaultWords("test param"));
        param.setXslName("testParam");
        param.setCategoryHid(categoryId);
        param.setType(Param.Type.ENUM);

        Option option1 = new OptionImpl();
        option1.setId(60L);
        option1.addName(WordUtil.defaultWord("option1"));
        param.addOption(option1);
        Option option2 = new OptionImpl();
        option2.setId(61L);
        option2.addName(WordUtil.defaultWord("option2"));
        param.addOption(option2);

        return param;
    }

    private CategoryParam getAnotherTestParameter(long categoryId) {
        CategoryParam param = new Parameter();
        param.setId(51L);
        param.setNames(WordUtil.defaultWords("test param 2"));
        param.setXslName("testParam2");
        param.setCategoryHid(categoryId);
        param.setType(Param.Type.ENUM);
        return param;
    }

    private CategoryParam getTestParameterWithAnotherOption(long categoryId) {
        CategoryParam param = new Parameter();
        param.setId(50L);
        param.setNames(WordUtil.defaultWords("test param"));
        param.setXslName("testParam");
        param.setCategoryHid(categoryId);
        param.setType(Param.Type.ENUM);

        Option option1 = new OptionImpl();
        option1.setId(60L);
        option1.addName(WordUtil.defaultWord("option1"));
        param.addOption(option1);
        Option option3 = new OptionImpl();
        option3.setId(62L);
        option3.addName(WordUtil.defaultWord("option3"));
        param.addOption(option3);
        return param;
    }

    private CategoryParam getNumericTestParameter(long categoryId) {
        CategoryParam param = new Parameter();
        param.setId(52L);
        param.setNames(WordUtil.defaultWords("test param 3"));
        param.setXslName("testParam3");
        param.setCategoryHid(categoryId);
        param.setType(Param.Type.NUMERIC);
        return param;
    }

    private ListOfModelParameterLandingConfig config(long... modelIds) {
        return new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY, TARGET_CATEGORY, modelIds)
                .build());
    }

    private ListOfModelParameterLandingConfig config(List<Long> parameterIds, long... modelIds) {
        return new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY, TARGET_CATEGORY, modelIds)
                .build(),
            new ListOfParametersConfig(parameterIds));
    }
}
