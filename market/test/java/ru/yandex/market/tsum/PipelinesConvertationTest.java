package ru.yandex.market.tsum;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.config.PipelineScanConfiguration;
import ru.yandex.market.tsum.converters.PipelineConverter;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.entity.pipeline.PipelineConfigurationEntity;
import ru.yandex.market.tsum.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.runtime.PipeProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.ReflectionsSourceCodeProvider;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.SourceCodeServiceImpl;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 04.05.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, PipelineScanConfiguration.class, MockCuratorConfig.class,

})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PipelinesConvertationTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PipeProvider pipeProvider;

    @Autowired
    private ListableBeanFactory listableBeanFactory;

    private PipelinesDao pipelinesDao;

    private PipelineConverter pipelineConverter;

    private final SourceCodeService sourceCodeService = new SourceCodeServiceImpl(
        new ReflectionsSourceCodeProvider(ReflectionsSourceCodeProvider.SOURCE_CODE_PACKAGE)
    );

    @Before
    public void setUp() {
        pipelineConverter = new PipelineConverter(mongoTemplate.getConverter(), sourceCodeService);
        pipelinesDao = new PipelinesDao(
            mongoTemplate, Collections.emptyList(), null, sourceCodeService, pipelineConverter, null
        );
    }

    @Test
    public void convertReleasePipelines() {
        List<Pipeline> pipelines = Stream.of(listableBeanFactory.getBeanNamesForType(Pipeline.class))
            .map(pipeId -> pipeProvider.get(pipeId))
            .collect(Collectors.toList());

        for (Pipeline pipeline : pipelines) {
            try {
                testConverts(pipeline);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to test pipeline " + pipeline.getId(), e);
            }
        }
    }

    private void testConverts(Pipeline pipeline) {
        PipelineEntity converted = pipelineConverter.toEntity(pipeline);

        pipelinesDao.save(converted);

        Pipeline restoredPipeline = pipelineConverter.toPipeline(pipelinesDao.get(converted.getId()));

        PipelineEntity reconverted = pipelineConverter.toEntity(restoredPipeline);

        PipelineConfigurationEntity configurationOne = new PipelineConfigurationEntity(
            converted.getCurrentConfiguration()
        );

        PipelineConfigurationEntity configurationTwo = new PipelineConfigurationEntity(
            reconverted.getCurrentConfiguration()
        );

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String stringOne = gson.toJson(configurationOne);
        String stringTwo = gson.toJson(configurationTwo);

        Assert.assertEquals(stringOne, stringTwo);
    }
}
