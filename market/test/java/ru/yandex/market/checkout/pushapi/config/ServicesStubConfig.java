package ru.yandex.market.checkout.pushapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.common.rest.TvmTicketProvider;
import ru.yandex.market.checkout.pushapi.helpers.StubbedCheckouterShopMetaDataRwService;

@Configuration
public class ServicesStubConfig {

    @Bean
    public StubbedCheckouterShopMetaDataRwService checkouterShopMetaDataGetter(
            RestTemplate checkouterSimpleRestOperations,
            @Value("${market.checkouter.client.url}") String serviceURL,
            @Value("${market.checkouter.shopMetaData.cache.expire.seconds:60}") int cacheExpireTimeSeconds,
            @Value("${market.checkouter.shopMetaData.cache.max_size:128}") int cacheMaxSize,
            TvmTicketProvider tvmTicketProvider
    ) {
        StubbedCheckouterShopMetaDataRwService service =
                new StubbedCheckouterShopMetaDataRwService(cacheExpireTimeSeconds, cacheMaxSize);
        service.setRestTemplate(checkouterSimpleRestOperations);
        service.setServiceURL(serviceURL);
        service.setTvmTicketProvider(tvmTicketProvider);
        return service;
    }
}
