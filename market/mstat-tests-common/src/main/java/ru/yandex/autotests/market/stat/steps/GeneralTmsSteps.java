package ru.yandex.autotests.market.stat.steps;

import java.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Assume;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.common.wait.FluentWait;
import ru.yandex.autotests.market.stat.beans.Job;
import ru.yandex.autotests.market.stat.beans.Packages;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.beans.meta.TmsStatus;
import ru.yandex.autotests.market.stat.conductor.ConductorClient;
import ru.yandex.autotests.market.stat.console.ITmsConsole;
import ru.yandex.autotests.market.stat.meta.TmsDao;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.market.stat.beans.matchers.JobHasStatusAsMatcher.hasStatus;
import static ru.yandex.autotests.market.stat.beans.matchers.TmsJobsSucceededOrRunningMatcher.allSucceededOrRunning;

/**
 * Created by entarrion on 06.04.15.
 */
public class GeneralTmsSteps<T extends Job> {
    protected TmsDao tmsMetaBase;
    protected List<Packages> packages;
    protected ITmsConsole<T> console;

    public GeneralTmsSteps(TmsDao tmsMetaBase, List<Packages> packages, ITmsConsole<T> console) {
        this.tmsMetaBase = tmsMetaBase;
        this.packages = packages;
        this.console = console;
    }

    public LocalDateTime getMinTimeToCheckAfter() {
        List<LocalDateTime> times = packages.stream().map(this::getDeployTimeWithLimit).collect(Collectors.toList());
        return times.stream().max(Comparator.comparing(it -> it)).orElse(null);
    }

    public LocalDateTime getDeployTimeWithLimit(Packages pkg) {
        LocalDateTime edge = LocalDateTime.now().minusDays(3);
        LocalDateTime result = getDeployTime(pkg);
        if (edge.isAfter(result)) {
            result = edge;
        }
        return result;
    }

    public LocalDateTime getDeployTime(Packages pkg) {
        LocalDateTime deployTimeForPackage;
        try {
            ConductorClient conductor = new ConductorClient();
            deployTimeForPackage = conductor.getLastDeployTimeForPackage(pkg.asString());
            Attacher.attach("Deploy Time", "Warning: deploy time is got by conductor client! " + deployTimeForPackage);
        } catch (IllegalStateException e) {
            deployTimeForPackage = LocalDateTime.now().minusHours(2);
            Attacher.attach("Deploy Time", "Warning: conductor request failed, using default value! " + deployTimeForPackage);
        }
        return deployTimeForPackage;
    }
//---------------------- RUN jobs -------------------------------------------------//

    /**
     * Runs job if not already running.
     * Otherwise does nothing.
     */
    @Step("Run job {0} if not running")
    public void runJobIfNotRunning(T job) {
        TmsRunState previousJob = getLastJobRunState(job);
        if (noPreviousJobRun(previousJob) || previousJob.completed()) {
            Attacher.attachAction("Running job " + job.getName());
            console.runJob(job);
        }
    }

    /**
     * Runs job if not already running and wait finish running job.
     * Otherwise does nothing.
     */
    @Step("Run job {0} if not running and wait finish running job")
    public TmsRunState runJobIfNotRunningAndWaitItFinish(T job) {
        TmsRunState jobState = getLastJobRunState(job);
        if (noPreviousJobRun(jobState) || jobState.completed()) {
            Attacher.attachAction("Running job " + job.getName());
            jobState = runJob(job);
        }
        return waitForJobRunToFinish(jobState, job.getMaxDuration());
    }

    /**
     * Waits for previous job to end if in progress,
     * runs new job, waits it to start, waits it to finish,
     * returns job state
     *
     * @param job
     * @return
     */
    @Step("Run job {0} and wait it finish")
    public TmsRunState runJobAndWaitItFinish(T job) {
        TmsRunState startedJob = runJob(job);
        return waitForJobRunToFinish(startedJob, job.getMaxDuration());
    }

    /**
     * Waits for previous jobs to end if in progress,
     * runs new jobs, waits them to start, waits them to finish,
     * returns job states
     *
     * @param jobs
     * @return
     */
    @Step("Run jobs and wait them finish: {0}")
    public List<TmsRunState> runJobsAndWaitThemFinish(List<T> jobs) {
        List<TmsRunState> startedJobs = runJobs(jobs);
        return waitForJobRunsToFinish(startedJobs,
            jobs.stream().map(Job::getMaxDuration).max(Comparator.naturalOrder()).get());
    }

    /**
     * Waits for previous job to end if in progress,
     * runs new job,
     * waits it to start,
     * returns job state
     */
    @Step("Run job: {0}")
    public TmsRunState runJob(T job) {
        return runJobs(Collections.singletonList(job)).get(0);
    }

    /**
     * Waits for previous jobs to end if in progress,
     * runs new jobs,
     * waits them to start,
     * returns job states
     */
    @Step("Run jobs: {0}")
    public List<TmsRunState> runJobs(List<T> jobsToRun) {
        return runJobsInternal(jobsToRun);
    }

