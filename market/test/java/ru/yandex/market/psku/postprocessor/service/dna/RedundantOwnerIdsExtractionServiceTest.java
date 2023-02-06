package ru.yandex.market.psku.postprocessor.service.dna;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;

import static org.assertj.core.api.Assertions.assertThat;

public class RedundantOwnerIdsExtractionServiceTest extends BaseDBTest {

    private static final int HID = 91491;
    private static final String SHOP_SKU1 = "SHOP_SKU1";
    private static final String SHOP_SKU2 = "SHOP_SKU2";
    private static final long EXISTING_MODEL_ID = 200501L;
    private static final long EXISTING_PSKU_ID1 = 100501L;

    private static final long PARAM_ID_1 = 1L;
    private static final long PARAM_ID_2 = 2L;
    private static final long PARAM_ID_3 = 3L;
    private static final long PARAM_ID_4 = 4L;

    private static final long OWNER_ID_1 = 1L;
    private static final long OWNER_ID_2 = 2L;
    private static final long OWNER_ID_3 = 3L;

    private static final String PICTURE_MD5 = "dsjfldfku23423gkj3";

    private Map<Long, ModelStorage.Model> modelsMap;
    private Map<ModelStorage.Model, Set<ModelStorage.Model>> modelsToSkus;

    private MboMappingsServiceMock mboMappingsServiceMock = new MboMappingsServiceMock();
    private RedundantOwnerIdsExtractionService ownerIdsExtractionService;

    @Before
    public void setUp() {
        ownerIdsExtractionService =
                new RedundantOwnerIdsExtractionService(new MboMappingsServiceHelper(mboMappingsServiceMock));
        modelsMap = new HashMap<>();
        List<ModelStorage.ParameterValue> parameterValueList = List.of(generatePV(PARAM_ID_1, OWNER_ID_1));
        List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesisList =
                List.of(generateHypothesis(PARAM_ID_2, OWNER_ID_2));

        ModelStorage.Model model = generateModel(EXISTING_MODEL_ID, parameterValueList, parameterValueHypothesisList)
                .toBuilder()
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(EXISTING_PSKU_ID1)
                        .setType(ModelStorage.RelationType.SKU_MODEL).build()).build();
        modelsMap.put(EXISTING_MODEL_ID, model);

        ModelStorage.Model sku1 = generateModel(EXISTING_PSKU_ID1,
                List.of(generatePV(PARAM_ID_3, OWNER_ID_2)),
                List.of(generateHypothesis(PARAM_ID_4, OWNER_ID_1)));
        modelsMap.put(EXISTING_PSKU_ID1, sku1);
        modelsToSkus = Map.of(model, Set.of(sku1));
    }

    @Test
    public void whenRedundantOwnersExistThenExtractThem() {
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_1), SHOP_SKU1, EXISTING_PSKU_ID1);
        Map<Long, List<MbocCommon.MappingInfoLite>> modelToMappings =
                ownerIdsExtractionService.actualizeSkuMappings(modelsMap.get(EXISTING_MODEL_ID));
        Map<Long, Set<Integer>> redundantOwnerIds =
                ownerIdsExtractionService.findRedundantOwnerIds(modelsToSkus, modelToMappings);
        assertThat(redundantOwnerIds).hasSize(2);
        redundantOwnerIds.forEach((modelId, owners) -> assertThat(owners).containsExactly(Math.toIntExact(OWNER_ID_2)));
    }

    @Test
    public void whenRedundantOwnersHasOnlyPicturesThenExtractThem() {
        ModelStorage.Model model = modelsMap.get(EXISTING_MODEL_ID);
        ModelStorage.Model sku1 = modelsMap.get(EXISTING_PSKU_ID1).toBuilder()
                .addPictures(generatePicture(PICTURE_MD5, OWNER_ID_3))
                .build();
        modelsMap.put(EXISTING_PSKU_ID1, sku1);
        Map<ModelStorage.Model, Set<ModelStorage.Model>> modelsToSkus = Map.of(model, Set.of(sku1));

        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_1), SHOP_SKU1, EXISTING_PSKU_ID1);
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_2), SHOP_SKU2, EXISTING_PSKU_ID1);
        Map<Long, List<MbocCommon.MappingInfoLite>> modelToMappings =
                ownerIdsExtractionService.actualizeSkuMappings(modelsMap.get(EXISTING_MODEL_ID));
        Map<Long, Set<Integer>> redundantOwnerIds =
                ownerIdsExtractionService.findRedundantOwnerIds(modelsToSkus, modelToMappings);
        assertThat(redundantOwnerIds).hasSize(1);
        assertThat(redundantOwnerIds)
                .containsExactlyInAnyOrderEntriesOf(Map.of(EXISTING_PSKU_ID1, Set.of(Math.toIntExact(OWNER_ID_3))));
    }

    @Test
    public void whenRedundantOwnersDoNotExistThenNoAction() {
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_1), SHOP_SKU1, EXISTING_PSKU_ID1);
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_2), SHOP_SKU2, EXISTING_PSKU_ID1);
        Map<Long, List<MbocCommon.MappingInfoLite>> modelToMappings =
                ownerIdsExtractionService.actualizeSkuMappings(modelsMap.get(EXISTING_MODEL_ID));
        Map<Long, Set<Integer>> redundantOwnerIds =
                ownerIdsExtractionService.findRedundantOwnerIds(modelsToSkus, modelToMappings);
        assertThat(redundantOwnerIds).isEmpty();
    }

    @Test
    public void whenNoBoundOffersThenAllOwnersAreRedundant() {
        Map<Long, List<MbocCommon.MappingInfoLite>> modelToMappings =
                ownerIdsExtractionService.actualizeSkuMappings(modelsMap.get(EXISTING_MODEL_ID));
        Map<Long, Set<Integer>> redundantOwnerIds =
                ownerIdsExtractionService.findRedundantOwnerIds(modelsToSkus, modelToMappings);
        assertThat(redundantOwnerIds).hasSize(2);
        assertThat(redundantOwnerIds.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(Math.toIntExact(OWNER_ID_2), Math.toIntExact(OWNER_ID_1));
    }

    private ModelStorage.Model generateModel(long id,
            List<ModelStorage.ParameterValue> parameterValueList,
                                             List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesisList) {
        return ModelStorage.Model.newBuilder()
                .setId(id)
                .addAllParameterValues(parameterValueList)
                .addAllParameterValueHypothesis(parameterValueHypothesisList)
                .build();
    }

    private ModelStorage.ParameterValue generatePV(Long paramId, Long ownerId) {
        return ModelStorage.ParameterValue.newBuilder()
                .setParamId(paramId)
                .setOwnerId(ownerId)
                .build();
    }

    private ModelStorage.ParameterValueHypothesis generateHypothesis(Long paramId, Long ownerId) {
        return ModelStorage.ParameterValueHypothesis.newBuilder()
                .setParamId(paramId)
                .setOwnerId(ownerId)
                .build();
    }

    private ModelStorage.Picture generatePicture(String md5, Long ownerId) {
        return ModelStorage.Picture.newBuilder()
                .setOrigMd5(md5)
                .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                .setOwnerId(ownerId)
                .build();
    }
}
