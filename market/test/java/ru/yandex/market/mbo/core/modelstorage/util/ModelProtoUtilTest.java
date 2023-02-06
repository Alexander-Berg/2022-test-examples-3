package ru.yandex.market.mbo.core.modelstorage.util;

import org.junit.Test;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.Relation;
import ru.yandex.market.mbo.http.ModelStorage.RelationType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class ModelProtoUtilTest {

    @Test
    public void testNegativeIdsNewModelsGrouped() {
        List<ModelStorage.Model.Builder> parents = Arrays.asList(
            ModelStorage.Model.newBuilder().setId(100L),
            ModelStorage.Model.newBuilder().setId(12L).addRelations(relation(10L, RelationType.SKU_MODEL)),
            ModelStorage.Model.newBuilder().setId(66L)
        );

        List<ModelStorage.Model.Builder> skus = Arrays.asList(
            ModelStorage.Model.newBuilder().setId(10L).addRelations(relation(12L, RelationType.SKU_PARENT_MODEL)),
            ModelStorage.Model.newBuilder().setId(0L).addRelations(relation(66L, RelationType.SKU_PARENT_MODEL)),
            ModelStorage.Model.newBuilder().setId(-2L).addRelations(relation(12L, RelationType.SKU_PARENT_MODEL))
        );

        Map<ModelStorage.Model.Builder, List<ModelStorage.Model.Builder>> groups = ModelProtoUtil.groupSkusByModel(
            parents,
            skus
        );

        List<ModelStorage.Model.Builder> groupedToModel100 = groups.get(parents.get(0));
        List<ModelStorage.Model.Builder> groupedToModel12 = groups.get(parents.get(1));
        List<ModelStorage.Model.Builder> groupedToModel66 = groups.get(parents.get(2));
        assertThat(groupedToModel100).isNullOrEmpty();
        assertThat(groupedToModel12).containsExactly(skus.get(0), skus.get(2));
        assertThat(groupedToModel66).containsExactly(skus.get(1));
    }

    @Test
    public void testNewGurusAreTreatedWell() {
        List<ModelStorage.Model.Builder> parents = Arrays.asList(
            ModelStorage.Model.newBuilder().setId(-2L),
            ModelStorage.Model.newBuilder().setId(0L),
            ModelStorage.Model.newBuilder().setId(-3L)
        );

        List<ModelStorage.Model.Builder> skus = Arrays.asList(
            ModelStorage.Model.newBuilder().setId(-100L).addRelations(relation(0L, RelationType.SKU_PARENT_MODEL)),
            ModelStorage.Model.newBuilder().setId(-200L).addRelations(relation(-3L, RelationType.SKU_PARENT_MODEL)),
            ModelStorage.Model.newBuilder().setId(-300L).addRelations(relation(-3L, RelationType.SKU_PARENT_MODEL))
        );

        Map<ModelStorage.Model.Builder, List<ModelStorage.Model.Builder>> groups = ModelProtoUtil.groupSkusByModel(
            parents,
            skus
        );

        List<ModelStorage.Model.Builder> groupedToModelMinusTwo = groups.get(parents.get(0));
        List<ModelStorage.Model.Builder> groupedToModelZero = groups.get(parents.get(1));
        List<ModelStorage.Model.Builder> groupedToModelMinusThree = groups.get(parents.get(2));
        assertThat(groupedToModelMinusTwo).isNullOrEmpty();
        assertThat(groupedToModelZero).containsExactly(skus.get(0));
        assertThat(groupedToModelMinusThree).containsExactly(skus.get(1), skus.get(2));
    }

    private Relation.Builder relation(long id, RelationType type) {
        return Relation.newBuilder().setId(id).setType(type);
    }
}
