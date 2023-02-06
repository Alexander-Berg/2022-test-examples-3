package ru.yandex.market.jmf.attributes;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ru.yandex.market.jmf.db.test.HibernateTestConfiguration;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metainfo.MetaInfoService;
import ru.yandex.market.jmf.security.SecurityDataService;
import ru.yandex.market.jmf.security.SecurityProfileService;
import ru.yandex.market.jmf.security.impl.action.ActionBasedSecurityProfileService;

@Configuration
@Import({
        HibernateTestConfiguration.class,
        AttributesConfiguration.class
})
public class AttributesTestConfiguration {
    @Bean
    @Primary
    @Conditional(AttributesTestConfiguration.BeanExist.class)
    public SecurityProfileService actionBased(SecurityDataService securityDataService,
                                              MetadataService metadataService,
                                              MetaInfoService metaInfoService) {
        return new ActionBasedSecurityProfileService(securityDataService, metadataService, metaInfoService);
    }


    private static class BeanExist implements ConfigurationCondition {
        @Override
        public boolean matches(ConditionContext context, @Nonnull AnnotatedTypeMetadata metadata) {
            return context.getBeanFactory().getBeanNamesForType(SecurityProfileService.class).length == 0;
        }

        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }
    }
}
