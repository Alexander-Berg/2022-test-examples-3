package ru.yandex.market.promoboss.postgres;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.promoboss.config.DbConfiguration;
import ru.yandex.market.promoboss.config.PostgresDbConfig;

@ContextConfiguration(initializers = PGaaSZonkyInitializer.class, classes = {PostgresDbConfig.class})
@ExtendWith(SpringExtension.class)
@TestExecutionListeners(
        value = {
                DependencyInjectionTestExecutionListener.class,
                DbUnitTestExecutionListener.class
        }
)
@TestPropertySource(locations = {
        "classpath:test_properties/pg_test.properties"
})
@DbUnitDataSet(
        nonTruncatedTables = {
                "public.databasechangelog",
                "public.databasechangeloglock"
        }
)
@Import({
        DbConfiguration.class
})
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = "datatypeFactory",
                value = "ru.yandex.market.promoboss.pg.ExtendedPostgresqlDataTypeFactory"
        )
)
public abstract class AbstractDbUnitPostgresTest extends AbstractJdbcRecipeTest {
}

