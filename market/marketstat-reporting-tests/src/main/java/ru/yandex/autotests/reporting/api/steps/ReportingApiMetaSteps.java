package ru.yandex.autotests.reporting.api.steps;

import com.google.gson.JsonObject;
import ru.yandex.autotests.market.common.wait.FluentWait;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.reporting.api.beans.BuildReportJob;
import ru.yandex.autotests.reporting.api.beans.BuildReportTmsJob;
import ru.yandex.autotests.reporting.api.dao.ReportingApiDao;
import ru.yandex.autotests.reporting.api.dao.ReportingApiJdbcDao;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by kateleb on 17.11.16.
 */
public class ReportingApiMetaSteps {
    private ReportingApiDao metaBase = ReportingApiJdbcDao.getInstance();

    public BuildReportTmsJob waitForJobToFinish(String jobName){
        FluentWait wait = getFluentWaitFor(jobName, Duration.ofMinutes(30));
        Attacher.attachAction("Waiting for job " + jobName + " to finish");
        BuildReportTmsJob tmsRunState = (BuildReportTmsJob) wait.until(jobIsFinished());
        Attacher.attachAction("Waiting done!");
        Attacher.attach("job", tmsRunState);
        return tmsRunState;
    }

    private Function jobIsFinished() {
        return (Function<String, BuildReportTmsJob>) name -> {
            BuildReportTmsJob job = metaBase.getJobDetails(name);
            Attacher.attachAction("found by now " + job);
            if (job.getStatus().equals("SUCCESSFUL") || job.getStatus().equals("FAILED")) {
                return job;
            }
            return null;
        };
    }

    private FluentWait getFluentWaitFor(String jobName, Duration duration) {
        return new FluentWait(jobName)
                .withTimeout(duration)
                .pollingEvery(20, ChronoUnit.SECONDS)
                .withMessage("Failed waiting for job [" + jobName + "] to finish");
    }


    @Step("Wait for build report job ti finish and check status")
    public BuildReportJob getBuildReportJob(JsonObject response) {
            BuildReportTmsJob jobStatus = waitForJobToFinish(new BuildReportJob(response).getJobId());
            assertThat("Job was not successful!", jobStatus.getStatus(), is("SUCCESSFUL"));
            return new BuildReportJob(jobStatus);
    }

    public BuildReportJob getFinishedJob(String jobIdName) {
        BuildReportTmsJob jobStatus = waitForJobToFinish(jobIdName);
        assertThat("Job was not successful!", jobStatus.getStatus(), is("SUCCESSFUL"));
        return new BuildReportJob(jobStatus);
    }
}
