package ru.yandex.market.tsup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsup.core.pipeline.config.CubeConfigImpl;
import ru.yandex.market.tsup.core.pipeline.config.PipelineConfig;
import ru.yandex.market.tsup.core.pipeline.config.PipelineConfigImpl;
import ru.yandex.market.tsup.core.pipeline.data.StringIntPayload;
import ru.yandex.market.tsup.domain.entity.TestPipelineName;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;

@Configuration
public class PipelineConfiguration {

    @Bean
    PipelineConfig simplePipeline() {
        return PipelineConfigImpl
            .builder(TestPipelineName.TEST_SIMPLE_PIPELINE, StringIntPayload.class)
            .withRelation(PipelineCubeName.ROOT_CUBE, PipelineCubeName.CARRIER_TRANSPORT_CREATOR)
            .build();
    }

    @Bean
    PipelineConfig complicatedPipeline() {

        CubeConfigImpl root = cube(PipelineCubeName.ROOT_CUBE, 0);
        CubeConfigImpl partner0 = cube(PipelineCubeName.CARRIER_TRANSPORT_CREATOR, 0);
        CubeConfigImpl point0 = cube(PipelineCubeName.CARRIER_COURIER_CREATOR, 0);
        CubeConfigImpl partner1 = cube(PipelineCubeName.CARRIER_TRANSPORT_CREATOR, 1);
        CubeConfigImpl point1 = cube(PipelineCubeName.CARRIER_COURIER_CREATOR, 1);
        CubeConfigImpl transport = cube(PipelineCubeName.ROUTE_SCHEDULE_CREATOR, 0);

        return PipelineConfigImpl
            .builder(TestPipelineName.TEST_COMPLICATED_PIPELINE, StringIntPayload.class)
            .withRelation(root, partner0)
            .withRelation(root, partner1)
            .withRelation(partner0, point0)
            .withRelation(partner1, point1)
            .withRelation(point0, transport)
            .withRelation(point1, transport)
            .build();
    }

    private CubeConfigImpl cube(PipelineCubeName name, int index) {
        return CubeConfigImpl.builder()
            .cubeName(name)
            .index(index)
            .build();
    }
}
