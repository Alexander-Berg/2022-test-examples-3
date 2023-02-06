package ru.yandex.market.yql_test.configuration;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.yql_query_service.config.YqlDatasourceProvider;
import ru.yandex.yql.settings.YqlProperties;

@Configuration
@ParametersAreNonnullByDefault
@EnableConfigurationProperties(TestYqlDataSourcePropertiesConfig.class)
public class TestYqlDataSourceConfig {

    @Autowired
    private TestYqlDataSourcePropertiesConfig testYqlDataSourcePropertiesConfig;

    @Primary
    @Bean(name = "yqlDatasourceProvider")
    public YqlDatasourceProvider yqlDatasourceProvider(@Value("${yql.socket.timeout:30000}") int socketTimeout) {
        YqlProperties yqlProperties = new YqlProperties();
        yqlProperties.setPassword(testYqlDataSourcePropertiesConfig.getToken());
        yqlProperties.setSyntaxVersion(1);
        yqlProperties.setSocketTimeout(socketTimeout);
        DataSource yqlDataSource = new LazyUrlYqlDataSource(testYqlDataSourcePropertiesConfig, yqlProperties);

        return () -> yqlDataSource;
    }

}
