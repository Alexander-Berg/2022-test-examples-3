package ru.yandex.cs.billing.tms;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.cs.billing.tms.config.CsBillingTmsExternalFunctionalTestConfig;
import ru.yandex.market.TestContainersInitializer;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

@SpringJUnitConfig(
    classes = CsBillingTmsExternalFunctionalTestConfig.class,
    initializers = TestContainersInitializer.class
)
@ActiveProfiles({"externalFunctionalTest", "development"})
@DbUnitDataSet(before = "/before-oracle.csv", dataSource = "csBillingDataSource")
public abstract class AbstractCsBillingTmsExternalFunctionalTest extends JupiterDbUnitTest {
}
