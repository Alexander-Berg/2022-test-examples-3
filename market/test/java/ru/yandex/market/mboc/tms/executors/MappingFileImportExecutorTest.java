package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mboc.common.contentprocessing.to.ContentProcessingObserver;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mapping_import.MappingFile;
import ru.yandex.market.mboc.common.services.mapping_import.MappingFileRepository;
import ru.yandex.market.mboc.common.services.mapping_import.MappingFileRow;
import ru.yandex.market.mboc.common.services.mapping_import.MappingFileRowRepository;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.mapping.context.SkuMappingContext;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;

public class MappingFileImportExecutorTest extends BaseDbTestClass {
    @Autowired
    private MappingFileRepository fileRepo;
    @Autowired
    private MappingFileRowRepository fileRowRepo;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private DataSource slaveDataSource;
    @Autowired
    private DataSource masterDataSource;
    @Autowired
    private ContentProcessingObserver contentProcessingObserver;

    private OfferRepositoryMock offerRepository;
    private ModelStorageCachingServiceMock modelCache;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private OfferMappingActionService mappingActionService;
    private OffersProcessingStatusService offersProcessingStatusService;
    private MappingFileImportExecutor executor;

    @Before
    public void setup() {
        modelCache = new ModelStorageCachingServiceMock();

        offerRepository = new OfferRepositoryMock();
        offerRepository.addObserver(contentProcessingObserver);

        var categoryCachingServiceMock = new CategoryCachingServiceMock();
        var categoryKnowledgeService = new CategoryKnowledgeServiceMock()
            .enableAllCategories();

        var legacyActionService = new LegacyOfferMappingActionService(null, null, offerDestinationCalculator,
            storageKeyValueService);
        mappingActionService = new OfferMappingActionService(legacyActionService);

        var offerBatchProcessor = new OfferBatchProcessor(slaveDataSource, masterDataSource, transactionManager,
            transactionManager,
            offerRepository, offerRepository, transactionTemplate);
        var supplierService = new SupplierService(supplierRepository);
        var needContentStatusService = new NeedContentStatusService(
            categoryCachingServiceMock, supplierService, Mockito.mock(BooksService.class));
        var fastSkuMappingsService = new FastSkuMappingsService(needContentStatusService);
        var retrieveMappingSkuTypeService = Mockito.mock(RetrieveMappingSkuTypeService.class);
        Mockito.when(retrieveMappingSkuTypeService.retrieveMappingSkuType(anyCollection(), anySet(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor,
            needContentStatusService,
            supplierService,
            categoryKnowledgeService,
            retrieveMappingSkuTypeService,
            mappingActionService,
            categoryInfoRepository,
            antiMappingRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            fastSkuMappingsService,
            true,
            true,
            3, categoryInfoCache);

        executor = new MappingFileImportExecutor(fileRepo, fileRowRepo, modelCache, offerRepository,
            mappingActionService, TransactionHelper.MOCK, storageKeyValueService, offerDestinationCalculator,
            offersProcessingStatusService);


        fileRepo.deleteAll();
        fileRowRepo.deleteAll();

        supplierRepository.deleteAll();
        var supplier1 = OfferTestUtils.simpleSupplier().setId(1019355);
        var supplier2 = OfferTestUtils.simpleSupplier().setId(1019351);
        var supplier3 = OfferTestUtils.simpleSupplier().setId(10339600);
        supplierRepository.insertBatch(supplier1, supplier2, supplier3);

        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(createOffer(), createInvalidOffer(), createOfferWithDeletedMapping());

        storageKeyValueService.putValue(MappingFileImportExecutor.VERIFICATION_PHASE_ENABLED, false);
        storageKeyValueService.putValue(MappingFileImportExecutor.APPROVE_ROBO_UPLOADS, false);
    }

    @Test
    public void testSingleFile() {
        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(2).build());

        createModel();

        var row1 = fileRowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).supplierId(1019355).ssku("669744").mskuId(5144704L).build());
        var row2 = fileRowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).supplierId(1019351).ssku("669741").mskuId(5144704L).build());
        var row3 = fileRowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).supplierId(1019351).ssku("669888").mskuId(5144704L).build());
        var row4 = fileRowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).supplierId(1019355).ssku("669744").mskuId(5144704L).build());


        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNotNull(row1.getProcessed());
        assertNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.DONE, row1.getResolution());

        var updatedOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertNotNull(updatedOffer.getAutoApprovedMappingSource());
        assertEquals(Offer.AutoApprovedMappingSource.XLS_MAPPING, updatedOffer.getAutoApprovedMappingSource());

        row2 = fileRowRepo.findById(row2.getId());
        assertNotNull(row2.getSkipped());
        assertNull(row2.getProcessed());
        assertEquals(MappingFileRow.Resolution.ACCEPTANCE_TRASH, row2.getResolution());

        row3 = fileRowRepo.findById(row3.getId());
        assertNotNull(row3.getSkipped());
        assertNull(row3.getProcessed());
        assertEquals(MappingFileRow.Resolution.NO_SUCH_OFFER, row3.getResolution());

        row4 = fileRowRepo.findById(row4.getId());
        assertNotNull(row4.getSkipped());
        assertNull(row4.getProcessed());
        assertEquals(MappingFileRow.Resolution.REPEATED, row4.getResolution());
    }

    @Test
    public void testSuccessfulMappingDeletionWithReset() {
        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(1).build());

        var offer = createOfferWithApprovedMapping(1L, Offer.SkuType.MARKET);
        offer.setContentSkuMapping(offer.getApprovedSkuMapping());
        offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED);
        offerRepository.insertOffer(offer);
        offer = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());

        var row1 = fileRowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).
                supplierId(offer.getBusinessId()).ssku(offer.getShopSku())
                .mskuId(0L)
                .resetOnDelete(true)
                .build()
        );

        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNotNull(row1.getProcessed());
        assertNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.DONE, row1.getResolution());

        var updatedOffer = offerRepository
            .findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku())).get(0);
        assertThat(updatedOffer.getApprovedSkuId()).isEqualTo(0L);
        assertThat(updatedOffer.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.RESET);
        assertThat(updatedOffer.getAutoSkuId()).isEqualTo(0L);
        assertThat(updatedOffer.getAutoApprovedMappingSource()).isEqualTo(Offer.AutoApprovedMappingSource.XLS_MAPPING);
        assertThat(updatedOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void testSuccessfulMappingDeletionWithoutReset() {
        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(1).build());

        var offer = createOfferWithApprovedMapping(1L, Offer.SkuType.MARKET);
        offer.setContentSkuMapping(offer.getApprovedSkuMapping());
        offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED);
        offerRepository.insertOffer(offer);
        offer = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());

        var row1 = fileRowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).
                supplierId(offer.getBusinessId()).ssku(offer.getShopSku())
                .mskuId(0L)
                .resetOnDelete(false)
                .build()
        );

        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNotNull(row1.getProcessed());
        assertNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.DONE, row1.getResolution());

        var updatedOffer = offerRepository
            .findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku())).get(0);
        assertThat(updatedOffer.getApprovedSkuId()).isEqualTo(0L);
        assertThat(updatedOffer.getApprovedSkuMappingConfidence()).isNull();
        assertThat(updatedOffer.getAutoSkuId()).isEqualTo(0L);
        assertThat(updatedOffer.getAutoApprovedMappingSource()).isEqualTo(Offer.AutoApprovedMappingSource.XLS_MAPPING);
        assertThat(updatedOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        assertThat(updatedOffer.getAcceptanceStatus()).isNotEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void testUndefinedResetOnDelete() {
        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(1).build());

        var offer = createOfferWithApprovedMapping(1L, Offer.SkuType.MARKET);
        offerRepository.insertOffer(offer);
        offer = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());

        var row1 = fileRowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).
                supplierId(offer.getBusinessId()).ssku(offer.getShopSku())
                .mskuId(0L)
                .build()
        );

        failFastExecution();

        List<MappingFileRow> all = fileRowRepo.findByIds(List.of(row1.getId()));
        all.forEach(
            row -> assertEquals(MappingFileRow.Resolution.UNDEFINED_RESET_ON_DELETE, row.getResolution())
        );

        MappingFile fileById = fileRepo.findById(file.getId());
        assertTrue(fileById.isCompleted());
    }

    @Test
    public void testMappingDeletionNotAllowed() {
        String login = "robot-smart-matcher";
        storageKeyValueService.putValue(MappingFileImportExecutor.SMART_MATCHER_ROBOT, login);
        var file = fileRepo.insert(
            MappingFile.builder().filename("filename.xls").size(1).login(login).build()
        );

        var offer = createOfferWithApprovedMapping(1L, Offer.SkuType.MARKET);
        offerRepository.insertOffer(offer);
        offer = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());

        var row1 = fileRowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).
                supplierId(offer.getBusinessId()).ssku(offer.getShopSku())
                .mskuId(0L)
                .resetOnDelete(true)
                .build()
        );

        failFastExecution();

        List<MappingFileRow> all = fileRowRepo.findByIds(List.of(row1.getId()));
        all.forEach(
            row -> assertEquals(MappingFileRow.Resolution.DELETION_NOT_ALLOWED, row.getResolution())
        );

        MappingFile fileById = fileRepo.findById(file.getId());
        assertTrue(fileById.isCompleted());
    }

    @Test
    public void testNoOffersFound() {
        createModel();

        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(2).build());
        var row1 = fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(10193550).ssku("669744"
        ).mskuId(5144704L).build());
        var row2 = fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(10193510).ssku("669741"
        ).mskuId(5144704L).build());
        var row3 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019351).ssku("669888").mskuId(5144704L).build());

        failFastExecution();

        List<MappingFileRow> all = fileRowRepo.findByIds(List.of(row1.getId(), row2.getId(), row3.getId()));
        all.forEach(
            row -> assertEquals(MappingFileRow.Resolution.NO_SUCH_OFFER, row.getResolution())
        );

        MappingFile fileById = fileRepo.findById(file.getId());
        assertTrue(fileById.isCompleted());
    }

    @Test
    public void noAutoApproveSmartmatcherWhenMappingWasDeletedBefore() {
        String login = "robot-smart-matcher";
        var file = fileRepo.insert(MappingFile.builder().filename("filename.tsv").size(2).login(login).build());
        storageKeyValueService.putValue(MappingFileImportExecutor.SMART_MATCHER_ROBOT, login);

        createModel();

        var row1 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(10339600).ssku("669741").mskuId(5144704L).build());

        failFastExecution();

        var updatedOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertFalse(updatedOffer.isAutoApprovedMapping());
        assertFalse(updatedOffer.hasApprovedSkuMapping());
        assertNull(updatedOffer.getApprovedSkuMappingConfidence());
        var firstRow = fileRowRepo.findAll().get(0);
        assertEquals(firstRow.getResolution(), MappingFileRow.Resolution.MAPPING_WAS_DELETED);
    }

    @Test
    public void testRewrites() {
        setupOffers();
        modelCache.addModel(createModel(100500));
        modelCache.addModel(createModel(888));

        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(2).build());
        var rewritingRow = MappingFileRow.builder()
            .fileId(file.getId())
            .supplierId(1019355)
            .ssku("669744")
            .mskuId(888L)
            .rewrite(true)
            .build();
        var nonRewritingRow =
            MappingFileRow.builder().fileId(file.getId()).supplierId(1019351).ssku("669741").mskuId(888L).build();
        rewritingRow = fileRowRepo.insert(rewritingRow);
        nonRewritingRow = fileRowRepo.insert(nonRewritingRow);

        var offers = offerRepository.findOffers(
            new OffersFilter().setBusinessSkuKeysInternal(
                List.of(rewritingRow.asBusinessSkuKey(), nonRewritingRow.asBusinessSkuKey())));
        offers.forEach(offer -> {
            var mapping = new Offer.Mapping(100500, LocalDateTime.now(), Offer.SkuType.MARKET);
            var context = new SkuMappingContext()
                .setSkuMapping(mapping)
                .setModelId(100500L);
            mappingActionService.CONTENT.setSkuMapping(offer, context);
        });
        offerRepository.updateOffers(offers);

        var lastRow = fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019351).ssku(
            "669888").mskuId(888L).build());

        failFastExecution();
        List<MappingFileRow> all = fileRowRepo.findByIds(List.of(rewritingRow.getId(), nonRewritingRow.getId(),
            lastRow.getId()));
        assertTrue(all.get(0).isProcessed());
        assertFalse(all.get(1).isProcessed());
        assertTrue(all.get(2).isProcessed());

        var allOffers = offerRepository.findAll();
        assertEquals(Long.valueOf(888L), allOffers.get(0).getApprovedSkuId());
        assertNotEquals(Long.valueOf(888L), allOffers.get(1).getApprovedSkuId());
        assertEquals(Long.valueOf(888L), allOffers.get(2).getApprovedSkuId());
    }

    @Test
    public void testSmartMatchersFile() {
        String login = "robot-smart-matcher";
        var file = fileRepo.insert(MappingFile.builder().filename("filename.tsv").size(2).login(login).build());
        storageKeyValueService.putValue(MappingFileImportExecutor.SMART_MATCHER_ROBOT, login);

        createModel();

        var row1 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019355).ssku("669744").mskuId(5144704L).build());
        var row2 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019351).ssku("669741").mskuId(5144704L).build());
        var row3 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019351).ssku("669888").mskuId(5144704L).build());
        var row4 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019355).ssku("669744").mskuId(5144704L).build());


        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNotNull(row1.getProcessed());
        assertNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.DONE, row1.getResolution());

        var updatedOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertNotNull(updatedOffer.getAutoApprovedMappingSource());
        assertEquals(Offer.AutoApprovedMappingSource.SMART_MATCHER, updatedOffer.getAutoApprovedMappingSource());

        row2 = fileRowRepo.findById(row2.getId());
        assertNotNull(row2.getSkipped());
        assertNull(row2.getProcessed());
        assertEquals(MappingFileRow.Resolution.ACCEPTANCE_TRASH, row2.getResolution());

        row3 = fileRowRepo.findById(row3.getId());
        assertNotNull(row3.getSkipped());
        assertNull(row3.getProcessed());
        assertEquals(MappingFileRow.Resolution.NO_SUCH_OFFER, row3.getResolution());

        row4 = fileRowRepo.findById(row4.getId());
        assertNotNull(row4.getSkipped());
        assertNull(row4.getProcessed());
        assertEquals(MappingFileRow.Resolution.REPEATED, row4.getResolution());

        storageKeyValueService.putValue(MappingFileImportExecutor.SMART_MATCHER_ROBOT, null);
    }

    public void testOfferDuplicatesFile() {
        String login = "robot-offer-duplicates";
        var file = fileRepo.insert(MappingFile.builder().filename("filename.tsv").size(2).login(login).build());
        storageKeyValueService.putValue(MappingFileImportExecutor.OFFER_DUPLICATES_ROBOT, login);

        createModel();

        var row1 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019355).ssku("669744").mskuId(5144704L).build());
        var row2 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019351).ssku("669741").mskuId(5144704L).build());
        var row3 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019351).ssku("669888").mskuId(5144704L).build());
        var row4 =
            fileRowRepo.insert(MappingFileRow.builder().fileId(file.getId()).supplierId(1019355).ssku("669744").mskuId(5144704L).build());


        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNotNull(row1.getProcessed());
        assertNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.DONE, row1.getResolution());

        var updatedOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertNotNull(updatedOffer.getAutoApprovedMappingSource());
        assertEquals(Offer.AutoApprovedMappingSource.OFFER_DUPLICATE, updatedOffer.getAutoApprovedMappingSource());

        row2 = fileRowRepo.findById(row2.getId());
        assertNotNull(row2.getSkipped());
        assertNull(row2.getProcessed());
        assertEquals(MappingFileRow.Resolution.ACCEPTANCE_TRASH, row2.getResolution());

        row3 = fileRowRepo.findById(row3.getId());
        assertNotNull(row3.getSkipped());
        assertNull(row3.getProcessed());
        assertEquals(MappingFileRow.Resolution.NO_SUCH_OFFER, row3.getResolution());

        row4 = fileRowRepo.findById(row4.getId());
        assertNotNull(row4.getSkipped());
        assertNull(row4.getProcessed());
        assertEquals(MappingFileRow.Resolution.REPEATED, row4.getResolution());

        storageKeyValueService.putValue(MappingFileImportExecutor.OFFER_DUPLICATES_ROBOT, null);
    }

    @Test
    public void testOfferNeedInfoComments() {
        var offer = offerRepository.findOfferByBusinessSkuKey(createOffer().getBusinessSkuKey())
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO)
            .setContentComments(new ContentComment(ContentCommentType.INCORRECT_INFORMATION));
        offerRepository.updateOffer(offer);

        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(2).build());

        createModel();

        var row1 = fileRowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).supplierId(1019355).ssku("669744").mskuId(5144704L).build());

        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNotNull(row1.getProcessed());
        assertNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.DONE, row1.getResolution());

        var updatedOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertNotNull(updatedOffer.getAutoApprovedMappingSource());
        assertEquals(Offer.AutoApprovedMappingSource.XLS_MAPPING, updatedOffer.getAutoApprovedMappingSource());
        assertEquals(Offer.ProcessingStatus.PROCESSED, updatedOffer.getProcessingStatus());
        assertThat(updatedOffer.getContentComments()).isEmpty();
    }

    @Test
    public void testVerificationAndConsecutiveApplication() {
        storageKeyValueService.putValue(MappingFileImportExecutor.VERIFICATION_PHASE_ENABLED, true);

        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(2).build());
        createModel();
        var row1 = fileRowRepo.insert(
            MappingFileRow.builder()
                .fileId(file.getId())
                .supplierId(1019355)
                .ssku("669744")
                .mskuId(5144704L)
                .build());

        failFastExecution();

        file = fileRepo.findById(file.getId());
        assertEquals(MappingFile.VerificationStatus.TO_BE_APPROVED, file.getVerificationStatus());
        file.setVerificationStatus(MappingFile.VerificationStatus.APPROVED_IGNORING_WARNINGS);
        fileRepo.update(file);

        var offerToUpdate =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertFalse(offerToUpdate.hasApprovedSkuMapping());
        assertNull(offerToUpdate.getAutoApprovedMappingSource());

        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNotNull(row1.getProcessed());
        assertNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.DONE, row1.getResolution());

        var updatedOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertNotNull(updatedOffer.getAutoApprovedMappingSource());
        assertEquals(Offer.AutoApprovedMappingSource.XLS_MAPPING, updatedOffer.getAutoApprovedMappingSource());
    }

    @Test
    public void testVerificationExcludingWarningRows() {
        storageKeyValueService.putValue(MappingFileImportExecutor.VERIFICATION_PHASE_ENABLED, true);

        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(1).build());
        createModel();
        var row1 = fileRowRepo.insert(
            MappingFileRow.builder()
                .fileId(file.getId())
                .supplierId(1019355)
                .ssku("669744")
                .mskuId(5144704L)
                .build());

        failFastExecution();

        file = fileRepo.findById(file.getId());
        assertEquals(MappingFile.VerificationStatus.TO_BE_APPROVED, file.getVerificationStatus());
        file.setVerificationStatus(MappingFile.VerificationStatus.APPROVED_EXCLUDING_WARNINGS);
        fileRepo.update(file);

        var offerToUpdate =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertFalse(offerToUpdate.hasApprovedSkuMapping());
        assertNull(offerToUpdate.getAutoApprovedMappingSource());

        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNull(row1.getProcessed());
        assertNotNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.EXCLUDING_WARNINGS, row1.getResolution());

        var updatedOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertFalse(updatedOffer.hasApprovedSkuMapping());
        assertNull(updatedOffer.getAutoApprovedMappingSource());

        file = fileRepo.findById(file.getId());
        assertNotNull(file.getCompleted());
    }

    @Test
    public void verificationFailTest() {
        storageKeyValueService.putValue(MappingFileImportExecutor.VERIFICATION_PHASE_ENABLED, true);

        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(2).build());
        createModel();
        var row1 = fileRowRepo.insert(
            MappingFileRow.builder()
                .fileId(file.getId())
                .supplierId(1019355)
                .ssku("669744111")
                .mskuId(5144704L)
                .build());

        failFastExecution();

        file = fileRepo.findById(file.getId());
        assertEquals(MappingFile.VerificationStatus.REJECTED, file.getVerificationStatus());

        row1 = fileRowRepo.findById(row1.getId());
        assertEquals(MappingFileRow.Resolution.NO_SUCH_OFFER, row1.getResolution());
    }

    @Test
    public void testOfferDuplicatesFileWithVerificationAndAutoApprove() {
        String login = "robot-offer-duplicates";
        var file = fileRepo.insert(MappingFile.builder().filename("filename.tsv").size(1).login(login).build());
        storageKeyValueService.putValue(MappingFileImportExecutor.VERIFICATION_PHASE_ENABLED, true);
        storageKeyValueService.putValue(MappingFileImportExecutor.APPROVE_ROBO_UPLOADS, true);
        storageKeyValueService.putValue(MappingFileImportExecutor.OFFER_DUPLICATES_ROBOT, login);

        createModel();

        var row1 = fileRowRepo.insert(
            MappingFileRow.builder()
                .fileId(file.getId())
                .supplierId(1019355)
                .ssku("669744")
                .mskuId(5144704L)
                .build());

        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNull(row1.getProcessed());
        assertNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.OK, row1.getResolution());

        file = fileRepo.findById(file.getId());
        assertEquals(MappingFile.VerificationStatus.AUTO_APPROVED, file.getVerificationStatus());
        failFastExecution();

        var updatedOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertNotNull(updatedOffer.getAutoApprovedMappingSource());
        assertEquals(Offer.AutoApprovedMappingSource.OFFER_DUPLICATE, updatedOffer.getAutoApprovedMappingSource());

        storageKeyValueService.putValue(MappingFileImportExecutor.OFFER_DUPLICATES_ROBOT, null);
    }

    @Test
    public void testCskuOfferInContentProcessingAfterRemapping() {
        var modelId = 5144704L;
        var businessId = 10339600;

        var offer = createOfferWithApprovedMapping(1L, Offer.SkuType.PARTNER20)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .setContentProcessed(false)
            .setMarketSpecificContentHash(1L)
            .setMarketSpecificContentHashSent(1L);

        offerRepository.insertOffer(offer);
        offer = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());

        var file = fileRepo.insert(MappingFile.builder().filename("filename.xls").size(2).build());

        createModel(modelId, SimpleModel.ModelType.SKU, SimpleModel.ModelQuality.PARTNER);

        var row1 = fileRowRepo.insert(
            MappingFileRow.builder()
                .fileId(file.getId())
                .supplierId(offer.getBusinessId())
                .ssku(offer.getShopSku())
                .mskuId(modelId)
                .rewrite(true)
                .build()
        );

        failFastExecution();

        row1 = fileRowRepo.findById(row1.getId());
        assertNotNull(row1.getProcessed());
        assertNull(row1.getSkipped());
        assertEquals(MappingFileRow.Resolution.DONE, row1.getResolution());

        var updatedOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(row1.getSupplierId(), row1.getSsku()))
                .get(0);
        assertNotNull(updatedOffer.getAutoApprovedMappingSource());
        assertEquals(Offer.AutoApprovedMappingSource.XLS_MAPPING, updatedOffer.getAutoApprovedMappingSource());
        assertEquals(Offer.ProcessingStatus.CONTENT_PROCESSING, updatedOffer.getProcessingStatus());
    }


    private void setupOffers() {
        offerRepository.insertOffers(List.of(
            new Offer().setBusinessId(1019355).setShopSku("669744"),
            new Offer().setBusinessId(1019351).setShopSku("669741"),
            new Offer().setBusinessId(1019351).setShopSku("669888")
        ));
    }

    private void failFastExecution() {
        executor.updateFeatureFlags();
        var uncompletedFiles = fileRepo.findUncompletedFiles();
        for (var file : uncompletedFiles) {
            executor.processFile(file);
            executor.tryToComplete(file);
        }
    }

    private Offer createOffer() {
        Offer offer = new Offer();
        offer.setId(1L);
        offer.setBusinessId(1019355);
        offer.setShopSku("669744");
        offer.setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);
        return offer;
    }

    private Offer createOfferWithDeletedMapping() {
        var suggest = new Offer.Mapping(0, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = new Offer();
        offer.setId(4L);
        offer.setTitle("Title");
        offer.setBusinessId(10339600);
        offer.setSuggestSkuMapping(suggest);

        offer.setSuggestSkuMappingType(SkuBDApi.SkutchType.SKUTCH_BY_SMARTMATCHER);
        offer.setShopSku("669741");
        offer.setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);
        offer.updateApprovedSkuMapping(suggest, Offer.MappingConfidence.CONTENT);
        return offer;
    }

    private Offer createOfferWithApprovedMapping(Long skuId, Offer.SkuType skuType) {
        var suggest = new Offer.Mapping(skuId, LocalDateTime.now(), skuType);
        Offer offer = new Offer();
        offer.setId(6L);
        offer.setTitle("Title");
        offer.setBusinessId(10339600);
        offer.setSuggestSkuMapping(suggest);

        offer.setSuggestSkuMappingType(SkuBDApi.SkutchType.SKUTCH_BY_SMARTMATCHER);
        offer.setShopSku("669966");
        offer.setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);
        offer.updateApprovedSkuMapping(suggest, Offer.MappingConfidence.CONTENT);
        return offer;
    }

    private Offer createInvalidOffer() {
        Offer offer = new Offer();
        offer.setId(2L);
        offer.setBusinessId(1019351);
        offer.setShopSku("669741");
        offer.setAcceptanceStatusInternal(Offer.AcceptanceStatus.TRASH);
        return offer;
    }

    private Model createModel() {
        long mId = 5144704L;
        return createModel(mId);
    }

    private Model createModel(long mId) {
        return createModel(mId, SimpleModel.ModelType.GURU, SimpleModel.ModelQuality.OPERATOR);
    }

    private Model createModel(long mId, SimpleModel.ModelType modelType, SimpleModel.ModelQuality modelQuality) {
        Model model = new Model()
            .setDeleted(false)
            .setPublishedOnMarket(true)
            .setId(mId)
            .setCategoryId(1)
            .setModelType(modelType)
            .setModelQuality(modelQuality);

        modelCache.addModel(model);
        return model;
    }
}
