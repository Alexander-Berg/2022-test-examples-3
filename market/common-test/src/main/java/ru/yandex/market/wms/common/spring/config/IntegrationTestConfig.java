package ru.yandex.market.wms.common.spring.config;

import java.sql.SQLException;
import java.time.Clock;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.spring.dao.implementation.UserDAO;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao;
import ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@Configuration
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
    @ConfigurationProperties("spring.datasource-scprdd1")
    DataSourceProperties scprdd1DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource scprdd1DataSource() throws SQLException {
        DataSource dataSource = scprdd1DataSourceProperties().initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("scprdd1-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Primary
    @Bean
    public UserDAO userDao()  throws SQLException {
        return new UserDAO(new NamedParameterJdbcTemplate(scprdd1DataSource()));
    }

    @Bean
    @Primary
    public GlobalConfigurationDao nSqlConfigDao(
            SecurityDataProvider securityDataProvider,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Clock clock
    ) {
        return new NSqlConfigDao(securityDataProvider, namedParameterJdbcTemplate, clock);
    }

    @Bean
    public DatabaseDataSourceConnection wmwhseConnection() {
        return dbUnitDatabaseConnection(DatabaseSchema.WMWHSE1.getName(), dataSource());
    }

    @Bean
    public DatabaseDataSourceConnection enterpriseConnection() {
        return dbUnitDatabaseConnection(DatabaseSchema.ENTERPRISE.getName(), dataSource());
    }

    @Bean
    public DatabaseDataSourceConnection scprdd1DboConnection() throws SQLException {
        return dbUnitDatabaseConnection("DBO", scprdd1DataSource());
    }

    public static DatabaseDataSourceConnection dbUnitDatabaseConnection(String schemaName, DataSource dataSource) {
        DatabaseConfigBean dbConfig = new DatabaseConfigBean();
        dbConfig.setDatatypeFactory(new H2DataTypeFactory());
        dbConfig.setAllowEmptyFields(true);

        DatabaseDataSourceConnectionFactoryBean dbConnectionFactory =
                new DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnectionFactory.setSchema(schemaName);
        dbConnectionFactory.setDatabaseConfig(dbConfig);

        try {
            return dbConnectionFactory.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public ServicebusClient servicebusClient() {
        return Mockito.mock(ServicebusClient.class);
    }
}
