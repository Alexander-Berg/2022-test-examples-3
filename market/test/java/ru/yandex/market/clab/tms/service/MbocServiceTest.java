package ru.yandex.market.clab.tms.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.clab.common.service.mapping.MbocService;
import ru.yandex.market.clab.common.service.good.GoodMapping;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 * @since 3/21/2019
 */
public class MbocServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private MboMappingsService mboMappingsService;

    @Mock
    private MboCategoryService mboCategoryService;

    private MbocService service;

    @Mock
    MbocService.MappingResponseProcessor processor;

    @Before
    public void before() {
        service = new MbocService(mboMappingsService, mboCategoryService);
    }

    @Test
    public void testMappingFound() {
        SupplierOffer.Mapping mapping = SupplierOffer.Mapping.newBuilder().build();
        SupplierOffer.Offer offer = SupplierOffer.Offer.newBuilder()
            .setSupplierId(1L)
            .setShopSkuId("sku1")
            .setApprovedMapping(mapping)
            .build();
        when(mboMappingsService.searchMappingsByKeys(any(MboMappings.SearchMappingsByKeysRequest.class)))
            .thenReturn(MboMappings.SearchMappingsResponse.newBuilder()
                .addOffers(offer)
                .build());
        GoodMapping goodMapping = new GoodMapping(2L, 1L, "sku1");
        service.requestMappings(Collections.singletonList(goodMapping), processor);

        verify(processor, times(1))
            .handleApprovedMapping(goodMapping, offer, mapping);
        verify(processor, never())
            .handleNoOfferInfo(any());
        verify(processor, never())
            .handleNoApprovedMapping(any());
    }

    @Test
    public void testMappingNotFound() {
        SupplierOffer.Offer offer = SupplierOffer.Offer.newBuilder()
            .setSupplierId(1L)
            .setShopSkuId("sku1")
            .build();
        when(mboMappingsService.searchMappingsByKeys(any(MboMappings.SearchMappingsByKeysRequest.class)))
            .thenReturn(MboMappings.SearchMappingsResponse.newBuilder()
                .addOffers(offer)
                .build());
        GoodMapping goodMapping = new GoodMapping(2L, 1L, "sku1");
        service.requestMappings(Collections.singletonList(goodMapping), processor);

        verify(processor, never())
            .handleApprovedMapping(any(), any(), any());
        verify(processor, never())
            .handleNoOfferInfo(any());
        verify(processor, times(1))
            .handleNoApprovedMapping(goodMapping);
    }

    @Test
    public void testOfferNotFound() {
        when(mboMappingsService.searchMappingsByKeys(any(MboMappings.SearchMappingsByKeysRequest.class)))
            .thenReturn(MboMappings.SearchMappingsResponse.newBuilder()
                .build());
        GoodMapping goodMapping = new GoodMapping(2L, 1L, "sku1");
        service.requestMappings(Collections.singletonList(goodMapping), processor);

        verify(processor, never())
            .handleApprovedMapping(any(), any(), any());
        verify(processor, times(1))
            .handleNoOfferInfo(goodMapping);
        verify(processor, never())
            .handleNoApprovedMapping(any());
    }
}
