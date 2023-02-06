package ru.yandex.market.tsum.dao;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.converters.PipelineConverter;
import ru.yandex.market.tsum.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersionName;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;
import ru.yandex.market.tsum.release.dao.ReleasePipeInfo;
import ru.yandex.market.tsum.release.dao.title_providers.OrdinalTitleProvider;

import static org.mockito.Mockito.mock;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 07.06.18
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
public class PipelinesDaoTest {
    @Autowired
    private MongoConverter mongoConverter;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final SourceCodeService sourceCodeService = mock(SourceCodeService.class);

    private PipelineConverter converter;

    @Before
    public void setup() {
        converter = new PipelineConverter(mongoConverter, sourceCodeService);
    }

    @Test
    public void equalsForSame() {
        Pipeline pipeline = getPipeline();

        PipelineEntity one = converter.toEntity(pipeline);
        PipelineEntity two = converter.toEntity(pipeline);

        boolean areEquals = PipelinesDao.isConfigurationsEqual(
            one.getCurrentConfiguration(), two.getCurrentConfiguration()
        );

        Assert.assertTrue("Configurations are not equals", areEquals);
    }

    @Test
    public void notEqualsForChanged() {
        Pipeline pipeline = getPipeline();

        PipelineEntity one = converter.toEntity(pipeline);
        PipelineEntity two = converter.toEntity(pipeline);

        two.getCurrentConfiguration().getJobs().iterator().next().setTitle("changed");

        boolean areEquals = PipelinesDao.isConfigurationsEqual(
            one.getCurrentConfiguration(), two.getCurrentConfiguration()
        );

        Assert.assertFalse("Configurations are equals", areEquals);
    }

    @Test
    public void notEqualsManualTriggerChanged() {
        Pipeline pipeline = getPipeline();

        PipelineEntity one = converter.toEntity(pipeline);
        PipelineEntity two = converter.toEntity(pipeline);

        one.getCurrentConfiguration().getJobs().iterator().next().setManualTrigger(false);
        two.getCurrentConfiguration().getJobs().iterator().next().setManualTrigger(true);

        boolean areEquals = PipelinesDao.isConfigurationsEqual(
            one.getCurrentConfiguration(), two.getCurrentConfiguration()
        );

        Assert.assertFalse("Configurations are equals", areEquals);
    }

    @Test
    public void doesNotOverwriteEqualsConfigurations() {
        Pipeline pipeline = getPipeline();

        mongoTemplate.save(converter.toEntity(pipeline));

        PipelinesDao pipelinesDao = new PipelinesDao(
            mongoTemplate, Collections.singletonList(pipeline), null, sourceCodeService, converter, null
        );

        pipelinesDao.copySourceCodePipelinesToDb();

        PipelineEntity pipelineEntity = mongoTemplate.findById(pipeline.getId(), PipelineEntity.class);
        Assert.assertEquals(1, pipelineEntity.getCurrentConfiguration().getVersion());
    }

    private ReleasePipeInfo getReleaseInfo() {
        return ReleasePipeInfo.create("test-id", "test", "test", new OrdinalTitleProvider(null));
    }

    private Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withManualResource(FixVersionName.class);

        JobBuilder firstJob = builder
            .withJob(DummyJob.class)
            .withTitle("dummy")
            .withResources(new GithubRepo("market-java/tsum"));

        JobBuilder secondJob = builder
            .withJob(DummyJob.class)
            .withUpstreams(firstJob)
            .withTitle("dummy2");

        Pipeline pipeline = builder.build();
        pipeline.setBeanName("test-id");

        return pipeline;
    }
}