    @Step("Pause job {0}")
    public void pauseJob(T job) {
        console.stopJob(job);
    }

    @Step("Resume cron job {0}")
    public void resumeJob(T job) {
        console.resumeJob(job);
    }


    //-------------------------------- Waiters -------------------------------------//
    public TmsRunState waitForJobRunToFinish(TmsRunState run) {
        return waitForJobRunToFinish(run, Duration.ofMinutes(30));
    }

    @Step("Wait for job to finish: {0}")
    private TmsRunState waitForJobRunToFinish(TmsRunState run, Duration maxTime) {
        return waitForJobRunsToFinishInternal(Collections.singletonList(run), maxTime).get(0);
    }

    @Step("Wait for job to finish: {0}")
    public TmsRunState waitForJobsToFinishInQrtzLog(TmsRunState run) {
        if (run != null && run.completed()) return run;
        return waitForJobRunsToFinishInQrtzLog(run);
    }

    @Step("Wait for finish job {0}")
    public TmsRunState waitForJobToFinish(T job) {
        TmsRunState lastRun = getLastJobRunState(job);
        return (noPreviousJobRun(lastRun) || lastRun.completed()) ? lastRun :
            waitForJobRunsToFinishInternal(Collections.singletonList(lastRun), job.getMaxDuration()).get(0);
    }

    @Step("Wait for jobs to finish: {0}")
    private List<TmsRunState> waitForJobRunsToFinish(List<TmsRunState> runs, Duration duration) {
        return waitForJobRunsToFinishInternal(runs, duration);
    }

    private List<TmsRunState> waitForJobRunsToFinishInternal(List<TmsRunState> run, Duration maxTime) {
        FluentWait wait = getFluentWaitFor(run, maxTime);
        Attacher.attachAction("Waiting for jobs " + getIds(run) + " to finish");
        wait.until(jobsAreFinished());
        Attacher.attachAction("Waiting done!");
        return tmsMetaBase.getJobsByIds(run);
    }

    private TmsRunState waitForJobRunsToFinishInQrtzLog(TmsRunState run) {
        FluentWait wait = getFluentWaitFor(Collections.singletonList(run), Duration.ofMinutes(30));
        wait.until(jobsAreFinishedInQrtz());
        return tmsMetaBase.getJobById(run);
    }

    private List<TmsRunState> runJobsInternal(List<T> jobsToRun) {
        Attacher.attachAction("Will run " + jobsToRun.size() + " jobs!");
        Attacher.attach("Jobs", jobsToRun);
        FluentWait<List<TmsRunState>> wait = getFluentWaitForJobs(jobsToRun,
            jobsToRun.stream().map(Job::getMaxDuration).max(Comparator.naturalOrder()).get());
        return (List<TmsRunState>) wait.until(allJobsStarted());
    }

    private TmsRunState runJobAfter(T job, TmsRunState state) {
        Attacher.attachAction("Running job " + job.getName());
        console.runJob(job);
        FluentWait wait = getFluentWaitForNewJob(state, job.getName());
        wait.until(nextJobStarted());
        return getLastJobRunState(job);
    }

    //-------------------------------- Check job run states -----------------------------------------//
    @Step("Getting state of job run {0}")
    public TmsRunState getRunState(TmsRunState run) {
        if (run == null || run.getId() == null) {
            throw new IllegalArgumentException("Can not find state for run without id. " + run);
        }
        TmsRunState runState = tmsMetaBase.getJobById(run);
        Attacher.attach("job run", runState);
        return runState;
    }

    @Step("Getting last job run {0}")
    public TmsRunState getLastJobRunState(T job) {
        TmsRunState lastJobRun = tmsMetaBase.getLastJobRun(job);
        Attacher.attach("job run", lastJobRun);
        return lastJobRun;
    }

    @Step("Getting last job run {0}")
    public TmsRunState getLastFinishedJobRunState(T job) {
        TmsRunState lastJobRun = tmsMetaBase.getLastFinishedJobRun(job);
        Attacher.attach("job run", lastJobRun);
        return lastJobRun;
    }

    private TmsRunState getLastJobRunState(String jobName) {
        TmsRunState lastJobRun = tmsMetaBase.getLastJobRun(jobName);
        Attacher.attach("job run", lastJobRun);
        return lastJobRun;
    }

    @Step("Get job {0} runs after {1}")
    public List<TmsRunState> getLastJobRunStates(T job, LocalDateTime after) {
        List<TmsRunState> tmsRunStates = tmsMetaBase.getJobRunStatesAfter(job, after);
        Attacher.attach("Statuses", tmsRunStates);
        Assume.assumeThat("Job never finished after " + after,
            tmsRunStates, hasItem(not(hasStatus(TmsStatus.RUNNING))));
        return tmsRunStates;
    }

