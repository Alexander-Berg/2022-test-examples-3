package ru.yandex.market.ff.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.ff.service.SupplierMappingService;

import static org.mockito.Mockito.mock;

@Configuration
public class MockSupplierMappingServiceConfig {

    @Bean
    public SupplierMappingService supplierMappingService() {
        return mock(SupplierMappingService.class);
    }

}
