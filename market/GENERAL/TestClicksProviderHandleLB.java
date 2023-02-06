package ru.yandex.autotests.market.stat.generator;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;

import static ru.yandex.autotests.market.stat.logbroker.LogbrokerConfig.CLICKS;

/**
 * Created by kateleb on 11.03.15.
 */
@Feature("Click generator")
@Aqua.Test(title = "Generate clicks with params for LB")
public class TestClicksProviderHandleLB extends AnyClicksProvider {

    public TestClicksProviderHandleLB() {
        super(CLICKS);
    }
}
