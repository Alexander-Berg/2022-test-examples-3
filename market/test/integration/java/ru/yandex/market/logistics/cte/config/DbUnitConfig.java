package ru.yandex.market.logistics.cte.config;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(LiquibaseConfig.class)
public class DbUnitConfig {
    @Bean
    public DatabaseConfigBean dbUnitDatabaseConfig() {
        DatabaseConfigBean bean = new DatabaseConfigBean();
        bean.setAllowEmptyFields(true);
        bean.setCaseSensitiveTableNames(false);
        bean.setDatatypeFactory(new PostgresqlDataTypeFactoryExt());
        return bean;
    }

    @Bean
    public DatabaseDataSourceConnection dbUnitDatabaseConnection(
            DatabaseConfigBean dbUnitDatabaseConfig,
            @Qualifier("dataSource") DataSource dataSource
    ) throws Exception {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        bean.setDatabaseConfig(dbUnitDatabaseConfig);
        bean.setSchema("public");

        return bean.getObject();
    }

    @Bean
    public DatabaseDataSourceConnection dbqueueDatabaseConnection(
            DatabaseConfigBean dbUnitDatabaseConfig,
            @Qualifier("dataSource") DataSource dataSource
    ) throws Exception {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        bean.setDatabaseConfig(dbUnitDatabaseConfig);
        bean.setSchema("dbqueue");

        return bean.getObject();
    }

}
