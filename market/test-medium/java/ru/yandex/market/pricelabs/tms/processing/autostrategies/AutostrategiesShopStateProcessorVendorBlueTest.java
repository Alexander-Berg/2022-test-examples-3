package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

class AutostrategiesShopStateProcessorVendorBlueTest extends AbstractAutostrategiesShopStateProcessorTest {

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.vendorBlue);
    }

}
