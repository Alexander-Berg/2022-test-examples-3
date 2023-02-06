package ru.yandex.market.psku.postprocessor;

import java.util.Set;

import ru.yandex.market.mbo.http.ModelStorage;

public class ModelsGenerator {

    private ModelsGenerator() {}

    public static ModelStorage.Model generateModelWithRelations(long modelId, ModelStorage.RelationType relationType,
                                                          Set<Long> relationIds,
                                                          long categoryId) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder().setId(modelId);

        relationIds.forEach(relationId -> builder.addRelations(ModelStorage.Relation
                .newBuilder()
                .setType(relationType)
                .setId(relationId)
                .setCategoryId(categoryId)
                .build()));
        return builder.build();
    }
}
