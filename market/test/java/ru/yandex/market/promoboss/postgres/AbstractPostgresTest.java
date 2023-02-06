package ru.yandex.market.promoboss.postgres;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.promoboss.config.PostgresDbConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PostgresDbConfig.class}, initializers = PGaaSZonkyInitializer.class)
@TestPropertySource({"classpath:test_properties/pg_test.properties"})
public abstract class AbstractPostgresTest {
}
