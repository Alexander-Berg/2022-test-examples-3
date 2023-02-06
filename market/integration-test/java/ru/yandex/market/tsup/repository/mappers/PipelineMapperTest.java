package ru.yandex.market.tsup.repository.mappers;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineNameImpl;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineStatus;

public class PipelineMapperTest extends AbstractContextualTest {

    @Autowired
    private PipelineMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    private PipelineEntity pipeline;

    @BeforeEach
    void init() throws IOException {
        pipeline = pipeline(
            1L,
            "aidenne",
            PipelineNameImpl.QUICK_TRIP_CREATOR,
            PipelineStatus.NEW
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        mapper.insert(pipeline);
    }

    @Test
    @DatabaseSetup("/repository/pipeline/after_insert.xml")
    void get() {
        PipelineEntity byId = mapper.getById(1L);
        assertThatModelEquals(pipeline, byId);
    }

    @Test
    @DatabaseSetup("/repository/pipeline/pipelines_in_different_statuses.xml")
    void findInStatus() throws IOException {
        List<PipelineEntity> errorPipelines = mapper.findInStatus(PipelineStatus.ERROR, 2);

        softly.assertThat(errorPipelines).containsExactlyInAnyOrder(
            pipeline(4L, "aezhko", PipelineNameImpl.QUICK_TRIP_CREATOR, PipelineStatus.ERROR),
            pipeline(5L, "aezhko", PipelineNameImpl.QUICK_TRIP_CREATOR, PipelineStatus.ERROR)
        );
    }

    @Test
    @DatabaseSetup("/repository/pipeline/pipelines_in_different_statuses.xml")
    @ExpectedDatabase(
        value = "/repository/pipeline/pipelines_in_different_statuses_after_set_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setStatus() {
        mapper.setStatus(List.of(2L, 3L), PipelineStatus.FINISHED);
    }

    private PipelineEntity pipeline(
        Long id,
        String author,
        PipelineNameImpl name,
        PipelineStatus status
    ) throws IOException {
        return new PipelineEntity()
            .setId(id)
            .setAuthor(author)
            .setName(name)
            .setPayload(objectMapper.readTree("{\"transportationId\":1}"))
            .setStatus(status);
    }
}
