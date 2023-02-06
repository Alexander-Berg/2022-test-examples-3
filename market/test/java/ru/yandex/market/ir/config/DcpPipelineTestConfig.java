package ru.yandex.market.ir.config;

import java.io.IOException;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.cleanweb.client.CleanWebService;
import ru.yandex.market.grpc.trace.TraceClientInterceptor;
import ru.yandex.market.gutgin.tms.config.TestCleanWebSubConfig;
import ru.yandex.market.gutgin.tms.config.pipeline.csku.CskuPipelineV1;
import ru.yandex.market.gutgin.tms.config.pipeline.csku.CskuPipelineV2;
import ru.yandex.market.gutgin.tms.config.pipeline.csku.CskuPipelineV3;
import ru.yandex.market.gutgin.tms.config.pipeline.csku.CskuPipelineV4;
import ru.yandex.market.gutgin.tms.config.pipeline.csku.CskuPipelineV5;
import ru.yandex.market.gutgin.tms.config.pipeline.csku.CskuPipelineV6;
import ru.yandex.market.gutgin.tms.config.pipeline.csku.CskuPipelineV7;
import ru.yandex.market.gutgin.tms.config.pipeline.csku.CskuPipelineV8;
import ru.yandex.market.gutgin.tms.engine.pipeline.PipelineTemplateHolder;
import ru.yandex.market.gutgin.tms.engine.pipeline.template.PipelineTemplate;
import ru.yandex.market.gutgin.tms.mocks.CleanWebServiceMock;
import ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket.AvatarImageDownloader;
import ru.yandex.market.gutgin.tms.service.FileDownloadService;
import ru.yandex.market.gutgin.tms.service.SskuLockService;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.DatacampPipelineSchedulerService;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.FastOffersProcessingStrategy;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferInfoBatchProducer;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferProcessingStrategy;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.controller.manager.DatacampConverter;
import ru.yandex.market.ir.autogeneration_api.partner.content.PartnerContentController;
import ru.yandex.market.ir.autogeneration_api.partner.content.PartnerContentParamController;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.http.MarkupService;
import ru.yandex.market.ir.pipeline.OfferContentProcessingResultsServiceMock;
import ru.yandex.market.markup3.api.Markup3IntegrationsApiSkuParametersConflictServiceGrpc;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.SkuBDApiService;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.OfferContentProcessingResultsServiceGrpc;
import ru.yandex.market.partner.content.common.db.dao.DataBucketDao;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.BusinessToLockInfoDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.partner.content.SourceController;
import ru.yandex.market.partner.content.common.service.MdsFileStorageService;
import ru.yandex.market.partner.content.common.service.PartnerContentFileService;
import ru.yandex.market.request.trace.Module;
import ru.yandex.passport.tvmauth.NativeTvmClient;

import static ru.yandex.market.gutgin.tms.config.OfferSchedulingConfig.OFFER_INFO_IS_NEW_PREDICATE;

@Configuration
//обязательно нужно сохранять порядок импортов, иначе появляются циклические зависимости
@Import({
        CskuPipelineV1.class,
        CskuPipelineV2.class,
        CskuPipelineV3.class,
        CskuPipelineV4.class,
        CskuPipelineV5.class,
        CskuPipelineV6.class,
        CskuPipelineV7.class,
        CskuPipelineV8.class,
        TestCleanWebSubConfig.class,
        CommonPipelineConfig.class
})
public class DcpPipelineTestConfig {
    @Bean
    public CategoryDataKnowledgeMock categoryDataKnowledgeMock() {
        return new CategoryDataKnowledgeMock();
    }

    @Bean
    public CategoryDataKnowledge categoryDataKnowledge(CategoryDataKnowledgeMock categoryDataKnowledgeMock) {
        return categoryDataKnowledgeMock;
    }

    // Replicates autogeneration-api.xml

    @Bean
    DatacampConverter datacampConverter(PartnerContentController partnerContentController) {
        return new DatacampConverter(partnerContentController);
    }

