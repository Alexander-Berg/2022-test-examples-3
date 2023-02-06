package ru.yandex.market.logistics.iris.configuration;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;

@Configuration
public class MboClientMockConfiguration {
    @Bean
    public MboMappingsService mboMappingsService() {
        final MboMappingsService service = Mockito.mock(MboMappingsService.class);
        MboMappings.SearchApprovedMappingsResponse responseToReturn = MboMappings.SearchApprovedMappingsResponse.newBuilder()
                .addMapping(MboMappings.ApprovedMappingInfo.newBuilder()
                        .setSupplierId(1)
                        .setShopSku("sku")
                        .setMarketSkuId(20L)
                        .addMskuVendorcode("333111")
                        .build()
                )
                .build();
        Mockito.when(service.searchApprovedMappingsByKeys(ArgumentMatchers.any())).thenReturn(responseToReturn);
        return service;
    }
}
