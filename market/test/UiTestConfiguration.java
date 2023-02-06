package ru.yandex.market.jmf.ui.test;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.jmf.module.http.metaclass.test.ModuleHttpMetaclassTestConfiguration;
import ru.yandex.market.jmf.module.relation.test.ModuleRelationTestConfiguration;
import ru.yandex.market.jmf.ui.UiConfiguration;

@Configuration
@Import({
        UiConfiguration.class,
        ModuleRelationTestConfiguration.class,
        ModuleHttpMetaclassTestConfiguration.class,
})
public class UiTestConfiguration {
    @Bean
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
