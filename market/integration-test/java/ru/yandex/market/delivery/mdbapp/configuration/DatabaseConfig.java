package ru.yandex.market.delivery.mdbapp.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnablePooledZonkyEmbeddedPostgres;

@Configuration
@EnablePooledZonkyEmbeddedPostgres
@Import({
    DbUnitTestConfiguration.class,
})
public class DatabaseConfig {
}
