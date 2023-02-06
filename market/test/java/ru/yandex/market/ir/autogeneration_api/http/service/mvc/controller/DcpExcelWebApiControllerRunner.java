package ru.yandex.market.ir.autogeneration_api.http.service.mvc.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.rating.DefaultRatingEvaluator;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.ir.autogeneration_api.export.excel.DcpExcelModelGenerator;
import ru.yandex.market.ir.autogeneration_api.export.excel.DcpSkuDataGenerator;
import ru.yandex.market.ir.autogeneration_api.export.excel.ExcelModelGenerator;
import ru.yandex.market.ir.autogeneration_api.export.excel.SkuExampleService;
import ru.yandex.market.ir.autogeneration_api.http.service.PartnerContentServiceImpl;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean.dcp.excel.AddFileResponse;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean.dcp.excel.GetFileTemplateRequest;
import ru.yandex.market.ir.autogeneration_api.partner.content.PartnerContentController;
import ru.yandex.market.ir.autogeneration_api.partner.content.PartnerContentParamController;
import ru.yandex.market.ir.autogeneration_api.service.FeedTemplateGenerator;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.ir.excel.generator.PartnerContentConverter;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.config.MdsS3Config;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.db.dao.PartnerContentDao;
import ru.yandex.market.partner.content.common.db.dao.SkuExampleDao;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.TemplateFeedUploadDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.FakeDatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.engine.manager.PipelineManager;
import ru.yandex.market.partner.content.common.entity.feed.DcpExcelFileDetails;
import ru.yandex.market.partner.content.common.entity.feed.DcpExcelFilesList;
import ru.yandex.market.partner.content.common.mocks.CategoryParametersFormParserMock;
import ru.yandex.market.partner.content.common.partner.content.SourceController;
import ru.yandex.market.partner.content.common.service.DataCampService;
import ru.yandex.market.partner.content.common.service.DataCampServiceImpl;
import ru.yandex.market.partner.content.common.service.MdsFileStorageService;
import ru.yandex.market.partner.content.common.service.PartnerContentFileService;
import ru.yandex.market.partner.content.common.service.StopPipelineService;
import ru.yandex.market.partner.content.common.service.admin.CWVerdictFixService;
import ru.yandex.market.partner.content.common.service.report.ui.DataBucketReportService;
import ru.yandex.market.partner.content.common.service.report.ui.DataCampOffersReportService;
import ru.yandex.market.partner.content.common.service.report.ui.ExternalRequestsReportService;
import ru.yandex.market.partner.content.common.service.report.ui.FileDataReportService;
import ru.yandex.market.partner.content.common.service.report.ui.GcDataBucketReportService;
import ru.yandex.market.partner.content.common.service.report.ui.GcFileDataReportService;
import ru.yandex.market.partner.content.common.service.report.ui.GcMessageReportService;
import ru.yandex.market.partner.content.common.service.report.ui.GcSingleTicketReportService;
import ru.yandex.market.partner.content.common.service.report.ui.GcSkuTicketReportService;
import ru.yandex.market.partner.content.common.service.report.ui.GcValidationReportService;
import ru.yandex.market.partner.content.common.service.report.ui.InternalErrorReportService;
import ru.yandex.market.partner.content.common.service.report.ui.LongPipelinesReportService;
import ru.yandex.market.partner.content.common.service.report.ui.MessageReportService;
import ru.yandex.market.partner.content.common.service.report.ui.VendorModelReportService;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;

import static org.mockito.Mockito.mock;

/**
 * Утилита (не авто-тест) для ручной проверки ручек Excel PSKU 2.0.
 */
@Ignore
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class, classes =
        {DcpExcelWebApiControllerRunner.TestConfig.class})
@TestPropertySource(properties = {
        // see https://github.yandex-team.ru/cs-admin/datasources-ng/blob/master/testing/etcd.yml for testing props
        "market.mds.s3.path=",
        "market.mds.s3.default.bucket.name=",
        "market.mds.s3.default.path.prefix=",
        "market.mds.s3.access.key=",
        "market.mds.s3.secret.key=",
})
public class DcpExcelWebApiControllerRunner extends BaseDbCommonTest {
    private static final Logger log = LogManager.getLogger();

    private static final int BUSINESS_ID = 1234;
    private static final int CATEGORY_ID = 12494574;

    @Autowired
    private PartnerContentService partnerContentService;

    @Autowired
    private PartnerContentFileService partnerContentFileService;

    @Autowired
    private MdsFileStorageService mdsFileStorageService;

    private DcpExcelWebApiController dcpExcelWebApiController;

    @Before
    public void setUp() {
        this.dcpExcelWebApiController = new DcpExcelWebApiController(
                partnerContentService, partnerContentFileService, mdsFileStorageService);
    }

