package ru.yandex.market.promoboss.dao;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.promoboss.config.DbConfiguration;
import ru.yandex.market.promoboss.postgres.AbstractPostgresTest;

@ContextConfiguration(initializers = PGaaSZonkyInitializer.class)
@ExtendWith(SpringExtension.class)
@TestExecutionListeners(
        value = {
                DependencyInjectionTestExecutionListener.class,
                DbUnitTestExecutionListener.class
        }
)
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
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJdbcRepositories("ru.yandex.market.promoboss.dao")
@DbUnitDataSet(before = "AbstractPromoRepositoryTest.before.csv")
public abstract class AbstractPromoRepositoryTest extends AbstractPostgresTest {
    protected static final Long PROMO_ID_1 = 1L;
}
