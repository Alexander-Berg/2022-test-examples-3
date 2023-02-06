package ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.jooq.InsertSetMoreStep;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.markup3.api.Markup3Api;
import ru.yandex.market.markup3.api.Markup3Api.CreateTasksRequest;
import ru.yandex.market.markup3.api.Markup3Api.CreateTasksResponse;
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.TaskPropertiesDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.records.ClusterContentRecord;
import ru.yandex.market.psku.postprocessor.service.deduplication.MappingModerationTolokaFastSender;
import ru.yandex.market.psku.postprocessor.service.deduplication.TaskPropertiesService;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterContent.CLUSTER_CONTENT;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterGeneration.CLUSTER_GENERATION;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterMeta.CLUSTER_META;

public class MappingModerationTolokaFastSenderTest extends BaseDBTest {

    private final static long MARKET_CATEGORY_ID = 100L;
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    @Autowired
    private ClusterContentDao clusterContentDao;
    @Autowired
    private ClusterMetaDao clusterMetaDao;
    @Autowired
    private TaskPropertiesDao taskPropertiesDao;
    private MboMappingsService mboMappingsService;
    private ModelStorageHelper modelStorageHelper;
    private MboMappingsServiceHelper mboMappingsServiceHelper;
    private TolokaSender tolokaSender;
    private MappingModerationTolokaFastSender sender;
    private TaskPropertiesService taskPropertiesService;
    private Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceBlockingStub api;
    private boolean failOnSend = false;

