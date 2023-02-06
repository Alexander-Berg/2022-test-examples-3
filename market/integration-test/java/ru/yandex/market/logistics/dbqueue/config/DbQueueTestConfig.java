package ru.yandex.market.logistics.dbqueue.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;

@Configuration
@EnableZonkyEmbeddedPostgres
@EnableAutoConfiguration(exclude = LiquibaseAutoConfiguration.class)
@SpringBootConfiguration
@Import({
    DbQueueConfig.class,
    DbUnitTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.logistics.dbqueue.processor")
public class DbQueueTestConfig {

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }
}
