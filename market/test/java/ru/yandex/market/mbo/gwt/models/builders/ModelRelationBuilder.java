package ru.yandex.market.mbo.gwt.models.builders;

import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

/**
 * Билдер для построения {@link ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation}.
 *
 * @author s-ermakov
 */
public class ModelRelationBuilder<T> {
    private CommonModelBuilder<T> modelBuilder;
    private ModelRelation modelRelation = new ModelRelation();

    private ModelRelationBuilder() {
    }

    public static ModelRelationBuilder<ModelRelationBuilder> newBuilder() {
        return new ModelRelationBuilder<>();
    }

    public static <T> ModelRelationBuilder<T> newBuilder(CommonModelBuilder<T> modelBuilder) {
        ModelRelationBuilder<T> builder = new ModelRelationBuilder<>();
        builder.modelBuilder = modelBuilder;
        return builder;
    }

    public ModelRelationBuilder<T> id(long id) {
        modelRelation.setId(id);
        return this;
    }

    public ModelRelationBuilder<T> categoryId(long categoryId) {
        modelRelation.setCategoryId(categoryId);
        return this;
    }

    public ModelRelationBuilder<T> type(ModelRelation.RelationType relationType) {
        modelRelation.setType(relationType);
        return this;
    }

    public ModelRelationBuilder<T> model(CommonModel model) {
        modelRelation.setModel(model);
        return this;
    }

    public ModelRelation build() {
        return modelRelation;
    }

    // dsl methods

    public CommonModelBuilder<ModelRelationBuilder<T>> startModel() {
        return CommonModelBuilder.builder(modelBuilder, model -> {
            modelRelation.setModel(model);
            return ModelRelationBuilder.this;
        });
    }

    public CommonModelBuilder<T> endModelRelation() {
        modelBuilder.getModel().addRelation(modelRelation);
        return modelBuilder;
    }
}
