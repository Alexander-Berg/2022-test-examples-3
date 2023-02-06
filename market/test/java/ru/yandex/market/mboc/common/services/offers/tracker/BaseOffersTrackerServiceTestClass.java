package ru.yandex.market.mboc.common.services.offers.tracker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.AutoClassificationResult;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.modelform.CachedModelForm;
import ru.yandex.market.mboc.common.modelform.ModelFormCachingService;
import ru.yandex.market.mboc.common.offers.ClassifierOffer;
import ru.yandex.market.mboc.common.offers.MatchingOffer;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleService;
import ru.yandex.market.mboc.common.offers.acceptance.service.AcceptanceService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepository;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoServiceForTesting;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceImpl;
import ru.yandex.market.mboc.common.services.category_manager.CategoryManagerServiceImpl;
import ru.yandex.market.mboc.common.services.category_manager.repository.CategoryManagerRepository;
import ru.yandex.market.mboc.common.services.category_manager.repository.CatteamRepository;
import ru.yandex.market.mboc.common.services.converter.OffersExcelFileConverter;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.managers.ManagersService;
import ru.yandex.market.mboc.common.services.managers.ManagersServiceImpl;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceImpl;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.OfferProcessingStrategiesHolder;
import ru.yandex.market.mboc.common.services.offers.auto_approves.CompositeAutoApproveService;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SuggestAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SupplierAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersProcessingService;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.MatchingOffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedSizeMeasureFilter;
import ru.yandex.market.mboc.common.services.offers.processing.NoSizeMeasureValueStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.ProcessingTicketHelper;
import ru.yandex.market.mboc.common.services.offers.processing.ReSortOffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.ReclassificationProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.WaitCatalogProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.WaitContentProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.queue.OfferQueueService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.proto.SizeMeasureHelper;
import ru.yandex.market.mboc.common.services.users.StaffService;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.services.users.UserCachingService;
import ru.yandex.market.mboc.common.services.users.UserCachingServiceImpl;
import ru.yandex.market.mboc.common.users.UserRepositoryMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.CategorySizeMeasureServiceStub;
import ru.yandex.market.mboc.common.utils.SecurityContextAuthenticationHelper;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingServiceMock;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public abstract class BaseOffersTrackerServiceTestClass extends BaseDbTestClass {

    public static final String TEST_USER = "test-user";
    protected static final String AUTHOR = "test-author";
    @Autowired
    protected OfferRepository offerRepository;
    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    protected SupplierRepository supplierRepository;
    @Autowired
    protected MboUsersRepository mboUsersRepository;
    @Autowired
    protected CategoryInfoRepository categoryInfoRepository;
    @Autowired
    protected CatteamRepository catteamRepository;
    @Autowired
    protected MboAuditServiceMock auditServiceMock;
    @Autowired
    protected CategoryManagerRepository categoryManagerRepository;
    @Autowired
    protected AntiMappingRepository antiMappingRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private CategoryRuleService categoryRuleService;
    @Autowired
    protected TransactionHelper transactionHelper;
    protected CompositeAutoApproveService autoApproveService;
    protected AcceptanceService acceptanceService;
    protected ApplySettingsService applySettingsService;
    protected OffersProcessingStatusService offersProcessingStatusService;
    protected ModelStorageCachingService modelStorageCachingService;
    protected TrackerServiceMock trackerServiceMock;
    protected OffersExcelFileConverter<MatchingOffer> matchingConverter;
    protected OffersExcelFileConverter<ClassifierOffer> classifierConverter;
    protected OffersExcelFileConverter<ClassifierOffer> reclassificationConverter;
    protected OffersExcelFileConverter<Offer> reSortConverter;
    protected CategoryCachingServiceMock categoryCachingService;
    protected OfferMappingActionService offerMappingActionService;
    protected ClassificationOffersProcessingStrategy classificationOffersProcessingStrategy;
    protected MatchingOffersProcessingStrategy matchingOffersProcessingStrategy;
    protected ReSortOffersProcessingStrategy reSortOffersProcessingStrategy;
    protected ReclassificationProcessingStrategy reclassificationProcessingStrategy;
    protected WaitContentProcessingStrategy waitContentProcessingStrategy;
    protected NoSizeMeasureValueStrategy noSizeMeasureValueStrategy;
    protected WaitCatalogProcessingStrategy waitCatalogProcessingStrategy;
    protected OffersTrackerService offersTrackerService;
    protected ManagersService managersService;
    protected CategoryKnowledgeServiceImpl categoryKnowledgeService;
    protected CategoryKnowledgeRepositoryMock categoryKnowledgeRepositoryMock;
    protected ModelFormCachingService modelFormService;
    protected MasterDataHelperService masterDataHelperService;
    protected SupplierService supplierService;
    @Autowired
    protected ProcessingTicketInfoRepository processingTicketInfoRepository;
    protected ProcessingTicketInfoServiceForTesting processingTicketInfoService;
    private CategoryManagerServiceImpl categoryManagerService;
    private ClassificationOffersProcessingService classificationOffersProcessingService;
    private StaffService staffService;
    private NeedContentStatusService needContentStatusService;
    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private CategorySizeMeasureService categorySizeMeasureService;
    private GlobalVendorsCachingService globalVendorsCachingService;
    private SizeMeasureHelper sizeMeasureHelper;
    private NeedSizeMeasureFilter filter;

    protected Supplier supplier1 = new Supplier(1, "Supplier 1", "", "ooo romashka");
    protected Supplier supplier2 = new Supplier(2, "Supplier 2", "", "");
    protected Supplier supplier3 = new Supplier(3, "Supplier 3", "", "");

    private static Map<Long, Model> buildResponse(Collection<Long> modelIds) {
        if (modelIds == null) {
            return Collections.emptyMap();
        }
        HashMap<Long, Model> result = new HashMap<>();
        for (Long modelId : modelIds) {
            if (modelId == null || modelId <= 0L) {
                continue;
            }
            result.put(modelId, new Model()
                .setId(modelId)
                .setCategoryId(13)
                .setVendorId(130)
                .setTitle("Test")
                .setSkuParentModelId(123)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.OPERATOR)
                .setPublishedOnBlueMarket(true)
            );
        }
        return result;
    }

    public static void setExcelFileSku(ExcelFile.Builder excelFile, int line, @Nonnull Object id) {
        excelFile.setValue(line, "Market_sku_id", id);
    }

    public static void setFixedCategoryId(ExcelFile.Builder excelFile, int line, long fixedCategoryId) {
        excelFile.setValue(line, "Исправленный category_id", fixedCategoryId);
    }

    public static void setExcelFileComment(ExcelFile.Builder excelFile, int line, @Nonnull String comment,
                                           @Nullable ContentCommentType type) {
        excelFile.setValue(line, ExcelHeaders.CONTENT_COMMENT_TYPE1.getTitle(),
            type != null ? type.getDescription() : "");
        excelFile.setValue(line, ExcelHeaders.CONTENT_COMMENT_ITEMS1.getTitle(), comment);
    }

    protected static Issue createTicket(
        OfferQueueService offerQueueService,
        Function<List<Long>, OffersProcessingStrategy.OptionalTicket> ticketFunction
    ) {
        AtomicReference<Issue> ticketReference = new AtomicReference<>();
        offerQueueService.handleQueueBatch(offerIds -> ticketReference.set(
            ticketFunction.apply(offerIds).getTicket()
        ));
        return ticketReference.get();
    }

    @Before
    public void setUp() throws Exception {
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        modelStorageCachingService = Mockito.mock(ModelStorageCachingServiceImpl.class);
        Mockito.when(modelStorageCachingService.getModelsFromMboThenPg(anyCollection()))
            .thenAnswer(call -> buildResponse(call.getArgument(0)));
        Mockito.when(modelStorageCachingService.getModelsFromPgThenMbo(any(), any(), anyBoolean()))
            .thenAnswer(call -> buildResponse(call.getArgument(0)));

        OfferCategoryRestrictionCalculator offerCategoryRestrictionCalculator =
            Mockito.mock(OfferCategoryRestrictionCalculator.class);
        HonestMarkClassificationService honestMarkClassificationService =
            Mockito.mock(HonestMarkClassificationService.class);

        staffService = new StaffServiceMock();
        SecurityContextAuthenticationHelper.setAuthenticationToken(TEST_USER);

        auditServiceMock.reset();

        managersService = new ManagersServiceImpl(mboUsersRepository, categoryInfoRepository);
        categoryCachingService = new CategoryCachingServiceMock();
        UserCachingService userCachingService = new UserCachingServiceImpl(new UserRepositoryMock());
        categoryManagerService = new CategoryManagerServiceImpl(categoryCachingService, categoryManagerRepository,
            userCachingService, transactionHelper, staffService, namedParameterJdbcTemplate, categoryInfoRepository,
            catteamRepository);
        supplierService = new SupplierService(supplierRepository);

        needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            offerCategoryRestrictionCalculator, offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        OffersToExcelFileConverterConfig converterConfig =
            new OffersToExcelFileConverterConfig(categoryCachingService);
        classifierConverter = converterConfig.classifierConverter(categoryCachingService);
        reclassificationConverter = converterConfig.reclassificationConverter(
            categoryCachingService, supplierRepository);
        matchingConverter = converterConfig.matchingConverter(modelStorageCachingService, offerRepository);
        reSortConverter = converterConfig.reSortConverter(supplierRepository);

        categoryKnowledgeRepositoryMock = new CategoryKnowledgeRepositoryMock();
        modelFormService = Mockito.mock(ModelFormCachingService.class);
        Mockito.when(modelFormService.getPublishedModelForms(Mockito.anyCollection()))
            .then(i -> {
                Collection<Long> categoryIds = i.getArgument(0);
                return categoryIds.stream().distinct()
                    .map(id -> new CachedModelForm(id, true))
                    .collect(Collectors.toMap(CachedModelForm::getCategoryId, Function.identity()));
            });
        categoryKnowledgeService = new CategoryKnowledgeServiceImpl(categoryKnowledgeRepositoryMock,
            modelFormService);

        RetrieveMappingSkuTypeService retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository);

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


        var suggestAutoApproveService = new SuggestAutoApproveServiceImpl(
            categoryInfoRepository, modelStorageCachingService, offerMappingActionService, antiMappingRepository
        );
        var supplierAutoApproveService = new SupplierAutoApproveServiceImpl(
            modelStorageCachingService, offerMappingActionService, antiMappingRepository);

        autoApproveService = new CompositeAutoApproveService(
            antiMappingRepository, supplierAutoApproveService, suggestAutoApproveService);
        acceptanceService = new AcceptanceService(categoryInfoRepository, categoryCachingService, supplierService,
            false, categoryRuleService, false, offerDestinationCalculator);
        var fastSkuMappingsService = new FastSkuMappingsService(needContentStatusService);
        applySettingsService = new ApplySettingsService(
            supplierService, acceptanceService, autoApproveService,
            offersProcessingStatusService, fastSkuMappingsService);
        classificationOffersProcessingService = new ClassificationOffersProcessingService(
            categoryCachingService,
            offerMappingActionService,
            offerDestinationCalculator
        );
        filter = Mockito.mock(NeedSizeMeasureFilter.class);
        Mockito.when(filter.createNeedSizeMeasureTickets(anyCollection()))
            .thenAnswer(invocation -> invocation.getArguments()[0]);
        trackerServiceMock = new TrackerServiceMock();
        processingTicketInfoService = new ProcessingTicketInfoServiceForTesting(processingTicketInfoRepository);
        ProcessingTicketHelper helper = new ProcessingTicketHelper("unit-tests", trackerServiceMock,
            managersService, categoryCachingService, processingTicketInfoService);

        Mockito.when(honestMarkClassificationService.getClassificationResult(
            Mockito.any(Offer.class),
            Mockito.anyLong(),
            Mockito.any(),
            Mockito.anySet(),
            Mockito.anySet())
        )
            .thenReturn(new AutoClassificationResult(ClassificationResult.CONFIDENT, null, true));
        needContentStatusService = Mockito.mock(NeedContentStatusService.class);
        classificationOffersProcessingStrategy = new ClassificationOffersProcessingStrategy(trackerServiceMock,
            offerRepository, supplierRepository, masterDataHelperService,
            classifierConverter, categoryKnowledgeService, classificationOffersProcessingService, helper,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            honestMarkClassificationService, needContentStatusService,
            applySettingsService, offersProcessingStatusService,
            false);
        matchingOffersProcessingStrategy = new MatchingOffersProcessingStrategy(trackerServiceMock,
            offerRepository, supplierRepository, masterDataHelperService,
            managersService, offerMappingActionService,
            matchingConverter, filter, helper, applySettingsService, offersProcessingStatusService);
        reSortOffersProcessingStrategy = new ReSortOffersProcessingStrategy(trackerServiceMock,
            offerRepository, masterDataHelperService,
            reSortConverter, categoryManagerService, modelStorageCachingService, "test", offersProcessingStatusService);
        reclassificationProcessingStrategy = new ReclassificationProcessingStrategy(trackerServiceMock,
            offerRepository, supplierRepository, masterDataHelperService,
            reclassificationConverter, categoryKnowledgeService, classificationOffersProcessingService, helper,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            honestMarkClassificationService, needContentStatusService,
            applySettingsService, offersProcessingStatusService,
            false);
        waitContentProcessingStrategy = new WaitContentProcessingStrategy(trackerServiceMock, offerRepository,
            supplierRepository, categoryInfoRepository, mboUsersRepository, categoryCachingService, staffService,
            offersProcessingStatusService);

        categorySizeMeasureService = new CategorySizeMeasureServiceStub();
        sizeMeasureHelper = new SizeMeasureHelper(categorySizeMeasureService, categorySizeMeasureService);

        globalVendorsCachingService = new GlobalVendorsCachingServiceMock();
        noSizeMeasureValueStrategy = new NoSizeMeasureValueStrategy(offerRepository, trackerServiceMock,
            supplierRepository, globalVendorsCachingService, sizeMeasureHelper, offersProcessingStatusService);

        waitCatalogProcessingStrategy = new WaitCatalogProcessingStrategy(trackerServiceMock, offerRepository,
            supplierRepository, categoryInfoRepository, mboUsersRepository, categoryCachingService, staffService,
            offersProcessingStatusService);

        OfferProcessingStrategiesHolder holder = new OfferProcessingStrategiesHolder(Arrays.asList(
            classificationOffersProcessingStrategy,
            matchingOffersProcessingStrategy,
            reSortOffersProcessingStrategy,
            reclassificationProcessingStrategy,
            waitContentProcessingStrategy,
            noSizeMeasureValueStrategy,
            waitCatalogProcessingStrategy));

        offersTrackerService = new OffersTrackerService(trackerServiceMock, holder);

        supplierRepository.insertBatch(supplier1, supplier2, supplier3);
    }

    protected void checkProcessingTicketCreated(Issue issue, Collection<Offer> offersFromDb) {
        Offer firstOffer = offersFromDb.iterator().next();
        Integer processingTicketId = firstOffer.getProcessingTicketId();
        Assertions.assertThat(processingTicketId).isNotNull();
        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getProcessingTicketId)
            .allMatch(pId -> processingTicketId.equals(pId));

        ProcessingTicketInfo processingTicketInfo = processingTicketInfoService.getById(processingTicketId);
        OfferProcessingStatus offerProcessingStatus = OfferProcessingStatus.valueOf(
            firstOffer.getProcessingStatus().name());
        Assertions.assertThat(processingTicketInfo).isNotNull();
        Assertions.assertThat(processingTicketInfo).extracting(
            ProcessingTicketInfo::getTitle,
            ProcessingTicketInfo::getDeadline,
            ProcessingTicketInfo::getTotalOffers,
            ProcessingTicketInfo::getStuckOffers,
            ProcessingTicketInfo::getOfferBaseStatus,
            ProcessingTicketInfo::getCritical,
            ProcessingTicketInfo::getSupplierId,
            ProcessingTicketInfo::getCompleted
        ).containsExactly(issue.getKey(), null, offersFromDb.size(),
            0, offerProcessingStatus, false, firstOffer.getBusinessId(), null);
        Assertions.assertThat(processingTicketInfo.getCreated()).isNotNull();
        Assertions.assertThat(processingTicketInfoService.convertActiveCounts(processingTicketInfo))
            .isEqualTo(ProcessingTicketInfoService.countByCategory(offersFromDb));
        Assertions.assertThat(processingTicketInfoService.convertTotalCounts(processingTicketInfo))
            .isEqualTo(ProcessingTicketInfoService.countByCategory(offersFromDb));
    }

    @After
    public void tearDown() throws Exception {
        SecurityContextAuthenticationHelper.clearAuthenticationToken();
    }
}