    @Test
    public void getFileTemplate() throws IOException {
        GetFileTemplateRequest request = new GetFileTemplateRequest();
        request.setOfferIds(List.of("020622-test40440-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        dcpExcelWebApiController.getFileTemplate(11553910, CATEGORY_ID, request, response);

        Path home = Paths.get(System.getProperty("user.home"));
        Path filePath = home.resolve(String.format("dcp-excel-template-%d.xlsx", CATEGORY_ID));
        try (OutputStream fileStream = Files.newOutputStream(filePath)) {
            fileStream.write(response.getContentAsByteArray());
        }
    }

    public long addFile() throws IOException {
        Path filePath = Paths.get(System.getProperty("user.home")).resolve("dcp-excel.xlsx");
        String filename = String.format("upload-%s.xlsx", CATEGORY_ID);
        try (InputStream fileStream = Files.newInputStream(filePath)) {
            MockMultipartFile file = new MockMultipartFile(filename, filename, "application/octet-stream", fileStream);
            AddFileResponse response = dcpExcelWebApiController.addFile(BUSINESS_ID, file);
            log.info(response);
            return response.getRequestId();
        }
    }

    public DcpExcelFileDetails getFileInfo(long requestId) {
        DcpExcelFileDetails response = dcpExcelWebApiController.getFileInfo(BUSINESS_ID, requestId);
        log.info(response);
        return response;
    }

    public DcpExcelFilesList getFiles() {
        DcpExcelFilesList response = dcpExcelWebApiController.getFiles(BUSINESS_ID, 1, 50);
        log.info(response);
        return response;
    }

    @Test
    public void scenario() throws IOException {
        long requestId = addFile();
        DcpExcelFilesList list = getFiles();
        log.info(list);

        DcpExcelFileDetails details = getFileInfo(requestId);
        log.info(details);
    }


    @Import({MdsS3Config.class})
    public static class TestConfig {
        private static final String MODEL_STORAGE_URL = "http://mbo-card-api.tst.vs.market.yandex.net:33714/modelStorage/";
        public static final String HTTP_EXPORTER_BASE_URL = "http://mbo-http-exporter.tst.vs.market.yandex.net:8084/";

        @Bean
        CategoryParametersService categoryParametersService() {
            CategoryParametersServiceStub categoryParametersService = new CategoryParametersServiceStub();
            categoryParametersService.setHost(HTTP_EXPORTER_BASE_URL + "categoryParameters/");
            categoryParametersService.setConnectionTimeoutMillis(300000);
            categoryParametersService.setTriesBeforeFail(1);

            return categoryParametersService;
        }

        @Bean
        CategorySizeMeasureService categorySizeMeasureService() {
            CategorySizeMeasureServiceStub categorySizeMeasureService = new CategorySizeMeasureServiceStub();
            categorySizeMeasureService.setHost(HTTP_EXPORTER_BASE_URL + "categorySizeMeasure/");
            return categorySizeMeasureService;
        }

        @Bean
        CategoryDataKnowledge categoryDataKnowledge(
                CategoryParametersService categoryParametersService,
                CategorySizeMeasureService categorySizeMeasureService
        ) {
            CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();
            categoryDataKnowledge.setCategoryParametersService(categoryParametersService);
            categoryDataKnowledge.setCategoryDataRefreshersCount(1);
            categoryDataKnowledge.setCategorySizeMeasureService(categorySizeMeasureService);
            return categoryDataKnowledge;
        }

        @Bean
        CategoryDataHelper categoryDataHelper(CategoryDataKnowledge categoryDataKnowledge,
                                              BookCategoryHelper bookCategoryHelper) {
            return new CategoryDataHelper(categoryDataKnowledge, bookCategoryHelper);
        }

        @Bean
        CategoryParametersFormParser categoryParametersFormParser(
        ) {
            return new CategoryParametersFormParserMock();
        }

        @Bean
        CategoryInfoProducer categoryInfoProducer(
                CategoryDataKnowledge categoryDataKnowledge,
                CategoryParametersFormParser categoryParametersFormParser
        ) {
            return new CategoryInfoProducer(
                    categoryDataKnowledge, categoryParametersFormParser
            );
        }

        @Bean
        BookCategoryHelper bookCategoryHelper() {
            return new BookCategoryHelper();
        }

        @Bean
        SkuExampleDao skuExampleDao(
                @Qualifier("jooq.config.configuration") org.jooq.Configuration configuration
        ) {
            return new SkuExampleDao(configuration);
        }

        @Bean(name = "model.storage.service.without.retry")
        ModelStorageService modelStorageWithoutRetryService() {
            ModelStorageServiceStub result = new ModelStorageServiceStub();
            result.setHost(MODEL_STORAGE_URL);
            result.setTriesBeforeFail(1);
            return result;
        }

        @Bean
        public SkuExampleService skuExampleService(
                @Qualifier("model.storage.service.without.retry") ModelStorageService modelStorageService,
                SkuExampleDao skuExampleDao,
                CategoryDataKnowledge categoryDataKnowledge
        ) {
            return new SkuExampleService(modelStorageService, skuExampleDao, categoryDataKnowledge);
        }

        @Bean
        public SkuRatingEvaluator skuRatingEvaluator(CategoryDataKnowledge categoryDataKnowledge) {
            return new DefaultRatingEvaluator(categoryDataKnowledge);
        }

        @Value("${datacamp.url:http://datacamp.white.tst.vs.market.yandex.net}")
        String dataCampUrl;
        @Value("${datacamp.connect.timeout.millis:10000}")
        int dataCampConnectTimeoutMillis;
        @Value("${datacamp.read.timeout.millis:10000}")
        int dataCampReadTimeoutMillis;
        @Value("${user.agent}")
        String defaultUserAgent;

        @Bean
        public ModelStorageHelper modelStorageHelper(
                @Qualifier("model.storage.service.without.retry") ModelStorageService modelStorageService) {
            return new ModelStorageHelper(modelStorageService, modelStorageService);
        }

        @Bean
        public DataCampService dataCampService() {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Math.toIntExact(TimeUnit.MILLISECONDS.toMillis(dataCampConnectTimeoutMillis)))
                    .setSocketTimeout(Math.toIntExact(TimeUnit.MILLISECONDS.toMillis(dataCampReadTimeoutMillis)))
                    .setConnectionRequestTimeout(Math.toIntExact(TimeUnit.MILLISECONDS.toMillis(dataCampReadTimeoutMillis)))
                    .build();
            CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(requestConfig)
                    .setUserAgent(defaultUserAgent)
                    .addInterceptorFirst(new TraceHttpRequestInterceptor(Module.DATACAMP_STROLLER))
                    //Для запуска на тестинге сгенерировать значение хедера:
                    // ya tool tvmknife get_service_ticket sshkey -d 2002296 -s 2016225
                    .setDefaultHeaders(Set.of(new BasicHeader("X-Ya-Service-Ticket",
                            "")))
                    .addInterceptorLast(new TraceHttpResponseInterceptor())
                    .build();
            return new DataCampServiceImpl(httpClient, dataCampUrl);
        }

        @Bean
        public DcpSkuDataGenerator dcpSkuDataGenerator(
                DataCampService dataCampService,
                SourceDao sourceDao,
                CategoryDataHelper categoryDataHelper,
                SkuRatingEvaluator skuRatingEvaluator,
                ModelStorageHelper modelStorageHelper
        ) {
            PartnerContentConverter partnerContentConverter = new PartnerContentConverter(categoryDataHelper);
            return new DcpSkuDataGenerator(dataCampService, sourceDao, partnerContentConverter,
                    modelStorageHelper, new Judge(), skuRatingEvaluator);
        }

        @Bean
        public DcpExcelModelGenerator dcpExcelModelGenerator(
                CategoryInfoProducer categoryInfoProducer,
                SkuExampleService skuExampleService,
                DcpSkuDataGenerator dcpSkuDataGenerator
        ) {
            return new DcpExcelModelGenerator(categoryInfoProducer, skuExampleService, dcpSkuDataGenerator, true);
        }

        @Bean
        public FeedTemplateGenerator feedTemplateGenerator(
                CategoryDataKnowledge categoryDataKnowledge,
                ExcelModelGenerator excelModelGenerator
        ) {
            return new FeedTemplateGenerator(categoryDataKnowledge, excelModelGenerator);
        }

        @Bean
        public PartnerContentParamController partnerContentParamController(CategoryInfoProducer categoryInfoProducer,
                                                                           CategoryDataKnowledge categoryDataKnowledge) {
            return new PartnerContentParamController(categoryInfoProducer, categoryDataKnowledge);
        }

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
        public PartnerContentService partnerContentService(
                FeedTemplateGenerator feedTemplateGenerator,
                PartnerContentController partnerContentController
        ) {
            PartnerContentServiceImpl partnerContentService = new PartnerContentServiceImpl(
                    partnerContentController,
                    mock(FileDataReportService.class),
                    mock(VendorModelReportService.class),
                    mock(MessageReportService.class),
                    mock(DataBucketReportService.class),
                    mock(StopPipelineService.class),
                    mock(GcSkuTicketReportService.class),
                    mock(GcValidationReportService.class),
                    mock(GcDataBucketReportService.class),
                    mock(GcFileDataReportService.class),
                    mock(GcSingleTicketReportService.class),
                    mock(GcMessageReportService.class),
                    mock(LongPipelinesReportService.class),
                    mock(InternalErrorReportService.class),
                    feedTemplateGenerator,
                    mock(CWVerdictFixService.class),
                    mock(DataCampOffersReportService.class),
                    mock(ExternalRequestsReportService.class),
                    "test",
                    mock(FakeDatacampOfferDao.class)
            );
            return partnerContentService;
        }

        @Bean
        public PartnerContentFileService partnerContentFileService(
                PartnerContentDao partnerContentDao,
                GcSkuTicketDao gcSkuTicketDao,
                SourceDao sourceDao,
                TemplateFeedUploadDao templateFeedUploadDao,
                MdsFileStorageService mdsFileStorageService,
                @Qualifier("dao.config.pipeline.manager") PipelineManager pipelineManager) {
            return new PartnerContentFileService(
                    partnerContentDao,
                    pipelineManager,
                    gcSkuTicketDao,
                    sourceDao,
                    templateFeedUploadDao,
                    mdsFileStorageService);
        }
    }
}
