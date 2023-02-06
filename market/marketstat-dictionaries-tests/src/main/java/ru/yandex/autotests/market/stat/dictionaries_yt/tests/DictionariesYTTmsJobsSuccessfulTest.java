package ru.yandex.autotests.market.stat.dictionaries_yt.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionariesJob;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionariesJobs;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.dictionaries_yt.steps.DictionariesYtTmsSteps;
import ru.yandex.autotests.market.stat.steps.GeneralTmsSteps;
import ru.yandex.autotests.market.stat.util.ParametersUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Created by kateleb on 23.05.17.
 */

@Feature("Dictionaries YT")
@Aqua.Test(title = "Тест отсутствия ошибок в тмс логах")
@RunWith(Parameterized.class)
public class DictionariesYTTmsJobsSuccessfulTest {

    private DictionariesJob job;
    private GeneralTmsSteps tmsSteps;

    public DictionariesYTTmsJobsSuccessfulTest(DictionariesJob job) {
        this.job = job;
        this.tmsSteps = getTmsSteps();
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return ParametersUtils.asParameters(DictionariesJobs.ytJobs());
    }

    protected GeneralTmsSteps getTmsSteps() {
        return new DictionariesYtTmsSteps();
    }

    @Test
    public void testJobsSucceed() {
        // берем дату после которой можно проверять
        LocalDateTime minTime = tmsSteps.getMinTimeToCheckAfter();
        // проверяем, что джоба отработала после выкладки пакета
        List<TmsRunState> jobRunStates = tmsSteps.getLastJobRunStates(job, minTime);
        // проверяем, что статусы запусков - ок
        tmsSteps.checkJobsSucceededOrRunning(jobRunStates);
    }
}
