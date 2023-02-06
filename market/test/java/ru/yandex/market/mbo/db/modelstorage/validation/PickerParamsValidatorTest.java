package ru.yandex.market.mbo.db.modelstorage.validation;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModelUtils;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;
import ru.yandex.market.mbo.gwt.models.params.SubType;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PickerParamsValidatorTest {

    private static final long SKU_ID = 101167600001L;
    private static final long GURU_ID = 101167600000L;
    private static final long CATEGORY_ID = 950132L;

    private static final long COLOUR_PARAM_ID = 12L;
    private static final long TASTE_PARAM_ID = 13L;

    private static final long PURPLE = 900L;
    private static final long GOLDEN = 585L;
    private static final long CHICKEN = 700L;
    private static final long FRENCH_FRIES = 701L;


    private CategoryParam paramColor;
    private CategoryParam paramTaste;
    private PickerParamsValidator validator;
    private ModelValidationContext context;
    private CommonModel sku;
    private CommonModel guru;
    List<ModelValidationError> errors;

    @Before
    public void setup() {
        validator = new PickerParamsValidator();
        paramColor = createCategoryParam(COLOUR_PARAM_ID, Arrays.asList(
            createOption(PURPLE, "Пурпурный", COLOUR_PARAM_ID),
            createOption(GOLDEN, "Золотой", COLOUR_PARAM_ID)
        ));
        paramTaste = createCategoryParam(TASTE_PARAM_ID, Arrays.asList(
            createOption(CHICKEN, "Куриный", TASTE_PARAM_ID),
            createOption(FRENCH_FRIES, "Картофель-фри", TASTE_PARAM_ID)
        ));
        sku = createBasicSku().getModel();
        guru = createBasicGuru(sku).getModel();
        sku.addRelation(CommonModelUtils.getRelationToModel(guru));

        context = mock(ModelValidationContextStub.class);
        when(context.getImagePickerParamIds(anyLong())).thenReturn(ImmutableSet.of(COLOUR_PARAM_ID, TASTE_PARAM_ID));
        when(context.getReadableParameterName(anyLong(), anyLong())).thenReturn("Any param name");
        when(context.getDumpModel(any(CommonModel.class), any())) //тут нам вообще пофиг что вернётся, лишь бы не null
            .thenReturn(SkuBuilderHelper.getGuruBuilder().getRawModel());
        when(context.getModel(anyLong(), anyLong(), anyCollection())).thenCallRealMethod();
        when(context.getBaseModel(any(CommonModel.class), anyCollection())).thenCallRealMethod();
        when(context.getRootModel(any(CommonModel.class), anyCollection())).thenCallRealMethod();
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), anyCollection(), anyBoolean()))
            .thenReturn(new RelatedModelsContainer(Collections.singletonList(sku), Collections.emptyList(),
                ModelRelation.RelationType.SKU_MODEL));
    }

    @Test
    public void testSkuNothingFilled() {
        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testNoPickerChangesGuruIgnored() {
        errors = validator.validate(context, changesOf(guru, guru), groupOf(guru));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testGuruHypothesisPickerError() {
        addValue(sku, paramColor, GOLDEN);
        addPickerToValue(sku, guru, paramColor, GOLDEN);
        guru.getParameterValueLinks()
            .forEach(valueLink -> valueLink.setType(Param.Type.HYPOTHESIS));

        errors = validator.validate(context, changesOf(guru), groupOf(guru));

        assertErrors(errors, ModelValidationError.ErrorSubtype.HYPOTHESIS_PICKER_IMAGE, true);
    }

    @Test
    public void testGuruPickerRemoveError() {
        addValue(sku, paramColor, GOLDEN);
        CommonModel guruBefore = new CommonModel(guru);
        addPickerToValue(sku, guruBefore, paramColor, GOLDEN);

        errors = validator.validate(context, changesOf(guruBefore, guru), groupOf(guru));

        assertErrors(errors, true);
    }

    @Test
    public void testParamWithoutPickerOnGuruIgnoredOnSkuFailed() {
        addValue(guru, paramColor, GOLDEN);
        errors = validator.validate(context, changesOf(guru), groupOf(guru));
        assertThat(errors).isEmpty();

        addValue(sku, paramColor, GOLDEN);
        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertErrors(errors, true);
    }

    @Test
    public void testNotPublishedSku() {
        sku.setPublished(false);
        addValue(sku, paramColor, GOLDEN);
        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testParamWithoutPickerForceSaved() {
        addValue(sku, paramColor, GOLDEN);
        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertErrors(errors, true);

        fakeForcePicker();

        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testParamWithPickerPassed() {
        addValue(sku, paramColor, GOLDEN);
        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertErrors(errors, true);

        addPickerToValue(sku, guru, paramColor, GOLDEN);
        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testSeveralOptions() {
        addValue(sku, paramColor, GOLDEN);
        addValue(sku, paramColor, PURPLE);
        addValue(sku, paramTaste, CHICKEN);
        addValue(sku, paramTaste, FRENCH_FRIES);

        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertErrors(errors, true);

        addPickerToValue(sku, guru, paramColor, GOLDEN);
        addPickerToValue(sku, guru, paramTaste, CHICKEN);

        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertErrors(errors, true);

        addPickerToValue(sku, guru, paramColor, PURPLE);

        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertErrors(errors, true);

        addPickerToValue(sku, guru, paramTaste, FRENCH_FRIES);

        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testWithModification() {
        CommonModel root = CommonModelBuilder.newBuilder()
            .startModel()
            .id(GURU_ID + 1L)
            .category(CATEGORY_ID)
            .currentType(CommonModel.Source.GURU)
            .parameters(Arrays.asList(paramColor, paramTaste))
            .getModel();
        CommonModel modification = guru;
        modification.setParentModel(root);
        modification.setParentModelId(root.getId());

        addValue(sku, paramColor, GOLDEN);
        errors = validator.validate(context, changesOf(sku), groupOf(root, modification, sku));
        assertErrors(errors, true);

        addPickerToValue(sku, root, paramColor, GOLDEN);
        errors = validator.validate(context, changesOf(sku), groupOf(root, modification, sku));
        assertThat(errors).isEmpty();

        addValue(sku, paramTaste, CHICKEN);
        errors = validator.validate(context, changesOf(sku), groupOf(root, modification, sku));
        assertErrors(errors, true);

        fakeForcePicker();
        errors = validator.validate(context, changesOf(sku), groupOf(root, modification, sku));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testSkuBeingPublishedWithForce() {
        addValue(sku, paramColor, GOLDEN);
        CommonModel beforeSku = new CommonModel(sku);
        beforeSku.setPublished(false);
        beforeSku.setBluePublished(false);
        errors = validator.validate(context, changesOf(beforeSku, sku), groupOf(guru, sku));
        assertErrors(errors, true);

        fakeForcePicker();

        errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testSkuNotBeingPublished() {
        addValue(sku, paramColor, GOLDEN);
        errors = validator.validate(context, changesOf(sku, sku), groupOf(guru, sku));
        assertErrors(errors, false);
    }

    private CommonModelBuilder<CommonModel> createBasicSku() {
        return CommonModelBuilder.newBuilder()
            .startModel()
            .id(SKU_ID)
            .category(CATEGORY_ID)
            .currentType(CommonModel.Source.SKU)
            .published(true)
            .parameters(Arrays.asList(paramColor, paramTaste));
    }

    private CommonModelBuilder<CommonModel> createStorageSku() {
        return CommonModelBuilder.newBuilder()
            .startModel()
            .id(SKU_ID)
            .category(CATEGORY_ID)
            .currentType(CommonModel.Source.SKU)
            .published(true)
            .parameters(Arrays.asList(paramColor, paramTaste));
    }

    private CommonModelBuilder<CommonModel> createBasicGuru(CommonModel sku) {
        return CommonModelBuilder.newBuilder()
            .startModel()
            .id(GURU_ID)
            .category(CATEGORY_ID)
            .currentType(CommonModel.Source.GURU)
            .parameters(Arrays.asList(paramColor, paramTaste))

            .startModelRelation()
            .type(ModelRelation.RelationType.SKU_MODEL)
            .id(sku.getId())
            .categoryId(CATEGORY_ID)
            .model(sku)
            .endModelRelation();
    }

    private static CategoryParam createCategoryParam(long id, List<Option> options) {
        return CategoryParamBuilder.newBuilder()
            .setId(id)
            .setCategoryHid(CATEGORY_ID)
            .setType(Param.Type.ENUM)
            .setLevel(CategoryParam.Level.OFFER)
            .setMultifield(true)
            .setXslName("Param#" + id)
            .setSkuParameterMode(SkuParameterMode.SKU_DEFINING)
            .setOptions(options)
            .setSubtype(SubType.IMAGE_PICKER)
            .build();
    }

    private Option createOption(long id, String name, long paramId) {
        Option newOption = new OptionImpl(id, name);
        newOption.setParamId(paramId);
        return newOption;
    }

    private static Collection<CommonModel> groupOf(CommonModel... models) {
        return new ArrayList<>(Arrays.asList(models));
    }

    private static ModelChanges changesOf(CommonModel model) {
        return new ModelChanges(null, model);
    }

    private static ModelChanges changesOf(CommonModel before, CommonModel model) {
        return new ModelChanges(before, model);
    }

    private void addValue(CommonModel model, CategoryParam param, long optionId) {
        ParameterValue value = new ParameterValue(param, optionId);
        model.addParameterValue(value);
    }

    private void addPickerToValue(CommonModel model, CommonModel root, CategoryParam param, long optionId) {
        ParameterValue linkFromSku = model.getParameterValues(param.getId()).getNotEmptyValues()
            .stream()
            .filter(parameterValue -> parameterValue.getOptionId() == optionId)
            .findAny()
            .get();

        ParameterValue newLink = new ParameterValue(linkFromSku);
        newLink.setPickerImage(new PickerImage());
        root.addParameterValueLink(newLink);
    }

    private static void assertErrors(List<ModelValidationError> errors, boolean critical) {
        assertErrors(errors, ModelValidationError.ErrorSubtype.PICKER_IMAGE_MISSING, critical);
    }

    private static void assertErrors(List<ModelValidationError> errors,
                                     ModelValidationError.ErrorSubtype errorSubtype,
                                     boolean critical) {
        assertThat(errors).isNotEmpty();
        errors.forEach(error -> {
            assertThat(ModelValidationError.ErrorType.INVALID_PARAM_PICKER_IMAGE).isEqualTo(error.getType());
            assertThat(errorSubtype).isEqualTo(error.getSubtype());
            assertThat(error.isCritical()).isEqualTo(critical);
        });
    }

    private void fakeForcePicker() {
        when(context.isForcedPicker()).thenReturn(true);
    }
}
