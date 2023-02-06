package ru.yandex.market.mboc.app;

import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mboc.common.IntegrationTestSourcesInitializer;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.mboc.common.ydb.TestYdbMockConfig;

/**
 * Common class for app integration tests with correct set of annotations.
 *
 * @author yuramalinov
 * @created 25.09.18
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = {
    IntegrationTestSourcesInitializer.class,
    PGaaSZonkyInitializer.class,
})
@SpringBootTest(
    properties = {
        "extra-properties=/app-integration-test.properties",
        "spring.profiles.active=test",
        "mboc.auth.debug=false", // This is useful, just set right user via token
        "mboc.auth.token-to-login=test:test"
    },
    classes = {AppTestConfiguration.class, TestYdbMockConfig.class,}
)
@AutoConfigureMockMvc
@Transactional
public abstract class BaseAppIntegrationTestClass {
}
