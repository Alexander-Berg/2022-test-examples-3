package ru.yandex.market.mboc.processing.assignment;

import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest;

import static org.assertj.core.api.Assertions.assertThat;

public class PriorityDifferenceTest extends BaseOfferProcessingTest {
    private static final long MAPPING_ID_1 = 15L;
    private static final long CATEGORY_ID1 = 1L;

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferProcessingAssignmentRepository assignmentRepository;

    @Test
    public void testPriorityDifference() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var recheckOffer = offerWithoutMappings(21)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        recheckOffer
            .setRecheckSkuMapping(recheckOffer.getApprovedSkuMapping())
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK);
        var rollbackOffer = offerWithoutMappings(22)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        rollbackOffer
            .setRecheckSkuMapping(rollbackOffer.getApprovedSkuMapping())
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_ROLLBACK);

        offerRepository.insertOffers(recheckOffer, rollbackOffer);

        var offerToAssignment = assignmentRepository.findAll()
            .stream().collect(Collectors.toMap(OfferProcessingAssignment::getOfferId, Function.identity()));

        assertThat(
            offerToAssignment.get(rollbackOffer.getId()).getPriority()
                - offerToAssignment.get(recheckOffer.getId()).getPriority()
        ).isEqualTo(1000);
    }

    private Offer offerWithoutMappings(long id) {
        return new Offer()
            .setId(id)
            .setShopSku("Sku" + id)
            .setCategoryIdForTests(CATEGORY_ID1, Offer.BindingKind.APPROVED)
            .setMappingDestination(Offer.MappingDestination.BLUE)
            .setTitle("Title")
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category")
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setProcessingCounter(null)
            .setBusinessId(250)
            .addNewServiceOfferIfNotExistsForTests(new Supplier(250, "new pipe"))
            .updateAcceptanceStatusForTests(250, Offer.AcceptanceStatus.OK)
            .markLoadedContent()
            .storeOfferContent(OfferContent.initEmptyContent());
    }
}
