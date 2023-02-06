package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

class AutostrategiesShopStateProcessorBlueTest extends AbstractAutostrategiesShopStateProcessorTest {

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.blue);
    }

}
