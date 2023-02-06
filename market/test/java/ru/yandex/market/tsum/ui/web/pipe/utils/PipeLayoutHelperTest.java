package ru.yandex.market.tsum.ui.web.pipe.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchParameters;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.model.JobExecutorObject;
import ru.yandex.market.tsum.utils.PipeLayoutHelper;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 27.03.17
 */
public class PipeLayoutHelperTest {
    private static final String START_JOB = "start";
    private static final String TOP_JOB = "top";
    private static final String BOTTOM_1_JOB = "bottom1";
    private static final String BOTTOM_2_JOB = "bottom2";
    private static final String END_JOB = "end";

    private ResourceService resourceService;

    @Before
    public void setUp() {
        resourceService = mock(ResourceService.class);
        when(resourceService.saveResources(any(), any())).thenReturn(StoredResourceContainer.empty());
    }

    @Test
    public void getJobToColumnMap() {
        Pipeline pipeline = getPipeline();

        SourceCodeService sourceCodeService = mock(SourceCodeService.class);
        when(sourceCodeService.getJobExecutor(any(Class.class)))
            .thenReturn(
                new JobExecutorObject(
                    UUID.fromString("c42a609e-b610-4404-9e84-ef5237e4f051"),
                    DummyJob.class,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
                )
            );

        PipeLaunchFactory pipeLaunchFactory = new PipeLaunchFactory(
            resourceService, id -> pipeline, sourceCodeService
        );

        PipeLaunch launch = pipeLaunchFactory.create(
            PipeLaunchParameters.builder()
                .withLaunchRef(PipeLaunchRefImpl.create("any"))
                .withManualResources(ResourceRefContainer.empty())
                .withTriggeredBy("user42")
                .withProjectId("prj")
                .build()
        );

        Map<String, Integer> jobIdToColumnMap = PipeLayoutHelper.getJobIdToColumnMap(launch);

        Assert.assertEquals(0, (int) jobIdToColumnMap.get(START_JOB));
        Assert.assertEquals(1, (int) jobIdToColumnMap.get(TOP_JOB));
        Assert.assertEquals(1, (int) jobIdToColumnMap.get(BOTTOM_1_JOB));
        Assert.assertEquals(2, (int) jobIdToColumnMap.get(BOTTOM_2_JOB));
        Assert.assertEquals(3, (int) jobIdToColumnMap.get(END_JOB));
    }

    private Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder start = builder.withJob(DummyJob.class)
            .withId("start");

        JobBuilder top = builder.withJob(DummyJob.class)
            .withId("top")
            .withUpstreams(start);

        JobBuilder bottom1 = builder.withJob(DummyJob.class)
            .withId("bottom1")
            .withUpstreams(start);

        JobBuilder bottom2 = builder.withJob(DummyJob.class)
            .withId("bottom2")
            .withUpstreams(bottom1);

        JobBuilder end = builder.withJob(DummyJob.class)
            .withId("end")
            .withUpstreams(top, bottom2);

        return builder.build();
    }

    private static class DummyJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("e19b0ebc-ca0c-4a2a-b688-90d59bcb9a58");
        }

        @Override
        public void execute(JobContext context) throws Exception {
        }
    }
}