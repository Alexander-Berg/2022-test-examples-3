package ru.yandex.market.psku.postprocessor.bazinga.dna;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.ModelProtoUtils;
import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsQueueDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsResultDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ExternalRequestResponseDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.RemovedNomappingModelsDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.CleanupStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.RequestStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModels;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModelsResult;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ExternalRequestResponse;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.RemovedNomappingModels;
import ru.yandex.market.psku.postprocessor.service.dna.CleanupDeletedOffersDataService;
import ru.yandex.market.psku.postprocessor.service.dna.ModelCleaningService;
import ru.yandex.market.psku.postprocessor.service.dna.ModelSaveService;
import ru.yandex.market.psku.postprocessor.service.dna.RedundantOwnerIdsExtractionService;
import ru.yandex.market.psku.postprocessor.service.dna.RedundantOwnersCleaningService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.psku.postprocessor.ModelsGenerator.generateModelWithRelations;

public class ProcessModelsTaskTest extends BaseDBTest {
    private static final int HID = 91491;
    private static final String SHOP_SKU1 = "SHOP_SKU1";
    private static final String SHOP_SKU2 = "SHOP_SKU2";
    private static final long EXISTING_MODEL_ID = 200501L;
    private static final long REMOVED_MODEL_ID = 200502L;
    private static final long NEW_MODEL_ID = 11111L;
    private static final long EXISTING_PSKU_ID1 = 100501L;
    private static final long EXISTING_PSKU_ID2 = 100502L;
    private static final long EXISTING_PSKU_ID3 = 100503L;
    private static final long EXISTING_MSKU_ID1 = 100504L;
    private static final int GROUP_ID = 12;
    private static final int GROUP_ID_1 = 11;

    private static final long PARAM_ID_1 = 1L;
    private static final long PARAM_ID_2 = 2L;
    private static final long PARAM_ID_3 = 3L;
    private static final long PARAM_ID_4 = 4L;

    private static final long OWNER_ID_1 = 1L;
    private static final long OWNER_ID_2 = 2L;

    private static final long QUEUE_ID_1 = 33L;
    private static final long QUEUE_ID_2 = 34L;
    private static final long QUEUE_ID_3 = 35L;
    private static final long QUEUE_ID_4 = 36L;

    private final Timestamp ts = Timestamp.from(Instant.now());
    private DeletedMappingModels deletedMappingModel;
    private DeletedMappingModels removedModel;
    private Map<Long, ModelStorage.Model> modelsMap;

    private MboMappingsServiceMock mboMappingsServiceMock = new MboMappingsServiceMock();
    private RedundantOwnerIdsExtractionService ownerIdsExtractionService;
    private RedundantOwnersCleaningService redundantOwnersCleaningService;
    private ModelCleaningService modelCleaningService;
    private CleanupDeletedOffersDataService addDataService;
    private ActivateModelProcessingTask activateTask;
    private ModelStorageHelper modelStorageHelper;
    private ModelSaveService modelSaveService;
    private ProcessModelsTask processModelsTask;
    @Autowired
    private DeletedMappingModelsQueueDao deletedMappingModelsQueueDao;
    @Autowired
    private DeletedMappingModelsDao deletedMappingModelsDao;
    @Autowired
    private DeletedMappingModelsResultDao deletedMappingModelsResultDao;
    @Autowired
    private RemovedNomappingModelsDao removedNomappingModelsDao;
    @Autowired
    private ExternalRequestResponseDao externalRequestResponseDao;


    @Before
    public void setUp() {
        modelStorageHelper = Mockito.mock(ModelStorageHelper.class);
        addDataService = new CleanupDeletedOffersDataService(deletedMappingModelsQueueDao, modelStorageHelper);
        activateTask = new ActivateModelProcessingTask(deletedMappingModelsDao, deletedMappingModelsQueueDao);
        modelSaveService = new ModelSaveService(modelStorageHelper, externalRequestResponseDao);
        ownerIdsExtractionService =
                new RedundantOwnerIdsExtractionService(new MboMappingsServiceHelper(mboMappingsServiceMock));
        redundantOwnersCleaningService = new RedundantOwnersCleaningService();
        modelCleaningService = new ModelCleaningService(ownerIdsExtractionService, redundantOwnersCleaningService);
        processModelsTask =
                new ProcessModelsTask(deletedMappingModelsDao,
                        deletedMappingModelsResultDao,
                        modelStorageHelper,
                        modelSaveService,
                        removedNomappingModelsDao,
                        modelCleaningService);
        deletedMappingModel = new DeletedMappingModels(EXISTING_MODEL_ID, CleanupStatus.READY_FOR_PROCESSING, ts, ts,
                1L);

        removedModel = new DeletedMappingModels(REMOVED_MODEL_ID, CleanupStatus.READY_FOR_PROCESSING, ts, ts, 2L);

        modelsMap = new HashMap<>();
        List<ModelStorage.ParameterValue> parameterValueList = List.of(generatePV(PARAM_ID_1, OWNER_ID_1));
        List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesisList =
                List.of(generateHypothesis(PARAM_ID_2, OWNER_ID_2));

        ModelStorage.Model model =
                generateModel(EXISTING_MODEL_ID,
                        parameterValueList,
                        parameterValueHypothesisList,
                        ModelStorage.ModelType.GURU,
                        ModelStorage.ModelType.PARTNER
                ).toBuilder()
                        .addRelations(ModelStorage.Relation.newBuilder()
                                .setId(EXISTING_PSKU_ID1)
                                .setType(ModelStorage.RelationType.SKU_MODEL).build()).build();

        ModelStorage.Model psku = generateModel(EXISTING_PSKU_ID1,
                List.of(generatePV(PARAM_ID_3, OWNER_ID_2)),
                List.of(generateHypothesis(PARAM_ID_4, OWNER_ID_1)),
                ModelStorage.ModelType.SKU,
                ModelStorage.ModelType.PARTNER_SKU
        );

        modelsMap.put(EXISTING_MODEL_ID, model);
        modelsMap.put(EXISTING_PSKU_ID1, psku);
        when(modelStorageHelper.findModelsWithChildrenMap(Set.of(REMOVED_MODEL_ID))).thenReturn(new HashMap<>());
        when(modelStorageHelper.findModelsWithChildrenMap(Set.of(EXISTING_MODEL_ID)))
                .thenReturn(Map.of(EXISTING_MODEL_ID, model, EXISTING_PSKU_ID1, psku));
    }

