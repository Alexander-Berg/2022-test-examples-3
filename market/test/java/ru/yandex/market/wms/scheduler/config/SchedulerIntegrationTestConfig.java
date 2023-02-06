package ru.yandex.market.wms.scheduler.config;

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
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.common.spring.utils.uuid.FixedUuidGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;
import ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao;
import ru.yandex.market.wms.shared.libs.configproperties.service.ConfigPropertiesService;
import ru.yandex.market.wms.trace.Module;
import ru.yandex.market.wms.trace.log.RequestTraceLog;
import ru.yandex.market.wms.trace.log.RequestTraceLogBase;

@Configuration
@SpringBootApplication(scanBasePackages = {
        "ru.yandex.market.wms.scheduler"
})
public class SchedulerIntegrationTestConfig {

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
    @ConfigurationProperties("spring.datasource-scheduler")
    DataSourceProperties schedulerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("dataSource")
    @Qualifier("scheduler")
    public DataSource schedulerDataSource() throws SQLException {
        DataSource dataSource = schedulerDataSourceProperties().initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("scheduler-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Primary
    @Bean
    public JdbcTemplate jdbcTemplate(@Qualifier("scprd") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Qualifier("schedulerJdbcTemplate")
    public JdbcTemplate schedulerJdbcTemplate(@Qualifier("scheduler") DataSource schedulerDataSource) {
        return new JdbcTemplate(schedulerDataSource);
    }

    @Bean
    @ConfigurationProperties("spring.datasource-archive")
    DataSourceProperties archiveDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("archive")
    public DataSource archiveDataSource() throws SQLException {
        DataSource dataSource = archiveDataSourceProperties().initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("archive-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("spring.datasource-scprdi1")
    DataSourceProperties scprdi1DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("scprdi1")
    public DataSource scprdi1DataSource() throws SQLException {
        DataSource dataSource = scprdi1DataSourceProperties().initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("scprdi1-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("spring.datasource-scprdd1")
    DataSourceProperties scprdd1DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("scprdd1")
    public DataSource scprdd1DataSource() throws SQLException {
        DataSource dataSource = scprdd1DataSourceProperties().initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("scprdd1-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean("clickHouseDataSourceProperties")
    @ConfigurationProperties("spring.datasource-click-house")
    public DataSourceProperties clickHouseDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("clickHouseDataSource")
    @ConfigurationProperties("spring.datasource-click-house")
    public DataSource clickHouseDataSource(
            @Qualifier("clickHouseDataSourceProperties") DataSourceProperties dataSourceProperties)
            throws SQLException {

        DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("click-house-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean("clickHouseNamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate clickHouseNamedParameterJdbcTemplate(
            @Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
        return new NamedParameterJdbcTemplate(clickHouseDataSource);
    }

    @Bean("scprdi1NamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate scprdi1NamedParameterJdbcTemplate(
            @Qualifier("scprdi1") DataSource scprdi1DataSource) {
        return new NamedParameterJdbcTemplate(scprdi1DataSource);
    }

    @Bean("scprdd1NamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate scprdd1NamedParameterJdbcTemplate(
            @Qualifier("scprdd1") DataSource scprdd1DataSource) {
        return new NamedParameterJdbcTemplate(scprdd1DataSource);
    }

    @Bean
    public DatabaseDataSourceConnection wmwhseConnection(@Qualifier("scprd") DataSource scprdDataSource) {
        return dbUnitDatabaseConnection(DatabaseSchema.WMWHSE1.getName(), scprdDataSource);
    }

    @Bean
    public DatabaseDataSourceConnection schedulerConnection(@Qualifier("scheduler") DataSource schedulerDataSource) {
        return dbUnitDatabaseConnection("DBO", schedulerDataSource);
    }

    @Bean
    public DatabaseDataSourceConnection archiveConnection(@Qualifier("archive") DataSource archiveDataSource) {
        return dbUnitDatabaseConnection(DatabaseSchema.WMWHSE1.getName(), archiveDataSource);
    }

    @Bean
    public DatabaseDataSourceConnection scprdi1Connection(@Qualifier("scprdi1") DataSource scprdi1DataSource) {
        return dbUnitDatabaseConnection("DBO", scprdi1DataSource);
    }

    @Bean
    public DatabaseDataSourceConnection scprdd1Connection(@Qualifier("scprdd1") DataSource scprdd1DataSource) {
        return dbUnitDatabaseConnection("DBO", scprdd1DataSource);
    }

    @Bean
    public DatabaseDataSourceConnection clickHouseConnection(@Qualifier("clickHouseDataSource") DataSource dataSource) {
        return dbUnitDatabaseConnection("WMS", dataSource);
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
    public UuidGenerator uuidGenerator() {
        return new FixedUuidGenerator("6d809e60-d707-11ea-9550-a9553a7b0571");
    }

    @Bean
    public RequestTraceLog requestTraceLog(Module applicationModuleName) {
        return new RequestTraceLogBase(applicationModuleName.toString() + "-trace");
    }

    @Bean
    ConfigPropertiesService configPropertiesService(@Qualifier("scprd") NSqlConfigDao nSqlConfigDao) {
        return new ConfigPropertiesService(nSqlConfigDao);
    }
}
