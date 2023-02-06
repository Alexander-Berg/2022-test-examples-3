package ru.yandex.market.mbo.pgaudit.config;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mbo.pgaudit.AuditListener;
import ru.yandex.market.mbo.pgaudit.PgAuditCleanerService;
import ru.yandex.market.mbo.pgaudit.PgAuditRepository;
import ru.yandex.market.mbo.pgaudit.PgAuditService;

/**
 * @author yuramalinov
 * @created 04.08.2019
 */
@Configuration
public class AuditTestConfig {
    private final DbConfig dbConfig;

    public AuditTestConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Bean
    public PgAuditRepository pgAuditRepository() {
        return new PgAuditRepository("audit_test", dbConfig.namedParameterJdbcTemplate());
    }

    @Bean
    public PgAuditCleanerService pgAuditCleanerService() {
        return new PgAuditCleanerService("audit_test",
            dbConfig.jdbcTemplate(), dbConfig.transactionTemplate()
        );
    }

    @Bean
    public PgAuditService pgAuditService() {
        return new PgAuditService("audit_test", dbConfig.jdbcTemplate());
    }

    @PostConstruct
    public void setupListener() {
        PgAuditService auditService = pgAuditService();
        dbConfig.delegatingJdbcInterceptorSupplier().setSupplier(() -> new AuditListener(auditService, () -> "test"));
    }
}
