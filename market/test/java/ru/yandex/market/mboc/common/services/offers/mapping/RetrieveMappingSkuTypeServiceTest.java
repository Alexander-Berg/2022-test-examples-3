package ru.yandex.market.mboc.common.services.offers.mapping;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class RetrieveMappingSkuTypeServiceTest extends BaseDbTestClass {

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private SupplierRepository supplierRepository;

    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;

    private RetrieveMappingSkuTypeService retrieveMappingSkuTypeService;

    @Before
    public void setUp() {
        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingServiceMock, offerBatchProcessor, supplierRepository
        );
    }

    @Test
    public void testSkuTypeRetrieved() {
        offerRepository.insertOffer(OfferTestUtils.simpleOffer()
            .setId(1)
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L))
            .setSupplierSkuMapping(OfferTestUtils.mapping(2L))
            .setContentSkuMapping(OfferTestUtils.mapping(3L))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(3L), Offer.MappingConfidence.CONTENT)
        );
        modelStorageCachingServiceMock
            .addModel(new Model()
                .setId(1L)
                .setTitle("Test msku")
                .setCategoryId(1L)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.OPERATOR)
                .setPublishedOnBlueMarket(true))
            .addModel(new Model()
                .setId(2L)
                .setTitle("Test psku10")
                .setCategoryId(1L)
                .setModelType(Model.ModelType.PARTNER_SKU)
                .setModelQuality(Model.ModelQuality.PARTNER)
                .setPublishedOnBlueMarket(true))
            .addModel(new Model()
                .setId(3L)
                .setTitle("Test psku20")
                .setCategoryId(1L)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER)
                .setPublishedOnBlueMarket(true));

        retrieveMappingSkuTypeService.retrieveMappingSkuTypeAndUpdate(List.of(1L));

        Offer offer = offerRepository.getOfferById(1L);
        MbocAssertions.assertThat(offer)
            .hasSuggestedMapping(OfferTestUtils.mapping(1L).copyWithSkuType(Offer.SkuType.MARKET))
            .hasSupplierMapping(OfferTestUtils.mapping(2L).copyWithSkuType(Offer.SkuType.PARTNER10))
            .hasContentMapping(OfferTestUtils.mapping(3L).copyWithSkuType(Offer.SkuType.PARTNER20))
            .hasApprovedMapping(OfferTestUtils.mapping(3L).copyWithSkuType(Offer.SkuType.PARTNER20));
    }
}
