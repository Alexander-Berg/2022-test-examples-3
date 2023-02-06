package ru.yandex.market.mbo.configs.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.core.audit.MboAuditServiceMock;

/**
 * @author anmalysh
 * @since 3/11/2019
 */
@Configuration
@Profile("test")
public class AuditTestConfig implements AuditConfig {

    @Bean
    public AuditService auditService() {
        MboAuditServiceMock auditServiceMock = new MboAuditServiceMock();
        return new AuditService(auditServiceMock, auditServiceMock, "test");
    }
}
