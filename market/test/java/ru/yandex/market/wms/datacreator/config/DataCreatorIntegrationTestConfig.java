package ru.yandex.market.wms.datacreator.config;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.common.spring.utils.uuid.FixedUuidGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;
import ru.yandex.market.wms.datacreator.dao.UserDao;

@TestConfiguration
@SpringBootApplication(scanBasePackages = {
        "ru.yandex.market.wms.datacreator"
})
public class DataCreatorIntegrationTestConfig {

    @Bean
    @Qualifier("scprd-datacreator")
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties scprdDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("scprd-datacreator")
    public DataSource scprdDataSource(
            @Qualifier("scprd-datacreator") DataSourceProperties scprdDataSourceProperties
    ) throws SQLException {
        DataSource dataSource = scprdDataSourceProperties.initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("scprd-schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    public DatabaseDataSourceConnection wmwhse1Connection(@Qualifier("scprd-datacreator") DataSource scprdDataSource) {
        return dbUnitDatabaseConnection(DatabaseSchema.WMWHSE1.getName(), scprdDataSource);
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
    public UserDao userDao(NamedParameterJdbcTemplate jdbcTemplate) {
        return new UserDao(jdbcTemplate);
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
    @Primary
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(@Qualifier("scprd-datacreator")
                                                                         DataSource scprdDataSource) {
        return new NamedParameterJdbcTemplate(scprdDataSource);
    }
}
