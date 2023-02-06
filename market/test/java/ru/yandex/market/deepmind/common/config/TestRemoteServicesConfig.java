package ru.yandex.market.deepmind.common.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.mbo.http.MboAuditService;
import ru.yandex.market.mboc.common.config.RemoteServicesConfig;
import ru.yandex.market.mboc.common.infrastructure.sql.AuditWriter;
import ru.yandex.market.mboc.common.infrastructure.sql.SyncAuditWriter;

@Profile("test")
@TestConfiguration
public class TestRemoteServicesConfig implements RemoteServicesConfig {
    @Override
    @Bean
    @Primary
    public MboAuditService mboAuditService() {
        return Mockito.mock(MboAuditService.class);
    }

    @Override
    @Bean
    @Primary
    public AuditWriter auditWriter() {
        return new SyncAuditWriter(mboAuditService());
    }
}
