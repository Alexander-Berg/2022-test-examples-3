package ru.yandex.market.wms.shippingsorter.configuration;

import java.time.Clock;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;
import ru.yandex.market.wms.shared.libs.configproperties.dao.DbConfigRepository;
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@TestConfiguration
@Profile({Profiles.TEST})
@SpringBootApplication(scanBasePackages = "ru.yandex.market.wms")
public class IntegrationTestConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        return dataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(dataSource());
    }

    @Bean
    public GlobalConfigurationDao configPropertyPostgreSqlDao(
            SecurityDataProvider securityDataProvider,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Clock clock
    ) {
        return new DbConfigRepository(namedParameterJdbcTemplate, securityDataProvider, clock);
    }

    @Bean
    public DbConfigService dbConfigService(GlobalConfigurationDao configPropertyPostgreSqlDao) {
        return new DbConfigService(configPropertyPostgreSqlDao);
    }

    @Bean
    @Primary
    public DatabaseConfigBean dbUnitDatabaseConfig() {
        final DatabaseConfigBean dbConfig = new DatabaseConfigBean();
        dbConfig.setDatatypeFactory(new PostgresqlDataTypeFactory());
        dbConfig.setCaseSensitiveTableNames(true);
        return dbConfig;
    }

    @Bean
    @Primary
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