    @Test
    public void whenRedundantOwnersDoNotExistThenNoAction() {
        deletedMappingModelsDao.insert(deletedMappingModel);
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_1), SHOP_SKU1, EXISTING_PSKU_ID1);
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_2), SHOP_SKU2, EXISTING_PSKU_ID1);
        processModelsTask.execute(null);
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(status(deletedMappingModelsResults))
                .isEqualTo(CleanupStatus.NO_ACTION);
        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(0);
    }

    @Test
    public void whenModelsAreSavedOkAndModelStatusUpdatedExpectFinishedStatus() {

        mockSuccessfulCardApiAnswer();
        Set<Long> deletedModels = Collections.emptySet();
        CleanupStatus resultStatus = CleanupStatus.READY_FOR_MBOC;

        deletedMappingModelsDao.insert(deletedMappingModel);
        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());

        modelSaveService.saveModels(
                context,
                models,
                (isSuccessful, response) -> processModelsTask.processSaveResponse(
                    deletedMappingModel, 1, deletedModels, resultStatus, response, isSuccessful)
        );

        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);

        List<DeletedMappingModels> deletedMappingModels = deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModels).hasSize(1);
        assertThat(deletedMappingModels.get(0).getStatus()).isEqualTo(CleanupStatus.READY_FOR_MBOC);
    }

    @Test
    public void fullSuccessFlow() {

        mockSuccessfulCardApiAnswer();
        ModelStorage.Model sku1 = generateModelWithRelations(EXISTING_PSKU_ID1,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(EXISTING_MODEL_ID), HID
        ).toBuilder()
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .build();
        when(modelStorageHelper.findModels(any()))
                .thenReturn(List.of(sku1));

        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_1), SHOP_SKU1, EXISTING_PSKU_ID1);

        addDataService.addToProcessQueue(List.of(EXISTING_PSKU_ID1));
        assertThat(deletedMappingModelsQueueDao.findAll().size()).isEqualTo(1);

        activateTask.execute(null);
        assertThat(deletedMappingModelsQueueDao.findAll().size()).isEqualTo(0);
        assertThat(deletedMappingModelsDao.fetchByStatus(CleanupStatus.READY_FOR_PROCESSING).size())
                .isEqualTo(1);

        // uses mocked cardapi response
        processModelsTask.execute(null);

        assertThat(externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID).get(0).getStatus())
                .isEqualTo(RequestStatus.FINISHED);
        assertThat(deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID).get(0).getStatus())
                .isEqualTo(CleanupStatus.READY_FOR_MBOC);
        assertThat(deletedMappingModelsQueueDao.findAll().size()).isEqualTo(0);
        assertThat(deletedMappingModelsResultDao.findAll().size()).isEqualTo(0);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(0);
    }

    //        model           удалён маппинг (sku2, group_id1)        model       model1
    //      /    |   \        ===============================>        /   \         |
    //    sku1  sku2  sku3                                          sku1  sku2     sku3
    //     \     / \    |
    //    group_id  group_id1
    @Test
    public void fullSuccessFlowForGroups() {
        List<ModelStorage.ParameterValue> parameterValueList = List.of(generatePV(PARAM_ID_1, OWNER_ID_1));
        List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesisList =
                List.of(generateHypothesis(PARAM_ID_2, OWNER_ID_2));

        ModelStorage.Model sku3 = generateModelWithRelations(EXISTING_PSKU_ID3,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(EXISTING_MODEL_ID), HID
        ).toBuilder()
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .build();
        //Хотим почистить гипотезу от OWNER_2
        ModelStorage.Model sku2 = generateModelWithRelations(EXISTING_PSKU_ID2,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(EXISTING_MODEL_ID), HID
        ).toBuilder()
                .addAllParameterValues(parameterValueList)
                .addAllParameterValueHypothesis(parameterValueHypothesisList)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .build();
        ModelStorage.Model sku1 = generateModelWithRelations(EXISTING_PSKU_ID1,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(EXISTING_MODEL_ID), HID
        ).toBuilder()
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .build();
        ModelStorage.Model model = generateModelWithRelations(EXISTING_MODEL_ID,
                ModelStorage.RelationType.SKU_MODEL,
                Set.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2, EXISTING_PSKU_ID3), HID
        ).toBuilder()
                .setCurrentType(ModelStorage.ModelType.GURU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER.name())
                .build();
        modelsMap.clear();
        modelsMap.put(EXISTING_MODEL_ID, model);
        modelsMap.put(EXISTING_PSKU_ID1, sku1);
        modelsMap.put(EXISTING_PSKU_ID2, sku2);
        modelsMap.put(EXISTING_PSKU_ID3, sku3);

        ModelStorage.Model newModel = generateModelWithRelations(NEW_MODEL_ID,
                ModelStorage.RelationType.SKU_MODEL,
                Set.of(EXISTING_PSKU_ID3), HID
        ).toBuilder()
                .setCurrentType(ModelStorage.ModelType.GURU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER.name())
                .build();
        mockSuccessfulCardApiAnswer(List.of(model, sku1, sku2, sku3, newModel));

        when(modelStorageHelper.findModels(any()))
                .thenReturn(List.of(sku1, sku2, sku3));
        when(modelStorageHelper.findModelsWithChildrenMap(Set.of(EXISTING_MODEL_ID)))
                .thenReturn(Map.of(EXISTING_MODEL_ID, model, EXISTING_PSKU_ID1, sku1,
                        EXISTING_PSKU_ID2, sku2, EXISTING_PSKU_ID3, sku3));

        mboMappingsServiceMock.addOfferMapping(Math.toIntExact(OWNER_ID_1),
                SHOP_SKU1, HID, "", "", "",
                SupplierOffer.Offer.InternalProcessingStatus.AUTO_PROCESSED,
                EXISTING_PSKU_ID1, GROUP_ID);
        mboMappingsServiceMock.addOfferMapping(Math.toIntExact(OWNER_ID_1),
                SHOP_SKU2, HID, "", "", "",
                SupplierOffer.Offer.InternalProcessingStatus.AUTO_PROCESSED,
                EXISTING_PSKU_ID2, GROUP_ID);
        mboMappingsServiceMock.addOfferMapping(Math.toIntExact(OWNER_ID_2),
                SHOP_SKU1, HID, "", "", "",
                SupplierOffer.Offer.InternalProcessingStatus.AUTO_PROCESSED,
                EXISTING_PSKU_ID3, GROUP_ID_1);

        addDataService.addToProcessQueue(List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2, EXISTING_PSKU_ID3));
        assertThat(deletedMappingModelsQueueDao.findAll().size()).isEqualTo(1);

        activateTask.execute(null);
        assertThat(deletedMappingModelsQueueDao.findAll().size()).isEqualTo(0);
        assertThat(deletedMappingModelsDao.fetchByStatus(CleanupStatus.READY_FOR_PROCESSING).size())
                .isEqualTo(1);

        // uses mocked cardapi response
        processModelsTask.execute(null);

        assertThat(externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID).get(0).getStatus())
                .isEqualTo(RequestStatus.FINISHED);
        assertThat(deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID).get(0).getStatus())
                .isEqualTo(CleanupStatus.READY_FOR_MBOC);
        assertThat(deletedMappingModelsDao.fetchByModelId(NEW_MODEL_ID).get(0).getStatus())
                .isEqualTo(CleanupStatus.READY_FOR_MBOC);
        assertThat(deletedMappingModelsQueueDao.findAll().size()).isEqualTo(0);
        assertThat(deletedMappingModelsResultDao.findAll().size()).isEqualTo(0);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(0);
    }

    @Test
    public void whenSuccessFlowForGroupsWithMskuDoNotRemoveLinkToMsku() {
        // тест что не затрагиваются связи на МСКУ
        Map<Long, ModelStorage.Model> models = prepareModelsHierarchyAndMockModelStorageFind(
                EXISTING_MODEL_ID, ModelStorage.ModelType.PARTNER,
                List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2, EXISTING_PSKU_ID3, EXISTING_MSKU_ID1),
                List.of(ModelStorage.ModelType.PARTNER_SKU, ModelStorage.ModelType.PARTNER_SKU,
                        ModelStorage.ModelType.PARTNER_SKU, ModelStorage.ModelType.SKU),
                Map.of(EXISTING_PSKU_ID2, List.of(generatePV(PARAM_ID_1, OWNER_ID_1))),
                Map.of(EXISTING_PSKU_ID2, List.of(generateHypothesis(PARAM_ID_2, OWNER_ID_2))));

        mockSuccessfulCardApiAnswer();

        mboMappingsServiceMock.addOfferMapping(Math.toIntExact(OWNER_ID_1),
                SHOP_SKU1, HID, "", "", "",
                SupplierOffer.Offer.InternalProcessingStatus.AUTO_PROCESSED,
                EXISTING_PSKU_ID1, GROUP_ID);
        mboMappingsServiceMock.addOfferMapping(Math.toIntExact(OWNER_ID_1),
                SHOP_SKU2, HID, "", "", "",
                SupplierOffer.Offer.InternalProcessingStatus.AUTO_PROCESSED,
                EXISTING_PSKU_ID2, GROUP_ID);
        mboMappingsServiceMock.addOfferMapping(Math.toIntExact(OWNER_ID_2),
                SHOP_SKU1, HID, "", "", "",
                SupplierOffer.Offer.InternalProcessingStatus.AUTO_PROCESSED,
                EXISTING_PSKU_ID3, GROUP_ID_1);

        deletedMappingModelsDao.insert(deletedMappingModel);

        processModelsTask.execute(null);

        //then
        List<DeletedMappingModels> deletedMappingModels =
                deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModels).hasSize(1);
        assertThat(deletedMappingModels)
                .extracting("status")
                .containsOnly(CleanupStatus.READY_FOR_MBOC);
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModelsResults).hasSize(0);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(0);

        //убедиться что в родительской модели остались нужные связи
        verifyModelChildrenExactlyInSaveRequest(EXISTING_MODEL_ID,
                List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2, EXISTING_MSKU_ID1));
        // в новую модель переехала ПСКУ 3
        verifyModelChildrenExactlyInSaveRequest(-1L, List.of(EXISTING_PSKU_ID3));
    }

    @Test
    public void whenNoBoundOffersAndNoChildPskuThenNoMappings() {
        // нет мапингов и нет дочерних ПСКУ - ничего не делаем, результат NO_MAPPINGS
        Map<Long, ModelStorage.Model> models = prepareModelsHierarchyAndMockModelStorageFind(
                EXISTING_MODEL_ID, ModelStorage.ModelType.PARTNER,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        deletedMappingModelsDao.insert(deletedMappingModel);

        processModelsTask.execute(null);

        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(status(deletedMappingModelsResults))
                .isEqualTo(CleanupStatus.NO_MAPPINGS);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(0);
        verify(modelStorageHelper, never()).executeSaveModelRequest(any());
    }

    @Test
    public void whenNoBoundOffersAndAllChildsArePSKUThenNoMappings() {
        // П-модель, все дочерние ску - пску без мапингов - удалять пску и модель
        Map<Long, ModelStorage.Model> models = prepareModelsHierarchyAndMockModelStorageFind(
                EXISTING_MODEL_ID, ModelStorage.ModelType.PARTNER,
                List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2),
                List.of(ModelStorage.ModelType.PARTNER_SKU, ModelStorage.ModelType.PARTNER_SKU),
                Collections.emptyMap(),
                Collections.emptyMap());
        mockSuccessfulCardApiAnswer();
        deletedMappingModelsDao.insert(deletedMappingModel);

        processModelsTask.execute(null);

        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(status(deletedMappingModelsResults))
                .isEqualTo(CleanupStatus.NO_MAPPINGS);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(3);
        assertThat(nomappingModels)
                .extracting("modelId", "isPsku", "queueModelId", "queueId")
                .containsExactlyInAnyOrder(
                        new Tuple(EXISTING_MODEL_ID, false, deletedMappingModel.getModelId(),
                                deletedMappingModel.getQueueId()),
                        new Tuple(EXISTING_PSKU_ID1, true, deletedMappingModel.getModelId(),
                                deletedMappingModel.getQueueId()),
                        new Tuple(EXISTING_PSKU_ID2, true, deletedMappingModel.getModelId(),
                                deletedMappingModel.getQueueId())
                );

        // проверить запрос сохранения
        verifySaveDeletedModels(Map.of(EXISTING_MODEL_ID, true,
                EXISTING_PSKU_ID1, true,
                EXISTING_PSKU_ID2, true));
    }

    @Test
    public void whenNoBoundOffersAndGuruModelAndAllChildsArePSKUThenNoMappings() {
        // гуру-модель, все дочерни ску - пску без мапингов - удалять только пску
        Map<Long, ModelStorage.Model> models = prepareModelsHierarchyAndMockModelStorageFind(
                EXISTING_MODEL_ID, ModelStorage.ModelType.GURU,
                List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2),
                List.of(ModelStorage.ModelType.PARTNER_SKU, ModelStorage.ModelType.PARTNER_SKU),
                Collections.emptyMap(),
                Collections.emptyMap());
        mockSuccessfulCardApiAnswer();
        deletedMappingModelsDao.insert(deletedMappingModel);

        processModelsTask.execute(null);

        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(status(deletedMappingModelsResults))
                .isEqualTo(CleanupStatus.NO_MAPPINGS);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(2);
        assertThat(nomappingModels)
                .extracting("modelId", "isPsku", "queueModelId", "queueId")
                .containsExactlyInAnyOrder(
                        new Tuple(EXISTING_PSKU_ID1, true, deletedMappingModel.getModelId(),
                                deletedMappingModel.getQueueId()),
                        new Tuple(EXISTING_PSKU_ID2, true, deletedMappingModel.getModelId(),
                                deletedMappingModel.getQueueId())
                );

        // проверить запрос сохранения
        verifySaveDeletedModels(Map.of(EXISTING_MODEL_ID, false,
                EXISTING_PSKU_ID1, true,
                EXISTING_PSKU_ID2, true));
        //убедиться что в родительской модели убрали связи на удаляемые ску
        verifyModelHasNoChildInSaveRequest(EXISTING_MODEL_ID, List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2));
    }

    @Test
    public void whenNoBoundOffersAndSomeChildsAreMSKUThenNoMappings() {
        // есть дочерни ску и пску без мапингов - удалять только пску
        Map<Long, ModelStorage.Model> models = prepareModelsHierarchyAndMockModelStorageFind(
                EXISTING_MODEL_ID, ModelStorage.ModelType.PARTNER,
                List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2),
                List.of(ModelStorage.ModelType.SKU, ModelStorage.ModelType.PARTNER_SKU),
                Collections.emptyMap(),
                Collections.emptyMap());
        mockSuccessfulCardApiAnswer();
        deletedMappingModelsDao.insert(deletedMappingModel);

        processModelsTask.execute(null);

        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(status(deletedMappingModelsResults))
                .isEqualTo(CleanupStatus.NO_MAPPINGS);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(1);
        assertThat(nomappingModels)
                .extracting("modelId", "isPsku", "queueModelId", "queueId")
                .containsExactlyInAnyOrder(
                        new Tuple(EXISTING_PSKU_ID2, true, deletedMappingModel.getModelId(),
                                deletedMappingModel.getQueueId())
                );

        // проверить запрос сохранения
        verifySaveDeletedModels(Map.of(EXISTING_MODEL_ID, false,
                EXISTING_PSKU_ID2, true));
        //убедиться что в родительской модели убрали связи на удаляемые ску
        verifyModelChildrenExactlyInSaveRequest(EXISTING_MODEL_ID, List.of(EXISTING_PSKU_ID1));
    }

    @Test
    public void whenSomePskuHasNoOffersAndSomeCleanThenRemoveAndRedyForMboc() {
        // есть дочерни ску и пску, часть пску без мапингов, часть чистятся - удалять пску без мапингов,
        // результат READY_FOR_MBOC

        //when
        Map<Long, ModelStorage.Model> models = prepareModelsHierarchyAndMockModelStorageFind(
                EXISTING_MODEL_ID, ModelStorage.ModelType.PARTNER,
                List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2),
                List.of(ModelStorage.ModelType.PARTNER_SKU, ModelStorage.ModelType.PARTNER_SKU),
                Map.of(EXISTING_PSKU_ID1, List.of(generatePV(PARAM_ID_1, OWNER_ID_1))),
                Map.of(EXISTING_PSKU_ID1, List.of(generateHypothesis(PARAM_ID_2, OWNER_ID_2))));

        mockSuccessfulCardApiAnswer();
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_1), SHOP_SKU1, EXISTING_PSKU_ID1);
        deletedMappingModelsDao.insert(deletedMappingModel);

        processModelsTask.execute(null);

        //then
        List<DeletedMappingModels> deletedMappingModels =
                deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModels).hasSize(1);
        assertThat(deletedMappingModels)
                .extracting("status")
                .containsOnly(CleanupStatus.READY_FOR_MBOC);
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModelsResults).hasSize(0);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(1);
        assertThat(nomappingModels)
                .extracting("modelId", "isPsku", "queueModelId", "queueId")
                .containsExactlyInAnyOrder(
                        new Tuple(EXISTING_PSKU_ID2, true, deletedMappingModel.getModelId(),
                                deletedMappingModel.getQueueId())
                );

        // проверить запрос сохранения
        verifySaveDeletedModels(Map.of(EXISTING_MODEL_ID, false,
                EXISTING_PSKU_ID1, false,
                EXISTING_PSKU_ID2, true));
        //убедиться что в родительской модели убрали связи на удаляемые ску
        verifyModelChildrenExactlyInSaveRequest(EXISTING_MODEL_ID, List.of(EXISTING_PSKU_ID1));
    }

    @Test
    public void whenSomePskuHasNoOffersAndNoCleanThenRemoveAndSuccess() {
        // есть дочерни ску и пску, часть пску без мапингов, никто не чистится - удалять пску без мапингов,
        // результат SUCCESS

        //when
        Map<Long, ModelStorage.Model> models = prepareModelsHierarchyAndMockModelStorageFind(
                EXISTING_MODEL_ID, ModelStorage.ModelType.PARTNER,
                List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2),
                List.of(ModelStorage.ModelType.PARTNER_SKU, ModelStorage.ModelType.PARTNER_SKU),
                Map.of(EXISTING_PSKU_ID1, List.of(generatePV(PARAM_ID_1, OWNER_ID_1))),
                Map.of(EXISTING_PSKU_ID1, List.of(generateHypothesis(PARAM_ID_2, OWNER_ID_2))));

        mockSuccessfulCardApiAnswer();
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_1), SHOP_SKU1, EXISTING_PSKU_ID1);
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_2), SHOP_SKU2, EXISTING_PSKU_ID1);
        deletedMappingModelsDao.insert(deletedMappingModel);

        processModelsTask.execute(null);

        //then
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModelsResults).hasSize(1);
        assertThat(deletedMappingModelsResults)
                .extracting("status")
                .containsOnly(CleanupStatus.SUCCESS);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(1);
        assertThat(nomappingModels)
                .extracting("modelId", "isPsku", "queueModelId", "queueId")
                .containsExactlyInAnyOrder(
                        new Tuple(EXISTING_PSKU_ID2, true, deletedMappingModel.getModelId(),
                                deletedMappingModel.getQueueId())
                );

        // проверить запрос сохранения
        verifySaveDeletedModels(Map.of(EXISTING_MODEL_ID, false,
                EXISTING_PSKU_ID2, true));
        //убедиться что в родительской модели убрали связи на удаляемые ску
        verifyModelChildrenExactlyInSaveRequest(EXISTING_MODEL_ID, List.of(EXISTING_PSKU_ID1));
    }

    @Test
    public void whenSomePskuHasNoOffersAndSaeFailThenNoChangesAndReadyForProcessing() {
        // при падении на сохранении переходим в READY_FOR_PROCESSING и ничего не сохраняется в удаленные

        //when
        Map<Long, ModelStorage.Model> models = prepareModelsHierarchyAndMockModelStorageFind(
                EXISTING_MODEL_ID, ModelStorage.ModelType.PARTNER,
                List.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2),
                List.of(ModelStorage.ModelType.PARTNER_SKU, ModelStorage.ModelType.PARTNER_SKU),
                Map.of(EXISTING_PSKU_ID1, List.of(generatePV(PARAM_ID_1, OWNER_ID_1))),
                Map.of(EXISTING_PSKU_ID1, List.of(generateHypothesis(PARAM_ID_2, OWNER_ID_2))));

        mockExceptionInCardApiOnSave();

        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_1), SHOP_SKU1, EXISTING_PSKU_ID1);
        mboMappingsServiceMock.addMapping(HID, Math.toIntExact(OWNER_ID_2), SHOP_SKU2, EXISTING_PSKU_ID1);
        deletedMappingModelsDao.insert(deletedMappingModel);

        processModelsTask.execute(null);

        //then
        List<DeletedMappingModels> deletedMappingModels =
                deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModels).hasSize(1);
        assertThat(deletedMappingModels)
                .extracting("status")
                .containsOnly(CleanupStatus.READY_FOR_PROCESSING);
        List<DeletedMappingModelsResult> deletedMappingModelsResults =
                deletedMappingModelsResultDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModelsResults).hasSize(0);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(0);
    }


    @Test
    public void whenNoModelsThenNoAction() {
        deletedMappingModelsDao.insert(removedModel);
        processModelsTask.execute(null);
        List<DeletedMappingModelsResult> results =
                deletedMappingModelsResultDao.fetchByModelId(REMOVED_MODEL_ID);
        assertThat(status(results))
                .isEqualTo(CleanupStatus.NO_ACTION);

        List<RemovedNomappingModels> nomappingModels = removedNomappingModelsDao.findAll();
        assertThat(nomappingModels).hasSize(0);
    }

    @Test
    public void whenResultForModelAlreadyExistsThenUpdate() {
        Timestamp oldCreateDate = Timestamp.from(
                Instant.now().truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.DAYS)
        );
        Timestamp newCreateDate = Timestamp.from(Instant.now().truncatedTo(ChronoUnit.DAYS));

        deletedMappingModelsResultDao.insertResultWithStatus(
            new DeletedMappingModels(EXISTING_MODEL_ID, null, null, oldCreateDate, QUEUE_ID_1),
            CleanupStatus.NO_MAPPINGS
        );

        deletedMappingModelsResultDao.insertResultWithStatus(
            Stream.of(
                new DeletedMappingModels(NEW_MODEL_ID, null, null, newCreateDate, QUEUE_ID_2),
                new DeletedMappingModels(EXISTING_MODEL_ID, null, null, newCreateDate, QUEUE_ID_3),
                new DeletedMappingModels(REMOVED_MODEL_ID, null, null, newCreateDate, QUEUE_ID_4)
            ).collect(Collectors.toList()),
            CleanupStatus.SUCCESS
        );

        List<DeletedMappingModelsResult> results =
                deletedMappingModelsResultDao.findAll();
        assertThat(results).hasSize(3);
        assertThat(results).extracting("queueId", "modelId", "status", "createDate")
        .containsExactlyInAnyOrder(
                new Tuple(QUEUE_ID_2, NEW_MODEL_ID, CleanupStatus.SUCCESS, newCreateDate),
                new Tuple(QUEUE_ID_3, EXISTING_MODEL_ID, CleanupStatus.SUCCESS, newCreateDate),
                new Tuple(QUEUE_ID_4, REMOVED_MODEL_ID, CleanupStatus.SUCCESS, newCreateDate)
        );
    }

    @Test
    public void whenExceptionOnSavingModelExpectRequestCreatedStatusAndModelsReadyForProcessing() {
        // кейс при эксепшене - переводим в READY_FOR_PROCESSING
        mockExceptionInCardApiOnSave();
        Set<Long> deletedModels = Collections.emptySet();
        CleanupStatus resultStatus = CleanupStatus.READY_FOR_MBOC;

        deletedMappingModelsDao.insert(deletedMappingModel);
        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());

        modelSaveService.saveModels(
                context,
                models,
                (isSuccessful, response) -> processModelsTask.processSaveResponse(
                        deletedMappingModel, 1, deletedModels, resultStatus, response, isSuccessful)
        );

        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.CREATED);

        List<DeletedMappingModels> deletedMappingModels = deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModels).hasSize(1);
        assertThat(deletedMappingModels.get(0).getStatus()).isEqualTo(CleanupStatus.READY_FOR_PROCESSING);
    }

    @Test
    public void whenFatalStatusOnSavingModelExpectRequestFinishedStatusAndModelsReadyForProcessing() {
        // кейс при фатальных статусах на модели - переводим в READY_FOR_PROCESSING

        mockBadCardApiAnswerOnModel(EXISTING_PSKU_ID1, false,
                ModelStorage.OperationStatusType.MODEL_NOT_FOUND);

        Set<Long> deletedModels = Collections.emptySet();
        CleanupStatus resultStatus = CleanupStatus.READY_FOR_MBOC;

        deletedMappingModelsDao.insert(deletedMappingModel);
        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());

        modelSaveService.saveModels(
                context,
                models,
                (isSuccessful, response) -> processModelsTask.processSaveResponse(
                        deletedMappingModel, 1, deletedModels, resultStatus, response, isSuccessful)
        );

        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);

        List<DeletedMappingModels> deletedMappingModels = deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModels).hasSize(1);
        assertThat(deletedMappingModels.get(0).getStatus()).isEqualTo(CleanupStatus.READY_FOR_PROCESSING);
    }

    @Test
    public void whenThreeUnsuccessfulSavingNotBrokenModelExpectRequestFinishedStatusAndModelsReadyForProcessing() {
        // кейс при неуспехе после 3х сохранений моделей без перевода в broken - переводим в READY_FOR_PROCESSING

        mockBadCardApiAnswerOnModel(EXISTING_PSKU_ID1, true,
                ModelStorage.OperationStatusType.MODEL_MODIFIED);

        Set<Long> deletedModels = Collections.emptySet();
        CleanupStatus resultStatus = CleanupStatus.READY_FOR_MBOC;

        deletedMappingModelsDao.insert(deletedMappingModel);
        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());

        modelSaveService.saveModels(
                context,
                models,
                (isSuccessful, response) -> processModelsTask.processSaveResponse(
                        deletedMappingModel, 1, deletedModels, resultStatus, response, isSuccessful)
        );

        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);

        List<DeletedMappingModels> deletedMappingModels = deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModels).hasSize(1);
        assertThat(deletedMappingModels.get(0).getStatus()).isEqualTo(CleanupStatus.READY_FOR_PROCESSING);
    }

    @Test
    public void whenThreeUnsuccessfulSavingWithBrokenModelExpectRequestFinishedStatusAndModelsReadyForProcessing() {
        // кейс при неуспехе после 3х сохранений моделей с переводом в broken - - переводим в READY_FOR_PROCESSING

        mockBadCardApiAnswerOnModel(EXISTING_PSKU_ID1, true,
                ModelStorage.OperationStatusType.VALIDATION_ERROR);

        Set<Long> deletedModels = Collections.emptySet();
        CleanupStatus resultStatus = CleanupStatus.READY_FOR_MBOC;

        deletedMappingModelsDao.insert(deletedMappingModel);
        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());

        modelSaveService.saveModels(
                context,
                models,
                (isSuccessful, response) -> processModelsTask.processSaveResponse(
                        deletedMappingModel, 1, deletedModels, resultStatus, response, isSuccessful)
        );

        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);

        List<DeletedMappingModels> deletedMappingModels = deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModels).hasSize(1);
        assertThat(deletedMappingModels.get(0).getStatus()).isEqualTo(CleanupStatus.READY_FOR_PROCESSING);
    }

    @Test
    public void whenSomeModelsHaveValidationErrorExpectRequestFinishedStatusAndModelsReadyForMboc() {
        // кейс не ок статуса по некоторым ску и повторного соханения при этом с broken/published
        // => в итоге FINISHED и READY_FOR_MBOC

        mockBadCardApiAnswerOnModel(EXISTING_PSKU_ID1, false,
                ModelStorage.OperationStatusType.VALIDATION_ERROR);

        Set<Long> deletedModels = Collections.emptySet();
        CleanupStatus resultStatus = CleanupStatus.READY_FOR_MBOC;

        deletedMappingModelsDao.insert(deletedMappingModel);
        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());

        modelSaveService.saveModels(
                context,
                models,
                (isSuccessful, response) -> processModelsTask.processSaveResponse(
                        deletedMappingModel, 1, deletedModels, resultStatus, response, isSuccessful)
        );

        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);

        List<DeletedMappingModels> deletedMappingModels = deletedMappingModelsDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(deletedMappingModels).hasSize(1);
        assertThat(deletedMappingModels.get(0).getStatus()).isEqualTo(CleanupStatus.READY_FOR_MBOC);
    }

    private void mockSuccessfulCardApiAnswer() {
        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
            .thenAnswer(invocationOnMock ->
                new ModelStorageHelper.SaveGroupResponse(
                    invocationOnMock.getArgument(0, ModelCardApi.SaveModelsGroupRequest.class),
                    ModelCardApi.SaveModelsGroupResponse.newBuilder()
                        .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                            .setStatus(ModelStorage.OperationStatusType.OK))
                        .build()));
    }

    //Делаем список пар, т.к. важен порядок
    private void mockSuccessfulCardApiAnswer(List<ModelStorage.Model> modelList) {
        ModelCardApi.SaveModelsGroupOperationResponse.Builder response =
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder();
        for (ModelStorage.Model model : modelList) {
            response.addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                            .setModelId(model.getId())
                            .setModel(model)
                            .setStatus(ModelStorage.OperationStatusType.OK)
                            .setType(ModelStorage.OperationType.CHANGE)
                            .build())
                    .setStatus(ModelStorage.OperationStatusType.OK);
        }
        ModelCardApi.SaveModelsGroupResponse.Builder saveModelsGroupResponseBuilder =
                ModelCardApi.SaveModelsGroupResponse.newBuilder()
                        .addResponse(response);

        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
            .thenAnswer(invocationOnMock ->
                new ModelStorageHelper.SaveGroupResponse(
                    invocationOnMock.getArgument(0, ModelCardApi.SaveModelsGroupRequest.class),
                    saveModelsGroupResponseBuilder.build()));
    }

    private void mockExceptionInCardApiOnSave() {
        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
                .thenThrow(new RuntimeException("Terrible things happened"));
    }

    private void mockBadCardApiAnswerOnModel(long modelId, boolean alwaysError,
                                             ModelStorage.OperationStatusType modelStatus) {
        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
                .thenAnswer(invocationOnMock -> {
                    ModelCardApi.SaveModelsGroupRequest request =
                            invocationOnMock.getArgument(0, ModelCardApi.SaveModelsGroupRequest.class);

                    if (request.getModelsRequestCount() != 1) {
                        throw new IllegalStateException("Expecting exactly 1 request for a group save");
                    }

                    // будет возвращать ошибку если есть модели с заданным id И
                    // если alwaysError = true ИЛИ если интересующая модель еще не broken == true
                    Predicate<ModelStorage.Model> brokenModelTest = m -> (m.getId() == modelId)
                            && (alwaysError || !m.hasBroken() || !m.getBroken());

                    boolean isFailed = request.getModelsRequest(0).getModelsList()
                            .stream()
                            .anyMatch(brokenModelTest);
                    List<ModelStorage.OperationStatus> modelStatuses = request.getModelsRequest(0).getModelsList()
                            .stream()
                            .map(m -> ModelStorage.OperationStatus.newBuilder()
                                    .setModelId(m.getId())
                                    .setModel(m)
                                    // для ошибочных моделей ставим заданный статус
                                    // для остальных OK если не было ошибок, FAILED_MODEL_IN_GROUP если ошибки в других моделях
                                    .setStatus(brokenModelTest.test(m)
                                            ? modelStatus
                                            : (isFailed
                                            ? ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP
                                            : ModelStorage.OperationStatusType.OK)
                                    )
                                    .setType(ModelStorage.OperationType.CHANGE)
                                    .build()
                            )
                            .collect(Collectors.toList());
                    ModelStorage.OperationStatusType overallStatus = isFailed
                            ? ModelStorage.OperationStatusType.VALIDATION_ERROR
                            : ModelStorage.OperationStatusType.OK;

                    ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                            .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                                    .setStatus(overallStatus)
                                    .addAllRequestedModelsStatuses(modelStatuses)
                            )
                            .build();

                    return new ModelStorageHelper.SaveGroupResponse(request, response);
                });
    }


    private ModelStorage.Model generateModel(long id,
                                             List<ModelStorage.ParameterValue> parameterValueList,
                                             List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesisList,
                                             ModelStorage.ModelType currentType,
                                             ModelStorage.ModelType sourceType
                                             ) {
        return ModelStorage.Model.newBuilder()
                .setId(id)
                .setCurrentType(currentType.name())
                .setSourceType(sourceType.name())
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


    private static CleanupStatus status(List<DeletedMappingModelsResult> resultList) {
        return resultList.get(0).getStatus();
    }

    private Map<Long, ModelStorage.Model> prepareModelsHierarchyAndMockModelStorageFind(
            Long modelId,
            ModelStorage.ModelType modelSourceType,
            List<Long> skuIds,
            List<ModelStorage.ModelType> skuSourceTypes,
            Map<Long, List<ModelStorage.ParameterValue>> skuValues,
            Map<Long, List<ModelStorage.ParameterValueHypothesis>> skuHypothesis
    ) {
        Map<Long, ModelStorage.Model> result = new HashMap<>();

        ModelStorage.Model model = generateModelWithRelations(modelId, ModelStorage.RelationType.SKU_MODEL,
                new HashSet<>(skuIds), HID
        ).toBuilder()
                .setCurrentType(ModelStorage.ModelType.GURU.name())
                .setSourceType(modelSourceType.name())
                .build();

        result.put(modelId, model);

        for (int i = 0; i < skuIds.size(); i++) {
            ModelStorage.Model sku = generateModelWithRelations(skuIds.get(i),
                    ModelStorage.RelationType.SKU_PARENT_MODEL, Set.of(modelId), HID
            ).toBuilder()
                    .addAllParameterValues(skuValues.get(skuIds.get(i)) != null
                            ? skuValues.get(skuIds.get(i))
                            : Collections.emptyList())
                    .addAllParameterValueHypothesis(skuHypothesis.get(skuIds.get(i)) != null
                            ? skuHypothesis.get(skuIds.get(i))
                            : Collections.emptyList())
                    .setCurrentType(ModelStorage.ModelType.SKU.name())
                    .setSourceType(skuSourceTypes.get(i).name())
                    .build();

            result.put(skuIds.get(i), sku);
        }

        when(modelStorageHelper.findModelsWithChildrenMap(Set.of(modelId))).thenReturn(result);
        return result;
    }

    private void verifySaveDeletedModels(Map<Long, Boolean> modelToDeletedMap) {
        ArgumentCaptor<ModelCardApi.SaveModelsGroupRequest> argumentCaptor =
                ArgumentCaptor.forClass(ModelCardApi.SaveModelsGroupRequest.class);

        verify(modelStorageHelper, times(1)).executeSaveModelRequest(argumentCaptor.capture());
        ModelCardApi.SaveModelsGroupRequest capturesRequest = argumentCaptor.getValue();

        Map<Long, Boolean> savedModelToDeletedMap = capturesRequest.getModelsRequest(0).getModelsList().stream()
                .collect(Collectors.toMap(ModelStorage.Model::getId, ModelStorage.Model::getDeleted));

        assertThat(savedModelToDeletedMap).containsExactlyInAnyOrderEntriesOf(modelToDeletedMap);
    }

    private void verifyModelHasNoChildInSaveRequest(long modelId, List<Long> skuIds) {
        Set<Long> relatedSkuIds = extractRelatedSkuFromModelInSaveRequest(modelId);
        assertThat(relatedSkuIds).doesNotContainAnyElementsOf(skuIds);
    }

    private void verifyModelChildrenExactlyInSaveRequest(long modelId, List<Long> skuIds) {
        Set<Long> relatedSkuIds = extractRelatedSkuFromModelInSaveRequest(modelId);
        assertThat(relatedSkuIds).containsExactlyInAnyOrderElementsOf(skuIds);
    }

    private Set<Long> extractRelatedSkuFromModelInSaveRequest(long modelId) {
        ArgumentCaptor<ModelCardApi.SaveModelsGroupRequest> argumentCaptor =
                ArgumentCaptor.forClass(ModelCardApi.SaveModelsGroupRequest.class);

        verify(modelStorageHelper, times(1)).executeSaveModelRequest(argumentCaptor.capture());
        ModelCardApi.SaveModelsGroupRequest capturesRequest = argumentCaptor.getValue();

        ModelStorage.Model requestedModel = capturesRequest.getModelsRequest(0).getModelsList().stream()
                .filter(m -> m.getId() == modelId)
                .findAny()
                .orElse(null);

        assertThat(requestedModel).isNotNull();

        return ModelProtoUtils.getUniqueRelatedSkuIds(requestedModel);
    }
}
