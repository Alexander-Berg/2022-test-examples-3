package ru.yandex.market.crm;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(RolesConfiguration.class)
@PropertySource("classpath:ru/yandex/market/crm/spring_infrastructure_test_properties.properties")
public class RolesTestConfiguration {
}
