package ru.yandex.market.wms.auth.config;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.TestSecurityDataProvider;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao;
import ru.yandex.market.wms.trace.Module;
import ru.yandex.market.wms.trace.log.RequestTraceLog;
import ru.yandex.market.wms.trace.log.RequestTraceLogBase;

@Configuration
@SpringBootApplication(scanBasePackages = {
        "ru.yandex.market.wms.auth",
        "ru.yandex.market.wms.common.spring.service.authentication",
        "ru.yandex.market.wms.common.spring.config.security"
})
@Import({TestSecurityDataProvider.class})
public class AuthIntegrationTestConfig {

    @Bean
    @Qualifier("scprd")
    @Primary
    @ConfigurationProperties("spring.datasource-scprd")
    public DataSourceProperties scprdDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("scprd")
    @Primary
    public DataSource scprdDataSource(
            @Qualifier("scprd") DataSourceProperties scprdDataSourceProperties
    ) throws SQLException {
        DataSource dataSource = scprdDataSourceProperties.initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("scprd-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    @Qualifier("scprdd1")
    @ConfigurationProperties("spring.datasource-scprdd1")
    public DataSourceProperties scprdd1DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("scprdd1")
    public DataSource scprdd1DataSource(
            @Qualifier("scprdd1") DataSourceProperties scprdd1DataSourceProperties
    ) throws SQLException {
        DataSource dataSource = scprdd1DataSourceProperties.initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("scprdd1-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    @Qualifier("auth")
    @ConfigurationProperties("spring.datasource-auth")
    DataSourceProperties authDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("auth")
    public DataSource authDataSource(
            @Qualifier("auth") DataSourceProperties authDataSourceProperties
    ) throws SQLException {
        DataSource dataSource = authDataSourceProperties.initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("auth-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    public DatabaseDataSourceConnection wmwhseConnection(@Qualifier("scprd") DataSource scprdDataSource) {
        return dbUnitDatabaseConnection(DatabaseSchema.WMWHSE1.getName(), scprdDataSource);
    }

    @Bean
    public DatabaseDataSourceConnection enterpriseConnection(@Qualifier("scprd") DataSource scprdDataSource) {
        return dbUnitDatabaseConnection(DatabaseSchema.ENTERPRISE.getName(), scprdDataSource);
    }

    @Bean
    public DatabaseDataSourceConnection scprdd1Connection(@Qualifier("scprdd1") DataSource scprdd1DataSource) {
        return dbUnitDatabaseConnection("DBO", scprdd1DataSource);
    }

    @Bean
    public DatabaseDataSourceConnection authConnection(@Qualifier("auth") DataSource authDataSource) {
        return dbUnitDatabaseConnection("AUTH", authDataSource);
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
    @Primary
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean
    DbConfigService dbConfigService(NSqlConfigDao nSqlConfigDao) {
        return new DbConfigService(nSqlConfigDao);
    }

    @Bean
    public RequestTraceLog requestTraceLog(Module applicationModuleName) {
        return new RequestTraceLogBase(applicationModuleName.toString() + "-trace");
    }
}
