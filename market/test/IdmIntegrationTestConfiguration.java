package ru.yandex.market.jmf.idm.integration.test;

import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ru.yandex.market.crm.dao.RolesDao;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.jmf.idm.integration.IdmIntegrationConfiguration;
import ru.yandex.market.jmf.security.test.SecurityTestConfiguration;

@Configuration
@Import({
        IdmIntegrationConfiguration.class,
        SecurityTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.idm.integration.test.impl")
@PropertySource(
        name = "testIdmIntegrationProperties",
        value = "classpath:idm/integration/test/idm-integration-test.properties"
)
public class IdmIntegrationTestConfiguration {
    @Bean
    @Conditional(RolesDaoBeanNotExistsCondition.class)
    public RolesDao mockRolesDao() {
        return Mockito.mock(RolesDao.class);
    }

    @Bean
    @Conditional(UsersRolesDaoBeanNotExistsCondition.class)
    public UsersRolesDao mockUsersRolesDao() {
        return Mockito.mock(UsersRolesDao.class);
    }

    private static class RolesDaoBeanNotExistsCondition implements ConfigurationCondition {
        @NotNull
        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }

        @Override
        public boolean matches(ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
            return context.getBeanFactory().getBeanNamesForType(RolesDao.class).length == 0;
        }
    }

    private static class UsersRolesDaoBeanNotExistsCondition implements ConfigurationCondition {
        @NotNull
        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }

        @Override
        public boolean matches(ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
            return context.getBeanFactory().getBeanNamesForType(UsersRolesDao.class).length == 0;
        }
    }
}
