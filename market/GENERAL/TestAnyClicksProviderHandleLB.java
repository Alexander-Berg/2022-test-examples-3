package ru.yandex.autotests.market.stat.generator;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;

/**
 * Created by kateleb on 02.09.17.
 */
@Feature("Click generator")
@Aqua.Test(title = "Generate cpa or vendor or cpc clicks with params for LB")
public class TestAnyClicksProviderHandleLB extends AnyClicksProvider {

    public TestAnyClicksProviderHandleLB() {
        super();
    }
}
