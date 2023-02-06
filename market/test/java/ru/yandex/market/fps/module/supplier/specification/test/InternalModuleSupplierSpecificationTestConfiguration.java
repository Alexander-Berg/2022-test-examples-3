package ru.yandex.market.fps.module.supplier.specification.test;


import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.fps.module.supplier.specification.SpecificationService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        ModuleSupplierSpecificationTestConfiguration.class,
})
public class InternalModuleSupplierSpecificationTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleSupplierSpecificationTestConfiguration() {
        super("test/fps/module/supplier/specification");
    }

    @Bean
    @Primary
    public SpecificationService mockAxaptaSpecificationService(SpecificationService specificationService) {
        return Mockito.spy(specificationService);
    }
}
