package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;

/**
 * Tests of {@link SignModificationsPreprocessor}.
 *
 * @author s-ermakov
 * @author pochemuto
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SignModificationsPreprocessorTest extends BasePreprocessorTest {
    private static final Long SIGN_TRUE_ID = 1001L;
    private static final Long SIGN_FALSE_ID = 1002L;

    private static final CategoryEntities CATEGORY_ENTITIES_1 = new CategoryEntities();
    private static final CategoryEntities CATEGORY_ENTITIES_2 = new CategoryEntities();
    private static final CategoryEntities CATEGORY_ENTITIES_3 = new CategoryEntities();

    private SignModificationsPreprocessor signModificationsPreprocessor;

    static {
        CATEGORY_ENTITIES_1.setHid(CATEGORY_HID);
        CATEGORY_ENTITIES_1.addParameter(CategoryParamBuilder.newBuilder(1, XslNames.OPERATOR_SIGN)
            .setType(Param.Type.BOOLEAN)
            .setUseForGuru(true)
            .addOption(OptionBuilder.newBuilder(SIGN_TRUE_ID).addName("TRUE"))
            .addOption(OptionBuilder.newBuilder(SIGN_FALSE_ID).addName("falSe"))
            .build());
        CATEGORY_ENTITIES_1.addParameter(
            CategoryParamBuilder.newBuilder(2, XslNames.PREVIEW)
                .setUseForGuru(true)
                .setType(Param.Type.STRING)
                .build()
        );

        CATEGORY_ENTITIES_2.setHid(CATEGORY_HID + 1);
        CATEGORY_ENTITIES_2.addParameter(CategoryParamBuilder.newBuilder(1, XslNames.OPERATOR_SIGN)
            .setType(Param.Type.BOOLEAN)
            .setUseForGuru(true)
            .addOption(OptionBuilder.newBuilder(SIGN_TRUE_ID).addName("TRUE"))
            .addOption(OptionBuilder.newBuilder(SIGN_FALSE_ID).addName("falSe"))
            .build());
        CATEGORY_ENTITIES_2.addParameter(
            CategoryParamBuilder.newBuilder(2, XslNames.PREVIEW)
                .setUseForGuru(true)
                .setType(Param.Type.STRING)
                .build()
        );
        CATEGORY_ENTITIES_2.addParameter(
            CategoryParamBuilder.newBuilder(3, XslNames.DESCRIPTION)
                .setUseForGuru(true)
                .setBindingParam(true)
                .setType(Param.Type.STRING)
                .build()
        );

        CATEGORY_ENTITIES_3.setHid(CATEGORY_HID + 2);
        CATEGORY_ENTITIES_3.addParameter(CategoryParamBuilder.newBuilder(1, XslNames.OPERATOR_SIGN)
            .setType(Param.Type.BOOLEAN)
            .setUseForGuru(true)
            .addOption(OptionBuilder.newBuilder(SIGN_TRUE_ID).addName("TRUE"))
            .addOption(OptionBuilder.newBuilder(SIGN_FALSE_ID).addName("falSe"))
            .build());
        CATEGORY_ENTITIES_3.addParameter(
            CategoryParamBuilder.newBuilder(2, XslNames.URL)
                .setUseForGuru(true)
                .setType(Param.Type.STRING)
                .setBindingParam(true)
                .build()
        );
    }

    @Before
    public void before() {
        super.before();
        signModificationsPreprocessor = new SignModificationsPreprocessor(categoryParametersServiceClient);
    }

    @Override
    protected void createCategoryParametersServiceClient() {
        categoryParametersServiceClient = CategoryParametersServiceClientStub.ofCategoryEntities(
            CATEGORY_ENTITIES_1,
            CATEGORY_ENTITIES_2,
            CATEGORY_ENTITIES_3
        );
    }

    @Test
    public void modificationsWontAffectsIfModelDoesntContainSignParamValue() {
        CommonModel modelInStorage = model(1);
        CommonModel modificationInStorage = model(2, builder -> builder.parentModelId(1));
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(modelInStorage, modificationInStorage);

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId()))
            .isEqualToComparingFieldByField(modelInStorage);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId()))
            .isEqualToComparingFieldByField(modificationInStorage);
    }

    @Test
    public void modificationsWontAffectsIfModelHasFalseSignParam() {
        CommonModel modelInStorage = signed(model(1));
        CommonModel modificationInStorage = model(2, builder -> builder.parentModelId(1));
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(unsigned(modelInStorage), modificationInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(m -> !isSigned(m));
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId()))
            .isEqualToComparingFieldByField(modificationInStorage);
    }

    @Test
    public void modificationsWontAffectsIfModelWasSigned() {
        CommonModel modelInStorage = signed(model(1));
        CommonModel modificationInStorage = model(2, builder -> builder.parentModelId(1));
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(modelInStorage, modificationInStorage);

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId()))
            .isEqualToComparingFieldByField(modelInStorage);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId()))
            .isEqualToComparingFieldByField(modificationInStorage);
    }

    @Test
    public void modificationsWontAffectIfTheyHasTrueSigns() {
        CommonModel modelInStorage = unsigned(model(1));
        CommonModel modificationInStorage = model(2, builder ->
            builder.parentModelId(1)
                   .putParameterValues(signValue(true))
        );
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), modificationInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId()))
            .isEqualToComparingFieldByField(modificationInStorage);
    }

    @Test
    public void modificationArentAffectedIfNoBinding() {
        CommonModel modelInStorage = model(1, CATEGORY_HID + 1, builder ->
            builder.putParameterValues(signValue(false)));

        CommonModel modificationInStorage = model(2, CATEGORY_HID + 1, builder -> builder.parentModelId(1));
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), modificationInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId()))
            .isEqualToComparingFieldByField(modificationInStorage);
    }

    @Test
    public void modificationAffectedIfHasBinding() {
        CommonModel modelInStorage = model(1, CATEGORY_HID + 1, builder ->
            builder.putParameterValues(signValue(false)));

        CommonModel modificationInStorage = model(2, CATEGORY_HID + 1, builder ->
            builder.parentModelId(1)
                   .putParameterValues(stringParamValue(3, XslNames.DESCRIPTION))
        );
        CommonModel modificationInStorage2 = model(3, CATEGORY_HID + 1, builder ->
            builder.parentModelId(1)
        );

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), modificationInStorage, modificationInStorage2),
            ImmutableList.of(modelInStorage, modificationInStorage, modificationInStorage2));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId())).matches(this::isSigned);
    }

    @Test
    public void bindingParamsInheritedFromModel() {
        CommonModel modelInStorage = model(1, CATEGORY_HID + 1, builder ->
            builder.putParameterValues(signValue(false))
                   .putParameterValues(stringParamValue(3, XslNames.DESCRIPTION))
        );

        CommonModel modificationInStorage = model(2, CATEGORY_HID + 1, builder ->
            builder.parentModelId(1)
        );
        CommonModel modificationInStorage2 = model(3, CATEGORY_HID + 1, builder ->
            builder.parentModelId(1)
        );

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), modificationInStorage, modificationInStorage2),
            ImmutableList.of(modelInStorage, modificationInStorage, modificationInStorage2));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage2.getId())).matches(this::isSigned);
    }

    @Test
    public void testNonInheritableBindingParams() {
        CommonModel modelInStorage = model(1, CATEGORY_HID + 2, builder ->
            builder.putParameterValues(signValue(false))
                .putParameterValues(stringParamValue(2, XslNames.URL))
        );

        CommonModel modificationInStorage = model(2, CATEGORY_HID + 2, builder ->
            builder.parentModelId(1)
                .putParameterValues(stringParamValue(2, XslNames.URL))
        );
        CommonModel modificationInStorage2 = model(3, CATEGORY_HID + 2, builder ->
            builder.parentModelId(1)
        );

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), modificationInStorage, modificationInStorage2),
            ImmutableList.of(modelInStorage, modificationInStorage, modificationInStorage2));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId())).matches(this::isSigned);
    }

    @Test
    public void modificationsWillSetSignToTrueFromFalse() {
        CommonModel modelInStorage = unsigned(model(1));
        CommonModel modificationInStorage = model(2, builder ->
            builder.parentModelId(1)
                   .putParameterValues(signValue(false))
        );

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), modificationInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId())).matches(this::isSigned);
    }

    @Test
    public void modificationsWillSetSignToTrueFromEmpty() {
        CommonModel modelInStorage = unsigned(model(1));
        CommonModel modificationInStorage = model(2, builder -> builder.parentModelId(1));
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), modificationInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId())).matches(this::isSigned);
    }

    @Test
    public void deletedModificationsWontAffectedAtAll() {
        CommonModel modelInStorage = unsigned(model(1));
        CommonModel modification = model(2, builder -> builder.parentModelId(1));
        CommonModel notDeletedModification = model(3, builder -> builder.parentModelId(1));
        CommonModel newDeletedModification = new CommonModel(notDeletedModification);
        newDeletedModification.setDeleted(true);
        CommonModel storageDeletedModification = model(4, builder -> builder
            .setDeleted(true)
            .parentModelId(1));

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), modification, newDeletedModification, storageDeletedModification),
            ImmutableList.of(modelInStorage, modification, notDeletedModification, storageDeletedModification));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modification.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(newDeletedModification.getId()))
            .isEqualToComparingFieldByField(newDeletedModification);
        Assertions.assertThat(modelSaveGroup.getById(storageDeletedModification.getId()))
            .isEqualToComparingFieldByField(storageDeletedModification);
    }

    @Test
    public void testNewModificationsAlsoBeChanged() {
        CommonModel modelInStorage = unsigned(model(1));
        CommonModel newModification = model(0, builder -> builder.parentModelId(1));

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), newModification),
            ImmutableList.of(modelInStorage));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel newModel = modelSaveGroup.getModels().stream()
            .filter(CommonModel::isNewModel)
            .findFirst().orElseThrow(() -> new IllegalStateException("No new models!!"));
        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(newModel).matches(this::isSigned);
    }

    @Test
    public void testCorrectWorkWithSeveralNewModelsInBatch() {
        CommonModel modelInStorage = unsigned(model(1));
        CommonModel modificationInStorage = model(2, builder -> builder.parentModelId(1));
        CommonModel newModel1 = model(0);
        CommonModel newModel2 = model(0);
        CommonModel newModel3 = model(0);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(signed(modelInStorage), modificationInStorage, newModel1, newModel2, newModel3),
            ImmutableList.of(modelInStorage, modificationInStorage));

        signModificationsPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId())).matches(this::isSigned);
        Assertions.assertThat(modelSaveGroup.getById(modificationInStorage.getId())).matches(this::isSigned);
    }

    private boolean isSigned(CommonModel commonModel) {
        return commonModel.getFlatParameterValues().stream()
            .anyMatch(pv -> pv.getXslName().equals(XslNames.OPERATOR_SIGN)
                && pv.getBooleanValue() && pv.getOptionId().equals(SIGN_TRUE_ID));
    }

    private static ParameterValues signValue(boolean value) {
        ParameterValues parameterValues = new ParameterValues(1, XslNames.OPERATOR_SIGN, Param.Type.BOOLEAN,
            value, value ? SIGN_TRUE_ID : SIGN_FALSE_ID);
        parameterValues.setModificationSource(ModificationSource.OPERATOR_FILLED);
        parameterValues.setLastModificationUid(USER_ID + 1); // добавляю 1, точно не совпадало с USER_ID
        parameterValues.setLastModificationDate(new Date());
        return parameterValues;
    }

    private ParameterValues stringParamValue(long paramId, String xslName) {
        return new ParameterValues(paramId, xslName, Param.Type.STRING,
            WordUtil.defaultWord("test"));
    }

    private CommonModel signed(CommonModel model) {
        return addSign(model, true);
    }

    private CommonModel unsigned(CommonModel model) {
        return addSign(model, false);
    }

    private CommonModel addSign(CommonModel model, boolean sign) {
        CommonModel result = new CommonModel(model);
        result.removeAllParameterValues(XslNames.OPERATOR_SIGN);
        result.putParameterValues(signValue(sign));
        return result;
    }
}
