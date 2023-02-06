package ru.yandex.market.psku.postprocessor.service.dna;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.MbocCommon;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.ModelsGenerator.generateModelWithRelations;

public class ModelSplitterTest {

    private static final long PARENT_MODEL_ID = 1L;
    private static final long SKU_ID_1 = 11L;
    private static final long SKU_ID_2 = 12L;
    private static final long SKU_ID_3 = 13L;
    private static final long SKU_ID_4 = 14L;
    private static final long HID = 11111L;
    private static final int GROUP_ID = 1;
    private static final int GROUP_ID1 = 2;
    private static final int GROUP_ID2 = 3;
    private static final int GROUP_ID3 = 4;
    private static final int GROUP_ID4 = 5;
    private static final int GROUP_ID5 = 6;
    private static final int SUPPLIER_ID = 10;
    private static final int SUPPLIER_ID1 = 20;
    private static final String SHOP_SKU_1 = "SKU_1";
    private static final String SHOP_SKU_2 = "SKU_2";
    private static final String SHOP_SKU_3 = "SKU_3";
    private static final String SHOP_SKU_4 = "SKU_4";
    private static final String SHOP_SKU_5 = "SKU_5";
    private static final String SHOP_SKU_6 = "SKU_6";
    private static final String SHOP_SKU_7 = "SKU_7";
    private static final String SHOP_SKU_8 = "SKU_8";
    private static final String SHOP_SKU_9 = "SKU_9";

    private final Map<Long, List<MbocCommon.MappingInfoLite>> skuIdToMappings = new HashMap<>();
    private final Map<Long, ModelStorage.Model> modelsMap = new HashMap<>();
    private ModelStorage.Model sku1;
    private ModelStorage.Model sku2;
    private ModelStorage.Model model;

    @Before
    public void setUp() {
        sku1 = generateModelWithRelations(SKU_ID_1,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(PARENT_MODEL_ID), HID
        );
        sku2 = generateModelWithRelations(SKU_ID_2,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(PARENT_MODEL_ID), HID
        );
        model = generateModelWithRelations(PARENT_MODEL_ID,
                ModelStorage.RelationType.SKU_MODEL,
                Set.of(SKU_ID_1, SKU_ID_2), HID
        );
        modelsMap.put(SKU_ID_1, sku1);
        modelsMap.put(SKU_ID_2, sku2);
        modelsMap.put(PARENT_MODEL_ID, model);
    }

