package ru.yandex.market.mboc.common.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.mboc.common.config.RemoteServicesConfig;
import ru.yandex.market.mboc.common.infrastructure.sql.AuditWriter;
import ru.yandex.market.mboc.common.infrastructure.sql.SyncAuditWriter;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;

/**
 * @author yuramalinov
 * @created 26.08.2019
 */

@Profile("test")
@TestConfiguration
@Primary
public class TestRemoteServicesConfig implements RemoteServicesConfig {
    @Override
    @Bean
    @Primary
    public MboAuditServiceMock mboAuditService() {
        return new MboAuditServiceMock();
    }

    @Override
    @Bean
    @Primary
    public AuditWriter auditWriter() {
        return new SyncAuditWriter(mboAuditService());
    }
}
