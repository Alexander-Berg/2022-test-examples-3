package ru.yandex.market.logistic.gateway.config;

import java.time.Clock;

import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;

@TestConfiguration
@Import(value = {
    DbUnitTestConfiguration.class,
    AppConfig.class,
    AsyncConfig.class,
    LogisticApiConfig.class,
    MdbConfig.class,
    ConsumerClientConfig.class,
    AwsConfig.class,
    GruzinConfig.class,
    PersonalConfig.class,
})
@EnableZonkyEmbeddedPostgres
@AutoConfigureDataJpa
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection"})
public class ExecutorTestConfiguration {

    @Bean
    public Clock clock() {
        return new TestableClock();
    }
}
