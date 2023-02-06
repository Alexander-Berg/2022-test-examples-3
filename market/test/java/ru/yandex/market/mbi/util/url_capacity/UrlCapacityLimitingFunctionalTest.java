package ru.yandex.market.mbi.util.url_capacity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.url_capacity.UrlCapacityLimiterConfig;

public class UrlCapacityLimitingFunctionalTest extends FunctionalTest {
    @Autowired
    @Qualifier("environmentService")
    protected EnvironmentService environmentService;

    protected void setFlagEnabled(boolean value) {
        environmentService.setValue(UrlCapacityLimiterConfig.ENABLED_VAR, Boolean.toString(value));
    }

    protected void setLogsOnly(boolean value) {
        environmentService.setValue(UrlCapacityLimiterConfig.LOGS_ONLY_VAR, Boolean.toString(value));
    }
}
