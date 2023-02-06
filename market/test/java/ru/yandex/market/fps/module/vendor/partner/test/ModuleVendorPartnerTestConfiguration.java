package ru.yandex.market.fps.module.vendor.partner.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.vendor.partner.ModuleVendorPartnerConfiguration;
import ru.yandex.market.fps.module.vendor.partner.VendorPartnerClient;
import ru.yandex.market.jmf.utils.UtilsTestConfiguration;

@Configuration
@Import({
        ModuleVendorPartnerConfiguration.class,
        UtilsTestConfiguration.class,
})
public class ModuleVendorPartnerTestConfiguration {

    @Bean
    public VendorPartnerClient mockVendorPartnerClient() {
        return Mockito.mock(VendorPartnerClient.class);
    }
}
