package ru.yandex.market.wms.shippingsorter.configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.utils.uuid.FixedUuidGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;
import ru.yandex.market.wms.shared.libs.configproperties.dao.ConfigPropertyDao;
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao;
import ru.yandex.market.wms.shared.libs.configproperties.service.ConfigPropertiesService;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@TestConfiguration
@Profile({Profiles.TEST})
public class BaseTestConfig {

    @Bean
    @Qualifier("postgresDataSourceProperties")
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties postgresDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("postgresDataSource")
    @ConfigurationProperties("spring.datasource")
    public DataSource postgresDataSource(
            @Qualifier("postgresDataSourceProperties") DataSourceProperties postgresDataSourceProperties) {
        return postgresDataSourceProperties.initializeDataSourceBuilder().build();
    }

    public DatabaseDataSourceConnection dbUnitDatabaseConnection(String schemaName, DataSource dataSource) {
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
    Clock clock() {
        return Clock.fixed(Instant.parse("2020-04-01T12:34:56.789Z"), ZoneOffset.UTC);
    }

    @Bean
    public UuidGenerator uuidGenerator() {
        return new FixedUuidGenerator("6d809e60-d707-11ea-9550-a9553a7b0571");
    }

    @Bean
    DbConfigService dbConfigService(ConfigPropertyDao repository) {
        DbConfigService spy = Mockito.spy(new DbConfigService(repository));
        Mockito.doReturn("99").when(spy).getConfig(eq("WAREHOUSE_PREFIX"), anyString());
        return spy;
    }

    @Bean
    public ConfigPropertiesService configPropertiesService(
            @Qualifier("dbConfigRepository") GlobalConfigurationDao configPropertyPostgreSqlDao) {
        return new ConfigPropertiesService(configPropertyPostgreSqlDao);
    }
}
