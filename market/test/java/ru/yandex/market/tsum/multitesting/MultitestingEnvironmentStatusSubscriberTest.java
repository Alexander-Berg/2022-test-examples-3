package ru.yandex.market.tsum.multitesting;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.common.UpstreamLink;
import ru.yandex.market.tsum.pipe.engine.definition.common.UpstreamStyle;
import ru.yandex.market.tsum.pipe.engine.definition.common.UpstreamType;
import ru.yandex.market.tsum.pipe.engine.definition.job.Job;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange;
import ru.yandex.market.tsum.pipe.engine.source_code.model.JobExecutorObject;
import ru.yandex.market.tsum.pipelines.common.jobs.multitesting.MultitestingTags;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment.Status.ARCHIVED;
import static ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment.Status.CLEANUP_FAILED;
import static ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment.Status.CLEANUP_TO_ARCHIVED;
import static ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment.Status.CLEANUP_TO_IDLE;
import static ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment.Status.DEPLOYING;
import static ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment.Status.DEPLOY_FAILED;
import static ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment.Status.IDLE;
import static ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment.Status.READY;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.executorFailed;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.executorInterrupted;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.executorKilled;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.executorSucceeded;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.failed;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.forcedExecutorSucceeded;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.interrupted;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.interrupting;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.killed;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.queued;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.running;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.subscribersFailed;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.subscribersSucceeded;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange.successful;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 15.11.2017
 */
public class MultitestingEnvironmentStatusSubscriberTest {
    private static int nextJobId = 1;

    @Test
    public void noJobs_deploying() {
        assertEquals(DEPLOYING, newStatus());
    }

    @Test
    public void oneUntaggedJob_deploying() {
        assertEquals(DEPLOYING, newStatus(job().status(queued())));
        assertEquals(DEPLOYING, newStatus(job().status(running())));
    }

    @Test
    public void oneUntaggedJob_deployFailed() {
        assertEquals(DEPLOY_FAILED, newStatus(job().status(interrupting())));
        assertEquals(DEPLOY_FAILED, newStatus(job().status(killed())));
        assertEquals(DEPLOY_FAILED, newStatus(job().status(executorInterrupted())));
        assertEquals(DEPLOY_FAILED, newStatus(job().status(executorKilled())));
        assertEquals(DEPLOY_FAILED, newStatus(job().status(executorFailed())));
        assertEquals(DEPLOY_FAILED, newStatus(job().status(subscribersFailed())));
        assertEquals(DEPLOY_FAILED, newStatus(job().status(failed())));
        assertEquals(DEPLOY_FAILED, newStatus(job().status(interrupted())));
    }

    @Test
    public void oneUntaggedJob_ready() {
        assertEquals(READY, newStatus(job().status(executorSucceeded())));
        assertEquals(READY, newStatus(job().status(forcedExecutorSucceeded())));
        assertEquals(READY, newStatus(job().status(subscribersSucceeded())));
        assertEquals(READY, newStatus(job().status(successful())));
    }


    @Test
    public void oneFailedUntaggedJobAndOneSuccessfulDeployJob_ready() {
        assertEquals(READY, newStatus(
            job().tagDeploy().status(executorSucceeded()),
            job().status(executorFailed())
        ));
    }

    @Test
    public void oneDeployJobWithFailedUpstream_deployFailed() {
        JobStateBuilder upstreamOfUpstream = job().status(executorFailed());
        JobStateBuilder upstream = job().upstreams(upstreamOfUpstream);
        JobStateBuilder deployJob = job().tagDeploy().upstreams(upstream);
        assertEquals(DEPLOY_FAILED, newStatus(deployJob, upstream, upstreamOfUpstream));
    }

    @Test
    public void successfulOutdatedJob_deploying() {
        assertEquals(DEPLOYING, newStatus(job().status(successful()).outdated()));
    }

