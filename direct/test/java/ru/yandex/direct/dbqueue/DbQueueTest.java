package ru.yandex.direct.dbqueue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.mutable.MutableLong;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.dbqueue.configuration.DbQueueTestingConfiguration;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.model.DbQueueJobStatus;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbqueue.service.DbQueueService;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertFalse;

@ParametersAreNonnullByDefault
@ContextConfiguration(classes = DbQueueTestingConfiguration.class)
public class DbQueueTest {
    private static final int SHARD = 1;
    private static final int MAX_ATTEMPTS = 5;
    private static final ClientId CLIENT_ID = ClientId.fromLong(1);
    private static final TemporalUnitWithinOffset FEW_SECONDS = new TemporalUnitWithinOffset(3, ChronoUnit.SECONDS);

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private DbQueueRepository dbQueueRepository;

    @Autowired
    private DbQueueService dbQueueService;

    @Autowired
    private DbQueueSteps dbQueueSteps;

    public static class JobArgs {
        private final Long entityId;

        @JsonCreator
        @SuppressWarnings("WeakerAccess") // не может быть pkg-private, это JsonCreator
        public JobArgs(@JsonProperty("entityId") Long entityId) {
            this.entityId = checkNotNull(entityId);
        }

        @SuppressWarnings("unused") // jackson использует
        public Long getEntityId() {
            return entityId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            JobArgs jobArgs = (JobArgs) o;
            return Objects.equals(entityId, jobArgs.entityId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityId);
        }

        @Override
        public String toString() {
            return "JobArgs{" +
                    "entityId=" + entityId +
                    '}';
        }
    }

    public static class JobResult {
        private final Long resultingEntityId;
        private final String error;

        private JobResult(@Nullable Long resultingEntityId, @Nullable String error) {
            this.resultingEntityId = resultingEntityId;
            this.error = error;
        }

        @JsonCreator
        public static JobResult fromJson(
                @JsonProperty("resultingEntityId") @Nullable Long resultingEntityId,
                @JsonProperty("error") @Nullable String error) {
            return new JobResult(resultingEntityId, error);
        }

        public static JobResult success(Long resultingEntityId) {
            return new JobResult(checkNotNull(resultingEntityId), null);
        }

        public static JobResult error(DbQueueJob<JobArgs, JobResult> result, String error) {
            return new JobResult(null, checkNotNull(error));
        }

        @SuppressWarnings("unused") // jackson использует
        public Long getResultingEntityId() {
            return resultingEntityId;
        }

        public String getError() {
            return error;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            JobResult jobResult = (JobResult) o;
            return Objects.equals(resultingEntityId, jobResult.resultingEntityId) &&
                    Objects.equals(error, jobResult.error);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resultingEntityId, error);
        }

