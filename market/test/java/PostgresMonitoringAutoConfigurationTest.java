package ru.yandex.market.starter.postgres;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.starter.postgres.config.PostgresMonitoringAutoConfiguration;
import ru.yandex.market.starter.postgres.monitoring.PostgresMonitoring;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgresMonitoringAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MonitoringTestConfiguration.class)
        .withConfiguration(AutoConfigurations.of(PostgresMonitoringAutoConfiguration.class));

    @Test
    public void monitoringTest() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(PostgresMonitoring.class));

        contextRunner.withPropertyValues("mj.postgres.monitoring.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(PostgresMonitoring.class));

        contextRunner.withPropertyValues("mj.postgres.monitoring.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(PostgresMonitoring.class));
    }

    static class MonitoringTestConfiguration {

        @Bean
        public ComplexMonitoring complexMonitoring() {
            return Mockito.mock(ComplexMonitoring.class);
        }

        @Bean
        public JdbcTemplate jdbcTemplate() {
            return Mockito.mock(JdbcTemplate.class);
        }
    }
}