    @Test
    public void twoDeployJobs() {
        assertEquals(DEPLOYING, newStatus(notStartedJob().tagDeploy(), notStartedJob().tagDeploy()));
        assertEquals(DEPLOYING, newStatus(notStartedJob().tagDeploy(), inProgressJob().tagDeploy()));
        assertEquals(DEPLOYING, newStatus(notStartedJob().tagDeploy(), successfulJob().tagDeploy()));
        assertEquals(DEPLOY_FAILED, newStatus(notStartedJob().tagDeploy(), failedJob().tagDeploy()));

        assertEquals(DEPLOYING, newStatus(inProgressJob().tagDeploy(), notStartedJob().tagDeploy()));
        assertEquals(DEPLOYING, newStatus(inProgressJob().tagDeploy(), inProgressJob().tagDeploy()));
        assertEquals(DEPLOYING, newStatus(inProgressJob().tagDeploy(), successfulJob().tagDeploy()));
        assertEquals(DEPLOY_FAILED, newStatus(inProgressJob().tagDeploy(), failedJob().tagDeploy()));

        assertEquals(DEPLOYING, newStatus(successfulJob().tagDeploy(), notStartedJob().tagDeploy()));
        assertEquals(DEPLOYING, newStatus(successfulJob().tagDeploy(), inProgressJob().tagDeploy()));
        assertEquals(READY, newStatus(successfulJob().tagDeploy(), successfulJob().tagDeploy()));
        assertEquals(DEPLOY_FAILED, newStatus(successfulJob().tagDeploy(), failedJob().tagDeploy()));

        assertEquals(DEPLOY_FAILED, newStatus(failedJob().tagDeploy(), notStartedJob().tagDeploy()));
        assertEquals(DEPLOY_FAILED, newStatus(failedJob().tagDeploy(), inProgressJob().tagDeploy()));
        assertEquals(DEPLOY_FAILED, newStatus(failedJob().tagDeploy(), successfulJob().tagDeploy()));
        assertEquals(DEPLOY_FAILED, newStatus(failedJob().tagDeploy(), failedJob().tagDeploy()));
    }

    @Test
    public void doNotIdleWhenArchive() {
        assertEquals(ARCHIVED, newStatus(ARCHIVED, successfulJob().tagCleanup()));
    }

    @Test
    public void deployJobAndCleanupJob_cleanupToArchived() {
        assertEquals(DEPLOYING, newStatus(CLEANUP_TO_ARCHIVED, notStartedJob().tagCleanup(),
            notStartedJob().tagDeploy()));
        assertEquals(DEPLOYING, newStatus(CLEANUP_TO_ARCHIVED, notStartedJob().tagCleanup(),
            inProgressJob().tagDeploy()));
        assertEquals(READY, newStatus(CLEANUP_TO_ARCHIVED, notStartedJob().tagCleanup(), successfulJob().tagDeploy()));
        assertEquals(DEPLOY_FAILED, newStatus(CLEANUP_TO_ARCHIVED, notStartedJob().tagCleanup(),
            failedJob().tagDeploy()));

        assertEquals(CLEANUP_TO_ARCHIVED, newStatus(CLEANUP_TO_ARCHIVED, inProgressJob().tagCleanup(),
            notStartedJob().tagDeploy()));
        assertEquals(CLEANUP_TO_ARCHIVED, newStatus(CLEANUP_TO_ARCHIVED, inProgressJob().tagCleanup(),
            inProgressJob().tagDeploy()));
        assertEquals(CLEANUP_TO_ARCHIVED, newStatus(CLEANUP_TO_ARCHIVED, inProgressJob().tagCleanup(),
            successfulJob().tagDeploy()));
        assertEquals(CLEANUP_TO_ARCHIVED, newStatus(CLEANUP_TO_ARCHIVED, inProgressJob().tagCleanup(),
            failedJob().tagDeploy()));

        assertEquals(ARCHIVED, newStatus(CLEANUP_TO_ARCHIVED, successfulJob().tagCleanup(),
            notStartedJob().tagDeploy()));
        assertEquals(ARCHIVED, newStatus(CLEANUP_TO_ARCHIVED, successfulJob().tagCleanup(),
            inProgressJob().tagDeploy()));
        assertEquals(ARCHIVED, newStatus(CLEANUP_TO_ARCHIVED, successfulJob().tagCleanup(),
            successfulJob().tagDeploy()));
        assertEquals(ARCHIVED, newStatus(CLEANUP_TO_ARCHIVED, successfulJob().tagCleanup(), failedJob().tagDeploy()));

        assertEquals(CLEANUP_FAILED, newStatus(CLEANUP_TO_ARCHIVED, failedJob().tagCleanup(),
            notStartedJob().tagDeploy()));
        assertEquals(CLEANUP_FAILED, newStatus(CLEANUP_TO_ARCHIVED, failedJob().tagCleanup(),
            inProgressJob().tagDeploy()));
        assertEquals(CLEANUP_FAILED, newStatus(CLEANUP_TO_ARCHIVED, failedJob().tagCleanup(),
            successfulJob().tagDeploy()));
        assertEquals(CLEANUP_FAILED, newStatus(CLEANUP_TO_ARCHIVED, failedJob().tagCleanup(), failedJob().tagDeploy()));
    }

