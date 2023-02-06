package ru.yandex.market.mboc.common.services.offers.mapping;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.utils.Pair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RecheckClassificationServiceTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    private SupplierService supplierService;
    private CategoryCachingServiceMock categoryCachingService;
    private OfferMappingActionService offerMappingActionService;
    private OfferCategoryRestrictionCalculator categoryRestrictionCalculator;

    private RecheckClassificationService recheckClassificationService;

    private static final Long CATEGORY_1 = 1L;
    private static final Long CATEGORY_2 = 2L;
    private static final Long CATEGORY_3 = 3L;
    private static final Supplier SUPPLIER_1 = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);

    @Before
    public void setUp() throws Exception {
        categoryCachingService = new CategoryCachingServiceMock();
        var needContentStatusService = Mockito.mock(NeedContentStatusService.class);
        categoryRestrictionCalculator = Mockito.mock(OfferCategoryRestrictionCalculator.class);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            categoryRestrictionCalculator, offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        supplierService = new SupplierService(supplierRepository);

        recheckClassificationService = new RecheckClassificationService(
            offerMappingActionService,
            categoryCachingService,
            supplierService
        );

        categoryCachingService.addCategory(CATEGORY_1)
            .addCategory(CATEGORY_2)
            .addCategory(CATEGORY_3);
        categoryCachingService.setAcceptContentFromWhiteShops(CATEGORY_1, true);
        categoryCachingService.setAcceptContentFromWhiteShops(CATEGORY_2, true);
        categoryCachingService.setAcceptContentFromWhiteShops(CATEGORY_3, true);
        supplierRepository.insert(SUPPLIER_1);
        supplierService.invalidateCache();
        storageKeyValueService.putValue(LegacyOfferMappingActionService.ENABLE_CHANGE_PSKU_CATEGORY_KEY, true);
    }

    @Test
    public void updateToRecheckCategoryIfValid_Success() {
        Offer offer = OfferTestUtils.simpleOffer(SUPPLIER_1)
            .setDataCampOffer(true)
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
            .setRecheckCategoryId(CATEGORY_1)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierCategoryId(CATEGORY_2)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_MODERATION);

        recheckClassificationService.updateToRecheckCategoryIfValid(List.of(offer));

        assertEquals(offer.getCategoryId(), offer.getSupplierCategoryId());
        assertEquals(offer.getRecheckClassificationStatus(), Offer.RecheckClassificationStatus.REJECTED);
        assertEquals(offer.getSupplierCategoryMappingStatus(), Offer.MappingStatus.ACCEPTED);
    }

    @Test
    public void updateToRecheckCategoryIfValid_NotUpdated() {
        categoryCachingService.setAcceptContentFromWhiteShops(CATEGORY_1, false);

        Offer offer = OfferTestUtils.simpleOffer(SUPPLIER_1)
            .setMappedCategoryConfidence(Offer.MappingConfidence.PARTNER)
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
            .setRecheckCategoryId(CATEGORY_1)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierCategoryId(CATEGORY_2)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .updateApprovedSkuMapping(new Offer.Mapping(1234, DateTimeUtils.dateTimeNow(), Offer.SkuType.PARTNER20))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF);

        recheckClassificationService.updateToRecheckCategoryIfValid(List.of(offer));

        assertEquals(offer.getCategoryId(), offer.getRecheckCategoryId());
        assertEquals(offer.getRecheckClassificationStatus(), Offer.RecheckClassificationStatus.ON_RECHECK);
        assertEquals(offer.getSupplierCategoryMappingStatus(), Offer.MappingStatus.NEW);
        assertNull(offer.getContentStatusActiveError());
    }

    @Test
    public void processOffersShouldSendToRecheckModeration() {
        var offerWithMsku = prepareOffer("shopSku1", CATEGORY_1);
        var offerWithForeignPsku = prepareOffer("shopSku2", CATEGORY_2)
            .updateApprovedSkuMapping(new Offer.Mapping(12345, DateTimeUtils.dateTimeNow(), Offer.SkuType.PARTNER20))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE)
            .setVendorId(1);
        var offerPairs = List.of(
            new Pair<>(offerWithMsku, CATEGORY_2),
            new Pair<>(offerWithForeignPsku, CATEGORY_1)
        );
        recheckClassificationService.processOffers(Offer.RecheckClassificationSource.PARTNER, offerPairs);

        assertEquals(offerWithMsku.getRecheckMappingStatus(), Offer.RecheckMappingStatus.NEED_RECHECK);
        assertEquals(offerWithMsku.getRecheckClassificationStatus(), Offer.RecheckClassificationStatus.ON_RECHECK);

        assertEquals(offerWithForeignPsku.getRecheckMappingStatus(), Offer.RecheckMappingStatus.NEED_RECHECK);
        assertEquals(offerWithForeignPsku.getRecheckClassificationStatus(),
            Offer.RecheckClassificationStatus.ON_RECHECK);
    }

    @Test
    public void processOffersShouldSendToRecheckClassification() {
        var offerWithForeignPskuWithRMM = prepareOffer("shopSku1", CATEGORY_1)
            .updateApprovedSkuMapping(new Offer.Mapping(1234, DateTimeUtils.dateTimeNow(), Offer.SkuType.PARTNER20))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.MAPPING_CONFIRMED); // ПММ у мскю уже была

        var offerWithMskuWithRMM = prepareOffer("shopSku2", CATEGORY_1)
            .updateApprovedSkuMapping(new Offer.Mapping(12345, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.MAPPING_CONFIRMED); // ПММ у пскю уже была

        recheckClassificationService.processOffers(Offer.RecheckClassificationSource.PARTNER,
            List.of(
                new Pair<>(offerWithForeignPskuWithRMM, CATEGORY_1),
                new Pair<>(offerWithMskuWithRMM, CATEGORY_1)
            )
        );

        Consumer<Offer> baseCheck = offer -> {
            assertEquals(offer.getRecheckCategoryId(), offer.getCategoryId());
            assertEquals(offer.getRecheckClassificationStatus(), Offer.RecheckClassificationStatus.ON_RECHECK);
        };

        baseCheck.accept(offerWithForeignPskuWithRMM);
        assertEquals(offerWithForeignPskuWithRMM.getRecheckMappingStatus(),
            Offer.RecheckMappingStatus.MAPPING_CONFIRMED);

        baseCheck.accept(offerWithMskuWithRMM);
        assertEquals(offerWithMskuWithRMM.getRecheckMappingStatus(), Offer.RecheckMappingStatus.MAPPING_CONFIRMED);
    }

    @Test
    public void processOffersShouldRejectInvalidRequests() {
        var validOffer = prepareOffer("shopSku1", CATEGORY_1);
        var validOffer2 = prepareOffer("shopSku1", CATEGORY_1);
        var unknownCategory = 123456L;

        List<Pair<Offer, Long>> invalidOfferPairs = List.of(
            new Pair<>(validOffer, null),
            new Pair<>(validOffer2, unknownCategory)
        );
        recheckClassificationService.processOffers(Offer.RecheckClassificationSource.PARTNER, invalidOfferPairs);

        Consumer<Offer> shouldInvalidateRequestCheck = offer -> {
            assertNull(offer.getRecheckClassificationSource());
            assertNull(offer.getRecheckClassificationStatus());
        };

        invalidOfferPairs.stream()
            .map(Pair::getFirst)
            .forEach(shouldInvalidateRequestCheck);
    }

    @Test
    public void processOffersShouldDenyAlreadyProcessed() {
        var supplierCategoryTimestamp = Instant.now();
        var validOffer = prepareOffer("shopSku1", CATEGORY_1)
            .setSupplierCategoryTimestamp(supplierCategoryTimestamp);
        var oldOfferState = validOffer.copy();

        var pairsToProcess = List.of(new Pair<>(validOffer, CATEGORY_1));
        recheckClassificationService.processOffers(Offer.RecheckClassificationSource.PARTNER, pairsToProcess);

        assertEquals(validOffer, oldOfferState);
    }

    private Offer prepareOffer(String shopSku, long categoryId) {
        return new Offer()
            .setBusinessId(SUPPLIER_1.getId())
            .setCategoryIdForTests(categoryId, Offer.BindingKind.APPROVED)
            .setShopSku(shopSku)
            .setTitle("Title")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("Category1")
            .setSupplierSkuMapping(new Offer.Mapping(123, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET))
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .addNewServiceOfferIfNotExistsForTests(SUPPLIER_1)
            .updateApprovedSkuMapping(new Offer.Mapping(1234, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE)
            .setDataCampOffer(true);
    }

}