    // Replica of partner-content.xml
    @Bean
    public PartnerContentController partnerContentController
    (
        PartnerContentFileService partnerContentFileService,
        PartnerContentParamController partnerContentParamController,
        SourceController sourceController) {
        return new PartnerContentController(partnerContentFileService,
            partnerContentParamController, sourceController);
    }

    @Bean
    public SourceController sourceController(SourceDao sourceDao) {
        return new SourceController(sourceDao);
    }

    @Bean
    PartnerContentParamController partnerContentParamController(
            CategoryInfoProducer categoryInfoProducer,
            CategoryDataKnowledge categoryDataKnowledge
    ) {
        return new PartnerContentParamController(categoryInfoProducer, categoryDataKnowledge);
    }

    @Bean
    public PipelineTemplateHolder pipelineTemplateHolder(
            PipelineTemplate<?, ?> cskuPipelineV1,
            PipelineTemplate<?, ?> cskuPipelineV2,
            PipelineTemplate<?, ?> cskuPipelineV3,
            PipelineTemplate<?, ?> cskuPipelineV4,
            PipelineTemplate<?, ?> cskuPipelineV5,
            PipelineTemplate<?, ?> cskuPipelineV6,
            PipelineTemplate<?, ?> cskuPipelineV7,
            PipelineTemplate<?, ?> cskuPipelineV8) {
        PipelineTemplateHolder pipelineTemplateHolder = new PipelineTemplateHolder();
        pipelineTemplateHolder.addPipeline(PipelineType.CSKU, 1, cskuPipelineV1);
        pipelineTemplateHolder.addPipeline(PipelineType.CSKU, 2, cskuPipelineV2);
        pipelineTemplateHolder.addPipeline(PipelineType.CSKU, 3, cskuPipelineV3);
        pipelineTemplateHolder.addPipeline(PipelineType.CSKU, 4, cskuPipelineV4);
        pipelineTemplateHolder.addPipeline(PipelineType.CSKU, 5, cskuPipelineV5);
        pipelineTemplateHolder.addPipeline(PipelineType.CSKU, 6, cskuPipelineV6);
        pipelineTemplateHolder.addPipeline(PipelineType.CSKU, 7, cskuPipelineV7);
        pipelineTemplateHolder.addPipeline(PipelineType.CSKU, 8, cskuPipelineV8);
        return pipelineTemplateHolder;
    }


    @Bean
    public MboMappingsServiceMock mboMappingsServiceMock() {
        return Mockito.spy(new MboMappingsServiceMock());
    }

    @Bean
    public MboMappingsService mboMappingsService(MboMappingsServiceMock mboMappingsServiceMock) {
        return mboMappingsServiceMock;
    }


    @Bean
    public ModelStorageServiceMock modelStorageServiceMock() {
        return Mockito.spy(new ModelStorageServiceMock());
    }

    @Bean(name = "model.storage.service.with.retry")
    public ModelStorageService modelStorageServiceWithRetry(ModelStorageServiceMock modelStorageServiceMock) {
        return modelStorageServiceMock;
    }

    @Bean(name = "model.storage.service.without.retry")
    public ModelStorageService modelStorageServiceWithoutRetry(ModelStorageServiceMock modelStorageServiceMock) {
        return modelStorageServiceMock;
    }

    @Bean
    OfferContentProcessingResultsServiceMock offerContentProcessingResultsServiceMock() {
        return new OfferContentProcessingResultsServiceMock();
    }

    @Bean
    public OfferContentProcessingResultsServiceGrpc.OfferContentProcessingResultsServiceBlockingStub
    offerContentProcessingServiceBlockingStub(OfferContentProcessingResultsServiceMock offerContentProcessingResultsServiceMock) {

        ManagedChannel channel = getManagedChannel(offerContentProcessingResultsServiceMock);

        return OfferContentProcessingResultsServiceGrpc.newBlockingStub(channel);
    }

    private ManagedChannel getManagedChannel(OfferContentProcessingResultsServiceMock offerContentProcessingResultsServiceMock) {
        String uniqueName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(uniqueName)
                .directExecutor()
                .addService(offerContentProcessingResultsServiceMock)
                .build();
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ManagedChannel channel = InProcessChannelBuilder.forName(uniqueName)
                .directExecutor()
                .build();
        return channel;
    }