    @Test
    public void deployJobAndCleanupJob_cleanupToIdle() {
        assertEquals(DEPLOYING, newStatus(CLEANUP_TO_IDLE, notStartedJob().tagCleanup(), notStartedJob().tagDeploy()));
        assertEquals(DEPLOYING, newStatus(CLEANUP_TO_IDLE, notStartedJob().tagCleanup(), inProgressJob().tagDeploy()));
        assertEquals(READY, newStatus(CLEANUP_TO_IDLE, notStartedJob().tagCleanup(), successfulJob().tagDeploy()));
        assertEquals(DEPLOY_FAILED, newStatus(CLEANUP_TO_IDLE, notStartedJob().tagCleanup(), failedJob().tagDeploy()));

        assertEquals(CLEANUP_TO_IDLE, newStatus(CLEANUP_TO_IDLE, inProgressJob().tagCleanup(),
            notStartedJob().tagDeploy()));
        assertEquals(CLEANUP_TO_IDLE, newStatus(CLEANUP_TO_IDLE, inProgressJob().tagCleanup(),
            inProgressJob().tagDeploy()));
        assertEquals(CLEANUP_TO_IDLE, newStatus(CLEANUP_TO_IDLE, inProgressJob().tagCleanup(),
            successfulJob().tagDeploy()));
        assertEquals(CLEANUP_TO_IDLE, newStatus(CLEANUP_TO_IDLE, inProgressJob().tagCleanup(),
            failedJob().tagDeploy()));

        assertEquals(IDLE, newStatus(CLEANUP_TO_IDLE, successfulJob().tagCleanup(), notStartedJob().tagDeploy()));
        assertEquals(IDLE, newStatus(CLEANUP_TO_IDLE, successfulJob().tagCleanup(), inProgressJob().tagDeploy()));
        assertEquals(IDLE, newStatus(CLEANUP_TO_IDLE, successfulJob().tagCleanup(), successfulJob().tagDeploy()));
        assertEquals(IDLE, newStatus(CLEANUP_TO_IDLE, successfulJob().tagCleanup(), failedJob().tagDeploy()));

        assertEquals(CLEANUP_FAILED, newStatus(CLEANUP_TO_IDLE, failedJob().tagCleanup(), notStartedJob().tagDeploy()));
        assertEquals(CLEANUP_FAILED, newStatus(CLEANUP_TO_IDLE, failedJob().tagCleanup(), inProgressJob().tagDeploy()));
        assertEquals(CLEANUP_FAILED, newStatus(CLEANUP_TO_IDLE, failedJob().tagCleanup(), successfulJob().tagDeploy()));
        assertEquals(CLEANUP_FAILED, newStatus(CLEANUP_TO_IDLE, failedJob().tagCleanup(), failedJob().tagDeploy()));
    }

