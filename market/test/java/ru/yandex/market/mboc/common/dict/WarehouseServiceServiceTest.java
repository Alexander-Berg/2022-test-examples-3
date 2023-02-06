package ru.yandex.market.mboc.common.dict;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WarehouseServiceServiceTest extends BaseDbTestClass {
    @SpyBean
    private SupplierRepository supplierRepository;
    @SpyBean
    private OfferRepository offerRepository;
    @SpyBean
    private MskuRepository mskuRepository;
    @SpyBean
    private WarehouseServiceRepository warehouseServiceRepository;

    private WarehouseServiceService service;

    @Before
    public void setup() {
        service = Mockito.spy(
            new WarehouseServiceService(
                warehouseServiceRepository,
                offerRepository,
                supplierRepository,
                mskuRepository,
                Mockito.mock(WarehouseServiceAuditRecorder.class)
            )
        );
    }

    @Test
    public void getAssortmentChildSskus_3P_returnOnlyChildSkus() {
        var requestShopSku = new ShopSkuKey(1, "shopsku1");
        var expectedResponseShopSku = new ShopSkuKey(requestShopSku.getSupplierId(), "shopsku2");

        var supplier1 = supplierRepository.insert(
            new Supplier(requestShopSku.getSupplierId(), "test")
                .setBusinessId(requestShopSku.getSupplierId())
                .setType(MbocSupplierType.THIRD_PARTY)
        );

        Msku childSku1 = createMsku(1, 1);
        Msku childSku2 = createMsku(2, 1);
        Msku notChildSku3 = createMsku(3, 2);

        mskuRepository.save(childSku1, childSku2, notChildSku3);

        var baseChildOffer1 = createOfferWithApprovedSku(
            supplier1.getBusinessId(), requestShopSku.getShopSku(), childSku1.getMarketSkuId(), supplier1
        );

        var childOffer2 = createOfferWithApprovedSku(
            supplier1.getBusinessId(), expectedResponseShopSku.getShopSku(), childSku2.getMarketSkuId(), supplier1
        );

        var notChildOffer3 = createOfferWithApprovedSku(
            supplier1.getBusinessId(), "unexpectedShopsku3", notChildSku3.getMarketSkuId(), supplier1
        );

        offerRepository.insertOffers(baseChildOffer1, childOffer2, notChildOffer3);

        var resultMap = service.getAssortmentChildSskus(
            List.of(baseChildOffer1.getShopSkuKey(supplier1.getId()))
        );

        assertEquals(1, resultMap.size());
        var childSskusResult = resultMap.get(baseChildOffer1.getShopSkuKey(supplier1.getId()));

        assertNull(childSskusResult.getErrorType());
        assertTrue(childSskusResult.getChildSskus().contains(expectedResponseShopSku.getShopSku()));
    }

    @Test
    public void getAssortmentChildSskus_1P_returnOnlyChildSkus() {
        var requestShopSku = new ShopSkuKey(1, "shopsku1");
        var realSupplierId = "1";
        var expectedResponseShopSku = new ShopSkuKey(requestShopSku.getSupplierId(), "shopsku2");

        var supplier1 = supplierRepository.insert(
            new Supplier(requestShopSku.getSupplierId(), "test")
                .setBusinessId(null)
                .setRealSupplierId(realSupplierId)
                .setType(MbocSupplierType.FIRST_PARTY)
        );

        Msku childSku1 = createMsku(1, 1);
        Msku childSku2 = createMsku(2, 1);

        mskuRepository.save(childSku1, childSku2);

        var baseChildOffer1 = createOfferWithApprovedSku(
            supplier1.getId(), requestShopSku.getShopSku(), childSku1.getMarketSkuId(), supplier1
        );
        var childOffer2 = createOfferWithApprovedSku(
            supplier1.getId(), expectedResponseShopSku.getShopSku(), childSku2.getMarketSkuId(), supplier1
        );
        offerRepository.insertOffers(baseChildOffer1, childOffer2);

        var resultMap = service.getAssortmentChildSskus(
            List.of(baseChildOffer1.getShopSkuKey(supplier1.getId()))
        );

        assertEquals(1, resultMap.size());
        var childSskusResult = resultMap.get(baseChildOffer1.getShopSkuKey(supplier1.getId()));

        assertNull(childSskusResult.getErrorType());
        final var externalFormatSsku = realSupplierId + "." + expectedResponseShopSku.getShopSku();
        assertTrue(childSskusResult.getChildSskus().contains(externalFormatSsku));
    }

    @Test
    public void getAssortmentChildSskus_willProcessRequestBatched() {
        var supplier1 = supplierRepository.insert(
            new Supplier(1, "test")
                .setBusinessId(null)
                .setType(MbocSupplierType.FIRST_PARTY)
        );

        var supplier2 = supplierRepository.insert(
            new Supplier(2, "test")
                .setBusinessId(2)
                .setType(MbocSupplierType.THIRD_PARTY)
        );

        Msku childSku1 = createMsku(1, 1);
        Msku childSku2 = createMsku(2, 1);
        Msku childSku3 = createMsku(3, 2);
        Msku childSku4 = createMsku(4, 2);

        mskuRepository.save(childSku1, childSku2, childSku3, childSku4);

        var childOffer1 = createOfferWithApprovedSku(
            supplier1.getId(), "shopsku1", childSku1.getMarketSkuId(), supplier1
        );
        var childOffer11 = createOfferWithApprovedSku(
            supplier1.getId(), "shopsku11", childSku2.getMarketSkuId(), supplier1
        );
        var childOffer2 = createOfferWithApprovedSku(
            supplier2.getBusinessId(), "shopsku2", childSku3.getMarketSkuId(), supplier2
        );
        var childOffer22 = createOfferWithApprovedSku(
            supplier2.getBusinessId(), "shopsku22", childSku4.getMarketSkuId(), supplier2
        );

        offerRepository.insertOffers(childOffer1, childOffer2, childOffer11, childOffer22);

        var resultMap = service.getAssortmentChildSskus(
            List.of(
                childOffer1.getShopSkuKey(supplier1.getId()),
                childOffer2.getShopSkuKey(supplier2.getId())
            )
        );

        assertEquals(2, resultMap.size());

        resultMap.values().forEach(result -> assertFalse(result.getChildSskus().isEmpty()));

        verify(supplierRepository, times(1)).findByIds(anySet());
        verify(offerRepository, times(1)).findOffersByBusinessSkuKeys(anyCollection());
        verify(offerRepository, times(1)).findOffersByBusinessApprovedSkuMappingKeys(anyCollection());
        verify(mskuRepository, times(1)).findByIds(anyCollection());
        verify(mskuRepository, times(1)).find(any());
    }

    @Test
    public void getAssortmentChildSskus_willProcessOnlyRequestSupplier() {
        var supplier1 = supplierRepository.insert(
            new Supplier(1, "test")
                .setBusinessId(1)
                .setType(MbocSupplierType.THIRD_PARTY)
        );

        Msku childSku1 = createMsku(1, 1);
        Msku childSku2 = createMsku(2, 1);

        mskuRepository.save(childSku1, childSku2);
        var childOffer1 = createOfferWithApprovedSku(
            supplier1.getId(), "shopsku1", childSku1.getMarketSkuId(), supplier1
        );
        var childOffer11 = createOfferWithApprovedSku(
            supplier1.getId(), "shopsku11", childSku2.getMarketSkuId(), supplier1
        );

        offerRepository.insertOffers(childOffer1, childOffer11);

        var resultMap = service.getAssortmentChildSskus(
            List.of(
                childOffer1.getShopSkuKey(supplier1.getId()),
                childOffer11.getShopSkuKey(supplier1.getId())
            )
        );

        Assertions.assertThat(resultMap.get(childOffer1.getShopSkuKey(supplier1.getId())))
            .extracting(ChildSskuResult::getChildSskus)
            .matches(sskus -> sskus.contains(childOffer11.getShopSku()));
        Assertions.assertThat(resultMap.get(childOffer11.getShopSkuKey(supplier1.getId())))
            .extracting(ChildSskuResult::getChildSskus)
            .matches(sskus -> sskus.contains(childOffer1.getShopSku()));
    }

    @Test
    public void getAssortmentChildSskus_willSetValidErrors() {
        var shopSkuKeySupplierNotFound = new ShopSkuKey(0, "UnknownShopSku");

        var supplier1 = supplierRepository.insert(
            new Supplier(1, "test1")
                .setBusinessId(1)
                .setType(MbocSupplierType.THIRD_PARTY)
        );
        var shopSkuKeyOfferNotFound = new ShopSkuKey(supplier1.getBusinessId(), "UnknownShopSku");

        var supplier2 = supplierRepository.insert(
            new Supplier(2, "test2")
                .setBusinessId(2)
                .setType(MbocSupplierType.THIRD_PARTY)
        );

        var supplier3 = supplierRepository.insert(
            new Supplier(3, "test3")
                .setBusinessId(3)
                .setType(MbocSupplierType.THIRD_PARTY)
        );

        var offerNotApproved = new Offer()
            .setBusinessId(supplier2.getBusinessId())
            .setShopSku("shopsku2")
            .setTitle("testTitle")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("testName")
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .addNewServiceOfferIfNotExistsForTests(supplier2)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        var offerMskuNotFound = createOfferWithApprovedSku(
            supplier3.getId(), "shopsku3", 100, supplier3
        );

        offerRepository.insertOffers(offerNotApproved, offerMskuNotFound);

        var assortmentChildSskus = service.getAssortmentChildSskus(
            List.of(
                shopSkuKeySupplierNotFound,
                shopSkuKeyOfferNotFound,
                offerNotApproved.getShopSkuKey(supplier2.getId()),
                offerMskuNotFound.getShopSkuKey(supplier3.getId())
            )
        );

        assertEquals(
            ChildSskuResult.ErrorType.SUPPLIER_NOT_FOUND,
            assortmentChildSskus.get(shopSkuKeySupplierNotFound).getErrorType()
        );

        assertEquals(
            ChildSskuResult.ErrorType.NO_OFFER_OR_APPROVED_MAPPING,
            assortmentChildSskus.get(shopSkuKeyOfferNotFound).getErrorType()
        );
        assertEquals(
            ChildSskuResult.ErrorType.NO_OFFER_OR_APPROVED_MAPPING,
            assortmentChildSskus.get(offerNotApproved.getShopSkuKey(supplier2.getId())).getErrorType()
        );
        assertEquals(
            ChildSskuResult.ErrorType.MSKU_NOT_FOUND,
            assortmentChildSskus.get(offerMskuNotFound.getShopSkuKey(supplier3.getId())).getErrorType()
        );
    }

    @Test
    public void saveOrUpdateWarehouseServices_shouldSaveOnlyHeadSskus() {
        var requestShopSku = new ShopSkuKey(1, "shopsku1");
        var realSupplierId = "1";
        var expectedResponseShopSku = new ShopSkuKey(requestShopSku.getSupplierId(), "shopsku2");

        var supplier1 = supplierRepository.insert(
            new Supplier(requestShopSku.getSupplierId(), "test")
                .setBusinessId(null)
                .setRealSupplierId(realSupplierId)
                .setType(MbocSupplierType.FIRST_PARTY)
        );

        Msku headChildSku = createMsku(1, 1)
            .setCargoTypeLmsIds(1L, 110L);
        Msku leafChildSku = createMsku(2, 1)
            .setCargoTypeLmsIds(1L);

        mskuRepository.save(headChildSku, leafChildSku);

        var headChildOffer = createOfferWithApprovedSku(
            supplier1.getId(), requestShopSku.getShopSku(), headChildSku.getMarketSkuId(), supplier1
        );
        var leafChildOffer = createOfferWithApprovedSku(
            supplier1.getId(), expectedResponseShopSku.getShopSku(), leafChildSku.getMarketSkuId(), supplier1
        );
        offerRepository.insertOffers(headChildOffer, leafChildOffer);

        var headService = WarehouseService.builder()
            .supplierId(headChildOffer.getBusinessId())
            .shopSku(headChildOffer.getShopSku())
            .needSort(true)
            .build();
        var leafService = WarehouseService.builder()
            .supplierId(leafChildOffer.getBusinessId())
            .shopSku(leafChildOffer.getShopSku())
            .needSort(true)
            .build();
        var resultMap = service.saveOrUpdateWarehouseServices(
            List.of(headService, leafService), "testUsername"
        );

        assertEquals(2, resultMap.size());
        var headChildSskusResult = resultMap.get(headService);
        var leafChildSskusResult = resultMap.get(leafService);

        assertNull(headChildSskusResult.getErrorType());
        final var externalFormatSsku = realSupplierId + "." + expectedResponseShopSku.getShopSku();
        assertTrue(headChildSskusResult.getChildSskus().contains(externalFormatSsku));
        assertNotNull(leafChildSskusResult.getErrorType());
        assertEquals(leafChildSskusResult.getErrorType(), ChildSskuResult.ErrorType.NOT_HEAD_SSKU);
    }

    private Offer createOfferWithApprovedSku(int businessId, String shopSku, long skuId, Supplier supplier) {
        return new Offer()
            .setBusinessId(businessId)
            .setShopSku(shopSku)
            .setTitle("testTitle")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("testName")
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setApprovedSkuMappingInternal(new Offer.Mapping(skuId, LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .addNewServiceOfferIfNotExistsForTests(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
    }

    private Msku createMsku(long skuId, long parentModelId) {
        return new Msku()
            .setMarketSkuId(skuId)
            .setParentModelId(parentModelId)
            .setCategoryId(1L)
            .setVendorId(1L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
    }
}
