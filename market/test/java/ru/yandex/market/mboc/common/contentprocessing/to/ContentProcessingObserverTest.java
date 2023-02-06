package ru.yandex.market.mboc.common.contentprocessing.to;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.assertions.custom.OfferAssertions;
import ru.yandex.market.mboc.common.contentprocessing.to.service.ReadyForContentProcessingServiceImpl;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.CONTENT_PROCESSING;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.NEED_CONTENT;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BIZ_ID_SUPPLIER;

public class ContentProcessingObserverTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;

    @Before
    public void setUp() {
        supplierRepository.insert(OfferTestUtils.businessSupplier());
    }

    @Test
    public void checkSendOffersOnlyWithoutRejectedCategories() {
        offerRepository.insertOffers(
            offer(1111L, BIZ_ID_SUPPLIER, "offer1", NEED_CONTENT, 11L, 1, null)
                .setMarketSpecificContentHash(1L),
            offer(1112L, BIZ_ID_SUPPLIER, "offer2", NEED_CONTENT, 11L, 1, null)
                .setSupplierCategoryId(1L).setSupplierCategoryMappingStatus(Offer.MappingStatus.REJECTED)
                .setMarketSpecificContentHash(1L));

        Map<String, Offer> offerMap = offerRepository.findOffersByBusinessSkuKeys(
            new BusinessSkuKey(BIZ_ID_SUPPLIER, "offer1"),
            new BusinessSkuKey(BIZ_ID_SUPPLIER, "offer2")
        ).stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));

        OfferAssertions.assertThat(offerMap.get("offer1")).hasProcessingStatus(CONTENT_PROCESSING);
        OfferAssertions.assertThat(offerMap.get("offer2")).hasProcessingStatus(NEED_CONTENT);
    }

    @Test
    public void testDoNotSendOldNeedContentOffersToContentProcessing() {
        offerRepository.insertOffers(
            offer(1111L, BIZ_ID_SUPPLIER, "newOffer", NEED_CONTENT, 11L, 1, null)
                .setMarketSpecificContentHash(1234L)
                .setCreated(ReadyForContentProcessingServiceImpl.DEFAULT_FROM_CREATED.atStartOfDay().plusDays(1)),
            offer(1112L, BIZ_ID_SUPPLIER, "oldOffer", NEED_CONTENT, 11L, 1, null)
                .setMarketSpecificContentHash(1234L)
                .setCreated(ReadyForContentProcessingServiceImpl.DEFAULT_FROM_CREATED.atStartOfDay().minusDays(1))
        );

        Map<String, Offer> offerMap = offerRepository.findOffersByBusinessSkuKeys(
            new BusinessSkuKey(BIZ_ID_SUPPLIER, "newOffer"),
            new BusinessSkuKey(BIZ_ID_SUPPLIER, "oldOffer")
        ).stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));

        OfferAssertions.assertThat(offerMap.get("newOffer")).hasProcessingStatus(CONTENT_PROCESSING);
        // TODO: Скоро отменится совсем
//        OfferAssertions.assertThat(offerMap.get("oldOffer")).hasProcessingStatus(NEED_CONTENT);
    }

    @Test
    public void testDoNotSendOldModifiedNeedContentOffersToContentProcessing() {
        offerRepository.insertOffers(
            offer(1111L, BIZ_ID_SUPPLIER, "oldOffer", NEED_CONTENT, 11L, 1, null)
                .setMarketSpecificContentHash(1234L)
                .setContentChangedTs(ReadyForContentProcessingServiceImpl.DEFAULT_FROM_CONTENT_CHANGED
                    .atStartOfDay().minusDays(1))
                .setCreated(ReadyForContentProcessingServiceImpl.DEFAULT_FROM_CREATED.atStartOfDay().plusDays(1)),
            offer(1112L, BIZ_ID_SUPPLIER, "newOffer", NEED_CONTENT, 11L, 1, null)
                .setMarketSpecificContentHash(1234L)
                .setContentChangedTs(ReadyForContentProcessingServiceImpl.DEFAULT_FROM_CONTENT_CHANGED
                    .atStartOfDay().plusDays(1))
                .setCreated(ReadyForContentProcessingServiceImpl.DEFAULT_FROM_CREATED.atStartOfDay().plusDays(1))
        );

        Map<String, Offer> offerMap = offerRepository.findOffersByBusinessSkuKeys(
            new BusinessSkuKey(BIZ_ID_SUPPLIER, "newOffer"),
            new BusinessSkuKey(BIZ_ID_SUPPLIER, "oldOffer")
        ).stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));

        OfferAssertions.assertThat(offerMap.get("newOffer")).hasProcessingStatus(CONTENT_PROCESSING);
        OfferAssertions.assertThat(offerMap.get("oldOffer")).hasProcessingStatus(NEED_CONTENT);
    }

    @Test
    public void checkDoNotSendPartnerConfidenceMapping() {
        offerRepository.insertOffers(
            offer(1111L, 1000, "offer1", NEED_CONTENT, 11L, 1, null)
                .updateApprovedSkuMapping(
                    new Offer.Mapping(123L, LocalDateTime.now()), Offer.MappingConfidence.PARTNER)
                .setMarketSpecificContentHash(1L),
            offer(1112L, 1000, "offer2", NEED_CONTENT, 11L, 1, null)
                .updateApprovedSkuMapping(
                    new Offer.Mapping(123L, LocalDateTime.now()), Offer.MappingConfidence.PARTNER_SELF)
                .setMarketSpecificContentHash(1L));

        Map<String, Offer> offerMap = offerRepository.findOffersByBusinessSkuKeys(
            new BusinessSkuKey(1000, "offer1"),
            new BusinessSkuKey(1000, "offer2")
        ).stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));

        OfferAssertions.assertThat(offerMap.get("offer1")).hasProcessingStatus(NEED_CONTENT);
        OfferAssertions.assertThat(offerMap.get("offer2")).hasProcessingStatus(CONTENT_PROCESSING);
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
