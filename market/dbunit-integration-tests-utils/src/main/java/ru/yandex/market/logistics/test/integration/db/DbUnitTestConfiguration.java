package ru.yandex.market.logistics.test.integration.db;

import java.util.List;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.logistics.test.integration.db.cleaner.DataSourceUtils;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DefaultSchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

@Configuration
@Import(LiquibaseTestConfiguration.class)
public class DbUnitTestConfiguration {

    private static final String[] TABLE_TYPE = {"TABLE", "VIEW", "PARTITIONED TABLE"};

    @Bean
    @Primary
    public DatabaseDataSourceConnection dbUnitDatabaseConnection(DataSource dataSource) throws Exception {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        bean.setDatabaseConfig(dbUnitDatabaseConfig());

        return bean.getObject();
    }

    @Bean
    protected SchemaCleanerConfigProvider defaultSchemaCleanerProvider(DataSource dataSource) {
        return new DefaultSchemaCleanerConfigProvider(DataSourceUtils.getDefaultSchemaFromConnection(dataSource));
    }

    @Bean
    @Primary
    public DatabaseCleanerConfig cleanerConfig(@Autowired List<SchemaCleanerConfigProvider> configList) {
        return new CompoundDatabaseCleanerConfig(configList);
    }


    @Bean
    public DatabaseConfigBean dbUnitDatabaseConfig() {
        DatabaseConfigBean config = new DatabaseConfigBean();
        config.setDatatypeFactory(new PostgresqlDataTypeFactoryExt());
        config.setTableType(TABLE_TYPE);
        return config;
    }
}
