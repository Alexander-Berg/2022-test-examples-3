package ru.yandex.market.mboc.tms.executors;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.logisticsparams.repository.SkuLogisticParamsRepository;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.OfferStatService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.pipeline.ChangeOfferPipelineService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author danfertev
 * @since 12.10.2019
 */
public class ChangeOfferPipelineExecutorTest extends BaseDbTestClass {
    private static final Supplier SUPPLIER_1P = OfferTestUtils.simpleSupplier()
        .setId(1)
        .setType(MbocSupplierType.FIRST_PARTY);
    private static final Supplier SUPPLIER_3P_OLD = OfferTestUtils.simpleSupplier()
        .setId(2)
        .setType(MbocSupplierType.THIRD_PARTY)
        .setNewContentPipeline(false);
    private static final Supplier SUPPLIER_3P_NEW = OfferTestUtils.simpleSupplier()
        .setId(3)
        .setType(MbocSupplierType.THIRD_PARTY)
        .setNewContentPipeline(true);
    private static final Supplier SUPPLIER_WHITE_NEW = OfferTestUtils.simpleSupplier()
        .setId(4)
        .setType(MbocSupplierType.MARKET_SHOP)
        .setNewContentPipeline(true);

    private static final long CATEGORY_ID_GOOD = 4L;
    private static final long CATEGORY_ID_NO_GOOD = 5L;
    private static final long CATEGORY_ID_NO_GOOD_NO_KNOWLEDGE = 6L;
    private static final long SKU_ID = 7L;

    private ChangeOfferPipelineExecutor executor;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private SkuLogisticParamsRepository skuLogisticParamsRepository;

    @Autowired
    private TransactionHelper transactionHelper;

    @Autowired
    private OfferRepositoryImpl offerRepository;

    @Autowired
    private OfferBatchProcessor offerBatchProcessor;

    @Autowired
    private SupplierRepository supplierRepository;
    private SupplierService supplierService;
    private OfferStatService offerStatService;

    @Autowired
    private StorageKeyValueService storageKeyValueService;

    private ChangeOfferPipelineService changeOfferPipelineService;

    @Autowired
    private AntiMappingRepository antiMappingRepository;

    private CategoryKnowledgeServiceMock categoryKnowledgeServiceMock;

    @Before
    public void setUp() {
        offerStatService = new OfferStatService(jdbcTemplate, jdbcTemplate, skuLogisticParamsRepository,
            transactionHelper, offerRepository, storageKeyValueService);
        offerStatService.subscribe();
        var categoryCachingServiceMock = new CategoryCachingServiceMock();
        supplierService = new SupplierService(supplierRepository);

        var needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        var modelCache = new ModelStorageCachingServiceMock();
        var retrieveService = new RetrieveMappingSkuTypeService(modelCache, offerBatchProcessor, supplierRepository);
        categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, null,
            offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var offerProcessingStatusService = new OffersProcessingStatusService(offerBatchProcessor,
            needContentStatusService, supplierService, categoryKnowledgeServiceMock, retrieveService,
            offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);

        changeOfferPipelineService = new ChangeOfferPipelineService(
            supplierRepository,
            offerRepository,
            offerBatchProcessor,
            categoryCachingServiceMock,
            offerProcessingStatusService
        );
        executor = new ChangeOfferPipelineExecutor(
            offerStatService,
            supplierService,
            categoryCachingServiceMock,
            changeOfferPipelineService
        );

        supplierRepository.insertBatch(
            SUPPLIER_1P, SUPPLIER_3P_NEW, SUPPLIER_3P_OLD, SUPPLIER_WHITE_NEW
        );

        categoryCachingServiceMock.addCategory(
            new Category().setCategoryId(CATEGORY_ID_GOOD)
                .setAcceptGoodContent(true)
                .setHasKnowledge(true)
        );
        categoryCachingServiceMock.addCategory(
            new Category().setCategoryId(CATEGORY_ID_NO_GOOD)
                .setAcceptGoodContent(false)
                .setAcceptContentFromWhiteShops(false)
                .setHasKnowledge(true)
        );
        categoryCachingServiceMock.addCategory(
            new Category().setCategoryId(CATEGORY_ID_NO_GOOD_NO_KNOWLEDGE)
                .setAcceptContentFromWhiteShops(false)
                .setAcceptGoodContent(false)
                .setHasKnowledge(false)
        );

        categoryKnowledgeServiceMock.addCategory(CATEGORY_ID_GOOD);
        categoryKnowledgeServiceMock.addCategory(CATEGORY_ID_NO_GOOD);
    }

