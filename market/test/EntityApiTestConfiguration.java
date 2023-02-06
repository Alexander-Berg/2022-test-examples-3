package ru.yandex.market.jmf.entity.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ru.yandex.market.jmf.entity.EntityAdapterService;
import ru.yandex.market.jmf.entity.EntityApiConfiguration;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.metadata.test.MetadataTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        EntityApiConfiguration.class,
        MetadataTestConfiguration.class,
})
public class EntityApiTestConfiguration extends AbstractModuleConfiguration {
    protected EntityApiTestConfiguration() {
        super("entity/api/test");
    }

    @Bean
    @Conditional(EntityAdapterServiceBeanNotExists.class)
    public EntityAdapterService mockEntityAdapterService() {
        return Mockito.mock(EntityAdapterService.class);
    }

    @Bean
    @Conditional(EntityServiceBeanNotExists.class)
    public EntityService mockEntityService() {
        return Mockito.mock(EntityService.class);
    }

    private static class EntityAdapterServiceBeanNotExists implements ConfigurationCondition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return context.getBeanFactory().getBeanNamesForType(EntityAdapterService.class).length == 0;
        }

        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }
    }

    private static class EntityServiceBeanNotExists implements ConfigurationCondition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return context.getBeanFactory().getBeanNamesForType(EntityService.class).length == 0;
        }

        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }
    }
}
