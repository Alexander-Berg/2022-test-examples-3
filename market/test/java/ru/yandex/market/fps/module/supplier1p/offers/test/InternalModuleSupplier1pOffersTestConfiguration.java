package ru.yandex.market.fps.module.supplier1p.offers.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

import ru.yandex.market.fps.module.supplier1p.offers.XlsxByMarketTemplateWritingService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleSupplier1pOffersTestConfiguration.class)
public class InternalModuleSupplier1pOffersTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleSupplier1pOffersTestConfiguration() {
        super("module/supplier1p/offers/test");
    }

    @Primary
    @Bean
    public ResourceLoader spyResourceLoader(ResourceLoader delegate) {
        return Mockito.spy(delegate);
    }

    @Primary
    @Bean
    public XlsxByMarketTemplateWritingService spyXlsxByMarketTemplateWritingService(
            XlsxByMarketTemplateWritingService xlsxByMarketTemplateWritingService
    ) {
        return Mockito.spy(xlsxByMarketTemplateWritingService);
    }
}
