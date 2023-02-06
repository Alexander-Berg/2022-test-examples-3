package ru.yandex.market.pharmatestshop.monitoring;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringUnit;

@Component
public class PostgresMonitoring {
    private static final long FIXED_DELAY_MILLIS = 60 * 1000;
    private static final long INITIAL_DELAY_MILLIS = FIXED_DELAY_MILLIS;

    private final MonitoringUnit postgreSqlStatus;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgresMonitoring(ComplexMonitoring monitoring, NamedParameterJdbcTemplate jdbcTemplate) {
        this.postgreSqlStatus = monitoring.createUnit("PostgreSqlStatus");
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = FIXED_DELAY_MILLIS, initialDelay = INITIAL_DELAY_MILLIS)
    public void createPostgresMonitoring() {
        try {
            jdbcTemplate.query("SELECT 1", (rs) -> {
                if (rs.getInt(1) == 1) {
                    postgreSqlStatus.ok("OK");
                } else {
                    postgreSqlStatus.critical("Something is wrong, 'SELECT 1' failed");
                }
            });
        } catch (Exception e) {
            postgreSqlStatus.critical(e.getMessage());
        }
    }
}
