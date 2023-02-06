package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;

@SuppressWarnings("checkstyle:magicNumber")
public class DeletedModelsPreprocessorTest extends BasePreprocessorTest {
    private DeletedModelsPrerocessor deletedModelsPrerocessor;

    @Before
    public void before() {
        super.before();
        deletedModelsPrerocessor = new DeletedModelsPrerocessor();
    }

    @Test
    public void testModelDeleted() {
        CommonModel model = model(1);
        CommonModel modification = modification(2, 1);
        CommonModel sku = generatedSku(3, model);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(deleted(model), modification, sku),
            ImmutableList.of(model, modification, sku));

        deletedModelsPrerocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(model.getId()).isDeleted()).isTrue();
        Assertions.assertThat(modelSaveGroup.getById(modification.getId()).isDeleted()).isTrue();
        Assertions.assertThat(modelSaveGroup.getById(sku.getId()).isDeleted()).isTrue();
    }

    @Test
    public void testModelRelationsDeleted() {
        CommonModel model = model(1);
        CommonModel modification = modification(2, 1);
        CommonModel sku = generatedSku(3, model);

        CommonModel model2 = model(4);
        CommonModel modification2 = modification(5, 4);
        CommonModel sku2 = generatedSku(6, model2);

        makeRelations(model, model2);
        makeRelations(modification, modification2);
        makeRelations(sku, sku2);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(deleted(model), modification, sku, model2, modification2, sku2),
            ImmutableList.of(model, modification, sku, model2, modification2, sku2));

        deletedModelsPrerocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(model2.getId()).getRelations())
            .noneMatch(r -> r.getType() == ModelRelation.RelationType.SYNC_SOURCE);
        Assertions.assertThat(modelSaveGroup.getById(modification2.getId()).getRelations())
            .noneMatch(r -> r.getType() == ModelRelation.RelationType.SYNC_SOURCE);
        Assertions.assertThat(modelSaveGroup.getById(sku2.getId()).getRelations())
            .noneMatch(r -> r.getType() == ModelRelation.RelationType.SYNC_SOURCE);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testModelAndRelationsDeletedOnCategoryChange() {
        CommonModel model = model(1);
        CommonModel model2 = model(4);
        CommonModel model3 = model(1);
        model3.setCategoryId(2);
        model.addRelation(new ModelRelation(model2.getId(), model2.getCategoryId(),
            ModelRelation.RelationType.EXPERIMENTAL_MODEL));

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(model, model2),
            ImmutableList.of(model3, model2));
        Assertions.assertThat(modelSaveGroup.getById(model.getId()).getRelations().size()).isEqualTo(1);
        Assertions.assertThat(modelSaveGroup.getById(model2.getId()).isDeleted()).isFalse();

        deletedModelsPrerocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(model.getId()).getRelations().size()).isEqualTo(0);
        Assertions.assertThat(modelSaveGroup.getById(model2.getId()).isDeleted()).isTrue();
    }

    public CommonModel deleted(CommonModel model) {
        return new CommonModel(model).setDeleted(true);
    }

    public CommonModel generatedSku(long id, CommonModel model) {
        CommonModel sku = sku(id, model);
        sku.setCurrentType(CommonModel.Source.GENERATED_SKU);
        return sku;
    }

    public void makeRelations(CommonModel syncSource, CommonModel syncTarget) {
        syncSource.addRelation(
            new ModelRelation(syncTarget.getId(), syncTarget.getCategoryId(), ModelRelation.RelationType.SYNC_TARGET));
        syncTarget.addRelation(
            new ModelRelation(syncSource.getId(), syncSource.getCategoryId(), ModelRelation.RelationType.SYNC_SOURCE));
    }
}
