package ru.yandex.market.jmf.module.ou.security;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.security.action.SecurityDomainAccessMatrixProvider;
import ru.yandex.market.jmf.security.action.SecurityDomainStructureProvider;
import ru.yandex.market.jmf.security.impl.action.ClassPathBasedSecurityDomainAccessMatrixProvider;
import ru.yandex.market.jmf.security.impl.action.ClassPathBasedSecurityDomainStructureProvider;
import ru.yandex.market.jmf.utils.XmlUtils;

// Конфигурация, которая используется только в этом модуле
@Configuration
@Import(ModuleOuSecurityTestConfiguration.class)
public class InternalModuleOuSecurityTestConfiguration {

    @Bean
    public SecurityDomainAccessMatrixProvider ouSecurityTestSecurityDomainAccessMatrixProvider(XmlUtils xmlUtils) {
        return new ClassPathBasedSecurityDomainAccessMatrixProvider("ocrm", Set.of(),
                "classpath:metadata/access_matrix.xml",
                xmlUtils);
    }

    @Bean
    public SecurityDomainStructureProvider ouSecurityTestSecurityDomainStructureProvider(XmlUtils xmlUtils) {
        return new ClassPathBasedSecurityDomainStructureProvider(
                "classpath:metadata/security_domain.xml",
                xmlUtils);
    }
}
