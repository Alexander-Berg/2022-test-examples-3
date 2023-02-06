package ru.yandex.market.tsum.pipelines.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipelines.common.jobs.juggler.JugglerWatchJob;
import ru.yandex.market.tsum.pipelines.common.jobs.juggler.JugglerWatchJobConfig;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 21/07/2017
 */
@Component
@Configuration
public class TestJugglerPipeline {
    @Bean(name = "test-juggler")
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder watch = builder.withJob(JugglerWatchJob.class)
            .withTitle("Watching Juggler")
            .withResources(
                JugglerWatchJobConfig.newBuilder()
                    .addHost("market_front_desktop")
                    .addService("ping")
                    .build()
            );

        return builder.build();
    }

    @Bean(name = "test-juggler-not-existing")
    public Pipeline pipelineForNotExisting() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder watch = builder.withJob(JugglerWatchJob.class)
            .withTitle("Watching Juggler")
            .withResources(
                JugglerWatchJobConfig.newBuilder()
                    .addHost("market_front_desktop_not_found")
                    .addService("ping")
                    .build()
            );

        return builder.build();
    }
}
