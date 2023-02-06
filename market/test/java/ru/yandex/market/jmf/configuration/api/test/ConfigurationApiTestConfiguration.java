package ru.yandex.market.jmf.configuration.api.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ru.yandex.market.jmf.configuration.api.ConfigurationApiConfiguration;
import ru.yandex.market.jmf.configuration.api.ConfigurationService;
import ru.yandex.market.jmf.configuration.api.test.impl.InMemoryConfigurationService;
import ru.yandex.market.jmf.script.test.ScriptSupportTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        ConfigurationApiConfiguration.class,
        ScriptSupportTestConfiguration.class,
})
public class ConfigurationApiTestConfiguration extends AbstractModuleConfiguration {
    protected ConfigurationApiTestConfiguration() {
        super("configuration/api/test");
    }

    @Bean
    @Conditional(ConfigurationBeanNotExists.class)
    public ConfigurationService mockConfigurationService() {
        return new InMemoryConfigurationService();
    }

    public static class ConfigurationBeanNotExists implements ConfigurationCondition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return context.getBeanFactory().getBeanNamesForType(ConfigurationService.class).length == 0;
        }

        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }
    }
}
