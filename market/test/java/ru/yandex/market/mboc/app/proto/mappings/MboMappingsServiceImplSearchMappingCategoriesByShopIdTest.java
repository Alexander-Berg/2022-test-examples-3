package ru.yandex.market.mboc.app.proto.mappings;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.SearchMappingCategoriesRequest;
import ru.yandex.market.mboc.http.MboMappings.SearchMappingCategoriesResponse;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author danfertev
 * @since 31.07.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MboMappingsServiceImplSearchMappingCategoriesByShopIdTest extends AbstractMboMappingsServiceImplTest {
    @Test
    public void testFindCategoriesByShopAndProcessingStatus() {
        SearchMappingCategoriesResponse response = service.searchMappingCategoriesByShopId(
            SearchMappingCategoriesRequest.newBuilder()
                .addOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
                .setSupplierId(60)
                .build());

        assertEquals(1, response.getCategoriesCount());
        assertEquals(33, response.getCategories(0).getCategoryId());
        assertEquals(1, response.getCategories(0).getOfferCount());
    }

    @Test
    public void testCategories() {
        SearchMappingCategoriesResponse response = service.searchMappingCategoriesByShopId(
            SearchMappingCategoriesRequest.newBuilder().setSupplierId(1001).build());

        assertEquals(2, response.getCategoriesCount());

        assertEquals(12, response.getCategories(0).getCategoryId());
        assertEquals(4, response.getCategories(0).getOfferCount());
        assertEquals(2, response.getCategories(0).getOfferMappedOnPskuCount());
        assertEquals(33, response.getCategories(1).getCategoryId());
        assertEquals(1, response.getCategories(1).getOfferCount());
        assertEquals(0, response.getCategories(1).getOfferMappedOnPskuCount());
    }

    @Test
    public void testFindMappingCategoriesByMarketCategoryId() {
        Category category = new Category().setCategoryId(33).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingService.addCategory(category);

        SearchMappingCategoriesResponse response =
            service.searchMappingCategoriesByShopId(SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(42)
                .addMarketCategoryIds(33)
                .build());

        assertEquals(1, response.getCategoriesCount());
        assertEquals(33, response.getCategories(0).getCategoryId());
        assertEquals(2, response.getCategories(0).getOfferCount());
    }

    @Test
    public void testFindMappingCategoriesByHasAnyMappingAndMappingFilters() {
        SearchMappingCategoriesResponse response =
            service.searchMappingCategoriesByShopId(SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(82)
                .setHasAnyMapping(MboMappings.MappingKind.APPROVED_MAPPING)
                .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                    .setMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                    .build())
                .build());

        assertEquals(SearchMappingCategoriesResponse.Status.ERROR, response.getStatus());
    }

    @Test
    public void testFindMappingCategoriesByMappingFilters() {
        SearchMappingCategoriesResponse response =
            service.searchMappingCategoriesByShopId(SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(82)
                .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                    .setMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                    .setMappingSkuKind(MboMappings.MappingSkuKind.MARKET)
                    .build())
                .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                    .setMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                    .build())
                .build());

        assertEquals(1, response.getCategoriesCount());
        assertEquals(11, response.getCategories(0).getCategoryId());
        assertEquals(2, response.getCategories(0).getOfferCount());
    }

    @Test
    public void testReadyToContentProcessingQueryFilter() {
        SearchMappingCategoriesResponse toUpdate =
            service.searchMappingCategoriesByShopId(SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(83)
                .addOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
                .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                    .setMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                    .setMappingSkuKind(MboMappings.MappingSkuKind.PARTNER))
                .build());
        SearchMappingCategoriesResponse toCreate =
            service.searchMappingCategoriesByShopId(SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(83)
                .addOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT)
                .addOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.CONTENT_PROCESSING)
                .build());

        SearchMappingCategoriesResponse queryResponse =
            service.searchMappingCategoriesByShopId(SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(83)
                .addOfferQueriesAnyOf(MboMappings.OfferQuery.READY_TO_CONTENT_PROCESSING_PARTNER_OFFERS)
                .build());

        assertEquals(queryResponse.getCategories(0).getOfferCount(),
            toUpdate.getCategories(0).getOfferCount() + toCreate.getCategories(0).getOfferCount());
    }

    @Test
    public void testVendorsFilter() {
        MboMappings.SearchMappingCategoriesResponse vendorDoesNotExist = service.searchMappingCategoriesByShopId(
            MboMappings.SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(42)
                .addVendors("vendorDoesNotExist")
                .build());
        MboMappings.SearchMappingCategoriesResponse supplierDoesNotExist = service.searchMappingCategoriesByShopId(
            MboMappings.SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(10033497)
                .addVendors("Samsung")
                .build());
        assertThat(vendorDoesNotExist.getCategoriesList()).isEmpty();
        assertThat(supplierDoesNotExist.getCategoriesList()).isEmpty();

        MboMappings.SearchMappingCategoriesResponse simple1 = service.searchMappingCategoriesByShopId(
            MboMappings.SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(42)
                .addVendors("Samsung")
                .addVendors("Apple")
                .build());
        assertThat(simple1.getCategoriesList())
            .extracting(SearchMappingCategoriesResponse.CategoryInfo::getCategoryId)
            .containsExactlyInAnyOrder(12, 33);

        MboMappings.SearchMappingCategoriesResponse simple2 = service.searchMappingCategoriesByShopId(
            MboMappings.SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(42)
                .addVendors("Apple")
                .build());
        assertThat(simple2.getCategoriesList())
            .extracting(SearchMappingCategoriesResponse.CategoryInfo::getCategoryId)
            .containsExactlyInAnyOrder(33);

        MboMappings.SearchMappingCategoriesResponse simple3 = service.searchMappingCategoriesByShopId(
            MboMappings.SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(79)
                .addVendors("Samsung") // actual vendor for 79 is "SamsunG"
                .build());
        assertThat(simple3.getCategoriesList()).isEmpty();
    }

    @Test
    public void testSupplierOnBusiness() {
        offerRepository.insertOffers(List.of(
            offer(1001, 10000, "bizsku1").setServiceOffers(List.of(
                new Offer.ServiceOffer(10001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            ))
        ));

        MboMappings.SearchMappingCategoriesResponse findOnBiz = service.searchMappingCategoriesByShopId(
            MboMappings.SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(10000)
                .build());
        assertThat(findOnBiz.getCategoriesList())
            .extracting(SearchMappingCategoriesResponse.CategoryInfo::getCategoryId)
            .containsExactlyInAnyOrder(12345);

        MboMappings.SearchMappingCategoriesResponse findOnSupplier = service.searchMappingCategoriesByShopId(
            MboMappings.SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(10001)
                .build());
        assertThat(findOnSupplier.getCategoriesList())
            .extracting(SearchMappingCategoriesResponse.CategoryInfo::getCategoryId)
            .containsExactlyInAnyOrder(12345);

        MboMappings.SearchMappingCategoriesResponse notFoundOnOtherSupplier = service.searchMappingCategoriesByShopId(
            MboMappings.SearchMappingCategoriesRequest.newBuilder()
                .setSupplierId(10002)
                .build());
        assertThat(notFoundOnOtherSupplier.getCategoriesList()).isEmpty();
    }
}
