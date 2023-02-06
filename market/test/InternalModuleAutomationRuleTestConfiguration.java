package ru.yandex.market.jmf.module.automation.test;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.security.action.SecurityDomainAccessMatrixProvider;
import ru.yandex.market.jmf.security.action.SecurityDomainStructureProvider;
import ru.yandex.market.jmf.security.impl.action.ClassPathBasedSecurityDomainAccessMatrixProvider;
import ru.yandex.market.jmf.security.impl.action.ClassPathBasedSecurityDomainStructureProvider;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;
import ru.yandex.market.jmf.utils.XmlUtils;

@Configuration
@Import({
        ModuleAutomationRuleTestConfiguration.class,
})
public class InternalModuleAutomationRuleTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleAutomationRuleTestConfiguration() {
        super("test/jmf/module/automation");
    }

    @Bean
    public SecurityDomainAccessMatrixProvider automationRuleTestSecurityDomainAccessMatrixProvider(XmlUtils xmlUtils) {
        return new ClassPathBasedSecurityDomainAccessMatrixProvider("automationTest", Set.of(),
                "classpath:test/jmf/module/automation/accessMatrix/access_matrix.xml",
                xmlUtils);
    }

    @Bean
    public SecurityDomainStructureProvider automationRuleTestSecurityDomainStructureProvider(XmlUtils xmlUtils) {
        return new ClassPathBasedSecurityDomainStructureProvider(
                "classpath:test/jmf/module/automation/security/security_domain.xml",
                xmlUtils);
    }
}
