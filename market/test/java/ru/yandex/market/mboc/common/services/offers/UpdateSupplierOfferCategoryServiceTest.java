package ru.yandex.market.mboc.common.services.offers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.RemovedMappingLogRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.antimapping.AntiMappingService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersProcessingService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.SupplierOffer;

/**
 * @author apluhin
 * @created 5/31/22
 */
public class UpdateSupplierOfferCategoryServiceTest extends BaseDbTestClass {

    @Autowired
    private OfferRepository offerRepository;
    private OfferRepository spyOfferRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    private AntiMappingService antiMappingService;
    private OfferMappingActionService offerMappingActionService;
    @Autowired
    private RemovedMappingLogRepository removedMappingLogRepository;
    private RemovedMappingLogRepository spyRemovedMappingLogRepository;
    @Autowired
    private TransactionHelper transactionHelper;

    private OffersProcessingStatusService spyOffersProcessingStatusService;

    private CategoryCachingServiceMock categoryCachingService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;

    private UpdateSupplierOfferCategoryService updateSupplierOfferCategoryService;

    @Before
    public void setUp() throws Exception {
        spyOfferRepository = Mockito.spy(offerRepository);
        spyRemovedMappingLogRepository = Mockito.spy(removedMappingLogRepository);
        categoryCachingService = new CategoryCachingServiceMock();
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        var supplierService = new SupplierService(supplierRepository);

        antiMappingService = new AntiMappingService(antiMappingRepository, transactionHelper);

        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var modelStorageCachingService = new ModelStorageCachingServiceMock();
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, null,
            offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingService,
            offerBatchProcessor, supplierRepository);
        var offersProcessingStatusService = new OffersProcessingStatusService(offerBatchProcessor,
            needContentStatusService, supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator,
            storageKeyValueService, new FastSkuMappingsService(needContentStatusService), false, false, 3,
            categoryInfoCache);
        spyOffersProcessingStatusService = Mockito.spy(offersProcessingStatusService);

        ClassificationOffersProcessingService classificationOffersProcessingService =
            new ClassificationOffersProcessingService(
                categoryCachingService,
                offerMappingActionService,
                offerDestinationCalculator
            );


        List<Supplier> suppliers = IntStream.of(
                42, 43, 44, 45, 50, 99, 100
            )
            .mapToObj(id -> OfferTestUtils.simpleSupplier().setId(id))
            .collect(Collectors.toList());
        supplierRepository.insertOrUpdateAll(suppliers);

        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/sample-offers.json");

        offerRepository.insertOffers(offers);

        updateSupplierOfferCategoryService = new UpdateSupplierOfferCategoryService(
            offerRepository, supplierRepository, classificationOffersProcessingService, offersProcessingStatusService,
            Mockito.mock(MigrationService.class)
        );
    }

    @Test
    public void testForceChangeOfferCategory() {
        var offer = offerRepository.findAll().get(0);
        Long categoryId = offer.getCategoryId();
        var newCategoryId = 90401L;
        SupplierOffer.ClassificationTaskResult test = SupplierOffer.ClassificationTaskResult.newBuilder()
            .setOfferId(String.valueOf(offer.getId()))
            .setFixedCategoryId(newCategoryId)
            .setSupplierId(offer.getBusinessId())
            .setStaffLogin("test")
            .build();
        MboCategory.UpdateSupplierOfferCategoryRequest.Builder updateRequest =
            MboCategory.UpdateSupplierOfferCategoryRequest.newBuilder();
        updateRequest.addResult(test);
        Assertions.assertThat(newCategoryId).isNotEqualTo(categoryId);
        updateSupplierOfferCategoryService.updateSupplierOfferCategory(
            updateRequest.build(), true
        );
        Assertions.assertThat(offerRepository.getOfferById(offer.getId()).getCategoryId()).isEqualTo(newCategoryId);
    }

    @Test
    public void testNonForceChangeOfferCategory() {
        var offer = offerRepository.findAll().get(0);
        Long categoryId = offer.getCategoryId();
        var newCategoryId = 90401L;
        SupplierOffer.ClassificationTaskResult test = SupplierOffer.ClassificationTaskResult.newBuilder()
            .setOfferId(String.valueOf(offer.getId()))
            .setFixedCategoryId(newCategoryId)
            .setSupplierId(offer.getBusinessId())
            .setStaffLogin("test")
            .build();
        MboCategory.UpdateSupplierOfferCategoryRequest.Builder updateRequest =
            MboCategory.UpdateSupplierOfferCategoryRequest.newBuilder();
        updateRequest.addResult(test);
        Assertions.assertThat(newCategoryId).isNotEqualTo(categoryId);
        updateSupplierOfferCategoryService.updateSupplierOfferCategory(
            updateRequest.build(), false
        );
        Assertions.assertThat(offerRepository.getOfferById(offer.getId()).getCategoryId()).isEqualTo(categoryId);
    }
}
