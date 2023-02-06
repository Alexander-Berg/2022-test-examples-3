package ru.yandex.market.logistics.utilizer.base;

import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.logistics.utilizer.config.DbqueueIntegrationTestConfig;
import ru.yandex.market.logistics.utilizer.config.IntegrationTestConfig;

@SpringBootTest(
        classes = {
                IntegrationTestConfig.class,
                DbqueueIntegrationTestConfig.class,
        }
)
@DbUnitConfiguration(
        databaseConnection = {"dbUnitDatabaseConnection", "dbqueueDatabaseConnection"},
        dataSetLoader = NullableColumnsDataSetLoader.class
)
public abstract class DbqueueContextualTest extends IntegrationTest {

}
