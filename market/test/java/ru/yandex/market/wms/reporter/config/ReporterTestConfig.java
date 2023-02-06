package ru.yandex.market.wms.reporter.config;

import java.sql.SQLException;
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
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.common.spring.utils.uuid.FixedUuidGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;
import ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao;
import ru.yandex.market.wms.shared.libs.configproperties.service.ConfigPropertiesService;
import ru.yandex.market.wms.trace.Module;
import ru.yandex.market.wms.trace.log.RequestTraceLog;
import ru.yandex.market.wms.trace.log.RequestTraceLogBase;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@Configuration
@SpringBootApplication(scanBasePackages = {
        "ru.yandex.market.wms.reporter",
        "ru.yandex.market.wms.shared.libs.label.printer"
})
public class ReporterTestConfig {
    @Bean
    Clock clock() {
        return Clock.fixed(Instant.parse("2020-04-01T12:34:56.789Z"), ZoneOffset.UTC);
    }

    @Bean
    @Qualifier("reporter")
    @ConfigurationProperties("spring.datasource-reporter")
    DataSourceProperties reporterDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("reporter")
    public DataSource reporterDataSource(
            @Qualifier("reporter") DataSourceProperties reporterDataSourceProperties
    ) throws SQLException {
        DataSource dataSource = reporterDataSourceProperties.initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("reporter-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    @Qualifier("scprd")
    @ConfigurationProperties("spring.datasource-scprd")
    DataSourceProperties scprdDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("scprd")
    public DataSource scprdDataSource(
            @Qualifier("scprd") DataSourceProperties reporterDataSourceProperties
    ) throws SQLException {
        DataSource dataSource = reporterDataSourceProperties.initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    @Qualifier("yql")
    public DataSource yqlDataSource() {
        return Mockito.mock(DataSource.class);
    }

    @Bean
    public DatabaseDataSourceConnection scprdConnection(
            @Qualifier("scprd") DataSource scprdDataSource
    ) throws SQLException {
        return dbUnitDatabaseConnection(DatabaseSchema.WMWHSE1.getName(), scprdDataSource);
    }

    @Bean
    public DatabaseDataSourceConnection reporterConnection(
            @Qualifier("reporter") DataSource reporterDataSource
    ) throws SQLException {
        return dbUnitDatabaseConnection("REPORTER", reporterDataSource);
    }

    @Bean
    public RequestTraceLog requestTraceLog(Module applicationModuleName) {
        return new RequestTraceLogBase(applicationModuleName.toString() + "-test");
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
    public UuidGenerator uuidGenerator() {
        return new FixedUuidGenerator("6d809e60-d707-11ea-9550-a9553a7b0571");
    }

    @Bean
    DbConfigService dbConfigService(@Qualifier("scprd") NSqlConfigDao nSqlConfigDao) {
        DbConfigService spy = Mockito.spy(new DbConfigService(nSqlConfigDao));
        Mockito.doReturn("99").when(spy).getConfig(eq("WAREHOUSE_PREFIX"), anyString());
        return spy;
    }

    @Bean
    ConfigPropertiesService configPropertiesService(@Qualifier("scprd") NSqlConfigDao nSqlConfigDao) {
        return new ConfigPropertiesService(nSqlConfigDao);
    }
}
