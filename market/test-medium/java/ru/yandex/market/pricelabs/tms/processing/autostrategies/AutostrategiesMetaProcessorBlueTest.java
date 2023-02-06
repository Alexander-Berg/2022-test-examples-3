package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

class AutostrategiesMetaProcessorBlueTest extends AbstractAutostrategiesMetaProcessorTest {

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.blue);
    }

}
