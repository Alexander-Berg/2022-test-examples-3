package ru.yandex.market.common.test.jdbc;

import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.Driver;

import ru.yandex.market.common.test.transformer.StringTransformer;

/**
 * Фабрика для создания {@link InstrumentedDataSource}, используемого в юнит-тестах.
 */
public class InstrumentedDataSourceFactory implements DataSourceFactory {

    private final InstrumentedDataSource dataSource;

    public InstrumentedDataSourceFactory(StringTransformer stringTransformer) {
        dataSource = new InstrumentedDataSource(stringTransformer);
    }

    @Override
    public ConnectionProperties getConnectionProperties() {
        return new ConnectionProperties() {
            @Override
            public void setDriverClass(Class<? extends Driver> driverClass) {
                dataSource.setDriverClass(driverClass);
            }

            @Override
            public void setUrl(String url) {
                dataSource.setUrl(url);
            }

            @Override
            public void setUsername(String username) {
                dataSource.setUsername(username);
            }

            @Override
            public void setPassword(String password) {
                dataSource.setPassword(password);
            }
        };
    }

    @Override
    public DataSource getDataSource() {
        return this.dataSource;
    }

}
