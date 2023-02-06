package ru.yandex.cs.billing;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.cs.billing.config.CsBillingTmsFunctionalTestConfig;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;

@SpringJUnitConfig(classes = CsBillingTmsFunctionalTestConfig.class)
@PreserveDictionariesDbUnitDataSet
public abstract class AbstractCsBillingTmsFunctionalTest extends FunctionalTest {
}
