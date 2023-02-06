package ru.yandex.market.mboc.tms.executors.contentprocessing;

import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.service.ReadyForContentProcessingService;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.NEED_CONTENT;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BIZ_ID_SUPPLIER;

public class ContentProcessingReadyOfferRecoveryExecutorTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    @Qualifier("slaveOfferRepository")
    private OfferRepository slaveOfferRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private ReadyForContentProcessingService readyForContentProcessingService;
    @Autowired
    private ContentProcessingQueueRepository queue;

    private ContentProcessingReadyOfferRecoveryExecutor executor;

    @Before
    public void setup() {
        supplierRepository.insert(OfferTestUtils.businessSupplier());

        executor = new ContentProcessingReadyOfferRecoveryExecutor(
            readyForContentProcessingService,
            offerBatchProcessor,
            offerRepository,
            queue
        );
    }

    @Test
    public void handlesAlreadyEnqueued() {
        var existingKey = new BusinessSkuKey(BIZ_ID_SUPPLIER, "existing");
        var lostKey = new BusinessSkuKey(BIZ_ID_SUPPLIER, "lost");

        var offers = List.of(
            offer(1111L, existingKey.getBusinessId(), existingKey.getShopSku(), NEED_CONTENT, 11L, 1, null)
                .setMarketSpecificContentHash(1L),
            offer(1112L, lostKey.getBusinessId(), lostKey.getShopSku(), NEED_CONTENT, 11L, 1, null)
                .setMarketSpecificContentHash(1L)
        );

        offerRepository.insertOffers(offers);

        queue.deleteChangedBeforeByBusinessSkuKeys(List.of(lostKey), Instant.now());
        Assertions.assertThat(queue.findAll()).hasSize(1).allMatch(it -> it.getKey().equals(existingKey));
        // Make offer ready without triggering observer
        jdbcTemplate.update("update " + OfferRepositoryImpl.OFFER_TABLE + " " +
            "set market_specific_content_hash_sent = null " +
            "where supplier_id = " + lostKey.getBusinessId() + " and shop_sku = '" + lostKey.getShopSku() + "'");

        executor.execute();

        Assertions.assertThat(queue.findAll()).hasSize(2).anyMatch(it -> it.getKey().equals(lostKey));
    }

    static Offer offer(long id, int businessId, String shopSku, Offer.ProcessingStatus status,
                       Long categoryId, Integer groupId, String barCode) {
        return Offer.builder()
            .id(id)
            .isDataCampOffer(true)
            .businessId(businessId)
            .shopSku(shopSku)
            .title(shopSku + " title")
            .shopCategoryName(categoryId + " cat name")
            .processingStatus(status)
            .categoryId(categoryId)
            .dataCampContentVersion(0L)
            .groupId(groupId)
            .barCode(barCode)
            .acceptanceStatus(Offer.AcceptanceStatus.OK)
            .build();
    }
}
