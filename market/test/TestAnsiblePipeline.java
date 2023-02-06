package ru.yandex.market.tsum.pipelines.test;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.AbstractTeamcityBuildJob;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.AnsibleTeamcityConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.TeamcityBuildConfig;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 23.08.2017
 */
@Configuration
public class TestAnsiblePipeline {
    @Bean(name = "ansible")
    public Pipeline ansible() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder dummyJob = builder.withJob(MyAnsibleJob.class);

        return builder.build();
    }


    public static class MyAnsibleJob extends AbstractTeamcityBuildJob {
        @Override
        protected TeamcityBuildConfig getTeamcityConfig(JobContext context) {
            return AnsibleTeamcityConfig.builder()
                .withHostGroup("myHostGroup")
                .withHosts("host1", "host2")
                .withTimeoutMinutes(1)
                .withYmlFilePaths("playbook/services/image_build.yml")
                .withParameter("p1", "v1")
                .withParameter("p2", "v2")
                .build()
                .toTeamcityBuildConfig();
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("c2fc4b7d-fcaa-4bfe-a0df-393b19df08de");
        }
    }
}
