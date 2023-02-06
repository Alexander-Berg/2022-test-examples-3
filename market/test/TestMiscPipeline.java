package ru.yandex.market.tsum.pipelines.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipelines.common.jobs.jenkins.JenkinsJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.jenkins.LoadJenkinsJob;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 08/06/2017
 */
@Component
@Configuration
public class TestMiscPipeline {

    @Bean(name = "test-misc")
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder build = builder.withJob(LoadJenkinsJob.class)
            .withTitle("Огонь батарея!")
            .withResources(
                JenkinsJobConfig.newBuilder("MARKETVERSTKA-frontend-manual-runtime")
                    .addParam("VHOST", "desktop-stress.market.fslb01ht.yandex.ru")
                    .addParam("RPS_SCHEDULE", "const(30,1m)")
                    .addParam("ssl", "true")
                    .addParam("nanny_service", "testing_market_front_desktop_load_sas")
                    .build()
            ).withResources(new ReleaseInfo(null, "MARKETINFRA-1303"));

        return builder.build();
    }
}
