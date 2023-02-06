package ru.yandex.market.wms.radiator.test;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import ru.yandex.market.wms.radiator.core.config.properties.DataSourceType;

@Configuration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
})
public class IntegrationTestDbConfiguration {

    private static final String[] TABLE_TYPE = {"TABLE", "VIEW"};
    public static final String SCHEMA = "WMWHSE1";

    @Value("classpath:wms-schema.sql")
    private Resource wmsSchemaSql;


    // populator
    // ---------

    @Bean
    protected DatabasePopulator wmsDatabasePopulator(
            @Qualifier("wh1DataSource") DataSource wh1DataSource,
            @Qualifier("wh2DataSource") DataSource wh2DataSource)
    {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(wmsSchemaSql);
        DatabasePopulatorUtils.execute(populator, wh1DataSource);
        DatabasePopulatorUtils.execute(populator, wh2DataSource);
        return populator;
    }

    // connection + ds
    // ---------------

    @Bean("wh1Connection")
    public DatabaseDataSourceConnection wh1Connection(@Qualifier("wh1DataSource") DataSource dataSource) {
        return createDbUnitConnection(dataSource, dbUnitDatabaseConfig());
    }

    @Bean("wh1DataSource")
    public DataSource wh1DataSource(Map<String, Map<DataSourceType, DataSource>> dataSourceMap) {
        return ds(dataSourceMap, IntegrationTestConstants.WH_1_TOKEN);
    }

    @Bean("wh2Connection")
    public DatabaseDataSourceConnection wh2Connection(@Qualifier("wh2DataSource") DataSource dataSource) {
        return createDbUnitConnection(dataSource, dbUnitDatabaseConfig());
    }

    @Bean("wh2DataSource")
    public DataSource wh2DataSource(Map<String, Map<DataSourceType, DataSource>> dataSourceMap) {
        return ds(dataSourceMap, IntegrationTestConstants.WH_2_TOKEN);
    }


    private static DataSource ds(Map<String, Map<DataSourceType, DataSource>> dataSourceMap, String token) {
        return dataSourceMap.get(token).get(DataSourceType.READ_WRITE);
    }

    private DatabaseDataSourceConnection createDbUnitConnection(DataSource dataSource, DatabaseConfigBean configBean) {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        bean.setSchema(SCHEMA);
        bean.setDatabaseConfig(configBean);

        try {
            return bean.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    protected DatabaseConfigBean dbUnitDatabaseConfig() {
        DatabaseConfigBean config = new DatabaseConfigBean();
        config.setDatatypeFactory(new CustomH2DataTypeFactory());
        config.setTableType(TABLE_TYPE);
        config.setAllowEmptyFields(true);
        return config;
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


    // data source map
    // ---------------

    @Primary
    @Bean
    public Map<String, Map<DataSourceType, DataSource>> testDataSourceMap() {
        return new HashMap<>() {{
            put(IntegrationTestConstants.WH_1_TOKEN, dataSources(IntegrationTestConstants.WH_1_KEY));
            put(IntegrationTestConstants.WH_2_TOKEN, dataSources(IntegrationTestConstants.WH_2_KEY));
        }};
    }

    private Map<DataSourceType, DataSource> dataSources(String name) {
        return new HashMap<>() {{
            org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource(poolProperties(name));
            put(DataSourceType.READ_ONLY, ds);
            put(DataSourceType.READ_WRITE, ds);
        }};
    }

    static PoolProperties poolProperties(String name) {
        PoolProperties poolProperties = new PoolProperties();
        poolProperties.setDriverClassName("org.h2.Driver");
        poolProperties.setUrl("jdbc:h2:mem:" + name + ";MODE=MSSQLServer"); // ;SCHEMA=" + SCHEMA (schema not created yet?)
        poolProperties.setUsername("sa");
        poolProperties.setPassword("");
        return poolProperties;
    }
}
