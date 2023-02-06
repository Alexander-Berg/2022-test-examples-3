package ru.yandex.market.tsup.core.event.impl.cube;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineStatus;
import ru.yandex.market.tsup.repository.mappers.PipelineMapper;

@DatabaseSetup("/repository/pipeline/two_pipelines_with_cubes_one_redy_to_be_finished.xml")
class FinishPipelineSubscriberTest extends AbstractContextualTest {
    @Autowired
    private FinishPipelineSubscriber subscriber;

    @Autowired
    private PipelineMapper pipelineMapper;

    @Test
    void pipelineFinished() {
        subscriber.accept(new CubePayload(4L));
        PipelineEntity pipeline = pipelineMapper.getById(1L);

        softly.assertThat(pipeline.getStatus()).isEqualTo(PipelineStatus.FINISHED);
    }

    @Test
    void pipelineNotFinished() {
        subscriber.accept(new CubePayload(10L));
        PipelineEntity pipeline = pipelineMapper.getById(2L);

        softly.assertThat(pipeline.getStatus()).isEqualTo(PipelineStatus.EXECUTING);
    }
}
