package ru.yandex.market.tsum.pipelines.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJob;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 13.04.17
 */
@Component
@Configuration
public class TestTeamcityNannyRecipePipeline {

    @Bean(name = "test-teamcity-nanny-recipe")
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder build = builder.withJob(MarketTeamcityBuildJob.class)
            .withTitle("Сборка")
            .withResources(
                MarketTeamcityBuildConfig.builder()
                    .withJobName("MarketInfra_Sandbox_TestAppUpload")
                    .build()
            );

        JobBuilder nannyDeploy = builder.withJob(NannyReleaseJob.class)
            .withTitle("Выкладка через Nanny рецепт")
            .withUpstreams(build)
            .withResources(
                new NannyReleaseJobConfig(SandboxReleaseType.TESTING, "market-infra-test", "test-recipe")
            );

        return builder.build();
    }
}
