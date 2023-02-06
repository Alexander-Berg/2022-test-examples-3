package ru.yandex.market.jmf.module.transfer.test;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.jmf.entity.test.EntityApiTestConfiguration;
import ru.yandex.market.jmf.http.test.HttpTestConfiguration;
import ru.yandex.market.jmf.module.transfer.ModuleDataTransferConfiguration;

@Configuration
@Import({
        ModuleDataTransferConfiguration.class,
        HttpTestConfiguration.class,
        EntityApiTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.module.transfer.test.impl")
@PropertySource(name = "testTransferClientProperties", value = "classpath:yc_test.properties")
public class ModuleDataTransferTestConfiguration {


    private static final String ENDPOINT_ID = "fakeEndpointId";

    @Bean
    @Primary
    public EnvironmentResolver environmentResolver() {
        return new EnvironmentResolver() {
            @Nonnull
            @Override
            public Environment get() {
                return Environment.INTEGRATION_TEST;
            }
        };
    }
}
