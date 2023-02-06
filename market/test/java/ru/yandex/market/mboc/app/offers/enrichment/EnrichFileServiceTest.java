package ru.yandex.market.mboc.app.offers.enrichment;

import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.io.ByteStreams;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundAction;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionRepositoryMock;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionServiceImpl;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.SupplyEvent;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.DefaultOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.offers.OfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.converter.ErrorAtLine;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResult;
import ru.yandex.market.mboc.common.services.idxapi.IndexerApiException;
import ru.yandex.market.mboc.common.services.idxapi.IndexerApiService;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.enrichment.EnrichedExcel;
import ru.yandex.market.mboc.common.services.offers.enrichment.EnrichedExcelRepository;
import ru.yandex.market.mboc.common.services.offers.enrichment.EnrichedExcelRepositoryMock;
import ru.yandex.market.mboc.common.services.offers.enrichment.OffersEnrichmentService;
import ru.yandex.market.mboc.common.services.offers.enrichment.TransformImportOffersService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.report.ReportPriceService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.web.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * @author galaev@yandex-team.ru
 * @since 26/07/2018.
 */
@SuppressWarnings("checkstyle:magicnumber")
public class EnrichFileServiceTest {

    public static final int TEST_SUPPLIER_ID = 1;
    public static final String TEST_SHOP_SKU = "shop-sku";
    private static final int TEMPLATE_HEADERS_OFFSET = 1;
    private BackgroundActionServiceImpl backgroundActionService;
    private BackgroundEnrichFileService enrichFileService;
    private OffersEnrichmentService enrichmentService;
    private BackgroundActionRepositoryMock actionRepository;
    private ReportPriceService reportPriceService;
    private EnrichedExcelRepository enrichedExcelRepository;
    private IndexerApiService indexerApiService;
    private AmazonS3Client s3Client;

    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;

    private final OfferDestinationCalculator offerDestinationCalculator = new DefaultOfferDestinationCalculator();


    @Before
    public void setup() {
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        var storageKeyValueService = new StorageKeyValueServiceMock();
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
                new SupplierConverterServiceMock(), storageKeyValueService);

        actionRepository = new BackgroundActionRepositoryMock();
        backgroundActionService = new BackgroundActionServiceImpl(actionRepository, new TransactionTemplateMock(), 1);
        backgroundActionService.init();
        enrichmentService = Mockito.mock(OffersEnrichmentService.class);
        reportPriceService = Mockito.mock(ReportPriceService.class);
        enrichedExcelRepository = new EnrichedExcelRepositoryMock();
        indexerApiService = Mockito.mock(IndexerApiService.class);
        s3Client = Mockito.mock(AmazonS3Client.class);

