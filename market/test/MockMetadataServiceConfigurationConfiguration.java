package ru.yandex.market.jmf.metadata.test;

import javax.annotation.Nonnull;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ru.yandex.market.jmf.metadata.MetadataServiceConfiguration;

@Configuration
public class MockMetadataServiceConfigurationConfiguration {
    @Bean
    @Conditional(BeanExist.class)
    public MetadataServiceConfiguration mockMetadataServiceConfiguration() {
        MetadataServiceConfiguration mock = Mockito.mock(MetadataServiceConfiguration.class);
        Mockito.when(mock.getMetadataUpdateTimeoutMinutes())
                .thenReturn(60L);
        return mock;
    }

    private static class BeanExist implements Condition {

        @Override
        public boolean matches(ConditionContext context, @Nonnull AnnotatedTypeMetadata metadata) {
            return !context.getRegistry().containsBeanDefinition("metadataServiceConfigurationImpl");
        }
    }
}
