package ru.yandex.market.tsup.core.pipeline.config;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;

public class PipelineConfigurationTest extends AbstractContextualTest {

    @Autowired
    private Collection<PipelineConfig> pipelineConfigs;

    @Test
    void testConfigHasRootCube() {
        boolean hasRootCube = pipelineConfigs.stream()
            .flatMap(config -> config.getRelations().stream())
            .map(CubeRelationConfig::getFromCube)
            .map(CubeConfig::getCubeName)
            .anyMatch(a -> a.equals(PipelineCubeName.ROOT_CUBE));

        softly.assertThat(hasRootCube).isTrue();
    }
}
