package ru.yandex.market.olap2.config;

import java.sql.Types;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import lombok.AllArgsConstructor;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.dataset.datatype.AbstractDataType;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Конфигурация эмбеддед постгри, на которой будет работать метабаза Коко.
 * {@link TestPostgresConfiguration} - это другое.
 */
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class EmbeddedPostgresConfiguration {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws Exception {
        return EmbeddedPostgres.start();
    }

    @Primary
    @Bean(name = {"dataSource", "metadataSource"})
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }

    @Bean
    public DatabaseConfigBean configBean() {
        final DatabaseConfigBean configBean = new DatabaseConfigBean();
        configBean.setDatatypeFactory(new JsonbDataFactory());
        return configBean;
    }

    @Bean
    public IDatabaseTester databaseTester(DataSource dataSource) {
        return new DataSourceDatabaseTester(dataSource);
    }

    @Bean(name = "dbUnitDatabaseConnection")
    public DatabaseDataSourceConnectionFactoryBean databaseDataSourceConnectionFactoryBean(
            DatabaseConfigBean configBean, DataSource dataSource)
    {
        DatabaseDataSourceConnectionFactoryBean factoryBean = new DatabaseDataSourceConnectionFactoryBean();
        factoryBean.setDatabaseConfig(configBean);
        factoryBean.setDataSource(dataSource);
        return factoryBean;
    }


    @AllArgsConstructor
    public class JsonbDataFactory extends DefaultDataTypeFactory {

        @Override
        public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
            if (sqlTypeName.equalsIgnoreCase("jsonb")) {
                return new JsonbDataType();
            }
            return super.createDataType(sqlType, sqlTypeName);
        }
    }

    public class JsonbDataType extends AbstractDataType {
        JsonbDataType() {
            super("jsonb", Types.OTHER, String.class, false);
        }

        @Override
        public Object typeCast(Object value) {
            return value.toString();
        }
    }
}
