package ru.yandex.market.mboc.app.proto.mappings;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.app.proto.MboMappingsServiceImpl;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MskuOfferJoin;
import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuCargoParameter;
import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuParameters;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.SearchMappingsBySupplierIdRequest;
import ru.yandex.market.mboc.http.MboMappings.SearchMappingsResponse;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author danfertev
 * @since 31.07.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MboMappingsServiceImplSearchMappingsByShopIdTest extends AbstractMboMappingsServiceImplTest {

    @Test
    public void testFindMappingsByShopIdMasterData() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setReturnMasterData(true)
            .setSupplierId(61)
            .build()
        );

        assertEquals(2, response.getOffersCount());
        Assertions.assertThat(response.getOffers(0).getMasterDataInfo()
            .getProviderProductMasterData()
            .getCustomsCommodityCode()
        ).isEqualTo("cccode4");
    }

    @Test
    public void testFindMappingsByShopIdNoMasterData() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(61)
            .build()
        );

        assertEquals(2, response.getOffersCount());
        Assertions.assertThat(response.getOffers(0).hasMasterDataInfo()).isFalse();
    }

    @Test
    public void testFindByShop() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setLimit(2)
            .setSupplierId(42)
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(1, response.getOffers(0).getInternalOfferId());
        assertEquals(2, response.getOffers(1).getInternalOfferId());
        assertTrue(response.hasNextOffsetKey());
        assertFalse(response.hasTotalCount());

        response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setLimit(3)
            .setOffsetKey(response.getNextOffsetKey())
            .setSupplierId(42)
            .build());
        assertEquals(2, response.getOffersCount());
        assertEquals(3, response.getOffers(0).getInternalOfferId());
        assertFalse(response.hasNextOffsetKey());
    }

    @Test
    public void testFindByShopAndProcessingStatus() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .addOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
            .setSupplierId(60)
            .build());

        assertEquals(1, response.getOffersCount());
        assertEquals(4, response.getOffers(0).getInternalOfferId());
    }

    @Test
    public void testFindWithPagingByShopSku() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(1001)
            .setLimit(3)
            .build());
        String shopSkuId = response.getOffers(2).getShopSkuId();
        assertEquals(shopSkuId, "sku2");
        response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(1001)
            .setLastShopSku(shopSkuId)
            .build());
        assertEquals(2, response.getOffersCount());
        assertEquals("sku37", response.getOffers(1).getShopSkuId());
    }

    @Test
    public void testGetTotalCount() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setLimit(2)
            .setSupplierId(42)
            .setReturnTotalCount(true)
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(1, response.getOffers(0).getInternalOfferId());
        assertEquals(2, response.getOffers(1).getInternalOfferId());
        assertTrue(response.hasNextOffsetKey());
        assertTrue(response.hasTotalCount());
        assertEquals(4, response.getTotalCount());
    }

    @Test
    public void testFindByShopAndTitle() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(42)
            .setTextQueryString("search")
            .build());

        assertEquals(1, response.getOffersCount());
        assertEquals(1, response.getOffers(0).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndSku() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(42)
            .setTextQueryString("sku3")
            .build());

        assertEquals(1, response.getOffersCount());
        assertEquals(3, response.getOffers(0).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndCategory() {
        Category category33 = new Category().setCategoryId(33).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingService.addCategory(category33);

        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(42)
            .addMarketCategoryIds(33)
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(2, response.getOffers(0).getInternalOfferId());
        assertEquals(3, response.getOffers(1).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndCategories() {
        Category category66 = new Category().setCategoryId(66).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        Category category77 = new Category().setCategoryId(77).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingService.addCategory(category66);
        categoryCachingService.addCategory(category77);

        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(43)
            .addMarketCategoryIds(66)
            .addMarketCategoryIds(77)
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(15, response.getOffers(0).getInternalOfferId());
        assertEquals(16, response.getOffers(1).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndSupplierStatus() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(42)
            .addHasSupplierMappingStatus(SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus.RE_SORT)
            .build());

        assertEquals(1, response.getOffersCount());
        assertEquals(3, response.getOffers(0).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndSupplierTwoStatus() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(42)
            .addHasSupplierMappingStatus(SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus.RE_SORT)
            .addHasSupplierMappingStatus(SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus.MODERATION)
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(2, response.getOffers(0).getInternalOfferId());
        assertEquals(3, response.getOffers(1).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndSupplierNoStatus() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(79)
            .addHasNoSupplierMappingStatus(SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus.MODERATION)
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(7, response.getOffers(0).getInternalOfferId());
        assertEquals(9, response.getOffers(1).getInternalOfferId());
    }

    @Test
    public void testSearchMappingsByShopIdHasMasterData() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(BERU_ID)
            .setReturnMasterData(true)
            .build());

        Assertions.assertThat(response.getOffersList())
            .extracting(
                o -> o.getSupplierId(),
                o -> o.getShopSkuId(),
                o -> o.getMasterDataInfo().getProviderProductMasterData().getCustomsCommodityCode()
            )
            .contains(
                Assertions.tuple((long) BERU_ID, "000042.sku5", "cccode5")
            );
    }

    @Test
    public void testFindByShopAndHasMapping() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(79)
            .setHasAnyMapping(MboMappings.MappingKind.SUGGEST_MAPPING)
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(8, response.getOffers(0).getInternalOfferId());
        assertEquals(9, response.getOffers(1).getInternalOfferId());
    }

    @Test
    public void testFindByShop1P() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setLimit(3)
            .setSupplierId(BERU_ID)
            .build());

        assertEquals(3, response.getOffersCount());
        assertEquals(10, response.getOffers(0).getInternalOfferId());
        assertEquals(11, response.getOffers(1).getInternalOfferId());
        assertEquals(14, response.getOffers(2).getInternalOfferId());
        assertTrue(response.hasNextOffsetKey());

        response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setLimit(3)
            .setOffsetKey(response.getNextOffsetKey())
            .setSupplierId(BERU_ID)
            .build());
        assertEquals(1, response.getOffersCount());
        assertEquals(5, response.getOffers(0).getInternalOfferId());
        assertFalse(response.hasNextOffsetKey());
    }

    @Test
    public void testFindByShop1PBlocked() {
        storageKeyValueService.putValue(MboMappingsServiceImpl.BERU_BLOCKED_KEY, true);
        Assertions.assertThatThrownBy(() -> service.searchMappingsByShopId(
            SearchMappingsBySupplierIdRequest.newBuilder()
                .setLimit(3)
                .setSupplierId(BERU_ID)
                .build()))
            .hasMessageContaining("Temporarily blocked");
    }

    @Test
    public void testFindByShopIdFMCGSupplier() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(80)
            .build());

        assertEquals(4, response.getOffersCount());

        response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(80)
            .setHasAnyMapping(MboMappings.MappingKind.APPROVED_MAPPING)
            .build());
        assertEquals(2, response.getOffersCount());
        assertEquals(20, response.getOffers(0).getInternalOfferId());
        assertEquals(21, response.getOffers(1).getInternalOfferId());
        assertTrue(response.getOffers(1).hasApprovedMapping());
        assertFalse(response.getOffers(1).hasSuggestMapping());

        response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(80)
            .setHasAnyMapping(MboMappings.MappingKind.SUGGEST_MAPPING)
            .build());
        assertEquals(2, response.getOffersCount());
        assertEquals(22, response.getOffers(0).getInternalOfferId());
        assertFalse(response.getOffers(0).hasApprovedMapping());
        assertTrue(response.getOffers(0).hasSuggestMapping());
        assertEquals(24, response.getOffers(1).getInternalOfferId());
        assertFalse(response.getOffers(1).hasApprovedMapping());
        assertTrue(response.getOffers(1).hasSuggestMapping());
        assertTrue(response.getOffers(1).hasSupplierMapping());
    }

    @Test
    public void testSimultaneousUsageOfHasAnyMappingAndMappingFilters() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(82)
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                .build())
            .setHasAnyMapping(MboMappings.MappingKind.SUGGEST_MAPPING)
            .build());

        assertEquals(SearchMappingsResponse.Status.ERROR, response.getStatus());
    }

    @Test
    public void testFindByShopAndByMappingSingleFilterWithoutSkuType() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(82)
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build())
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(27, response.getOffers(0).getInternalOfferId());
        assertEquals(28, response.getOffers(1).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndBySingleMappingFilterWithSkuType() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(82)
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .setMappingSkuKind(MboMappings.MappingSkuKind.PARTNER)
                .build())
            .build());

        assertEquals(1, response.getOffersCount());
        assertEquals(28, response.getOffers(0).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndByMultipleMappingFilterWithoutSkuType() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(82)
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build())
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                .build())
            .build());

        assertEquals(3, response.getOffersCount());
        assertEquals(26, response.getOffers(0).getInternalOfferId());
        assertEquals(27, response.getOffers(1).getInternalOfferId());
        assertEquals(28, response.getOffers(2).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndByMultipleMappingFilterWithSkuType() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(82)
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .setMappingSkuKind(MboMappings.MappingSkuKind.PARTNER)
                .build())
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.SUPPLIER_MAPPING)
                .build())
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(26, response.getOffers(0).getInternalOfferId());
        assertEquals(28, response.getOffers(1).getInternalOfferId());
    }

    @Test
    public void testFindByShopAndByMultipleMappingFilter() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(82)
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .setMappingSkuKind(MboMappings.MappingSkuKind.PARTNER)
                .build())
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build())
            .build());

        assertEquals(2, response.getOffersCount());
        assertEquals(27, response.getOffers(0).getInternalOfferId());
        assertEquals(28, response.getOffers(1).getInternalOfferId());
    }

    @Test
    public void testModelIsEditableQueryFilter() {
        SearchMappingsResponse response = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(83)
            .addOfferQueriesAnyOf(MboMappings.OfferQuery.MODEL_IS_EDITABLE)
            .build());
        response.getOffersList().forEach(o ->
            assertTrue("approved confidence should be PARTNER_SELF or need_content should be TRUE",
                o.getProcessingStatus() == SupplierOffer.OfferProcessingStatus.NEED_CONTENT
                    || o.getProcessingStatus() == SupplierOffer.OfferProcessingStatus.CONTENT_PROCESSING
                    || o.getApprovedMappingConfidence() ==
                        SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER_SELF));
        assertEquals(3, response.getOffersCount());
    }

    @Test
    public void testReadyToContentProcessingQueryFilter() {
        SearchMappingsResponse toUpdate = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(83)
            .addOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
            .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                .setMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .setMappingSkuKind(MboMappings.MappingSkuKind.PARTNER))
            .build());
        SearchMappingsResponse toCreate = service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
            .setSupplierId(83)
            .addOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT)
            .addOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.CONTENT_PROCESSING)
            .build());

        SearchMappingsResponse queryResponse =
            service.searchMappingsByShopId(SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(83)
                .addOfferQueriesAnyOf(MboMappings.OfferQuery.READY_TO_CONTENT_PROCESSING_PARTNER_OFFERS)
                .build());

        assertEquals(queryResponse.getOffersCount(), toUpdate.getOffersCount() + toCreate.getOffersCount());
        List<Long> filterIds = Stream.of(toUpdate, toCreate)
            .map(SearchMappingsResponse::getOffersList)
            .map(Collection::stream)
            .reduce(Stream::concat)
            .orElse(Stream.empty())
            .map(SupplierOffer.Offer::getInternalOfferId)
            .collect(Collectors.toList());

        Assertions.assertThat(queryResponse.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrderElementsOf(filterIds);
    }

    @Test
    public void testFindByVendor() {
        SearchMappingsResponse vendorDoesNotExist = service.searchMappingsByShopId(
            SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(42)
                .addVendors("vendorDoesNotExist")
                .build());
        SearchMappingsResponse supplierDoesNotExist = service.searchMappingsByShopId(
            SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(10033497)
                .addVendors("Samsung")
                .build());
        Assertions.assertThat(vendorDoesNotExist.getOffersList()).isEmpty();
        Assertions.assertThat(supplierDoesNotExist.getOffersList()).isEmpty();

        SearchMappingsResponse simple1 = service.searchMappingsByShopId(
            SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(42)
                .addVendors("Samsung")
                .addVendors("Apple")
                .build());
        Assertions.assertThat(simple1.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrder(1L, 2L, 3L);

        SearchMappingsResponse simple2 = service.searchMappingsByShopId(
            SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(42)
                .addVendors("Apple")
                .build());
        Assertions.assertThat(simple2.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrder(3L);

        SearchMappingsResponse simple3 = service.searchMappingsByShopId(
            SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(79)
                .addVendors("Samsung") // actual vendor for 79 is "SamsunG"
                .build());
        Assertions.assertThat(simple3.getOffersList()).isEmpty();
    }

    @Test
    public void testSupplierOnBusiness() {
        offerRepository.insertOffers(List.of(
            offer(1001, 10000, "bizsku1").setServiceOffers(List.of(
                new Offer.ServiceOffer(10001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            ))
        ));

        SearchMappingsResponse findOnSupplier = service.searchMappingsByShopId(
            SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(10001)
                .build());
        Assertions.assertThat(findOnSupplier.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactly(1001L);
        Assertions.assertThat(findOnSupplier.getOffersList())
            .extracting(SupplierOffer.Offer::getSupplierId)
            .containsExactly(10001L);

        SearchMappingsResponse notFoundServiceOffer = service.searchMappingsByShopId(
            SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(10002)
                .build());
        Assertions.assertThat(notFoundServiceOffer.getOffersList()).isEmpty();
    }

    @Test
    public void testFilterCargoTypes() {
        MskuCargoParameter cargo985 = new MskuCargoParameter(MskuOfferJoin.CARGO_TYPE_985_PARAM_ID,
            "cargoType985", 123L);

        long mskuId1 = 101010L;
        var mskuParameters1 = new MskuParameters();
        mskuParameters1.setCargoParameters(Map.of(MskuOfferJoin.CARGO_TYPE_985_PARAM_ID, cargo985));
        Msku msku1 = TestUtils.newMsku(mskuId1, TestUtils.DEFAULT_CATEGORY_ID);
        msku1.setMskuParameterValues(mskuParameters1);
        mskuRepository.save(msku1);

        SearchMappingsResponse response = service.searchMappingsByShopId(
            SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(60)
                .addApprovedMappingCargoTypeFilters(SearchMappingsBySupplierIdRequest.CargoTypeFilter.newBuilder()
                    .setCargoType985(true).build())
                .build());
        Assertions.assertThat(response.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrder(4L);
        var offer = response.getOffersList().get(0);
        assertTrue(offer.getApprovedMappingSkuCargoType().getCargoType985());
    }
}
