package ru.yandex.market.mboc.common.services.offers.moderation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepository;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoServiceForTesting;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.offers.processing.ModerationOffersService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusServiceTestBase;
import ru.yandex.market.mboc.common.services.offers.tracker.OffersTrackerService;
import ru.yandex.market.mboc.common.services.smartmatcher.runtime.RuntimeSmartMatcherService;
import ru.yandex.market.mboc.common.services.smartmatcher.runtime.RuntimeSmartMatcherServiceMock;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

@SuppressWarnings("checkstyle:magicnumber")
public class ModerationOffersServiceTest extends OffersProcessingStatusServiceTestBase {
    private static final int SUPPLIER_ID_1 = 11;
    private static final long AUTO_APPROVED_OFFER_ID = 100500;

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private CategoryInfoRepository categoryInfoRepository;
    @Autowired
    private ProcessingTicketInfoRepository processingTicketInfoRepository;

    private ModerationOffersService moderationOffersService;
    private ProcessingTicketInfoServiceForTesting processingTicketInfoService;

    private OfferRepositoryMock offerRepositoryMock;
    private ModerationOffersService offersModerationServiceWithOffersMock;
    private RuntimeSmartMatcherServiceMock runtimeSmartMatcherServiceMock;

    @Before
    public void setUp() {
        storageKeyValueService.putValue(RuntimeSmartMatcherService.CONFIDENCE_THRESHOLD, 2);
        storageKeyValueService.putValue(ModerationOffersService.AUTO_APPROVE_BY_SM_ENABLED_FLAG, true);

        supplierRepository.insert(new Supplier(SUPPLIER_ID_1, "supplier11"));
        categoryInfoRepository.insertBatch(Arrays.asList(
            new CategoryInfo(1).setModerationInYang(true),
            new CategoryInfo(2).setModerationInYang(false)));
        categoryCachingServiceMock.addCategory(1);

        processingTicketInfoService = new ProcessingTicketInfoServiceForTesting(processingTicketInfoRepository);

        runtimeSmartMatcherServiceMock = new RuntimeSmartMatcherServiceMock();
        Offer autoApprovedOffer = createSuggestedTestOffer(AUTO_APPROVED_OFFER_ID);
        autoApprovedOffer.setAutoApprovedMapping(true);
        autoApprovedOffer.updateApprovedSkuMapping(autoApprovedOffer.getSuggestSkuMapping(), Offer.MappingConfidence.CONTENT);
        autoApprovedOffer.setAutoApprovedMappingSource(Offer.AutoApprovedMappingSource.SMART_MATCHER);
        // Pass via repo to trigger observers
        autoApprovedOffer = offerRepository.insertAndGetOffer(autoApprovedOffer);
        offerRepository.removeOffer(autoApprovedOffer);
        runtimeSmartMatcherServiceMock.putOffer(autoApprovedOffer);

        moderationOffersService = new ModerationOffersService(offerRepository,
            runtimeSmartMatcherServiceMock, storageKeyValueService, offersProcessingStatusService);

        offerRepositoryMock = new OfferRepositoryMock();

        offersModerationServiceWithOffersMock = new ModerationOffersService(offerRepositoryMock,
            runtimeSmartMatcherServiceMock, storageKeyValueService, offersProcessingStatusService);
    }

    @Test
    public void testOffersForModerationOverLimit() {
        int count = (int) ((float) OffersTrackerService.MAX_FIND_OFFERS_LIMIT * 1.5f);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Offer offer = createTestOffer(i + 1);
            ids.add(offer.getId());
            offerRepositoryMock.insertOffer(offer);
        }
        int updateCount = offersModerationServiceWithOffersMock.findAndMarkOffersToModeration(ids);
        Assert.assertEquals(count, updateCount);
    }

    @Test
    public void testOffersForModerationOverLimitByIds() {
        int count = (int) ((float) OffersTrackerService.MAX_FIND_OFFERS_LIMIT * 2.5d);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Offer offer = createTestOffer(i + 1);
            ids.add(offer.getId());
            offerRepositoryMock.insertOffer(offer);
        }
        int updateCount = offersModerationServiceWithOffersMock.findAndMarkOffersToModeration(ids);
        Assert.assertEquals(count, updateCount);
    }

    @Test
    public void testProcessingTicketIdForModeration() {
        // correct offer
        offerRepository.insertOffer(createTestOffer(1));
        offerRepository.insertOffer(createTestOffer(2));

        moderationOffersService.findAndMarkOffersToModeration(null);

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        List<Offer> inModeration = new ArrayList<>();
        for (Offer offer : offers) {
            if (offer.getProcessingStatus() == Offer.ProcessingStatus.IN_MODERATION) {
                inModeration.add(offer);
                Assertions.assertThat(offer.getProcessingTicketId()).isNotNull();
            } else {
                Assertions.assertThat(offer.getProcessingTicketId()).isNull();
            }
        }
    }

    @Test
    public void testAutoApproveFromSmartMatcher() {
        offerRepository.insertOffer(createSuggestedTestOffer(AUTO_APPROVED_OFFER_ID));

        moderationOffersService.findAndMarkOffersToModeration(List.of(AUTO_APPROVED_OFFER_ID));

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());

        Assertions.assertThat(offers).hasSize(1).singleElement()
            .extracting(Offer::getApprovedSkuId, Offer::getProcessingStatus)
            .containsExactly(15L, Offer.ProcessingStatus.PROCESSED);
    }

    private Offer createTestOffer(long id) {
        return new Offer()
            .setId(id)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("Sku-" + id)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("shop_category_name")
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.simpleSupplier())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierSkuMapping(OfferTestUtils.mapping(15L))
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setTitle("Offer_" + id);
    }

    private Offer createSuggestedTestOffer(long id) {
        return new Offer()
            .setId(id)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("Sku-" + id)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("shop_category_name")
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.simpleSupplier())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSuggestSkuMapping(OfferTestUtils.mapping(15L))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setTitle("Offer_" + id);
    }
}
