package ru.yandex.market.logistics.cs.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.dataset.datatype.DataType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.cs.config.dbunit.CsDataTypeFactory;

@Configuration
public class DatabaseConfig {

    @Bean
    public DatabaseConfigBean dbUnitDatabaseConfig() {
        DatabaseConfigBean dbConfig = new com.github.springtestdbunit.bean.DatabaseConfigBean();
        dbConfig.setDatatypeFactory(new CsDataTypeFactory());
        dbConfig.setAllowEmptyFields(true);
        return dbConfig;
    }

    @Bean
    public Clock dbUnitClock() {
        return Clock.fixed(Instant.now(), ZoneId.of("UTC"));
    }

    @Bean
    public DatabaseDataSourceConnectionFactoryBean dbUnitDatabaseConnection(
        DataSource dataSource,
        DatabaseConfigBean dbUnitDatabaseConfig
    ) {
        // Force UTC time zone for [now] expressions
        DataType.RELATIVE_DATE_TIME_PARSER.setClock(dbUnitClock());

        DatabaseDataSourceConnectionFactoryBean dbConnection = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnection.setDatabaseConfig(dbUnitDatabaseConfig);
        dbConnection.setSchema("public");
        return dbConnection;
    }
}
