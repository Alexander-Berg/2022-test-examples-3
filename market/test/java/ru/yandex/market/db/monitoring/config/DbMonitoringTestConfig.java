package ru.yandex.market.db.monitoring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.db.monitoring.DbMonitoringRepository;

@Configuration
@Import({
    DbMonitoringConfig.class
})
public class DbMonitoringTestConfig {

    private final DbMonitoringConfig dbMonitoringConfig;
    @Value("${db_monitoring.tables.schema}")
    private String taskqueueTablesSchema;

    public DbMonitoringTestConfig(DbMonitoringConfig dbMonitoringConfig) {
        this.dbMonitoringConfig = dbMonitoringConfig;
    }

    @Bean
    public DbMonitoringRepository taskQueueRepository() {
        return new DbMonitoringRepository(dbMonitoringConfig.namedParameterJdbcTemplate(), taskqueueTablesSchema);
    }
}
