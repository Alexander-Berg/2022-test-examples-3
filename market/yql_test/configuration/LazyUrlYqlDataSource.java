package ru.yandex.market.yql_test.configuration;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

import ru.yandex.market.yql_query_service.config.AbstractYqlDatasource;
import ru.yandex.yql.YqlJdbcUrlParser;
import ru.yandex.yql.settings.YqlProperties;


public class LazyUrlYqlDataSource extends AbstractYqlDatasource {
    private final TestYqlDataSourcePropertiesConfig config;
    private String url;

    public LazyUrlYqlDataSource(TestYqlDataSourcePropertiesConfig config, YqlProperties properties) {
        super(properties);
        if (config == null) {
            throw new IllegalArgumentException("Incorrect Yql config: " + null);
        }
        this.config = config;
    }

    @Override
    public Connection getConnection() throws SQLException {
        init();
        return driver.connect(url, properties);
    }

    @Override
    public Connection getConnection(String user, String password) throws SQLException {
        init();
        return driver.connect(url, properties.withCredentials(user, password));
    }

    private void init() {
        if (url == null) {
            url = config.getUrl();
            try {
                this.properties = YqlJdbcUrlParser.parse(url, properties.asProperties());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

}
