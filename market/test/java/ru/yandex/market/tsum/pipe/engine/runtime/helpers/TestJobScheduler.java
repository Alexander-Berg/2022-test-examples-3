package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.core.mongo.MongoTransaction;
import ru.yandex.market.tsum.pipe.engine.runtime.FullJobLaunchId;
import ru.yandex.market.tsum.pipe.engine.runtime.bazinga.JobScheduler;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 07.03.17
 */
@Component
public class TestJobScheduler implements JobScheduler {
    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    private Queue<TriggeredJob> triggeredJobs = new ArrayDeque<>();

    public class TriggeredJob {
        private final FullJobLaunchId jobLaunchId;

        TriggeredJob(FullJobLaunchId jobLaunchId) {
            this.jobLaunchId = jobLaunchId;
        }

        public FullJobLaunchId getJobLaunchId() {
            return jobLaunchId;
        }

        @Override
        public String toString() {
            return "TriggeredJob{" +
                "jobLaunchId=" + jobLaunchId +
                '}';
        }
    }

    @Override
    public void schedule(FullJobLaunchId jobLaunchId) {
        // Эта проверка нужна, чтобы убедиться, что исполнитель джобы
        // не увидит пайплайн в несохранённом состоянии. Проще говоря,
        // пересчёт состояния НЕ должен работать так:
        // 1. пересчёт состояния
        // 2. планирование джоб
        // 3. сохранение состояния
        checkJobIsQueuedOrForcedExecutorSucceeded(jobLaunchId);

        triggeredJobs.add(new TriggeredJob(jobLaunchId));
    }

    @Override
    public void schedule(FullJobLaunchId fullJobLaunchId, MongoTransaction transaction) {
        triggeredJobs.add(new TriggeredJob(fullJobLaunchId));
    }

    private void checkJobIsQueuedOrForcedExecutorSucceeded(FullJobLaunchId jobLaunchId) {
        String pipeLaunchId = jobLaunchId.getPipeLaunchId();
        String jobId = jobLaunchId.getJobId();
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Preconditions.checkNotNull(pipeLaunch);
        JobState jobState = pipeLaunch.getJobState(jobId);
        JobLaunch lastLaunch = jobState.getLastLaunch();
        Preconditions.checkNotNull(lastLaunch);
        Preconditions.checkState(
            lastLaunch.getLastStatusChange().getType().equals(StatusChangeType.QUEUED)
            || lastLaunch.getLastStatusChange().getType().equals(StatusChangeType.FORCED_EXECUTOR_SUCCEEDED)
        );
    }

    public Queue<TriggeredJob> getTriggeredJobs() {
        return triggeredJobs;
    }
}