        var categoryCachingServiceMock = new CategoryCachingServiceMock();
        OffersToExcelFileConverterConfig config =
            new OffersToExcelFileConverterConfig(categoryCachingServiceMock);
        MboTimeUnitAliasesService timeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);

        var supplierRepository = new SupplierRepositoryMock();
        var supplierService = new SupplierService(supplierRepository);
        var categoryInfoRepository = new CategoryInfoRepositoryMock(new MboUsersRepositoryMock());
        var modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();

        var needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, null, offerDestinationCalculator,
            storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock, null,
            supplierRepository);
        var antiMappingRepositoryMock = new AntiMappingRepositoryMock();
        var categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        var offersProcessingStatusService = new OffersProcessingStatusService(null, needContentStatusService,
            supplierService, categoryKnowledgeServiceMock, retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepository, antiMappingRepositoryMock, offerDestinationCalculator,
            new StorageKeyValueServiceMock(), new FastSkuMappingsService(needContentStatusService), false, false, 3,
            new CategoryInfoCacheImpl(categoryInfoRepository));
        TransformImportOffersService transformImportOffersService =
            new TransformImportOffersService(offerMappingActionService, offersProcessingStatusService,
                offerDestinationCalculator);

        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        enrichFileService = new BackgroundEnrichFileService(backgroundActionService, enrichedExcelRepository,
            new EnrichFileService(enrichmentService, reportPriceService,
                config.enrichmentConverter(modelStorageCachingServiceMock, timeUnitAliasesService,
                    supplierRepository, storageKeyValueService),
                enrichedExcelRepository, indexerApiService, s3Client, "",
                transformImportOffersService));
    }

    @After
    public void tearDown() throws InterruptedException {
        backgroundActionService.stop();
    }

    @Test
    public void testServiceWorksForExcel() throws IOException, InterruptedException {
        mockCorrectResponses();

        byte[] fileBytes = readResource("excel/CorrectSample.xls");

        int actionId = enrichFileService.enrichFile("test_file.xls", fileBytes, "василий");
        backgroundActionService.stop(); // ждем выполнения

        EnrichedExcel enrichedExcel = enrichFileService.getEnrichedExcel(actionId);
        Assertions.assertThat(enrichedExcel.getFileName()).startsWith("test_file.xls");
        Assertions.assertThat(enrichedExcel.getExcelFile()).isNotNull();

        // assert that file is deleted from enrichedExcelRepository
        enrichedExcel = enrichFileService.getEnrichedExcel(actionId);
        Assertions.assertThat(enrichedExcel).isNull();
    }


    @Test
    public void testServiceWorksForYml() throws IOException, InterruptedException {
        mockCorrectResponses();

        byte[] fileBytes = readResource("yml/valid-one-offer.yml");
        int actionId = enrichFileService.enrichFile("valid-one-offer.yml", fileBytes, "петрович");
        backgroundActionService.stop(); // ждем выполнения

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Done");
        Result result = (Result) action.getResult();
        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);

        EnrichedExcel enrichedExcel = enrichFileService.getEnrichedExcel(actionId);
        Assertions.assertThat(enrichedExcel.getFileName()).startsWith("valid-one-offer.yml");
        Assertions.assertThat(enrichedExcel.getExcelFile()).isNotNull();

        // assert that file is deleted from enrichedExcelRepository
        enrichedExcel = enrichFileService.getEnrichedExcel(actionId);
        Assertions.assertThat(enrichedExcel).isNull();
    }

    @Test
    public void testReportPriceException() throws IOException, InterruptedException {
        Mockito.doThrow(new RuntimeException())
            .when(reportPriceService).fetchPrices(anyList(), any());

        byte[] fileBytes = readResource("excel/CorrectSample.xls");
        int actionId = enrichFileService.enrichFile("test_file.xls", fileBytes, "василий");
        backgroundActionService.stop(); // ждем выполнения

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).contains("RuntimeException");
    }

    @Test
    public void testEnrichmentException() throws IOException, InterruptedException {
        Mockito.doThrow(new RuntimeException()).when(enrichmentService).enrichOffers(anyList(), any());

        byte[] fileBytes = readResource("excel/CorrectSample.xls");
        int actionId = enrichFileService.enrichFile("test_file.xls", fileBytes, "василий");
        backgroundActionService.stop(); // ждем выполнения

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).contains("RuntimeException");
    }

    @Test
    public void testBrokenFileError() throws IOException, InterruptedException {
        byte[] fileBytes = readResource("excel/BrokenSample.xls");
        int actionId = enrichFileService.enrichFile("test_file.xls", fileBytes, "василий");
        backgroundActionService.stop(); // ждем выполнения

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Done");

        Result result = (Result) action.getResult();
        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
    }

    @Test
    public void testUnknownFiletypeError() throws IOException, InterruptedException {
        byte[] fileBytes = readResource("report/good_response.json");
        int actionId = enrichFileService.enrichFile("good_response.json", fileBytes, "василий");
        backgroundActionService.stop(); // ждем выполнения

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Done");

        Result result = (Result) action.getResult();
        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        assertThat(result.getMessage()).isEqualTo("Неправильный формат файла: .json");
    }

    @Test
    public void testYmlS3LoadError() throws IOException, InterruptedException {
        Mockito.when(s3Client.putObject(anyString(), anyString(), any(), any()))
            .thenAnswer(invocation -> {
                throw new RuntimeException();
            });

        byte[] fileBytes = readResource("yml/valid-one-offer.yml");
        int actionId = enrichFileService.enrichFile("valid-one-offer.yml", fileBytes, "петрович");
        backgroundActionService.stop(); // ждем выполнения

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Done");

        Result result = (Result) action.getResult();
        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        assertThat(result.getMessage()).startsWith("Ошибка загрузки файла в S3");
    }

    @Test
    public void testYmlIndexerApiError() throws IOException, InterruptedException {
        Mockito.when(s3Client.getUrl(anyString(), anyString()))
            .thenAnswer(invocation -> new URL("http://s3.com/url"));
        Mockito.when(indexerApiService.parseYmlOffers(anyString()))
            .thenAnswer(invocation -> {
                throw new IndexerApiException("smth bad happened");
            });

        byte[] fileBytes = readResource("yml/valid-one-offer.yml");
        int actionId = enrichFileService.enrichFile("valid-one-offer.yml", fileBytes, "петрович");
        backgroundActionService.stop(); // ждем выполнения

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Done");

        Result result = (Result) action.getResult();
        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        assertThat(result.getMessage()).startsWith("Ошибка api индексатора");
    }

    @Test
    public void testYmlParseErrors() throws IOException, InterruptedException {
        Mockito.when(s3Client.getUrl(anyString(), anyString()))
            .thenAnswer(invocation -> new URL("http://s3.com/url"));
        Mockito.when(indexerApiService.parseYmlOffers(anyString()))
            .thenAnswer(invocation -> {
                OffersParseResult.Builder<ImportedOffer> result = OffersParseResult.newBuilder();
                result.addParseError(ErrorAtLine.NO_LINE,
                    new ErrorInfo("32f", "bad error", ErrorInfo.Level.ERROR, Collections.emptyMap()));
                return result.build();
            });

        byte[] fileBytes = readResource("yml/valid-one-offer.yml");
        int actionId = enrichFileService.enrichFile("valid-one-offer.yml", fileBytes, "петрович");
        backgroundActionService.stop(); // ждем выполнения

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Done");

        Result result = (Result) action.getResult();
        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        assertThat(result.getMessage()).startsWith("bad error");
    }

    private void mockCorrectResponses() {
        Mockito.doAnswer(invocation -> {
            List<Offer> offers = invocation.getArgument(0);
            offers.forEach(offer -> {
                offer.setBeruPrice(1.0);
                offer.setReferencePrice(2.0);
            });
            return null;
        }).when(reportPriceService).fetchPrices(anyList(), any());
        Mockito.when(s3Client.getUrl(anyString(), anyString()))
            .thenAnswer(invocation -> new URL("http://s3.com/url"));
        Mockito.when(indexerApiService.parseYmlOffers(anyString()))
            .thenAnswer(invocation -> {
                OffersParseResult.Builder<ImportedOffer> result = OffersParseResult.newBuilder();
                result.addOffer(0, new ImportedOffer()
                    .setTitle("title")
                    .setUrls(Collections.singletonList("url"))
                    .setPrice("100")
                    .setCategoryName("category")
                    .setMarketSku("2")
                    .setBarCode("bar")
                    .setShopSkuId("1"));
                return result.build();
            });
    }

    private byte[] readResource(String fileName) throws IOException {
        return ByteStreams.toByteArray(
            getClass().getClassLoader().getResourceAsStream(fileName));
    }

    private MasterData getMasterData() {
        MasterData masterData = new MasterData();
        masterData.setSupplierId(TEST_SUPPLIER_ID);
        masterData.setShopSku(TEST_SHOP_SKU);
        masterData.setMinShipment(1);
        masterData.setQuantumOfSupply(1);
        masterData.setTransportUnitSize(1);
        masterData.setDeliveryTime(1);
        masterData.setSupplySchedule(Collections.singletonList(new SupplyEvent(DayOfWeek.FRIDAY)));
        masterData.setShelfLife(1, TimeInUnits.TimeUnit.DAY);
        masterData.setLifeTime(1, TimeInUnits.TimeUnit.DAY);
        masterData.setGuaranteePeriod(1, TimeInUnits.TimeUnit.DAY);
        masterData.setShelfLifeComment("comment1");
        masterData.setLifeTimeComment("comment2");
        masterData.setGuaranteePeriodComment("comment3");
        masterData.setGuaranteePeriodComment("comment3");
        masterData.setCustomsCommodityCode("code");
        masterData.setManufacturer("manufacturer");
        masterData.setBoxCount(1);
        masterData.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(10.0, 20.0, 30.0, 2.0, 1.0, null).build());
        masterData.setUseInMercury(true);
        masterData.setVetisGuids(Collections.singletonList("b04cdcd1-ed94-49ad-a0e9-e58f4184f6f6"));
        return masterData;
    }
}