    //        model
    //      /       \
    //    sku1     sku2         ===============================>      не меняем
    //      group_id
    @Test
    public void whenActualStateForOneGroupThenDoNotChangeRelations() {
        skuIdToMappings.put(sku1.getId(),
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                        .setModelId(SKU_ID_1)
                        .setGroupId(GROUP_ID)
                        .setSupplierId(SUPPLIER_ID)
                        .setShopSku(SHOP_SKU_1)
                        .build()));
        skuIdToMappings.put(sku2.getId(),
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                        .setModelId(SKU_ID_2)
                        .setGroupId(GROUP_ID)
                        .setSupplierId(SUPPLIER_ID)
                        .setShopSku(SHOP_SKU_2)
                        .build()));
        Map<ModelStorage.Model, Set<ModelStorage.Model>> modelWithSkus =
                ModelSplitter.splitModel(model, skuIdToMappings, modelsMap);
        assertThat(modelWithSkus).hasSize(1);
        Map.Entry<ModelStorage.Model, Set<ModelStorage.Model>> resultingModel =
                modelWithSkus.entrySet().stream().findFirst().get();
        assertThat(resultingModel.getKey().getId()).isEqualTo(PARENT_MODEL_ID);
        assertThat(resultingModel.getKey().getRelationsList().stream().map(ModelStorage.Relation::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(SKU_ID_1, SKU_ID_2);

        assertThat(resultingModel.getValue()).hasSize(2);
        assertThat(resultingModel.getValue().stream().map(ModelStorage.Model::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(SKU_ID_1, SKU_ID_2);
        assertThat(resultingModel.getValue().stream().flatMap(sku -> sku.getRelationsList().stream())
                .map(ModelStorage.Relation::getId)
                .collect(Collectors.toSet()))
                .containsExactly(PARENT_MODEL_ID);
    }

    //        model                   model    model1
    //      /       \         =>        |         |
    //    sku1     sku2               sku1       sku2
    //     нет группы
    @Test
    public void whenAllSkusWOGroupThenCreateNewModels() {
        skuIdToMappings.put(SKU_ID_1,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                        .setModelId(SKU_ID_1)
                        .setSupplierId(SUPPLIER_ID)
                        .setShopSku(SHOP_SKU_1)
                        .build()));
        skuIdToMappings.put(SKU_ID_2,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                        .setModelId(SKU_ID_2)
                        .setSupplierId(SUPPLIER_ID)
                        .setShopSku(SHOP_SKU_2)
                        .build()));
        Map<ModelStorage.Model, Set<ModelStorage.Model>> modelWithSkusList = ModelSplitter.splitModel(model,
                skuIdToMappings,
                modelsMap);
        assertThat(modelWithSkusList).hasSize(2);
        assertThat(modelWithSkusList.keySet().stream().map(ModelStorage.Model::getId)
                .collect(Collectors.toSet())).containsExactlyInAnyOrder(PARENT_MODEL_ID, -1L);
        modelWithSkusList.forEach((key, value) -> assertThat(value).hasSize(1));
        assertThat(modelWithSkusList.keySet().stream().filter(m -> m.getId() == -1L)
                .flatMap(m -> m.getRelationsList().stream())
                .map(ModelStorage.Relation::getId)
                .collect(Collectors.toSet())
        ).containsExactly(SKU_ID_2);
        assertThat(modelWithSkusList.keySet().stream().filter(m -> m.getId() == PARENT_MODEL_ID)
                .flatMap(m -> m.getRelationsList().stream())
                .map(ModelStorage.Relation::getId)
                .collect(Collectors.toSet())
        ).containsExactly(SKU_ID_1);
        assertThat(modelWithSkusList.values().stream().flatMap(Collection::stream)
                .filter(m -> m.getId() == SKU_ID_2)
                .flatMap(m -> m.getRelationsList().stream())
                .map(ModelStorage.Relation::getId)
                .collect(Collectors.toSet())
        ).containsExactly(-1L);
        assertThat(modelWithSkusList.values().stream().flatMap(Collection::stream)
                .filter(m -> m.getId() == SKU_ID_1)
                .flatMap(m -> m.getRelationsList().stream())
                .map(ModelStorage.Relation::getId)
                .collect(Collectors.toSet())
        ).containsExactly(PARENT_MODEL_ID);
    }

    //        model           удалён маппинг (sku2, group_id1)        model1       model
    //      /    |   \        ===============================>        /   \         |
    //    sku1  sku2  sku3                                          sku1  sku2     sku3
    //     \     / \    |
    //    group_id  group_id1
    @Test
    public void whenIntersectionIsRemovedThenSplitModel() {
        ModelStorage.Model sku3 = generateModelWithRelations(SKU_ID_3,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(PARENT_MODEL_ID), HID
        );
        model = generateModelWithRelations(PARENT_MODEL_ID,
                ModelStorage.RelationType.SKU_MODEL,
                Set.of(SKU_ID_1, SKU_ID_2, SKU_ID_3), HID
        );
        modelsMap.put(PARENT_MODEL_ID, model);
        modelsMap.put(SKU_ID_3, sku3);
        skuIdToMappings.put(SKU_ID_1,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                        .setModelId(SKU_ID_1)
                        .setSupplierId(SUPPLIER_ID)
                        .setGroupId(GROUP_ID)
                        .setShopSku(SHOP_SKU_1)
                        .build()));
        skuIdToMappings.put(SKU_ID_2,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                        .setModelId(SKU_ID_2)
                        .setSupplierId(SUPPLIER_ID)
                        .setGroupId(GROUP_ID)
                        .setShopSku(SHOP_SKU_2)
                        .build()));
        skuIdToMappings.put(SKU_ID_3,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                        .setModelId(SKU_ID_3)
                        .setSupplierId(SUPPLIER_ID1)
                        .setGroupId(GROUP_ID1)
                        .setShopSku(SHOP_SKU_3)
                        .build()));
        Map<ModelStorage.Model, Set<ModelStorage.Model>> modelWithSkusList = ModelSplitter.splitModel(model,
                skuIdToMappings, modelsMap);
        assertThat(modelWithSkusList).hasSize(2);
        assertThat(modelWithSkusList.keySet().stream().map(ModelStorage.Model::getId)
                .collect(Collectors.toSet())).containsExactlyInAnyOrder(PARENT_MODEL_ID, -1L);
        modelWithSkusList.forEach((key, value) -> {
            if (value.size() == 1) {
                assertThat(value.stream().map(ModelStorage.Model::getId)
                        .collect(Collectors.toList())).containsExactly(SKU_ID_3);
                assertThat(value.stream().flatMap(skus -> skus.getRelationsList().stream())
                        .map(ModelStorage.Relation::getId).collect(Collectors.toSet()))
                        .containsExactly(PARENT_MODEL_ID);
                key.getRelationsList()
                        .forEach(relation -> assertThat(relation.getId()).isEqualTo(SKU_ID_3));
            } else {
                assertThat(value.stream().map(ModelStorage.Model::getId)
                        .collect(Collectors.toList())).containsExactlyInAnyOrder(SKU_ID_1, SKU_ID_2);
                assertThat(value.stream().flatMap(skus -> skus.getRelationsList().stream())
                        .map(ModelStorage.Relation::getId).collect(Collectors.toSet())).containsExactly(-1L);
                assertThat(key.getRelationsList().stream()
                        .map(ModelStorage.Relation::getId)
                        .collect(Collectors.toList()))
                        .containsExactlyInAnyOrder(SKU_ID_1, SKU_ID_2);
            }
        });
    }

    //        model
    //      /    |
    //    sku1  sku2             ===============================>      не меняем
    //     \     / \
    //    group_id  нет группы
    @Test
    public void whenMappingWithAndWOGroupThenDoNotSplitModel() {
        model = generateModelWithRelations(PARENT_MODEL_ID,
                ModelStorage.RelationType.SKU_MODEL,
                Set.of(SKU_ID_1, SKU_ID_2), HID
        );
        skuIdToMappings.put(SKU_ID_1,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                        .setModelId(SKU_ID_1)
                        .setSupplierId(SUPPLIER_ID)
                        .setGroupId(GROUP_ID)
                        .setShopSku(SHOP_SKU_1)
                        .build()));
        skuIdToMappings.put(SKU_ID_2,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                                .setModelId(SKU_ID_2)
                                .setSupplierId(SUPPLIER_ID)
                                .setGroupId(GROUP_ID)
                                .setShopSku(SHOP_SKU_2)
                                .build(),
                        MbocCommon.MappingInfoLite.newBuilder()
                                .setModelId(SKU_ID_2)
                                .setSupplierId(SUPPLIER_ID1)
                                .setShopSku(SHOP_SKU_3)
                                .build())
        );
        Map<ModelStorage.Model, Set<ModelStorage.Model>> modelWithSkusList
                = ModelSplitter.splitModel(model, skuIdToMappings, modelsMap);
        assertThat(modelWithSkusList).hasSize(1);
        Map.Entry<ModelStorage.Model, Set<ModelStorage.Model>> resultingModel =
                modelWithSkusList.entrySet().stream().findFirst().get();
        assertThat(resultingModel.getKey().getId()).isEqualTo(PARENT_MODEL_ID);
        assertThat(resultingModel.getValue()).hasSize(2);
        assertThat(resultingModel.getValue().stream().map(ModelStorage.Model::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(SKU_ID_1, SKU_ID_2);

        assertThat(resultingModel.getKey().getRelationsList().stream()
                .map(ModelStorage.Relation::getId)
                .collect(Collectors.toList())
        )
                .containsExactlyInAnyOrder(SKU_ID_1, SKU_ID_2);
        assertThat(resultingModel.getValue().stream().flatMap(sku -> sku.getRelationsList().stream())
                .map(ModelStorage.Relation::getId)
                .collect(Collectors.toSet()))
                .containsExactly(PARENT_MODEL_ID);
    }

    //          model
    //      /     |     \      \
    //    sku1   sku2  sku3   sku4          ===============================>      не меняем
    //     |      |     |      |
    //   grp1   grp2  grp5   grp1
    //   grp3   grp4  grp6   grp2
    //                       grp5
    // перекрестные связи в итоге образующие одну группу
    @Test
    public void whenAllGroupsIntersectedThenDoNotChangeRelations() {
        ModelStorage.Model sku3 = generateModelWithRelations(SKU_ID_3,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(PARENT_MODEL_ID), HID
        );
        ModelStorage.Model sku4 = generateModelWithRelations(SKU_ID_4,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(PARENT_MODEL_ID), HID
        );
        model = generateModelWithRelations(PARENT_MODEL_ID,
                ModelStorage.RelationType.SKU_MODEL,
                Set.of(SKU_ID_1, SKU_ID_2, SKU_ID_3, SKU_ID_4), HID
        );
        modelsMap.put(PARENT_MODEL_ID, model);
        modelsMap.put(SKU_ID_3, sku3);
        modelsMap.put(SKU_ID_4, sku4);

        // плохое место:
        // ошибка которую призван поймать этот тест проявляется только при опеределенном порядке обхода мапы groupToSkus
        // чтоб подобрать нужный порядок подсовываю большие GroupId тем элементам которые должны обработаться раньше
        // но все это естественно может развалиться (тест будет проходить, но не будет ловить багу :( )
        skuIdToMappings.put(SKU_ID_1,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                            .setModelId(SKU_ID_1)
                            .setSupplierId(SUPPLIER_ID)
                            .setGroupId(GROUP_ID5)
                            .setShopSku(SHOP_SKU_1)
                            .build(),
                        MbocCommon.MappingInfoLite.newBuilder()
                            .setModelId(SKU_ID_1)
                            .setSupplierId(SUPPLIER_ID)
                            .setGroupId(GROUP_ID3)
                            .setShopSku(SHOP_SKU_4)
                            .build()
                )
        );
        skuIdToMappings.put(SKU_ID_2,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                            .setModelId(SKU_ID_2)
                            .setSupplierId(SUPPLIER_ID)
                            .setGroupId(GROUP_ID4)
                            .setShopSku(SHOP_SKU_2)
                            .build(),
                        MbocCommon.MappingInfoLite.newBuilder()
                            .setModelId(SKU_ID_2)
                            .setSupplierId(SUPPLIER_ID)
                            .setGroupId(GROUP_ID2)
                            .setShopSku(SHOP_SKU_5)
                            .build()
                )
        );
        skuIdToMappings.put(SKU_ID_3,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                            .setModelId(SKU_ID_3)
                            .setSupplierId(SUPPLIER_ID)
                            .setGroupId(GROUP_ID1)
                            .setShopSku(SHOP_SKU_3)
                            .build(),
                        MbocCommon.MappingInfoLite.newBuilder()
                            .setModelId(SKU_ID_3)
                            .setSupplierId(SUPPLIER_ID)
                            .setGroupId(GROUP_ID)
                            .setShopSku(SHOP_SKU_6)
                            .build()
                )
        );
        skuIdToMappings.put(SKU_ID_4,
                List.of(MbocCommon.MappingInfoLite.newBuilder()
                            .setModelId(SKU_ID_4)
                            .setSupplierId(SUPPLIER_ID)
                            .setGroupId(GROUP_ID)
                            .setShopSku(SHOP_SKU_7)
                            .build(),
                        MbocCommon.MappingInfoLite.newBuilder()
                            .setModelId(SKU_ID_4)
                            .setSupplierId(SUPPLIER_ID)
                            .setGroupId(GROUP_ID2)
                            .setShopSku(SHOP_SKU_8)
                            .build(),
                        MbocCommon.MappingInfoLite.newBuilder()
                            .setModelId(SKU_ID_4)
                            .setSupplierId(SUPPLIER_ID)
                            .setGroupId(GROUP_ID3)
                            .setShopSku(SHOP_SKU_9)
                            .build()
                )
        );

        Map<ModelStorage.Model, Set<ModelStorage.Model>> modelWithSkusList
                = ModelSplitter.splitModel(model, skuIdToMappings, modelsMap);

        assertThat(modelWithSkusList).hasSize(1);
        Map.Entry<ModelStorage.Model, Set<ModelStorage.Model>> resultingModel =
                modelWithSkusList.entrySet().stream().findFirst().get();
        assertThat(resultingModel.getKey().getId()).isEqualTo(PARENT_MODEL_ID);
        assertThat(resultingModel.getValue()).hasSize(4);
        assertThat(resultingModel.getValue().stream().map(ModelStorage.Model::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(SKU_ID_1, SKU_ID_2, SKU_ID_3, SKU_ID_4);

        assertThat(resultingModel.getKey().getRelationsList().stream()
                .map(ModelStorage.Relation::getId)
                .collect(Collectors.toList())
        )
                .containsExactlyInAnyOrder(SKU_ID_1, SKU_ID_2, SKU_ID_3, SKU_ID_4);
        assertThat(resultingModel.getValue().stream().flatMap(sku -> sku.getRelationsList().stream())
                .map(ModelStorage.Relation::getId)
                .collect(Collectors.toSet()))
                .containsExactly(PARENT_MODEL_ID);
    }
}
