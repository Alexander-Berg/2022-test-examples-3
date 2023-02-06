package ru.yandex.market.tsum.pipelines.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipelines.common.jobs.tsum.TsumInnerReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.tsum.TsumReleaseJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.TicketsList;
import ru.yandex.market.tsum.pipelines.wood.resources.Wood;
import ru.yandex.market.tsum.release.ReleaseJobTags;

/**
 * @author Nikolay Firov
 * @date 14.12.17
 */
@Configuration
public class TestInnerReleasePipeline {
    @Bean(name = "test-inner-release")
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder startJob = builder.withJob(DummyJob.class)
            .withTitle("start");

        JobBuilder beOrNotToBeRelease = builder.withJob(TsumInnerReleaseJob.class)
            .withTitle("be or not to be release")
            .withUpstreams(startJob)
            .withResources(TsumReleaseJobConfig.builder()
                .withProjectId("test")
                .withPipelineId("test-be-or-not-to-be")
                .withDisabledManualTriggers("A", "C", "OR")
                .withAllowManualPrematureFinish(true)
                .build()
            );

        JobBuilder woodCutterRelease = builder.withJob(WoodReleaseJob.class)
            .withTitle("woodCutter")
            .withUpstreams(startJob)
            .withResources(TsumReleaseJobConfig.builder()
                .withProjectId("test")
                .withPipelineId("wood")
                .build()
            );

        builder.withJob(DummyJob.class)
            .withTitle("end")
            .withUpstreams(beOrNotToBeRelease, woodCutterRelease)
            .withTags(ReleaseJobTags.FINAL_JOB);

        return builder.build();
    }

    public static class WoodReleaseJob extends TsumInnerReleaseJob {
        @Override
        protected Collection<Resource> getResources() {
            return Arrays.asList(new Wood(1, false), new TicketsList());
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("d7bda4fd-f02d-425c-81bf-6357c8d0cef4");
        }
    }
}
