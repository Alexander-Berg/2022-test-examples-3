package ru.yandex.market.jmf.metadata.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ru.yandex.market.jmf.metadata.MetadataConfiguration;
import ru.yandex.market.jmf.metadata.MetadataServiceConfiguration;
import ru.yandex.market.jmf.script.storage.ScriptStorageTestConfiguration;

@Configuration
@Import({
        MetadataConfiguration.class,
        ScriptStorageTestConfiguration.class,
})
public class MetadataTestConfiguration {
    private static class MetadataServiceConfigurationBeanNotExistsCondition implements ConfigurationCondition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return context.getBeanFactory().getBeanNamesForType(MetadataServiceConfiguration.class).length == 0;
        }

        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }
    }

    @Bean
    @Conditional(MetadataServiceConfigurationBeanNotExistsCondition.class)
    public MetadataServiceConfiguration testMetadataServiceConfiguration() {
        return () -> 60;
    }
}
