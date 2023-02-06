package ru.yandex.market.mbo.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import ru.yandex.market.mbo.configs.db.category.CategoryTransitionsRepositoryConfig;
import ru.yandex.market.mbo.configs.db.model.ModelTransitionsRepositoryConfig;
import ru.yandex.market.mbo.database.configs.MboPgLiquibaseConfig;
import ru.yandex.market.mbo.database.configs.PostgresDatabaseConfig;

@Configuration
@PropertySource("classpath:db-test.properties")
@Import({
    PostgresDatabaseConfig.class,
    MboPgLiquibaseConfig.class,
    ModelTransitionsRepositoryConfig.class,
    CategoryTransitionsRepositoryConfig.class
})
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class DbTestConfiguration {
    @Bean
    public PostgresSQLBeanPostProcessor postgresSQLBeanPostProcessor() {
        return new PostgresSQLBeanPostProcessor();
    }
}
