package ru.yandex.market.logistics.dbqueue.config;


import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.IDatabaseConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataBaseConnectionConfig {

    @Bean
    @Qualifier("dbqueueDatabaseConnection")
    public IDatabaseConnection dbqueueDatabaseConnection(DataSource dataSource, DatabaseConfigBean config)
            throws Exception {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        bean.setDatabaseConfig(config);
        bean.setSchema("dbqueue");
        return bean.getObject();
    }


}
