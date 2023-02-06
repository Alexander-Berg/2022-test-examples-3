package ru.yandex.market.jmf.security.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.entity.test.EntityApiTestConfiguration;
import ru.yandex.market.jmf.security.SecurityConfiguration;
import ru.yandex.market.jmf.security.SecurityProfileService;
import ru.yandex.market.jmf.security.impl.action.ActionBasedSecurityProfileService;
import ru.yandex.market.jmf.security.test.impl.MockAuthRunnerService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;

@Configuration
@Import({
        SecurityConfiguration.class,
        EntityApiTestConfiguration.class,
})
public class SecurityTestConfiguration {
    @Bean
    @Primary
    public MockSecurityDataService alwaysSuperUser() {
        return new MockSecurityDataService();
    }

    @Bean
    @Primary
    public MockAuthRunnerService mockAuthRunnerService() {
        return new MockAuthRunnerService();
    }

    @Bean
    @Primary
    @Conditional(HasNotPrimarySecurityProfileServiceBean.class)
    public SecurityProfileService actionBased(ActionBasedSecurityProfileService securityProfileService) {
        return securityProfileService;
    }

}
