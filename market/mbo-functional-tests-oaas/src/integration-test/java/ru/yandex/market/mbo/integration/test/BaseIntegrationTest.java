package ru.yandex.market.mbo.integration.test;

import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.mbo.common.utils.PGaaSInitializer;
import ru.yandex.market.mbo.integration.test.config.IntegrationTestConfig;
import ru.yandex.market.mbo.integration.test.initializers.DatabasesInitializer;

/**
 * Общая конфигурация для переиспользования контекста в тестах.
 *
 * @author s-ermakov
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    IntegrationTestConfig.class,
}, initializers = {
    DatabasesInitializer.class,
    PGaaSInitializer.class,
})
@ActiveProfiles({"test", "functional-tests"})
@Transactional("multiTransactionManager")
public abstract class BaseIntegrationTest {
}
