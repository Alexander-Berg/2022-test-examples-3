package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.TovarTreeDaoMock;
import ru.yandex.market.mbo.db.TovarTreeProtoServiceMock;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ExporterModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.export.client.licensor.CacheLicensorServiceClient;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.ErrorParamName;
import ru.yandex.market.mbo.licensor.MboLicensors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author ayratgdl
 * @date 22.06.18
 */
public class LicensorModelValidatorTest {
    private static final long ROOT_CATEGORY_ID = 1;
    private static final long CATEGORY_ID_1 = 101;
    private static final long CATEGORY_ID_2 = 102;
    private static final long VENDOR_ID_1 = 201;
    private static final long VENDOR_ID_2 = 202;
    private static final long MODEL_ID_1 = 301;
    private static final long LICENSOR_ID_1 = 401;
    private static final long LICENSOR_ID_2 = 402;
    private static final long FRANCHISE_ID_1 = 501;
    private static final long PERSONAGE_ID_1 = 601;

    private LicensorModelValidator validator;

    private ModelValidationContext context;
    // валидатор LicensorModelValidator при валидации не рассматривает список обновляемых моделей
    private Collection<CommonModel> updatedModels = Collections.emptyList();

    private List<MboLicensors.Licensor> licensors;

    @Before
    public void setUp() throws Exception {
        validator = new LicensorModelValidator();

        TovarTreeDaoMock tovarTreeDaoMock = new TovarTreeDaoMock();
        tovarTreeDaoMock
            .addCategory(new TovarCategory("Root", ROOT_CATEGORY_ID, 0))
            .addCategory(new TovarCategory("Category1", CATEGORY_ID_1, ROOT_CATEGORY_ID))
            .addCategory(new TovarCategory("Category2", CATEGORY_ID_2, ROOT_CATEGORY_ID));
        TovarTreeProtoServiceMock tovarTreeProtoService = new TovarTreeProtoServiceMock(tovarTreeDaoMock);

        CacheLicensorServiceClient licensorServiceClient = Mockito.mock(CacheLicensorServiceClient.class);
        licensors = new ArrayList<>();
        Mockito.when(licensorServiceClient.getLicensorConstrains()).thenReturn(licensors);
        context = new ExporterModelValidationContext(
            null, null, null, null,
            tovarTreeProtoService, licensorServiceClient
        );
    }

    @Test
    public void validateOnlyOnCreateAndUpdate() {
        Assert.assertEquals(Arrays.asList(ModelChanges.Operation.CREATE, ModelChanges.Operation.UPDATE),
                            validator.getSupportedOperations()
        );
    }