        @Override
        public String toString() {
            return "JobResult{" +
                    "resultingEntityId=" + resultingEntityId +
                    ", error='" + error + '\'' +
                    '}';
        }
    }

    private static class JobExecutionException extends RuntimeException {
    }

    private static final DbQueueJobType<JobArgs, JobResult> JOB_TYPE =
            new DbQueueJobType<>("test", JobArgs.class, JobResult.class);

    @Before
    public void setUp() {
        dbQueueSteps.registerJobType(JOB_TYPE);
        dbQueueSteps.clearQueue(JOB_TYPE);
    }

    @Test
    public void insertJobTest() {
        JobArgs args = new JobArgs(53L);
        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.NEW);
            softly.assertThat(fetchedJob.getArgs()).isEqualTo(args);
        });
    }

    @Test
    public void insertAndGrabJobTest() {
        JobArgs args = new JobArgs(53L);

        dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        DbQueueJob<JobArgs, JobResult> grabbedJob = dbQueueRepository.grabSingleJob(SHARD, JOB_TYPE);

        assertSoftly(softly -> {
            softly.assertThat(grabbedJob).isNotNull();
            softly.assertThat(grabbedJob.getStatus()).isEqualTo(DbQueueJobStatus.GRABBED);
            softly.assertThat(grabbedJob.getArgs()).isEqualTo(args);
            softly.assertThat(grabbedJob.getTryCount()).isEqualTo(1L);
        });
    }

    @Test
    public void insertAndExecuteJobTest() {
        JobArgs args = new JobArgs(53L);
        JobResult successfulResult = JobResult.success(68L);

        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        dbQueueService.grabAndProcessJob(SHARD, JOB_TYPE,
                grabbedJob -> successfulResult,
                MAX_ATTEMPTS,
                JobResult::error);

        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.FINISHED);
            softly.assertThat(fetchedJob.getArgs()).isEqualTo(args);
            softly.assertThat(fetchedJob.getResult()).isEqualTo(successfulResult);
        });
    }

    @Test
    public void jobExecutionSingleFailureTest() {
        JobArgs args = new JobArgs(53L);
        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        dbQueueService.grabAndProcessJob(SHARD, JOB_TYPE,
                grabbedJob -> {
                    throw new JobExecutionException();
                },
                MAX_ATTEMPTS,
                JobResult::error);

        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.NEW);
            softly.assertThat(fetchedJob.getArgs()).isEqualTo(args);
            softly.assertThat(fetchedJob.getTryCount()).isEqualTo(1L);
        });
    }

    @Test
    public void jobExecutionWithTryLaterException() {
        JobArgs args = new JobArgs(53L);
        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);

        JobArgs updatedArgs = new JobArgs(35L);
        dbQueueService.grabAndProcessJob(SHARD, JOB_TYPE,
                grabbedJob -> {
                    throw new JobFailedWithTryLaterException(Duration.ofDays(1), updatedArgs);
                },
                MAX_ATTEMPTS,
                JobResult::error);

        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.NEW);
            softly.assertThat(fetchedJob.getArgs()).isEqualTo(updatedArgs);
            softly.assertThat(fetchedJob.getTryCount()).isEqualTo(1L);
            softly.assertThat(fetchedJob.getRunAfter()).isAfter(LocalDateTime.now());
        });
    }

    @Test
    public void jobExecutionRetryTest() {
        JobArgs args = new JobArgs(53L);
        JobResult successfulResult = JobResult.success(68L);
        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        MutableLong attempts = new MutableLong(0);
        for (int currentAttempt = 0; currentAttempt < 2; currentAttempt++) {
            dbQueueService.grabAndProcessJob(SHARD, JOB_TYPE,
                    grabbedJob -> {
                        attempts.increment();
                        if (attempts.getValue() > 1) {
                            return successfulResult;
                        }
                        throw new JobExecutionException();
                    },
                    MAX_ATTEMPTS,
                    JobResult::error);
        }

        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.FINISHED);
            softly.assertThat(fetchedJob.getArgs()).isEqualTo(args);
            softly.assertThat(fetchedJob.getResult()).isEqualTo(successfulResult);
            softly.assertThat(fetchedJob.getTryCount()).isEqualTo(2L);
        });
    }

    @Test
    public void jobExecutionRepeatedFailureTest() {
        JobArgs args = new JobArgs(53L);
        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        for (int currentAttempt = 0; currentAttempt < MAX_ATTEMPTS; currentAttempt++) {
            dbQueueService.grabAndProcessJob(SHARD, JOB_TYPE,
                    grabbedJob -> {
                        throw new JobExecutionException();
                    },
                    MAX_ATTEMPTS,
                    JobResult::error);
        }

        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.FAILED);
            softly.assertThat(fetchedJob.getArgs()).isEqualTo(args);
            softly.assertThat(fetchedJob.getResult().getError()).isNotBlank();
            softly.assertThat(fetchedJob.getTryCount()).isEqualTo(5L);
        });
    }

    @Test
    public void jobExecutionPermanentlyFailureTest() {
        JobArgs args = new JobArgs(53L);
        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        dbQueueService.grabAndProcessJob(SHARD, JOB_TYPE,
                grabbedJob -> {
                    throw new JobFailedPermanentlyException();
                },
                MAX_ATTEMPTS,
                JobResult::error);

        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.FAILED);
            softly.assertThat(fetchedJob.getArgs()).isEqualTo(args);
            softly.assertThat(fetchedJob.getResult().getError()).isNotBlank();
            softly.assertThat(fetchedJob.getTryCount()).isEqualTo(1L);
        });
    }

    @Test
    @SuppressWarnings("squid:S2925") // Thread.sleep is fine
    public void workerMissingInActionRetryTest() throws InterruptedException {
        JobArgs args = new JobArgs(53L);
        JobResult successfulResult = JobResult.success(68L);

        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);

        dbQueueRepository.grabSingleJob(SHARD, JOB_TYPE, Duration.ofSeconds(1));
        Thread.sleep(2000L);

        dbQueueService.grabAndProcessJob(SHARD, JOB_TYPE,
                grabbedJob -> successfulResult,
                MAX_ATTEMPTS,
                JobResult::error);

        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.FINISHED);
            softly.assertThat(fetchedJob.getArgs()).isEqualTo(args);
            softly.assertThat(fetchedJob.getResult()).isEqualTo(successfulResult);
            softly.assertThat(fetchedJob.getTryCount()).isEqualTo(2L);
        });

    }

    @Test
    public void failedGrabOnTimeLessThanRunAfter() {
        JobArgs args = new JobArgs(53L);
        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        dbQueueRepository.markJobFailedOnce(SHARD, job, LocalDateTime.now().plusMinutes(1));

        boolean grabbed = dbQueueService.grabAndProcessJob(SHARD, JOB_TYPE,
                grabbedJob -> JobResult.success(68L),
                MAX_ATTEMPTS,
                JobResult::error);

        assertFalse(grabbed);
    }

    @Test
    public void insertMultipleJobsAndGrabSingleJobTest() {
        JobArgs args1 = new JobArgs(42L);
        JobArgs args2 = new JobArgs(43L);
        dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args1);
        dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args2);

        DbQueueJob<JobArgs, JobResult> grabbedJob = dbQueueRepository.grabSingleJob(SHARD, JOB_TYPE);
        assertThat(grabbedJob).isNotNull();
    }

    @Test
    public void markJobFailedOnceTest() {
        JobArgs args = new JobArgs(53L);

        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        dbQueueRepository.markJobFailedOnce(SHARD, job);

        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.NEW);
            softly.assertThat(fetchedJob.getGrabbedUntil()).isNull();
            softly.assertThat(fetchedJob.getRunAfter()).isNull();
        });
    }

    @Test
    public void markJobFailedOnceRunAfterTest() {
        JobArgs args = new JobArgs(53L);
        LocalDateTime runAfter = LocalDateTime.now();

        DbQueueJob<JobArgs, JobResult> job = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, 1, args);
        dbQueueRepository.markJobFailedOnce(SHARD, job, runAfter);

        DbQueueJob<JobArgs, JobResult> fetchedJob = dbQueueRepository.findJobById(SHARD, JOB_TYPE, job.getId());

        assertSoftly(softly -> {
            softly.assertThat(fetchedJob).isNotNull();
            softly.assertThat(fetchedJob.getStatus()).isEqualTo(DbQueueJobStatus.NEW);
            softly.assertThat(fetchedJob.getGrabbedUntil()).isNull();
            softly.assertThat(fetchedJob.getRunAfter()).isCloseTo(runAfter, FEW_SECONDS);
        });
    }

    @Test
    public void canFindArchiveJobs() {
        var someArg = 53L;
        var someUid = 1;
        JobArgs args = new JobArgs(someArg);

        var job1 = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, someUid, args);
        var job2 = dbQueueRepository.insertJob(SHARD, JOB_TYPE, CLIENT_ID, someUid, args);
        dbQueueRepository.markJobFinished(SHARD, job1, JobResult.success(someArg));
        dbQueueRepository.markJobFinished(SHARD, job2, JobResult.success(someArg));

        var jobs = dbQueueRepository.findArchivedJobsByIds(SHARD, JOB_TYPE, Set.of(job1.getId(), job2.getId()));

        assertSoftly(softly -> {
            softly.assertThat(jobs).extracting(DbQueueJob::getId).containsExactlyInAnyOrder(job1.getId(), job2.getId());
        });
    }
}
