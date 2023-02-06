package ru.yandex.market.mboc.app.offers.enrichment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerService;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.http.SkuBDApi.SkutchType;
import ru.yandex.market.mboc.app.importexcel.BackgroundImportService;
import ru.yandex.market.mboc.app.offers.ImportExcelService;
import ru.yandex.market.mboc.app.offers.ImportFileService;
import ru.yandex.market.mboc.app.offers.ImportOffersProcessService;
import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundAction;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionRepositoryMock;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionServiceImpl;
import ru.yandex.market.mboc.common.golden.GoldenMatrixService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkDepartmentService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter.MasterDataConvertResult;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataParsingConfig;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.ExcelS3ServiceMock;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.MasterDataRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.converter.LineWith;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.idxapi.IndexerApiService;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
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
import ru.yandex.market.mboc.common.services.ultracontroller.UltraControllerServiceImpl;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.web.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class EnrichAndImportOffersTest {
    private static final Logger log = LoggerFactory.getLogger(EnrichAndImportOffersTest.class);

    private BackgroundEnrichFileService enrichFileService;
    private BackgroundImportService backgroundImportService;

    private BackgroundActionServiceImpl backgroundActionService;
    private OffersEnrichmentService enrichmentService;
    private BackgroundActionRepositoryMock actionRepository;
    private ReportPriceService reportPriceService;
    private EnrichedExcelRepository enrichedExcelRepository;
    private OfferRepositoryMock offerRepository;
    private MasterDataRepositoryMock masterDataRepositoryMock;
    private UltraControllerService ultraControllerServiceRemote;
    private ModelStorageCachingServiceMock modelStorageCachingService;
    private IndexerApiService indexerApiService;
    private AmazonS3Client s3Client;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private MboTimeUnitAliasesService mboTimeUnitAliasesService;
    private ImportedOfferToMasterDataConverter masterDataConverter;
    private SupplierRepositoryMock supplierRepository;
    private MasterDataHelperService masterDataHelperService;
    private ApplySettingsService applySettingsService;
    private ImportExcelService importExcelService;
    private ImportOffersProcessService importOffersProcessService;
    private SupplierService supplierService;

    @Before
    public void setup() {
        offerRepository = new OfferRepositoryMock();
        masterDataRepositoryMock = new MasterDataRepositoryMock();
        supplierRepository = new SupplierRepositoryMock();
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        supplierService = new SupplierService(supplierRepository);
        masterDataHelperService = Mockito.mock(MasterDataHelperService.class);
        Mockito.doAnswer(call -> {
                Collection<MasterData> masterData = call.getArgument(0);
                Map<ShopSkuKey, List<ErrorInfo>> result = masterData.stream()
                    .collect(Collectors.toMap(md -> md.getShopSkuKey(), md -> new ArrayList<>()));
                return result;
            })
            .when(masterDataHelperService).saveSskuMasterDataAndDocuments(Mockito.any());

        Mockito.doAnswer(call -> {
                Collection<MasterData> masterData = call.getArgument(0);
                Map<ShopSkuKey, List<ErrorInfo>> result = masterData.stream()
                    .collect(Collectors.toMap(md -> md.getShopSkuKey(), md -> new ArrayList<>()));
                return result;
            })
            .when(masterDataHelperService).validateMasterData(any());
        actionRepository = new BackgroundActionRepositoryMock();
        backgroundActionService = new BackgroundActionServiceImpl(actionRepository, new TransactionTemplateMock(), 1);
        backgroundActionService.init();
        reportPriceService = Mockito.mock(ReportPriceService.class);
        enrichedExcelRepository = new EnrichedExcelRepositoryMock();
        indexerApiService = Mockito.mock(IndexerApiService.class);
        s3Client = Mockito.mock(AmazonS3Client.class);
        categoryCachingServiceMock = new CategoryCachingServiceMock();
        ultraControllerServiceRemote = Mockito.mock(UltraControllerService.class);
        UltraControllerServiceImpl ultraControllerService =
            new UltraControllerServiceImpl(
                ultraControllerServiceRemote,
                UltraControllerServiceImpl.DEFAULT_RETRY_COUNT,
                UltraControllerServiceImpl.DEFAULT_RETRY_SLEEP_MS);
        modelStorageCachingService = new ModelStorageCachingServiceMock();

        var mboUsersRepo = new MboUsersRepositoryMock();
        var categoryInfoRepository = new CategoryInfoRepositoryMock(mboUsersRepo);
        var categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepository);
        var keyValueService = new StorageKeyValueServiceMock();
        var offerDestinationCalculator = new ContextedOfferDestinationCalculator(categoryInfoCache, keyValueService);

        var needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        var honestMarkDepartmentServiceMock = Mockito.mock(HonestMarkDepartmentService.class);
        var calculator = new OfferCategoryRestrictionCalculator(
            honestMarkDepartmentServiceMock, categoryInfoCache);
        var honestMarkClassificationService = new HonestMarkClassificationService(
            Collections.emptySet(),
            categoryCachingServiceMock,
            needContentStatusService,
            calculator);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, calculator,
            offerDestinationCalculator, new StorageKeyValueServiceMock());
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingService, null,
            supplierRepository);
        var antiMappingRepositoryMock = new AntiMappingRepositoryMock();
        var categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        var offersProcessingStatusService = new OffersProcessingStatusService(null, needContentStatusService,
            supplierService, categoryKnowledgeServiceMock, retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepository, antiMappingRepositoryMock, offerDestinationCalculator,
            keyValueService, new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);
        enrichmentService = new OffersEnrichmentService(
            Mockito.mock(GoldenMatrixService.class),
            ultraControllerService,
            offerMappingActionService,
            supplierService, categoryKnowledgeServiceMock,
            honestMarkClassificationService,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            Mockito.mock(BooksService.class), offerDestinationCalculator,
            categoryInfoCache);

        var config = new OffersToExcelFileConverterConfig(categoryCachingServiceMock);

        mboTimeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        var storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        masterDataConverter = Mockito.spy(
            new ImportedOfferToMasterDataConverter(
                new MasterDataParsingConfig(mboTimeUnitAliasesService, storageKeyValueServiceMock),
                masterDataHelperService)
        );
        applySettingsService = Mockito.mock(ApplySettingsService.class);

        TransformImportOffersService transformImportOffersService =
            new TransformImportOffersService(offerMappingActionService, offersProcessingStatusService,
                offerDestinationCalculator);

        importOffersProcessService = new ImportOffersProcessService(
            offerRepository,
            masterDataRepositoryMock,
            masterDataHelperService,
            enrichmentService,
            TransactionHelper.MOCK,
            masterDataConverter,
            supplierRepository,
            applySettingsService,
            offersProcessingStatusService,
            transformImportOffersService,
            Mockito.mock(GlobalVendorsCachingService.class),
            false,
            keyValueService);

        importExcelService = new ImportExcelService(
            config.importedExcelFileConverter(
                modelStorageCachingService,
                mboTimeUnitAliasesService,
                supplierRepository,
                storageKeyValueServiceMock
            ),
            new ExcelS3ServiceMock(),
            importOffersProcessService);

        backgroundImportService = new BackgroundImportService(backgroundActionService);

        enrichFileService = new BackgroundEnrichFileService(backgroundActionService, enrichedExcelRepository,
            new EnrichFileService(
                enrichmentService,
                reportPriceService,
                config.enrichmentConverter(
                    modelStorageCachingService,
                    mboTimeUnitAliasesService,
                    supplierRepository,
                    storageKeyValueServiceMock
                ),
                enrichedExcelRepository,
                indexerApiService,
                s3Client,
                "",
                transformImportOffersService));

        Mockito.when(ultraControllerServiceRemote.enrich(Mockito.any())).then(call -> {
            UltraController.DataRequest request = call.getArgument(0);

            UltraController.DataResponse.Builder builder = UltraController.DataResponse.newBuilder();
            for (UltraController.Offer o : request.getOffersList()) {
                builder.addOffers(UltraController.EnrichedOffer.newBuilder()
                    .setCategoryId(1)
                    .setModelId(2)
                    .setMatchedId(2)
                    .setVendorId(3)
                    .setMarketSkuId(o.getShopId())
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.NOT_APPROVED_SKU)
                    .setSkutchType(SkutchType.SKUTCH_BY_SKU_ID)
                    .setMarketCategoryName("market category name")
                    .setMarketModelName("market model name")
                    .setMarketVendorName("market vendor name")
                    .setMarketSkuPublishedOnMarket(true)
                    .setMarketSkuPublishedOnBlueMarket(true)
                    .build());
            }
            return builder.build();
        });
    }

    @After
    public void tearDown() throws InterruptedException {
        backgroundActionService.stop();
    }

    /**
     * Тест проверяет, что если мы обогащаем, а затем импортируем файл.
     * То колонки не будут записываться в extraShopFields.
     */
    @Test
    public void testEnrichAndImportResult() throws IOException {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        modelStorageCachingService
            .addModel(new Model()
                .setId(2L)
                .setCategoryId(1))
            .addModel(new Model()
                .setId(42L)
                .setCategoryId(1)
                .setSkuParentModelId(2L));

        // ФАЗА 1. Обогащаем файл и получаем результат
        byte[] fileBytes = readResource("excel/CorrectSample.xls");
        int enrichmentActionId = enrichFileService.enrichFile("test_file.xls", fileBytes, "василий");
        waitForFinish(enrichmentActionId); // ждем выполнения
        EnrichedExcel enrichedExcel = enrichFileService.getEnrichedExcel(enrichmentActionId);

        // промежуточный этап - печатаем результат переобогащения в консоль
        ExcelFile excelFile = ExcelFileConverter.convert(new ByteArrayInputStream(enrichedExcel.getExcelFile()),
            ExcelHeaders.MBOC_EXCEL_IGNORES_CONFIG);
        log.info("Enrichment file\n" + excelFile);

        // ФАЗА 2. Импортируем полученный файл
        Mockito
            .doAnswer(invocation -> {
                List<LineWith<ImportedOffer>> argument = invocation.getArgument(0);
                return argument
                    .stream()
                    .map(x ->
                        new MasterDataConvertResult(x.getLine(), new MasterData(), Collections.emptyList()))
                    .collect(Collectors.toList());
            })
            .when(masterDataConverter).convert(anyList());
        // т/к/ происходит конверт в Offer и обратно в файл. Пока отрубаю валидацию, надо чинить в отдельном тикете

        int actionId = backgroundImportService.startImportExcel("some file.xlsx", "description", "василий",
            actionHandle -> importExcelService
                .importExcel(42, "some file.xlsx", enrichedExcel.getExcelFile(),
                    actionHandle::updateState, "василий",
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        waitForFinish(actionId);  // ждем выполнения
        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(((Result) action.getResult()).getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);

        // ФАЗА 3. Сравниваем оффер с тем, что ожидали
        List<Offer> offers = offerRepository.findAll();
        Offer expected = new Offer()
            .setId(1)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("12222")
            .setTitle("Дрель Makita 6413 безударная")
            .setVendorId(3)
            .setModelId(2L)
            .setShopCategoryName("Дрели и миксеры")
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.builder()
                .id(1)
                .urls(Collections.singletonList("https://www.ozon.ru/context/detail/id/6374230"))
                .addExtraShopFields(ExcelHeaders.PRICE.getTitle(), "2450")
                .addExtraShopFields(ExcelHeaders.NDS.getTitle(), "10%")
                .build())
            .setMarketModelName("market model name")
            .setMarketVendorName("market vendor name")
            .setVendorCode("12345678")
            .setBarCode("4607004650642")
            .setRealization(true)
            .setCreatedByLogin("василий")
            .setModifiedByLogin("василий")
            .setSuggestSkuMapping(new Offer.Mapping(42, DateTimeUtils.dateTimeNow()))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_SKU_ID)
            .setSuggestModelMappingId(2L)
            .setSuggestMarketModelName("market model name")
            .setSuggestCategoryMappingId(1L)
            .addNewServiceOfferIfNotExistsForTests(supplier)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .markLoadedContent();
        MbocAssertions.assertThat(offers).hasSize(1).first().isEqualTo(expected);
    }

    private void waitForFinish(int actionId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Object> task = () -> {
            while (!actionRepository.findById(actionId).isActionFinished()) {
                Thread.sleep(100);
            }
            return null;
        };
        Future<Object> future = executor.submit(task);
        try {
            future.get(1, TimeUnit.MINUTES);
        } catch (TimeoutException | InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        } finally {
            future.cancel(true);
        }
    }

    private byte[] readResource(String fileName) throws IOException {
        return ByteStreams.toByteArray(
            getClass().getClassLoader().getResourceAsStream(fileName));
    }
}
