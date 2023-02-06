package ru.yandex.market.mbo.integration.test.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.mbo.core.conf.databases.DBConfigUtils;

import javax.sql.DataSource;

/**
 * @author s-ermakov
 */
@Configuration
public class MasterDatabaseConfig {

    @Bean(name = "masterDataSource")
    public HikariDataSource masterDataSource(
        @Value("${master.scat.jdbc.driverClassName}") String driverClassName,
        @Value("${master.scat.jdbc.url}") String url,
        @Value("${master.scat.username}") String username,
        @Value("${master.scat.password}") String password
    ) {
        return DBConfigUtils.createHikariDataSource(
            driverClassName,
            url,
            username,
            password,
            "mbo-functional-tests-oaas",
            "MasterDB"
        );
    }

    @Bean(name = "masterJdbTemplate")
    public JdbcTemplate masterJdbTemplate(DataSource masterDataSource) {
        return new JdbcTemplate(masterDataSource);
    }
}
