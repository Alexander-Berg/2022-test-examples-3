package ru.yandex.market.tsum;

import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.converters.PipelineConverter;
import ru.yandex.market.tsum.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.Stage;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.model.forms.ResourceFieldControl;
import ru.yandex.market.tsum.pipe.engine.source_code.model.forms.controls.VersionFieldControl;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersionName;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;

import static org.mockito.Mockito.mock;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 07.06.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
public class PipelineConverterTest {
    @Autowired
    private MongoConverter mongoConverter;

    private final SourceCodeService sourceCodeService = mock(SourceCodeService.class);

    @Test
    public void convertsAndRestoresPipeline() {
        PipelineConverter pipelineConverter = new PipelineConverter(mongoConverter, sourceCodeService);

        StageGroup stages = new StageGroup("first");
        Stage firstStage = stages.getStage("first");

        PipelineBuilder builder = PipelineBuilder.create();
        builder.withManualResource(
            FixVersionName.class,
            ImmutableMap.<String, ResourceFieldControl>builder()
                .put("name", new VersionFieldControl("MARKETINFRATEST", false))
                .build()
        );

        JobBuilder firstJob = builder
            .withJob(DummyJob.class)
            .withTitle("dummy")
            .withResources(new GithubRepo("market-java/tsum"))
            .beginStage(firstStage);

        JobBuilder secondJob = builder
            .withJob(DummyJob.class)
            .withUpstreams(firstJob)
            .withScheduler()
            .workDaysHours(0, (int) TimeUnit.DAYS.toHours(1))
            .preHolidayHours(0, (int) TimeUnit.DAYS.toHours(1))
            .build()
            .withTitle("dummy2");

        Pipeline pipeline = builder.build();
        pipeline.setBeanName("test-id");

        PipelineEntity entity = pipelineConverter.toEntity(pipeline);

        Pipeline convertedPipeline = pipelineConverter.toPipeline(entity);

        PipelineEntity reconvertedEntity = pipelineConverter.toEntity(convertedPipeline);

        reconvertedEntity.getCurrentConfiguration().setTimestamp(entity.getCurrentConfiguration().getTimestamp());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String stringOne = gson.toJson(entity);
        String stringTwo = gson.toJson(reconvertedEntity);

        Assert.assertEquals(stringOne, stringTwo);
    }
}
