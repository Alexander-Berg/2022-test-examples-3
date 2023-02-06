package {root_package}.postgres;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import {root_package}.config.EmbeddedDbConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EmbeddedDbConfig.class}, initializers = PGaaSZonkyInitializer.class)
@TestPropertySource({"classpath:test_properties/postgres_test.properties"})
public abstract class AbstractPostgresTest {

}
