package ru.yandex.market.mboc.app.tool;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class MigratePartnerSkuTypeToolTest extends BaseMbocAppTest {

    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    private RetrieveMappingSkuTypeService retrieveMappingSkuTypeService;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;

    private MigratePartnerSkuTypeTool migratePartnerSkuTypeTool;

    @Before
    public void setUp() throws Exception {
        categoryCachingServiceMock = new CategoryCachingServiceMock();
        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();

        retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock,
            offerBatchProcessor, supplierRepository);
        migratePartnerSkuTypeTool = new MigratePartnerSkuTypeTool(offerBatchProcessor, retrieveMappingSkuTypeService);

        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void testMigratePartnerSkuTypeTool() throws Exception {
        Offer offer = OfferTestUtils.nextOffer()
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L, Offer.SkuType.PARTNER10))
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);

        offerRepository.insertOffers(offer);
        categoryCachingServiceMock.addCategory(1);
        modelStorageCachingServiceMock.addModel(new Model().setId(1)
            .setCategoryId(1)
            .setModelType(Model.ModelType.SKU)
            .setModelQuality(Model.ModelQuality.PARTNER));

        migratePartnerSkuTypeTool.call();

        var updated = offerRepository.getOfferById(offer.getId());
        Assert.assertEquals(Offer.SkuType.PARTNER20, updated.getSuggestSkuMapping().getSkuType());
    }
}
