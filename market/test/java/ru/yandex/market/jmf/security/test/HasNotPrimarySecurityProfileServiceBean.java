package ru.yandex.market.jmf.security.test;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ru.yandex.market.jmf.security.SecurityProfileService;

public class HasNotPrimarySecurityProfileServiceBean implements ConfigurationCondition {
    @Override
    public boolean matches(ConditionContext context, @Nonnull AnnotatedTypeMetadata metadata) {
        return context.getBeanFactory().getBeanNamesForType(SecurityProfileService.class).length == 2;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }
}
