package ru.yandex.market.tsum.pipelines.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.TeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.TeamcityBuildJob;

/**
 * @author Andrey Yashnev <a href="mailto:andjash@yandex-team.ru"></a>
 * @date 7/11/2017
 */
@Component
@Configuration
public class TestHybridUITestsPipeline {

    public static final String TEST_HYBRID_PIPE_ID = "test-hybrid-uitests";

    @Bean(name = TEST_HYBRID_PIPE_ID)
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withJob(TeamcityBuildJob.class)
            .withTitle("Запуск UI тестов гибридного приложения")
            .withResources(
                TeamcityBuildConfig.builder()
                    .withJobName("Mobile_MobileMarketClientUiTest_AllPlatformTest")
                    .build()
            );

        return builder.build();
    }
}
