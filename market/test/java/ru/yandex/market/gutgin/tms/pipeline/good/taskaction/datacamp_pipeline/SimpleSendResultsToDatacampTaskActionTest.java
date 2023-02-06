package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.assertions.GutginAssertions;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.rating.DefaultRatingEvaluator;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.OfferContentProcessingResults.UpdateContentProcessingTasksRequest;
import ru.yandex.market.mboc.http.OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask;
import ru.yandex.market.mboc.http.OfferContentProcessingResults.UpdateContentProcessingTasksResponse;
import ru.yandex.market.mboc.http.OfferContentProcessingResultsServiceGrpc;
import ru.yandex.market.mboc.http.OfferContentProcessingResultsServiceGrpc.OfferContentProcessingResultsServiceBlockingStub;
import ru.yandex.market.mboc.http.OfferContentProcessingResultsServiceGrpc.OfferContentProcessingResultsServiceImplBase;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;

import static Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_MSKU;
import static Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateMModel;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateModel;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateMsku;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateSku;

public class SimpleSendResultsToDatacampTaskActionTest extends DBDcpStateGenerator {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();
    private SendResultsToDatacampTaskAction sender;
    private ModelStorageHelper modelStorage;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        String serverName = InProcessServerBuilder.generateName();
        try {
            grpcCleanup.register(
                    InProcessServerBuilder
                            .forName(serverName)
                            .directExecutor()
                            .fallbackHandlerRegistry(serviceRegistry)
                            .build()
                            .start()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder
                        .forName(serverName)
                        .directExecutor()
                        .build()
        );


        OfferContentProcessingResultsServiceBlockingStub api =
                OfferContentProcessingResultsServiceGrpc.newBlockingStub(channel);
        this.modelStorage = mock(ModelStorageHelper.class);
        this.sender = new SendResultsToDatacampTaskAction(gcSkuTicketDao, datacampOfferDao, api,
                new DefaultRatingEvaluator(new CategoryDataKnowledgeMock()), modelStorage);
    }

    @Test
    public void useDifferentMappingTypesForOfferWithMskuAndPskuMappings() {
        //mock models
        long skuId1 = 100;
        int modelId1 = 1000;
        ModelStorage.Model mSku1 = generateMsku(skuId1, modelId1);
        ModelStorage.Model mModel1 = generateMModel(modelId1, skuId1);
        long skuId2 = 200;
        int modelId2 = 2000;
        ModelStorage.Model sku2 = generateSku(skuId2, modelId2);
        ModelStorage.Model model2 = generateModel(modelId2, skuId2);

        Map<Long, ModelStorage.Model> skus = new HashMap<>();
        skus.put(mSku1.getId(), mSku1);
        skus.put(sku2.getId(), sku2);
        Map<Long, ModelStorage.Model> models = new HashMap<>();
        models.put(mModel1.getId(), mModel1);
        models.put(model2.getId(), model2);
        doReturn(skus)
                .doReturn(models)
                .when(modelStorage)
                .findModelsMap(any());

        //mock mboc call
        List<UpdateContentProcessingTasksRequest> requests = new ArrayList<>();
        UpdateContentProcessingTasksResponse ok = UpdateContentProcessingTasksResponse.newBuilder()
                .addStatusPerBusinessId(
                        UpdateContentProcessingTasksResponse.OfferStatus.newBuilder()
                                .setShopSku("offer_1")
                                .setStatus(
                                        UpdateContentProcessingTasksResponse.OfferStatus.Status.OK)
                                .build())
                .addStatusPerBusinessId(
                        UpdateContentProcessingTasksResponse.OfferStatus.newBuilder()
                                .setShopSku("offer_0")
                                .setStatus(
                                        UpdateContentProcessingTasksResponse.OfferStatus.Status.OK)
                                .build())
                .build();

        OfferContentProcessingResultsServiceImplBase serviceImplBase =
                new OfferContentProcessingResultsServiceImplBase() {
                    @Override
                    public void updateDataCampContentProcessingTasks(
                            UpdateContentProcessingTasksRequest request,
                            StreamObserver<UpdateContentProcessingTasksResponse> responseObserver) {
                        requests.add(request);
                        responseObserver.onNext(ok);
                        responseObserver.onCompleted();
                    }
                };
        serviceRegistry.addService(serviceImplBase);

        //generate tickets
        List<GcSkuTicket> tickets = generateTicketsWithPskuAndMskuMapping(skuId1, skuId2);

        //run task
        ProcessTaskResult<ProcessDataBucketData> result =
                sender.doRun(new ProcessDataBucketData(tickets.get(0).getDataBucketId()));

        //assert request
        GutginAssertions.assertThat(result).doesntHaveProblems();
        assertThat(requests).hasSize(1);
        UpdateContentProcessingTasksRequest req = requests.get(0);
        BusinessIdsProcessingTask task0 = takeWithSsku(req, "offer_0");
        assertThat(task0).extracting(
                        t -> t.getContentProcessing().getContent().getBinding().getPartner().getMarketSkuType())
                .isEqualTo(MARKET_SKU_TYPE_MSKU);
        assertThat(task0).extracting(
                        t -> t.getContentProcessing().getContent().getBinding().getPartner().getMarketSkuId())
                .isEqualTo(skuId1);

        BusinessIdsProcessingTask task1 = takeWithSsku(req, "offer_1");
        assertThat(task1).extracting(
                        t -> t.getContentProcessing().getContent().getBinding().getPartner().getMarketSkuType())
                .isEqualTo(MARKET_SKU_TYPE_PSKU);
        assertThat(task1).extracting(
                        t -> t.getContentProcessing().getContent().getBinding().getPartner().getMarketSkuId())
                .isEqualTo(skuId2);
    }

    private BusinessIdsProcessingTask takeWithSsku(
            UpdateContentProcessingTasksRequest req,
            String ssku
    ) {
        return req.getContentProcessingTaskList()
                .stream()
                .filter(businessIdsProcessingTask -> businessIdsProcessingTask.getShopSku().equals(ssku))
                .findFirst()
                .orElseThrow();
    }

    private List<GcSkuTicket> generateTicketsWithPskuAndMskuMapping(long skuId1, long skuId2) {
        List<GcSkuTicket> tickets = generateDBDcpInitialState(2, offers -> {
            for (int i = 0; i < offers.size(); i++) {
                DatacampOffer datacampOffer = offers.get(i);
                datacampOffer.setData(
                        DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(
                                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                .setShopId(Math.toIntExact(PARTNER_SHOP_ID))
                                                .setOfferId("offer_" + i)
                                )
                                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                        .setBinding(
                                                DataCampOfferMapping.ContentBinding.newBuilder()
                                                        .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                                                                .setMarketCategoryId(1000)
                                                                .setMarketSkuId(i == 0 ? skuId1 : skuId2)
                                                                .setMarketSkuType(
                                                                        i == 0 ? MARKET_SKU_TYPE_MSKU
                                                                                : MARKET_SKU_TYPE_PSKU
                                                                )
                                                                .build())
                                                        .build()
                                        )
                                        .build()
                                )
                                .build());
            }
        });
        tickets.get(0).setType(GcSkuTicketType.CSKU_MSKU);
        tickets.get(0).setResultMboPskuId(skuId1);
        tickets.get(1).setResultMboPskuId(skuId2);
        tickets.forEach(ticket -> ticket.setStatus(GcSkuTicketStatus.RESULT_UPLOAD_STARTED));
        gcSkuTicketDao.update(tickets);
        return tickets;
    }
}
