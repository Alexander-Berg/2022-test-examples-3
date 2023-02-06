package ru.yandex.autotests.market.billing.backend.tms;

import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.jobstate.JobState;
import ru.yandex.autotests.market.billing.backend.steps.TmsSteps;


@Aqua.Test(title = "Мониторинг сломанных tms  задачек в биллинге за последний день")
@Feature("tmsTask.monitoring")
@RunWith(Parameterized.class)
public class TMSFailMonitoring {

    private transient static final Logger log = LogManager.getLogger(TMSFailMonitoring.class);

    static TmsSteps tester = new TmsSteps();
    private final JobState failedJob;

    public TMSFailMonitoring(JobState failedJob) {
        this.failedJob = failedJob;
    }

    @Parameterized.Parameters(name = "Проверка успешного запуска джобы {0}")
    public static Collection<Object[]> testData() {
        // получить список упавших джоб за сегодня
        Collection<Object[]> tests = tester.getFailedTmsJobsOnToday(
                MarketBillingConsole.SYNC_MARKET_HOSTS_EXECUTOR,
                MarketBillingConsole.SYNC_MARKET_DYNAMIC_HOSTS_EXECUTOR,
                MarketBillingConsole.IMPORT_MANAGERS_INFO_EXECUTOR,
                MarketBillingConsole.EXPORT_MESSAGES_TO_CRM_EXECUTOR,
                MarketBillingConsole.ORDER_TRANSACTION_STATUS_UPDATE_EXECUTOR,
                MarketBillingConsole.SYNC_YA_MONEY_SHOPS_EXECUTOR,
                MarketBillingConsole.SHOP_OUTLET_EXECUTOR,
                MarketBillingConsole.DAILY_PAYMENTS_IMPORT_EXECUTOR, // MBI-67892
                MarketBillingConsole.FULFILLMENT_SUPPLY_DRAFTS_IMPORT_EXECUTOR, // MBI-68210
                MarketBillingConsole.ORA_PG_VALIDATION_EXECUTOR, // временная от команды монетизации
                MarketBillingConsole.CREATE_BUSINESS_METRIKA_COUNTER_EXECUTOR, // MBI-65461
                MarketBillingConsole.UPDATE_BUSINESS_METRIKA_COUNTER_EXECUTOR, // MBI-64681
                MarketBillingConsole.MARKETING_CAMPAIGN_IMPORT_EXECUTOR,                // MBI-68211
                MarketBillingConsole.PARTNER_MARKETING_FIXED_BILLING_EXECUTOR,          // MBI-68211
                MarketBillingConsole.PARTNER_MARKETING_COMPENSATION_BILLING_EXECUTOR,   // MBI-68211
                "dummy" // просто хвост, чтобы дельта была меньше при изменениях
        );
        return tests.isEmpty()
                ? Collections.singleton(new Object[]{null})
                : tests;
    }

    @Test
    public void checkTMSJobs() {
        if (failedJob != null) {
            //возмём последний запуск данной джобы
            JobState lastFireJob = tester.getLastFinishedJobStateOnToday(failedJob);
            // проверим успешный ли он?
            tester.checkStatusLastJob(lastFireJob);
            //  проверим, что последний успешный запуск был после неудачного
            tester.checkLastJob(lastFireJob, failedJob);
            log.info(" lastFireJob  is  " + lastFireJob);
        }
    }
}
