package ru.yandex.market.logistics.calendaring.config

import com.github.springtestdbunit.bean.DatabaseConfigBean
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean
import org.dbunit.database.IDatabaseConnection
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

class DataBaseConnectionConfig {

    @Bean
    @Qualifier("dbqueueDatabaseConnection")
    fun dbqueueDatabaseConnection(dataSource: DataSource?, config: DatabaseConfigBean?): IDatabaseConnection {
        val bean = DatabaseDataSourceConnectionFactoryBean(dataSource)
        bean.setDatabaseConfig(config)
        bean.setSchema("dbqueue")
        return bean.getObject() as IDatabaseConnection
    }
}
