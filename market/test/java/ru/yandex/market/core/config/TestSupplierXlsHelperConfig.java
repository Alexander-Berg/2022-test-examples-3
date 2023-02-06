package ru.yandex.market.core.config;

import javax.annotation.ParametersAreNonnullByDefault;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.feed.supplier.config.SupplierXlsHelperConfig;

@ParametersAreNonnullByDefault
@Configuration
public class TestSupplierXlsHelperConfig extends SupplierXlsHelperConfig {

    @Bean
    @Primary
    public SupplierXlsHelper supplierXlsHelper() {
        ClassPathResource classPathResource = new ClassPathResource("supplier/feed/marketplace-catalog.xlsm");
        return Mockito.spy(super.supplierXlsHelper(classPathResource));
    }

    @Bean
    @Primary
    public SupplierXlsHelper unitedSupplierXlsHelper() {
        ClassPathResource classPathResource = new ClassPathResource("supplier/feed/marketplace-catalog-united.xlsx");
        return Mockito.spy(super.unitedSupplierXlsHelper(classPathResource));
    }
}
