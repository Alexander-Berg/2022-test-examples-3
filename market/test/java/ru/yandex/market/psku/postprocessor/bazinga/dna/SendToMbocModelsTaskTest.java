package ru.yandex.market.psku.postprocessor.bazinga.dna;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsResultDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.CleanupStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModels;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModelsResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendToMbocModelsTaskTest extends BaseDBTest {
    private static final int HID = 91491;
    private static final String SHOP_SKU1 = "SHOP_SKU1";
    private static final String SHOP_SKU2 = "SHOP_SKU2";
    private static final String SHOP_SKU3 = "SHOP_SKU3";
    private static final String SHOP_SKU4 = "SHOP_SKU4";
    private static final long MODEL_ID1 = 111L; // с 2 ску обе с мапингами
    private static final long MODEL_ID2 = 112L; // с 2 ску одна с мапингами
    private static final long MODEL_ID3 = 113L; // без ску
    private static final long MODEL_ID4 = 114L; // отсутсвующая
    private static final long MODEL_ID5 = 115L; // с 2 ску без мапингов
    private static final long PSKU_ID1 = 100501L;
    private static final long PSKU_ID2 = 100502L;
    private static final long PSKU_ID3 = 100503L;
    private static final long PSKU_ID4 = 100504L;
    private static final long PSKU_ID5 = 100505L;
    private static final long PSKU_ID6 = 100506L;
    private static final long OWNER_ID_1 = 1L;
    private static final long OWNER_ID_2 = 2L;
    private static final long OWNER_ID_3 = 3L;
    private static final long OWNER_ID_4= 3L;

    private final Timestamp ts = Timestamp.from(Instant.now());
    private Map<Long, DeletedMappingModels> deletedMappingModels;
    private Map<Long, ModelStorage.Model> modelsMap;

    @Autowired
    private DeletedMappingModelsDao deletedMappingModelsDao;
    @Autowired
    private DeletedMappingModelsResultDao deletedMappingModelsResultDao;

    private ModelStorageHelper modelStorageHelper;
    private MboMappingsServiceMock mboMappingsService;
    private SendToMbocModelsTask sendToMbocModelsTask;

    @Before
    public void setUp() {
        modelStorageHelper = Mockito.mock(ModelStorageHelper.class);
        mboMappingsService = setupMappingsServiceMock();
        sendToMbocModelsTask = new SendToMbocModelsTask(deletedMappingModelsDao, deletedMappingModelsResultDao,
                modelStorageHelper, mboMappingsService);

        deletedMappingModels = prepareDeletedMappingModels();
        modelsMap = prepareModels();
        setupModelStorageMock(modelsMap);
        setupMappingsServiceMock();
    }

    @Test
    public void whenSendToMbocSuccessfulThenMoveToResult() {
        // все ответили хорошо при обновлении

        DeletedMappingModels testModel = deletedMappingModels.get(MODEL_ID1);
        deletedMappingModelsDao.insert(testModel);
        // null - КИ успешно добавит все мапинги
        setupMappingsServiceMockErrorOnShopSku(null);

        sendToMbocModelsTask.execute(null);

        //проверить что этой модели больше нет в deleted_mapping_models
        List<DeletedMappingModels> deletedMappingModelsAfter =
                deletedMappingModelsDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsAfter).hasSize(0);

        //проверить что эта модель теперь в deleted_mapping_models_result в статусе SUCCESS
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsResults).hasSize(1);
        assertThat(deletedMappingModelsResults)
                .extracting(DeletedMappingModelsResult::getStatus)
                .containsOnly(CleanupStatus.SUCCESS);

        //проверить что все мапинги были отправлены в ручку mboMappingsService.addOfferToContentProcessing
        ArgumentCaptor<MboMappings.AddToContentProcessingRequest> argumentCaptor =
                ArgumentCaptor.forClass(MboMappings.AddToContentProcessingRequest.class);
        verify(mboMappingsService, times(1)).addOfferToContentProcessing(argumentCaptor.capture());
        MboMappings.AddToContentProcessingRequest request = argumentCaptor.getValue();
        assertThat(request.getBusinessSkuKeyList())
                .extracting("businessId", "offerId")
                .containsExactlyInAnyOrder(
                    new Tuple(Math.toIntExact(OWNER_ID_1), SHOP_SKU1),
                    new Tuple(Math.toIntExact(OWNER_ID_2), SHOP_SKU2),
                    new Tuple(Math.toIntExact(OWNER_ID_3), SHOP_SKU3)
                );
    }

    @Test
    public void whenErrorSendToMbocThenChangeStatusToReadyToMboc() {
        // плохой ответ при обновлении - возвращаем в обработку

        DeletedMappingModels testModel = deletedMappingModels.get(MODEL_ID1);
        deletedMappingModelsDao.insert(testModel);
        // КИ вернет ошибку при обновлении мапинга на SHOP_SKU1
        setupMappingsServiceMockErrorOnShopSku(SHOP_SKU1);

        sendToMbocModelsTask.execute(null);

        //проверить что модель в deleted_mapping_models со статусом READY_FOR_MBOC
        List<DeletedMappingModels> deletedMappingModelsAfter =
                deletedMappingModelsDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsAfter).hasSize(1);
        assertThat(deletedMappingModelsAfter)
                .extracting(DeletedMappingModels::getStatus)
                .containsOnly(CleanupStatus.READY_FOR_MBOC);

        //проверить что модели нет в deleted_mapping_models_result
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsResults).hasSize(0);

        //проверить что все мапинги были отправлены в ручку mboMappingsService.addOfferToContentProcessing
        ArgumentCaptor<MboMappings.AddToContentProcessingRequest> argumentCaptor =
                ArgumentCaptor.forClass(MboMappings.AddToContentProcessingRequest.class);
        verify(mboMappingsService, times(1)).addOfferToContentProcessing(argumentCaptor.capture());
        MboMappings.AddToContentProcessingRequest request = argumentCaptor.getValue();
        assertThat(request.getBusinessSkuKeyList())
                .extracting("businessId", "offerId")
                .containsExactlyInAnyOrder(
                        new Tuple(Math.toIntExact(OWNER_ID_1), SHOP_SKU1),
                        new Tuple(Math.toIntExact(OWNER_ID_2), SHOP_SKU2),
                        new Tuple(Math.toIntExact(OWNER_ID_3), SHOP_SKU3)
                );
    }

    @Test
    public void whenSomeMappingsHasErrorOnSendToMbocThenChangeStatusToReadyToMbocOnlyForFailedModels() {
        // для части моделей плохой ответ при обновлении => ее на повторную обработку, остальные успешные

        Collection<DeletedMappingModels> testModels = deletedMappingModels.values();
        Long expectedFailedModelId = MODEL_ID1;
        List<Long> expectedSuccessfulModelIds = testModels.stream()
                .map(DeletedMappingModels::getModelId)
                .filter(modelId -> !modelId.equals(expectedFailedModelId))
                .collect(Collectors.toList());
        deletedMappingModelsDao.insert(testModels);
        // КИ вернет ошибку при обновлении мапинга на SHOP_SKU1 => должна упасть MODEL_ID1
        setupMappingsServiceMockErrorOnShopSku(SHOP_SKU1);

        sendToMbocModelsTask.execute(null);

        //проверить что модель c ошибкой в deleted_mapping_models со статусом READY_FOR_MBOC
        List<DeletedMappingModels> deletedMappingModelsAfter = deletedMappingModelsDao.findAll();
        assertThat(deletedMappingModelsAfter).hasSize(1);
        assertThat(deletedMappingModelsAfter)
                .extracting("modelId", "status")
                .containsOnly(new Tuple(expectedFailedModelId, CleanupStatus.READY_FOR_MBOC));

        //проверить что успешные модели в deleted_mapping_models_result
        List<DeletedMappingModelsResult> deletedMappingModelsResults = deletedMappingModelsResultDao.findAll();
        assertThat(deletedMappingModelsResults).hasSize(testModels.size() - 1);
        assertThat(deletedMappingModelsResults)
                .extracting(DeletedMappingModelsResult::getStatus)
                .containsOnly(CleanupStatus.SUCCESS);
        assertThat(deletedMappingModelsResults)
                .extracting(DeletedMappingModelsResult::getModelId)
                .containsExactlyInAnyOrderElementsOf(expectedSuccessfulModelIds);

        //проверить что все мапинги были отправлены в ручку mboMappingsService.addOfferToContentProcessing
        ArgumentCaptor<MboMappings.AddToContentProcessingRequest> argumentCaptor =
                ArgumentCaptor.forClass(MboMappings.AddToContentProcessingRequest.class);
        verify(mboMappingsService, times(1)).addOfferToContentProcessing(argumentCaptor.capture());
        MboMappings.AddToContentProcessingRequest request = argumentCaptor.getValue();
        assertThat(request.getBusinessSkuKeyList())
                .extracting("businessId", "offerId")
                .containsExactlyInAnyOrder(
                        new Tuple(Math.toIntExact(OWNER_ID_1), SHOP_SKU1),
                        new Tuple(Math.toIntExact(OWNER_ID_2), SHOP_SKU2),
                        new Tuple(Math.toIntExact(OWNER_ID_3), SHOP_SKU3),
                        new Tuple(Math.toIntExact(OWNER_ID_4), SHOP_SKU4)
                );
    }

    @Test
    public void whenModelNotFoundThenMoveToResult() {
        // крайние кейсы - нет модели => считаем успешно обработано (делать то ничего не надо)

        DeletedMappingModels testModel = deletedMappingModels.get(MODEL_ID4);
        deletedMappingModelsDao.insert(testModel);
        // null - КИ успешно добавит все мапинги
        setupMappingsServiceMockErrorOnShopSku(null);

        sendToMbocModelsTask.execute(null);

        //проверить что этой модели больше нет в deleted_mapping_models
        List<DeletedMappingModels> deletedMappingModelsAfter =
                deletedMappingModelsDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsAfter).hasSize(0);

        //проверить что эта модель теперь в deleted_mapping_models_result в статусе SUCCESS
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsResults).hasSize(1);
        assertThat(deletedMappingModelsResults)
                .extracting(DeletedMappingModelsResult::getStatus)
                .containsOnly(CleanupStatus.SUCCESS);

        //проверить что не было обращений в ручку mboMappingsService.addOfferToContentProcessing
        verify(mboMappingsService, never())
                .addOfferToContentProcessing(any(MboMappings.AddToContentProcessingRequest.class));
    }

    @Test
    public void whenModelHasNoSkuThenMoveToResult() {
        // крайние кейсы - нет ску => считаем успешно обработано

        DeletedMappingModels testModel = deletedMappingModels.get(MODEL_ID3);
        deletedMappingModelsDao.insert(testModel);
        // null - КИ успешно добавит все мапинги
        setupMappingsServiceMockErrorOnShopSku(null);

        sendToMbocModelsTask.execute(null);

        //проверить что этой модели больше нет в deleted_mapping_models
        List<DeletedMappingModels> deletedMappingModelsAfter =
                deletedMappingModelsDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsAfter).hasSize(0);

        //проверить что эта модель теперь в deleted_mapping_models_result в статусе SUCCESS
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsResults).hasSize(1);
        assertThat(deletedMappingModelsResults)
                .extracting(DeletedMappingModelsResult::getStatus)
                .containsOnly(CleanupStatus.SUCCESS);

        //проверить что не было обращений в ручку mboMappingsService.addOfferToContentProcessing
        verify(mboMappingsService, never())
                .addOfferToContentProcessing(any(MboMappings.AddToContentProcessingRequest.class));
    }

    @Test
    public void whenModelHasNoMappingsThenMoveToResult() {
        // крайние кейсы - нет мапингов => считаем успешно обработано

        DeletedMappingModels testModel = deletedMappingModels.get(MODEL_ID5);
        deletedMappingModelsDao.insert(testModel);
        // null - КИ успешно добавит все мапинги
        setupMappingsServiceMockErrorOnShopSku(null);

        sendToMbocModelsTask.execute(null);

        //проверить что этой модели больше нет в deleted_mapping_models
        List<DeletedMappingModels> deletedMappingModelsAfter =
                deletedMappingModelsDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsAfter).hasSize(0);

        //проверить что эта модель теперь в deleted_mapping_models_result в статусе SUCCESS
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(testModel.getModelId());
        assertThat(deletedMappingModelsResults).hasSize(1);
        assertThat(deletedMappingModelsResults)
                .extracting(DeletedMappingModelsResult::getStatus)
                .containsOnly(CleanupStatus.SUCCESS);

        //проверить что не было обращений в ручку mboMappingsService.addOfferToContentProcessing
        verify(mboMappingsService, never())
                .addOfferToContentProcessing(any(MboMappings.AddToContentProcessingRequest.class));
    }

    private void setupModelStorageMock(Map<Long, ModelStorage.Model> modelsMap) {
        when(modelStorageHelper.findModelsMap(any(Set.class)))
            .thenAnswer(invocationOnMock -> {
                Set<Long> ids = invocationOnMock.getArgument(0, Set.class);
                return ids.stream()
                    .filter(modelsMap::containsKey)
                    .collect(Collectors.toMap(Function.identity(), modelsMap::get));
            });
    }

    private MboMappingsServiceMock setupMappingsServiceMock() {
        MboMappingsServiceMock result = spy(new MboMappingsServiceMock());
        result.addMapping(HID, Math.toIntExact(OWNER_ID_1), SHOP_SKU1, PSKU_ID1);
        result.addMapping(HID, Math.toIntExact(OWNER_ID_2), SHOP_SKU2, PSKU_ID1);
        result.addMapping(HID, Math.toIntExact(OWNER_ID_3), SHOP_SKU3, PSKU_ID2);
        result.addMapping(HID, Math.toIntExact(OWNER_ID_4), SHOP_SKU4, PSKU_ID3);
        return result;
    }

    private void setupMappingsServiceMockErrorOnShopSku(String failShopSku) {
        when(mboMappingsService.addOfferToContentProcessing(any(MboMappings.AddToContentProcessingRequest.class)))
            .thenAnswer(invocationOnMock -> {
                MboMappings.AddToContentProcessingRequest req =
                        invocationOnMock.getArgument(0, MboMappings.AddToContentProcessingRequest.class);

                List<MboMappings.AddToContentProcessingResponse.Result> results = req.getBusinessSkuKeyList()
                        .stream()
                        .map(k -> MboMappings.AddToContentProcessingResponse.Result.newBuilder()
                                .setBusinessSkuKey(k)
                                .setStatus(failShopSku != null && failShopSku.equals(k.getOfferId())
                                        ? MboMappings.AddToContentProcessingResponse.Status.ERROR
                                        : MboMappings.AddToContentProcessingResponse.Status.OK)
                                .build()
                        )
                        .collect(Collectors.toList());

                return MboMappings.AddToContentProcessingResponse.newBuilder()
                        .addAllResult(results)
                        .build();
            });
    }

    private Map<Long, DeletedMappingModels> prepareDeletedMappingModels() {
        return Map.of(
            MODEL_ID1, new DeletedMappingModels(MODEL_ID1, CleanupStatus.READY_FOR_MBOC, ts, ts, 1L),
            MODEL_ID2, new DeletedMappingModels(MODEL_ID2, CleanupStatus.READY_FOR_MBOC, ts, ts, 1L),
            MODEL_ID3, new DeletedMappingModels(MODEL_ID3, CleanupStatus.READY_FOR_MBOC, ts, ts, 1L),
            MODEL_ID4, new DeletedMappingModels(MODEL_ID4, CleanupStatus.READY_FOR_MBOC, ts, ts, 1L),
            MODEL_ID5, new DeletedMappingModels(MODEL_ID5, CleanupStatus.READY_FOR_MBOC, ts, ts, 1L)
        );
    }

    private Map<Long, ModelStorage.Model> prepareModels() {
        Map<Long, ModelStorage.Model> result = new HashMap<>();

        ModelStorage.Model model1 =
            generateModel(MODEL_ID1).toBuilder()
                .addRelations(ModelStorage.Relation.newBuilder()
                    .setId(PSKU_ID1)
                    .setType(ModelStorage.RelationType.SKU_MODEL).build())
                .addRelations(ModelStorage.Relation.newBuilder()
                    .setId(PSKU_ID2)
                    .setType(ModelStorage.RelationType.SKU_MODEL).build())
                .build();

        ModelStorage.Model psku1 = generateModel(PSKU_ID1);
        ModelStorage.Model psku2 = generateModel(PSKU_ID2);

        ModelStorage.Model model2 =
                generateModel(MODEL_ID2).toBuilder()
                        .addRelations(ModelStorage.Relation.newBuilder()
                                .setId(PSKU_ID3)
                                .setType(ModelStorage.RelationType.SKU_MODEL).build())
                        .addRelations(ModelStorage.Relation.newBuilder()
                                .setId(PSKU_ID4)
                                .setType(ModelStorage.RelationType.SKU_MODEL).build())
                        .build();

        ModelStorage.Model psku3 = generateModel(PSKU_ID3);
        ModelStorage.Model psku4 = generateModel(PSKU_ID4);

        ModelStorage.Model model3 = generateModel(MODEL_ID3);

        ModelStorage.Model model4 =
                generateModel(MODEL_ID5).toBuilder()
                        .addRelations(ModelStorage.Relation.newBuilder()
                                .setId(PSKU_ID5)
                                .setType(ModelStorage.RelationType.SKU_MODEL).build())
                        .addRelations(ModelStorage.Relation.newBuilder()
                                .setId(PSKU_ID6)
                                .setType(ModelStorage.RelationType.SKU_MODEL).build())
                        .build();

        ModelStorage.Model psku5 = generateModel(PSKU_ID5);
        ModelStorage.Model psku6 = generateModel(PSKU_ID6);

        result.put(MODEL_ID1, model1);
        result.put(PSKU_ID1, psku1);
        result.put(PSKU_ID2, psku2);

        result.put(MODEL_ID2, model2);
        result.put(PSKU_ID3, psku3);
        result.put(PSKU_ID4, psku4);

        result.put(MODEL_ID3, model3);

        result.put(MODEL_ID5, model4);
        result.put(PSKU_ID5, psku5);
        result.put(PSKU_ID6, psku6);
        return result;
    }

    private ModelStorage.Model generateModel(long id) {
        return ModelStorage.Model.newBuilder()
                .setId(id)
                .build();
    }
}
