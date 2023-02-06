package ru.yandex.market.mboc.processing.assignment.strategy;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferTarget;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest;
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository;
import ru.yandex.market.mboc.processing.assignment.TolokaHidingSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecheckClassificationAssignmentStrategyTest extends BaseOfferProcessingTest {
    private static final AtomicLong OFFER_IDS = new AtomicLong();

    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private SolomonPushService solomonPushService;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferProcessingAssignmentRepository assignmentRepository;
    @Autowired
    private TolokaHidingSettings tolokaHidingSettings;

    private RecheckClassificationAssignmentStrategy assignmentStrategy;

    @Before
    public void setup() {
        assignmentStrategy = new RecheckClassificationAssignmentStrategy(transactionHelper, assignmentRepository,
            storageKeyValueService, solomonPushService);
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        categoryInfoRepository.insert(OfferTestUtils.categoryInfoWithManualAcceptance().setHideFromToloka(false));
        tolokaHidingSettings.reset();
    }

    @Test
    public void assignUpToTheLimit() {
        storageKeyValueService.putValue(assignmentStrategy.getTotalLimitKey(), 10);

        var offers = IntStream.range(0, 20)
            .mapToObj(__ -> prepareOffer())
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);

        assignmentStrategy.actualize();

        var stats = assignmentRepository.getAssignedCountFor(OfferTarget.YANG,
            OfferProcessingType.IN_RECHECK_CLASSIFICATION);

        assertThat(stats).isEqualTo(10);
    }

    @Test
    public void getOfferIdsToAssignOk() {
        storageKeyValueService.putValue(assignmentStrategy.getTotalLimitKey(), 10);

        var offers = IntStream.range(0, 10)
            .mapToObj(__ -> prepareOffer())
            .collect(Collectors.toSet());
        offerRepository.insertOffers(offers);

        var offerIds = offers.stream()
            .map(Offer::getId)
            .collect(Collectors.toSet());

        var offerIdsToAssign = assignmentStrategy.getOfferIdsToAssign(10, 91497L);

        assertTrue(offerIdsToAssign.containsAll(offerIds));
    }

    @Test
    public void assignFillRequiredFields() {
        storageKeyValueService.putValue(assignmentStrategy.getTotalLimitKey(), 10);

        var offers = IntStream.range(0, 10)
            .mapToObj(__ -> prepareOffer())
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);

        assignmentStrategy.actualize();
        var assignments = assignmentRepository.findAll();

        assignments.forEach(assignment -> {
            assertEquals(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION, assignment.getProcessingStatus());
            assertEquals(OfferTarget.YANG, assignment.getTarget());
        });
    }

    private Offer prepareOffer() {
        var id = OFFER_IDS.incrementAndGet();
        return OfferTestUtils.simpleOkOffer()
            .setId(id)
            .setShopSku("uniq-shop-sku-" + id)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(0)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION)
            .updateApprovedSkuMapping(new Offer.Mapping(
                1, LocalDateTime.now()
            ))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
    }

}
