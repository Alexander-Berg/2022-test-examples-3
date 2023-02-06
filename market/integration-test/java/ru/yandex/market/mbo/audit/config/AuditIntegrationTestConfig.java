package ru.yandex.market.mbo.audit.config;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.mbo.audit.conf.AuditProperties;
import ru.yandex.market.mbo.audit.conf.LogbrokerConfig;
import ru.yandex.market.mbo.audit.conf.MboAuditServiceConfig;
import ru.yandex.market.mbo.audit.conf.MboAuditTableConfig;
import ru.yandex.market.mbo.audit.conf.MboAuditYtIndexConfig;
import ru.yandex.market.mbo.audit.yt.YtActionLogRepository;

/**
 * @author s-ermakov
 */
@Configuration
@Import({
    MboAuditServiceConfig.class,
    IntegrationTestPropertyConfiguration.class,
    AuditProperties.class,
    TestAuditTableConfig.class
})
public class AuditIntegrationTestConfig {

    private MboAuditTableConfig mboAuditTableConfig;
    private MboAuditYtIndexConfig mboAuditYtIndexConfig;
    private AuditProperties auditProperties;
    private LogbrokerConfig logbrokerConfig;
    private TestAuditTableConfig testAuditTableConfig;

    public AuditIntegrationTestConfig(MboAuditTableConfig mboAuditTableConfig,
                                      MboAuditYtIndexConfig mboAuditYtIndexConfig, AuditProperties auditProperties,
                                      LogbrokerConfig logbrokerConfig, TestAuditTableConfig testAuditTableConfig) {
        this.mboAuditTableConfig = mboAuditTableConfig;
        this.mboAuditYtIndexConfig = mboAuditYtIndexConfig;
        this.auditProperties = auditProperties;
        this.logbrokerConfig = logbrokerConfig;
        this.testAuditTableConfig = testAuditTableConfig;
    }

    @Bean
    @Primary
    public YtActionLogRepository ytActionLogRepository() {
        return new YtActionLogRepository(
            testAuditTableConfig.ytClientWrapperHahn(),
            mboAuditTableConfig.ytTableService(),
            mboAuditTableConfig.auditEventLogTable(),
            mboAuditYtIndexConfig.compositeIndexDecider(),
            new CopyOnWriteArrayList<>(
                Arrays.asList(
                    mboAuditYtIndexConfig.timestampIndexWriter(),
                    mboAuditYtIndexConfig.eventIdIndexWriter()
                )
            ),
            auditProperties,
            logbrokerConfig.auditEventsPublisher());
    }


}
