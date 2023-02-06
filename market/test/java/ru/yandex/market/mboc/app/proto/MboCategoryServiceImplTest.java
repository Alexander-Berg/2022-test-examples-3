package ru.yandex.market.mboc.app.proto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mboc.app.offers.OfferProtoConverter;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.categorygroups.CategoryGroupService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferTarget;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.ManualVendorService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentScope;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepositoryMock;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoServiceForTesting;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.UpdateSupplierOfferCategoryService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.mapping.SaveMappingModerationService;
import ru.yandex.market.mboc.common.services.offers.mapping.SaveTaskMappingsService;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersProcessingService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.proto.MboMappingsHelperService;
import ru.yandex.market.mboc.common.services.sku.OfferClearMappingService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategory.ForceOfferCategoryRequest;
import ru.yandex.market.mboc.http.MboCategory.ForceOfferReclassificationRequest;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.ContentTaskResult;
import ru.yandex.market.mboc.http.SupplierOffer.OperationStatus;
import ru.yandex.market.mboc.http.SupplierOffer.SupplierOfferMappingStatus;
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicNumber")
public class MboCategoryServiceImplTest extends BaseMbocAppTest {
    private static final int BERU_ID = 100500;
    private static final int MOCK_MARKET_SKU_ID = 12345;
    private static final long CATEGORY_ID = 123L;
    private static final long CATEGORY_ID_1 = 125L;
    private static final int SUPPLIER_ID = 44;

    @Autowired
    private OfferRepository offerRepository;
    private OfferRepository repository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;

    private MboCategoryServiceImpl mboCategoryService;
    private OfferMappingActionService offerMappingActionService;
    private CategoryCachingServiceMock categoryCachingService;
    private MboMappingsHelperService mboMappingsHelperService;
    private ManualVendorService manualVendorService;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;
    private OfferProtoConverter offerProtoConverter;
    private ProcessingTicketInfoServiceForTesting processingTicketInfoService;
    private BusinessSupplierService businessSupplierService;
    private MskuRepository mskuRepository;
    private MigrationService migrationService;
    private SaveMappingModerationService mappingModerationServiceMock;
    private NeedContentStatusService needContentStatusService;
    private CategoryKnowledgeServiceMock categoryKnowledgeServiceMock;
    private OffersProcessingStatusService offersProcessingStatusService;
    private ClassificationOffersProcessingService classificationOffersProcessingService;
    private CategoryInfoRepository categoryInfoRepository;
    private OfferClearMappingService offerClearMappingService;

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    protected MigrationOfferRepository migrationOfferRepository;
    @Autowired
    protected MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    @Autowired
    protected OfferUpdateSequenceService offerUpdateSequenceService;
    @Autowired
    protected OfferProcessingAssignmentRepository assignmentRepository;
    @Autowired
    protected CategoryGroupService categoryGroupService;

    @Before
    public void setup() {
        supplierRepository.insertBatch(
            Stream.of(42, 43, 44, 45, 50, 99, 100, 200)
                .map(id -> OfferTestUtils.simpleSupplier().setId(id))
                .collect(Collectors.toList())
        );

        migrationService = new MigrationService(migrationStatusRepository,
            migrationOfferRepository, migrationRemovedOfferRepository,
            supplierRepository, offerUpdateSequenceService, offerMetaRepository);
        repository = Mockito.spy(offerRepository);
        repository.insertOffers(YamlTestUtil.readOffersFromResources("offers/offers-for-priority.json"));
        modelStorageCachingServiceMock = Mockito.spy(new ModelStorageCachingServiceMock())
            .addModel(new Model()
                .setId(MOCK_MARKET_SKU_ID)
                .setCategoryId(CATEGORY_ID)
                .setModelType(Model.ModelType.SKU)
                .setTitle("Test title")
                .setPublishedOnBlueMarket(true));

        categoryInfoRepository = Mockito.mock(CategoryInfoRepository.class);
        mappingModerationServiceMock = Mockito.mock(SaveMappingModerationService.class);

        categoryCachingService = new CategoryCachingServiceMock();
        categoryCachingService.addCategory(new Category()
            .setCategoryId(1L)
            .setHasKnowledge(true)
            .setAcceptGoodContent(true)
        );

        SupplierService supplierService = new SupplierService(supplierRepository);
        needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = Mockito.spy(new OfferMappingActionService(legacyOfferMappingActionService));

        categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        categoryKnowledgeServiceMock.addCategory(1L);

        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingServiceMock, offerBatchProcessor, supplierRepository);

        offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor,
            needContentStatusService,
            supplierService,
            categoryKnowledgeServiceMock,
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