    @Test  // https://st.yandex-team.ru/MARKETINFRA-5360
    public void deployJobsSucceededButSomeUpstreamsDidNot() {
        JobStateBuilder upstream1 = job().status(executorSucceeded());
        JobStateBuilder upstream2 = job().status(executorFailed());
        JobStateBuilder deployJob = successfulJob().tagDeploy().upstreams(upstream1, upstream2);
        assertEquals(READY, newStatus(deployJob, upstream1, upstream2));
    }

    private static MultitestingEnvironment.Status newStatus(JobState... jobStates) {
        return newStatus(null, jobStates);
    }

    private static MultitestingEnvironment.Status newStatus(MultitestingEnvironment.Status oldStatus,
                                                            JobState... jobStates) {
        return MultitestingEnvironmentStatusSubscriber.calculateNewStatus(oldStatus, asList(jobStates));
    }


    private JobStateBuilder job() {
        int id = nextJobId++;
        Job job = mock(Job.class);
        Mockito.<Class<? extends JobExecutor>>when(job.getExecutorClass()).thenReturn(DummyJob.class);
        when(job.getId()).thenReturn(Integer.toString(id));

        return new JobStateBuilder(job, Collections.emptySet(), false, Collections.emptyList(),
            Collections.emptyList());
    }

    private JobStateBuilder notStartedJob() {
        return job();
    }

    private JobStateBuilder inProgressJob() {
        return job().status(running());
    }

    private JobStateBuilder successfulJob() {
        return job().status(successful());
    }

    private JobStateBuilder failedJob() {
        return job().status(failed());
    }

    private static class JobStateBuilder extends JobState {
        private final Job job;
        private static final JobExecutorObject EXECUTOR_OBJECT = new JobExecutorObject(
            UUID.randomUUID(), DummyJob.class, Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList()
        );

        private JobStateBuilder(Job job, Set<UpstreamLink<String>> upstreams, boolean outdated,
                                Iterable<JobLaunch> launches, Collection<String> tags) {
            super(
                job,
                EXECUTOR_OBJECT,
                upstreams,
                ResourceRefContainer.empty()
            );
            setOutdated(outdated);
            launches.forEach(this::addLaunch);
            getTags().addAll(tags);
            this.job = job;
        }

        JobStateBuilder outdated() {
            return new JobStateBuilder(this.job, getUpstreams(), true, getLaunches(), getTags());
        }

        JobStateBuilder upstreams(JobState... jobStates) {
            return new JobStateBuilder(
                this.job,
                Arrays.stream(jobStates)
                    .map(jobState -> new UpstreamLink<>(jobState.getJobId(), UpstreamType.ALL_RESOURCES,
                        UpstreamStyle.MODERN))
                    .collect(Collectors.toSet()),
                isOutdated(),
                getLaunches(),
                getTags()
            );
        }

        JobStateBuilder status(StatusChange... statusChanges) {
            return new JobStateBuilder(
                this.job,
                getUpstreams(),
                isOutdated(),
                Collections.singletonList(
                    new JobLaunch(
                        1,
                        "",
                        Collections.emptyList(),
                        asList(statusChanges)
                    )
                ),
                getTags()
            );
        }

        JobStateBuilder tag(String tag) {
            return new JobStateBuilder(this.job, getUpstreams(), false, getLaunches(), Collections.singleton(tag));
        }

        JobStateBuilder tagDeploy() {
            return tag(MultitestingTags.FINALIZING);
        }

        JobStateBuilder tagCleanup() {
            return tag(MultitestingTags.CLEANUP);
        }
    }
}
