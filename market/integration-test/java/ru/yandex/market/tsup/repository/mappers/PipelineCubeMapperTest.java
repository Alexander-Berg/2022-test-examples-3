package ru.yandex.market.tsup.repository.mappers;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeEntity;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeStatus;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class PipelineCubeMapperTest extends AbstractContextualTest {
    @Autowired
    private PipelineCubeMapper mapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final PipelineCubeEntity pipelineCube = cube(
        2L,
        PipelineCubeName.CARRIER_COURIER_CREATOR,
        PipelineCubeStatus.NEW,
        1L
    );

    @Test
    @DatabaseSetup({
        "/repository/pipeline/after_insert.xml",
        "/repository/pipeline_cube/existing_cubes.xml"
    })
    @ExpectedDatabase(
        value = "/repository/pipeline_cube/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertWithRelations() {
        mapper.insertWithRelations(pipelineCube, Set.of(2L));
    }

    @Test
    @DatabaseSetup({
        "/repository/pipeline/after_insert.xml",
        "/repository/pipeline_cube/after_insert.xml"
    })
    void getById() {
        PipelineCubeEntity byId = mapper.getById(1L);
        assertThatModelEquals(byId, pipelineCube);
    }

    @Test
    @DatabaseSetup("/repository/pipeline/two_pipelines_with_cubes.xml")
    void findCubesInStatusWithAllParentsInStatus() {
        List<Long> newCubesWithAllFinishedParents = mapper.findCubesInStatusWithAllParentsInStatus(
            PipelineCubeStatus.NEW,
            PipelineCubeStatus.FINISHED,
            10,
            null
        );

        softly.assertThat(newCubesWithAllFinishedParents).containsExactlyInAnyOrder(2L, 3L, 7L);

        newCubesWithAllFinishedParents = mapper.findCubesInStatusWithAllParentsInStatus(
            PipelineCubeStatus.NEW,
            PipelineCubeStatus.FINISHED,
            10,
            2L
        );

        softly.assertThat(newCubesWithAllFinishedParents).containsExactlyInAnyOrder(7L);
    }

    @Test
    @DatabaseSetup("/repository/pipeline/two_pipelines_with_cubes.xml")
    @ExpectedDatabase(
        value = "/repository/pipeline/two_pipelines_with_cubes_after_set_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setStatus() {
        mapper.setStatus(
            List.of(2L, 3L, 7L),
            PipelineCubeStatus.EXECUTING
        );
    }

    @Test
    @DatabaseSetup("/repository/pipeline/pipeline_with_cubes.xml")
    void getParents() {
        Set<PipelineCubeEntity> parents = mapper.getParents(3L);
        softly.assertThat(parents).containsExactlyInAnyOrder(
            cube(2L, PipelineCubeName.CARRIER_COURIER_CREATOR, PipelineCubeStatus.NEW, 1L),
            cube(1L, PipelineCubeName.CARRIER_COURIER_CREATOR, PipelineCubeStatus.NEW, 1L)
        );
    }

    @Test
    @DatabaseSetup("/repository/pipeline/two_pipelines_with_cubes.xml")
    void find() {
        List<PipelineCubeEntity> pipelineCubes = mapper.find(1L, PipelineCubeName.CARRIER_COURIER_CREATOR);

        softly.assertThat(pipelineCubes).containsExactlyInAnyOrder(
            cube(1L, PipelineCubeName.CARRIER_COURIER_CREATOR, PipelineCubeStatus.FINISHED, 1L),
            cube(2L, PipelineCubeName.CARRIER_COURIER_CREATOR, PipelineCubeStatus.NEW, 1L),
            cube(3L, PipelineCubeName.CARRIER_COURIER_CREATOR, PipelineCubeStatus.NEW, 1L),
            cube(4L, PipelineCubeName.CARRIER_COURIER_CREATOR, PipelineCubeStatus.NEW, 1L)
        );
    }

    @Test
    @DatabaseSetup("/repository/pipeline/two_pipelines_with_cubes.xml")
    void findNotInStatus() {
        List<PipelineCubeEntity> notInStatus = mapper.findNotInStatus(1L, PipelineCubeStatus.FINISHED);

        softly.assertThat(notInStatus).containsExactlyInAnyOrder(
            cube(2L, PipelineCubeName.CARRIER_COURIER_CREATOR, PipelineCubeStatus.NEW, 1L),
            cube(3L, PipelineCubeName.CARRIER_COURIER_CREATOR, PipelineCubeStatus.NEW, 1L),
            cube(4L, PipelineCubeName.CARRIER_COURIER_CREATOR, PipelineCubeStatus.NEW, 1L)
        );

        mapper.setStatus(List.of(2L, 3L, 4L), PipelineCubeStatus.FINISHED);
        notInStatus = mapper.findNotInStatus(1L, PipelineCubeStatus.FINISHED);

        softly.assertThat(notInStatus).isEmpty();
    }

    @Test
    @DatabaseSetup("/repository/pipeline/pipeline_with_cubes.xml")
    @ExpectedDatabase(
        value = "/repository/pipeline/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateResult() throws IOException {
        mapper.updateResult(1L, objectMapper.readTree("{\"id\":1}"));
    }

    @Test
    @DatabaseSetup("/repository/pipeline/pipeline_with_cubes.xml")
    @DatabaseSetup(
        value = "/repository/dbqueue/logs.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/pipeline_cube/after_log_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLog() {
        mapper.updateLog(1L, 2L);
    }

    private static PipelineCubeEntity cube(
        Long id,
        PipelineCubeName name,
        PipelineCubeStatus status,
        Long pipelineId
    ) {
        PipelineCubeEntity pipelineCube = new PipelineCubeEntity()
            .setId(id)
            .setName(name)
            .setStatus(status)
            .setPipelineId(pipelineId);

        return pipelineCube;
    }
}
