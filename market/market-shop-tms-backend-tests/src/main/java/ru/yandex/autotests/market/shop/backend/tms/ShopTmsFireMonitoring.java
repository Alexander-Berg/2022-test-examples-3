package ru.yandex.autotests.market.shop.backend.tms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.shop.backend.steps.TmsSteps;

import static ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsole.IMPORT_GENERATIONS_EXECUTOR;
import static ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsole.SCHEDULE_CUTOFF_EXECUTOR;
import static ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsole.SHOP_DATA_EXECUTOR;
import static ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsole.SHOP_OUTLET_EXECUTOR;
import static ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsole.SHOP_STATE_REPORT_EXECUTOR;
import static ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsole.UPDATE_CPA_LATEST_ACTIVITY_PARAM_EXECUTOR;


@Aqua.Test(title = "Мониторинг  важных tms  задачек в shop-tms за последний день")
@Feature("tmsTask.monitoring")
@RunWith(Parameterized.class)
public class ShopTmsFireMonitoring {
    static TmsSteps tester = new TmsSteps();
    private final String jobName;

    public ShopTmsFireMonitoring(String jobName) {
        this.jobName = jobName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        Collection<Object[]> result = new ArrayList<>(Arrays.asList(new Object[][]{
                {IMPORT_GENERATIONS_EXECUTOR}
                , {SCHEDULE_CUTOFF_EXECUTOR}
                , {SHOP_DATA_EXECUTOR}
                , {SHOP_OUTLET_EXECUTOR}
                , {SHOP_STATE_REPORT_EXECUTOR}
                , {UPDATE_CPA_LATEST_ACTIVITY_PARAM_EXECUTOR}
        }));
        return result;
    }

    @Test
    public void checkTMSJobs() {
        tester.checkLastJobSuccess(jobName);
    }
}
