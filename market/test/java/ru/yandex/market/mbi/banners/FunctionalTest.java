package ru.yandex.market.mbi.banners;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mbi.banners.config.AppConfig;
import ru.yandex.market.mbi.banners.config.EnvironmentConfig;
import ru.yandex.market.mbi.banners.config.MbiDbConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

/**
 * Корневой класс для написания функциональных тестов.
 */
@ExtendWith({
        SpringExtension.class,
})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class,
                EnvironmentConfig.class,
                AppConfig.class,
                MbiDbConfig.class,
                TestConfig.class
        }
)
@ActiveProfiles(profiles = {"functionalTest"})
@ContextConfiguration(initializers = {PGaaSZonkyInitializer.class})
@TestPropertySource({
        "classpath:test_properties/functional-test.properties",
        "classpath:test_properties/postgres_test.properties"
})
@TestExecutionListeners(
        listeners = {
                DbUnitTestExecutionListener.class,
        },
        mergeMode = MERGE_WITH_DEFAULTS
)
public abstract class FunctionalTest {

    @Value("http://localhost:${local.server.port}")
    protected String baseUrl;
}
