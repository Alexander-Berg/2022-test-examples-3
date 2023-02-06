package ru.yandex.market.mboc.common.services.offers.auto_approves;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SuggestAutoApproveServiceImplTest {

    public static final long CATEGORY_ID = 1001L;
    private SuggestAutoApproveServiceImpl service;

    private CategoryInfoRepositoryMock categoryInfoRepository;
    private ModelStorageCachingServiceMock modelCache;
    private OfferMappingActionService offerMappingActionService;
    private AntiMappingRepository antiMappingRepository;

    @Before
    public void setUp() throws Exception {
        var storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        var mboUserRepo = new MboUsersRepositoryMock();
        categoryInfoRepository = new CategoryInfoRepositoryMock(mboUserRepo);
        var categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepository);
        var offerDestinationCalculator = new ContextedOfferDestinationCalculator(categoryInfoCache,
            storageKeyValueServiceMock);
        modelCache = new ModelStorageCachingServiceMock();
        modelCache.setAutoModel(new Model().setId(101).setTitle("some model title"));

        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(null, null, offerDestinationCalculator,
            storageKeyValueServiceMock);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        antiMappingRepository = new AntiMappingRepositoryMock();
        service = new SuggestAutoApproveServiceImpl(categoryInfoRepository, modelCache, offerMappingActionService,
            antiMappingRepository);
    }

    @Test
    public void autoApproveOkWithBarcodeSuggest() {
        var suggest = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(suggest);

        categoryInfoRepository.insertOrUpdate(autoApprovingCategory());

        service.autoApproveIfPossible(List.of(offer));

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(suggest)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE)
            .doesNotHaveContentMapping();
        assertTrue(offer.isAutoApprovedMapping());
    }

    @Test
    public void notAutoApproveWithUncheckedSupplierMappingId() {
        var suggest = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        var supplier = new Offer.Mapping(102, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(suggest);
        offer.setSupplierSkuMapping(supplier);
        offer.setSupplierSkuMappingStatus(Offer.MappingStatus.NEW);

        categoryInfoRepository.insertOrUpdate(autoApprovingCategory());

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    @Test
    public void autoApproveWithUncheckedSupplierMappingIdAndAntiMappingOnIt() {
        var suggest = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        var supplier = new Offer.Mapping(102, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(suggest);
        offer.setSupplierSkuMapping(supplier);
        offer.setSupplierSkuMappingStatus(Offer.MappingStatus.NEW);

        antiMappingRepository.insertOrUpdate(new AntiMapping().setOfferId(1L).setNotSkuId(102L));

        categoryInfoRepository.insertOrUpdate(autoApprovingCategory());

        service.autoApproveIfPossible(List.of(offer));

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(suggest)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE)
            .doesNotHaveContentMapping();
        assertTrue(offer.isAutoApprovedMapping());
    }

    @Test
    public void noAutoApproveWithZeroSuggestMappingId() {
        Offer offer = getOfferWithDeletedApprovedMapping();

        categoryInfoRepository.insertOrUpdate(autoApprovingCategory());

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    @Test
    public void noAutoApproveNewWithBarcodeSuggest() {
        var suggest = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(suggest).setAcceptanceStatusInternal(Offer.AcceptanceStatus.NEW);

        categoryInfoRepository.insertOrUpdate(autoApprovingCategory());

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    @Test
    public void noAutoApproveOkWithParameterSkutchedSuggest() {
        var suggest = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(suggest).setSuggestSkuMappingType(SkuBDApi.SkutchType.SKUTCH_BY_PARAMETERS);

        categoryInfoRepository.insertOrUpdate(autoApprovingCategory());

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    @Test
    public void noAutoApproveInCategoryWithoutAutoApprove() {
        var suggest = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(suggest);

        categoryInfoRepository.insertOrUpdate(noAutoApprovingCategory());

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    @Test
    public void noAutoApproveForAntiMappings() {
        antiMappingRepository.insertOrUpdate(new AntiMapping().setOfferId(1L).setNotSkuId(101L));
        var suggest = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(suggest);

        categoryInfoRepository.insertOrUpdate(autoApprovingCategory());

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    @Test
    public void noAutoApproveForNotDSBSAndNeedInfo() {
        var suggest = new Offer.Mapping(101, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offer = getOffer(suggest);
        offer.setProcessingStatusInternal(Offer.ProcessingStatus.NEED_INFO);
        offer.setMappingDestination(Offer.MappingDestination.BLUE);

        categoryInfoRepository.insertOrUpdate(autoApprovingCategory());

        service.autoApproveIfPossible(List.of(offer));

        assertFalse(offer.hasApprovedSkuMapping());
        assertNull(offer.getApprovedSkuMappingConfidence());
        assertFalse(offer.isAutoApprovedMapping());
    }

    private Offer getOffer(Offer.Mapping suggest) {
        return new Offer()
            .setId(1L)
            .setCategoryIdInternal(CATEGORY_ID)
            .setTitle("Title")
            .setSuggestSkuMapping(suggest)
            .setSuggestSkuMappingType(SkuBDApi.SkutchType.BARCODE_SKUTCH)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);
    }

    private Offer getOfferWithDeletedApprovedMapping() {
        var suggest = new Offer.Mapping(0, LocalDateTime.now(), Offer.SkuType.MARKET);
        var offer = getOffer(suggest);
        offer.setApprovedSkuMappingInternal(suggest);
        return offer;
    }

    private CategoryInfo autoApprovingCategory() {
        var categoryInfo = new CategoryInfo().setCategoryId(CATEGORY_ID);
        categoryInfo.setCategorySuggestAutoApprove(true);

        return categoryInfo;
    }

    private CategoryInfo noAutoApprovingCategory() {
        var categoryInfo = new CategoryInfo().setCategoryId(CATEGORY_ID);
        categoryInfo.setCategorySuggestAutoApprove(false);

        return categoryInfo;
    }
}