    @Test
    public void testNewSupplierInNoGoodCategoryHandled() {
        Offer offer = good3pNew().setCategoryIdForTests(CATEGORY_ID_NO_GOOD, Offer.BindingKind.SUGGESTED);
        offerRepository.insertOffers(offer);

        offerStatService.updateOfferStat();
        executor.execute();

        MbocAssertions.assertThat(offerRepository.getOfferById(offer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    @Test
    public void testUpdateOffer() {
        Offer offerGood3pOld = good3pOld();
        Offer offerNoGood3pNew = noGood3pNew();
        Offer offerNoGood3pOld = noGood3pOld();
        Offer offerNullCategoryNoGood3pOld = noGood3pOld().setCategoryIdForTests(null, Offer.BindingKind.SUGGESTED);
        Offer offerNoGood1p = noGood1p();
        Offer offerSkutchedNoGoodWhiteNew = noGoodWhiteNew().setSuggestSkuMapping(OfferTestUtils.mapping(SKU_ID));
        Offer offerMatchedNoGoodWhiteNew = noGoodWhiteNew().setModelId(1L);
        offerRepository.insertOffers(
            offerGood3pOld,
            offerNoGood3pNew,
            offerNoGood3pOld,
            offerNullCategoryNoGood3pOld,
            offerNoGood1p,
            offerSkutchedNoGoodWhiteNew,
            offerMatchedNoGoodWhiteNew
        );

        offerStatService.updateOfferStat();
        executor.execute();

        MbocAssertions.assertThat(offerRepository.getOfferById(offerGood3pOld.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION);
        MbocAssertions.assertThat(offerRepository.getOfferById(offerNoGood3pNew.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION);
        MbocAssertions.assertThat(offerRepository.getOfferById(offerNoGood3pOld.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION);
        MbocAssertions.assertThat(offerRepository.getOfferById(offerNullCategoryNoGood3pOld.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION);
        MbocAssertions.assertThat(offerRepository.getOfferById(offerNoGood1p.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION);
        MbocAssertions.assertThat(offerRepository.getOfferById(offerSkutchedNoGoodWhiteNew.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
        MbocAssertions.assertThat(offerRepository.getOfferById(offerMatchedNoGoodWhiteNew.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
    }

    @Test
    public void testUpdateOfferNoKnowledge() {
        Offer offer1 = noGood3pNew().setCategoryIdForTests(CATEGORY_ID_NO_GOOD_NO_KNOWLEDGE, Offer.BindingKind.SUGGESTED)
            .setContentComments(new ContentComment(ContentCommentType.NO_KNOWLEDGE, ""));
        Offer offer2 = noGood3pOld().setCategoryIdForTests(CATEGORY_ID_NO_GOOD_NO_KNOWLEDGE, Offer.BindingKind.SUGGESTED)
            .setContentComments(new ContentComment(ContentCommentType.NO_KNOWLEDGE, ""));
        offerRepository.insertOffers(offer1, offer2);

        offerStatService.updateOfferStat();
        executor.execute();

        MbocAssertions.assertThat(offerRepository.getOfferById(offer1.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NO_KNOWLEDGE);
        MbocAssertions.assertThat(offerRepository.getOfferById(offer2.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NO_KNOWLEDGE);
    }

    @Test
    public void testUpdateOfferRemoveContentActiveError() {
        Offer offerNoGood3pNew = noGood3pNew()
            .setContentStatusActiveError(MbocErrors.get().barcodeRequired("stub"));
        offerRepository.insertOffers(
            offerNoGood3pNew
        );

        offerStatService.updateOfferStat();
        executor.execute();

        MbocAssertions.assertThat(offerRepository.getOfferById(offerNoGood3pNew.getId()))
            .hasContentStatusActiveError(null);
    }

    @Test
    public void testDoNotUpdateOffer() {
        Offer offer1 = noGood3pNew().updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW);
        Offer offer2 = noGood3pNew().updateApprovedSkuMapping(OfferTestUtils.mapping(SKU_ID),
            Offer.MappingConfidence.CONTENT);
        offerRepository.insertOffers(offer1, offer2);

        Offer offer3copy = offer1.copy();
        Offer offer4copy = offer2.copy();
        offerStatService.updateOfferStat();
        executor.execute();

        MbocAssertions.assertThat(offerRepository.getOfferById(offer1.getId())).isEqualTo(offer3copy);
        MbocAssertions.assertThat(offerRepository.getOfferById(offer2.getId())).isEqualTo(offer4copy);
    }

    private Offer good3pOld() {
        return OfferTestUtils.nextOffer()
            .setCategoryIdForTests(CATEGORY_ID_GOOD, Offer.BindingKind.SUGGESTED)
            .setBusinessId(SUPPLIER_3P_OLD.getId())
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(SUPPLIER_3P_OLD)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
    }

    private Offer good3pNew() {
        return OfferTestUtils.nextOffer()
            .setCategoryIdForTests(CATEGORY_ID_GOOD, Offer.BindingKind.SUGGESTED)
            .setBusinessId(SUPPLIER_3P_NEW.getId())
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(SUPPLIER_3P_NEW)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
    }

    private Offer noGood3pNew() {
        return OfferTestUtils.nextOffer()
            .setCategoryIdForTests(CATEGORY_ID_NO_GOOD, Offer.BindingKind.SUGGESTED)
            .setBusinessId(SUPPLIER_3P_NEW.getId())
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(SUPPLIER_3P_NEW)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
    }

    private Offer noGood3pOld() {
        return OfferTestUtils.nextOffer()
            .setCategoryIdForTests(CATEGORY_ID_NO_GOOD, Offer.BindingKind.SUGGESTED)
            .setBusinessId(SUPPLIER_3P_OLD.getId())
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(SUPPLIER_3P_OLD)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
    }

    private Offer noGood1p() {
        return OfferTestUtils.nextOffer()
            .setCategoryIdForTests(CATEGORY_ID_NO_GOOD, Offer.BindingKind.SUGGESTED)
            .setBusinessId(SUPPLIER_1P.getId())
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(SUPPLIER_1P)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
    }

    private Offer noGoodWhiteNew() {
        return OfferTestUtils.nextOffer(SUPPLIER_WHITE_NEW)
            .setCategoryIdForTests(CATEGORY_ID_NO_GOOD, Offer.BindingKind.SUGGESTED)
            .setBusinessId(SUPPLIER_WHITE_NEW.getId())
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(SUPPLIER_WHITE_NEW)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);
    }
}
