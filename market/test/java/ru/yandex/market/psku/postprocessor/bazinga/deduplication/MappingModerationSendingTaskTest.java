package ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.assertj.core.api.Assertions;
import org.jooq.InsertSetMoreStep;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.markup3.api.Markup3Api;
import ru.yandex.market.markup3.api.Markup3Api.CreateTasksRequest;
import ru.yandex.market.markup3.api.Markup3Api.CreateTasksResponse;
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc;
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceImplBase;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategoryService;
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
import ru.yandex.market.psku.postprocessor.service.deduplication.TaskPropertiesService;

import static ru.yandex.market.mbo.http.ModelStorage.ModelType.PARTNER_SKU;
import static ru.yandex.market.mbo.http.ModelStorage.ModelType.SKU;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterContent.CLUSTER_CONTENT;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterGeneration.CLUSTER_GENERATION;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterMeta.CLUSTER_META;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.TaskProperties.TASK_PROPERTIES;

public class MappingModerationSendingTaskTest extends BaseDBTest {

    private final static long MARKET_CATEGORY_ID = 100L;

    @Autowired
    private ClusterContentDao clusterContentDao;
    @Autowired
    private ClusterMetaDao clusterMetaDao;

    @Autowired
    private TaskPropertiesDao taskPropertiesDao;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    Map<CreateTasksRequest, CreateTasksResponse> requestToResponseMap = new HashMap<>();
    private Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceBlockingStub markup3ApiTaskServiceStub;
    private MboMappingsService mboMappingsService;
    private ModelStorageHelper modelStorageHelper;
    private MboMappingsServiceHelper mboMappingsServiceHelper;
    private MboCategoryService mboCategoryService;

