package ru.yandex.market.mstat.planner.config;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mstat.planner.client.StaffClient;
import ru.yandex.market.mstat.planner.service.AuthInfoService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Configuration
public class TestConfig {

    @Value("${STAFFAPI_TOKEN}")
    private String staffApiToken;

    @Bean
    public StaffClient staffClient() {
        return new StaffClient(staffApiToken);
    }

    @Bean
    public StaffConfig staffConfig() {
        return new StaffConfig();
    }

    @Bean
    public AuthInfoService authInfoService() {
        AuthInfoService authInfoService = mock(AuthInfoService.class);
        when(authInfoService.getLogin()).thenReturn(AuthInfoService.TEST_LOGIN);
        return authInfoService;
    }

    @Bean
    public NamedParameterJdbcTemplate postgresJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource,
                                     @Value("${postgres.embedded.liquibase.changelog:}") String changeLog) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:" + changeLog);
        liquibase.setDataSource(dataSource);
        return liquibase;
    }

}
