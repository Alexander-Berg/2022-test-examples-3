package ru.yandex.market.tms.quartz2.solomon;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.market.tms.quartz2.spring.config.TmsDataSourceConfig;

@Configuration
public class BaseTmsTestConfig {

    private final DataSource dataSource;

    public BaseTmsTestConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public TmsDataSourceConfig tmsDataSourceConfig() {
        return new TestTmsDataSourceConfig(dataSource);
    }
}
