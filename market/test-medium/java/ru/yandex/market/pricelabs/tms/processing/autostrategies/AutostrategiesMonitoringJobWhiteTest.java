package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

public class AutostrategiesMonitoringJobWhiteTest extends AbstractAutostrategiesMonitoringJobTest {

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.white);
    }
}
