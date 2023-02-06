package ru.yandex.market.mbi.partner_stat.config;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Lqb для тестов.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@Configuration
@ParametersAreNonnullByDefault
public class LiquibaseConfig {

    private final String defaultLiquibaseChangelog;
    private final DataSource dataSource;

    public LiquibaseConfig(
            final @Qualifier(EmbeddedPostgresConfig.DATA_SOURCE) DataSource dataSource,
            @Value("${liquibase.changelog}") final String defaultLiquibaseChangelog
    ) {
        this.dataSource = dataSource;
        this.defaultLiquibaseChangelog = defaultLiquibaseChangelog;
    }

    @Bean
    public SpringLiquibase liquibase() {
        final var liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(defaultLiquibaseChangelog);
        return liquibase;
    }
}
