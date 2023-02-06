package ru.yandex.market.clab.db.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.clab.common.config.component.ServicesConfig;
import ru.yandex.market.clab.db.config.ControlledClockConfiguration;
import ru.yandex.market.clab.db.config.MainLiquibaseConfig;
import ru.yandex.market.clab.test.BaseIntegrationTest;
import ru.yandex.market.clab.test.IntegrationTestContextInitializer;
import ru.yandex.market.clab.test.config.TestRemoteServicesConfiguration;
import ru.yandex.market.common.postgres.embedded.PGSpringInitializer;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 02.10.2018
 */
@ContextConfiguration(initializers = {PGSpringInitializer.class, IntegrationTestContextInitializer.class})
@SpringBootTest(
    properties = {
        "spring.main.allow-bean-definition-overriding=true"
    },
    classes = {
        ServicesConfig.class,
        ControlledClockConfiguration.class,
        MainLiquibaseConfig.class,
        TestRemoteServicesConfiguration.class
    })
@Transactional
@ActiveProfiles("test")
public abstract class BasePgaasIntegrationTest extends BaseIntegrationTest {

}
