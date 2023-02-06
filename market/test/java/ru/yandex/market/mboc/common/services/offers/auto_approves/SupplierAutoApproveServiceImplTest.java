package ru.yandex.market.mboc.common.services.offers.auto_approves;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.context.ApprovedSkuMappingContext;
import ru.yandex.market.mboc.common.services.offers.mapping.context.SkuMappingContext;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SupplierAutoApproveServiceImplTest {

    public static final long CATEGORY_ID = 1001L;
    private SupplierAutoApproveServiceImpl service;

    private CategoryInfoRepositoryMock categoryInfoRepository;
    private ModelStorageCachingServiceMock modelCache;
    private OfferMappingActionService offerMappingActionService;
    private AntiMappingRepository antiMappingRepository;


    @Before
    public void setUp() throws Exception {
        var mboUserRepo = new MboUsersRepositoryMock();
        categoryInfoRepository = new CategoryInfoRepositoryMock(mboUserRepo);
        var categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepository);
        var offerDestinationCalculator = new ContextedOfferDestinationCalculator(categoryInfoCache,
            new StorageKeyValueServiceMock());
        modelCache = new ModelStorageCachingServiceMock();
        modelCache.setAutoModel(new Model().setId(101).setTitle("some model title"));

        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(null, null, offerDestinationCalculator,
            new StorageKeyValueServiceMock());
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        antiMappingRepository = new AntiMappingRepositoryMock();
        service = new SupplierAutoApproveServiceImpl(modelCache, offerMappingActionService, antiMappingRepository);
    }

    @Test
    public void autoApproveOkWithDatacampCardSourceEqualsSupplierMapping() {
        var supplierMapping = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(supplierMapping);
        offer.setDatacampSkuIdFromSearch(101L);

        service.autoApproveIfPossible(List.of(offer));

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(supplierMapping)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE)
            .doesNotHaveContentMapping();
        assertTrue(offer.isAutoApprovedMapping());
    }

    @Test
    public void notAutoApproveWithDatacampCardSourceNonEqualsSupplierMapping() {
        var supplierMapping = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(supplierMapping);
        offer.setDatacampSkuIdFromSearch(102L);

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    @Test
    public void notAutoApproveWithDatacampCardSourceIgnoreByAntiMapping() {
        var supplierMapping = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(supplierMapping);

        antiMappingRepository.insertOrUpdate(new AntiMapping().setOfferId(1L).setNotSkuId(101L));

        offer.setDatacampSkuIdFromSearch(101L);

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    @Test
    public void skipAutoApproveWithoutDatacampCardSource() {
        var supplierMapping = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(supplierMapping);

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    @Test
    public void updateFilledApprovedMappingId() {
        var supplierMapping = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(supplierMapping);
        offer.setDatacampSkuIdFromSearch(101L);
        Model model = new Model().setId(1L).setSkuParentModelId(1L).setCategoryId(1L);
        offerMappingActionService.APPROVED.setSkuMapping(
            offer,
            new ApprovedSkuMappingContext()
                .setSkuMappingContext(SkuMappingContext.fromSku(model))
                .setMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
        );

        assertFalse(offer.isAutoApprovedMapping());
        assertEquals(offer.getApprovedSkuId(), Long.valueOf(model.getId()));
        service.autoApproveIfPossible(List.of(offer));

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(supplierMapping)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE)
            .doesNotHaveContentMapping()
            .hasApprovedMapping(offer.getDatacampSkuIdFromSearch());
        assertTrue(offer.isAutoApprovedMapping());
    }

    @Test
    public void updateFilledApprovedMappingIdWhenStatusIsNotNew() {
        long fromSearchSkuId = 101L;
        var supplierMapping = new Offer.Mapping(fromSearchSkuId, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(supplierMapping);
        offer
            .setDatacampSkuIdFromSearch(fromSearchSkuId)
            .setDatacampSkuIdFromSearchStatus(Offer.MappingStatus.ACCEPTED);
        Model approvedSku = new Model().setId(1L).setSkuParentModelId(1L).setCategoryId(1L);
        offerMappingActionService.APPROVED.setSkuMapping(
            offer,
            new ApprovedSkuMappingContext()
                .setSkuMappingContext(SkuMappingContext.fromSku(approvedSku))
                .setMappingConfidence(Offer.MappingConfidence.CONTENT)
        );

        assertThat(offer.isAutoApprovedMapping()).isFalse();
        assertThat(offer.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.CONTENT);
        assertThat(offer.getApprovedSkuId()).isEqualTo(approvedSku.getId());

        var approvedSkuMapping = offer.getApprovedSkuMapping();

        service.autoApproveIfPossible(List.of(offer));

        // nothing changed
        assertThat(offer.isAutoApprovedMapping()).isFalse();
        assertThat(offer.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.CONTENT);
        assertThat(offer.getApprovedSkuId()).isEqualTo(approvedSku.getId());
    }

    private Offer getOffer(Offer.Mapping supplierMapping) {
        return new Offer()
            .setId(1L)
            .setCategoryIdInternal(CATEGORY_ID)
            .setTitle("Title")
            .setSupplierSkuMapping(supplierMapping)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);
    }

}
