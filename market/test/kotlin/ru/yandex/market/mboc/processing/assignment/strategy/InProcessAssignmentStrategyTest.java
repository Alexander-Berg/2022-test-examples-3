package ru.yandex.market.mboc.processing.assignment.strategy;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest;
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository;

import static org.assertj.core.api.Assertions.assertThat;


public class InProcessAssignmentStrategyTest extends BaseOfferProcessingTest {
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private SolomonPushService solomonPushService;
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferProcessingAssignmentRepository assignmentRepository;

    private InProcessAssignmentStrategy strategy;

    @Before
    public void setup() {
        strategy = new InProcessAssignmentStrategy(transactionHelper, assignmentRepository, storageKeyValueService,
            solomonPushService);
    }

    @Test
    public void getOfferIdsToAssign() {
        categoryRepository.insert(OfferTestUtils.defaultCategory());
        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        offerRepository.insertOffers(OfferTestUtils.simpleOkOffer()
                .setId(1L)
                .setShopSku("shop-sku-1")
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS)
                .setProcessingTicketId(100500),
            OfferTestUtils.simpleOkOffer()
                .setId(2L)
                .setShopSku("shop-sku-2")
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS)
                .setProcessingTicketId(100500)
                .updateApprovedSkuMapping(
                    new Offer.Mapping(100L, LocalDateTime.now(), Offer.SkuType.FAST_SKU),
                    Offer.MappingConfidence.CONTENT
                )
        );
        storageKeyValueService.putValue(InProcessAssignmentStrategy.DONT_ASSIGN_APPROVED, false);
        assertThat(assignmentRepository.findAll()).hasSize(2);
        var withApproved = strategy.getOfferIdsToAssign(100, OfferTestUtils.TEST_CATEGORY_INFO_ID);
        assertThat(withApproved).hasSize(2);

        storageKeyValueService.putValue(InProcessAssignmentStrategy.DONT_ASSIGN_APPROVED, true);
        var withOutApproved = strategy.getOfferIdsToAssign(100, OfferTestUtils.TEST_CATEGORY_INFO_ID);
        assertThat(withOutApproved).hasSize(1);
    }
}
