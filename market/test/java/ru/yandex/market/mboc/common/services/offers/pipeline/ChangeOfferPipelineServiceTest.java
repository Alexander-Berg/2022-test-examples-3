package ru.yandex.market.mboc.common.services.offers.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mboc.common.services.offers.pipeline.ChangeOfferPipelineService.CATEGORY_IS_GOOD;
import static ru.yandex.market.mboc.common.services.offers.pipeline.ChangeOfferPipelineService.CATEGORY_IS_NOT_GOOD;
import static ru.yandex.market.mboc.common.services.offers.pipeline.ChangeOfferPipelineService.CATEGORY_NOT_FOUND;
import static ru.yandex.market.mboc.common.services.offers.pipeline.ChangeOfferPipelineService.NEW_PIPELINE_DISABLED;
import static ru.yandex.market.mboc.common.services.offers.pipeline.ChangeOfferPipelineService.NO_OFFERS;
import static ru.yandex.market.mboc.common.services.offers.pipeline.ChangeOfferPipelineService.OFFERS_UPDATED;
import static ru.yandex.market.mboc.common.services.offers.pipeline.ChangeOfferPipelineService.SUPPLIER_NOT_FOUND;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_SUPPLIER_ID;

/**
 * @author danfertev
 * @since 07.08.2019
 */
public class ChangeOfferPipelineServiceTest extends BaseDbTestClass {
    private static final long GOOD_CATEGORY = 1L;
    private static final long NO_GOOD_CATEGORY = 2L;
    private static final long NOT_FOUND_CATEGORY = 3L;
    private static final long SKU_ID = 2L;
    private static final String OFFER_RESULTS_FIELD = "offerResults";

    private ChangeOfferPipelineService changeOfferPipelineService;

    @Autowired
    private SupplierRepository supplierRepositoryInternal;
    private SupplierRepository supplierRepository;

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;

    private OffersProcessingStatusService offersProcessingStatusServiceSpied;