        classificationOffersProcessingService = new ClassificationOffersProcessingService(
            categoryCachingService,
            offerMappingActionService,
            offerDestinationCalculator
        );
        businessSupplierService = new BusinessSupplierService(supplierRepository, repository);
        mboMappingsHelperService = new MboMappingsHelperService(supplierRepository, null,
            businessSupplierService, BERU_ID);
        manualVendorService = new ManualVendorService(offerMappingActionService);
        offerProtoConverter = new OfferProtoConverter(categoryCachingService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), null, BERU_ID);
        processingTicketInfoService = new ProcessingTicketInfoServiceForTesting(
            new ProcessingTicketInfoRepositoryMock());
        mskuRepository = Mockito.mock(MskuRepository.class);
        offerClearMappingService = mock(OfferClearMappingService.class);
        var saveTaskMappingsService = new SaveTaskMappingsService(
            modelStorageCachingServiceMock, repository, offerMappingActionService,
            manualVendorService, offersProcessingStatusService, migrationService
        );
        var updateSupplierOfferCategoryService = new UpdateSupplierOfferCategoryService(
            repository, supplierRepository, classificationOffersProcessingService, offersProcessingStatusService,
            migrationService
        );
        mboCategoryService = new MboCategoryServiceImpl(
            repository, repository, offersProcessingStatusService, mappingModerationServiceMock,
            mboMappingsHelperService, supplierRepository, offerProtoConverter, processingTicketInfoService,
            mskuRepository, migrationService, assignmentRepository, offerClearMappingService,
            saveTaskMappingsService, updateSupplierOfferCategoryService, categoryGroupService);

        migrationService.checkAndUpdateCache();
    }

    @After
    public void tearDown() {
        migrationService.invalidateAll();
    }

    @Test
    public void updateSupplierOfferMappings() {
        MboCategory.SaveTaskMappingsRequest request = MboCategory.SaveTaskMappingsRequest.newBuilder()
            .addMapping(ContentTaskResult.newBuilder()
                .setStatus(SupplierOfferMappingStatus.MAPPED)
                .setOfferId("11")
                .setMarketSkuId(MOCK_MARKET_SKU_ID)
                .setWorkerId("sjsd-hlt2-2sa5s-df77vmdf")
                .setStaffLogin("johnny"))
            .build();
        MboCategory.SaveTaskMappingsResponse response = mboCategoryService.saveTaskMappings(request);
        verify(modelStorageCachingServiceMock).getModelsFromMboThenPg(anyCollection());
        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);
        Offer offer = repository.getOfferById(11);
        MbocAssertions.assertThat(offer)
            .hasContentMapping(MOCK_MARKET_SKU_ID, Offer.SkuType.MARKET)
            .hasMappingModifiedBy("johnny")
            .hasCategoryId(CATEGORY_ID)
            .hasProcessingStatus(Offer.ProcessingStatus.PROCESSED)
            .hasContentLabState(Offer.ContentLabState.CL_NONE)
            .hasMappingsEqualAndNotNull(Offer.MappingType.CONTENT, Offer.MappingType.APPROVED)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
    }

    @Test
    public void updateSupplierOfferMappingsBusinessIsInMigration() {
        MigrationStatus migrationStatus = new MigrationStatus()
            .setId(1L)
            .setTargetBusinessId(44)
            .setSupplierId(1234123)
            .setSourceBusinessId(1)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.ACTIVE);
        migrationStatusRepository.save(migrationStatus);

        // force cache update
        migrationService.checkAndUpdateCache();

        MboCategory.SaveTaskMappingsRequest request = MboCategory.SaveTaskMappingsRequest.newBuilder()
            .addMapping(ContentTaskResult.newBuilder()
                .setStatus(SupplierOfferMappingStatus.MAPPED)
                .setOfferId("11")
                .setMarketSkuId(MOCK_MARKET_SKU_ID)
                .setWorkerId("sjsd-hlt2-2sa5s-df77vmdf")
                .setStaffLogin("johnny"))
            .build();

        MboCategory.SaveTaskMappingsResponse response = mboCategoryService.saveTaskMappings(request);

        Assertions.assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.ERROR);
        Assertions.assertThat(response.getResult().getMessage()).contains("Businesses [44] are in migration");
    }

    @Test
    public void updateSupplierOfferMappingsWithCategory() {
        MboCategory.SaveTaskMappingsRequest request = MboCategory.SaveTaskMappingsRequest.newBuilder()
            .addMapping(ContentTaskResult.newBuilder()
                .setStatus(SupplierOfferMappingStatus.MAPPED)
                .setOfferId("11")
                .setMarketSkuId(MOCK_MARKET_SKU_ID)
                .setWorkerId("sjsd-hlt2-2sa5s-df77vmdf")
                .setStaffLogin("johnny")
                .setCategoryId(CATEGORY_ID))
            .build();
        MboCategory.SaveTaskMappingsResponse response = mboCategoryService.saveTaskMappings(request);
        verify(modelStorageCachingServiceMock).getModelsFromMboThenPg(any(Multimap.class));
        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);
        Offer offer = repository.getOfferById(11);
        MbocAssertions.assertThat(offer)
            .hasContentMapping(MOCK_MARKET_SKU_ID, Offer.SkuType.MARKET)
            .hasMappingModifiedBy("johnny")
            .hasCategoryId(CATEGORY_ID)
            .hasProcessingStatus(Offer.ProcessingStatus.PROCESSED)
            .hasContentLabState(Offer.ContentLabState.CL_NONE)
            .hasMappingsEqualAndNotNull(Offer.MappingType.CONTENT, Offer.MappingType.APPROVED)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
    }

    @Test
    public void updateSupplierOfferMappingsContentLab() {
        Offer offer = repository.getOfferById(11);
        offer.setThroughContentLab(true);
        repository.updateOffer(offer);

        MboCategory.SaveTaskMappingsRequest request = MboCategory.SaveTaskMappingsRequest.newBuilder()
            .addMapping(ContentTaskResult.newBuilder()
                .setStatus(SupplierOfferMappingStatus.MAPPED)
                .setOfferId("11")
                .setMarketSkuId(MOCK_MARKET_SKU_ID)
                .setWorkerId("sjsd-hlt2-2sa5s-df77vmdf")
                .setStaffLogin("johnny"))
            .build();
        MboCategory.SaveTaskMappingsResponse response = mboCategoryService.saveTaskMappings(request);

        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);

        offer = repository.getOfferById(11);

        MbocAssertions.assertThat(offer)
            .hasContentLabState(Offer.ContentLabState.CL_READY);
    }

    @Test
    public void updateSupplierOfferMappingsWithSpecialComments() {
        MboCategory.SaveTaskMappingsRequest request = MboCategory.SaveTaskMappingsRequest.newBuilder()
            .addMapping(ContentTaskResult.newBuilder()
                .setOfferId("11")
                .setManualVendorId(1L)
                .setStatus(SupplierOfferMappingStatus.TRASH)
                .addContentComment(SupplierOffer.ContentComment.newBuilder()
                    .setType(ContentCommentType.NO_KNOWLEDGE.name())
                    .build())
            )
            .addMapping(ContentTaskResult.newBuilder()
                .setOfferId("12")
                .setStatus(SupplierOfferMappingStatus.TRASH)
                .addContentComment(SupplierOffer.ContentComment.newBuilder()
                    .setType(ContentCommentType.WRONG_CATEGORY.name())
                    .addItems("Правильная категория 1234")
                    .build())
            )
            .addMapping(ContentTaskResult.newBuilder()
                .setOfferId("13")
                .setStatus(SupplierOfferMappingStatus.TRASH)
                .addContentComment(SupplierOffer.ContentComment.newBuilder()
                    .setType(ContentCommentType.LEGAL_CONFLICT.name())
                    .build())
            )
            .build();
        MboCategory.SaveTaskMappingsResponse response = mboCategoryService.saveTaskMappings(request);
        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);

        Offer offer = repository.getOfferById(11);
        MbocAssertions.assertThat(offer)
            .doesNotHaveContentMapping()
            .hasVendorId(1)
            .hasProcessingStatus(Offer.ProcessingStatus.NO_KNOWLEDGE);
        Offer offer2 = repository.getOfferById(12);
        MbocAssertions.assertThat(offer2)
            .doesNotHaveContentMapping()
            .hasContentComments(
                new ContentComment(ContentCommentType.WRONG_CATEGORY,
                    Collections.singletonList("Правильная категория 1234")))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_RECLASSIFICATION);
        Offer offer3 = repository.getOfferById(13);
        MbocAssertions.assertThat(offer3)
            .doesNotHaveContentMapping()
            .hasProcessingStatus(Offer.ProcessingStatus.LEGAL_PROBLEM)
            .hasAcceptanceStatus(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void updateSupplierOfferMappingsWithTrash() {
        MboCategory.SaveTaskMappingsRequest request = MboCategory.SaveTaskMappingsRequest.newBuilder()
            .addMapping(ContentTaskResult.newBuilder()
                .setOfferId("11")
                .setStatus(SupplierOfferMappingStatus.TRASH)
                .setWorkerId("etw-sxcn1-csxnd-323mas-asdasd")
                .addContentComment(SupplierOffer.ContentComment.newBuilder()
                    .setType(ContentCommentType.INCORRECT_INFORMATION.name())
                    .addItems("test item")
                    .build())
            )
            .build();
        MboCategory.SaveTaskMappingsResponse response = mboCategoryService.saveTaskMappings(request);
        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);

        Offer offer = repository.getOfferById(11);
        MbocAssertions.assertThat(offer)
            .doesNotHaveContentMapping()
            .hasContentComments(
                new ContentComment(ContentCommentType.INCORRECT_INFORMATION, Collections.singletonList("test " +
                    "item")))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_INFO);
    }

    @Test
    public void updateSupplierOfferMappingsWithAlreadyProcessedOffers() {
        MboCategory.SaveTaskMappingsRequest request = MboCategory.SaveTaskMappingsRequest.newBuilder()
            .addMapping(ContentTaskResult.newBuilder()
                .setStatus(SupplierOfferMappingStatus.MAPPED)
                .setOfferId("9") // processed offer
                .setMarketSkuId(MOCK_MARKET_SKU_ID)
                .setWorkerId("sjsd-hlt2-2sa5s-df77vmdf")
                .setStaffLogin("johnny"))
            .addMapping(ContentTaskResult.newBuilder()
                .setStatus(SupplierOfferMappingStatus.MAPPED)
                .setOfferId("10") // need info offer
                .setMarketSkuId(MOCK_MARKET_SKU_ID)
                .setWorkerId("sjsd-hlt2-2sa5s-df77vmdf")
                .setStaffLogin("johnny"))
            .build();

        MboCategory.SaveTaskMappingsResponse response = mboCategoryService.saveTaskMappings(request);
        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);
        // do not update mappings if offers are already processed
        Mockito.verifyZeroInteractions(offerMappingActionService);

        // and do not update offers
        ArgumentCaptor<List<Offer>> updatedOffersCapture = ArgumentCaptor.forClass(List.class);
        Mockito.verify(repository).updateOffers(updatedOffersCapture.capture());
        assertThat(updatedOffersCapture.getValue()).isEmpty();
    }

    @Test
    public void updateSupplierOfferMappingWithSameContentMapping() {
        MboCategory.SaveTaskMappingsRequest request = MboCategory.SaveTaskMappingsRequest.newBuilder()
            .addMapping(ContentTaskResult.newBuilder()
                .setStatus(SupplierOfferMappingStatus.MAPPED)
                .setOfferId("100")
                .setMarketSkuId(MOCK_MARKET_SKU_ID)
                .setWorkerId("sjsd-hlt2-2sa5s-df77vmdf")
                .setStaffLogin("johnny"))
            .build();

        MboCategory.SaveTaskMappingsResponse response = mboCategoryService.saveTaskMappings(request);
        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);

        Offer offer = repository.getOfferById(100);
        MbocAssertions.assertThat(offer)
            .hasBindingKind(Offer.BindingKind.APPROVED)
            .hasContentMapping(MOCK_MARKET_SKU_ID, Offer.SkuType.MARKET)
            .hasMappingModifiedBy("johnny")
            .hasCategoryId(CATEGORY_ID)
            .hasProcessingStatus(Offer.ProcessingStatus.PROCESSED)
            .hasContentLabState(Offer.ContentLabState.CL_NONE)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        assertThat(offer.getApprovedSkuMapping())
            .matches(approved -> Offer.Mapping.mappingEqual(approved, offer.getContentSkuMapping()));
    }

    @Test
    public void updateSupplierOfferMappingWithUnpublishedContentMapping() {
        modelStorageCachingServiceMock.addModel(
            new Model()
                .setId(MOCK_MARKET_SKU_ID)
                .setCategoryId(CATEGORY_ID)
                .setModelType(Model.ModelType.SKU)
                .setTitle("Test title")
                .setPublishedOnBlueMarket(false)
        );

        MboCategory.SaveTaskMappingsRequest request = MboCategory.SaveTaskMappingsRequest.newBuilder()
            .addMapping(ContentTaskResult.newBuilder()
                .setStatus(SupplierOfferMappingStatus.MAPPED)
                .setOfferId("11")
                .setMarketSkuId(MOCK_MARKET_SKU_ID)
                .setWorkerId("sjsd-hlt2-2sa5s-df77vmdf")
                .setStaffLogin("johnny"))
            .build();

        MboCategory.SaveTaskMappingsResponse response = mboCategoryService.saveTaskMappings(request);

        verify(modelStorageCachingServiceMock).getModelsFromMboThenPg(anyCollection());

        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);
        Offer offer = repository.getOfferById(11);
        MbocAssertions.assertThat(offer)
            .hasContentMapping(MOCK_MARKET_SKU_ID, Offer.SkuType.MARKET)
            .hasMappingModifiedBy("johnny")
            .hasCategoryId(CATEGORY_ID)
            .hasProcessingStatus(Offer.ProcessingStatus.PROCESSED)
            .hasContentLabState(Offer.ContentLabState.CL_NONE)
            .hasMappingsEqualAndNotNull(Offer.MappingType.CONTENT, Offer.MappingType.APPROVED)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
    }

    @Test
    public void testSearchMappingsByInternalOfferId() {
        MboCategory.SearchMappingsByInternalOfferIdRequest request = MboCategory.SearchMappingsByInternalOfferIdRequest
            .newBuilder()
            .addOfferIds(11L)
            .build();
        MboCategory.SearchMappingsByInternalOfferIdResponse response = mboCategoryService
            .searchMappingsByInternalOfferId(request);

        List<SupplierOffer.Offer> offers = response.getOfferList();
        assertThat(offers).hasSize(1);
        assertThat(offers).extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrder(11L);
    }

    @Test
    public void testGetContentCommentTypes() {
        MboCategory.ContentCommentTypes.Request request = MboCategory.ContentCommentTypes.Request.getDefaultInstance();
        MboCategory.ContentCommentTypes.Response response = mboCategoryService.getContentCommentTypes(request);
        assertThat(response).isNotNull();
        assertThat(response.getContentCommentTypeList()).hasSize(ContentCommentType.actualValues().size());

        for (MboCategory.ContentCommentTypes.ContentCommentType type : response.getContentCommentTypeList()) {
            ContentCommentType expectedType = ContentCommentType.valueOf(type.getType());
            assertThat(type.getDescription()).isEqualTo(expectedType.getDescription());
            assertThat(type.getRequireItems()).isEqualTo(expectedType.requireItems());
            assertThat(type.getVariantList()).hasSize(expectedType.getVariants().size());
            assertThat(type.getScopeList()).hasSize(expectedType.getScopes().size());

            if (type.hasAllowOther()) {
                assertThat(type.getAllowOther()).isEqualTo(expectedType.getAllowOther());
            } else {
                assertThat(expectedType.getAllowOther()).isNull();
            }
        }

        MboCategory.ContentCommentTypes.Request classificationRequest =
            MboCategory.ContentCommentTypes.Request.newBuilder()
                .setScope(MboCategory.ContentCommentTypes.Scope.CLASSIFICATION)
                .build();
        MboCategory.ContentCommentTypes.Response classificationResponse =
            mboCategoryService.getContentCommentTypes(classificationRequest);
        assertThat(classificationResponse).isNotNull();
        assertThat(classificationResponse.getContentCommentTypeList())
            .hasSize(ContentCommentType.forScope(ContentCommentScope.CLASSIFICATION).size());

        MboCategory.ContentCommentTypes.Request matchingRequest =
            MboCategory.ContentCommentTypes.Request.newBuilder()
                .setScope(MboCategory.ContentCommentTypes.Scope.MATCHING)
                .build();
        MboCategory.ContentCommentTypes.Response matchingResponse =
            mboCategoryService.getContentCommentTypes(matchingRequest);
        assertThat(matchingResponse).isNotNull();
        assertThat(matchingResponse.getContentCommentTypeList())
            .hasSize(ContentCommentType.forScope(ContentCommentScope.MATCHING).size());

        MboCategory.ContentCommentTypes.Request contentProcessingRequest =
            MboCategory.ContentCommentTypes.Request.newBuilder()
                .setScope(MboCategory.ContentCommentTypes.Scope.CONTENT_PROCESSING)
                .build();
        MboCategory.ContentCommentTypes.Response contentProcessingResponse =
            mboCategoryService.getContentCommentTypes(contentProcessingRequest);
        assertThat(contentProcessingResponse).isNotNull();
        assertThat(contentProcessingResponse.getContentCommentTypeList())
            .hasSize(ContentCommentType.forScope(ContentCommentScope.CONTENT_PROCESSING).size());
    }

    @Test
    public void updateSupplierOfferCategoryShouldReturnCorrectStatusMessage() {
        OfferRepository offerRepository = Mockito.mock(OfferRepository.class);
        var saveTaskMappingsService = new SaveTaskMappingsService(
            Mockito.mock(ModelStorageCachingServiceMock.class), offerRepository,
            offerMappingActionService, manualVendorService, offersProcessingStatusService, migrationService
        );
        var updateSupplierOfferCategoryService = new UpdateSupplierOfferCategoryService(
            offerRepository, supplierRepository, classificationOffersProcessingService, offersProcessingStatusService,
            migrationService
        );
        mboCategoryService = new MboCategoryServiceImpl(
            offerRepository, offerRepository, Mockito.mock(OffersProcessingStatusService.class),
            Mockito.mock(SaveMappingModerationService.class), mboMappingsHelperService, supplierRepository,
            offerProtoConverter, processingTicketInfoService, null, migrationService,
            assignmentRepository, offerClearMappingService, saveTaskMappingsService,
            updateSupplierOfferCategoryService, categoryGroupService);

        //  success
        MboCategory.UpdateSupplierOfferCategoryResponse response = mboCategoryService.updateSupplierOfferCategory(
            MboCategory.UpdateSupplierOfferCategoryRequest.newBuilder().build());
        Assertions.assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);

        //  error
        when(offerRepository.updateOffers(Mockito.anyCollection())).thenThrow(new RuntimeException("Test"));
        response = mboCategoryService.updateSupplierOfferCategory(
            MboCategory.UpdateSupplierOfferCategoryRequest.newBuilder().build());
        Assertions.assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.INTERNAL_ERROR);
        Assertions.assertThat(response.getResult().getMessage()).contains("Test");
    }

    @Test
    public void testUpdateSupplierOfferCategoryWithRootBookCategory() {
        Offer offer1 = OfferTestUtils.simpleOffer(1L)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        Offer offer2 = OfferTestUtils.simpleOffer(2L)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);

        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(offer1, offer2);

        long rootBookCategory = 90829L;
        categoryCachingService.addCategory(new Category().setCategoryId(rootBookCategory));
        categoryCachingService.addCategory(new Category().setCategoryId(1L));
        categoryKnowledgeServiceMock.removeCategory(1L);

        MboCategory.UpdateSupplierOfferCategoryRequest request =
            MboCategory.UpdateSupplierOfferCategoryRequest.newBuilder()
                .addResult(SupplierOffer.ClassificationTaskResult.newBuilder()
                    .setFixedCategoryId(rootBookCategory)
                    .setOfferId("1"))
                .addResult(SupplierOffer.ClassificationTaskResult.newBuilder()
                    .setFixedCategoryId(1L)
                    .setOfferId("2"))
                .build();

        mboCategoryService.updateSupplierOfferCategory(request);

        Assertions.assertThat(offerRepository.getOfferById(1L))
            .isNotNull()
            .extracting(Offer::getProcessingStatus)
            .isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        Assertions.assertThat(offerRepository.getOfferById(2L))
            .isNotNull()
            .extracting(Offer::getProcessingStatus)
            .isEqualTo(Offer.ProcessingStatus.NO_KNOWLEDGE);
    }


    @Test
    public void updateSupplierOfferCategoryBusinessIsInMigration() {
        MigrationStatus migrationStatus = new MigrationStatus()
            .setId(1L)
            .setTargetBusinessId(44)
            .setSupplierId(1234123)
            .setSourceBusinessId(1)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.ACTIVE);
        migrationStatusRepository.save(migrationStatus);

        // force cache update
        migrationService.checkAndUpdateCache();

        categoryCachingService.addCategory(new Category().setCategoryId(1234L));

        MboCategory.UpdateSupplierOfferCategoryResponse response = mboCategoryService.updateSupplierOfferCategory(
            MboCategory.UpdateSupplierOfferCategoryRequest.newBuilder()
                .addResult(SupplierOffer.ClassificationTaskResult.newBuilder()
                    .setOfferId("11")
                    .setFixedCategoryId(1234))
                .build());

        Assertions.assertThat(response.getResult()).extracting(SupplierOffer.OperationResult::getStatus)
            .isEqualTo(OperationStatus.ERROR);
        Assertions.assertThat(response.getResult()).extracting(SupplierOffer.OperationResult::getMessage)
            .isEqualTo("Businesses [44] are in migration");
    }

    @Test
    public void forceOfferCategoryProxiesCallToUpdateSupplierOfferCategory() {
        ForceOfferCategoryRequest.Operation operation1 = ForceOfferCategoryRequest.Operation.newBuilder()
            .setBusinessId(456L)
            .setShopSku("sku1")
            .setCategoryId(123L)
            .setStaffLogin("staff1")
            .build();
        ForceOfferCategoryRequest.Operation operation2 =
            ForceOfferCategoryRequest.Operation.newBuilder()
                .setBusinessId(789L)
                .setShopSku("sku2")
                .setCategoryId(345L)
                .setStaffLogin("staff2")
                .build();

        OfferRepository offerRepository = Mockito.mock(OfferRepository.class);

        Offer offer1 = mock(Offer.class);
        doReturn(1000L).when(offer1).getId();
        doReturn(new BusinessSkuKey(456, "sku1")).when(offer1).getBusinessSkuKey();
        Offer offer2 = mock(Offer.class);
        doReturn(2000L).when(offer2).getId();
        doReturn(new BusinessSkuKey(789, "sku2")).when(offer2).getBusinessSkuKey();

        doReturn(List.of(offer1, offer2)).when(offerRepository).findOffersByBusinessSkuKeys(anyCollection());

        var saveTaskMappingsService = new SaveTaskMappingsService(
            modelStorageCachingServiceMock, offerRepository, offerMappingActionService,
            manualVendorService, offersProcessingStatusService, migrationService
        );
        var updateSupplierOfferCategoryService = new UpdateSupplierOfferCategoryService(
            offerRepository, supplierRepository, classificationOffersProcessingService, offersProcessingStatusService,
            migrationService
        );
        updateSupplierOfferCategoryService = spy(updateSupplierOfferCategoryService);
        MboCategoryService categoryService = new MboCategoryServiceImpl(
            offerRepository, offerRepository, offersProcessingStatusService, mappingModerationServiceMock,
            mboMappingsHelperService, supplierRepository, offerProtoConverter, processingTicketInfoService, null,
            migrationService, assignmentRepository, offerClearMappingService,
            saveTaskMappingsService, updateSupplierOfferCategoryService, categoryGroupService);
        categoryService = spy(categoryService);

        SupplierOffer.OperationResult operationResult = SupplierOffer.OperationResult.newBuilder().build();
        doReturn(
            MboCategory.UpdateSupplierOfferCategoryResponse.newBuilder().setResult(operationResult).build()
        ).when(updateSupplierOfferCategoryService).updateSupplierOfferCategory(any(), Mockito.eq(true));

        MboCategory.ForceOfferCategoryResponse response = categoryService.forceOfferCategory(
            ForceOfferCategoryRequest.newBuilder().addAllOperations(List.of(operation1, operation2)).build());

        ArgumentCaptor<MboCategory.UpdateSupplierOfferCategoryRequest> argumentCaptor =
            ArgumentCaptor.forClass(MboCategory.UpdateSupplierOfferCategoryRequest.class);

        verify(updateSupplierOfferCategoryService).updateSupplierOfferCategory(
            argumentCaptor.capture(), Mockito.eq(true));

        Assert.assertSame(operationResult, response.getResult());

        MboCategory.UpdateSupplierOfferCategoryRequest proxied = argumentCaptor.getValue();
        Map<String, SupplierOffer.ClassificationTaskResult> results = proxied.getResultList().stream()
            .collect(Collectors.toMap(SupplierOffer.ClassificationTaskResult::getOfferId, Function.identity()));

        SupplierOffer.ClassificationTaskResult result1 = results.get("1000");
        SupplierOffer.ClassificationTaskResult result2 = results.get("2000");

        Assert.assertEquals(operation1.getCategoryId(), result1.getFixedCategoryId());
        Assert.assertEquals(operation1.getStaffLogin(), result1.getStaffLogin());
        Assert.assertEquals(operation1.getBusinessId(), result1.getSupplierId());
        Assert.assertEquals(operation2.getCategoryId(), result2.getFixedCategoryId());
        Assert.assertEquals(operation2.getStaffLogin(), result2.getStaffLogin());
        Assert.assertEquals(operation2.getBusinessId(), result2.getSupplierId());
    }

    @Test
    public void forceOfferReclassificationSendsToClassification() {
        var operation1 = ForceOfferReclassificationRequest.Operation.newBuilder()
            .setBusinessId(42L)
            .setShopSku("sku1")
            .setStaffLogin("staff1")
            .build();
        var operation2 = ForceOfferReclassificationRequest.Operation.newBuilder()
            .setBusinessId(43L)
            .setShopSku("sku2")
            .setStaffLogin("staff2")
            .build();

        Offer offer1 = OfferTestUtils.simpleOffer(1000L)
            .setBusinessId((int) operation1.getBusinessId())
            .setShopSku(operation1.getShopSku())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
        repository.insertOffer(offer1);

        Offer offer2 = OfferTestUtils.simpleOffer(2000L)
            .setBusinessId((int) operation2.getBusinessId())
            .setShopSku(operation2.getShopSku())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
        repository.insertOffer(offer2);

        var response = mboCategoryService.forceOfferReclassification(
            ForceOfferReclassificationRequest.newBuilder().addAllOperations(List.of(operation1, operation2)).build());

        Assert.assertEquals(OperationStatus.SUCCESS, response.getResult().getStatus());

        var updated1 = repository.getOfferById(offer1.getId());
        var updated2 = repository.getOfferById(offer2.getId());

        Assertions.assertThat(updated1.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        Assertions.assertThat(updated2.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    @Test
    public void forceOfferReclassificationBusinessIsInMigration() {
        MigrationStatus migrationStatus = new MigrationStatus()
            .setId(1001L)
            .setTargetBusinessId(45)
            .setSupplierId(42)
            .setSourceBusinessId(0)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.ACTIVE);
        migrationStatusRepository.save(migrationStatus);

        // force cache update
        migrationService.checkAndUpdateCache();

        var operation1 = ForceOfferReclassificationRequest.Operation.newBuilder()
            .setBusinessId(42L)
            .setShopSku("sku1")
            .setStaffLogin("staff1")
            .build();
        var operation2 = ForceOfferReclassificationRequest.Operation.newBuilder()
            .setBusinessId(43L)
            .setShopSku("sku2")
            .setStaffLogin("staff2")
            .build();

        Offer offer1 = OfferTestUtils.simpleOffer(1001L)
            .setBusinessId((int) operation1.getBusinessId())
            .setShopSku(operation1.getShopSku())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
        repository.insertOffer(offer1);

        Offer offer2 = OfferTestUtils.simpleOffer(2001L)
            .setBusinessId((int) operation2.getBusinessId())
            .setShopSku(operation2.getShopSku())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
        repository.insertOffer(offer2);

        var response = mboCategoryService.forceOfferReclassification(
            ForceOfferReclassificationRequest.newBuilder().addAllOperations(List.of(operation1, operation2)).build());

        Assertions.assertThat(response.getResult()).extracting(SupplierOffer.OperationResult::getStatus)
            .isEqualTo(OperationStatus.ERROR);
        Assertions.assertThat(response.getResult()).extracting(SupplierOffer.OperationResult::getMessage)
            .isEqualTo("Businesses [42] are in migration");
    }

    @Test
    public void update3pOfferState() {
        MboCategory.UpdateContentLabStatesRequest request = MboCategory.UpdateContentLabStatesRequest.newBuilder()
            .addOfferState(SupplierOffer.OfferContentLabState.newBuilder()
                .setShopId(44)
                .setShopSkuId("qwerty")
                .setState(SupplierOffer.ContentLabState.CL_READY)
                .setMessage("Ready")
                .build())
            .build();

        MboCategory.UpdateContentLabStatesResponse response = mboCategoryService.updateContentLabStates(request);
        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);

        Offer offer = repository.getOfferById(14);
        MbocAssertions.assertThat(offer)
            .hasContentLabState(Offer.ContentLabState.CL_READY)
            .hasContentLabMessage("Ready");
    }

    @Test
    public void update1pOfferState() {
        MboCategory.UpdateContentLabStatesRequest request = MboCategory.UpdateContentLabStatesRequest.newBuilder()
            .addOfferState(SupplierOffer.OfferContentLabState.newBuilder()
                .setShopId(BERU_ID)
                .setShopSkuId("00034.qwerty123")
                .setState(SupplierOffer.ContentLabState.CL_READY)
                .setMessage("Ready")
                .build())
            .build();

        supplierRepository.insertOrUpdate(OfferTestUtils.simpleSupplier()
            .setId(45)
            .setRealSupplierId("00034")
            .setType(MbocSupplierType.REAL_SUPPLIER));

        MboCategory.UpdateContentLabStatesResponse response = mboCategoryService.updateContentLabStates(request);
        assertThat(response.getResult().getStatus()).isEqualTo(OperationStatus.SUCCESS);

        Offer offer = repository.getOfferById(15);
        MbocAssertions.assertThat(offer)
            .hasContentLabState(Offer.ContentLabState.CL_READY)
            .hasContentLabMessage("Ready");
    }

    @Test
    public void testGetShortOfferInfos() {
        MboCategory.GetShortOfferInfosRequest request = MboCategory.GetShortOfferInfosRequest.newBuilder()
            .setDeadlineStartDate(LocalDate.parse("2019-08-05").toEpochDay())
            .setDeadlineFinishDate(LocalDate.parse("2019-08-12").toEpochDay())
            .build();

        MboCategory.GetShortOfferInfosResponse response = mboCategoryService.getShortOfferInfos(request);
        List<MboCategory.GetShortOfferInfosResponse.OfferInfo> offerInfosList = response.getOfferInfosList();
        assertThat(offerInfosList).hasSize(2);
        assertThat(offerInfosList).extracting(MboCategory.GetShortOfferInfosResponse.OfferInfo::getOfferId)
            .containsExactlyInAnyOrder(5L, 12L);
    }

    @Test
    public void testGetShortOfferInfosWithNullDeadline() {
        MboCategory.GetShortOfferInfosRequest request = MboCategory.GetShortOfferInfosRequest.newBuilder()
            .setDeadlineStartDate(LocalDate.parse("2018-05-10").toEpochDay())
            .setDeadlineFinishDate(LocalDate.parse("2018-05-11").toEpochDay())
            .build();

        MboCategory.GetShortOfferInfosResponse response = mboCategoryService.getShortOfferInfos(request);
        List<MboCategory.GetShortOfferInfosResponse.OfferInfo> offerInfosList = response.getOfferInfosList();
        assertThat(offerInfosList).hasSize(3);
        assertThat(offerInfosList).extracting(MboCategory.GetShortOfferInfosResponse.OfferInfo::getOfferId)
            .containsExactlyInAnyOrder(15L, 11L, 1L);
        MboCategory.GetShortOfferInfosResponse.OfferInfo offerInfo = offerInfosList.get(0);
        Assert.assertTrue(offerInfo.hasAutoDeadlineDate());
        Assert.assertFalse(offerInfo.hasDeadlineDate());
    }

    @Test
    public void testGetShortSupplierInfos() {
        Supplier supplier1 = new Supplier()
            .setId(10001)
            .setName("Some name")
            .setRealSupplierId("00034")
            .setType(MbocSupplierType.REAL_SUPPLIER);
        Supplier supplier2 = new Supplier()
            .setId(10002)
            .setName("Another name")
            .setRealSupplierId("00035")
            .setType(MbocSupplierType.FIRST_PARTY);

        supplierRepository.insertOrUpdate(supplier1);
        supplierRepository.insertOrUpdate(supplier2);

        var request = MboCategory.GetShortSupplierInfosRequest.newBuilder()
            .addAllSupplierId(Set.of(supplier1.getId(), supplier2.getId())).build();
        var response = mboCategoryService.getShortSupplierInfos(request);
        var offerInfosList = response.getShortSupplierInfoList();
        assertThat(offerInfosList).hasSize(2);
        assertThat(offerInfosList).extracting(
                MboCategory.GetShortSupplierInfosResponse.ShortSupplierInfo::getSupplierId)
            .containsExactlyInAnyOrder(supplier1.getId(), supplier2.getId());
        var expectedList = List.of(supplier1, supplier2).stream()
            .map(supplier -> MboCategory.GetShortSupplierInfosResponse.ShortSupplierInfo.newBuilder()
                .setSupplierName(supplier.getName())
                .setSupplierId(supplier.getId())
                .setSupplierType(OfferProtoConverter.convertSupplierType(supplier.getType()))
                .build())
            .collect(Collectors.toList());
        assertThat(offerInfosList).containsExactlyInAnyOrderElementsOf(expectedList);

        // empty query
        var emptyRequest = MboCategory.GetShortSupplierInfosRequest.newBuilder().build();
        var responseForEmpty = mboCategoryService.getShortSupplierInfos(emptyRequest);
        assertThat(responseForEmpty.getShortSupplierInfoList()).isEmpty();
    }

    @Test
    public void testGetTicketStatusesSimple() {
        ProcessingTicketInfo ticketInfo = new ProcessingTicketInfo()
            .setTotalOffers(10)
            .setSupplierId(SUPPLIER_ID)
            .setCritical(true)
            .setOfferBaseStatus(OfferProcessingStatus.IN_PROCESS)
            .setTitle("MCP-1")
            .setComputedDeadline(LocalDate.parse("2018-05-09"));
        processingTicketInfoService.setCounts(ImmutableMap.of(CATEGORY_ID, 5, CATEGORY_ID_1, 1),
            ticketInfo,
            ProcessingTicketInfo::getActiveOffers,
            ProcessingTicketInfo::setActiveOffers);

        processingTicketInfoService.setCounts(ImmutableMap.of(CATEGORY_ID, 10, CATEGORY_ID_1, 8),
            ticketInfo,
            ProcessingTicketInfo::getOffersByCategory,
            ProcessingTicketInfo::setOffersByCategory);

        ticketInfo = processingTicketInfoService.update(ticketInfo);

        supplierRepository.insertOrUpdate(
            new Supplier()
                .setId(SUPPLIER_ID)
                .setRealSupplierId("00034")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setName("SUPP44")
        );

        MboCategory.GetTicketStatusesRequest request = MboCategory.GetTicketStatusesRequest.newBuilder()
            .build();

        MboCategory.GetTicketStatusesResponse response = mboCategoryService.getTicketStatuses(request);
        assertThat(response).isEqualTo(
            MboCategory.GetTicketStatusesResponse.newBuilder()
                .addTicketStatus(
                    MboCategory.GetTicketStatusesResponse.TicketStatus
                        .newBuilder()
                        .setProcessingTicketId(ticketInfo.getId())
                        .setAutoDeadlineDate(ticketInfo.getComputedDeadline().toEpochDay())
                        .addCategories(MboCategory.GetTicketStatusesResponse.CategoryInfo.newBuilder()
                            .setCategoryId(CATEGORY_ID)
                            .setActiveOffers(5)
                            .setTotalOffers(10)
                        )
                        .addCategories(MboCategory.GetTicketStatusesResponse.CategoryInfo.newBuilder()
                            .setCategoryId(CATEGORY_ID_1)
                            .setActiveOffers(1)
                            .setTotalOffers(8)
                        )
                        .setTicketCritical(true)
                        .setIdentifier("MCP-1")
                        .setSupplierId(SUPPLIER_ID)
                        .setSupplierName("SUPP44")
                        .setSupplierType(OfferProtoConverter.convertSupplierType(MbocSupplierType.REAL_SUPPLIER))
                        .setBaseStatus(SupplierOffer.Offer.InternalProcessingStatus.IN_PROCESS)
                ).build()
        );
    }

    @Test
    public void testGetTicketStatusesNoTitle() {
        ProcessingTicketInfo ticketInfo = new ProcessingTicketInfo()
            .setStuckOffers(2)
            .setCritical(false)
            .setOfferBaseStatus(OfferProcessingStatus.IN_MODERATION)
            .setDeadline(LocalDate.parse("2018-05-09"));
        updateCounts(ticketInfo, CATEGORY_ID, 5, 10);
        processingTicketInfoService.update(ticketInfo);
        MboCategory.GetTicketStatusesRequest request = MboCategory.GetTicketStatusesRequest.newBuilder()
            .build();
        MboCategory.GetTicketStatusesResponse response = mboCategoryService.getTicketStatuses(request);
        MboCategory.GetTicketStatusesResponse.TicketStatus ticketStatus = response.getTicketStatus(0);
        assertThat(ticketStatus.hasIdentifier()).isFalse();
        assertThat(ticketStatus.getProcessingTicketId()).isEqualTo(ticketInfo.getId());
    }

    @Test
    public void testGetTicketStatusesDeadlineFilter() {
        ProcessingTicketInfo ticketInfo1 = processingTicketInfoService.update(new ProcessingTicketInfo()
            .setStuckOffers(0)
            .setActiveOffers(ProcessingTicketInfoService.EMPTY_MAP)
            .setOffersByCategory(ProcessingTicketInfoService.EMPTY_MAP)
            .setCritical(false)
            .setOfferBaseStatus(OfferProcessingStatus.IN_PROCESS)
            .setTitle("MCP-1")
            .setDeadline(LocalDate.parse("2018-05-09")));

        ProcessingTicketInfo ticketInfo2 = processingTicketInfoService.update(new ProcessingTicketInfo()
            .setStuckOffers(0)
            .setCritical(false)
            .setActiveOffers(ProcessingTicketInfoService.EMPTY_MAP)
            .setOffersByCategory(ProcessingTicketInfoService.EMPTY_MAP)
            .setOfferBaseStatus(OfferProcessingStatus.IN_PROCESS)
            .setTitle("MCP-2")
            .setComputedDeadline(LocalDate.parse("2018-05-10")));

        ProcessingTicketInfo ticketInfo3 = processingTicketInfoService.update(new ProcessingTicketInfo()
            .setStuckOffers(0)
            .setActiveOffers(ProcessingTicketInfoService.EMPTY_MAP)
            .setOffersByCategory(ProcessingTicketInfoService.EMPTY_MAP)
            .setCritical(false)
            .setOfferBaseStatus(OfferProcessingStatus.IN_PROCESS)
            .setTitle("MCP-3")
            .setComputedDeadline(LocalDate.parse("2018-05-12")));

        MboCategory.GetTicketStatusesRequest request = MboCategory.GetTicketStatusesRequest.newBuilder()
            .setDeadlineStartDate(LocalDate.parse("2018-05-10").toEpochDay())
            .build();
        assertThat(mboCategoryService.getTicketStatuses(request).getTicketStatusList())
            .extracting(MboCategory.GetTicketStatusesResponse.TicketStatus::getIdentifier)
            .containsExactlyInAnyOrder("MCP-2", "MCP-3");

        request = MboCategory.GetTicketStatusesRequest.newBuilder()
            .setDeadlineFinishDate(LocalDate.parse("2018-05-11").toEpochDay())
            .build();
        assertThat(mboCategoryService.getTicketStatuses(request).getTicketStatusList())
            .extracting(MboCategory.GetTicketStatusesResponse.TicketStatus::getIdentifier)
            .containsExactlyInAnyOrder("MCP-1", "MCP-2");

        request = MboCategory.GetTicketStatusesRequest.newBuilder()
            .setDeadlineStartDate(LocalDate.parse("2018-05-10").toEpochDay())
            .setDeadlineFinishDate(LocalDate.parse("2018-05-11").toEpochDay())
            .build();
        assertThat(mboCategoryService.getTicketStatuses(request).getTicketStatusList())
            .extracting(MboCategory.GetTicketStatusesResponse.TicketStatus::getIdentifier)
            .containsExactlyInAnyOrder("MCP-2");
    }

    @Test
    public void assignEmptyList() {
        storageKeyValueService.putValue("getTicketPriorities.ClassificationQueue", true);
        assignmentRepository.assign(List.of(), OfferTarget.YANG, OfferProcessingType.IN_CLASSIFICATION);
    }

    private void updateCounts(ProcessingTicketInfo ticketInfo, long categoryId, int active, int total) {
        ticketInfo.setTotalOffers(total);
        processingTicketInfoService.setCounts(ImmutableMap.of(categoryId, active),
            ticketInfo,
            ProcessingTicketInfo::getActiveOffers,
            ProcessingTicketInfo::setActiveOffers);
        processingTicketInfoService.setCounts(ImmutableMap.of(categoryId, total),
            ticketInfo,
            ProcessingTicketInfo::getOffersByCategory,
            ProcessingTicketInfo::setOffersByCategory);
    }
}
