package ru.yandex.autotests.market.billing.backend.tms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.steps.TmsSteps;
import ru.yandex.autotests.market.common.environment.EnvironmentProperties;
import ru.yandex.autotests.market.common.environment.MarketEnvironment;
import ru.yandex.qatools.allure.annotations.Issue;

import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.COMPLETE_DAILY_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.DELIVERY_SERVICE_CATEGORIES_AVAILIABILITY_EXPORT_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.DYNAMIC_STATS_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.FEED_VALIDATION_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.FINANCE_CUTOFF_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.IMPORT_BANNERS_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.IMPORT_FEED_LOGS_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.IMPORT_GENERATIONS_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.IMPORT_INDEXER_FEED_CATEGORIES_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.IMPORT_SURVEY_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.LOAD_DELIVERY_SERVICES_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.RATE_LOADER_MANAGER;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.SCHEDULE_CUTOFF_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.SEND_SHIPMENTS_TO_BALANCE_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.SHOP_DATA_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.SHOP_OUTLET_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.SHOP_STATE_REPORT_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.UPDATE_ACTUAL_BALANCE_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.UPDATE_CPA_LATEST_ACTIVITY_PARAM_EXECUTOR;

/**
 * User:    Olka Kuzmina (strangelet)
 * Date:    13.09.13 : 17:23
 * Project: market-billing-backend
 */

@Aqua.Test(title = "Мониторинг  важных tms  задачек в биллинге за последний день")
@Feature("tmsTask.monitoring")
@RunWith(Parameterized.class)
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-2194")
public class TMSFireMonitoring {

    static TmsSteps tester = new TmsSteps();
    private final String jobName;

    public TMSFireMonitoring(String jobName) {
        this.jobName = jobName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        Collection<Object[]> result = new ArrayList<>(Arrays.asList(new Object[][]{
                {COMPLETE_DAILY_EXECUTOR}
                , {FEED_VALIDATION_EXECUTOR}
                , {FINANCE_CUTOFF_EXECUTOR}
                , {IMPORT_GENERATIONS_EXECUTOR}
                , {IMPORT_FEED_LOGS_EXECUTOR}
                , {IMPORT_INDEXER_FEED_CATEGORIES_EXECUTOR}
                , {RATE_LOADER_MANAGER}
                , {SCHEDULE_CUTOFF_EXECUTOR}
                , {SEND_SHIPMENTS_TO_BALANCE_EXECUTOR}
                , {SHOP_DATA_EXECUTOR}
                , {SHOP_OUTLET_EXECUTOR}
                , {SHOP_STATE_REPORT_EXECUTOR}
                , {UPDATE_ACTUAL_BALANCE_EXECUTOR}
                , {UPDATE_CPA_LATEST_ACTIVITY_PARAM_EXECUTOR}
                , {LOAD_DELIVERY_SERVICES_EXECUTOR}
                , {DELIVERY_SERVICE_CATEGORIES_AVAILIABILITY_EXPORT_EXECUTOR}
                , {IMPORT_BANNERS_EXECUTOR}
                , {IMPORT_SURVEY_EXECUTOR}
        }));
        if (new EnvironmentProperties().getEnvironment() != MarketEnvironment.MULTITESTING) {
            result.add(new Object[]{DYNAMIC_STATS_EXECUTOR});
        }
        return result;
    }

    @Test
    public void checkTMSJobs() {
        tester.checkLastJobSuccess(jobName);
    }
}