    @Bean
    @Lazy
    public Markup3IntegrationsApiSkuParametersConflictServiceGrpc
            .Markup3IntegrationsApiSkuParametersConflictServiceBlockingStub
    markup3ApiConflictServiceBlockingStubRunner() {
        return Markup3IntegrationsApiSkuParametersConflictServiceGrpc.newBlockingStub(
                NettyChannelBuilder.forAddress("markup3.vs.market.yandex.net", 8080)
                        .usePlaintext()
                        .maxInboundMessageSize(100 * 1024 * 1024)
                        .userAgent("gutgin-tms")
                        .intercept(new TraceClientInterceptor(Module.MBO_MARKUP))
                        .build()
        );
    }

    @Bean
    @Lazy
    public Markup3IntegrationsApiSkuParametersConflictServiceGrpc
            .Markup3IntegrationsApiSkuParametersConflictServiceBlockingStub markup3ApiConflictServiceBlockingStub() {
        return Markup3IntegrationsApiSkuParametersConflictServiceGrpc.newBlockingStub(
                NettyChannelBuilder.forAddress("markup3.vs.market.yandex.net", 8080)
                        .usePlaintext()
                        .maxInboundMessageSize(100 * 1024 * 1024)
                        .userAgent("gutgin-tms")
                        .intercept(new TraceClientInterceptor(Module.MBO_MARKUP))
                        .build()
        );
    }

    @Bean
    AvatarImageDownloader avatarImageDownloader() {
        return new AvatarImageDownloaderMock();
    }

    @Bean
    public CleanWebServiceMock cleanWebServiceMock() {
        return new CleanWebServiceMock();
    }

    @Bean
    public CleanWebService cleanWebService(CleanWebServiceMock cleanWebServiceMock) {
        return cleanWebServiceMock;
    }


    // Override to block original resource initialization.

    @Bean
    public MarkupService markupService() {
        return Mockito.mock(MarkupService.class);
    }

    @Bean
    public SkuBDApiService skuBDApiService() {
        return Mockito.mock(SkuBDApiService.class);
    }

    @Bean
    public NativeTvmClient tvmClient() {
        return null;
    }

    @Bean
    public MdsFileStorageService mdsFileStorageService() {
        return Mockito.mock(MdsFileStorageService.class);
    }

    @Bean
    public Yt arnoldYtApi() {
        return Mockito.mock(Yt.class);
    }

    @Bean
    public Yt hahnYtApi() {
        return Mockito.mock(Yt.class);
    }

    @Bean
    public FileDownloadService fileDownloadService() {
        return Mockito.mock(FileDownloadService.class);
    }

    @Bean
    public CloseableHttpClient httpClientMock() {
        return Mockito.mock(CloseableHttpClient.class);
    }

    @Bean
    OfferProcessingStrategy fastCreateStrategy() {
        return new FastOffersProcessingStrategy(
                OFFER_INFO_IS_NEW_PREDICATE,
                OfferProcessingStrategy.Priority.HIGH);
    }

    @Bean
    OfferProcessingStrategy fastEditStrategy() {
        return new FastOffersProcessingStrategy(
                OFFER_INFO_IS_NEW_PREDICATE.negate(),
                OfferProcessingStrategy.Priority.DEFAULT);
    }

    @Bean
    DatacampPipelineSchedulerService datacampPipelineSchedulerService(
            DatacampOfferDao datacampOfferDao,
            PipelineService pipelineService,
            DataBucketDao dataBucketDao,
            GcSkuTicketDao gcSkuTicketDao,
            SskuLockService sskuLockService,
            SourceController sourceController,
            BusinessToLockInfoDao businessToLockInfoDao,
            OfferInfoBatchProducer offerInfoBatchProducer
    ) {
        return new DatacampPipelineSchedulerService(datacampOfferDao,
                pipelineService,
                dataBucketDao,
                gcSkuTicketDao,
                sskuLockService,
                sourceController,
                businessToLockInfoDao,
                offerInfoBatchProducer
        );
    }
}