    @Step("Check jobs run without failures")
    public void checkJobsSucceededOrRunning(Collection<TmsRunState> jobStatuses) {
        Assert.assertThat(jobStatuses, allSucceededOrRunning());
    }

    // ----------------------
    private Function<TmsRunState, Boolean> nextJobStarted() {
        return previousJob -> {
            TmsRunState currentJob = getLastJobRunState(previousJob.getJobName());
            return currentJob != null && !previousJob.getId().equals(currentJob.getId());
        };
    }

    private Function<List<TmsRunState>, Boolean> jobsAreFinished() {
        return jobs -> {
            Boolean actuallyRunning = firedTriggersContainJob(jobs);
            return !(actuallyRunning);
        };
    }

    private Function<List<TmsRunState>, Boolean> jobsAreFinishedInQrtz() {
        return this::qrtzLogJobsCompleted;
    }

    private Boolean qrtzLogJobsCompleted(List<TmsRunState> jobs) {
        List<TmsRunState> states = tmsMetaBase.getJobsByIds(jobs);
        Boolean allJobsCompleted = true;
        for (TmsRunState state : states) {
            allJobsCompleted = allJobsCompleted && state.completed();
        }
        return allJobsCompleted;
    }

    private Function allJobsStarted() {
        return new Function<List<T>, List<TmsRunState>>() {
            private Map<String, TmsRunState> previousRuns = new HashMap<>();
            private Set<T> runningJob = new HashSet<>();
            private List<TmsRunState> startedJobs = new ArrayList<>();

            @Override
            public List<TmsRunState> apply(List<T> input) {
                Set<T> jobsToRun = new HashSet(input);
                jobsToRun.removeAll(runningJob);
                Attacher.attachAction("Jobs in queue: " + jobsToRun.size());
                for (T jobToRun : jobsToRun) {
                    TmsRunState lastJobRunState;
                    if (previousRuns.get(jobToRun.getName()) != null) {
                        lastJobRunState = getRunState(previousRuns.get(jobToRun.getName()));
                    } else {
                        lastJobRunState = getLastJobRunState(jobToRun);
                    }
                    previousRuns.put(jobToRun.getName(), lastJobRunState);
                    if (noPreviousJobRun(lastJobRunState) || !firedTriggersContainJob(Collections.singletonList(lastJobRunState))) {
                        startedJobs.add(runJobAfter(jobToRun, lastJobRunState));
                        runningJob.add(jobToRun);
                    } else {
                        Attacher.attachAction("Previous run for job " + jobToRun.getName() + " is still in progress");
                    }
                }
                jobsToRun.removeAll(runningJob);
                return jobsToRun.isEmpty() ? startedJobs : null;
            }
        };
    }

    private FluentWait getFluentWaitFor(List<TmsRunState> jobs, Duration maxDuration) {
        return new FluentWait(jobs)
            .withTimeout(maxDuration.toMinutes(), ChronoUnit.MINUTES)
            .pollingEvery(20, ChronoUnit.SECONDS)
            .withMessage("Failed waiting for jobs [" + jobs + "] to finish");
    }

    private FluentWait getFluentWaitForNewJob(TmsRunState previousJob, String jobName) {
        String message = "Failed waiting  ";
        message += previousJob == null ? "first job to run for " + jobName : "job to run after [" +
            previousJob.getJobName() + ":" + previousJob.getId() + "]";

        if (previousJob == null) {
            previousJob = fakePreviousRunState(jobName);
        }

        Attacher.attachAction("Tms state for fluent wait " + previousJob);
        return new FluentWait(previousJob)
            .withTimeout(Duration.ofMinutes(10))
            .pollingEvery(5, ChronoUnit.SECONDS)
            .withMessage(message);
    }

    private FluentWait<List<TmsRunState>> getFluentWaitForJobs(List<T> jobsToRun, Duration duration) {
        return new FluentWait(jobsToRun)
            .withTimeout(duration.toMinutes(), ChronoUnit.MINUTES)
            .pollingEvery(5, ChronoUnit.SECONDS)
            .withMessage("Failed waiting for jobs [" + jobsToRun + "] to start");
    }

    private List<String> getIds(List<TmsRunState> runs) {
        return runs.stream().map(TmsRunState::getId).collect(Collectors.toList());
    }

    private Boolean firedTriggersContainJob(List<TmsRunState> jobs) {
        return tmsMetaBase.checkFiredTriggersForJobs(jobs);
    }

    private boolean noPreviousJobRun(TmsRunState previousJob) {
        return previousJob == null;
    }

    /**
     * This is for multi-testing where yet there is no previous job
     *
     * @param jobName
     * @return
     */
    private TmsRunState fakePreviousRunState(String jobName) {
        TmsRunState state;
        state = new TmsRunState();
        state.setId("0");
        state.setJobName(jobName);
        state.setFireTime(LocalDateTime.now().minusMinutes(10));
        state.setFinishTime(LocalDateTime.now().minusMinutes(5));
        state.setJobStatus("OK");
        return state;
    }
}
