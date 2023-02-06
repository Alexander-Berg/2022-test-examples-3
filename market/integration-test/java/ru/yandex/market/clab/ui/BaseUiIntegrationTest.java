package ru.yandex.market.clab.ui;

import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.market.clab.db.config.ControlledClockConfiguration;
import ru.yandex.market.clab.db.config.MainLiquibaseConfig;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;
import ru.yandex.market.clab.test.NasTestHelper;
import ru.yandex.market.clab.test.config.TestRemoteServicesConfiguration;
import ru.yandex.market.clab.ui.config.UIServiceConfig;

@SpringBootTest(
    properties = {
        "spring.main.allow-bean-definition-overriding=true"
    },
    classes = {
        MainLiquibaseConfig.class,
        ControlledClockConfiguration.class,
        UIServiceConfig.class,
        NasTestHelper.class,
        TestRemoteServicesConfiguration.class
    })
public abstract class BaseUiIntegrationTest extends BasePgaasIntegrationTest {
}
