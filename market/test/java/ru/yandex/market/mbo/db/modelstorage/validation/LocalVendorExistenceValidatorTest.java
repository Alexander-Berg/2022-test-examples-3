package ru.yandex.market.mbo.db.modelstorage.validation;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.modelstorage.stubs.GroupStorageUpdatesStub;
import ru.yandex.market.mbo.db.params.guru.BaseGuruServiceImpl;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ayratgdl
 * @date 30.08.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LocalVendorExistenceValidatorTest {
    private static final long MODEL_ID = 101;
    private static final long MODIFICATION_ID_1 = 102;
    private static final long MODIFICATION_ID_2 = 103;
    private static final long CATEGORY_ID = 201;
    private static final long NEW_CATEGORY_ID = 202;
    private static final long NONGURU_VENDOR_ID = 301;
    private static final long GURU_VENDOR_ID = 302;
    private static final long MISSING_VENDOR_ID = 404;

    private LocalVendorExistenceValidator validator;
    private BaseGuruServiceImpl guruService;
    private ModelValidationContextStub context;
    private GroupStorageUpdatesStub groupStorageUpdatesStub;

    @Before
    public void setUp() throws Exception {
        validator = new LocalVendorExistenceValidator();

        guruService = new BaseGuruServiceImpl();
        guruService.addVendor(GURU_VENDOR_ID, GURU_VENDOR_ID * 2, CATEGORY_ID, true);
        guruService.addVendor(NONGURU_VENDOR_ID, NONGURU_VENDOR_ID * 2, CATEGORY_ID, false);
        guruService.addVendor(GURU_VENDOR_ID, GURU_VENDOR_ID * 2, NEW_CATEGORY_ID, true);
        guruService.addVendor(NONGURU_VENDOR_ID, NONGURU_VENDOR_ID * 2, NEW_CATEGORY_ID, false);

        groupStorageUpdatesStub = new GroupStorageUpdatesStub();

        context = new ModelValidationContextStub(null);
        context.setGuruService(guruService);
        context.setStatsModelStorageService(groupStorageUpdatesStub);
    }

    @Test
    public void ifGuruModelHasAnyGuruVendorThenOK() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, GURU_VENDOR_ID)
            .currentType(CommonModel.Source.GURU)
            .endModel();
        ModelChanges modelChanges = new ModelChanges(null, model);

        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(model));

        Assertions.assertThat(actualErrors).isEmpty();
    }

    @Test
    public void ifVendorModelHasAnyGuruVendorThenOK() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, GURU_VENDOR_ID)
            .currentType(CommonModel.Source.VENDOR)
            .endModel();
        ModelChanges modelChanges = new ModelChanges(null, model);

        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(model));

        Assertions.assertThat(actualErrors).isEmpty();
    }

    @Test
    public void ifModelIsNewAndHasNonGuruVendorThenError() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, NONGURU_VENDOR_ID).endModel();
        ModelChanges modelChanges = new ModelChanges(null, model);

        // NONGURU_VENDOR_ID isn't contained in guru vendors of CATEGORY_ID
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(model));

        Assertions.assertThat(actualErrors)
            .containsExactly(
                missingGuruVendorError(true)
            );
    }

    @Test
    public void ifModelHasNotChangedAndHasNonGuruVendorThenNonCriticalError() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, NONGURU_VENDOR_ID).endModel();
        ModelChanges modelChanges = new ModelChanges(model, model); // model doesn't changed

        // NONGURU_VENDOR_ID isn't contained in guru vendors of CATEGORY_ID
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(model));

        Assertions.assertThat(actualErrors)
            .containsExactly(
                missingGuruVendorError(false)
            );
    }

    @Test
    public void ifModelHasNotChangedAndHasNonGuruVendorButCategoryChanged() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, NONGURU_VENDOR_ID).endModel();
        CommonModel changedCategoryModel = new CommonModel(model);
        changedCategoryModel.setCategoryId(NEW_CATEGORY_ID);
        ModelChanges modelChanges = new ModelChanges(model, changedCategoryModel); // model doesn't changed

        // NONGURU_VENDOR_ID isn't contained in guru vendors of CATEGORY_ID
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(changedCategoryModel));

        Assertions.assertThat(actualErrors)
            .containsExactly(
                missingGuruVendorError(true, NEW_CATEGORY_ID)
            );
    }

    @Test
    public void ifModificationVendorNotEqualsToModelThenError() {
        // assume
        CommonModel beforeModel = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, GURU_VENDOR_ID).endModel();
        CommonModel beforeModif1 = CommonModelBuilder.newBuilder(MODIFICATION_ID_1, CATEGORY_ID, GURU_VENDOR_ID)
            .parentModelId(MODEL_ID).endModel();
        CommonModel beforeModif2 = CommonModelBuilder.newBuilder(MODIFICATION_ID_2, CATEGORY_ID, GURU_VENDOR_ID)
            .parentModelId(MODEL_ID).endModel();
        groupStorageUpdatesStub.putToStorage(Arrays.asList(beforeModel, beforeModif1, beforeModif2));

        // assert
        CommonModel afterModel = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, GURU_VENDOR_ID).endModel();
        CommonModel afterModif2 = CommonModelBuilder.newBuilder(MODIFICATION_ID_2, CATEGORY_ID, NONGURU_VENDOR_ID)
            .parentModelId(MODEL_ID)
            .endModel();

        ModelChanges modelChanges = new ModelChanges(beforeModel, afterModel);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Arrays.asList(afterModel, afterModif2));

        // validate
        Assertions.assertThat(actualErrors)
            .containsExactly(
                inconsistentVendorError(afterModif2, afterModel, true)
            );
    }

    @Test
    public void ifModificationNotChangedAndHasVendorNotEqualsToModelThenNonCriticalError() {
        // assume
        CommonModel beforeModel = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, GURU_VENDOR_ID).endModel();
        CommonModel beforeModif1 = CommonModelBuilder.newBuilder(MODIFICATION_ID_1, CATEGORY_ID, NONGURU_VENDOR_ID)
            .parentModelId(MODEL_ID).endModel();
        CommonModel beforeModif2 = CommonModelBuilder.newBuilder(MODIFICATION_ID_2, CATEGORY_ID, GURU_VENDOR_ID)
            .parentModelId(MODEL_ID).endModel();
        groupStorageUpdatesStub.putToStorage(Arrays.asList(beforeModel, beforeModif1, beforeModif2));

        // assert
        CommonModel afterModel = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, GURU_VENDOR_ID).endModel();
        CommonModel afterModif1 = CommonModelBuilder.newBuilder(MODIFICATION_ID_1, CATEGORY_ID, NONGURU_VENDOR_ID)
            .parentModelId(MODEL_ID).endModel();

        ModelChanges modelChanges = new ModelChanges(beforeModel, afterModel);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Arrays.asList(afterModel, afterModif1));

        // validate
        Assertions.assertThat(actualErrors)
            .containsExactly(
                inconsistentVendorError(afterModif1, afterModel, false)
            );
    }

    @Test
    public void ifModelHasModificationWithNotEqualsVendorThenNonCriticalError() {
        // assume
        CommonModel beforeModel = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, GURU_VENDOR_ID).endModel();
        CommonModel beforeModif1 = CommonModelBuilder.newBuilder(MODIFICATION_ID_1, CATEGORY_ID, NONGURU_VENDOR_ID)
            .parentModelId(MODEL_ID).endModel();
        CommonModel beforeModif2 = CommonModelBuilder.newBuilder(MODIFICATION_ID_2, CATEGORY_ID, GURU_VENDOR_ID)
            .parentModelId(MODEL_ID).endModel();
        groupStorageUpdatesStub.putToStorage(Arrays.asList(beforeModel, beforeModif1, beforeModif2));

        // assert
        CommonModel afterModel = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, GURU_VENDOR_ID).endModel();
        ModelChanges modelChanges = new ModelChanges(beforeModel, afterModel);
        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singletonList(afterModel));

        // validate
        Assertions.assertThat(actualErrors)
            .containsExactly(
                inconsistentVendorError(beforeModif1, afterModel, false)
            );
    }

    @Test
    public void ifPartnerQualityModelIsNewAndHasNonGuruVendorThenOk() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, NONGURU_VENDOR_ID)
            .parameterValues(KnownIds.MODEL_QUALITY_PARAM_ID, XslNames.MODEL_QUALITY, 1L)
            .endModel();
        ModelChanges modelChanges = new ModelChanges(null, model);

        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(model));

        Assertions.assertThat(actualErrors).isEmpty();
    }

    @Test
    public void ifPartnerQualityModelIsNewAndHasMissingVendorThenError() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, MISSING_VENDOR_ID)
            .parameterValues(KnownIds.MODEL_QUALITY_PARAM_ID, XslNames.MODEL_QUALITY, 1L)
            .endModel();
        ModelChanges modelChanges = new ModelChanges(null, model);

        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(model));

        Assertions.assertThat(actualErrors)
            .containsExactly(
                missingLocalVendorError(true, CATEGORY_ID)
            );
    }

    @Test
    public void ifPartnerModelBecomesOperatorAndHasNonGuruVendorThenError() {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, NONGURU_VENDOR_ID)
            .parameterValues(KnownIds.MODEL_QUALITY_PARAM_ID, XslNames.MODEL_QUALITY, KnownIds.MODEL_QUALITY_OPERATOR)
            .endModel();
        CommonModel before = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID, NONGURU_VENDOR_ID)
            .parameterValues(KnownIds.MODEL_QUALITY_PARAM_ID, XslNames.MODEL_QUALITY, 1L)
            .endModel();
        ModelChanges modelChanges = new ModelChanges(before, model);

        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(model));

        Assertions.assertThat(actualErrors)
            .containsExactly(
                missingGuruVendorError(true)
            );
    }

    private ModelValidationError inconsistentVendorError(CommonModel modif, CommonModel model, boolean critical) {
        return new ModelValidationError(
            MODEL_ID, ModelValidationError.ErrorType.INVALID_MODIFICATION_VENDOR, critical)
            .addLocalizedMessagePattern("Вендор %{MODIFICATION_VENDOR_ID} модификации %{MODIFICATION_ID} " +
                "не соответствует вендору %{VENDOR_ID} модели %{MODEL_ID}")
            .addParam(ModelStorage.ErrorParamName.MODIFICATION_VENDOR_ID, modif.getVendorId())
            .addParam(ModelStorage.ErrorParamName.MODIFICATION_ID, modif.getId())
            .addParam(ModelStorage.ErrorParamName.VENDOR_ID, model.getVendorId())
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, model.getId());
    }

    private ModelValidationError missingGuruVendorError(boolean critical, long categoryId) {
        return new ModelValidationError(MODEL_ID, ModelValidationError.ErrorType.MISSING_LOCAL_VENDOR, critical)
            .addParam(ModelStorage.ErrorParamName.VENDOR_ID, NONGURU_VENDOR_ID)
            .addParam(ModelStorage.ErrorParamName.CATEGORY_ID, categoryId)
            .addLocalizedMessagePattern(
                "Несуществующий гуру вендор для глобального '%{VENDOR_ID}' в категории '%{CATEGORY_ID}'.");
    }

    private ModelValidationError missingGuruVendorError(boolean critical) {
        return missingGuruVendorError(critical, CATEGORY_ID);
    }

    private ModelValidationError missingLocalVendorError(boolean critical, long categoryId) {
        return new ModelValidationError(MODEL_ID, ModelValidationError.ErrorType.MISSING_LOCAL_VENDOR, critical)
            .addParam(ModelStorage.ErrorParamName.VENDOR_ID, MISSING_VENDOR_ID)
            .addParam(ModelStorage.ErrorParamName.CATEGORY_ID, categoryId)
            .addLocalizedMessagePattern(
                "Вендор '%{VENDOR_ID}' не найден в категории '%{CATEGORY_ID}'.");
    }
}
