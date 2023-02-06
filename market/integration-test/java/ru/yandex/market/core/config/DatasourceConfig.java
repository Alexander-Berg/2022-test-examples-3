package ru.yandex.market.core.config;

import javax.sql.DataSource;

import oracle.jdbc.driver.OracleDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.common.util.db.NamedDataSource;
import ru.yandex.market.core.database.JdbcBoilerplateConfig;
import ru.yandex.market.request.trace.Module;

@Configuration
@PropertySource({
        "classpath:common/common-servant.properties",
        "classpath:common-servant.properties",
})
@Import(JdbcBoilerplateConfig.class)
public class DatasourceConfig {
    @Value("${datasource.url}")
    private String datasourceUrl;

    @Value("${datasource.user}")
    private String user;

    @Value("${datasource.password}")
    private String password;

    @Bean
    Module sourceModule() {
        return Module.MBI_ADMIN;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        var configurer = new PropertySourcesPlaceholderConfigurer();
        // configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }

    @Bean
    public DataSource dataSource() {
        var dataSource = new NamedDataSource();
        dataSource.setDriver(new OracleDriver());
        dataSource.setUrl(datasourceUrl);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setModuleName(sourceModule().toString());
        return dataSource;
    }
}
