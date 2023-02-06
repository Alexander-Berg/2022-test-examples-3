package ru.yandex.market.fulfillment.stockstorage.configuration;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;

@Configuration
@Import(DbUnitTestConfiguration.class)
public class StockStorageDbUnitTestConfiguration {

    @Bean
    public DatabaseDataSourceConnection qrtzDbUnitDatabaseConnection(
            DatabaseConfigBean databaseConfigBean,
            @Qualifier("qrtzDataSource") DataSource dataSource) throws Exception {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        bean.setDatabaseConfig(databaseConfigBean);

        return bean.getObject();
    }

    @Bean
    public DatabaseDataSourceConnection replicaDbUnitDatabaseConnection(
            DatabaseConfigBean databaseConfigBean,
            @Qualifier("replicaDataSource") DataSource dataSource) throws Exception {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        bean.setDatabaseConfig(databaseConfigBean);

        return bean.getObject();
    }

    @Bean
    public DatabaseDataSourceConnection archiveDbUnitDatabaseConnection(
            DatabaseConfigBean databaseConfigBean,
            DataSource dataSource) throws Exception {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        bean.setDatabaseConfig(databaseConfigBean);
        bean.setSchema("archive");

        return bean.getObject();
    }
}
