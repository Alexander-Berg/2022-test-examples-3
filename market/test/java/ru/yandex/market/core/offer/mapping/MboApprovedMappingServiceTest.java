package ru.yandex.market.core.offer.mapping;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Vadim Lyalin
 */
class MboApprovedMappingServiceTest extends FunctionalTest {
    @Autowired
    private MboMappingsService mboMappingsService;

    private MboApprovedMappingService mboApprovedMappingService;

    @BeforeEach
    void init() {
        mboApprovedMappingService =
                new MboApprovedMappingServiceImpl(mboMappingsService, Executors.newFixedThreadPool(10), 2);
    }

    @Test
    void testGetMarketSkuMappingByMarketSku() {
        // передаем 7 sku, должно сгенерироваться 4 запроса к стабу.
        // Два возвращают map, один пустую map, один бросает исключение
        MboMappings.SearchApprovedMappingsResponse mappingResponse1 = MboMappings.SearchApprovedMappingsResponse.newBuilder().build();
        MboMappings.SearchApprovedMappingsResponse mappingResponse2 = MboMappings.SearchApprovedMappingsResponse.newBuilder()
                .addMapping(MboMappings.ApprovedMappingInfo.newBuilder()
                        .setShopSku("3")
                        .setMarketSkuId(1)
                        .setShopTitle("title1")
                        .setMarketCategoryId(1L)
                        .build()
                ).build();
        MboMappings.SearchApprovedMappingsResponse mappingResponse4 = MboMappings.SearchApprovedMappingsResponse.newBuilder()
                .addMapping(MboMappings.ApprovedMappingInfo.newBuilder()
                        .setShopSku("7")
                        .setMarketSkuId(5)
                        .build())
                .build();

        when(mboMappingsService.searchApprovedMappingsByMarketSkuId(any()))
                .thenReturn(mappingResponse1, mappingResponse2)
                .thenThrow(new RuntimeException())
                .thenReturn(mappingResponse4);

        Map<Long, List<MarketSkuMappingInfo>> mapping =
                mboApprovedMappingService.getMappingByMarketSku(
                        LongStream.range(1, 8).boxed().collect(Collectors.toList())).join();

        verify(mboMappingsService, times(3)).searchApprovedMappingsByMarketSkuId(argThat(argument -> argument.getMarketSkuIdCount() == 2));
        verify(mboMappingsService, times(1)).searchApprovedMappingsByMarketSkuId(argThat(argument -> argument.getMarketSkuIdCount() == 1));
        verifyNoMoreInteractions(mboMappingsService);

        assertThat(mapping.size(), is(2));
        MarketSkuMappingInfo mksuOneMapping = mapping.get(1L).iterator().next();
        assertThat(mksuOneMapping.shopOffer().shopSku(), is("3"));
        assertThat(mksuOneMapping.shopOffer().title(), is("title1"));
        assertThat(mksuOneMapping.marketCategoryId(), is(1L));

        MarketSkuMappingInfo mskuSecondMapping = mapping.get(5L).iterator().next();
        assertThat(mskuSecondMapping.shopOffer().shopSku(), is("7"));
        assertThat(mskuSecondMapping.marketSku(), is(5L));
    }

}
