package ru.yandex.market.wms.timetracker.config;

import java.util.TimeZone;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.wms.timetracker.utils.FileContentUtils;


@Configuration
public class TtsTestConfig {

    @Autowired
    TtsTestConfig(CommonAnnotationBeanPostProcessor postProcessor) {
        postProcessor.setInitAnnotationType(PseudoPostConstruct.class);
    }

    @Bean
    public DatabaseDataSourceConnection dashConnection(@Qualifier("dashDataSource") DataSource dashDataSource) {
        return dbUnitDatabaseConnection("dbo", dashDataSource, "dash-schema.sql");
    }

    @Bean
    public DatabaseDataSourceConnection postgresConnection(@Qualifier("postgresDataSource") DataSource dataSource) {
        return dbUnitDatabaseConnection("PUBLIC", dataSource, "postgresql-schema.sql");
    }

    @Bean
    public DatabaseDataSourceConnection clickhouseConnection(@Qualifier("clickHouseDataSource") DataSource dataSource) {
        return dbUnitDatabaseConnection("WMS", dataSource, "clickhouse-schema.sql");
    }

    @Bean
    public TimeZone testTimeZone() {
        var defaultTimeZone = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(defaultTimeZone);
        return defaultTimeZone;
    }

    private DatabaseDataSourceConnection dbUnitDatabaseConnection(
            String schemaName,
            DataSource dataSource,
            String initScriptName
    ) {
        var dbConfig = new DatabaseConfigBean();
        dbConfig.setDatatypeFactory(new H2DataTypeFactory());
        dbConfig.setAllowEmptyFields(true);

        var dbConnectionFactory = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnectionFactory.setSchema(schemaName);
        dbConnectionFactory.setDatabaseConfig(dbConfig);

        try {
            var connectionFactoryObject = dbConnectionFactory.getObject();
            connectionFactoryObject
                    .getConnection()
                    .prepareStatement(FileContentUtils.getFileContent(initScriptName))
                    .execute();
            return connectionFactoryObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