    @Before
    public void setUp() throws IOException {
        requestToResponseMap = new HashMap<>();
        Markup3ApiTaskServiceImplBase markup3ApiTaskServiceImplBase = new Markup3ApiTaskServiceImplBase() {
            @Override
            public void createTask(CreateTasksRequest request, StreamObserver<CreateTasksResponse> responseObserver) {
                responseObserver.onNext(requestToResponseMap.get(request));
                responseObserver.onCompleted();
            }
        };

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder
                        .forName(serverName)
                        .directExecutor()
                        .addService(markup3ApiTaskServiceImplBase)
                        .build()
                        .start()
        );
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder
                        .forName(serverName)
                        .directExecutor()
                        .build()
        );
        markup3ApiTaskServiceStub = Markup3ApiTaskServiceGrpc.newBlockingStub(channel);

        mboMappingsService = Mockito.mock(MboMappingsService.class);
        mboMappingsServiceHelper = Mockito.mock(MboMappingsServiceHelper.class);
        modelStorageHelper = Mockito.mock(ModelStorageHelper.class);
        mboCategoryService = Mockito.mock(MboCategoryService.class);
    }

    @Test
    public void test() {
        prepareState();
        initMocks();

        MappingModerationSendingTask task = new MappingModerationSendingTask(
                markup3ApiTaskServiceStub, new TaskPropertiesService(taskPropertiesDao),
                clusterContentDao, clusterMetaDao,
                mboMappingsService, modelStorageHelper, mboMappingsServiceHelper,
                mboCategoryService);

        task.execute(null);

        List<ClusterContent> contentList = dsl().selectFrom(CLUSTER_CONTENT).fetchInto(ClusterContent.class);
        Assertions.assertThat(contentList.size()).isEqualTo(13);

        Assertions.assertThat(contentList.stream().filter(c -> c.getTaskId() == null).count()).isEqualTo(2);

        List<ClusterMeta> metas = dsl().selectFrom(CLUSTER_META).fetchInto(ClusterMeta.class);
        Assertions.assertThat(metas.stream().filter(c -> c.getStatus() == ClusterStatus.MAPPING_MODERATION_IN_PROCESS).count()).isEqualTo(2);

        contentList.get(1).setTargetSkuId(-1L);
        contentList.get(2).setTargetSkuId(-1L);
        clusterContentDao.update(contentList);

        task.execute(null);
        Assertions.assertThat(contentList.stream().filter(c -> c.getTaskId() == null).count()).isEqualTo(2);

    }

    private void prepareState() {
        dsl().insertInto(TASK_PROPERTIES)
                .set(TASK_PROPERTIES.KEY, "ppp.deduplication.mapping_moderation.sending.task_limit")
                .set(TASK_PROPERTIES.VALUE, "10")
                .execute();

        dsl().insertInto(TASK_PROPERTIES)
                .set(TASK_PROPERTIES.KEY, "ppp.deduplication.mapping_moderation.sending.task_key")
                .set(TASK_PROPERTIES.VALUE, "deduplication_mapping_moderation")
                .execute();

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
                .set(CLUSTER_META.STATUS, ClusterStatus.MAPPING_MODERATION_NEW)
                .set(CLUSTER_META.TYPE, ClusterType.PSKU_EXISTS)
                .set(CLUSTER_META.WEIGHT, 0.0)
                .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
                .set(CLUSTER_META.CATEGORY_ID, 1L)
                .execute();
        dsl().insertInto(CLUSTER_META)
                .set(CLUSTER_META.ID, clusterMetaId2)
                .set(CLUSTER_META.CLUSTER_GENERATION_ID, clusterGenerationId)
                .set(CLUSTER_META.STATUS, ClusterStatus.MAPPING_MODERATION_NEW)
                .set(CLUSTER_META.TYPE, ClusterType.MSKU_EXISTS)
                .set(CLUSTER_META.WEIGHT, 0.0)
                .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
                .set(CLUSTER_META.CATEGORY_ID, 2L)
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

    private void initMocks() {

        Mockito.when(modelStorageHelper.findModelsMap(Mockito.eq(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L))))
                .thenReturn(findModelsMapResponse());

        Mockito.when(mboMappingsServiceHelper.searchBaseOfferMappingsByMarketSkuId(
                        List.of(1L, 2L, 3L, 5L, 6L),
                        Collections.singletonList(MboMappings.MappingKind.APPROVED_MAPPING)))
                .thenReturn(
                        List.of(
                                searchBaseOfferMappingsByMarketSkuIdResponse(2L),
                                searchBaseOfferMappingsByMarketSkuIdResponse(3L),
                                searchBaseOfferMappingsByMarketSkuIdResponse(5L),
                                searchBaseOfferMappingsByMarketSkuIdResponse(6L)
                        )
                );

        // запрос идет сразу по всем кластерам
        Mockito.when(mboMappingsService
                        .searchMappingsByBusinessKeys(
                                searchMappingsByBusinessKeysRequest(List.of(
                                        new Pair<>(1, "1"),
                                        new Pair<>(1, "2"),
                                        new Pair<>(1, "3"),
                                        new Pair<>(1, "4"),
                                        new Pair<>(1, "5"),
                                        new Pair<>(1, "6"),
                                        new Pair<>(1, "7")
                                ))
                        ))
                .thenReturn(mappingsResponse());


        var contentId1 = dsl()
                .select(CLUSTER_CONTENT.ID)
                .from(CLUSTER_CONTENT)
                .where(
                        CLUSTER_CONTENT.SKU_ID.eq(5L)
                )
                .fetch(CLUSTER_CONTENT.ID)
                .get(0);
        requestToResponseMap.put(
                markupRequest(String.valueOf(contentId1), List.of(5L, 6L, 7L, 8L, 9L, 10L), 4L,
                        Markup3Api.ModerationTaskSubtype.UNDEFINED),
                CreateTasksResponse.newBuilder().addResponseItems(
                        Markup3Api.CreateTaskResponseItem.newBuilder()
                                .setTaskId(Int64Value.newBuilder().setValue(10327).build())
                                .build()
                ).build()
        );

        var contentId2 = dsl()
                .select(CLUSTER_CONTENT.ID)
                .from(CLUSTER_CONTENT)
                .where(
                        CLUSTER_CONTENT.SKU_ID.eq(2L)
                )
                .fetch(CLUSTER_CONTENT.ID)
                .get(0);
        requestToResponseMap.put(
                markupRequest(String.valueOf(contentId2), List.of(2L, 3L, 4L, 5L, 6L), 1L,
                        Markup3Api.ModerationTaskSubtype.TO_PSKU),
                CreateTasksResponse.newBuilder().addResponseItems(
                        Markup3Api.CreateTaskResponseItem.newBuilder()
                                .setTaskId(Int64Value.newBuilder().setValue(10392).build())
                                .build()
                ).build()
        );

        Mockito.when(mboCategoryService.getCategoryGroups(Mockito.any()))
                .thenReturn(
                        MboCategory.GetCategoryGroupsResponse.newBuilder()
                                .addCategoryGroups(
                                        MboCategory.GetCategoryGroupsResponse.CategoryGroup.newBuilder()
                                                .setId(1L)
                                                .addCategories(1L)
                                                .addCategories(2L)
                                                .build()
                                )
                                .build()
                );
    }

    private void insertContent(
            long clusterMetaId, ClusterContentType clusterContentType, Long skuId, String offerId, Long businessId
    ) {
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


    private MboMappings.SearchMappingsByBusinessKeysRequest searchMappingsByBusinessKeysRequest(
            List<Pair<Integer, String>> businessKeys
    ) {
        List<MbocCommon.BusinessSkuKey> keys = businessKeys.stream().map(pair ->
                MbocCommon.BusinessSkuKey.newBuilder()
                        .setOfferId(pair.getSecond())
                        .setBusinessId(pair.getFirst())
                        .build()
        ).collect(Collectors.toList());

        return MboMappings.SearchMappingsByBusinessKeysRequest.newBuilder()
                .addAllKeys(keys)
                .build();
    }

    private Map<Long, ModelStorage.Model> findModelsMapResponse() {
        Map<Long, ModelStorage.Model> res = new HashMap<>();
        res.put(1L, buildModel(1, PARTNER_SKU));
        res.put(2L, buildModel(2, PARTNER_SKU));
        res.put(3L, buildModel(3, PARTNER_SKU));
        res.put(4L, buildModel(4, SKU));
        res.put(5L, buildModel(5, SKU));
        res.put(6L, buildModel(6, SKU));
        return res;
    }

    private static ModelStorage.Model buildModel(long id, ModelStorage.ModelType sourceType) {
        return ModelStorage.Model.newBuilder()
                .setId(id)
                .setSupplierId(1)
                .setSourceType(sourceType.name())
                .setCurrentType(SKU.name())
                .setPublished(true)
                .build();
    }

    private SupplierOffer.Offer searchBaseOfferMappingsByMarketSkuIdResponse(long skuId) {
        return SupplierOffer.Offer.newBuilder().setInternalOfferId(900 + skuId)
                .setApprovedMapping(SupplierOffer.Mapping.newBuilder().setSkuId(skuId).build())
                .setMarketCategoryId(MARKET_CATEGORY_ID).build();
    }

    private CreateTasksRequest markupRequest(String contentId, List<Long> skuIds, long targetSkuId,
                                             Markup3Api.ModerationTaskSubtype taskType) {

        return CreateTasksRequest
                .newBuilder()
                .setTaskTypeIdentity(Markup3Api.TaskTypeIdentity.newBuilder()
                        .setType(Markup3Api.TaskType.YANG_MAPPING_MODERATION)
                        .setGroupKey("deduplication_mapping_moderation")
                        .build())
                .addTasks(Markup3Api.TaskForCreate.newBuilder()
                        .setExternalKey(StringValue.newBuilder()
                                .setValue(String.valueOf(contentId))
                                .build())
                        .setInput(Markup3Api.TaskInputData.newBuilder()
                                .setYangMappingModerationInput(Markup3Api.YangMappingModerationInput.newBuilder()
                                        .setPriority(1000.0)
                                        .setCategoryId(-1)
                                        .addCategoryGroupIds(1)
                                        .setData(Markup3Api.YangMappingModerationInput.YangMappingModerationInputData.newBuilder()
                                                .addAllOffers(
                                                        skuIds.stream().map(skuId ->
                                                                        Markup3Api.YangMappingModerationInput.YangMappingModerationInputDataOffer.newBuilder()
                                                                                .setOfferId(String.valueOf(900 + skuId))
                                                                                .setId(900 + skuId)
                                                                                .setCategoryId(MARKET_CATEGORY_ID)
                                                                                .setTargetSkuId(Int64Value.newBuilder().setValue(targetSkuId).build())
                                                                                .build())
                                                                .collect(Collectors.toList())
                                                )
                                                .setTaskType(Markup3Api.YangMappingModerationInput.ModerationTaskType.MAPPING_MODERATION)
                                                .setTaskSubtype(taskType)
                                                .build())
                                        .build()).build()).build())
                .build();
    }

    private MboMappings.SearchMappingsResponse mappingsResponse() {
        return MboMappings.SearchMappingsResponse.newBuilder()
                .addAllOffers(listOf(
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(904)
                                .setSupplierId(1)
                                .setShopSkuId("1")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(905)
                                .setSupplierId(1)
                                .setShopSkuId("2")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(906)
                                .setSupplierId(1)
                                .setShopSkuId("3")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(907)
                                .setSupplierId(1)
                                .setShopSkuId("4")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(908)
                                .setSupplierId(1)
                                .setShopSkuId("5")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(909)
                                .setSupplierId(1)
                                .setShopSkuId("6")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build(),
                        SupplierOffer.Offer.newBuilder()
                                .setInternalOfferId(910)
                                .setSupplierId(1)
                                .setShopSkuId("7")
                                .setMarketCategoryId(MARKET_CATEGORY_ID)
                                .build()
                ))
                .build();
    }

    @SafeVarargs
    private final <T> List<T> listOf(T... t) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, t);
        return list;
    }
}