    @Test
    public void ifModelDoNotContainsLFCThenOkAndInformationAboutLicensorsIsNotLoading() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        boolean shouldValidate = validator.isPropertyChanged(context, modelChanges, updatedModels);
        Assert.assertFalse(shouldValidate);
    }

    @Test
    public void ifModelUnchangedThenOk() {
        CommonModel beforeModel = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1)
            .parameterValues(KnownIds.FRANCHISE_PARAM_ID, "franchise", FRANCHISE_ID_1)
            .parameterValues(KnownIds.PERSONAGE_PARAM_ID, "personage", PERSONAGE_ID_1)
            .getModel();

        CommonModel afterModel = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1)
            .parameterValues(KnownIds.FRANCHISE_PARAM_ID, "franchise", FRANCHISE_ID_1)
            .parameterValues(KnownIds.PERSONAGE_PARAM_ID, "personage", PERSONAGE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(beforeModel, afterModel, ModelChanges.Operation.UPDATE);
        boolean shouldValidate = validator.isPropertyChanged(context, modelChanges, updatedModels);
        Assert.assertTrue(shouldValidate);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    // region Валидация допустимости Лицензиаров для заданного вендора

    @Test
    public void validateLicensorByVendorWhenLVConstrainsL1V1C1AndModelC1V1L1ThenOk() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_ID_1)
                        .setCategoryId(CATEGORY_ID_1)
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validateLicensorByVendorWhenLVConstrainsL1V1C1AndModelC1V2L1ThenError() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_ID_1)
                        .setCategoryId(CATEGORY_ID_1)
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_2)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Arrays.asList(
            new ModelValidationError(MODEL_ID_1, ModelValidationError.ErrorType.ILLEGAL_LICENSOR)
                .addLocalizedMessagePattern(
                    "Модель содержит лицензиара %{LICENSOR_ID} который не допустим для вендора %{VENDOR_ID}.")
                .addParam(ModelStorage.ErrorParamName.LICENSOR_ID, LICENSOR_ID_1)
                .addParam(ModelStorage.ErrorParamName.VENDOR_ID, VENDOR_ID_2)
        );
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validateLicensorByVendorWhenLVConstrainsL1V1C1AndModelC2V1L1ThenOk() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_ID_1)
                        .setCategoryId(CATEGORY_ID_1)
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_2, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validateLicensorByVendorWhenLVConstraintsL1V1C1AndL2V2C1AndModelC1V1L1L2ThenError() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_ID_1)
                        .setCategoryId(CATEGORY_ID_1)
                )
                .build()
        );
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_2)
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_ID_2)
                        .setCategoryId(CATEGORY_ID_1)
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1, LICENSOR_ID_2)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Arrays.asList(
            new ModelValidationError(MODEL_ID_1, ModelValidationError.ErrorType.ILLEGAL_LICENSOR)
                .addLocalizedMessagePattern(
                    "Модель содержит лицензиара %{LICENSOR_ID} который не допустим для вендора %{VENDOR_ID}.")
                .addParam(ModelStorage.ErrorParamName.LICENSOR_ID, LICENSOR_ID_2)
                .addParam(ModelStorage.ErrorParamName.VENDOR_ID, VENDOR_ID_1)
        );
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validateLicensorByVendorWithInheritance() {
        // добавляем связь вендор лицензиар отличающиеся от параметров в модели, валидация не пройдет
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_ID_2)
                        .setCategoryId(CATEGORY_ID_1)
                )
                .build()
        );

        // добавляем правильную связь на уровень выше
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_ID_1)
                        .setCategoryId(ROOT_CATEGORY_ID)
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    // endregion

    // region Проверка свойства неухудшающего сохранения

    @Test
    public void validateAnyErrorOnUnchangedModelIsWarning() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_2)
            .parameterValues(KnownIds.FRANCHISE_PARAM_ID, "franchise", FRANCHISE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(model, model, ModelChanges.Operation.CREATE);
        boolean shouldValidate = validator.isPropertyChanged(context, modelChanges, updatedModels);
        Assert.assertTrue(shouldValidate);

        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);
        List<ModelValidationError> expectedErrors = Collections.singletonList(
            new ModelValidationError(MODEL_ID_1, ErrorType.ILLEGAL_LICENSOR, false) // <-- warn
                .addLocalizedMessagePattern(
                    "Модель содержит франшизу %{FRANCHISE_ID} для которой не найден подходящий лицензиар.")
                .addParam(ErrorParamName.FRANCHISE_ID, FRANCHISE_ID_1)
        );
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    // endregion

    // region Валидация допустимости Лицензиара в зависимости от Франшиз и Персонажей
    // При валидации допустимости Лицензиара Франшизы и Персонажы не рассматриваются

    @Test
    public void validateLicensorWhenLFPConstraintsLFAndModelLThenOk() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    // endregion

    // region Валидация допустимости Франшизы в зависимости от Лицензиара и Персонажа
    // При валидации допустимости Франшизы Персонажы не рассматриаются только Лицензиары

    @Test
    public void validateFranchiseWhenLFPConstraintsL1F1AndModelL2F1ThenError() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_2)
            .parameterValues(KnownIds.FRANCHISE_PARAM_ID, "franchise", FRANCHISE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Arrays.asList(
            new ModelValidationError(MODEL_ID_1, ModelValidationError.ErrorType.ILLEGAL_LICENSOR)
                .addLocalizedMessagePattern(
                    "Модель содержит франшизу %{FRANCHISE_ID} для которой не найден подходящий лицензиар.")
                .addParam(ModelStorage.ErrorParamName.FRANCHISE_ID, FRANCHISE_ID_1)
        );
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validateFranchiseWhenLFPConstraintsL1F1AndModelF1ThenError() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.FRANCHISE_PARAM_ID, "franchise", FRANCHISE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Arrays.asList(
            new ModelValidationError(MODEL_ID_1, ModelValidationError.ErrorType.ILLEGAL_LICENSOR)
                .addLocalizedMessagePattern(
                    "Модель содержит франшизу %{FRANCHISE_ID} для которой не найден подходящий лицензиар.")
                .addParam(ModelStorage.ErrorParamName.FRANCHISE_ID, FRANCHISE_ID_1)
        );
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validateFranchiseWhenLFPConstraintsL1F1P1AndModelL1F1ThenOk() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_ID_1)
                        )
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1)
            .parameterValues(KnownIds.FRANCHISE_PARAM_ID, "franchise", FRANCHISE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validateFranchiseWhenLFPConstraintsF1P1AndModelF1ThenOk() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(0)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_ID_1)
                        )
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.FRANCHISE_PARAM_ID, "franchise", FRANCHISE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    //endregion

    // region Валидация допустимости Персонажа в зависимости от Лицензиаров и Франшиз

    @Test
    public void validatePersonageWhenLFPConstraintsL1F1P1AndModelP1ThenError() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_ID_1)
                        )
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.PERSONAGE_PARAM_ID, "personage", PERSONAGE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Arrays.asList(
            new ModelValidationError(MODEL_ID_1, ModelValidationError.ErrorType.ILLEGAL_LICENSOR)
                .addLocalizedMessagePattern(
                    "Модель содержит персонажа %{PERSONAGE_ID} для которого не найдена подходящая франшиза.")
                .addParam(ModelStorage.ErrorParamName.PERSONAGE_ID, PERSONAGE_ID_1)
        );
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validatePersonageWhenLFPConstraintsIsEmptyAndModelP1ThenOk() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.PERSONAGE_PARAM_ID, "personage", PERSONAGE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validatePersonageWhenLFPConstraintsP1AndModelP1ThenOk() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(0)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(0)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_ID_1)
                        )
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.PERSONAGE_PARAM_ID, "personage", PERSONAGE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validatePersonageWhenLFPConstraintsL1F1P1AndP1AndModelP1ThenOk() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_ID_1)
                        )
                )
                .build()
        );
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(0)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(0)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_ID_1)
                        )
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.PERSONAGE_PARAM_ID, "personage", PERSONAGE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Collections.emptyList();
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    @Test
    public void validatePersonageWhenLFPConstraintsL1F1AndL2F1P1AndModelL1F1P1ThenError() {
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_1)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                )
                .build()
        );
        addLicensorConstraint(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_ID_2)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_ID_1)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_ID_1)
                        )
                )
                .build()
        );

        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID_1, CATEGORY_ID_1, VENDOR_ID_1)
            .parameterValues(KnownIds.LICENSOR_PARAM_ID, "licensor", LICENSOR_ID_1)
            .parameterValues(KnownIds.FRANCHISE_PARAM_ID, "franchise", FRANCHISE_ID_1)
            .parameterValues(KnownIds.PERSONAGE_PARAM_ID, "personage", PERSONAGE_ID_1)
            .getModel();

        ModelChanges modelChanges = new ModelChanges(null, model, ModelChanges.Operation.CREATE);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges, updatedModels);

        List<ModelValidationError> expectedErrors = Arrays.asList(
            new ModelValidationError(MODEL_ID_1, ModelValidationError.ErrorType.ILLEGAL_LICENSOR)
                .addLocalizedMessagePattern(
                    "Модель содержит персонажа %{PERSONAGE_ID} для которого не найдена подходящая франшиза.")
                .addParam(ModelStorage.ErrorParamName.PERSONAGE_ID, PERSONAGE_ID_1)
        );
        Assert.assertEquals(expectedErrors, actualErrors);
    }

    private void addLicensorConstraint(MboLicensors.Licensor licensor) {
        licensors.add(licensor);
    }

    // endregion
}
