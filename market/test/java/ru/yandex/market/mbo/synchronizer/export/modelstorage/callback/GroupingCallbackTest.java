package ru.yandex.market.mbo.synchronizer.export.modelstorage.callback;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.export.modelstorage.callback.GroupingCallback;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipePart;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GroupingCallbackTest {

    @Test
    public void testCorrectGrouping() {
        List<ModelStorage.Model> models = Arrays.asList(
            Models.M1_ENRICHED,
            Models.SKU1_1_ENRICHED,
            Models.SKU1_2_ENRICHED,
            Models.MODIF1_ENRICHED,
            Models.SKU_MODIF_11_ENRICHED,
            Models.SKU_MODIF_12_ENRICHED,
            Models.MODIF2_ENRICHED,
            Models.SKU_MODIF_2_ENRICHED
        );

        ModelPipeContext context = groupModels(Models.M1_ENRICHED.getId(), models);

        Assertions.assertThat(context.getModel().build()).isEqualTo(Models.M1_ENRICHED);
        Assertions.assertThat(context.getModifications().stream()
            .map(ModelStorage.Model.Builder::build).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                Models.MODIF1_ENRICHED,
                Models.MODIF2_ENRICHED
            );
        Assertions.assertThat(context.getSkusForOutput())
            .containsExactlyInAnyOrder(
                Models.SKU1_1_ENRICHED,
                Models.SKU1_2_ENRICHED,
                Models.SKU_MODIF_11_ENRICHED,
                Models.SKU_MODIF_12_ENRICHED,
                Models.SKU_MODIF_2_ENRICHED
            );
    }

    @Test
    public void testCorrectGroupingOfUnsortedData() {
        List<ModelStorage.Model> models = new ArrayList<>(Arrays.asList(
            Models.M1_ENRICHED,
            Models.SKU1_1_ENRICHED,
            Models.SKU1_2_ENRICHED,
            Models.MODIF1_ENRICHED,
            Models.SKU_MODIF_11_ENRICHED,
            Models.SKU_MODIF_12_ENRICHED,
            Models.MODIF2_ENRICHED,
            Models.SKU_MODIF_2_ENRICHED
        ));
        Collections.shuffle(models, new Random(1));

        ModelPipeContext context = groupModels(Models.M1_ENRICHED.getId(), models);

        Assertions.assertThat(context.getModel().build()).isEqualTo(Models.M1_ENRICHED);
        Assertions.assertThat(context.getModifications().stream()
            .map(ModelStorage.Model.Builder::build).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                Models.MODIF1_ENRICHED,
                Models.MODIF2_ENRICHED
            );
        Assertions.assertThat(context.getSkusForOutput())
            .containsExactlyInAnyOrder(
                Models.SKU1_1_ENRICHED,
                Models.SKU1_2_ENRICHED,
                Models.SKU_MODIF_11_ENRICHED,
                Models.SKU_MODIF_12_ENRICHED,
                Models.SKU_MODIF_2_ENRICHED
            );
    }

    @Test
    public void testFailIfNoGroupModel() {
        List<ModelStorage.Model> models = Arrays.asList(
            Models.MODIF1_ENRICHED,
            Models.MODIF2_ENRICHED
        );

        Assertions.assertThatThrownBy(() -> groupModels(Models.M1_ENRICHED.getId(), models))
            .hasMessage("No group model with id: %d", Models.M1_ENRICHED.getId());
    }

    @Test
    public void testFailIfGroupContainsOtherModel() {
        List<ModelStorage.Model> models = Arrays.asList(
            Models.M1_ENRICHED,
            Models.M2_ENRICHED
        );

        Assertions.assertThatThrownBy(() -> groupModels(Models.M1_ENRICHED.getId(), models))
            .hasMessage("No all models accepted to group (group-id: %d): model-id: %s",
                Models.M1_ENRICHED.getId(), Collections.singletonList(Models.M2_ENRICHED.getId()));
    }

    @Test
    public void testModelIsSkuCorrectGroup() {
        List<ModelStorage.Model> models = Arrays.asList(
            Models.M3_ENRICHED,
            Models.M3_SKU_ENRICHED
        );
        List<ModelStorage.Model> reverseOrder = Arrays.asList(
            Models.M3_SKU_ENRICHED,
            Models.M3_ENRICHED
        );

        Consumer<List<ModelStorage.Model>> isOk = new Consumer<List<ModelStorage.Model>>() {
            @Override
            public void accept(List<ModelStorage.Model> models) {
                ModelPipeContext context = groupModels(Models.M3_ENRICHED.getId(), models);

                Assertions.assertThat(context.getModel().build()).isEqualTo(Models.M3_ENRICHED);
                Assertions.assertThat(context.getSkusForOutput()).containsExactlyInAnyOrder(Models.M3_SKU_ENRICHED);
            }
        };

        isOk.accept(models);
        isOk.accept(reverseOrder);
    }

    private static ModelPipeContext groupModels(long groupModelId, List<ModelStorage.Model> models) {
        try {
            RememberContextPipePart rememberContextPipePart = new RememberContextPipePart();
            GroupingCallback groupingCallback = new GroupingCallback(rememberContextPipePart);
            groupingCallback.acceptThrows(groupModelId, models);
            return rememberContextPipePart.getContext();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class RememberContextPipePart implements ModelPipePart {
        private ModelPipeContext context;

        @Override
        public void acceptModelsGroup(ModelPipeContext context) {
            this.context = context;
        }

        public ModelPipeContext getContext() {
            return context;
        }
    }
}
