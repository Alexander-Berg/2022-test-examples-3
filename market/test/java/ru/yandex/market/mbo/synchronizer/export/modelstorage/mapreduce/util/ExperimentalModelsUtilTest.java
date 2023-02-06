package ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.util;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.test.util.random.RandomBean;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.util.ExperimentalModelsUtil.withNoRelationsToExperimentalModels;

/**
 * @author jkt on 06.02.18.
 */
public class ExperimentalModelsUtilTest {

    private static final List<Long> EXP_MODEL_IDS = Arrays.asList(123456L, 234567L);
    private static final int CATEGORY_ID = 11111;
    private static final int BASE_MODEL_ID = 98765;

    @Test
    public void whenRemovingRelationShouldRemoveOnlyExperimentalModelRelationFromBaseModel() {
        final ModelStorage.Model model = generateModel();

        List<ModelStorage.Relation> experimentModelRelations = generateRelation(
            ModelStorage.RelationType.EXPERIMENTAL_MODEL,
            EXP_MODEL_IDS);

        ModelStorage.Model modelWithExpRelations = model.toBuilder()
            .addAllRelations(experimentModelRelations)
            .build();

        ModelStorage.Model modelWithNoExp = ExperimentalModelsUtil
            .withNoRelationsToExperimentalModels(modelWithExpRelations, EXP_MODEL_IDS);

        assertSoftly(softly -> {
            softly.assertThat(modelWithNoExp.getRelationsList()).containsOnlyElementsOf(model.getRelationsList());
            softly.assertThat(modelWithNoExp.getRelationsList()).doesNotContainAnyElementsOf(experimentModelRelations);
            softly.assertThat(modelWithNoExp).isEqualTo(model);
        });
    }

    @Test
    public void whenDoesNotRelateToThisModelShouldModifyNothing() {
        ModelStorage.Model model = generateModel();

        ModelStorage.Model modifiedModel = withNoRelationsToExperimentalModels(model, EXP_MODEL_IDS);

        assertThat(modifiedModel.getRelationsList()).containsOnlyElementsOf(model.getRelationsList());
    }

    @Test
    public void whenRelationsNotPresentShouldNotFail() {
        ModelStorage.Model model = generateModel().toBuilder().clearRelations().build();

        assertThatCode(() -> {
            withNoRelationsToExperimentalModels(model, EXP_MODEL_IDS);
        }).doesNotThrowAnyException();
    }

    @Test
    public void whenExpModelIdsIsNullShouldModifyNothing() {
        ModelStorage.Model model = generateModel();

        ModelStorage.Model modifiedModel = withNoRelationsToExperimentalModels(model, null);

        assertThat(modifiedModel.getRelationsList()).containsOnlyElementsOf(model.getRelationsList());
    }

    @Test
    public void whenExpModelIdsIsEmptyShouldModifyNothing() {
        ModelStorage.Model model = generateModel();

        ModelStorage.Model modifiedModel = withNoRelationsToExperimentalModels(model, Collections.emptyList());

        assertThat(modifiedModel.getRelationsList()).containsOnlyElementsOf(model.getRelationsList());
    }

    private ModelStorage.Model generateModel() {
        return ModelStorage.Model.newBuilder()
            .setId(BASE_MODEL_ID)
            .setParentId(RandomBean.generate(Long.class))
            .setCategoryId(CATEGORY_ID)
            .setVendorId(RandomBean.generate(Long.class))
            .setSourceType(CommonModel.Source.GURU.name())
            .addTitles(randomLocalizedString())
            .addAliases(randomLocalizedString())
            .addParameterValues(ModelStorage.ParameterValue.newBuilder().addStrValue(randomLocalizedString()).build())
            .setPublished(true)
            .setDeleted(false)
            .setChecked(true)
            .addClusterizerOfferIds(randomLocalizedString().getValue())
            .addAllRelations(generateRelationsOfAllTypes())
            .build();
    }

    @NotNull
    private ModelStorage.LocalizedString randomLocalizedString() {
        return ModelStorage.LocalizedString.newBuilder().setValue(RandomBean.generate(String.class)).build();
    }

    private List<ModelStorage.Relation> generateRelationsOfAllTypes() {
        return Stream.of(ModelStorage.RelationType.values())
            .map(type -> generateRelation(type, Collections.singletonList(RandomBean.generate(Long.class))))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<ModelStorage.Relation> generateRelation(ModelStorage.RelationType type, List<Long> ids) {
        return ids.stream()
            .map(id -> ModelStorage.Relation.newBuilder()
                .setId(id)
                .setType(type)
                .setCategoryId(CATEGORY_ID)
                .build())
            .collect(Collectors.toList());

    }
}
