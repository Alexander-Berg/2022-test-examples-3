package ru.yandex.market.tsup.core.pipeline;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.data.StringIntPayload;
import ru.yandex.market.tsup.domain.entity.TestPipelineName;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeEntity;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineStatus;

class PipelineFactoryTest extends AbstractContextualTest {

    @Autowired
    private PipelineFactory pipelineFactory;

    @Test
    void createPipelineEntity() {

        PipelineFactory.PipelineFactoryResult result = pipelineFactory.createPipelineEntity(
            TestPipelineName.TEST_COMPLICATED_PIPELINE,
            new StringIntPayload().setA("1").setB(1),
            "author"
        );

        softly.assertThat(result.getPipeline()).isEqualTo(pipeline());

        softly.assertThat(result.getCubes()).hasSize(6);
        softly.assertThat(result.getRelations()).hasSize(6);

        Map<PipelineCubeName, List<PipelineCubeEntity>> cubesByName = result.getCubes().stream()
            .collect(Collectors.groupingBy(PipelineCubeEntity::getName));

        softly.assertThat(cubesByName.get(PipelineCubeName.CARRIER_TRANSPORT_CREATOR)).hasSize(2);
        softly.assertThat(
            cubesByName.get(PipelineCubeName.CARRIER_TRANSPORT_CREATOR).get(0)
                != cubesByName.get(PipelineCubeName.CARRIER_TRANSPORT_CREATOR).get(1)
        ).isTrue();

        softly.assertThat(cubesByName.get(PipelineCubeName.CARRIER_COURIER_CREATOR)).hasSize(2);
        softly.assertThat(
            cubesByName.get(PipelineCubeName.CARRIER_COURIER_CREATOR).get(0) !=
                cubesByName.get(PipelineCubeName.CARRIER_TRANSPORT_CREATOR).get(1)
        ).isTrue();
    }

    private PipelineEntity pipeline() {
        return new PipelineEntity()
            .setStatus(PipelineStatus.NEW)
            .setAuthor("author")
            .setName(TestPipelineName.TEST_COMPLICATED_PIPELINE)
            .setPayload(JsonNodeFactory.instance.objectNode().put("a", "1").put("b", 1));
    }
}
