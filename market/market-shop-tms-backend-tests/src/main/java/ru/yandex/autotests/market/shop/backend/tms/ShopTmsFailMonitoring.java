package ru.yandex.autotests.market.shop.backend.tms;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.jobstate.JobState;
import ru.yandex.autotests.market.shop.backend.steps.TmsSteps;


import java.util.Collection;
import java.util.Collections;

import static ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsole.IMPORT_MANAGERS_INFO_EXECUTOR;
import static ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsole.SHOP_OUTLET_EXECUTOR;


@Aqua.Test(title = "Мониторинг сломанных tms задачек в shop-tms за последний день")
@Feature("tmsTask.monitoring")@RunWith(Parameterized.class)
public class ShopTmsFailMonitoring {

    private transient static final Logger log = LogManager.getLogger(ShopTmsFailMonitoring.class);

    static TmsSteps tester = new TmsSteps();
    private final JobState failedJob;

    public ShopTmsFailMonitoring(JobState failedJob) {
        this.failedJob = failedJob;
    }

    @Parameterized.Parameters(name = "Проверка успешного запуска упавшей джобы {0}")
    public static Collection<Object[]> testData() {
        // получить список упавших джоб за сегодня
        Collection<Object[]> tests = tester.getFailedTmsJobsOnToday(
                IMPORT_MANAGERS_INFO_EXECUTOR,
                SHOP_OUTLET_EXECUTOR
        );
        return !tests.isEmpty()
                ? tests
                : Collections.singleton(new Object[]{null});
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
