package ru.yandex.market.gutgin.tms.service.goodcontent;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;

public class ModelsSavingContextTest {
    @Test
    public void whenContextWithLinkedRelationsShouldBeValid() {
        ModelsSavingContext context = new ModelsSavingContext();
        ModelStorage.Model model1 = ModelStorage.Model.newBuilder()
                .setId(1L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(11L)
                        .setType(ModelStorage.RelationType.SKU_MODEL)
                        .build()
                )
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(12L)
                        .setType(ModelStorage.RelationType.SKU_MODEL)
                        .build()
                )
                .build();
        ModelStorage.Model model2 = ModelStorage.Model.newBuilder()
                .setId(1L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(21L)
                        .setType(ModelStorage.RelationType.SKU_MODEL)
                        .build()
                )
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(22L)
                        .setType(ModelStorage.RelationType.SKU_MODEL)
                        .build()
                )
                .build();
        ModelStorage.Model psku1 = ModelStorage.Model.newBuilder()
                .setId(11L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(1L)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build()
                )
                .build();
        ModelStorage.Model psku2 = ModelStorage.Model.newBuilder()
                .setId(12L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(1L)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build()
                )
                .build();
        ModelStorage.Model psku3 = ModelStorage.Model.newBuilder()
                .setId(21L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(2L)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build()
                )
                .build();
        ModelStorage.Model psku4 = ModelStorage.Model.newBuilder()
                .setId(22L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(2L)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build()
                )
                .build();
        context.updatePModels(ImmutableMap.of(
                1L, model1,
                2L, model2
        ));
        context.updatePSkus(ImmutableMap.of(
                11L, psku1,
                12L, psku2,
                21L, psku3,
                22L, psku4
        ));
        Assert.assertTrue(context.isValid());
    }

    @Test
    public void whenContextWithNoRelationsFromModelToSkuShouldNotBeValid() {
        ModelsSavingContext context = new ModelsSavingContext();
        ModelStorage.Model model1 = ModelStorage.Model.newBuilder()
                .setId(1L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(11L)
                        .setType(ModelStorage.RelationType.SKU_MODEL)
                        .build()
                )
                .build();
        ModelStorage.Model psku1 = ModelStorage.Model.newBuilder()
                .setId(11L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(1L)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build()
                )
                .build();
        ModelStorage.Model psku2 = ModelStorage.Model.newBuilder()
                .setId(12L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(1L)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build()
                )
                .build();
        context.updatePModels(ImmutableMap.of(
                1L, model1
        ));
        context.updatePSkus(ImmutableMap.of(
                11L, psku1,
                12L, psku2
        ));
        Assert.assertFalse(context.isValid());
    }

    @Test
    public void whenContextWithNoRelationsFromSkuToModelShouldNotBeValid() {
        ModelsSavingContext context = new ModelsSavingContext();
        ModelStorage.Model model1 = ModelStorage.Model.newBuilder()
                .setId(1L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(11L)
                        .setType(ModelStorage.RelationType.SKU_MODEL)
                        .build()
                )
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(12L)
                        .setType(ModelStorage.RelationType.SKU_MODEL)
                        .build()
                )
                .build();
        ModelStorage.Model psku1 = ModelStorage.Model.newBuilder()
                .setId(11L)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(1L)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build()
                )
                .build();
        ModelStorage.Model psku2 = ModelStorage.Model.newBuilder()
                .setId(12L)
                .build();
        context.updatePModels(ImmutableMap.of(
                1L, model1
        ));
        context.updatePSkus(ImmutableMap.of(
                11L, psku1,
                12L, psku2
        ));
        Assert.assertFalse(context.isValid());
    }
}
