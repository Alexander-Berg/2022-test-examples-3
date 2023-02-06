package ru.yandex.market.wms.servicebus.configuration;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao;
import ru.yandex.market.wms.shared.libs.configproperties.service.ConfigPropertiesService;

@Configuration
public class DbTestConfiguration {

    @Bean
    public DatabaseDataSourceConnection serviceBusConnection(@Autowired DataSource datasource) {
        return dbUnitDatabaseConnection("DBO", datasource);
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
    ConfigPropertiesService configPropertiesService(
            @Qualifier("servicebus") GlobalConfigurationDao globalConfigurationDao) {
        return new ConfigPropertiesService(globalConfigurationDao);
    }
}