    @Before
    public void setUp() {
        supplierRepository = Mockito.spy(supplierRepositoryInternal);
        when(supplierRepository.findByCategoryIds(anyList(), anyBoolean(), any(SupplierRepository.ByType.class)))
            .then(args -> supplierRepository.findByType(args.getArgument(2)));

        var supplierService = new SupplierService(supplierRepository);
        var categoryCachingServiceMock = new CategoryCachingServiceMock();
        var categoryInfoRepository = new CategoryInfoRepositoryMock(new MboUsersRepositoryMock());
        var needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));

        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        var modelStorageCachingService = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository);

        var categoryKnowledgeService = new CategoryKnowledgeServiceMock();

        var offersProcessingStatusService = new OffersProcessingStatusService(
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
        offersProcessingStatusServiceSpied = Mockito.spy(offersProcessingStatusService);

        changeOfferPipelineService = new ChangeOfferPipelineService(
            supplierRepository,
            offerRepository,
            offerBatchProcessor,
            categoryCachingServiceMock,
            offersProcessingStatusServiceSpied
        );
        categoryCachingServiceMock.addCategories(
            new Category().setCategoryId(GOOD_CATEGORY)
                .setAcceptGoodContent(true)
                .setAcceptContentFromWhiteShops(true)
                .setHasKnowledge(true),
            new Category().setCategoryId(NO_GOOD_CATEGORY)
                .setAcceptGoodContent(false)
                .setAcceptContentFromWhiteShops(false)
                .setHasKnowledge(true)
        );
        categoryKnowledgeService.addCategory(GOOD_CATEGORY);
        categoryKnowledgeService.addCategory(NO_GOOD_CATEGORY);
    }

    @Test
    public void testForceGoodContent() {
        Supplier noGoodSupplier = OfferTestUtils.simpleSupplier()
            .setId(TEST_SUPPLIER_ID + 1)
            .setNewContentPipeline(false);
        supplierRepository.insert(noGoodSupplier);
        int noGoodSupplierId = noGoodSupplier.getId();

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.REOPEN);
        Offer offer3 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION);
        Offer offer4 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        Offer offer5 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        offerRepository.insertOffers(offer1, offer2, offer3, offer4, offer5);

        List<Long> offerIds = Stream.of(offer1, offer2, offer3, offer4, offer5)
            .map(Offer::getId).collect(Collectors.toList());
        List<Offer> offers = offerRepository.getOffersByIds(offerIds);

        Offer offer1copy = offer1.copy();
        Offer offer2copy = offer2.copy();
        Offer offer3copy = offer3.copy();
        Offer offer4copy = offer4.copy();
        Offer offer5copy = offer5.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.forceGoodContent(offers, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offer1copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer2copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer3copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer4copy, Offer.ProcessingStatus.NEED_CONTENT),
            new OfferResult(offer5copy, Offer.ProcessingStatus.NEED_CONTENT)
        );

        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer3, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer4, Offer.ProcessingStatus.NEED_CONTENT);
        assertOfferProcessingStatus(offer5, Offer.ProcessingStatus.NEED_CONTENT);

        assertThat(offerRepository.getOffersByIds(offerIds))
            .allMatch(o -> o.getForceGoodContentStatus() == Offer.ForceGoodContentStatus.FORCE_GOOD_CONTENT);
    }

    @Test
    public void testForceNotGoodContent() {
        Supplier noGoodSupplier = OfferTestUtils.simpleSupplier()
            .setId(TEST_SUPPLIER_ID + 1)
            .setNewContentPipeline(false);
        Supplier goodSupplier = OfferTestUtils.simpleSupplier()
            .setId(TEST_SUPPLIER_ID + 2)
            .setNewContentPipeline(true);
        supplierRepository.insertBatch(noGoodSupplier, goodSupplier);
        int goodSupplierId = goodSupplier.getId();
        int noGoodSupplierId = noGoodSupplier.getId();

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(goodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        Offer offer3 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        Offer offer4 = OfferTestUtils.nextOffer()
            .setBusinessId(goodSupplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        Offer offer5 = OfferTestUtils.nextOffer()
            .setBusinessId(goodSupplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        offerRepository.insertOffers(offer1, offer2, offer3, offer4, offer5);

        List<Long> offerIds = Stream.of(offer1, offer2, offer3, offer4, offer5)
            .map(Offer::getId).collect(Collectors.toList());
        List<Offer> offers = offerRepository.getOffersByIds(offerIds);

        Offer offer1copy = offer1.copy();
        Offer offer2copy = offer2.copy();
        Offer offer3copy = offer3.copy();
        Offer offer4copy = offer4.copy();
        Offer offer5copy = offer5.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.forceNotGoodContent(offers, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offer1copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer2copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer3copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer4copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer5copy, Offer.ProcessingStatus.IN_PROCESS)
        );

        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer3, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer4, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer5, Offer.ProcessingStatus.IN_PROCESS);

        assertThat(offerRepository.getOffersByIds(offerIds))
            .allMatch(o -> o.getForceGoodContentStatus() == Offer.ForceGoodContentStatus.FORCE_NOT_GOOD_CONTENT);
    }

    @Test
    public void testUndoForceGoodContent() {
        Supplier noGoodSupplier = OfferTestUtils.simpleSupplier()
            .setId(TEST_SUPPLIER_ID + 1)
            .setNewContentPipeline(false);
        Supplier goodSupplier = OfferTestUtils.simpleSupplier()
            .setId(TEST_SUPPLIER_ID + 2)
            .setNewContentPipeline(true);
        supplierRepository.insertBatch(noGoodSupplier, goodSupplier);
        int goodSupplierId = goodSupplier.getId();
        int noGoodSupplierId = noGoodSupplier.getId();

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(goodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        Offer offer3 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        Offer offer4 = OfferTestUtils.nextOffer()
            .setBusinessId(goodSupplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        List<Offer> offers = Stream.of(offer1, offer2, offer3, offer4)
            .map(offer -> offer.setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_GOOD_CONTENT))
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);

        List<Long> offerIds = offers.stream().map(Offer::getId).collect(Collectors.toList());
        offers = offerRepository.getOffersByIds(offerIds);

        Offer offer1copy = offer1.copy();
        Offer offer2copy = offer2.copy();
        Offer offer3copy = offer3.copy();
        Offer offer4copy = offer4.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.undoAnyForceGoodContent(offers, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offer1copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer2copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer3copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer4copy, Offer.ProcessingStatus.IN_CLASSIFICATION)
        );

        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer3, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer4, Offer.ProcessingStatus.IN_CLASSIFICATION);

        assertThat(offerRepository.getOffersByIds(offerIds))
            .allMatch(o -> o.getForceGoodContentStatus() == null);
    }

    @Test
    public void testUndoForceNotGoodContent() {
        Supplier noGoodSupplier = OfferTestUtils.simpleSupplier()
            .setId(TEST_SUPPLIER_ID + 1)
            .setNewContentPipeline(false);
        Supplier goodSupplier = OfferTestUtils.simpleSupplier()
            .setId(TEST_SUPPLIER_ID + 2)
            .setNewContentPipeline(true);
        supplierRepository.insertBatch(noGoodSupplier, goodSupplier);
        int goodSupplierId = goodSupplier.getId();
        int noGoodSupplierId = noGoodSupplier.getId();

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(goodSupplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        Offer offer3 = OfferTestUtils.nextOffer()
            .setBusinessId(noGoodSupplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        Offer offer4 = OfferTestUtils.nextOffer()
            .setBusinessId(goodSupplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        List<Offer> offers = Stream.of(offer1, offer2, offer3, offer4)
            .map(offer -> offer.setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_NOT_GOOD_CONTENT))
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);

        List<Long> offerIds = offers.stream().map(Offer::getId).collect(Collectors.toList());
        offers = offerRepository.getOffersByIds(offerIds);

        Offer offer1copy = offer1.copy();
        Offer offer2copy = offer2.copy();
        Offer offer3copy = offer3.copy();
        Offer offer4copy = offer4.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.undoAnyForceGoodContent(offers, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offer1copy, Offer.ProcessingStatus.IN_PROCESS),
            new OfferResult(offer2copy, Offer.ProcessingStatus.IN_PROCESS),
            new OfferResult(offer3copy, Offer.ProcessingStatus.IN_PROCESS),
            new OfferResult(offer4copy, Offer.ProcessingStatus.NEED_CONTENT)
        );

        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.IN_PROCESS);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.IN_PROCESS);
        assertOfferProcessingStatus(offer3, Offer.ProcessingStatus.IN_PROCESS);
        assertOfferProcessingStatus(offer4, Offer.ProcessingStatus.NEED_CONTENT);

        assertThat(offerRepository.getOffersByIds(offerIds))
            .allMatch(o -> o.getForceGoodContentStatus() == null);
    }

    @Test
    public void enableForSupplierNotFound() {
        Supplier supplier = OfferTestUtils.simpleSupplier();
        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForSupplier(supplier.getId(), true);

        verify(offersProcessingStatusServiceSpied, never()).processOffers(anyCollection());
        assertResult(result, ChangeOfferPipelineResult.error(SUPPLIER_NOT_FOUND));
    }

    @Test
    public void enableForSupplierNewPipelineDisabledHasOffers() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(false);
        supplierRepository.insert(supplier);
        int supplierId = supplier.getId();
        Offer offer = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        offerRepository.insertOffers(offer);
        Offer offerCopy = offer.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForSupplier(supplier.getId(), true);

        assertResult(result, ChangeOfferPipelineResult.error(NEW_PIPELINE_DISABLED),
            new OfferResult(offerCopy, Offer.ProcessingStatus.IN_CLASSIFICATION)
        );
        assertOfferProcessingStatus(offer, Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void enableForSupplierNewPipelineDisabledNoOffers() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(false);
        supplierRepository.insert(supplier);
        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForSupplier(supplier.getId(), true);

        assertResult(result, ChangeOfferPipelineResult.error(NEW_PIPELINE_DISABLED));
    }

    @Test
    public void enableForSupplierNewPipelineDisabledWriteFalse() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(false);
        supplierRepository.insert(supplier);
        int supplierId = supplier.getId();
        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.REOPEN);
        offerRepository.insertOffers(offer1, offer2);

        Offer offer1copy = offer1.copy();
        Offer offer2copy = offer2.copy();

        doCallRealMethod()
            .when(offersProcessingStatusServiceSpied)
            .processOffers(anyCollection());

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForSupplier(supplierId, false);

        assertResult(result, ChangeOfferPipelineResult.error(NEW_PIPELINE_DISABLED),
            new OfferResult(offer1copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer2copy, Offer.ProcessingStatus.IN_CLASSIFICATION)
        );
        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.OPEN);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.REOPEN);
    }

    @Test
    public void enableForSupplierNoApplicableOffersInRepository() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        supplierRepository.insert(supplier);
        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForSupplier(supplier.getId(), true);

        assertResult(result, ChangeOfferPipelineResult.ok(NO_OFFERS));
    }

    @Test
    public void enableForSupplierFetchOffersWithAllowedStatusAndNoApprovedMapping() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED);
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.REOPEN);
        Offer offer3 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        Offer offer4 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_INFO);
        Offer offer5 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION);
        offerRepository.insertOffers(offer1, offer2, offer3, offer4, offer5);

        Offer offer2copy = offer2.copy();
        Offer offer3copy = offer3.copy();
        Offer offer5copy = offer5.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForSupplier(supplierId, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offer2copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer3copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer5copy, Offer.ProcessingStatus.IN_CLASSIFICATION)
        );
        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.PROCESSED);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer3, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer5, Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    @Test
    public void enableForSupplierSkipNeedInfoInNotGoodCategories() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_INFO);
        offerRepository.insertOffers(offer1);

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForSupplier(supplierId, true);

        assertResult(result, ChangeOfferPipelineResult.ok(NO_OFFERS));
    }

    @Test
    public void enableForSupplierMoveInProcess() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        offerRepository.insertOffers(offer1, offer2);
        Offer offer1Copy = offer1.copy();
        Offer offer2Copy = offer2.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForSupplier(supplierId, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offer1Copy, Offer.ProcessingStatus.NEED_CONTENT),
            new OfferResult(offer2Copy, Offer.ProcessingStatus.IN_PROCESS)
        );
        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.NEED_CONTENT);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.IN_PROCESS);
    }

    @Test
    public void enableForSupplierMoveInClassification() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION)
            .resetSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierSkuMapping(OfferTestUtils.mapping(SKU_ID));
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION)
            .resetSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
            .setSupplierSkuMapping(OfferTestUtils.mapping(SKU_ID));
        Offer offer3 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION);
        offerRepository.insertOffers(offer1, offer2, offer3);
        Offer offer1Copy = offer1.copy();
        Offer offer2Copy = offer2.copy();
        Offer offer3Copy = offer3.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForSupplier(supplierId, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offer1Copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer2Copy, Offer.ProcessingStatus.IN_CLASSIFICATION),
            new OfferResult(offer3Copy, Offer.ProcessingStatus.IN_CLASSIFICATION)
        );
        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer3, Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    @Test
    public void enableForCategoryNotFound() {
        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForCategory(NOT_FOUND_CATEGORY, true);

        verify(offersProcessingStatusServiceSpied, never()).processOffers(anyCollection());
        assertResult(result, ChangeOfferPipelineResult.error(CATEGORY_NOT_FOUND));
    }

    @Test
    public void enableForCategoryNoNewPipelineSuppliers() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(false);
        supplierRepository.insert(supplier);

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForCategory(GOOD_CATEGORY, true);

        verify(offersProcessingStatusServiceSpied, never()).processOffers(anyCollection());
        assertResult(result, ChangeOfferPipelineResult.ok(NO_OFFERS));
    }

    @Test
    public void enableForCategoryNewPipelineSupplierNoOffers() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        supplierRepository.insert(supplier);

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForCategory(GOOD_CATEGORY, true);

        assertResult(result, ChangeOfferPipelineResult.ok(NO_OFFERS));
    }

    @Test
    public void enableForCategoryGoodContentDisabled() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.REOPEN)
            .setSuggestSkuMapping(OfferTestUtils.mapping(SKU_ID));
        offerRepository.insertOffers(offer);

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForCategory(NO_GOOD_CATEGORY, true);

        verify(offersProcessingStatusServiceSpied, never()).processOffers(anyCollection());
        assertResult(result, ChangeOfferPipelineResult.error(CATEGORY_IS_NOT_GOOD),
            new OfferResult(offer)
        );
        assertOfferProcessingStatus(offer, Offer.ProcessingStatus.REOPEN);
    }

    @Test
    public void enableForCategoryFetchCorrectOffers() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED);
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .setProcessingStatusInternal(Offer.ProcessingStatus.REOPEN);
        Offer offer3 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        Offer offer4 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_INFO);
        Offer offer5 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION);
        Offer offer6 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.REOPEN);
        offerRepository.insertOffers(offer1, offer2, offer3, offer4, offer5, offer6);

        Offer offer6copy = offer6.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForCategory(GOOD_CATEGORY, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offer6copy, Offer.ProcessingStatus.IN_CLASSIFICATION)
        );
        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.PROCESSED);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.REOPEN);
        assertOfferProcessingStatus(offer3, Offer.ProcessingStatus.OPEN);
        assertOfferProcessingStatus(offer4, Offer.ProcessingStatus.NEED_INFO);
        assertOfferProcessingStatus(offer5, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer6, Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    @Test
    public void enableForCategoryMoveInProcess() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        offerRepository.insertOffers(offer);
        Offer offerCopy = offer.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.enableForCategory(GOOD_CATEGORY, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offerCopy, Offer.ProcessingStatus.NEED_CONTENT)
        );
        assertOfferProcessingStatus(offer, Offer.ProcessingStatus.NEED_CONTENT);
    }

    private void assertResult(ChangeOfferPipelineResult actual,
                              ChangeOfferPipelineResult expected,
                              OfferResult... expectedResults) {
        assertThat(actual).isEqualToIgnoringGivenFields(expected, OFFER_RESULTS_FIELD);
        assertThat(actual.getOfferResults()).containsExactlyInAnyOrder(expectedResults);
    }

    @Test
    public void disableForCategoryNotFound() {
        ChangeOfferPipelineResult result = changeOfferPipelineService.disableForCategory(NOT_FOUND_CATEGORY, true);

        verify(offersProcessingStatusServiceSpied, never()).processOffers(anyCollection());
        assertResult(result, ChangeOfferPipelineResult.error(CATEGORY_NOT_FOUND));
    }

    @Test
    public void disableForCategoryNoNewPipelineSuppliers() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(false);
        supplierRepository.insert(supplier);

        ChangeOfferPipelineResult result = changeOfferPipelineService.disableForCategory(GOOD_CATEGORY, true);

        verify(offersProcessingStatusServiceSpied, never()).processOffers(anyCollection());
        assertResult(result, ChangeOfferPipelineResult.ok(NO_OFFERS));
    }

    @Test
    public void disableForCategoryGoodContentEnable() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT)
            .setSuggestSkuMapping(OfferTestUtils.mapping(SKU_ID));
        offerRepository.insertOffers(offer);

        ChangeOfferPipelineResult result = changeOfferPipelineService.disableForCategory(GOOD_CATEGORY, true);

        verify(offersProcessingStatusServiceSpied, never()).processOffers(anyCollection());
        assertResult(result, ChangeOfferPipelineResult.error(CATEGORY_IS_GOOD),
            new OfferResult(offer)
        );
        assertOfferProcessingStatus(offer, Offer.ProcessingStatus.NEED_CONTENT);
    }

    @Test
    public void disableForCategoryFetchCorrectOffers() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer1 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED);
        Offer offer2 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .setProcessingStatusInternal(Offer.ProcessingStatus.REOPEN);
        Offer offer3 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        Offer offer4 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);
        Offer offer5 = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION);
        offerRepository.insertOffers(offer1, offer2, offer3, offer4, offer5);

        Offer offer4copy = offer4.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.disableForCategory(NO_GOOD_CATEGORY, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offer4copy, Offer.ProcessingStatus.IN_CLASSIFICATION)
        );
        assertOfferProcessingStatus(offer1, Offer.ProcessingStatus.PROCESSED);
        assertOfferProcessingStatus(offer2, Offer.ProcessingStatus.REOPEN);
        assertOfferProcessingStatus(offer3, Offer.ProcessingStatus.OPEN);
        assertOfferProcessingStatus(offer4, Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertOfferProcessingStatus(offer5, Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    @Test
    public void disableForCategoryRemoveContentActiveError() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        int supplierId = supplier.getId();
        supplierRepository.insert(supplier);

        Offer offer = OfferTestUtils.nextOffer()
            .setBusinessId(supplierId)
            .setCategoryIdForTests(NO_GOOD_CATEGORY, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT);

        offerRepository.insertOffers(offer);

        Offer offerCopy = offer.copy();

        ChangeOfferPipelineResult result = changeOfferPipelineService.disableForCategory(NO_GOOD_CATEGORY, true);

        assertResult(result, ChangeOfferPipelineResult.ok(OFFERS_UPDATED),
            new OfferResult(offerCopy, Offer.ProcessingStatus.IN_CLASSIFICATION)
        );

        Offer offerInDB = offerRepository.getOfferById(offer.getId());
        MbocAssertions.assertThat(offerInDB)
            .hasContentStatusActiveError(null);
    }

    private void assertOfferProcessingStatus(Offer offer, Offer.ProcessingStatus expectedStatus) {
        assertThat(offerRepository.getOfferById(offer.getId()).getProcessingStatus()).isEqualTo(expectedStatus);
    }
}
