package ru.yandex.market.clab.tms;

import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.market.clab.common.config.component.ServicesConfig;
import ru.yandex.market.clab.db.config.MainLiquibaseConfig;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;
import ru.yandex.market.clab.test.config.TestRemoteServicesConfiguration;
import ru.yandex.market.clab.tms.config.BillingConfig;

@SpringBootTest(classes = {
    BillingConfig.class,
    ServicesConfig.class,
    MainLiquibaseConfig.class,
    TestRemoteServicesConfiguration.class
})
public abstract class BaseTmsIntegrationTest extends BasePgaasIntegrationTest {
}
