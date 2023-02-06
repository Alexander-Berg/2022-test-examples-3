package ru.yandex.market.logistics.logging.scheduled;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.logging.scheduled.dao.ScheduledTaskDao;
import ru.yandex.market.logistics.logging.scheduled.settings.ScheduledTaskLogConfig;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;

@Configuration
@EnableZonkyEmbeddedPostgres
@Import({DbUnitTestConfiguration.class})
public class SpringTestConfiguration {

    private static final String TABLE_NAME = "scheduled_tasks_log";

    @Bean
    public ScheduledTaskDao scheduledTaskDao(DataSource datasource) {
        return new ScheduledTaskDao(new ScheduledTaskLogConfig(TABLE_NAME), new JdbcTemplate(datasource));
    }
}
