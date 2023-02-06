package ru.yandex.market.mboc.app.proto.mappings;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.SearchOfferProcessingStatusesResponse;
import ru.yandex.market.mboc.http.SupplierOffer;

/**
 * @author danfertev
 * @since 31.07.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MboMappingsServiceImplSearchOfferProcessingStatusesByShopIdTest
    extends AbstractMboMappingsServiceImplTest {

    @Test
    public void testIncorrectSearchOfferProcessingStatusesByShopIdRequest() {
        // empty supplier_id
        SearchOfferProcessingStatusesResponse response = service.searchOfferProcessingStatusesByShopId(
            MboMappings.SearchOfferProcessingStatusesRequest.newBuilder()
                .build());
        Assertions.assertThat(response.getStatus())
            .isEqualTo(SearchOfferProcessingStatusesResponse.Status.ERROR);

        // internal error
        Mockito.when(offerStatService.countOffersByProcessingStatus(Mockito.any(OffersFilter.class)))
            .thenThrow(new RuntimeException("test message"));
        response = service.searchOfferProcessingStatusesByShopId(
            MboMappings.SearchOfferProcessingStatusesRequest.newBuilder()
                .setSupplierId(1)
                .build()
        );
        Mockito.verify(offerStatService, Mockito.times(1))
            .countOffersByProcessingStatus(Mockito.any(OffersFilter.class));
        Assertions.assertThat(response.getStatus())
            .isEqualTo(SearchOfferProcessingStatusesResponse.Status.ERROR);
        Assertions.assertThat(response.getMessage())
            .isEqualTo("RuntimeException: test message");
    }


    @Test
    public void testCorrectSearchOfferProcessingStatusesByShopIdRequest() {
        Mockito.when(offerStatService.countOffersByProcessingStatus(Mockito.any(OffersFilter.class)))
            .thenReturn(ImmutableMap.of(
                SupplierOffer.OfferProcessingStatus.IN_WORK, 2,
                SupplierOffer.OfferProcessingStatus.READY, 4
            ));

        SearchOfferProcessingStatusesResponse response = service.searchOfferProcessingStatusesByShopId(
            MboMappings.SearchOfferProcessingStatusesRequest.newBuilder()
                .setSupplierId(1)
                .addAvailability(SupplierOffer.Availability.ACTIVE)
                .setTextQueryString("test_string")
                .build());

        Mockito.verify(offerStatService, Mockito.times(1))
            .countOffersByProcessingStatus(Mockito.any(OffersFilter.class));

        Assertions.assertThat(response.getStatus())
            .isEqualTo(SearchOfferProcessingStatusesResponse.Status.OK);
        Assertions.assertThat(response.getOfferProcessingStatusesList())
            .containsExactlyInAnyOrder(
                SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                    .setOfferCount(2)
                    .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.IN_WORK)
                    .build(),
                SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                    .setOfferCount(4)
                    .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
                    .build());
    }

    @Test
    public void testSearchOfferProcessingStatusesByHasAnyMappingAndMappingFilters() {
        SearchOfferProcessingStatusesResponse response = service.searchOfferProcessingStatusesByShopId(
            MboMappings.SearchOfferProcessingStatusesRequest.newBuilder()
                .setSupplierId(82)
                .addMappingFilters(MboMappings.MappingFilter.newBuilder()
                    .setMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                    .build())
                .setHasAnyMapping(MboMappings.MappingKind.SUGGEST_MAPPING)
                .build());

        Mockito.verify(offerStatService, Mockito.never())
            .countOffersByProcessingStatus(Mockito.any(OffersFilter.class));

        Assertions.assertThat(response.getStatus())
            .isEqualTo(SearchOfferProcessingStatusesResponse.Status.ERROR);
    }

    @Test
    public void testSupplierOnBusinessId() {
        Mockito.when(offerStatService.countOffersByProcessingStatus(Mockito.any(OffersFilter.class)))
            .thenReturn(ImmutableMap.of(
                SupplierOffer.OfferProcessingStatus.IN_WORK, 2
            ));

        SearchOfferProcessingStatusesResponse response = service.searchOfferProcessingStatusesByShopId(
            MboMappings.SearchOfferProcessingStatusesRequest.newBuilder()
                .setSupplierId(10001)
                .addAvailability(SupplierOffer.Availability.ACTIVE)
                .setTextQueryString("test_string")
                .build());

        ArgumentCaptor<OffersFilter> filterCaptor = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(offerStatService, Mockito.times(1))
            .countOffersByProcessingStatus(filterCaptor.capture());

        OffersFilter filter = filterCaptor.getValue();
        Integer supplierId = filter.getBusinessIds().stream().findFirst().orElseThrow();
        // searching on biz_id
        Assert.assertEquals(10000L, (long) supplierId);
    }
}
