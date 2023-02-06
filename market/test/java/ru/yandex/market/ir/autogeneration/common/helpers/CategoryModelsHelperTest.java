package ru.yandex.market.ir.autogeneration.common.helpers;

import org.junit.Test;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CategoryModelsHelperTest {
    @Test
    public void testReorderModelResponse() {
        Function<Integer, Function<ModelStorage.ModelType, Function<Boolean, ModelStorage.Model>>> modelCreator =
                id -> type -> hasSkuParam -> {
                    ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
                    builder.setId(id);
                    builder.setCurrentType(type.name());
                    if (hasSkuParam) {
                        builder.addParameterValuesBuilder()
                                .setXslName(CategoryData.IS_SKU)
                                .setValueType(MboParameters.ValueType.BOOLEAN)
                                .setBoolValue(true);
                    }
                    return builder.build();
                };

        ModelStorage.Model guru1 = modelCreator.apply(1).apply(ModelStorage.ModelType.GURU).apply(false);
        ModelStorage.Model guruAsSku1 = modelCreator.apply(1).apply(ModelStorage.ModelType.GURU).apply(true);
        ModelStorage.Model sku1 = modelCreator.apply(1).apply(ModelStorage.ModelType.SKU).apply(false);
        ModelStorage.Model guruAsSku2 = modelCreator.apply(2).apply(ModelStorage.ModelType.GURU).apply(true);
        ModelStorage.Model sku2 = modelCreator.apply(2).apply(ModelStorage.ModelType.SKU).apply(false);
        ModelStorage.Model sku3 = modelCreator.apply(2).apply(ModelStorage.ModelType.SKU).apply(false);
        ModelStorage.Model toloka3 = modelCreator.apply(3).apply(ModelStorage.ModelType.TOLOKA).apply(false);

        BiFunction<Long, List<? extends ModelStorage.Model>, ModelStorage.Model> resolver =
                ModelStorageHelper.ModelSkuDuplicationHelper::chooseSku;

        assertThat(CategoryModelsHelper.reorderModelResponse(Arrays.asList(guruAsSku1, sku1), Arrays.asList(1L, 1L), resolver))
                .isEqualTo(Arrays.asList(sku1, sku1));

        assertThat(CategoryModelsHelper.reorderModelResponse(Arrays.asList(guruAsSku2, guru1, sku2), Arrays.asList(2L, 1L), resolver))
                .isEqualTo(Arrays.asList(sku2, guru1));

        assertThat(CategoryModelsHelper.reorderModelResponse(Arrays.asList(guru1, sku3), Arrays.asList(1L, 2L, 3L), resolver))
                .isEqualTo(Arrays.asList(guru1, sku3));

        assertThatThrownBy(
                () -> CategoryModelsHelper.reorderModelResponse(Arrays.asList(guru1, sku1), Collections.singletonList(1L), resolver)
        ).hasMessageContaining("not a model-is-sku/sku pair");

        assertThatThrownBy(
                () -> CategoryModelsHelper.reorderModelResponse(Arrays.asList(guruAsSku1, sku1, sku1), Collections.singletonList(1L), resolver)
        ).hasMessageContaining("More than 2 models");

        assertThatThrownBy(
                () -> CategoryModelsHelper.reorderModelResponse(Arrays.asList(guruAsSku1, guruAsSku1, guruAsSku1, sku1), Collections.singletonList(1L), resolver)
        ).hasMessageContaining("More than 2 models");

        CategoryModelsHelper.reorderModelResponse(Arrays.asList(sku1, sku2, toloka3), Arrays.asList(1L, 2L, 3L), resolver);
    }
}
