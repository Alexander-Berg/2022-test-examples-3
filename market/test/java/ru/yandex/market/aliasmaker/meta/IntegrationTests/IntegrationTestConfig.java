package ru.yandex.market.aliasmaker.meta.IntegrationTests;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.aliasmaker.meta.config.MetaDbConfig;

@Configuration
@PropertySource("classpath:test.properties")
@Import({MetaDbConfig.class})
@Transactional
public class IntegrationTestConfig {
}
