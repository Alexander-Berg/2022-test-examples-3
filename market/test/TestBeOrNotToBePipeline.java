package ru.yandex.market.tsum.pipelines.test;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.AdapterJobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.common.CanRunWhen;
import ru.yandex.market.tsum.pipelines.common.jobs.dummy.SleepDummyJob;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.metrics.ProduceFakeChangelogJob;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;
import ru.yandex.market.tsum.release.ReleaseJobTags;

/**
 * @author Nikolay Firov
 * @date 14.12.17
 */
@Configuration
public class TestBeOrNotToBePipeline {
    @Bean(name = "test-be-or-not-to-be")
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder startJob = builder.withJob(DummyJob.class)
            .withTitle("start");

        Map<String, JobBuilder> secondLineJobs = new HashMap<>();

        for (char name = 'A'; name <= 'D'; name++) {
            secondLineJobs.put(String.valueOf(name),
                builder.withJob(SleepDummyJob.class)
                    .withId(String.valueOf(name))
                    .withUpstreams(startJob)
                    .withManualTrigger()
                    .withTitle(String.valueOf(name))
            );
        }

        AdapterJobBuilder andJob = builder.withAdapterJob("and")
            .withId("AND")
            .withTitle("AND")
            .withUpstreams(secondLineJobs.get("A"), secondLineJobs.get("B"));

        AdapterJobBuilder orJob = builder.withAdapterJob("or")
            .withId("OR")
            .withTitle("OR")
            .withUpstreams(CanRunWhen.ANY_COMPLETED, secondLineJobs.get("C"), secondLineJobs.get("D"));

        JobBuilder manualOrJob = builder.withJob(DummyJob.class)
            .withId("MANUAL_OR")
            .withTitle("Manual or")
            .withUpstreams(CanRunWhen.ANY_COMPLETED, orJob, andJob)
            .withManualTrigger();

        JobBuilder produceFakeChangelogJob = builder.withJob(ProduceFakeChangelogJob.class)
            .withTitle("Produce changelog")
            .withUpstreams(manualOrJob);

        builder.withJob(FinishReleaseJob.class)
            .withTitle("Подсчет метрик")
            .withResources(
                new GithubRepo("market-infra/tsum"),
                FinishReleaseJobConfig.DO_NOTHING_CONFIG
            )
            .withUpstreams(produceFakeChangelogJob)
            .withTags(ReleaseJobTags.FINAL_JOB);

        return builder.build();
    }
}
