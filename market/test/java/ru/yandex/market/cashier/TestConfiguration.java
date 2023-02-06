package ru.yandex.market.cashier;

import com.opentable.db.postgres.embedded.ConnectionInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static ru.yandex.market.cashier.AbstractApplicationTest.preparedDbExtension;

@Configuration
//перечислить вс проперти, которые в папке properties.d - общие для всех сред настройки.
// плюс то, что специфичное для тестов - оно по смыслу эквивалентно тому что папках properties.d/<environment>
// но лежит в test/resources
@PropertySource(value = {
        "classpath:00_application.properties",
        "classpath:/local/90_local.properties",
        "classpath:999_test.properties"})
@ComponentScan(value = {
        "ru.yandex.market.cashier",
})
public class TestConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public static ConnectionInfo embeddedPostgres(){
        return preparedDbExtension.getConnectionInfo();
    }
}
