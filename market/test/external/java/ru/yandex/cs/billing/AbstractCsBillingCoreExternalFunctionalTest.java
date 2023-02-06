package ru.yandex.cs.billing;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.cs.billing.config.CsBillingCoreExternalFunctionalTestConfig;
import ru.yandex.market.TestContainersInitializer;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

@SpringJUnitConfig(
        classes = CsBillingCoreExternalFunctionalTestConfig.class,
        initializers = TestContainersInitializer.class
)
@ActiveProfiles({"externalFunctionalTest", "development"})
@DbUnitDataSet(before = "/before-oracle.csv", dataSource = "csBillingDataSource")
public abstract class AbstractCsBillingCoreExternalFunctionalTest extends JupiterDbUnitTest {
}
