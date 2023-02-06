package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.core.modelstorage.util.ModelComparator;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author dmserebr
 */
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:methodlength"})
public class ModelComparatorTest {
    private ModelStorage.Model model1;
    private ModelStorage.Model model2;

    private ModelStorage.Model modif11;
    private ModelStorage.Model modif12;
    private ModelStorage.Model modif21;
    private ModelStorage.Model modif22;

    private ModelStorage.Model sku101;
    private ModelStorage.Model sku102;
    private ModelStorage.Model sku111;
    private ModelStorage.Model sku112;
    private ModelStorage.Model sku121;
    private ModelStorage.Model sku122;
    private ModelStorage.Model sku201;
    private ModelStorage.Model sku202;
    private ModelStorage.Model sku211;
    private ModelStorage.Model sku212;
    private ModelStorage.Model sku221;
    private ModelStorage.Model sku222;

    private List<ModelStorage.Model> modelList;
    private Map<Long, ModelStorage.Model> modelMap;

    @Before
    public void init() {
        model1 = ModelStorage.Model.newBuilder()
            .setId(1L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(101L))
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(102L))
            .build();
        model2 = ModelStorage.Model.newBuilder()
            .setId(2L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(202L))
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(201L))
            .build();

        modif11 = ModelStorage.Model.newBuilder()
            .setId(11L)
            .setParentId(1L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(111L))
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(112L))
            .build();
        modif12 = ModelStorage.Model.newBuilder()
            .setId(12L)
            .setParentId(1L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(122L))
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(121L))
            .build();
        modif21 = ModelStorage.Model.newBuilder()
            .setId(21L)
            .setParentId(2L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(211L))
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(212L))
            .build();
        modif22 = ModelStorage.Model.newBuilder()
            .setId(22L)
            .setParentId(2L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(221L))
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(222L))
            .build();

        sku111 = ModelStorage.Model.newBuilder()
            .setId(111L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(11L))
            .build();
        sku112 = ModelStorage.Model.newBuilder()
            .setId(112L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(11L))
            .build();
        sku121 = ModelStorage.Model.newBuilder()
            .setId(121L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(12L))
            .build();
        sku122 = ModelStorage.Model.newBuilder()
            .setId(122L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(12L))
            .build();

        // SKUs of both parent models
        sku101 = ModelStorage.Model.newBuilder()
            .setId(101L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(1L))
            .build();
        sku102 = ModelStorage.Model.newBuilder()
            .setId(102L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(1L))
            .build();
        sku201 = ModelStorage.Model.newBuilder()
            .setId(201L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(2L))
            .build();
        sku202 = ModelStorage.Model.newBuilder()
            .setId(202L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(2L))
            .build();

        sku211 = ModelStorage.Model.newBuilder()
            .setId(211L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(21L))
            .build();
        sku212 = ModelStorage.Model.newBuilder()
            .setId(212L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(21L))
            .build();
        sku221 = ModelStorage.Model.newBuilder()
            .setId(221L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(22L))
            .build();
        sku222 = ModelStorage.Model.newBuilder()
            .setId(222L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(22L))
            .build();

        modelList = Arrays.asList(
            model1, model2,
            modif11, modif12, modif21, modif22,
            sku101, sku102, sku111, sku112, sku121, sku122,
            sku201, sku202, sku211, sku212, sku221, sku222);
        modelMap = modelList.stream().collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));
    }
    @Test
    public void testModelComparison() {
        Assert.assertEquals(-1,
            ModelComparator.compare(model1, model2, modelMap));
        Assert.assertEquals(-1,
            ModelComparator.compare(modif11, model2, modelMap));
        Assert.assertEquals(1,
            ModelComparator.compare(modif21, model1, modelMap));
        Assert.assertEquals(1,
            ModelComparator.compare(sku201, modif12, modelMap));
        Assert.assertEquals(-1,
            ModelComparator.compare(sku101, sku102, modelMap));
        Assert.assertEquals(1,
            ModelComparator.compare(sku221, sku202, modelMap));
        Assert.assertEquals(-1,
            ModelComparator.compare(modif11, sku112, modelMap));
    }

    @Test
    public void testModelSorting() {
        for (int i = 0; i < 17; ++i) {
            Collections.shuffle(modelList, new Random(42 * i));

            List<ModelStorage.Model> sortedModels = modelList.stream()
                .sorted((a, b) -> ModelComparator.compare(a, b, modelMap))
                .collect(Collectors.toList());

            Assert.assertArrayEquals(new ModelStorage.Model[] {
                model1, sku101, sku102,
                modif11, sku111, sku112,
                modif12, sku121, sku122,
                model2, sku201, sku202,
                modif21, sku211, sku212,
                modif22, sku221, sku222
            }, sortedModels.toArray(new ModelStorage.Model[0]));
        }
    }
}
