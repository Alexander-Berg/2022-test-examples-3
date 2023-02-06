package ru.yandex.market.jmf.logic.def.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.security.action.SecurityDomainStructureProvider;
import ru.yandex.market.jmf.security.impl.action.ClassPathBasedSecurityDomainStructureProvider;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;
import ru.yandex.market.jmf.utils.XmlUtils;

@Configuration
@Import(LogicDefaultTestConfiguration.class)
@PropertySource({"classpath:/do_not_require_getters_for_all_attributes.properties"})
public class InternalLogicDefaultTestConfiguration extends AbstractModuleConfiguration {
    protected InternalLogicDefaultTestConfiguration() {
        super("logic/default/test");
    }

    @Bean
    public SecurityDomainStructureProvider defaultLogicTestOldSecurityDomainStructureProvider(XmlUtils xmlUtils) {
        return new ClassPathBasedSecurityDomainStructureProvider(
                "classpath:logic/default/test/security/securityProfileTest.domain.old.xml",
                xmlUtils
        );
    }
}