    @Before
    public void setUp() throws IOException {
        System.setProperty("configs.path",
                getClass().getClassLoader().getResource("task_properties_service_test.properties").getFile());
        CreateTasksResponse responseItems = CreateTasksResponse.newBuilder()
                .addResponseItems(Markup3Api.CreateTaskResponseItem.newBuilder()
                        .setResult(Markup3Api.CreateTaskResponseItem.CreateTaskResult.OK)
                        .build())
                .build();
        Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceImplBase serviceImpl =
                new Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceImplBase() {
                    public void createTask(CreateTasksRequest request,
                                           io.grpc.stub.StreamObserver<CreateTasksResponse> responseObserver) {
                        if (failOnSend) {
                            responseObserver.onError(new IllegalStateException());
                        } else {
                            responseObserver.onNext(responseItems);
                            responseObserver.onCompleted();
                        }
                    }
                };
        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(serviceImpl).build().start());
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());
        api = Markup3ApiTaskServiceGrpc.newBlockingStub(channel);

        tolokaSender = new TolokaSender(api, 100);
        mboMappingsService = mock(MboMappingsService.class);
        mboMappingsServiceHelper = mock(MboMappingsServiceHelper.class);
        modelStorageHelper = mock(ModelStorageHelper.class);
        taskPropertiesService = new TaskPropertiesService(taskPropertiesDao);
        taskPropertiesService.updateProperty("ppp.deduplication.toloka_task_limit", 50);
        sender = new MappingModerationTolokaFastSender(clusterContentDao,
                clusterMetaDao,
                mboMappingsService,
                modelStorageHelper,
                mboMappingsServiceHelper,
                tolokaSender,
                taskPropertiesService);
    }

    @Test
    public void noExceptionOnEmptyDb() {
        sender.send();
    }

    @Test
    public void whenTargetSkusAreInvalidThenChangeStatusOfCluster() {
        prepareState();
        initMocks();
        Mockito.when(modelStorageHelper.findModelsMap(Mockito.eq(Stream.of(1L, 2L, 3L, 4L, 5L, 6L)
                .collect(Collectors.toSet()))))
                .thenReturn(clusterFindModelsMapResponseWithInvalidSku());

        sender.send();
        List<ClusterMeta> clusters = clusterMetaDao.findAll();
        assertThat(clusters).extracting(ClusterMeta::getStatus).containsOnly(ClusterStatus.INVALID);
    }

    @Test
    public void updateClusterMetaAndSetTaskIdForClusterElements() {
        prepareState();
        initMocks();

        sender.send();

        List<ClusterMeta> clusters = clusterMetaDao.findAll();
        assertThat(clusters).extracting(ClusterMeta::getStatus)
                .containsOnly(ClusterStatus.MAPPING_MODERATION_TOLOKA_IN_PROCESS);

        List<ClusterContent> clusterContent = clusterContentDao.findAll();
        Set<Long> tasksInContent = clusterContent.stream()
                .map(ClusterContent::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        assertThat(tasksInContent).hasSize(1);

        assertThat(clusterContent)
                .filteredOn(content -> content.getTaskId() != null)
                .extracting(ClusterContent::getMbocId)
                .doesNotContainNull();

        Set<Long> targetSkuInContent = clusterContent.stream()
                .map(ClusterContent::getSupposedTargetSkuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        assertThat(targetSkuInContent).hasSize(2); // на 1 targetSku для каждого кластера
        assertThat(clusterContent)
                .filteredOn(content -> content.getTaskId() != null)
                .extracting(ClusterContent::getSupposedTargetSkuId)
                .doesNotContainNull();

        assertThat(clusterContent)
                .filteredOn(content -> content.getTaskId() == null)
                .hasSize(2); // на 1 targetSku для каждого кластера
    }

    @Test
    public void whenBatchIsLessThenSizeOfClusterThenCreateMultipleTasks() {
        prepareState();
        initMocks();

        tolokaSender = new TolokaSender(api, 4);
        sender = new MappingModerationTolokaFastSender(clusterContentDao,
                clusterMetaDao,
                mboMappingsService,
                modelStorageHelper,
                mboMappingsServiceHelper,
                tolokaSender,
                taskPropertiesService);
        sender.send();

        Long tasksCreated = clusterContentDao.tolokaTaskInProcess();
        int expectedNumberOfTasks = 3;
        assertThat(tasksCreated).isEqualTo(expectedNumberOfTasks);

        List<ClusterMeta> clusters = clusterMetaDao.findAll();
        assertThat(clusters).extracting(ClusterMeta::getStatus)
                .containsOnly(ClusterStatus.MAPPING_MODERATION_TOLOKA_IN_PROCESS);

        List<ClusterContent> clusterContent = clusterContentDao.findAll();
        Set<Long> tasksInContent = clusterContent.stream()
                .map(ClusterContent::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        assertThat(tasksInContent).hasSize(expectedNumberOfTasks);

        assertThat(clusterContent)
                .filteredOn(content -> content.getTaskId() != null)
                .extracting(ClusterContent::getMbocId)
                .doesNotContainNull();

        Set<Long> targetSkuInContent = clusterContent.stream()
                .map(ClusterContent::getSupposedTargetSkuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        assertThat(targetSkuInContent).hasSize(2); // на 1 targetSku для каждого кластера
        assertThat(clusterContent)
                .filteredOn(content -> content.getTaskId() != null)
                .extracting(ClusterContent::getSupposedTargetSkuId)
                .doesNotContainNull();

        assertThat(clusterContent)
                .filteredOn(content -> content.getTaskId() == null)
                .hasSize(2); // на 1 targetSku для каждого кластера
    }

    @Test
    public void whenOnly1TaskLeftThenCreateOnlyOneTask() {
        prepareState();
        initMocks();
        Stream.iterate(1000, integer -> integer + 1)
                .limit(49)
                .forEach(taskId -> clusterContentDao.addTolokaTask(taskId));

        tolokaSender = new TolokaSender(api, 3);
        sender = new MappingModerationTolokaFastSender(clusterContentDao,
                clusterMetaDao,
                mboMappingsService,
                modelStorageHelper,
                mboMappingsServiceHelper,
                tolokaSender,
                taskPropertiesService);
        sender.send();

        Long tasksCreated = clusterContentDao.tolokaTaskInProcess();
        int expectedNumberOfTasks = 50;
        assertThat(tasksCreated).isEqualTo(expectedNumberOfTasks);

        List<ClusterMeta> clusters = clusterMetaDao.findAll();
        //досрочно закончили обработку кластера, не обновляем статус кластеров
        assertThat(clusters).extracting(ClusterMeta::getStatus)
                .containsOnly(ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW);

        List<ClusterContent> clusterContent = clusterContentDao.findAll();
        Set<Long> tasksInContent = clusterContent.stream()
                .map(ClusterContent::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        assertThat(tasksInContent).hasSize(1);

        assertThat(clusterContent)
                .filteredOn(content -> content.getTaskId() != null)
                .extracting(ClusterContent::getMbocId)
                .doesNotContainNull();

        Set<Long> targetSkuInContent = clusterContent.stream()
                .map(ClusterContent::getSupposedTargetSkuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        assertThat(targetSkuInContent).hasSize(1); // на 1 targetSku для каждого кластера
        assertThat(clusterContent)
                .filteredOn(content -> content.getTaskId() != null)
                .extracting(ClusterContent::getSupposedTargetSkuId)
                .doesNotContainNull();

        assertThat(clusterContent)
                .filteredOn(content -> content.getTaskId() != null)
                .hasSize(3);
    }

    @Test
    public void whenClusterIsFullyProcessedButFailOnSendDontUpdateClusterStatus() {
        failOnSend = true;
        prepareState();
        initMocks();

        tolokaSender = new TolokaSender(api, 7);
        sender = new MappingModerationTolokaFastSender(clusterContentDao,
                clusterMetaDao,
                mboMappingsService,
                modelStorageHelper,
                mboMappingsServiceHelper,
                tolokaSender,
                taskPropertiesService);
        assertThatThrownBy(() -> sender.send()).isExactlyInstanceOf(StatusRuntimeException.class);
        List<ClusterMeta> clusters = clusterMetaDao.findAll();
        assertThat(clusters).extracting(ClusterMeta::getStatus)
                .containsOnly(ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW);
    }

    private void initMocks() {
        Mockito.when(modelStorageHelper.findModelsMap(Mockito.eq(Stream.of(1L, 2L, 3L, 4L, 5L, 6L)
                .collect(Collectors.toSet()))))
                .thenReturn(clusterFindModelsMapResponse());

        Mockito.when(mboMappingsServiceHelper.searchBaseOfferMappingsByMarketSkuId(
                Arrays.asList(1L,2L,3L,5L,6L),
                Collections.singletonList(MboMappings.MappingKind.APPROVED_MAPPING)))
                .thenReturn(
                        Arrays.asList(
                                SupplierOffer.Offer.newBuilder()
                                        .setInternalOfferId(901)
                                        .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                                                .setSkuId(1)
                                                .build())
                                        .setMarketCategoryId(MARKET_CATEGORY_ID).build(),
                                SupplierOffer.Offer.newBuilder().setInternalOfferId(902)
                                        .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                                                .setSkuId(2)
                                                .build())
                                        .setMarketCategoryId(MARKET_CATEGORY_ID).build(),
                                SupplierOffer.Offer.newBuilder().setInternalOfferId(903)
                                        .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                                                .setSkuId(3)
                                                .build())
                                        .setMarketCategoryId(MARKET_CATEGORY_ID).build(),
                                SupplierOffer.Offer.newBuilder().setInternalOfferId(905)
                                        .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                                                .setSkuId(5)
                                                .build())
                                        .setMarketCategoryId(MARKET_CATEGORY_ID).build(),
                                SupplierOffer.Offer.newBuilder().setInternalOfferId(906)
                                        .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                                                .setSkuId(6)
                                                .build())
                                        .setMarketCategoryId(MARKET_CATEGORY_ID).build()
                        )
                );

        Mockito.when(mboMappingsService
                .searchMappingsByBusinessKeys(searchMappingsByBusinessKeysRequest(1, "1", "2", "3", "4", "5", "6", "7")))
                .thenReturn(offerCacheSearchMappingsByBusinessKeysResponse());
    }

    private MboMappings.SearchMappingsResponse offerCacheSearchMappingsByBusinessKeysResponse() {
        return MboMappings.SearchMappingsResponse.newBuilder()
                .addAllOffers(listOf(
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(901)
                                .setSupplierId(1)
                                .setShopSkuId("1")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(908)
                                .setSupplierId(1)
                                .setShopSkuId("2")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(909)
                                .setSupplierId(1)
                                .setShopSkuId("3")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(910)
                                .setSupplierId(1)
                                .setShopSkuId("4")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(911)
                                .setSupplierId(1)
                                .setShopSkuId("5")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(912)
                                .setSupplierId(1)
                                .setShopSkuId("6")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(913)
                                .setSupplierId(1)
                                .setShopSkuId("7")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build()
                ))
                .build();
    }


    private <T> List<T> listOf(T... t) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, t);
        return list;
    }

    private void prepareState() {
        long clusterGenerationId = 101;
        long clusterMetaId1 = 1001;
        long clusterMetaId2 = 1002;

        dsl().insertInto(CLUSTER_GENERATION)
                .set(CLUSTER_GENERATION.ID, clusterGenerationId)
                .set(CLUSTER_GENERATION.YT_PATH, "/test")
                .set(CLUSTER_GENERATION.IS_CURRENT, true)
                .set(CLUSTER_GENERATION.CREATE_DATE, Timestamp.from(Instant.now()))
                .execute();

        dsl().insertInto(CLUSTER_META)
                .set(CLUSTER_META.ID, clusterMetaId1)
                .set(CLUSTER_META.CLUSTER_GENERATION_ID, clusterGenerationId)
                .set(CLUSTER_META.STATUS, ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW)
                .set(CLUSTER_META.TYPE, ClusterType.PSKU_EXISTS)
                .set(CLUSTER_META.WEIGHT, 0.0)
                .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
                .execute();
        dsl().insertInto(CLUSTER_META)
                .set(CLUSTER_META.ID, clusterMetaId2)
                .set(CLUSTER_META.CLUSTER_GENERATION_ID, clusterGenerationId)
                .set(CLUSTER_META.STATUS, ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW)
                .set(CLUSTER_META.TYPE, ClusterType.MSKU_EXISTS)
                .set(CLUSTER_META.WEIGHT, 0.0)
                .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
                .execute();

        insertContent(clusterMetaId1, ClusterContentType.PSKU, 1L, null, null);
        insertContent(clusterMetaId1, ClusterContentType.PSKU, 2L, null, null);
        insertContent(clusterMetaId1, ClusterContentType.PSKU, 3L, null, null);
        insertContent(clusterMetaId1, ClusterContentType.DSBS, null, "1", 1L);
        insertContent(clusterMetaId1, ClusterContentType.DSBS, null, "2", 1L);
        insertContent(clusterMetaId1, ClusterContentType.DSBS, null, "3", 1L);

        insertContent(clusterMetaId2, ClusterContentType.MSKU, 4L, null, null);
        insertContent(clusterMetaId2, ClusterContentType.PSKU, 5L, null, null);
        insertContent(clusterMetaId2, ClusterContentType.PSKU, 6L, null, null);
        insertContent(clusterMetaId2, ClusterContentType.DSBS, null, "4", 1L);
        insertContent(clusterMetaId2, ClusterContentType.DSBS, null, "5", 1L);
        insertContent(clusterMetaId2, ClusterContentType.DSBS, null, "6", 1L);
        insertContent(clusterMetaId2, ClusterContentType.DSBS, null, "7", 1L);
    }

    private MboMappings.SearchMappingsByBusinessKeysRequest searchMappingsByBusinessKeysRequest(
            int businessId, String... offerIds) {
        List<MbocCommon.BusinessSkuKey> keys = Stream.of(offerIds).map(offerId ->
                MbocCommon.BusinessSkuKey.newBuilder()
                        .setOfferId(offerId)
                        .setBusinessId(businessId)
                        .build())
                .sorted(Comparator.comparing(MbocCommon.BusinessSkuKey::getOfferId))
                .collect(Collectors.toList());

        return MboMappings.SearchMappingsByBusinessKeysRequest.newBuilder()
                .addAllKeys(keys)
                .build();
    }

    private Map<Long, ModelStorage.Model> clusterFindModelsMapResponse() {
        Map<Long, ModelStorage.Model> res = new HashMap<>();
        res.put(1L, ModelStorage.Model.newBuilder().setId(1)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .setPublished(true)
                .setSupplierId(1).build());
        res.put(2L, ModelStorage.Model.newBuilder().setId(2).setPublished(true).setSupplierId(1).build());
        res.put(3L, ModelStorage.Model.newBuilder().setId(3).setPublished(true).setSupplierId(1).build());
        res.put(4L, ModelStorage.Model.newBuilder().setId(4)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.SKU.name())
            .setPublished(true)
                .setSupplierId(1).build());
        res.put(5L, ModelStorage.Model.newBuilder().setId(5).setPublished(true).setSupplierId(1).build());
        res.put(6L, ModelStorage.Model.newBuilder().setId(3).setPublished(true).setSupplierId(1).build());
        return res;
    }

    private Map<Long, ModelStorage.Model> clusterFindModelsMapResponseWithInvalidSku() {
        Map<Long, ModelStorage.Model> res = new HashMap<>();
        res.put(1L, ModelStorage.Model.newBuilder().setId(1).setSupplierId(1).build());
        res.put(2L, ModelStorage.Model.newBuilder().setId(2).setSupplierId(1).build());
        res.put(3L, ModelStorage.Model.newBuilder().setId(3).setSupplierId(1).build());
        res.put(4L, ModelStorage.Model.newBuilder().setId(4).setSupplierId(1).build());
        res.put(5L, ModelStorage.Model.newBuilder().setId(5).setSupplierId(1).build());
        res.put(6L, ModelStorage.Model.newBuilder().setId(3).setSupplierId(1).build());
        return res;
    }

    private List<SupplierOffer.Offer> searchBaseOfferMappingsByMarketSkuIdResponse(long skuId) {
        List<SupplierOffer.Offer> res = new ArrayList<>();
        res.add(SupplierOffer.Offer.newBuilder().setInternalOfferId(900 + skuId)
                .setMarketCategoryId(MARKET_CATEGORY_ID).build());
        return res;
    }

    private void insertContent(long clusterMetaId,
                               ClusterContentType clusterContentType,
                               Long skuId, String offerId, Long businessId) {
        InsertSetMoreStep<ClusterContentRecord> step = dsl().insertInto(CLUSTER_CONTENT)
                .set(CLUSTER_CONTENT.CLUSTER_META_ID, clusterMetaId)
                .set(CLUSTER_CONTENT.WEIGHT, 0.0)
                .set(CLUSTER_CONTENT.TYPE, clusterContentType)
                .set(CLUSTER_CONTENT.STATUS, ClusterContentStatus.NEW);
        if (skuId != null) {
            step.set(CLUSTER_CONTENT.SKU_ID, skuId);
        } else {
            step.set(CLUSTER_CONTENT.OFFER_ID, offerId);
            step.set(CLUSTER_CONTENT.BUSINESS_ID, businessId);
        }
        step.execute();
    }


}