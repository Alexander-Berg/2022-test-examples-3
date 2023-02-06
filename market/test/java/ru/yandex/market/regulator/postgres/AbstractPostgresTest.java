package ru.yandex.market.regulator.postgres;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.regulator.config.EmbeddedDbConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EmbeddedDbConfig.class}, initializers = PGaaSZonkyInitializer.class)
@TestPropertySource({"classpath:test_properties/postgres_test.properties"})
public abstract class AbstractPostgresTest {

}
