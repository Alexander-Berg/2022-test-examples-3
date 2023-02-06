package ru.yandex.market.wrap.infor.configuration;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static ru.yandex.market.wrap.infor.configuration.DataSourcesConfiguration.WRAP_DATASOURCE;
import static ru.yandex.market.wrap.infor.configuration.IntegrationTestDataSourcesConfiguration.TEST_WMS_FIRST_DATASOURCE;
import static ru.yandex.market.wrap.infor.configuration.IntegrationTestDataSourcesConfiguration.TEST_WMS_SECOND_DATASOURCE;
import static ru.yandex.market.wrap.infor.configuration.IntegrationTestDataSourcesConfiguration.WMS_DATASOURCE_SCHEMA;
import static ru.yandex.market.wrap.infor.configuration.IntegrationTestDataSourcesConfiguration.WRAP_DATASOURCE_SCHEMA;

@Configuration
public class IntegrationTestDbUnitConfiguration {

    private static final String[] TABLE_TYPE = {"TABLE", "VIEW"};

    @Bean
    public DatabaseDataSourceConnection wrapConnection(@Qualifier(WRAP_DATASOURCE) DataSource dataSource) {
        return createDbUnitConnection(WRAP_DATASOURCE_SCHEMA, dataSource, dbUnitPostgresConfig());
    }

    @Bean
    public DatabaseDataSourceConnection wmsConnection(@Qualifier(TEST_WMS_FIRST_DATASOURCE) DataSource dataSource) {
        return createDbUnitConnection(WMS_DATASOURCE_SCHEMA, dataSource, dbUnitDatabaseConfig());
    }

    @Bean
    public DatabaseDataSourceConnection secondWmsConnection(@Qualifier(TEST_WMS_SECOND_DATASOURCE) DataSource dataSource) {
        return createDbUnitConnection(WMS_DATASOURCE_SCHEMA, dataSource, dbUnitDatabaseConfig());
    }

    @Bean
    protected DatabaseConfigBean dbUnitPostgresConfig() {
        DatabaseConfigBean config = new DatabaseConfigBean();
        config.setTableType(TABLE_TYPE);
        config.setAllowEmptyFields(true);
        return config;
    }

    @Bean
    protected DatabaseConfigBean dbUnitDatabaseConfig() {
        DatabaseConfigBean config = new DatabaseConfigBean();
        config.setDatatypeFactory(new CustomH2DataTypeFactory());
        config.setTableType(TABLE_TYPE);
        config.setAllowEmptyFields(true);

        return config;
    }

    private DatabaseDataSourceConnection createDbUnitConnection(String defaultSchema,
                                                                DataSource dataSource,
                                                                DatabaseConfigBean configBean) {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        bean.setSchema(defaultSchema);
        bean.setDatabaseConfig(configBean);

        try {
            return bean.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CustomH2DataTypeFactory extends H2DataTypeFactory {

        @Override
        public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
            if (sqlTypeName.equalsIgnoreCase("TIMESTAMP WITH TIME ZONE")) {
                return DataType.TIMESTAMP;
            }

            return super.createDataType(sqlType, sqlTypeName);
        }
    }
}
