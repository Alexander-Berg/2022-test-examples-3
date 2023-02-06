package ru.yandex.market.tsum.pipelines.test;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.release.ReleaseJobTags;
import ru.yandex.market.tsum.release.ReleaseStages;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 31.07.17
 */
@Configuration
public class TestSimplePipeline {
    public static final String JOB_ID = "job";
    public static final String FIRST_JOB_ID = "first_job";
    public static final String SECOND_JOB_ID = "second_job";
    public static final String SIMPLE_PIPE_ID = "test-simple";
    public static final String SIMPLE_PIPE_WITH_FINAL_JOB_ID = "test-simple-pipe-with-final-job";
    public static final String MT_SIMPLE_PIPE_ID = "mt-" + SIMPLE_PIPE_ID;
    public static final String SIMPLE_PIPE_WITH_MANUAL_TRIGGER_ID = "test-simple-manual-trigger";
    public static final String SIMPLE_STAGE_GROUP_ID = "simple-stages";
    public static final String SIMPLE_STAGED_PIPELINE_ID = "simple-staged-pipeline";
    public static final String TWO_JOB_STAGED_PIPELINE_ID = "two-job-staged-pipeline";
    public static final String SIMPLE_RELEASE_PIPE_ID = "test-simple-release-pipe";

    @Bean(name = SIMPLE_PIPE_ID)
    public Pipeline simplePipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class, JOB_ID);
        return builder.build();
    }

    @Bean(name = SIMPLE_RELEASE_PIPE_ID)
    public Pipeline simpleReleasePipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(ReleaseInfoProducer.class, JOB_ID);
        return builder.withCustomCleanupJob(DummyJob.class).build();
    }

    @Bean(name = SIMPLE_PIPE_WITH_FINAL_JOB_ID)
    public Pipeline simplePipelineWithFinalJob() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder firstJob = builder.withJob(DummyJob.class).withId(FIRST_JOB_ID);

        builder.withJob(DummyJob.class, SECOND_JOB_ID)
            .withUpstreams(firstJob)
            .withTags(ReleaseJobTags.FINAL_JOB);

        builder.withJob(DummyJob.class)
            .withUpstreams(firstJob)
            .withManualTrigger();

        return builder.withCustomCleanupJob(DummyJob.class).build();
    }

    @Bean(name = MT_SIMPLE_PIPE_ID)
    public Pipeline mtSimplePipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class, JOB_ID);
        return builder.withCustomCleanupJob(DummyJob.class).build();
    }

    @Bean(name = SIMPLE_PIPE_WITH_MANUAL_TRIGGER_ID)
    public Pipeline simplePipelineWithManualTrigger() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class, JOB_ID).withPrompt();
        return builder.build();
    }

    @Bean(name = SIMPLE_STAGE_GROUP_ID)
    public ReleaseStages releaseStages() {
        return ReleaseStages.INSTANCE;
    }

    @Bean(name = SIMPLE_STAGED_PIPELINE_ID)
    public Pipeline simpleStagedPipelineWithManualTrigger() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(ReleaseInfoProducer.class, JOB_ID)
            .withManualTrigger()
            .beginStage(releaseStages().testing());

        return builder.withCustomCleanupJob(DummyJob.class).build();
    }

    @Bean(name = TWO_JOB_STAGED_PIPELINE_ID)
    public Pipeline twoJobStagedPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder firstJob = builder.withJob(ReleaseInfoProducer.class)
            .withId(FIRST_JOB_ID)
            .beginStage(releaseStages().testing());

        builder.withJob(DummyJob.class, SECOND_JOB_ID)
            .withUpstreams(firstJob)
            .beginStage(releaseStages().stable());

        return builder.withCustomCleanupJob(DummyJob.class).build();
    }

    @Produces(single = ReleaseInfo.class)
    public static class ReleaseInfoProducer implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("98bfe58e-b9f0-4999-86fe-826950ed004d");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new ReleaseInfo(new FixVersion(1L, "2018.1.1"), "MARKETINFRA-1"));
        }
    }
}
