package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.core.mongo.MongoTransaction;
import ru.yandex.market.tsum.pipe.engine.runtime.FullJobLaunchId;
import ru.yandex.market.tsum.pipe.engine.runtime.bazinga.JobScheduleTask;
import ru.yandex.market.tsum.pipe.engine.runtime.bazinga.JobScheduleTaskParameters;
import ru.yandex.market.tsum.pipe.engine.runtime.bazinga.JobWaitingScheduler;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 18.03.2019
 */
@Component
public class TestJobWaitingScheduler implements JobWaitingScheduler {
    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    private Queue<SchedulerTriggeredJob> triggeredJobs = new ArrayDeque<>();

    public class SchedulerTriggeredJob {
        private final FullJobLaunchId jobLaunchId;

        SchedulerTriggeredJob(FullJobLaunchId jobLaunchId) {
            this.jobLaunchId = jobLaunchId;
        }

        public FullJobLaunchId getJobLaunchId() {
            return jobLaunchId;
        }

        @Override
        public String toString() {
            return "SchedulerTriggeredJob{" +
                "jobLaunchId=" + jobLaunchId +
                '}';
        }
    }

    @Override
    public void schedule(FullJobLaunchId fullJobLaunchId) {
        checkJobIsWaitingForSchedule(fullJobLaunchId);

        triggeredJobs.add(new SchedulerTriggeredJob(fullJobLaunchId));
    }

    @Override
    public void schedule(FullJobLaunchId fullJobLaunchId, MongoTransaction transaction) {
        triggeredJobs.add(new SchedulerTriggeredJob(fullJobLaunchId));
    }

    @Override
    public void retry(JobScheduleTask jobScheduleTask, Instant date) {
        JobScheduleTaskParameters parameters = (JobScheduleTaskParameters) jobScheduleTask.getParameters();
        FullJobLaunchId fullJobLaunchId = new FullJobLaunchId(
            parameters.getPipeLaunchId(),
            parameters.getJobId(),
            parameters.getJobLaunchNumber());

        schedule(fullJobLaunchId);
    }

    private void checkJobIsWaitingForSchedule(FullJobLaunchId fullJobLaunchId) {
        String pipeLaunchId = fullJobLaunchId.getPipeLaunchId();
        String jobId = fullJobLaunchId.getJobId();
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Preconditions.checkNotNull(pipeLaunch);
        JobState jobState = pipeLaunch.getJobState(jobId);
        JobLaunch lastLaunch = jobState.getLastLaunch();
        Preconditions.checkNotNull(lastLaunch);
        Preconditions.checkState(
            jobState.isWaitingForScheduleChangeType()
        );
    }

    public Queue<SchedulerTriggeredJob> getTriggeredJobs() {
        return triggeredJobs;
    }
}
