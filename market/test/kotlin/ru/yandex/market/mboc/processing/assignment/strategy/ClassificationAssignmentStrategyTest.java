package ru.yandex.market.mboc.processing.assignment.strategy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest;
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassificationAssignmentStrategyTest extends BaseOfferProcessingTest {
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

    private ClassificationAssignmentStrategy assignmentStrategy;

    @Before
    public void setup() {
        assignmentStrategy = new ClassificationAssignmentStrategy(transactionHelper, assignmentRepository,
            storageKeyValueService, solomonPushService);
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void assignUpToTheLimit() {
        storageKeyValueService.putValue(assignmentStrategy.getTotalLimitKey(), 10);

        var offers = IntStream.range(0, 20)
            .mapToObj(__ -> prepareInClassificationOffer())
            .collect(Collectors.toList());

        offerRepository.insertOffers(offers);

        assignmentStrategy.actualize();

        var stats = assignmentRepository.gatherCategoryStatsFor(getTarget(), getProcessingType());

        assertThat(stats).hasSize(1);
        assertThat(stats.get(OfferTestUtils.TEST_CATEGORY_INFO_ID)).isEqualTo(10);
    }

    @Test
    public void assignHighPriorityOffers() {
        storageKeyValueService.putValue(assignmentStrategy.getTotalLimitKey(), 10);

        var offers = IntStream.range(0, 5)
            .mapToObj(__ -> prepareInClassificationOffer())
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);
        assignmentStrategy.actualize();

        var stats = assignmentRepository.gatherCategoryStatsFor(getTarget(), getProcessingType());
        assertThat(stats).hasSize(1);
        assertThat(stats.get(OfferTestUtils.TEST_CATEGORY_INFO_ID)).isEqualTo(5);


        var offers2 = IntStream.range(0, 15)
            .mapToObj(__ -> prepareInClassificationOffer())
            .peek(offer -> offer.setTicketDeadline(LocalDate.now()))
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers2);
        assignmentStrategy.actualize();

        stats = assignmentRepository.gatherCategoryStatsFor(getTarget(), getProcessingType());
        assertThat(stats).hasSize(1);
        assertThat(stats.get(OfferTestUtils.TEST_CATEGORY_INFO_ID)).isEqualTo(10);

        var assignments = assignmentRepository.findAssignedOffersWithCommonOrder(getTarget(),
            getProcessingType());

        assertThat(assignments).hasSize(10);
        var numberOfOffersWithDeadline = assignments.stream()
            .map(OfferProcessingAssignment::getTicketDeadline)
            .filter(LocalDate.now()::isEqual)
            .count();
        assertThat(numberOfOffersWithDeadline).isEqualTo(5L);
    }

    @Test
    public void resetAssignment() {
        storageKeyValueService.putValue(assignmentStrategy.getTotalLimitKey(), 100);

        var offers = IntStream.range(0, 200)
            .mapToObj(__ -> prepareInClassificationOffer())
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);

        assignmentStrategy.actualize();

        var offer = offerRepository.findOfferByBusinessSkuKey(offers.get(0).getBusinessSkuKey());
        offer.setTicketDeadline(LocalDate.now().plus(1, ChronoUnit.YEARS));
        offerRepository.updateOffer(offer);

        assertThat(assignmentRepository.findById(offer.getId()).isToReset()).isTrue();

        assignmentStrategy.actualize();
        assertNotAssigned(assignmentRepository.findById(offer.getId()));
    }

    @Test
    public void resetAssignmentStaysAssigned() {
        storageKeyValueService.putValue(assignmentStrategy.getTotalLimitKey(), 100);

        var offers = IntStream.range(0, 200)
            .mapToObj(__ -> prepareInClassificationOffer())
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);

        assignmentStrategy.actualize();

        var offer = offerRepository.findOfferByBusinessSkuKey(offers.get(0).getBusinessSkuKey());
        offer.setTicketCritical(true);
        offerRepository.updateOffer(offer);

        assertThat(assignmentRepository.findById(offer.getId()).isToReset()).isTrue();

        assignmentStrategy.actualize();
        var assignment = assignmentRepository.findById(offer.getId());
        assertThat(assignment.isToReset()).isFalse();
        assertThat(assignment.getType()).isNotNull();
        assertThat(assignment.getTarget()).isNotNull();
    }

    @Test
    public void resetUnAssignedAndDontGetLost() {
        storageKeyValueService.putValue(assignmentStrategy.getTotalLimitKey(), 100);

        var offers = IntStream.range(0, 200)
            .mapToObj(__ -> prepareInClassificationOffer())
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);

        var keys = offers.stream().map(Offer::getBusinessSkuKey).collect(Collectors.toList());
        var updatedOffers = offerRepository.findOffersByBusinessSkuKeys(keys)
            .stream()
            .limit(100)
            .peek(offer -> offer
                .setTicketCritical(true)
                .setTicketDeadline(LocalDate.now().minus(1, ChronoUnit.DAYS)))
            .collect(Collectors.toList());
        offerRepository.updateOffers(updatedOffers);

        assignmentStrategy.actualize();

        var assignments = assignmentRepository.findByIds(updatedOffers.stream()
            .map(Offer::getId).collect(Collectors.toList()));

        assertThat(assignments).hasSize(100);
        assignments.forEach(assignment -> {
            assertThat(assignment.isToReset()).isFalse();
            assertThat(assignment.getTarget()).isNotNull();
            assertThat(assignment.getType()).isNotNull();
        });
    }

    private void assertNotAssigned(OfferProcessingAssignment assignment) {
        assertThat(assignment.getType()).isNull();
        assertThat(assignment.getTarget()).isNull();
    }

    private Offer prepareInClassificationOffer() {
        var id = OFFER_IDS.incrementAndGet();
        return OfferTestUtils.simpleOkOffer()
            .setId(id)
            .setShopSku("uniq-shop-sku-" + id)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    protected OfferTarget getTarget() {
        return OfferTarget.YANG;
    }

    protected OfferProcessingType getProcessingType() {
        return OfferProcessingType.IN_CLASSIFICATION;
    }
}
