package ru.yandex.market.tpl.integration.tests.facade;

import ru.yandex.market.tpl.integration.tests.configuration.DeliveryTestConfiguration;
import ru.yandex.market.tpl.integration.tests.configuration.TestConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContext;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;

public abstract class BaseFacade {
    protected AutoTestContext getContext() {
        return AutoTestContextHolder.getContext();
    }

    protected TestConfiguration getConfig() {
        return getContext().getTestConfiguration();
    }

    protected DeliveryTestConfiguration getDeliveryConfig() {
        return getContext().getDeliveryTestConfiguration();
    }
}
