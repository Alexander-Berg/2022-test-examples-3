package ru.yandex.market.promoboss;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.promoboss.config.DbConfiguration;
import ru.yandex.market.promoboss.config.PostgresDbConfig;

@ExtendWith(SpringExtension.class)
@TestExecutionListeners(
        value = {
                DependencyInjectionTestExecutionListener.class,
                DbUnitTestExecutionListener.class
        }
)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        }
)
@TestPropertySource(locations = {
        "classpath:test_properties/pg_test.properties",
        "classpath:test_properties/yt_test.properties",
        "classpath:test_properties/application.properties"
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
                value = "ru.yandex.market.common.test.db.ddl.datatype.CustomPostgresqlDataTypeFactory"
        )
)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class, classes = {PostgresDbConfig.class})
public class AbstractDBFunctionalTest {
}
