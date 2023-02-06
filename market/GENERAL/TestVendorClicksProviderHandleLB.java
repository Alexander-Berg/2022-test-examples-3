package ru.yandex.autotests.market.stat.generator;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;

import static ru.yandex.autotests.market.stat.logbroker.LogbrokerConfig.VENDOR_CLICKS;

/**
 * Created by kateleb on 11.03.15.
 */
@Feature("Click generator")
@Aqua.Test(title = "Generate vendor clicks with params for LB")
public class TestVendorClicksProviderHandleLB extends AnyClicksProvider {

    public TestVendorClicksProviderHandleLB() {
        super(VENDOR_CLICKS);
    }
}
