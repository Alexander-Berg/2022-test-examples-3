package ru.yandex.market.mboc.common.services.offers.fashion;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.DatabaseCategoryCachingService;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class FashionClassificationCheckImplTest extends BaseDbTestClass {

    private FashionClassificationCheckImpl service;

    @Autowired
    private QueueFromContentProcessingRepository queue;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private AntiMappingRepository antiMappingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Before
    public void setup() {
        var supplierService = new SupplierService(supplierRepository);
        var categoryCachingService = new DatabaseCategoryCachingService(categoryRepository,
            storageKeyValueService, Mockito.mock(ScheduledExecutorService.class), 100);
        var booksService = new BooksService(categoryCachingService, Set.of());
        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            booksService);

        var categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        categoryKnowledgeService.enableAllCategories();

        var modelStorageCachingService = new ModelStorageCachingServiceMock();

        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingService, null,
            supplierRepository);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, null,
            offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        var offersProcessingStatusService = new OffersProcessingStatusService(null, needContentStatusService,
            supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator,
            storageKeyValueService, new FastSkuMappingsService(needContentStatusService), true, true, 3,
            categoryInfoCache);

        service = new FashionClassificationCheckImpl(storageKeyValueService, offersProcessingStatusService, queue,
            offerRepository, offerDestinationCalculator);

        storageKeyValueService.putValue(FashionClassificationCheckImpl.ENABLED_FLAG, true);
    }


    @Test
    public void excludeUngroupedFashionDSBS() {
        var category = OfferTestUtils.defaultCategory();
        categoryRepository.insert(category);
        categoryRepository.insert(OfferTestUtils.defaultCategory().setCategoryId(90401L));

        var categoryInfo = OfferTestUtils.categoryInfoWithManualAcceptance()
            .addTag(CategoryInfo.CategoryTag.FASHION)
            .addTag(CategoryInfo.CategoryTag.SIZED_FASHION);
        categoryInfoRepository.insert(categoryInfo);

        var supplier = OfferTestUtils.simpleSupplier().setType(MbocSupplierType.DSBS);
        supplierRepository.insert(supplier);

        var offer = OfferTestUtils.simpleOkOffer(supplier).setCategoryId(offerDestinationCalculator,
            category.getCategoryId(), Offer.BindingKind.SUGGESTED).setId(100L);
        offerRepository.insertOffers(offer);
        offer = offerRepository.getOfferById(100L);

        service.skippedExcludeUngroupedFashionNotFMCG(Stream.of(offer).collect(Collectors.toList()));

        offer = offerRepository.getOfferById(100L);
        assertThat(offer.getContentStatusActiveError()).isNotNull();
        assertThat(offer.getContentStatusActiveError())
            .isEqualTo(MbocErrors.get().contentProcessingFailed(offer.getShopSku()));
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_INFO);

        var contentProcessingResponse = queue.findById(100L);
        assertThat(contentProcessingResponse).isNotNull();
        assertThat(contentProcessingResponse.getSkuRating()).isNull();
    }
}
