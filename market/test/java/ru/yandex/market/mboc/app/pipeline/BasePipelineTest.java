package ru.yandex.market.mboc.app.pipeline;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.application.properties.utils.Environments;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerService;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.app.content.ContentController;
import ru.yandex.market.mboc.app.mapping.RecheckMappingService;
import ru.yandex.market.mboc.app.offers.ExcelS3Service;
import ru.yandex.market.mboc.app.offers.ImportExcelService;
import ru.yandex.market.mboc.app.offers.ImportFileService;
import ru.yandex.market.mboc.app.offers.ImportOffersProcessService;
import ru.yandex.market.mboc.app.offers.OfferProtoConverter;
import ru.yandex.market.mboc.app.offers.OffersController;
import ru.yandex.market.mboc.app.offers.OffersWebService;
import ru.yandex.market.mboc.app.offers.enrichment.BackgroundEnrichFileService;
import ru.yandex.market.mboc.app.offers.models.SimpleUpdateRequest;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.MboCategoryServiceImpl;
import ru.yandex.market.mboc.app.proto.MboMappingsServiceImpl;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.categorygroups.CategoryGroupService;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.datacamp.repository.TempImportChangeDeltaRepository;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.dict.WarehouseServiceService;
import ru.yandex.market.mboc.common.golden.GoldenMatrixService;
import ru.yandex.market.mboc.common.honestmark.AutoClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkDepartmentService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.logisticsparams.repository.SkuLogisticParamsRepository;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataFromMdiConverter;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataParsingConfig;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.modelform.CachedModelForm;
import ru.yandex.market.mboc.common.modelform.ModelFormCachingService;
import ru.yandex.market.mboc.common.msku.BuyPromoPriceRepository;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.offers.ManualVendorService;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleService;
import ru.yandex.market.mboc.common.offers.acceptance.service.AcceptanceService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.IMasterDataRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferMappingHistoryRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.OfferStatService;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.offers.repository.RemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepository;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.ImportCategoryKnowledgeService;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledge;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeRepository;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeService;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceImpl;
import ru.yandex.market.mboc.common.services.converter.ExcelFileToOffersConverter;
import ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandler;
import ru.yandex.market.mboc.common.services.datacamp.SendDataCampOfferStatesService;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.managers.ManagersServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.OfferProcessingStrategiesHolder;
import ru.yandex.market.mboc.common.services.offers.ReopenNeedInfoService;
import ru.yandex.market.mboc.common.services.offers.UpdateSupplierOfferCategoryService;
import ru.yandex.market.mboc.common.services.offers.antimapping.AntiMappingService;
import ru.yandex.market.mboc.common.services.offers.auto_approves.CompositeAutoApproveService;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SuggestAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SupplierAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.enrichment.OffersEnrichmentService;
import ru.yandex.market.mboc.common.services.offers.enrichment.TransformImportOffersService;
import ru.yandex.market.mboc.common.services.offers.fashion.FashionClassificationCheckImpl;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingRollbackService;
import ru.yandex.market.mboc.common.services.offers.mapping.PskuHasContentMappingsService;
import ru.yandex.market.mboc.common.services.offers.mapping.RecheckClassificationService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.mapping.SaveMappingModerationService;
import ru.yandex.market.mboc.common.services.offers.mapping.SaveTaskMappingsService;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersProcessingService;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersService;
import ru.yandex.market.mboc.common.services.offers.processing.MatchingOffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.MatchingOffersService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedSizeMeasureFilter;
import ru.yandex.market.mboc.common.services.offers.processing.NoSizeMeasureValueStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.ProcessingTicketHelper;
import ru.yandex.market.mboc.common.services.offers.processing.WaitCatalogProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.WaitContentProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.queue.OfferQueueService;
import ru.yandex.market.mboc.common.services.offers.tracker.OffersTrackerService;
import ru.yandex.market.mboc.common.services.offers.upload.ErpOfferUploadQueueService;
import ru.yandex.market.mboc.common.services.offers.upload.MdmOfferUploadQueueService;
import ru.yandex.market.mboc.common.services.offers.upload.OfferChangeForUploadObserver;
import ru.yandex.market.mboc.common.services.offers.upload.YtOfferUploadQueueService;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoHelperService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.proto.MboMappingsHelperService;
import ru.yandex.market.mboc.common.services.proto.SizeMeasureHelper;
import ru.yandex.market.mboc.common.services.sku.OfferClearMappingService;
import ru.yandex.market.mboc.common.services.ultracontroller.UltraControllerServiceImpl;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.utils.CategorySizeMeasureServiceStub;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.SecurityContextAuthenticationHelper;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingServiceMock;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.ProductUpdateRequestInfo.ChangeSource;
import ru.yandex.market.mboc.http.MboMappings.UpdateContentProcessingTasksRequest.ContentProcessingTask;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult;
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;

/**
 * Base class for pipeline integration tests.
 */
public abstract class BasePipelineTest extends BaseMbocAppTest {

    protected static final String SHOP_SKU = "shop-sku-1";
    protected static final long MODEL_PARENT_ID = 1;
    protected static final String MODEL_PARENT_TITLE = "parent model title";
    protected static final long MARKET_SKU_ID_1 = 1000;
    protected static final String MARKET_SKU_TITLE_1 = "title 1";
    protected static final long MARKET_SKU_ID_2 = 2000;
    protected static final String MARKET_SKU_TITLE_2 = "title 2";
    protected static final String DESCRIPTION = "DESCRIPTION UPDATED";
    protected static final long DELETED_SKU_ID = 3000;
    protected static final String DELETED_SKU_TITLE = "deleted title 1";
    protected static final long FAST_SKU_ID = 4000;
    protected static final String FAST_SKU_TITLE = "fast title 1";
    protected static final long PARTNER_SKU_ID_1 = 5000;
    protected static final String PARTNER_SKU_TITLE_1 = "ptitle 1";
    protected static final long PARTNER_SKU_ID_2 = 6000;
    protected static final String PARTNER_SKU_TITLE_2 = "ptitle 2";

