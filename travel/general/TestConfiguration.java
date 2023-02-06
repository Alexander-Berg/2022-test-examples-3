package ru.yandex.travel.orders.configurations;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@Profile("test")
public class TestConfiguration {
    /**
     * As we run tests in parallel on PRs, different tests may reuse the same db.
     * To separate tests from each other, we need to have a unique db name per spring container.
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        // unique db name. Setting jdbc url to "jdbc:h2:mem:" will mess with connection pool
        dataSource.setUrl("jdbc:h2:mem:" + System.currentTimeMillis() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");

        return dataSource;
    }
}
