package ru.yandex.market.tsup.core.pipeline;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.PipelineFactory.PipelineFactoryResult;
import ru.yandex.market.tsup.domain.entity.TestPipelineName;
import ru.yandex.market.tsup.domain.entity.pipeline.CubeRelation;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeEntity;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeStatus;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineStatus;

@DatabaseSetup("/repository/pipeline/save/before.xml")
class PipelineSaverTest extends AbstractContextualTest {

    @Autowired
    private PipelineSaver pipelineSaver;

    @ExpectedDatabase(
        value = "/repository/pipeline/save/after.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void save() {
        PipelineFactoryResult pipeline = buildPipeline();
        pipelineSaver.save(pipeline);
    }

    private PipelineFactoryResult buildPipeline() {
        return new PipelineFactoryResult(pipeline(), cubes(), relations());
    }

    private PipelineEntity pipeline() {
        return new PipelineEntity()
            .setName(TestPipelineName.TEST_COMPLICATED_PIPELINE)
            .setStatus(PipelineStatus.NEW)
            .setAuthor("staff-login")
            .setPayload(JsonNodeFactory.instance.objectNode().put("a", "11").put("b", 11));
    }

    private List<PipelineCubeEntity> cubes() {
        return List.of(
            cube(101L, PipelineCubeName.ROOT_CUBE),
            cube(102L, PipelineCubeName.CARRIER_TRANSPORT_CREATOR),
            cube(103L, PipelineCubeName.CARRIER_TRANSPORT_CREATOR),
            cube(104L, PipelineCubeName.CARRIER_COURIER_CREATOR),
            cube(105L, PipelineCubeName.CARRIER_COURIER_CREATOR),
            cube(106L, PipelineCubeName.ROUTE_SCHEDULE_CREATOR)
        );
    }

    private PipelineCubeEntity cube(long id, PipelineCubeName name) {
        return new PipelineCubeEntity()
            .setId(id)
            .setName(name)
            .setStatus(PipelineCubeStatus.NEW);
    }

    private Set<CubeRelation> relations() {
        return Set.of(
            new CubeRelation(101L, 102L),
            new CubeRelation(101L, 103L),
            new CubeRelation(102L, 104L),
            new CubeRelation(103L, 105L),
            new CubeRelation(104L, 106L),
            new CubeRelation(105L, 106L)
        );
    }
}