    protected static final Offer.Mapping MAPPING_1 = OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1);
    protected static final Offer.Mapping MAPPING_2 = OfferTestUtils.mapping(MARKET_SKU_ID_2, MARKET_SKU_TITLE_2);

    protected static final String BAR_CODE = "bar code";
    protected static final String URL = "http://test-shop.com/lol/kek";

    protected static final String YANG_OPERATOR_LOGIN = "yang operator";
    protected static final long CONTENT_PROCESSING_TASK_ID_1 = 1;
    protected static final long CONTENT_PROCESSING_TASK_ID_2 = 2;

    // repos
    @Autowired
    protected TransactionHelper transactionHelper;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected OfferRepositoryImpl offerRepository;
    @Autowired
    protected IMasterDataRepository masterDataFor1pRepository;
    @Autowired
    protected AntiMappingRepository antiMappingRepository;
    @Autowired
    protected OfferBatchProcessor offerBatchProcessor;
    @Autowired
    @Qualifier("slaveOfferRepository")
    protected OfferRepository slaveOfferRepository;
    @Autowired
    protected SupplierRepository supplierRepository;
    @Autowired
    protected CategoryInfoRepository categoryInfoRepository;
    @Autowired
    protected MskuRepository mskuRepository;
    @Autowired
    protected RemovedOfferRepository removedOfferRepository;
    @Autowired
    protected ContentProcessingQueueRepository contentProcessingQueue;
    @Autowired
    protected QueueFromContentProcessingRepository queueFromContentProcessingRepository;
    @Autowired
    protected OfferMetaRepository offerMetaRepository;
    @Autowired
    protected OfferProcessingAssignmentRepository assignmentRepository;
    @Autowired
    protected MigrationStatusRepository migrationStatusRepository;
    @Autowired
    protected MigrationOfferRepository migrationOfferRepository;
    @Autowired
    protected MigrationRemovedOfferRepository migrationRemovedOfferRepository;

    // Most of work is done by these services:
    @Autowired
    protected OfferUpdateSequenceService offerUpdateSequenceService;
    @Autowired
    protected TempImportChangeDeltaRepository tempImportChangeDeltaRepository;
    @Autowired
    protected OfferChangeForUploadObserver offerChangeForUploadObserver;
    @Autowired
    protected YtOfferUploadQueueService ytOfferUploadQueueService;
    @Autowired
    protected ErpOfferUploadQueueService erpOfferUploadQueueService;
    @Autowired
    protected MdmOfferUploadQueueService mdmOfferUploadQueueService;
    @Autowired
    @Qualifier("forModerationQueueService")
    protected OfferQueueService forModerationQueueService;
    @Autowired
    @Qualifier("createModerationTickets")
    protected OfferQueueService createModerationTicketsQueueService;
    @Autowired
    @Qualifier("createMatchingTickets")
    protected OfferQueueService createMatchingTicketsQueueService;
    @Autowired
    @Qualifier("createClassificationTickets")
    protected OfferQueueService createClassificationTicketsQueueService;
    @Autowired
    @Qualifier("createWaitCatalogTickets")
    protected OfferQueueService createWaitCatalogTicketsQueueService;
    @Autowired
    @Qualifier("createWaitContentTickets")
    protected OfferQueueService createWaitContentTicketsQueueService;
    @Autowired
    @Qualifier("createNoSizeMeasureValueTickets")
    protected OfferQueueService createNoSizeMeasureValueTicketsQueueService;
    @Autowired
    @Qualifier("reopenNeedInfoQueueService")
    protected OfferQueueService reopenNeedInfoQueue;
    @Autowired
    protected CategoryRuleService categoryRuleService;

    protected OfferStatService offerStatService;
    protected MasterDataHelperService masterDataHelperService;
    protected MigrationService migrationService;
    // import offer via excel file
    protected ImportExcelService importExcelService;
    protected ImportOffersProcessService importOffersProcessService;
    // transforms and enriches offers (used in import)
    protected OffersEnrichmentService offersEnrichmentService;
    // sets ACCEPTANCE_STATUS from UI
    protected OffersController offersController;
    // force offers to classification
    protected ContentController contentController;
    protected OffersTrackerService offersTrackerService;
    // handles to new(updated) mappings from supplier and content
    // and content processing changes
    protected MboMappingsService mboMappingsService;
    // service responsible for mapping (supplier and content)
    protected OfferMappingActionService offerMappingActionService;
    // handles moderation results
    protected MboCategoryService mboCategoryService;
    // returns need_info offers to in_moderation if offer changed
    protected ReopenNeedInfoService reopenNeedInfoService;
    // returns no_knowledge offers to need_mapping if category knowledge appeared
    protected ImportCategoryKnowledgeService importCategoryKnowledgeService;
    // check if category has knowledge
    protected CategoryKnowledgeService categoryKnowledgeService;
    // send offer to matching
    protected MatchingOffersService matchingOffersService;
    // send offer to classification
    protected ClassificationOffersService classificationOffersService;
    // tms tasks
    protected OffersProcessingStatusService offersProcessingStatusService;

    protected Map<Long, CachedModelForm> modelFormMap;
    // mbo models mock
    protected ModelStorageCachingServiceMock modelStorageCachingService;
    protected ModelFormCachingService modelFormServiceMock;
    protected NeedContentStatusService needContentStatusService;
    protected RetrieveMappingSkuTypeService retrieveMappingSkuTypeService;
    protected CategoryCachingServiceMock categoryCachingService;
    protected MboMappingsHelperService mboMappingsHelperService;
    protected CompositeAutoApproveService autoApproveService;
    protected AcceptanceService acceptanceService;
    protected FastSkuMappingsService fastSkuMappingsService;
    protected ApplySettingsService applySettingsService;
    protected ManualVendorService manualVendorService;
    protected ProcessingTicketInfoService processingTicketInfoService;
    protected PskuHasContentMappingsService pskuHasContentMappingsService;
    protected AddProductInfoHelperService addProductInfoHelperService;
    protected SupplierService supplierService;
    protected ClassificationOffersProcessingService classificationOffersProcessingService;
    protected HonestMarkClassificationService honestMarkClassificationService;
    protected TrackerServiceMock trackerServiceMock;
    protected StaffServiceMock staffService;
    protected MboUsersRepositoryMock mboUsersRepository;
    protected GlobalVendorsCachingService globalVendorsCachingService;
    protected SizeMeasureHelper sizeMeasureHelper;
    protected CategorySizeMeasureServiceStub sizeMeasureService;
    protected OffersWebService offersWebService;
    protected AntiMappingService antiMappingService;
    protected BooksService booksService;
    protected OfferClearMappingService offerClearMappingService;
    protected OfferCategoryRestrictionCalculator offerCategoryRestrictionCalculator;
    protected WarehouseServiceService warehouseServiceService;
    protected OfferMappingRollbackService offerMappingRollbackService;
    protected RecheckClassificationService recheckClassificationService;
    private ExcelFileToOffersConverter<ImportedOffer> importedExcelFileConverter;
    private ImportedOfferToMasterDataConverter offerToMasterDataConverter;
    private OfferProtoConverter offerProtoConverter;
    private DataCampConverterService dataCampConverterService;

    @Autowired
    private CategoryKnowledgeRepository categoryKnowledgeRepository;
    @Autowired
    private BuyPromoPriceRepository buyPromoPriceRepository;
    @Autowired
    private BusinessSupplierService businessSupplierService;
    @Autowired
    private ProcessingTicketInfoRepository processingTicketInfoRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    protected OfferMappingHistoryRepository offerMappingHistoryRepository;
    @Autowired
    protected CategoryGroupService categoryGroupService;

    @Before
    public void setUp() {
        storageKeyValueService.putValue(LogbrokerDatacampOfferMessageHandler.OFFER_ACCEPTANCE_REPORT_FLAG, true);
        storageKeyValueService.putValue(SendDataCampOfferStatesService.ALIVE_OFFER_ACCEPTANCE_REPORT_FLAG, true);
        storageKeyValueService.putValue(SaveMappingModerationService.ROLLBACK_ENABLED_FLAG, true);

        processingTicketInfoService = new ProcessingTicketInfoService(processingTicketInfoRepository);
        pskuHasContentMappingsService = new PskuHasContentMappingsService(offerRepository);
        trackerServiceMock = new TrackerServiceMock();
        manualVendorService = new ManualVendorService(offerMappingActionService);
        supplierService = new SupplierService(supplierRepository);
        offersWebService = new OffersWebService(offerRepository, null, categoryCachingService,
            businessSupplierService, transactionHelper);
        antiMappingService = new AntiMappingService(antiMappingRepository, transactionHelper);
        offerClearMappingService = Mockito.mock(OfferClearMappingService.class);
        booksService = Mockito.mock(BooksService.class);
        offerCategoryRestrictionCalculator = new OfferCategoryRestrictionCalculator(
            Mockito.mock(HonestMarkDepartmentService.class),
            new CategoryInfoCacheImpl(categoryInfoRepository));
        warehouseServiceService = Mockito.mock(WarehouseServiceService.class);

        offerStatService = new OfferStatService(
            namedParameterJdbcTemplate, slaveNamedParameterJdbcTemplate,
            Mockito.mock(SkuLogisticParamsRepository.class), transactionHelper,
            offerRepository, storageKeyValueService);
        offerStatService.subscribe();

        var masterDataServiceMock = new MasterDataServiceMock();
        var supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        offerToMasterDataConverter = new ImportedOfferToMasterDataConverter(
            new MasterDataParsingConfig(Mockito.mock(MboTimeUnitAliasesService.class), storageKeyValueService),
            masterDataHelperService
        );

        migrationService = new MigrationService(migrationStatusRepository,
            migrationOfferRepository, migrationRemovedOfferRepository,
            supplierRepository, offerUpdateSequenceService, offerMetaRepository);

        dataCampConverterService = new DataCampConverterService(
            Mockito.mock(DataCampIdentifiersService.class),
            Mockito.mock(OfferCategoryRestrictionCalculator.class),
            storageKeyValueService,
            true
        );

        mockRecheckClassificationService();
        mockUserServices();
        mockNoSizeMeasureServices();
        mockCategoryTree();
        mockNeedContentStatusService();
        mockMappingActionService();
        mockMboModelStorage();
        mockCategoryKnowledgeService();
        mockRetrieveMappingSkuTypeService();
        mockOffersProcessingStatusService();
        mockHonestMarkClassificationService();
        mockClassificationProcessingService();
        mockImportCategoryKnowledge();
        mockAcceptanceServices();
        mockOffersTrackerService();
        mockEnrichmentService();
        mockImportOffersProcessService();
        mockImportExcelService();
        mockOffersController();
        mockContentController();
        mockMboMappingsHelperService();
        mockMboMappingsService();
        mockMboCategoryService();
        mockReopenNeedInfoExecutor();

        supplierRepository.insert(OfferTestUtils.simpleSupplier()
            .setType(MbocSupplierType.THIRD_PARTY)
            .setFulfillment(true)
            .setNewContentPipeline(true));
        supplierRepository.insert(OfferTestUtils.businessSupplier());
        supplierRepository.insert(OfferTestUtils.blueSupplierUnderBiz1()
            .setFulfillment(true)
        );
        supplierRepository.insert(OfferTestUtils.blueSupplierUnderBiz2()
            .setFulfillment(true)
        );
        supplierRepository.insert(OfferTestUtils.whiteSupplierUnderBiz());
        supplierRepository.insert(OfferTestUtils.dsbsSupplierUnderBiz()
            .setDropshipBySeller(true)
        );
        categoryInfoRepository.insert(OfferTestUtils.categoryInfoWithManualAcceptance()
            .setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
            .setFbyPlusAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
            .setFbsAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
            .setDsbsAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
            .setExpressAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
        );
        SecurityContextAuthenticationHelper.setAuthenticationToken();

        migrationService.checkAndUpdateCache();
    }

    private void mockNoSizeMeasureServices() {
        sizeMeasureService = new CategorySizeMeasureServiceStub();
        globalVendorsCachingService = new GlobalVendorsCachingServiceMock();
        sizeMeasureHelper = new SizeMeasureHelper(sizeMeasureService, sizeMeasureService);
    }

    @After
    public void tearDown() {
        SecurityContextAuthenticationHelper.clearAuthenticationToken();
    }

    private void mockCategoryTree() {
        Category category = new Category().setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setAcceptGoodContent(true)
            .setAcceptContentFromWhiteShops(true)
            .setHasKnowledge(true)
            .setAcceptPartnerSkus(true);
        categoryCachingService = new CategoryCachingServiceMock();
        categoryCachingService.addCategory(category);
    }

    private void mockUserServices() {
        staffService = new StaffServiceMock();
        mboUsersRepository = new MboUsersRepositoryMock();
    }

    private void mockNeedContentStatusService() {
        needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            booksService);
    }

    private void mockMappingActionService() {
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            offerCategoryRestrictionCalculator,
            offerDestinationCalculator,
            storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
    }

    @SuppressWarnings("unchecked")
    private void mockEnrichmentService() {
        // ignoring golden matrix and UC
        offersEnrichmentService = Mockito.spy(new OffersEnrichmentService(
            Mockito.mock(GoldenMatrixService.class),
            new UltraControllerServiceImpl(
                Mockito.mock(UltraControllerService.class),
                UltraControllerServiceImpl.DEFAULT_RETRY_COUNT,
                UltraControllerServiceImpl.DEFAULT_RETRY_SLEEP_MS),
            offerMappingActionService,
            supplierService,
            categoryKnowledgeService,
            honestMarkClassificationService,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            booksService, offerDestinationCalculator,
            categoryInfoCache));
        doAnswer(i -> {
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID,
                Offer.BindingKind.SUGGESTED));
            return null;
        }).when(offersEnrichmentService).enrichOffers(anyList(), any());

        Mockito.doCallRealMethod()
            .when(offersEnrichmentService)
            .enrichOffers(Mockito.anyList(), Mockito.eq(true), Mockito.anyMap());

        Mockito.doAnswer(invocation ->
            ((List<Offer>) invocation.getArgument(0)).stream()
                .peek(offer -> {
                    if (offer.hasCategoryId(Offer.BindingKind.SUGGESTED)) {
                        offer.setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID,
                            Offer.BindingKind.SUGGESTED);
                    }
                })
                .collect(Collectors.toList()))
            .when(offersEnrichmentService)
            .enrichOffers(Mockito.anyList(), Mockito.eq(false), Mockito.anyMap());
    }

    private void mockMboModelStorage() {
        modelStorageCachingService = new ModelStorageCachingServiceMock()
            .addModel(model(MODEL_PARENT_ID, MODEL_PARENT_TITLE, false, Model.ModelType.GURU))
            .addModel(model(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1))
            .addModel(model(MARKET_SKU_ID_2, MARKET_SKU_TITLE_2))
            .addModel(model(PARTNER_SKU_ID_1, PARTNER_SKU_TITLE_1, false, Model.ModelType.PARTNER_SKU))
            .addModel(model(PARTNER_SKU_ID_2, PARTNER_SKU_TITLE_2, false, Model.ModelType.PARTNER_SKU))
            .addModel(model(DELETED_SKU_ID, DELETED_SKU_TITLE, true, Model.ModelType.SKU))
            .addModel(model(FAST_SKU_ID, FAST_SKU_TITLE, true, Model.ModelType.FAST_SKU));
    }

    private void mockHonestMarkClassificationService() {
        honestMarkClassificationService = Mockito.mock(HonestMarkClassificationService.class);

        Mockito.when(honestMarkClassificationService
            .getClassificationResult(any(Offer.class), any(), any(Supplier.class), anySet(), anySet()))
            .thenAnswer(invocation -> {
                UltraController.EnrichedOffer enrichedOffer = invocation.getArgument(0);
                Offer offer = invocation.getArgument(1);
                Set<Long> categoriesWithKnowledge = invocation.getArgument(3);
                var classificationResult = offerCategoryRestrictionCalculator
                    .calculateClassificationResult(enrichedOffer, offer);
                return new AutoClassificationResult(
                    classificationResult,
                    null,
                    categoriesWithKnowledge.contains((long) enrichedOffer.getCategoryId()));
            });
        Mockito.when(honestMarkClassificationService
            .getClassificationResult(any(Offer.class), any(), any(Supplier.class), anySet(), anySet()))
            .thenAnswer(invocation -> {
                Offer offer = invocation.getArgument(0);
                Long targetCategoryId = invocation.getArgument(1);
                Set<Long> categoriesWithKnowledge = invocation.getArgument(3);
                var classificationResult = offerCategoryRestrictionCalculator
                    .calculateClassificationResult(targetCategoryId, offer);
                return new AutoClassificationResult(
                    classificationResult,
                    null,
                    categoriesWithKnowledge.contains(targetCategoryId));
            });
    }

    private void mockClassificationProcessingService() {
        classificationOffersProcessingService = new ClassificationOffersProcessingService(
            categoryCachingService,
            offerMappingActionService,
            offerDestinationCalculator
        );
    }

    private void mockOffersTrackerService() {
        ManagersServiceMock managersService = new ManagersServiceMock();

        var processingTicketHelper = new ProcessingTicketHelper(
            Environments.INTEGRATION_TEST,
            trackerServiceMock, managersService,
            categoryCachingService,
            processingTicketInfoService);

        OffersToExcelFileConverterConfig converterConfig =
            new OffersToExcelFileConverterConfig(categoryCachingService);

        storageKeyValueService.putValue(FashionClassificationCheckImpl.ENABLED_FLAG, true);
        var fashionClassificationCheck = new FashionClassificationCheckImpl(storageKeyValueService,
            offersProcessingStatusService, queueFromContentProcessingRepository, offerRepository,
            offerDestinationCalculator);

        var classificationStrategy = new ClassificationOffersProcessingStrategy(
            trackerServiceMock, offerRepository, supplierRepository, masterDataHelperService,
            converterConfig.classifierConverter(categoryCachingService),
            categoryKnowledgeService,
            classificationOffersProcessingService,
            processingTicketHelper,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            honestMarkClassificationService,
            needContentStatusService,
            applySettingsService,
            offersProcessingStatusService,
            false);

        NeedSizeMeasureFilter needSizeMeasureFilter = Mockito.mock(NeedSizeMeasureFilter.class);
        Mockito.when(needSizeMeasureFilter.createNeedSizeMeasureTickets(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var matchingStrategy = new MatchingOffersProcessingStrategy(
            trackerServiceMock, offerRepository, supplierRepository, masterDataHelperService,
            managersService, offerMappingActionService,
            converterConfig.matchingConverter(modelStorageCachingService, offerRepository),
            needSizeMeasureFilter,
            processingTicketHelper, applySettingsService, offersProcessingStatusService);

        var waitContentProcessingStrategy = new WaitContentProcessingStrategy(
            trackerServiceMock, offerRepository, supplierRepository, categoryInfoRepository,
            mboUsersRepository, categoryCachingService, staffService, offersProcessingStatusService
        );

        var waitCatalogProcessingStrategy = new WaitCatalogProcessingStrategy(
            trackerServiceMock, offerRepository, supplierRepository, categoryInfoRepository,
            mboUsersRepository, categoryCachingService, staffService, offersProcessingStatusService
        );

        var noSizeMeasureValueStrategy = new NoSizeMeasureValueStrategy(offerRepository,
            trackerServiceMock, supplierRepository,
            globalVendorsCachingService, sizeMeasureHelper, offersProcessingStatusService);

        var holder = new OfferProcessingStrategiesHolder(
            List.of(classificationStrategy, matchingStrategy, waitContentProcessingStrategy,
                waitCatalogProcessingStrategy, noSizeMeasureValueStrategy)
        );

        offersTrackerService = new OffersTrackerService(trackerServiceMock, holder);

        classificationOffersService = new ClassificationOffersService(offerRepository, offersTrackerService,
            holder, fashionClassificationCheck);
        matchingOffersService = new MatchingOffersService(offersTrackerService, offerRepository, holder);
    }

    private void mockRetrieveMappingSkuTypeService() {
        retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository);
    }

    private void mockOffersProcessingStatusService() {
        offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor,
            needContentStatusService,
            supplierService,
            categoryKnowledgeService,
            retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository,
            antiMappingRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService),
            false,
            false,
            3, categoryInfoCache);
    }

    private void mockImportCategoryKnowledge() {
        importCategoryKnowledgeService = new ImportCategoryKnowledgeService(
            offerBatchProcessor, offerStatService, modelFormServiceMock, categoryKnowledgeRepository,
            offersProcessingStatusService);
    }

    private void mockImportOffersProcessService() {
        importOffersProcessService = new ImportOffersProcessService(
            offerRepository, masterDataFor1pRepository, masterDataHelperService, offersEnrichmentService,
            transactionHelper,
            offerToMasterDataConverter,
            supplierRepository,
            applySettingsService,
            offersProcessingStatusService,
            new TransformImportOffersService(offerMappingActionService, offersProcessingStatusService,
                offerDestinationCalculator),
            Mockito.mock(GlobalVendorsCachingService.class),
            false,
            storageKeyValueService
        );
    }

    private void mockImportExcelService() {
        OffersToExcelFileConverterConfig config =
            new OffersToExcelFileConverterConfig(categoryCachingService);
        importedExcelFileConverter = config.importedExcelFileConverter(
            modelStorageCachingService,
            Mockito.mock(MboTimeUnitAliasesService.class),
            supplierRepository,
            storageKeyValueService
        );
        importExcelService = new ImportExcelService(
            importedExcelFileConverter, Mockito.mock(ExcelS3Service.class),
            importOffersProcessService);
    }

    private void mockCategoryKnowledgeService() {
        modelFormMap = new HashMap<>();
        modelFormServiceMock = categoryIds -> categoryIds.stream()
            .map(id -> modelFormMap.get(id))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(CachedModelForm::getCategoryId, Function.identity()));
        categoryKnowledgeService = new CategoryKnowledgeServiceImpl(categoryKnowledgeRepository, modelFormServiceMock);

        updateCategoryKnowledgeInRepo(OfferTestUtils.TEST_CATEGORY_INFO_ID, true);
    }

    private void mockOffersController() {
        offerProtoConverter = new OfferProtoConverter(categoryCachingService,
            offerCategoryRestrictionCalculator, mskuRepository, -1);
        offersController = new OffersController(offerRepository, null,
            Mockito.mock(BackgroundEnrichFileService.class), supplierRepository,
            mskuRepository, offerProtoConverter,
            offerMappingActionService,
            new ObjectMapper(), null,
            buyPromoPriceRepository,
            offersProcessingStatusService,
            offersWebService,
            needContentStatusService,
            null,
            processingTicketInfoService,
            new BusinessSupplierService(supplierRepository, offerRepository),
            offerBatchProcessor, offerDestinationCalculator, false,
            warehouseServiceService,
            new RecheckMappingService(
                offerRepository,
                offerMappingActionService,
                offersProcessingStatusService,
                transactionHelper,
                antiMappingService
            ));
    }

    @SuppressWarnings("unchecked")
    private void mockContentController() {
        contentController = new ContentController(
            null,
            offerRepository,
            supplierRepository,
            null,
            null,
            null,
            manualVendorService,
            offersWebService,
            null,
            processingTicketInfoService,
            masterDataFor1pRepository,
            categoryCachingService, false, categoryInfoRepository,
            offersProcessingStatusService,
            new BusinessSupplierService(supplierRepository, offerRepository), offerBatchProcessor);
    }

    private void mockMboMappingsService() {
        MboTimeUnitAliasesService timeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        MasterDataFromMdiConverter masterDataFromMdiConverter = new MasterDataFromMdiConverter(timeUnitAliasesService);
        addProductInfoHelperService = new AddProductInfoHelperService(offerRepository,
            supplierService, modelStorageCachingService,
            offerMappingActionService,
            transactionHelper,
            categoryCachingService, masterDataHelperService, masterDataFromMdiConverter,
            offersEnrichmentService, applySettingsService, offersProcessingStatusService,
            migrationService,
            removedOfferRepository,
            antiMappingRepository,
            tempImportChangeDeltaRepository,
            offerDestinationCalculator, storageKeyValueService, hashCalculator,
            recheckClassificationService ,false);
        mboMappingsService = new MboMappingsServiceImpl(offerRepository, slaveOfferRepository,
            addProductInfoHelperService,
            null,
            supplierRepository, null, modelStorageCachingService, null,
            categoryCachingService, null, 1, null,
            mboMappingsHelperService, businessSupplierService,
            supplierService, storageKeyValueService, null, migrationService,
            antiMappingRepository, antiMappingService,
            offersProcessingStatusService, contentProcessingQueue, transactionHelper, warehouseServiceService,
            dataCampConverterService, offerMappingActionService,
            new RecheckMappingService(offerRepository, offerMappingActionService, offersProcessingStatusService,
                transactionHelper, antiMappingService));
    }

    private void mockMboMappingsHelperService() {
        mboMappingsHelperService =
            new MboMappingsHelperService(supplierRepository, masterDataHelperService,
                businessSupplierService, 1);
    }

    private void mockRecheckClassificationService() {
        recheckClassificationService = new RecheckClassificationService(
                offerMappingActionService,
                categoryCachingService,
                supplierService
        );
    }

    private void mockMboCategoryService() {
        offerMappingRollbackService = new OfferMappingRollbackService(offerRepository, offerMappingHistoryRepository,
            offersProcessingStatusService);
        SaveMappingModerationService mappingModerationService = new SaveMappingModerationService(
            offerRepository, offersProcessingStatusService, supplierRepository,
            offerMappingActionService, modelStorageCachingService,
            needContentStatusService, pskuHasContentMappingsService,
            migrationService, antiMappingService, transactionHelper,
            offerMappingRollbackService, storageKeyValueService, recheckClassificationService
        );
        var saveTaskMappingsService = new SaveTaskMappingsService(
            modelStorageCachingService, offerRepository, offerMappingActionService,
            manualVendorService, offersProcessingStatusService, migrationService
        );
        var updateSupplierOfferCategoryService = new UpdateSupplierOfferCategoryService(
            offerRepository, supplierRepository, classificationOffersProcessingService, offersProcessingStatusService,
            migrationService
        );
        mboCategoryService = new MboCategoryServiceImpl(offerRepository, slaveOfferRepository,
            offersProcessingStatusService, mappingModerationService, mboMappingsHelperService, supplierRepository,
            offerProtoConverter, processingTicketInfoService, null, migrationService,
            assignmentRepository, offerClearMappingService, saveTaskMappingsService,
            updateSupplierOfferCategoryService, categoryGroupService);
    }

    private void mockReopenNeedInfoExecutor() {
        reopenNeedInfoService = new ReopenNeedInfoService(
            offerRepository,
            transactionHelper,
            reopenNeedInfoQueue::enqueueByIds,
            offersProcessingStatusService
        );
    }

    private void mockAcceptanceServices() {
        var supplierAutoApproveService = new SupplierAutoApproveServiceImpl(
            modelStorageCachingService, offerMappingActionService, antiMappingRepository
        );
        var suggestAutoApproveService = new SuggestAutoApproveServiceImpl(
            categoryInfoRepository,
            modelStorageCachingService, offerMappingActionService, antiMappingRepository
        );
        autoApproveService = new CompositeAutoApproveService(
            antiMappingRepository, supplierAutoApproveService, suggestAutoApproveService
        );
        acceptanceService = new AcceptanceService(categoryInfoRepository, categoryCachingService, supplierService,
            false, categoryRuleService, false,
            offerDestinationCalculator);
        fastSkuMappingsService = new FastSkuMappingsService(needContentStatusService);

        applySettingsService = new ApplySettingsService(supplierService,
            acceptanceService, autoApproveService, offersProcessingStatusService, fastSkuMappingsService);
    }

    protected Model model(long modelId, String title) {
        return model(modelId, title, false, Model.ModelType.SKU);
    }

    protected Model model(long modelId, String title, boolean isDeleted, Model.ModelType type) {
        return new Model().setId(modelId)
            .setPublishedOnMarket(true)
            .setPublishedOnBlueMarket(true)
            .setModelType(type)
            .setDeleted(isDeleted)
            .setSkuParentModelId(MODEL_PARENT_ID)
            .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
            .setTitle(title)
            .setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID);
    }

    protected Offer startingOffer() {
        return OfferTestUtils.simpleOffer()
            .setShopSku(SHOP_SKU)
            .setBarCode(BAR_CODE)
            .setRealization(true)
            .setVendorCode(OfferTestUtils.DEFAULT_VENDORCODE)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.builder().urls(Collections.singletonList(URL)).build())
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.simpleSupplier());
    }

    protected Offer assertOfferInRepoAndGet(BusinessSkuKey businessSkuKey) {
        Offer storedOffer = offerRepository.findOfferByBusinessSkuKey(businessSkuKey);
        Assertions.assertThat(storedOffer).isNotNull();
        return storedOffer;
    }

    // supplier imported offer via excel
    @SuppressWarnings("UnstableApiUsage")
    protected Consumer<Offer> importOfferFromExcel() {
        return offer -> {
            try {
                importExcelService.importExcel(OfferTestUtils.TEST_SUPPLIER_ID, "excel/CorrectSampleApp.xls",
                    ByteStreams.toByteArray(Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream("excel/CorrectSampleApp.xls"))),
                    s -> {
                    },
                    "test_user", new ImportFileService.ImportSettings(
                        ImportFileService.SavePolicy.ALL_OR_NOTHING));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected void updateCategoryKnowledgeInRepo(boolean hasKnowledge) {
        updateCategoryKnowledgeInRepo(OfferTestUtils.TEST_CATEGORY_INFO_ID, hasKnowledge);
    }

    protected void updateCategoryKnowledgeInRepo(long categoryId, boolean hasKnowledge) {
        // setting up category knowledge
        if (!hasKnowledge) {
            modelFormMap.remove(categoryId);
        } else {
            modelFormMap.put(categoryId,
                new CachedModelForm(categoryId, true));
        }
        categoryKnowledgeRepository.insertOrUpdate(new CategoryKnowledge(categoryId)
            .setHasKnowledge(hasKnowledge));
        categoryCachingService.getCategory(categoryId)
            .ifPresent(c -> c.setHasKnowledge(hasKnowledge));
    }

    protected <T> Consumer<T> updateCategoryManualAcceptanceInRepo(boolean manualAcceptance) {
        return ignored -> {
            CategoryInfo categoryInfo = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setManualAcceptance(manualAcceptance);
            var acceptanceMode = manualAcceptance ? CategoryInfo.AcceptanceMode.MANUAL :
                CategoryInfo.AcceptanceMode.AUTO_ACCEPT;
            categoryInfo.setDsbsAcceptanceMode(acceptanceMode);
            categoryInfoRepository.update(categoryInfo);
        };
    }

    // catman uses UI to accept or reject offer
    protected Consumer<Offer> catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus acceptanceStatus,
                                                            boolean hasKnowledge) {
        return catmanUpdatesAcceptanceStatus(acceptanceStatus, OfferTestUtils.simpleSupplier(), hasKnowledge);
    }

    protected Consumer<Offer> catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus acceptanceStatus,
                                                            Supplier supplier,
                                                            boolean hasKnowledge) {
        return offer -> {
            updateCategoryKnowledgeInRepo(hasKnowledge);
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());
            offersController.updateAcceptance(Collections.singletonList(
                new OffersController.AcceptanceUpdate()
                    .setOfferId(offerInRepo.getId())
                    .setSupplierId(supplier.getId())
                    .setNewStatus(acceptanceStatus)));
        };
    }

    protected Consumer<Offer> catmanResetsOfferStatus(int supplierId) {
        return offer -> {
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());
            SimpleUpdateRequest request = new SimpleUpdateRequest();
            request.setSupplierId(supplierId)
                .setOfferIds(List.of(offerInRepo.getId()));
            offersController.reset(request);
        };
    }

    //  tms task updates category knowledge
    protected <T> Consumer<T> tmsImportCategoryKnowledge(boolean hasKnowledge) {
        return ignored -> {
            offerStatService.updateOfferStat(); // required for category stat
            updateCategoryKnowledgeInRepo(hasKnowledge);
            task(importCategoryKnowledgeService::execute)
                .accept(ignored);
        };
    }

    //  tms task updates need_info
    protected <T> Consumer<T> tmsReopenNeedInfoOffers() {
        return task(this::executeReopen);
    }

    private void executeReopen() {
        reopenNeedInfoQueue.handleQueueBatch(reopenNeedInfoService::processChunkFromQueue);
    }

    protected <T> Consumer<T> tmsProcessClassificationTicketsResults() {
        return task(offersTrackerService::processClassificationTickets);
    }

    protected <T> Consumer<T> tmsProcessMatchingTicketsResults() {
        return task(offersTrackerService::processMatchingTickets);
    }

    protected <T> Consumer<T> tmsCreateTrackerTickets() {
        return task(() -> {
            createMatchingTicketsQueueService.handleQueueBatch(
                matchingOffersService::findAndMarkOffersToMatching);
            createClassificationTicketsQueueService.handleQueueBatch(
                classificationOffersService::findAndMarkOffersToInClassification);
        });
    }

    protected <T> Consumer<T> task(Runnable action) {
        return ignored -> {
            try {
                action.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    // supplier changes offer with NEED_INFO status
    protected Consumer<Offer> supplierUpdatesOfferInformation(long marketSkuId) {
        return offer -> {
            MboMappings.ProviderProductInfoResponse response = mboMappingsService.addProductInfo(
                MboMappings.ProviderProductInfoRequest.newBuilder()
                    .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                        .setShopSkuId(SHOP_SKU)
                        .setShopId(OfferTestUtils.TEST_SUPPLIER_ID)
                        .setDescription(DESCRIPTION)
                        .setMarketSkuId(marketSkuId)
                        .setMappingType(MboMappings.MappingType.SUPPLIER)
                        .build()).build());
            Assertions.assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);
            // have to hack time here to not wait 15 minutes
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());
            offerRepository.updateOffer(offerInRepo
                .setProcessingStatusModifiedInternal(offerInRepo
                    .getProcessingStatusModified()
                    .minus(ReopenNeedInfoService.SEND_IF_NO_ACTIVITY))
                .setContentChangedTs(offerInRepo
                    .getContentChangedTs()
                    .minus(ReopenNeedInfoService.SEND_IF_NO_ACTIVITY)));
        };
    }

    // supplier sets mapping in partner interface
    protected Consumer<Offer> supplierSendsNewMapping(long marketSkuId) {
        return offer -> {
            MboMappings.ProductUpdateRequestInfo.Builder requestInfo = MboMappings.ProductUpdateRequestInfo.newBuilder()
                .setChangeSource(ChangeSource.SUPPLIER);
            MboMappings.ProviderProductInfoResponse response = mboMappingsService.addProductInfo(
                MboMappings.ProviderProductInfoRequest.newBuilder()
                    .setRequestInfo(requestInfo)
                    .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                        .setShopId(OfferTestUtils.TEST_SUPPLIER_ID)
                        .setShopSkuId(SHOP_SKU)
                        .setMarketSkuId(marketSkuId)
                        .setMappingType(MboMappings.MappingType.SUPPLIER).build())
                    .build());
            Assertions.assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);
        };
    }

    //  supplier does not know mapping
    protected Consumer<Offer> supplierDoesNotKnowMapping() {
        return offer -> {
            MboMappings.UpdateOfferProcessingStatusResponse response = mboMappingsService.updateOfferProcessingStatus(
                MboMappings.UpdateOfferProcessingStatusRequest.newBuilder()
                    .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT)
                    .addMappings(MboMappings.UpdateOfferProcessingStatusRequest.Locator.newBuilder()
                        .setShopSku(SHOP_SKU)
                        .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID))
                    .build());
            Assertions.assertThat(response.getStatus())
                .isEqualTo(MboMappings.UpdateOfferProcessingStatusResponse.Status.OK);
        };
    }

    protected Consumer<Offer> offerGetsConfidentCategoryFromUc(long categoryId) {
        return offer -> {
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());
            offerInRepo.setCategoryIdForTests(categoryId, Offer.BindingKind.APPROVED);
            offerInRepo.setClassifierCategoryId(categoryId,
                OfferCategoryRestrictionCalculator.DEFAULT_CLASSIFIER_TRUST_THRESHOLD);
            offerRepository.updateOffer(offerInRepo);
        };
    }

    protected Consumer<Offer> operatorClassifiesOfferByHandle(ContentCommentType commentType, String... items) {
        return offer -> {
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());

            SupplierOffer.ClassificationTaskResult.Builder results =
                SupplierOffer.ClassificationTaskResult.newBuilder()
                    .setOfferId(String.valueOf(offerInRepo.getId()))
                    .setStaffLogin(YANG_OPERATOR_LOGIN);
            if (commentType != null) {
                results.addContentComment(SupplierOffer.ContentComment.newBuilder()
                    .setType(commentType.name())
                    .addAllItems(List.of(items)));
            }
            MboCategory.UpdateSupplierOfferCategoryResponse response = mboCategoryService.updateSupplierOfferCategory(
                MboCategory.UpdateSupplierOfferCategoryRequest.newBuilder()
                    .addResult(results)
                    .build());
            Assertions.assertThat(response.getResult().getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);
        };
    }

    protected Consumer<Offer> operatorClassifiesOfferByFile(ContentCommentType commentType, String items) {
        return offer -> {
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());
            Issue ticket = trackerServiceMock.getTicket(offerInRepo.getTrackerTicket());
            Assertions.assertThat(ticket).isNotNull();
            Assertions.assertThat(ticket.getTags())
                .contains(trackerServiceMock.getTicketTag(TicketType.CLASSIFICATION));

            ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
            excelFile.setValue(1, ExcelHeaders.CONTENT_COMMENT_TYPE1.getTitle(), commentType.getDescription());
            excelFile.setValue(1, ExcelHeaders.CONTENT_COMMENT_ITEMS1.getTitle(), items);

            trackerServiceMock.commentWithAttachment(ticket, excelFile.build());
        };
    }

    protected Consumer<Offer> operatorClassifiesOfferByFile(long categoryId) {
        return offer -> {
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());
            Issue ticket = trackerServiceMock.getTicket(offerInRepo.getTrackerTicket());
            Assertions.assertThat(ticket).isNotNull();
            Assertions.assertThat(ticket.getTags())
                .contains(trackerServiceMock.getTicketTag(TicketType.CLASSIFICATION));

            ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
            excelFile.setValue(1, ExcelHeaders.FIXED_CATEGORY_ID.getTitle(), categoryId);

            trackerServiceMock.commentWithAttachment(ticket, excelFile.build());
        };
    }

    protected Consumer<Offer> operatorMatchesOffer(long skuId) {
        return offer -> {
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());
            Issue ticket = trackerServiceMock.getTicket(offerInRepo.getTrackerTicket());
            Assertions.assertThat(ticket.getTags())
                .contains(trackerServiceMock.getTicketTag(TicketType.MATCHING));

            ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
            excelFile.setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), skuId);

            trackerServiceMock.commentWithAttachment(ticket, excelFile.build());
        };
    }

    // operator accepts or rejects mapping
    protected Consumer<Offer> operatorModeratesMapping(SupplierMappingModerationResult result,
                                                       long marketSkuId) {
        return operatorModeratesMapping(result, marketSkuId, null);
    }

    protected Consumer<Offer> operatorModeratesMapping(SupplierMappingModerationResult result,
                                                       long marketSkuId,
                                                       ContentCommentType commentType) {
        return offer -> {
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());
            SupplierOffer.MappingModerationTaskResult.Builder results =
                SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setStatus(result)
                    .setOfferId(Long.toString(offer.getId()))
                    .setMarketSkuId(marketSkuId)
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setShopSkuId(SHOP_SKU)
                    .setOfferId(String.valueOf(offerInRepo.getId()))
                    .setStaffLogin(YANG_OPERATOR_LOGIN);
            if (commentType != null) {
                results.addContentComment(SupplierOffer.ContentComment.newBuilder()
                    .setType(commentType.name()));
            }
            MboCategory.SaveMappingModerationResponse response = mboCategoryService.saveMappingsModeration(
                MboCategory.SaveMappingsModerationRequest.newBuilder()
                    .addResults(results)
                    .build());
            Assertions.assertThat(response.getResult().getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);
        };
    }

    // operator forces offer classification
    protected Consumer<Offer> operatorForcesOfferClassification() {
        return offer -> {
            Offer offerInRepo = assertOfferInRepoAndGet(offer.getBusinessSkuKey());
            contentController.forceToClassification(
                new ContentController.ForceToClassificationRequest()
                    .setSupplierId(offerInRepo.getBusinessId())
                    .setOfferIds(Collections.singletonList(offerInRepo.getId())));
        };
    }

    protected Consumer<Offer> irUpdatesContentProcessingTask(long contentProcessingTask,
                                                             ContentProcessingTask.State state) {
        return offer -> {
            MboMappings.UpdateContentProcessingTasksResponse response =
                mboMappingsService.updateContentProcessingTasks(
                    MboMappings.UpdateContentProcessingTasksRequest.newBuilder()
                        .addContentProcessingTask(
                            ContentProcessingTask.newBuilder()
                                .setShopSku(SHOP_SKU)
                                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                                .setContentProcessingTaskId(contentProcessingTask)
                                .setContentProcessingState(state)
                                .build())
                        .build());
            Assertions.assertThat(response.getStatus())
                .isEqualTo(MboMappings.UpdateContentProcessingTasksResponse.Status.OK);
        };
    }

    // ir sets mapping via handle
    protected Consumer<Offer> irSendsNewMapping(long marketSkuId, String login) {
        return offer -> {
            MboMappings.ProductUpdateRequestInfo.Builder requestInfo = MboMappings.ProductUpdateRequestInfo.newBuilder()
                .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.AUTO)
                .setVerifyNoApprovedMapping(true)
                .setUserLogin(login);
            MboMappings.ProviderProductInfoResponse response = mboMappingsService.addProductInfo(
                MboMappings.ProviderProductInfoRequest.newBuilder()
                    .setRequestInfo(requestInfo)
                    .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                        .setShopId(OfferTestUtils.TEST_SUPPLIER_ID)
                        .setShopSkuId(SHOP_SKU)
                        .setMarketSkuId(marketSkuId)
                        .setMappingType(MboMappings.MappingType.SUPPLIER).build())
                    .build());
            Assertions.assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);
        };
    }

    protected Consumer<Offer> irPPPRemapsOffer(long marketSkuId, String login,
                                               MboMappings.ProductUpdateRequestInfo.ChangeType changeType) {
        return offer -> {
            MboMappings.ProductUpdateRequestInfo.Builder requestInfo = MboMappings.ProductUpdateRequestInfo
                .newBuilder()
                .setChangeSource(ChangeSource.CONTENT)
                .setChangeType(changeType)
                .setUserLogin(login);
            MboMappings.UpdateMappingsRequest request = MboMappings.UpdateMappingsRequest.newBuilder()
                .addUpdates(MboMappings.UpdateMappingsRequest.MappingUpdate.newBuilder()
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setShopSku(SHOP_SKU)
                    .setMarketSkuId(marketSkuId))
                .setRequestInfo(requestInfo)
                .build();
            MboMappings.ProviderProductInfoResponse response =
                mboMappingsService.updateMappings(request);
            Assertions.assertThat(response.getStatus())
                .isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);
        };
    }

    @SuppressWarnings("ConstantConditions")
    protected BiConsumer<String, Offer> offerInRepoIsValid() {
        return (description, offer) -> {
            //ignore id field
            Offer offerByBusinessSkuKey =
                offerRepository.findOfferByBusinessSkuKeyWithContent(offer.getBusinessSkuKey());
            offer.storeOfferContent(
                OfferContent.copyToBuilder(offer.extractOfferContent()).id(offerByBusinessSkuKey.getId()).build());

            offerByBusinessSkuKey.setServiceOffers(offerByBusinessSkuKey.getServiceOffers().stream()
                .sorted(Comparator.comparing(Offer.ServiceOffer::getSupplierId))
                .collect(Collectors.toList()));
            offer.setServiceOffers(offer.getServiceOffers().stream()
                .sorted(Comparator.comparing(Offer.ServiceOffer::getSupplierId))
                .collect(Collectors.toList()));

            Assertions.assertThat(offerByBusinessSkuKey)
                .as("step: %s", description)
                .usingComparatorForFields((o1, o2) -> {
                        // just checking that both are set
                        return o1 != null && o2 != null ||
                            o1 == null && o2 == null ? 0 : 1;
                    }, "contentChangedTs",
                    "processingStatusModified", "contentProcessingStatusModified", "acceptanceStatusModified",
                    "created", "updated", "supplierSkuMappingCheckTs", "smLastExecutionTs")
                .usingComparatorForFields((o1, o2) -> {
                        if (o1 == null && o2 != null || o1 != null && o2 == null) {
                            return 1;
                        } else if (o1 == null && o2 == null) {
                            return 0;
                        }
                        Offer.Mapping map1 = (Offer.Mapping) o1;
                        Offer.Mapping map2 = (Offer.Mapping) o2;
                        // just checking that both are set
                        int compareDates = map1.getTimestamp() != null && map2.getTimestamp() != null ||
                            map1.getTimestamp() == null && map2.getTimestamp() == null ? 0 : 1;
                        // bit OR because used only in equals
                        return Long.compare(map1.getMappingId(), map2.getMappingId()) | compareDates;
                    }, "suggestSkuMapping", "supplierSkuMapping",
                    "approvedSkuMapping", "deletedApprovedSkuMapping", "contentSkuMapping",
                    "approvedSkuMapping", "gutginSkuMapping", "autoSkuMapping", "smSkuMapping",
                    "recheckSkuMapping")
                .isEqualToIgnoringGivenFields(offer,
                    "id", "lastVersion", "createdByLogin", "modifiedByLogin", "mappingModifiedBy",
                    "trackerTicket", "processingTicketId", "marketSpecificContentHash", "marketSpecificContentHashSent",
                    "isOfferContentPresent", "ticketCritical", "contentProcessed");
        };
    }
}
